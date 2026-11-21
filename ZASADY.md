# ZASADY PRACY (jedyne źródło; aktualne na 19.11.2026)

Zasady właściciela dla każdego wątku i agenta. Inne pliki tylko tu odsyłają. Nowa zasada: dopisz TUTAJ (z datą), nigdzie indziej.
Stare wersje: `plan/archiwum/zasady/` i historia gita.

## 1. Sposób pracy
- „kontynuuj” → otwórz `plan/TERAZ.md`, wykonuj sekcję „NAJPIERW” i kolejkę. Dawne plany są w `plan/archiwum/` (tylko grep).
- Pracuj autonomicznie, po polsku, krótko. Nie pytaj o potwierdzenie ani o kontynuację, nie kończ tury pytaniem. Przy niejasności wybierz wariant łatwiejszy do cofnięcia i mniej zmieniający rdzeń, zapisz go jednym zdaniem w `plan/DECYZJE.md`.
- Zadanie nie wychodzi po 2 różnych podejściach albo brakuje czegoś (TV, konto, klucz, plik) → wpis w `plan/BLOKADY.md`, bierz następne.
- Decyzja naprawdę potrzebna od właściciela → `afplay -v 10 /System/Library/Sounds/Sosumi.aiff`.
- Właściciel jest nietechniczny: nie każ mu uruchamiać skryptów.
- Tylko zadanie główne: bez tłumaczeń (wyjątek: usunięcie duplikatów zasobów blokujących build), bez naprawiania i optymalizowania cudzego kodu.
- Inne wątki pracują równolegle: przed commitem `git log`/`git pull`.
- Przy ok. 95% kontekstu: zapisz stan, wyniki testów i dokładne następne kroki w `plan/TERAZ.md` (sekcja „NAJPIERW” = gotowy prompt na samo „kontynuuj”), skrypty i notatki w `NuvioFork/` (nie w `/tmp`), commit + push.

## 2. Kod i moduły
- Jak najmniej zmian w kodzie bazy (NuvioTV) i forków (KE, Echo, Reshaped, Arc, One): kopie 1:1 z przypiętym SHA (`UPSTREAM_SOURCES.md`), nowe rzeczy w osobnych plikach/modułach (`fork/…`, adaptery). Nie podmieniaj plików bazy wersją z forka.
- Wpięcie w bazę = jedna instrukcja z komentarzem `// Fork hook: <moduł>`, wiersz w `fork-tools/hooks.tsv`, zmiany jako łatki w `fork-tools/patches/<seria>/`. Przed commitem `bash fork-tools/check.sh` (limit plików z wpięciami: 65).
- Kicia (`~/Desktop/nuvio-tv-kicia`) jest zamrożona: jej kod można dowolnie dopasować.
- Każda funkcja: osobny moduł, przełącznik we wspólnej kategorii ustawień forka; wyłączona nic nie kosztuje (hook wraca od razu, gdy klucz `ForkFeatures` wyłączony) i nie zmienia zachowania.
- Funkcja jest zrobiona dopiero, gdy da się jej użyć w aplikacji (wejście w UI, domyślnie włączona) — nie gdy przechodzą testy.
- Nowe teksty: `res/values/<moduł>_strings.xml`, bez duplikatów ze `strings.xml`. Teksty dla użytkownika po polsku, proste, bez słów technicznych.
- Efekty interfejsu (rozmycie itp.) tylko natywnie (RenderEffect API 31+, RuntimeShader API 33+); na starszych (JMGO, Android 11) zwykłe tło z tokenów skórki, bez symulacji.
- Wygląd: istniejące komponenty; skórki przez `skin/` (właściciel zrobi własny projekt).
- Zmiana formatu danych użytkownika: zawsze zachowaj stary plik obok.

## 3. Logi
- Każdy moduł loguje (`android.util.Log`, tag `Fork<Moduł>`, trafia do `fork/family/FullLog`): ważne czynności, wybory (co i dlaczego), wyniki i KAŻDY błąd (`${e.javaClass.simpleName}: ${e.message}`). Żadnych połkniętych wyjątków (`runCatching{}.getOrNull()`, pusty `catch`) — najpierw `Log.w`/`.logged(...)`.
- Błąd odczuwalny przez użytkownika („nic się nie dzieje”) także krótko na ekranie (`ForkLog.say`).
- W logach bez kluczy, haseł, tokenów i tekstu dialogów.

## 4. Buildy
- Gradle WYŁĄCZNIE `bash fork-tools/build.sh <nazwa-logu> <zadania...>` w `NuvioFork`, w zakładce terminala aplikacji (skrypt daje `--console=rich` i `/tmp/<nazwa>.log`). Bezpośrednie `./gradlew` zakazane.
- Najwyżej 1 build naraz, 1 w kolejce; nowe zlecenie anuluje czekające (kod 75 = anulowany, nie błąd).
- Nigdy `./gradlew --stop`, `pkill` demona ani zatrzymywanie cudzego buildu.
- Build tylko z DUŻEJ paczki poprawek, nigdy dla jednej. Drobne poprawki: commit bez buildu, lista paczki w `plan/TERAZ.md`; testy w tym czasie na ostatnim APK.
- Jedna architektura: JMGO/Xiaomi `-PforkAbi=armeabi-v7a`, emulator `-PforkAbi=arm64-v8a`.
- Buildy wykonywane przez Codexa zawsze dają jeden APK ARM32 z `-PforkAbi=armeabi-v7a` (21.11.2026).
- Przed buildem: `df -h ~` (ok. 15 GB wolne), zamknij zbędne procesy (emulator). Pilnuj CPU.
- W czasie builda nie czekaj bezczynnie: pisz dalej w `.stage/` (nie ruszaj plików, które Gradle czyta).

## 5. Testy na urządzeniu
- Tylko fork `com.nuviodebug.com` (nie `com.nuvio.tv`), start: `am start -n com.nuviodebug.com/com.nuvio.tv.launcher.AppIconDefault`. Nie ruszaj cudzych aplikacji i danych.
- Po instalacji: porównaj md5 (`adb pull`) z plikiem z buildu, potem `cmd package compile -m speed -f com.nuviodebug.com`.
- JMGO: `adb connect 192.168.31.252:5555` (Android 11, armeabi-v7a, profil właściciela zalogowany). Ostrożnie — ktoś może oglądać.
- Testy tekstowo (`fork-tools/test/{ui,tap,go,focus}.sh`, uiautomator); zrzuty ekranu tylko wyjątkowo. OK tylko gdy fokus jest na celu, nigdy na ślepo.
- Emulator tylko na testy, nigdy razem z Gradle (8 GB RAM); po testach `adb emu kill`. Logowanie QR na emulatorze nie działa → „Continue without account”.
- Czego nie da się sprawdzić bez TV → `plan/DO_SPRAWDZENIA_NA_TV.md`.

## 6. Bezpieczeństwo i git
- Klucze API tylko w `local.properties` (poza gitem); nigdy ich nie wpisuj na TV, nie commituj, nie wypisuj. Płatne testy dopiero po wpisaniu klucza i zgodzie właściciela.
- Repo prywatne `stefankot/nuvio-tv-fork`; zmiana widoczności tylko na polecenie właściciela.
- Zakazane: `push --force`, `reset --hard` na wypchniętych commitach, scalanie do `main`, usuwanie gałęzi, zmiana podpisu wydań i `applicationId`.

## 7. Stałe decyzje produktowe
- Napisy wbudowane w plik zawsze przed napisami z dodatków (wybór w odtwarzaczu, źródło AI napisów, ranking).
- KE: tylko katalogi IPTV, filtry kanałów w EPG, testy łącza i sprzętu. IPTV/EPG z Reshaped.
- Reshaped: z Dolby Vision i HDR10, bez aktualizatora, bez `epg-header`. Oficjalny odtwarzacz zostaje.
- Pilot Arc w całej aplikacji (także IPTV). Rozpoznawanie mowy wielojęzyczne (Whisper tiny/base/small do wyboru).

## 8. Środowisko i pułapki
- `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`; przy `cd` zawsze pełna ścieżka `/Users/milajovovich/Desktop/nuvio 2/NuvioFork`.
- Mac 8 GB RAM, Gradle `-Xmx3072m`; pakowanie APK czasem pada bez przyczyny — powtórzyć.
- Upstream tylko do odczytu: `upstream/{nuvio,ke,reshaped,arc,echo,one}`.
- KSP ukrywa prawdziwe błędy („could not be resolved”): szukaj brakującej klasy w konstruktorze `@Inject`.
- Reshaped wymaga ffmpeg z `app/libs/*.aar`. Stringi Arc są w osobnych plikach `values/strings_*.xml`.
- Pliki w `/tmp` mogą zniknąć po restarcie.
