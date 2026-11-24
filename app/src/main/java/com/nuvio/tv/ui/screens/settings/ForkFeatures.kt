package com.nuvio.tv.ui.screens.settings

import android.content.Context

object ForkFeatures {
    /** Set by NuvioNavHost; opens a route. */
    var open: ((String) -> Unit)? = null

    // Debug `com.nuvio.fork.OPEN` (TERAZ P4, 19.11): trasa dana nawigatorowi sprzed wyboru profilu przepadała.
    // Pamiętamy, który nawigator ją dostał; nowy nawigator (po wyborze profilu) w ciągu 2 min dostaje ją jeszcze raz.
    private var nav: androidx.navigation.NavHostController? = null
    private var pendingRoute: String? = null
    private var pendingNav: androidx.navigation.NavHostController? = null
    private var pendingUntilMs = 0L

    /** NuvioNavHost (hook): każda kompozycja. */
    fun bind(navController: androidx.navigation.NavHostController) {
        open = { r -> navController.navigate(r) }
        com.nuvio.tv.fork.family.FamilyUsage.watch(navController) // statystyki używania (panel Rodzina)
        com.nuvio.tv.fork.perf.ForkFocusTrace.watch(navController) // 24.11 UX JMGO: trasa do pomiaru fokusu
        if (nav === navController) return
        nav = navController
        val r = pendingRoute ?: return
        if (pendingNav !== navController && android.os.SystemClock.uptimeMillis() < pendingUntilMs) {
            pendingRoute = null
            android.os.Handler(android.os.Looper.getMainLooper()).post { runCatching { navController.navigate(r) } }
        }
    }

    /** Tylko debug: otwórz [route] teraz (jeśli jest nawigator) i jeszcze raz w nowym nawigatorze, gdy powstanie w ciągu 2 min. */
    fun openDebug(route: String) {
        pendingRoute = route; pendingNav = nav; pendingUntilMs = android.os.SystemClock.uptimeMillis() + 120_000L
        // UX 22.11 (testy ścieżek): każda trasa dokładała ekran na stos — po kilkudziesięciu ścieżkach stos był długi
        // (wolniejsze ekrany, Wstecz trafiało w poprzedni test). Ta sama trasa zastępuje swoją poprzednią kopię.
        nav?.let { n ->
            runCatching { n.navigate(route) { launchSingleTop = true; popUpTo(route) { inclusive = true } } }.onFailure {
                android.util.Log.w("ForkDebug", "trasa $route bez zdjęcia ze stosu: ${it.javaClass.simpleName}: ${it.message}")
                runCatching { n.navigate(route) }.onFailure { e -> android.util.Log.w("ForkDebug", "trasa $route: ${e.javaClass.simpleName}: ${e.message}") }
            }
        }
    }
    private fun prefs(c: Context) = c.getSharedPreferences("fork_features", Context.MODE_PRIVATE)
    /**
     * Funkcje forków włączone od pierwszego uruchomienia (polecenie właściciela 18.11): wygrywa z domyślną wartością
     * podaną w miejscu odczytu. Zapisany wybór użytkownika zawsze ma pierwszeństwo.
     */
    val ON_BY_DEFAULT = setOf(
        "family_usage_stats",
        // Kicia: kontrole otwartego pliku i pamięć złych źródeł
        com.nuvio.tv.fork.sources.StartupChecks.KEY, com.nuvio.tv.fork.sources.MpvContainers.KEY,
        // Echo
        com.nuvio.tv.fork.echo.FastStart.KEY,
        "echo_source_fallback", "echo_race", "echo_audio_menu",
        // YouTube, katalog KE
        "fork_youtube", "fork_ke_catalog",
        // Moduły rejestru wyłączone domyślnie przez autora
        "module_lektor",
        // Lektor sam, gdy brak polskiego dźwięku; głos: Whisper offline (gdy model pobrany), przycisk asystenta przez usługę
        "lektor_auto", "voice_whisper", "voice_key_service",
        // Start Nuvio z telewizorem
        "launcher_autostart",
        // W1: limit łącza KE w rankingu
        com.nuvio.tv.fork.net.KeNetwork.KEY,
    )
    fun enabled(c: Context, key: String, default: Boolean = false) = prefs(c).getBoolean(key, default || key in ON_BY_DEFAULT)
    fun set(c: Context, key: String, v: Boolean) {
        com.nuvio.tv.fork.family.FamilyUsage.feature("przełącznik:$key=" + if (v) "wł" else "wył")
        prefs(c).edit().putBoolean(key, v).apply()
    }
    fun str(c: Context, key: String, default: String = "") = prefs(c).getString(key, default) ?: default
    fun setStr(c: Context, key: String, v: String) = prefs(c).edit().putString(key, v).apply()
}

