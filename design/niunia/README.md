# Niunia — makiety i design system (Nuvio TV, styl Apple TV)

Stan: canvas Claude Design **v89** (20.11.2026). Ekran bazowy 960×540 dp (Android TV 1080p ÷ 2).

## Struktura
- `design-system/nuvio.css`: tokeny (`:root`) i wszystkie klasy komponentów
- `design-system/tokens.json`: tokeny (kolory, promienie, cienie, skala fokusu, font)
- `design-system/components.html` / `.jpg`: katalog komponentów zbudowany z tych klas
- `design-system/COMPONENTS.md`: lista klas komponentów
- `mockups/*.html`: wszystkie ekrany zbudowane z komponentów (otwórz w przeglądarce, działają offline); `mockups/canvas.json`: układ canvasu i notatki przepływów
- `screens/*.jpg`: render każdego ekranu (2×)
- `assets/`: obrazy TMDB i font Satoshi (TTF); `assets/manifest.json`: źródła plików
- `docs/WPIECIE.md` (PL, dla Claude Code) i `docs/SKIN.md` (EN): specyfikacja wdrożenia z historią zmian pod znacznikiem wdrożenia

Uwaga: 15 obrazów z wczesnych wersji nie było na dysku i w makietach zastąpiono je zastępczym tłem (lista w `assets/manifest.json` → `missing`).

## Home and navigation

### Home
[`mockups/Main.html`](mockups/Main.html)

![Home](screens/Main.jpg)

### Home, scrolled to rows
[`mockups/HomeRows.html`](mockups/HomeRows.html)

![Home, scrolled to rows](screens/HomeRows.jpg)

### Menu open
[`mockups/Sidebar.html`](mockups/Sidebar.html)

![Menu open](screens/Sidebar.jpg)

### Switch profile
[`mockups/Profiles.html`](mockups/Profiles.html)

![Switch profile](screens/Profiles.jpg)

## Series and movie detail

### Series detail
[`mockups/Detail.html`](mockups/Detail.html)

![Series detail](screens/Detail.jpg)

### Series, episodes
[`mockups/Episodes.html`](mockups/Episodes.html)

![Series, episodes](screens/Episodes.jpg)

### Series, cast and about
[`mockups/DetailMore.html`](mockups/DetailMore.html)

![Series, cast and about](screens/DetailMore.jpg)

### Episode detail
[`mockups/Episode.html`](mockups/Episode.html)

![Episode detail](screens/Episode.jpg)

### Movie detail
[`mockups/Movie.html`](mockups/Movie.html)

![Movie detail](screens/Movie.jpg)

## Search, people, library, settings

### Search
[`mockups/Search.html`](mockups/Search.html)

![Search](screens/Search.jpg)

### Search results
[`mockups/SearchResults.html`](mockups/SearchResults.html)

![Search results](screens/SearchResults.jpg)

### Person
[`mockups/Person.html`](mockups/Person.html)

![Person](screens/Person.jpg)

### Library, empty
[`mockups/Library.html`](mockups/Library.html)

![Library, empty](screens/Library.jpg)

### Ustawienia — wygląd dla trybu NOOB (najprostszy z 3 poziomów; Prosty i Zaawansowany bez zmian)
[`mockups/Settings.html`](mockups/Settings.html)

![Ustawienia — wygląd dla trybu NOOB (najprostszy z 3 poziomów; Prosty i Zaawansowany bez zmian)](screens/Settings.jpg)

## Live TV

### Live TV guide, Wszystkie (ostatnio oglądane)
[`mockups/LiveTV.html`](mockups/LiveTV.html)

![Live TV guide, Wszystkie (ostatnio oglądane)](screens/LiveTV.jpg)

### Live TV, Ulubione (nowe odcinki)
[`mockups/LiveTVFav.html`](mockups/LiveTVFav.html)

![Live TV, Ulubione (nowe odcinki)](screens/LiveTVFav.jpg)

### Live TV, channel row selected (timeline scrolls)
[`mockups/LiveTVRow.html`](mockups/LiveTVRow.html)

![Live TV, channel row selected (timeline scrolls)](screens/LiveTVRow.jpg)

### Live TV, Szukaj (drzewo kategorii + klawiatura + wyniki)
[`mockups/LiveTVSearch.html`](mockups/LiveTVSearch.html)

![Live TV, Szukaj (drzewo kategorii + klawiatura + wyniki)](screens/LiveTVSearch.jpg)

### Live TV, Odkrywaj (wszystkie zbiory programów)
[`mockups/LiveTVDiscover.html`](mockups/LiveTVDiscover.html)

![Live TV, Odkrywaj (wszystkie zbiory programów)](screens/LiveTVDiscover.jpg)

## Players

### Player - main, all video sources apart from IPTV and Youtube
[`mockups/Player.html`](mockups/Player.html)

![Player - main, all video sources apart from IPTV and Youtube](screens/Player.jpg)

### Player main, film: ▼▼ szczegóły (pola Meta)
[`mockups/PlayerFilmInfo.html`](mockups/PlayerFilmInfo.html)

![Player main, film: ▼▼ szczegóły (pola Meta)](screens/PlayerFilmInfo.jpg)

### Player - main, Napisy AI (auto)
[`mockups/PlayerSubs.html`](mockups/PlayerSubs.html)

![Player - main, Napisy AI (auto)](screens/PlayerSubs.jpg)

### Player main, serial: ▼ lista odcinków
[`mockups/PlayerSeries.html`](mockups/PlayerSeries.html)

![Player main, serial: ▼ lista odcinków](screens/PlayerSeries.jpg)

### Player main, serial: ▼▼ szczegóły (pola Meta)
[`mockups/PlayerSeriesInfo.html`](mockups/PlayerSeriesInfo.html)

![Player main, serial: ▼▼ szczegóły (pola Meta)](screens/PlayerSeriesInfo.jpg)

### Player YouTube (tylko materiały z YouTube)
[`mockups/PlayerLeanback.html`](mockups/PlayerLeanback.html)

![Player YouTube (tylko materiały z YouTube)](screens/PlayerLeanback.jpg)

### Player YouTube: ▼▼ szczegóły (pola YtVideoInfo)
[`mockups/PlayerLeanbackInfo.html`](mockups/PlayerLeanbackInfo.html)

![Player YouTube: ▼▼ szczegóły (pola YtVideoInfo)](screens/PlayerLeanbackInfo.jpg)

### Player IPTV 1/3: na żywo (czerwony)
[`mockups/PlayerLive.html`](mockups/PlayerLive.html)

![Player IPTV 1/3: na żywo (czerwony)](screens/PlayerLive.jpg)

### Player IPTV 2/3: z opóźnieniem (żółty)
[`mockups/PlayerLiveDelay.html`](mockups/PlayerLiveDelay.html)

![Player IPTV 2/3: z opóźnieniem (żółty)](screens/PlayerLiveDelay.jpg)

### Player IPTV 3/3: z archiwum (biały), fokus na przyciskach
[`mockups/PlayerLiveArchive.html`](mockups/PlayerLiveArchive.html)

![Player IPTV 3/3: z archiwum (biały), fokus na przyciskach](screens/PlayerLiveArchive.jpg)

### Player IPTV: ▼▼ szczegóły programu (pola EPG + strona programu)
[`mockups/PlayerLiveInfo.html`](mockups/PlayerLiveInfo.html)

![Player IPTV: ▼▼ szczegóły programu (pola EPG + strona programu)](screens/PlayerLiveInfo.jpg)

### Player IPTV, group (row raised)
[`mockups/PlayerLiveGroup.html`](mockups/PlayerLiveGroup.html)

![Player IPTV, group (row raised)](screens/PlayerLiveGroup.jpg)

## Components

### Poster variants
[`mockups/Posters.html`](mockups/Posters.html)

![Poster variants](screens/Posters.jpg)

## Full pages (TMDB content)

### Home, full page
[`mockups/HomePage.html`](mockups/HomePage.html)

![Home, full page](screens/HomePage.jpg)

### Series, full page
[`mockups/SeriesPage.html`](mockups/SeriesPage.html)

![Series, full page](screens/SeriesPage.jpg)

### Movie, full page
[`mockups/MoviePage.html`](mockups/MoviePage.html)

![Movie, full page](screens/MoviePage.jpg)

## Przepływy

Flows (press Play on any screen and click):
• Home pill → Menu → Search, Home, Live TV, Library, Settings
• Profile pill or menu header → Switch profile
• Home: Play → Player, (i) → Movie, › or Continue Watching → Home rows
• Home rows: focused episode → Series detail
• Series detail: resume → Player, row below → Episodes, doc button → Cast and about
• Episodes: episode 1 → Episode detail → Player; Trailers → Cast and about
• Cast: first person → Person → Series detail
• Search: key a → Results → Movie or Series
• Live TV: focused programme → Player
BACK has no click equivalent; each tab's top-left pill returns to the menu.
• Full pages: Home → Spotlight / Rings of Power → Series page; Odyssey → Movie page; Morfydd Clark → Person
• Player IPTV: cykliczny program → na dole inne odcinki serii (archiwum / wkrótce); niecykliczny → programy z innych kanałów z najmniejszej gałęzi (liścia) drzewa kategorii aplikacji, np. „Wiadomości” zamiast całych „Informacji i publicystyki”; gałąź wyżej tylko gdy liść pusty (▼ podnosi rząd). Zamiast tytułu logo programu: TVDB → TMDB → epg.ovh → playlista; tekst tylko bez logo. Logo kanału z epg.ovh.
• Player YouTube (układ jak odtwarzacz YouTube na TV) — tylko dla materiałów z YouTube (zakładka YouTube, zwiastuny); filmy i seriale z innych źródeł → Player, kanały IPTV → Player IPTV. Tytuł filmu w panelu po lewej, kanał · wyświetlenia · data; po prawej Subskrybuj, awatar kanału, napisy, 👍 👎, opis, zapisz na playliście, ustawienia; pasek z rozdziałami, pod nim czas, rozdział, długość; na dole wystają miniatury innych filmów.
• Player główny, Napisy: opcja „Napisy AI · polski” włącza się sama, gdy nie ma dobrych polskich napisów (brak, autosync zawiódł / słabe dopasowanie); dymek informuje o przełączeniu, przy pozycji etykieta AUTO; przełącznik „Napisy AI, gdy brak dobrych polskich”.
• Live TV: wybór w linii filtrów zmienia przewodnik i rząd kafelków na górze. Wszystkie → ostatnio oglądane z postępem; Ulubione → ulubione kanały/programy, znacznik „Nowy odcinek” (kafel) i NOWY (komórka EPG); kategoria → programy z tej kategorii (teraz i wkrótce).
• Live TV, wybrany rząd kanału w EPG: krawędzie linii programów przechodzą gradientem w tło (lewa i prawa), na nich strzałki ‹ › — sygnał, że oś czasu przewija się w bok; programy poza widokiem wystają spod gradientu. Na kafelkach zamiast LIVE / Archiwum tylko kropka: czerwona = na żywo, zielona = archiwum.
• Live TV → Odkrywaj (obok Dziś): wszystkie zbiory programów, które aplikacja już liczy: Ulubione (przypominają o nowych odcinkach), Dla ciebie, Nowe odcinki Twoich programów, Teraz na żywo, 3 najliczniejsze kategorie EPG teraz, Najnowsze w archiwum, Polecane, Popularne, Wkrótce, Ostatnio oglądane kanały i programy.