# Zakładka IPTV (Live TV) — wyniki wątku IPTV, 26.11

**Stan: 5 poprawek aplikacji (#73, #81, #82, #83, #84), 0 sprawdzonych na projektorze po poprawce** — #73/#82/#83 są w `uxr17` (build wątku VOD), #81/#84 czekają na następny build. Przyczyna #81 potwierdzona na projektorze (`uxr16`).

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

## Do sprawdzenia na projektorze
- [ ] `uxr17`: Szukaj „gry” → „Gry małżeńskie” → OK → strona programu: archiwum na górze, OK włącza archiwum; Wstecz z odtwarzacza → wyniki.
- [ ] `uxr17`: długie OK na „Gry małżeńskie” → komunikat z tytułem; Szukaj › drzewo „Ulubione” ma ten tytuł.
- [ ] następny build: „Kraj i język” — Polska/Niemcy/Wielka Brytania z tysiącami kanałów z nazwami i programem; język „angielski”.
- [ ] następny build: „Odkrywaj” po restarcie aplikacji — rzędy od razu (log „odkrywaj z dysku”).

Zdjęcia: 01 panel krajów, 02 „Polska”, 03 „Stany Zjednoczone” (pusta lista), 04 zakładka „Język”, 05 wyniki „gry”.
