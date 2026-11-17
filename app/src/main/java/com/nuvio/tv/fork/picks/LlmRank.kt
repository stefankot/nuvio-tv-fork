package com.nuvio.tv.fork.picks

import kotlinx.coroutines.withTimeoutOrNull

/** Klient modelu do rankingu: dostaje kandydatów i opis gustu, zwraca kolejność z jednym zdaniem „dlaczego dla Ciebie”. */
interface RankLlm {
    suspend fun rank(mood: String, tasteDescription: String, candidates: List<Pair<Int, String>>): List<Pair<Int, String>>
}

data class RankedPick(val title: PickTitle, val why: String?)

/** Ranking przez model (B10-8): 60 najlepszych z punktacji, każdy zwrócony id musi być z listy; błąd lub brak klucza = reguły. */
class LlmRanker(private val llm: RankLlm?, private val clock: () -> Long = System::currentTimeMillis) {
    private val cache = HashMap<String, Pair<Long, List<RankedPick>>>()

    fun describe(profile: TasteProfile, liked: List<PickTitle>, abandoned: List<PickTitle>): String =
        "gatunki: " + profile.genreWeights.entries.sortedByDescending { it.value }.take(6).joinToString { "${it.key}=%.1f".format(it.value) } +
            "; suwaki: nieodkryte=${profile.sliders.undiscovered}, wymagające=${profile.sliders.demanding}, klasyka=${profile.sliders.classic}" +
            "; lubiane: " + liked.take(15).joinToString { it.title } + "; porzucone: " + abandoned.take(5).joinToString { it.title }

    suspend fun rank(profileKey: String, mood: Mood, ruleRanked: List<PickTitle>, taste: String, size: Int = 20): List<RankedPick> {
        val fallback = ruleRanked.take(size).map { RankedPick(it, null) }
        val client = llm ?: return fallback
        val key = "$profileKey|${mood.id}"
        cache[key]?.takeIf { clock() - it.first < 24 * 3_600_000L }?.let { return it.second }
        val pool = ruleRanked.take(60)
        val reply = try { withTimeoutOrNull(8_000L) { client.rank(mood.title, taste, pool.map { it.tmdbId to it.title }) } } catch (_: Exception) { null } ?: return fallback
        val byId = pool.associateBy { it.tmdbId }
        val picks = reply.mapNotNull { (id, why) -> byId[id]?.let { RankedPick(it, why.take(160)) } }.distinctBy { it.title.tmdbId }.take(size)
        if (picks.isEmpty()) return fallback
        cache[key] = clock() to picks
        return picks
    }
}
