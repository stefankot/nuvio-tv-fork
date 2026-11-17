package com.nuvio.tv.fork.discover

import com.nuvio.tv.fork.tv.Followed
import com.nuvio.tv.fork.tv.LogEntry
import com.nuvio.tv.fork.tv.ProgramType
import com.nuvio.tv.fork.tv.ProgramTypes
import com.nuvio.tv.fork.tv.WatchRecord
import com.nuvio.tv.fork.tv.WatchState

data class DiscoverCard(val programKey: String, val title: String, val badge: String? = null, val guideKey: String? = null, val startMs: Long = 0L)
data class DiscoverRow(val id: String, val title: String, val cards: List<DiscoverCard>)

/** Wejście do budowy rzędów Odkrywaj z dziennika odcinków i oglądania (B08-2). */
data class DiscoverInput(
    val log: List<LogEntry>,
    val records: List<WatchRecord>,
    val followed: List<Followed>,
    val yourChannels: Set<String>,
    val catchupDays: Map<String, Int>,
    val bucketOf: (LogEntry) -> String?,
    val hiddenChannels: Set<String> = emptySet(),
    val nowMs: Long,
)

object DiscoverRows {
    const val MAX_CARDS = 30
    private const val DAY = 24 * 3_600_000L

    /** Rzędy w kolejności; puste znikają; pasma nie trafiają do „Seriale”; emisje serialu scalone w jedną kartę. */
    fun build(i: DiscoverInput, order: List<String> = DEFAULT_ORDER, hiddenRows: Set<String> = emptySet()): List<DiscoverRow> {
        val log = i.log.filter { it.guideKey !in i.hiddenChannels }
        val byProgram = log.groupBy { it.programKey }
        val types = byProgram.mapValues { (_, v) -> ProgramTypes.classify(v, i.nowMs) }
        fun inArchive(e: LogEntry) = e.startMs + e.durMs <= i.nowMs && e.startMs >= i.nowMs - (i.catchupDays[e.guideKey] ?: 0) * DAY
        fun card(key: String, badge: String? = null): DiscoverCard {
            val e = byProgram.getValue(key).maxBy { it.startMs }
            return DiscoverCard(key, e.title, badge, e.guideKey, e.startMs)
        }
        val rows = LinkedHashMap<String, DiscoverRow>()
        fun put(id: String, title: String, cards: List<DiscoverCard>) { if (cards.isNotEmpty()) rows[id] = DiscoverRow(id, title, cards.take(MAX_CARDS)) }

        put("continue", "Oglądaj dalej", i.records.filter { it.state == WatchState.IN_PROGRESS && it.programKey in byProgram }
            .sortedByDescending { it.lastWatchedMs }.distinctBy { it.programKey }.map { card(it.programKey, "Wznów") })
        val followedKeys = i.followed.filterNot { it.dismissed }.map { it.programKey }.filter { it in byProgram }
        put("followed", "Obserwowane", followedKeys.map { k ->
            val unwatched = byProgram.getValue(k).filter { inArchive(it) }.map { it.episodeId }.filter { it.isNotEmpty() }.distinct().count { ep ->
                i.records.none { r -> r.programKey == k && r.episodeId == ep && r.state == WatchState.WATCHED }
            }
            card(k, if (types[k] == ProgramType.BAND) null else unwatched.takeIf { it > 0 }?.toString())
        })
        val dayStart = i.nowMs - i.nowMs % DAY
        put("tonight", "Dziś wieczorem", log.filter { it.guideKey in i.yourChannels }.filter { it.startMs in (dayStart + 20 * 3_600_000L)..(dayStart + 23 * 3_600_000L) }
            .sortedBy { it.startMs }.distinctBy { it.programKey }.map { card(it.programKey) })
        put("films", "Filmy w archiwum", byProgram.filter { (k, v) -> types[k] == ProgramType.FILM && v.any(::inArchive) }.keys.map { card(it) })
        put("series", "Seriale w archiwum", byProgram.filter { (k, v) -> types[k] == ProgramType.SERIES && v.any(::inArchive) }.keys.map { card(it) })
        for (b in GenreBuckets.ALL) {
            put("genre_$b", b, byProgram.filter { (_, v) -> v.any { i.bucketOf(it) == b && inArchive(it) } }.keys.map { card(it) })
        }
        return RowPrefs.apply(rows.values.toList(), order, hiddenRows)
    }

    val DEFAULT_ORDER = listOf("continue", "followed", "tonight", "films", "series") + GenreBuckets.ALL.map { "genre_$it" }
}

/** Ustawienia rzędów (B08-5): widoczność i kolejność. */
object RowPrefs {
    fun apply(rows: List<DiscoverRow>, order: List<String>, hidden: Set<String>): List<DiscoverRow> {
        val rank = order.withIndex().associate { it.value to it.index }
        return rows.filter { it.id !in hidden }.sortedBy { rank[it.id] ?: Int.MAX_VALUE }
    }
}

/** Kolejka pobierania okładek w tle (B08-4): najwyżej [LIMIT] zapytań na odświeżenie, tylko poza odtwarzaniem, obserwowane najpierw. */
class CoverFetchQueue(private val limit: Int = LIMIT) {
    private val done = HashSet<String>()
    var requestsThisRefresh = 0
        private set

    fun startRefresh() { requestsThisRefresh = 0 }

    /** Kolejne klucze do pobrania z [candidates] (kolejność = priorytet); pusty, gdy trwa odtwarzanie lub limit wyczerpany. */
    fun next(candidates: List<String>, playing: Boolean, batch: Int = 5): List<String> {
        if (playing || requestsThisRefresh >= limit) return emptyList()
        val take = candidates.filter { it !in done }.take(minOf(batch, limit - requestsThisRefresh))
        done += take; requestsThisRefresh += take.size
        return take
    }

    companion object { const val LIMIT = 40 }
}
