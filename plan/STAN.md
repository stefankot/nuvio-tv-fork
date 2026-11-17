# STAN (czytaj najpierw; aktualizuj po każdym zadaniu)

## Teraz

- Bieżący blok: **B00**
- Następne zadanie: **B00-7**
- Gałąź robocza: `auto/wdrozenie` (z `final`)
- Build w tle: commit B00-4 (log /tmp/fork-build.log, status /tmp/fork-build.status)
- Niezweryfikowane buildem commity: brak

## Środowisko (uzupełnij w pierwszej turze)

- JDK 17: tak, /opt/homebrew/opt/openjdk@17 (export JAVA_HOME)
- Android SDK: tak, ~/Library/Android/sdk
- Gradle z dostępem do sieci: do sprawdzenia (build B00-2)
- Emulator Android TV / adb: adb tak, emulator w SDK; tylko na testy
- `gh` i push do origin: tak (konto stefankot, repo prywatne nuvio-tv-fork)
- Katalog `../nuvio-tv-kicia`: sklonowany (tylko odczyt)

## Dziennik (najnowsze na górze, jedna linia na zadanie)

- 2026-11-17 B00-5/6: wpięcia registry w NuvioNavHost, MainActivity, HomeScreen, PlayerScreen (ustawienia przez ForkModuleSettingsItems w pliku forka); check.sh obsługuje pliki łatane + selftest-patched.sh. Kompilacja wpięć niezweryfikowana (build w tle).

- 2026-11-17 B00-4: pakiet fork/registry (ForkModule, ForkRegistry, ForkModules, testy). Build bazowy: assembleFullDebug zielony; testy JVM nie kompilowały się (testy usuniętego aktualizatora DownloadedUpdateTest, TesterChannelTest) — usunięte.

- 2026-11-17 B00-3: workflow fork-verify.yml + scripts/fork (ci-gradle-env, prefetch-sherpa, run-tests).

- 2026-11-17 B00-1: gałąź auto/wdrozenie; check.sh OK (23 pliki) po dopisaniu MetaDetailsScreen.kt do base-hooks (hook Echo z wcześniejszego commita).

- 2026-11-17: plan przestawiony na gałąź `final` (commit `464b04d`). Uwaga dla wykonawcy: bloki B01–B05 były pisane na podstawie gałęzi `main` (`c30cfc4`); ścieżki plików Reshaped i Live TV są te same, ale przed każdym zadaniem sprawdź, czy opisany plik i miejsce istnieją na `final`, i dostosuj wpięcie do `ForkHooks`.
- 2026-11-17: plan utworzony. Kicia: commit `58b3a0a`.
