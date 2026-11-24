# Wydajność JMGO — pętla 1 (E3, 24.11) vs pętla 0

**Uwaga o wiarygodności (przeczytaj najpierw).** Te liczby pochodzą z przebiegu C pętli 1 (17:06–17:19) na APK `uxj1` (md5 286737dd…: E1 + E2),
przy nowym szybkim czytniku ekranu (`fork-tools/uitest/fastdump/`). W tym przebiegu czytnik czytał fokus ~0,4 s po klawiszu i spóźniał się o jeden klawisz
(poprawione później: pauza 0,7 s, commit `e1698f73f`), więc **oceny kroków fokusu (OK / BŁĄD) w tym przebiegu są zaniżone — 21 BŁĘDÓW to głównie błąd testera**.
Liczby sprzętowe (starty, pamięć, gfxinfo) nie zależą od czytnika i są wiarygodne. Przebieg na `uxj3` z poprawionym czytnikiem został przerwany na polecenie właściciela.

| Pomiar | Cel | Pętla 0 (`tv2e`) | Pętla 1 (`uxj1`, przebieg C) | Ocena p.1 |
|---|---|---|---|---|
| Zimny start → `Fully drawn` Głównej (mediana, n = 10) | ≤ 3,5 s | 3 371 ms | 3 372 ms (2 336–3 653) | OK |
| Zimny start → `Displayed` | — | 1 446 ms | 1 455 ms | — |
| Zimny start → pierwszy fokus (`firstFocus` z logu) | ≤ 3,5 s | — (nie mierzono) | mediana 1 994 ms (1 222–2 335) | OK |
| Ciepły start (`TotalTime`, mediana) | ≤ 1,0 s | 229 ms | 252 ms (224–370) | OK |
| Główna ▼ z banera (ścieżka 4) | Janky ≤ 5 % | 92,9 % (p95 65 ms) | 44,4 % (p95 53 ms, 54 klatki); przebieg trial: 3,0 % | UWAGA/BŁĄD (niestabilne) |
| Główna, przytrzymana ▼ (ścieżka 7) | j.w. | 92,7 % | brak wiarygodnego pomiaru (tester przestrzelił do menu, 1 klatka) | — |
| Główna, przytrzymana ► (ścieżka 8) | j.w. | 98,4 % (p95 93 ms) | 21,7 % (p95 21 ms, 60 klatek); w E1: 41–50 % | BŁĄD |
| Szukaj (ścieżka 17) | j.w. | 8,8 % | 0,0 % (p95 11 ms) | OK |
| Live TV, lista kanałów (ścieżka 39/„kanały”) | j.w. | 70,9 % | 1,4 % (p95 7 ms, 73 klatki) | OK |
| Live TV, przewodnik EPG (ścieżka 42) | j.w. | 1,8 % | 10,5 % (p95 22 ms, 19 klatek) | UWAGA |
| PSS po startach | — | 350 MB | 367 MB (Java 65, natywna 48, grafika 61) | — |
| PSS po 50 ścieżkach | ≤ 400 MB (błąd > 480) | 884 MB | **1 009 MB** (Java 187, natywna 400, grafika 125) | BŁĄD |
| PSS w Live TV / po wyjściu | — | 1 054 / 1 035 MB | **1 172 / 1 163 MB** (Java 350/348, natywna 396/405) | BŁĄD |
| Czas całej pętli (50 ścieżek) | — | 40,6 min | **11,9 min** (czytnik: 1 068 odczytów, 20 % czasu, śr. 0,14 s) | — |

## Wnioski
- **Start i ciepły start bez zmian** (OK). Pierwszy fokus po zimnym starcie ~2 s — pierwszy raz zmierzony, nie ma go w pętli 0.
- **Zacięcia GPU na Głównej** spadły (ścieżka 8: 98 % → 22–50 %), ale nie do celu 5 %; Live TV lista kanałów 71 % → 1,4 % (nie ruszana przez E1 — zmiana wynika z innego stanu/źródła kanałów, do potwierdzenia).
- **Pamięć jest gorsza niż w pętli 0** (884 → 1 009 MB po ścieżkach; natywna 384 → 400 MB, Java 116 → 187 MB). Nie wraca po wyjściu z Live TV. **Nieotwarty, bez znanej przyczyny.**
- **Czas testu:** 40,6 → 11,9 min (stały czytnik ekranu zamiast `uiautomator dump` ~2,1 s z czekaniem na ciszę 1 s).
