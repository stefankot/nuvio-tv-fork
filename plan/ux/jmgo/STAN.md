# UX JMGO — stan pętli (ZASADY §4: zapis po każdej pętli)

## Pętla 0 — przed zmianami (zrobiona 24.11, analiza E2)
- APK na JMGO: `tv2e`, md5 63a75317… (zbudowane przed etapem 0 — bez `ForkFocusTrace`, bez poprawek Głównej z `bc12adecd`). Test 13:52–14:33 (40,6 min), 50 ścieżek.
- Wynik: **15 OK / 17 UWAGA / 15 BŁĄD / 3 pominięte**. Po analizie: 5 błędów aplikacji (powrót fokusu F3/F5, panele F4), 4 × GPU, 5 błędów oceny w skrypcie, 1 do wyjaśnienia.
- Czasy (patrz `cykl0/wydajnosc.md`): zimny start `Fully drawn` 3,37 s (OK), ciepły 229 ms (OK), Janky na Głównej 93–98 % (BŁĄD, GPU), PSS 884 MB po ścieżkach (BŁĄD, limit 480).
- Awarie/ANR aplikacji: 0.

## Stan kodu (24.11, po E2) — napisane, NIEZBUDOWANE, niesprawdzone na JMGO
| Co | Plik | Test JVM |
|---|---|---|
| Pamięć fokusu list po kluczu + przewinięcie (Biblioteka: siatka, półki, Historia; Odkrywaj) | `components/NiuniaFocusMemory.kt`, `NiuniaLibraryShelves.kt`, `NiuniaLibraryHistory.kt`, `NiuniaDiscover.kt` | `NiuniaFocusMemoryTest` |
| ▲ z treści Biblioteki → ostatni chip | `NiuniaLibraryShelves.kt` (`focusRestorer`) | — (framework) |
| Szukaj: Wstecz z Odkrywaj → chip „Odkrywaj” | `NiuniaFocusKeys.searchReturn`, `NiuniaSearch.kt` | `NiuniaFocusKeysTest` |
| Logi Live TV bez adresu strumienia (login i hasło IPTV) | `livetv/NiuniaLiveReturn.kt`, `NiuniaLiveTv.kt` | `NiuniaLiveReturnTest` |
| Panele nie wypuszczają fokusu na ekran pod spodem (5 paneli) | `components/NiuniaPanelTrap.kt` + 5 miejsc użycia | — (framework) |
| Skrypt 50 ścieżek: ocena menu ▲/Wstecz, F7 wiersze pełnej szerokości, tolerancja położenia, YouTube, przewodnik | `fork-tools/uitest/sciezki_jmgo.py` | — |

## Co dalej
1. **E1** (równolegle): zacięcia GPU na Głównej, build `uxj1`, pomiar przed/po (`plan/ux/jmgo/etap1-gpu.md`).
2. **E3**: build `uxj2` (E1 + E2, polecenie z `ETAPY.md`), instalacja na JMGO, `petla_jmgo.sh 1`. W pętli 1 sprawdzić: P1–P5 z `RAPORT.md` (ścieżki 19, 21, 22, 41, 49), pomiar `ForkFocus` (dtFrame, OK → fokus, Wstecz), PSS po grupach ścieżek (O3), ścieżki, które nie doszły do celu (O7).
3. Otwarte pytanie do właściciela (nie blokuje): ◄ z pierwszej kolumny a menu (`RAPORT.md`, `[makieta]`).

## Pętla 1 (E3, 24.11) — stan na koniec dnia
- APK na JMGO: **`uxj3`** (md5 6deb562d…) = `uxj1` (E1 + E2) + poprawka Biblioteki ▲ (E3). Test JVM niunia/perf: przechodzą; `SettingsTreeSpecTest` (cudzy, tryb prosty ≤ 20 pozycji) pada od `uxj1` — nie ruszany.
- **Pełna, wiarygodna pętla 1 NIE została wykonana.** Przebieg C (17:06–17:19, `cykl1/`, APK `uxj1`): 11 OK / 14 UWAGA / 21 BŁĄD / 4 pominięte — ale tester czytał fokus za wcześnie (spóźnienie o jeden klawisz), więc 21 BŁĘDÓW to głównie błąd testera. Poprawiony tester (pauza 0,7 s, `e1698f73f`) nie dokończył przebiegu na `uxj3` — przerwany na polecenie właściciela. Pętla 2 nie była robiona.
- Narzędzia (to jest realny wynik E3): stały czytnik ekranu `fork-tools/uitest/fastdump/` (odczyt 2,1 s → 0,05–0,6 s, pętla 40,6 → ~12 min), naprawione `jmgo_czekaj_wolny.sh` (zgubione `max=`, zamrożony drugi blok `RecentQueue`), trasa `home` na otwartej Głównej wchodzi ► w treść.
- Co zostało (dane w `RAPORT.md` i `cykl1/wydajnosc.md`): pamięć (1 009 MB po ścieżkach, limit 480, gorzej niż pętla 0), zacięcia przy przytrzymaniu (E1: wątek UI i GC), Wstecz ze strony tytułu o 49–125 px niżej (28, 30), Szukaj ◄ z wyników (17), Live TV bez fokusu (40–42) — **bez poprawek**. Poprawka E2 „▲ w Bibliotece” (`focusRestorer`) nie zadziałała w 3 przebiegach (ścieżka 21); zastąpiona w E3 jawnym `exit` grupy — niesprawdzona.

## Pętla 2 / E4 (24.11)
- **Nie wykonana.** Właściciel zakazał kolejnych przebiegów (zapis E3: `PRZEKAZANIE.md`, `TERAZ.md`); `app/` bez zmian po `uxj3` (d59614ca8), więc bez builda `uxj3` ponownie. Kryterium końca (2 pętle: 0 BŁĄD i ≥ 48/50 OK) niespełnione — zostało to, co w „Pętla 1”; opis dla właściciela: `PODSUMOWANIE.md`.
