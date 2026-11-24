# Wydajność JMGO — pętla 0 (przed zmianami)

APK `tv2e` (md5 63a75317…, przed etapem 0 — bez `ForkFocusTrace`), JMGO 192.168.31.252 (Android 11, ARM32), `compile -m speed`, 24.11 13:52–14:33.
Budżety: `plan/DECYZJE.md` (24.11, UX JMGO). Ocena: OK = w celu, UWAGA = cel przekroczony, BŁĄD = przekroczony próg „błąd”, „—” = nie zmierzono.

**Ważne:** w logu pętli 0 jest 0 wierszy `ForkFocus` (tv2e nie ma pomiaru fokusu), więc czasy ruchu fokusu (`dtFrame`) i „OK → pierwszy fokus” są
znane tylko z odczytu `uiautomator` (jeden odczyt ≈ 2,2 s). Wartości poniżej ≈ 2–3 s w kolumnie „odczyt” to **granica rozdzielczości**, nie czas aplikacji.
Dokładne czasy dopiero od pętli 1 (APK `uxj2`).

| Pomiar | Cel | Błąd | Pętla 0 | Ocena |
|---|---|---|---|---|
| Zimny start → `Fully drawn` Głównej | ≤ 3,5 s | > 5 s | mediana 3 371 ms (3 123–3 482, n = 10); `Displayed` 1 446 ms | OK |
| Zimny start → fokus na banerze | ≤ 3,5 s | > 5 s | — (odczyt 7,7 s = rozdzielczość + start) | — |
| Ciepły start (`TotalTime`) | ≤ 1,0 s | > 2 s | mediana 229 ms (206–255, n = 10) | OK |
| OK → pierwszy fokus na nowym ekranie | ≤ 600 ms | > 1,5 s | odczyt: Szukaj 2,1–2,2 s, Biblioteka 2,2–2,5 s, Live TV 2,4–2,7 s, Odkrywaj 2,3 s, „Szukaj w telewizji” 2,9 s, Główna 3,1–5,1 s | — (poniżej rozdzielczości odczytu) |
| Pełne dane ekranu (z dysku / z sieci) | ≤ 1,5 s (≤ 3 s) | > 5 s | — (Główna: stabilnie po 3,1–3,9 s odczytu, tylko górna granica) | — |
| Wstecz → fokus na źródle | ≤ 300 ms | > 800 ms | odczyt „pierwszy fokus” 2,0–2,1 s (rozdzielczość) | — |
| Strzałka → nowy fokus (`dtFrame`), mediana / p95 | ≤ 50 / ≤ 100 ms | p95 > 200 ms | — (brak `ForkFocusTrace` w tv2e) | — |
| Klatki w nawigacji (gfxinfo), Główna ▼ z banera (ścieżka 4) | ≤ 5 % / ≤ 34 ms | > 15 % | Janky 92,9 %, p95 65 ms, 42 klatki | BŁĄD |
| — Główna, przytrzymana ▼ (ścieżka 7) | j.w. | j.w. | Janky 92,7 %, p95 150 ms, 68 klatek | BŁĄD |
| — Główna, przytrzymana ► (ścieżka 8) | j.w. | j.w. | Janky 98,4 %, p95 93 ms, 246 klatek | BŁĄD |
| — Szukaj, klawiatura → wyniki (ścieżka 17) | j.w. | j.w. | Janky 8,8 %, p95 19 ms, 34 klatki | UWAGA |
| — Live TV, przytrzymana ▼ 5 s (ścieżka 39) | j.w. | j.w. | Janky 70,9 %, p95 89 ms, 199 klatek | BŁĄD |
| — Live TV, przewodnik ▼▲ (ścieżka 42) | j.w. | j.w. | Janky 1,8 %, p95 10 ms, 223 klatki | OK |
| — Odkrywaj, siatka ▼► i przytrzymanie (ścieżka 49) | j.w. | j.w. | Janky 58,1 %, p95 105 ms, 277 klatek | BŁĄD |
| Obraz plakatu/hero po fokusie (z pamięci / z sieci) | ≤ 300 ms (≤ 1 s) | > 2 s | — | — |
| Live TV: lista kanałów z dysku | ≤ 1 s | > 3 s | odczyt 2,4–2,7 s (rozdzielczość) | — |
| OK „Odtwórz” / kanał → pierwsza klatka (źródła cudze) | ≤ 3 s | > 8 s | „Oglądaj dalej” → odtwarzacz: pierwszy fokus 5,6 s; kanał: 5,8 s; całość kroku 11,9–15,9 s | UWAGA [i] |
| PSS po starcie (13:52) | — | — | 350 MB (Java 65, natywna 43, grafika 44) | — |
| PSS po 50 ścieżkach | ≤ 400 MB | > 480 MB | **884 MB** (Java 116, natywna 384, grafika 127); w Live TV 1 054 MB; po wyjściu z Live TV 1 035 MB | BŁĄD |
| Awarie, ANR aplikacji, nieobsłużone błędy `Fork*` | 0 | ≥ 1 | 0 awarii i 0 ANR aplikacji; 1 awaria systemowej klawiatury JMGO (`com.android.inputmethod.latin`, 21:26:32) | OK [i] |

## Wnioski z gfxinfo
- We wszystkich ścieżkach z Janky > 15 %: `Slow UI thread` 0, `Slow issue draw commands` 0, `Missed Vsync` 0 → wąskie gardło to **GPU** (rysowanie pikseli), nie kod Kotlin. Zajmuje się tym etap E1 (Główna: tło hero, warstwy plakatu).
- E1 nie obejmuje **listy kanałów Live TV (ścieżka 39)** ani **siatki Odkrywaj (ścieżka 49)** — te dwa ekrany mają ten sam objaw (Janky 58–71 %, p50 25–28 ms) i trzeba je zmierzyć po `uxj1`/`uxj2`, a jeśli nadal > 15 % — osobna poprawka (plakaty tego samego komponentu, warstwy `Offscreen`).
- **Pamięć:** natywna 43 → 384 MB (grafika 44 → 127), Java 65 → 116 MB; w Live TV Java rośnie do 303 MB, a po wyjściu prawie nic nie wraca (natywna 372 MB, Java 276 MB). Przyczyna nieustalona (podejrzenie: dekodery/bufory odtwarzacza po ścieżkach 33–40 albo cache obrazów). E3 ma zmierzyć PSS po grupach ścieżek (start / Główna / szczegóły / odtwarzacz / Live TV).
