package com.nuvio.tv.fork.tv

import kotlinx.serialization.Serializable
import java.util.Calendar

/** Obserwowany program (dzwonek). [auto] = dodany automatycznie; cofnięty automat nie wraca ([dismissed]). */
@Serializable
data class Followed(val programKey: String, val title: String, val auto: Boolean, val addedMs: Long, val dismissed: Boolean = false)

/** Sesja oglądania kanału (statystyka „twoje kanały”). */
@Serializable
data class ViewSession(val guideKey: String, val startMs: Long, val minutes: Int)

enum class DayPart { NIGHT, MORNING, AFTERNOON, EVENING;
    companion object {
        fun of(epochMs: Long, zone: java.util.TimeZone = java.util.TimeZone.getDefault()): DayPart {
            val h = Calendar.getInstance(zone).apply { timeInMillis = epochMs }.get(Calendar.HOUR_OF_DAY)
            return when { h < 6 -> NIGHT; h < 12 -> MORNING; h < 18 -> AFTERNOON; else -> EVENING }
        }
    }
}

object Following {
    const val AUTO_EPISODES = 2

    fun follow(list: List<Followed>, programKey: String, title: String, nowMs: Long): List<Followed> =
        list.filterNot { it.programKey == programKey } + Followed(programKey, title, auto = false, addedMs = nowMs)

    /** Cofnięcie: ręcznie dodany znika; automatyczny zostaje jako [Followed.dismissed], żeby nie wrócił. */
    fun unfollow(list: List<Followed>, programKey: String): List<Followed> =
        list.mapNotNull { f ->
            when {
                f.programKey != programKey -> f
                f.auto -> f.copy(dismissed = true)
                else -> null
            }
        }

    fun active(list: List<Followed>) = list.filterNot { it.dismissed }

    /** Po 2 obejrzanych odcinkach serialu: obserwuj automatycznie (jeśli włączone i nie cofnięte). */
    fun autoFollow(list: List<Followed>, records: List<WatchRecord>, titles: Map<String, String>, series: Set<String>, nowMs: Long, enabled: Boolean): List<Followed> {
        if (!enabled) return list
        val known = list.map { it.programKey }.toSet()
        val add = records.filter { it.state == WatchState.WATCHED && it.programKey in series && it.programKey !in known }
            .groupBy { it.programKey }.filter { (_, v) -> v.map { it.episodeId.ifEmpty { it.key } }.toSet().size >= AUTO_EPISODES }
            .map { (k, _) -> Followed(k, titles[k] ?: k, auto = true, addedMs = nowMs) }
        return list + add
    }
}

object ChannelStats {
    private const val DAY = 24 * 3_600_000L
    const val YOUR_MIN_SESSIONS = 3

    /** Waga: pełna do 14 dni, połowa po 14 dniach, nic po 60. */
    fun weight(ageMs: Long) = when { ageMs < 0 -> 1.0; ageMs <= 14 * DAY -> 1.0; ageMs <= 60 * DAY -> 0.5; else -> 0.0 }

    fun prune(sessions: List<ViewSession>, nowMs: Long) = sessions.filter { nowMs - it.startMs <= 60 * DAY }

    /** „Twoje kanały”: co najmniej 3 oglądania w 14 dni, uszeregowane wagą minut. */
    fun yourChannels(sessions: List<ViewSession>, nowMs: Long): List<String> {
        val recent = sessions.filter { nowMs - it.startMs in 0..14 * DAY }.groupBy { it.guideKey }.filter { it.value.size >= YOUR_MIN_SESSIONS }
        return recent.keys.sortedByDescending { k -> sessions.filter { it.guideKey == k }.sumOf { it.minutes * weight(nowMs - it.startMs) } }
    }

    /** Kanały zwykle oglądane o tej porze dnia (waga jak wyżej). */
    fun usualForDayPart(sessions: List<ViewSession>, part: DayPart, nowMs: Long, zone: java.util.TimeZone = java.util.TimeZone.getDefault()): List<String> =
        sessions.filter { DayPart.of(it.startMs, zone) == part }.groupBy { it.guideKey }
            .mapValues { (_, v) -> v.sumOf { it.minutes * weight(nowMs - it.startMs) } }
            .filter { it.value > 0 }.entries.sortedByDescending { it.value }.map { it.key }
}
