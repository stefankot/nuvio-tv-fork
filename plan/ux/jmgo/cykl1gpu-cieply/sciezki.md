# Ścieżki UX JMGO — cykl 1gpu-cieply (24.11 15:36, 192.168.31.252:5555, 1.4 min)

OK 0, UWAGA 0, BŁĄD 3, POMINIĘTA 0

| # | Ścieżka | Wynik | s | Uwagi |
|---|---|---|---|---|
| 4 | Baner ▼ → pierwszy rząd: plakat w całości widoczny, hero bez skoku | **BŁĄD** | 26 | BŁĄD: klatki gfx baner_rzad: Janky 45.28%, p95 53 ms, klatek 53 |
| 7 | Przytrzymana ▼ 3 s przez rzędy i ▲ do banera | **BŁĄD** | 24 | BŁĄD: klatki gfx przytrzymanie_v: Janky 78.38%, p95 117 ms, klatek 74 |
| 8 | Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku | **BŁĄD** | 35 | BŁĄD: klatki gfx przytrzymanie_h: Janky 50.53%, p95 69 ms, klatek 283 |

## Kroki

### 4. Baner ▼ → pierwszy rząd: plakat w całości widoczny, hero bez skoku [F2 F7, gfxinfo] — BŁĄD
- miary: `{"open:home": 3.6, "gfx:baner_rzad": {"frames": 53, "janky%": 45.28, "p95": 53, "slowUI": 8, "slowBmp": 0, "50th": 10, "90th": 46, "99th": 65}}`
- trasa home [F1] OK:  → n:Siostra Kopciuszka Animacja · ★ 6.8 (3.6 s) fokus 3.6 s, stabilnie 3.6 s; dtFocus - / dtFrame 85 ms; firstFocus 423 ms
-   ForkFocus: dtFocus 9 / dtFrame 36 ms
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (3.3 s) przewinięcie/zmiana układu
- ▼ do rzędu [F7] OK:  → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (0.0 s)
-   ForkFocus: dtFocus 6 / dtFrame 26 ms
- ► [F2] OK: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 (3.1 s)
-   ForkFocus: dtFocus 7 / dtFrame 27 ms
- ► [F2] OK: n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 → n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (3.2 s)
- gfx baner_rzad [klatki] BŁĄD:  →  (0.0 s) Janky 45.28%, p95 53 ms, klatek 53

### 7. Przytrzymana ▼ 3 s przez rzędy i ▲ do banera [F9, gfxinfo] — BŁĄD
- miary: `{"open:home": 3.7, "gfx:przytrzymanie_v": {"frames": 74, "janky%": 78.38, "p95": 117, "slowUI": 29, "slowBmp": 0, "50th": 42, "90th": 61, "99th": 200}, "klatki>=100ms(log)": 0}`
- trasa home [F1] OK:  → n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (3.7 s) fokus 3.7 s, stabilnie 3.7 s; dtFocus - / dtFrame 32 ms; firstFocus 400 ms
- ▼ 3 s [F9] OK:  → n:Dig (2026) Komedia (3.0 s)
- ▲ 3 s [F9] OK: n:Dig (2026) Komedia → n:Stef+Patryk S | Stef+Patryk | Zmień profil (3.0 s)
- gfx przytrzymanie_v [klatki] BŁĄD:  →  (0.0 s) Janky 78.38%, p95 117 ms, klatek 74

### 8. Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku [F4 F7 F9, gfxinfo] — BŁĄD
- miary: `{"open:home": 3.3, "gfx:przytrzymanie_h": {"frames": 283, "janky%": 50.53, "p95": 69, "slowUI": 61, "slowBmp": 0, "50th": 17, "90th": 42, "99th": 200}, "klatki>=100ms(log)": 0}`
- trasa home [F1] OK:  → n:Odtwórz Odtwórz (3.3 s) fokus 3.3 s, stabilnie 3.3 s; dtFocus - / dtFrame 62 ms; firstFocus 1145 ms
-   ForkFocus: dtFocus 7 / dtFrame 35 ms
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (3.4 s) przewinięcie/zmiana układu
-   ForkFocus: dtFocus 61 / dtFrame 88 ms
- ▼ [F2] OK: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → n:Siostra Kopciuszka Animacja · ★ 6.8 (3.4 s)
- ► 3 s [F7] OK:  → n:Ty, ja, my Dramat · ★ 6.3 (3.0 s)
- ► na krawędzi (po 1) [F4] OK: n:Zobacz wszystko: TMDB Popular Zobacz wszystko | TMDB Popular → n:Zobacz wszystko: TMDB Popular Zobacz wszystko | TMDB Popular (0.0 s)
- ◄ 4 s [F9] OK: n:Ty, ja, my Dramat · ★ 6.3 → n:Siostra Kopciuszka Animacja · ★ 6.8 (4.0 s)
- gfx przytrzymanie_h [klatki] BŁĄD:  →  (0.0 s) Janky 50.53%, p95 69 ms, klatek 283


## Pomiar fokusu (ForkFocus)

- strzałka → klatka (dtFrame): n=5, mediana 35 ms, p95 88 ms, max 88 ms (cel ≤ 50 / ≤ 100)
- strzałka → onFocusChanged (dtFocus): mediana 7 ms, p95 61 ms
- Wstecz → fokus (dtFrame): n=0, mediana None ms, p95 None ms (cel ≤ 300)
- pierwszy fokus na ekranie (firstFocus): n=3, mediana 423 ms, p95 1145 ms, max 1145 ms (cel ≤ 600)
  - trasa home: 1145 ms
  - trasa home: 423 ms
  - trasa home: 400 ms
