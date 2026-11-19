package com.nuvio.tv.fork.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import dagger.hilt.components.SingletonComponent

/** Domyślne wartości pierwszego uruchomienia profilu. Nie zmienia tego, co użytkownik już zapisał. */
object FirstRunDefaults {
    @EntryPoint @InstallIn(SingletonComponent::class)
    interface Deps { fun factory(): ProfileDataStoreFactory; fun profiles(): ProfileManager }

    const val LEVEL_KEY = "fork_settings_level"

    /**
     * Raz na instalację (także istniejącą): funkcje Reshaped, które autor ma domyślnie wyłączone, a właściciel chce
     * mieć od razu (18.11). Ustawiane tylko, gdy użytkownik nie zapisał wyboru. Funkcje forka: ForkFeatures.ON_BY_DEFAULT.
     */
    private fun reshapedOn(app: Context) {
        if (ForkFeatures.enabled(app, "fork_defaults_v1_done")) return
        if (!app.getSharedPreferences("nuvio_live_tv_menu", Context.MODE_PRIVATE).contains("live_tv_enabled"))
            com.nuvio.tv.reshaped.livetv.LiveTvPreferences.setEnabled(app, true)
        if (!app.getSharedPreferences("nuvio_tv_autosync", Context.MODE_PRIVATE).contains("automatic_subtitle_sync_enabled"))
            com.nuvio.tv.ui.screens.player.autosync.AutoSyncPreferences.setEnabled(app, true)
        ForkFeatures.set(app, "fork_defaults_v1_done", true)
    }

    /** Raz (także na istniejącej instalacji): napisy po polsku, gdy język napisów to wciąż domyślny angielski (właściciel 18.11). */
    private suspend fun polishSubtitles(app: Context) {
        if (ForkFeatures.enabled(app, "fork_defaults_v2_done")) return
        val store = EntryPointAccessors.fromApplication(app, com.nuvio.tv.fork.sources.SourceChoice.Deps::class.java).player()
        val s = store.playerSettings.first()
        if (s.subtitleStyle.preferredLanguage == "en") store.setSubtitlePreferredLanguage("pl")
        ForkFeatures.set(app, "fork_defaults_v2_done", true)
    }

    /**
     * Fork 19.11 (JMGO: „odtwarzanie się klatkuje”): seriale HDTV 25 kl./s na ekranie 60 Hz szarpią. Raz włącza
     * dopasowanie częstotliwości ekranu (START_STOP: 50 Hz dla 25 kl./s i powrót po wyjściu), gdy było wyłączone.
     */
    private suspend fun frameRateMatching(app: Context) {
        if (ForkFeatures.enabled(app, "fork_defaults_frm_done")) return
        val store = EntryPointAccessors.fromApplication(app, com.nuvio.tv.fork.sources.SourceChoice.Deps::class.java).player()
        if (store.playerSettings.first().frameRateMatchingMode == com.nuvio.tv.data.local.FrameRateMatchingMode.OFF)
            store.setFrameRateMatchingMode(com.nuvio.tv.data.local.FrameRateMatchingMode.START_STOP)
        ForkFeatures.set(app, "fork_defaults_frm_done", true)
    }

    /**
     * Kolejka 19.11 (W1 etap 2, „następny odcinek zawczasu”): baza ma wczytywanie źródeł następnego odcinka przed końcem
     * (`preloadNextEpisodeSources`, wybór przez StreamAutoPlaySelector = ranking KE), ale domyślnie wyłączone. Raz włącza.
     */
    private suspend fun preloadNext(app: Context) {
        if (ForkFeatures.enabled(app, "fork_defaults_preload_done")) return
        val store = EntryPointAccessors.fromApplication(app, com.nuvio.tv.fork.sources.SourceChoice.Deps::class.java).player()
        if (!store.playerSettings.first().preloadNextEpisodeSources) store.setPreloadNextEpisodeSources(true)
        ForkFeatures.set(app, "fork_defaults_preload_done", true)
    }

    /**
     * Raz na profil (także istniejący, właściciel 18.11 P2): automatyczny wybór źródła = tryb „pierwsze źródło”
     * z rankingiem KE (KeRanking) zamiast ręcznej listy. Zmienia tylko tryb MANUAL; Regex i inne zostają.
     */
    private suspend fun autoSource(app: Context, pid: Int) {
        val flag = "fork_defaults_v3_done_$pid"
        if (ForkFeatures.enabled(app, flag)) return
        val store = EntryPointAccessors.fromApplication(app, com.nuvio.tv.fork.sources.SourceChoice.Deps::class.java).player()
        if (store.playerSettings.first().streamAutoPlayMode == com.nuvio.tv.data.local.StreamAutoPlayMode.MANUAL)
            store.setStreamAutoPlayMode(com.nuvio.tv.data.local.StreamAutoPlayMode.FIRST_STREAM)
        ForkFeatures.set(app, flag, true)
    }

    suspend fun run(c: Context) {
        val app = c.applicationContext
        reshapedOn(app)
        runCatching { polishSubtitles(app) }
        runCatching { frameRateMatching(app) }
        runCatching { preloadNext(app) }
        val d = EntryPointAccessors.fromApplication(app, Deps::class.java)
        val pid = d.profiles().activeProfileId.value
        runCatching { autoSource(app, pid) }
        val flag = "first_run_done_$pid"
        if (ForkFeatures.enabled(app, flag)) return
        val en = booleanPreferencesKey("tmdb_enabled"); val lang = stringPreferencesKey("tmdb_language")
        d.factory().get(pid, "tmdb_settings").edit { p ->
            val r = FirstRunPlan.decide(FirstRunPlan.Stored(p[en], p[lang], null, null))
            r.tmdbEnabled?.let { p[en] = it }; r.tmdbLanguage?.let { p[lang] = it }
        }
        val locPrefs = app.getSharedPreferences("app_locale", Context.MODE_PRIVATE)
        val recoveryPrefs = app.getSharedPreferences("nuvio_auto_source_recovery", Context.MODE_PRIVATE)
        val r = FirstRunPlan.decide(FirstRunPlan.Stored(true, "x", locPrefs.getString("locale_tag", null), ForkFeatures.str(app, LEVEL_KEY).ifEmpty { null },
            autoRecovery = if (recoveryPrefs.contains("enabled")) true else null))
        if (r.autoRecovery == true) {
            com.nuvio.tv.core.player.recovery.AutoSourceRecoverySettings.setEnabled(app, true)
            val player = EntryPointAccessors.fromApplication(app, com.nuvio.tv.fork.sources.SourceChoice.Deps::class.java).player()
            player.setStreamAutoPlayFallbackOnFailure(true); player.setStreamAutoPlayFallbackMaxSources(3); player.setStreamAutoPlayFallbackStartupTimeoutSeconds(15)
        }
        r.appLocale?.let { locPrefs.edit().putString("locale_tag", it).apply() }
        r.level?.let { ForkFeatures.setStr(app, LEVEL_KEY, it) }
        ForkFeatures.set(app, flag, true)
    }
}
