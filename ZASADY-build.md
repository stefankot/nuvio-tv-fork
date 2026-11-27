# ZASADY — buildy, APK, instalacja (czytaj przed każdym buildem)

Część jedynego źródła zasad (rdzeń: `ZASADY.md`). Nowe zasady o buildach dopisuj tutaj, z datą.

## Kiedy budować
- Build tylko z DUŻEJ paczki poprawek, nigdy dla jednej. Drobne poprawki: commit bez buildu, lista paczki w `plan/TERAZ.md`; testy w tym czasie na ostatnim APK.
- Przed buildem i instalacją: `git diff --stat <commit APK>..HEAD -- app`. Brak zmian = ten sam APK: nie buduję, nie instaluję, wpis w `plan/DECYZJE.md`. Gdy właściciel wyraźnie kazał zbudować — buduję, bez pytania.
- Równoległe wątki (26.11, właściciel: „commituj i buildy rób razem z drugim wątkiem”): jeden wspólny build z zacommitowanymi poprawkami wszystkich wątków. Przed buildem napisz do pozostałych wątków (SendMessage), poczekaj, aż zacommitują gotowe poprawki, buduj z HEAD; po buildzie podaj im nazwę APK i md5.
- Build z cudzymi niezacommitowanymi (niedziałającymi) plikami: nie ruszaj ich, buduj z osobnego katalogu (`git worktree add --detach ../NuvioFork-build HEAD` + kopia `local.properties`).

## Jak budować
- WYŁĄCZNIE `bash fork-tools/build.sh <nazwa-logu> <zadania...>` w `NuvioFork`, w zakładce terminala aplikacji (`run_in_terminal`, nowa zakładka); skrypt daje `--console=rich` i log `/tmp/<nazwa>.log`. Bezpośrednie `./gradlew` zakazane.
- Najwyżej 1 build naraz, 1 w kolejce; nowe zlecenie anuluje czekające (kod 75 = anulowany, nie błąd). Nigdy `./gradlew --stop`, `pkill` demona ani zatrzymywanie cudzego buildu.
- Jedna architektura: JMGO/Xiaomi `-PforkAbi=armeabi-v7a`, emulator `-PforkAbi=arm64-v8a`. Buildy Codexa zawsze jeden APK ARM32 (`armeabi-v7a`).
- Typowo (~14–16 min): `bash fork-tools/build.sh uxrNN :app:assembleFullDebug :app:testFullDebugUnitTest --tests 'com.nuvio.tv.<pakiet>.*' … -PforkAbi=armeabi-v7a` (testy JVM tylko pakietów, których dotyczy paczka).
- Środowisko: `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`; Mac 8 GB RAM, Gradle `-Xmx3072m` (także w `~/.gradle/gradle.properties` — 27.11 było tam 5g i build trwał 40 min); pakowanie APK czasem pada bez przyczyny — powtórz.
- Od aktualizacji bazy 1.2.0-beta.4 (gałąź `aktualizacja-1126`): `build.sh` daje `--no-configuration-cache`, bo `~/.gradle/gradle.properties` ma `configuration-cache=true`, a zadania bazy (`downloadAudioSyncRuntime`, `buildUsenet*`) padają z nią na „this.this$0 is null”. Nie usuwaj tej flagi.
- Przed buildem: `df -h ~` (ok. 15 GB wolne), zamknij zbędne procesy (emulator — emulator i kompilator NIGDY razem). Pilnuj CPU.
- Czekanie w tle (`run_in_background`), limit 20 min: `until grep -aqE "BUILD SUCCESSFUL|BUILD FAILED|FAILURE:" /tmp/<nazwa>.log; do sleep 20; done`. Długie czekanie na pierwszym planie — nie.
- Build > 10 min → sprawdź, czy naprawdę się kompiluje (postęp w logu, CPU); > 20 min → przerwij własny build (22.11, właściciel). Wyjątek: build od zera (po skasowaniu cache), dopóki w logu widać postęp.
- W czasie builda nie czekaj bezczynnie: pisz dalej w `.stage/`; nie ruszaj plików, które Gradle czyta (`app/`).

## Po buildzie
- APK: `app/build/outputs/apk/full/debug/app-full-armeabi-v7a-debug.apk` → skopiuj RAZ do `.stage/apk/<nazwa>-v7a.apk` (build.sh nie kopiuje; dwa wątki kopiujące naraz dały złe md5).
- Instalacja i test: `ZASADY-tv.md` (md5 z urządzenia = md5 pliku, potem `compile -m speed`).
