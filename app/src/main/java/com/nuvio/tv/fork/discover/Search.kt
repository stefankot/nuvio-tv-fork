package com.nuvio.tv.fork.discover

import com.nuvio.tv.fork.voice.Text

data class SearchChannel(val key: String, val name: String, val group: String = "")
data class SearchAiring(val programKey: String, val title: String, val channelKey: String, val startMs: Long, val stopMs: Long)

data class SearchResults(
    val channels: List<SearchChannel>,
    /** Teraz i wkrótce: jedna pozycja na program (najbliższa emisja). */
    val nowAndSoon: List<SearchAiring>,
    /** W archiwum: emisje jednego programu scalone w jedną pozycję (najnowsza). */
    val archive: List<SearchAiring>,
)

/** Indeks wyszukiwania: dopasowanie bez diakrytyków, od 2 liter, po początku słów. */
class TvSearchIndex(private val channels: List<SearchChannel>, private val airings: List<SearchAiring>) {
    private val chKeys = channels.map { Text.norm(it.name).split(' ') }
    private val airKeys = airings.map { Text.norm(it.title).split(' ') }

    private fun matches(words: List<String>, q: List<String>): Boolean = q.all { qt -> words.any { it.startsWith(qt) } }

    fun search(query: String, nowMs: Long, soonMs: Long = 3 * 3_600_000L, limit: Int = 30): SearchResults {
        val q = Text.norm(query).split(' ').filter { it.isNotEmpty() }
        if (q.isEmpty() || q.sumOf { it.length } < 2) return SearchResults(emptyList(), emptyList(), emptyList())
        val ch = channels.filterIndexed { i, _ -> matches(chKeys[i], q) }.take(limit)
        val hits = airings.filterIndexed { i, _ -> matches(airKeys[i], q) }
        val now = hits.filter { it.stopMs > nowMs && it.startMs <= nowMs + soonMs }.sortedBy { it.startMs }.distinctBy { it.programKey }.take(limit)
        val archive = hits.filter { it.stopMs <= nowMs }.sortedByDescending { it.startMs }.distinctBy { it.programKey }.take(limit)
        return SearchResults(ch, now, archive)
    }
}

/** Ostatnie wyszukiwania (najnowsze pierwsze, bez duplikatów, do 8). */
object RecentSearches {
    fun add(list: List<String>, q: String, max: Int = 8): List<String> {
        val t = q.trim(); if (t.length < 2) return list
        return (listOf(t) + list.filterNot { it.equals(t, ignoreCase = true) }).take(max)
    }
}
