package com.nuvio.tv.fork.tv.ekrany

import com.nuvio.tv.fork.tv.GuideProgramme
import com.nuvio.tv.fork.tv.LogEntry
import com.nuvio.tv.fork.tv.WatchRecord
import com.nuvio.tv.fork.tv.WatchState

/** Inne emisje tego samego tytułu na kanale i to, co idzie dalej. */
data class Airings(val others: List<GuideProgramme>, val next: List<GuideProgramme>)

fun airingsOf(schedule: List<GuideProgramme>, programme: GuideProgramme, nowMs: Long, windowMs: Long = 7L * 86_400_000): Airings {
    val title = programme.title.trim().lowercase()
    val inWindow = schedule.filter { it.stopMs > nowMs - windowMs && it.startMs < nowMs + windowMs }
    val others = inWindow.filter { it.title.trim().lowercase() == title && it.startMs != programme.startMs }.sortedBy { it.startMs }
    val next = inWindow.filter { it.startMs >= programme.stopMs }.sortedBy { it.startMs }.take(12)
    return Airings(others, next)
}

/** Etykieta odcinka: „Sezon 3 · Odcinek 168”, „Odcinek 6009”; rozumie S3E168, E6009, 3x168 i xmltv_ns „2.167.0/1”. */
fun episodeLabel(raw: String?): String? {
    val t = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    Regex("(?i)^s\\s*(\\d+)\\s*e\\s*(\\d+)").find(t)?.let { return "Sezon ${it.groupValues[1].toInt()} · Odcinek ${it.groupValues[2].toInt()}" }
    Regex("(?i)^(\\d+)\\s*x\\s*(\\d+)").find(t)?.let { return "Sezon ${it.groupValues[1].toInt()} · Odcinek ${it.groupValues[2].toInt()}" }
    Regex("(?i)^e\\s*(\\d+)").find(t)?.let { return "Odcinek ${it.groupValues[1].toInt()}" }
    Regex("^(\\d*)\\s*\\.\\s*(\\d*)").find(t)?.let { m ->
        val season = m.groupValues[1].toIntOrNull()?.plus(1); val ep = m.groupValues[2].toIntOrNull()?.plus(1)
        return listOfNotNull(season?.let { "Sezon $it" }, ep?.let { "Odcinek $it" }).joinToString(" · ").takeIf { it.isNotEmpty() }
    }
    return t.takeIf { it.all { c -> c.isDigit() } }?.let { "Odcinek ${it.toInt()}" }
}

/** (sezon, odcinek) z etykiety odcinka; sezon null, gdy go nie ma. */
fun seasonEpisode(raw: String?): Pair<Int?, Int>? {
    val t = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    Regex("(?i)^s\\s*(\\d+)\\s*e\\s*(\\d+)").find(t)?.let { return it.groupValues[1].toInt() to it.groupValues[2].toInt() }
    Regex("(?i)^(\\d+)\\s*x\\s*(\\d+)").find(t)?.let { return it.groupValues[1].toInt() to it.groupValues[2].toInt() }
    Regex("(?i)^e\\s*(\\d+)").find(t)?.let { return null to it.groupValues[1].toInt() }
    Regex("^(\\d+)\\s*\\.\\s*(\\d+)").find(t)?.let { return (it.groupValues[1].toInt() + 1) to (it.groupValues[2].toInt() + 1) }
    return t.takeIf { it.all(Char::isDigit) }?.let { null to it.toInt() }
}

/** Odcinki pogrupowane w sezony (zakładki). Gdy nie ma numerów sezonów: jedna grupa „null” posortowana według daty. */
fun groupBySeason(entries: List<LogEntry>): Map<Int?, List<LogEntry>> {
    val byEpisode = entries.groupBy { it.episodeId }.mapValues { (_, v) -> v.minBy { it.startMs } }.values
    val parsed = byEpisode.map { it to seasonEpisode(it.episodeId) }
    val anySeason = parsed.any { it.second?.first != null }
    return if (!anySeason) mapOf(null to byEpisode.sortedBy { it.startMs })
    else parsed.groupBy({ it.second?.first }, { it }).toSortedMap(compareBy { it ?: Int.MAX_VALUE })
        .mapValues { (_, v) -> v.sortedWith(compareBy({ it.second?.second ?: Int.MAX_VALUE }, { it.first.startMs })).map { it.first } }
}

/** „dziś”, „jutro”, „wczoraj” albo „pon. 10.11”. */
fun dayLabel(ms: Long, nowMs: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String {
    val d = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    val today = java.time.Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    return when (d) {
        today -> "dziś"
        today.plusDays(1) -> "jutro"
        today.minusDays(1) -> "wczoraj"
        else -> listOf("pon.", "wt.", "śr.", "czw.", "pt.", "sob.", "niedz.")[d.dayOfWeek.value - 1] + " %d.%02d".format(d.dayOfMonth, d.monthValue)
    }
}

enum class Availability { LIVE, IN_ARCHIVE, FUTURE, GONE }

/** Dostępność wiersza odcinka: na żywo / w archiwum do dnia / w przyszłości. */
fun availability(startMs: Long, stopMs: Long, nowMs: Long, catchupDays: Int): Availability = when {
    startMs <= nowMs && nowMs < stopMs -> Availability.LIVE
    startMs > nowMs -> Availability.FUTURE
    catchupDays > 0 && startMs >= nowMs - catchupDays * 86_400_000L -> Availability.IN_ARCHIVE
    else -> Availability.GONE
}

/** Wiersz odcinka na stronie programu. */
data class EpisodeRow(val entry: LogEntry, val label: String?, val state: WatchState, val progress: Float, val availability: Availability)

fun episodeRows(entries: List<LogEntry>, records: Map<String, WatchRecord>, catchupDays: Map<String, Int>, nowMs: Long): List<EpisodeRow> =
    entries.map { e ->
        val rec = records.values.firstOrNull { it.programKey == e.programKey && it.episodeId == e.episodeId && e.episodeId.isNotEmpty() }
        EpisodeRow(e, episodeLabel(e.episodeId), rec?.state ?: WatchState.UNWATCHED,
            rec?.let { (it.positionMs.toFloat() / it.durMs.coerceAtLeast(1)).coerceIn(0f, 1f) } ?: 0f,
            availability(e.startMs, e.startMs + e.durMs, nowMs, catchupDays[e.guideKey] ?: 0))
    }
