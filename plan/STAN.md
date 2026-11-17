# STAN (czytaj najpierw; aktualizuj po każdym zadaniu)

## Teraz

- Bieżący blok: **B00**
- Następne zadanie: **B05-1**
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
