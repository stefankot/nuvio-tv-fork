# Stan prac: fork Nuvio Android TV (16.11.2026)

Baza: oficjalny NuvioTV, gałąź `dev`, commit `2c853c1` (w gicie: "Baseline NuvioTV 2c853c1", `85f9010aa`).
Pochodzenie plików: `UPSTREAM_SOURCES.md`. Zasady i preferencje: `../CLAUDE.md`.

## Gałęzie (git, katalog NuvioFork)
| Gałąź | Zawartość | Kompiluje |
|---|---|---|
| `rs-module` | baza + Reshaped jako moduł (759 nowych plików, 74 pliki rdzenia zmienione, bez aktualizatora Reshaped, z DV/HDR10) | tak, APK zbudowany |
| `yt` | rs-module + Nuvio One YouTube (mediaservicecore, pakiet `youtube`, trasa, adaptery) | tak |
| `echo` | yt + Echo `AutoQualityPolicy`, `StreamQuality` (czysta logika, niepodpięta) | tak |
| `ke2` | echo + KE katalog IPTV (pack z GitHuba + iptv-org), most do Reshaped, skan łączy, przełączniki w ustawieniach | tak (aktualna, zbiorcza) |
| `main` | STARY monolit Reshaped nałożony w całości (porzucony, nie spełnia wymogu modułu) | tak |
| `ke` | pierwsza próba KE (cały Live TV KE), porzucona | nie (1391 błędów) |
| `wip-all` | surowe kopie Arc, Echo, KE, One (archiwum) | nie |

## Wdrożone (kompiluje się, nie sprawdzone na telewizorze)
- Reshaped: IPTV (M3U/Xtream/Stalker), EPG, catch-up, szklana nawigacja, synchronizacja napisów z audio, drugi język, rozpoznawanie mowy (model angielski), lokalne miniatury, Dolby Vision, HDR10.
- Nuvio One: YouTube (przeglądanie, odtwarzanie w oficjalnym odtwarzaczu, ustawienia).
- KE: katalog IPTV z kategoriami/krajami/językami (repozytorium `Arshad-Kamal/Nuvio-KE-channels` + metadane iptv-org), dodawanie kraju jako źródła Live TV, filtry kategoria/język, test łączy kanałów.
- Echo: tylko czysta logika Auto Quality (niepodpięta).
- Ustawienia z forków pod jedną kategorią (sekcja Reshaped): KE katalog, YouTube.

## Zaparkowane (za duża ingerencja w rdzeń)
- Echo: wyścig źródeł (`AutoPlayRace`), zamiana w locie (`HotSwap`), przełączanie źródeł, fallback jakości, hedge auto-play, ekran ustawień wydajności. Wymagają ~40 pól stanu w kontrolerze odtwarzacza i selektora Echo.
- KE: testowanie sprzętu i łącza urządzenia (`DeviceAssessmentEngine`, `Applier`, `DeviceWelcome`, `ServiceNetworkTestRunner`, `StreamSpeedTester`). 441 błędów, powiązane z ustawieniami odtwarzacza KE.
- KE: przełączana powłoka, Home, Layout Settings, TV Home (nie zaczęte).
- Arc: kalendarz (2172 błędów), wyszukiwarka ustawień, miniatury, cache przewijania, diagnostyka, Home Catalog Order.
- Arc: pilot (`RemoteButtons`) w całej aplikacji, także w IPTV (w toku: analiza przeniesienia zadań przycisków na zdarzenia odtwarzacza).
- YouTube: miniatury podglądu (storyboard) i raport diagnostyczny zastąpione pustymi adapterami.

## Decyzje użytkownika (16.11, wieczór)
- KE: reszta ZAPARKOWANA (powłoka/Home/Layout/TV Home, testowanie sprzętu). W zakresie KE zostają tylko katalog IPTV, filtry kanałów i skan łączy.
- Arc miniatury: bierzemy implementację z Reshaped (`seekpreview/engine`, `ArcPreviewAdapter`, `local`, `cpp/seekpreview`), nie kopiujemy `core/player/thumbnail` z Arc (60 plików). Mniej ingerencji w rdzeń. Przełącznik: sekcja Reshaped, `seekPreviewSettingsItems`.
- Arc pilot: w toku na gałęzi `arc-remote` (mapa przycisków + hook obok Live TV w PlayerScreen).

- Arc: Home Catalog Order: JUŻ W BAZIE (`ui/screens/addon/CatalogOrder*`, wersja dev jest nowsza od Arc). Nic do dodania.
- Arc: cache przewijania (= cache podglądu przy przewijaniu/seek): pokrywa go silnik podglądu z Reshaped (ta sama decyzja co miniatury).
- Arc: diagnostyka (panel statystyk): fork ma już `PlayerDebugStatsOverlay` (bogatszy niż Arc); przycisk pilota STATS przełącza go (`OnTogglePlayerStatsHud`).

## Zredukowane względem Reshaped
- Aktualizator Reshaped usunięty (polecenie użytkownika), wraca natywny.
- Pominięte ~125 zmian Reshaped poza odtwarzaczem (debrid, Trakt, biblioteka, serwer addonów itp.).
- Limit ok. 35 plików rdzenia przekroczony (74).

## Do dodania (lista użytkownika, kolejność)
1. Arc pilot w całej aplikacji.
2. Arc: miniatury, cache przewijania, diagnostyka, Home Catalog Order, wyszukiwarka ustawień, kalendarz.
3. KE: testowanie sprzętu i łącza, powłoka/Home.
4. Echo: wpięcie w odtwarzacz.
5. Model rozpoznawania mowy wielojęzyczny (wybór w ustawieniach: Whisper wielojęzyczny, Parakeet TDT 0.6B v3; pliki hostowane u nas). Obecny model: zipformer GigaSpeech 2023-12-12, tylko angielski, 74 MB.

## Budowanie
```
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew :app:assembleFullDebug -PforgeArm64Only=true --console=rich
```
APK: `app/build/outputs/apk/full/debug/app-full-arm64-v8a-debug.apk`. Gradle ma `-Xmx3072m` (Mac 8 GB RAM).

## Koniec wątku 1
- `arc-remote`: pilot Arc + wybór modelu mowy (Whisper) zacommitowane jako WIP; ostatnia kompilacja (build39) w toku, wynik nieznany. Szczegóły i procedura: `../CLAUDE.md`.

## 16.11.2026 – wątek 2
- Krok 1 planu architekta wykonany: `arc-remote` kompiluje się (build40), APK zbudowany (arm64, 105 MB). Pakowanie za pierwszym razem padło bez winy kodu, powtórka przeszła.
- Nic nie uruchomiono na emulatorze. Następny: krok 2 (pierwsze uruchomienie, checks 1–5, 7), potem krok 3 (gałąź `final`, `fork-tools/`).

## Krok 2 planu: pierwszy test na emulatorze (16.11.2026, fork `com.nuviodebug.com`, skrót APK sprawdzony)
- 1 Start: OK (onboarding, Home z katalogami, 0 crashy).
- 2 Kategoria „Nuvio Reshaped": OK (AutoSync, Arc, Live TV, KE, YouTube, pilot, model mowy).
- 3 Live TV: OK (M3U iptv-org PL załadowane, lista kanałów, kanał przechodzi w PLAYING).
- 4 KE: przełącznik działa, bez crasha; zawartość katalogu NIEZWERYFIKOWANA.
- 5 YouTube: crash po włączeniu przełącznika (zagnieżdżone przewijanie w `KeCatalogSettingsItems.kt`), NAPRAWIONY (Box height 900.dp), OneTube się otwiera; zawartość wymaga logowania.
- 7 Model mowy: wybór działa (cykl modeli, Whisper tiny); trwałość po restarcie i pobieranie NIEZWERYFIKOWANE.
- Logowanie QR nie działa: build łączy się z Supabase pod localhost:443 (brak konfiguracji kluczy).
- Następny krok: 3 (gałąź `final`, `fork-tools/`), potem 4–11.

## Krok 3 planu (16.11.2026): gałąź `final`, `vendor/nuvio`, `fork-tools/`
- `vendor/nuvio` = 85f9010aa (czysta baza). `final` = praca od teraz.
- `fork-tools/`: pins.env, overrides.txt (68 plików identycznych z Reshaped), hooked-overrides.txt (4), base-hooks.txt (3), hooks.tsv, check.sh. `bash fork-tools/check.sh` → OK (ręcznie utrzymywane: 7, limit 35).

## Kroki 4–5 (16.11.2026): ForkHooks + pilot w całej aplikacji
- `fork/ForkHooks.kt`, `fork/arc/RemoteGlobalHook.kt`; hook w `MainActivity.dispatchKeyEvent` (1 linia). `ForkFeatures.enabled(c,key,default)`.
- Poza odtwarzaczem pilot wycisza tylko przyciski przypisane do „Nic"; reszta zadań nie ma odpowiednika poza odtwarzaczem. W odtwarzaczu i Live TV działa hook w PlayerScreen.
- Kompiluje się (build45), check.sh OK (ręcznie utrzymywane: 8). Niezweryfikowane na urządzeniu.

## Kroki 6–7 (16.11.2026)
- B: wiersz „Test łącza i raport sprzętu" w kategorii forka (`DeviceTestSettingsItems.kt`); sam test i raport są w bazie (Ustawienia → Advanced). Kompiluje się.
- E: wyszukiwarka ustawień z Nuvio One (`SettingsSearch.kt`, `SettingsSearchOverlay.kt`) + 3 hooki w `SettingsScreen.kt` (pasek, szyna, overlay). Wynik „Playback → sekcja" otwiera tylko kategorię (fork nie ma `initialPlaybackSection`). Kompiluje się (build47), niezweryfikowane na urządzeniu. Zmiany w kopii: `UPSTREAM_SOURCES.md`.

## Kroki 8–10 (16.11.2026)
- Krok 8, kalendarz Echo: 6 plików 1:1 + `echo_calendar_strings.xml` + ikona; shimy w `fork/echo/shims/` (ApiKeys, glassSurface, underSidebar, NewLookNotifications [nic nie pokazuje], AmbientPageWash, selectionSurface, BrowsePill/scrollFade, BetaPerformanceSettings); `TmdbApi.kt` +getTvSchedule i klasy; trasa `calendar`, pozycja menu (`fork_calendar`). Kompiluje się; bez klucza TMDB kalendarz może być pusty (D5). Niezweryfikowane na urządzeniu.
- Krok 9: przełączniki (Kalendarz, wyszukiwarka ustawień, pilot Arc) w kategorii forka; wyszukiwarka respektuje przełącznik.
- Krok 10: moduł skin: `skin/api` (SkinId, SkinPreferences, Skin, SkinRegistry), pusty `KitSkin`, hooki 2–4 (Home, nakładka odtwarzacza przez `ForkHooks`), wiersz „Wygląd: Nuvio/Netflix/Mój projekt". NETFLIX włącza istniejący przełącznik Reshaped. Hook 1 (opakowanie motywu w `MainActivity`) i hook powłoki nawigacji NIE dodane (wymagają opakowania bloku; do zrobienia przy pierwszym ekranie kit). Generator tokenów w makiecie: pakiet zmieniony na `com.nuvio.tv.skin.kit.tokens` (`npm run build:tokens` OK; `check:kotlin` nie ruszył: brak bibliotek w cache Gradle). Tokeny NIE skopiowane do aplikacji.
- check.sh OK; hand-maintained: 10 (limit 35).

## Krok 11: przejście testowe (16.11.2026, fork `com.nuviodebug.com`, skrót APK OK, testy tekstowe)
- 1 Start: OK. 2 Kategoria forka: OK (+ wiersze: test łącza, przełączniki, Wygląd). 3 Live TV: OK (wcześniej). 5 YouTube: OK po poprawce (wcześniej).
- 8 B: wiersz „Test łącza i raport sprzętu" widoczny (informuje, gdzie jest test; sam test w Ustawienia → Advanced, niesprawdzany).
- 9 E: wyszukiwarka otwiera się, wpisanie „sub" daje wyniki (Subtitles, Nuvio Reshaped, Playback); wybór wyniku klawiaturą NIEZWERYFIKOWANY.
- 10 D: pozycja Calendar w menu, ekran się otwiera, pusty stan (brak klucza TMDB/biblioteki), bez crasha.
- 11 Wygląd: przełączanie przez wiersz „Wygląd" nie crashuje; po przełączeniu Home zmienił układ (Netflix). Powrót do Nuvio i brak zmian dla pustego Kit: niezweryfikowane wzrokowo.
- 12 Regresja Echo: nic z Echo jeszcze nie podpięte w odtwarzaczu.
- Niesprawdzone: 4 (zawartość KE), 7 (trwałość modelu mowy), YouTube bez logowania.

## Krok 12–13: Echo A1–A3 (16.11.2026) — KOMPILUJE SIĘ, NIEPRZETESTOWANE
- Skopiowane 1:1 z UP/echo @ e952419: SourceSwitch, QualityFallback, AutoQuality, AutoQualityHotSwap, AutoPlayHedge, AudioMenu, QualitySelectionOverlay, AudioLanguageMenuContent, PlayerPopCard, PlaybackIntro, AutoPlayRace, AutoPlayPreferences, AudioTrackProbe, StreamAudio, PlaybackIntroState (+ zasoby intro i napisy `echo_*_strings.xml`).
- Adaptery (`fork/echo/`): `EchoControllerState` (pola Echo jako rozszerzenia kontrolera, bez edycji PlayerRuntimeController.kt), `EchoReshapedBridge` (switchToSourceStream, StreamPlayback), `EchoRaceCandidates`, shimy (`BetaFeatures` → ForkFeatures, wszystkie klucze Echo domyślnie WYŁĄCZONE; `FileKeyframes` = stub, `NewLookNotifications` = nic nie pokazuje).
- Hooki (R2, ~25 linii) w: PlaybackEvents, Initialization, Streams, Lifecycle, SourceFallback, PlayerScreen, PlayerViewModel; widoczność `private`→`internal` dla 2 funkcji w Streams (hot swap). PlayerUiState: dodane pola/zdarzenia Echo.
- Dostępne: menu jakości (zadanie pilota „Quality” w Arc + przełącznik „Echo: przełączanie źródła”), Auto Quality (monitor startuje przy włączonym przełączniku), hot swap. Przełączniki w kategorii forka (wszystkie wyłączone domyślnie).
- NIE podpięte jeszcze: A4 wyścig źródeł (StreamScreenViewModel, Streams, Initialization: armAutoPlayHedge), A5 menu audio (Tracks, Observers, nakładka audio), A6 intro (NavHost, StreamScreen, PlayerScreen, Streams). Kod tych funkcji jest w aplikacji, ale nieaktywny.
- Testy 13–18 wymagają addonu z kilkoma źródłami (czeka na użytkownika).

## 17.11.2026: testy na żywo (emulator, addony użytkownika: Torrentio/Comet/TorBox, YouStream)
- Odtwarzanie źródła z Torrentio TB: OK (720p x264). 4K HEVC nie startuje na emulatorze (brak dekodera; ograniczenie emulatora).
- Echo A1-A3: Auto Quality + hot swap: OK (720p → 1080p w tym samym punkcie, 66767 ms).
- Echo A4 wyścig źródeł: OK ("AUTOPLAY_RACE: 8 raced, won #1 po 3560 ms"; komunikat „Checking 8 links…”).
- Echo A5 menu języka audio: OK (AUDIO_MENU: 303 źródła, wiersze ko/en/ja; przełączenie źródła działa; Echo odrzuca źródło bez ścieżki ja).
- Echo A6 intro: OK (NuvioIntro: 145 klatek, zakończone).
- Test prędkości (Advanced): OK (55 ms, 395,5 Mbps). Live TV: lista kanałów OK. AutoSync napisów: OK. Wyszukiwarka: OK.
- NIEZWERYFIKOWANE: zawartość katalogu KE, YouTube bez logowania, trwałość modelu mowy, wyścig przy następnym odcinku (hook w Streams nie dodany), Echo source fallback po błędzie dekodera (nie przełączył).
- Uwaga: w emulatorze przypadkowo przełączono „Playback issue reports” (Advanced) i ustawiono Auto Stream Selection na „Auto-play first source”.

## 17.11.2026 (2): preloading źródeł, preferencje auto-play, fallback
- Preloading źródeł na ekranie szczegółów (`fork/echo/EchoSourcePreload.kt`, hook w `MetaDetailsScreen`, przełącznik w kategorii forka, domyślnie WŁĄCZONY): film (start/koniec po ~0,9–1,4 s) i serial S1E1 (1,5 s) OK; korzysta z istniejącego cache sesji wyszukiwania `StreamRepository`.
- Auto-play z wyborem rozdzielczości i kodeka (wiersze „Auto-play: rozdzielczość” dowolna/4K/1080p/720p i „kodek” dowolny/AVC/HEVC, `EchoRaceHook.preferred/prefs`): sprawdzone na żywo: 720p → wybrano „Torrentio 720p”; 1080p → „1080p HDR”; 1080p+AVC → „1080p” i film gra; 4K → „4k DV|HDR”. Echo stosuje pigułki tylko w trybie regex; fork stosuje je też w trybie „first source”.
- Zapamiętany ostatni link (funkcja Reshaped „reuse last link”) omija selektor i preferencje; to zachowanie bazy.
- Fallback po błędzie: „Automatically try another source” (Reshaped, osobny przełącznik w kategorii Reshaped) działa przy braku startu źródła, NIE przy błędzie dekodera (MediaCodec): wtedy gracz robi 2 próby tego samego źródła i pokazuje błąd. To logika bazy, niezmieniona. Echo „następne źródło po błędzie” osobno niezweryfikowane.
- NIE podpięte (świadomie): wyścig źródeł przy następnym odcinku (Streams.kt, kilka ścieżek wyboru).
- Błąd pakowania APK (`packageFullDebug`) wystąpił raz; powtórka przeszła.
- Uwaga: w emulatorze przypadkowo wyłączony i przywrócony „Echo: przełączanie źródła”; otwarty dialog „Sign out?” anulowany (konto nadal zalogowane).

## 17.11.2026: LISTA FUNKCJI (krok 15)
Gałąź robocza: `auto/wdrozenie` (czysta; wcześniej `final`). Serie B00–B07 = rdzeń TV/EPG/Reshaped (patrz `git log`).
**Działające (sprawdzone na emulatorze):** Reshaped (Live TV, IPTV/EPG, DV/HDR10, bez aktualizatora), YouTube, pilot Arc w całej aplikacji, wyszukiwarka ustawień, kalendarz (pusty bez klucza TMDB), test łącza, wiersz „Wygląd" (przełączanie bez crasha), Echo: Auto Quality + hot swap, wyścig źródeł, menu audio, intro, preloading, preferencje auto-play, AutoSync napisów.
**Częściowe:** katalog KE (widoczny, zawartość niezweryfikowana), model mowy Whisper (trwałość niezweryfikowana), Echo fallback po błędzie dekodera (nie przełącza), wyścig przy następnym odcinku (hook w Streams.kt nie dodany), skórka Kit (pusty szkielet; hook 1 motywu i powłoki nawigacji nie dodane).
**Zablokowane/zaparkowane:** logowanie QR (Supabase localhost), 4K HEVC na emulatorze (brak dekodera), KE tuner sprzętu, miniatury Echo, zgłoszenia problemów, tłumaczenia.

## LISTA MODUŁÓW (17.11.2026, gałąź `auto/wdrozenie`)
Rejestr: `fork/registry/ForkModules.kt`. Każdy moduł ma wyłącznik (Ustawienia → kategoria forka albo Prosty → „Moduły”).
| Moduł | Co robi | Stan |
|---|---|---|
| tv | dziennik odcinków, „Dla Ciebie”, strona programu, przypomnienia, logo, drugi przewodnik | działa (emulator: Live TV, Kalendarz) |
| sources | trzy odpowiedzi „polski dźwięk czy jakość”, auto-próba źródła, napisy, linia „Gotowe” | linia „Gotowe” sprawdzona; sprawdzenie ścieżek audio nieprzetestowane |
| night | tryb nocny: harmonogram, dźwięk, ciepła nakładka (nad treścią po naprawie zIndex) | kod OK, wygląd niesprawdzony |
| launcher | Aplikacje, wyjście 5×Wstecz, start z TV, ekran główny | niesprawdzone na urządzeniu |
| voice | asystent/klawisz → mowa (system lub Whisper offline) → reguły → akcje, potwierdzenia, wybór kanału | nakładka i uprawnienie sprawdzone; mowa niesprawdzona (emulator bez mikrofonu) |
| discover | rzędy z dziennika, ekran Odkrywaj, Szukaj w TV, wyłączniki rzędów | Szukaj sprawdzone; rzędy na Home tylko układ Classic |
| lektor | TTS → miks z ściszaniem w odtwarzaczu, polecenie głosowe, ustawienia głośności | niesprawdzone na urządzeniu |
| picks | „Na co masz ochotę?” (5 nastrojów, TMDB, punktacja regułowa) | wymaga TMDB_API_KEY (brak) |
Testy JVM: 4223 testy, 2 padają (Trakt: brak kluczy w local.properties).

## Aktualizacja 18.11.2026 (gałąź aktualizacja-1118)
Baza Nuvio 2b02219d5, Reshaped 0ca5a08c2, KE 78f1d82 (6 plików livetv + GuideShift), Arc 90fd0be71 (bez zmian w używanych plikach). Echo, One bez zmian. Kompilacja compileFullDebugKotlin: BUILD SUCCESSFUL (po usunieciu duplikatow autosync w pt-rPT). APK niezbudowany, nic nie testowane na urzadzeniu.
