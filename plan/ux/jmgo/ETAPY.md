# UX JMGO — etapy (właściciel 24.11: „Podziel pracę na etapy, każdy w osobnym wątku. Cel: oszczędność tokenów”)

Pełny prompt zadania: `plan/ux/jmgo/PROMPT.md` (czytaj TYLKO sekcje wskazane w swoim etapie). Zasady: `ZASADY.md` (ładowane same).
Stan wyjściowy: commit `bc12adecd` (etap 0). Na JMGO `tv2e` (md5 63a75317…). Pętla 0 (przed zmianami) idzie w tle: wynik w
`plan/ux/jmgo/cykl0/` (`przebieg.log` kończy się wierszem „pętla 0: koniec”).

## Oszczędzanie tokenów (obowiązuje każdy etap)
- Nie czytaj całych dużych plików: `grep -n`, `sed -n 'a,bp'`. Nie czytaj `sciezki_jmgo.py` w całości (nagłówek + `grep -n "^def \|@path"`).
- Na build, pętlę testów i czekanie na inny etap: JEDNO polecenie w tle (`run_in_background`), które kończy się samo
  (`until grep -q … ; do sleep 60; done`) — żadnego sprawdzania co chwilę.
- Raporty krótko, tabelami. Bez zrzutów ekranu (wyjątek: 1 na typ elementu, `sips -Z 960`).
- Wątki dzielą jeden katalog `NuvioFork` (gałąź `skin-niunia`): ruszaj tylko pliki swojego etapu, commituj tylko swoje pliki
  (`git add <pliki>`, nie `git add -A`), przed commitem `git pull --rebase --autostash` i `bash fork-tools/check.sh`, potem push.
- Build tylko jeden naraz: buduje wyłącznie etap, który ma to w opisie.
- Build: znany nieudany `SettingsTreeSpecTest` blokuje pakowanie APK, gdy testy są w tym samym wywołaniu — najpierw
  `:app:assembleFullDebug -PforkAbi=armeabi-v7a`, testy JVM osobnym wywołaniem (E1, 24.11).

## Status (każdy etap dopisuje swój wiersz na końcu: `E<n>: ZROBIONE <commit> — <1 zdanie>` albo `E<n>: BLOKADA — <powód>`)
E2: ZROBIONE c0021653b — pętla 0 (15 OK / 17 UWAGA / 15 BŁĄD / 3 pominięte) przeanalizowana: 5 błędów aplikacji poprawionych w kodzie z testami JVM (powrót fokusu Biblioteka/Odkrywaj/Szukaj, wyciek fokusu z paneli, logi z adresem IPTV), 5 błędów oceny w skrypcie poprawionych; niezbudowane (wejdzie do `uxj2` w E3); otwarte: GPU poza Główną, PSS 884 MB, czasy fokusu (tv2e bez `ForkFocusTrace`) — `RAPORT.md`, `STAN.md`, `cykl0/wydajnosc.md`.

## E1 — zacięcia GPU na Głównej + build uxj1 + pomiar przed/po (JMGO)
Ustalenie z pętli 0 (`cykl0/gfx/p04,p07,p08`): Janky 93–98 %, p95 65–150 ms, ALE „Slow UI thread 0”, „Slow issue draw commands 0”
→ wąskie gardło to GPU (zapis pikseli), nie kod Kotlin. Każda klatka rysuje od nowa: tło hero (pełny ekran, skala 1,08),
rozmyte tło z `alpha`, przyciemnienie z `alpha`, scrim, hero z `alpha` (`skin/niunia/components/NiuniaHeroPage.kt`) oraz w każdym
plakacie 3–4 warstwy: grafika, `BlurLayer` (Offscreen + maska DstIn), `Tint` (4 gradienty) (`NiuniaPoster.kt`).
Pliki etapu: TYLKO `NiuniaHeroPage.kt`, `NiuniaPoster.kt` (ew. `niuniaFocusLook` w `NiuniaAtoms.kt`).
1. Poprawki bez zmiany wyglądu w stanie spoczynku: po zejściu pod hero (t = 1 i rozmyty obraz wczytany) nie rysuj zasłoniętego
   obrazu bazowego; przyciemnienie wlicz w rozmyty obraz (`colorFilter`) zamiast osobnej warstwy na cały ekran; warstwy z jedną
   treścią i `alpha` → `CompositingStrategy.ModulateAlpha` (bez bufora pozaekranowego). Plakat: rozważ buforowanie treści plakatu
   jako warstwy sprzętowej (`graphicsLayer { compositingStrategy = Offscreen }` na treści, skala fokusu na zewnątrz) — zmierz PSS
   (próg JMGO 480 MB). Każda zmiana: `Log` raz (tag ForkNiunia), decyzja jednym zdaniem w `plan/DECYZJE.md`.
2. Build w zakładce terminala: `bash fork-tools/build.sh uxj1 :app:assembleFullDebug :app:testFullDebugUnitTest --tests 'com.nuvio.tv.skin.niunia.*' --tests 'com.nuvio.tv.fork.perf.*' --tests 'com.nuvio.tv.fork.settings.*' -PforkAbi=armeabi-v7a`
   (znany nieudany test spoza etapu: `SettingsTreeSpecTest` „tryb prosty ≤ 20”). APK → `.stage/apk/uxj1-v7a.apk`.
3. Czekaj w tle na „pętla 0: koniec”. Potem `bash fork-tools/test/jmgo_instaluj.sh .stage/apk/uxj1-v7a.apk` i
   `python3 -I fork-tools/uitest/sciezki_jmgo.py --cykl 1gpu 4 7 8 39 42 49` + `bash fork-tools/uitest/perf_jmgo.sh pamiec plan/ux/jmgo/cykl1gpu "po ścieżkach"`.
   Gdy nadal Janky > 15 %: jeden ślad `perf_jmgo.sh perfetto … slad8 10` w czasie ścieżki 8, wnioski 3 zdania.
4. Wynik: `plan/ux/jmgo/etap1-gpu.md` (tabela gfx: pętla 0 / uxj1 dla ścieżek 4, 7, 8, 39, 42, 49; PSS), commit + push, wiersz statusu.

## E2 — analiza pętli 0 + poprawki fokusu (bez buildu, bez JMGO)
1. Czekaj w tle na „pętla 0: koniec”. Czytaj: tabelę z `cykl0/sciezki.md` (bez sekcji „Kroki”, poza krokami ścieżek z BŁĄD),
   `cykl0/starty.txt`, `cykl0/pamiec.txt`, `cykl0/fokus.csv` (tylko wiersze BŁĄD/UWAGA: `grep`).
2. Napisz `cykl0/wydajnosc.md` (tabela budżetów z promptu, sekcja „Budżety czasu”, kolumna „pętla 0”), `plan/ux/jmgo/RAPORT.md`
   (format z promptu, sekcja „Pętle i zapis”), `plan/ux/jmgo/STAN.md`.
3. Oddziel błędy skryptu od błędów aplikacji (np. ścieżka 7: ▲ 3 s kończy na „Zmień profil” = najwyższy element — poprawne;
   popraw ocenę w `sciezki_jmgo.py`). Błędy aplikacji: popraw w `skin/niunia/**` lub `fork/**` — NIE w `NiuniaHeroPage.kt`,
   `NiuniaPoster.kt` (etap E1); najpierw czysta funkcja + test JVM (prompt: „Testy jednostkowe”), potem poprawka. Bez buildu —
   wejdzie do buildu etapu E3. Wygląd → `[makieta]`, kod bazy/obcy → `[i]`.
4. Commit + push, wiersz statusu.

## E3 — pętla 1 na JMGO (po E1 i E2)
1. Czekaj w tle, aż w statusie są wiersze E1 i E2. `git pull`. Build `uxj2` (to samo polecenie co E1, nazwa `uxj2`), APK →
   `.stage/apk/uxj2-v7a.apk`, `jmgo_instaluj.sh`.
2. W tle: `bash fork-tools/uitest/petla_jmgo.sh 1 > plan/ux/jmgo/cykl1/przebieg.log 2>&1` (50 ścieżek, starty, pamięć).
3. `cykl1/wydajnosc.md` (kolumny pętla 0 / pętla 1, dane ForkFocus z końca `cykl1/sciezki.md`), aktualizacja `RAPORT.md`
   ([B] sprawdzone na buildzie) i `STAN.md`. Nowe błędy aplikacji: poprawka + test, commit bez buildu.
4. Commit + push, wiersz statusu (podaj liczby OK / UWAGA / BŁĄD).

## E4 — pętla 2 i podsumowanie (po E3)
1. Czekaj w tle na wiersz E3. Jeśli E3 commitował poprawki: build `uxj3`, instalacja, `petla_jmgo.sh 2`; inaczej tylko pętla 2 na uxj2.
2. Koniec, gdy dwie pętle z rzędu: 0 BŁĄD i ≥ 48/50 OK, a przekroczone budżety mają przyczynę. Inaczej zapisz w `STAN.md`, co
   zostało (bez kolejnych pętli — właściciel zdecyduje).
3. `plan/ux/jmgo/PODSUMOWANIE.md` dla właściciela (prosto: co było wolne/mylące, co poprawiono, czasy przed/po, co zostało i
   dlaczego; trzy stany: napisane / zbudowane / sprawdzone na JMGO). Sekcja „NAJPIERW” w `plan/TERAZ.md` (3–5 wierszy).
4. Commit + push, wiersz statusu.
E1: ZROBIONE cb7675630 — uxj1 (md5 286737dd47baeac15ab74f502d74871a, zawiera E2 c0021653b) na JMGO: hero bez zasłoniętych warstw i plakat w warstwie sprzętowej, przytrzymana ► Janky 98 → 41–50 % (p50 69 → 14–17 ms), baner ▼ 93 → 45 %, ▼ 93 → 78 %; GPU teraz w klatce (mediana 10 ms), reszta zacięć to wątek UI; PSS 647–877 MB (grafika +6…19 MB) — `etap1-gpu.md`.
