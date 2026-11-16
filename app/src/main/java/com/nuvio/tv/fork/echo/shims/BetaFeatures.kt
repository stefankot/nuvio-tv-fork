package com.nuvio.tv.core.player

import android.content.Context
import com.nuvio.tv.data.local.BetaPerformanceSettings
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/** Echo shim: Echo reads these from a DataStore; the fork reads its own ForkFeatures switches (default off). */
object BetaFeatures {
    fun settings(context: Context): BetaPerformanceSettings = BetaPerformanceSettings(
        raceAutoPlaySources = ForkFeatures.enabled(context, "echo_race"),
        sourceFallback = ForkFeatures.enabled(context, "echo_source_fallback"),
        sourceSwitchTimeoutSeconds = 8,
        autoQuality = ForkFeatures.enabled(context, "echo_auto_quality"),
        autoQualityHotSwap = ForkFeatures.enabled(context, "echo_hot_swap"),
        playbackIntro = ForkFeatures.enabled(context, "echo_playback_intro"),
        audioLanguageMenu = ForkFeatures.enabled(context, "echo_audio_menu"),
    )
}
