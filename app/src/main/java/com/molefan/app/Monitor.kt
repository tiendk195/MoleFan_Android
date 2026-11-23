package com.molefan.app

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.app.ActivityManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import java.io.File
import kotlin.math.abs

data class Snap(
    val cpu: Float? = null, val cpuFromFreq: Boolean = false,
    val ramUsed: Long = 0, val ramTotal: Long = 1,
    val stUsed: Long = 0, val stTotal: Long = 1,
    val batLevel: Int = 0, val batTemp: Float = 0f, val charging: Boolean = false,
    val batMa: Int = 0, val batV: Float = 0f, val cycles: Int = -1,
    val thermal: Int = 0, val headroom: Float = Float.NaN,
    val sensors: List<Pair<String, Float>> = emptyList(),
    val rx: Long = 0, val tx: Long = 0, val uptimeMs: Long = 0,
)

class Monitor(private val ctx: Context) {
    var snap by mutableStateOf(Snap())
    val cpuHist = mutableStateListOf<Float>()
    val tempHist = mutableStateListOf<Float>()
    private var pTot = 0L; private var pIdle = 0L
    private var pRx = 0L; private var pTx = 0L; private var pT = 0L

    private fun rd(p: String): String? = try { File(p).readText().trim() } catch (e: Exception) { null }

    private fun cpuStat(): Float? = try {
        val l = File("/proc/stat").bufferedReader().use { it.readLine() }
        val p = l.trim().split(Regex("\\s+")).drop(1).map { it.toLong() }
        val idle = p[3] + p[4]; val tot = p.sum()
        val r = if (pTot > 0 && tot > pTot) 100f * (1f - (idle - pIdle).toFloat() / (tot - pTot)) else null
        pTot = tot; pIdle = idle; r
    } catch (e: Exception) { null }

    private fun freqLoad(): Float? {
        var c = 0f; var n = 0
        for (i in 0 until Runtime.getRuntime().availableProcessors()) {
            val b = "/sys/devices/system/cpu/cpu$i/cpufreq/"
            val cur = rd(b + "scaling_cur_freq")?.toFloatOrNull()
            val mx = rd(b + "cpuinfo_max_freq")?.toFloatOrNull()
            if (cur != null && mx != null && mx > 0) { c += cur / mx; n++ }
        }
        return if (n > 0) 100f * c / n else null
    }

    private fun zones(): List<Pair<String, Float>> {
        val out = ArrayList<Pair<String, Float>>()
        try {
            File("/sys/class/thermal").listFiles()?.filter { it.name.startsWith("thermal_zone") }?.forEach {
                val t = rd(it.path + "/temp")?.toFloatOrNull() ?: return@forEach
                val c = if (abs(t) > 200) t / 1000f else t
                if (c in 1f..150f) out.add((rd(it.path + "/type") ?: it.name) to c)
            }
        } catch (e: Exception) { }
        return out.sortedByDescending { it.second }.take(14)
    }

    fun read(): Snap {
        var cpu = cpuStat(); var fromFreq = false
        if (cpu == null && pTot == 0L) { /* first sample */ }
        if (cpu == null) { val f = freqLoad(); if (f != null) { cpu = f; fromFreq = true } }
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo(); am.getMemoryInfo(mi)
        val sf = StatFs(Environment.getDataDirectory().path)
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val scale = i?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val level = (i?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0) * 100 / maxOf(scale, 1)
        val st = i?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val cur = abs(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))
        val ma = if (cur > 20000) cur / 1000 else cur
        val cyc = if (Build.VERSION.SDK_INT >= 34) (i?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1) else -1
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        val head = try { pm.getThermalHeadroom(10) } catch (e: Exception) { Float.NaN }
        val now = SystemClock.elapsedRealtime()
        val rx = TrafficStats.getTotalRxBytes(); val tx = TrafficStats.getTotalTxBytes()
        val dt = if (pT > 0) (now - pT) / 1000.0 else 0.0
        val rxR = if (dt > 0 && pRx > 0 && rx >= 0) ((rx - pRx) / dt).toLong() else 0L
        val txR = if (dt > 0 && pTx > 0 && tx >= 0) ((tx - pTx) / dt).toLong() else 0L
        pRx = rx; pTx = tx; pT = now
        return Snap(
            cpu = cpu, cpuFromFreq = fromFreq,
            ramUsed = mi.totalMem - mi.availMem, ramTotal = mi.totalMem,
            stUsed = sf.totalBytes - sf.availableBytes, stTotal = sf.totalBytes,
            batLevel = level, batTemp = (i?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f,
            charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL,
            batMa = ma, batV = (i?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000f, cycles = cyc,
            thermal = pm.currentThermalStatus, headroom = head, sensors = zones(),
            rx = rxR, tx = txR, uptimeMs = now,
        )
    }

    fun apply(s: Snap) {
        snap = s
        s.cpu?.let { cpuHist.add(it.coerceIn(0f, 100f)); if (cpuHist.size > 90) cpuHist.removeAt(0) }
        tempHist.add(s.batTemp); if (tempHist.size > 90) tempHist.removeAt(0)
    }

    fun score(s: Snap = snap): Int {
        val ramP = 100f * s.ramUsed / s.ramTotal
        val stP = 100f * s.stUsed / s.stTotal
        var sc = 100f
        sc -= (s.cpu ?: 0f) * 0.2f
        sc -= maxOf(0f, ramP - 60f) * 0.4f
        sc -= maxOf(0f, stP - 80f) * 0.6f
        sc -= maxOf(0f, s.batTemp - 35f) * 1.8f
        sc -= s.thermal * 6f
        return sc.toInt().coerceIn(0, 100)
    }
}
