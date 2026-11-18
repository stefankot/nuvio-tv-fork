package com.nuvio.tv.core.autoquality

import com.nuvio.tv.core.autoquality.echo.StreamQuality
import com.nuvio.tv.core.streams.smart.VideoCodec
import com.nuvio.tv.domain.model.Stream
import java.security.MessageDigest

/**
 * forge M4a (project point 6): what the quality change during playback remembers, in process memory only (never on
 * disk; the disk memory of bad sources, `SmartBadSources`, works next to it). The clock is passed in by the caller,
 * always the same one ([AutoQualitySession.nowMs]). Keys only: no address is stored here or written to a log.
 */
class SessionMemory {
    private val blockedServers = HashMap<String, Long>()
    private val blockedLevels = HashMap<StreamQuality, Long>()
    private var ceilingLevel: StreamQuality? = null
    private var ceilingUntilMs = 0L
    private val penalisedDecoders = HashSet<DecoderKey>()
    private val failedSources = HashSet<String>()

    private data class DecoderKey(val codec: VideoCodec?, val height: Int?, val fps: Double?)

    /** A server that failed is left alone for [SERVER_BLOCK_MS]. */
    @Synchronized
    fun blockServer(serverKey: String, nowMs: Long) {
        blockedServers[serverKey] = nowMs + SERVER_BLOCK_MS
        if (blockedServers.size > MAX_SERVERS) blockedServers.entries.removeAll { it.value <= nowMs }
    }

    /** Cheap check before computing keys: is any server blocked right now? */
    @Synchronized
    fun anyServerBlocked(nowMs: Long): Boolean = blockedServers.values.any { it > nowMs }

    @Synchronized
    fun isServerBlocked(serverKey: String?, nowMs: Long): Boolean =
        serverKey != null && (blockedServers[serverKey] ?: return false) > nowMs

    /** E10: no source of [level] played; the level is left alone for [LEVEL_BLOCK_MS] (Echo SENSITIVE `block`). */
    @Synchronized
    fun blockLevel(level: StreamQuality, nowMs: Long) {
        blockedLevels[level] = nowMs + LEVEL_BLOCK_MS
    }

    @Synchronized
    fun isLevelBlocked(level: StreamQuality?, nowMs: Long): Boolean =
        level != null && (blockedLevels[level] ?: return false) > nowMs

    /** The highest level the session plays after a step down for the link; null = none (or expired). */
    @Synchronized
    fun ceiling(nowMs: Long): StreamQuality? {
        val level = ceilingLevel ?: return null
        if (ceilingUntilMs <= nowMs) {
            ceilingLevel = null
            return null
        }
        return level
    }

    @Synchronized
    fun setCeiling(level: StreamQuality, nowMs: Long) {
        ceilingLevel = level
        ceilingUntilMs = nowMs + CEILING_MS
    }

    /** A successful return up lifts the ceiling. */
    @Synchronized
    fun clearCeiling() {
        ceilingLevel = null
    }

    /** This (codec, height, fps) dropped frames: penalised until the process ends. */
    @Synchronized
    fun penaliseDecoder(codec: VideoCodec?, height: Int?, fps: Double?) {
        penalisedDecoders += DecoderKey(codec, height, fps)
    }

    /** An unknown fps on either side matches any fps of the same codec and height. */
    @Synchronized
    fun isDecoderPenalised(codec: VideoCodec?, height: Int?, fps: Double?): Boolean =
        penalisedDecoders.any { it.codec == codec && it.height == height && (it.fps == null || fps == null || it.fps == fps) }

    /** E10: a source that failed does not come back in this session (title or episode). */
    @Synchronized
    fun failedThisSession(sourceKey: String): Boolean = sourceKey in failedSources

    @Synchronized
    fun markFailed(sourceKey: String) {
        failedSources += sourceKey
    }

    /** A new title or episode: sources failed in the last one may be tried again. Blocks, ceiling and penalties stay. */
    @Synchronized
    fun reset() {
        failedSources.clear()
    }

    /** Everything forgotten (tests). */
    @Synchronized
    internal fun clearAll() {
        blockedServers.clear()
        blockedLevels.clear()
        ceilingLevel = null
        penalisedDecoders.clear()
        failedSources.clear()
    }

    companion object {
        const val SERVER_BLOCK_MS = 30 * 60_000L
        const val LEVEL_BLOCK_MS = 3 * 60_000L
        const val CEILING_MS = 30 * 60_000L
        private const val MAX_SERVERS = 200
    }
}

/** forge M4a: the one session memory of the process. */
object AutoQualitySession {
    val memory = SessionMemory()

    /** The clock every caller of [memory] uses. */
    fun nowMs(): Long = android.os.SystemClock.elapsedRealtime()

    /** Is the server of [stream] blocked right now? Unknown server → no. */
    fun isServerBlocked(stream: Stream): Boolean {
        val now = nowMs()
        return memory.anyServerBlocked(now) && memory.isServerBlocked(serverKey(stream), now)
    }
}

/** Addon name + a hash of the address's host; null when the stream has no address to read a host from. */
fun serverKey(stream: Stream): String? = serverKeyOf(stream.addonName, stream.getStreamUrl())

fun serverKeyOf(addonName: String?, url: String?): String? {
    val host = url?.let { runCatching { java.net.URI(it.trim()).host }.getOrNull() }
        ?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
    val digest = MessageDigest.getInstance("SHA-256").digest(host.toByteArray(Charsets.UTF_8))
    return "srv:${addonName.orEmpty()}:" + digest.joinToString("") { "%02x".format(it) }.take(16)
}
