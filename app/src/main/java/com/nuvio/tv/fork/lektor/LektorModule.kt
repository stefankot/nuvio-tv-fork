package com.nuvio.tv.fork.lektor

import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ModuleAction
import com.nuvio.tv.fork.registry.ModuleSetting
import com.nuvio.tv.fork.registry.SettingsLevel
import java.io.File

/** Lektor (B09): rejestracja i ustawienia. Miks w odtwarzaczu (B09-5) wymaga wpięcia w procesory dźwięku: jeszcze niepodpięty. */
object LektorModule : ForkModule {
    override val id = "lektor"
    override val title = "Lektor (głos syntetyczny z napisów)"
    override val defaultEnabled = false

    fun cache(c: android.content.Context) = SynthCache(File(c.cacheDir, "fork_lektor"))

    override fun settings() = listOf(
        ModuleSetting("lektor_auto", SettingsLevel.ESSENTIAL, "Lektor", "Włączaj automatycznie, gdy brak polskiego dźwięku", "Przy polskich napisach i braku polskiej ścieżki", defaultValue = false),
    )

    override fun actions() = listOf(
        ModuleAction("Lektor: wyczyść pamięć podręczną", SettingsLevel.ADVANCED, "Usuwa zsyntezowane kwestie (limit 200 MB)") { c -> cache(c).clear() },
    )
}
