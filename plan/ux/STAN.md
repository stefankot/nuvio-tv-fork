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

## Pętla 2 (22:15–23:20) — APK ux2 (commit 5ded5239f), arm64
- Build: krok A `compileFullDebugKotlin` + krok B `assembleFullDebug` (9 min 15 s) — oba < 20 min.
- Darmowa lista IPTV dodana na emulatorze (iptv-org PL, 188 kanałów) przez nowy przycisk pustego Live TV.
- Wynik: `plan/ux/cykl2/sciezki.md` (44,3 min; OK 23 / UWAGA 25 / BŁĄD 2) + ok. 15 min ręcznie (RAPORT „Pętla 2”).
- Poprawki po pętli 2 (do builda ux3): podpowiedź EPG nad listą bez programu, ponowienie fokusu na kanale po powrocie z odtwarzacza.
- Następny krok: `adb emu kill`, build `ux3` (`assembleFullDebug` arm64), potem testy JVM Niuni (`NiuniaOkGateTest` i reszta `*Niunia*`),
  pętla 3 (≥ 10 min): Live TV powrót fokusu, podpowiedź EPG, „Szukaj w telewizji” z Ustawień Niuni, zwiastun, sezon.

## Pętla 3 (22.11 23:25–23:51) — APK ux3 (md5 b9acf233…)
- Testy JVM Niuni 82/82 (NiuniaOkGateTest 8/8). Ręcznie ok. 15 min + automatycznie 15,4 min (`cykl3/sciezki.md`: OK 0 / UWAGA 18 / BŁĄD 0,
  uwagi = czas wczytania na obciążonym emulatorze). Potwierdzone: Live TV, Ustawienia Niuni, zwiastun, „Więcej/Mniej”, długie OK na sezonie.
- Poprawki po pętli 3 (RAPORT 27–30, do builda ux4): Wstecz w „Kanał nie odpowiada”, powrót fokusu na stronie tytułu (osoba/podobne),
  długie OK na odcinku = opcje odcinka Nuvio, ▲ z kanałów na wybraną pigułkę.
- Następny krok: `adb emu kill`, build `ux4` (`assembleFullDebug` arm64), instalacja, pętla 4 (≥ 10 min, połowa przez uiautomator).

## Pętla 4 (23.11 02:06–02:31) — APK ux4 (md5 30f3abb22ad368f80ff3815f734b4490, `.stage/apk/ux4-arm64.apk`)
- Build 9 min 21 s. Ręcznie ok. 13 min + automatycznie 11,6 min (`cykl4/sciezki.md`: OK 25 / UWAGA 4 / BŁĄD 0).
- Potwierdzone [B]: Wstecz w „Kanał nie odpowiada”, ▲ na wybraną kategorię, opcje odcinka na długie OK, powrót na plakat Głównej.
- Poprawki po pętli 4 (RAPORT 33–36, do builda ux5): przyciski okna „Kanał nie odpowiada” w Niuni, lista pod hero od góry,
  komunikat osoby bez strony, fokus w „Co mogę powiedzieć”. Skrypt: ścieżka 19 i 29 (nowe sprawdzenie ▲ na „Wszystkie”).
