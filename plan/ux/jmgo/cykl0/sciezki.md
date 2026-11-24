# Ścieżki UX JMGO — cykl 0 (24.11 14:32, 192.168.31.252:5555, 40.6 min)

OK 15, UWAGA 17, BŁĄD 15, POMINIĘTA 3

| # | Ścieżka | Wynik | s | Uwagi |
|---|---|---|---|---|
| 1 | Zimny start: fokus na „Odtwórz” w banerze; po dociągnięciu rzędów fokus nie skacze | **UWAGA** | 18 | UWAGA: zimny start do fokusu 7.7 s (budżet 3.5 s, rozdzielczość odczytu ~2 s) |
| 2 | Ciepły start (HOME → ikona): fokus tam, gdzie był, albo na banerze | **OK** | 18 |  |
| 3 | Baner: ◄► po przyciskach, skrajne bez zawijania, ▲ nie gubi fokusu | **OK** | 52 |  |
| 4 | Baner ▼ → pierwszy rząd: plakat w całości widoczny, hero bez skoku | **BŁĄD** | 21 | BŁĄD: klatki gfx baner_rzad: Janky 92.86%, p95 65 ms, klatek 42 |
| 5 | Rząd ►×6 → ▼ → ▲: ten sam plakat | **UWAGA** | 42 | UWAGA: F1 trasa home: fokus 3.9 s, stabilnie 3.9 s (budżet 0.6/3.0 s) |
| 6 | Rząd 1, 4. plakat ▼ → rząd 2 (konwencja F3/F2) | **OK** | 32 |  |
| 7 | Przytrzymana ▼ 3 s przez rzędy i ▲ do banera | **BŁĄD** | 23 | UWAGA: F9 ▲ 3 s: S / Stef+Patryk / Zmień profil; BŁĄD: klatki gfx przytrzymanie_v: Janky 92.65%, p95 150 ms, klatek 68 |
| 8 | Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku | **BŁĄD** | 48 | BŁĄD: klatki gfx przytrzymanie_h: Janky 98.37%, p95 93 ms, klatek 246 |
| 9 | Plakat OK → tytuł → Wstecz: ten sam plakat, to samo przewinięcie | **OK** | 45 |  |
| 10 | Długie OK na plakacie → menu → Wstecz: fokus na plakacie, puszczenie nic nie klika | **OK** | 56 |  |
| 11 | ◄ z pierwszego plakatu → menu z fokusem na „Główna” | **BŁĄD** | 39 | BŁĄD: F11 ◄ do menu: fokus „Animacja · ★ 6.8” |
| 12 | Menu ▼ do końca i ▲ bez zawijania, ► → element, z którego wyszło | **BŁĄD** | 107 | BŁĄD: F11 ► z menu: „Odtwórz” zamiast „Breaking Bad, S1, E1 · 58m, Zostało 58 min / Zostało 58 min” |
| 13 | Menu → Szukaj → Biblioteka → Live TV → Główna: pierwszy fokus w treści, Główna przywraca fokus | **UWAGA** | 184 | UWAGA: F5 Główna: powrót: „Breaking Bad, S1, E1 · 58m, Zostało 58 min / Zostało 58 min” zamiast „W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min / Zostało 1” |
| 14 | Wstecz z głębi rzędu: baner/menu, drugi Wstecz nie zamyka bez pytania | **OK** | 39 |  |
| 15 | Długie OK na pozycji menu: nic się nie psuje | **UWAGA** | 34 | UWAGA: F10 długie OK w menu: długie OK zadziałało jak zwykłe OK |
| 16 | Szukaj: „bat” klawiaturą ekranową; fokus na klawiszu, wyniki bez kradzieży fokusu | **OK** | 46 |  |
| 17 | Szukaj: z klawiatury do wyników (liczba ►), ◄ wraca na ten sam klawisz | **UWAGA** | 61 | UWAGA: klatki gfx szukaj: Janky 8.82%, p95 19 ms, klatek 34 |
| 18 | Szukaj: wynik OK → tytuł → Wstecz: fokus na wyniku | **OK** | 76 |  |
| 19 | Szukaj: polecane — długie OK → menu → Wstecz | **BŁĄD** | 42 | UWAGA: F8 długie OK: zmiana ekranu bez logu „długie OK” (element spoza Niuni?); BŁĄD: F5 Wstecz: fokus „a” zamiast „Odkrywaj: gatunek, rok, katalog” |
| 20 | Szukaj: kasowanie liter do pustego pola, fokus nie spada w niewidoczne miejsce | **UWAGA** | 96 | UWAGA: nie znaleziono: ⌫/Usuń/Kasuj/Backspace/Delete (▼×8); UWAGA: nie znaleziono: ⌫/Usuń/Kasuj/Backspace/Delete (►×8) |
| 21 | Biblioteka: chipy ▼ do treści, ▲ na wybrany chip | **BŁĄD** | 17 | BŁĄD: F3 ▲ na chip: „Seriale (42)” zamiast „Filmy (120)” |
| 22 | Biblioteka: Historia — OK → tytuł → Wstecz; długie OK na pozycji | **BŁĄD** | 54 | BŁĄD: F5 Wstecz: fokus „Wszystko” zamiast „STAR TREK: NASTĘPNE POKOLENIE / S7 E4 · dziś 06:00”; BŁĄD: F8 długie OK: brak akcji długiego OK; BŁĄD: F5 Wstecz: fokus „Bat / Film · Animacja · 2018” zamiast „STAR TREK: NASTĘPNE POKOLENIE / S7 E4 · dziś 06:00” |
| 23 | Biblioteka: pusta lista — fokus na przycisku stanu pustego | **POMINIĘTA** | 50 | POMINIĘTA: brak pustej listy w Bibliotece |
| 24 | Film: fokus na „Odtwórz” w budżecie; ◄► po przyciskach bez zawijania | **OK** | 39 |  |
| 25 | „Więcej” → „Mniej”: fokus zostaje na przycisku po zmianie układu | **OK** | 31 |  |
| 26 | Serial: ▼ pigułki sezonów, ► sezon, ▼ odcinki, ▲ na wybraną pigułkę | **UWAGA** | 27 | UWAGA: nie znaleziono: Sezon 1/Sezon (▼×6) |
| 27 | Tabela sezon × odcinek (Z-8): ◄►▲▼ po komórkach, przewijanie trzyma fokus | **POMINIĘTA** | 27 | UWAGA: nie znaleziono: ★ (►×6); POMINIĘTA: brak ★ (tabela ocen) w hero |
| 28 | Odcinek OK → wybór źródła → Wstecz: ten odcinek; długie OK = opcje odcinka | **UWAGA** | 38 | UWAGA: F5 Wstecz: ten sam element, inne położenie [80, 416, 490, 955] vs [80, 388, 490, 927]; UWAGA: F5 Wstecz: ten sam element, inne położenie [80, 416, 490, 955] vs [80, 388, 490, 927] |
| 29 | Obsada → Osoba → plakat → tytuł → Wstecz ×3: za każdym razem element źródłowy | **UWAGA** | 41 | UWAGA: nie znaleziono osoby w obsadzie |
| 30 | Zwiastun: OK → od razu „Wczytuję zwiastun…” → Wstecz na kartę | **OK** | 29 |  |
| 31 | Wstecz z głębi strony (▼×6): góra albo wyjście, fokus zawsze widoczny | **OK** | 24 |  |
| 32 | Wybór źródła: pierwszy fokus na pierwszym źródle; filtry ▲▼ wracają na wybrany filtr | **UWAGA** | 24 | UWAGA: zamiast listy źródeł: odtwarzanie albo brak listy |
| 33 | Odtwarzacz: OK nakładka, ▲ do przycisków nad paskiem, ◄►, ▼ na pasek | **BŁĄD** | 80 | UWAGA: F1 trasa home: fokus 5.1 s, stabilnie 5.1 s (budżet 0.6/3.0 s); UWAGA: F1 OK „Oglądaj dalej” → odtwarzacz: pierwszy fokus 5.6 s; BŁĄD: F2 ◄: fokus zgubiony |
| 34 | Przewijanie ◄► pojedyncze i przytrzymane; po schowaniu nakładki klawisz nie gubi fokusu | **UWAGA** | 72 | UWAGA: F1 trasa home: fokus 4.1 s, stabilnie 4.1 s (budżet 0.6/3.0 s) |
| 35 | „Zmień źródło” → lista → Wstecz na ten przycisk | **UWAGA** | 110 | UWAGA: nie znaleziono: Więcej (▲×3); UWAGA: nie znaleziono: Zmień źródło (▼×6); UWAGA: brak „Zmień źródło” |
| 36 | Wstecz z odtwarzacza → strona/Główna, fokus na źródle; czas pierwszej klatki | **UWAGA** | 51 | UWAGA: F1 trasa home: fokus 4.1 s, stabilnie 4.1 s (budżet 0.6/3.0 s) |
| 37 | Live TV: wejście — fokus na ostatnim lub pierwszym kanale; czas listy z dysku | **OK** | 13 |  |
| 38 | Chip „Kraj i język” → panel → Wstecz: fokus na chipie (bez zmiany wyboru) | **POMINIĘTA** | 64 | UWAGA: nie znaleziono: Kraj/Język/kraje (►×14); POMINIĘTA: brak chipa „Kraj i język” |
| 39 | Lista kanałów: przytrzymana ▼ 5 s i ▲ | **BŁĄD** | 27 | BŁĄD: F7 ▼ 5 s: fokus przy krawędzi/poza ekranem [0, 728, 1920, 824]; BŁĄD: klatki gfx kanaly: Janky 70.85%, p95 89 ms, klatek 199 |
| 40 | Kanał OK → odtwarzanie → Wstecz: ten sam kanał (okno „nie odpowiada”: przyciski i Wstecz) | **UWAGA** | 41 | UWAGA: F1 OK kanał: pierwszy fokus 5.8 s |
| 41 | Długie OK na kanale → „Opcje kanału” → ▼ po opcjach → Wstecz na kanał | **BŁĄD** | 52 | UWAGA: F4 ▼×8: nie doszedł do krawędzi; BŁĄD: F5 Wstecz: fokus „5 / Strong8K · ENDED / ELLIOT SLESSOR - ARTEMIJS ZIZINS / Mon 23 Nov 20:30 CET (DE) / 8K E” zamiast „1 / Strong8K · ENDED / BEN MERTENS - JIMMY WHITE / Mon 23 Nov 15:30 CET (DE) / 8K EXCLUSIV” |
| 42 | Przewodnik EPG: ►◄ w czasie, ▼▲ trzyma czas; podgląd nie kradnie fokusu | **BŁĄD** | 30 | BŁĄD: F2 ►: fokus nie ruszył się w stronę strzałki |
| 43 | „Szukaj w telewizji”: fokus w polu, wynik OK → Wstecz | **BŁĄD** | 40 | BŁĄD: F1 trasa fork_tv_search: fokus w menu („Szukaj kanału lub programu”), nie w treści; UWAGA: F6 klawiatura „tvp”: BRAK fokusu albo klawisza t; UWAGA: nie znaleziono: wynik TVP (▼×8) |
| 44 | Ustawienia: ▼ kategorie, ► treść, ◄ ta sama kategoria; Wygląd → „Szkło” niedostępne | **UWAGA** | 67 | UWAGA: nie znaleziono: Szkło (▼×12) |
| 45 | Przełącznik OK ×2: stan wraca, fokus zostaje | **OK** | 20 |  |
| 46 | Napisy → wygląd: Atkinson Hyperlegible domyślna, na liście Tiresias; lista → Wstecz bez zmiany | **UWAGA** | 89 | UWAGA: nie znaleziono: Czcionka (▼×25); UWAGA: brak pozycji „Czcionka” |
| 47 | YouTube: czas do fokusu w treści, OK → odtwarzacz → Wstecz (kod obcy: pomiar) | **BŁĄD** | 21 | BŁĄD: F1 trasa youtube: fokus w menu („Główna”), nie w treści; UWAGA: YouTube: brak fokusu w treści po otwarciu (kod obcy — [i]); UWAGA: nie znaleziono: film (▼×4) |
| 48 | Profile: ◄► po profilach, (PIN: wejście i Wstecz bez wpisywania) | **OK** | 35 |  |
| 49 | Odkrywaj / Zobacz wszystko: siatka ▼►, przytrzymana ▼, OK → tytuł → Wstecz na ten sam kafelek | **BŁĄD** | 48 | UWAGA: F1 OK kafelek: pierwszy fokus 3.8 s; BŁĄD: F5 Wstecz: fokus „Seriale” zamiast „Dramat · ★ 6.6”; BŁĄD: klatki gfx siatka: Janky 58.12%, p95 105 ms, klatek 277 |
| 50 | Kalendarz, Dodatki, Kolejność katalogów, Kolekcje: pierwszy fokus, Wstecz z pozycji na źródło | **UWAGA** | 92 | UWAGA: F5 addon_manager: Wstecz: ten sam element, inne położenie [72, 389, 1848, 539] vs [72, 390, 1848, 540] |

## Kroki

### 1. Zimny start: fokus na „Odtwórz” w banerze; po dociągnięciu rzędów fokus nie skacze [F1 F6, czas] — UWAGA
- miary: `{"zimny TotalTime ms": 1469, "zimny fokus s (dump)": 7.7}`
- zimny start [F1] OK:  → Odtwórz (7.7 s) TotalTime 1469 ms; fokus po 7.7 s
- po 6 s [F6] OK: Odtwórz → Odtwórz (6.0 s)

### 2. Ciepły start (HOME → ikona): fokus tam, gdzie był, albo na banerze [F1] — OK
- miary: `{"open:home": 3.7, "ciepły TotalTime ms": 231, "ciepły: fokus": "tam, gdzie był"}`
- trasa home [F1] OK:  → Odtwórz (3.7 s) fokus 3.7 s, stabilnie 3.7 s
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.6 s) przewinięcie/zmiana układu
- HOME → ikona [F1] OK: Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (2.6 s) TotalTime 231 ms; fokus: tam, gdzie był

### 3. Baner: ◄► po przyciskach, skrajne bez zawijania, ▲ nie gubi fokusu [F4 F10] — OK
- miary: `{"open:home": 3.6}`
- trasa home [F1] OK:  → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.6 s) fokus 3.6 s, stabilnie 3.6 s
- ► na krawędzi (po 3) [F4] OK: Następny wyróżniony tytuł → Następny wyróżniony tytuł (0.0 s)
- ◄ na pierwszym [F11] OK: Odtwórz → Główna (0.0 s) otwiera menu
- ▲ [F10] OK: Odtwórz → Główna (3.2 s)

### 4. Baner ▼ → pierwszy rząd: plakat w całości widoczny, hero bez skoku [F2 F7, gfxinfo] — BŁĄD
- miary: `{"open:home": 3.1, "gfx:baner_rzad": {"frames": 42, "janky%": 92.86, "p95": 65, "slowUI": 0, "slowBmp": 0, "50th": 42, "90th": 61, "99th": 77}}`
- trasa home [F1] OK:  → Odtwórz (3.1 s) fokus 3.1 s, stabilnie 3.1 s
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.4 s) przewinięcie/zmiana układu
- ▼ do rzędu [F7] OK:  → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (0.0 s)
- ► [F2] OK: Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min → W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 (3.3 s)
- ► [F2] OK: W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 → Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (3.3 s)
- gfx baner_rzad [klatki] BŁĄD:  →  (0.0 s) Janky 92.86%, p95 65 ms, klatek 42

### 5. Rząd ►×6 → ▼ → ▲: ten sam plakat [F3] — UWAGA
- miary: `{"open:home": 3.9}`
- trasa home [F1] UWAGA:  → Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (3.9 s) fokus 3.9 s, stabilnie 3.9 s (budżet 0.6/3.0 s)
- ▼ [F2] OK: Odtwórz → Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (3.5 s) przewinięcie/zmiana układu
- ► [F2] OK: Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min → Apple's Local AI App for Mac Is Coming (Harbor), Apple's Local AI App for Mac Is Coming (H (3.2 s)
- ► [F2] OK: Apple's Local AI App for Mac Is Coming (Harbor), Apple's Local AI App for Mac Is Coming (H → Patoterapie - historie pacjentów skrzywdzonych przez terapeutów || Marianna Fijewska-Kalin (3.4 s)
- ► [F2] OK: Patoterapie - historie pacjentów skrzywdzonych przez terapeutów || Marianna Fijewska-Kalin → Igrzyska śmierci, Igrzyska śmierci · 135m, Zostało 135 min | Zostało 135 min (3.2 s)
- ► [F2] OK: Igrzyska śmierci, Igrzyska śmierci · 135m, Zostało 135 min | Zostało 135 min → Zobaczysz wszystko, Zobaczysz wszystko · 173m, Zostało 173 min | Zostało 173 min (3.3 s)
- ► [F2] OK: Zobaczysz wszystko, Zobaczysz wszystko · 173m, Zostało 173 min | Zostało 173 min → AI agents are already going rogue, AI agents are already going rogue · 8m, Zostało 8 min | (3.3 s)
- ► [F2] OK: AI agents are already going rogue, AI agents are already going rogue · 8m, Zostało 8 min | → Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min (3.2 s)
- ▼ [F2] OK: Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min → Romans · ★ 5.4 (3.4 s)
- ▲ [F2] OK: Romans · ★ 5.4 → Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min (3.4 s)
- powrót ▲ [F3] OK: Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min → Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min (0.0 s)

### 6. Rząd 1, 4. plakat ▼ → rząd 2 (konwencja F3/F2) [F2 F3] — OK
- miary: `{"open:home": 3.6}`
- trasa home [F1] OK:  → Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min (3.6 s) fokus 3.6 s, stabilnie 3.6 s
- ▼ [F2] OK: Odtwórz → Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min (3.2 s) przewinięcie/zmiana układu
- ► [F2] OK: Kino oczami Cunk, S1, E1 · 12m, Zostało 12 min | Zostało 12 min → Badhaai Do, Badhaai Do · 144m, Zostało 144 min | Zostało 144 min (3.3 s)
- ► [F2] OK: Badhaai Do, Badhaai Do · 144m, Zostało 144 min | Zostało 144 min → Star Trek: Następne pokolenie, S7, E5, Następny: S7 E5 | Następny: S7 E5 (3.4 s)
- ► [F2] OK: Star Trek: Następne pokolenie, S7, E5, Następny: S7 E5 | Następny: S7 E5 → Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min (3.5 s)
- ▼ [F2] OK: Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min → Thriller · ★ 5.5 (3.3 s)
- kolumna ▼ [F2] OK: Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min → Thriller · ★ 5.5 (0.0 s) Δx=90 px
- ▲ [F2] OK: Thriller · ★ 5.5 → Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min (3.3 s)
- ▲ wraca [F3] OK: Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min → Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min (0.0 s)

### 7. Przytrzymana ▼ 3 s przez rzędy i ▲ do banera [F9, gfxinfo] — BŁĄD
- miary: `{"open:home": 3.5, "gfx:przytrzymanie_v": {"frames": 68, "janky%": 92.65, "p95": 150, "slowUI": 0, "slowBmp": 0, "50th": 53, "90th": 97, "99th": 250}, "klatki>=100ms(log)": 0}`
- trasa home [F1] OK:  → Made in Heaven, S1, E2 · 44m, Zostało 44 min | Zostało 44 min (3.5 s) fokus 3.5 s, stabilnie 3.5 s
- ▼ 3 s [F9] OK:  → Komedia (3.0 s)
- ▲ 3 s [F9] UWAGA: Komedia → S | Stef+Patryk | Zmień profil (3.0 s)
- gfx przytrzymanie_v [klatki] BŁĄD:  →  (0.0 s) Janky 92.65%, p95 150 ms, klatek 68

### 8. Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku [F4 F7 F9, gfxinfo] — BŁĄD
- miary: `{"open:home": 3.2, "gfx:przytrzymanie_h": {"frames": 246, "janky%": 98.37, "p95": 93, "slowUI": 0, "slowBmp": 0, "50th": 69, "90th": 89, "99th": 300}, "klatki>=100ms(log)": 0}`
- trasa home [F1] OK:  → Odtwórz (3.2 s) fokus 3.2 s, stabilnie 3.2 s
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.2 s) przewinięcie/zmiana układu
- ▼ [F2] OK: Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min → Animacja · ★ 6.8 (3.3 s)
- ► 3 s [F7] OK:  → Dramat · ★ 6.6 (3.0 s)
- ► na krawędzi (po 5) [F4] OK: Zobacz wszystko | TMDB Popular → Zobacz wszystko | TMDB Popular (0.0 s)
- ◄ 4 s [F9] OK: Dramat · ★ 6.6 → Animacja · ★ 6.8 (4.0 s)
- gfx przytrzymanie_h [klatki] BŁĄD:  →  (0.0 s) Janky 98.37%, p95 93 ms, klatek 246

### 9. Plakat OK → tytuł → Wstecz: ten sam plakat, to samo przewinięcie [F5 F12, czas] — OK
- miary: `{"open:home": 3.5}`
- trasa home [F1] OK:  → Animacja · ★ 6.8 (3.5 s) fokus 3.5 s, stabilnie 3.5 s
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.3 s) przewinięcie/zmiana układu
- ▼ [F2] OK: Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min → Animacja · ★ 6.8 (3.3 s)
- ► [F2] OK: Animacja · ★ 6.8 → Romans · ★ 6.2 (3.6 s)
- ► [F2] OK: Romans · ★ 6.2 → Sci-fi · ★ 7.5 (3.5 s)
- OK na plakacie [F1] OK: Sci-fi · ★ 7.5 → Odtwórz (7.5 s) pierwszy fokus 2.2 s
- Wstecz [F5] OK: Odtwórz → Sci-fi · ★ 7.5 (7.9 s) pierwszy fokus 2.1 s

### 10. Długie OK na plakacie → menu → Wstecz: fokus na plakacie, puszczenie nic nie klika [F8 F5] — OK
- miary: `{"open:home": 3.6}`
- trasa home [F1] OK:  → Sci-fi · ★ 7.5 (3.6 s) fokus 3.6 s, stabilnie 3.6 s
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.4 s) przewinięcie/zmiana układu
- ▼ [F2] OK: Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min → Sci-fi · ★ 7.5 (3.3 s)
- ► [F2] OK: Sci-fi · ★ 7.5 → Dramat · ★ 5.6 (3.2 s)
- długie OK [F8] OK: Dramat · ★ 5.6 → Szczegóły filmu (5.0 s) długie OK
- Wstecz [F5] OK: Szczegóły filmu → Dramat · ★ 5.6 (7.5 s) pierwszy fokus 2.1 s
- krótkie OK po długim [F1] OK: Dramat · ★ 5.6 → Odtwórz (7.6 s) pierwszy fokus 2.1 s
- Wstecz [F5] OK: Odtwórz → Dramat · ★ 5.6 (10.0 s) pierwszy fokus 2.3 s

### 11. ◄ z pierwszego plakatu → menu z fokusem na „Główna” [F11] — BŁĄD
- miary: `{"open:home": 3.6}`
- trasa home [F1] OK:  → Dramat · ★ 5.6 (3.6 s) fokus 3.6 s, stabilnie 3.6 s
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.2 s) przewinięcie/zmiana układu
- ▼ [F2] OK: Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min → Dramat · ★ 5.6 (3.2 s)
- ◄ na krawędzi (po 3) [F4] OK: Animacja · ★ 6.8 → Animacja · ★ 6.8 (0.0 s)
- ◄ do menu [F11] BŁĄD:  → Animacja · ★ 6.8 (0.0 s) fokus „Animacja · ★ 6.8”

### 12. Menu ▼ do końca i ▲ bez zawijania, ► → element, z którego wyszło [F4 F11] — BŁĄD
- miary: `{"open:home": 3.6}`
- trasa home [F1] OK:  → Animacja · ★ 6.8 (3.6 s) fokus 3.6 s, stabilnie 3.6 s
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.3 s) przewinięcie/zmiana układu
- ▼ na krawędzi (po 9) [F4] OK: Komedia → Komedia (0.0 s)
- ▲ na krawędzi (po 8) [F4] OK: S | Stef+Patryk | Zmień profil → S | Stef+Patryk | Zmień profil (0.0 s)
- ▼×2 [-] OK:  → Główna (0.0 s) → Główna
- ► [F2] OK: Główna → Odtwórz (3.3 s)
- ► z menu [F11] BŁĄD: Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min → Odtwórz (0.0 s) „Odtwórz” zamiast „Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min”

### 13. Menu → Szukaj → Biblioteka → Live TV → Główna: pierwszy fokus w treści, Główna przywraca fokus [F1 F5, czas] — UWAGA
- miary: `{"open:home": 3.1}`
- trasa home [F1] OK:  → Odtwórz (3.1 s) fokus 3.1 s, stabilnie 3.1 s
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.3 s) przewinięcie/zmiana układu
- ► [F2] OK: Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min → W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 (3.4 s)
- menu → Szukaj [F1] OK: Szukaj → a (8.0 s) pierwszy fokus 2.3 s
- menu → Biblioteka [F1] OK: Biblioteka → Wszystko (7.7 s) pierwszy fokus 2.3 s
- menu → Live TV [F1] OK: Live TV → Wszystkie (10.2 s) pierwszy fokus 2.9 s
- menu → Główna [F1] OK: Główna → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (7.9 s) pierwszy fokus 2.3 s
- Główna: powrót [F5] UWAGA: W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (0.0 s) „Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min” zamiast „W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1”

### 14. Wstecz z głębi rzędu: baner/menu, drugi Wstecz nie zamyka bez pytania [F12] — OK
- miary: `{"open:home": 3.6}`
- trasa home [F1] OK:  → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.6 s) fokus 3.6 s, stabilnie 3.6 s
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.4 s) przewinięcie/zmiana układu
- ▼ [F2] OK: Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min → Animacja · ★ 6.8 (3.2 s)
- ▼ [F2] OK: Animacja · ★ 6.8 → Sci-fi · ★ 8.7 (3.3 s)
- Wstecz 1 [F12] OK: Sci-fi · ★ 8.7 → Odtwórz (8.1 s) pierwszy fokus 2.0 s
- Wstecz 2 [F12] OK: Odtwórz → Główna (7.3 s) pierwszy fokus 2.0 s

### 15. Długie OK na pozycji menu: nic się nie psuje [F10] — UWAGA
- miary: `{"open:home": 3.2}`
- trasa home [F1] OK:  → Odtwórz (3.2 s) fokus 3.2 s, stabilnie 3.2 s
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.5 s) przewinięcie/zmiana układu
- długie OK w menu [F10] UWAGA: Główna → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (5.6 s) długie OK zadziałało jak zwykłe OK
- po długim OK [F10] OK:  → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (0.0 s)

### 16. Szukaj: „bat” klawiaturą ekranową; fokus na klawiszu, wyniki bez kradzieży fokusu [F1 F6] — OK
- miary: `{"open:search": 2.1}`
- trasa search [F1] OK:  → a (2.1 s) fokus 2.1 s, stabilnie 2.1 s
- klawiatura „bat” [F6] OK:  → t (33.5 s) wpisane: bat
- wyniki po 3 s [F6] OK: t → t (3.0 s) wyniki: tak

### 17. Szukaj: z klawiatury do wyników (liczba ►), ◄ wraca na ten sam klawisz [F2 F3, gfxinfo] — UWAGA
- miary: `{"open:search": 2.2, "► do wyników": 5, "gfx:szukaj": {"frames": 34, "janky%": 8.82, "p95": 19, "slowUI": 0, "slowBmp": 0, "50th": 7, "90th": 14, "99th": 21}}`
- trasa search [F1] OK:  → a (2.2 s) fokus 2.2 s, stabilnie 2.2 s
- klawiatura „bat” [F6] OK:  → t (33.6 s) wpisane: bat
- ►×5 do wyników [F2] OK:  → Bat | Film · Animacja · 2018 (0.0 s) 5 naciśnięć
- ◄ [F2] OK: Bat | Film · Animacja · 2018 → x (3.3 s)
- ◄ z wyników [F3] OK: x → x (0.0 s)
- gfx szukaj [klatki] UWAGA:  →  (0.0 s) Janky 8.82%, p95 19 ms, klatek 34

### 18. Szukaj: wynik OK → tytuł → Wstecz: fokus na wyniku [F5] — OK
- miary: `{"open:search": 2.3}`
- trasa search [F1] OK:  → a (2.3 s) fokus 2.3 s, stabilnie 2.3 s
- klawiatura „bat” [F6] OK:  → t (33.3 s) wpisane: bat
- ►×5 [-] OK:  → Bat | Film · Animacja · 2018 (0.0 s) → wynik
- OK na wyniku [F1] OK: Bat | Film · Animacja · 2018 → Odtwórz (10.2 s) pierwszy fokus 2.8 s
- Wstecz [F5] OK: Odtwórz → Bat | Film · Animacja · 2018 (7.6 s) pierwszy fokus 2.1 s

### 19. Szukaj: polecane — długie OK → menu → Wstecz [F8 F5] — BŁĄD
- miary: `{"open:search": 2.1}`
- trasa search [F1] OK:  → a (2.1 s) fokus 2.1 s, stabilnie 2.1 s
- ►×6 [-] OK:  → Odkrywaj: gatunek, rok, katalog (0.0 s) → polecany plakat
- długie OK [F8] UWAGA: Odkrywaj: gatunek, rok, katalog → Seriale (5.6 s) zmiana ekranu bez logu „długie OK” (element spoza Niuni?)
- Wstecz [F5] BŁĄD: Seriale → a (7.7 s) fokus „a” zamiast „Odkrywaj: gatunek, rok, katalog”

### 20. Szukaj: kasowanie liter do pustego pola, fokus nie spada w niewidoczne miejsce [F6 F7] — UWAGA
- miary: `{"open:search": 2.2}`
- trasa search [F1] OK:  → a (2.2 s) fokus 2.2 s, stabilnie 2.2 s
- klawiatura „bat” [F6] OK:  → t (32.6 s) wpisane: bat

### 21. Biblioteka: chipy ▼ do treści, ▲ na wybrany chip [F3] — BŁĄD
- miary: `{"open:library": 2.5}`
- trasa library [F1] OK:  → Wszystko (2.5 s) fokus 2.5 s, stabilnie 2.5 s
- ► [F2] OK: Wszystko → Filmy (120) (3.3 s)
- ▼ [F2] OK: Filmy (120) → Sci-fi · ★ 6.4 (3.2 s)
- ▲ [F2] OK: Sci-fi · ★ 6.4 → Seriale (42) (3.2 s)
- ▲ na chip [F3] BŁĄD: Filmy (120) → Seriale (42) (0.0 s) „Seriale (42)” zamiast „Filmy (120)”

### 22. Biblioteka: Historia — OK → tytuł → Wstecz; długie OK na pozycji [F5 F8] — BŁĄD
- miary: `{"open:library": 2.2}`
- trasa library [F1] OK:  → Wszystko (2.2 s) fokus 2.2 s, stabilnie 2.2 s
- ►×3 [-] OK:  → Historia (0.0 s) → Historia
- ▼ [F2] OK: Historia → STAR TREK: NASTĘPNE POKOLENIE | S7 E4 · dziś 06:00 (3.2 s)
- OK na pozycji historii [F1] OK: STAR TREK: NASTĘPNE POKOLENIE | S7 E4 · dziś 06:00 → Wznów, zostało S1, E1, 84 min | 84m (9.0 s) pierwszy fokus 3.7 s
- Wstecz [F5] BŁĄD: Wznów, zostało S1, E1, 84 min | 84m → Wszystko (7.6 s) fokus „Wszystko” zamiast „STAR TREK: NASTĘPNE POKOLENIE | S7 E4 · dziś 06:00”
- długie OK [F8] BŁĄD: Wszystko → Wszystko (5.7 s) brak akcji długiego OK
- Wstecz [F5] BŁĄD: Wszystko → Bat | Film · Animacja · 2018 (7.9 s) fokus „Bat | Film · Animacja · 2018” zamiast „STAR TREK: NASTĘPNE POKOLENIE | S7 E4 · dziś 06:00”

### 23. Biblioteka: pusta lista — fokus na przycisku stanu pustego [F1 F10] — POMINIĘTA
- miary: `{"open:library": 2.4}`
- trasa library [F1] OK:  → Wszystko (2.4 s) fokus 2.4 s, stabilnie 2.4 s

### 24. Film: fokus na „Odtwórz” w budżecie; ◄► po przyciskach bez zawijania [F1 F4, czas] — OK
- miary: `{"open:detail/tt15398776/movie": 3.2}`
- trasa detail/tt15398776/movie [F1] OK:  → Wznów, zostało 178 min | 178m (3.2 s) fokus 3.2 s, stabilnie 3.2 s
- ► na krawędzi (po 3) [F4] OK: Zwiastun → Zwiastun (0.0 s)
- ◄ na krawędzi (po 3) [F4] OK: Wznów, zostało 178 min | 178m → Wznów, zostało 178 min | 178m (0.0 s)

### 25. „Więcej” → „Mniej”: fokus zostaje na przycisku po zmianie układu [F6] — OK
- miary: `{"open:detail/tt15398776/movie": 2.1}`
- trasa detail/tt15398776/movie [F1] OK:  → Wznów, zostało 178 min | 178m (2.1 s) fokus 2.1 s, stabilnie 2.1 s
- ▼×5 [-] OK:  → Pokaż cały opis | Więcej (0.0 s) → Więcej
- OK „Więcej” [F6] OK: Pokaż cały opis | Więcej → Zwiń opis | Mniej (0.0 s) fokus „Zwiń opis | Mniej”
- OK „Mniej” [F6] OK: Zwiń opis | Mniej → Pokaż cały opis | Więcej (0.0 s) fokus „Pokaż cały opis | Więcej”

### 26. Serial: ▼ pigułki sezonów, ► sezon, ▼ odcinki, ▲ na wybraną pigułkę [F3 F6] — UWAGA
- miary: `{"open:detail/tt0903747/series": 2.3}`
- trasa detail/tt0903747/series [F1] OK:  → Wznów, zostało 178 min | 178m (2.3 s) fokus 2.3 s, stabilnie 2.3 s

### 27. Tabela sezon × odcinek (Z-8): ◄►▲▼ po komórkach, przewijanie trzyma fokus [F2 F7, gfxinfo] — POMINIĘTA
- miary: `{"open:detail/tt0903747/series": 3.0}`
- trasa detail/tt0903747/series [F1] OK:  → Wznów, zostało S1, E1, 58 min | 58m (3.0 s) fokus 3.0 s, stabilnie 3.0 s

### 28. Odcinek OK → wybór źródła → Wstecz: ten odcinek; długie OK = opcje odcinka [F5 F8] — UWAGA
- miary: `{"open:detail/tt0903747/series": 2.2}`
- trasa detail/tt0903747/series [F1] OK:  → Wznów, zostało S1, E1, 58 min | 58m (2.2 s) fokus 2.2 s, stabilnie 2.2 s
- ▼×1 [-] OK:  → Odcinek 1, Pilot | ODCINEK 1 | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się,  (0.0 s) → Odcinek/Pilot/E1
- OK na odcinku [F1] OK: Odcinek 1, Pilot | ODCINEK 1 | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się,  → Odtwórz (7.6 s) pierwszy fokus 2.2 s
- Wstecz [F5] UWAGA: Odtwórz → Odcinek 1, Pilot | ODCINEK 1 | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się,  (7.5 s) ten sam element, inne położenie [80, 416, 490, 955] vs [80, 388, 490, 927]
- długie OK [F8] OK: Odcinek 1, Pilot | ODCINEK 1 | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się,  → Oznacz jako obejrzany (5.1 s) długie OK
- Wstecz [F5] UWAGA: Oznacz jako obejrzany → Odcinek 1, Pilot | ODCINEK 1 | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się,  (7.3 s) ten sam element, inne położenie [80, 416, 490, 955] vs [80, 388, 490, 927]

### 29. Obsada → Osoba → plakat → tytuł → Wstecz ×3: za każdym razem element źródłowy [F5] — UWAGA
- miary: `{"open:detail/tt15398776/movie": 2.3}`
- trasa detail/tt15398776/movie [F1] OK:  → Wznów, zostało S1, E1, 58 min | 58m (2.3 s) fokus 2.3 s, stabilnie 2.3 s

### 30. Zwiastun: OK → od razu „Wczytuję zwiastun…” → Wstecz na kartę [F5 F12, czas] — OK
- miary: `{"open:detail/tt15398776/movie": 2.2}`
- trasa detail/tt15398776/movie [F1] OK:  → Wznów, zostało 178 min | 178m (2.2 s) fokus 2.2 s, stabilnie 2.2 s
- ▼×1 [-] OK:  → Nowy zwiastun (0.0 s) → Zwiastun
- OK zwiastun [F12] OK: Nowy zwiastun → 21:15 | built-1h (3.5 s)
- Wstecz [F5] OK: 21:15 | built-1h → Nowy zwiastun (7.5 s) pierwszy fokus 2.0 s

### 31. Wstecz z głębi strony (▼×6): góra albo wyjście, fokus zawsze widoczny [F7 F12] — OK
- miary: `{"open:detail/tt15398776/movie": 2.2}`
- trasa detail/tt15398776/movie [F1] OK:  → Wznów, zostało 178 min | 178m (2.2 s) fokus 2.2 s, stabilnie 2.2 s
- Wstecz [F12] OK: Oscar: 4 Oscary, 2024 | 4 Oscary | 2024 → Wznów, zostało 178 min | 178m (7.3 s) pierwszy fokus 2.0 s
- po Wstecz [F7] OK:  → Wznów, zostało 178 min | 178m (0.0 s)

### 32. Wybór źródła: pierwszy fokus na pierwszym źródle; filtry ▲▼ wracają na wybrany filtr [F1 F3] — UWAGA
- miary: `{"open:detail/tt15398776/movie": 2.3}`
- trasa detail/tt15398776/movie [F1] OK:  → Wznów, zostało 178 min | 178m (2.3 s) fokus 2.3 s, stabilnie 2.3 s
- OK „Odtwórz” → źródła [F1] OK: Wznów, zostało 178 min | 178m → Przerażające sceny | · | Silne | Nagość | Umiarkowane | Wulgaryzmy | Przemoc | Łagodne | A (14.2 s) pierwszy fokus 2.7 s

### 33. Odtwarzacz: OK nakładka, ▲ do przycisków nad paskiem, ◄►, ▼ na pasek [F3 F4] — BŁĄD
- miary: `{"open:home": 5.1}`
- trasa home [F1] UWAGA:  → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (5.1 s) fokus 5.1 s, stabilnie 5.1 s (budżet 0.6/3.0 s)
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.4 s) przewinięcie/zmiana układu
- OK „Oglądaj dalej” → odtwarzacz [F1] UWAGA: Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min → 21:17 | built-1h (15.9 s) pierwszy fokus 5.6 s
- nakładka [F1] OK:  → [bez nazwy 1594x48 @148,940] (0.0 s) fokus „[bez nazwy 1594x48 @148,940]”
- ▲ [F2] OK: [bez nazwy 1594x48 @148,940] → Dźwięk (3.9 s)
- ◄ [F2] BŁĄD: Dźwięk → None (4.4 s) fokus zgubiony
- ► [F2] OK: None → [bez nazwy 1594x48 @148,940] (3.6 s)
- ▼ [F2] OK: [bez nazwy 1594x48 @148,940] → Episode 1 | Teraz | E1 · Episode 1 (3.3 s)
- Wstecz [F12] OK: Episode 1 | Teraz | E1 · Episode 1 → [bez nazwy 1594x48 @148,940] (10.3 s) pierwszy fokus 2.1 s
- Wstecz [F12] OK: [bez nazwy 1594x48 @148,940] → 21:18 | Syncing subtitles to audio… | Listened 0s ahead | Analysed 0s audio · ~0s speech a (7.4 s) pierwszy fokus 2.0 s

### 34. Przewijanie ◄► pojedyncze i przytrzymane; po schowaniu nakładki klawisz nie gubi fokusu [F9 F6] — UWAGA
- miary: `{"open:home": 4.1}`
- trasa home [F1] UWAGA:  → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (4.1 s) fokus 4.1 s, stabilnie 4.1 s (budżet 0.6/3.0 s)
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min (3.4 s) przewinięcie/zmiana układu
- OK → odtwarzacz [F1] OK: Breaking Bad, S1, E1 · 58m, Zostało 58 min | Zostało 58 min → Przemoc | · | Silne | Alkohol/Narkotyki | Przerażające sceny | Nagość | Umiarkowane | Wulg (15.7 s) pierwszy fokus 3.6 s
- klawisz po schowaniu nakładki [F6] OK:  → [bez nazwy 1577x48 @154,940] (0.0 s) fokus „[bez nazwy 1577x48 @154,940]”
- Wstecz [F12] OK: [bez nazwy 1577x48 @154,940] → Take a couple of deep breaths
for me. | 21:19 | built-1h (13.0 s) pierwszy fokus 2.5 s
- Wstecz [F12] OK: Take a couple of deep breaths
for me. | 21:19 | built-1h → Wznów, zostało S1, E1, 40 min | 40m (7.7 s) pierwszy fokus 2.1 s

### 35. „Zmień źródło” → lista → Wstecz na ten przycisk [F5] — UWAGA
- miary: `{"open:home": 3.6}`
- trasa home [F1] OK:  → Breaking Bad, S1, E1 · 40m, Zostało 40 min | Zostało 40 min (3.6 s) fokus 3.6 s, stabilnie 3.6 s
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 40m, Zostało 40 min | Zostało 40 min (3.3 s) przewinięcie/zmiana układu
- OK → odtwarzacz [F1] OK: Breaking Bad, S1, E1 · 40m, Zostało 40 min | Zostało 40 min → Przemoc | · | Silne | Alkohol/Narkotyki | Przerażające sceny | Nagość | Umiarkowane | Wulg (16.4 s) pierwszy fokus 3.5 s
- ►×3 [-] OK:  → Więcej (0.0 s) → Więcej
- Wstecz [F12] OK: Źródła → 21:21 | Breaking Bad | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się, że ma ni (13.8 s) pierwszy fokus 2.9 s
- Wstecz [F12] OK: 21:21 | Breaking Bad | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się, że ma ni → [bez nazwy 1594x48 @148,940] (8.2 s) pierwszy fokus 2.5 s

### 36. Wstecz z odtwarzacza → strona/Główna, fokus na źródle; czas pierwszej klatki [F5 F12, czas] — UWAGA
- miary: `{"open:home": 4.1, "pierwsza klatka (log)": "brak wpisu"}`
- trasa home [F1] UWAGA:  → Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (4.1 s) fokus 4.1 s, stabilnie 4.1 s (budżet 0.6/3.0 s)
- ▼ [F2] OK: Odtwórz → Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (3.3 s) przewinięcie/zmiana układu
- OK → odtwarzacz [F1] OK: Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → Przemoc | · | Silne | Alkohol/Narkotyki | Przerażające sceny | Nagość | Umiarkowane | Wulg (13.1 s) pierwszy fokus 3.3 s
- Wstecz [F12] OK: Przemoc | · | Silne | Alkohol/Narkotyki | Przerażające sceny | Nagość | Umiarkowane | Wulg → Wznów, zostało S1, E1, 57 min | 57m (7.7 s) pierwszy fokus 2.1 s
- Wstecz do Głównej [F5] OK: Wznów, zostało S1, E1, 57 min | 57m → Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (7.7 s) pierwszy fokus 2.2 s

### 37. Live TV: wejście — fokus na ostatnim lub pierwszym kanale; czas listy z dysku [F1, czas] — OK
- miary: `{"open:reshaped_live_tv": 3.5}`
- trasa reshaped_live_tv [F1] OK:  → Wszystkie (3.5 s) fokus 3.5 s, stabilnie 3.5 s
- pierwszy fokus Live TV [F1] OK:  → Wszystkie (0.0 s) „Wszystkie”

### 38. Chip „Kraj i język” → panel → Wstecz: fokus na chipie (bez zmiany wyboru) [F5 F6] — POMINIĘTA
- miary: `{"open:reshaped_live_tv": 3.0}`
- trasa reshaped_live_tv [F1] OK:  → Wszystkie (3.0 s) fokus 3.0 s, stabilnie 3.0 s

### 39. Lista kanałów: przytrzymana ▼ 5 s i ▲ [F9, gfxinfo] — BŁĄD
- miary: `{"open:reshaped_live_tv": 2.7, "gfx:kanaly": {"frames": 199, "janky%": 70.85, "p95": 89, "slowUI": 0, "slowBmp": 0, "50th": 28, "90th": 73, "99th": 250}, "klatki>=100ms(log)": 0}`
- trasa reshaped_live_tv [F1] OK:  → Wszystkie (2.7 s) fokus 2.7 s, stabilnie 2.7 s
- ▼×1 [-] OK:  → 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.0 s) → wiersz kanału
- ▼ 5 s [F7] BŁĄD:  → 74 | Strong8K · LIVE | WEEK 11 | MONDAY NIGHT: BENGALS AT COMMANDERS | Tue 24 Nov 02:10 CE (5.0 s) fokus przy krawędzi/poza ekranem [0, 728, 1920, 824]
- ▲ 5 s [F9] OK: 74 | Strong8K · LIVE | WEEK 11 | MONDAY NIGHT: BENGALS AT COMMANDERS | Tue 24 Nov 02:10 CE → S | Stef+Patryk | Zmień profil (5.0 s) „S | Stef+Patryk | Zmień profil”
- gfx kanaly [klatki] BŁĄD:  →  (0.0 s) Janky 70.85%, p95 89 ms, klatek 199

### 40. Kanał OK → odtwarzanie → Wstecz: ten sam kanał (okno „nie odpowiada”: przyciski i Wstecz) [F5 F1 F12] — UWAGA
- miary: `{"open:reshaped_live_tv": 2.6}`
- trasa reshaped_live_tv [F1] OK:  → Wszystkie (2.6 s) fokus 2.6 s, stabilnie 2.6 s
- ▼×1 [-] OK:  → 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.0 s) → wiersz kanału
- OK kanał [F1] UWAGA: 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → 21:24 | built-1h (11.9 s) pierwszy fokus 5.8 s
- Wstecz z kanału [F5] OK: 21:24 | built-1h → 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (8.5 s) pierwszy fokus 2.8 s

### 41. Długie OK na kanale → „Opcje kanału” → ▼ po opcjach → Wstecz na kanał [F8 F5] — BŁĄD
- miary: `{"open:reshaped_live_tv": 2.4}`
- trasa reshaped_live_tv [F1] OK:  → Wszystkie (2.4 s) fokus 2.4 s, stabilnie 2.4 s
- ▼×1 [-] OK:  → 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.0 s) → wiersz kanału
- długie OK [F8] OK: 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → Dodaj do ulubionych (5.4 s) długie OK
- ▼×8 [F4] UWAGA:  → 5 | Strong8K · ENDED | ELLIOT SLESSOR - ARTEMIJS ZIZINS | Mon 23 Nov 20:30 CET (DE) | 8K E (0.0 s) nie doszedł do krawędzi
- Wstecz [F5] BŁĄD: 5 | Strong8K · ENDED | ELLIOT SLESSOR - ARTEMIJS ZIZINS | Mon 23 Nov 20:30 CET (DE) | 8K E → 5 | Strong8K · ENDED | ELLIOT SLESSOR - ARTEMIJS ZIZINS | Mon 23 Nov 20:30 CET (DE) | 8K E (8.3 s) fokus „5 | Strong8K · ENDED | ELLIOT SLESSOR - ARTEMIJS ZIZINS | Mon 23 Nov 20:30 CET (DE) | 8K E” zamiast „1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV”

### 42. Przewodnik EPG: ►◄ w czasie, ▼▲ trzyma czas; podgląd nie kradnie fokusu [F2 F6, gfxinfo] — BŁĄD
- miary: `{"open:reshaped_live_tv": 2.7, "gfx:epg": {"frames": 223, "janky%": 1.79, "p95": 10, "slowUI": 0, "slowBmp": 0, "50th": 6, "90th": 9, "99th": 27}}`
- trasa reshaped_live_tv [F1] OK:  → Wszystkie (2.7 s) fokus 2.7 s, stabilnie 2.7 s
- ▼×1 [-] OK:  → 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.0 s) → wiersz kanału
- ► [F2] BŁĄD: 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (3.2 s) fokus nie ruszył się w stronę strzałki
- ▼ [F2] OK: 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → 2 | Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUS (3.3 s)
- ▼ trzyma czas [F2] OK: 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → 2 | Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUS (0.0 s) x 0 → 0
- podgląd po 2,5 s [F6] OK: 2 | Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUS → 2 | Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUS (2.5 s)
- ▲ [F2] OK: 2 | Strong8K · ENDED | BEN WOOLLASTON - MARK JOYCE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUS → 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (3.4 s)
- ◄ [F2] OK: 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (3.6 s)
- gfx epg [klatki] OK:  →  (0.0 s) Janky 1.79%, p95 10 ms, klatek 223

### 43. „Szukaj w telewizji”: fokus w polu, wynik OK → Wstecz [F1 F5] — BŁĄD
- miary: `{"open:fork_tv_search": 2.9}`
- trasa fork_tv_search [F1] BŁĄD:  → Szukaj kanału lub programu (2.9 s) fokus w menu („Szukaj kanału lub programu”), nie w treści
- fokus w polu [F1] OK:  → Szukaj kanału lub programu (0.0 s) „Szukaj kanału lub programu”
- klawiatura „tvp” [F6] UWAGA:  → Szukaj kanału lub programu (6.4 s) BRAK fokusu albo klawisza t

### 44. Ustawienia: ▼ kategorie, ► treść, ◄ ta sama kategoria; Wygląd → „Szkło” niedostępne [F3 F10] — UWAGA
- miary: `{"open:settings": 2.8}`
- trasa settings [F1] OK:  → Oglądanie (2.8 s) fokus 2.8 s, stabilnie 2.8 s
- ▼ [F2] OK: Oglądanie → Napisy i dźwięk (3.7 s)
- ► [F2] OK: Napisy i dźwięk → Rozmiar napisów | — (3.4 s)
- ◄ [F2] OK: Rozmiar napisów | — → Napisy i dźwięk (3.2 s)
- ◄ na kategorię [F3] OK: Napisy i dźwięk → Napisy i dźwięk (0.0 s)
- ▼×2 [-] OK:  → Wygląd (0.0 s) → Wygląd
- ► [F2] OK: Wygląd → Skórka (3.1 s)

### 45. Przełącznik OK ×2: stan wraca, fokus zostaje [F6] — OK
- miary: `{"open:playback_settings": 2.1}`
- trasa playback_settings [F1] OK:  → Odtwarzacz | Domyślny odtwarzacz, silnik wbudowany i awaryjne przełączanie. | Zamknięte (2.1 s) fokus 2.1 s, stabilnie 2.1 s
- ▼ [F2] OK: Odtwarzacz | Domyślny odtwarzacz, silnik wbudowany i awaryjne przełączanie. | Zamknięte → Wybór strumienia | Sposób wyboru strumienia po naciśnięciu odtwarzania. | Zamknięte (3.2 s)
- OK×2 [F6] OK: Wybór strumienia | Sposób wyboru strumienia po naciśnięciu odtwarzania. | Zamknięte → Wybór strumienia | Sposób wyboru strumienia po naciśnięciu odtwarzania. | Zamknięte (0.0 s) stan/fokus wrócił

### 46. Napisy → wygląd: Atkinson Hyperlegible domyślna, na liście Tiresias; lista → Wstecz bez zmiany [F1 F5] — UWAGA
- miary: `{"open:playback_settings": 2.1}`
- trasa playback_settings [F1] OK:  → Odtwarzacz | Domyślny odtwarzacz, silnik wbudowany i awaryjne przełączanie. | Zamknięte (2.1 s) fokus 2.1 s, stabilnie 2.1 s

### 47. YouTube: czas do fokusu w treści, OK → odtwarzacz → Wstecz (kod obcy: pomiar) [F1 F5, czas] — BŁĄD
- miary: `{"open:youtube": 2.3}`
- trasa youtube [F1] BŁĄD:  → Główna (2.3 s) fokus w menu („Główna”), nie w treści

### 48. Profile: ◄► po profilach, (PIN: wejście i Wstecz bez wpisywania) [F1 F5] — OK
- miary: `{"open:profiles": 2.0}`
- trasa profiles [F1] OK:  → Na żywo (2.0 s) fokus 2.0 s, stabilnie 2.0 s
- ► na krawędzi (po 3) [F4] OK: 13:59 → 13:59 (0.0 s)
- ◄ na krawędzi (po 3) [F4] OK: Główna → Główna (0.0 s)
- PIN [-] OK:  → Główna (0.0 s) brak profilu z PIN-em — bez OK (nie przełączamy profilu)

### 49. Odkrywaj / Zobacz wszystko: siatka ▼►, przytrzymana ▼, OK → tytuł → Wstecz na ten sam kafelek [F2 F5 F7 F9, gfxinfo] — BŁĄD
- miary: `{"open:discover": 2.3, "gfx:siatka": {"frames": 277, "janky%": 58.12, "p95": 105, "slowUI": 0, "slowBmp": 0, "50th": 25, "90th": 65, "99th": 250}}`
- trasa discover [F1] OK:  → Seriale (2.3 s) fokus 2.3 s, stabilnie 2.3 s
- ▼×2 [-] OK:  → Dramat (0.0 s) → kafelek siatki
- ► [F2] OK: Dramat → Kryminał · ★ 8.3 (3.3 s)
- ▼ [F2] OK: Kryminał · ★ 8.3 → Kryminał (3.7 s)
- ▼ 2 s [F7] OK: Kryminał → Kryminał · ★ 7.1 (2.0 s)
- ► [F2] OK: Kryminał · ★ 7.1 → Dramat · ★ 6.6 (4.0 s)
- OK kafelek [F1] UWAGA: Dramat · ★ 6.6 → Odtwórz S1, O1 (9.8 s) pierwszy fokus 3.8 s
- Wstecz [F5] BŁĄD: Odtwórz S1, O1 → Seriale (7.7 s) fokus „Seriale” zamiast „Dramat · ★ 6.6”
- gfx siatka [klatki] BŁĄD:  →  (0.0 s) Janky 58.12%, p95 105 ms, klatek 277

### 50. Kalendarz, Dodatki, Kolejność katalogów, Kolekcje: pierwszy fokus, Wstecz z pozycji na źródło [F1 F5] — UWAGA
- miary: `{"open:calendar": 2.3, "open:addon_manager": 2.0, "open:catalog_order": 2.1, "open:collections": 2.4}`
- trasa calendar [F1] OK:  → WT. | 24 | Blade Runner 2099 | wt. 24 lis · S1 E1 · Prime Video | SERIES PREMIERE | Today (2.3 s) fokus 2.3 s, stabilnie 2.3 s
- ▼ [F2] OK: WT. | 24 | Blade Runner 2099 | wt. 24 lis · S1 E1 · Prime Video | SERIES PREMIERE | Today → ŚR. | 25 | Rywale | śr. 25 lis · S2 E11 · Disney+ | Tomorrow (3.3 s)
- calendar: OK [F1] OK: ŚR. | 25 | Rywale | śr. 25 lis · S2 E11 · Disney+ | Tomorrow → Open the show (7.2 s) pierwszy fokus 2.0 s
- calendar: Wstecz [F5] OK: Open the show → ŚR. | 25 | Rywale | śr. 25 lis · S2 E11 · Disney+ | Tomorrow (7.3 s) pierwszy fokus 2.0 s
- trasa addon_manager [F1] OK:  → https://example.com (2.0 s) fokus 2.0 s, stabilnie 2.0 s
- ▼ [F2] OK: https://example.com → Zarządzaj z telefonu | Zeskanuj kod QR, aby zarządzać dodatkami, katalogami i kolekcjami z (3.1 s)
- addon_manager: OK [F1] OK: Zarządzaj z telefonu | Zeskanuj kod QR, aby zarządzać dodatkami, katalogami i kolekcjami z → Zamknij (7.2 s) pierwszy fokus 2.0 s
- addon_manager: Wstecz [F5] UWAGA: Zamknij → Zarządzaj z telefonu | Zeskanuj kod QR, aby zarządzać dodatkami, katalogami i kolekcjami z (7.1 s) ten sam element, inne położenie [72, 389, 1848, 539] vs [72, 390, 1848, 540]
- trasa catalog_order [F1] OK:  → [bez nazwy 120x96 @1704,189] (2.1 s) fokus 2.1 s, stabilnie 2.1 s
- ▼ [F2] OK: [bez nazwy 120x96 @1704,189] → Przypięte (3.1 s)
- catalog_order: OK [F1] OK: Przypięte → Przypięte (7.7 s) pierwszy fokus 2.0 s
- trasa collections [F1] OK:  → Nowa kolekcja (2.4 s) fokus 2.4 s, stabilnie 2.4 s
- ▼ [F2] OK: Nowa kolekcja → Edytuj (3.3 s)
- collections: OK [F1] OK: Edytuj → Spotlight (7.1 s) pierwszy fokus 2.0 s
- collections: Wstecz [F5] OK: Spotlight → Edytuj (7.3 s) pierwszy fokus 2.1 s

