# KONTYNUUJ: protokół pracy autonomicznej

Ten plik jest jedyną instrukcją startową. Gdy właściciel napisze „kontynuuj” (albo cokolwiek równoważnego), wykonaj pętlę poniżej. Nie streszczaj planu, nie pytaj o zgodę, nie proponuj: pracuj.

Język pracy, commitów i notatek: polski. Właściciel nie jest techniczny i nie uruchamia żadnych poleceń.

## 1. Pętla pracy

1. Przeczytaj `plan/STAN.md`. Wskazuje bieżący blok i zadanie.
2. Jeśli w `STAN.md` jest wpis „build w tle”, sprawdź jego wynik (sekcja 4). Czerwony build naprawiasz przed wszystkim innym.
3. Otwórz plik bieżącego bloku z `plan/bloki/` i wykonaj pierwsze zadanie ze stanem `[ ]`.
4. Wykonaj zadanie do końca: kod, testy, kryterium ukończenia z pliku bloku.
5. Zapisz wynik: oznacz zadanie `[x]` w pliku bloku, dopisz jedną linię do dziennika w `STAN.md`, zrób commit (sekcja 5).
6. Wróć do kroku 2. Po ostatnim zadaniu bloku przejdź do następnego bloku według tabeli w sekcji 8.
7. Pętla kończy się dopiero, gdy wszystkie bloki mają wszystkie zadania `[x]` albo `[blokada]`.

Jedno zadanie = jedna paczka = jeden commit. Nie zaczynaj drugiego zadania, zanim pierwsze nie jest zapisane w `STAN.md`.

## 2. Zakazy

- **Nie pytaj właściciela o nic.** Odpowiedzi na znane pytania są w `plan/DECYZJE.md`. Nową decyzję podejmujesz sam według sekcji 6 i zapisujesz ją tam.
- **Nie czekaj bezczynnie na build.** Żadnego `sleep`, żadnego obserwowania logu, żadnego kończenia tury „do czasu builda”. Build idzie w tle, a ty robisz następną rzecz (sekcja 4).
- **Nie przerywaj pracy bez bezpośredniej zgody właściciela.** Nie kończ tury podsumowaniem, pytaniem „czy kontynuować” ani listą propozycji. Jeśli tura musi się skończyć z przyczyn technicznych, ostatnią czynnością jest aktualizacja `STAN.md`, tak aby następne „kontynuuj” ruszyło od tego samego miejsca.
- **Nie zatrzymuj się na przeszkodzie.** Zadanie, którego nie da się dokończyć, dostaje stan `[blokada]` i wpis w `plan/BLOKADY.md`; bierzesz następne.
- **Nie oznaczaj `[x]` czegoś, co nie przeszło kryterium ukończenia.** Jeśli kryterium wymaga telewizora, zadanie jest `[x]` po spełnieniu części możliwej bez telewizora, a część telewizyjna trafia do `plan/DO_SPRAWDZENIA_NA_TV.md`.

Jedyny wyjątek od zakazu przerywania to stały protokół limitów właściciela: gdy wskaźnik limitu 5-godzinnego przekroczy 95% albo tygodniowego 97%, dokończ bieżący krok, zrób commit i push, zaktualizuj `STAN.md` i podaj krótki prompt do kontynuacji w innym wątku (cel, stan, gdzie zapisane, następny krok, kluczowe decyzje).

## 3. Zasady architektury (obowiązują w każdym zadaniu)

- **Baza pracy to gałąź `final`**, nie `main`. Obowiązuje na niej architektura z `ARCHITECT_PLAN.md`; przeczytaj jego sekcje 2.0 (zasady R1–R9), 2.1, 2.7 i 4 przed pierwszym zadaniem. Ten plan ją rozszerza, nie zastępuje. Kroków z `ARCHITECT_PLAN.md` sekcja 7 nie wykonujesz; to osobny wątek.
- Każda funkcja to osobny moduł w pakiecie `com.nuvio.tv.fork.<nazwa>`. Kod bazy Nuvio i forków (Reshaped, KE, One, Arc, Echo) zmieniasz tylko punktami wpięcia.
- Punkt wpięcia to jedna instrukcja wołająca `ForkHooks` (plik `fork/ForkHooks.kt`), zakończona komentarzem `// Fork hook: <moduł>`, z wierszem w `fork-tools/hooks.tsv`. Limit: 35 plików ręcznie utrzymywanych; liczy go `fork-tools/check.sh`, który uruchamiasz przed każdym commitem. Margines jest mały (ok. 26 zajętych), więc nowe wpięcia dodawaj przede wszystkim do plików, które już są na liście.
- Wyłączona funkcja nic nie kosztuje: każde wpięcie przechodzi przez funkcję, która wraca natychmiast, gdy klucz w `ForkFeatures` jest wyłączony.
- Wyjątek: seria łatek „dostawca” z bloku B01 zmienia wnętrze plików Reshaped. Każda zmiana ma komentarz `// Fork patch: dostawca #<nr>` i plik łatki w `fork-tools/patches/` (szczegóły w B00-6).
- Wygląd jest wymienny przez kontrakt skórki (`skin/api`). Nowe ekrany modułów rejestruj pod nazwą trasy, tak aby skórka mogła je zastąpić przez `Skin.screen(trasa)`.
- Każdy moduł ma główny wyłącznik w ustawieniach. Wyłączony moduł nie zmienia zachowania aplikacji.
- Logika bez Androida idzie do czystego Kotlina z testami JVM. Typy Reshaped importuje tylko pakiet `most` danego modułu.
- Kod z Kicia kopiuj 1:1 tam, gdzie blok tak mówi, zmieniając tylko nazwę pakietu. Źródło: katalog `../nuvio-tv-kicia`. Gdy go nie ma: `git clone --depth 1 https://github.com/stefankot/nuvio-tv-kicia ../nuvio-tv-kicia` (tylko do odczytu).
- Wygląd: używaj istniejących komponentów aplikacji. Skórka zostanie podłączona osobno; nie projektuj własnych kolorów ani typografii.
- Teksty dla użytkownika po polsku, proste, bez słów technicznych (bufor, dekoder, strumień).

## 4. Build i testy bez czekania

Na początku pierwszej tury ustal i zapisz w `STAN.md`, co jest dostępne w tym środowisku: JDK 17, Android SDK, emulator, dostęp do sieci dla Gradle, `gh`.

**Gdy Gradle działa lokalnie:**

1. Po commicie uruchom w tle, jeden build naraz (komputer właściciela ma 8 GB RAM):
   `nohup ./gradlew :app:assembleFullDebug :app:testFullDebugUnitTest -PforgeArm64Only=true > /tmp/fork-build.log 2>&1; echo $? > /tmp/fork-build.status` (całość w tle).
2. Zapisz w `STAN.md`: „build w tle: commit <hash>”.
3. Od razu zacznij następne zadanie.
4. Na granicy każdego zadania sprawdź `/tmp/fork-build.status`. Brak pliku = build trwa, pracuj dalej. `0` = zielony, usuń wpis. Inna wartość = przeczytaj końcówkę logu, napraw, commit, nowy build w tle.
5. Zadanie zależne od wyniku builda, który jeszcze trwa: weź inne zadanie z tego samego albo następnego bloku. Zawsze jest co robić: testy JVM, testy kontraktowe, kolejny moduł czystej logiki.

**Gdy Gradle nie działa w tym środowisku:** nie próbuj go naprawiać dłużej niż 10 minut. Pracuj dalej, a weryfikację zostaw CI z bloku B00 (push gałęzi uruchamia build i testy na GitHubie). Wynik CI sprawdzasz przez `gh run list` na granicy zadań, tak samo jak plik statusu. Gdy nie ma ani Gradle, ani `gh`: dopisz commit do listy „niezweryfikowane buildem” w `STAN.md` i pracuj dalej.

**Testowanie każdego zadania:** testy JVM dla logiki; test kontraktowy dla wpięć; gdy jest emulator, instalacja APK i kroki z kryterium ukończenia przez `adb`. Czego nie da się sprawdzić bez prawdziwego telewizora, zapisz w `plan/DO_SPRAWDZENIA_NA_TV.md` jako listę „co kliknąć, czego się spodziewać”.

## 5. Git

- Gałąź robocza: `auto/wdrozenie`, utworzona w zadaniu B00-1 z gałęzi `final`. Nigdy nie commituj do `main`, `final` ani do gałęzi forków. Na początku każdego bloku wciągnij nowe commity z `final` (`git merge origin/final`); konflikt rozwiąż na korzyść `final` i popraw swój kod.
- Commit po każdym zadaniu: `B03-2: krótki opis`. Po commicie `git push -u origin auto/wdrozenie`, jeśli push jest możliwy.
- Zakazane: `push --force`, `reset --hard` na wypchniętych commitach, scalanie do `main`, usuwanie gałęzi, zmiany poza katalogiem repozytorium (poza odczytem `../nuvio-tv-kicia`).
- Żadnych kluczy, haseł ani adresów list kanałów w repozytorium. Adres listy właściciela zawiera token.
- Nie zmieniaj podpisywania wydań ani `applicationId`.

## 6. Decyzje bez pytania

1. Sprawdź `plan/DECYZJE.md`.
2. Jeśli decyzji tam nie ma: wybierz wariant, który łatwiej cofnąć i który mniej zmienia rdzeń. Przy remisie wybierz prostszy dla użytkownika nietechnicznego.
3. Dopisz decyzję do `DECYZJE.md` w sekcji „Podjęte przez wykonawcę” z jednym zdaniem uzasadnienia.
4. Rzeczy nieodwracalne dla danych użytkownika (migracja formatu zapisu, kasowanie plików profilu) rób zawsze z zachowaniem starego pliku obok.

## 7. Blokady

Zadanie blokujesz, gdy po dwóch różnych podejściach nadal nie działa albo wymaga czegoś, czego tu nie ma (telewizor, konto, klucz, plik wideo). Wpis w `BLOKADY.md`: numer zadania, co próbowałeś, czego brakuje, co właściciel może dostarczyć. Potem następne zadanie. Na początku każdego nowego bloku przejrzyj `BLOKADY.md`: jeśli coś da się już odblokować, zrób to najpierw.

## 8. Kolejność bloków

Od najpilniejszego. Czas to budżet pracy modelu; przekroczenie o więcej niż 100% oznacza blokadę reszty zadań bloku i przejście dalej.

| # | Plik | Co daje | Budżet |
| --- | --- | --- | --- |
| B00 | `bloki/B00-fundament.md` | gałąź, build, CI, rejestr modułów, test wpięć, autoaktualizacja | 4 h |
| B01 | `bloki/B01-dostawca-epg-logo.md` | telewizja działa dobrze u dostawcy właściciela; EPG i logo same | 4 h |
| B02 | `bloki/B02-tv-rdzen.md` | scalanie serii, postęp, obejrzane, alerty | 4 h |
| B03 | `bloki/B03-tv-ekrany.md` | przewodnik z rzędem „Dla Ciebie”, grupy, strona programu, MiniGuide | 4 h |
| B04 | `bloki/B04-zrodla-napisy.md` | brak wyboru źródeł, polski dźwięk z automatu, napisy bez ręcznej pracy | 4 h |
| B05 | `bloki/B05-ustawienia-noc.md` | poziom Prosty, tryb nocny z harmonogramem | 4 h |
| B06 | `bloki/B06-launcher-blokada.md` | Nuvio jako ekran główny, blokada wyjścia, start z telewizorem | 3 h |
| B06a | `bloki/B06a-skorka-ke.md` | układ ekranu głównego z Nuvio KE jako skórka | 4 h + 4 h |
| B06b | `bloki/B06b-silnik-ai.md` | lokalny model znaczeń i paczka wektorów; AI bez klucza | 4 h |
| B07 | `bloki/B07-glos.md` | sterowanie głosem: 60 wzorców, Whisper, model po API | 4 h + 4 h |
| B08 | `bloki/B08-tv-odkrywaj-szukaj.md` | widok Odkrywaj, szukanie, typy programów, okładki | 4 h |
| B09 | `bloki/B09-lektor-ai.md` | głos czytający polskie napisy | 4 h |
| B10 | `bloki/B10-polecenia.md` | listy według nastroju; ocena Scout z Arc; ranking modelem | 4 h + 4 h |
| B11 | `bloki/B11-eksperymenty.md` | próba DubLift, pomiar mowy, głos neuronowy, tłumaczenie napisów | 4 h |

Pełny opis projektu (tło, uzasadnienia): https://claude.ai/artifact/6xPkSbauAknNMT5sUuRDqr. Pliki bloków są samowystarczalne; dokument czytaj tylko, gdy czegoś w nich brakuje.
