# Funkcje Kicii (z kodu) a nowy fork — 18.11.2026

Źródło: najnowsza Kicia, `~/Desktop/nuvio-tv-kicia`, gałąź `feat/auto-odtwarzanie`, commit `34c80c6` (15.11). Porównanie: wszystkie pliki, które Kicia dodała albo zmieniła względem swojej bazy (Reshaped `e440349`, 08.11), sprawdzone w kodzie, nie w notatkach.

Oznaczenia: ✅ jest w nowym forku · 🎨 przyjdzie ze skórką Kicia (kod przeniesiony 18.11, niekompilowany) · ❌ brak w nowym forku

## 1. Wygląd i nawigacja
- 🎨 Górny pasek zakładek (Szukaj, Home, Live TV, YouTube, Biblioteka) z rozmyciem pod spodem; Wstecz z treści wraca na pasek. *Kolejność zakładek w menu i „Wstecz na pasek” w trasach jeszcze nie przeniesione.*
- 🎨 Ekran główny Kicii: hero opisujące kartę pod fokusem, półki, „YouTube dla Ciebie”, kafle kanałów, „Nowe odcinki” z przewodnika, karty wznowienia przerwanych programów TV, szkielet przy pierwszym starcie, zakładki wczytywane w tle.
- 🎨 Szukaj: mikrofon, polska klawiatura ekranowa, „Ostatnio szukane”, wyniki ze WSZYSTKICH dodatków naraz na jednej półce (filmy, seriale oraz kanały i YouTube, jeśli dają je dodatki), Odkrywaj (Typ/Katalog/Gatunek) pod spodem.
- 🎨 Szczegóły filmu w układzie makiety: linia meta z oceną IMDb, linia „Gotowe” z plakietkami pliku, karty odcinków, ▼ z sezonu do odcinka, karty źródeł („4K · Dolby Vision · REMUX / TrueHD Atmos 7.1 · 70,8 GB”).
- 🎨 Biblioteka z podzakładkami i licznikiem; osobny ekran Historia.
- 🎨 „Kto ogląda?” z PIN-em jako cztery kółka; kreator „Skonfiguruj dla tego TV”.
- 🎨 Ustawienia z szyną makiety; ustawienia podstawowe (8 kategorii); kategoria „Nuvio Forge” (start z TV, ekran główny, asystent).
- 🎨 Wygaszacz z dużym zegarem; znaczek wersji w rogu; komunikaty; kolory ocen; czcionka Satoshi.
- 🎨 Odtwarzacz: sterowanie makiety (zegar, „Koniec o”, przewijanie pilotem z przyspieszaniem), przycisk napisów z postępem AutoSync, ratunek fokusu, panel „Źródło i jakość” (3 poziomy, lista po długim OK).

## 2. Live TV
- ✅ Start HLS od dowolnej klatki I, nie tylko IDR (kanały typu TVP 1 HD bez IDR wisiały na „Buforowanie…”), w odtwarzaczu i w podglądach na kartach. **Przeniesione 18.11** (commit 85f92bd18). Dla MPEG-TS nowy fork miał to już z Reshaped.
- 🎨 Strażnik kanału co sekundę: brak danych lub błąd 6 s → ponowny start; dane bez obrazu 3 s → komunikat; zerwany kanał startuje ponownie zamiast „Błąd odtwarzania”; okno „Kanał nie odpowiada”. (W nakładce Live TV, przeniesionej w etapie 3.)
- 🎨 Nagłówek przewodnika (ostatnio oglądane programy + wybór dnia), MiniGuide, szuflada „Info i odcinki”, legenda pilota, długie naciśnięcia strzałek (od początku, następny odcinek, szczegóły).
- 🎨 Drzewo kategorii z EPG (13 rodzin), widok programów kategorii z sortowaniem (Najnowsze, Polecane, Popularne, Na żywo, Wkrótce) i szukaniem, Ulubione programy.
- 🎨 Szukanie programów Live TV z klawiaturą abc/123.
- 🎨 Archiwum: wyścig emisji (pierwsza działająca kopia wygrywa), propozycja wcześniejszej emisji podczas oglądania na żywo, karta „Następny odcinek za 10 s” na końcu programu, Wstecz w archiwum pyta przed powrotem na żywo, zapping po 500 ms postoju.
- 🎨 Zrzuty kanałów i podgląd wideo na kartach.
- ❌ Szybszy start przewodnika: najpierw mały wycinek „teraz”, potem cały tydzień (pliki danych Live TV; wymaga dopasowania do wcześniejszych łatek).
- ❌ Pamięć przerwanych programów z archiwum (`TvResume`) i wznowienie z ekranu głównego.
- ✅ Logo kanałów (paczka epg.ovh), dziennik, obserwowane, przypomnienia, drugi przewodnik (przeniesione wcześniej jako moduł `fork/tv`).

## 3. Wybór źródła i odtwarzanie filmów (logika, nie wygląd)
- ❌ **Tryb automatyczny z presetami** (np. Zbalansowany, Najlepsza jakość): aplikacja czyta nazwy wydań (rozdzielczość, HDR/Dolby Vision, kodek, rozmiar, lektor/dubbing, stara synteza mowy na końcu), sama sprawdza dekodery i ekran urządzenia, zgodność tytułu, budżet łącza (z już pobranych danych), tryb dla słabych urządzeń; reguła widoczna jako tekst do poprawienia.
- ❌ **Nauka na wynikach**: dla każdego dodatku i usługi (np. Comet/TorBox) liczba prób i porażek wpływa na ranking.
- ❌ **Inteligentny ratunek**: pamięć złych źródeł, „zły plik” (ten sam plik z innego dodatku nie jest kolejną próbą), sprawdzenie długości pliku i dźwięku, kontrola wolnego startu, błąd ostateczny (serwer odmówił), przycisk „Spróbuj innego źródła” w „Więcej”, wznowienie tym samym plikiem.
- ❌ **Rozgrzewka przed Odtwórz**: adres pliku rozwiązywany i połączenie otwierane zawczasu.
- ❌ **Bufor dopasowany do pliku**: budżet bufora = bitrate pliku × 60 s, wznowienie po zacięciu z większym zapasem.
- ❌ **Awaryjne P2P**: gdy usługa debrid nie poda linku, cichy powrót do P2P.
- ❌ **Mózg jakości** (przyczyny problemów, poziomy, drabina następców, pamięć seansu) i zamiana jakości w locie na klatce kluczowej. Nowy fork ma politykę Echo bez tych dodatków.
- ❌ AVI/WMV/ASF odtwarzane przez mpv.
- ❌ Wygasły adres debrid (401/403/410) nie wraca z pamięci podręcznej.
- ✅ Wyścig źródeł, menu audio, intro, przełączanie źródła (Echo, przeniesione wcześniej); linia „Gotowe” (moduł `fork/sources`).

## 4. YouTube
- ✅ Prawie cała warstwa YouTube Kicii jest w nowym forku (z Nuvio One): SponsorBlock, czat na żywo, tłumaczenie napisów, napisy automatyczne, ścieżki dźwiękowe, podgląd miniatur, „następny film”, kolejność zakładek.
- ❌ Różnice w ekranie YouTube (362 linie) i kilku plikach (konta, serwer strumienia); pamięć miniatur przewijania i przewijana strona rozdziałów.
- 🎨 Ekran YouTube w wyglądzie makiety i sterowanie odtwarzacza YouTube (rozdziały, odcinki SponsorBlock na pasku).

## 5. Drobne ustawienia domyślne Kicii
- ❌ Aplikacja po polsku, dopóki nie wybrano języka; dopasowanie liczby klatek domyślnie włączone; następny odcinek domyślnie włączony; pamięć TMDB na dysku przez 7 dni.

## Co z tego wynika
Najwięcej brakuje w punkcie 3 (logika wyboru i ratowania źródeł). To ok. 40 plików Kicii (`core/streams/smart`, `core/player/recovery`, `core/autoquality`, `ui/screens/player/Player*Smart*`) i wpięcia w odtwarzacz oraz ekran źródeł. Da się je przenieść jako osobny moduł z łatkami, tak jak skórkę.
