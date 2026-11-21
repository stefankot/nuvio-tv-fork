package com.nuvio.tv.ui.screens.player

import kotlin.math.roundToLong

/** Pure timing rules shared by the calibration UI, Media3 source and libmpv adapter. */
object DubbingTiming {
    const val MIN_OFFSET_MS: Long = -30L * 60L * 1_000L
    const val MAX_OFFSET_MS: Long = 30L * 60L * 1_000L

    fun clampOffset(offsetMs: Long): Long = offsetMs.coerceIn(MIN_OFFSET_MS, MAX_OFFSET_MS)

    /**
     * Offset convention: at a main-video instant A, source B must be at A + offset.
     */
    fun sourcePositionMs(mainPositionMs: Long, offsetMs: Long, sourceDurationMs: Long? = null): Long {
        val position = (mainPositionMs + clampOffset(offsetMs)).coerceAtLeast(0L)
        return sourceDurationMs?.takeIf { it >= 0L }?.let(position::coerceAtMost) ?: position
    }

    fun sourcePositionUs(mainPositionUs: Long, offsetMs: Long): Long =
        (mainPositionUs + clampOffset(offsetMs) * 1_000L).coerceAtLeast(0L)

    fun presentationPositionUs(sourcePositionUs: Long, offsetMs: Long): Long =
        (sourcePositionUs - clampOffset(offsetMs) * 1_000L).coerceAtLeast(0L)

    /** Keep A's requested clock while B is still in its intentional leading silent gap. */
    fun resolvedSeekPositionUs(requestedMainUs: Long, childPositionUs: Long, offsetMs: Long): Long =
        if (requestedMainUs + clampOffset(offsetMs) * 1_000L < 0L) {
            requestedMainUs
        } else {
            presentationPositionUs(childPositionUs, offsetMs)
        }

    /**
     * mpv's audio-delay uses the opposite sign from our B=A+offset convention.
     * Device lip-sync compensation remains independent and is composed here.
     */
    fun effectiveMpvAudioDelaySeconds(deviceDelayMs: Int, offsetMs: Long): Double =
        (deviceDelayMs.toLong() - clampOffset(offsetMs)).toDouble() / 1_000.0

    fun secondsToOffsetMs(seconds: Double): Long = clampOffset((seconds * 1_000.0).roundToLong())
}
