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
        store.setStreamAutoPlayRankedRegex(preset.rules)
        // Hybryda 18.11: tryb „pierwsze źródło” = ranking KE + polski dźwięk z tych reguł (KeRanking); Regex omijałby ranking.
        store.setStreamAutoPlayMode(StreamAutoPlayMode.FIRST_STREAM)
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

    /** Tryb Regex zapisany przez starą wersję odpowiedzi: przestaw na „pierwsze źródło”, gdy reguły to wciąż zestaw odpowiedzi. */
    suspend fun useRanking(c: Context) {
        val store = EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java).player()
        val s = store.playerSettings.first()
        if (s.streamAutoPlayMode == StreamAutoPlayMode.REGEX_MATCH && SourcePresets.matching(s.streamAutoPlayRankedRegex, presets(c)) != SourcePresets.CUSTOM)
            store.setStreamAutoPlayMode(StreamAutoPlayMode.FIRST_STREAM)
    }

    /** B04-9: napisy dla seniorów: rozmiar, półprzezroczyste tło, obrys. */
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
