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
        sourceSwitchTimeoutSeconds = 20, // przegląd 27.11, ekran 05 pkt 3: plik 1080p otwierany w połowie filmu na JMGO nie zdążał w 8 s (6 prób w 63 s)
        // Właściciel 22.11 (aktualizacja bazy): automatyczna zmiana źródła tylko jak w Reshaped — Auto jakość Echo odłączona (wpięcie w PlayerViewModel usunięte).
        autoQuality = false,
        autoQualityHotSwap = false,
        playbackIntro = false, // intro wyłączone na stałe (właściciel 19.11)
        audioLanguageMenu = ForkFeatures.enabled(context, "echo_audio_menu"),
        // Test VOD JMGO 26.11: domyślne języki Echo (oryginał, angielski, japoński, koreański) dawały w oknie Dźwięk
        // japoński i koreański „No sources”, a polskiego lektora/dubbingu z innych plików nie było. Polski użytkownik: oryginał, polski, angielski.
        audioMenuLanguages = setOf(
            com.nuvio.tv.core.streams.AudioLanguage.ORIGINAL_KEY,
            com.nuvio.tv.core.streams.AudioLanguage.POLISH.code,
            com.nuvio.tv.core.streams.AudioLanguage.ENGLISH.code,
        ),
    )
}
