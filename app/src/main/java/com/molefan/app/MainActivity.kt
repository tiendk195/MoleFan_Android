package com.molefan.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@Volatile var sink = 0.0

class Pal(val bg: Color, val card: Color, val text: Color, val sub: Color, val accent: Color, val dark: Boolean)
val DarkPal = Pal(Color(0xFF241C14), Color(0xFF362B1F), Color(0xFFF5ECDD), Color(0xFFB8A98F), Color(0xFFE8A33D), true)
val LightPal = Pal(Color(0xFFFAF2E0), Color(0xFFFFFBF0), Color(0xFF3A2E1E), Color(0xFF8A7A5E), Color(0xFFC77D12), false)
val LocalPal = compositionLocalOf { DarkPal }

fun heat(c: Float) = when { c < 38 -> Color(0xFF4CAF50); c < 43 -> Color(0xFFC0CA33); c < 48 -> Color(0xFFFF9800); else -> Color(0xFFE53935) }
fun load(p: Float) = when { p < 50 -> Color(0xFF4CAF50); p < 80 -> Color(0xFFFF9800); else -> Color(0xFFE53935) }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("p", 0) }
    remember { I18n.lang = prefs.getString("lang", "vi") ?: "vi"; 0 }
    var themeMode by remember { mutableIntStateOf(prefs.getInt("theme", 0)) }
    val dark = when (themeMode) { 1 -> false; 2 -> true; else -> androidx.compose.foundation.isSystemInDarkTheme() }
    val pal = if (dark) DarkPal else LightPal
    val mon = remember { Monitor(ctx) }
    LaunchedEffect(Unit) {
        while (true) {
            val s = withContext(Dispatchers.Default) { mon.read() }
            mon.apply(s); delay(1000)
        }
    }
    var tab by remember { mutableIntStateOf(0) }
    val scheme = if (dark) darkColorScheme(primary = pal.accent, surface = pal.card, onSurface = pal.text, background = pal.bg)
    else lightColorScheme(primary = pal.accent, surface = pal.card, onSurface = pal.text, background = pal.bg)
    CompositionLocalProvider(LocalPal provides pal) {
        MaterialTheme(colorScheme = scheme) {
            Column(Modifier.fillMaxSize().background(pal.bg).imePadding()) {
                TopBar(themeMode, { themeMode = it; prefs.edit().putInt("theme", it).apply() },
                    { I18n.lang = it; prefs.edit().putString("lang", it).apply() })
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        0 -> OverviewTab(mon)
                        1 -> ThermalTab(mon)
                        2 -> CleanTab()
                        3 -> AnalyzeTab()
                        4 -> AppsTab()
                        else -> OptimizeTab()
                    }
                }
                BottomBar(tab) { tab = it }
            }
        }
    }
}

@Composable
fun TopBar(theme: Int, onTheme: (Int) -> Unit, onLang: (String) -> Unit) {
    val p = LocalPal.current
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🐹 MoleFan", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Box {
            Text("🌐 " + when (I18n.lang) { "en" -> "EN"; "zh" -> "中文"; else -> "VI" }, color = p.accent, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { menu = true }.padding(horizontal = 10.dp, vertical = 8.dp))
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                listOf("vi" to "Tiếng Việt", "en" to "English", "zh" to "简体中文").forEach { (k, n) ->
                    DropdownMenuItem(text = { Text(n) }, onClick = { onLang(k); menu = false })
                }
            }
        }
        Text(listOf("🌓", "☀️", "🌙")[theme], fontSize = 18.sp,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onTheme((theme + 1) % 3) }.padding(horizontal = 10.dp, vertical = 8.dp))
    }
}

@Composable
fun BottomBar(sel: Int, onSel: (Int) -> Unit) {
    val p = LocalPal.current
    val items = listOf("📊" to "tab.overview", "🌡️" to "tab.thermal", "🧹" to "tab.clean", "🗂️" to "tab.analyze", "📦" to "tab.apps", "⚡" to "tab.optimize")
    Row(Modifier.fillMaxWidth().background(p.card).navigationBarsPadding().padding(vertical = 4.dp)) {
        items.forEachIndexed { i, (e, k) ->
            Column(Modifier.weight(1f).clickable { onSel(i) }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(e, fontSize = 20.sp, color = if (i == sel) p.accent else p.sub)
                Text(tr(k), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (i == sel) p.accent else p.sub, fontWeight = if (i == sel) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(LocalPal.current.card).padding(14.dp), content = content)
}

@Composable
fun Bar(frac: Float, color: Color, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    Box(modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(p.text.copy(alpha = 0.12f))) {
        Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }
}

@Composable
fun StatCard(title: String, big: String, sub: String, frac: Float?, color: Color, modifier: Modifier) {
    val p = LocalPal.current
    Panel(modifier) {
        Text(title, color = p.sub, fontSize = 12.sp)
        Text(big, color = p.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(sub, color = p.sub, fontSize = 11.sp, maxLines = 2, minLines = 2)
        Spacer(Modifier.height(6.dp))
        Bar(frac ?: 0f, color)
    }
}

@Composable
fun Ring(score: Int, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    val c = if (score >= 80) Color(0xFF4CAF50) else if (score >= 60) Color(0xFFFF9800) else Color(0xFFE53935)
    Box(modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val inset = 6.dp.toPx()
            drawArc(p.text.copy(alpha = 0.12f), -90f, 360f, false, Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset), style = s)
            drawArc(c, -90f, 3.6f * score, false, Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset), style = s)
        }
        Text("$score", color = p.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Chart(values: List<Float>, color: Color, lo: Float? = null, hi: Float? = null, modifier: Modifier = Modifier) {
    val p = LocalPal.current
    Canvas(modifier.fillMaxWidth().height(90.dp)) {
        drawLine(p.text.copy(alpha = 0.12f), Offset(0f, size.height), Offset(size.width, size.height), 2f)
        if (values.size < 2) return@Canvas
        val mn = lo ?: (values.min() - 1f); val mx = hi ?: (values.max() + 1f)
        val rng = (mx - mn).coerceAtLeast(0.5f)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height * (1f - ((v - mn) / rng).coerceIn(0f, 1f))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun Page(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPal.current
    Button(onClick, modifier, colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = Color(0xFF241C14)),
        shape = RoundedCornerShape(14.dp)) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun rememberResumeTick(): Int {
    var t by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) t++ }
        owner.lifecycle.addObserver(o)
        onDispose { owner.lifecycle.removeObserver(o) }
    }
    return t
}

@Composable
fun PermBanner() {
    val ctx = LocalContext.current; val p = LocalPal.current
    Panel(Modifier.fillMaxWidth()) {
        Text("🔒 " + tr("perm.title"), color = p.text, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Btn(tr("perm.btn")) {
            val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + ctx.packageName))
            runCatching { ctx.startActivity(i) }
        }
    }
}

// ───────────────────────── Tổng quan
@Composable
fun OverviewTab(mon: Monitor) {
    val p = LocalPal.current; val s = mon.snap
    val score = mon.score(s)
    val ramP = 100f * s.ramUsed / s.ramTotal; val stP = 100f * s.stUsed / s.stTotal
    Page {
        Panel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Ring(score)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(tr("health"), color = p.sub, fontSize = 12.sp)
                    Text(tr(if (score >= 80) "good" else if (score >= 60) "fair" else "poor"), color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("${Build.MANUFACTURER} ${Build.MODEL}", color = p.sub, fontSize = 12.sp)
                    Text(tr("uptime") + ": " + (s.uptimeMs / 3600000) + "h " + (s.uptimeMs / 60000 % 60) + "m", color = p.sub, fontSize = 12.sp)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(tr("cpu"), s.cpu?.let { "%.0f%%".format(it) } ?: "—", if (s.cpu == null) tr("na") else if (s.cpuFromFreq) "≈ clock" else "${Runtime.getRuntime().availableProcessors()} cores",
                s.cpu?.div(100f), load(s.cpu ?: 0f), Modifier.weight(1f))
            StatCard(tr("ram"), "%.0f%%".format(ramP), "${fmt(s.ramUsed)} / ${fmt(s.ramTotal)}", ramP / 100f, load(ramP), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(tr("storage"), "%.0f%%".format(stP), tr("free", fmt(s.stTotal - s.stUsed)), stP / 100f, load(stP), Modifier.weight(1f))
            StatCard(tr("battery"), "${s.batLevel}%", (if (s.charging) tr("charging") else tr("discharging")) + " · %.1f°C".format(s.batTemp) +
                (if (s.cycles >= 0) " · " + tr("cycles", s.cycles) else ""), s.batLevel / 100f, if (s.batLevel < 20) Color(0xFFE53935) else Color(0xFF4CAF50), Modifier.weight(1f))
        }
        Panel(Modifier.fillMaxWidth()) {
            Text(tr("network"), color = p.sub, fontSize = 12.sp)
            Text("↓ ${fmt(s.rx)}/s    ↑ ${fmt(s.tx)}/s", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("%.2f V · %d mA".format(s.batV, s.batMa) + if (s.batV > 0) " · %.1f W".format(s.batV * s.batMa / 1000f) else "", color = p.sub, fontSize = 12.sp)
        }
        Panel(Modifier.fillMaxWidth()) {
            Text(tr("cpu"), color = p.sub, fontSize = 12.sp)
            Chart(mon.cpuHist, p.accent, 0f, 100f)
            if (s.cpuFromFreq) Text(tr("cpu.note"), color = p.sub, fontSize = 10.sp)
        }
    }
}

// ───────────────────────── Nhiệt độ
@Composable
fun ThermalTab(mon: Monitor) {
    val p = LocalPal.current; val s = mon.snap
    var running by remember { mutableStateOf(false) }
    var elapsed by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<String?>(null) }
    var passed by remember { mutableStateOf(true) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        result = null; elapsed = 0
        val stop = AtomicBoolean(false)
        val t0 = mon.snap.batTemp; var peak = mon.snap.thermal; var done = false
        repeat(Runtime.getRuntime().availableProcessors()) {
            Thread { var x = 1.0; while (!stop.get()) { x = Math.sqrt(x * 1.0001 + 1.0); if (x > 1e9) x = 1.0 }; sink = x }.start()
        }
        try {
            for (i in 1..60) { delay(1000); elapsed = i; peak = maxOf(peak, mon.snap.thermal) }
            done = true
        } finally { stop.set(true); running = false }
        if (done) { passed = peak < 3; result = tr("stress.res", t0, mon.snap.batTemp, tr("th.$peak")) }
    }
    Page {
        Panel(Modifier.fillMaxWidth()) {
            Text(tr("battery") + " 🌡️", color = p.sub, fontSize = 12.sp)
            Text("%.1f°C".format(s.batTemp), color = heat(s.batTemp), fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(tr("thermal.status") + ": " + tr("th.${s.thermal.coerceIn(0, 6)}"), color = p.text, fontSize = 14.sp)
            if (!s.headroom.isNaN()) Text(tr("headroom") + ": %.2f".format(s.headroom), color = p.sub, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Text(tr("history"), color = p.sub, fontSize = 11.sp)
            Chart(mon.tempHist, heat(s.batTemp))
        }
        Panel(Modifier.fillMaxWidth()) {
            Text(tr("sensors"), color = p.sub, fontSize = 12.sp)
            if (s.sensors.isEmpty()) Text(tr("sensors.none"), color = p.text, fontSize = 12.sp)
            s.sensors.forEach { (n, t) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(n, color = p.text, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("%.1f°C".format(t), color = heat(t), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
        Panel(Modifier.fillMaxWidth()) {
            Text(tr("stress"), color = p.text, fontWeight = FontWeight.Bold)
            Text(tr("fan.note"), color = p.sub, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            if (running) {
                Text(tr("stress.run", elapsed, s.batTemp), color = p.text, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp)); Bar(elapsed / 60f, p.accent); Spacer(Modifier.height(8.dp))
                Btn(tr("stress.stop")) { running = false }
            } else Btn(tr("stress.start")) { running = true }
            result?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = p.text, fontSize = 12.sp)
                Text(if (passed) "✅ " + tr("pass") else "⚠️ " + tr("fail"), color = if (passed) Color(0xFF4CAF50) else Color(0xFFE53935), fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ───────────────────────── Dọn dẹp
@Composable
fun CleanTab() {
    val ctx = LocalContext.current; val p = LocalPal.current
    val tick = rememberResumeTick()
    val granted = remember(tick) { Environment.isExternalStorageManager() }
    var junk by remember { mutableStateOf<List<Junk>>(emptyList()) }
    var trashN by remember { mutableIntStateOf(0) }
    var trashB by remember { mutableLongStateOf(0L) }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }
    val sel = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    var key by remember { mutableIntStateOf(0) }
    LaunchedEffect(granted, key) {
        if (!granted) return@LaunchedEffect
        busy = true
        val (j, t) = withContext(Dispatchers.IO) { Fs.scan(ctx) to Fs.trashItems() }
        junk = j
        trashN = t.size; trashB = withContext(Dispatchers.IO) { t.sumOf { Fs.size(it.first) } }
        if (sel.isEmpty()) sel.addAll(j.filter { it.id != "apk" }.map { it.id })
        busy = false
    }
    Page {
        if (!granted) { PermBanner(); return@Page }
        Panel(Modifier.fillMaxWidth()) {
            Text(tr("trash.note"), color = p.sub, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            if (busy) Text(tr("scanning"), color = p.accent)
            junk.forEach { j ->
                Row(Modifier.fillMaxWidth().clickable { if (j.id in sel) sel.remove(j.id) else sel.add(j.id) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = j.id in sel, onCheckedChange = { if (it) sel.add(j.id) else sel.remove(j.id) })
                    Column(Modifier.weight(1f)) {
                        Text(tr(j.key), color = p.text, fontSize = 14.sp)
                        Text(tr("items", j.files.size), color = p.sub, fontSize = 11.sp)
                    }
                    Text(fmt(j.bytes), color = p.accent, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(tr("scan"), Modifier.weight(1f)) { msg = null; key++ }
                Btn(tr("clean.sel"), Modifier.weight(2f)) {
                    busy = true
                    val chosen = junk.filter { it.id in sel }
                    val bytes = chosen.sumOf { it.bytes }
                    scope.launch(Dispatchers.IO) {
                        chosen.forEach { j -> j.files.forEach { if (j.permanent) Fs.delete(it) else Fs.toTrash(it) } }
                        withContext(Dispatchers.Main) { msg = tr("clean.done", fmt(bytes)); key++ }
                    }
                }
            }
            msg?.let { Spacer(Modifier.height(6.dp)); Text("✅ $it", color = Color(0xFF4CAF50)) }
        }
        Panel(Modifier.fillMaxWidth()) {
            Text("🗑️ " + tr("trash"), color = p.text, fontWeight = FontWeight.Bold)
            Text(tr("items", trashN) + " · " + fmt(trashB), color = p.sub, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(tr("trash.restore"), Modifier.weight(1f)) { scope.launch(Dispatchers.IO) { Fs.restoreAll(); withContext(Dispatchers.Main) { key++ } } }
                Btn(tr("trash.empty"), Modifier.weight(1f)) { scope.launch(Dispatchers.IO) { Fs.emptyTrash(); withContext(Dispatchers.Main) { key++ } } }
            }
        }
    }
}

// ───────────────────────── Phân tích đĩa
@Composable
fun AnalyzeTab() {
    val p = LocalPal.current
    val tick = rememberResumeTick()
    val granted = remember(tick) { Environment.isExternalStorageManager() }
    var path by remember { mutableStateOf(Fs.root) }
    var entries by remember { mutableStateOf<List<Pair<File, Long>>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var key by remember { mutableIntStateOf(0) }
    var del by remember { mutableStateOf<File?>(null) }
    LaunchedEffect(path, granted, key) {
        if (!granted) return@LaunchedEffect
        loading = true; entries = emptyList()
        entries = withContext(Dispatchers.IO) {
            (path.listFiles()?.toList() ?: emptyList()).map { it to Fs.size(it) }.sortedByDescending { it.second }
        }
        loading = false
    }
    BackHandler(enabled = granted && path != Fs.root) { path.parentFile?.let { path = it } }
    if (!granted) { Page { PermBanner() }; return }
    val max = (entries.firstOrNull()?.second ?: 1L).coerceAtLeast(1L)
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Text(path.path.removePrefix(Fs.root.path).ifEmpty { "/" }, color = p.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (path != Fs.root) Text(tr("analyze.up"), color = p.accent, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { path.parentFile?.let { path = it } }.padding(vertical = 8.dp))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(entries, key = { it.first.path }) { (f, sz) ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.card)
                        .clickable(enabled = f.isDirectory) { path = f }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (f.isDirectory) "📁" else "📄", fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(f.name, color = p.text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp)); Bar(sz.toFloat() / max, p.accent)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(fmt(sz), color = p.sub, fontSize = 12.sp)
                        Text("🗑️", fontSize = 18.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { del = f }.padding(start = 8.dp, top = 4.dp, bottom = 4.dp))
                    }
                }
            }
            if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center), color = p.accent)
        }
    }
    del?.let { f ->
        AlertDialog(onDismissRequest = { del = null },
            title = { Text(tr("delete")) }, text = { Text(tr("confirm.del", f.name)) },
            confirmButton = { TextButton({ Fs.toTrash(f); del = null; key++ }) { Text(tr("ok")) } },
            dismissButton = { TextButton({ del = null }) { Text(tr("cancel")) } })
    }
}

// ───────────────────────── Gỡ ứng dụng
data class AppRow(val pkg: String, val label: String, val size: Long)

@Composable
fun AppsTab() {
    val ctx = LocalContext.current; val p = LocalPal.current
    val tick = rememberResumeTick()
    var apps by remember { mutableStateOf<List<AppRow>>(emptyList()) }
    var q by remember { mutableStateOf("") }
    LaunchedEffect(tick) {
        apps = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            pm.getInstalledApplications(0).filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 && it.packageName != ctx.packageName }.map {
                val sz = (listOf(it.sourceDir) + (it.splitSourceDirs?.toList() ?: emptyList())).sumOf { d -> File(d).length() }
                AppRow(it.packageName, pm.getApplicationLabel(it).toString(), sz)
            }.sortedByDescending { it.size }
        }
    }
    val shown = apps.filter { q.isBlank() || it.label.contains(q, true) || it.pkg.contains(q, true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(tr("search")) })
        Text(tr("apps.note"), color = p.sub, fontSize = 10.sp, modifier = Modifier.padding(vertical = 6.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(shown, key = { it.pkg }) { a ->
                val icon = remember(a.pkg) { runCatching { ctx.packageManager.getApplicationIcon(a.pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull() }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.card).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) Image(icon, null, Modifier.size(40.dp)) else Spacer(Modifier.size(40.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(a.label, color = p.text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(a.pkg, color = p.sub, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(fmt(a.size), color = p.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton({ runCatching { ctx.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:" + a.pkg))) } }) {
                        Text(tr("uninstall"), color = Color(0xFFE53935))
                    }
                }
            }
        }
    }
}

// ───────────────────────── Tối ưu
@Composable
fun OptimizeTab() {
    val ctx = LocalContext.current; val p = LocalPal.current
    var done by remember { mutableStateOf<String?>(null) }
    fun open(a: String) { runCatching { ctx.startActivity(Intent(a).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    Page {
        Text(tr("opt.note"), color = p.sub, fontSize = 12.sp)
        val rows: List<Pair<String, () -> Unit>> = listOf(
            "🧽 " + tr("opt.cache") to { listOfNotNull(ctx.cacheDir, ctx.externalCacheDir).forEach { d -> d.listFiles()?.forEach { Fs.delete(it) } }; done = "opt.cache" },
            "🧠 " + tr("opt.gc") to { System.gc(); Runtime.getRuntime().gc(); done = "opt.gc" },
            "💾 " + tr("opt.storage") to { open(Settings.ACTION_INTERNAL_STORAGE_SETTINGS) },
            "🔋 " + tr("opt.battery") to { open(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS) },
            "📱 " + tr("opt.apps") to { open(Settings.ACTION_APPLICATION_SETTINGS) },
            "🛠️ " + tr("opt.dev") to { open(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) },
        )
        rows.forEach { (t, f) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.card).clickable { f() }.padding(16.dp)) {
                Text(t, color = p.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("›", color = p.sub, fontSize = 18.sp)
            }
        }
        done?.let { Text("✅ " + tr("opt.done"), color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold) }
    }
}
