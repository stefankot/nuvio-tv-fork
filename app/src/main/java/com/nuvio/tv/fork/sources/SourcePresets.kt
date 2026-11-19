package com.nuvio.tv.fork.sources

import com.nuvio.tv.core.streams.ranking.RankedRegexRules
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.nuvio.tv.fork.logged

/** Zestaw reguł rankingu źródeł dla jednej odpowiedzi na pytanie „polski dźwięk czy jakość?”. */
@Serializable
data class SourcePreset(
    val id: String,
    val title: String,
    val rules: List<String>,
    val audio: List<String> = emptyList(),
    val subs: List<String> = emptyList(),
)

@Serializable
private data class PresetFile(val presets: List<SourcePreset> = emptyList())

object SourcePresets {
    const val PL_ALWAYS = "pl_always"
    const val PL_IF_GOOD = "pl_if_good"
    const val ORIGINAL = "original_subs"
    const val CUSTOM = "custom"

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<SourcePreset> = runCatching { json.decodeFromString<PresetFile>(text).presets }.logged("ForkSources", "SourcePresets.kt:28").getOrDefault(emptyList())

    /** Szczebel (od 0), w którym nazwa źródła ląduje przy regułach zestawu; null = żaden (źródło odpada). */
    fun tierOf(preset: SourcePreset, sourceText: String): Int? {
        for ((i, rule) in preset.rules.withIndex()) {
            val r = RankedRegexRules.create(listOf(rule)) ?: continue
            if (r.rank(listOf(sourceText)) { it }?.isNotEmpty() == true) return i
        }
        return null
    }

    /** Który zestaw odpowiada zapisanym regułom; [CUSTOM], gdy żaden (ręczna zmiana w Advanced). Puste reguły = null (brak odpowiedzi). */
    fun matching(stored: List<String>, presets: List<SourcePreset>): String? =
        if (stored.isEmpty()) null else presets.firstOrNull { it.rules == stored }?.id ?: CUSTOM
}
