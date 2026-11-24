# UX JMGO — przekazanie po E3 (24.11.2026, wieczór)

Wszystko, co ustaliłem w E3. Właściciel: żadnych kolejnych przebiegów (zakaz z 24.11 wieczorem), E4 w obecnej postaci (pętla 2 + podsumowanie) nic nie wnosi — nie uruchamiać.

## 1. Stan builda i urządzenia
- Na JMGO: **`uxj3`** (md5 `6deb562d448e2dd6152abaea1e0daa59`, `.stage/apk/uxj3-v7a.apk`) = `uxj1` (E1 + E2, md5 286737dd…) + poprawka Biblioteki ▲ (E3). Zainstalowany, `compile -m speed` zrobione.
- `uxj2` = identyczny APK jak `uxj1` (brak zmian w `app/` — zbudowany niepotrzebnie).
- Test JVM `SettingsTreeSpecTest › tryb prosty to podzbiór pełnego, najwyżej 20 pozycji` pada (cudzy kod ustawień, od `uxj1`); `uxj3` zbudowany bez niego (`--tests niunia.* --tests fork.perf.*`).
- Pełny, wiarygodny przebieg pętli 1 NIE istnieje. `cykl1/` = przebieg C na `uxj1` z testerem, który czytał fokus za wcześnie (21 z 21 BŁĘDÓW to głównie tester).

## 2. Realne problemy APLIKACJI (do poprawy; zero poprawek)
| Sprawa | Dowód | Uwagi |
|---|---|---|
| Pamięć PSS 1 009 MB po ścieżkach (limit 480), 1 172 MB w Live TV; Java 65→187 MB, natywna 48→400 MB, nie wraca | `cykl1/pamiec.txt` (p.0: 884 MB) | przyczyna nieznana; zacząć od pamięci podręcznej obrazów (Coil) i odtwarzaczy Live TV |
| Zacięcia przy przytrzymaniu na Głównej (► 21–50 %, ▼ baner 3–44 %, niestabilne) | `cykl1/wydajnosc.md`, `etap1-gpu.md` | E1: GPU już w klatce, zostaje wątek UI (rekompozycja p90 53 ms, układ 46 ms, klawisz/fokus 76 ms, GC) |
| Wstecz ze strony tytułu (odcinek/zwiastun): ten sam element, ale 49–125 px niżej (ścieżki 28, 30) | `cykl1/sciezki.md`, w p.0 28 px | strona tytułu nie przywraca tego samego przewinięcia |
| Szukaj: ◄ z wyników ląduje na „Sterowanie głosem”, nie na klawiszu (17) | 3 przebiegi z rzędu | możliwy artefakt odczytu — sprawdzić |
| Live TV: brak fokusu po otwarciu, długie OK nie otwiera „Opcji kanału” (40–42) | przebiegi B, C | zależy od wczytanych kanałów; sprawdzić log `ForkNiunia` |
| Biblioteka ▲ na „Seriale” zamiast „Filmy” (21) | 3 przebiegi na `uxj1` | poprawka E2 (`focusRestorer`) NIE zadziałała; w `uxj3` jawne `exit` grupy treści (`NiuniaLibraryShelves.kt`) — niesprawdzone |
| Ścieżki 22 i 38 pomijane (brak zakładki „Historia”, brak chipa „Kraj i język”) | `cykl1/sciezki.md` | P2/P5 z E2 nie potwierdzone |

## 3. Narzędzia testowe (to jest realny wynik E3)
- **`fork-tools/uitest/fastdump/`** (`FastDump.java`, `fastdump.jar`, `zbuduj.sh`): stały czytnik ekranu — jedno połączenie z UiAutomation, klawisze bez `input`, odczyt po ~350 ms ciszy zamiast `uiautomator dump` (start procesu + `waitForIdle(1000, 10000)` = 2,1 s). Odczyt 0,05–0,6 s; pętla 50 ścieżek 40,6 → ~12 min. Ten sam XML (122 węzły, ten sam fokus i teksty). Błąd czytnika → zwykły dump; `FASTDUMP=0` wyłącza.
- **Pułapki czytnika:** (a) `adb exec-out` NIE przekazuje stdin — użyć `adb shell -T`; (b) odczyt drzewa Compose spóźnia się o jeden klawisz, jeśli czytamy < ~0,7 s po naciśnięciu (`FD_KEY_SETTLE`, domyślnie 0,7) — pętle „▲ aż do banera” przestrzeliwały do menu; (c) w menu fokus bywa odczytany jako „Główna” (aktywna pozycja) mimo ruchu — niepewne; (d) UiAutomation trzyma połączenie, więc inny `uiautomator dump` w tym czasie nie działa (czytnik zamykany na końcu skryptu).
- **`jmgo_czekaj_wolny.sh`:** dwa błędy naprawione — `max=` zjedzone przez komentarz (czekanie kończyło się od razu), oraz `dumpsys input` ma dwa bloki `RecentQueue`, drugi zamrożony (wiek stały ~1,2 s) → „wolny” nigdy nie następował; teraz tylko pierwszy blok. Próg 1 min.
- **`sciezki_jmgo.py`:** trasa `home` na już otwartej Głównej otwiera menu z fokusem „Główna” (zdarzenie testowe) — skrypt wchodzi ► w treść; sprawdzanie pierwszego fokusu tytułu uwzględnia prefiks „n:” i „Wznów”; statystyka czasu odczytów na końcu `sciezki.md`.
- **Instalacja:** `jmgo_instaluj.sh` przerywa (kod 3), gdy gra wyjście dźwięku — także po moim własnym ostatnim odtwarzaniu Live TV; `compile -m speed` trwa 3–5 min (to nie jest sama instalacja).
- **`build.sh`:** komunikat „koniec: kod N” idzie na terminal, NIE do `/tmp/<nazwa>.log` — do czekania w tle używać `BUILD SUCCESSFUL|BUILD FAILED` z logu.

## 4. Co proponuję zamiast E4 (decyzja właściciela)
Osobne zadanie „poprawki aplikacji” bez przebiegów na zapas: pamięć (najpierw ustalić przyczynę), powrót ze strony tytułu, Szukaj ◄, Live TV fokus; jeden build; jedna weryfikacja ścieżek, których dotyczy (nie 50). Zasady pracy: `ZASADY.md` §9 (dopisane 24.11).
