package com.nuvio.tv.fork.settings.nav

/**
 * Drzewo nawigacji ustawień w trybie Zaawansowanym (19.11, właściciel): 4–8 kategorii, w każdej 4–8 podkategorii.
 * Zbiera w jednym miejscu kategorie Nuvio, całe menu Reshaped (grupy pozycji), Nuvio Forge i moduły forka.
 * Czysty Kotlin (testy jednostkowe); rysowanie: [ForkAdvancedSettings].
 */
sealed interface NavTarget {
    /** Kategoria pełnych ustawień Nuvio (nazwa `SettingsCategory`), otwierana w pełnym ekranie; Wstecz wraca do drzewa. */
    data class Category(val name: String) : NavTarget
    /** Ekran z wybranymi częściami: grupami pozycji Reshaped ([ItemsGroup]) i modułami forka (id z rejestru). */
    data class Parts(val items: List<ItemsGroup> = emptyList(), val modules: List<String> = emptyList()) : NavTarget
    /** Trasa nawigacji (dodatki, szukaj w telewizji, wszystkie moduły). */
    data class Route(val route: String) : NavTarget
    /** Wybór poziomu ustawień (NOOB / Prosty / Zaawansowany). */
    data object Level : NavTarget
}

/** Grupy pozycji z kategorii Nuvio Reshaped (każda = jedna funkcja `xxxSettingsItems` z `ui/screens/settings`). */
enum class ItemsGroup {
    AUTOSYNC,
    PILL_NAV, LIVE_TV, RESHAPED_SYNC, KE_CATALOG, DEVICE_TEST, FORK_SWITCHES, SKIN, ECHO, YOUTUBE,
    REMOTE_BUTTONS,
}

data class NavLeaf(val id: String, val title: String, val description: String, val target: NavTarget)

data class NavSection(val id: String, val title: String, val leaves: List<NavLeaf>)

object SettingsTree {
    const val MIN = 4
    const val MAX = 8

    private fun cat(id: String, title: String, d: String, c: String) = NavLeaf(id, title, d, NavTarget.Category(c))
    private fun items(id: String, title: String, d: String, vararg g: ItemsGroup) = NavLeaf(id, title, d, NavTarget.Parts(items = g.toList()))
    private fun module(id: String, title: String, d: String, m: String, vararg g: ItemsGroup) =
        NavLeaf(id, title, d, NavTarget.Parts(items = g.toList(), modules = listOf(m)))
    private fun route(id: String, title: String, d: String, r: String) = NavLeaf(id, title, d, NavTarget.Route(r))

    val sections: List<NavSection> = listOf(
        NavSection("konto", "Konto i profile", listOf(
            cat("konto", "Konto", "Logowanie i konto Nuvio", "ACCOUNT"),
            cat("profile", "Profile", "Profile domowników, zdjęcia, blokada", "PROFILES"),
            cat("wspolne", "Wspólne oglądanie", "Oglądanie razem z innymi", "WATCH_PARTY"),
            module("rodzina", "Rodzina", "Wspólne ustawienia całej rodziny", "family"),
            items("synchronizacja", "Synchronizacja", "Kopia ustawień i przenoszenie na inny telewizor", ItemsGroup.RESHAPED_SYNC),
            cat("sledzenie", "Śledzenie obejrzanych", "Trakt i Simkl", "TRACKING"),
        )),
        NavSection("wyglad", "Wygląd i ekran główny", listOf(
            cat("motyw", "Motyw i język", "Kolory, tryb AMOLED, język aplikacji", "APPEARANCE"),
            cat("uklad", "Układ strony głównej", "Rzędy, plakaty, kolejność", "LAYOUT"),
            items("skorka", "Skórka", "Wygląd całej aplikacji", ItemsGroup.SKIN),
            items("menu", "Menu i styl", "Pasek menu u góry ekranu", ItemsGroup.PILL_NAV),
            cat("nocny", "Tryb nocny", "Godziny, ciepłe kolory, cichszy dźwięk", "NIGHT_MODE"),
            module("odkrywaj", "Odkrywaj telewizję", "Rzędy z telewizji na ekranie głównym", "discover"),
            module("nastroje", "Na co masz ochotę?", "Propozycje według nastroju", "picks"),
        )),
        NavSection("odtwarzanie", "Odtwarzanie", listOf(
            cat("ogolne", "Odtwarzacz i źródła", "Wybór źródła, języki, następny odcinek", "PLAYBACK"),
            cat("obraz", "Dźwięk i obraz", "Dolby Vision, HDR10, formaty dźwięku", "PLAYBACK"),
            items("echo", "Jakość i odtwarzacz Echo", "Szybszy start, menu jakości i języka, polski dźwięk", ItemsGroup.ECHO), // 22.11: Auto jakość wyłączona (DECYZJE)
            module("dobor", "Dobór źródeł", "Polski dźwięk, napisy i jakość przy wyborze", "sources"),
            cat("naprawa", "Gdy film się zatnie", "Przejście na następne pasujące źródło", "PLAYBACK"),
            items("pilot", "Przyciski pilota", "Co robi każdy przycisk", ItemsGroup.REMOTE_BUTTONS),
        )),
        NavSection("napisy", "Napisy i lektor", listOf(
            items("dopasowanie", "Dopasowanie napisów", "Napisy równo z mową", ItemsGroup.AUTOSYNC),
            cat("czcionka", "Wygląd napisów", "Czcionka i rozmiar", "PLAYBACK"),
            module("ai", "Napisy AI", "Tłumaczenie napisów na polski", "ai_subtitles"),
            module("lektor", "Lektor", "Głos czytający napisy", "lektor"),
            cat("mowa", "Rozpoznawanie mowy", "Model do napisów z dźwięku", "PLAYBACK"),
            module("glos", "Sterowanie głosem", "Polecenia mówione do pilota", "voice"),
        )),
        NavSection("tv", "Telewizja", listOf(
            items("livetv", "Live TV", "Kanały na żywo i przewodnik", ItemsGroup.LIVE_TV),
            cat("epg", "Źródła, EPG i logotypy", "Listy kanałów, program, logo, układ", "FORGE"),
            items("katalogi", "Katalogi IPTV", "Filmy i seriale z IPTV", ItemsGroup.KE_CATALOG),
            module("dodatki_tv", "Dodatki telewizji", "Przewodnik w tle, logo, obejrzane", "tv"),
            module("tvdb", "Seriale w IPTV", "Opisy odcinków z TVDB", "tvdb"),
            route("szukaj_tv", "Szukaj w telewizji", "Programy i kanały", "fork_tv_search"),
        )),
        NavSection("zrodla", "Dodatki i usługi", listOf(
            route("dodatki", "Dodatki", "Instalowanie i kolejność dodatków", "addons"),
            cat("katalogi", "Katalogi i odkrywanie", "Skąd biorą się filmy na ekranie głównym", "CONTENT_DISCOVERY"),
            cat("serwery", "Serwery mediów i usługi", "Plex, Jellyfin, połączone konta", "INTEGRATION"),
            items("youtube", "YouTube", "Konto, jakość, napisy, Shorts", ItemsGroup.YOUTUBE),
            module("test", "Test łącza i sprzętu", "Sprawdź internet i telewizor", "ke_network", ItemsGroup.DEVICE_TEST),
        )),
        NavSection("system", "System", listOf(
            cat("start", "Start z telewizorem", "Uruchamianie, ekran główny, asystent", "FORGE"),
            module("launcher", "Ekran główny i wyjście", "Nuvio jako ekran główny telewizora", "launcher"),
            items("przelaczniki", "Przełączniki", "Kalendarz, wyszukiwarka ustawień, pilot", ItemsGroup.FORK_SWITCHES),
            route("moduly", "Wszystkie moduły", "Każdy dodatek tej wersji w jednym miejscu", "fork_modules"),
            cat("zaawansowane", "Zaawansowane", "Pamięć, bufor, czyszczenie", "ADVANCED"),
            cat("debug", "Diagnostyka", "Dla testów", "DEBUG"),
            cat("o_aplikacji", "O aplikacji", "Wersja, aktualizacje, prywatność", "ABOUT"),
            NavLeaf("poziom", "Poziom ustawień", "NOOB, prosty albo zaawansowany", NavTarget.Level),
        )),
    )

    /** Widoczne sekcje: liście kategorii spoza [visibleCategories] znikają (np. Diagnostyka poza wersją testową). */
    fun visible(visibleCategories: Set<String>): List<NavSection> = sections.map { s ->
        s.copy(leaves = s.leaves.filter { l -> (l.target as? NavTarget.Category)?.name?.let { it in visibleCategories } ?: true })
    }.filter { it.leaves.isNotEmpty() }
}
