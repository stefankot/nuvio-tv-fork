# Zakładka IPTV (Live TV) — wyniki wątku IPTV, 26.11

**Pętla 3 (26.11, 22:15–22:28, `uxr18` na projektorze, md5 a2d6801b…): sprawdzone na projektorze 3 poprawki — #81 ✔, #82b ✔, #84 częściowo; #85 nie sprawdzone. Nowych poprawek aplikacji: 0.**

## Pętla 3 (`uxr18`)
- #81 ✔ Kraj i język: Polska 1151, Stany Zjednoczone 11 121, Argentyna 3472, Wielka Brytania 2704, Niemcy 2264 kanałów (było: Polska 334, USA 245). Po wyborze USA przewodnik pełny: nazwy („US: TENNIS CHANNEL”) i program („2026 Davis Cup Tennis…”). Przywrócone „Wszystkie kraje”.
  - Uwagi (bez poprawki): nad przewodnikiem nadal napis „Nic tu jeszcze nie ma.” — to pusty rząd kafli „Dla ciebie” dla USA, nie lista; nazwy mają przedrostek „Strong8K · US:”; wydarzenia PPV „Strong8K · NEXT | …” bez programu.
- #82b ✔ Szukaj „gry ma” → „Gry małżeńskie” → strona serialu: 9 emisji z archiwum, 10 przyszłych, numer odcinka w wierszu, fokus na 1. emisji. OK na „Wczoraj 07:00 · Z archiwum · Polo TV” włączył Polo TV (log „archiwum »Gry małżeńskie«”); czy obraz był z archiwum, a nie na żywo — do sprawdzenia (nakładka zgłosiła stan LIVE).
- #84 częściowo: pierwsze wejście po instalacji 9 s „Wczytuję programy…” (pliku na dysku jeszcze nie było — zapisał się wtedy). Do sprawdzenia: po restarcie aplikacji od razu (log „odkrywaj z dysku”).
- #85 (fokus po OK na kanale) — nie sprawdzone.


**Stan: 7 poprawek aplikacji (#73, #81, #82, #82b, #83, #84, #85), 3 sprawdzone na projektorze (`uxr17`: #73 ✔, #82 ✔ częściowo, #83 ✔)** — #81, #82b, #84, #85 czekają na wspólny build `uxr18` z wątkiem VOD. Przyczyna #81 potwierdzona na projektorze (`uxr16`).

Zadanie właściciela (26.11 ~20:30): „sprawdź zakładkę IPTV: nie działają kraje i języki — po wyborze pusta lista; szukanie odsyła do programu, którego jeszcze nie emitowano, zamiast do strony szczegółów programu, gdzie można go obejrzeć; program „Gry małżeńskie” dodany przez długie naciśnięcie na wyszukany element zgłasza komunikat, że dodano, a dodano inny program — „Nasz nowy dom”; sprawdź inne błędy”.

## 1. Kraje i języki — „pusta lista” (#81)
- Na projektorze (`uxr16`, 59 063 kanały): panel pokazywał Polska 334, Meksyk 326, Stany Zjednoczone 245, Szwecja 217, Norwegia 122 (01). Po wyborze „Stany Zjednoczone” przewodnik to same wiersze „43 · Brak programu” bez nazw, a nad nim „Nic tu jeszcze nie ma.” (03) — to „pusta lista”. Były to wydarzenia PPV („Strong8K · NEXT | … (US) | 8K EXCLUSIVE | US: FIFA+ PPV 131”) bez programu i z niedziałającym logo.
- Przyczyna: kraj kanału był brany z nazwy dopiero po zdjęciu przedrostka dostawcy, ale tylko gdy przedrostek był **dokładnie** nazwą źródła. Na projektorze tak nie jest, więc „Strong8K · PL: TVN CZAS NA ŚLUB ᴴᴰ”, „Strong8K · PL VIP: CANAL+ SPORT 2” nie miały kraju. Kraj miało ok. 1,5 tys. z 59 tys. kanałów: lista hls.gd (Polska) i wydarzenia PPV z „(US)”, „(MX)”, „(DE)” w nawiasie.
- Poprawka (łatka `.stage/iptv/patch81.py`, w następnym buildzie): przedrostek dostawcy przed znacznikiem kraju zdejmowany zawsze („Strong8K · ”, „Strong8K: ”), rozpoznawane też „PL VIP:”, „|PL|” i kraj z nazwy grupy listy; pamięć krajów na dysku i zapisana kolejność kanałów liczone raz od nowa. Testy JVM w `TvCountryTest`.
- Skutek uboczny załatwiony od razu: „Kraj kanałów” na projektorze był ustawiony na „Niemcy” (najpewniej przez dawny automat). Po poprawce niemieckie kanały weszłyby na górę każdej listy, więc ustawiłem „Polska”, zgodnie z Twoim „dla mnie polskie” z 22.11. Zmienisz to w Ustawienia › Telewizja › „Kraj kanałów” (jedno OK = następny kraj).

## 2. Szukanie prowadziło do emisji, której jeszcze nie było (#82) — w `uxr17`
- Przyczyna: wynik szukania brał najlepszą emisję tylko z pamięci przewodnika (−12 h…+36 h, a dla większości kanałów tylko „teraz i następny”). Wczorajsze emisje w archiwum nie trafiały do wyniku, więc wygrywała przyszła; OK kończyło się komunikatem „Ten program zacznie się…”.
- Poprawka: OK na programie w wynikach otwiera **stronę programu** (panel jak „Opcje kanału”): opis, „Dodaj do ulubionych programów”, lista emisji — **na żywo**, **z archiwum** (od najnowszej, 7 dni wstecz) i **jeszcze nie leci** (8 dni naprzód). OK na emisji na żywo/z archiwum ją włącza, na przyszłej mówi, kiedy się zacznie. Emisje z bazy przewodnika (`GuideDbBridge.findTitle`), kopie tego samego kanału raz, archiwum z kopii, która je ma. OK na **kanale** w wynikach nadal włącza kanał.

## 3. Długie OK dodawało inny program (#83) — w `uxr17`
- Przyczyna: długie OK na kafelku w wynikach szukania (i w „Odkrywaj”) dodawało do ulubionych **kanał**, nie program. Komunikat „Dodano do ulubionych: <kanał>”, a w „Ulubione” pojawiał się program, który akurat leciał na tym kanale („Nasz nowy dom”).
- Poprawka: na kafelku programu długie OK dodaje **program** do ulubionych programów — komunikat „Dodano do ulubionych programów: Gry małżeńskie” (drugi raz: „Usunięto…”). Na kafelku kanału — kanał, komunikat „Dodano kanał do ulubionych: …”.

## 4. Inne błędy
- #73 (w `uxr17`): po obejrzeniu kanału włączonego z wyszukiwarki Wstecz wracał do przewodnika — teraz wraca do wyników (to samo zapytanie).
- #84 (łatka `.stage/iptv/patch84.py`, następny build): „Odkrywaj” przy pierwszym wejściu po starcie aplikacji — 6,8 s „Wczytuję…” (log „odkrywaj policzone w tle w 6767 ms”). Ostatnie rzędy zapisywane na dysku i pokazywane od razu; nowe liczą się w tle.
- Do obejrzenia (bez zmian): „Ulubione” pokazuje 2 kanały, choć ulubionych jest 4 (log „ulubione kanały 4, kanały=2”) — dwa zapisane kanały nie istnieją już na liście (dostawca zmienił adresy?); kanały-wydarzenia PPV mają wszystkie numer „43”.

## Pętla 2 — `uxr17` na projektorze (26.11 21:17–21:24)
- #82 ✔ (06): Szukaj „gry m” → „Gry małżeńskie (196) · Dziś 06:00” → OK → strona programu z tytułem, „Usuń z ulubionych programów” i emisją „Dziś 06:00 – 07:00 · Jeszcze nie leci · Polsat Cafe”; fokus na emisji; Wstecz → fokus wraca na wynik. Baza zwróciła 17 emisji na 6 kanałach w 2,3 s.
- ✘ → #82b: strona pokazała tylko 1 emisję z 17, bo tytuł z numerem odcinka („(196)”) pasował tylko do siebie. Poprawka: strona serialu „Gry małżeńskie” ze wszystkimi odcinkami (także z archiwum), numer odcinka w wierszu emisji.
- #83 ✔ (07): długie OK na wyniku → „Usunięto z ulubionych programów: Gry małżeńskie (196)” (był już obserwowany), licznik „Ulubione” 3 → 2; drugie długie OK → dodany z powrotem.
- #73 ✔: „tvn” → kanał TVN 24 z wyników → OK → gra → Wstecz → Live TV › Szukaj z „tvn” i wynikami (log „powrót do wyników wyszukiwania”).
- Nowe #85: po OK na kanale z listy nakładka odtwarzacza stawiała fokus na ▶ (log „nakładka otwarta OK (pauza) — fokus na ▶”) — to samo OK i obraz jeszcze w buforze brane za pauzę; ▲▼ nie zmieniały wtedy kanału. Poprawione: ▶ tylko po OK na tym samym kanale.
- Projektor zwolniony 21:24 dla wątku VOD (na JMGO zostaje `uxr17`).

## Do sprawdzenia na projektorze
- [ ] `uxr18`: strona „Gry małżeńskie” — odcinki z archiwum na górze (jeśli Polsat Cafe ma archiwum), OK włącza archiwum.
- [ ] `uxr18`: OK na kanale z listy → nakładka z fokusem na przełączniku kanałów, ▲▼ zmienia kanał (#85).
- [ ] następny build: „Kraj i język” — Polska/Niemcy/Wielka Brytania z tysiącami kanałów z nazwami i programem; język „angielski”.
- [ ] następny build: „Odkrywaj” po restarcie aplikacji — rzędy od razu (log „odkrywaj z dysku”).

Zdjęcia: 01 panel krajów, 02 „Polska”, 03 „Stany Zjednoczone” (pusta lista), 04 zakładka „Język”, 05 wyniki „gry”, 06 strona programu (`uxr17`), 07 komunikat długiego OK.
