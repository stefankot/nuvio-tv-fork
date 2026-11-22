# UX/UI — testy w pętlach (wątek 22.11 wieczór, polecenie właściciela)

Polecenie: przejść całą aplikację, sprawdzić interfejs, poprawić błędy i nieoptymalne miejsca; szczególnie długie naciśnięcie OK
(„w większości elementów nie działa”). 50 ścieżek użytkownika, cykl: ≥ 10 min testów → build → poprawki → ≥ 10 min testów.
Emulator GTV (`nuvio_tv`, Google TV API 34, arm64 → build `-PforkAbi=arm64-v8a`). Emulator i Gradle nigdy razem.
Ścieżki: `fork-tools/uitest/sciezki.py` (uiautomator; wynik `plan/ux/cykl<N>/sciezki.md`), do tego testy ręczne (klawisze + odczyt ekranu).

## Pętla 0 (21:00–) — analiza kodu przed pierwszym APK
- Build `ux1` (arm64, kod = build173) — w toku.
- Ustalenia z kodu (skórka Niunia = domyślna):
  1. Plakaty Niuni (`NiuniaPoster`: Główna, Biblioteka, Szukaj — polecane, Osoba, Szczegóły — podobne) NIE mają długiego OK
     (parametr `onLongOk` nigdzie nie podany). Baza ma menu opcji plakatu (`PosterOptionsHost`: szczegóły, biblioteka/listy, obejrzane).
     Ekran główny bazy już przekazuje skórce `onCatalogItemLongPress` — Niunia go nie używa.
  2. „Oglądaj dalej” w Niuni bez długiego OK (baza: usuń, od początku, wybierz źródło, obejrzane, szczegóły).
  3. Wyniki szukania (`NiuniaResultCard`), kafelki i wiersze kanałów Live TV Niuni — bez długiego OK.
  4. `niuniaOk` (wspólne OK/długie OK Niuni): (a) puszczenie OK bez wciśnięcia na tym elemencie (np. po długim OK, które otworzyło
     okno) wywołuje zwykłe OK; (b) po długim OK, które przeniosło fokus, element zostaje w stanie „wciśnięty” — następne krótkie OK
     na nim jest gubione; (c) liczy tylko własny zegar 520 ms, ignoruje systemową flagę długiego naciśnięcia.
- Szkic modułu: `.stage/ux/NiuniaPosterOptions.kt` (wspólne menu długiego OK dla plakatów).

## Pętla 1 (21:30–) — testy na APK b158-arm64 (21.11; brak świeżego APK arm64)
- Build `ux1` (pełna kompilacja: inny wątek wyczyścił katalogi build) przerwany po 22 min (limit 20 min); daemon pracował z dysku
  wymiany (sterta 5 GB z `~/.gradle/gradle.properties` przy 8 GB RAM; w RAM ~430 MB). Kompilacja Kotlina ok. 10 min + pakowanie
  wpisu pamięci podręcznej ~5 min. Następny build w dwóch krokach (`compileFullDebugKotlin`, potem `assembleFullDebug`).
- Emulator: Google TV API 34 tylko arm64 (bez 32-bit), bez roota → długie OK tylko `input keyevent --longpress`
  (flaga systemowa; brak realnego trzymania). Na b158 Niunia liczy wyłącznie własny zegar 520 ms → w teście każde długie OK
  Niuni wygląda jak zwykłe OK (na pilocie zegar działa, ale elementy nie mają akcji).
- Ścieżki: `python3 -I fork-tools/uitest/sciezki.py --cykl 1` (wynik `plan/ux/cykl1/`), log przebiegu `plan/ux/cykl1-przebieg.log`.
- Poprawki w kodzie w trakcie pętli 1 (NIESKOMPILOWANE): patrz `plan/ux/RAPORT.md` [P].
- Wynik pętli 1: 20,9 min automatycznie + ok. 6 min ręcznie; OK 18 / UWAGA 25 / BŁĄD 7. Ustalenia → `RAPORT.md` (pkt 7–20).
- Następny krok (22:00): `adb emu kill`, build `ux2a` (`:app:compileFullDebugKotlin -PforkAbi=arm64-v8a`), potem `ux2b`
  (`:app:assembleFullDebug -PforkAbi=arm64-v8a`), instalacja na emulatorze, pętla 2: dodanie darmowej listy IPTV przez nowy
  przycisk w pustym Live TV, testy długiego OK (menu plakatów, „Oglądaj dalej”, kanały), powrót fokusu w Szukaj.
