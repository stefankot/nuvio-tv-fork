package com.nuvio.tv.fork.tv

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.composable
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Moduł telewizji (B01+): wczytanie przewodnika w tle po starcie. */
object TvModule : ForkModule {
    private const val WARM_UP_DELAY_MS = 8_000L
    override val id = "tv"
    override val title = "Telewizja: dodatki (przewodnik w tle, logo, obejrzane)"
    override val defaultEnabled = true

    override fun actions() = listOf(
        com.nuvio.tv.fork.registry.ModuleAction("Wyczyść historię oglądania TV", com.nuvio.tv.fork.registry.SettingsLevel.ADVANCED, "Kasuje stan obejrzanych odcinków, obserwowane i statystykę kanałów") {
            com.nuvio.tv.fork.tv.most.TvBridge.data?.clearHistory()
        },
    )

    override fun settings() = listOf(
        com.nuvio.tv.fork.registry.ModuleSetting(com.nuvio.tv.fork.perf.PerfFlags.PREVIEW_AUTOSTOP_KEY, com.nuvio.tv.fork.registry.SettingsLevel.ADVANCED, "Telewizja", "Zatrzymaj podgląd kanału po minucie", "Oszczędza procesor słabych telewizorów; zostaje logo, zmiana kanału wznawia podgląd", defaultValue = true),
        com.nuvio.tv.fork.registry.ModuleSetting("tv_for_you_row", com.nuvio.tv.fork.registry.SettingsLevel.ESSENTIAL, "Telewizja", "Rząd „Dla Ciebie” nad przewodnikiem", "Karty: wznów, nowy odcinek, na żywo", defaultValue = true),
        com.nuvio.tv.fork.registry.ModuleSetting("tv_reminders", com.nuvio.tv.fork.registry.SettingsLevel.ESSENTIAL, "Telewizja", "Przypomnienia o programach", "Karta nad obrazem na chwilę przed startem", defaultValue = true),
        com.nuvio.tv.fork.registry.ModuleSetting("tv_auto_follow", com.nuvio.tv.fork.registry.SettingsLevel.ESSENTIAL, "Telewizja", "Automatyczne obserwowane programy", "Serial staje się obserwowany po 2 obejrzanych odcinkach (można cofnąć)", defaultValue = true),
        com.nuvio.tv.fork.registry.ModuleSetting("tv_watched_strict", com.nuvio.tv.fork.registry.SettingsLevel.ESSENTIAL, "Telewizja", "Surowe liczenie obejrzanych", "Odcinek jest obejrzany dopiero po 80% czasu (zamiast 50%)", defaultValue = false),
        com.nuvio.tv.fork.registry.ModuleSetting(
            com.nuvio.tv.fork.tv.guide.ExtraGuides.KEY, com.nuvio.tv.fork.registry.SettingsLevel.ADVANCED, "Telewizja",
            "Przewodnik uzupełniający dla kraju listy", "Dodaje drugi przewodnik (obrazy, odcinki), gdy rozpozna kraj kanałów", defaultValue = true,
        ),
    )

    /** B03-9: rząd „Telewizja: dla Ciebie” na ekranie głównym; OK startuje kanał przez PendingPlay. */
    override fun homeRows() = listOf(
        com.nuvio.tv.fork.registry.ModuleHomeRow("tv_for_you", "Telewizja: dla Ciebie", order = 50) {
            com.nuvio.tv.fork.tv.ekrany.ForYouRow { channel, _ ->
                com.nuvio.tv.fork.tv.ekrany.PendingPlay.ask(channel.streamUrl)
                com.nuvio.tv.ui.screens.settings.ForkFeatures.open?.invoke(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE)
            }
        },
    )

    override fun routes() = listOf(
        com.nuvio.tv.fork.registry.ModuleRoute("fork_program/{key}") {
            composable("fork_program/{key}") { entry ->
                com.nuvio.tv.fork.tv.ekrany.ProgramPage(entry.arguments?.getString("key").orEmpty())
            }
        },
    )

    @EntryPoint @InstallIn(SingletonComponent::class)
    interface Deps { fun profiles(): ProfileManager }

    override fun rootOverlays(): List<@androidx.compose.runtime.Composable () -> Unit> = listOf({
        com.nuvio.tv.fork.tv.ekrany.ReminderToast()
        val c: Context = LocalContext.current
        LaunchedEffect(Unit) {
            com.nuvio.tv.fork.tv.logo.ForkLogoPrefs.load(c)
            val pid = EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).profiles().activeProfileId.value
            // Wydajność: rozgrzewka przewodnika dopiero po uspokojeniu startu (Home i okładki mają pierwszeństwo).
            kotlinx.coroutines.delay(WARM_UP_DELAY_MS)
            LiveTvRepository.warmUp(c, pid)
        }
    })
}
