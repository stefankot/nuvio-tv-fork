package com.nuvio.tv.core.autoquality

/**
 * forge M4: one switch per part of the quality change during playback, so each can be turned off alone.
 * Parts not wired yet stay `false` until the module that wires them (as in `SmartRecoveryFlags`).
 */
object AutoQualityFlags {
    /** The whole Echo package (`core/autoquality/echo`): off = no policy samples, route 4B with our layer only. */
    const val ECHO_ENABLED = true
    /** Route 4A (planned, seamless; Echo hot swap). Off = every change goes route 4B. Wired in M4b. */
    const val HOT_SWAP_ENABLED = true // owner 13.11: on (keyframes from the seek-preview index readers)
    /** N10: warming the next file's server. Wired in M4c. */
    const val PREFETCH_ENABLED = true
    /** N11: buffer tuning by bitrate, link and memory. Wired in M4c. */
    const val BUFFER_TUNING_ENABLED = true
}
