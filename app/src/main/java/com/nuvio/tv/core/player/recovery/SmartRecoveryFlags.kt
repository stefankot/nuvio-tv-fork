package com.nuvio.tv.core.player.recovery

/**
 * forge M3c: switches for the startup checks taken from Echo and for our own additions, so each can be turned off
 * without touching the rest. `false` = startup behaves as after M3b.
 */
object SmartRecoveryFlags {
    /** K1 + K2: the address probe and the slow-start check (Echo `AutoPlayRace`, `…AutoPlayHedge`). */
    const val ECHO_PROBE_ENABLED = true
    /** K4: Echo's definitive source failure (no file, no access, 4xx except 408/416/429) skips the retry ladder. */
    const val ECHO_DEFINITIVE_ENABLED = true
    /** K6 (M4b): Echo's source switch session for „Spróbuj innego źródła” and route 4B. Wired 13.11 (forge M4b K6): „Inne źródło”, player errors, Back. */
    const val ECHO_SWITCH_SESSION_ENABLED = true
    /** K3 (ours): an attempt that is still receiving data is not cut at 15 s. */
    const val ADAPTIVE_ATTEMPT_ENABLED = true
    /** K5 (ours): a file whose sound the device does not play fails the attempt. */
    const val SILENT_CHECK_ENABLED = true
}
