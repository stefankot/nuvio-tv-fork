package com.nuvio.tv.fork.tv.guide

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.nuvio.tv.fork.logged

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

    fun parse(text: String): List<EpgSource> = runCatching { json.decodeFromString<EpgSourceList>(text).sources }.logged("ForkTv", "EpgSources.kt:22").getOrDefault(emptyList())

    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")

    /**
     * Najlepsze źródło dla kanałów (tvg-id, grupa), gdy dopasowuje się co najmniej [minShare] z nich.
     *
     * JMGO 22.11 (252 827 kanałów z trzech list): wcześniej każde sprawdzenie grupy tworzyło od nowa wyrażenie
     * z klasami Unicode (kanał × podpowiedź, ok. 1,3 mln kompilacji ICU na każde przeładowanie listy) — wątek w tle
     * liczył po kilkanaście minut, a pamięć natywna wymuszała odśmiecanie co ~2,5 s. Teraz wyrażenie jest jedno,
     * a słowa grupy liczone raz na grupę (grup jest kilkaset, kanałów setki tysięcy). Wynik bez zmian.
     */
    fun detect(channels: List<Pair<String?, String>>, sources: List<EpgSource>, minShare: Double = 0.3): EpgSource? {
        if (channels.isEmpty() || sources.isEmpty()) return null
        val suffixes = sources.map { s -> s.idSuffixes.map { it.lowercase() }.filter { it.isNotEmpty() } }
        val hints = sources.map { s -> s.groupHints.mapTo(HashSet()) { it.lowercase() } }
        val counts = IntArray(sources.size)
        val words = HashMap<String, Set<String>>()
        for ((tvgId, group) in channels) {
            val id = tvgId?.trim()?.lowercase().orEmpty()
            val groupWords = if (group.isEmpty()) emptySet() else words.getOrPut(group) { group.lowercase().split(NON_WORD).toHashSet() }
            for (i in sources.indices) {
                val byId = id.isNotEmpty() && suffixes[i].any { id.endsWith(it) }
                if (byId || (groupWords.isNotEmpty() && hints[i].any { it in groupWords })) counts[i]++
            }
        }
        var best = -1
        for (i in sources.indices) {
            if (counts[i].toDouble() / channels.size < minShare) continue
            if (best < 0 || counts[i] > counts[best]) best = i
        }
        return if (best >= 0) sources[best] else null
    }

    /**
     * Przewodniki uzupełniające dla każdej listy osobno ([lists] = kanały jednej listy, w kolejności list), bez powtórzeń.
     * JMGO 22.11: rozpoznawanie na wszystkich listach naraz gubiło przewodnik polskiej listy hls.gd (326 kanałów), gdy
     * doszły listy FlixNest (252 tys. pozycji, głównie zagraniczne) — udział polskich spadał poniżej [minShare].
     */
    fun detectPerList(lists: Collection<List<Pair<String?, String>>>, sources: List<EpgSource>, minShare: Double = 0.3): List<EpgSource> =
        lists.mapNotNull { detect(it, sources, minShare) }.distinct()
}
