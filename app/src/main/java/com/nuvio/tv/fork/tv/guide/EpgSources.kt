package com.nuvio.tv.fork.tv.guide

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EpgSource(
    val country: String,
    val epg: String,
    val logo: String = "",
    val idSuffixes: List<String> = emptyList(),
    val groupHints: List<String> = emptyList(),
)

@Serializable
data class EpgSourceList(val sources: List<EpgSource> = emptyList())

/** Rozpoznawanie kraju listy kanałów i dobór przewodnika uzupełniającego. Adresy są w JSON, nie w kodzie. */
object EpgSources {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<EpgSource> = runCatching { json.decodeFromString<EpgSourceList>(text).sources }.getOrDefault(emptyList())

    /** Najlepsze źródło dla kanałów (tvg-id, grupa), gdy dopasowuje się co najmniej [minShare] z nich. */
    fun detect(channels: List<Pair<String?, String>>, sources: List<EpgSource>, minShare: Double = 0.3): EpgSource? {
        if (channels.isEmpty()) return null
        return sources.map { s -> s to channels.count { (id, group) -> matches(s, id, group) } }
            .filter { (_, n) -> n.toDouble() / channels.size >= minShare }
            .maxByOrNull { it.second }?.first
    }

    private fun matches(s: EpgSource, tvgId: String?, group: String): Boolean {
        val id = tvgId?.trim()?.lowercase().orEmpty()
        if (id.isNotEmpty() && s.idSuffixes.any { id.endsWith(it.lowercase()) }) return true
        val g = group.lowercase()
        return g.isNotEmpty() && s.groupHints.any { h -> g.split(Regex("[^\\p{L}\\p{N}]+")).any { it == h.lowercase() } }
    }
}
