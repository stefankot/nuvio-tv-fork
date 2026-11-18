package com.nuvio.tv.fork.echo

import android.content.Context
import com.nuvio.tv.data.local.BufferSettings
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * W7 (18.11, zgoda właściciela): szybszy start z Echo (`core/player/FastPlaybackStart`, `START_BUFFER_MS = 1_500`):
 * odtwarzanie rusza po 1,5 s bufora zamiast 5 s. Echo trzyma przełącznik w swoim BetaPerformanceSettingsDataStore;
 * tu przełącznik forka [KEY] (domyślnie ON). Wartość ustawiona ręcznie przez użytkownika (inna niż domyślna bazy) zostaje.
 */
object FastStart {
    const val KEY = "echo_fast_start"
    const val START_BUFFER_MS = 1_500 // Echo FastPlaybackStart.START_BUFFER_MS

    fun startBufferMs(context: Context?, configuredMs: Int): Int =
        if (context != null && configuredMs == BufferSettings.DEFAULT_BUFFER_FOR_PLAYBACK_MS && ForkFeatures.enabled(context, KEY, true))
            configuredMs.coerceAtMost(START_BUFFER_MS) else configuredMs
}
