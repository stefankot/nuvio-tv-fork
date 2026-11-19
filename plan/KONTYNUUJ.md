# KONTYNUUJ: protokół pracy autonomicznej (HISTORYCZNY — aktualnie `plan/TERAZ.md` i `ZASADY.md`)

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

## 2–7. Zasady
Przeniesione do `ZASADY.md` (jedyne źródło); dawna treść w historii gita. Gałąź `auto/wdrozenie` i budowanie przez `./gradlew` są nieaktualne.

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
