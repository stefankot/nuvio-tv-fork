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
