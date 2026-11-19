# Kontynuacja: fork Nuvio Android TV

**Stan na koniec wątku 14 (19.11 ok. 15:55 zegar Maca):** gałąź `hybryda-zrodel`, na JMGO build120, kolejka w `NuvioFork/plan/TERAZ.md` sekcja „NAJPIERW (wątek 14)” (lektor czeka na odnowienie limitu Gemini TTS). Dawniej — **Stan na koniec wątku 12 (19.11 ok. 15:10 zegar Maca):** gałąź `hybryda-zrodel`, kolejka w `NuvioFork/plan/TERAZ.md` sekcja „NAJPIERW (wątek 13)” (na JMGO build119; build120 ze scenami lektora do zbudowania; lektor blokowany limitem Gemini TTS Tier 1: 100 zapytań/dzień). Dawniej — **Stan na koniec wątku 10 (19.11 ok. 11:20):** gałąź `hybryda-zrodel`, kolejka w `NuvioFork/plan/TERAZ.md` sekcja „NAJPIERW (wątek 11)”. Dawniej — **Stan na koniec wątku 3 (18.11 ok. 22:45):** gałąź `hybryda-zrodel` (GitHub), kolejka i decyzje o lektorze/TTS w `NuvioFork/plan/TERAZ.md` sekcja „NAJPIERW (wątek 4)”.

**NAJPIERW (18.11.2026, nadrzędne wobec wszystkiego niżej):** gdy użytkownik napisze „kontynuuj”, otwórz `NuvioFork/plan/TERAZ.md` i wykonuj jego instrukcję i kolejkę. Starsze sekcje tego pliku i `NuvioFork/CLAUDE.md` / `plan/KONTYNUUJ.md` są historyczne.

**ZASADA STAŁA (19.11, właściciel): jak najmniejsze zmiany w kodzie bazy i forków, konstrukcja modułowa (cel: łatwe scalanie nowych wydań).** Nowe rzeczy w osobnych plikach, w bazie tylko jednoliniowe hooki. Pełny tekst: `~/Desktop/zasady developementu/ZASADY_OGOLNE.md`.

**ZASADA STAŁA (19.11, właściciel): wszystkie moduły zapisują do logu** — każda ważna czynność, wybór i KAŻDY błąd (typ + komunikat), żadnych połkniętych wyjątków; błąd odczuwalny przez użytkownika także na ekranie; bez kluczy i tekstu dialogów. Pełny tekst: `~/Desktop/zasady developementu/ZASADY_OGOLNE.md`.

**ZASADA STAŁA (19.11, właściciel): ostry limit buildów — najwyżej 1 build równocześnie, najwyżej 1 w kolejce, przed każdym buildem wyzerować kolejkę.** Gradle WYŁĄCZNIE przez `bash fork-tools/build.sh <nazwa-logu> <zadania...>` (w `NuvioFork`, w zakładce terminala); bezpośrednie `./gradlew` zakazane. Pełny tekst: `NuvioFork/fork-tools/ZASADA_LIMIT_BUILDOW.md`.

**ZASADA STAŁA (18.11, właściciel): NIE wolno robić buildu z jedną poprawką; buildy tylko w DUŻYCH paczkach** (drobne poprawki z testów: commit bez buildu, do następnej paczki). Pełny tekst: `~/Desktop/zasady developementu/ZASADA_BUILDY_W_PACZKACH.md`.

**ZASADA STAŁA (18.11): przy ok. 95% kontekstu** przerwij pracę, zapisz wszystko w repozytorium (`NuvioFork/plan/TERAZ.md`: stan, wyniki testów, paczka poprawek, dokładne następne kroki; skrypty i notatki w `NuvioFork/`, nie w `/tmp`), commit + push (`origin`, bieżąca gałąź). Sekcja „NAJPIERW” w TERAZ.md ma być takim promptem, żeby samo „kontynuuj” w nowym wątku od razu uruchomiło pracę bez pytań.

Gdy użytkownik napisze „kontynuuj", wykonaj sekcję „PROCEDURA KONTYNUACJI" poniżej. Pracuj autonomicznie, po polsku, krótko. Nie pytaj o potwierdzenie ani o kontynuację; nie czekaj bezczynnie na kompilację (rób następny punkt równolegle).

## PROCEDURA KONTYNUACJI
1. `cd NuvioFork && git status && git branch --show-current`. Aktualna gałąź robocza: `arc-remote` (patrz „Gałęzie").
2. Przeczytaj `NuvioFork/ARCHITECT_PLAN.md`, jeśli istnieje (plan innego, mocniejszego modelu; patrz „Plan architekta"). Jeśli istnieje, wykonuj jego kroki po kolei zamiast listy „Do zrobienia" poniżej; w razie sprzeczności z wymaganiami użytkownika wygrywają wymagania użytkownika.
3. Jeśli planu nie ma: sprawdź, czy ostatnia kompilacja `arc-remote` przeszła (komenda w „Środowisko"). Napraw błędy, zacommituj, zbuduj APK, zaktualizuj `NuvioFork/STATUS.md` i `HANDOFF.md`.
4. Potem „Do zrobienia" od góry.
5. Po każdym kroku: commit, aktualizacja `STATUS.md`. Na końcu: APK + lista funkcji (działające / częściowe / zablokowane).

## STAN NA KONIEC WĄTKU 1 (16.11.2026)
- Ostatnia kompilacja `arc-remote` (build39, po naprawie 4 błędów: 3 teksty `player_pill_*` + ikona w `PlaybackRemoteButtons.kt`) była w toku; wynik NIEZNANY. Log: `/tmp/build39.log` (jeśli zniknął, uruchom kompilację od nowa).
- Zmiany w `arc-remote` są zacommitowane (WIP). Jeśli kompilacja przejdzie: zbuduj APK, zaktualizuj `STATUS.md`/`HANDOFF.md`, potem hook pilota w `MainActivity`.
- Użytkownik wkleja `ARCHITECT_PROMPT.md` do innego modelu; sprawdź, czy istnieje `NuvioFork/ARCHITECT_PLAN.md`.


## STAN NA KONIEC WĄTKU 2 (16.11.2026) — nadrzędny wobec „STAN NA KONIEC WĄTKU 1"
- Gałąź robocza: `final` (z `vendor/nuvio` = czysta baza). Kroki 1–13 planu architekta wykonane; szczegóły i wyniki testów: `NuvioFork/STATUS.md`. `bash fork-tools/check.sh` przed każdym commitem (limit plików z hookami: 50 od 18.11; teraz 36).
- Zostało: Echo A4 (wyścig źródeł), A5 (menu audio: hooki i UI), A6 (intro) — kod skopiowany i kompiluje się, ale NIE podpięty w plikach odtwarzacza; potem dokumenty końcowe (HANDOFF.md, UPSTREAM_SOURCES.md) i lista funkcji. Testy Echo (13–18) wymagają addonu od użytkownika.
- Emulator: TYLKO fork `com.nuviodebug.com` (applicationId z build.gradle.kts:369; NIE `com.nuvio.tv`), start: `am start -n com.nuviodebug.com/com.nuvio.tv.launcher.AppIconDefault`; po każdej instalacji porównaj skrót APK (`adb pull` + `md5`). Testuj tekstem (`uiautomator dump`), bez zrzutów. Emulator tylko na testy, nigdy razem z Gradle. Nie ruszaj cudzych aplikacji na emulatorze. Logowanie QR nie działa (Supabase = localhost): „Continue without account".
- Wygląd (skin) nie ma znaczenia teraz: właściciel zmieni wszystko własnym projektem (moduł `skin/`, rama gotowa, KitSkin pusty).
- Pamięć użytkownika: działaj autonomicznie, nie pytaj, nie zatrzymuj się.

## Pliki z wiedzą (czytaj w razie potrzeby)
- `NuvioFork/HANDOFF.md`: dokładny stan, ścieżki, przyczyny zaparkowania.
- `NuvioFork/STATUS.md`: krótki stan i decyzje użytkownika.
- `NuvioFork/UPSTREAM_SOURCES.md`: pochodzenie plików (repo, SHA, ścieżki).
- `NuvioFork/ARCHITECT_BRIEF.md` i `ARCHITECT_PROMPT.md`: zlecenie dla architekta; wynik: `ARCHITECT_PLAN.md`.

## Wymagania użytkownika (obowiązujące, nadrzędne)
- Baza: NuvioTV `dev` commit `2c853c1` (w forku `85f9010aa`). Wcześniej tylko lokalnie; 16.11 użytkownik poprosił o wypchnięcie do PRYWATNEGO repo https://github.com/stefankot/nuvio-tv-fork (remote `origin`; `arc-remote` → `main`, plus rs-module, yt, echo, ke2, wip-all). Dalsze pushe i zmiana widoczności tylko na wyraźne polecenie użytkownika.
- Każdy fork = osobny moduł (katalogi, wyjątek: YouTube wymaga modułów Gradle `mediaservicecore`), CIENKIE adaptery, kod 1:1, bez przepisywania i optymalizacji. Limit plików rdzenia z punktami wpięcia: 50 (18.11: właściciel uznał 35 za sztuczny; ważniejsze, żeby wszystko działało). Każda funkcja musi mieć wejście w interfejsie, nie tylko kod i testy. Każda funkcja z przełącznikiem w jednej wspólnej kategorii ustawień (obecnie sekcja Reshaped). Oficjalny odtwarzacz zostaje.
- Zakres KE: tylko katalogi IPTV (kraje/kategorie/języki), filtry kanałów w EPG, testowanie łącza i sprzętu. Reszta KE zaparkowana. IPTV/EPG tylko z Reshaped.
- Arc: pilot ma działać w CAŁEJ aplikacji, także w IPTV. Miniatury i cache przewijania z Reshaped (nie kopiować z Arc). Home Catalog Order jest już w bazie. Kalendarz i wyszukiwarka ustawień zaparkowane.
- Reshaped: z Dolby Vision i HDR10, BEZ aktualizatora (usunięty). Wzorzec przełączania wyglądu: DaoeZ/notflix (nie analizowany). NIE dodawać `epg-header`.
- Rozpoznawanie mowy ma być wielojęzyczne (Whisper tiny/base/small wybierane w ustawieniach).
- Nie zajmować się tłumaczeniami (poza usuwaniem duplikatów zasobów, gdy blokują build). Nie naprawiać cudzego kodu, nie optymalizować.
- Zakaz zajmowania się czymkolwiek poza zadaniem głównym.

## Preferencje użytkownika
- Nietechniczny; nie każ mu uruchamiać skryptów.
- Gdy potrzebujesz czegoś od użytkownika: zagraj głośny dźwięk: `afplay -v 10 /System/Library/Sounds/Sosumi.aiff`.
- **(18.11, ZASADA STAŁA) Każdą kompilację/build uruchamiaj ZAWSZE w zakładce terminala aplikacji z `--console=rich`** (właściciel chce widzieć postęp procentowy), z `| tee /tmp/buildN.log`, żeby sprawdzić wynik.
- Pilnuj obciążenia CPU (co ~30 s) i zawsze zamykaj emulator Androida po sprawdzaniu (`adb emu kill`).
- Dysk bywa pełny: przed dużymi operacjami `df -h ~` (potrzeba ~15 GB).

## Środowisko
- **(18.11, ZASADA STAŁA) Buduj tylko jedną architekturę, tę potrzebną do testu:** JMGO/Xiaomi = `-PforkAbi=armeabi-v7a` (32 bit), emulator arm64 = `-PforkAbi=arm64-v8a`. Nigdy wszystkich naraz (oszczędność czasu). Przed buildem zamknij zbędne procesy (emulator, stare demony).
- `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`
- Kompilacja kodu: `cd NuvioFork && ./gradlew :app:compileFullDebugKotlin -PforgeArm64Only=true --console=plain > /tmp/buildN.log 2>&1` (uruchamiaj w tle, wynik: `grep BUILD`, `grep "^e: "`). Zawsze używaj bezwzględnej ścieżki `/Users/milajovovich/Desktop/nuvio 2/NuvioFork` przy `cd` (kilka razy build ruszył w złym katalogu).
- APK: `./gradlew :app:assembleFullDebug -PforgeArm64Only=true`; wynik: `NuvioFork/app/build/outputs/apk/full/debug/app-full-arm64-v8a-debug.apk`.
- Mac 8 GB RAM; Gradle `-Xmx3072m` (`NuvioFork/gradle.properties`). Kompilacja Kotlin 4–11 min (swap). Pakowanie APK raz padło bez przyczyny w kodzie; powtórzenie przeszło.
- Nic nie było uruchamiane na urządzeniu ani emulatorze. „Kompiluje się" to jedyna weryfikacja.
- Klony upstream (tylko odczyt): `upstream/{nuvio,ke,reshaped,arc,echo,one}`; łatka Reshaped: `upstream/reshaped.patch`.

## Gałęzie (`NuvioFork`, git)
- `rs-module` → `yt` → `echo` → `ke2` (kompiluje, APK 105 MB) → `arc-remote` (robocza: pilot Arc + wybór modelu mowy; ostatnie 4 błędy naprawione, kompilacja w toku/niezweryfikowana).
- `main` (porzucony monolit Reshaped), `ke` (porzucona próba KE), `wip-all` (archiwum surowych kopii Arc/Echo/KE/One). Nie używać.

## Co jest wdrożone (szczegóły: HANDOFF.md)
Reshaped (moduł, 759 nowych plików + 71 rdzenia), YouTube, katalog IPTV KE (`livetv/bridge/KeCatalogViewModel.kt` → Reshaped Live TV przez M3U), przełączniki w ustawieniach (`ForkFeatures`), w toku: pilot Arc (`RemotePlayerHook`, hook w `PlayerScreen.kt` obok Live TV) i wybór modelu mowy (`AsrModelChoice`).

## Do zrobienia (kolejność, jeśli nie ma ARCHITECT_PLAN.md)
1. Dokończyć `arc-remote`: kompilacja, commit, hook pilota w `MainActivity.dispatchKeyEvent` dla ekranów poza odtwarzaczem.
2. Zbudować APK z `arc-remote`; zmergować gałęzie w jedną gałąź docelową (np. `final`).
3. Zaparkowane: Echo Auto Quality (problem A), KE testowanie sprzętu/łącza (B), KE powłoka/Home/TV Home + notflix (C), Arc kalendarz (D), Arc wyszukiwarka ustawień (E). Rozwiązania ma wskazać architekt.
4. Redukcja zmodyfikowanych plików rdzenia (limit 50) i strategia scalania nowych wydań bazy/forków.
5. Test na emulatorze Android TV (Live TV, katalog KE, YouTube, napisy z audio z Whisper, pilot).
6. Finalne APK + lista funkcji (działające / częściowe / zablokowane).

## Plan architekta
Użytkownik wkleja `NuvioFork/ARCHITECT_PROMPT.md` do innego modelu; wynik: `NuvioFork/ARCHITECT_PLAN.md`. Gdy plik istnieje, ma pierwszeństwo przed listą „Do zrobienia" (patrz procedura).

## Pułapki, które już kosztowały czas
- Pliki stringów Arc leżą w osobnych plikach (`values/strings_remote_buttons.xml`, `strings_*.xml`), nie w `strings.xml`.
- Zdublowane zasoby między `strings.xml` a plikami modułów psują build: usuwaj duplikaty z pliku modułu.
- KSP ukrywa prawdziwe błędy ("could not be resolved"); szukaj brakującej klasy w konstruktorze `@Inject`.
- Reshaped ma bibliotekę ffmpeg w `app/libs/*.aar` (wymagana, inaczej `setDeniedTranscodeMimes` nie istnieje).
- Wyniki skryptów w `/tmp` mogą zniknąć po restarcie (`/tmp/rs/*`, `/tmp/whisper/*`).
