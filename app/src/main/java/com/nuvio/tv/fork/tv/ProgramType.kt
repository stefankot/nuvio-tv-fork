package com.nuvio.tv.fork.tv

enum class ProgramType { SERIES, FILM, BAND, ONE_OFF }

/** Typ programu (B02-5): serial, film, pasmo albo jednorazowy. Pasmo nie ma licznika nieobejrzanych (DECYZJE #3). */
object ProgramTypes {
    private const val DAY = 24 * 3_600_000L

    /** [airings]: wszystkie znane emisje tego programu (z dziennika odcinków). [tmdbMovie]: TMDB dopasował film. */
    fun classify(airings: List<LogEntry>, nowMs: Long, tmdbMovie: Boolean = false, tmdbTv: Boolean = false): ProgramType {
        if (airings.isEmpty()) return ProgramType.ONE_OFF
        val ids = airings.map { it.episodeId }.filter { it.isNotEmpty() && !it.startsWith("d:") }.toSet()
        val markers = airings.count { it.episodeId.isNotEmpty() && !it.episodeId.startsWith("d:") }
        if (markers > 0 && (ids.size >= 2 || tmdbTv)) return ProgramType.SERIES
        if (tmdbMovie) return ProgramType.FILM
        val longest = airings.maxOf { it.durMs }
        val week = airings.filter { it.startMs in (nowMs - 7 * DAY)..(nowMs + 8 * DAY) }
        val distinctDescriptions = airings.map { it.episodeId }.filter { it.startsWith("d:") }.toSet()
        if (markers == 0 && week.size >= 4 && distinctDescriptions.size <= 1) return ProgramType.BAND
        if (longest > 75 * 60_000L && ids.isEmpty()) return ProgramType.FILM
        if (airings.map { it.episodeId }.filter { it.startsWith("d:") }.toSet().size >= 2) return ProgramType.SERIES
        return ProgramType.ONE_OFF
    }

    fun hasUnwatchedCounter(type: ProgramType) = type == ProgramType.SERIES
}
