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
    // Fork 20.11 (JMGO build134, właściciel: „regulacja głośności lektora/tła nie działa”): przyciski przełączały wartość
    // po cichu (stały tytuł, bez komunikatu), a w odtwarzaczu nie było regulacji. Teraz: komunikat z nową wartością, log,
    // regulacja w panelu „Więcej” odtwarzacza (mikser czyta wartości co bufor — działa od razu), szerszy zakres.
    private val volumes = listOf("70", "50", "30", "100", "130")
    private val ducks = listOf("-6", "-3", "0", "-10", "-14")

    fun volumeLabel(c: android.content.Context) = com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, K_VOL, volumes[0]) + "%"
    fun duckLabel(c: android.content.Context): String {
        val v = com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, K_DUCK, ducks[0])
        return if (v.toFloatOrNull() == 0f) c.getString(com.nuvio.tv.R.string.lektor_duck_none) else "−" + v.removePrefix("-") + " dB"
    }

    /** Wczytuje głośność lektora i siłę ściszania do silnika (przy włączeniu lektora i przy każdej zmianie). */
    fun applySettings(c: android.content.Context) {
        LektorEngine.voiceGain = (com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, K_VOL, volumes[0]).toIntOrNull() ?: 70) / 100f
        val duck = com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, K_DUCK, ducks[0]).toFloatOrNull() ?: -6f
        LektorEngine.setDuckDepth(duck)
        android.util.Log.i("ForkLektor", "ustawienia: głos=${(LektorEngine.voiceGain * 100).toInt()}%, ściszanie filmu=$duck dB (lektor ${if (LektorEngine.enabled) "gra" else "wyłączony"})")
        watch(c)
    }

    // Fork 20.11: zmiana zapisana inną drogą (serwer ustawień, inny ekran) też trafia do grającego miksera, nie dopiero po ponownym włączeniu.
    private var listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener? = null
    @Synchronized private fun watch(c: android.content.Context) {
        if (listener != null) return
        val app = c.applicationContext
        val l = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key == K_VOL || key == K_DUCK) applySettings(app) }
        listener = l // silna referencja: SharedPreferences trzyma słuchaczy słabo
        app.getSharedPreferences("fork_features", android.content.Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(l)
    }

    private fun cycle(c: android.content.Context, key: String, values: List<String>) {
        val cur = com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, key, values[0])
        com.nuvio.tv.ui.screens.settings.ForkFeatures.setStr(c, key, values[(values.indexOf(cur) + 1) % values.size]); applySettings(c)
    }

    /** Następna głośność lektora; zwraca komunikat z nową wartością. */
    fun cycleVolume(c: android.content.Context): String { cycle(c, K_VOL, volumes); return c.getString(com.nuvio.tv.R.string.lektor_volume_now, volumeLabel(c)) }
    /** Następne ściszanie filmu pod lektorem; zwraca komunikat z nową wartością. */
    fun cycleDuck(c: android.content.Context): String { cycle(c, K_DUCK, ducks); return c.getString(com.nuvio.tv.R.string.lektor_duck_now, duckLabel(c)) }

    override fun actions() = listOf(
        ModuleAction("Lektor: głośność względem filmu (70% / 50% / 30% / 100% / 130%)", SettingsLevel.ESSENTIAL, "Kolejne naciśnięcia przełączają wartość; też w odtwarzaczu: Więcej") { c -> LektorController.say(c, cycleVolume(c)) },
        ModuleAction("Lektor: ściszanie filmu pod lektorem (−6 / −3 / bez / −10 / −14 dB)", SettingsLevel.ESSENTIAL, "Kolejne naciśnięcia przełączają wartość; też w odtwarzaczu: Więcej") { c -> LektorController.say(c, cycleDuck(c)) },
        ModuleAction("Lektor: wyczyść pamięć podręczną", SettingsLevel.ADVANCED, "Usuwa zsyntezowane kwestie (limit 200 MB)") { c -> cache(c).clear(); com.nuvio.tv.ainarrator.GeminiLektor(c).clearCache(); com.nuvio.tv.ainarrator.VertexGeminiLektor(c).clearCache() },
    ) + com.nuvio.tv.ainarrator.LektorVoiceChoice.actions() // scalenie 19.11: głos Gemini
}
