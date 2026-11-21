package com.nuvio.tv.fork.echo

import com.nuvio.tv.core.streams.AutoQualitySample

/** Counts consecutive samples of an empty playback buffer, scoped to one playback session. */
internal class PlaybackStarvation {
    private var ignoreUntilMs = Long.MIN_VALUE
    private var lastSampleMs: Long? = null
    private var emptySinceMs: Long? = null

    fun localTransition(nowMs: Long, graceMs: Long) {
        ignoreUntilMs = maxOf(ignoreUntilMs, nowMs + graceMs)
        clearSamples()
    }

    fun isLocalTransition(nowMs: Long): Boolean = nowMs < ignoreUntilMs

    fun clearSamples() {
        lastSampleMs = null
        emptySinceMs = null
    }

    fun observe(sample: AutoQualitySample) {
        if (!isStarving(sample)) {
            clearSamples()
            return
        }
        val now = sample.nowMs
        val last = lastSampleMs
        if (last == null || now - last > MAX_SAMPLE_GAP_MS) emptySinceMs = now
        lastSampleMs = now
    }

    fun permitsDowngrade(sample: AutoQualitySample): Boolean {
        val since = emptySinceMs ?: return false
        val last = lastSampleMs ?: return false
        return isStarving(sample) && sample.nowMs - last <= MAX_SAMPLE_GAP_MS &&
            sample.nowMs - since >= MIN_STARVATION_MS
    }

    private fun isStarving(sample: AutoQualitySample): Boolean =
        sample.wantsToPlay && sample.stalled && !isLocalTransition(sample.nowMs) &&
            sample.bufferedAheadMs?.let { it <= EMPTY_BUFFER_MS } != false

    private companion object {
        const val EMPTY_BUFFER_MS = 1_000L
        const val MAX_SAMPLE_GAP_MS = 2_500L
        const val MIN_STARVATION_MS = 8_000L
    }
}
