# Ścieżki UX JMGO — cykl 98 (24.11 16:14, 192.168.31.252:5555, 1.5 min)

OK 3, UWAGA 1, BŁĄD 2, POMINIĘTA 0

| # | Ścieżka | Wynik | s | Uwagi |
|---|---|---|---|---|
| 3 | Baner: ◄► po przyciskach, skrajne bez zawijania, ▲ nie gubi fokusu | **OK** | 11 |  |
| 4 | Baner ▼ → pierwszy rząd: plakat w całości widoczny, hero bez skoku | **BŁĄD** | 6 | BŁĄD: klatki gfx baner_rzad: Janky 44.23%, p95 48 ms, klatek 52 |
| 5 | Rząd ►×6 → ▼ → ▲: ten sam plakat | **OK** | 15 |  |
| 6 | Rząd 1, 4. plakat ▼ → rząd 2 (konwencja F3/F2) | **OK** | 12 |  |
| 7 | Przytrzymana ▼ 3 s przez rzędy i ▲ do banera | **UWAGA** | 12 | UWAGA: klatki gfx przytrzymanie_v: Janky 0.0%, p95 4950 ms, klatek 0 |
| 8 | Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku | **BŁĄD** | 36 | BŁĄD: klatki gfx przytrzymanie_h: Janky 32.55%, p95 29 ms, klatek 513 |

## Kroki

### 3. Baner: ◄► po przyciskach, skrajne bez zawijania, ▲ nie gubi fokusu [F4 F10] — OK
- miary: `{"open:home": 2.4}`
- trasa home [F1] OK:  → n:Odtwórz Odtwórz (2.4 s) fokus 1.2 s, stabilnie 2.4 s; dtFocus - / dtFrame 92 ms; firstFocus 411 ms
- ► na krawędzi (po 3) [F4] OK: n:Następny wyróżniony tytuł Następny wyróżniony tytuł → n:Następny wyróżniony tytuł Następny wyróżniony tytuł (0.0 s)
- ◄ na pierwszym [F11] OK: n:Odtwórz Odtwórz → n:Główna Główna (0.0 s) otwiera menu
-   ForkFocus: dtFocus 19 / dtFrame 44 ms
- ▲ [F10] OK: n:Odtwórz Odtwórz → n:Główna Główna (0.8 s)

### 4. Baner ▼ → pierwszy rząd: plakat w całości widoczny, hero bez skoku [F2 F7, gfxinfo] — BŁĄD
- miary: `{"open:home": 2.0, "gfx:baner_rzad": {"frames": 52, "janky%": 44.23, "p95": 48, "slowUI": 6, "slowBmp": 0, "50th": 13, "90th": 48, "99th": 65}}`
- trasa home [F1] OK:  → n:Odtwórz Odtwórz (2.0 s) fokus 0.1 s, stabilnie 2.0 s; dtFocus - / dtFrame 56 ms; firstFocus 1108 ms
-   ForkFocus: dtFocus 21 / dtFrame 48 ms
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (1.3 s) przewinięcie/zmiana układu
- ▼ do rzędu [F7] OK:  → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (0.0 s)
-   ForkFocus: dtFocus 12 / dtFrame 35 ms
- ► [F2] OK: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 (0.7 s)
-   ForkFocus: dtFocus 16 / dtFrame 39 ms
- ► [F2] OK: n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 → n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (0.7 s)
- gfx baner_rzad [klatki] BŁĄD:  →  (0.0 s) Janky 44.23%, p95 48 ms, klatek 52

### 5. Rząd ►×6 → ▼ → ▲: ten sam plakat [F3] — OK
- miary: `{"open:home": 2.2}`
- trasa home [F1] OK:  → n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (2.2 s) fokus 2.2 s, stabilnie 2.2 s; dtFocus - / dtFrame 33 ms; firstFocus 420 ms
-   ForkFocus: dtFocus 19 / dtFrame 44 ms
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (1.2 s) przewinięcie/zmiana układu
-   ForkFocus: dtFocus 17 / dtFrame 41 ms
- ► [F2] OK: n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min → n:Apple's Local AI App for Mac Is Coming (Harbor), Apple's Loc Apple's Local AI App for Mac Is Coming (Harbor), Apple's Local AI App for Mac Is Coming (H (0.7 s)
-   ForkFocus: dtFocus 17 / dtFrame 71 ms
- ► [F2] OK: n:Apple's Local AI App for Mac Is Coming (Harbor), Apple's Loc Apple's Local AI App for Mac Is Coming (Harbor), Apple's Local AI App for Mac Is Coming (H → n:Patoterapie - historie pacjentów skrzywdzonych przez terapeu Patoterapie - historie pacjentów skrzywdzonych przez terapeutów || Marianna Fijewska-Kalin (0.8 s)
-   ForkFocus: dtFocus 20 / dtFrame 42 ms
- ► [F2] OK: n:Patoterapie - historie pacjentów skrzywdzonych przez terapeu Patoterapie - historie pacjentów skrzywdzonych przez terapeutów || Marianna Fijewska-Kalin → n:Igrzyska śmierci, Igrzyska śmierci · 135m, Zostało 135 min Igrzyska śmierci, Igrzyska śmierci · 135m, Zostało 135 min | Zostało 135 min (0.7 s)
-   ForkFocus: dtFocus 21 / dtFrame 46 ms
- ► [F2] OK: n:Igrzyska śmierci, Igrzyska śmierci · 135m, Zostało 135 min Igrzyska śmierci, Igrzyska śmierci · 135m, Zostało 135 min | Zostało 135 min → n:Zobaczysz wszystko, Zobaczysz wszystko · 173m, Zostało 173 m Zobaczysz wszystko, Zobaczysz wszystko · 173m, Zostało 173 min | Zostało 173 min (0.9 s)
-   ForkFocus: dtFocus 8 / dtFrame 33 ms
- ► [F2] OK: n:Zobaczysz wszystko, Zobaczysz wszystko · 173m, Zostało 173 m Zobaczysz wszystko, Zobaczysz wszystko · 173m, Zostało 173 min | Zostało 173 min → n:AI agents are already going rogue, AI agents are already goi AI agents are already going rogue, AI agents are already going rogue · 8m, Zostało 8 min | (1.1 s)
-   ForkFocus: dtFocus 18 / dtFrame 47 ms
- ► [F2] OK: n:AI agents are already going rogue, AI agents are already goi AI agents are already going rogue, AI agents are already going rogue · 8m, Zostało 8 min | → n:Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min (1.3 s)
-   ForkFocus: dtFocus 21 / dtFrame 49 ms
- ▼ [F2] OK: n:Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min → n:Tylko na święta Romans · ★ 5.4 (1.2 s)
-   ForkFocus: dtFocus 12 / dtFrame 44 ms
- ▲ [F2] OK: n:Tylko na święta Romans · ★ 5.4 → n:Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min (1.1 s)
- powrót ▲ [F3] OK: n:Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min → n:Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min (0.0 s)

### 6. Rząd 1, 4. plakat ▼ → rząd 2 (konwencja F3/F2) [F2 F3] — OK
- miary: `{"open:home": 2.4}`
- trasa home [F1] OK:  → n:Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min (2.4 s) fokus 0.3 s, stabilnie 2.4 s; dtFocus - / dtFrame 29 ms; firstFocus 415 ms
-   ForkFocus: dtFocus 21 / dtFrame 44 ms
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min (1.3 s) przewinięcie/zmiana układu
-   ForkFocus: dtFocus 8 / dtFrame 31 ms
- ► [F2] OK: n:Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min → n:Badhaai Do, Badhaai Do · 144m, Zostało 144 min Badhaai Do, Badhaai Do · 144m, Zostało 144 min | Zostało 144 min (0.7 s)
-   ForkFocus: dtFocus 15 / dtFrame 38 ms
- ► [F2] OK: n:Badhaai Do, Badhaai Do · 144m, Zostało 144 min Badhaai Do, Badhaai Do · 144m, Zostało 144 min | Zostało 144 min → n:Star Trek: Następne pokolenie, S7, E5, Następny: S7 E5 Star Trek: Następne pokolenie, S7, E5, Następny: S7 E5 | Następny: S7 E5 (0.8 s)
-   ForkFocus: dtFocus 18 / dtFrame 40 ms
- ► [F2] OK: n:Star Trek: Następne pokolenie, S7, E5, Następny: S7 E5 Star Trek: Następne pokolenie, S7, E5, Następny: S7 E5 | Następny: S7 E5 → n:Made in Heaven, S1, E2 · 44m, Zostało 44 min Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min (0.9 s)
-   ForkFocus: dtFocus 12 / dtFrame 39 ms
- ▼ [F2] OK: n:Made in Heaven, S1, E2 · 44m, Zostało 44 min Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min → n:Heartland Thriller · ★ 5.5 (1.3 s)
- kolumna ▼ [F2] OK: n:Made in Heaven, S1, E2 · 44m, Zostało 44 min Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min → n:Heartland Thriller · ★ 5.5 (0.0 s) Δx=90 px
-   ForkFocus: dtFocus 22 / dtFrame 55 ms
- ▲ [F2] OK: n:Heartland Thriller · ★ 5.5 → n:Made in Heaven, S1, E2 · 44m, Zostało 44 min Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min (1.2 s)
- ▲ wraca [F3] OK: n:Made in Heaven, S1, E2 · 44m, Zostało 44 min Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min → n:Made in Heaven, S1, E2 · 44m, Zostało 44 min Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min (0.0 s)

### 7. Przytrzymana ▼ 3 s przez rzędy i ▲ do banera [F9, gfxinfo] — UWAGA
- miary: `{"open:home": 2.5, "gfx:przytrzymanie_v": {"frames": 0, "janky%": 0.0, "p95": 4950, "slowUI": 0, "slowBmp": 0, "50th": 4950, "90th": 4950, "99th": 4950}, "klatki>=100ms(log)": 0}`
- trasa home [F1] OK:  → n:Made in Heaven, S1, E2 · 44m, Zostało 44 min Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min (2.5 s) fokus 0.3 s, stabilnie 2.5 s; dtFocus - / dtFrame 27 ms; firstFocus 395 ms
- ▼ 3 s [F9] OK:  → n:Odtwórz Odtwórz (3.0 s)
- ▲ 3 s [F9] OK: n:Odtwórz Odtwórz → n:Odtwórz Odtwórz (3.0 s)
- gfx przytrzymanie_v [klatki] UWAGA:  →  (0.0 s) Janky 0.0%, p95 4950 ms, klatek 0

### 8. Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku [F4 F7 F9, gfxinfo] — BŁĄD
- miary: `{"open:home": 0.3, "gfx:przytrzymanie_h": {"frames": 513, "janky%": 32.55, "p95": 29, "slowUI": 38, "slowBmp": 0, "50th": 14, "90th": 25, "99th": 34}, "klatki>=100ms(log)": 0}`
- trasa home [F1] OK:  → n:Odtwórz Odtwórz (0.3 s) fokus 0.3 s, stabilnie 0.3 s; dtFocus - / dtFrame 62 ms; firstFocus 414 ms
-   ForkFocus: dtFocus 23 / dtFrame 51 ms
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (1.2 s) przewinięcie/zmiana układu
-   ForkFocus: dtFocus 14 / dtFrame 38 ms
- ▼ [F2] OK: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → n:Siostra Kopciuszka Animacja · ★ 6.8 (1.2 s)
- ► 3 s [F7] OK:  → n:Siostra Kopciuszka Animacja · ★ 6.8 (3.0 s)
- ► na krawędzi (po 20) [F4] OK: n:Zobacz wszystko: TMDB Popular Zobacz wszystko | TMDB Popular → n:Zobacz wszystko: TMDB Popular Zobacz wszystko | TMDB Popular (0.0 s)
- ◄ 4 s [F9] OK: n:Siostra Kopciuszka Animacja · ★ 6.8 → n:Zobacz wszystko: TMDB Popular Zobacz wszystko | TMDB Popular (4.0 s)
- gfx przytrzymanie_h [klatki] BŁĄD:  →  (0.0 s) Janky 32.55%, p95 29 ms, klatek 513


## Pomiar fokusu (ForkFocus)

- strzałka → klatka (dtFrame): n=21, mediana 44 ms, p95 55 ms, max 71 ms (cel ≤ 50 / ≤ 100)
- strzałka → onFocusChanged (dtFocus): mediana 18 ms, p95 22 ms
- Wstecz → fokus (dtFrame): n=0, mediana None ms, p95 None ms (cel ≤ 300)
- pierwszy fokus na ekranie (firstFocus): n=6, mediana 414 ms, p95 1108 ms, max 1108 ms (cel ≤ 600)
  - trasa home: 1108 ms
  - trasa home: 420 ms
  - trasa home: 415 ms
  - trasa home: 414 ms
  - trasa home: 411 ms
  - trasa home: 395 ms

## Czas testu

- odczyty ekranu: 82 (czytnik 82, zwykły 0, błędy czytnika 0), razem 0.8 min = 51% czasu, średnio 0.57 s; klawisze przez czytnik: 54
