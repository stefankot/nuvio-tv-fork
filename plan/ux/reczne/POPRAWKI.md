# Poprawki z testów ręcznych na projektorze JMGO (24–25.11.2026)

Jak czytać: **✔** — sprawdzone na projektorze, działa · **✘** — sprawdzone, nie działa · **?** — jeszcze nie sprawdzone na projektorze.
Paczki poprawek mają nazwy R1…R14. Na projektorze była `uxr13` (sprawdzona 26.11 po południu); powstaje `uxr14` (#67–#72). `uxr7` przerwana (za długo się kompilowała).
Zdjęcia ekranów (numery w nawiasach) leżą w `plan/ux/reczne/`.

## Ekran główny
| # | Co było nie tak | Co zmieniłem | Stan |
|---|---|---|---|
| 1 | Gdy serwer katalogów odmawia („za dużo zapytań”), rzędy były puste, bez słowa | Komunikat, przycisk „Spróbuj ponownie” i 3 ponowienia same (po 20, 40 i 60 s). **R15 (Twoja uwaga „dodaj cache”):** rzędy zapisują się na dysku; gdy serwis nie odpowie, pokazują się ostatnio wczytane (przełącznik „Pamiętaj rzędy” w Wygląd › Ekran główny, domyślnie włączony) | ? `uxr8` |
| 2 | Bez dużego baneru tytuł „Oglądaj dalej” wchodził pod przycisk „Główna” | Większy odstęp | ? |
| 3 | Bez baneru po starcie i po wygaszaczu nic nie było zaznaczone | Zaznaczenie na pierwszym elemencie | ? |
| 4 | Wygaszacz: zegar zasłaniał logo filmu | Zegar i data w prawym dolnym rogu | ? |
| 14 | Dwa rzędy „TMDB Popular” nie do odróżnienia | Dopisek „· filmy” / „· seriale” | ✔ |
| 15 | Karta tytułu pisała „1h47min”, baner „1 godz. 47 min” | **R15 (Twoja uwaga „jak najkrócej”):** wszędzie „1h47” | ? `uxr8` |
| 16 | Po powrocie z karty tytułu zaznaczenie skakało na pierwszy plakat | Wraca na plakat, który był oglądany (poprawione drugi raz w R8). **R17 (25.11):** znaleziona przyczyna — po zniknięciu karty telewizor sam zaznaczał lewy górny plakat. Teraz karta znika dopiero, gdy zaznaczony jest już oglądany plakat | **✘** w `uxr9`: rząd przewija się już na oglądany tytuł, ale zaznaczenie wciąż na 1. plakacie (plakat nie był jeszcze gotowy na zaznaczenie). Poprawione drugi raz — ? `uxr10` |
| 28 | Po wyjściu z filmu przycisk „Główna” nachodził na tytuł rzędu | Przycisk chowa się, gdy jesteś w rzędach | ✔ |
| 29 | Po wyjściu z filmu zaznaczona karta była poza ekranem, ◄ nie działało | Rząd przewija się na początek | ✔ |

## Szukaj
| # | Co było nie tak | Co zmieniłem | Stan |
|---|---|---|---|
| 5 | „dun” → najpierw „Muriel”, a „Diuna” dalej | Najpierw tytuły, które zaczynają się od wpisanych liter | ✔ |
| 6 | ◄ z lewej kolumny klawiatury nic nie robiło | ◄ otwiera menu | ✔ |
| 17 | „Dunkirk” dwa razy | Ukryty jest tylko **ten sam film podany przez dwa dodatki** (ta sama nazwa, ten sam rok, ten sam rodzaj). Dwa różne filmy o tej samej nazwie są widoczne oba: „Dunkirk” z 2017 i „Dunkirk” z 1958 r. (zdjęcie 93) | ✔ |
| 36 | Klawiatura bez polskich liter | Dodane ą…ż (R11), potem **usunięte na Twoje polecenie (R14)**. Wpisanie „l” znajduje też „ł”, „a” znajduje „ą” | R11 ✔, R14 ✔ (25.11: same litery a–z) |

## Telewizja (Live TV)
| # | Co było nie tak | Co zmieniłem | Stan |
|---|---|---|---|
| 7 | Okno „kanał nie odpowiada” było przezroczyste, przebijał spod niego ekran | Ciemne tło | ✔ |
| 8 | Kafelki na górze proponowały kanały, które nie działają | Bez niedziałających kanałów | ✔ |
| 18 | W „Kraj i język” Polska była za USA | Polska i polski na górze | ✔ |
| 19 | Przycisk „Kraj i język” nie mówił, co wybrano | Pokazuje nazwę wybranego kraju lub języka | ✔ |
| 20 | Wstecz z listy dni → zaznaczenie na kanale, OK włączało archiwum | Wraca na „Dziś” | ✔ |
| 25 | Zaraz po wejściu napis „te kanały nie mają programu TV”, choć program się wczytywał | Napis dopiero po 20 s bez programu | ? |
| 32 | ◄ z „Szukaj” (górny pasek) skakało na kafelek | ◄ otwiera menu | ✔ (25.11) |
| 33 | Po „Następny kanał” Wstecz wracało na pierwszy kanał | Wraca na kanał ostatnio oglądany | ? (25.11 nie dało się sprawdzić: kanały 1–5 dostawcy mają tę samą nazwę) |
| 34 | „Odkrywaj”: 4 razy „Kuchenne rewolucje”, niedziałające kanały w „Dla ciebie” | Jeden kafelek na program i godzinę, bez niedziałających kanałów | ✔ (25.11, zdjęcie 161) |
| 35 | Wstecz z „Odkrywaj” / „Szukaj” → zaznaczenie na „Wszystkie” | Wraca na przycisk, z którego weszłaś/wszedłeś | ✔ (25.11) |

## Biblioteka
| # | Co było nie tak | Co zmieniłem | Stan |
|---|---|---|---|
| 9, 22 | Przez sekundę ekran „Biblioteka pusta”, potem zaznaczenie na złej zakładce | W czasie wczytywania samo tło, zaznaczenie na pierwszym plakacie | ✔ |
| 23 | ◄ z pierwszego plakatu nic nie robiło | ◄ otwiera menu | ✔ |

## Strona filmu lub serialu
| # | Co było nie tak | Co zmieniłem | Stan |
|---|---|---|---|
| 10 | „Odtwórz S1, O1” obok „S1, E1” | Wszędzie „S1, E1” | ✔ |
| 11 | W opisie przypisy z Wikipedii, np. „Europe.[1]” | Przypisy usuwane | ✔ |
| 12 | Odcinki z nazwą zastępczą „Episode 2” | Nazwa zastępcza ukryta | ✔ |
| 24 | W opisie „S1, E1 · Odcinek 1:” | Bez nazwy zastępczej | ? |
| 27 | „Usuń z »Oglądaj dalej«” bez ikony | Ikona „x” | ✔ (25.11, zdjęcie 168) |

## Profile
| # | Co było nie tak | Co zmieniłem | Stan |
|---|---|---|---|
| 26, 37 | Dwa profile „Stef+Patryk (K…” ucięte, nie do odróżnienia | Imię w dwóch liniach | ✔ (25.11, zdjęcie 147) |

## YouTube
| # | Co było nie tak | Co zmieniłem | Stan |
|---|---|---|---|
| 13 | ◄ na lewym pasku ikon nic nie robiło | ◄ otwiera menu | ✔ |
| 38 | Film z „Oglądaj dalej” na Głównej: Wstecz prowadził do zakładki YouTube | Powrót na Główną (R13) | **✘** — nadal zakładka YouTube (25.11, zdjęcie 151) |
| 43 | **Duży błąd (Twoje zgłoszenie 25.11):** film z polskiego kanału gra z angielskim dubbingiem automatycznym i nie da się przełączyć na polski | Nie poprawiałem (polecenie: na razie bez poprawek). Przyczyna znaleziona: część forka, która porządkuje ścieżki dźwięku YouTube, z dwóch ścieżek zostawia jedną i bierze angielski dubbing, a oryginał po polsku wyrzuca (log: „audio sets 2 -> 1”, wybrany dźwięk „English · auto-dubbed”) | do poprawy |

## Ustawienia
| # | Co było nie tak | Co zmieniłem | Stan |
|---|---|---|---|
| 21 | „Rozmiar napisów —” bez wartości | Pokazuje „Małe / Średnie / Duże” | ✔ |
| 30 | „Kraj kanałów” bez wartości (u Ciebie jest „Niemcy”) | Wartość widać przy pozycji. Samego ustawienia nie zmieniałem | ✔ |
| 40 | „Pomoc i dziennik błędów” otwierało ekran „Narzędzia” (pomocy ani dziennika nie ma) | Pozycja nazywa się „Narzędzia” i ma opis tego, co jest w środku | ✔ (25.11: opis „Szukaj w telewizji, Odkrywaj, przykłady poleceń głosowych”) |
| 41 | Wyłączony przełącznik znikał na zaznaczonym (białym) wierszu | Ciemny tor, przełącznik widać | ✔ (25.11, zdjęcie 188: „Kalendarz w menu”) |
| 42 | „Na co masz ochotę?” — pusty ekran, dopóki nie naciśniesz OK | Od razu pokazuje pierwszy nastrój („Lekkie na dziś”) | R14, ? w `uxr8` |

## R15 (25.11, po Twoich uwagach „wdrażaj dalej wszystkie zmiany”) — wersja `uxr8` jest na projektorze (md5 zgodne)
| # | Co poprawione | Stan |
|---|---|---|
| 1 | Pamięć rzędów Głównej, gdy serwis nie odpowiada (wyżej) | zapis działa ✔ (log), podmiana przy błędzie czeka na błąd serwisu |
| 15 | Czas „1h47” zamiast „1 godz. 47 min” | ✔ (zdjęcie 170) |
| — | Dymek „24 kl/s przy 60 Hz” w odtwarzaczu usunięty | ✔ (25.11, zdjęcie 184) |
| 16 / 44 | Karta tytułu: przy przewijaniu kart zaznaczenie nie spada już na plakat pod kartą | **✘** — przewijanie poprawione, ale po Wstecz nadal pierwszy plakat i ◄ nie otwiera menu (zdjęcie 172); przyczyna głębiej, w następnej paczce |
| 38 | YouTube z „Oglądaj dalej”: Wstecz wraca na Główną | ✔ (zdjęcie 173) |
| 43 | YouTube: dubbing AI nie włącza się sam — gra oryginał (np. polski); dubbing zostaje w menu dźwięku do ręcznego wyboru | ✔ (log: „Polish · original”) |
| 45 | Karta tytułu i baner: gatunki po polsku, jak w rzędach | ✔ (25.11: „Animacja · Familijny”) |
| 47 | Okno opcji filmu YouTube w „Oglądaj dalej”: bez „Od początku”, „Wybierz źródło”, „Szczegóły” | ✔ (25.11: „Wznów od 0:34”, „Oznacz jako obejrzane”, „Usuń”) |

## Nowe znaleziska 25.11
| # | Co jest nie tak | Zdjęcie |
|---|---|---|
| 39 | „Co mogę powiedzieć” — goła czarna lista, inny wygląd niż reszta | 130 |
| 44 | Główna: po zamknięciu karty tytułu ◄ z pierwszego plakatu w rzędzie nie otwiera menu (z dużego baneru działa) | 156, 157 |
| 45 | Karta tytułu „Shetland”: gatunki i opis po angielsku („Drama · Crime”), a w rzędzie „Dramat” | 152 |
| 46 | Live TV: kafelki na górze znów pokazują RT i CNBC — to nie błąd: oznaczenie „niedziałający” trwa 24 godziny i wygasło; po pierwszej nieudanej próbie kanał znowu znika | 158 |

## R17–R18 (25.11 po południu) — w wersji `uxr10`
| # | Co było nie tak | Co zmieniłem | Stan |
|---|---|---|---|
| 16 / 44 | Wstecz z karty tytułu wracało na 1. plakat rzędu, a ◄ potem nie otwierało menu | Karta znika dopiero, gdy zaznaczony jest oglądany plakat; plakat ma własny „uchwyt” zaznaczenia (R17) | ✔ powrót na oglądany plakat (25.11, `uxr10`); ◄ → menu poprawione w `uxr11` |
| 49 | Zatrzymany film: żeby wyjść, trzeba było 3× Wstecz — pierwsze tylko zamieniało ekran pauzy na panel, wyglądało jak „nic się nie stało” | Wstecz na ekranie pauzy od razu go chowa; wyjście 2× Wstecz (R18) | ✔ (25.11, `uxr10`) |
| 50 | Biblioteka: ▲ z plakatu zaraz po wejściu szło na zakładkę „Historia” (najbliższą), a nie na wybraną „Wszystko” | ▲ idzie na wybraną zakładkę (R18) | ✔ (25.11, `uxr11`) |
| 48 | Serial otwarty z „Oglądaj dalej” („Następny: S7 E6”) pokazuje S1 E1 i „▶ 84m” (wznowienie starego, niedokończonego S1 E1) (zdjęcie 183) | Nie zmieniałem — odcinek wybiera główna aplikacja, nie skórka. Do Twojej decyzji: czy na stronie serialu ma być zawsze odcinek z „Oglądaj dalej” | — |

## Odtwarzacz telewizji (IPTV) i przewodnik — 25.11, Twoje polecenie „zoptymalizuj focusy i przejścia” (wersja `uxr11`)
| # | Co było nie tak | Co zmieniłem | Stan |
|---|---|---|---|
| 51 | Karta „Program jest w archiwum — OK” przełączyła „Świat według Kiepskich” na **wczorajszy, inny odcinek** | Archiwum proponowane tylko, gdy to na pewno ten sam odcinek | ? (karta nie pojawiła się w czasie testów) |
| 52 | OK na kanale w przewodniku: „nie ma w archiwum” zamiast włączyć to, co leci (dwa programy nachodziły na siebie o 21:30) | Włącza się program, który leci teraz | ✔ (25.11, Polsat 2) |
| 53 | W przewodniku ramka stała na „Kuchenne rewolucje”, a OK włączał poranny program; ► chodził po porannych | Ramka i OK wskazują zawsze ten sam program | ✔ (25.11, Nowa TV 22:00) |
| 54 | Pod odtwarzaczem kafelki „Nowa TV · 21:00” dwa razy — nie wiadomo, z którego dnia | Dopisany dzień: „wczoraj 21:00” | ✔ (25.11, zdjęcia 199, 200: „pon 22:00”, „wczoraj 23:00”) |
| 55 | Po pauzie i Wstecz zostawał zamrożony obraz bez znaku pauzy | Wstecz przy pauzie wznawia obraz i chowa opis | ✔ (25.11, zdjęcie 197) |
| 44 | ◄ z pierwszego plakatu rzędu na Głównej nie otwierało menu | ◄ przy lewej krawędzi otwiera menu | ✔ (25.11) |

## Odtwarzacz telewizji i przewodnik — pętla 9 (25.11 po południu, wersja `uxr12`)
Najważniejsze: **powrót z odtwarzacza do listy „Ulubione” trwał 7–11 sekund** (ekran stał, klawisze nie działały), a włączenie kanału — ok. 5 s. Znalazłem przyczyny pomiarem (próbkowanie programu): aplikacja na bieżąco przeliczała „obserwowane programy” dla wszystkich 59 tysięcy kanałów i zapisywała historię oglądania w chwili, gdy rysował się ekran. Teraz robi to w tle.

| # | Co było nie tak | Co zmieniłem | Stan |
|---|---|---|---|
| 56 | O pełnej godzinie lista „Ulubione” sama skurczyła się z 6 kanałów do 1, a zaznaczenie uciekło na „Szukaj” | Pierwsza poprawka trafiła obok: w „Ulubione” są też kanały, na których leci obserwowany program („Kiepscy”) — o pełnej godzinie program się kończy i kanał znikał. Teraz takie kanały zostają na liście, dopóki jesteś w tym filtrze | ✘ w `uxr12` → poprawione drugi raz; w `uxr13` ✔ powrót z kanału na ten sam kanał w 0,5 s (26.11). Znikanie o pełnej godzinie nie do sprawdzenia, dopóki nie leci obserwowany program |
| 57 | Nakładka odtwarzacza: długi napis „Poprzedni: Strong8K · AR: MUHAMMAD…” zabierał całe miejsce — **znikał tytuł programu i godziny** (zdjęcie 196) | Krótka nazwa kanału, przycisk ograniczony, tytuł zawsze widać | ✔ (zdjęcie 204: „Squawk on the Street”, „Poprzedni: FOOD NETWO…”); przy 5 przyciskach „Na żywo” było ucięte — poprawione w `uxr13`, ✔ 26.11 (zdjęcie 213: „Poprzedni: MUH…”, ↻, „Na żywo”, + i tytuł w dwóch liniach) |
| 58 | W szczegółach programu ▲ z góry nic nie robiło — wyjść można było tylko przez Wstecz | ▲ wraca do przycisków | ✔ (25.11, `uxr12`) |
| 59 | Co minutę w czasie oglądania obraz i pilot stawały na ok. 2 s; przy włączaniu kanału — na ok. 2 s | Zapis historii oglądania w tle | ✔ (25.11: 83 s oglądania bez zatrzymania) |
| 60 | Powrót z odtwarzacza do „Ulubione” 7–11 s, wejście w kanał ok. 5 s | Liczenie obserwowanych programów i kategorii w tle | ✔ powrót 0,9 s, wejście w kanał 2,5 s. Przy powrocie zaznaczenie wracało na „Ulubione” zamiast na kanał — poprawione w `uxr13` |
| 61 | ▲ z pierwszego kanału przewodnika szło na „Oglądaj dalej”, a wybrany był filtr „Ulubione” | ▲ wraca na wybrany filtr | ✔ `uxr13` (26.11, „Ulubione”) |
| 63 | ► pokazywał opis, ale zaznaczenie zostawało na przełączniku kanałów — następne ▼ **zmieniało kanał** zamiast zejść do rzędu odcinków | ► od razu zaznacza pierwszy przycisk, ▼ schodzi do odcinków | ✔ `uxr13` (26.11, zdjęcia 208–209) |
| 64 | Gatunek w opisie programu po angielsku („Comedy”) | Po polsku, jak w reszcie aplikacji | ✔ `uxr13` („Komediowe”, zdjęcie 208) |
| 65 | Rząd „inne odcinki” pokazywał 12 najstarszych (od poniedziałku), **bez dzisiejszych i następnych** (zdjęcie 200) | Najbliższe minione i do 4 następnych; ▼ zaznacza najbliższy | ✔ ▼ zaznacza najbliższy (26.11). Ale przy „Świecie według Bundych” rząd miał tylko 1 kafelek — patrz #70 |

## Odtwarzacz telewizji i przewodnik — pętla 10 (26.11, wersja `uxr14`)
Najpierw sprawdziłem `uxr13`: wszystkie poprawki z pętli 9 działają (#56, #57, #61, #63, #64, #65 ✔). Przełączenie kanału ▲▼: opis pojawia się po 0,1 s, obraz po ok. 1,5 s.

| # | Co było nie tak | Co zmieniłem | Stan |
|---|---|---|---|
| 67 | Wejście w kanał: ok. 0,6 s pracy ekranu na szukanie „poprzedniego kanału” na liście 59 tys. kanałów przy każdym odświeżeniu opisu i na porównywanie tytułów | Poprzedni kanał i porównania tytułów z pamięci | ? `uxr14` |
| 68 | Kanał oznaczony „Niedostępny” grał w podglądzie przewodnika, a znacznik zostawał (zdjęcie 205) | Gdy podgląd pokazuje obraz 3 s, znacznik znika | ? `uxr14` |
| 69 | Po wyborze dnia („Wczoraj”) zaznaczenie spadało na kafelek u góry — OK włączał kanał na żywo zamiast wybranego dnia (zdjęcie 206, potwierdzone 26.11) | Zaznaczenie na 1. kanale przewodnika, na godzinie wybranego dnia | ? `uxr14` |
| 70 | Pod „Światem według Bundych” rząd „inne odcinki” miał 1 kafelek — przewodnik pisze „Świat według Bundych 3: Can't dance…” (serial + nazwa odcinka) i szukane były tylko emisje tego samego odcinka (zdjęcia 209–210) | Odcinki rozpoznawane po nazwie serialu (bez nazwy odcinka i numeru sezonu) | ? `uxr14` |
| 71 | W tytułach dziwne znaki: „Can&#39;t dance, don&#39;t ask me” (zdjęcia 207–212) | Zamieniane na litery: „Can't dance, don't ask me” | ? `uxr14` |
| 72 | W programie z archiwum były tylko ↻ ▶ + — **nie było jak wrócić do transmisji na żywo** (zdjęcie 212) | Przycisk „Na żywo” także w archiwum | ? `uxr14` |
| — | Przy opóźnieniu linia u góry kończyła się w pół słowa: „· Nowa TV · na” (zdjęcie 213) | Wielokropek zamiast uciętego słowa | ? `uxr14` |

Następne (zapisane, wejdą w kolejną wersję): #70b „360° Świat bez granic (3)” — numer w nawiasie też rozbija serial (zdjęcie 215); #74 każdy klawisz w opisie odtwarzacza przeszukuje 59 tys. kanałów; #75 ze szczegółów programu do schowania opisu trzeba 3 × Wstecz — Wstecz z rzędu ma chować od razu.
Do Twojej decyzji / później: #73 kanał włączony z wyszukiwarki Live TV — po Wstecz wraca przewodnik (pigułka „Wszystkie”), a nie wyniki wyszukiwania.

Do Twojej wiadomości (bez zmian):
- 62: w archiwum, gdy opis jest schowany, ► przewija, a ▼ zmienia kanał bez pytania — tak ustaliłeś 21.11.
- 66: archiwum Nowej TV „Świat według Kiepskich, wczoraj 21:00” pokazuje inny serial (zdjęcie 202). To raczej błędnie przypisany program telewizyjny albo przesunięta godzina archiwum, nie fokus — sprawdzę osobno.

## Zauważone, ale świadomie niezmienione
- Teksty bazy po angielsku (np. „Finding stream source”) — zasada: bez tłumaczeń.
- Stare ekrany forka (Narzędzia, Co mogę powiedzieć, Rodzina, Na co masz ochotę) mają goły czarny wygląd, nie wygląd Niuni (zdjęcia 130, 132, 138, 141). To większa praca projektowa, nie drobna poprawka.
- Kraj kanałów „Niemcy” w Twoim profilu i pozycje testowe „autoplayqa-…” w danych — do Twojej decyzji.
- Strefa czasowa projektora to Chiny (zegar pokazuje 19:xx, gdy w Polsce jest 12:xx). To ustawienie projektora, nie aplikacji.

