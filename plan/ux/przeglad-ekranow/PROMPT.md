# PROMPT — wdrożenie poprawek z przeglądu ekranów 27.11 (pętla: kod → build → testy ręczne)

Wdroż poprawki z `plan/ux/przeglad-ekranow/NOTATKA.md` w pętlach, bez zatrzymywania się, aż lista będzie pusta albo zablokowana. OSZCZĘDZAJ TOKENY.

## Czytanie (tylko to, co potrzebne)
- `ZASADY.md` ładuje się sam. Przed pierwszą zmianą w `app/` → `ZASADY-kod.md`; przed buildem → `ZASADY-build.md`; przed testami na TV → `ZASADY-tv.md`. Każdy raz na sesję.
- NOTATKA: najpierw `grep -n '^## ' plan/ux/przeglad-ekranow/NOTATKA.md`, potem `sed -n` TYLKO sekcji, którą właśnie robisz. Nie czytaj całej.
- Kod: `grep -n` + `sed -n` zakresu z NOTATKI (ścieżki i numery linii tam są). Żadnego czytania całych plików, żadnych `Read` > 150 linii.
- Decyzje właściciela z 27.11: `grep -n '27.11 (właściciel, przegląd' plan/DECYZJE.md`.

## Kolejność (paczki ≈ 1 build każda)
1. **Psuje oglądanie:** 03 (kontrola długości pliku ±10% + klip < 3 min → następne źródło), 26 (przyciski ekranu wczytywania), 04 (fokus gubiony w liście napisów; polskie na górze; „Inne języki” zwinięte), 05 pkt 3 (wielokrotne przełączanie źródła), 22 (archiwum: bez „Na żywo”/zmiany kanału, ◀▶ przewija).
2. **Live TV dane:** 16 („AR:” = arabskie, nie Argentyna), 15 (Indie / kody 3-literowe), 14 (najpierw Polska po dociągnięciu pełnej listy; kategorie filtrują od razu), 20 (Ulubione/Oglądaj dalej bez filtra kraju; komunikat; pamięć podręczna sekcji), 21 (stałe numery kanałów), 12 (czysta nazwa kanału, tytuł bez ucinania).
3. **Nawigacja / YouTube:** 01 (karta tytułu: ←/→ zmienia tytuł, OK = Szczegóły), 08 („O serialu” działa + podpis), 23 (fokus na 1. filmie + podgląd z dźwiękiem; Mixy), 24 (Szukaj w YouTube), 17 (bez „Rzędy”; historia wyszukiwań z usuwaniem długim OK), 25 („Przeglądaj” = rząd przycisków), 18 (rząd archiwum pod odtwarzaczem IPTV).
4. **Wygląd:** „CAŁA APLIKACJA” (szare gradienty/tła → rozmyty obraz spod spodu, wyjątek: podgląd kanału Live TV) + plakaty (dół w rozmycie, rozmycia na dysku) + 27 (bez gradientu od lewej), 10 (kafle Live TV na `NiuniaMediaCard`, okładki TMDB wszędzie — `fork/tv/ProgramCard`, `TvTmdb`), 13, 11 (większy podgląd), 19, 05/06 (panele jakości i dźwięku w stylu Niuni, po polsku, dźwięk: prosta lista + reszta zwinięta), 07, 02 (nagrody po lewej + dłuższa forma), „Gotowe” w rogu (znaleźć i usunąć).
Punkty „do decyzji właściciela” w NOTATCE: wybierz wariant łatwiejszy do cofnięcia, jedno zdanie w `plan/DECYZJE.md`, nie pytaj.

## Pętla (powtarzaj bez przerwy)
1. **Kod ≥ 10 min** — kilka punktów z bieżącej paczki. Po każdej poprawce: `bash fork-tools/check.sh`, commit po ścieżkach (najpierw `git pull`), push.
2. **Build** — `bash fork-tools/build.sh <log> <zadania>` w zakładce terminala aplikacji (ZASADY-build). **W trakcie builda pisz dalej kod następnej paczki** (te zmiany wejdą do następnego builda). Czekanie: najpierw jedno ręczne sprawdzenie, limit ≤ 20 min, zawsze jedno zdanie, na co czekasz.
3. **Instalacja na JMGO** (md5 + `cmd package compile -m speed`), wpis w `.stage/jmgo_zajety.txt`.
4. **Testy ręczne ≥ 10 min i nie krócej niż czas pisania kodu w tej pętli** — każdy poprawiony punkt na JMGO wg „Poprawka/Sprawdzić” z NOTATKI; fokus przed każdym OK (`fork-tools/test/fx2.sh`), zrzut `plan/ux/przeglad-ekranow/zrzut.sh NAZWA` tylko gdy uiautomator nie wystarczy; logi bez adresów `http(s)://`.
5. **Zapis**: w NOTATCE przy punkcie `✅ TV <build>` albo `❌ TV <build>: co nie działa`; podsumowanie pętli w `plan/ux/przeglad-ekranow/WYNIKI.md` (pierwsze zdanie: ile poprawek, ile sprawdzonych na TV) → SendUserFile → commit + push. ❌ wraca na początek następnej pętli; 2 nieudane podejścia → `plan/BLOKADY.md`, dalej.

## Zasady stałe
- Każda nowa funkcja: osobny moduł/plik (`fork/…`, `skin/niunia/…`), przełącznik `ForkFeatures` domyślnie włączony; pliki bazy tylko wg wyjątków `ZASADY-kod.md` (limit wpięć 65).
- Logi `Fork<Moduł>`, każdy błąd zalogowany; błąd odczuwalny → `ForkLog.say`. Teksty po polsku w `res/values/<moduł>_strings.xml`.
- Nie pytaj, nie kończ tury pytaniem, nie zatrzymuj się po pętli. Profil właściciela na JMGO: zmiany wymień w WYNIKI.
- ~95% kontekstu: stan + następne kroki do `plan/TERAZ.md` (sekcja „NAJPIERW”, ≤ 4 KB), commit + push.
