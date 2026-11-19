package com.nuvio.tv.fork.family

import android.content.Context
import android.util.Base64
import java.io.File
import java.util.zip.GZIPOutputStream

/**
 * Właściciel 19.11: „logi mają się zachowywać w całości i być synchronizowane z panelem administracyjnym na zenbox
 * w całości, z całego używania”. Logcat telewizora gubi wpisy (bufor, „chatty”), więc aplikacja sama czyta logi
 * SWOJEGO procesu przez cały czas działania (`logcat --pid`, bez uprawnień) i zapisuje je do plików w filesDir.
 * Kawałki po [SEGMENT_BYTES] są pakowane gzip i wysyłane z raportem rodziny (FamilySync.report, `a=logs`);
 * wysłane kasowane. Lokalnie najwyżej [MAX_LOCAL_BYTES] (najstarsze niewysłane wypadają dopiero przy przepełnieniu).
 */
object FullLog {
    private const val SEGMENT_BYTES = 2L * 1024 * 1024
    private const val MAX_LOCAL_BYTES = 150L * 1024 * 1024
    private const val TAG = "FullLog"
    @Volatile private var started = false
    private val lock = Any()

    private fun dir(c: Context) = File(c.applicationContext.filesDir, "fulllog").apply { mkdirs() }

    /** Raz na proces (FamilyStats.install przy pokazaniu aplikacji): wątek czytający logcat tego procesu. */
    fun start(c: Context) {
        if (started) return
        started = true
        val d = dir(c)
        Thread({
            runCatching {
                val pid = android.os.Process.myPid()
                // -v threadtime: data, godzina, wątek; bez -T: od początku procesu (to, co bufor jeszcze ma), potem na bieżąco.
                val proc = ProcessBuilder("logcat", "-v", "threadtime", "--pid=$pid").redirectErrorStream(true).start()
                val header = "===== Nuvio ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} Android ${android.os.Build.VERSION.RELEASE} pid=$pid start=${java.util.Date()} =====\n"
                append(d, header.toByteArray())
                proc.inputStream.bufferedReader().useLines { lines ->
                    val buf = StringBuilder()
                    var lastFlush = System.currentTimeMillis()
                    lines.forEach { line ->
                        buf.append(line).append('\n')
                        val now = System.currentTimeMillis()
                        if (buf.length > 32_000 || now - lastFlush > 2_000) {
                            append(d, buf.toString().toByteArray()); buf.setLength(0); lastFlush = now
                        }
                    }
                }
            }.onFailure { android.util.Log.w(TAG, "logcat reader stopped: ${it.javaClass.simpleName}") }
        }, "fork-fulllog").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }.start()
    }

    private fun append(d: File, bytes: ByteArray) = synchronized(lock) {
        val cur = File(d, "current.log")
        cur.appendBytes(bytes)
        if (cur.length() >= SEGMENT_BYTES) rotate(d)
    }

    /** current.log → seg_<czas>.log.gz (do wysłania). */
    private fun rotate(d: File) {
        val cur = File(d, "current.log")
        if (!cur.exists() || cur.length() == 0L) return
        val out = File(d, "seg_${System.currentTimeMillis()}.log.gz")
        GZIPOutputStream(out.outputStream()).use { gz -> cur.inputStream().use { it.copyTo(gz) } }
        cur.delete()
        trim(d)
    }

    private fun trim(d: File) {
        val segs = d.listFiles { f -> f.name.startsWith("seg_") }.orEmpty().sortedBy { it.name }
        var total = segs.sumOf { it.length() }
        for (f in segs) { if (total <= MAX_LOCAL_BYTES) break; total -= f.length(); f.delete() }
    }

    /**
     * Z raportu rodziny: domyka bieżący kawałek i wysyła wszystkie niewysłane (najstarsze pierwsze). [send] = wysyłka
     * jednego kawałka (nazwa, base64 gzip) → true, gdy serwer przyjął. Przerywa na pierwszym błędzie (ponowi za 15 min).
     */
    fun upload(c: Context, send: (String, String) -> Boolean) {
        val d = dir(c)
        synchronized(lock) { rotate(d) }
        d.listFiles { f -> f.name.startsWith("seg_") }.orEmpty().sortedBy { it.name }.forEach { f ->
            val ok = runCatching { send(f.name, Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)) }.getOrDefault(false)
            if (!ok) return
            f.delete()
        }
    }
}
