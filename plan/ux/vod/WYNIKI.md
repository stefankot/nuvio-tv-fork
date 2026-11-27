# Odtwarzacz filmów i seriali (TorBox) — wyniki sprawdzenia

**Pętla 4 (26.11, 21:44–22:20; `uxr17`, potem `uxr18` zainstalowany): 3 nowe poprawki aplikacji (V7b, V9, V10 — w HEAD, niezbudowane, wejdą do wspólnego buildu z aktualizacją bazy), sprawdzona na projektorze 1 (V8 ✔). Znalezione 2 awarie aplikacji.**

## Pętla 4

| Co | Wynik |
| --- | --- |
| V8: „Taskmaster” S18 O3 | ✔ Start w 4,9 s, plik SELection 1080p WEB-DL 1,82 GB z TorBox (wydanie RAWR) = plik nr 1 wyliczony z Twoich ustawień, nie 360p. Napisy AI włączyły się same (brak polskich). |
| „The Last of Us” S1 O1 | ✘ Wybrany 4K BluRay DV **18,8 Mb/s** zamiast 28 Mb/s. Przyczyna: stara reguła „wolny pośrednik 4K” brała cały dodatek SELection za wolny i tasowała pliki 4K. Poprawka **V10** (reguła tylko dla MediaFusion). |
| ✘ **Awaria w trakcie filmu** („The Last of Us”, 4K) | Po ok. 20 s aplikacja się zamknęła: dopasowanie polskich napisów do mowy wczytywało model Whisper „base”, a w 32-bitowej aplikacji przy 4K zabrakło 200 MB ciągłej pamięci. Poprawka **V9**: przed wczytaniem modelu aplikacja sprawdza wolną pamięć; gdy brak — mniejszy model albo dopasowanie bez rozpoznawania mowy, bez awarii. |
| ✘ V7: ekran źródeł | **Nadal pada** przy „Wybierz źródło” z „Oglądaj dalej” (ta sama przyczyna co w pętli 2). Prawdziwy powód: pod przyciskiem „Ponów” trafiała funkcja filtra. Poprawka **V7b**. |
| V2c (okna Jakość, Dźwięk, Napisy z panelu) | Nie sprawdzone — `uxr18` zainstalowany dopiero o 22:15. |

Zmiany na Twoim profilu: „Taskmaster” S18 O3 ~1 min (jest w „Oglądaj dalej”), „The Last of Us” S1 O1 ~20 s (do awarii).


**Pętla 3 (26.11, 21:24–21:31, wersja `uxr17` na projektorze; przerwana — zapis stanu): 1 nowa poprawka aplikacji (V2c), sprawdzonych na projektorze: 0 (wejdzie do `uxr18`). Z poprawek `uxr17` działają V6 i migracja reguł v5; V2b sama nie wystarczyła; V7 i V8 jeszcze nie sprawdzone do końca.**

## Pętla 3 (`uxr17`)

| Co | Wynik |
| --- | --- |
| Reguły v5 po instalacji | ✔ „reguły autoplay v5: profil 1, zestaw original_subs: 6 → 6 reguł (stare w kopii)”. |
| V6: okno Dźwięk | ✔ Wiersze: „Original • Angielski” (2 ścieżki w pliku) i „**Polski — 16 sources**”, bez japońskiego i koreańskiego. Zdjęcie 15. |
| Zmiana dźwięku na polski w trakcie filmu | ✔ „F1”: wybór „Polski” w 2–3 s przełączył na inny plik 4K (WEB-DL DV 25,6 Mb/s) ze ścieżką „Lektor PL”, w tym samym miejscu filmu. Zdjęcie 16. Przy lektorze zostają też włączone polskie napisy. |
| V2b: okno Jakość | ✘ Nadal bez zaznaczenia. Prawdziwa przyczyna: panel „Więcej” w czasie animacji zamykania blokuje wyjście zaznaczenia i odrzuca jedyną prośbę okna Jakość. Poprawka **V2c** (w `uxr18`). |
| Okno Dźwięk z panelu | ✘ Raz z zaznaczeniem, raz bez (ta sama przyczyna w panelu „Audio, lektor i napisy”) — też V2c. |
| V7: ekran źródeł | Częściowo: jedno świeże „Odtwórz” („Doktor Strange”) bez awarii; trzeba jeszcze 2–3 startów. |
| V8: „Taskmaster” S18 O3 w 1080p | Nie sprawdzone (przerwane). |

**Moja pomyłka na Twoim profilu:** gdy okno Dźwięk było bez zaznaczenia, kolejne klawisze wysłane bez sprawdzenia, gdzie jest zaznaczenie, trafiły w rząd „Podobne” i włączyły „Doktor Strange” (ok. 1 min). Wyłączyłem go od razu, ale może być w „Oglądaj dalej”. „F1” przy następnym „Wznów” włączy plik z polskim lektorem (ostatnio grany).

**Pętla 2 (26.11, 20:15–20:55, wersja `uxr16` na projektorze): 4 nowe poprawki aplikacji (V2b, V6, V7, V8), sprawdzonych na projektorze: 0 — wejdą do wersji `uxr17`. Z 5 poprawek wersji `uxr16` na projektorze działają 4 (V2 w oknie Dźwięk, V3, V5, migracja reguł), 1 nie zadziałała (V2 w oknie Jakość → V2b); V1 i V4 nie miały okazji się pokazać.**

## Pętla 2 — najważniejsze

- **Aplikacja padła** przy pierwszym „Odtwórz” w filmie „F1” (ekran źródeł). Za drugim razem film ruszył. Poprawione (V7).
- **„Taskmaster” sezon 18 grał w 360p**, bo wpis „Dream 4K VOD” (nazwa serwisu w dodatku Flix-Streams) udawał 4K. Poprawione (V8): wygra plik 1080p z TorBox.
- **Napisy AI działają**: komunikat w lewym górnym rogu, po ok. 30 s polskie napisy na ekranie, Wstecz w czasie komunikatu anuluje i film gra dalej.
- **Następny odcinek** włączył się sam, z tego samego wydania.
- Okno **Jakość** nadal nie daje się obsłużyć pilotem (V2b), okno **Dźwięk** pokazywało japoński i koreański zamiast polskiego (V6).

## Czy wybrany plik był najlepszy? (pętla 2)

| Materiał | Co wybrała aplikacja | Ocena |
| --- | --- | --- |
| „F1” (2025), pierwsze włączenie | 4K BluRay, Dolby Vision, 26 GB, **22 Mb/s**, gotowy w TorBox, dźwięk angielski Atmos, polskie napisy w pliku | ✔ Dokładnie plik nr 1 wyliczony z Twoich ustawień. Start w 20 s, grał płynnie (0 doczytywań). Zdjęcie 10. |
| „Oppenheimer” (Wznów) | ten sam plik 4K 35 Mb/s co ostatnio | ✔ Wznów używa pliku, który grał poprzednio; start w 10 s, angielski dźwięk, polskie napisy z pliku. |
| „Taskmaster” S18 O1 i O2 | „**Dream 4K VOD** • S18E01 • MP4” z Flix-Streams — naprawdę **640×360** | ✘ Najgorszy obraz z dostępnych, start 20–24 s. Po poprawce V8 (wyliczone na tej samej liście): 1080p WEB-DL, 1,8 GB, gotowy w TorBox. |

Na liście źródeł „F1” dodatek SELection podsuwa też plik innego filmu („Apex (2026)”). Nie wygrał, bo był niżej — nie poprawiałem (to błąd dodatku).

## Napisy AI (pętla 2, „Taskmaster” S18 O1 — polskich napisów nie ma nigdzie)

| Co | Wynik |
| --- | --- |
| Włączenie samo, gdy brak polskich | ✔ 13 s po starcie: „Brak polskich napisów — tłumaczę je na polski (AI). Naciśnij Wstecz, aby anulować” w lewym górnym rogu, bez okna na środku. Zdjęcie 14. |
| Polskie napisy na ekranie | ✔ Po ok. 30 s: „A obok mnie siedzi człowiek, który w sekrecie wyznał mi,”. Zdjęcie 12. |
| Wstecz w czasie komunikatu | ✔ Tłumaczenie anulowane, wróciły napisy angielskie, film grał dalej; automat wyłączony dla tego odcinka. |
| Wstecz później (po ok. 80 s) | Zamyka film, jak zwykły Wstecz — tak ma być. |
| Następny odcinek | Napisy AI włączyły się same i dla O2; anulowałem Wsteczem (koszt). |
| Koszt | Przetłumaczone ok. 3 z 12 części jednego odcinka — kilka centów (dokładna kwota: Ustawienia › Napisy AI, „dziś wydano”). |

Uwagi (bez poprawki): komunikat o postępie („AI: tłumaczenie — całość za 3–4 min”) w prawym górnym rogu zasłania zegar (zdjęcia 11, 12); po anulowaniu zapytania wysłane już do AI kończą się jeszcze ok. 40 s w tle (ich koszt i tak jest naliczony).

## Odtwarzacz (pętla 2, `uxr16`)

| Funkcja | Wynik |
| --- | --- |
| Następny odcinek | ✔ Koniec S18 O1 → karta Niuni → O2 gra po 13 s, ten sam serwis/wydanie. |
| Więcej → Jakość | ✘ Okno się otwiera, ale zaznaczenie nie chodzi (fokus na całym ekranie). Przyczyna: po 3 s przyciski odtwarzacza się chowają, a wtedy odtwarzacz zabiera fokus oknu. Poprawka V2b. |
| Dźwięk → Ścieżka dźwiękowa | ✔ Okno trzyma zaznaczenie (V2 działa). ✘ Pokazywało japoński i koreański „No sources” zamiast polskiego — poprawka V6. |
| Ekran źródeł przy „Odtwórz” | ✘ Aplikacja padła raz („F1”); poprawka V7. |
| Mniej zapytań o napisy z innego wydania (V5) | ✔ 29 prób w całej sesji zamiast ok. 1000 w 78 s. |
| Reguły v4 na projektorze | ✔ Przeszły same po instalacji (kopia starych zachowana). |
| Przewijanie ◄ ►, wyjście, Wznów | ✔ |

## Nowe poprawki (wejdą do `uxr17`)

| # | Problem | Poprawka |
| --- | --- | --- |
| V2b | Okno „Jakość” bez zaznaczenia — pilot nic nie wybiera. | Odtwarzacz nie zabiera fokusu, gdy okno jakości jest otwarte; po jego zamknięciu fokus wraca na przyciski. |
| V6 | Okno „Dźwięk”: japoński i koreański „No sources”, brak polskiego. | Lista języków: oryginał, polski, angielski (polski lektor/dubbing z innych plików widoczny z liczbą źródeł). |
| V7 | Aplikacja padła przy wejściu na ekran źródeł. | Wpięcie ekranu źródeł Niuni ma własną grupę w pamięci ekranu — przy przerysowaniu nie myli przycisków. |
| V8 | „Dream 4K VOD” (360p) wygrywał jako 4K. | Reguły v5: „Dream 4K” i „4K VOD” to nie rozdzielczość. Twoje reguły przejdą na v5 same, raz; poprzednie zostają w kopii. Sprawdzone na zapisanych listach 5 materiałów: zmienia się tylko ten jeden wybór. |

## Zmiany na Twoim profilu w pętli 2

„F1” był włączony ok. 40 s i „Taskmaster” S18 O1 przewinięty do końca (O2 grał ok. 30 s) — mogą być w „Oglądaj dalej”. „Oppenheimer” wrócił do 2:28 (było ok. 2:15).

## Pętla 1 (26.11, 16:20–16:40, wersja `uxr15`)

**Pętla 1 (26.11, 16:20–16:40, wersja `uxr15`): 5 poprawek aplikacji, sprawdzonych na projektorze: 0** — testy przerwane na Twoje polecenie (projektor zajęty). Poprawki są w wersji `uxr16` (zbudowana 16:58, test reguł zaliczony, jeszcze nie zainstalowana); sprawdzę je, gdy projektor będzie wolny.

Sprawdzone na projektorze JMGO, wersja `uxr15`, na 2 materiałach: film „Oppenheimer” (2023) i serial „Taskmaster” S1 O1.

### Twoje ustawienia, według których aplikacja wybiera plik

Odczytane z projektora 26.11 (profil 1).

| Ustawienie (tak jak w aplikacji) | Wartość | Co to znaczy przy wyborze pliku |
| --- | --- | --- |
| Oglądanie → Włączanie filmu → **Polski dźwięk czy jakość** | **Oryginał z napisami** | Najpierw najlepszy obraz: 4K, potem 1080p, potem 720p, potem reszta. Język dźwięku w nazwie pliku nie ma znaczenia. Od `uxr16`: w 4K najpierw plik lżejszy niż REMUX (patrz niżej). |
| (ta sama odpowiedź) — pliki zakazane | nagrania z kina (CAM, TS, TC) | Nigdy nie wybierane. |
| (ta sama odpowiedź) — pliki niegotowe | na samym końcu | Plik, którego TorBox nie ma jeszcze u siebie (⬇️, ⏳, „caches on play”), tylko gdy nie ma nic innego. |
| Oglądanie → **Wybór źródła** | automatycznie, według listy reguł | Z kilku plików tego samego szczebla wygrywa ten, który stoi wyżej na liście dodatków. |
| Kolejność dodatków ze źródłami (włączone) | SELection → Comet TB → SELection (PL) → Torrentio TB → Flix-Streams → NoTorrent → TorrentClaw → PenguPlay | Remis w szczeblu rozstrzyga ta kolejność, a w obrębie dodatku jego własna kolejność. |
| Czas czekania na dodatki | 15 s | Po 15 s wybór z tego, co już przyszło. |
| Seriale → **Następny odcinek** | włączone, to samo wydanie co poprzedni odcinek | Następny odcinek z tej samej grupy wydania, jeśli jest. |
| Napisy i dźwięk → **Język i dźwięk** | angielski, potem polski | Ścieżka dźwięku w pliku. |
| Napisy i dźwięk → **Napisy: język** | polski, potem angielski | Napisy wbudowane w plik przed napisami z dodatków. |
| Napisy i dźwięk → **Napisy AI** | gdy brak dobrych polskich: włączają się same | Limit 10 $ dziennie, 3 $ na tytuł. |
| Oglądanie → **Gdy film się zatnie** | włączone | Gdy film nie ruszy w 15 s, aplikacja sama próbuje następnego pliku (najwyżej 3 próby). W trakcie filmu nie zmienia pliku. |

Projektor ma sprzętowe dekodery Dolby Vision, HEVC (H.265) i AV1, więc pliki z Dolby Vision i HDR nie są wykluczane.

### Czy wybrany plik był najlepszy?

| Materiał | Co wybrała aplikacja | Ocena |
| --- | --- | --- |
| „Oppenheimer” (pierwsze włączenie) | 4K REMUX, Dolby Vision, 97 GB, **72 Mb/s**, gotowy w TorBox, dźwięk angielski, polskie napisy w pliku | ✘ Zgodny z ustawieniami, ale za ciężki: Twoje łącze daje ok. 40 Mb/s. Inny plik 4K REMUX (77 Mb/s) zatrzymywał się **10 razy na minutę** (łącznie 37 s czekania w ciągu 70 s). |
| „Oppenheimer” po poprawce (`uxr16`, wyliczone na tej samej liście) | 4K BluRay, Dolby Vision, 23 GB, **17 Mb/s**, gotowy w TorBox, dźwięk angielski i polski | ✔ Najlepszy obraz, który łącze uciągnie. Plik 4K 35 Mb/s grał na projektorze płynnie. |
| „Taskmaster” S1 O1 | „SmartPlay — Auto Verified” z Flix-Streams, opisany jako **4K**, a naprawdę **1080p** | ✘ Wpis „kliknij, aby sprawdzić” udawał 4K i wygrał z prawdziwymi plikami. Gra płynnie, ale bez nazwy pliku i bez napisów w pliku. |
| „Taskmaster” po poprawce (wyliczone) | 1080p WEB-DL, 1,7 GB, gotowy w TorBox | ✔ Prawdziwy najlepszy plik (4K tego serialu nie ma). |

### Odtwarzacz — co działa

| Funkcja | Wynik |
| --- | --- |
| Zmiana źródła w trakcie filmu (Więcej → Źródła) | ✔ Lista 235 źródeł, obecne oznaczone „teraz odtwarzane”. Nowy plik ruszył po 11 s **w tym samym miejscu** filmu, z angielskim dźwiękiem i polskimi napisami. Zdjęcia 02, 03. |
| Napisy wbudowane przed napisami z dodatków | ✔ Wybrane „Polish • wbudowane”, na liście przed 76 napisami z dodatków; na górze „Napisy AI”. Zdjęcie 05. |
| Napisy z dodatku dopasowane do mowy | ✔ „Taskmaster”: polskie napisy z dodatku same dopasowane do dialogów. Zdjęcie 09. |
| Napisy AI nie włączają się, gdy są dobre polskie | ✔ Na obu materiałach słusznie się nie włączyły (nic nie zapłacono). |
| Wybór dźwięku | ✔ Angielski (oryginalny) wybrany sam, zgodnie z ustawieniem. Zdjęcie 07. |
| Pauza i wznowienie (OK) | ✔ Zdjęcie 08. |
| Przewijanie (◄ ►) | ✔ Działa; raz przy cofaniu plik REMUX dał błąd odczytu, odtwarzacz sam spróbował ponownie i po ok. 3 s grał dalej od właściwego miejsca. |
| Wyjście (Wstecz) i „Wznów” | ✔ Pierwszy Wstecz chowa przyciski, drugi zamyka film. „Wznów” włączył ten sam plik od zapisanego miejsca w ok. 3 s. |

### Co nie działało i co poprawiłem (w `uxr16`)

| # | Problem | Poprawka |
| --- | --- | --- |
| V1 | Film ruszył po 14,7 s, a 0,3 s później aplikacja uznała start za zbyt długi i sama przełączyła na inny, cięższy plik (który się zacinał). | Gdy obraz już jest i film gra, aplikacja nie zmienia pliku w ostatnich 2 s czekania. |
| V2 | **Menu „Jakość” nie da się obsłużyć pilotem**: okno się pokazuje, ale strzałki i OK działają na przycisku „Więcej” pod spodem. To samo w oknie „Ścieżka dźwiękowa”. Zdjęcie 04. | Okno otwarte z panelu zatrzymuje zaznaczenie u siebie. |
| V3 | Plik 4K REMUX (70–100 Mb/s) wybierany przed lżejszym plikiem 4K — za ciężki dla łącza. | W 4K najpierw plik bez REMUX, REMUX tuż za nim (nadal przed 1080p). Twoje obecne reguły przejdą na nową wersję same, raz; poprzednie zostają w kopii. |
| V4 | Wpis „SmartPlay… Click to verify & play” (Flix-Streams) udawał 4K i wygrywał. | Takie wpisy idą na koniec, razem z plikami niegotowymi. |
| V5 | Przy otwieraniu filmu aplikacja ok. 1000 razy w 78 s pytała serwer o polskie napisy z innego wydania, choć serwer każdorazowo odmawiał — w czasie startu filmu. | Po pierwszej odmowie przestaje pytać. |

### Zauważone, niepoprawione

- Okna „Jakość” i „Dźwięk” mają napisy po angielsku („Quality”, „sources”, „Hold OK on a language…”, „No sources”, „Volume boost”). Zasada: bez tłumaczeń — zostawione.
- Okno „Dźwięk” pokazuje japoński i koreański z dopiskiem „No sources”, a nie pokazuje polskiego lektora, który jest w innych plikach. Do zbadania w następnej pętli.
- Gdy przyciski na pauzie same znikną, pierwsze OK tylko je pokazuje, dopiero drugie wznawia film.
- Błąd odczytu przy cofaniu w pliku REMUX (patrz Przewijanie) — to kod odtwarzacza, nie forka; odtwarzacz sam się z niego podnosi.

### Nie sprawdzone w pętli 1 (sprawdzone w pętli 2 — wyżej)

- Napisy AI, gdy polskich napisów nie ma: komunikat w rogu i anulowanie klawiszem Wstecz.
- Automatyczne włączenie następnego odcinka.
- Zmiana jakości po poprawce V2 i pozostałe poprawki `uxr16` na projektorze.
- Trzeci, czwarty i piąty materiał.

Uwaga: „Taskmaster” S1 O1 był włączony na Twoim profilu i przewinięty prawie do końca (do testu następnego odcinka), więc może być widoczny w „Oglądaj dalej” jako obejrzany albo z następnym odcinkiem. „Oppenheimer” wrócił do miejsca sprzed testu (zostało 179 min).
