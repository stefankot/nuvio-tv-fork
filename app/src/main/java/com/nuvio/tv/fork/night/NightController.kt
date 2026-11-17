package com.nuvio.tv.fork.night

import java.util.Calendar
import java.util.TimeZone

enum class NightSwitch { OFF, ON, SCHEDULE }

/** Siła trybu nocnego (wspólna dla dźwięku i obrazu na poziomie Prostym). */
enum class Strength { OFF, LIGHT, MEDIUM, STRONG }

data class NightSchedule(val startMinute: Int = 22 * 60, val endMinute: Int = 7 * 60) {
    /** Czy [minuteOfDay] leży w oknie; okno przez północ (start > koniec) obsługiwane. */
    fun contains(minuteOfDay: Int): Boolean =
        if (startMinute == endMinute) false
        else if (startMinute < endMinute) minuteOfDay in startMinute until endMinute
        else minuteOfDay >= startMinute || minuteOfDay < endMinute
}

/** Ręczne przełączenie, które obowiązuje do najbliższej granicy harmonogramu. */
data class ManualOverride(val on: Boolean, val untilMs: Long)

/**
 * Sterownik trybu nocnego: wyłączony / włączony / według godzin. Czysta logika (czas z zewnątrz, bez alarmów systemowych).
 */
class NightController(
    var mode: NightSwitch = NightSwitch.OFF,
    var schedule: NightSchedule = NightSchedule(),
    var strength: Strength = Strength.MEDIUM,
    private val zone: TimeZone = TimeZone.getDefault(),
) {
    var override: ManualOverride? = null
        private set

    private fun minuteOfDay(ms: Long): Int {
        val c = Calendar.getInstance(zone).apply { timeInMillis = ms }
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    private fun scheduleActive(ms: Long) = schedule.contains(minuteOfDay(ms))

    /** Czy tryb obowiązuje teraz (po uwzględnieniu ręcznego przełączenia). */
    fun active(nowMs: Long): Boolean {
        override?.let { if (nowMs < it.untilMs) return it.on else override = null }
        return when (mode) {
            NightSwitch.OFF -> false
            NightSwitch.ON -> true
            NightSwitch.SCHEDULE -> scheduleActive(nowMs)
        }
    }

    /** Obowiązująca siła: [Strength.OFF], gdy tryb nie obowiązuje. */
    fun effective(nowMs: Long): Strength = if (active(nowMs)) strength else Strength.OFF

    /** Ręczne przełączenie na wartość przeciwną do obowiązującej; trwa do najbliższej granicy harmonogramu (lub 24 h bez harmonogramu). */
    fun toggleManually(nowMs: Long) {
        val on = !active(nowMs)
        override = ManualOverride(on, nextBoundary(nowMs))
    }

    /** Najbliższy początek lub koniec okna po [nowMs]. Bez harmonogramu: za 24 godziny. */
    fun nextBoundary(nowMs: Long): Long {
        if (mode != NightSwitch.SCHEDULE || schedule.startMinute == schedule.endMinute) return nowMs + 24 * 3_600_000L
        val now = minuteOfDay(nowMs)
        val candidates = listOf(schedule.startMinute, schedule.endMinute).map { b ->
            val d = (b - now + 1440) % 1440
            nowMs - nowMs % 60_000L + (if (d == 0) 1440 else d) * 60_000L
        }
        return candidates.min()
    }
}
