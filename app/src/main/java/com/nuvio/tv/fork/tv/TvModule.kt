package com.nuvio.tv.fork.tv

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
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
    override val id = "tv"
    override val title = "Telewizja: dodatki (przewodnik w tle, logo, obejrzane)"
    override val defaultEnabled = true

    override fun actions() = listOf(
        com.nuvio.tv.fork.registry.ModuleAction("Wyczyść historię oglądania TV", com.nuvio.tv.fork.registry.SettingsLevel.ADVANCED, "Kasuje stan obejrzanych odcinków, obserwowane i statystykę kanałów") {
            com.nuvio.tv.fork.tv.most.TvBridge.data?.clearHistory()
        },
    )

    override fun settings() = listOf(
        com.nuvio.tv.fork.registry.ModuleSetting("tv_auto_follow", com.nuvio.tv.fork.registry.SettingsLevel.ESSENTIAL, "Telewizja", "Automatyczne obserwowane programy", "Serial staje się obserwowany po 2 obejrzanych odcinkach (można cofnąć)", defaultValue = true),
        com.nuvio.tv.fork.registry.ModuleSetting("tv_watched_strict", com.nuvio.tv.fork.registry.SettingsLevel.ESSENTIAL, "Telewizja", "Surowe liczenie obejrzanych", "Odcinek jest obejrzany dopiero po 80% czasu (zamiast 50%)", defaultValue = false),
        com.nuvio.tv.fork.registry.ModuleSetting(
            com.nuvio.tv.fork.tv.guide.ExtraGuides.KEY, com.nuvio.tv.fork.registry.SettingsLevel.ADVANCED, "Telewizja",
            "Przewodnik uzupełniający dla kraju listy", "Dodaje drugi przewodnik (obrazy, odcinki), gdy rozpozna kraj kanałów", defaultValue = true,
        ),
    )

    @EntryPoint @InstallIn(SingletonComponent::class)
    interface Deps { fun profiles(): ProfileManager }

    override fun rootOverlays(): List<@androidx.compose.runtime.Composable () -> Unit> = listOf({
        val c: Context = LocalContext.current
        LaunchedEffect(Unit) {
            com.nuvio.tv.fork.tv.logo.ForkLogoPrefs.load(c)
            val pid = EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).profiles().activeProfileId.value
            LiveTvRepository.warmUp(c, pid)
        }
    })
}
