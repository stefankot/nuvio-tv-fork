package com.nuvio.tv.fork.tv

import kotlinx.serialization.Serializable

@Serializable
enum class WatchState { IN_PROGRESS, WATCHED, UNWATCHED }

/** Progi „obejrzane” (DECYZJE #1). */
data class WatchRules(
    /** Pozycja musi dojść do tego ułamka emisji (domyślnie ostatnie 10% = 0,9). */
    val reachFraction: Double = 0.9,
    /** Co najmniej tyle czasu emisji faktycznie obejrzano (zbiór minut). */
    val watchedFraction: Double = 0.5,
    /** „W trakcie” po tylu minutach oglądania. */
    val inProgressMinutes: Int = 2,
)

/** Stan jednego odcinka (program + odcinek; bez identyfikatora: kanał + start). */
@Serializable
data class WatchRecord(
    val key: String,
    val programKey: String,
    val episodeId: String,
    val guideKey: String,
    val startMs: Long,
    val durMs: Long,
    val positionMs: Long = 0L,
    /** Indeksy minut emisji, które faktycznie obejrzano. */
    val minutes: Set<Int> = emptySet(),
    val lastWatchedMs: Long = 0L,
    val state: WatchState = WatchState.UNWATCHED,
    val manual: Boolean = false,
)

object WatchLog {
    fun keyFor(programKey: String, episodeId: String, guideKey: String, startMs: Long) =
        if (episodeId.isNotEmpty()) "$programKey|$episodeId" else "$guideKey|$startMs"

    private fun totalMinutes(durMs: Long) = maxOf(1, (durMs / 60_000L).toInt())

    /**
     * Tyknięcie odtwarzania: obraz jest w [playheadMs] czasu kanału. Liczy minutę emisji tylko, gdy pozycja leży w emisji
     * (przewinięcie na koniec nie zalicza reszty).
     */
    fun tick(rec: WatchRecord, playheadMs: Long, nowMs: Long, rules: WatchRules = WatchRules()): WatchRecord {
        val offset = playheadMs - rec.startMs
        if (offset < 0 || offset > rec.durMs) return rec
        if (rec.manual && rec.state != WatchState.IN_PROGRESS) return rec
        val minute = minOf((offset / 60_000L).toInt(), totalMinutes(rec.durMs) - 1)
        val minutes = rec.minutes + minute
        val pos = maxOf(rec.positionMs, offset)
        val total = totalMinutes(rec.durMs)
        val reached = pos >= rec.durMs * rules.reachFraction
        val watchedEnough = minutes.size.toDouble() / total >= rules.watchedFraction
        val state = when {
            rec.state == WatchState.WATCHED -> WatchState.WATCHED
            reached && watchedEnough -> WatchState.WATCHED
            minutes.size >= rules.inProgressMinutes -> WatchState.IN_PROGRESS
            else -> rec.state
        }
        return rec.copy(positionMs = pos, minutes = minutes, lastWatchedMs = nowMs, state = state)
    }

    fun markWatched(rec: WatchRecord, nowMs: Long) =
        rec.copy(state = WatchState.WATCHED, manual = true, lastWatchedMs = nowMs, positionMs = rec.durMs)

    fun markUnwatched(rec: WatchRecord) =
        rec.copy(state = WatchState.UNWATCHED, manual = true, minutes = emptySet(), positionMs = 0L)

    /** „Wszystkie wcześniejsze” odcinki tego programu (według czasu emisji) jako obejrzane. */
    fun markAllEarlier(all: List<WatchRecord>, ref: WatchRecord, nowMs: Long): List<WatchRecord> =
        all.map { if (it.programKey == ref.programKey && it.startMs < ref.startMs) markWatched(it, nowMs) else it }

    /** Dokąd wznowić: archiwum od zapisanej pozycji albo najbliższa powtórka z dziennika odcinków. */
    sealed interface Resume {
        data class Archive(val guideKey: String, val startMs: Long, val positionMs: Long) : Resume
        data class Repeat(val entry: LogEntry) : Resume
        object None : Resume
    }

    fun resume(rec: WatchRecord, log: List<LogEntry>, catchupDays: Map<String, Int>, nowMs: Long): Resume {
        val days = catchupDays[rec.guideKey] ?: 0
        if (days > 0 && rec.startMs >= nowMs - days * 24L * 3_600_000L && rec.startMs + rec.durMs <= nowMs)
            return Resume.Archive(rec.guideKey, rec.startMs, rec.positionMs)
        val repeat = log.filter { it.programKey == rec.programKey && it.episodeId == rec.episodeId && rec.episodeId.isNotEmpty() && it.startMs != rec.startMs }
            .minByOrNull { if (it.startMs >= nowMs) it.startMs - nowMs else Long.MAX_VALUE / 2 + (nowMs - it.startMs) }
        return repeat?.let { Resume.Repeat(it) } ?: Resume.None
    }
}
