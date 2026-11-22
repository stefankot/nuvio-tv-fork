package com.nuvio.tv.core.player

import android.content.Context
import com.nuvio.tv.data.local.BetaPerformanceSettings
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/** Echo shim: Echo reads these from a DataStore; the fork reads its own ForkFeatures switches (default off). */
object BetaFeatures {
    fun settings(context: Context): BetaPerformanceSettings = BetaPerformanceSettings(
        // Właściciel 22.11: automatyczny wybór źródła tylko natywny (Regex Nuvio) — wyścig i zapasowe źródło Echo wyłączone na stałe.
        raceAutoPlaySources = false,
        sourceFallback = false,
        sourceSwitchTimeoutSeconds = 8,
        autoQuality = ForkFeatures.enabled(context, "echo_auto_quality"),
        autoQualityHotSwap = ForkFeatures.enabled(context, "echo_hot_swap"),
        playbackIntro = false, // intro wyłączone na stałe (właściciel 19.11)
        audioLanguageMenu = ForkFeatures.enabled(context, "echo_audio_menu"),
    )
}
