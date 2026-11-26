package com.nuvio.tv.fork.settings.tree

import com.nuvio.tv.fork.settings.nav.ItemsGroup

/**
 * Jedno drzewo ustawień (23.11, właściciel: „ustawienia trudne w obsłudze… stwórz całe drzewo”). Projekt i uzasadnienie:
 * `plan/ustawienia/DRZEWO.md`. Czysty Kotlin (testy JVM): 7 kategorii → grupy → pozycje. Poziom pozycji:
 * [TreeLevel.NOOB] widać też w trybie prostym, [TreeLevel.MAIN] w pełnym, [TreeLevel.MORE] dopiero po „Więcej” w grupie.
 * Pozycje modułów forka (rejestr `fork/registry`) wpinane przez sloty [Slot.Module] (klucz ustawienia albo tytuł czynności);
 * moduł może też sam wskazać miejsce polem `place` w `ModuleSetting`/`ModuleAction` („kategoria/grupa”).
 */
enum class TreeLevel { NOOB, MAIN, MORE }

sealed interface TreeTarget {
    /** Przełącznik w prefs `fork_features` (ForkFeatures). */
    data class Toggle(val key: String, val default: Boolean) : TreeTarget
    /** Czynność modułu z rejestru (tytuł bez znaczka „✓ ”). */
    data class Action(val module: String, val title: String) : TreeTarget
    /** Główny wyłącznik modułu (`module_<id>`). */
    data class ModuleSwitch(val module: String, val default: Boolean) : TreeTarget
    /** Osobny ekran (trasa nawigacji). */
    data class Route(val route: String) : TreeTarget
    /** Kategoria pełnych ustawień Nuvio (nazwa `SettingsCategory`); Wstecz wraca do drzewa. */
    data class Base(val category: String) : TreeTarget
    /** Ekran Odtwarzanie z rozwiniętą sekcją (nazwa `PlaybackSection`). */
    data class Playback(val section: String) : TreeTarget
    /** Grupy pozycji Reshaped/forka rysowane w panelu (te same funkcje co w kategorii „Nuvio Reshaped”). */
    data class Parts(val groups: List<ItemsGroup>) : TreeTarget
    /** Pozycja obsługiwana przez ekran (okno, wybór z wartością). Id w [SettingsTreeSpec.SPECIALS]. */
    data class Special(val id: String) : TreeTarget
}

data class TreeItem(
    val id: String,
    val title: String,
    val description: String,
    val level: TreeLevel,
    val target: TreeTarget,
    val keywords: String = "",
    /** Pliki ekranów (nazwa bez .kt) z [SettingsTreeWords] — szukanie po nazwach wierszy w środku. */
    val words: List<String> = emptyList(),
    /** Widoczne tylko, gdy ta kategoria Nuvio jest widoczna (profil główny, wersja testowa, wspólne oglądanie). */
    val needsCategory: String? = null,
    /** Moduł, z którego pochodzi pozycja (wyłączony moduł = brak pozycji). */
    val module: String? = null,
)

data class TreeGroup(val id: String, val title: String, val items: List<TreeItem>)

data class TreeCategory(val id: String, val title: String, val description: String, val icon: String, val groups: List<TreeGroup>)

/** Pozycja modułu z rejestru (wejście do [SettingsTreeSpec.build]); [ref] = klucz ustawienia albo „a:” + tytuł czynności. */
data class ModuleEntry(
    val module: String,
    val ref: String,
    val title: String,
    val description: String,
    val level: TreeLevel,
    val target: TreeTarget,
    val place: String? = null,
)

/** Tryb widoku: prosty (tylko NOOB) albo pełny. */
enum class TreeMode { SIMPLE, FULL }

sealed interface Slot {
    data class Static(val item: TreeItem) : Slot
    /** Miejsce na pozycję modułu; nadpisania nazwy/opisu/poziomu (prostsze słowa niż w module). */
    data class Module(val ref: String, val level: TreeLevel?, val title: String?, val description: String?, val keywords: String) : Slot
}

class SlotGroup(val id: String, val title: String, val slots: List<Slot>)
class SlotCategory(val id: String, val title: String, val description: String, val icon: String, val groups: List<SlotGroup>)

object SettingsTreeSpec {
    const val MAX_CATEGORIES = 7
    /** Klucz wyłącznika nowego drzewa (ZASADY §2: każda funkcja ma przełącznik). */
    const val KEY = "fork_settings_tree"
    const val FALLBACK_GROUP = "inne"

    val SPECIALS = setOf(
        "tv_sources", "tv_search", "level", "logo_mode", "home_youtube", "reminder_card", "tv_layout",
        "sources_answer", "subtitle_size", "night_mode", "start_page", "start_permission", "glass",
    )

    private val N = TreeLevel.NOOB
    private val M = TreeLevel.MAIN
    private val W = TreeLevel.MORE

    private fun s(id: String, title: String, d: String, level: TreeLevel, target: TreeTarget, kw: String = "", words: List<String> = emptyList(), needs: String? = null, module: String? = null) =
        Slot.Static(TreeItem(id, title, d, level, target, kw, words, needs, module))
    private fun m(ref: String, level: TreeLevel? = null, title: String? = null, d: String? = null, kw: String = "") = Slot.Module(ref, level, title, d, kw)
    private fun pb(section: String) = TreeTarget.Playback(section)
    private fun parts(vararg g: ItemsGroup) = TreeTarget.Parts(g.toList())
    private fun sp(id: String) = TreeTarget.Special(id)
    private fun route(r: String) = TreeTarget.Route(r)
    private fun base(c: String) = TreeTarget.Base(c)
    private fun g(id: String, title: String, vararg slots: Slot) = SlotGroup(id, title, slots.toList())

    /** Słowa z ekranów odtwarzania (nazwy plików z [SettingsTreeWords]). */
    private val PLAYER = listOf("PlaybackPlayerSettings")
    private val AUTOPLAY = listOf("PlaybackAutoPlaySettings", "EssentialPlaybackSettingsContent")

    val layout: List<SlotCategory> = listOf(
        SlotCategory("ogladanie", "Oglądanie", "Jak włączają się filmy i seriale", "play", listOf(
            g("start", "Włączanie filmu",
                s("sources_answer", "Polski dźwięk czy jakość", "Co ważniejsze przy wyborze źródła: polski dźwięk czy obraz", N, sp("sources_answer"),
                    "lektor dubbing polski jezyk jakosc zrodlo oryginal", module = "sources"),
                s("stream_selection", "Wybór źródła", "Jak aplikacja wybiera źródło po naciśnięciu Odtwórz", M, pb("STREAM_SELECTION"),
                    "zrodlo stream autoplay automatyczny wybor regex dodatki", AUTOPLAY),
                s("source_recovery", "Gdy film się zatnie", "Przejście na następne pasujące źródło, gdy film nie rusza", M, pb("STREAM_SELECTION"),
                    "zacina buforuje zmiana zrodla blad nastepne zrodlo", AUTOPLAY),
                s("echo", "Szybszy start filmu", "Wczytywanie źródeł z wyprzedzeniem i polski dźwięk z innego wydania", M, parts(ItemsGroup.ECHO),
                    "echo szybki start preload polski dzwiek wydanie", listOf("EchoPlaybackSettingsItems")),
                s("p2p", "Torrenty", "Odtwarzanie źródeł z sieci torrent", W, pb("P2P"), "p2p torrent peer", listOf("PlaybackP2pSettings")),
            ),
            g("seriale", "Seriale",
                s("up_next", "Następny odcinek", "Automatyczne włączanie kolejnego odcinka", N, pb("UP_NEXT"),
                    "nastepny odcinek autoplay binge czy nadal ogladasz", AUTOPLAY),
                s("skip", "Pomijanie czołówek", "Pomijaj czołówki, streszczenia i napisy końcowe", N, pb("SKIP_SEGMENTS"),
                    "czolowka intro outro pomin recap napisy koncowe", PLAYER),
            ),
            g("obraz", "Obraz i odtwarzacz",
                s("video", "Obraz", "Dopasowanie klatek i rozdzielczości, Dolby Vision i HDR", M, pb("VIDEO"),
                    "obraz klatki hz rozdzielczosc dolby vision hdr", listOf("PlaybackVideoSettings")),
                s("player_ui", "Wygląd odtwarzacza", "Zegar, informacje i przyciski na ekranie podczas filmu", M, pb("PLAYER_INTERFACE"),
                    "zegar osd przyciski pauza ladowanie rozdzialy", PLAYER),
                s("player", "Odtwarzacz", "Wbudowany albo zewnętrzny odtwarzacz", W, pb("PLAYER"),
                    "odtwarzacz exoplayer mpv vlc zewnetrzny silnik", PLAYER),
                s("buffer", "Pamięć na film", "Ile filmu wczytywać z wyprzedzeniem", W, pb("BUFFER_NETWORK"),
                    "bufor cache pamiec siec", listOf("PlaybackBufferNetworkSettings")),
            ),
        )),
        SlotCategory("napisy", "Napisy i dźwięk", "Język, napisy, napisy AI i lektor", "subtitles", listOf(
            g("napisy", "Napisy",
                s("subtitle_size", "Rozmiar napisów", "Małe, średnie albo duże napisy", N, sp("subtitle_size"), "napisy wielkosc rozmiar czcionka", module = "sources"),
                s("subtitles", "Napisy: język i wygląd", "Preferowany język, kolor i styl napisów", N, pb("SUBTITLES"),
                    "napisy jezyk kolor styl wymuszone sdh", listOf("PlaybackSubtitleSettings", "SubtitleAppearanceEditor")),
                m(com.nuvio.tv.fork.subs.PolishFirst.KEY, M),
                s("autosync", "Napisy równo z mową", "Automatyczne dopasowanie napisów do dialogów", M, parts(ItemsGroup.AUTOSYNC),
                    "synchronizacja napisow opoznienie przesuniecie autosync", listOf("AutoSyncSettingsItems", "AudioSyncSettingsItems")),
                s("subtitle_font", "Czcionka napisów", "Krój pisma napisów", M, pb("SUBTITLES"), "czcionka font napisy", listOf("PlaybackSubtitleSettings")),
            ),
            g("ai", "Napisy AI",
                m("a:AI napisy: klucz, model, limity, zapisane", N, "Napisy AI: tłumaczenie na polski", "Klucz, model, limit wydatków i zapisane tłumaczenia", "gemini tlumaczenie ai"),
                m(com.nuvio.tv.fork.ai.subtitles.AiAuto.K_AUTO, M),
                m(com.nuvio.tv.fork.ai.subtitles.AiSettings.K_NOTICE, M),
                m(com.nuvio.tv.fork.ai.subtitles.AiSettings.K_REVIEW, W),
                m(com.nuvio.tv.fork.ai.subtitles.AiSettings.K_FALLBACK, W),
            ),
            g("dzwiek", "Dźwięk i lektor",
                s("audio", "Język i dźwięk", "Preferowany język dźwięku, ściszanie i kanały", N, pb("AUDIO"),
                    "dzwiek audio jezyk glosnosc kanaly downmix tryb nocny", listOf("PlaybackAudioSettings")),
                s("lektor", "Lektor", "Głos czytający polskie napisy", N, TreeTarget.ModuleSwitch("lektor", false), "lektor glos czytanie napisow tts"),
                m("lektor_auto", M),
                m("a:Lektor: silnik głosu*", M, kw = "glos chirp gemini synteza"),
                m("a:Lektor: głośność względem filmu (100% / 70% / 50% / 130% / 160%)", M, "Głośność lektora"),
                m("a:Lektor: ściszanie filmu pod lektorem (−6 / −3 / bez / −10 / −14 dB)", M, "Ściszanie filmu pod lektorem"),
                m("lektor_band_duck", M),
                m(com.nuvio.tv.fork.lektor.LektorStore.KEY_DRIVE, W),
                m(com.nuvio.tv.fork.lektor.LektorStore.KEY, W),
                m("a:Lektor: wyczyść pamięć podręczną", W, "Lektor: wyczyść zapisane nagrania"),
                m("a:Lektor Chirp*", W, kw = "glos lektora"),
                m("a:Lektor Gemini*", W, kw = "glos lektora gemini"),
                s("asr", "Rozpoznawanie mowy", "Angielski model mowy do dopasowania napisów (pobierany raz)", W, pb("SUBTITLES"),
                    "mowa model rozpoznawanie angielski"),
            ),
        )),
        SlotCategory("tv", "Telewizja", "Kanały na żywo, program i archiwum", "tv", listOf(
            g("kanaly", "Kanały i program",
                s("tv_sources", "Listy kanałów i program", "Dodaj listę kanałów i adres programu telewizyjnego", N, sp("tv_sources"),
                    "iptv m3u xtream epg zrodla lista kanalow program przewodnik", listOf("ForgeSettingsContent")),
                s("tv_search", "Szukaj w telewizji", "Znajdź kanał albo program", M, sp("tv_search"), "szukaj kanal program"),
                m(com.nuvio.tv.fork.tv.country.TvCountry.KEY_PREFER, M),
                m(com.nuvio.tv.fork.tv.country.TvCountry.KEY_ONLY, M),
                m("a:Zmień kraj IPTV", M, "Kraj kanałów", "Każde naciśnięcie wybiera następny kraj (domyślnie Polska)", "kraj polska"),
                m(com.nuvio.tv.fork.tv.vod.LiveVod.KEY, M),
                m("tv_plain_names", M),
                s("logo_mode", "Logo kanałów", "Skąd brać logo kanałów", M, sp("logo_mode"), "logo logotypy"),
                s("forge_tv", "Własne logo i kanały bez programu", "Adres logotypów i lista kanałów, dla których nie ma programu", W, base("FORGE"),
                    "logo epg bez programu", listOf("ForgeSettingsContent")),
                m(com.nuvio.tv.fork.tv.guide.ExtraGuides.KEY, W),
            ),
            g("ogladanie_tv", "Oglądanie telewizji",
                m("tv_reminders", M),
                m(com.nuvio.tv.fork.tv.LiveNoAsk.KEY, M),
                m("tv_for_you_row", M),
                s("live_preview", "Podgląd kanałów", "Mały podgląd kanału i jego dźwięk", M, parts(ItemsGroup.LIVE_TV),
                    "podglad kanalu dzwiek podgladu live tv", listOf("LiveTvSettingsItems")),
                m("tv_auto_follow", M),
                m("tv_watched_strict", W),
                m("a:Wyczyść historię oglądania TV", W),
            ),
            g("iptv", "Filmy i seriale z list IPTV",
                s("ke_catalog", "Katalogi IPTV", "Filmy i seriale z list IPTV na ekranie głównym", M, parts(ItemsGroup.KE_CATALOG),
                    "iptv vod katalog filmy seriale", listOf("KeCatalogSettingsItems")),
                m("a:TVDB: sprawdź połączenie", W, "Opisy odcinków seriali (TVDB)"),
            ),
            g("tv_pamiec", "Pamięć telewizji",
                m(com.nuvio.tv.fork.tv.ram.TvRam.KEY, W),
                m("tv_guide_db", W),
                m("tv_guide_reuse", W),
                m(com.nuvio.tv.fork.perf.PerfFlags.PREVIEW_AUTOSTOP_KEY, W),
                s("reminder_card", "Karta przypomnień (skórka Kicia)", "Karta w rogu, gdy program z przypomnieniem się zaczyna", W, sp("reminder_card"), "przypomnienie karta"),
                s("tv_layout", "Układ telewizji (skórka Kicia)", "Przewodnik z ostatnimi albo osobna strona Przeglądaj", W, sp("tv_layout"), "uklad przewodnik"),
            ),
        )),
        SlotCategory("wyglad", "Wygląd", "Skórka, ekran główny i tryb nocny", "home", listOf(
            g("skorka", "Skórka i język",
                s("skin", "Skórka", "Wygląd całej aplikacji", N, parts(ItemsGroup.SKIN), "skorka niunia kicia wyglad motyw"),
                s("glass", "Szkło", "Przezroczyste menu, panele i przyciski. Na starszych telewizorach niedostępne.", N, sp("glass"),
                    "szklo przezroczyste menu panele przyciski czyste grafit mroz"),
                s("theme", "Kolory, czcionka i język", "Motyw, czarne tło i język aplikacji", N, route("theme_settings"),
                    "jezyk aplikacji motyw kolory amoled czcionka ikona", listOf("ThemeSettingsScreen")),
                s("menu_style", "Menu i styl ekranu głównego", "Pasek menu u góry ekranu", W, parts(ItemsGroup.PILL_NAV),
                    "menu pasek pill", listOf("PillNavSettingsItems")),
            ),
            g("glowny", "Ekran główny",
                s("layout", "Układ ekranu głównego", "Rzędy, plakaty, zwiastuny i strona tytułu", M, route("layout_settings"),
                    "uklad rzedy plakaty zwiastun hero kontynuuj ogladanie", listOf("LayoutHomeSettings", "LayoutCardSettings", "LayoutDetailSettings")),
                m("a:Odkrywaj telewizję (rzędy)", M, "Odkrywaj telewizję", "Rzędy z telewizji: wznów, nowe odcinki, gatunki"),
                m("a:Na co masz ochotę?", M, "Na co masz ochotę?", "Wybierz nastrój, dostaniesz listę filmów"),
                s("home_youtube", "YouTube na ekranie głównym", "Półka z filmami YouTube (więcej danych i pamięci)", M, sp("home_youtube"), "youtube polka"),
                m(com.nuvio.tv.fork.images.ForkImageQuality.KEY, M),
                m(com.nuvio.tv.fork.images.NativeBlur.KEY, W),
                m("discover_show_*", W, kw = "odkrywaj rzad"),
                s("calendar", "Kalendarz w menu", "Pozycja Kalendarz w menu aplikacji", W, TreeTarget.Toggle("fork_calendar", true), "kalendarz premiery"),
                s("rows_cache", "Pamiętaj rzędy", "Gdy serwis z filmami nie odpowiada, pokaż ostatnio wczytane", M, TreeTarget.Toggle(com.nuvio.tv.skin.niunia.home.NiuniaRowsCache.KEY, true), "pamiec cache rzedy blad 429"),
            ),
            g("noc", "Tryb nocny",
                s("night_mode", "Tryb nocny", "Wyłączony, według godzin albo zawsze", N, sp("night_mode"), "noc nocny wieczor cieply", module = "night"),
                m("a:Tryb nocny: początek 21:00 / 22:00 / 23:00 (kolejno)", M, "Początek trybu nocnego"),
                m("a:Tryb nocny: koniec 06:00 / 07:00 / 08:00 (kolejno)", M, "Koniec trybu nocnego"),
                m("a:Siła: lekka / średnia / mocna (kolejno)", M, "Siła trybu nocnego"),
                m("night_overlay", M),
                m("night_dim", M),
                m("night_skip_hdr", W),
                s("night_more", "Dźwięk nocą i filtr światła", "Ciche sceny głośniejsze, głośne cichsze; harmonogram", W, base("NIGHT_MODE"),
                    "dynamika dzwieku filtr niebieskiego swiatla", listOf("ForgeNightSettings")),
            ),
        )),
        SlotCategory("konta", "Konta i dodatki", "Profile, logowanie, dodatki i usługi", "profile", listOf(
            g("ty", "Ty i domownicy",
                s("profiles", "Profile", "Profile domowników, zdjęcia i blokada", N, route("manage_profiles"), "profil pin dzieci awatar", listOf("ProfileSettingsContent"), needs = "PROFILES"),
                s("account", "Konto Nuvio", "Logowanie kodem z telefonu", N, route("account"), "konto logowanie qr synchronizacja", needs = "ACCOUNT"),
                m("a:Rodzina: wspólne ustawienia", M, "Rodzina", "Wspólne ustawienia całej rodziny (kod 6 cyfr)"),
                s("party", "Wspólne oglądanie", "Oglądanie razem z innymi", M, base("WATCH_PARTY"), "razem party", needs = "WATCH_PARTY"),
            ),
            g("zrodla", "Skąd są filmy",
                s("addons", "Dodatki", "Instalowanie i kolejność dodatków", N, route("addon_manager"), "dodatki addon stremio katalogi zrodla"),
                s("services", "Usługi i dane filmów", "Debrid, TMDB, MDBList i pomijanie anime", M, base("INTEGRATION"),
                    "debrid real torbox tmdb mdblist anime", listOf("DebridSettingsScreen", "TmdbSettingsScreen", "MDBListSettingsScreen", "AnimeSkipSettingsScreen")),
                s("servers", "Serwery mediów i wtyczki", "Plex, Jellyfin, Emby i wtyczki", M, base("CONTENT_DISCOVERY"),
                    "plex jellyfin emby serwer wtyczki", listOf("ContentDiscoverySettingsContent", "MediaServerSettingsScreen")),
                s("youtube", "YouTube", "Konto, jakość, napisy i Shorts", M, base("YOUTUBE"), "youtube shorts", listOf("YouTubeSettingsContent")),
                s("youtube_switch", "YouTube w aplikacji", "Włącza zakładkę YouTube", W, parts(ItemsGroup.YOUTUBE), "youtube"),
            ),
            g("obejrzane", "Obejrzane i kopia",
                s("tracking", "Trakt i Simkl", "Zapis obejrzanych filmów i seriali", M, route("trakt"), "trakt simkl obejrzane postep", listOf("TrackingSettingsScreen")),
                s("sync", "Kopia ustawień", "Synchronizacja ustawień z kontem Google", M, parts(ItemsGroup.RESHAPED_SYNC), "kopia synchronizacja google", listOf("ReshapedSyncSettingsItems")),
                m(com.nuvio.tv.fork.family.FamilyUsage.KEY, W),
            ),
        )),
        SlotCategory("pilot", "Pilot i głos", "Przyciski pilota i polecenia głosowe", "voice-over", listOf(
            g("pilot", "Pilot",
                s("remote", "Przyciski pilota", "Co robi każdy przycisk", M, parts(ItemsGroup.REMOTE_BUTTONS), "pilot przyciski klawisze arc", listOf("PlaybackRemoteButtons")),
                s("arc_remote", "Przyciski pilota w całej aplikacji", "Przyciski ustawione na „Nic” są wyciszone wszędzie", M, TreeTarget.Toggle("fork_arc_remote", true), "pilot arc"),
                m("launcher_lock", M),
            ),
            g("glos", "Głos",
                m("a:Sterowanie głosem: przykłady poleceń", N, "Co mogę powiedzieć"),
                m(com.nuvio.tv.fork.voice.VoiceEntry.KEY, M),
                m("a:Przycisk mikrofonu: włącz usługę Nuvio", M),
                m("a:Przycisk mikrofonu: naucz przycisk", M),
                m("a:Ustaw Nuvio jako asystenta", M),
                m("voice_key_service", M),
                m("voice_whisper", W),
            ),
        )),
        SlotCategory("system", "System", "Start z telewizorem, pomoc i aktualizacje", "settings", listOf(
            g("start", "Start z telewizorem",
                m("launcher_autostart", N),
                s("start_page", "Po uruchomieniu pokaż", "Ekran główny, telewizję albo ostatni kanał", N, sp("start_page"), "start ekran startowy", module = "launcher"),
                m("a:Nuvio jako ekran główny", M),
                s("start_permission", "Zgoda na start po włączeniu", "Ustawienia systemu: „Wyświetlanie nad innymi aplikacjami”", W, sp("start_permission"), "autostart zgoda"),
            ),
            g("pomoc", "Pomoc",
                s("help", "Narzędzia", "Szukaj w telewizji, Odkrywaj, przykłady poleceń głosowych", N, route("fork_tools"), "pomoc narzedzia blad dziennik log"), // UX 25.11: ekran to „Narzędzia”, nie dziennik błędów
                m("a:Test łącza (ostatni film)", M, "Test łącza"),
                m(com.nuvio.tv.fork.net.KeAutoTest.KEY, M),
                s("device_test", "Test telewizora i łącza", "Sprawdź internet i sprzęt", W, parts(ItemsGroup.DEVICE_TEST), "test sprzet"),
                s("focus_trace", "Pomiar szybkości pilota", "Zapisuje w dzienniku, jak szybko reaguje zaznaczenie (do testów)", W, TreeTarget.Toggle(com.nuvio.tv.fork.perf.ForkFocusTrace.KEY, false), "fokus pomiar szybkosc pilot test dziennik"),
                s("about", "O aplikacji i aktualizacje", "Wersja, aktualizacje i prywatność", M, route("about"), "wersja aktualizacja prywatnosc", listOf("AboutScreen", "UpdateChannelSettings")),
            ),
            g("ustawienia", "Ustawienia",
                s("level", "Tryb ustawień", "Prosty: tylko najważniejsze; pełny: wszystko", N, sp("level"), "noob prosty pelny zaawansowany poziom"),
                s("advanced", "Pamięć i wydajność", "Pamięć podręczna, obrazy, raporty błędów", W, base("ADVANCED"),
                    "pamiec cache wydajnosc sentry raporty", listOf("NetworkSettingsScreen", "AmbientModeSettings")),
                s("modules", "Wszystkie moduły", "Włączanie i wyłączanie całych funkcji", W, route("fork_modules"), "moduly funkcje"),
                s("tree_switch", "Nowy układ ustawień", "Wyłączony = dawne ustawienia", W, TreeTarget.Toggle(KEY, true), "stare ustawienia"),
                s("old_nav", "Dawne ustawienia: menu w kategoriach", "Dotyczy dawnego ekranu ustawień", W, TreeTarget.Toggle("fork_settings_nav", true)),
                s("old_search", "Dawne ustawienia: przycisk szukania", "Dotyczy dawnego ekranu ustawień", W, TreeTarget.Toggle("fork_settings_search", true)),
                s("debug", "Diagnostyka", "Dla testów", W, base("DEBUG"), "debug", needs = "DEBUG"),
            ),
        )),
    )

    /** Wszystkie odwołania do modułów w slotach (test: każde istnieje, każda pozycja modułu ma miejsce). „x*” = wszystkie zaczynające się od x. */
    val moduleRefs: List<String> = layout.flatMap { c -> c.groups.flatMap { g -> g.slots.filterIsInstance<Slot.Module>().map { it.ref } } }

    /** Czynności modułów obsłużone przez pozycje [TreeTarget.Special] (jeden wiersz z wartością zamiast kilku) — bez osobnych wierszy. */
    val COVERED = setOf(
        "a:Zawsze po polsku", "a:Po polsku przy dobrej jakości", "a:Oryginał z napisami",
        "a:Napisy: małe", "a:Napisy: średnie", "a:Napisy: duże",
        "a:Tryb nocny: według godzin 22:00–07:00", "a:Tryb nocny: włączony", "a:Tryb nocny: wyłączony",
        "a:Po uruchomieniu pokaż: ekran główny", "a:Po uruchomieniu pokaż: telewizję", "a:Po uruchomieniu pokaż: ostatni kanał",
    )

    private fun slotted(ref: String) = ref in COVERED || moduleRefs.any { if (it.endsWith("*")) ref.startsWith(it.dropLast(1)) else it == ref }

    /**
     * Pełne drzewo: sloty modułów wypełnione pozycjami z rejestru (wyłączony moduł = pustka), pozycje bez slotu w miejscu
     * z ich `place` albo w grupie [FALLBACK_GROUP] kategorii System (pod „Więcej”). Puste grupy znikają.
     */
    fun build(entries: List<ModuleEntry>, visibleCategories: Set<String>? = null, enabledModules: Set<String>? = null): List<TreeCategory> {
        val byRef = entries.associateBy { it.ref }
        val extra = entries.filter { !slotted(it.ref) && it.place != null }
            .groupBy({ it.place!! }, { it.toItem(null) }).mapValues { it.value.toMutableList() }.toMutableMap()
        fun visible(i: TreeItem) = (i.needsCategory == null || visibleCategories == null || i.needsCategory in visibleCategories) &&
            (i.module == null || enabledModules == null || i.module in enabledModules)
        val cats = layout.map { c ->
            TreeCategory(c.id, c.title, c.description, c.icon, c.groups.map { g ->
                val items = g.slots.flatMap { sl ->
                    when (sl) {
                        is Slot.Static -> listOf(sl.item)
                        is Slot.Module -> if (sl.ref.endsWith("*")) entries.filter { it.ref.startsWith(sl.ref.dropLast(1)) }.map { it.toItem(sl.copy(title = null)) }
                            else listOfNotNull(byRef[sl.ref]?.toItem(sl))
                    }
                }.filter(::visible) + (extra.remove(c.id + "/" + g.id) ?: emptyList<TreeItem>())
                TreeGroup(g.id, g.title, items)
            })
        }
        val leftover = entries.filter { !slotted(it.ref) && it.place == null }.map { it.toItem(null).copy(level = TreeLevel.MORE) } +
            extra.values.flatten().map { it.copy(level = TreeLevel.MORE) }
        return cats.map { c ->
            if (c.id != "system" || leftover.isEmpty()) c else c.copy(groups = c.groups + TreeGroup(FALLBACK_GROUP, "Inne", leftover))
        }.map { c -> c.copy(groups = c.groups.filter { it.items.isNotEmpty() }) }.filter { it.groups.isNotEmpty() }
    }

    private fun ModuleEntry.toItem(sl: Slot.Module?) = TreeItem(
        id = "m:" + module + ":" + ref, title = sl?.title ?: title, description = sl?.description ?: description,
        level = sl?.level ?: level, target = target, keywords = sl?.keywords.orEmpty(), module = module,
    )

    /** Pozycje widoczne w trybie: prosty = tylko NOOB; pełny = NOOB+MAIN (+MORE, gdy grupa rozwinięta — decyduje ekran). */
    fun itemsFor(group: TreeGroup, mode: TreeMode, expanded: Boolean): List<TreeItem> = when (mode) {
        TreeMode.SIMPLE -> group.items.filter { it.level == TreeLevel.NOOB }
        TreeMode.FULL -> group.items.filter { it.level != TreeLevel.MORE || expanded }
    }

    fun moreCount(group: TreeGroup, mode: TreeMode) = if (mode == TreeMode.SIMPLE) 0 else group.items.count { it.level == TreeLevel.MORE }

    /** Kategorie/grupy widoczne w trybie (prosty: tylko te z pozycjami NOOB). */
    fun forMode(tree: List<TreeCategory>, mode: TreeMode): List<TreeCategory> =
        if (mode == TreeMode.FULL) tree
        else tree.map { c -> c.copy(groups = c.groups.filter { g -> g.items.any { it.level == TreeLevel.NOOB } }) }.filter { it.groups.isNotEmpty() }

    /** Ścieżka do pokazania w wynikach szukania: „Kategoria › Grupa”. */
    fun path(tree: List<TreeCategory>, itemId: String): Triple<TreeCategory, TreeGroup, TreeItem>? {
        tree.forEach { c -> c.groups.forEach { g -> g.items.firstOrNull { it.id == itemId }?.let { return Triple(c, g, it) } } }
        return null
    }
}
