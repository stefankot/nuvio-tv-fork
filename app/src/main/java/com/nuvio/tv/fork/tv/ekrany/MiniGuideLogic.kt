package com.nuvio.tv.fork.tv.ekrany

import com.nuvio.tv.fork.tv.GuideProgramme

enum class MiniSlot { GUIDE, TILE, BAR, INFO, RESTART, REMIND, FAV, NEXT }

object MiniGuideLogic {
    /** Przyciski karty MiniGuide w kolejności ◀▶. */
    fun buttons(ahead: Int, startOver: Boolean, hasNext: Boolean): List<MiniSlot> = buildList {
        add(MiniSlot.INFO)
        if (ahead == 0 && startOver) add(MiniSlot.RESTART)
        if (ahead > 0) add(MiniSlot.REMIND)
        add(MiniSlot.FAV)
        if (hasNext) add(MiniSlot.NEXT)
    }

    fun upcoming(schedule: List<GuideProgramme>, onNow: GuideProgramme?, nowMs: Long): List<GuideProgramme> {
        val from = onNow?.stopMs ?: nowMs
        return schedule.filter { it.startMs >= from }.sortedBy { it.startMs }.distinctBy { it.startMs }.take(40)
    }

    fun left(stopMs: Long, nowMs: Long) = "zostało " + ((stopMs - nowMs + 59_999) / 60_000).coerceAtLeast(0) + " min"

    fun progress(startMs: Long, stopMs: Long, nowMs: Long): Float =
        if (stopMs <= startMs) 0f else ((nowMs - startMs).toFloat() / (stopMs - startMs)).coerceIn(0f, 1f)

    fun time(ms: Long, zone: java.util.TimeZone = java.util.TimeZone.getDefault()): String {
        val c = java.util.Calendar.getInstance(zone).apply { timeInMillis = ms }
        return "%02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
    }

    /** Przypomnienie do ogłoszenia teraz: startuje w granicach minuty, jeszcze nieogłoszone. */
    fun dueReminder(list: List<com.nuvio.tv.fork.tv.Reminder>, announced: Set<String>, nowMs: Long) =
        list.firstOrNull { it.key !in announced && it.startMs in (nowMs - 60_000)..(nowMs + 60_000) }
}
