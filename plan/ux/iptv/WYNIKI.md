# Zakładka IPTV (Live TV) — wyniki wątku IPTV, 26.11

**Stan: 3 poprawki aplikacji w kodzie (#82, #83, #73), 0 sprawdzonych na urządzeniu** — projektor zajęty przez wątek VOD, build czeka na paczkę.

Zadanie właściciela (26.11 ~20:30): „sprawdź zakładkę IPTV: nie działają kraje i języki — po wyborze pusta lista; szukanie odsyła do programu, którego jeszcze nie emitowano, zamiast do strony szczegółów programu, gdzie można go obejrzeć; program „Gry małżeńskie” dodany przez długie naciśnięcie na wyszukany element zgłasza komunikat, że dodano, a dodano inny program — „Nasz nowy dom”; sprawdź inne błędy”.

## 1. Kraje i języki — pusta lista (#81)
- Stan: **przyczyna jeszcze nieustalona** — w kodzie filtra (`NiuniaLiveRegion`) pusty wynik daje pełną listę, więc pustka bierze się dalej (filtr pigułki: „Ulubione” × kraj = pusto?) albo z samego panelu. Logi właściciela z 26.11 wieczorem już wypadły z bufora projektora (1 MB, zalewany przez `p2pfile`).
- Następny krok: na projektorze, gdy wolny — panel „Kraj i język” z „Wszystkie” i z „Ulubione”, log `live tv: kraj …: N z M kanałów` i `filtr=…, kanały=…`.

## 2. Szukanie prowadziło do emisji, której jeszcze nie było (#82) — poprawione w kodzie
- Przyczyna: wynik szukania brał najlepszą emisję tylko z pamięci przewodnika (−12 h…+36 h, a dla większości kanałów tylko „teraz i następny”). Wczorajsze emisje w archiwum nie trafiały do wyniku, więc wygrywała przyszła; OK kończyło się komunikatem „Ten program zacznie się…”.
- Poprawka: OK na programie w wynikach otwiera **stronę programu** (panel jak „Opcje kanału”): opis, „Dodaj do ulubionych programów”, lista emisji — **na żywo**, **z archiwum** (od najnowszej, 7 dni wstecz) i **jeszcze nie leci** (8 dni naprzód). OK na emisji na żywo/z archiwum włącza ją, na przyszłej mówi, kiedy się zacznie. Emisje z bazy przewodnika (`GuideDbBridge.findTitle`), kopie tego samego kanału raz, archiwum z kopii, która je ma. OK na **kanale** w wynikach nadal włącza kanał.
- Pliki: `skin/niunia/livetv/NiuniaLiveProgramme.kt` (nowy), `NiuniaLiveSearch.kt`, `fork/tv/plus/GuideDbBridge.kt`, `res/values/niunia_tvprog_strings.xml`; test JVM `NiuniaProgrammeAiringsTest`.

## 3. Długie OK dodawało inny program (#83) — poprawione w kodzie
- Przyczyna: długie OK na kafelku w wynikach szukania (i w „Odkrywaj”) dodawało do ulubionych **kanał**, nie program. Komunikat „Dodano do ulubionych: <kanał>”, a w „Ulubione” pojawiał się program, który akurat leciał na tym kanale („Nasz nowy dom”).
- Poprawka: na kafelku programu długie OK dodaje **program** do ulubionych programów — komunikat „Dodano do ulubionych programów: Gry małżeńskie” (drugi raz: „Usunięto…”). Na kafelku kanału — kanał, komunikat „Dodano kanał do ulubionych: …”.

## 4. Inne błędy
- #73 (z listy „do decyzji”): po obejrzeniu kanału włączonego z wyszukiwarki Wstecz wracał do przewodnika — teraz wraca do wyników (to samo zapytanie). Test JVM w `NiuniaLiveReturnTest`.
- Przegląd zakładki na projektorze — gdy wolny.

## Do sprawdzenia na projektorze (po buildzie)
- [ ] Szukaj „gry” → „Gry małżeńskie” → OK → strona programu: emisje z archiwum (wczoraj) na górze, OK włącza archiwum.
- [ ] Długie OK na „Gry małżeńskie” → komunikat z tytułem; Live TV › Szukaj › drzewo „Ulubione” ma ten tytuł.
- [ ] Wstecz z odtwarzacza (kanał z wyszukiwarki) → wyniki wyszukiwania.
- [ ] Kraj i język: wybór kraju i języka z „Wszystkie” i z „Ulubione”.
