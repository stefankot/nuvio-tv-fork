# AIOMetadata — dlaczego mało okładek programów (27.11.2026)

Poprawek w aplikacji: 0, sprawdzonych na urządzeniu: 0. To raport o ustawieniach wtyczki. Panel wtyczki tylko przeczytałem, niczego w nim nie zmieniłem.

## Stan po zmianach (27.11, ok. 17:50)

- **Wyszukiwanie seriali jest już przełączone na TMDB.** W tym samym teście wtyczka znajduje teraz 53 z 87 tytułów (wcześniej 44), m.in. „Zabójcza broń”, „Piotruś Królik”, „Z kamerą u Kardashianów”.
- **Aplikacja dopytuje teraz wtyczkę o szczegóły znalezionego tytułu** i bierze z nich poziomy kadr. Na szczegółach wtyczka łączy serwisy, a obrazy wybiera według ustawień „Art Providers” w panelu. Gdy szczegóły nie mają kadru, zostaje obraz z wyszukiwania. Zmiana czeka na najbliższy wspólny build. Okładki zostaną pobrane od nowa (nowa pamięć, stara zostaje obok).
- **Najlepiej oceniany obraz: wtyczka już go wybiera**, nic nie trzeba zmieniać. Fanart.tv — obraz z największą liczbą polubień. TMDB — obraz z najwyższą oceną. Najpierw szuka obrazów po polsku, potem po angielsku, potem bez napisów. Gdy Fanart.tv nie ma obrazu dla tytułu, wtyczka bierze go z innego serwisu.
- **Jeśli kadry mają być z Fanart.tv** (zwykle najładniejsze, bez napisów): w panelu wybierz sekcję „Art Providers”. W ramce „Art Sources”, w grupach MOVIES i SERIES, w wierszu **Background** wybierz **Fanart.tv** (dziś: „Meta Provider (default)”, czyli TMDB dla filmów i TheTVDB dla seriali). Potem sekcja „Configuration” → **Save Configuration**. To samo zmieni tła na stronach filmów i seriali w całej aplikacji.

## W skrócie (pierwsza wersja raportu, przed zmianami)

- **Przy wyszukiwaniu wtyczka nie łączy danych z wielu serwisów.** W AIOMetadata do każdej rzeczy (wyszukiwanie filmów, wyszukiwanie seriali, opisy, obrazy) wybiera się **jeden** serwis. „Wiele źródeł” oznacza, że dla każdej z tych rzeczy można wybrać inny serwis, a nie że wyniki z kilku serwisów są sumowane.
- **Wyszukiwanie seriali jest u Ciebie ustawione na TheTVDB** (sekcja „Search”, wiersz „Series Search”). TheTVDB słabo zna polskie tytuły. Nie znajduje m.in. „Zabójcza broń”, „Z kamerą u Kardashianów”, „Piotruś Królik”, „Kot z piekła rodem”, „Sposób na zamek”, „Polowanie na ogród”, „15 na żywo”, „Wakacyjne domy na wynajem”. TMDB zna je wszystkie.
- **Wyniki z TheTVDB mają tylko pionowy plakat, bez poziomego kadru.** Dlatego karta programu (pozioma) dostaje wtedy przycięty plakat. Wyniki z TMDB mają oba obrazy.
- Wyszukiwanie filmów jest już ustawione na TMDB i działa dobrze.
- Klucze TMDB, TheTVDB, Fanart i pozostałe są w panelu wpisane. Język: polski. Tu wszystko jest w porządku.

## Pomiar: te same 87 tytułów z przewodnika TV

| | AIOMetadata teraz | samo TMDB |
|---|---|---|
| Seriale i programy (każdy program krótszy niż 70 min) | 24 (28 %) | 34–39 (39–45 %) |
| Seriale i filmy razem | 44 (51 %) | 49 (56 %) |

Programy krótsze niż 70 minut aplikacja szuka tylko wśród seriali, a takich programów jest najwięcej. Dlatego w aplikacji trafień jest tylko ok. 33 %.

Po zmianie z punktu niżej wtyczka będzie szukać seriali tak samo jak TMDB: **ok. 39 trafień zamiast 24**, z poziomymi kadrami. Stracimy 2 polskie programy, które zna tylko TheTVDB („Idealna niania”, „Mieszkanie na miarę”).

## Co zmienić w panelu wtyczki (krok po kroku)

Panel: strona aiometadata.elfhosted.com. Jest otwarty i zalogowany w przeglądarce Claude (aplikacja Claude).

1. Pod polem „Search settings…” kliknij listę sekcji (teraz pokazuje np. „General”) i wybierz **Search**.
2. W ramce **Search Engines**, w wierszu **Series Search**, kliknij listę z napisem **TheTVDB Search** i wybierz **TMDB Search**.
3. Wiersz **Movies Search** zostaw bez zmian (**TMDB Search**).
4. Reszty nie ruszaj. W sekcji „Meta Providers” pole „Series Provider: TheTVDB” może zostać, bo dotyczy tylko opisów i odcinków na stronie serialu, nie wyszukiwania.
5. Z listy sekcji wybierz ostatnią, **Configuration**, i kliknij **Save Configuration** (na dole).
6. Adres wtyczki się nie zmienia, więc w aplikacji nie trzeba jej instalować od nowa.

Potem (zrobi to Claude): wyczyścić w aplikacji zapamiętane „brak okładki” (`files/fork_tv_covers_aio2.json`) i porównać liczbę trafień w logu `ForkTvCovers` z obecnymi 33 %.

## Czy po zmianie wtyczka da tylko dane z TMDB?

Przy okładkach programów TV — praktycznie tak. Wtyczka działa w dwóch krokach:
1. **Znalezienie tytułu po nazwie.** Zawsze w jednym serwisie, tym wybranym w „Search”. Do polskich nazw najlepszy jest TMDB.
2. **Strona szczegółów tytułu** (opis, obrazy, odcinki, oceny). Tu wtyczka łączy serwisy: opisy seriali bierze z TheTVDB, obrazy z serwisów wybranych w „Art Providers” (np. Fanart.tv), a gdy jednemu czegoś brakuje — sięga do innego.

Okładki programów w aplikacji korzystają dziś tylko z kroku 1, więc z łączenia serwisów nic nie mają. Sprawdzenie: dla „Świat według Kiepskich” i „Kuchenne rewolucje” krok 1 (TheTVDB) daje tylko pionowy plakat, a krok 2 ma już poziomy kadr.

Żeby okładki korzystały z łączenia serwisów, aplikacja musiałaby po znalezieniu tytułu dopytać wtyczkę o szczegóły (drugie zapytanie na każdy tytuł). To zmiana w aplikacji (`TvCovers`), nie w panelu, i decyzja należy do właściciela. Zmiana wyszukiwarki seriali na TMDB i tak jest potrzebna — bez niej wtyczka wielu tytułów w ogóle nie znajdzie.

## Czy aplikacja dobrze pyta wtyczkę (`TvCovers`)

Tak. Nie znalazłem niczego, co zaniżałoby wyniki:
- pyta właściwe katalogi: najpierw „Series Search” (`search.series`), potem „Movies Search” (`search.movie`), ten drugi tylko dla programów od 70 minut;
- zapytanie ma poprawny format, polskie znaki są dobrze zakodowane (mój test z komputera dał te same wyniki co aplikacja);
- bierze poziome tło (`background`), a gdy go brak — plakat; brak tła to sprawa wyników z TheTVDB, nie aplikacji;
- wysyła najwyżej 2 zapytania naraz. To dobrze: publiczny serwer wtyczki przy 4 zapytaniach naraz odrzucał część z nich (w teście), a w logu aplikacji błędów jest 0.

Programy informacyjne, pogoda i transmisje („Fakty po południu”, „Piłka nożna”, „Serwis informacyjny”) nie mają okładek w żadnym serwisie. To normalne. Przy bardzo ogólnych tytułach (np. „Pogoda”) TMDB może podać okładkę innego programu o tej samej nazwie. Tak było też dawniej, przy samym TMDB.

## Dla następnego agenta (szczegóły techniczne)

- Wtyczka: `https://aiometadata.elfhosted.com/stremio/<id konfiguracji>/manifest.json` (id w `installed_addon_urls_ordered`, magazyn `addon_preferences` na profilu 1; adresu nie wypisywać). Wersja 4.1.0, 35 katalogów. Katalogi wyszukiwania: `movie/search.movie`, `series/search.series`, `anime.series/search.anime_series`, `anime.movie/search.anime_movie`, `other/gemini.search`. Id katalogów są ogólne, więc zmiana wyszukiwarki w panelu ich nie zmienia (w panelu widać `tmdb.search` / `tvdb.search`, w manifeście `search.*`).
- Ustawienia z panelu (27.11): Movies Search = TMDB, Series Search = TheTVDB (do wyboru: TMDB, TheTVDB, TVmaze, MDBList, IMDb, LumiereDB), Anime = Kitsu. Meta Providers: filmy TMDB, seriale TheTVDB, anime TheTVDB. Art Providers: wszędzie „Meta Provider (default)”. Panel ostrzega, że ustawienia obrazów nie dotyczą wyników wyszukiwania, bo te mają obrazy z wybranej wyszukiwarki.
- Pomiar: skrypt `.stage/aio/porownaj_aio_tmdb.py` (poza gitem; czyta adres z `.stage/aio/aio_url.txt` i klucz TMDB z `local.properties`), wyniki w `.stage/aio/cmp2.json`, lista tytułów `.stage/aio/titles.txt` (z logcatu JMGO). Zapytania wysyłaj po kolei, nie po 4 naraz, bo publiczny serwer zwraca wtedy odpowiedzi, które nie są JSON-em. Uruchomienie: `python3 -I porownaj_aio_tmdb.py <.stage/aio> <local.properties> <titles.txt> <wynik.json>`.
