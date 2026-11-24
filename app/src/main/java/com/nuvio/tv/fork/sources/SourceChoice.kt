package com.nuvio.tv.fork.sources

import android.content.Context
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.PlayerSettingsDataStore
import com.nuvio.tv.data.local.StreamAutoPlayMode
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import com.nuvio.tv.fork.logged

/** Odpowiedź na jedno pytanie „Co ważniejsze: polski dźwięk czy jakość obrazu?” zapisuje ustawienia przez istniejące DataStore. */
object SourceChoice {
    @EntryPoint @InstallIn(SingletonComponent::class)
    interface Deps { fun player(): PlayerSettingsDataStore; fun profiles(): ProfileManager }

    private const val ASSET = "fork/source_presets.json"

    fun presets(c: Context): List<SourcePreset> =
        runCatching { SourcePresets.parse(c.assets.open(ASSET).bufferedReader().use { it.readText() }) }.logged("ForkSources", "SourceChoice.kt:22").getOrDefault(emptyList())

    private fun stateKey() = "sources_answer"

    /** Zapisana odpowiedź (id zestawu albo [SourcePresets.CUSTOM]); pusta = jeszcze nie odpowiedziano. */
    fun answer(c: Context): String = ForkFeatures.str(c, stateKey())

    /** Zapisuje reguły rankingu, tryb Regex oraz języki dźwięku i napisów wybranego zestawu. */
    suspend fun apply(c: Context, presetId: String) {
        val preset = presets(c).firstOrNull { it.id == presetId } ?: return
        val store = EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).player()
        useRules(c, preset)
        preset.audio.getOrNull(0)?.let { store.setPreferredAudioLanguage(langCode(it)) }
        store.setSecondaryPreferredAudioLanguage(preset.audio.getOrNull(1)?.let(::langCode))
        preset.subs.getOrNull(0)?.let { store.setSubtitlePreferredLanguage(langCode(it)) }
        store.setSubtitleSecondaryLanguage(preset.subs.getOrNull(1)?.let(::langCode))
        // B04-5: pamięć serialu - istniejąca ciągłość grupy wydania i ostatniego źródła
        store.setStreamAutoPlayReuseBingeGroup(true)
        store.setStreamAutoPlayPreferBingeGroupForNextEpisode(true)
        ForkFeatures.setStr(c, stateKey(), presetId)
        KeRanking.warm(c)
    }

    /**
     * Właściciel 22.11: automatyczny wybór źródła tylko natywny. Reguły zestawu trafiają do pola „Regex” Nuvio (lista
     * priorytetów) i włączają tryb Regex; ranking forka (KeRanking) nie bierze udziału w wyborze.
     */
    suspend fun useRules(c: Context, preset: SourcePreset) {
        val store = EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).player()
        store.setStreamAutoPlayRankedRegex(preset.rules)
        store.setStreamAutoPlayMode(StreamAutoPlayMode.REGEX_MATCH)
        ForkFeatures.setStr(c, stateKey(), preset.id)
        android.util.Log.i("ForkSources", "autoplay: natywny Regex, zestaw ${preset.id} (${preset.rules.size} reguł)")
    }

    /** B04-9: napisy dla seniorów: rozmiar, półprzezroczyste tło, obrys. */
    /**
     * UX JMGO (ręczne testy 24.11): bieżący rozmiar napisów z ustawień odtwarzacza (drzewo ustawień pokazywało „—”, dopóki
     * rozmiaru nie wybrano w nim samym). Domyślne 100 % = „Średnie”.
     */
    suspend fun subtitleSizeChoice(c: Context): SubtitleSizeChoice {
        val store = EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).player()
        val size = store.playerSettings.first().subtitleStyle.size
        return if (size in 95..140) SubtitleSizeChoice.MEDIUM else SubtitleSizeChoice.fromSize(size)
    }

    suspend fun applySubtitleSize(c: Context, choice: SubtitleSizeChoice) {
        val store = EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).player()
        store.setSubtitleSize(choice.sizePercent)
        store.setSubtitleBackgroundColor(SubtitleSizeChoice.BACKGROUND_ARGB)
        store.setSubtitleOutlineEnabled(true)
    }

    /** Porównuje zapisane reguły z zestawami: ręczna zmiana w Advanced daje „Własne”. */
    suspend fun refresh(c: Context): String {
        val store = EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).player()
        val stored = store.playerSettings.first().streamAutoPlayRankedRegex
        val match = SourcePresets.matching(stored, presets(c))
        if (match != null && answer(c).isNotEmpty()) ForkFeatures.setStr(c, stateKey(), match)
        return match.orEmpty()
    }

    fun title(c: Context, id: String): String = if (id == SourcePresets.CUSTOM) "Własne" else presets(c).firstOrNull { it.id == id }?.title ?: "—"

    private fun langCode(iso3: String) = when (iso3) { "pol" -> "pl"; "eng" -> "en"; else -> iso3 }
}
