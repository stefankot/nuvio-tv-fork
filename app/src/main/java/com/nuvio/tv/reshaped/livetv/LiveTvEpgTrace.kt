package com.nuvio.tv.reshaped.livetv

import android.os.SystemClock
import android.util.Log

/**
 * Timing log for the guide's start-up (logcat tag `LiveTvEpg`). One line per step, with the time
 * since [begin], so a cold start, a saved-guide hit or a slow download can be read off the debug
 * log. It only logs; it changes no behaviour. Never put a guide link here (links can hold a login).
 */
// Fork patch: dostawca #11 (plik z Kicia 1:1)
internal object LiveTvEpgTrace {
    const val TAG = "LiveTvEpg"

    @Volatile
    private var startedAt = 0L

    fun begin(detail: String) {
        startedAt = SystemClock.elapsedRealtime()
        Log.i(TAG, "begin $detail")
    }

    fun step(what: String) {
        Log.i(TAG, "+${SystemClock.elapsedRealtime() - startedAt}ms $what")
    }
}
