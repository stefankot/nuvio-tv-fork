package com.nuvio.tv.fork.perf

import android.content.Context
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/** Przełączniki poprawek wydajności (moduł perf). Wyłączone = zachowanie jak w bazie. */
object PerfFlags {
    const val PREVIEW_AUTOSTOP_KEY = "perf_preview_autostop"
    const val PREVIEW_IDLE_STOP_MS = 60_000L
    fun previewAutoStop(c: Context) = ForkFeatures.enabled(c, PREVIEW_AUTOSTOP_KEY, true)
}
