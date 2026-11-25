# UX — testy ręczne na JMGO (24.11.2026, od 19:13)

Polecenie właściciela: ręczne testy (nie automat) ≥ 10 min → wszystkie możliwe poprawki → build → instalacja na JMGO → znowu testy. Zrzuty 960 px: `fork-tools/test/r.sh NAZWA [klawisze…]` → `plan/ux/reczne/NAZWA.png`.

## Pętla 1 (APK `uxj3` → build `uxr1`)
Testy 19:13–19:32 (Główna, wygaszacz, Szukaj, Live TV, odtwarzacz TV, Biblioteka, strona tytułu, Ustawienia, YouTube).

| # | Znalezisko (zrzut) | Stan |
|---|---|---|
| 1 | Główna: dodatek katalogów odpowiada 429 („za dużo zapytań”) → tylko „Oglądaj dalej”, reszta pusta bez słowa (01, 04) | R1: komunikat + „Spróbuj ponownie” + 3 same ponowienia (20/40/60 s) |
| 2 | Główna bez baneru: tytuł „Oglądaj dalej” pod pigułką „Główna” (01) | R1: odstęp 56 dp |
| 3 | Główna bez baneru: brak fokusu po starcie i po wygaszaczu (02) | R1: fokus na pierwszy element pierwszego rzędu |
| 4 | Wygaszacz: zegar „02:14” na logo tytułu (00) | R1: zegar i data w prawym dolnym rogu |
| 5 | Szukaj „dun”: „Muriel”, „Cztery noce marzeń” przed „Dune” (07) | R1: najpierw równe / zaczynające się / słowo od wpisu (test `NiuniaSearchRankTest`) |
| 6 | Szukaj: ◄ z 1. kolumny klawiatury nic nie robi (10) | R1: ◄ otwiera menu |
| 7 | Live TV, „RT nie odpowiada”: spod okna przebija nakładka (16) | R2: tło 92 % czerni |
| 8 | Live TV: kafelki na górze proponują kanały „Niedostępne” (RT, CNBC) (19) | R2: kafelki Wszystkie/kategorii bez nich |
| 9 | Biblioteka: aktywna „Seriale”, fokus na „Wszystko” (23) | R2: `focusRestorer(aktywna zakładka)` |
| 10 | Strona tytułu: „S1, E1 · …” obok „Odtwórz S1, O1” (25) | R2: „Odtwórz S1, E1” |
| 11 | Strona tytułu: przypisy „Europe.[1]” w opisie (25) | R2: usuwane |
| 12 | Odcinki: „ODCINEK 2 / Episode 2” (26) | R2: zastępczy tytuł ukryty (test `NiuniaGenericEpisodeTitleTest`) |
| 13 | YouTube: ◄ na szynie ikon nic nie robi (38) | R2: ◄ otwiera menu |

Działa dobrze (bez zmian): Szukaj ◄ z wyników wraca na klawiaturę; Live TV długie OK → „Opcje kanału”; „Następny kanał”; powrót z odtwarzacza YouTube na ten sam film; powrót ze strony tytułu do Biblioteki (ten sam plakat i przewinięcie); Ustawienia.
Nie ruszam: dymek „50 kl/s przy 60 Hz” w odtwarzaczu (makieta Z-22 właściciela); kanały RT/CNBC — serwer źródła odrzuca połączenie (nie aplikacja).
Drobne na później: brak ikony przy „Usuń z »Oglądaj dalej«” (w zestawie ikon nie ma „x”); ▲ z kafelków Live TV trafia na „Szukaj” w prawym rogu; po „Następny kanał” Wstecz wraca na pierwszy kanał, nie na ostatnio włączony.

- `uxr1`: commit 6b53fc9c2, BUILD SUCCESSFUL 10 min 11 s (testy `niunia.*` OK), `.stage/apk/uxr1-v7a.apk` md5 `30de24d4c17a274865d530de18eb14a9`, zainstalowany na JMGO 19:34 (md5 zgodny), `compile -m speed` w toku.
- `uxr2`: commit 0699da986 (poprawki 7–13), build start ~19:36.

## Pętla 2 (testy na `uxr1` od 19:34, build `uxr2` 19:36–19:46)
Sprawdzone na `uxr1`: kolejność wyników Szukaj „dun” (Dunkirk, Dune, Dungeons…) ✔; ◄ z klawiatury Szukaj → menu ✔ (log „menu ← wyszukiwarka ◄”); powrót karta → strona tytułu → Wstecz: ten sam plakat bez przesunięcia ✔; panel „Kraj i język” i filtr Polska (348 kanałów) ✔; wybór dnia ✔.

| # | Znalezisko (zrzut) | Stan |
|---|---|---|
| 14 | Dwa rzędy „TMDB Popular” (filmy i seriale) nie do odróżnienia (42) | R3: „· filmy” / „· seriale” przy powtórzonej nazwie |
| 15 | Karta tytułu „1h47min”, baner „1 godz. 47 min” (44) | R3: jeden format |
| 16 | Wstecz z karty przewiniętej na #2 wraca na #1 (46) | R3: fokus i przewinięcie na oglądany tytuł |
| 17 | Szukaj: „Dunkirk” dwa razy (50) | R3: jeden wpis na tytuł+rok+rodzaj |
| 18 | „Kraj i język”: Polska za USA (56) | R3: Polska i polski na górze |
| 19 | Chip „Kraj i język” nie mówi, co wybrano (58) | R3: nazwa wybranego kraju/języka |
| 20 | Wstecz z listy dni → fokus na 1. kafelek, OK włączył archiwum RT (60–61) | R3: fokus wraca na „Dziś” |
| 21 | Ustawienia: „Rozmiar napisów —” (65) | R3: odczyt rozmiaru z odtwarzacza (100 % = „Średnie”) |

Uwagi: plakaty wyników Szukaj szare — serwer obrazków odpowiada HTTP 410 (dodatek); „Polska” w Live TV = kanały z polskiej listy dostawcy (są tam też RT, CNBC).
- `uxr2`: commit 0699da986, BUILD SUCCESSFUL 11 min 55 s, `.stage/apk/uxr2-v7a.apk`.
- R3: commit 77445a42f (+ rozmiar napisów, niezacommitowane w chwili zapisu).

## Pętla 3 (testy na `uxr2` od 19:47; build `uxr3` od 19:47)
Sprawdzone na `uxr2` ✔: „Odtwórz S1, E1”; opis bez „[1]”; odcinki bez „Episode 2” (70); YouTube ◄ z szyny → menu (log); kafelki Live TV bez RT/CNBC (73); ciemne tło „CNBC nie odpowiada” (74). ✘: Biblioteka nadal fokus na „Wszystko” (68) — przyczyna niżej (22).

| # | Znalezisko (zrzut) | Stan |
|---|---|---|
| 22 | Biblioteka: przez ~1 s ekran „pusta” z fokusem na „Przeglądaj”, po wczytaniu fokus spada na „Wszystko” (68, log) | R4: w czasie wczytywania samo tło; fokus startowy na 1. plakat |
| 23 | Biblioteka: ◄ z 1. plakatu nic nie robi | R4: ◄ przy krawędzi = menu (onKeyEvent, bez panelu) |
| 24 | Strona tytułu: „S1, E1 · Odcinek 1:” w opisie | R4: bez zastępczego tytułu |
| 25 | Live TV po starcie: „Te kanały nie mają programu TV…”, gdy program się wczytuje (72) | R4: podpowiedź po 20 s bez programu |
| 26 | Profile: dwa „Stef+Patryk (K…” nie do odróżnienia (75) | R4: imię w 2 liniach |
| 27 | „Usuń z »Oglądaj dalej«” bez ikony | R4: ikona „close” (x) |
| 28 | Po odtwarzaczu Główna w rzędach z pigułką „Główna” na tytule rzędu (79) | R5: pigułka schowana, gdy Główna pod hero (`NiuniaNavChrome.homeRows`) |
| 29 | Po odtwarzaczu obejrzany tytuł na początku rzędu, rząd przewinięty — fokus na niewidocznej karcie, ◄ nic nie robi (79–80) | R5: rząd przewija się na początek |

Nie ruszam: „Finding stream source” po angielsku (zasada: bez tłumaczeń); chwilowa nakładka „Odświeżanie/Rozdzielczość” na zegarze w odtwarzaczu (kod bazy, kilka sekund).
| 30 | Ustawienia › Telewizja: „Kraj kanałów” bez wartości; na JMGO ustawione „Niemcy” (log `country=de`) przy „Najpierw kanały z mojego kraju” (83) | R6: wartość przy pozycji. Samego ustawienia nie zmieniam (profil właściciela) — do decyzji: prawdopodobnie przestawione przez dawny automat testowy |

- `uxr3`: commit 6965fce4a (R3, R3b), BUILD SUCCESSFUL 14 min 29 s, `.stage/apk/uxr3-v7a.apk`, instalacja 20:02.
- `uxr4`: R4 + R5 + R6 (5229dfaaa, 3bfb5d97a, f682e7b45), build start 20:03.

## Pętla 4 (testy na `uxr3` od 20:05; build `uxr4` od 20:03)
Sprawdzone na `uxr3` ✔: „TMDB Popular · filmy / · seriale” (88); karta „1 godz. 47 min” (89); panel „Kraj i język” — Polska na górze, chip „Polska” (94–95); Wstecz z listy dni → „Dziś” (log); „Rozmiar napisów: Małe” (96); Szukaj — drugi „Dunkirk” to film z 1958 (93, poprawne). ✘: Wstecz z karty tytułu na oglądany tytuł — efekt anulowany (`LeftCompositionCancellationException`), poprawione w R8.
Filtr kraju przywrócony na „Wszystkie kraje” po teście.

| # | Znalezisko | Stan |
|---|---|---|
| 31 | Wstecz z karty: wyzerowanie stanu na początku efektu anulowało przewinięcie | R8: zerowanie na końcu |
| 32 | Live TV: ◄ z „Szukaj” skacze na kafelek zamiast do menu | R8: ◄ z „Szukaj” = menu |
| 33 | Po „Następny kanał” Wstecz wraca na kanał otwarty z listy | R7: na ostatnio oglądany (`recentChannel`) |
| 34 | Live TV › Odkrywaj: 4× „Kuchenne rewolucje” w Ulubionych, CNBC/RT w „Dla ciebie” i „Teraz na żywo” (104) | R9: jeden kafel na program+godzinę, bez Niedostępnych |
| 35 | Live TV: Wstecz z Odkrywaj/Szukaj → fokus na „Wszystkie” | R10: na przycisk trybu |
| 36 | Szukaj: klawiatura bez polskich liter (Live TV › Szukaj ma) (105) | R11: ą…ż, dwie frazy podpowiedzi |

Działa dobrze: Live TV › Szukaj „kuch” → 18 programów, 8 kanałów (106); odtwarzacz filmu — nakładka (koniec o…, UHD, dźwięk, lektor, napisy) (98); trzecie Wstecz z odtwarzacza wraca na ten sam film.
- `uxr4`: R4–R6, BUILD SUCCESSFUL 13 min 57 s, instalacja 20:16. `uxr5`: R7–R11, build start 20:16.

## Pętla 5 (testy na `uxr4` od 20:17; build `uxr5` od 20:16)
Sprawdzone na `uxr4` ✔: Biblioteka — bez ekranu „pusta” w czasie wczytywania, fokus na 1. plakacie, aktywna „Seriale” widoczna (107, log); ◄ z 1. plakatu → menu (log); po obejrzeniu tytuł na początku „Oglądaj dalej”, fokus widoczny, pigułka schowana (112); „Kraj kanałów: Niemcy” (113). ✘: imiona profili nadal ucięte — R4 zmienił nieużywany `ProfileItem`; właściwa karta `NiuniaProfileCard` poprawiona w R12.

| # | Znalezisko | Stan |
|---|---|---|
| 37 | Profile: imię ucięte (właściwy komponent) (108) | R12 |
| 38 | Film YouTube z „Oglądaj dalej” na Głównej: Wstecz z odtwarzacza ląduje w zakładce YouTube (111) | R13: powrót na Główną (`ForgeYouTubeLaunch.returnRoute`, tylko gdy odtwarzacz się pokazał) |

Uwaga dla właściciela: w danych są pozycje testowe `autoplayqa-subtitles` / `autoplayqa-finalsubtitles` (log MetaRepository) — ślad testów innego wątku.

## Koniec dnia (25.11 12:30) — polecenie właściciela: zapisać stan, naprawić zebrane, build, instalacja, koniec
- Po ostatnim teście: podstrona Ustawień („Skórka”, „Menu i styl…”) — log fokusu pokazywał „Szukaj”, ale ▼ przesuwa podświetlenie po pozycjach podstrony (118): fokus jest, tylko pozycje bazy mają inny (szary) wygląd fokusu. Bez zmian.
- `uxr5` (R7–R11): BUILD SUCCESSFUL 12 min 49 s — niezainstalowany (zastąpiony przez `uxr6`).
- `uxr6` = wszystko R1–R13 (HEAD ca18bbfed): build 25.11 ~12:27.
- Niesprawdzone na TV (są w `uxr5/uxr6`, nie było ich w zainstalowanych APK): R7 powrót na ostatnio oglądany kanał, R8 Wstecz z karty tytułu na oglądany tytuł + ◄ z „Szukaj” w Live TV, R9 Odkrywaj bez powtórek/niedostępnych, R10 Wstecz z Odkrywaj/Szukaj na przycisk trybu, R11 polskie litery w Szukaj, R12 imiona profili w 2 liniach, R13 YouTube z „Oglądaj dalej” → Wstecz na Główną. Lista też w `plan/DO_SPRAWDZENIA_NA_TV.md` („UX ręczne”).
- Nie ruszone (DECYZJE 24.11 „UX ręczne”): teksty bazy po angielsku, dymek kl/s, nakładka „Odświeżanie”, „Gotowe” na zwiastunie (najpewniej napis z materiału), kraj IPTV „Niemcy”, pozycje testowe `autoplayqa-*` w danych.
- 25.11 12:32: `uxr6` w trakcie builda (start 12:27, `/tmp/uxr6.log`), niezainstalowany. JMGO wolny (właściciel 12:31). Przekazanie do nowego wątku na polecenie właściciela (kontekst zużyty).

## Pętla 6 (25.11, nowy wątek; `uxr6` na JMGO, build `uxr7`)
- `uxr6` (R1–R13): BUILD SUCCESSFUL 14 min 20 s, `.stage/apk/uxr6-v7a.apk` md5 `278f2ccb43736eb5356e08ef55671ffd`, zainstalowany 12:44 (md5 z urządzenia zgodny), `compile -m speed` OK.
- Testy na `uxr4` w czasie builda (Główna, menu, Ustawienia: Oglądanie, Napisy, Wygląd, Konta, Pilot, System) i na `uxr6`.
- Sprawdzone na `uxr6`: R11 polskie litery ✔ (144–145; potem usunięte na polecenie właściciela), R12 profile w 2 liniach ✔ (147), R8 Live TV ◄ z „Szukaj” → menu ✔ (log), R9 Odkrywaj ✔ (161), R10 Wstecz z Odkrywaj → „Odkrywaj” ✔ (log). ✘: R8 Wstecz z karty tytułu → wraca na 1. plakat (154; przy ► z ostatniego przycisku karty fokus na chwilę ucieka do rzędu pod kartą), R13 YouTube z „Oglądaj dalej” → Wstecz = zakładka YouTube (151). R7 nierozstrzygnięte (kanały 1–5 dostawcy o tej samej nazwie).
- Przy testach przypadkiem przestawione i od razu przywrócone: „Rozmiar napisów” (OK zmienia wartość w kółko) → „Małe”; „YouTube na ekranie głównym” → włączone.

| # | Znalezisko (zrzut) | Stan |
|---|---|---|
| 39 | „Co mogę powiedzieć” — goła czarna lista, bez wyglądu Niuni (130) | bez zmian (DECYZJE 25.11) |
| 40 | „Pomoc i dziennik błędów” otwiera ekran „Narzędzia” (132) | R14: pozycja „Narzędzia” z opisem |
| 41 | Wyłączony przełącznik niewidoczny na zaznaczonym białym wierszu (136) | R14: ciemny tor (`NiuniaSwitch(onLight)`) |
| 42 | „Na co masz ochotę?” pusty do pierwszego OK (138–139) | R14: od razu 1. nastrój |
| 43 | YouTube: polski kanał gra z angielskim dubbingiem automatycznym, bez przełączenia (zgłoszenie właściciela) | NIEPOPRAWIONE (polecenie właściciela: na razie bez poprawek). Przyczyna: `fork/yt/YouTubeAudioSlim.prepare` grupuje zestawy audio po `lang` (log `ForkYtAudio: audio sets 2 -> 1`) i zostawia pierwszy AAC = „English · auto-dubbed”; oryginał PL usunięty z manifestu |
| 44 | Główna: po zamknięciu karty tytułu ◄ z 1. plakatu rzędu nie otwiera menu (156–157) | niepoprawione |
| 45 | Karta tytułu „Shetland”: gatunki/opis po angielsku, w rzędzie „Dramat” (152) | niepoprawione |
| 46 | Live TV: kafelki na górze znów z RT/CNBC po restarcie aplikacji (158) | niepoprawione |
| — | Właściciel 25.11: „usuń polskie litery diakrytyczne, to anty-UX” | R14: klawiatury Szukaj i Live TV › Szukaj tylko a–z |

- R14: commit c5343fd36. Właściciel 25.11 ~12:50: „rozpisz dokument ze wszystkimi poprawkami, na razie nie poprawiaj więcej, ale możesz testować i zrobić builda” → `plan/ux/reczne/POPRAWKI.md` (wysłany). Build `uxr7` (R1–R14) start ~12:49 (`/tmp/uxr7.log`); przed buildem zwolnione miejsce: wpisy `~/.gradle/caches/build-cache-1` starsze niż 2 h (6,9 → 14 GB wolne).
- `check.sh`: 2 błędy w plikach Reshaped/ASR (SherpaSpeechToText, AudioSyncSettingsItems) z wcześniejszych commitów innego wątku — nie moje, nie ruszane.

## Pętla 6b (25.11 ~13:00) — uwagi właściciela do POPRAWKI.md, R15
- Właściciel: „1. dodaj cache; 15. jak najkrócej wszędzie, 1h47; 17. nie rozumiem — są dwa filmy, a nie widać jednego?; dymek kl/s — usuń; wdrażaj dalej wszystkie zmiany”.
- R15 (commit 41d971398): pamięć rzędów Głównej `NiuniaRowsCache` (#1), „1h47” (#15), dymek kl/s wyłączony, karta tytułu z punktem fokusu w czasie przewijania + `rememberFocus` ignorowany pod kartą (R8 ✘, #44), YouTube: `playerShown` przy uruchomieniu odtwarzacza bazy (R13 ✘), dubbing AI bez `lang` po tidy (#43), gatunki po polsku w karcie/banerze (#45). #17: dedup tylko nazwa+rok+rodzaj (Dunkirk 1958 widoczny) — wyjaśnione w POPRAWKI.md. #46 to nie błąd (oznaczenie 24 h).
- Build `uxr7` przerwany po 19 min (13:08, swap 5,8 GB). Build `uxr8` (R14+R15) start ~13:20, `/tmp/uxr8.log`.
- W czasie builda `uxr8` (na `uxr6`): #27 ikona „x” przy „Usuń z »Oglądaj dalej«” ✔ (168); Biblioteka — fokus na 1. plakacie ✔ (log); Wstecz z odtwarzacza Live TV → oglądany kanał ✔ (log).
| 47 | Opcje „Oglądaj dalej” dla filmu YouTube: „Wybierz źródło” i „Szczegóły filmu” (ścieżki filmów z dodatków, bez sensu dla YouTube) (168) | do następnej paczki |
- `uxr8` (R14+R15, commit 41d971398): BUILD SUCCESSFUL 11 min 11 s, `.stage/apk/uxr8-v7a.apk` md5 `bc5d6725757642b1845dcc993ec53564`, zainstalowany 13:24 (md5 zgodny), `compile -m speed`.
- Na `uxr8`: „1h47” ✔ (170); pamięć rzędów zapisuje (log „zapisano 4 rzędów”) ✔; YouTube z „Oglądaj dalej” → „Polish · original” ✔ i Wstecz → Główna z fokusem na tym filmie ✔ (173); karta tytułu: fokus nie ucieka przy ► ✔ (log), ALE Wstecz → nadal 1. plakat, rząd nieprzewinięty, ◄ nie otwiera menu ✘ (172). Log „karta tytułu: Wstecz → plakat 3 w rzędzie” jest, a ekran nie — podejrzenie: po zamknięciu nakładki rząd/LazyRow tworzony od nowa (inny `st`), a fokus trafia na nieaktywny węzeł. Następny krok: zalogować `st.firstVisibleItemIndex` po scrollToItem i `isAttached`/hash `st`, sprawdzić, czy pagerOverlay przy hero ≠ null zmienia drzewo (rows() w innym Box).
- R16 (#47, commit po uxr8): opcje YouTube w „Oglądaj dalej” — w następnym buildzie.

## Pętla 7 (25.11 13:40, nowy wątek) — #44 i R16
- #44 przyczyna (logi `ForkFocus` na `uxr8`): Wstecz 15.711 → 15.751 fokus na 1. plakat (Compose po usunięciu nakładki z fokusem sam daje fokus lewemu górnemu elementowi) → `rememberFocus` nadpisuje cel → efekt `cardReturn` (15.817) daje fokus znów 1. plakatowi. Poprawka (commit 3d83b7835): przy powrocie na inny plakat karta zostaje, rząd przewija się i daje fokus, dopiero potem `pager = null`; zabezpieczenie 0,8 s.
- Build `uxr9` (R16 + #44, commit 3d83b7835): BUILD SUCCESSFUL 14:01 (17 min, testy Niuni OK), `.stage/apk/uxr9-v7a.apk` md5 `2165bdf9b2cd220e277ad6bca9bd5397`. Instalacja czeka na wolny JMGO.
- Na `uxr8` w czasie builda: gatunki po polsku na banerze i w karcie (#45) ✔; Szukaj: klawiatura a–z bez polskich liter (#36/R14) ✔; „Narzędzia” w System › Pomoc z opisem „Szukaj w telewizji, Odkrywaj, przykłady poleceń głosowych” (#40) ✔; przełącznik „Pamiętaj rzędy” w Wygląd › Ekran główny ✔ (182). #41 (wyłączony przełącznik na białym wierszu) — nie znalazłem wyłączonego (180, 181 włączone), ?.
- 13:50 właściciel przejął JMGO („napiszę gdy będzie wolne”) — testy wstrzymane.
- 14:03 właściciel: „kontynuuj” — `uxr9` zainstalowany (md5 `2165bdf9…` zgodny, compile speed).
- Na `uxr9`: #44 nadal ✘ — rząd przewija się (pierwszy widoczny 2), ale fokus na 1. plakacie; w logcat `System.out: FocusRequester is not initialized` (Compose 1.11 nie rzuca wyjątku, tylko wypisuje) → `restoreFocus` nie był przypięty do plakatu 3. Poprawka 2 (e44b34db5): plakat z karty ma własny `returnFocus`, `requestFocus(Enter)` ponawiane do 10 klatek, log z numerem klatki.
- Na `uxr9` ✔: R16 (#47: okno YouTube „Wznów od 0:34 / Oznacz jako obejrzane / Usuń”); Wstecz ze strony serialu → fokus na tym serialu w „Oglądaj dalej”; Live TV › Szukaj a–z, „wia” → „Wiadomości”, „Wiarołom”; film bez dymka kl/s (184); Wstecz z filmu → Główna z fokusem na tym filmie.
- #48 (183): „Szczegóły serialu” z „Oglądaj dalej” (Następny: S7 E6) → strona pokazuje S1 E1 i „▶ 84m” (wznów S1 E1). Wybór odcinka to baza (`resolveHeroPlaybackVideo`, `nextToWatch` z postępem S1E1) — bez zmian, do decyzji właściciela.
- #49: wyjście z zatrzymanego filmu = 3× Wstecz (ekran pauzy bazy → Wstecz pokazuje panel → Wstecz chowa → Wstecz wychodzi). Poprawka w skórce (commit po e44b34db5): `NiuniaPlayerBack.controller` (słaba referencja) — Wstecz na ekranie pauzy: `cancelPauseOverlay()` + `hideControls()`. ▼ w czasie filmu nic nie pokazuje (OK pokazuje panel i pauzuje) — tak w bazie, bez zmian.
- Build `uxr10` (#44 poprawka 2 + #49) start 14:21, `/tmp/uxr10.log`, zakładka c2.
- W czasie builda `uxr10` (na `uxr9`): Biblioteka — fokus na 1. plakacie ✔, ▲ z 3. plakatu → „Historia” zamiast wybranej „Wszystko” (#50, poprawione 79836e210 — NIE w `uxr10`, w następnym buildzie); YouTube otwiera się z fokusem na „Główna” ✔; #41 ✔ (188, „Kalendarz w menu” wyłączony na białym wierszu).
