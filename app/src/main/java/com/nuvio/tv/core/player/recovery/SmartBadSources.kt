package com.nuvio.tv.core.player.recovery

import android.content.Context
import android.util.Log
import com.nuvio.tv.domain.model.Stream
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/** forge: why a source was put aside. [fileInherent] = the file itself is bad, so every host of it is. */
enum class BadReason(val fileInherent: Boolean) {
    NO_START(false), STALL(false), AUTH(false), USER_SKIP(false),
    WRONG_DURATION(true), DECODER(true), AUDIO_MISSING(true), FAKE_QUALITY(true)
}

/** [key] is a source key ([badSourceKey]) or a file key ([fileIdentityKey]); never a raw URL. */
data class BadEntry(val key: String, val reason: BadReason, val untilMs: Long, val count: Int)

/**
 * forge: memory of sources that failed in automatic mode. Pure: the clock is passed in, nothing is stored here.
 * A blocked source is ranked last, never removed.
 */
class BadSourceBook(entries: List<BadEntry> = emptyList()) {
    private val entries = LinkedHashMap<String, BadEntry>()

    init {
        entries.forEach { this.entries[it.key] = it }
        trim()
    }

    @Synchronized
    fun mark(key: String, reason: BadReason, nowMs: Long) {
        val previous = entries[key]
        val live = previous != null && previous.untilMs > nowMs
        // A NO_START entry starts SHORT_MS before its end; a second one within a day of it blocks for a day.
        val repeatedNoStart = reason == BadReason.NO_START && previous?.reason == BadReason.NO_START &&
            nowMs - (previous.untilMs - SHORT_MS) <= LONG_MS
        val blockMs = if (reason.fileInherent || repeatedNoStart) LONG_MS else SHORT_MS
        val count = if (previous != null && nowMs - previous.untilMs <= LONG_MS) previous.count + 1 else 1
        // A longer block already in place is never shortened by a lighter reason.
        val keepPrevious = live && previous!!.untilMs > nowMs + blockMs
        val entry = if (keepPrevious) previous!!.copy(count = count) else BadEntry(key, reason, nowMs + blockMs, count)
        entries.remove(key)
        entries[key] = entry
        trim()
    }

    @Synchronized
    fun isBlocked(key: String, nowMs: Long): Boolean = (entries[key]?.untilMs ?: return false) > nowMs

    @Synchronized
    fun clear(key: String) { entries.remove(key) }

    @Synchronized
    fun clearAll() = entries.clear()

    /** Adds entries whose key is not in the book yet; entries already here (newer marks) stay as they are. */
    @Synchronized
    fun mergeIfAbsent(more: List<BadEntry>) {
        more.forEach { if (it.key !in entries) entries[it.key] = it }
        trim()
    }

    /** Live entries only. */
    @Synchronized
    fun snapshot(nowMs: Long): List<BadEntry> = entries.values.filter { it.untilMs > nowMs }

    /** Entries the file keeps: live ones plus expired NO_START within a day (the "second time" rule). */
    @Synchronized
    internal fun persistable(nowMs: Long): List<BadEntry> =
        entries.values.filter { it.untilMs > nowMs || (it.reason == BadReason.NO_START && it.untilMs > nowMs - LONG_MS) }

    private fun trim() {
        while (entries.size > MAX) {
            val soonest = entries.values.minByOrNull { it.untilMs } ?: return
            entries.remove(soonest.key)
        }
    }

    companion object {
        const val SHORT_MS = 30 * 60_000L
        const val LONG_MS = 24 * 3_600_000L
        const val MAX = 200
        const val MAX_FILE_BYTES = 32 * 1024

        /** File text → book. Anything unreadable gives an empty book, never an exception. */
        fun decode(text: String?): BadSourceBook = runCatching {
            if (text.isNullOrBlank() || text.length > MAX_FILE_BYTES) return@runCatching BadSourceBook()
            val list = Json.parseToJsonElement(text).jsonObject["e"]?.jsonArray ?: return@runCatching BadSourceBook()
            BadSourceBook(list.mapNotNull { element ->
                runCatching {
                    val o = element.jsonObject
                    val key = o["k"]!!.jsonPrimitive.content
                    if (key.startsWith("url:") || key.contains("://")) null // never trust a raw address from disk
                    else BadEntry(key, BadReason.valueOf(o["r"]!!.jsonPrimitive.content), o["u"]!!.jsonPrimitive.long, o["c"]!!.jsonPrimitive.int)
                }.getOrNull()
            })
        }.getOrElse { BadSourceBook() }

        /** Book → file text, at most [MAX_FILE_BYTES]: the entries closest to expiry are left out first. */
        fun encode(entries: List<BadEntry>): String {
            var kept = entries.sortedByDescending { it.untilMs }
            while (true) {
                val text = JsonObject(mapOf("v" to JsonPrimitive(1), "e" to buildJsonArray {
                    kept.forEach { e ->
                        add(buildJsonObject { put("k", e.key); put("r", e.reason.name); put("u", e.untilMs); put("c", e.count) })
                    }
                })).toString()
                if (text.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES || kept.isEmpty()) return text
                kept = kept.dropLast(maxOf(1, kept.size / 10))
            }
        }
    }
}

/** forge: the process-wide memory of bad sources; loaded once, written in the background. Automatic mode only. */
object SmartBadSources {
    private const val TAG = "SmartRecovery"
    private const val FILE = "smart_bad_sources.json"
    @Volatile private var book = BadSourceBook()
    @Volatile private var file: File? = null
    @Volatile private var loaded = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Any()

    fun init(context: Context) {
        if (file != null) return
        val target = File(context.applicationContext.filesDir, FILE)
        file = target
        scope.launch {
            val text = runCatching { if (target.exists() && target.length() <= BadSourceBook.MAX_FILE_BYTES) target.readText() else null }
                .getOrNull()
            val disk = BadSourceBook.decode(text).persistable(now())
            // M3b: merged into the live book, never swapped for a new one, so a mark made while the file
            // was loading is kept (and wins over the file's entry for the same key).
            val early = book.persistable(now()).isNotEmpty()
            book.mergeIfAbsent(disk)
            this@SmartBadSources.loaded = true
            if (early) save()
        }
    }

    private fun now() = System.currentTimeMillis()

    fun mark(key: String, reason: BadReason) {
        book.mark(key, reason, now())
        save()
    }

    /** Marks the source; a [BadReason.fileInherent] reason marks the file too, wherever it is hosted. */
    fun mark(stream: Stream, reason: BadReason) {
        // forge M6 (N6): a failure counts against the origin (the viewer's own skip does not); M7: the family is off
        if (reason != BadReason.USER_SKIP) {
            com.nuvio.tv.core.streams.smart.SmartOutcomeStore.record(
                com.nuvio.tv.core.streams.smart.SmartOutcomeStore.originOf(stream.addonName, stream.clientResolve?.service), ok = false, ttffMs = null)
        }
        val t = now()
        book.mark(badSourceKey(stream), reason, t)
        if (reason.fileInherent) fileIdentityKey(stream)?.let { book.mark(it, reason, t) }
        save()
    }

    fun isBlocked(key: String): Boolean = book.isBlocked(key, now())

    fun isBlocked(stream: Stream): Boolean {
        val t = now()
        return book.isBlocked(badSourceKey(stream), t) || fileIdentityKey(stream)?.let { book.isBlocked(it, t) } == true
    }

    fun clear(key: String) {
        book.clear(key)
        save()
    }

    /** The viewer chose this source by hand: lift both of its blocks. */
    fun clearFor(stream: Stream) {
        book.clear(badSourceKey(stream))
        fileIdentityKey(stream)?.let { book.clear(it) }
        save()
    }

    fun clearAll() {
        book.clearAll()
        save()
    }

    /** How many sources are put aside right now (the settings row shows it). */
    fun liveCount(): Int = book.snapshot(now()).size

    private fun save() {
        val target = file ?: return
        if (!loaded) return // the load writes once it has merged
        val current = book
        scope.launch {
            synchronized(writeLock) {
                runCatching {
                    val tmp = File(target.parentFile, "$FILE.tmp")
                    tmp.writeText(BadSourceBook.encode(current.persistable(now())))
                    if (!tmp.renameTo(target)) { target.delete(); tmp.renameTo(target) }
                }.onFailure { Log.w(TAG, "bad sources not saved: ${it.javaClass.simpleName}") }
            }
        }
    }
}

/** Source key without any address: a URL-based key becomes the first 16 hex characters of its SHA-256. */
fun badSourceKey(stream: Stream): String = badSourceKeyOf(recoverySourceKey(stream))

internal fun badSourceKeyOf(sourceKey: String): String {
    if (!sourceKey.startsWith("url:")) return sourceKey
    val digest = MessageDigest.getInstance("SHA-256").digest(sourceKey.removePrefix("url:").toByteArray(Charsets.UTF_8))
    return "u:" + digest.joinToString("") { "%02x".format(it) }.take(16)
}

/** File key shared by every addon that offers the same file; null when the file cannot be told apart. */
fun fileIdentityKey(stream: Stream): String? = fileIdentityKeyOf(
    hash = stream.getEffectiveInfoHash(),
    fileIdx = stream.getEffectiveFileIdx(),
    fileName = stream.behaviorHints?.filename ?: stream.clientResolve?.filename,
    sizeBytes = stream.behaviorHints?.videoSize?.takeIf { it > 0 } ?: stream.debridCacheStatus?.cachedSize?.takeIf { it > 0 },
)

internal fun fileIdentityKeyOf(hash: String?, fileIdx: Int?, fileName: String?, sizeBytes: Long?): String? {
    val name = fileName?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()
    if (!hash.isNullOrBlank()) return "h:${hash.lowercase()}:${fileIdx?.toString() ?: name.orEmpty()}"
    if (name != null && sizeBytes != null && sizeBytes > 0) return "n:$name:$sizeBytes"
    return null
}
