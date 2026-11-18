package com.nuvio.tv.core.autoquality

import com.nuvio.tv.core.autoquality.echo.StreamQuality
import com.nuvio.tv.core.player.recovery.badSourceKey
import com.nuvio.tv.core.player.recovery.fileIdentityKey
import com.nuvio.tv.core.streams.smart.ReleaseInfo
import com.nuvio.tv.core.streams.smart.ReleaseParser
import com.nuvio.tv.core.streams.smart.SmartCandidate
import com.nuvio.tv.core.streams.smart.SmartExpected
import com.nuvio.tv.core.streams.smart.SmartPolicy
import com.nuvio.tv.core.streams.smart.SmartPreset
import com.nuvio.tv.core.streams.smart.SmartSourceRanker
import com.nuvio.tv.core.streams.smart.VideoCodec
import com.nuvio.tv.domain.model.Stream
import kotlin.math.abs
import kotlin.math.max

/**
 * forge M4a: what the ladder must know about a candidate that [SmartCandidate] does not carry (it holds names only).
 * Keys only, never an address.
 */
interface CandidateKeys<T> {
    /** As `badSourceKey`: the key of the failed-this-session memory. */
    fun sourceKey(item: T): String
    /** As [serverKey]; null = the server is not known. */
    fun serverKey(item: T): String?
    /** As `fileIdentityKey`; null = the file cannot be told apart. */
    fun fileKey(item: T): String?
    /** The file's running time when something has measured it; null = unknown. */
    fun durationMs(item: T): Long? = null
}

object StreamCandidateKeys : CandidateKeys<Stream> {
    override fun sourceKey(item: Stream): String = badSourceKey(item)
    override fun serverKey(item: Stream): String? = com.nuvio.tv.core.autoquality.serverKey(item)
    override fun fileKey(item: Stream): String? = fileIdentityKey(item)
}

/**
 * forge M4a (project point 3). [measuredBps]: the link the successor must fit in (÷ [FIT_HEADROOM]); null = not
 * measured. [policy] and [expected]: the ranking of the start (same preset), used inside each step.
 */
data class SuccessorQuery<T>(
    val current: SmartCandidate<T>,
    val currentInfo: ReleaseInfo,
    val cause: Cause,
    val listeningLanguage: String?,
    val durationMs: Long?,
    val measuredBps: Long?,
    val policy: SmartPolicy,
    val expected: SmartExpected? = null,
)

/** One successor and the step of the ladder it came from (1–4; 0 = the return up to the preset). */
data class Successor<T>(val candidate: SmartCandidate<T>, val info: ReleaseInfo, val step: Int)

/** E6: a successor's own bitrate must fit in the link with this much to spare. */
const val FIT_HEADROOM = 1.25
/** Without a measured link the ladder does not go below this height (Echo `LOWEST_UNMEASURED_STEP`). */
const val LOWEST_UNMEASURED_HEIGHT = 720
/** Two running times within this share of the longer one are the same timeline. */
const val SAME_DURATION_SHARE = 0.01

/** The ladder, flat: steps in order, the ranking's order inside each step. */
fun <T> ladder(
    query: SuccessorQuery<T>,
    candidates: List<SmartCandidate<T>>,
    memory: SessionMemory,
    nowMs: Long,
    keys: CandidateKeys<T>,
): List<SmartCandidate<T>> = successors(query, candidates, memory, nowMs, keys).map { it.candidate }

/**
 * Steps: (1) the same file, a new address; (2) the same release on another server; (3) the same edition, audio tier
 * and level, running time within 1%; (4) the same edition, a lower level — only for [Cause.LINK] and [Cause.DECODER]
 * (for the decoder: a lower height or AVC, the same audio tier). [Cause.ADDRESS_EXPIRED] has no ladder.
 */
fun <T> successors(
    query: SuccessorQuery<T>,
    candidates: List<SmartCandidate<T>>,
    memory: SessionMemory,
    nowMs: Long,
    keys: CandidateKeys<T>,
): List<Successor<T>> {
    if (query.cause == Cause.ADDRESS_EXPIRED) return emptyList()
    val current = Facts(query.current, query.currentInfo, keys, query.listeningLanguage)
    val lastStep = if (query.cause == Cause.LINK || query.cause == Cause.DECODER) 4 else 3
    val byStep = HashMap<Int, MutableList<Successor<T>>>()
    for (candidate in candidates) {
        if (candidate.item === query.current.item || keys.sourceKey(candidate.item) == current.sourceKey) continue
        val other = Facts(candidate, ReleaseParser.parse(candidate.text, candidate.sizeBytes), keys, query.listeningLanguage)
        if (!passes(other, query, memory, nowMs, applyCeiling = true)) continue
        val step = stepOf(current, other, query) ?: continue
        if (step > lastStep) continue
        byStep.getOrPut(step) { mutableListOf() } += Successor(candidate, other.info, step)
    }
    return (1..lastStep).flatMap { step -> ranked(byStep[step].orEmpty(), query) }
}

/**
 * The return up to [target] (the preset's level): the same edition and audio tier, the filter without the session
 * ceiling (a successful return is what lifts it), and only files that share the timeline (the return goes route 4A).
 */
fun <T> returnUp(
    query: SuccessorQuery<T>,
    candidates: List<SmartCandidate<T>>,
    memory: SessionMemory,
    nowMs: Long,
    keys: CandidateKeys<T>,
    target: StreamQuality,
): List<Successor<T>> {
    val current = Facts(query.current, query.currentInfo, keys, query.listeningLanguage)
    val found = candidates.mapNotNull { candidate ->
        if (candidate.item === query.current.item || keys.sourceKey(candidate.item) == current.sourceKey) return@mapNotNull null
        val other = Facts(candidate, ReleaseParser.parse(candidate.text, candidate.sizeBytes), keys, query.listeningLanguage)
        if (other.level != target || other.info.edition != current.info.edition || other.audioTier != current.audioTier) return@mapNotNull null
        if (!passes(other, query, memory, nowMs, applyCeiling = false)) return@mapNotNull null
        if (!sharedTimeline(current.info, other.info, query.durationMs, other.durationMs)) return@mapNotNull null
        Successor(candidate, other.info, 0)
    }
    return ranked(found, query)
}

/**
 * Do two files share the timeline (a switch keeps the position)? The same edition and running times within 1%;
 * unknown running time = no proof = false. The fps is not compared: [ReleaseInfo] does not carry it.
 */
fun sharedTimeline(a: ReleaseInfo, b: ReleaseInfo, durA: Long?, durB: Long?): Boolean {
    if (a.edition != b.edition) return false
    if (durA == null || durB == null || durA <= 0L || durB <= 0L) return false
    return sameDuration(durA, durB)
}

private fun sameDuration(a: Long, b: Long): Boolean = abs(a - b) <= SAME_DURATION_SHARE * max(a, b)

private class Facts<T>(val candidate: SmartCandidate<T>, val info: ReleaseInfo, keys: CandidateKeys<T>, language: String?) {
    val sourceKey = keys.sourceKey(candidate.item)
    val serverKey = keys.serverKey(candidate.item)
    val fileKey = keys.fileKey(candidate.item)
    val durationMs = keys.durationMs(candidate.item)
    val level = levelOf(info)
    val audioTier = SmartSourceRanker.audioTier(info, language)
    val sizeBytes = candidate.sizeBytes?.takeIf { it > 0 } ?: info.textSizeBytes
    val fileName = candidate.fileName?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    /** The file name without its extension: the release, wherever it is hosted. */
    val releaseName = fileName?.substringBeforeLast('.')?.takeIf { it.isNotEmpty() }
}

private fun <T> stepOf(current: Facts<T>, other: Facts<T>, query: SuccessorQuery<T>): Int? {
    val sameFileKey = current.fileKey != null && current.fileKey == other.fileKey
    val sameNameAndSize = current.fileName != null && current.fileName == other.fileName &&
        current.sizeBytes != null && current.sizeBytes == other.sizeBytes
    if (sameFileKey || sameNameAndSize) return 1
    val otherServer = current.serverKey == null || other.serverKey == null || current.serverKey != other.serverKey
    if (current.releaseName != null && current.releaseName == other.releaseName && otherServer) return 2
    val sameEdition = current.info.edition == other.info.edition
    if (!sameEdition) return null
    val otherDuration = other.durationMs
    val durationsAgree = query.durationMs == null || otherDuration == null || sameDuration(query.durationMs, otherDuration)
    if (other.audioTier == current.audioTier && other.level == current.level && durationsAgree) return 3
    return when (query.cause) {
        Cause.LINK -> {
            val from = current.level
            val to = other.level
            if (from != null && to != null && to.ordinal > from.ordinal) 4 else null
        }
        Cause.DECODER -> {
            val fromHeight = current.info.height
            val toHeight = other.info.height
            val lower = fromHeight != null && toHeight != null && toHeight < fromHeight
            val toAvc = other.info.codec == VideoCodec.AVC && current.info.codec != VideoCodec.AVC
            if (other.audioTier == current.audioTier && (lower || toAvc)) 4 else null
        }
        else -> null
    }
}

/** The filter every step shares (E5–E7, E10). Sound and first-byte checks are I/O: M4c does them through the port. */
private fun <T> passes(other: Facts<T>, query: SuccessorQuery<T>, memory: SessionMemory, nowMs: Long, applyCeiling: Boolean): Boolean {
    val measured = query.measuredBps
    if (measured != null) {
        val bitrate = fileBitrateBps(other.sizeBytes, query.durationMs)
        if (bitrate != null && bitrate > measured / FIT_HEADROOM) return false
    } else {
        val level = other.level
        if (level != null && level.height < LOWEST_UNMEASURED_HEIGHT) return false
    }
    // The text names another language (or subtitles only) for the one the viewer listens to: not the same audio.
    if (query.listeningLanguage != null &&
        (other.audioTier == SmartSourceRanker.AUDIO_OTHER || other.audioTier == SmartSourceRanker.AUDIO_SUBTITLES_ONLY)
    ) return false
    if (memory.failedThisSession(other.sourceKey)) return false
    if (memory.isServerBlocked(other.serverKey, nowMs)) return false
    if (memory.isLevelBlocked(other.level, nowMs)) return false
    if (memory.isDecoderPenalised(other.info.codec, other.info.height, null)) return false
    if (applyCeiling) {
        val ceiling = memory.ceiling(nowMs)
        val level = other.level
        if (ceiling != null && level != null && level.ordinal < ceiling.ordinal) return false
    }
    return true
}

/** [SmartSourceRanker] order inside one step, with the start's preset (a metered network ranks as SAVER). */
private fun <T> ranked(found: List<Successor<T>>, query: SuccessorQuery<T>): List<Successor<T>> {
    if (found.isEmpty()) return found
    val policy = if (query.policy.metered) query.policy.copy(preset = SmartPreset.SAVER) else query.policy
    val byIndex = found.mapIndexed { index, s ->
        SmartCandidate(index, s.candidate.text, s.candidate.sizeBytes, s.candidate.playsNow, s.candidate.fileName, s.candidate.blocked)
    }
    // The ranker leaves out junk and other episodes: never a successor either.
    return SmartSourceRanker.rank(byIndex, policy, query.expected).map { found[it.item] }
}
