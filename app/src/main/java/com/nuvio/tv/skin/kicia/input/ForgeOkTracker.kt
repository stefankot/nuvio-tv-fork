package com.nuvio.tv.skin.kicia.input

import com.nuvio.tv.skin.kicia.tokens.ForgeMotion

/**
 * OK = act at once, long OK (>= 520 ms) = more options. One rule for the whole app (owner's decision).
 *
 * Pure logic with explicit timestamps so it can be tested on the JVM. Feed it the key-down events
 * (including the auto-repeats the remote sends while the key is held) and the key-up event.
 */
class ForgeOkTracker(private val thresholdMs: Long = ForgeMotion.LongPressMs) {
    enum class Result { NONE, CLICK, LONG_PRESS }

    private var downAt = -1L
    private var longFired = false

    /** OK key went down or auto-repeated. Returns [Result.LONG_PRESS] exactly once when the threshold is crossed. */
    fun down(timeMs: Long, systemLong: Boolean = false): Result { // v7.4: systemLong = the system's own long-press flag on the repeat
        if (downAt < 0) {
            downAt = timeMs
            longFired = false
            return Result.NONE
        }
        if (!longFired && (systemLong || timeMs - downAt >= thresholdMs)) {
            longFired = true
            return Result.LONG_PRESS
        }
        return Result.NONE
    }

    /** OK key went up. A short press is a click; a hold whose repeat never arrived still counts as long. */
    fun up(timeMs: Long): Result {
        val started = downAt
        val alreadyLong = longFired
        reset()
        if (started < 0) return Result.NONE
        if (alreadyLong) return Result.NONE
        return if (timeMs - started >= thresholdMs) Result.LONG_PRESS else Result.CLICK
    }

    fun reset() {
        downAt = -1L
        longFired = false
    }

    val isHeld: Boolean get() = downAt >= 0
}
