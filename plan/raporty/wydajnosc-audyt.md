# Audyt Kicii → fork (17.11.2026)

Kicia (SHA 58b3a0a) ma audyt z pomiarami, a w kodzie prawie nic z poprawek. Zakres forka po poleceniu właściciela: tylko moduły (nie Home/Settings bazy). Znaleziska dotyczące `ui/forge/**` (Forge Home, pasek, Haze) nie dotyczą forka: ten kod nie istnieje w forku.

| WY | Dotyczy forka? | W kodzie Kicii? | Decyzja |
|---|---|---|---|
| 307 EAGER_BACKGROUND_LOAD | nie (to flaga Forge) | włączona (ForgeUi.kt:55), tylko zalecenie wyłączenia | fork: odpowiednik = opóźniona rozgrzewka przewodnika (TvModule, 8 s) – wdrożone |
| 101/104/107/103/110 wczytanie EPG | częściowo (Reshaped LiveTvEpg wspólny) | tylko zalecenie | na Xiaomi start 17% rdzenia przez 60 s, rozgrzewka pomijana na słabych TV: nie jest wąskim gardłem; zostaje |
| 301/405 debug bez R8, logowanie OkHttp | tak | opis | w forku logowanie OkHttp = BASIC tylko w debug; wariant wydaniowy: niezmierzony |
| 206/315 klatki/podgląd kanałów w pętli | tak (podgląd Live TV) | tylko zalecenie | **zmierzone: 46–86% rdzenia w bezczynności** → poprawka: auto-stop podglądu po 60 s (PerfFlags, przełącznik w Telewizja) |
| 308/309/312/313/314/310/311/406 Forge Home/pasek | nie | Forge, nie fork | pominięte |
| 303 profil bazowy, `baselineprofile/` | tak | moduł istnieje w Kicii | niewdrożone w tej sesji (wymaga osobnego buildu i pomiaru) |
| 203/205/207/204/202/201 obrazy/logotypy | częściowo | tylko zalecenie | niezmierzone w forku |
| 402/403/404 szum TMDB/IMDb/DeArrow | baza/moduły | zalecenie | niewdrożone |

## KOREKTA (po wskazaniu właściciela): właściwe repozytorium Kicii
Audyt powyżej oparłem na jednowersyjnej kopii `Desktop/nuvio 2/nuvio-tv-kicia` (1 commit, bez poprawek). Pełne repozytorium to `Desktop/nuvio-tv-kicia` (976 commitów; kopie robocze `-makieta`, `-poprawki` mają tę samą historię, bez niezacommitowanych zmian wydajnościowych). Wdrożone tam poprawki wydajności (wszystkie w `ui/forge/**`, kod Forge Home/Live TV Kicii, którego fork nie ma):
| Commit | Co |
|---|---|
| 3976e3b7b, 1b16f1682 | `ForgeUi.EAGER_BACKGROUND_LOAD=false` (WY-307: bez ładowania EPG/Live TV/YouTube w tle po starcie) |
| db0388847, 7c12e95be | `LIVE_CARD_PREVIEW=false` (bez podglądu live na fokusie karty), `CHANNEL_FRAMES` (klatki ze streamów tylko, gdy EPG nie ma obrazu) |
| be6999959 | WY-308: zasłona 96% czytana w fazie rysowania |
| 6924140f9 | WY-309/314: przewijanie półek Home z `snapshotFlow` |
Dotyczy forka wprost tylko to, co jest odpowiednikiem podglądu live: w Reshaped Live TV podgląd na fokusie kanału gra domyślnie i to on był zmierzonym kosztem (46–86% rdzenia). Kicia wyłączyła go całkiem; fork ma teraz auto-stop po 60 s.
