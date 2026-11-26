# Odtwarzacz filmów i seriali (TorBox) — wyniki sprawdzenia

**Pętla 1 (26.11, 16:20–16:40): 5 poprawek aplikacji, sprawdzonych na projektorze: 0** — testy przerwane na Twoje polecenie (projektor zajęty). Poprawki są w wersji `uxr16` (budowana), sprawdzę je, gdy projektor będzie wolny.

Sprawdzone na projektorze JMGO, wersja `uxr15`, na 2 materiałach: film „Oppenheimer” (2023) i serial „Taskmaster” S1 O1.

## Twoje ustawienia, według których aplikacja wybiera plik

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

## Czy wybrany plik był najlepszy?

| Materiał | Co wybrała aplikacja | Ocena |
| --- | --- | --- |
| „Oppenheimer” (pierwsze włączenie) | 4K REMUX, Dolby Vision, 97 GB, **72 Mb/s**, gotowy w TorBox, dźwięk angielski, polskie napisy w pliku | ✘ Zgodny z ustawieniami, ale za ciężki: Twoje łącze daje ok. 40 Mb/s. Inny plik 4K REMUX (77 Mb/s) zatrzymywał się **10 razy na minutę** (łącznie 37 s czekania w ciągu 70 s). |
| „Oppenheimer” po poprawce (`uxr16`, wyliczone na tej samej liście) | 4K BluRay, Dolby Vision, 23 GB, **17 Mb/s**, gotowy w TorBox, dźwięk angielski i polski | ✔ Najlepszy obraz, który łącze uciągnie. Plik 4K 35 Mb/s grał na projektorze płynnie. |
| „Taskmaster” S1 O1 | „SmartPlay — Auto Verified” z Flix-Streams, opisany jako **4K**, a naprawdę **1080p** | ✘ Wpis „kliknij, aby sprawdzić” udawał 4K i wygrał z prawdziwymi plikami. Gra płynnie, ale bez nazwy pliku i bez napisów w pliku. |
| „Taskmaster” po poprawce (wyliczone) | 1080p WEB-DL, 1,7 GB, gotowy w TorBox | ✔ Prawdziwy najlepszy plik (4K tego serialu nie ma). |

## Odtwarzacz — co działa

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

## Co nie działało i co poprawiłem (w `uxr16`)

| # | Problem | Poprawka |
| --- | --- | --- |
| V1 | Film ruszył po 14,7 s, a 0,3 s później aplikacja uznała start za zbyt długi i sama przełączyła na inny, cięższy plik (który się zacinał). | Gdy obraz już jest i film gra, aplikacja nie zmienia pliku w ostatnich 2 s czekania. |
| V2 | **Menu „Jakość” nie da się obsłużyć pilotem**: okno się pokazuje, ale strzałki i OK działają na przycisku „Więcej” pod spodem. To samo w oknie „Ścieżka dźwiękowa”. Zdjęcie 04. | Okno otwarte z panelu zatrzymuje zaznaczenie u siebie. |
| V3 | Plik 4K REMUX (70–100 Mb/s) wybierany przed lżejszym plikiem 4K — za ciężki dla łącza. | W 4K najpierw plik bez REMUX, REMUX tuż za nim (nadal przed 1080p). Twoje obecne reguły przejdą na nową wersję same, raz; poprzednie zostają w kopii. |
| V4 | Wpis „SmartPlay… Click to verify & play” (Flix-Streams) udawał 4K i wygrywał. | Takie wpisy idą na koniec, razem z plikami niegotowymi. |
| V5 | Przy otwieraniu filmu aplikacja ok. 1000 razy w 78 s pytała serwer o polskie napisy z innego wydania, choć serwer każdorazowo odmawiał — w czasie startu filmu. | Po pierwszej odmowie przestaje pytać. |

## Zauważone, niepoprawione

- Okna „Jakość” i „Dźwięk” mają napisy po angielsku („Quality”, „sources”, „Hold OK on a language…”, „No sources”, „Volume boost”). Zasada: bez tłumaczeń — zostawione.
- Okno „Dźwięk” pokazuje japoński i koreański z dopiskiem „No sources”, a nie pokazuje polskiego lektora, który jest w innych plikach. Do zbadania w następnej pętli.
- Gdy przyciski na pauzie same znikną, pierwsze OK tylko je pokazuje, dopiero drugie wznawia film.
- Błąd odczytu przy cofaniu w pliku REMUX (patrz Przewijanie) — to kod odtwarzacza, nie forka; odtwarzacz sam się z niego podnosi.

## Nie sprawdzone (przerwane — projektor zajęty)

- Napisy AI, gdy polskich napisów nie ma: komunikat w rogu i anulowanie klawiszem Wstecz.
- Automatyczne włączenie następnego odcinka.
- Zmiana jakości po poprawce V2 i pozostałe poprawki `uxr16` na projektorze.
- Trzeci, czwarty i piąty materiał.

Uwaga: „Taskmaster” S1 O1 był włączony na Twoim profilu i przewinięty prawie do końca (do testu następnego odcinka), więc może być widoczny w „Oglądaj dalej” jako obejrzany albo z następnym odcinkiem. „Oppenheimer” wrócił do miejsca sprzed testu (zostało 179 min).
