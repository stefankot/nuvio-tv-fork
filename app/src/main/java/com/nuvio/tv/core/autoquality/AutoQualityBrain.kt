package com.nuvio.tv.core.autoquality

import com.nuvio.tv.core.autoquality.echo.AutoQualityDecision
import com.nuvio.tv.core.autoquality.echo.AutoQualityPolicy
import com.nuvio.tv.core.autoquality.echo.AutoQualitySample
import com.nuvio.tv.core.autoquality.echo.StreamQuality
import com.nuvio.tv.core.streams.smart.ReleaseInfo
import com.nuvio.tv.core.streams.smart.ReleaseParser
import com.nuvio.tv.core.streams.smart.SmartCandidate
import com.nuvio.tv.core.streams.smart.SmartExpected
import com.nuvio.tv.core.streams.smart.SmartPolicy

/** forge M4a: what the brain tells the player to do. */
sealed interface Plan {
    data object Stay : Plan
    /** The address expired: resolve the same file again and resume; no notice, not counted as a change. */
    data class RenewAddress(val reason: String) : Plan
    /** [ladder]: the successors to try in order ([SmartCandidate]s); [target]: the level of the first. */
    data class Switch(
        val way: Way, val target: StreamQuality?, val cause: Cause?, val isUpgrade: Boolean,
        val ladder: List<Any>, val timelineChanged: Boolean,
    ) : Plan
    data object NoAlternative : Plan
    enum class Way { PLANNED_4A, IMMEDIATE_4B }
}

/**
 * forge M4a: what the brain needs to know about the playback right now. [presetLevel]: the level the preset started
 * on (the only level a return up goes to); [remainingMs]: time to the end; [startupBps]: the link measured at start
 * (M2b); [currentBitrateBps]: from the player when it knows it; [pinned]: the viewer picked the source by hand.
 */
data class PlaybackFacts<T>(
    val current: SmartCandidate<T>,
    val currentInfo: ReleaseInfo,
    val candidates: List<SmartCandidate<T>>,
    val policy: SmartPolicy,
    val expected: SmartExpected? = null,
    val listeningLanguage: String? = null,
    val durationMs: Long? = null,
    val remainingMs: Long? = null,
    val presetLevel: StreamQuality? = null,
    val startupBps: Long? = null,
    val currentBitrateBps: Long? = null,
    val currentFps: Double? = null,
    val pinned: Boolean = false,
)

/**
 * forge M4a: the decision during playback, and the only place that calls Echo's policy (SENSITIVE mode). Echo says
 * when quality must go down or may go up; our layer says why (address / server / link / decoder) and to what.
 * One brain per title or episode; [memory] outlives it. Every time is on [AutoQualitySession.nowMs]'s clock.
 */
class AutoQualityBrain<T>(
    private val keys: CandidateKeys<T>,
    private val memory: SessionMemory = AutoQualitySession.memory,
    private val echoEnabled: Boolean = AutoQualityFlags.ECHO_ENABLED,
    private val hotSwapEnabled: Boolean = AutoQualityFlags.HOT_SWAP_ENABLED,
) {
    private val policy = AutoQualityPolicy(AutoQualityPolicy.SENSITIVE)
    private val rescues = ArrayDeque<Long>()
    private var upgradeUsed = false
    private var lastServerSwitchAtMs: Long? = null
    private var levelsOf: List<SmartCandidate<T>>? = null
    private var levels: Set<StreamQuality> = emptySet()

    private val plannedPossible get() = echoEnabled && hotSwapEnabled

    /** One look at playback (about once a second). */
    @Synchronized
    fun onSample(sample: AutoQualitySample, facts: PlaybackFacts<T>): Plan {
        if (!echoEnabled || facts.pinned) return Plan.Stay
        val level = levelOf(facts.currentInfo)
        val decision = policy.evaluate(sample, level, availableLevels(facts, level), fileBitrate(facts))
        if (decision !is AutoQualityDecision.Change) return Plan.Stay
        val now = sample.nowMs
        if (decision.isUpgrade) return planReturnUp(decision.target, level, now, facts)
        val signal = if (decision.early) Signal.Early(now) else Signal.Urgent(now)
        return rescue(diagnose(signal, linkFacts(facts, level), now), planned = decision.early, now, facts, decision.target)
    }

    /** Something the player or the monitor reported outside the samples (an error, dropped frames). */
    @Synchronized
    fun onSignal(signal: Signal, nowMs: Long, facts: PlaybackFacts<T>): Plan {
        val cause = diagnose(signal, linkFacts(facts, levelOf(facts.currentInfo)), nowMs)
        if (cause == Cause.ADDRESS_EXPIRED) {
            val status = (signal as? Signal.PlayerError)?.httpStatus
            return Plan.RenewAddress("http ${status ?: "-"}")
        }
        if (facts.pinned) return Plan.Stay
        val planned = signal is Signal.Early || signal is Signal.DroppedFrames
        return rescue(cause, planned, nowMs, facts, policyTarget = null)
    }

    /** The switch of [plan] was made (playing from [fromLevel] before). */
    @Synchronized
    fun onSwitched(nowMs: Long, plan: Plan.Switch, fromLevel: StreamQuality?) {
        val target = plan.target
        val down = target != null && fromLevel != null && target.ordinal > fromLevel.ordinal
        policy.onSourceChanged(nowMs, wasDowngrade = down)
        if (plan.isUpgrade) {
            upgradeUsed = true
            memory.clearCeiling()
        } else {
            rescues.addLast(nowMs)
        }
        if (plan.cause == Cause.SERVER) lastServerSwitchAtMs = nowMs
        if (plan.cause == Cause.LINK && target != null) memory.setCeiling(target, nowMs)
    }

    /** Playback started (first frame): the policy settles before judging it. */
    @Synchronized
    fun onStarted(nowMs: Long) = policy.onSourceChanged(nowMs, wasDowngrade = false)

    /** The same file plays again from a renewed address: judged afresh, not counted as a change. */
    @Synchronized
    fun onAddressRenewed(nowMs: Long) = policy.onSourceChanged(nowMs, wasDowngrade = false)

    /** The viewer cancelled an announced change: nothing changes for [CANCEL_QUIET_MS]. */
    @Synchronized
    fun onCancelled(nowMs: Long) = policy.snooze(nowMs, CANCEL_QUIET_MS)

    /** No source of [level] played. */
    @Synchronized
    fun onLevelFailed(level: StreamQuality, nowMs: Long) {
        policy.block(level, nowMs)
        memory.blockLevel(level, nowMs)
    }

    private fun planReturnUp(target: StreamQuality, level: StreamQuality?, nowMs: Long, facts: PlaybackFacts<T>): Plan {
        val preset = facts.presetLevel ?: return Plan.Stay
        if (level == null || upgradeUsed || facts.policy.metered || !plannedPossible) return Plan.Stay
        // Up is only the return to the preset's level, and only when the link carries at least that.
        if (level.ordinal <= preset.ordinal || target.ordinal > preset.ordinal) return Plan.Stay
        val remaining = facts.remainingMs
        if (remaining != null && remaining < NO_RETURN_BEFORE_END_MS) return Plan.Stay
        val found = returnUp(query(Cause.LINK, facts), facts.candidates, memory, nowMs, keys, preset)
        if (found.isEmpty()) return Plan.Stay
        return Plan.Switch(Plan.Way.PLANNED_4A, preset, cause = null, isUpgrade = true, ladder = found.map { it.candidate }, timelineChanged = false)
    }

    private fun rescue(cause: Cause, planned: Boolean, nowMs: Long, facts: PlaybackFacts<T>, policyTarget: StreamQuality?): Plan {
        if (cause == Cause.ADDRESS_EXPIRED) return Plan.RenewAddress("expired")
        while (rescues.isNotEmpty() && nowMs - rescues.first() >= FUSE_WINDOW_MS) rescues.removeFirst()
        if (rescues.size >= FUSE_RESCUES) return Plan.NoAlternative
        when (cause) {
            Cause.SERVER -> keys.serverKey(facts.current.item)?.let { memory.blockServer(it, nowMs) }
            Cause.DECODER -> memory.penaliseDecoder(facts.currentInfo.codec, facts.currentInfo.height, facts.currentFps)
            else -> Unit
        }
        val all = successors(query(cause, facts), facts.candidates, memory, nowMs, keys)
        val chosen = when (cause) {
            // the same quality on another server: the policy's target down is ignored
            Cause.SERVER -> all.filter { it.step <= 3 }
            // the policy's target (or lower), the target first
            Cause.LINK -> all.filter { it.step == 4 }
                .filter { s -> policyTarget == null || (levelOf(s.info)?.ordinal ?: -1) >= policyTarget.ordinal }
                .sortedBy { levelOf(it.info)?.ordinal ?: Int.MAX_VALUE }
            Cause.DECODER -> all.filter { it.step == 4 }
            Cause.ADDRESS_EXPIRED -> emptyList()
        }
        val first = chosen.firstOrNull() ?: return Plan.NoAlternative
        val timelineChanged = first.step != 1 &&
            !sharedTimeline(facts.currentInfo, first.info, facts.durationMs, keys.durationMs(first.candidate.item))
        val way = if (planned && plannedPossible && !timelineChanged) Plan.Way.PLANNED_4A else Plan.Way.IMMEDIATE_4B
        return Plan.Switch(way, levelOf(first.info), cause, isUpgrade = false, ladder = chosen.map { it.candidate }, timelineChanged = timelineChanged)
    }

    private fun query(cause: Cause, facts: PlaybackFacts<T>) = SuccessorQuery(
        current = facts.current,
        currentInfo = facts.currentInfo,
        cause = cause,
        listeningLanguage = facts.listeningLanguage,
        durationMs = facts.durationMs,
        // a sick server says nothing about the link: there the start's measurement counts
        measuredBps = if (cause == Cause.SERVER) facts.startupBps ?: policy.throughputEstimateBps
        else policy.throughputEstimateBps ?: facts.startupBps,
        policy = facts.policy,
        expected = facts.expected,
    )

    private fun linkFacts(facts: PlaybackFacts<T>, level: StreamQuality?) = LinkFacts(
        startupBps = facts.startupBps,
        currentBitrateBps = fileBitrate(facts) ?: level?.let { AutoQualityPolicy.typicalBitrateBps(it) },
        lastServerSwitchAtMs = lastServerSwitchAtMs,
    )

    /** The player's figure, else size ÷ time; null lets the policy use the level's typical bitrate. */
    private fun fileBitrate(facts: PlaybackFacts<T>): Long? = facts.currentBitrateBps
        ?: fileBitrateBps(facts.current.sizeBytes?.takeIf { it > 0 } ?: facts.currentInfo.textSizeBytes, facts.durationMs)

    /** Levels on offer; parsed once per list of candidates, not on every sample. */
    private fun availableLevels(facts: PlaybackFacts<T>, level: StreamQuality?): Set<StreamQuality> {
        if (levelsOf !== facts.candidates) {
            levelsOf = facts.candidates
            levels = facts.candidates.mapNotNull { levelOf(ReleaseParser.parse(it.text, it.sizeBytes)) }.toSet()
        }
        return if (level == null) levels else levels + level
    }

    companion object {
        const val CANCEL_QUIET_MS = 5 * 60_000L
        const val NO_RETURN_BEFORE_END_MS = 10 * 60_000L
        /** Fuse: this many rescue changes within [FUSE_WINDOW_MS] → no alternative. */
        const val FUSE_RESCUES = 4
        const val FUSE_WINDOW_MS = 10 * 60_000L
    }
}
