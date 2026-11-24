# E1 — koszt rysowania Głównej na JMGO (24.11)

APK `uxj1` (md5 `286737dd47baeac15ab74f502d74871a`, v7a, zawiera E2 `c0021653b`), JMGO po `compile -m speed`.
Pętla 0 = `tv2e` (`cykl0/gfx`). uxj1 „1. przebieg” = zaraz po instalacji i zimnym starcie (`cykl1gpu/`),
„rozgrzany” = powtórka 4/7/8 na działającej aplikacji (`cykl1gpu-cieply/`).

## Zmiany (tylko koszt rysowania, wygląd w spoczynku ten sam)
| Plik | Zmiana |
|---|---|
| `NiuniaHeroPage.kt` | pod hero (t = 1, rozmyty obraz wczytany): bez tła ekranu i obrazu bazowego; przyciemnienie 55 % w rozmytym obrazie (`ColorFilter.tint SrcAtop`) zamiast warstwy na cały ekran; rozmyty obraz z alfą bez bufora (`ModulateAlpha`) — 3 wypełnienia ekranu mniej na klatkę |
| `NiuniaPoster.kt` | treść plakatu w warstwie sprzętowej (`Offscreen` + kształt i przycięcie zamiast `clip`), skala/cień fokusu poza nią → przy przewijaniu 1 obraz na plakat zamiast ok. 7–11 wypełnień; bufory rozmycia tylko na dolnej części plakatu (od 66/78/82 %) |

## Klatki (gfxinfo): Janky % / p50 / p95 ms / klatek
| Ścieżka | Pętla 0 (tv2e) | uxj1 1. przebieg | uxj1 rozgrzany |
|---|---|---|---|
| 4 baner ▼ rząd | 92,9 / 42 / 65 / 42 | 90,5 / 29 / 125 / 42 (zimny start) | **45,3 / 10 / 53 / 53** |
| 7 przytrzymana ▼ | 92,7 / 53 / 150 / 68 | 96,4 / 48 / 250 / 56 (zimny start) | **78,4 / 42 / 117 / 74** |
| 8 przytrzymana ► | 98,4 / 69 / 93 / 246 | **41,2 / 14 / 65 / 308** | **50,5 / 17 / 69 / 283** |
| 39 lista kanałów | 70,9 / 28 / 89 / 199 | 49,3 / 16 / 150 / 213 | — |
| 42 EPG | 1,8 / 6 / 10 / 223 | 1,6 / 6 / 10 / 304 | — |
| 49 siatka Odkrywaj | 58,1 / 25 / 105 / 277 | 45,8 / 15 / 121 / 284 | — |

Ścieżki 39/42 nie mają kodu E1 (różnice to rozrzut pomiaru i E2); 49 korzysta z plakatu Niuni.

## Pamięć (PSS)
| Moment | PSS | Java | natywna | grafika |
|---|---|---|---|---|
| Pętla 0 po 50 ścieżkach | 905 MB | 119 | 393 | 130 |
| uxj1 po 6 ścieżkach | 877 MB | 363 | 237 | 136 |
| uxj1 po powtórce 4/7/8 | 647 MB | 156 | 202 | 149 |

Grafika +6…19 MB (warstwy plakatów), w granicach rozrzutu. Próg 480 MB był przekroczony już w pętli 0 (Java/natywna,
nie warstwy) — sprawa dla E3/E4.

## Ślad perfetto ścieżki 8 (`cykl1gpu/slad8.perfetto-trace`, 40 s, 32 MB — tylko lokalnie, poza gitem; odczyt `fork-tools/uitest/perfetto_plasterki.py <ślad> <pid>`)
Usługa śledzenia na JMGO włączona na polecenie właściciela (`persist.traced.enable=1`, zostaje). Plasterki atrace aplikacji:
| Wątek: plasterek | n | mediana | p90 |
|---|---|---|---|
| główny: `Choreographer#doFrame` | 260 | 7,8 ms | 103 ms |
| główny: `animation` (rekompozycja Compose) | 234 | 6,6 ms | 53 ms |
| główny: `traversal` (pomiar/układ, w tym komponowanie nowych elementów listy) | 225 | 1,7 ms | 46 ms |
| główny: `ViewPostImeInputStage` (obsługa klawisza, ruch fokusu) | 77 | 7,3 ms | 76 ms |
| główny: blokada przez GC (`Lock contention … HeapTaskDaemon`) | 4 | 2,0 ms | 138 ms |
| RenderThread: `DrawFrame` | 226 | 7,6 ms | 45 ms |
| GPU completion: `waiting for GPU completion` | 224 | 9,3 ms | 14 ms |

Wnioski (3 zdania): (1) GPU po E1 mieści się w klatce (czekanie na GPU: mediana 9 ms, p90 14 ms) — zacięć Głównej nie robi
już GPU. (2) Wąskim gardłem jest wątek główny: p90 klatki 103 ms to rekompozycja (p90 53 ms) + pomiar/układ z komponowaniem
nowych plakatów (p90 46 ms) oraz obsługa klawisza z ruchem fokusu (p90 76 ms). (3) Dochodzą pauzy GC (blokada wątku
głównego do 138 ms, sterta Java do 363 MB) — następny krok (E3/E4): mniej rekompozycji i alokacji przy ruchu fokusu
i wchodzeniu nowych elementów listy, nie dalsze cięcie GPU.

## Framestats ścieżki 8 (ostatnie 120 klatek, `fork-tools/uitest/framestats_faza.py`)
| Faza | mediana | p90 |
|---|---|---|
| UI (wejście → zapis rysowania) | 9,5 ms | 65,3 ms |
| RenderThread (sync + polecenia) | 3,6 ms | 5,6 ms |
| GPU (swap → gotowe) | 10,2 ms | 15,8 ms |
| całość | 14,9 ms | 71,3 ms |

Te same wnioski z drugiego źródła: (1) GPU po E1 mieści się w klatce (mediana 10 ms, p90 16 ms), więc przewijanie w rzędzie spadło z 69 do 14–17 ms
w medianie. (2) Pozostałe zacięcia (Janky 45–78 %) to teraz wątek UI: p90 65 ms przy wchodzeniu nowych plakatów/rzędów
(komponowanie i układ nowych elementów, pierwsze rysowanie ich warstw) — najmocniej przy przytrzymanej ▼ (ścieżka 7).
(3) Następny krok to koszt komponowania nowych elementów listy (wątek UI), nie GPU — do oceny w pętli 1 (E3) z `ForkFocusTrace`.

## Uwagi do skryptów (dla E3)
- `sciezki_jmgo.py:213` dawał `IndexError`, gdy w logu był `dtFrame` bez `dtFocus` (każda ścieżka z włączonym `ForkFocus`) — poprawione.
- `jmgo_czekaj_wolny.sh` uznawał dźwięk ekranu głównego JMGO (`com.jmgo.arwen`) za oglądanie → godzina czekania; poprawione + zasada w `ZASADY.md` §5 (7d60aec49).
- Build: znany `SettingsTreeSpecTest` blokuje pakowanie APK, gdy testy są w tym samym wywołaniu (dopisane w `ETAPY.md`).
