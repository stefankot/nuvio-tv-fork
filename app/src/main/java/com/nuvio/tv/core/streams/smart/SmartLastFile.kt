package com.nuvio.tv.core.streams.smart

import android.content.Context
import android.util.Log
import com.nuvio.tv.core.player.recovery.badSourceKey
import com.nuvio.tv.core.player.recovery.fileIdentityKey
import com.nuvio.tv.domain.model.Stream
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/** [fileKey] is a [fileIdentityKey] or a [badSourceKey]: never an address. [preset] = the preset it was picked with. */
data class LastFile(val contentKey: String, val fileKey: String, val preset: SmartPreset, val atMs: Long)

/**
 * forge M3b (N8): the file each title (or episode) last played in the automatic mode, so resuming takes the same
 * file — the position and the subtitle timing stay right. Pure: the clock is passed in, nothing is stored here.
 */
class LastFileBook(entries: List<LastFile> = emptyList()) {
    private val entries = LinkedHashMap<String, LastFile>()

    init {
        entries.sortedBy { it.atMs }.forEach { this.entries[it.contentKey] = it }
        trim()
    }

    @Synchronized
    fun remember(contentKey: String, fileKey: String, preset: SmartPreset, nowMs: Long) {
        if (fileKey.startsWith("url:") || fileKey.contains("://")) return // never an address
        entries.remove(contentKey)
        entries[contentKey] = LastFile(contentKey, fileKey, preset, nowMs)
        trim()
    }

    /** The file to prefer, or null: none, too old, or picked with another preset (the viewer changed it). */
    @Synchronized
    fun get(contentKey: String, preset: SmartPreset, nowMs: Long): String? =
        entries[contentKey]?.takeIf { nowMs - it.atMs <= MAX_AGE_MS && it.preset == preset }?.fileKey

    @Synchronized
    fun snapshot(nowMs: Long): List<LastFile> = entries.values.filter { nowMs - it.atMs <= MAX_AGE_MS }

    @Synchronized
    fun mergeIfAbsent(more: List<LastFile>) {
        more.sortedBy { it.atMs }.forEach { if (it.contentKey !in entries) entries[it.contentKey] = it }
        trim()
    }

    private fun trim() {
        while (entries.size > MAX) entries.remove(entries.keys.first())
    }

    companion object {
        const val MAX = 300
        const val MAX_AGE_MS = 30L * 24 * 3_600_000L
        const val MAX_FILE_BYTES = 32 * 1024

        fun decode(text: String?): LastFileBook = runCatching {
            if (text.isNullOrBlank() || text.length > MAX_FILE_BYTES) return@runCatching LastFileBook()
            val list = Json.parseToJsonElement(text).jsonObject["e"]?.jsonArray ?: return@runCatching LastFileBook()
            LastFileBook(list.mapNotNull { element ->
                runCatching {
                    val o = element.jsonObject
                    val file = o["f"]!!.jsonPrimitive.content
                    if (file.startsWith("url:") || file.contains("://")) null
                    else LastFile(o["k"]!!.jsonPrimitive.content, file, SmartPreset.valueOf(o["p"]!!.jsonPrimitive.content),
                        o["t"]!!.jsonPrimitive.long)
                }.getOrNull()
            })
        }.getOrElse { LastFileBook() }

        /** Newest first; the oldest are left out until the text fits [MAX_FILE_BYTES]. */
        fun encode(entries: List<LastFile>): String {
            var kept = entries.sortedByDescending { it.atMs }
            while (true) {
                val text = JsonObject(mapOf("v" to JsonPrimitive(1), "e" to buildJsonArray {
                    kept.forEach { e ->
                        add(buildJsonObject { put("k", e.contentKey); put("f", e.fileKey); put("p", e.preset.name); put("t", e.atMs) })
                    }
                })).toString()
                if (text.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES || kept.isEmpty()) return text
                kept = kept.dropLast(maxOf(1, kept.size / 10))
            }
        }
    }
}

/** Title, or title + season + episode. */
fun lastFileContentKey(contentId: String?, season: Int?, episode: Int?): String? {
    val id = contentId?.takeIf { it.isNotBlank() } ?: return null
    return if (season != null && episode != null) "$id:$season:$episode" else id
}

/** The key a source is remembered and found by: the file (same file from any addon), else the source. */
fun lastFileKeyOf(stream: Stream): String = fileIdentityKey(stream) ?: badSourceKey(stream)

/**
 * forge (check 13.11): the source the player was launched with, rebuilt from the launch arguments, for [SmartLastFile]
 * when nothing better is known. Only the fields [lastFileKeyOf] reads; the address stays out of the file key.
 */
fun lastFileLaunchStream(
    url: String?, infoHash: String?, fileIdx: Int?, filename: String?, videoHash: String?, videoSize: Long?, addonName: String?,
): Stream = Stream(
    name = null, title = null, description = null, url = url, ytId = null, infoHash = infoHash, fileIdx = fileIdx,
    externalUrl = null, addonName = addonName.orEmpty(), addonLogo = null,
    behaviorHints = com.nuvio.tv.domain.model.StreamBehaviorHints(
        notWebReady = null, bingeGroup = null, countryWhitelist = null, proxyHeaders = null,
        videoHash = videoHash, videoSize = videoSize, filename = filename,
    ),
)

/** forge M3b (N8): the process-wide memory of last files; loaded once, written in the background. */
object SmartLastFile {
    private const val TAG = "SmartPick"
    private const val FILE = "smart_last_files.json"
    private val book = LastFileBook()
    @Volatile private var file: File? = null
    @Volatile private var loaded = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Any()

    fun init(context: Context) {
        if (file != null) return
        val target = File(context.applicationContext.filesDir, FILE)
        file = target
        scope.launch {
            val text = runCatching { if (target.exists() && target.length() <= LastFileBook.MAX_FILE_BYTES) target.readText() else null }
                .getOrNull()
            val early = book.snapshot(now()).isNotEmpty()
            book.mergeIfAbsent(LastFileBook.decode(text).snapshot(now()))
            loaded = true
            if (early) save()
        }
    }

    private fun now() = System.currentTimeMillis()

    fun remember(contentId: String?, season: Int?, episode: Int?, stream: Stream, preset: SmartPreset) {
        val key = lastFileContentKey(contentId, season, episode) ?: return
        book.remember(key, lastFileKeyOf(stream), preset, now())
        save()
    }

    fun get(contentId: String?, season: Int?, episode: Int?, preset: SmartPreset): String? =
        lastFileContentKey(contentId, season, episode)?.let { book.get(it, preset, now()) }

    private fun save() {
        val target = file ?: return
        if (!loaded) return
        scope.launch {
            synchronized(writeLock) {
                runCatching {
                    val tmp = File(target.parentFile, "$FILE.tmp")
                    tmp.writeText(LastFileBook.encode(book.snapshot(now())))
                    if (!tmp.renameTo(target)) { target.delete(); tmp.renameTo(target) }
                }.onFailure { Log.w(TAG, "last files not saved: ${it.javaClass.simpleName}") }
            }
        }
    }
}
