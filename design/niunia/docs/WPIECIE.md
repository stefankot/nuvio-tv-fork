# Skórka Niunia — wpięcie (etap 1–2: ekran główny, wyszukiwarka, biblioteka)

Przygotowane 19.11 w czacie z projektem canvas (bez buildu: na Macu nie było wtedy narzędzi). Pliki są w `.stage/niunia/` — build ich nie czyta, dopóki ktoś ich nie przeniesie.
Specyfikacja wyglądu: `~/Desktop/appletv/skin/SKIN.md`, wzór: `~/Desktop/appletv/skin/design/*.dc.html`.

## 1. Gałąź
`git pull`, potem nowa gałąź `skin-niunia` od bieżącej (`hybryda-zrodel`). Nie scalać do `main`.

## 2. Przenieś pliki (bez zmian w treści)
| z `.stage/niunia/` | do |
|---|---|
| `src/NiuniaSkin.kt` | `app/src/main/java/com/nuvio/tv/skin/niunia/NiuniaSkin.kt` |
| `src/NiuniaLabels.kt` | `app/src/main/java/com/nuvio/tv/skin/niunia/NiuniaLabels.kt` |
| `src/tokens/NiuniaTokens.kt` | `app/src/main/java/com/nuvio/tv/skin/niunia/tokens/NiuniaTokens.kt` |
| `src/components/NiuniaFocus.kt` | `app/src/main/java/com/nuvio/tv/skin/niunia/components/NiuniaFocus.kt` |
| `src/components/NiuniaArtColors.kt` | `app/src/main/java/com/nuvio/tv/skin/niunia/components/NiuniaArtColors.kt` |
| `src/components/NiuniaPoster.kt` | `app/src/main/java/com/nuvio/tv/skin/niunia/components/NiuniaPoster.kt` |
| `src/components/NiuniaButtons.kt` | `app/src/main/java/com/nuvio/tv/skin/niunia/components/NiuniaButtons.kt` |
| `src/home/NiuniaHome.kt` | `app/src/main/java/com/nuvio/tv/skin/niunia/home/NiuniaHome.kt` |
| `src/search/NiuniaSearch.kt` | `app/src/main/java/com/nuvio/tv/skin/niunia/search/NiuniaSearch.kt` |
| `src/library/NiuniaLibrary.kt` | `app/src/main/java/com/nuvio/tv/skin/niunia/library/NiuniaLibrary.kt` |
| `src/niunia_strings.xml` | `app/src/main/res/values/niunia_strings.xml` |

## 3. Trzy małe zmiany w `skin/api` i ustawieniach (pliki forka, nie bazy)
1. `skin/api/SkinId.kt`: dopisz `NIUNIA("niunia")` na końcu listy (po `KICIA("kicia")`) i w komentarzu „NIUNIA = projekt właściciela (canvas)”.
2. `skin/api/SkinRegistry.kt`: w `when` dopisz `SkinId.NIUNIA -> com.nuvio.tv.skin.niunia.NiuniaSkin`.
3. `ui/screens/settings/SkinSettingsItems.kt`: w `when (current)` dopisz `SkinId.NIUNIA -> "Niunia"` (inaczej `when` nie jest wyczerpujący). Opis przełącznika: „Kliknij, aby zmienić: Nuvio → Netflix → Mój projekt → Kicia → Niunia”.
Domyślna skórka zostaje bez zmian (Kicia). Kicia, Kit, Nuvio i Netflix — bez zmian.

## 4. Sprawdzenie
- `bash fork-tools/check.sh`, potem build jedną paczką przez `bash fork-tools/build.sh` (armeabi-v7a), test na JMGO wg ZASADY §5: Ustawienia → Wygląd → klikać do „Niunia” → ekran główny.
- Ekrany: główny, Szukaj (klawiatura po lewej, wyniki po prawej), Biblioteka (siatka / pusty stan).
- Logi `ForkNiunia`: `ekran główny: hero=…, dalej oglądaj=…, rzędy=…`, `wyszukiwarka: N wyników`, `biblioteka: N pozycji`, `próbka koloru: …` (błędy pobierania obrazu do koloru etykiety).
- Zrzuty: `~/Desktop/appletv/skin/screens/home.png`, `search.png`, `search-results.png`, `library.png`.

## 5. Decyzje (dopisać do `plan/DECYZJE.md`)
- Plakaty z dodatków mają zwykle tytuł w grafice → domyślnie wariant „baked” (stopka: tylko gatunek · ocena). Logo z TMDB na plakacie dopiero, gdy aplikacja będzie wiedzieć, że plakat jest bez napisów (np. plakat TMDB z językiem `null`).
- Ocena = średnia dostępnych serwisów; w danych aplikacji jest dziś tylko IMDb (`imdbRating`).
- Etykiety: „#n Dziś” w rzędach o nazwie z trend/popul/top, „Nowość” (premiera ≤ 45 dni), „Klasyka” (rok < 2000). Kolor etykiety = średnia pikseli spod niej, tekst czarny/biały wg jasności.
- Profile zostają ekranem Nuvio, dopóki Niunia nie obsłuży PIN-ów profili.
- Rozmycie plakatu tylko API 31+ (RenderEffect); na JMGO (Android 11) sam gradient.

## 6. Następne etapy (po zaakceptowaniu ekranu głównego)
Profile (`profiles`, z obsługą PIN jak w Kicii), nakładka odtwarzacza (`PlayerChrome`), YouTube.
**Odtwarzacz IPTV** (canvas: `PlayerLive`, `PlayerLiveGroup`; opis w SKIN.md): w `PlayerChrome` dla `playbackTimeline.isLive`. Logo programu zamiast tytułu, kolejność źródeł (decyzja właściciela 20.11): **TVDB → TMDB → epg.ovh (własny EPG) → playlista z programami**; tekst tylko, gdy żadne źródło nie ma logo. Log `ForkNiunia`: z którego źródła wzięto logo albo „bez logo”. **Logo kanału z epg.ovh przez istniejące `fork/tv/logo/ForgeChannelLogoUrl` / `ForgeLogoPack`** — bez nowego źródła. Dolny rząd: program cykliczny → inne odcinki serii (archiwum / wkrótce); niecykliczny → programy z innych kanałów z **najbardziej specyficznej grupy kategorii, czyli najmniejszej gałęzi (liścia) drzewa kategorii programów, które już jest w aplikacji** (decyzja właściciela 20.11). Użyj istniejącego `skin/kicia/livetv/ForgeEpgCategories.programmePlace(guideKey, p)` (`Place.branch` + `Place.leaf`, reguły `ForgeCategoryRules.PROGRAMMES`), bez nowego podziału. Przykład: „Fakty” → liść „Wiadomości” w gałęzi „Informacje i publicystyka”, więc rząd pokazuje Wiadomości, Wydarzenia, Panoramę, Teleexpress, a nie publicystykę ani reportaże. Do gałęzi nadrzędnej przejdź dopiero, gdy liść nie ma innych programów (teraz, w archiwum `LiveTvCatchupLinks.isPlayable` albo wkrótce, jak w `programmeTree`); „Inne” tylko na końcu; bez kategorii w przewodniku — brak rzędu. Tytuł rzędu = nazwa liścia (po przejściu wyżej — nazwa gałęzi). Log `ForkNiunia`: `zajawki: liść=…/gałąź=…, N programów`. Szczegóły serialu/filmu i Live TV nie mają dziś trasy skórki — wymagają nowych wpięć (`// Fork hook: skin`, wiersz w `fork-tools/hooks.tsv`).

**Odtwarzacz YouTube** (canvas: `PlayerLeanback`, opis w SKIN.md; decyzja właściciela 20.11): układ jak odtwarzacz YouTube na TV, **tylko dla materiałów z YouTube** — w `NiuniaPlayerChrome` warunek `YouTubeProgress.isYouTubeId(contentId)` albo strumień `isYouTube()`; filmy i seriale z innych źródeł zostają na zwykłym odtwarzaczu Niuni, kanały IPTV na odtwarzaczu IPTV. Nie ruszać panelu 1Tube Kicii (`skin/kicia/youtube`). Log `ForkNiunia`: `odtwarzacz: youtube|zwykły|iptv`.

**Odtwarzacz główny Niuni** (canvas: `Player`, decyzja właściciela 20.11): nad paskiem po prawej tylko ikony: napisy, jakość (napis w kółku „UHD”/„HD”/„SD” z grającego strumienia), dźwięk; przycisk prędkości usunięty. Panel boczny ma zakładki **Audio · Lektor · Napisy**. Zakładka „Lektor” korzysta z istniejącego `fork/lektor/LektorModule` (bez nowego silnika):
- „Głośność lektora” → `cycleVolume`, „Ściszanie filmu pod lektorem” → `cycleDuck`. Każde OK = następna wartość, zmiana od razu na grającym filmie (`applySettings`, mikser czyta co bufor), panel zostaje otwarty i pokazuje nową wartość (`volumeLabel`/`duckLabel`).
- Zakres wg właściciela: lektor **70 / 50 / 100 / 130%**, film **−6 / −3 / bez ściszania / −10 / −14 dB**. Uwaga: w kodzie `volumes` ma jeszcze 30% — usunąć 30 tylko po potwierdzeniu właściciela.
- „Głos lektora” → lista głosów z istniejącego wyboru (`ainarrator.LektorVoiceChoice`), „Ekspresyjność” → poziom emocji w intonacji (Brak / Lekka / Średnia / Mocna; dziś `LektorScript` daje stałe „lekkie zabarwienie” — poziom trzeba dodać jako ustawienie i przekazać do skryptu).
- W Ustawieniach każda zmiana pokazuje komunikat z nową wartością, np. „Głośność lektora: 50%” (`LektorController.say`).

**Napisy AI** (canvas: `PlayerSubs`, decyzja właściciela 20.11): w zakładce „Napisy” opcja „Napisy AI · polski”, która **włącza się sama, gdy nie ma polskich napisów dobrej jakości** — brak polskiej ścieżki albo AutoSync zawiódł / dopasowanie za słabe (ten sam próg co w `AutoSyncSettingsItems`/`AudioSyncSettings`). Dziś aplikacja ma tylko rozpoznawanie mowy do synchronizacji (`ui/screens/player/audiosync/asr/SherpaSpeechToText`, model pobierany raz) — generowania napisów z mowy jeszcze nie ma, to nowa funkcja (rozpoznanie mowy + tłumaczenie na polski). Ustawienie „Napisy AI, gdy brak dobrych polskich” (domyślnie Tak); ręczny wybór innej ścieżki wyłącza automat dla tego filmu. Komunikat w dymku AutoSync („Polskie napisy nie pasują do filmu — włączono Napisy AI”). Log `ForkNiunia`: `napisy AI: włączone (powód: brak|autosync|słabe dopasowanie)`.

**Live TV — linia filtrów** (canvas: `LiveTV`, decyzja właściciela 20.11): pod górnym rzędem programów jedna linia: „Wszystkie” (**fokus na wejściu**) → „Ulubione” → „Oglądaj dalej” → kategorie. **Kategorie wyłącznie z EPG, tak jak dziś liczy je aplikacja** (`skin/kicia/livetv/ForgeEpgCategories.channelTree`, reguły `ForgeCategoryRules`), nigdy z grup playlisty; puste gałęzie ukryte. Kolejność wg częstości wyboru — licznik użyć to nowe ustawienie (zapis przy każdym wyborze kategorii); bez historii: Informacje, Filmy i seriale, Sport, Dokumentalne, Dla dzieci, Muzyka, Styl życia, Regionalne, Zagraniczne, … Prawy górny róg, w linii i wysokości zwiniętej nawigacji „Live TV”, wszystkie z ikoną i napisem: „Szukaj” (na wejściu bez fokusu), „Dziś” (kalendarz), „Odkrywaj” (kompas). Dni nie są widoczne na stałe: lista dni otwiera się dopiero po naciśnięciu „Dziś”; po wyborze przycisk pokazuje wybrany dzień („Jutro”, „Pt 21”). Log `ForkNiunia`: `live tv: kategorie=N, kolejność=…`.

**Live TV — zachowanie filtrów** (canvas: `LiveTV`, `LiveTVFav`; decyzja właściciela 20.11): wybór w linii filtrów zmienia **jednocześnie** przewodnik i rząd kafelków na górze.
- „Wszystkie” → kafelki: ostatnio oglądane z postępem (LIVE gdy dalej leci, „Archiwum” gdy już się skończył); przewodnik: wszystkie kanały.
- „Ulubione” → kafelki: ulubione kanały/programy; program z odcinkiem jeszcze nieobejrzanym ma znacznik **„Nowy odcinek”** na kafelku i „NOWY” w komórce przewodnika; przewodnik: tylko ulubione kanały.
- „Oglądaj dalej” → niedokończone programy z archiwum.
- Kategoria (z EPG) → kafelki: programy tej kategorii teraz i wkrótce; przewodnik: kanały tej kategorii.
Log `ForkNiunia`: `live tv: filtr=…, kafelki=N, kanały=N`.

**Kropka statusu zamiast napisów** (decyzja właściciela 20.11, cała aplikacja Niuni): na kafelkach nie ma słów „LIVE”, „Na żywo” ani „Archiwum” — tylko kropka 10 dp w lewym górnym rogu, bez obwódki: czerwona (`NiuniaColors.Live`) = leci teraz, zielona (`NiuniaColors.Archive`) = jest w archiwum. Opis dla czytnika ekranu (`contentDescription`) zostaje słowem. Jeden wspólny komponent `NiuniaStatusDot` w `components/`, używany w Live TV, odtwarzaczu IPTV, rzędach ekranu głównego i plakatach. Inne znaczniki („Nowy odcinek”, „Wkrótce”, godziny) zostają tekstem. Wyjątek: opis wybranego programu na dole Live TV ma czerwony napis „LIVE”, nie kropkę.

**EPG — wybrany rząd kanału** (canvas: `LiveTVRow`, decyzja właściciela 20.11): gdy fokus jest w rzędzie kanału, oba końce linii programów przechodzą gradientem w tło (64 dp, kolor tła → przezroczysty), a na gradientach są strzałki ‹ i › pokazujące, że oś czasu przewija się w bok (strzałka znika, gdy dalej nic nie ma). Gradienty leżą wewnątrz okna przewodnika (te same krawędzie co w innych rzędach — okno czasu jest wspólne dla wszystkich rzędów), na przyciętych końcach programów; rząd nie wychodzi poza okno. Kafelek kanału w tym rzędzie podświetlony. Pozostałe rzędy bez gradientu.

**Live TV → Odkrywaj** (canvas: `LiveTVDiscover`, decyzja właściciela 20.11): ekran z rzędami **wszystkich zbiorów programów, które aplikacja już liczy** — bez nowych źródeł danych, tylko nowy układ: „Dla ciebie” (`fork/tv/ForYouQueue`), „Nowe odcinki Twoich programów” (`EpisodeAlerts`, jak w `ForgeTvBrowse`), „Teraz na żywo”, 3 najliczniejsze kategorie EPG teraz (`ForgeEpgCategories.programmeBranchName`, jak `groupRows` w `ForgeTvBrowse`), „Najnowsze w archiwum” / „Polecane” / „Popularne” / „Wkrótce” (sortowania `ForgeProgrammeSort` z `ForgeProgrammeView`), „Ulubione” (`Following` / ulubione programy), „Ostatnio oglądane kanały” (`RecentChannels`), „Ostatnio oglądane programy” (`RecentlyViewed`). Kolejność startowa (decyzja właściciela 20.11): Ulubione · Dla ciebie · Teraz na żywo · Nowe odcinki Twoich programów · Ostatnio oglądane programy · Ostatnio oglądane kanały · 3 kategorie EPG · Polecane · Popularne · Wkrótce · Najnowsze w archiwum. **W IPTV nie ma „Przypomnij mi”** (`fork/tv/Reminders` nie jest używane w Niuni) — o nowych odcinkach przypominają **Ulubione** (programy z „+”, `FORGE_PROG_FAVOURITES`/`Following`): w rzędzie Ulubione najpierw kafelki z nowym odcinkiem („Nowy odcinek”, ten sam znacznik co w `LiveTVFav`). **Kolejność uczy się ze statystyki użycia**: licznik na rząd (OK na kafelku, który uruchamia program = użycie), ze starzeniem (np. waga ×0,9 na dzień), zapis w ustawieniach profilu; rzędy sortowane malejąco wg wyniku, przy remisie kolejność startowa. Przestawienie tylko przy wejściu na ekran, nigdy w trakcie. Log `ForkNiunia`: `odkrywaj: kolejność=…`. Puste rzędy ukryte. Log `ForkNiunia`: `odkrywaj: rzędy=…`.

**Live TV → Szukaj** (canvas: `LiveTVSearch`, decyzja właściciela 20.11): klawiatura ekranowa jak na głównym ekranie Szukaj Niuni (`NiuniaSearch`), obok **całe drzewo kategorii** (`ForgeEpgCategories`: gałęzie i liście z `ForgeCategoryRules.PROGRAMMES` + trzeci poziom = surowe kategorie przewodnika zapisane przez `record`, ~600), z liczbą programów przy każdej; wpisywanie filtruje drzewo (pasujące kategorie + ich rodzice). Po prawej wyniki: programy (tytuł) i kanały (nazwa), przełącznik Rzędy / Siatka (zapamiętany). Istniejące szukanie Kicii: `skin/kicia/livetv/ForgeTvSearch.kt` — użyć jego logiki dopasowania (małe litery, bez polskich znaków jak `ForgeProgrammeView`), nie pisać nowej. Log `ForkNiunia`: `szukaj tv: „…” → programy=N, kanały=N, kategorie=N`.

**Oznaczenie serialu w IPTV** (decyzja właściciela 20.11): każdy kafelek serialu w Live TV (rzędy, Ulubione, Odkrywaj, Szukaj) ma pod spodem dyskretny „stos” — dwie warstwy węższe o 8/16 dp, niżej o 5/10 dp, przyciemnione (0,55 / 0,38), z tym samym obrazkiem co kafelek: środkowa warstwa odwrócona do góry nogami, dolna obrócona o 90° i pokazuje tylko połowę wysokości obrazka (obrazek dwa razy wyższy niż warstwa, wyśrodkowany). Bez napisu. Serial rozpoznać istniejącym `fork/tv/ProgramType` (`SERIES`; `BAND` — pasmo — też jako serial? do potwierdzenia u właściciela). Wspólny komponent `NiuniaSeriesStack` w `components/`.

**IPTV — poprawki UX (20.11)**: podpisy kafelków „program · kanał” wszędzie (też stopka przewodnika); nazwa kanału w jednej linii z wielokropkiem; podziałka co pół godziny (słabsza); w wybranym rzędzie tytuł przyciętego programu z fokusem zaczyna się za strzałką (40 dp); w Szukaj tylko jeden fokus naraz (kategoria w drzewie = zaznaczona, nie fokus), w polu liczniki wyników zamiast podpowiedzi; w odtwarzaczu IPTV przycisk „Od początku” obok pauzy (istniejące `ekrany/DelayedPlayback` / `MiniSlot.RESTART`), „+” = Dodaj do ulubionych; tytuł rzędu grupy „Wiadomości · na innych kanałach”.

**EPG — układ jak w przewodnikach nadawców** (canvas: `LiveTV`, `LiveTVFav`, `LiveTVRow`; decyzja właściciela 20.11): kolumna kanału = numer + logo (epg.ovh), komórki programów w jednej linii (sam tytuł), rzędy dochodzą do prawej krawędzi ekranu, program trwający ma ikonę ▶, fokus = biała obwódka 2 dp (nie białe wypełnienie), podziałka co 30 min, ok. 1 h 40 min w oknie, czerwona linia „teraz” ze znacznikiem. (Wycofane 20.11: opis wybranego programu zamiast kafelków.) Rząd kafelków i linia filtrów zostają zawsze — po wyborze kategorii, Ulubionych itd. przewodnik pokazuje pasujące kanały, a kafelki u góry pasujące programy; opis programu z fokusem jest w linii pod przewodnikiem. Dawny pomysł, nieużywany: gdy fokus jest w siatce, górny rząd kafelków i linia filtrów ustępują miejsca opisowi wybranego programu (tytuł, LIVE, godziny, kanał, wiek, HD, „OK — od początku”, 2 linie opisu) i **podglądowi na żywo w prawym górnym rogu** — wyciszony strumień kanału wybranego programu (drugi, mały odtwarzacz; na słabych pudełkach jak JMGO sprawdzić wydajność, w razie problemów kadr co kilka sekund), ostry obraz wygaszony maską od lewej i od dołu + pod nim rozmyta, lekko powiększona kopia, żeby obraz wtapiał się w tło (rozmycie API 31+; na Androidzie 11 sama maska); napis „● Na żywo · kanał”. Program nie na żywo (archiwum / później) → nieruchomy kadr bez napisu. Podgląd startuje po ~600 ms bez ruchu fokusu, żeby przewijanie siatki nie przełączało strumieni co chwilę; zostaje tylko chip aktywnego filtra. Strzałki ‹ › na końcach wybranego rzędu. Wcześniejsze gradienty `.tfade` zastąpione tym układem.

**Odtwarzacz IPTV — 3 stany** (canvas: `PlayerLive`, `PlayerLiveDelay`, `PlayerLiveArchive`; decyzja właściciela 20.11):
- **Na żywo** (czerwony `NiuniaColors.Live`), **z opóźnieniem** = przesunięcie programu, który wciąż leci (żółty, nowy token `Delay` #f5c542; istniejące `fork/tv/ekrany/DelayedPlayback` `LiveMode.DELAYED`), **z archiwum** (biały). Kolor stanu: etykieta przed godzinami („LIVE” / „−12:40” / „ARCHIWUM”), wypełnienie paska, napis pod znacznikiem odtwarzania.
- Z opóźnieniem: pasek do miejsca odtwarzania + jaśniejszy bufor do krawędzi „na żywo” (czerwona kreska), przyciski −30 s / Od początku / Pauza / +30 s / „● Na żywo”. Archiwum: pasek z gałką, minął / zostało, bez „Na żywo”. Na żywo: Od początku, Pauza.
- **Pilot: ▲/▼ zmienia kanał od razu w trakcie odtwarzania, bez przechodzenia gdziekolwiek** (to samo co dziś w odtwarzaczu Kicii, jeśli jest — nie dublować obsługi klawiszy); dopiero ► przenosi fokus na przyciski; ▼ z przycisków podnosi dolny rząd. Podpowiedź pod paskiem „▲▼ kanał · ► przyciski” (znika po kilku sekundach).
- Log `ForkNiunia`: `iptv: stan=live|delay|archive, przesunięcie=…`.

**Odkrywaj — mniej tekstu** (decyzja właściciela 20.11): bez dopisków przy tytułach rzędów, krótkie tytuły rzędów (Nowe odcinki, Ostatnio oglądane, Ostatnie kanały, Informacje, Archiwum), na kafelku sam tytuł programu (godzina tylko dla programów, które dopiero będą), znaczniki jednym słowem: „Nowy”, „Wznów”, „Wkrótce”.

**Odtwarzacz IPTV — uproszczenie** (decyzja właściciela 20.11): bez przycisków −30 s / +30 s — **przewijanie na pasku postępu** (fokus na pasku, ◄/► krok, przytrzymanie = szybciej). Na środku tylko Od początku i Pauza (+ „● Na żywo” przy opóźnieniu). Pilot: ▲▼ kanał od razu; ► wchodzi w sterowanie (fokus na pasek), ◄► na pasku przewija, ▼ z paska na przyciski. Blok kanału: **samo logo kanału z przezroczystością** (epg.ovh przez `ForgeChannelLogoUrl`/`ForgeLogoPack`), bez kafelka i bez zaślepki jak ikona aplikacji; gdy logo brak — nazwa kanału pogrubionym tekstem.

**Jeden pasek postępu we wszystkich odtwarzaczach** (decyzja właściciela 20.11): jak w głównym odtwarzaczu — czas · pasek 8 dp zaokrąglony z gałką 16 dp · czas, w jednej linii; jeden komponent `NiuniaProgressLine(state)`: biały (główny, archiwum), czerwony (na żywo, YouTube), żółty (opóźnienie, z buforem i czerwoną kreską „na żywo”). Rozdziały YouTube jako cienkie przerwy w tym samym pasku.

**Odtwarzacz IPTV — blok kanału** (decyzja właściciela 20.11): bez podpowiedzi o działaniu przycisków pilota na ekranie. Strzałki ▲/▼ większe (26 dp) i tuż przy logo kanału (bez odstępu).

**Odtwarzacze — ▼ i rząd pod spodem** (decyzja właściciela 20.11): każdy odtwarzacz ma pod spodem **4 filmy** (wystają u dołu, ▼ je podnosi). IPTV i YouTube: najpierw powiązane (jak teraz). Główny odtwarzacz, film: 4 podobne. **Główny odtwarzacz, serial** (canvas `PlayerSeries`, `PlayerSeriesInfo`): ▼ = lista odcinków sezonu (bieżący „Teraz”, postęp na każdym); kolejne ▼ = obraz bardzo mocno rozmyty i szczegóły serialu jak na stronie serialu (logo, opis, obsada, sezony, odcinki); BACK cofa krok po kroku. Rozmycie API 31+, na Androidzie 11 mocne przyciemnienie.
**Strona serialu / filmu pod hero** (canvas `SeriesPage`, `MoviePage`): tło pod hero = obraz z hero bardzo rozmyty, na górze tej części logo programu (przypięte przy przewijaniu), rzędy jak dotąd.

**Odtwarzacz YouTube — tytuł jak w głównym** (decyzja 20.11): mały szary napis wersalikami = nazwa kanału, tytuł 24 pogrubiony do 2 linii (bez szklanego panelu), szara linia „wyświetlenia · kiedy · długość” + znacznik jakości.

**Główny odtwarzacz — bez przycisków Pauza i Informacje** (decyzja 20.11): pauza = OK na pilocie, szczegóły = ▼. Zostają tylko napisy · jakość · dźwięk nad paskiem po prawej.

**Strona serialu / filmu — przejście z hero** (decyzja 20.11): bez szarego gradientu na dole hero; pierwsze ▼ od razu chowa hero i pokazuje stronę na bardzo rozmytym tle (bez przejściowego pasa). Logo programu na środku, większe (do 64 dp wysokości), ok. 40 dp odstępu pod nim do przycisków sezonów / pierwszego rzędu.

**Poprawka po wdrożeniu — etykieta na plakacie** (zgłoszenie właściciela 20.11: zamiast „Sci-Fi” jest „Science Fiction”):
- `skin/niunia/NiuniaLabels.kt`, `meta()`: zamiast `item.genres.firstOrNull()` użyj `shortGenre(item.genres.firstOrNull())`:
```kotlin
private val SHORT_GENRE = mapOf(
    "science fiction" to "Sci-Fi", "sci-fi & fantasy" to "Sci-Fi", "action & adventure" to "Action",
    "war & politics" to "War", "documentary" to "Docu", "tv movie" to "TV Film",
)
fun shortGenre(g: String?): String? = g?.trim()?.let { SHORT_GENRE[it.lowercase()] ?: it.substringBefore(" & ") }
```
- `skin/niunia/tokens/NiuniaTokens.kt`: `NiuniaText.PosterMeta` → `fontSize = 12.sp, lineHeight = 16.sp`, kolor `0xE6FFFFFF`, cień (`shadow = Shadow(Color(0x80000000), blurRadius = 4f)`); w wariancie dużym (`large`) 14.sp / 18.sp.
- Sprawdzenie: zrzut ekranu głównego na JMGO, etykieta „Sci-Fi · ★ 7.5” mieści się w jednej linii na plakacie 130 dp.

**Wyszukiwarka Niuni — wyniki** (canvas `SearchResults`, decyzja 20.11): zakładki **Wszystko · Filmy · Seriale · TV · YouTube · Osoby** (TV = programy i kanały z przewodnika jak w Live TV → Szukaj; YouTube = istniejące wyszukiwanie zakładki YouTube). **Pod klawiaturą** lista do 3 fraz: przy wpisanej frazie „Podpowiedzi” (dopełnienia z tytułów w wynikach, OK wstawia frazę), przy pustym polu „Ostatnio wyszukiwane” (istniejąca historia wyszukiwań `SearchViewModel` / recent searches, ikona zegara). Kafelek wyniku: rodzaj na początku szarej linii (Film / Serial / TV / YouTube / Osoba); TV z kropką statusu, YouTube z miniaturą 16:9, osoby z inicjałami. Klawiatura po polsku.

**Czcionka Satoshi wszędzie** (zgłoszenie właściciela 20.11): pliki Satoshi mają polskie litery, ale nie mają znaków ★ ▼ ► ◄ — te spadały na czcionkę systemową. W Niuni: gwiazdka oceny i strzałki jako ikony wektorowe (np. `Icons.Filled.Star`, rozmiar = wysokość tekstu), nigdy jako znak w tekście; `NiuniaLabels.meta()` zwraca gatunek i liczbę osobno, gwiazdkę rysuje `NiuniaPoster`. Każdy `Text` w `skin/niunia` korzysta z `NiuniaText` (rodzina Satoshi 500/700) — sprawdzić grepem, czy nie ma `Text(` bez stylu z `NiuniaText` ani `MaterialTheme.typography`. Grubość tylko 500 i 700.
**Live TV → Szukaj — podpowiedzi** pod klawiaturą jak w głównej wyszukiwarce (dopełnienia z tytułów programów, nazw kanałów i kategorii; przy pustym polu „Ostatnio wyszukiwane”).

**Przegląd UX/UI wszystkich ekranów** (decyzja właściciela 20.11):
- **Jeden język — polski** w całej Niuni: wszystkie napisy interfejsu w `niunia_strings.xml` (nie w kodzie), także nawigacja, nazwy rzędów, przyciski, metadane („2 godz. 53 min”, daty „2 wrz 2022” przez `DateTimeFormatter` z `Locale("pl")`), gatunki po polsku (TMDB z `language=pl-PL`, potem `shortGenre`).
- **Jeden fokus na ekran**; pełne strony: fokus na głównym przycisku hero; odtwarzacz YouTube startuje z fokusem na pasku postępu (grubszy pasek, większa gałka z poświatą).
- **Pusta biblioteka**: jeden przycisk z fokusem „Przeglądaj filmy i seriale” (→ ekran główny).
- **Ustawienia Niuni — tylko poziom NOOB** (decyzja właściciela 20.11): układ z canvasu (`Settings`: obrazek profilu po lewej, jedna lista po prawej) dotyczy wyłącznie najprostszego z 3 poziomów ustawień (`UiLevel.SIMPLE`, `fork/settings/SimpleSettingsScreen`). Poziomy Prosty i Zaawansowany zostają bez zmian. Pozycje listy prowadzą do istniejących ekranów forka.

**Odstęp pasek ↔ przyciski** (decyzja 20.11): we wszystkich odtwarzaczach 30 dp między paskiem postępu a rzędem przycisków (główny i YouTube — przyciski nad paskiem, IPTV — pod nim). Jeden token `NiuniaDims.PlayerGap = 30.dp`.

**Szczegóły w odtwarzaczach (▼▼)** (canvas `PlayerFilmInfo`, `PlayerSeriesInfo`, `PlayerLeanbackInfo`, `PlayerLiveInfo`; decyzja 20.11): pierwsze ▼ podnosi rząd pod odtwarzaczem, drugie — szczegóły na mocnym rozmyciu. Tylko istniejące pola:
- film/serial: `domain/model/Meta` (name/logo, genres, releaseInfo, runtime, ageRating, imdbRating, description, director, writer, productionCompanies, country, language, trailers/trailerYtIds, castMembers, videos);
- YouTube: `youtube/YouTubePlayerViewModel.YtVideoInfo` (author, authorImage, subscribers, viewCount, publishedDate, likeCount, subscribed, chapters, commentsKey/newestCommentsKey) — bez opisu, bo go tu nie ma;
- IPTV: `LiveTvProgramme` (title, start/stop, description, image, episode) + `fork/tv/ekrany/ProgramPage` (typ, Obserwuj, Najbliższe emisje, sezony/odcinki) + `skin/kicia/livetv/ForgeProgramDetail` (archiwum N dni, Obsada i twórcy) + kategoria z `ForgeEpgCategories.programmePlace`.

---
## Dawny znacznik — 20.11.2026, 11:27 (canvas: wersja 82, id `1795170339-99bc`); aktualny znacznik jest na końcu pliku
## Po wdrożeniu
Zmiany dopisane po znaczniku (najnowsze na dole). Claude Code: „zrób wszystko spod znacznika”, potem przenieś znacznik niżej z nową datą i numerem wersji canvasu.

### 1. Strony szczegółów playerów (▼▼) — układ jak strona szczegółów serialu (canvas v83)
Dotyczy: PlayerFilmInfo, PlayerSeriesInfo, PlayerLeanbackInfo (YouTube), PlayerLiveInfo (IPTV).
- Logo programu **wyśrodkowane** u góry (top 28dp, max 64×320dp, PNG z przezroczystością; brak logo → tytuł tekstem).
- Pod logo wyśrodkowana kolumna (marginesy 160dp): linia meta, opis max 2 linie, linia „Obsada … · Twórcy …”; w serialu pigułki sezonów. Sekcja pod pigułkami nazywa się **„Odcinki”**.
- YouTube: wyśrodkowany nagłówek (KANAŁ FILMOWY, tytuł, meta, karta kanału + Subskrybuj), dwa panele „Rozdziały” / „Komentarze”, rząd „Więcej z kanału”.
- IPTV: nagłówek LIVE · godziny · kanał · wiek · typ/kategoria EPG; opis z EPG; „Obserwuj” + „Dostępne od początku (archiwum N dni)”; rząd odcinków sezonu z kropkami statusu.
- **Odstępy sekcji (wszędzie):** tytuł sekcji → rząd 34dp; po rzędzie 36dp do następnego tytułu sekcji (naprawia „Obsada i twórcy” za blisko poprzedniego rzędu).
- **„Najbliższe emisje”**: bez ramki/panelu wokół — osobny kafelek (rozmiar karty rzędu, tło surface) dla każdej emisji: dzień wielkimi literami (DZIŚ / JUTRO / SOB…), godzina duża (28sp bold), na dole kanał (bold) + etykieta po prawej (Wkrótce / Powtórka / Nowy odcinek, może być pusta). Kafelki w zwykłym rzędzie przewijanym poziomo, fokus jak na kartach.

### 2. Hero bez szarego gradientu + przewinięcie o pełny ekran (canvas v84)
Dotyczy: Główna (także pełna strona Główna), Menu, Profile, Szczegóły serialu, Szczegóły odcinka, Szczegóły filmu.
- Usuń szary gradient na dole hero (`scrim-b` → w kodzie dolny gradient Brush.verticalGradient do koloru tła pod hero). Zostaje tylko lewy scrim pod tekstem.
- Fokus z hero na pierwszy element pod nim (▼) przewija o **100% wysokości ekranu** (jedna strona w dół): hero znika całkowicie, nie zostaje pasek obrazka ani przyciski hero. Rzędy zaczynają się od góry (pierwszy tytuł rzędu 40dp od góry). ▲ z pierwszego rzędu wraca do hero tym samym skokiem.

### 3. ZASADA: brak centrowania w poziomie (canvas v86) — nadpisuje pkt 1
- W całej aplikacji **nic nie jest centrowane poziomo** — wszystko wyrównane do lewej krawędzi treści (40dp). **Jedyny wyjątek: logo programu na stronach szczegółów** (szczegóły serialu/odcinków, pełne strony serialu/filmu, strony ▼▼ playerów).
- Strony ▼▼ playerów: logo nadal na środku, ale linia meta, opis, obsada/twórcy, sezony, przycisk Obserwuj — od lewej (kolumna 40dp, szer. maks. 680dp). Strona YouTube: cały nagłówek (kanał, tytuł, meta, karta kanału) od lewej.
- Player IPTV (3 stany + grupa): przyciski Od początku / Pauza / Na żywo od lewej, zaraz za przełącznikiem kanałów (x=170dp), nie na środku ekranu. Przełącznik kanałów: strzałki i logo kanału wyrównane do lewej.
- Player YouTube: nazwa rozdziału pod paskiem od lewej. Player serial (▼): podpowiedź „▼ Szczegóły serialu” od lewej.
- Kropki karuzeli hero (Główna, Menu, Profile) od lewej pod przyciskami (x=40dp), nie na środku.
- Biblioteka pusta: komunikat i przycisk od lewej.
- Komponent osoby (Obsada i twórcy): kółko, imię i rola wyrównane do lewej.
- Wewnątrz komponentów bez zmian: plakaty (stopka plakatu, zakładka u góry), etykieta LIVE nad znacznikiem paska postępu, napisy na filmie.

### 4. Strony szczegółów playerów (▼▼) — nowa architektura informacji (canvas v88) — zastępuje pkt 1 (poza logo i odstępami)
Wspólny szkielet dla 4 stron (przewijana strona pionowo, rozmyte tło z obrazu programu):
1. **Logo** na środku u góry (jedyny centrowany element). YouTube nie ma logo → u góry od lewej etykieta „YOUTUBE” i tytuł tekstem.
2. **Nagłówek** od lewej (40dp, szer. 680dp): linia meta (typ · gatunki · rok · czas · wiek · jakość · ocena) → opis max 3 linie → **przyciski akcji** (sm). Bez linii „Obsada/Twórcy” w nagłówku (jest w sekcji niżej).
3. **Sekcje w kolejności: najpierw co obejrzeć, potem ludzie, potem podobne, na końcu fakty.** Odstępy: tytuł → treść 34dp, po treści 36dp. Fokus po wejściu: pierwszy element pierwszej sekcji.

Per typ:
- **Film:** akcje [+ Do biblioteki] [▶ Zwiastun] (zwiastuny jako przycisk, nie rząd). Sekcje: Obsada i twórcy → Podobne → Informacje.
- **Serial:** akcje jak film. Sekcje: Odcinki (pigułki sezonów w linii tytułu sekcji, obok „Odcinki”) → Obsada i twórcy → Podobne → Informacje.
- **YouTube:** nagłówek: tytuł, meta, karta kanału + [Subskrybuj] + [👍 liczba]. Sekcje: Rozdziały (kafelki: czas, nazwa, „teraz” na bieżącym; OK przewija film do rozdziału) → Więcej z kanału → Komentarze (rząd kart komentarzy + [Wszystkie]). Brak opisu (YtVideoInfo go nie ma).
- **IPTV:** nagłówek: [LIVE] godziny · kanał · wiek / linia typ · odcinek · kategoria EPG / opis EPG / akcje [🔔 Obserwuj] [↺ Od początku] + „● Archiwum 7 dni”. Sekcje: Odcinki · Sezon N (kropki statusu) → Najbliższe emisje (kafelki) → Obsada i twórcy → Informacje.
- **Informacje** = siatka 3 kolumn, etykieta wielkimi literami 12sp + wartość 14sp. Film: Reżyseria, Scenariusz, Produkcja, Kraj, Język, Premiera. Serial: Twórcy, Sieć, Produkcja, Kraj, Język, Premiera · liczba odcinków. IPTV: Kanał (nr · nazwa), Kategoria EPG, Emisja, Archiwum, Wiek, Źródło danych. Pola puste pomijać.

### 6. Canvas przeniesiony na nowe konto (canvas v4, id `1795173663-a608`, nowy artefakt)
Dotyczy tylko makiety — w kodzie nic do zmiany.
- Canvas „Nuvio TV Apple TV Style App” odtworzony z `.stage/niunia/canvas-export/` (35 ekranów, 7 grup, notatki bez zmian). Odtąd numery wersji canvasu liczone od nowa w tym artefakcie.
- Satoshi 500/700 wgrane jako TTF (`format("truetype")`), nie WOFF2.
- 15 obrazków, których brakowało w eksporcie, zastąpione podobnymi z `appletv/content/img`: kafle 16:9 → backdropy (VisionQuest, The Beast, Guarding Stars, Other Mommy, Avengers: Doomsday); kadry odcinków Rings of Power → `tv-84773-s1e1…s1e5`; plakaty 2:3 → Jailer 2, NAZA, You Can See Everything; logo → Modern Family (`tv-1421-logo`), Teach You a Lesson (`tv-276161-logo`). To tylko treść przykładowa.

### 7. Wdrożenie pkt 1–4 w kodzie (canvas v4, id `1795173663-a608`) — 20.11, w drzewie roboczym, NIEZBUDOWANE i bez commita
- Zrobione w `skin/niunia`: hero bez dolnego gradientu i na cały ekran, zejście ▼ o 100% ekranu (Główna, strona filmu/serialu); pod hero na stronie filmu/serialu rozmyte tło (API 31+, na Androidzie 11 przyciemnienie) i logo na środku; odstępy rzędów 34/36 dp; brak centrowania (pusta biblioteka, pusty Live TV, osoby, karty profili, okno PIN, kolumna przycisku Odtwórz); kropka statusu `NiuniaStatusDot` (kafelki „Teraz w telewizji”); odtwarzacz: ▼ z przycisków = rząd odcinków 200×113 (4 + kawałek piątego, wystaje 22 dp), ▼▼ = szczegóły (`player/NiuniaPlayerInfo.kt`), BACK krok po kroku, odstęp pasek ↔ przyciski 30 dp. Nowe: `tokens/NiuniaLayout.kt`, `components/NiuniaStatusDot.kt`, `res/values/niunia_deploy_strings.xml`.
- Decyzje: film w odtwarzaczu nie ma rzędu „Podobne” (odtwarzacz nie ma tych danych) — ▼ od razu otwiera szczegóły. W szczegółach odtwarzacza bez przycisków „Do biblioteki” / „Zwiastun” (brak danych w `PlayerUiState`); obsada jako inicjały, bez zdjęć. Strony ▼▼ YouTube i IPTV czekają na odtwarzacze YouTube/IPTV Niuni (dziś nakładka domyślna).

### 8. Odtwarzacz YouTube Niuni (canvas v4: `PlayerLeanback`, `PlayerLeanbackInfo`) — 20.11, wątek odtwarzaczy
- Plik `skin/niunia/player/NiuniaYouTubePlayer.kt`, wpięcie w `NiuniaSkin.PlayerChrome` (bez nowego wpięcia w bazę). Włączany tylko dla YouTube: id treści `youtube:` (`YouTubeProgress.isYouTubeId`) albo sesja YouTube trasy odtwarzacza (`ForgeYouTubeHost.session`).
- Układ jak makieta: od lewej kanał wersalikami, tytuł 24 do 2 linii, „wyświetlenia · kiedy · długość” + znacznik jakości (tylko gdy nazwa źródła go podaje); po prawej [Subskrybuj], awatar kanału, okrągłe: napisy, podoba się, nie podoba się, opis, zapisz, ustawienia; pasek przez całą szerokość, czerwony, z przerwami rozdziałów; nazwa rozdziału pod paskiem od lewej; na dole wystają miniatury (bez fokusu 55 %).
- Fokus startuje na pasku. ▲ z paska → przyciski, ▼ z paska → rząd „Więcej z kanału” (4 kafle 16:9 + kawałek piątego, podniesione tak, że do dołu zostaje 36 dp), ▼ z rzędu → szczegóły. Wstecz krok po kroku.
- „Więcej z kanału” = propozycje YouTube dla filmu (`YouTubePlayerViewModel.loadRelated`): najpierw filmy tego samego kanału, potem pozostałe (bez Shorts i kanałów). YouTube nie daje tu osobnej listy filmów kanału.
- Szczegóły (▼▼ i przycisk „Opis”): YOUTUBE, tytuł, meta, karta kanału + [Subskrybuj] + [👍 liczba]; Rozdziały (kafle: czas, nazwa, „teraz”; OK przewija film), Więcej z kanału, Komentarze (karty + [Wszystkie]). „Zapisz na playliście” i „Wszystkie” komentarze otwierają istniejący panel 1Tube; „Ustawienia” = panel „Więcej” Niuni. Panel 1Tube nie otwiera się już sam na ▼ w Niuni.

### 9. Odtwarzacz IPTV Niuni (canvas v4: `PlayerLive`, `PlayerLiveDelay`, `PlayerLiveArchive`, `PlayerLiveGroup`, `PlayerLiveInfo`)
- Plik `skin/niunia/live/NiuniaLivePlayer.kt`. Rysowany zamiast karty MiniGuide Kicii (jedna linia w `ForgeMiniGuideCard`), klawisze prowadzi `NiuniaLiveNav` (jedna linia w `ForgeMiniGuideNav.onKey`) — bez nowych wpięć w bazę; pozostałe zachowania Live TV (zapping, pauza, archiwum, przewijanie) zostają z Reshaped/Kicii.
- 3 stany: na żywo (czerwony LIVE), z opóźnieniem po pauzie (żółty „−mm:ss”, jaśniejszy bufor do „teraz”, czerwona kreska krawędzi i mały LIVE), archiwum (biały ARCHIWUM, czasy „upłynęło / −zostało”). Kolor w znaczniku, wypełnieniu paska i napisie pod gałką.
- Przełącznik kanałów (▲ logo ▼, logo z epg.ovh przez `forgeChannelLogo`) od lewej; z fokusem ▲▼ od razu zmienia kanał. Przyciski od x=170: [↺ Od początku] (gdy kanał pozwala), [❚❚ Pauza] (archiwum, grupa, po pauzie), [● Na żywo] (z opóźnieniem); po prawej [+] (ulubione programy — `TvStore.toggleFavoriteShow`, te same co rząd Ulubione), w grupie także [i] i [⋯] (dźwięk).
- ▼ z przycisków podnosi rząd: program powtarzalny (≥2 emisje tytułu w przewodniku kanału) → inne odcinki (zielona kropka = w archiwum, „Wkrótce”/„Za N min” = przyszłe); jednorazowy → programy teraz/następne z innych kanałów z tego samego liścia kategorii EPG (potem gałąź), tytuł rzędu = nazwa kategorii „· na innych kanałach”. ▼ z rzędu → szczegóły.
- Szczegóły (▼▼): rozmyte tło, tytuł (logo programu — gdy któreś źródło je ma), [LIVE] godziny · kanał, typ · odcinek · kategoria EPG (gałąź › liść), opis, [🔔 Obserwuj] [↺ Od początku] + „● Archiwum N dni”; sekcje Odcinki · Sezon N → Najbliższe emisje (kafle: dzień, godzina, kanał, Powtórka/Nowy odcinek/Wkrótce) → Obsada i twórcy (inicjały) → Informacje (Kanał, Kategoria EPG, Emisja, Archiwum, Źródło danych).
- Dane: TVDB > TMDB > EPG > playlista (ten sam `TvStoreMetaViewModel` co karta programu Kicii); tekst zastępczy tylko, gdy żadne źródło nie ma wartości. Logo programu: żadne z tych źródeł go dziś nie podaje → tytuł tekstem (pole gotowe na logo).

### 10. Film w głównym odtwarzaczu: rząd „Podobne” i przyciski (canvas v4: `Player`, `PlayerFilmInfo`, `PlayerSeriesInfo`) — zastępuje decyzję z pkt 7
- ▼ z przycisków filmu podnosi rząd „Podobne” (4 kafle 16:9 + kawałek piątego), dopiero ▼ z rzędu otwiera szczegóły. OK na kaflu otwiera stronę tytułu.
- W szczegółach odtwarzacza (film i serial): [+ Do biblioteki] / [✓ W bibliotece] i [▶ Zwiastun] pod opisem; sekcje film: Obsada i twórcy → Podobne → Informacje; serial: Odcinki → Obsada i twórcy → Podobne → Informacje.
- Dane (`skin/niunia/player/NiuniaPlayerExtras.kt`): kolejność TVDB > TMDB > EPG > playlista; TVDB nie ma listy podobnych ani zwiastunów, EPG/playlista nie dotyczą filmów z dodatków — w praktyce TMDB (te same usługi co strona filmu). Zwiastun gra na pełnym ekranie nad wstrzymanym filmem, Wstecz wraca.

### 11. Menu Niuni: Wstecz i ▶ zawsze wychodzą do treści (właściciel 20.11)
- Także na Głównej (wcześniej Wstecz na Głównej szło dalej). Wybór zakładki, na której się jest, tylko zamyka menu. Ekran podaje cel fokusu po wyjściu (Główna: przycisk hero).

### 12. Budowanie bez cudzych zmian
- Niezacommitowane pliki innego wątku (`NiuniaLabels`, `NiuniaPoster`, `NiuniaTokens`, `NiuniaIcons`) nie kompilowały się (brak tekstów `niunia_genre_*`, brak importu `appendInlineContent`). Nie były ruszane — build z osobnego katalogu roboczego gita (`../NuvioFork-build`, tylko zacommitowane zmiany), więc w APK ich nie ma.

---
## ▶ ZNACZNIK WDROŻENIA — 20.11.2026, 13:30 (canvas: wersja 4 nowego artefaktu, id `1795173663-a608`)
Wszystko **powyżej** tej linii jest w kodzie (pkt 8–10 w buildzie z tego wątku). Nowe zmiany dopisuj poniżej.

### 13. Live TV Niuni od nowa (canvas v4: `LiveTV`, `LiveTVFav`, `LiveTVRow`, `LiveTVDiscover`, `LiveTVSearch`) — 20.11
- Pliki `skin/niunia/livetv/`: `NiuniaLiveTv.kt` (przewodnik, przyciski Szukaj · Dziś · Odkrywaj, wybór dnia), `NiuniaLiveData.kt` (kafelki dla filtrów, kolejność kategorii, rzędy Odkrywaj), `NiuniaLiveTiles.kt` (kafelek 160×114: kropka statusu, znacznik jednym słowem, ▶ i pasek), `NiuniaLiveDiscover.kt`, `NiuniaLiveSearch.kt`; `components/NiuniaSeriesStack.kt` (stos serialu); teksty `res/values/niunia_tv_strings.xml`. Bez nowych wpięć.
- Linia filtrów: Wszystkie (fokus na wejściu) · Ulubione · Oglądaj dalej · kategorie z drzewa kanałów EPG (`ForgeEpgCategories.channelTree`), kolejność wg licznika wyborów (`niunia_livetv`, klucz `use_<kategoria>`), bez historii — kolejność właściciela. Filtr zmienia naraz kafelki i przewodnik.
- Przewodnik: numer + logo epg.ovh, komórki w jednej linii, ▶ przy trwającym, fokus = obwódka 2 dp, podziałka co 30 min, czerwona linia „teraz” ze znacznikiem, w rzędzie z fokusem gradienty i strzałki ‹ ›, przycięty tytuł z fokusem od 40 dp; ◄► po programach przesuwa wspólną oś, ▲▼ kanał na ten sam czas. OK: trwający — na żywo, miniony — archiwum, przyszły — komunikat. Na dole „LIVE · program · kanał · godziny”.
- Odkrywaj: Ulubione · Dla ciebie · Teraz na żywo · Nowe odcinki · Ostatnio oglądane · Ostatnie kanały · 3 najliczniejsze kategorie teraz · Polecane · Popularne · Wkrótce · Archiwum (te same zbiory co Kicia).
- Szukaj: klawiatura (z polskimi literami), podpowiedzi / ostatnio wyszukiwane, drzewo kategorii programów z liczbami (filtrowane wpisywaniem, kategoria = zaznaczenie), wyniki programy + kanały, Rzędy / Siatka (zapamiętane). Logi `ForkNiunia`: `live tv: filtr=…, kafelki=N, kanały=N`, `live tv: odkrywaj — rzędy=…`, `szukaj tv: „…” → programy=N, kanały=N, kategorie=N`.

### 14. Hero bez ujemnego przesunięcia + animacja zejścia pod hero + rzędy bez skakania (właściciel 20.11)
- Błąd: na stronach z hero (Główna, strona filmu/serialu) hero był elementem przewijanej listy wyższym niż obszar listy, więc fokus na przycisku przesuwał go do góry. Teraz hero nie jest w liście: `components/NiuniaHeroPage.kt` — trzy warstwy na cały ekran (tło, hero, strona pod hero).
- ▼ z hero: napisy i przyciski hero wyjeżdżają do góry i znikają, tło dostaje bardzo mocne rozmycie (60 dp, API 31+; Android 11 — mocne przyciemnienie), strona pod hero wjeżdża od dołu (520 ms). Strona filmu/serialu: na górze logo programu na środku (przypięte, 64 dp, 40 dp od góry i 40 dp do rzędów), niżej sezony/odcinki, podobne, obsada, informacje. Główna: rzędy (pierwszy 40 dp od góry). ▲ z pierwszego rzędu i Wstecz → hero (animacja w drugą stronę). Log `ForkNiunia`: `<ekran>: ▼ → pod hero`, `<ekran>: ▲ → hero`.
- Skakanie rzędów: listy Niuni przewijają się stałym regułami (`NiuniaVerticalScroll` — rząd z fokusem zawsze w tym samym miejscu, ruch w bok nic nie przewija w pionie; `NiuniaRowScroll` — w poziomie tylko, gdy kafel wychodzi poza margines 40 dp).

### 15. Poprawki Live TV po teście build141 na JMGO (20.11) — NIESKOMPILOWANE
- Kafelki, linia filtrów i podziałka czasu stoją na miejscu; przewija się tylko lista kanałów (rząd z fokusem jako drugi od góry). Wcześniej cały nagłówek przewijał się pod „Live TV” i Szukaj/Dziś/Odkrywaj.
- Rząd kafelków i filtrów przewija się w poziomie dopiero, gdy element wychodzi poza margines 40 dp (wcześniej uciekał do lewej krawędzi, „Wszystkie” znikało).
- Rząd kanału z fokusem bez cienia (był szary pas po lewej). Odkrywaj: lista ucięta pod przyciskami u góry, rząd z fokusem zawsze w tym samym miejscu.
