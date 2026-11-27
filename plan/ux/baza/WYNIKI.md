# Aktualizacja bazy do NuvioTV 1.2.0-beta.4 — test na JMGO (27.11)

Realne poprawki aplikacji: 1 aktualizacja bazy (pliki NuvioTV 1:1) + 6 poprawek kompilacji i testów; sprawdzone na urządzeniu: 7 ścieżek, 0 awarii.

- APK `baza1126f` (commit 2378da978, ARM32), md5 `9ad0f7050379b161f774a6fd54b57868`, `.stage/apk/baza1126f-v7a.apk`. Zainstalowany 27.11 10:28, md5 z TV zgodne, `compile -m speed`.
- Testy JVM forka: 632, 3 nieudane (drzewo ustawień) → poprawione w d7f7334e0 (2 nowe), 1 stary (tryb prosty 21 > 20 — był już przed aktualizacją). Niezbudowane: sprawdzi następny wspólny build.

## Sprawdzone na JMGO
| Ścieżka | Wynik |
|---|---|
| Start aplikacji (Niunia) | ✔ bez awarii, Główna, fokus na „Odtwórz” |
| Ranking reguł Regex po aktualizacji | ✔ `reguły aktywnego profilu: 6`, pole „Regex” dostało sumę reguł (raz na profil) |
| Live TV: menu → przewodnik | ✔ 59 573 kanały, przewodnik z dysku, fokus na „Wszystkie” |
| Live TV: kanał na pełnym ekranie | ✔ „13 Ulica” gra, nakładka Niuni z programem |
| Live TV: kanał w górę | ✔ „Bloomberg TV” bez nowego odtwarzacza (wpięcie Reshaped „zap”) |
| Live TV: Wstecz | ✔ przewodnik, potem filtry; Wstecz → Główna |
| Film bez postępu („The Brutalist”) | ✔ `wybór: 120 źródeł, 6 reguł` → 2160p BluRay 10bit 35,3 GB = #0 według `zrodla.py` |
| Dolby Vision (oficjalne) | ✔ natywna biblioteka wczytana (`buildNative=true libraryLoaded=true`), film DV gra (JMGO: warstwa HDR10) |
| Napisy | ✔ „najpierw polskie”: angielskie wbudowane → polskie z opensubtitles PRO; oficjalny AutoSync dopasowuje je do pliku (bez dymków) |
| Drzewo ustawień | ✔ „Gdy film się zatnie” otwiera oficjalny „Automatyczny wybór strumienia”; nie ma „Przewijanie” ani „Szybkość internetu” |

## Uwagi (bez poprawki)
- Oficjalne moduły w logach: `ImdbEpisodeRatingsRepo` łączy się z `localhost:80` (brak adresu w konfiguracji, 40 ostrzeżeń), `ParentalGuide` — błąd odczytu JSON. Pliki bazy 1:1, nie ruszam.
- Pamięć aplikacji przy Live TV: sterta 475/481 MB po wczytaniu indeksu 276 tys. tytułów (GC 680 ms) — obserwować.

## Zmiany na profilu właściciela
- „Guarding Stars” (z „Oglądaj dalej”) wznowiony ok. 1 min; „The Brutalist” obejrzany ok. 3 min (teraz „Wznów, zostało 199 min”).
