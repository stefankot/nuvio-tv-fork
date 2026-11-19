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

    private const val K_VOL = "lektor_volume"; private const val K_DUCK = "lektor_duck"
    // Fork 20.11 (właściciel: „Gemini brzmi świetnie, ale film za cicho w porównaniu do lektora”): domyślnie lektor 70%
    // i film ściszany tylko o 6 dB (było 100% i −10 dB). Pierwsza wartość listy = domyślna.
    private val volumes = listOf("70", "50", "100", "130")
    private val ducks = listOf("-6", "-3", "-10", "-14")

    /** Wczytuje głośność lektora i siłę ściszania do silnika (przy włączeniu lektora). */
    fun applySettings(c: android.content.Context) {
        LektorEngine.voiceGain = (com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, K_VOL, volumes[0]).toIntOrNull() ?: 70) / 100f
        LektorEngine.setDuckDepth(com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, K_DUCK, ducks[0]).toFloatOrNull() ?: -6f)
    }

    private fun cycle(c: android.content.Context, key: String, values: List<String>) {
        val cur = com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, key, values[0])
        com.nuvio.tv.ui.screens.settings.ForkFeatures.setStr(c, key, values[(values.indexOf(cur) + 1) % values.size]); applySettings(c)
    }

    override fun actions() = listOf(
        ModuleAction("Lektor: głośność względem filmu (70% / 50% / 100% / 130%)", SettingsLevel.ESSENTIAL, "Kolejne naciśnięcia przełączają wartość") { c -> cycle(c, K_VOL, volumes) },
        ModuleAction("Lektor: ściszanie filmu pod lektorem (−6 / −3 / −10 / −14 dB)", SettingsLevel.ESSENTIAL, "Kolejne naciśnięcia przełączają wartość") { c -> cycle(c, K_DUCK, ducks) },
        ModuleAction("Lektor: wyczyść pamięć podręczną", SettingsLevel.ADVANCED, "Usuwa zsyntezowane kwestie (limit 200 MB)") { c -> cache(c).clear(); com.nuvio.tv.ainarrator.GeminiLektor(c).clearCache(); com.nuvio.tv.ainarrator.VertexGeminiLektor(c).clearCache() },
    ) + com.nuvio.tv.ainarrator.LektorVoiceChoice.actions() // scalenie 19.11: głos Gemini
}
