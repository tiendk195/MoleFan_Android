package com.molefan.app

import android.content.Context
import android.os.Environment
import java.io.File
import java.nio.file.Files

data class Junk(val id: String, val key: String, val files: List<File>, val bytes: Long, val permanent: Boolean = false)

fun fmt(b: Long): String {
    var v = b.toDouble(); var i = 0
    val u = arrayOf("B", "KB", "MB", "GB", "TB")
    while (v >= 1024 && i < 4) { v /= 1024; i++ }
    return if (i == 0) "$b B" else String.format("%.1f %s", v, u[i])
}

object Fs {
    val root: File get() = Environment.getExternalStorageDirectory()
    val trash: File get() = File(root, ".MoleFanTrash")
    private val index: File get() = File(trash, ".index")

    fun size(f: File): Long = try {
        if (Files.isSymbolicLink(f.toPath())) 0L
        else if (f.isFile) f.length()
        else f.listFiles()?.sumOf { size(it) } ?: 0L
    } catch (e: Exception) { 0L }

    fun scan(ctx: Context): List<Junk> {
        val temp = ArrayList<File>(); val thumbs = ArrayList<File>(); val apks = ArrayList<File>()
        val tmpExt = setOf("tmp", "temp", "log", "bak", "cache", "crdownload", "part")
        val dl = File(root, "Download")
        try {
            root.walkTopDown().onEnter { d ->
                when {
                    d == trash -> false
                    d.name == "Android" && d.parentFile == root -> false
                    d.name == ".thumbnails" -> { thumbs.add(d); false }
                    else -> true
                }
            }.forEach { f ->
                if (f.isFile) {
                    val e = f.extension.lowercase()
                    if (e in tmpExt) temp.add(f)
                    else if (e == "apk" && f.path.startsWith(dl.path)) apks.add(f)
                }
            }
        } catch (e: Exception) { }
        val cache = listOfNotNull(ctx.cacheDir, ctx.externalCacheDir).flatMap { it.listFiles()?.toList() ?: emptyList() }
        fun j(id: String, k: String, l: List<File>, perm: Boolean = false) = Junk(id, k, l, l.sumOf { size(it) }, perm)
        return listOf(j("cache", "j.cache", cache, true), j("temp", "j.temp", temp), j("thumb", "j.thumb", thumbs), j("apk", "j.apk", apks))
    }

    fun delete(f: File) { try { f.deleteRecursively() } catch (e: Exception) { } }

    fun toTrash(f: File): Boolean {
        trash.mkdirs()
        val n = "${System.nanoTime()}_${f.name}"
        val ok = f.renameTo(File(trash, n))
        if (ok) index.appendText("$n\t${f.path}\n")
        return ok
    }

    fun trashItems(): List<Pair<File, String>> = try {
        index.readLines().mapNotNull {
            val p = it.split("\t"); if (p.size < 2) null else File(trash, p[0]) to p[1]
        }.filter { it.first.exists() }
    } catch (e: Exception) { emptyList() }

    fun restoreAll(): Int {
        var n = 0
        trashItems().forEach { (f, orig) ->
            val o = File(orig); o.parentFile?.mkdirs()
            if (!o.exists() && f.renameTo(o)) n++
        }
        index.writeText(trashItems().joinToString("") { "${it.first.name}\t${it.second}\n" })
        return n
    }

    fun emptyTrash() { trash.listFiles()?.forEach { delete(it) } }
}
