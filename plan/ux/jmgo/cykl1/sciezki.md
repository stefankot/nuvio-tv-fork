# Ścieżki UX JMGO — cykl 1 (24.11 17:18, 192.168.31.252:5555, 11.9 min)

OK 11, UWAGA 14, BŁĄD 21, POMINIĘTA 4

| # | Ścieżka | Wynik | s | Uwagi |
|---|---|---|---|---|
| 1 | Zimny start: fokus na „Odtwórz” w banerze; po dociągnięciu rzędów fokus nie skacze | **OK** | 13 |  |
| 2 | Ciepły start (HOME → ikona): fokus tam, gdzie był, albo na banerze | **BŁĄD** | 7 | BŁĄD: F1 HOME → ikona: TotalTime 505 ms; fokus: inaczej: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min / Zostało 57 min |
| 3 | Baner: ◄► po przyciskach, skrajne bez zawijania, ▲ nie gubi fokusu | **OK** | 12 |  |
| 4 | Baner ▼ → pierwszy rząd: plakat w całości widoczny, hero bez skoku | **BŁĄD** | 4 | BŁĄD: klatki gfx baner_rzad: Janky 44.44%, p95 53 ms, klatek 54 |
| 5 | Rząd ►×6 → ▼ → ▲: ten sam plakat | **BŁĄD** | 10 | BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki; BŁĄD: F2 ►: fokus nie ruszył się w stronę strzałki; BŁĄD: F2 ►: fokus nie ruszył się w stronę strzałki; BŁĄD: F2 ▲: fokus nie ruszył się w stronę strzałki; BŁĄD: F3 powrót ▲: „n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52  |
| 6 | Rząd 1, 4. plakat ▼ → rząd 2 (konwencja F3/F2) | **BŁĄD** | 9 | BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki; UWAGA: F2 kolumna ▼: Δx=-340 px; BŁĄD: F2 ▲: fokus nie ruszył się w stronę strzałki; BŁĄD: F3 ▲ wraca: „n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min / Zostało 57 min” zamiast „n:Szczegóły Szczegóły” |
| 7 | Przytrzymana ▼ 3 s przez rzędy i ▲ do banera | **BŁĄD** | 13 | BŁĄD: klatki gfx przytrzymanie_v: Janky 100.0%, p95 17 ms, klatek 1 |
| 8 | Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku | **BŁĄD** | 21 | BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki; BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki; BŁĄD: klatki gfx przytrzymanie_h: Janky 21.67%, p95 21 ms, klatek 60 |
| 9 | Plakat OK → tytuł → Wstecz: ten sam plakat, to samo przewinięcie | **BŁĄD** | 6 | BŁĄD: F2 ▼: fokus zgubiony; BŁĄD: F2 ►: fokus zgubiony; BŁĄD: F2 ►: fokus zgubiony |
| 10 | Długie OK na plakacie → menu → Wstecz: fokus na plakacie, puszczenie nic nie klika | **BŁĄD** | 11 | BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki; BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki; BŁĄD: F8 długie OK: brak akcji długiego OK; BŁĄD: F5 Wstecz: fokus „n:Główna Główna” zamiast „n:Odtwórz Odtwórz”; BŁĄD: F5 Wstecz: fokus „n:Główna Główna” zamiast „n:Odtwórz Odtwórz” |
| 11 | Pierwszy plakat: ◄ bez zawijania; menu otwiera ▲ z góry ekranu, fokus na „Główna” | **BŁĄD** | 7 | BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki; BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki |
| 12 | Menu ▼ do końca i ▲ bez zawijania, ► → element, z którego wyszło | **BŁĄD** | 8 | BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki; BŁĄD: F11 ► z menu: „n:Odtwórz Odtwórz” zamiast „n:Główna Główna” |
| 13 | Menu → Szukaj → Biblioteka → Live TV → Główna: pierwszy fokus w treści, Główna przywraca fokus | **BŁĄD** | 45 | UWAGA: menu: brak pozycji „Szukaj”; UWAGA: menu: brak pozycji „Biblioteka”; UWAGA: menu: brak pozycji „Live TV”; BŁĄD: F1 menu → Główna: brak fokusu po OK; UWAGA: F5 Główna: powrót: „None” zamiast „n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta |
| 14 | Wstecz z głębi rzędu: baner/menu, drugi Wstecz nie zamyka bez pytania | **BŁĄD** | 8 | BŁĄD: F2 ▼: fokus zgubiony; BŁĄD: F2 ▼: fokus zgubiony |
| 15 | Długie OK na pozycji menu: nic się nie psuje | **BŁĄD** | 8 | BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki |
| 16 | Szukaj: „bat” klawiaturą ekranową; fokus na klawiszu, wyniki bez kradzieży fokusu | **UWAGA** | 43 | UWAGA: F6 klawiatura „bat”: nie doszedłem do b |
| 17 | Szukaj: z klawiatury do wyników (liczba ►), ◄ wraca na ten sam klawisz | **BŁĄD** | 45 | UWAGA: F6 klawiatura „bat”: nie doszedłem do b; UWAGA: F2 ►×7 do wyników: 7 naciśnięć (> 6 — propozycja skrótu); BŁĄD: F3 ◄ z wyników: „n:Sterowanie głosem: powiedz, co włączyć Sterowanie głosem: powiedz, co włączyć / Powiedz” zamiast „n:f f” |
| 18 | Szukaj: wynik OK → tytuł → Wstecz: fokus na wyniku | **UWAGA** | 46 | UWAGA: F6 klawiatura „bat”: nie doszedłem do b |
| 19 | Szukaj: polecane — długie OK → menu → Wstecz | **UWAGA** | 10 | UWAGA: nie znaleziono: polecany plakat (▼×6); UWAGA: nie znaleziono: polecany plakat (►×12) |
| 20 | Szukaj: kasowanie liter do pustego pola, fokus nie spada w niewidoczne miejsce | **UWAGA** | 47 | UWAGA: F6 klawiatura „bat”: nie doszedłem do b; UWAGA: nie znaleziono: ⌫/Usuń/Kasuj/Backspace/Delete (▼×8); UWAGA: nie znaleziono: ⌫/Usuń/Kasuj/Backspace/Delete (►×8) |
| 21 | Biblioteka: chipy ▼ do treści, ▲ na wybrany chip | **BŁĄD** | 3 | BŁĄD: F3 ▲ na chip: „n:Seriale (40) Seriale (40)” zamiast „n:Filmy (121) Filmy (121)” |
| 22 | Biblioteka: Historia — OK → tytuł → Wstecz; długie OK na pozycji | **POMINIĘTA** | 5 | UWAGA: nie znaleziono: Historia (►×8); POMINIĘTA: brak zakładki „Historia” |
| 23 | Biblioteka: pusta lista — fokus na przycisku stanu pustego | **POMINIĘTA** | 16 | POMINIĘTA: brak pustej listy w Bibliotece |
| 24 | Film: fokus na „Odtwórz” w budżecie; ◄► po przyciskach bez zawijania | **OK** | 6 |  |
| 25 | „Więcej” → „Mniej”: fokus zostaje na przycisku po zmianie układu | **OK** | 8 |  |
| 26 | Serial: ▼ pigułki sezonów, ► sezon, ▼ odcinki, ▲ na wybraną pigułkę | **UWAGA** | 6 | UWAGA: nie znaleziono: Sezon 1/Sezon (▼×6) |
| 27 | Tabela sezon × odcinek (Z-8): ◄►▲▼ po komórkach, przewijanie trzyma fokus | **POMINIĘTA** | 7 | UWAGA: nie znaleziono: ★ (►×6); POMINIĘTA: brak ★ (tabela ocen) w hero |
| 28 | Odcinek OK → wybór źródła → Wstecz: ten odcinek; długie OK = opcje odcinka | **BŁĄD** | 8 | UWAGA: F5 Wstecz: ten sam element, inne położenie [80, 416, 490, 955] vs [80, 515, 490, 1054]; BŁĄD: F8 długie OK: brak akcji długiego OK; BŁĄD: F5 Wstecz: fokus „n:Wznów, zostało S1, E1, 57 min Wznów, zostało S1, E1, 57 min / 57m” zamiast „n:Odcinek 1, Pilot Odcinek 1, Pilot / ODCINEK 1 / Pilot / N |
| 29 | Obsada → Osoba → plakat → tytuł → Wstecz ×3: za każdym razem element źródłowy | **UWAGA** | 8 | UWAGA: nie znaleziono osoby w obsadzie |
| 30 | Zwiastun: OK → od razu „Wczytuję zwiastun…” → Wstecz na kartę | **UWAGA** | 10 | UWAGA: F5 Wstecz: ten sam element, inne położenie [80, 388, 490, 717] vs [80, 437, 490, 766] |
| 31 | Wstecz z głębi strony (▼×6): góra albo wyjście, fokus zawsze widoczny | **OK** | 6 |  |
| 32 | Wybór źródła: pierwszy fokus na pierwszym źródle; filtry ▲▼ wracają na wybrany filtr | **UWAGA** | 5 | UWAGA: zamiast listy źródeł: odtwarzanie albo brak listy |
| 33 | Odtwarzacz: OK nakładka, ▲ do przycisków nad paskiem, ◄►, ▼ na pasek | **BŁĄD** | 20 | BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki |
| 34 | Przewijanie ◄► pojedyncze i przytrzymane; po schowaniu nakładki klawisz nie gubi fokusu | **OK** | 21 |  |
| 35 | „Zmień źródło” → lista → Wstecz na ten przycisk | **UWAGA** | 24 | UWAGA: nie znaleziono: Więcej (▲×3); UWAGA: nie znaleziono: Więcej (►×6); UWAGA: nie znaleziono: Zmień źródło (▼×6); UWAGA: brak „Zmień źródło” |
| 36 | Wstecz z odtwarzacza → strona/Główna, fokus na źródle; czas pierwszej klatki | **BŁĄD** | 16 | BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki |
| 37 | Live TV: wejście — fokus na ostatnim lub pierwszym kanale; czas listy z dysku | **OK** | 3 |  |
| 38 | Chip „Kraj i język” → panel → Wstecz: fokus na chipie (bez zmiany wyboru) | **POMINIĘTA** | 11 | UWAGA: nie znaleziono: Kraj/Język/kraje (►×14); POMINIĘTA: brak chipa „Kraj i język” |
| 39 | Lista kanałów: przytrzymana ▼ 5 s i ▲ | **OK** | 16 |  |
| 40 | Kanał OK → odtwarzanie → Wstecz: ten sam kanał (okno „nie odpowiada”: przyciski i Wstecz) | **BŁĄD** | 14 | BŁĄD: F5 Wstecz z kanału: fokus „00:16 / Strong8K · ENDED / BEN MERTENS - JIMMY WHITE / Mon 23 Nov 15:30 CET (DE) / 8K EXCL” zamiast „n:Strong8K · ENDED / BEN MERTENS - JIMMY WHITE / Mon 23 Nov 15 1 / Strong8K · ENDED / BEN MERTENS - JIMMY WHITE / Mon 23 Nov 15:30 CET (DE) / 8K EXCLUSIV” |
| 41 | Długie OK na kanale → „Opcje kanału” → ▼ po opcjach → Wstecz na kanał | **BŁĄD** | 6 | BŁĄD: F8 długie OK: brak akcji długiego OK; BŁĄD: F8 panel opcji: brak „Opcje kanału”; BŁĄD: F5 Wstecz: fokus „n:Wszystkie Wszystkie” zamiast „n:Strong8K · ENDED / BEN MERTENS - JIMMY WHITE / Mon 23 Nov 15 1 / Strong8K · ENDED / BEN MERTENS - JIMMY WHITE / Mon 23 Nov 15:30 CET (DE) / 8K EXCLUSIV” |
| 42 | Przewodnik EPG: ►◄ w czasie, ▼▲ trzyma czas; podgląd nie kradnie fokusu | **BŁĄD** | 14 | BŁĄD: F1 trasa reshaped_live_tv: brak fokusu po otwarciu; BŁĄD: F2 ▼: fokus nie ruszył się w stronę strzałki; BŁĄD: F2 ▲: fokus nie ruszył się w stronę strzałki; UWAGA: klatki gfx epg: Janky 10.53%, p95 22 ms, klatek 19 |
| 43 | „Szukaj w telewizji”: fokus w polu, wynik OK → Wstecz | **UWAGA** | 48 | UWAGA: F6 klawiatura „tvp”: nie doszedłem do v; UWAGA: nie znaleziono: wynik TVP (▼×8) |
| 44 | Ustawienia: ▼ kategorie, ► treść, ◄ ta sama kategoria; Wygląd → „Szkło” niedostępne | **UWAGA** | 10 | UWAGA: nie znaleziono: Szkło (▼×12) |
| 45 | Przełącznik OK ×2: stan wraca, fokus zostaje | **OK** | 4 |  |
| 46 | Napisy → wygląd: Atkinson Hyperlegible domyślna, na liście Tiresias; lista → Wstecz bez zmiany | **UWAGA** | 13 | UWAGA: nie znaleziono: Czcionka (▼×25); UWAGA: brak pozycji „Czcionka” |
| 47 | YouTube: czas do fokusu w treści, OK → odtwarzacz → Wstecz (kod obcy: pomiar) | **UWAGA** | 3 | UWAGA: nie znaleziono: film (▼×4) |
| 48 | Profile: ◄► po profilach, (PIN: wejście i Wstecz bez wpisywania) | **OK** | 7 |  |
| 49 | Odkrywaj / Zobacz wszystko: siatka ▼►, przytrzymana ▼, OK → tytuł → Wstecz na ten sam kafelek | **UWAGA** | 4 | UWAGA: nie znaleziono: kafelek siatki (▼×5) |
| 50 | Kalendarz, Dodatki, Kolejność katalogów, Kolekcje: pierwszy fokus, Wstecz z pozycji na źródło | **OK** | 15 |  |

## Kroki

### 1. Zimny start: fokus na „Odtwórz” w banerze; po dociągnięciu rzędów fokus nie skacze [F1 F6, czas] — OK
- miary: `{"zimny TotalTime ms": 1471, "zimny fokus s (dump)": 4.8}`
- zimny start [F1] OK:  → n:Odtwórz Odtwórz (4.8 s) TotalTime 1471 ms; fokus po 4.8 s
- po 6 s [F6] OK: n:Odtwórz Odtwórz → n:Odtwórz Odtwórz (6.0 s)

### 2. Ciepły start (HOME → ikona): fokus tam, gdzie był, albo na banerze [F1] — BŁĄD
- miary: `{"open:home": 0.2, "ciepły TotalTime ms": 505, "ciepły: fokus": "inaczej: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min"}`
- trasa home [F1] OK:  → n:Odtwórz Odtwórz (0.2 s) fokus 0.2 s, stabilnie 0.2 s
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (0.7 s) przewinięcie/zmiana układu
- HOME → ikona [F1] BŁĄD: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (1.1 s) TotalTime 505 ms; fokus: inaczej: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min

### 3. Baner: ◄► po przyciskach, skrajne bez zawijania, ▲ nie gubi fokusu [F4 F10] — OK
- miary: `{"open:home": 0.9}`
- trasa home [F1] OK:  → n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 (0.9 s) fokus 0.4 s, stabilnie 0.9 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ► na krawędzi (po 4) [F4] OK: n:Następny wyróżniony tytuł Następny wyróżniony tytuł → n:Następny wyróżniony tytuł Następny wyróżniony tytuł (0.0 s)
- ◄ na pierwszym [F11] OK: n:Odtwórz Odtwórz → n:Główna Główna (0.0 s) otwiera menu
- ▲ [F10] OK: n:Odtwórz Odtwórz → n:Główna Główna (0.5 s)

### 4. Baner ▼ → pierwszy rząd: plakat w całości widoczny, hero bez skoku [F2 F7, gfxinfo] — BŁĄD
- miary: `{"open:home": 0.1, "gfx:baner_rzad": {"frames": 54, "janky%": 44.44, "p95": 53, "slowUI": 7, "slowBmp": 0, "50th": 11, "90th": 46, "99th": 65}}`
- trasa home [F1] OK:  → n:Odtwórz Odtwórz (0.1 s) fokus 0.1 s, stabilnie 0.1 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Oglądaj dalej | Zostało 57 min (0.8 s) przewinięcie/zmiana układu
- ▼ do rzędu [F7] OK:  → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Oglądaj dalej | Zostało 57 min (0.0 s)
- ► [F2] OK: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Oglądaj dalej | Zostało 57 min → n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min (0.4 s)
- ► [F2] OK: n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min → n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (0.4 s)
- gfx baner_rzad [klatki] BŁĄD:  →  (0.0 s) Janky 44.44%, p95 53 ms, klatek 54

### 5. Rząd ►×6 → ▼ → ▲: ten sam plakat [F3] — BŁĄD
- miary: `{"open:home": 0.9}`
- trasa home [F1] OK:  → n:Apple's Local AI App for Mac Is Coming (Harbor), Apple's Loc Apple's Local AI App for Mac Is Coming (Harbor), Apple's Local AI App for Mac Is Coming (H (0.9 s) fokus 0.1 s, stabilnie 0.9 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ▼ [F2] BŁĄD: n:Główna Główna → n:Główna Główna (0.4 s) fokus nie ruszył się w stronę strzałki
- ► [F2] OK: n:Główna Główna → n:Odtwórz Odtwórz (0.5 s)
- ► [F2] OK: n:Odtwórz Odtwórz → n:Dodaj do biblioteki Dodaj do biblioteki (0.3 s)
- ► [F2] OK: n:Dodaj do biblioteki Dodaj do biblioteki → n:Szczegóły Szczegóły (0.4 s)
- ► [F2] OK: n:Szczegóły Szczegóły → n:Następny wyróżniony tytuł Następny wyróżniony tytuł (0.4 s)
- ► [F2] BŁĄD: n:Następny wyróżniony tytuł Następny wyróżniony tytuł → n:Następny wyróżniony tytuł Następny wyróżniony tytuł (0.4 s) fokus nie ruszył się w stronę strzałki
- ► [F2] BŁĄD: n:Następny wyróżniony tytuł Następny wyróżniony tytuł → n:Następny wyróżniony tytuł Następny wyróżniony tytuł (0.5 s) fokus nie ruszył się w stronę strzałki
- ▼ [F2] OK: n:Następny wyróżniony tytuł Następny wyróżniony tytuł → n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (0.9 s) przewinięcie/zmiana układu
- ▲ [F2] BŁĄD: n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min → n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (0.5 s) fokus nie ruszył się w stronę strzałki
- powrót ▲ [F3] BŁĄD: n:Następny wyróżniony tytuł Następny wyróżniony tytuł → n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min (0.0 s) „n:Rywale, S1, E1 · 52m, Zostało 52 min Rywale, S1, E1 · 52m, Zostało 52 min | Zostało 52 min” zamiast „n:Następny wyróżniony tytuł Następny wyróżniony tytuł”

### 6. Rząd 1, 4. plakat ▼ → rząd 2 (konwencja F3/F2) [F2 F3] — BŁĄD
- miary: `{"open:home": 0.8}`
- trasa home [F1] OK:  → n:Dodaj do biblioteki Dodaj do biblioteki (0.8 s) fokus 0.1 s, stabilnie 0.8 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ▼ [F2] BŁĄD: n:Główna Główna → n:Główna Główna (0.4 s) fokus nie ruszył się w stronę strzałki
- ► [F2] OK: n:Główna Główna → n:Odtwórz Odtwórz (0.4 s)
- ► [F2] OK: n:Odtwórz Odtwórz → n:Dodaj do biblioteki Dodaj do biblioteki (0.4 s)
- ► [F2] OK: n:Dodaj do biblioteki Dodaj do biblioteki → n:Szczegóły Szczegóły (0.5 s)
- ▼ [F2] OK: n:Szczegóły Szczegóły → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (0.7 s) przewinięcie/zmiana układu
- kolumna ▼ [F2] UWAGA: n:Szczegóły Szczegóły → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (0.0 s) Δx=-340 px
- ▲ [F2] BŁĄD: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (0.6 s) fokus nie ruszył się w stronę strzałki
- ▲ wraca [F3] BŁĄD: n:Szczegóły Szczegóły → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (0.0 s) „n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min” zamiast „n:Szczegóły Szczegóły”

### 7. Przytrzymana ▼ 3 s przez rzędy i ▲ do banera [F9, gfxinfo] — BŁĄD
- miary: `{"open:home": 0.8, "gfx:przytrzymanie_v": {"frames": 1, "janky%": 100.0, "p95": 17, "slowUI": 1, "slowBmp": 0, "50th": 17, "90th": 17, "99th": 17}, "klatki>=100ms(log)": 0}`
- trasa home [F1] OK:  → n:Dodaj do biblioteki Dodaj do biblioteki (0.8 s) fokus 0.1 s, stabilnie 0.8 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ▼ 3 s [F9] OK:  → n:Główna Główna (3.0 s)
- ▲ 3 s [F9] OK: n:Główna Główna → n:Główna Główna (3.0 s)
- gfx przytrzymanie_v [klatki] BŁĄD:  →  (0.0 s) Janky 100.0%, p95 17 ms, klatek 1

### 8. Przytrzymana ► 3 s w najdłuższym rzędzie, potem ◄ do początku [F4 F7 F9, gfxinfo] — BŁĄD
- miary: `{"open:home": 0.5, "gfx:przytrzymanie_h": {"frames": 60, "janky%": 21.67, "p95": 21, "slowUI": 0, "slowBmp": 0, "50th": 8, "90th": 20, "99th": 23}, "klatki>=100ms(log)": 0}`
- trasa home [F1] OK:  → n:Dodaj do biblioteki Dodaj do biblioteki (0.5 s) fokus 0.2 s, stabilnie 0.5 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ▼ [F2] BŁĄD: n:Główna Główna → n:Główna Główna (0.5 s) fokus nie ruszył się w stronę strzałki
- ▼ [F2] BŁĄD: n:Główna Główna → n:Główna Główna (0.4 s) fokus nie ruszył się w stronę strzałki
- ► 3 s [F7] OK:  → n:Główna Główna (3.0 s)
- ► na krawędzi (po 4) [F4] OK: n:Następny wyróżniony tytuł Następny wyróżniony tytuł → n:Następny wyróżniony tytuł Następny wyróżniony tytuł (0.0 s)
- ◄ 4 s [F9] OK: n:Główna Główna → n:Następny wyróżniony tytuł Następny wyróżniony tytuł (4.0 s)
- gfx przytrzymanie_h [klatki] BŁĄD:  →  (0.0 s) Janky 21.67%, p95 21 ms, klatek 60

### 9. Plakat OK → tytuł → Wstecz: ten sam plakat, to samo przewinięcie [F5 F12, czas] — BŁĄD
- miary: `{"open:home": 1.5}`
- trasa home [F1] OK:  → n:Odtwórz Odtwórz (1.5 s) fokus 0.1 s, stabilnie 1.5 s
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (0.7 s) przewinięcie/zmiana układu
- ▼ [F2] BŁĄD: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → None (0.5 s) fokus zgubiony
- ► [F2] BŁĄD: None → None (0.4 s) fokus zgubiony
- ► [F2] BŁĄD: None → None (0.4 s) fokus zgubiony
- OK na plakacie [F1] OK: None → n:Odtwórz Odtwórz (1.0 s) pierwszy fokus 0.4 s
- Wstecz [F12] OK: n:Odtwórz Odtwórz → n:Siostra Kopciuszka Animacja · ★ 6.8 (0.8 s) pierwszy fokus 0.3 s

### 10. Długie OK na plakacie → menu → Wstecz: fokus na plakacie, puszczenie nic nie klika [F8 F5] — BŁĄD
- miary: `{"open:home": 0.7}`
- trasa home [F1] OK:  → n:W te święta mam cię na oku Romans · ★ 6.2 (0.7 s) fokus 0.2 s, stabilnie 0.7 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ▼ [F2] BŁĄD: n:Główna Główna → n:Główna Główna (0.4 s) fokus nie ruszył się w stronę strzałki
- ▼ [F2] BŁĄD: n:Główna Główna → n:Główna Główna (0.4 s) fokus nie ruszył się w stronę strzałki
- ► [F2] OK: n:Główna Główna → n:Odtwórz Odtwórz (0.4 s)
- długie OK [F8] BŁĄD: n:Odtwórz Odtwórz → n:Odtwórz Odtwórz (1.2 s) brak akcji długiego OK
- Wstecz [F5] BŁĄD: n:Odtwórz Odtwórz → n:Główna Główna (0.5 s) fokus „n:Główna Główna” zamiast „n:Odtwórz Odtwórz”
- krótkie OK po długim [F1] OK: n:Główna Główna → n:Odtwórz Odtwórz (0.9 s) pierwszy fokus 0.3 s
- Wstecz [F5] BŁĄD: n:Odtwórz Odtwórz → n:Główna Główna (0.5 s) fokus „n:Główna Główna” zamiast „n:Odtwórz Odtwórz”

### 11. Pierwszy plakat: ◄ bez zawijania; menu otwiera ▲ z góry ekranu, fokus na „Główna” [F4 F11] — BŁĄD
- miary: `{"open:home": 0.7}`
- trasa home [F1] OK:  → n:Dodaj do biblioteki Dodaj do biblioteki (0.7 s) fokus 0.3 s, stabilnie 0.7 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ▼ [F2] BŁĄD: n:Główna Główna → n:Główna Główna (0.4 s) fokus nie ruszył się w stronę strzałki
- ▼ [F2] BŁĄD: n:Główna Główna → n:Główna Główna (0.4 s) fokus nie ruszył się w stronę strzałki
- ◄ na krawędzi (po 0) [F4] OK: n:Główna Główna → n:Główna Główna (0.0 s)
- ▲ do menu [F11] OK: n:Główna Główna → n:Główna Główna (0.0 s) fokus „n:Główna Główna”

### 12. Menu ▼ do końca i ▲ bez zawijania, ► → element, z którego wyszło [F4 F11] — BŁĄD
- miary: `{"open:home": 0.7}`
- trasa home [F1] OK:  → n:Dodaj do biblioteki Dodaj do biblioteki (0.7 s) fokus 0.4 s, stabilnie 0.7 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ▼ [F2] BŁĄD: n:Główna Główna → n:Główna Główna (0.4 s) fokus nie ruszył się w stronę strzałki
- ▼ na krawędzi (po 0) [F4] OK: n:Główna Główna → n:Główna Główna (0.0 s)
- ▲ na krawędzi (po 0) [F4] OK: n:Główna Główna → n:Główna Główna (0.0 s)
- ▼×1 [-] OK:  → n:Główna Główna (0.0 s) → Główna
- ► [F2] OK: n:Główna Główna → n:Odtwórz Odtwórz (0.4 s)
- ► z menu [F11] BŁĄD: n:Główna Główna → n:Odtwórz Odtwórz (0.0 s) „n:Odtwórz Odtwórz” zamiast „n:Główna Główna”

### 13. Menu → Szukaj → Biblioteka → Live TV → Główna: pierwszy fokus w treści, Główna przywraca fokus [F1 F5, czas] — BŁĄD
- miary: `{"open:home": 0.1}`
- trasa home [F1] OK:  → n:Odtwórz Odtwórz (0.1 s) fokus 0.1 s, stabilnie 0.1 s
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Oglądaj dalej | Zostało 57 min (0.7 s) przewinięcie/zmiana układu
- ► [F2] OK: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Oglądaj dalej | Zostało 57 min → n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min (0.5 s)
- menu → Główna [F1] BŁĄD: n:Główna Główna → None (10.6 s) brak fokusu po OK
- Główna: powrót [F5] UWAGA: n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min → None (0.0 s) „None” zamiast „n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min”

### 14. Wstecz z głębi rzędu: baner/menu, drugi Wstecz nie zamyka bez pytania [F12] — BŁĄD
- miary: `{"open:home": 2.0}`
- trasa home [F1] OK:  → n:Czarne lustro Sci-fi · ★ 8.7 (2.0 s) fokus 0.8 s, stabilnie 2.0 s
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (0.8 s) przewinięcie/zmiana układu
- ▼ [F2] BŁĄD: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → None (0.6 s) fokus zgubiony
- ▼ [F2] BŁĄD: None → None (0.5 s) fokus zgubiony
- Wstecz 1 [F12] OK: None → n:Odtwórz Odtwórz (0.9 s) pierwszy fokus 0.1 s
- Wstecz 2 [F12] OK: n:Odtwórz Odtwórz → n:Główna Główna (0.5 s) pierwszy fokus 0.1 s

### 15. Długie OK na pozycji menu: nic się nie psuje [F10] — BŁĄD
- miary: `{"open:home": 0.5}`
- trasa home [F1] OK:  → n:Dodaj do biblioteki Dodaj do biblioteki (0.5 s) fokus 0.1 s, stabilnie 0.5 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ▼ [F2] BŁĄD: n:Główna Główna → n:Główna Główna (0.4 s) fokus nie ruszył się w stronę strzałki
- długie OK w menu [F10] OK: n:Główna Główna → n:Główna Główna (1.3 s)
- po długim OK [F10] OK:  → n:Główna Główna (0.0 s)

### 16. Szukaj: „bat” klawiaturą ekranową; fokus na klawiszu, wyniki bez kradzieży fokusu [F1 F6] — UWAGA
- miary: `{"open:search": 0.5}`
- trasa search [F1] OK:  → n:a a (0.5 s) fokus 0.4 s, stabilnie 0.5 s
- klawiatura „bat” [F6] UWAGA:  → n:Stef+Patryk S | Stef+Patryk | Zmień profil (38.8 s) nie doszedłem do b
- wyniki po 3 s [F6] OK: n:Stef+Patryk S | Stef+Patryk | Zmień profil → n:Stef+Patryk S | Stef+Patryk | Zmień profil (3.0 s) wyniki: brak

### 17. Szukaj: z klawiatury do wyników (liczba ►), ◄ wraca na ten sam klawisz [F2 F3, gfxinfo] — BŁĄD
- miary: `{"open:search": 0.7, "► do wyników": 7, "gfx:szukaj": {"frames": 32, "janky%": 0.0, "p95": 11, "slowUI": 0, "slowBmp": 0, "50th": 7, "90th": 9, "99th": 13}}`
- trasa search [F1] OK:  → n:a a (0.7 s) fokus 0.1 s, stabilnie 0.7 s
- klawiatura „bat” [F6] UWAGA:  → n:Stef+Patryk S | Stef+Patryk | Zmień profil (39.3 s) nie doszedłem do b
- ►×7 do wyników [F2] UWAGA:  → n:Odkrywaj: gatunek, rok, katalog Odkrywaj: gatunek, rok, katalog (0.0 s) 7 naciśnięć (> 6 — propozycja skrótu)
- ◄ [F2] OK: n:Odkrywaj: gatunek, rok, katalog Odkrywaj: gatunek, rok, katalog → n:Sterowanie głosem: powiedz, co włączyć Sterowanie głosem: powiedz, co włączyć | Powiedz (0.5 s)
- ◄ z wyników [F3] BŁĄD: n:f f → n:Sterowanie głosem: powiedz, co włączyć Sterowanie głosem: powiedz, co włączyć | Powiedz (0.0 s) „n:Sterowanie głosem: powiedz, co włączyć Sterowanie głosem: powiedz, co włączyć | Powiedz” zamiast „n:f f”
- gfx szukaj [klatki] OK:  →  (0.0 s) Janky 0.0%, p95 11 ms, klatek 32

### 18. Szukaj: wynik OK → tytuł → Wstecz: fokus na wyniku [F5] — UWAGA
- miary: `{"open:search": 0.3}`
- trasa search [F1] OK:  → n:a a (0.3 s) fokus 0.0 s, stabilnie 0.3 s
- klawiatura „bat” [F6] UWAGA:  → n:Stef+Patryk S | Stef+Patryk | Zmień profil (39.1 s) nie doszedłem do b
- ►×7 [-] OK:  → n:Odkrywaj: gatunek, rok, katalog Odkrywaj: gatunek, rok, katalog (0.0 s) → wynik
- OK na wyniku [F1] OK: n:Odkrywaj: gatunek, rok, katalog Odkrywaj: gatunek, rok, katalog → n:Seriale Seriale (1.0 s) pierwszy fokus 0.3 s
- Wstecz [F5] OK: n:Seriale Seriale → n:Odkrywaj: gatunek, rok, katalog Odkrywaj: gatunek, rok, katalog (0.9 s) pierwszy fokus 0.1 s

### 19. Szukaj: polecane — długie OK → menu → Wstecz [F8 F5] — UWAGA
- miary: `{"open:search": 0.1}`
- trasa search [F1] OK:  → n:Odkrywaj: gatunek, rok, katalog Odkrywaj: gatunek, rok, katalog (0.1 s) fokus 0.1 s, stabilnie 0.1 s

### 20. Szukaj: kasowanie liter do pustego pola, fokus nie spada w niewidoczne miejsce [F6 F7] — UWAGA
- miary: `{"open:search": 0.5}`
- trasa search [F1] OK:  → n:Odkrywaj: gatunek, rok, katalog Odkrywaj: gatunek, rok, katalog (0.5 s) fokus 0.5 s, stabilnie 0.5 s
- klawiatura „bat” [F6] UWAGA:  → n:Stef+Patryk S | Stef+Patryk | Zmień profil (38.9 s) nie doszedłem do b

### 21. Biblioteka: chipy ▼ do treści, ▲ na wybrany chip [F3] — BŁĄD
- miary: `{"open:library": 0.9}`
- trasa library [F1] OK:  → n:Wszystko Wszystko (0.9 s) fokus 0.9 s, stabilnie 0.9 s
- ► [F2] OK: n:Wszystko Wszystko → n:Filmy (121) Filmy (121) (0.4 s)
- ▼ [F2] OK: n:Filmy (121) Filmy (121) → n:MTV Europe Music Awards Reality show (0.4 s)
- ▲ [F2] OK: n:MTV Europe Music Awards Reality show → n:Seriale (40) Seriale (40) (0.4 s)
- ▲ na chip [F3] BŁĄD: n:Filmy (121) Filmy (121) → n:Seriale (40) Seriale (40) (0.0 s) „n:Seriale (40) Seriale (40)” zamiast „n:Filmy (121) Filmy (121)”

### 22. Biblioteka: Historia — OK → tytuł → Wstecz; długie OK na pozycji [F5 F8] — POMINIĘTA
- miary: `{"open:library": 0.7}`
- trasa library [F1] OK:  → n:MTV Europe Music Awards Reality show (0.7 s) fokus 0.0 s, stabilnie 0.7 s

### 23. Biblioteka: pusta lista — fokus na przycisku stanu pustego [F1 F10] — POMINIĘTA
- miary: `{"open:library": 1.1}`
- trasa library [F1] OK:  → n:F is for Family Komedia · ★ 8.0 (1.1 s) fokus 0.0 s, stabilnie 1.1 s

### 24. Film: fokus na „Odtwórz” w budżecie; ◄► po przyciskach bez zawijania [F1 F4, czas] — OK
- miary: `{"open:detail/tt15398776/movie": 1.5}`
- trasa detail/tt15398776/movie [F1] OK:  → n:Wznów, zostało 178 min Wznów, zostało 178 min | 178m (1.5 s) fokus 1.4 s, stabilnie 1.5 s
- ► na krawędzi (po 3) [F4] OK: n:Zwiastun Zwiastun → n:Zwiastun Zwiastun (0.0 s)
- ◄ na krawędzi (po 3) [F4] OK: n:Wznów, zostało 178 min Wznów, zostało 178 min | 178m → n:Wznów, zostało 178 min Wznów, zostało 178 min | 178m (0.0 s)

### 25. „Więcej” → „Mniej”: fokus zostaje na przycisku po zmianie układu [F6] — OK
- miary: `{"open:detail/tt15398776/movie": 1.3}`
- trasa detail/tt15398776/movie [F1] OK:  → n:Wznów, zostało 178 min Wznów, zostało 178 min | 178m (1.3 s) fokus 0.9 s, stabilnie 1.3 s
- ▼×5 [-] OK:  → n:Pokaż cały opis Pokaż cały opis | Więcej (0.0 s) → Więcej
- OK „Więcej” [F6] OK: n:Pokaż cały opis Pokaż cały opis | Więcej → n:Zwiń opis Zwiń opis | Mniej (0.0 s) fokus „n:Zwiń opis Zwiń opis | Mniej”
- OK „Mniej” [F6] OK: n:Zwiń opis Zwiń opis | Mniej → n:Pokaż cały opis Pokaż cały opis | Więcej (0.0 s) fokus „n:Pokaż cały opis Pokaż cały opis | Więcej”

### 26. Serial: ▼ pigułki sezonów, ► sezon, ▼ odcinki, ▲ na wybraną pigułkę [F3 F6] — UWAGA
- miary: `{"open:detail/tt0903747/series": 1.1}`
- trasa detail/tt0903747/series [F1] OK:  → n:Wznów, zostało 178 min Wznów, zostało 178 min | 178m (1.1 s) fokus 1.1 s, stabilnie 1.1 s

### 27. Tabela sezon × odcinek (Z-8): ◄►▲▼ po komórkach, przewijanie trzyma fokus [F2 F7, gfxinfo] — POMINIĘTA
- miary: `{"open:detail/tt0903747/series": 2.4}`
- trasa detail/tt0903747/series [F1] OK:  → n:Wznów, zostało S1, E1, 57 min Wznów, zostało S1, E1, 57 min | 57m (2.4 s) fokus 1.5 s, stabilnie 2.4 s

### 28. Odcinek OK → wybór źródła → Wstecz: ten odcinek; długie OK = opcje odcinka [F5 F8] — BŁĄD
- miary: `{"open:detail/tt0903747/series": 1.0}`
- trasa detail/tt0903747/series [F1] OK:  → n:Wznów, zostało S1, E1, 57 min Wznów, zostało S1, E1, 57 min | 57m (1.0 s) fokus 1.0 s, stabilnie 1.0 s
- ▼×1 [-] OK:  → n:Odcinek 1, Pilot Odcinek 1, Pilot | ODCINEK 1 | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się, (0.0 s) → Odcinek/Pilot/E1
- OK na odcinku [F1] OK: n:Odcinek 1, Pilot Odcinek 1, Pilot | ODCINEK 1 | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się, → n:Odtwórz Odtwórz | 21 sty 2008 · 59 min (0.8 s) pierwszy fokus 0.1 s
- Wstecz [F5] UWAGA: n:Odtwórz Odtwórz | 21 sty 2008 · 59 min → n:Odcinek 1, Pilot Odcinek 1, Pilot | ODCINEK 1 | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się, (0.9 s) ten sam element, inne położenie [80, 416, 490, 955] vs [80, 515, 490, 1054]
- długie OK [F8] BŁĄD: n:Odcinek 1, Pilot Odcinek 1, Pilot | ODCINEK 1 | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się, → n:Odcinek 1, Pilot Odcinek 1, Pilot | ODCINEK 1 | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się, (1.4 s) brak akcji długiego OK
- Wstecz [F5] BŁĄD: n:Odcinek 1, Pilot Odcinek 1, Pilot | ODCINEK 1 | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się, → n:Wznów, zostało S1, E1, 57 min Wznów, zostało S1, E1, 57 min | 57m (1.1 s) fokus „n:Wznów, zostało S1, E1, 57 min Wznów, zostało S1, E1, 57 min | 57m” zamiast „n:Odcinek 1, Pilot Odcinek 1, Pilot | ODCINEK 1 | Pilot | Nauczyciel chemii w szkole średniej dowiaduje się,”

### 29. Obsada → Osoba → plakat → tytuł → Wstecz ×3: za każdym razem element źródłowy [F5] — UWAGA
- miary: `{"open:detail/tt15398776/movie": 0.4}`
- trasa detail/tt15398776/movie [F1] OK:  → n:Wznów, zostało S1, E1, 57 min Wznów, zostało S1, E1, 57 min | 57m (0.4 s) fokus 0.4 s, stabilnie 0.4 s

### 30. Zwiastun: OK → od razu „Wczytuję zwiastun…” → Wstecz na kartę [F5 F12, czas] — UWAGA
- miary: `{"open:detail/tt15398776/movie": 0.8}`
- trasa detail/tt15398776/movie [F1] OK:  → n:Wznów, zostało 178 min Wznów, zostało 178 min | 178m (0.8 s) fokus 0.8 s, stabilnie 0.8 s
- ▼×1 [-] OK:  → n:Nowy zwiastun Nowy zwiastun (0.0 s) → Zwiastun
- OK zwiastun [F12] OK: n:Nowy zwiastun Nowy zwiastun → 00:13 | built-2h (0.7 s)
- Wstecz [F5] UWAGA: 00:14 | built-2h → n:Nowy zwiastun Nowy zwiastun (0.9 s) ten sam element, inne położenie [80, 388, 490, 717] vs [80, 437, 490, 766]

### 31. Wstecz z głębi strony (▼×6): góra albo wyjście, fokus zawsze widoczny [F7 F12] — OK
- miary: `{"open:detail/tt15398776/movie": 0.9}`
- trasa detail/tt15398776/movie [F1] OK:  → n:Wznów, zostało 178 min Wznów, zostało 178 min | 178m (0.9 s) fokus 0.9 s, stabilnie 0.9 s
- Wstecz [F12] OK: n:4 Oscary Oscar: 4 Oscary, 2024 | 4 Oscary | 2024 → n:Wznów, zostało 178 min Wznów, zostało 178 min | 178m (1.4 s) pierwszy fokus 0.9 s
- po Wstecz [F7] OK:  → n:Wznów, zostało 178 min Wznów, zostało 178 min | 178m (0.0 s)

### 32. Wybór źródła: pierwszy fokus na pierwszym źródle; filtry ▲▼ wracają na wybrany filtr [F1 F3] — UWAGA
- miary: `{"open:detail/tt15398776/movie": 0.7}`
- trasa detail/tt15398776/movie [F1] OK:  → n:Wznów, zostało 178 min Wznów, zostało 178 min | 178m (0.7 s) fokus 0.7 s, stabilnie 0.7 s
- OK „Odtwórz” → źródła [F1] OK: n:Wznów, zostało 178 min Wznów, zostało 178 min | 178m → n:Inne źródło Inne źródło (1.4 s) pierwszy fokus 0.5 s

### 33. Odtwarzacz: OK nakładka, ▲ do przycisków nad paskiem, ◄►, ▼ na pasek [F3 F4] — BŁĄD
- miary: `{"open:home": 1.4}`
- trasa home [F1] OK:  → n:Dodaj do biblioteki Dodaj do biblioteki (1.4 s) fokus 0.4 s, stabilnie 1.4 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ▼ [F2] BŁĄD: n:Główna Główna → n:Główna Główna (0.7 s) fokus nie ruszył się w stronę strzałki
- OK „Oglądaj dalej” → odtwarzacz [F1] OK: n:Główna Główna → n:Odkrywaj: gatunek, rok, katalog Odkrywaj: gatunek, rok, katalog (1.5 s) pierwszy fokus 0.6 s
- nakładka [F1] OK:  → n:Seriale Seriale (0.0 s) fokus „n:Seriale Seriale”
- ▲ [F2] OK: n:Seriale Seriale → n:Seriale Seriale (0.4 s)
- ◄ [F2] OK: n:Seriale Seriale → n:Filmy Filmy (0.5 s)
- ► [F2] OK: n:Filmy Filmy → n:Seriale Seriale (0.4 s)
- ▼ [F2] OK: n:Seriale Seriale → n:Wszystkie gatunki Wszystkie gatunki (0.4 s)
- Wstecz [F12] OK: n:Wszystkie gatunki Wszystkie gatunki → n:Odkrywaj: gatunek, rok, katalog Odkrywaj: gatunek, rok, katalog (1.0 s) pierwszy fokus 0.1 s
- Wstecz [F12] OK: n:Odkrywaj: gatunek, rok, katalog Odkrywaj: gatunek, rok, katalog → n:Odtwórz Odtwórz (1.5 s) pierwszy fokus 0.2 s

### 34. Przewijanie ◄► pojedyncze i przytrzymane; po schowaniu nakładki klawisz nie gubi fokusu [F9 F6] — OK
- miary: `{"open:home": 0.2}`
- trasa home [F1] OK:  → n:Odtwórz Odtwórz (0.2 s) fokus 0.2 s, stabilnie 0.2 s
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (0.7 s) przewinięcie/zmiana układu
- OK → odtwarzacz [F1] OK: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → n:Inne źródło Inne źródło (1.4 s) pierwszy fokus 0.8 s
- klawisz po schowaniu nakładki [F6] OK:  → [bez nazwy 1579x48 @156,940] (0.0 s) fokus „[bez nazwy 1579x48 @156,940]”
- Wstecz [F12] OK: [bez nazwy 1579x48 @156,940] → 00:14 | built-2h (2.1 s) pierwszy fokus 0.2 s
- Wstecz [F12] OK: 00:14 | built-2h → n:Wznów, zostało S1, E1, 57 min Wznów, zostało S1, E1, 57 min | 57m (1.0 s) pierwszy fokus 0.3 s

### 35. „Zmień źródło” → lista → Wstecz na ten przycisk [F5] — UWAGA
- miary: `{"open:home": 0.8}`
- trasa home [F1] OK:  → n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 (0.8 s) fokus 0.4 s, stabilnie 0.8 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ▼ [F2] OK: n:Odtwórz Odtwórz → n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min (0.9 s) przewinięcie/zmiana układu
- OK → odtwarzacz [F1] OK: n:Breaking Bad, S1, E1 · 57m, Zostało 57 min Breaking Bad, S1, E1 · 57m, Zostało 57 min | Zostało 57 min → n:Inne źródło Inne źródło (2.7 s) pierwszy fokus 1.0 s
- Wstecz [F12] OK: [bez nazwy 1577x48 @157,940] → n:Episode 1 Episode 1 | Teraz | E1 · Episode 1 (0.7 s) pierwszy fokus 0.1 s
- Wstecz [F12] OK: n:Episode 1 Episode 1 | Teraz | E1 · Episode 1 → n:Episode 1 Episode 1 | Teraz | E1 · Episode 1 (0.8 s) pierwszy fokus 0.1 s

### 36. Wstecz z odtwarzacza → strona/Główna, fokus na źródle; czas pierwszej klatki [F5 F12, czas] — BŁĄD
- miary: `{"open:home": 0.9, "pierwsza klatka (log)": "brak wpisu"}`
- trasa home [F1] OK:  → n:W te święta mam cię na oku, W te święta mam cię na oku · 107 W te święta mam cię na oku, W te święta mam cię na oku · 107m, Zostało 107 min | Zostało 1 (0.9 s) fokus 0.1 s, stabilnie 0.9 s; trasa na otwartej Głównej: menu z fokusem „Główna”, ► do treści
- ▼ [F2] BŁĄD: n:Główna Główna → n:Główna Główna (0.5 s) fokus nie ruszył się w stronę strzałki
- OK → odtwarzacz [F1] OK: n:Główna Główna → n:Odkrywaj: gatunek, rok, katalog Odkrywaj: gatunek, rok, katalog (1.1 s) pierwszy fokus 0.5 s
- Wstecz [F12] OK: n:Odkrywaj: gatunek, rok, katalog Odkrywaj: gatunek, rok, katalog → n:Odtwórz Odtwórz (1.2 s) pierwszy fokus 0.2 s
- Wstecz do Głównej [F5] OK: n:Odtwórz Odtwórz → n:Główna Główna (0.6 s) pierwszy fokus 0.1 s

### 37. Live TV: wejście — fokus na ostatnim lub pierwszym kanale; czas listy z dysku [F1, czas] — OK
- miary: `{"open:reshaped_live_tv": 2.0}`
- trasa reshaped_live_tv [F1] OK:  → n:Wszystkie Wszystkie (2.0 s) fokus 0.0 s, stabilnie 2.0 s
- pierwszy fokus Live TV [F1] OK:  → n:Wszystkie Wszystkie (0.0 s) „n:Wszystkie Wszystkie”

### 38. Chip „Kraj i język” → panel → Wstecz: fokus na chipie (bez zmiany wyboru) [F5 F6] — POMINIĘTA
- miary: `{"open:reshaped_live_tv": 0.7}`
- trasa reshaped_live_tv [F1] OK:  → n:Wszystkie Wszystkie (0.7 s) fokus 0.7 s, stabilnie 0.7 s

### 39. Lista kanałów: przytrzymana ▼ 5 s i ▲ [F9, gfxinfo] — OK
- miary: `{"open:reshaped_live_tv": 1.9, "gfx:kanaly": {"frames": 73, "janky%": 1.37, "p95": 7, "slowUI": 1, "slowBmp": 0, "50th": 5, "90th": 7, "99th": 65}, "klatki>=100ms(log)": 0}`
- trasa reshaped_live_tv [F1] OK:  → n:Wszystkie Wszystkie (1.9 s) fokus 1.9 s, stabilnie 1.9 s
- ▼×1 [-] OK:  → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.0 s) → wiersz kanału
- ▼ 5 s [F7] OK:  → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (5.0 s)
- ▲ 5 s [F9] OK: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (5.0 s) „n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV”
- gfx kanaly [klatki] OK:  →  (0.0 s) Janky 1.37%, p95 7 ms, klatek 73

### 40. Kanał OK → odtwarzanie → Wstecz: ten sam kanał (okno „nie odpowiada”: przyciski i Wstecz) [F5 F1 F12] — BŁĄD
- miary: `{"open:reshaped_live_tv": 2.3}`
- trasa reshaped_live_tv [F1] OK:  → n:Wszystkie Wszystkie (2.3 s) fokus 0.1 s, stabilnie 2.3 s
- ▼×1 [-] OK:  → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.0 s) → wiersz kanału
- OK kanał [F1] OK: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → 00:16 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCL (1.3 s) pierwszy fokus 0.2 s
- Wstecz z kanału [F5] BŁĄD: 00:16 | Source: Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | → 00:16 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCL (0.9 s) fokus „00:16 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCL” zamiast „n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV”

### 41. Długie OK na kanale → „Opcje kanału” → ▼ po opcjach → Wstecz na kanał [F8 F5] — BŁĄD
- miary: `{"open:reshaped_live_tv": 2.2}`
- trasa reshaped_live_tv [F1] OK:  → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (2.2 s) fokus 0.1 s, stabilnie 2.2 s
- ▼×1 [-] OK:  → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.0 s) → wiersz kanału
- długie OK [F8] BŁĄD: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (1.4 s) brak akcji długiego OK
- panel opcji [F8] BŁĄD:  → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.0 s) brak „Opcje kanału”
- Wstecz [F5] BŁĄD: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Wszystkie Wszystkie (0.7 s) fokus „n:Wszystkie Wszystkie” zamiast „n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV”

### 42. Przewodnik EPG: ►◄ w czasie, ▼▲ trzyma czas; podgląd nie kradnie fokusu [F2 F6, gfxinfo] — BŁĄD
- miary: `{"open:reshaped_live_tv": 8.3, "gfx:epg": {"frames": 19, "janky%": 10.53, "p95": 22, "slowUI": 2, "slowBmp": 0, "50th": 8, "90th": 20, "99th": 22}}`
- trasa reshaped_live_tv [F1] BŁĄD:  → None (8.3 s) brak fokusu po otwarciu
- ▼×1 [-] OK:  → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.0 s) → wiersz kanału
- ► [F2] OK: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.4 s)
- ▼ [F2] BŁĄD: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.6 s) fokus nie ruszył się w stronę strzałki
- ▼ trzyma czas [F2] OK: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.0 s) x 0 → 0
- podgląd po 2,5 s [F6] OK: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (2.5 s)
- ▲ [F2] BŁĄD: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.4 s) fokus nie ruszył się w stronę strzałki
- ◄ [F2] OK: n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV → n:Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15 1 | Strong8K · ENDED | BEN MERTENS - JIMMY WHITE | Mon 23 Nov 15:30 CET (DE) | 8K EXCLUSIV (0.4 s)
- gfx epg [klatki] UWAGA:  →  (0.0 s) Janky 10.53%, p95 22 ms, klatek 19

### 43. „Szukaj w telewizji”: fokus w polu, wynik OK → Wstecz [F1 F5] — UWAGA
- miary: `{"open:fork_tv_search": 1.3}`
- trasa fork_tv_search [F1] OK:  → Szukaj kanału lub programu (1.3 s) fokus 0.2 s, stabilnie 1.3 s
- fokus w polu [F1] OK:  → Szukaj kanału lub programu (0.0 s) „Szukaj kanału lub programu”
- klawiatura „tvp” [F6] UWAGA:  → Szukaj kanału lub programu (42.2 s) nie doszedłem do v

### 44. Ustawienia: ▼ kategorie, ► treść, ◄ ta sama kategoria; Wygląd → „Szkło” niedostępne [F3 F10] — UWAGA
- miary: `{"open:settings": 1.0}`
- trasa settings [F1] OK:  → n:Oglądanie Oglądanie (1.0 s) fokus 0.1 s, stabilnie 1.0 s
- ▼ [F2] OK: n:Oglądanie Oglądanie → n:Napisy i dźwięk Napisy i dźwięk (0.4 s)
- ► [F2] OK: n:Napisy i dźwięk Napisy i dźwięk → n:Rozmiar napisów Rozmiar napisów | — (0.3 s)
- ◄ [F2] OK: n:Rozmiar napisów Rozmiar napisów | — → n:Napisy i dźwięk Napisy i dźwięk (0.4 s)
- ◄ na kategorię [F3] OK: n:Napisy i dźwięk Napisy i dźwięk → n:Napisy i dźwięk Napisy i dźwięk (0.0 s)
- ▼×2 [-] OK:  → n:Wygląd Wygląd (0.0 s) → Wygląd
- ► [F2] OK: n:Wygląd Wygląd → n:Skórka Skórka (0.4 s)

### 45. Przełącznik OK ×2: stan wraca, fokus zostaje [F6] — OK
- miary: `{"open:playback_settings": 0.8}`
- trasa playback_settings [F1] OK:  → Odtwarzacz | Domyślny odtwarzacz, silnik wbudowany i awaryjne przełączanie. | Zamknięte (0.8 s) fokus 0.1 s, stabilnie 0.8 s
- ▼ [F2] OK: Odtwarzacz | Domyślny odtwarzacz, silnik wbudowany i awaryjne przełączanie. | Zamknięte → Wybór strumienia | Sposób wyboru strumienia po naciśnięciu odtwarzania. | Zamknięte (0.4 s)
- OK×2 [F6] OK: Wybór strumienia | Sposób wyboru strumienia po naciśnięciu odtwarzania. | Zamknięte → Wybór strumienia | Sposób wyboru strumienia po naciśnięciu odtwarzania. | Zamknięte (0.0 s) stan/fokus wrócił

### 46. Napisy → wygląd: Atkinson Hyperlegible domyślna, na liście Tiresias; lista → Wstecz bez zmiany [F1 F5] — UWAGA
- miary: `{"open:playback_settings": 0.8}`
- trasa playback_settings [F1] OK:  → Odtwarzacz | Domyślny odtwarzacz, silnik wbudowany i awaryjne przełączanie. | Zamknięte (0.8 s) fokus 0.1 s, stabilnie 0.8 s

### 47. YouTube: czas do fokusu w treści, OK → odtwarzacz → Wstecz (kod obcy: pomiar) [F1 F5, czas] — UWAGA
- miary: `{"open:youtube": 0.8}`
- trasa youtube [F1] OK:  → n:Główna Główna (0.8 s) fokus 0.1 s, stabilnie 0.8 s

### 48. Profile: ◄► po profilach, (PIN: wejście i Wstecz bez wpisywania) [F1 F5] — OK
- miary: `{"open:profiles": 0.1}`
- trasa profiles [F1] OK:  → n:Na żywo Na żywo (0.1 s) fokus 0.1 s, stabilnie 0.1 s
- ► na krawędzi (po 3) [F4] OK: n:You NEED to STOP Using Windows 11 Right Now. Microsoft is OV 2:50:09 → n:You NEED to STOP Using Windows 11 Right Now. Microsoft is OV 2:50:09 (0.0 s)
- ◄ na krawędzi (po 3) [F4] OK: n:Główna Główna → n:Główna Główna (0.0 s)
- PIN [-] OK:  → n:You NEED to STOP Using Windows 11 Right Now. Microsoft is OV 2:50:09 (0.0 s) brak profilu z PIN-em — bez OK (nie przełączamy profilu)

### 49. Odkrywaj / Zobacz wszystko: siatka ▼►, przytrzymana ▼, OK → tytuł → Wstecz na ten sam kafelek [F2 F5 F7 F9, gfxinfo] — UWAGA
- miary: `{"open:discover": 0.8}`
- trasa discover [F1] OK:  → n:Seriale Seriale (0.8 s) fokus 0.2 s, stabilnie 0.8 s

### 50. Kalendarz, Dodatki, Kolejność katalogów, Kolekcje: pierwszy fokus, Wstecz z pozycji na źródło [F1 F5] — OK
- miary: `{"open:calendar": 1.1, "open:addon_manager": 0.8, "open:catalog_order": 0.8, "open:collections": 0.7}`
- trasa calendar [F1] OK:  → ŚR. | 25 | Rywale | śr. 25 lis · S2 E11 · Disney+ | Today (1.1 s) fokus 1.1 s, stabilnie 1.1 s
- ▼ [F2] OK: ŚR. | 25 | Rywale | śr. 25 lis · S2 E11 · Disney+ | Today → ŚR. | 25 | Władca Pierścieni: Pierścienie Władzy | śr. 25 lis · S3 E7 · Prime Video | Toda (0.5 s)
- calendar: OK [F1] OK: ŚR. | 25 | Władca Pierścieni: Pierścienie Władzy | śr. 25 lis · S3 E7 · Prime Video | Toda → Open the show (1.1 s) pierwszy fokus 0.4 s
- calendar: Wstecz [F5] OK: Open the show → ŚR. | 25 | Władca Pierścieni: Pierścienie Władzy | śr. 25 lis · S3 E7 · Prime Video | Toda (0.8 s) pierwszy fokus 0.4 s
- trasa addon_manager [F1] OK:  → https://example.com (0.8 s) fokus 0.1 s, stabilnie 0.8 s
- ▼ [F2] OK: https://example.com → Zarządzaj z telefonu | Zeskanuj kod QR, aby zarządzać dodatkami, katalogami i kolekcjami z (0.4 s)
- addon_manager: OK [F1] OK: Zarządzaj z telefonu | Zeskanuj kod QR, aby zarządzać dodatkami, katalogami i kolekcjami z → Zamknij (0.6 s) pierwszy fokus 0.1 s
- addon_manager: Wstecz [F5] OK: Zamknij → Zarządzaj z telefonu | Zeskanuj kod QR, aby zarządzać dodatkami, katalogami i kolekcjami z (0.8 s) pierwszy fokus 0.3 s
- trasa catalog_order [F1] OK:  → [bez nazwy 120x96 @1704,189] (0.8 s) fokus 0.1 s, stabilnie 0.8 s
- ▼ [F2] OK: [bez nazwy 120x96 @1704,189] → Przypięte (0.4 s)
- catalog_order: OK [F1] OK: Przypięte → Przypięte (0.6 s) pierwszy fokus 0.1 s
- trasa collections [F1] OK:  → Nowa kolekcja (0.7 s) fokus 0.7 s, stabilnie 0.7 s
- ▼ [F2] OK: Nowa kolekcja → Edytuj (0.4 s)
- collections: OK [F1] OK: Edytuj → Spotlight (0.9 s) pierwszy fokus 0.4 s
- collections: Wstecz [F5] OK: Spotlight → Edytuj (1.0 s) pierwszy fokus 0.6 s


## Czas testu

- odczyty ekranu: 1068 (czytnik 1068, zwykły 0, błędy czytnika 0), razem 2.4 min = 20% czasu, średnio 0.14 s; klawisze przez czytnik: 502
