package com.nuvio.tv.fork.voice

/** Wzorzec polecenia: zdanie z miejscem na parametr (regex na tekście znormalizowanym) i przykłady (min. 3). */
data class CommandPattern(val id: String, val regexes: List<Regex>, val examples: List<String>, val needsConfirm: Boolean = false)

private fun p(id: String, vararg rx: String, ex: List<String>, confirm: Boolean = false) =
    CommandPattern(id, rx.map { Regex("^\\s*$it\\s*$") }, ex, confirm)

/** 66 wzorców w 6 grupach. Parametry jako nazwane grupy: kanał, grupa, n, dzien, godzina, rodzaj, gatunek, czas, jezyk, tytul, tekst, nazwa, profil, aplikacja. */
object Patterns {
    val all: List<CommandPattern> = listOf(
        // --- Kanały ---
        p("channel.next", "(?:nastepny|kolejny) kanal", "kanal wyzej", "przelacz (?:na )?nastepny", ex = listOf("następny kanał", "kolejny kanał", "kanał wyżej")),
        p("channel.prev", "poprzedni kanal", "kanal nizej", "wroc na poprzedni kanal", ex = listOf("poprzedni kanał", "kanał niżej", "wróć na poprzedni kanał")),
        p("channel.last", "(?:poprzednio ogladany|ostatnio ogladany)(?: kanal)?", "wroc do ostatniego kanalu", ex = listOf("poprzednio oglądany", "ostatnio oglądany kanał", "wróć do ostatniego kanału")),
        p("channel.number", "kanal (?:numer )?(?<n>\\d{1,4})", "numer (?<n>\\d{1,4})", "wlacz kanal (?:numer )?(?<n>\\d{1,4})", ex = listOf("kanał numer 5", "kanał 12", "włącz kanał numer 7")),
        p("fav.add", "dodaj (?:go |ten kanal )?do ulubionych", "dodaj kanal do ulubionych", "do ulubionych", ex = listOf("dodaj do ulubionych", "dodaj ten kanał do ulubionych", "do ulubionych")),
        p("fav.remove", "usun (?:go |ten kanal )?z ulubionych", "usun kanal z ulubionych", "wyrzuc z ulubionych", ex = listOf("usuń z ulubionych", "usuń ten kanał z ulubionych", "wyrzuć z ulubionych")),
        p("fav.show", "(?:pokaz|wlacz|otworz) ulubione", "ulubione kanaly", "moje ulubione", ex = listOf("pokaż ulubione", "ulubione kanały", "moje ulubione")),
        p("now.list", "co leci teraz", "co jest teraz w telewizji", "co teraz grają", ex = listOf("co leci teraz", "co jest teraz w telewizji", "co teraz grają")),
        p("next.on", "co (?:bedzie|leci|jest) potem (?:na|w) (?<kanal>.+)", "co dalej (?:na|w) (?<kanal>.+)", "co pozniej (?:na|w) (?<kanal>.+)", ex = listOf("co będzie potem na TVP 1", "co dalej na Polsacie", "co później w TVN")),
        p("now.on", "co (?:leci|jest|gra|lecialo) (?:teraz )?(?:na|w) (?<kanal>.+)", "co ogladac (?:na|w) (?<kanal>.+)", "program (?:na|w) (?<kanal>.+) teraz", ex = listOf("co leci na TVP 2", "co jest w TVN", "co gra na Polsacie")),
        p("group.show", "(?:pokaz|wlacz|otworz) kanaly (?<grupa>.+)", "kanaly (?<grupa>sportowe|filmowe|informacyjne|dzieciece|muzyczne)", "grupa (?<grupa>.+)", ex = listOf("pokaż kanały sportowe", "kanały filmowe", "otwórz kanały dziecięce")),
        p("channel.play", "(?:wlacz|przelacz na|wlacz mi|daj|pokaz|ogladam|otworz) (?:kanal )?(?<kanal>(?!.*\\b(?:przewodnik|ustawienia|telewizje|napisy|lektora|zwiastun|ekran glowny|bibliotek)).+)", "(?:na )?kanal (?<kanal>.+)", ex = listOf("włącz TVP 1", "przełącz na Polsat", "włącz kanał TVN 24")),
        // --- Przewodnik ---
        p("guide.show", "(?:pokaz|otworz|wlacz) przewodnik", "przewodnik(?: tv)?", "program tv", ex = listOf("pokaż przewodnik", "przewodnik", "program TV")),
        p("guide.tonight", "co (?:jest|leci) dzis wieczorem", "dzis wieczorem", "wieczorny program", ex = listOf("co jest dziś wieczorem", "co leci dziś wieczorem", "dziś wieczorem")),
        p("guide.at", "co (?:jest|leci) (?:o|na) (?:godzinie )?(?<godzina>\\d{1,2}(?::\\d{2})?)", "co (?:jest|leci) o (?<godzina>dwudziestej|dwudziestej pierwszej|osiemnastej|dziewietnastej|dwudziestej drugiej|dwudziestej trzeciej)", "program na (?<godzina>\\d{1,2}(?::\\d{2})?)", ex = listOf("co jest o 20", "co leci o 21:30", "co jest o dwudziestej")),
        p("guide.day", "przewodnik (?:na )?(?<dzien>dzisiaj|dzis|jutro|wczoraj|pojutrze|poniedzialek|wtorek|srode|czwartek|piatek|sobote|niedziele)", "program (?:na|w) (?<dzien>jutro|wczoraj|poniedzialek|wtorek|srode|czwartek|piatek|sobote|niedziele)", "(?<dzien>jutro|wczoraj) w telewizji", ex = listOf("przewodnik na jutro", "program w sobotę", "wczoraj w telewizji")),
        p("now.kind", "(?<rodzaj>filmy|seriale|sport|wiadomosci|bajki) teraz", "co leci z (?<rodzaj>filmow|serialow|sportu)", "teraz (?<rodzaj>filmy|seriale|sport)", ex = listOf("filmy teraz", "seriale teraz", "sport teraz")),
        p("archive.movies", "filmy w archiwum", "archiwalne filmy", "co jest w archiwum", ex = listOf("filmy w archiwum", "archiwalne filmy", "co jest w archiwum")),
        p("nav.page", "(?:nastepna|poprzednia) strona", "strona (?:nizej|wyzej)", "kolejna strona", ex = listOf("następna strona", "poprzednia strona", "kolejna strona")),
        p("nav.down", "(?:w dol|nizej|przewin w dol)", "(?:przejdz )?w dol", ex = listOf("w dół", "niżej", "przewiń w dół")),
        p("nav.up", "(?:w gore|wyzej|przewin w gore)", "(?:przejdz )?w gore", ex = listOf("w górę", "wyżej", "przewiń w górę")),
        p("select.n", "(?:wybierz|otworz|wlacz) (?:numer |pozycje )?(?<n>\\d{1,2}|jeden|dwa|trzy|cztery|piec|szesc|siedem|osiem|dziewiec|dziesiec)", "(?:numer|pozycja) (?<n>\\d{1,2})", "wybierz (?<n>pierwszy|drugi|trzeci|czwarty|piaty)", ex = listOf("wybierz trzy", "wybierz numer 2", "otwórz pozycję 5")),
        p("back", "(?:wroc|cofnij sie|powrot|wstecz)", "wroc do poprzedniego ekranu", ex = listOf("wróć", "wstecz", "powrót")),
        p("genre.show", "pokaz (?<gatunek>komedie|dramaty|horrory|thrillery|kryminaly|dokumenty|dla dzieci|filmy akcji|romanse)", "(?<gatunek>horrory|komedie|dramaty|thrillery|dokumenty) w przewodniku", "gatunek (?<gatunek>.+)", ex = listOf("pokaż komedie", "pokaż horrory", "pokaż dokumenty")),
        // --- Program i archiwum ---
        p("prog.fromstart", "od poczatku", "zacznij od poczatku", "wlacz od poczatku", ex = listOf("od początku", "zacznij od początku", "włącz od początku")),
        p("prog.live", "(?:wroc do |przejdz na )(?:na )?zywo", "na zywo", "wroc na zywo", ex = listOf("wróć do na żywo", "na żywo", "przejdź na żywo")),
        p("seek.back", "(?:cofnij|przewin do tylu|przewin w tyl|cofnij sie) (?:o )?(?<czas>.+)", "(?:cofnij|przewin) (?:o )?(?<czas>.+) (?:do tylu|w tyl)", ex = listOf("cofnij o minutę", "przewiń w tył o pięć minut", "cofnij o 30 sekund")),
        p("seek.forward", "(?:przewin|przeskocz|do przodu) (?:o )?(?<czas>.+)", "(?:przewin|przeskocz) (?:o )?(?<czas>.+) (?:do przodu|naprzod)", ex = listOf("przewiń o minutę", "przeskocz o pięć minut", "przewiń o dwie minuty do przodu")),
        p("skip.ads", "przewin reklamy", "pomin reklamy", "przeskocz reklamy", ex = listOf("przewiń reklamy", "pomiń reklamy", "przeskocz reklamy")),
        p("pause", "(?:pauza|zatrzymaj|stop|wstrzymaj)", "zapauzuj", ex = listOf("pauza", "zatrzymaj", "wstrzymaj")),
        p("play", "(?:graj|wznow|odtwarzaj|dalej|kontynuuj)", "(?:zacznij|wlacz) odtwarzanie", ex = listOf("graj", "wznów", "odtwarzaj")),
        p("ep.next", "(?:nastepny|kolejny) odcinek", "odcinek dalej", "nastepny epizod", ex = listOf("następny odcinek", "kolejny odcinek", "następny epizod")),
        p("ep.prev", "poprzedni odcinek", "odcinek wczesniej", "poprzedni epizod", ex = listOf("poprzedni odcinek", "odcinek wcześniej", "poprzedni epizod")),
        p("ep.n", "odcinek (?<n>\\d{1,4})", "wlacz odcinek (?<n>\\d{1,4})", "epizod (?<n>\\d{1,4})", ex = listOf("odcinek 5", "włącz odcinek 12", "epizod 3")),
        p("follow.remove", "przestan obserwowac(?: ten program)?", "nie obserwuj", "usun z obserwowanych", ex = listOf("przestań obserwować", "nie obserwuj", "usuń z obserwowanych")),
        p("follow.add", "obserwuj(?: ten program)?", "dodaj do obserwowanych", "sledz ten program", ex = listOf("obserwuj", "obserwuj ten program", "dodaj do obserwowanych")),
        p("remind", "przypomnij mi o tym", "przypomnij(?: mi)?", "ustaw przypomnienie", ex = listOf("przypomnij mi o tym", "przypomnij", "ustaw przypomnienie")),
        p("play.title", "(?:wlacz|pusc|puszcz|zagraj|ogladam|odtworz) (?<tytul>.+)", ex = listOf("włącz Pana Tadeusza", "puść Klan", "odtwórz Wiedźmina")),
        // --- Dźwięk i napisy ---
        p("subs.on", "(?:wlacz|pokaz) napisy", "napisy wlacz", "z napisami", ex = listOf("włącz napisy", "pokaż napisy", "z napisami")),
        p("subs.off", "(?:wylacz|ukryj) napisy", "bez napisow", "napisy wylacz", ex = listOf("wyłącz napisy", "ukryj napisy", "bez napisów")),
        p("subs.lang", "napisy po (?<jezyk>polsku|angielsku|niemiecku|francusku|hiszpansku|wlosku|rosyjsku)", "(?:wlacz )?napisy (?<jezyk>polskie|angielskie|niemieckie|francuskie)", ex = listOf("napisy po polsku", "napisy po angielsku", "włącz napisy polskie")),
        p("subs.bigger", "wieksze napisy", "powieksz napisy", "napisy wieksze", ex = listOf("większe napisy", "powiększ napisy", "napisy większe")),
        p("subs.smaller", "mniejsze napisy", "pomniejsz napisy", "napisy mniejsze", ex = listOf("mniejsze napisy", "pomniejsz napisy", "napisy mniejsze")),
        p("subs.mismatch", "napisy nie pasuja", "napisy sie nie zgadzaja", "napisy sa nie w tym miejscu", ex = listOf("napisy nie pasują", "napisy się nie zgadzają", "napisy są nie w tym miejscu")),
        p("audio.lang", "dzwiek po (?<jezyk>polsku|angielsku|niemiecku|francusku|oryginalny)", "(?:wlacz )?(?:dzwiek|audio) (?<jezyk>polski|angielski|oryginalny)", ex = listOf("dźwięk po polsku", "dźwięk po angielsku", "włącz dźwięk oryginalny")),
        p("lektor.off", "(?:wylacz|bez) lektora", "wylacz lektor", ex = listOf("wyłącz lektora", "bez lektora", "wyłącz lektor")),
        p("lektor.on", "(?:wlacz|dodaj) lektora", "wlacz lektor", "z lektorem", ex = listOf("włącz lektora", "dodaj lektora", "z lektorem")),
        p("dialog.louder", "glosniej dialogi", "podglos dialogi", "dialogi glosniej", ex = listOf("głośniej dialogi", "podgłoś dialogi", "dialogi głośniej")),
        p("dialog.quieter", "ciszej dialogi", "sciszaj dialogi", "dialogi ciszej", ex = listOf("ciszej dialogi", "ścisz dialogi", "dialogi ciszej")),
        p("night.untilmorning", "tryb nocny do rana", "wlacz tryb nocny do rana", "noc do rana", ex = listOf("tryb nocny do rana", "włącz tryb nocny do rana", "noc do rana")),
        p("night.on", "(?:wlacz )?tryb nocny(?: wlacz)?", "wlacz noc", "tryb nocny wlaczony", ex = listOf("tryb nocny włącz", "włącz tryb nocny", "włącz noc")),
        p("night.off", "(?:wylacz tryb nocny|tryb nocny wylacz|wylacz noc)", "koniec trybu nocnego", ex = listOf("tryb nocny wyłącz", "wyłącz tryb nocny", "koniec trybu nocnego")),
        p("source.other", "(?:inne|nastepne|kolejne) zrodlo", "sprobuj innego zrodla", "zmien zrodlo", ex = listOf("inne źródło", "spróbuj innego źródła", "zmień źródło")),
        // --- Aplikacja ---
        p("home", "(?:ekran glowny|strona glowna|dom|pulpit)", "wroc do ekranu glownego", "idz do domu", ex = listOf("ekran główny", "strona główna", "wróć do ekranu głównego")),
        p("tv", "(?:telewizja|tv|telewizor|otworz telewizje|przejdz do telewizji)", "wlacz telewizje", ex = listOf("telewizja", "otwórz telewizję", "przejdź do telewizji")),
        p("library", "(?:biblioteka|moja biblioteka|otworz biblioteke|pokaz biblioteke)", "moje filmy", ex = listOf("biblioteka", "otwórz bibliotekę", "pokaż bibliotekę")),
        p("search", "(?:szukaj|wyszukaj|znajdz) (?<tekst>.+)", "wyszukiwarka (?<tekst>.+)", ex = listOf("szukaj Matrix", "wyszukaj Wiedźmin", "znajdź komedie romantyczne")),
        p("settings.open", "otworz ustawienie (?<nazwa>.+)", "ustawienia (?<nazwa>napisy|dzwiek|jezyk|konto|profil|tryb nocny|telewizja)", "przejdz do ustawien (?<nazwa>.+)", ex = listOf("otwórz ustawienie napisy", "ustawienia dźwięk", "przejdź do ustawień tryb nocny")),
        p("settings", "(?:ustawienia|otworz ustawienia|pokaz ustawienia)", "ustawienia aplikacji", ex = listOf("ustawienia", "otwórz ustawienia", "pokaż ustawienia")),
        p("profile.switch", "(?:zmien|przelacz) profil na (?<profil>.+)", "profil (?<profil>.+)", "zalogowac (?<profil>.+)", confirm = true, ex = listOf("zmień profil na Ania", "przełącz profil na Tata", "profil Kasia")),
        p("apps", "(?:aplikacje|pokaz aplikacje|moje aplikacje)", "otworz aplikacje", ex = listOf("aplikacje", "pokaż aplikacje", "otwórz aplikacje")),
        p("app.open", "otworz (?:aplikacje )?(?<aplikacja>youtube|netflix|disney|hbo|spotify|prime video|canal plus|player)", "uruchom (?<aplikacja>.+)", ex = listOf("otwórz YouTube", "uruchom Netflix", "otwórz aplikację Spotify")),
        p("help", "(?:pomoc|co moge powiedziec|co umiesz|przyklady)", "pokaz przyklady", ex = listOf("pomoc", "co mogę powiedzieć", "co umiesz")),
        // --- Odkrywanie ---
        p("mood.light", "(?:cos lekkiego|cos na luzie|cos latwego|cos milego)", "daj cos lekkiego", ex = listOf("coś lekkiego", "coś na luzie", "daj coś lekkiego")),
        p("mood.heavy", "(?:cos wymagajacego|cos ciezkiego|cos ambitnego|cos powaznego)", "daj cos wymagajacego", ex = listOf("coś wymagającego", "coś ambitnego", "daj coś wymagającego")),
        p("mood.gems", "(?:perelki|cos malo znanego|ukryte perelki|cos nieoczywistego)", "pokaz perelki", ex = listOf("perełki", "coś mało znanego", "ukryte perełki")),
        p("duration", "cos na (?<czas>.+)", "mam (?<czas>.+) na film", ex = listOf("coś na dwie godziny", "coś na godzinę", "mam 90 minut na film")),
        p("like.title", "cos (?:jak|podobnego do) (?<tytul>.+)", "podobne do (?<tytul>.+)", ex = listOf("coś jak Matrix", "coś podobnego do Wiedźmina", "podobne do Breaking Bad")),
        p("genre.pick", "(?:daj |wlacz |pokaz )?(?<gatunek>kryminal|komedie|dramat|horror|thriller|western|sf|romans|animacje)", "cos z gatunku (?<gatunek>.+)", ex = listOf("kryminał", "komedia", "włącz horror")),
        p("continue", "ogladaj dalej", "kontynuuj ogladanie", "wznow ogladanie", ex = listOf("oglądaj dalej", "kontynuuj oglądanie", "wznów oglądanie")),
        p("followed.news", "co nowego w obserwowanych", "nowe odcinki obserwowanych", "co nowego u obserwowanych", ex = listOf("co nowego w obserwowanych", "nowe odcinki obserwowanych", "co nowego u obserwowanych")),
        p("random.ep", "losowy odcinek (?<tytul>.+)", "wylosuj odcinek (?<tytul>.+)", "jakis odcinek (?<tytul>.+)", ex = listOf("losowy odcinek Przyjaciół", "wylosuj odcinek Klanu", "jakiś odcinek Simpsonów")),
        p("cast.who", "kto tu gra", "kto gra w tym filmie", "kto to gra", ex = listOf("kto tu gra", "kto gra w tym filmie", "kto to gra")),
        p("cast.show", "pokaz obsade", "obsada", "kto w obsadzie", ex = listOf("pokaż obsadę", "obsada", "kto w obsadzie")),
        p("trailer", "(?:zagraj|pokaz|wlacz) zwiastun", "zwiastun", ex = listOf("zagraj zwiastun", "pokaż zwiastun", "włącz zwiastun")),
    )
}
