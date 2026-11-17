package com.nuvio.tv.fork.tv.logo

import android.content.Context
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * A logo package given as a ZIP (owner 10.11: https://epg.ovh/logo.zip, 500+ PNGs named after the channels). It is downloaded once,
 * unpacked into the app's files and indexed by a normalised channel name (case, spaces, "+" and punctuation ignored), so a channel
 * finds its logo locally, without a request and without 404s.
 */
object ForgeLogoPack {
    private val _version = MutableStateFlow(0)
    /** Goes up when a pack has been unpacked, so screens redraw their logos. */
    val version: StateFlow<Int> = _version.asStateFlow()
    @Volatile private var index: Map<String, File> = emptyMap()
    @Volatile private var dir: File? = null
    @Volatile var status: String = ""
        private set

    fun isZip(address: String): Boolean = address.substringBefore('?').trim().endsWith(".zip", ignoreCase = true)

    private fun key(name: String): String = name.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), "")

    fun ensure(context: Context) {
        if (dir != null) return
        val d = File(context.applicationContext.filesDir, "forge_logos").apply { mkdirs() }
        dir = d
        loadIndex(d)
    }

    private fun loadIndex(d: File) {
        index = d.listFiles { f -> f.isFile && f.name.endsWith(".png") }?.associateBy { it.name.removeSuffix(".png") }.orEmpty()
        _version.value = _version.value + 1
    }

    val size: Int get() = index.size

    /** A file address ("file:///…") of the channel's logo from the pack, or null. */
    fun fileFor(channel: LiveTvChannel): String? {
        val map = index
        if (map.isEmpty()) return null
        val names = listOfNotNull(forgeLogoName(channel), channel.tvgName, channel.name)
        for (name in names) {
            val k = key(name)
            if (k.isEmpty()) continue
            (map[k] ?: map[k.removeSuffix("hd")] ?: map[k.removeSuffix("fhd")])?.let { return "file://" + it.absolutePath }
        }
        return null
    }

    /** Downloads and unpacks [address] in the background; replaces the earlier pack when it worked. */
    fun install(context: Context, address: String) {
        ensure(context)
        val d = dir ?: return
        status = "pobieram…"
        Thread {
            runCatching {
                val connection = (URL(address.trim()).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000; readTimeout = 60_000
                    setRequestProperty("User-Agent", "Mozilla/5.0")
                }
                val fresh = File(d, ".new").apply { deleteRecursively(); mkdirs() }
                var count = 0
                ZipInputStream(connection.inputStream.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (!entry.isDirectory && entry.name.endsWith(".png", ignoreCase = true)) {
                            val k = key(entry.name.substringAfterLast('/').substringBeforeLast('.'))
                            if (k.isNotEmpty() && count < 5_000) {
                                File(fresh, "$k.png").outputStream().use { out -> zip.copyTo(out) }
                                count++
                            }
                        }
                        zip.closeEntry()
                    }
                }
                if (count > 0) {
                    d.listFiles { f -> f.isFile && f.name.endsWith(".png") }?.forEach { it.delete() }
                    fresh.listFiles()?.forEach { it.renameTo(File(d, it.name)) }
                    fresh.deleteRecursively()
                    loadIndex(d)
                    status = "$count logo"
                } else {
                    fresh.deleteRecursively()
                    status = "pusta paczka"
                }
            }.onFailure { status = "błąd: " + (it.message ?: it.javaClass.simpleName) }
        }.apply { isDaemon = true; name = "forge-logo-pack" }.start()
    }
}
