# Fork „kitty”: czego nie ma nigdzie indziej (18.11.2026, porównanie w kodzie)

## Co porównano
- GitHub `stefankot/nuvio-tv-kitty` (sklonowany do `upstream/kitty`, tylko odczyt): wszystkie gałęzie, najnowsza 10.11.
- Nowsza wersja lokalna zrobiona przez ChatGPT/Codex: `~/nuvio-tv-kicia`, gałąź `feature/kicia-modules-20261111` (11.11, 436 commitów, których NIE ma w Kicii z pulpitu).
- Z: Nuvio, Reshaped, KE, Arc, Echo, One (`upstream/*`), Kicia z pulpitu (34c80c6) i nowy fork.
- Metoda: pliki kodu kitty, których nazw nie ma w żadnym z tych projektów (55 w Codex, 15 w gałęziach GitHub), potem odczyt, co robią. Funkcje obecne gdzie indziej, także podobne, pominięte.

## Funkcje, których nie ma w żadnym innym forku
1. **Jedno wyszukiwanie dla wszystkiego** (Codex: `core/kicia/UnifiedSearchService.kt` + adaptery katalogu, Live TV i YouTube). Jedno pole szuka naraz filmów/seriali, kanałów i programów z przewodnika oraz filmów YouTube. Kicia ma w wyszukiwarce tylko zostawione miejsce na półki „Telewizja” i „YouTube” (TODO), fork ma osobne „Szukaj w telewizji”.
2. **Szyfrowana synchronizacja wszystkich ustawień przez Google Drive** (Codex: `reshaped/sync/Settings*`). Reshaped synchronizuje część ustawień i Live TV; kitty dodaje szyfrowanie kluczy dostępu (np. klucze API) hasłem, przenoszenie własnej czcionki i pełny spis ustawień. Częściowo nowe.

## Pominięte, bo są gdzie indziej (także podobne)
- Kreator i ankieta preferencji (jakość, dźwięk, napisy → reguły auto-odtwarzania): fork ma pytanie o polski dźwięk i kreator pierwszego uruchomienia.
- Sprawdzanie urządzenia i plików przed auto-odtwarzaniem: KE (dekodery) + kontrole Kicii.
- Polecenia na podstawie ankiety: fork „Na co masz ochotę?”.
- Drugi przewodnik EPG z listy dostawców: fork B01-10.
- YouTube: wznawianie od miejsca i napisy generowane/tłumaczone: są w One (i w forku).
- Diagnostyka odtwarzania: baza Nuvio.
- Bezpieczny magazyn haseł profilu, autoryzacja urządzenia, polityki adresów: zabezpieczenia bez funkcji widocznej dla widza.

## Propozycja
1. Przenieść **jedno wyszukiwanie** do skórki Kicia (półki „Telewizja” i „YouTube” w jej wyszukiwarce, których brakuje) — adaptery Codex są małe (27–146 linii) i używają istniejących repozytoriów.
2. Szyfrowanie kluczy w synchronizacji: tylko jeśli właściciel używa synchronizacji Google Drive z kluczami API.
