package com.nuvio.tv.core.player.recovery

import com.nuvio.tv.core.streams.smart.Edition
import kotlin.math.abs

/** forge (N1): does the opened file last as long as the title? Pure; automatic mode, startup only. */
enum class DurationVerdict { OK, WRONG, NOT_JUDGED }

/** Shorter declared times are not trusted (menus, trailers, wrong metadata). */
const val DURATION_MIN_EXPECTED_SEC = 120L

/**
 * Film: wrong when it differs from the declared time by more than 10%. Episode: only when it is more than twice as
 * long or less than half (an episode's declared time is often the series' average). A named edition, anything
 * missing or a declared time under two minutes is not judged. The caller passes null for live or non-seekable media.
 */
fun judgeDuration(fileMs: Long?, expectedSec: Long?, isEpisode: Boolean, edition: Edition?): DurationVerdict {
    if (fileMs == null || expectedSec == null || fileMs <= 0 || expectedSec < DURATION_MIN_EXPECTED_SEC || edition != null) {
        return DurationVerdict.NOT_JUDGED
    }
    val expectedMs = expectedSec * 1000.0
    val file = fileMs.toDouble()
    val wrong = if (isEpisode) file > 2 * expectedMs || file < expectedMs / 2 else abs(file - expectedMs) > 0.10 * expectedMs
    return if (wrong) DurationVerdict.WRONG else DurationVerdict.OK
}

/** Files shorter than this are always just wrong, never evidence that the declared time is wrong. */
const val SANITY_MIN_FILE_MS = 5 * 60 * 1000L

/**
 * One title's startup. Two wrong files in a row that agree with each other (within 2%) mean the declared time is
 * wrong, not the files: the second is played and judging stops for this title.
 */
class DurationSanity {
    private var lastKey: String? = null
    private var lastMs: Long? = null

    var disabled: Boolean = false
        private set

    /**
     * true = this is a good file after all (the declaration is wrong); the caller plays it. A file under five minutes
     * never trips it (owner 13.11): two identical "Not ready yet" placeholders must not be taken for the real thing.
     */
    fun onWrong(key: String, fileMs: Long): Boolean {
        if (fileMs < SANITY_MIN_FILE_MS) return false
        if (disabled) return true
        val previous = lastMs
        if (previous != null && previous > 0 && abs(fileMs - previous) <= previous * 0.02) {
            disabled = true
            return true
        }
        lastKey = key
        lastMs = fileMs
        return false
    }
}
