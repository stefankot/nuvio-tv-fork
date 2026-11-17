package com.nuvio.tv.fork.launcher

/** Licznik naciśnięć Wstecz (czysta klasa): [PRESSES] w [WINDOW_MS] otwiera „Wyjście i ustawienia”. */
class BackPressCounter(private val presses: Int = PRESSES, private val windowMs: Long = WINDOW_MS) {
    private val times = ArrayDeque<Long>()

    /** true = to naciśnięcie otwiera ekran wyjścia (licznik zerowany). */
    fun press(nowMs: Long): Boolean {
        times.addLast(nowMs)
        while (times.isNotEmpty() && nowMs - times.first() > windowMs) times.removeFirst()
        if (times.size >= presses) { times.clear(); return true }
        return false
    }

    companion object { const val PRESSES = 5; const val WINDOW_MS = 3_000L }
}

/** Co robi Wstecz na ekranie głównym przy włączonej blokadzie wyjścia. */
enum class ExitDecision { NOTHING, OPEN_EXIT_SCREEN, ASK_PIN }

object ExitPolicy {
    fun onHomeBack(lockOn: Boolean, counter: BackPressCounter, nowMs: Long, hasPin: Boolean): ExitDecision {
        if (!lockOn) return ExitDecision.NOTHING
        return if (counter.press(nowMs)) (if (hasPin) ExitDecision.ASK_PIN else ExitDecision.OPEN_EXIT_SCREEN) else ExitDecision.NOTHING
    }

    /** Pozycje ekranu „Wyjście i ustawienia”. */
    val exitScreen = listOf("Zamknij aplikację", "Aplikacje", "Ustawienia systemu")
}

/** Dwa poziomy blokady (B06-5). Pełna wymaga aliasu HOME i trasy „Aplikacje”. */
enum class LockLevel { OFF, SOFT, FULL }

object LockLevels {
    /** Czy alias HOME wolno włączyć: trasa „Aplikacje” musi istnieć, inaczej użytkownik nie uruchomi niczego poza Nuvio. */
    fun homeAllowed(appsRouteExists: Boolean) = appsRouteExists

    fun backLockOn(level: LockLevel) = level != LockLevel.OFF
    fun autostart(level: LockLevel, userFlag: Boolean) = userFlag || level == LockLevel.FULL
}

/** Trasa startowa („Po uruchomieniu pokaż”). */
enum class StartPage { HOME, TV, LAST_CHANNEL }

object StartRoute {
    /** [lastChannelUrl] wymagany dla „ostatni kanał”; bez niego wraca do TV. */
    fun resolve(page: StartPage, lastChannelUrl: String?, liveTvAvailable: Boolean): String = when {
        page == StartPage.HOME || !liveTvAvailable -> "home"
        page == StartPage.LAST_CHANNEL && lastChannelUrl != null -> "tv_channel"
        else -> "tv"
    }
}

/** Lista aplikacji (Leanback) do ekranu „Aplikacje”. */
data class AppEntry(val label: String, val packageName: String)

object AppList {
    fun filterSort(apps: List<AppEntry>, self: String): List<AppEntry> =
        apps.filter { it.packageName != self }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
}
