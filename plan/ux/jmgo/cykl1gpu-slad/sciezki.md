# Ścieżki UX JMGO — cykl 1gpu-slad (24.11 15:40, 192.168.31.252:5555, 0.7 min)

OK 0, UWAGA 0, BŁĄD 1, POMINIĘTA 0

| # | Ścieżka | Wynik | s | Uwagi |
|---|---|---|---|---|
| 8 | Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku | **BŁĄD** | 41 | UWAGA: F1 trasa home: fokus 3.9 s, stabilnie 3.9 s (budżet 0.6/3.0 s); dtFocus - / dtFrame 92 ms; firstFocus 408 ms; BŁĄD: klatki gfx przytrzymanie_h: Janky 51.81%, p95 57 ms, klatek 249 |

## Kroki

### 8. Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku [F4 F7 F9, gfxinfo] — BŁĄD
- miary: `{"open:home": 3.9, "gfx:przytrzymanie_h": {"frames": 249, "janky%": 51.81, "p95": 57, "slowUI": 46, "slowBmp": 0, "50th": 17, "90th": 38, "99th": 250}, "klatki>=100ms(log)": 0}`
- trasa home [F1] UWAGA:  → n:Siostra Kopciuszka Animacja · ★ 6.8 (3.9 s) fokus 3.9 s, stabilnie 3.9 s (budżet 0.6/3.0 s); dtFocus - / dtFrame 92 ms; firstFocus 408 ms
-   ForkFocus: dtFocus 7 / dtFrame 30 ms
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (3.3 s) przewinięcie/zmiana układu
-   ForkFocus: dtFocus 6 / dtFrame 29 ms
- ▼ [F2] OK: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → n:Siostra Kopciuszka Animacja · ★ 6.8 (3.3 s)
- ► 3 s [F7] OK:  → n:Zobacz wszystko: TMDB Popular Zobacz wszystko | TMDB Popular (3.0 s)
- ► na krawędzi (po 0) [F4] OK: n:Zobacz wszystko: TMDB Popular Zobacz wszystko | TMDB Popular → n:Zobacz wszystko: TMDB Popular Zobacz wszystko | TMDB Popular (0.0 s)
- ◄ 4 s [F9] OK: n:Zobacz wszystko: TMDB Popular Zobacz wszystko | TMDB Popular → n:Siostra Kopciuszka Animacja · ★ 6.8 (4.0 s)
- gfx przytrzymanie_h [klatki] BŁĄD:  →  (0.0 s) Janky 51.81%, p95 57 ms, klatek 249


## Pomiar fokusu (ForkFocus)

- strzałka → klatka (dtFrame): n=2, mediana 29 ms, p95 30 ms, max 30 ms (cel ≤ 50 / ≤ 100)
- strzałka → onFocusChanged (dtFocus): mediana 6 ms, p95 7 ms
- Wstecz → fokus (dtFrame): n=0, mediana None ms, p95 None ms (cel ≤ 300)
- pierwszy fokus na ekranie (firstFocus): n=1, mediana 408 ms, p95 408 ms, max 408 ms (cel ≤ 600)
  - trasa home: 408 ms
