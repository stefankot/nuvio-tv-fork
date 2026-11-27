# ZASADY PRACY — rdzeń (ładowany zawsze; aktualne na 27.11.2026)

Jedyne źródło zasad właściciela = ten plik + trzy pliki warunkowe niżej. Nowa zasada: dopisz z datą tu (jeśli dotyczy każdej pracy) albo do pasującego `ZASADY-*.md` — nigdzie indziej. Stare wersje: `plan/archiwum/zasady/`, historia gita.

## 0. Co czytać i kiedy (oszczędzaj tokeny)
| Kiedy | Co |
| --- | --- |
| „kontynuuj” | `plan/TERAZ.md` — sekcja „NAJPIERW” i kolejka |
| zanim zmienisz kod w `app/` dotykający plików bazy, wpięć, łatek, kopii z forków, efektów UI, ciężkich obliczeń; aktualizacja bazy | `ZASADY-kod.md` |
| zanim zbudujesz, skopiujesz lub zainstalujesz APK | `ZASADY-build.md` |
| zanim zaczniesz testy na JMGO albo emulatorze | `ZASADY-tv.md` (zasady + ściąga poleceń) |
| decyzja w obszarze zadania | `plan/DECYZJE.md` — TYLKO sekcja obszaru (`grep -n '^## ' plan/DECYZJE.md`, potem `sed -n`) |
| kolejka pusta / sprawdzenie, czy coś już zablokowane | `plan/BLOKADY.md` |
| temat zadania | `plan/iptv/`, `plan/autoplay/`, `plan/lektor/` + `plan/LEKTOR_*.md`, `plan/ustawienia/`, `plan/niunia-ke/`, `plan/glass-spis.md`, `plan/TESTY_PLAN.md`, raporty pętli `plan/ux/<temat>/WYNIKI.md`, `plan/raporty/<temat>.md` — tylko pasujący plik |
| pochodzenie skopiowanego kodu | `UPSTREAM_SOURCES.md` |

Nie czytaj: `plan/archiwum/` (tylko `grep`), `*.log`, `*.txt`, `plan/raporty/wydajnosc-surowe/`, `plan/probki/`, pliki bazy NuvioTV w katalogu głównym (`README.md`, `CONTRIBUTING.md`, `PRIVACY.md`, `ACCESSIBILITY_HANDOVER.md`). Duży plik: najpierw `grep`, potem tylko potrzebny fragment.
Dopisywanie bez czytania całości: `plan/DECYZJE.md`, `plan/BLOKADY.md`, `plan/DO_SPRAWDZENIA_NA_TV.md` — `cat >> plik <<'EOF'` (wpis na końcu).

## 1. Sposób pracy
- Autonomicznie, po polsku, krótko. Nie pytaj o potwierdzenie ani o kontynuację, nie kończ tury pytaniem. Przy niejasności wybierz wariant łatwiejszy do cofnięcia i mniej zmieniający rdzeń; jedno zdanie w `plan/DECYZJE.md`.
- Właściciela pytam tylko o decyzję, której nie rozstrzygają zasady ani DECYZJE; wtedy `afplay -v 10 /System/Library/Sounds/Sosumi.aiff`. Właściciel jest nietechniczny: nie każ mu uruchamiać skryptów.
- Zadanie nie wychodzi po 2 różnych podejściach albo czegoś brakuje (TV, konto, klucz, plik) → wpis w `plan/BLOKADY.md`, bierz następne.
- Zadanie jest tym, co napisano. Tylko zadanie główne: bez tłumaczeń (wyjątek: usunięcie duplikatów zasobów blokujących build), bez naprawiania i optymalizowania cudzego kodu. Narzędzia testowe zmieniam tylko, gdy blokują zadanie, najwyżej 30 min, potem BLOKADY.
- Raport etapu zaczyna się jednym zdaniem: ile realnych poprawek aplikacji powstało i ile sprawdzono na urządzeniu.
- Każde czekanie w tle: limit ≤ 20 min i warunek sprawdzony najpierw jednym ręcznym poleceniem (na pliku/wyniku, który naprawdę powstaje). Nigdy nie czekaj w ciszy — jedno zdanie, na co czekasz.
- Inne wątki pracują równolegle: przed commitem `git log`/`git pull`; commituj swoje pliki (po ścieżkach) od razu po poprawce.
- Ok. 95% kontekstu: zapisz stan w `plan/TERAZ.md` — ZASTĄP sekcję „NAJPIERW” gotowym promptem na samo „kontynuuj” (stan, wyniki testów, dokładne następne kroki; ≤ ok. 4 KB), poprzednią dopisz na koniec `plan/archiwum/TERAZ-historia-<daty>.md`; skrypty i notatki w `NuvioFork/` (nie w `/tmp` — znika po restarcie); commit + push.
- `cd` zawsze pełną ścieżką `/Users/milajovovich/Desktop/nuvio 2/NuvioFork`.

## 2. Kod (szczegóły: `ZASADY-kod.md`)
- Jak najmniej zmian w bazie (NuvioTV) i forkach (KE, Echo, Reshaped, Arc, One): kopie 1:1 z przypiętym SHA, nowe rzeczy w osobnych plikach (`fork/…`, `skin/…`, adaptery). Pliki bazy = kopie 1:1 z wydania — edycja zakazana poza wyjątkami z `ZASADY-kod.md`. Nowe funkcje nigdy w plikach bazy.
- Przed commitem zmian w `app/`: `bash fork-tools/check.sh` (limit plików bazy z wpięciami: 65).
- Każda funkcja: osobny moduł, przełącznik we wspólnej kategorii ustawień forka; wyłączona nic nie kosztuje (hook wraca od razu przy wyłączonym kluczu `ForkFeatures`) i nie zmienia zachowania.
- Funkcja jest zrobiona dopiero, gdy da się jej użyć w aplikacji (wejście w UI, domyślnie włączona) — nie gdy przechodzą testy.
- Nowe teksty: `res/values/<moduł>_strings.xml`, bez duplikatów ze `strings.xml`; dla użytkownika po polsku, prosto, bez słów technicznych.
- Zmiana formatu danych użytkownika: zawsze zachowaj stary plik obok.

## 3. Logi
- Każdy moduł loguje (`android.util.Log`, tag `Fork<Moduł>`, trafia do `fork/family/FullLog`): ważne czynności, wybory (co i dlaczego), wyniki i KAŻDY błąd (`${e.javaClass.simpleName}: ${e.message}`). Żadnych połkniętych wyjątków (`runCatching{}.getOrNull()`, pusty `catch`) — najpierw `Log.w`/`.logged(...)`.
- Błąd odczuwalny przez użytkownika („nic się nie dzieje”) także krótko na ekranie (`ForkLog.say`).
- W logach bez kluczy, haseł, tokenów, tekstu dialogów i adresów strumieni IPTV (login i hasło dostawcy w ścieżce).

## 4. Buildy — twarde zakazy (procedura: `ZASADY-build.md`)
- Gradle WYŁĄCZNIE `bash fork-tools/build.sh <nazwa-logu> <zadania...>` w zakładce terminala aplikacji. Zakazane: `./gradlew`, `./gradlew --stop`, `pkill` demona, zatrzymywanie cudzego buildu. Najwyżej 1 build naraz; build tylko z dużej paczki poprawek, wspólny z innymi wątkami.
- Emulator i kompilator nigdy razem (8 GB RAM).

## 5. Urządzenia — twarde zakazy (procedura, ustawienia urządzeń, pętle testów: `ZASADY-tv.md`)
- Na urządzeniach tylko fork `com.nuviodebug.com` (nie `com.nuvio.tv`); nie ruszaj cudzych aplikacji i danych. JMGO: ktoś może oglądać. OK tylko, gdy fokus jest na celu — nigdy na ślepo.

## 6. Bezpieczeństwo i git
- Klucze API tylko w `local.properties` (poza gitem); nigdy ich nie wpisuj na TV, nie commituj, nie wypisuj. Płatne testy dopiero po wpisaniu klucza i zgodzie właściciela.
- Repo prywatne `stefankot/nuvio-tv-fork`; zmiana widoczności tylko na polecenie właściciela. Gałąź robocza: `skin-niunia`.
- Zakazane: `push --force`, `reset --hard` na wypchniętych commitach, scalanie do `main`, usuwanie gałęzi, zmiana podpisu wydań i `applicationId`.

Dawne numery (odsyłacze w kodzie i starych plikach): §7 „Stałe decyzje produktowe” → `plan/DECYZJE.md`, pierwsza sekcja; §8 „Środowisko i pułapki” → `ZASADY-build.md`, `ZASADY-kod.md`, `ZASADY-tv.md`; §9 „Dyscyplina pracy” → §1 i `ZASADY-tv.md` / `ZASADY-build.md`; zapis stanu po każdej pętli testów → `ZASADY-tv.md`.
