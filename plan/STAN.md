# STAN (czytaj najpierw; aktualizuj po każdym zadaniu)

## Teraz

- Bieżący blok: **B00**
- Następne zadanie: **B05–B10 wdrożone w src i kompilują się (build50). Dalej: testy JVM, wpięcia B04/B07, B06a-2/3/5, B11**
- Gałąź robocza: `auto/wdrozenie` (z `final`)
- Build w tle: commit B01-6 (log /tmp/fork-build.log, status /tmp/fork-build.status)
- Niezweryfikowane buildem commity: brak

## Środowisko (uzupełnij w pierwszej turze)

- JDK 17: tak, /opt/homebrew/opt/openjdk@17 (export JAVA_HOME)
- Android SDK: tak, ~/Library/Android/sdk
- Gradle z dostępem do sieci: do sprawdzenia (build B00-2)
- Emulator Android TV / adb: adb tak, emulator w SDK; tylko na testy
- `gh` i push do origin: tak (konto stefankot, repo prywatne nuvio-tv-fork)
- Katalog `../nuvio-tv-kicia`: sklonowany (tylko odczyt)

## Dziennik (najnowsze na górze, jedna linia na zadanie)

- 2026-11-17 JMGO: właściciel zlecił instalację i test od instalacji na projektorze JMGO przez ADB (najpierw testy tekstowe, UI automation potem). Urządzenie NIE jest widoczne: `adb devices` puste; 192.168.31.29 i .44 → „No route to host”. Potrzebny adres IP JMGO (debugowanie sieciowe) albo USB. Skrypt: scratchpad/jmgo_install.sh <ip:port> (dobiera APK wg ABI, instaluje, sprawdza hash).

- 2026-11-17 emulator (APK b92, skrót OK): menu „Narzędzia” + hub działa; nakładka głosu „Słucham…” widoczna nad Live TV (zIndex); brak crashy. Dodany WhisperTranscriptsTest (test na transkrypcjach Whisper small/base). Kolejka: wszystko, co wykonalne bez kluczy TMDB/Trakt i bez telewizora, wdrożone; dalej testy uzupełniające.

- 2026-11-17 emulator (APK b90, skrót OK): menu główne ma pozycję „Narzędzia” (hub: Szukaj w TV, Odkrywaj, Na co masz ochotę?, przykłady poleceń, Aplikacje, Moduły) – SPRAWDZONE; ekran „Co mogę powiedzieć” – SPRAWDZONY. Wdrożone po b90 (test JVM w toku t91): fav/seek/kanał numer głosem, DurationParser. Nakładka nocna: pomiar koloru niedokładny (dark UI), niepotwierdzona wzrokowo.

- 2026-11-17 13:48 Testy JVM (t86, 8:48): 2 padają (TraktAuthServiceTest x2: brak TRAKT_CLIENT_ID/SECRET w local.properties = środowisko; patrz BLOKADY). Wcześniejszych 9 naprawiono kopiując testy z Reshaped (DolbyVision, Http1Only, SettingsCatalog, SearchViewModelSuggestions) i SearchViewModel.kt z Reshaped (poprawka focus-recomposition; baza miała test sprzeczny z własnym kodem). Whisper offline (WhisperListen), polecenia głosowe Odkrywaj/nastroje/rozmiar napisów, potwierdzenia głosowe: kompilują się (b83 APK 6:15; t86 testy). Do sprawdzenia na emulatorze/TV: lektor, głos, nocna nakładka.

- 2026-11-17 13:12 Wdrożone (kompilacja/testy w toku, b81): PicksModule (nastroje, wymaga TMDB_API_KEY: brak → „Brak klucza TMDB”, patrz BLOKADY), Odkrywaj jako ekran + wyłączniki rzędów, głos: kontekst kanałów, nawigacja (home/tv/library/search/settings/apps), kanał, „napisy nie pasują” (PlayerBridge), lektor on/off (LektorEngine+LektorAudioProcessor+LektorController, hook `// Fork hook: lektor` w Initialization), potwierdzenia „tak” i wybór numeru kanału (B07-7), prośba o mikrofon. NIEsprawdzone na urządzeniu: lektor, napisy nie pasują, głos na prawdziwej mowie. Zasada pracy: build APK i testy osobno (razem >10 min przez pamięć), kolejne funkcje pisać do .stage2/ w trakcie builda.

- 2026-11-17 12:27 BŁĄD NAKŁADEK NAPRAWIONY: rootOverlays modułów (tryb nocny, głos, przypomnienia, linia Gotowe) rysowały się POD treścią (hook jest pierwszym elementem Box w MainActivity). Teraz Box z zIndex(100) w ForkHooks.moduleOverlays. SPRAWDZONE na emulatorze (build74, skrót OK): na szczegółach The Matrix widać „Gotowe” (B04-6). Buildy w tej sesji: max 8:07. Emulator tylko -no-window (polecenie użytkownika).

- 2026-11-17 Hook rzędów modułów w ModernHomeRowsList WYCOFANY: Modern Home mapuje indeks wiersza na indeks elementu listy, dodatkowy element przesuwa indeksy (ryzyko regresji fokusu/przewijania). Zostaje hook w ClassicHomeContent (układ Classic). Rzędy modułów w układzie Modern: do zrobienia inaczej. Smoke emulator (build70): Live TV (lista kanałów), Kalendarz (odcinki), Szukaj w TV, Moduły: OK, bez crashy.

- 2026-11-17 B05-3: ekran Moduły (route fork_modules; przycisk w Prosty): lista modułów z wyłącznikiem i pozycjami; SPRAWDZONE na emulatorze (build70, skrót OK). Brak jeszcze wpięcia jako kategoria w standardowym SettingsScreen (Essential/Advanced) – dostęp tylko z ekranu Prosty. check.sh: SettingsCatalogTest bazy przywrócony (pada z powodu kategorii Reshaped; oczekiwane).

- 2026-11-17 B11-1..4 zrobione jako raporty (plan/raporty: mowa-lokalna, glos-neuronowy, dublift, tlumaczenie-napisow); bez nowego kodu. Pełne testy JVM (test68, 3:37): zostało 11 padających testów BAZY (DolbyVision 6, Trakt 2, Http1Only, SearchViewModelSuggestions; SettingsCatalog poprawiony o NUVIO_RESHAPED). DV/Http1Only/SettingsCatalog padają przez zmiany Reshaped (DV81 konwersja, h2, nowa kategoria), nie naprawiane (cudzy kod).

- 2026-11-17 emulator (APK build67, skrót OK): ekran Szukaj w telewizji (TvSearchScreen) DZIAŁA: Ustawienia→Prosty→„Szukaj w telewizji”, wpisanie „tvp” zwraca kanały (TVP1, TVP Info, TVP Sport…). Rzędy modułów na Home (hook w ClassicHomeContent i ModernHomeRowsList jako pierwszy element) NIEWIDOCZNE na Home w Modern (lista przewinięta za pierwszy rząd); dostęp tylko przez ustawienia. Dotyk (adb tap) nie aktywuje przycisków TV; używać DPAD. Buildy APK 4:48–7:01 (limit 10 min).

- 2026-11-17 B09: LektorModule+SystemLektor (TTS→PCM), kompiluje; miks w procesorach dźwięku NIE wpięty. B04-4: hook ForkHooks.wantsNextSourceForAudio w PlayerRuntimeControllerTracks (brak polskiej ścieżki → następne źródło), kompiluje (build60), nieprzetestowane na urządzeniu.

- 2026-11-17 B08: DiscoverModule (rzędy Odkrywaj na Home z dziennika/oglądania; koszyki gatunków tylko z grupy kanału, bo LogEntry nie ma kategorii); kompiluje (build58), niezweryfikowane na urządzeniu. Brak: ekran Szukaj (TvSearchIndex), moduły lektor/picks/ai.

- 2026-11-17 B07: VoiceActions (klawisze pilota: pauza/play/przewijanie/wstecz/nawigacja/kanał/odcinek + tryb nocny) podpięte do VoiceModule; kompiluje (build57), niezweryfikowane na urządzeniu (SpeechRecognizer w emulatorze zwykle niedostępny). Dalej: moduły discover/lektor/picks/ai, wpięcia B04, B06a, B11.

- 2026-11-17 emulator (APK z B05–B07, skrót OK): start OK, wybór profilu OK, ekran Prosty (Ustawienia: Ty/Konto/Profil/Źródła/Dodatki, Oglądanie, tryb nocny, telewizja; Zawsze po polsku...) OK, bez crasha. VoiceModule dodany (nakładka nasłuchu, SpeechRecognizer pl-PL → RuleParser, wynik tylko jako komunikat; wykonanie poleceń NIE zrobione). Brak modułów: discover, lektor, picks, ai (tylko logika bez UI/rejestracji). lintVital pada w buildzie APK (błąd lint) → budować z -x lintVitalAnalyzeFullDebug -x lintVitalReportFullDebug.

- 2026-11-17 testy: Discover/Voice/EpgExtraParse naprawione (genres.json, liczebniki 'dwadzieścia cztery'/'dwójce', łatka dostawca #05: odcinek w Pending). Zielone. Dalej: wpięcia B04/B07, test na emulatorze (tekstowo), B06a, B11.

- 2026-11-17 testy JVM (test52): moje padające: DiscoverTest (2: kolejność wyników, koszyki 'serial kryminalny'/'soap opera'), VoiceTest (3: atrapa embeddera, szyna reguł, dopasowanie kanałów 'tvn dwadzieścia cztery'/'tvp dwójce'), EpgExtraParseTest (numer odcinka S1E2 = null). Reszta (DolbyVision 6, Trakt 2, Http1Only, SearchViewModelSuggestions, SettingsCatalog) = baza. Do naprawy w następnej kolejności.

- 2026-11-17 B05–B10: skopiowane z .stage/ do src, apply_b07 zastosowany; compileFullDebugKotlin OK (build50). Testy JVM i wpięcia niezweryfikowane.

- 2026-11-17 STOP (limit): B05/B06/B06b/B07/B08/B09/B10 napisane w `.stage/` (main, test2, res, assets, apply_b05_b06.py, apply_b07.py) i NIEWDROŻONE do src ani niezbudowane; B06a-2/3/5, B11 niezrobione. Ostatni build testów (commit z B04) przeszedł kompilację; znane padające testy bazy: DolbyVision(6), Trakt(2), Http1Only, SearchViewModelSuggestions, SettingsCatalog.

- 2026-11-17 B04: SourcePresets (source_presets.json + 30 przypadków testu), SourceChoice (zapis przez DataStore), SourcesModule (3 odpowiedzi + rozmiar napisów w ustawieniach), FirstRun: auto-próba źródła, AudioCheck, ReadyPick/SourceLanguage, SubtitleQueue, test kodowania. Build w tle sprawdzi.

- 2026-11-17 B03: rząd Dla Ciebie (łatka LiveTvScreen), CellMark (łatka LiveTvGuide), ProgramPage (trasa fork_program/{key}), PendingPlay, ReminderToast, wiersz domowy w rejestrze; logika+testy dla filtrów/MiniGuide/z opóźnieniem. UI MiniGuide/panelu grup/odtwarzacza -> BLOKADY.

- 2026-11-17 B02-3..9: EpisodeLog, WatchLog (reguły #1), ProgramTypes, Following+ChannelStats, ProgramCard+cache, ForYouQueue, TvData (pliki fork_tv), TvModule (ustawienia, akcja czyszczenia); testy JVM dla każdego. Kompilacja i testy czekają na build.

- 2026-11-17 B02-1/2: logika TV z Kicia (fork/tv, pakiet jednolity, TvStore=ForgeTv), searchMulti w TmdbApi, TvBridge (jedyny import reshaped.livetv) + wpięcia w repozytorium i overlay (łatki). Testy mostu na prawdziwym Contexcie niemożliwe w JVM: EpgExtrasTest zamiast. Zbieranie emisji obserwowanych poza oknem (watchedShows) pominięte.

- 2026-11-17 B01-11: pola EPG/logo w stronie konfiguracji (łatka), Coverage (logika+test); komunikat i lista „bez programu” w UI odłożone do B03 (brak ekranu).

- 2026-11-17 B01-8/9/10: klawisze+potwierdzenie zap (ForkZapConfirm), logo (fork/tv/logo + 3 testy z Kicia), drugi przewodnik (assets/fork/epg_sources.json, EpgSources, ExtraGuides + test). Nadpisanie JSON z repozytorium: tylko lokalny plik filesDir/fork/epg_sources.json (pobieranie przez PackStore odłożone).

- 2026-11-17 B01-7: okno 7d/8d dla Regular, LowMemory bez zmian (DECYZJE #6), zachowanie historii; GuideWindowTest.

- 2026-11-17 B01-2/4: łatka repozytorium (warmUp, ranking dodanego przewodnika, extraLists+enrich), TvModule (rootOverlays → warmUp), EnrichProgrammesTest. cancelWarmUp nie podpięty do startu odtwarzania (limit 10 min); do rozważenia w B02.

- 2026-11-17 B01-3/5/6: łatki dostawca (dziennik EPG, episode+obrazy, szczegóły XMLTV przez ForkTvHooks) + EpgExtraParseTest; łatki są w fork-tools/patches/dostawca/<plik>/. Pierwszy build testów padł tylko na wyścigu plików, powtórzony.

- 2026-11-17 B01-1: łatka dostawca #1 (klucz sumy) + MatchingKeyTest; narzędzie fork-tools/patchstep.sh. Niezweryfikowane buildem do końca builda w tle.

- 2026-11-17 B00-7/8: FirstRunDefaults (fork/settings, wołane z moduleOverlays) + test; scripts/fork/autoupdate.sh i fork-autoupdate.yml (tylko ręcznie).

- 2026-11-17 B00-5/6: wpięcia registry w NuvioNavHost, MainActivity, HomeScreen, PlayerScreen (ustawienia przez ForkModuleSettingsItems w pliku forka); check.sh obsługuje pliki łatane + selftest-patched.sh. Kompilacja wpięć niezweryfikowana (build w tle).

- 2026-11-17 B00-4: pakiet fork/registry (ForkModule, ForkRegistry, ForkModules, testy). Build bazowy: assembleFullDebug zielony; testy JVM nie kompilowały się (testy usuniętego aktualizatora DownloadedUpdateTest, TesterChannelTest) — usunięte.

- 2026-11-17 B00-3: workflow fork-verify.yml + scripts/fork (ci-gradle-env, prefetch-sherpa, run-tests).

- 2026-11-17 B00-1: gałąź auto/wdrozenie; check.sh OK (23 pliki) po dopisaniu MetaDetailsScreen.kt do base-hooks (hook Echo z wcześniejszego commita).

- 2026-11-17: plan przestawiony na gałąź `final` (commit `464b04d`). Uwaga dla wykonawcy: bloki B01–B05 były pisane na podstawie gałęzi `main` (`c30cfc4`); ścieżki plików Reshaped i Live TV są te same, ale przed każdym zadaniem sprawdź, czy opisany plik i miejsce istnieją na `final`, i dostosuj wpięcie do `ForkHooks`.
- 2026-11-17: plan utworzony. Kicia: commit `58b3a0a`.
