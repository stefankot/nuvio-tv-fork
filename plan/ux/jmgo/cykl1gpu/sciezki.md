# Ścieżki UX JMGO — cykl 1gpu (24.11 15:31, 192.168.31.252:5555, 3.5 min)

OK 1, UWAGA 0, BŁĄD 5, POMINIĘTA 0

| # | Ścieżka | Wynik | s | Uwagi |
|---|---|---|---|---|
| 4 | Baner ▼ → pierwszy rząd: plakat w całości widoczny, hero bez skoku | **BŁĄD** | 25 | UWAGA: F1 trasa home: fokus 9.5 s, stabilnie 9.5 s (budżet 0.6/3.0 s); BŁĄD: klatki gfx baner_rzad: Janky 90.48%, p95 125 ms, klatek 42 |
| 7 | Przytrzymana ▼ 3 s przez rzędy i ▲ do banera | **BŁĄD** | 26 | UWAGA: F1 trasa home: fokus 6.1 s, stabilnie 6.1 s (budżet 0.6/3.0 s); BŁĄD: klatki gfx przytrzymanie_v: Janky 96.43%, p95 250 ms, klatek 56 |
| 8 | Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku | **BŁĄD** | 45 | UWAGA: F1 trasa home: fokus 4.0 s, stabilnie 4.0 s (budżet 0.6/3.0 s); BŁĄD: klatki gfx przytrzymanie_h: Janky 41.23%, p95 65 ms, klatek 308 |
| 39 | Lista kanałów: przytrzymana ▼ 5 s i ▲ | **BŁĄD** | 35 | UWAGA: F1 trasa reshaped_live_tv: fokus 4.1 s, stabilnie 4.1 s (budżet 0.6/3.0 s); BŁĄD: klatki gfx kanaly: Janky 49.3%, p95 150 ms, klatek 213 |
| 42 | Przewodnik EPG: ►◄ w czasie, ▼▲ trzyma czas; podgląd nie kradnie fokusu | **OK** | 29 |  |
| 49 | Odkrywaj / Zobacz wszystko: siatka ▼►, przytrzymana ▼, OK → tytuł → Wstecz na ten sam kafelek | **BŁĄD** | 50 | BŁĄD: klatki gfx siatka: Janky 45.77%, p95 121 ms, klatek 284 |

## Kroki

### 4. Baner ▼ → pierwszy rząd: plakat w całości widoczny, hero bez skoku [F2 F7, gfxinfo] — BŁĄD
- miary: `{"open:home": 9.5, "gfx:baner_rzad": {"frames": 42, "janky%": 90.48, "p95": 125, "slowUI": 12, "slowBmp": 0, "50th": 29, "90th": 97, "99th": 133}}`
- trasa home [F1] UWAGA:  → n:Odtwórz Odtwórz (9.5 s) fokus 9.5 s, stabilnie 9.5 s (budżet 0.6/3.0 s)
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (3.3 s) przewinięcie/zmiana układu
- ▼ do rzędu [F7] OK:  → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (0.0 s)
- ► [F2] OK: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 (3.3 s)
- ► [F2] OK: n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 → n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (3.3 s)
- gfx baner_rzad [klatki] BŁĄD:  →  (0.0 s) Janky 90.48%, p95 125 ms, klatek 42

### 7. Przytrzymana ▼ 3 s przez rzędy i ▲ do banera [F9, gfxinfo] — BŁĄD
- miary: `{"open:home": 6.1, "gfx:przytrzymanie_v": {"frames": 56, "janky%": 96.43, "p95": 250, "slowUI": 32, "slowBmp": 1, "50th": 48, "90th": 150, "99th": 1400}, "klatki>=100ms(log)": 0}`
- trasa home [F1] UWAGA:  → n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (6.1 s) fokus 6.1 s, stabilnie 6.1 s (budżet 0.6/3.0 s)
- ▼ 3 s [F9] OK:  → n:Dig (2026) Komedia (3.0 s)
- ▲ 3 s [F9] OK: n:Dig (2026) Komedia → n:Stef+Patryk S | Stef+Patryk | Zmień profil (3.0 s)
- gfx przytrzymanie_v [klatki] BŁĄD:  →  (0.0 s) Janky 96.43%, p95 250 ms, klatek 56

### 8. Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku [F4 F7 F9, gfxinfo] — BŁĄD
- miary: `{"open:home": 4.0, "gfx:przytrzymanie_h": {"frames": 308, "janky%": 41.23, "p95": 65, "slowUI": 79, "slowBmp": 0, "50th": 14, "90th": 53, "99th": 250}, "klatki>=100ms(log)": 0}`
- trasa home [F1] UWAGA:  → n:Odtwórz Odtwórz (4.0 s) fokus 4.0 s, stabilnie 4.0 s (budżet 0.6/3.0 s)
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (3.3 s) przewinięcie/zmiana układu
- ▼ [F2] OK: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → n:Siostra Kopciuszka Animacja · ★ 6.8 (3.3 s)
- ► 3 s [F7] OK:  → n:Dziki Koń Dziewięć Komedia · ★ 8.1 (3.0 s)
- ► na krawędzi (po 3) [F4] OK: n:Zobacz wszystko: TMDB Popular Zobacz wszystko | TMDB Popular → n:Zobacz wszystko: TMDB Popular Zobacz wszystko | TMDB Popular (0.0 s)
- ◄ 4 s [F9] OK: n:Dziki Koń Dziewięć Komedia · ★ 8.1 → n:Siostra Kopciuszka Animacja · ★ 6.8 (4.0 s)
- gfx przytrzymanie_h [klatki] BŁĄD:  →  (0.0 s) Janky 41.23%, p95 65 ms, klatek 308

### 39. Lista kanałów: przytrzymana ▼ 5 s i ▲ [F9, gfxinfo] — BŁĄD
- miary: `{"open:reshaped_live_tv": 4.1, "gfx:kanaly": {"frames": 213, "janky%": 49.3, "p95": 150, "slowUI": 81, "slowBmp": 0, "50th": 16, "90th": 85, "99th": 300}, "klatki>=100ms(log)": 0}`
- trasa reshaped_live_tv [F1] UWAGA:  → n:Wszystkie Wszystkie (4.1 s) fokus 4.1 s, stabilnie 4.1 s (budżet 0.6/3.0 s)
- ▼×1 [-] OK:  → n:RT HD 110 | RT HD | Headline News | Documentary | In Conversation with Salman Khurshid (0.0 s) → wiersz kanału
- ▼ 5 s [F7] OK:  → n:PL: Eurosport 2 HD 176 | PL: Eurosport 2 HD | Igrzyska olimpijskie: Galeria Sław Mediolan-Cortina 2026 | Kola (5.0 s)
- ▲ 5 s [F9] OK: n:PL: Eurosport 2 HD 176 | PL: Eurosport 2 HD | Igrzyska olimpijskie: Galeria Sław Mediolan-Cortina 2026 | Kola → n:Stef+Patryk S | Stef+Patryk | Zmień profil (5.0 s) „n:Stef+Patryk S | Stef+Patryk | Zmień profil”
- gfx kanaly [klatki] BŁĄD:  →  (0.0 s) Janky 49.3%, p95 150 ms, klatek 213

### 42. Przewodnik EPG: ►◄ w czasie, ▼▲ trzyma czas; podgląd nie kradnie fokusu [F2 F6, gfxinfo] — OK
- miary: `{"open:reshaped_live_tv": 2.4, "gfx:epg": {"frames": 304, "janky%": 1.64, "p95": 10, "slowUI": 4, "slowBmp": 0, "50th": 6, "90th": 9, "99th": 18}}`
- trasa reshaped_live_tv [F1] OK:  → n:Wszystkie Wszystkie (2.4 s) fokus 2.4 s, stabilnie 2.4 s
- ▼×1 [-] OK:  → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.0 s) → wiersz kanału
- ► [F2] OK: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (3.3 s)
- ▼ [F2] OK: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov  2 | Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUS (3.4 s)
- ▼ trzyma czas [F2] OK: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov  2 | Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUS (0.0 s) x 0 → 0
- podgląd po 2,5 s [F6] OK: n:Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov  2 | Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUS → n:Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov  2 | Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUS (2.5 s)
- ▲ [F2] OK: n:Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov  2 | Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUS → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (3.4 s)
- ◄ [F2] OK: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (3.5 s)
- gfx epg [klatki] OK:  →  (0.0 s) Janky 1.64%, p95 10 ms, klatek 304

### 49. Odkrywaj / Zobacz wszystko: siatka ▼►, przytrzymana ▼, OK → tytuł → Wstecz na ten sam kafelek [F2 F5 F7 F9, gfxinfo] — BŁĄD
- miary: `{"open:discover": 2.8, "gfx:siatka": {"frames": 284, "janky%": 45.77, "p95": 121, "slowUI": 57, "slowBmp": 1, "50th": 15, "90th": 81, "99th": 400}}`
- trasa discover [F1] OK:  → n:Seriale Seriale (2.8 s) fokus 2.8 s, stabilnie 2.8 s
- ▼×2 [-] OK:  → n:The Body Dramat (0.0 s) → kafelek siatki
- ► [F2] OK: n:The Body Dramat → n:MobLand Kryminał · ★ 8.3 (3.3 s)
- ▼ [F2] OK: n:MobLand Kryminał · ★ 8.3 → n:The Good Daughter Kryminał (3.6 s)
- ▼ 2 s [F7] OK: n:The Good Daughter Kryminał → n:Supernatural Dramat · ★ 8.4 (2.0 s)
- ► [F2] OK: n:Supernatural Dramat · ★ 8.4 → n:Only Murders in the Building Komedia · ★ 8.0 (3.6 s)
- OK kafelek [F1] OK: n:Only Murders in the Building Komedia · ★ 8.0 → n:Odtwórz S1, O1 Odtwórz S1, O1 (11.1 s) pierwszy fokus 2.9 s
- Wstecz [F5] OK: n:Odtwórz S1, O1 Odtwórz S1, O1 → n:Only Murders in the Building Komedia · ★ 8.0 (8.2 s) pierwszy fokus 2.4 s
- gfx siatka [klatki] BŁĄD:  →  (0.0 s) Janky 45.77%, p95 121 ms, klatek 284

