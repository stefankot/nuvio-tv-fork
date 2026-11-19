# DECYZJE

Wykonawca nie pyta właściciela. Korzysta z tej listy. Właściciel może zmienić dowolną linię; wykonawca stosuje aktualną treść.

## Przyjęte domyślnie (17.11.2026)

| # | Pytanie | Decyzja |
| --- | --- | --- |
| 1 | Kiedy odcinek jest obejrzany | odtwarzanie doszło do ostatnich 10% emisji i obejrzano co najmniej 50% jej czasu; „w trakcie” po 2 minutach |
| 2 | Ekran telewizji | domyślnie przewodnik; nad nim jeden mieszany rząd „Dla Ciebie”, który chowa się po zejściu do siatki |
| 3 | Wiadomości i podobne pasma | bez licznika nieobejrzanych |
| 4 | Poziom ustawień po instalacji | Prosty |
| 5 | Stan „obejrzane” | tylko lokalnie na urządzeniu, osobno dla profilu |
| 6 | Zakres przewodnika | pełny (7 dni wstecz, 8 naprzód), gdy urządzenie nie jest „low memory” według `LiveTvDevice.isLowMemory`; w innym razie zakres z Reshaped |
| 7 | Automatyczne ulubione i obserwowane | włączone; kanał po 3 oglądaniach w 14 dni, serial po 2 obejrzanych odcinkach; da się cofnąć |
| 8 | Ulubione | dwa pojęcia: „ulubiony kanał” (gwiazdka) i „obserwowany program” (dzwonek) |
| 9 | Kategorie | grupy kanałów filtrują siatkę; gatunki programów tylko w Odkrywaj i Szukaj |
| 10 | Objaw „działa źle” u dostawcy | nieznany; przenieść wszystkie 11 zmian z bloku B01 |
| 11 | Pytanie o dźwięk w kreatorze | trzy odpowiedzi: zawsze po polsku / po polsku przy dobrej jakości / oryginał z napisami; domyślnie środkowa |
| 12 | Automatyczna próba innego źródła | włączona w poziomie Prostym |
| 13 | Lektor AI a Dolby | lektor wyłącza przekazywanie Dolby do amplitunera; pokazać jedno zdanie informacji przy włączaniu |
| 14 | AI | wszystko działa od razu u każdego użytkownika: lokalny model osadzeń (EmbeddingGemma 300M skwantyzowany) plus paczka wektorów liczona poza urządzeniem. Model przez API jest opcjonalnym ulepszeniem jakości z kluczem użytkownika (nigdy w repozytorium). Gdy model lokalny jest niedostępny, działają reguły |
| 15 | Głos: rozumienie poleceń | minimum 60 wzorców z parametrami na regułach; prośby swobodne przez model po API (pierwszy dostawca: Gemini) |
| 16 | Głos: rozpoznawanie mowy | Whisper już obecny w forku jako silnik lokalny (small przy 3 GB RAM i więcej); systemowe na urządzeniach z 2 GB; wybór automatyczny |
| 17 | DubLift | tylko próba i raport w B11; bez integracji |
| 18 | Blokada wyjścia | 5 naciśnięć Wstecz w 3 sekundy otwiera „Wyjście i ustawienia”; PIN profilu, jeśli jest ustawiony |
| 19 | Urządzenia docelowe | Xiaomi TV Box S 3. generacji (2 GB RAM), Onn 4K Pro, projektor JMGO (4 GB RAM); wszystkie z Google TV. Najsłabsze (2 GB) wyznacza domyślne limity pamięci. Wszystko zależne od urządzenia ma przełącznik i wpis w `DO_SPRAWDZENIA_NA_TV.md` |
| 20 | Nazwy pakietów | `com.nuvio.tv.fork.registry`, `.tv`, `.sources`, `.settings`, `.night`, `.launcher`, `.voice`, `.lektor`, `.picks` |
| 21 | Gałąź bazowa | `final` (ma `ForkHooks`, `fork-tools`, kontrakt skórki i `ARCHITECT_PLAN.md`). `main` jest starszy i nie jest bazą |
| 22 | Drugi przewodnik EPG i paczka logo | dla kanałów polskich `https://epg.ovh/pl.gz` i `https://epg.ovh/logo/{name}.png`; lista adresów w pliku JSON w repozytorium, nie w kodzie |
| 24 | Polecenia (rekomendacje) | najpierw ocena modułu Scout z Nuvio Arc; przenieść go, jeśli jakość i budowa na to pozwalają, inaczej własny moduł. Ranking końcowy może robić model po API |
| 25 | Przycisk głosu na Google TV | usługa ułatwień dostępu przechwytująca jeden klawisz, włączana świadomie przez użytkownika |
| 26 | Układy Nuvio KE | jako druga skórka `MARQUEE` przez kontrakt `skin/api`; ekran główny najpierw; Live TV z KE nie jest przenoszone |
| 27 | Relacja do `ARCHITECT_PLAN.md` (archiwum) | jego zasady przeniesione do `ZASADY.md` |
| 23 | Zmienione wartości domyślne z Kicia | przez moduł „pierwsze uruchomienie”, nie przez edycję plików bazy |

## Podjęte przez wykonawcę

(dopisuj tutaj: data, zadanie, decyzja, jedno zdanie uzasadnienia)

- 2026-11-17 B00-5: ustawienia modułów renderuje `ForkModuleSettingsItems` wołane z istniejącego `forkSwitchesSettingsItems` (plik forka), nie z SettingsScreen — zero nowych zmian w rdzeniu.
- 2026-11-17 B01: łatki Reshaped trzymane per plik w `fork-tools/patches/<seria>/<plik>/NN-opis.patch` (seria w patched-overrides.txt to `seria/plik`) — odwracanie łatek jednego pliku nie może dotykać innych.
- 2026-11-17 B02: logika TV z Kicia w jednym pakiecie `com.nuvio.tv.fork.tv` (bez podpakietów rdzen/dane) — prostsze importy; jedyny import `reshaped.livetv` w `most/`.
- 2026-11-17 B04-5: pamięć serialu = istniejące „ciągłość grupy wydania” (ReuseBingeGroup + PreferBingeGroupForNextEpisode) włączane przy zapisie odpowiedzi — nie piszemy nowego mechanizmu.
- 2026-11-17 B04-4: decyzja o ścieżkach (AudioCheck) jest czystą logiką z testami; podpięcie do odtwarzacza nie wykonane (brak bezpiecznego publicznego wejścia mechanizmu odzyskiwania; patrz BLOKADY).
- 2026-11-17 B10-0: Scout z Arc NIE jest przenoszony (71 plików, ok. 23 000 linii, silne sprzężenie z modelami aplikacji; patrz plan/raporty/scout.md). Budujemy własny moduł `fork/picks` według B10-1..8.
- 2026-11-17 B06a: skórka KE (B06a-2/3/5) odłożona — zgodnie z CLAUDE.md „wygląd nie ma znaczenia teraz”; wykonane B06a-1 i raport B06a-4.
- 2026-11-19 Lektor (właściciel): głos = Google Cloud TTS Chirp 3 HD pl-PL, domyślnie Enceladus; wysyłanie scenami (markup + `[pause long]` + SilenceSplit); wybór głosu w ustawieniach z próbkami; klucz Cloud TTS na sztywno (local.properties → BuildConfig). Powód: Gemini TTS Tier 1 = 100 zapytań/dzień. Szczegóły: `plan/lektor/GLOS_CHIRP.md`.
- 2026-11-19 Buildy (właściciel): najwyżej 1 build naraz, 1 w kolejce, przed każdym buildem zerowanie kolejki — tylko przez `fork-tools/build.sh` (`ZASADY.md`).

## 19.11.2026 — skórka Niunia (gałąź skin-niunia)
- Plakaty z dodatków domyślnie w wariancie „baked” (stopka: gatunek · ocena); logo TMDB na plakacie dopiero, gdy wiadomo, że plakat jest bez napisów.
- Ocena = średnia dostępnych serwisów; dziś w danych jest tylko IMDb (`imdbRating`).
- Etykiety: „#n Dziś” w rzędach trend/popul/top, „Nowość” (premiera ≤ 45 dni), „Klasyka” (rok < 2000); kolor = średnia pikseli spod etykiety, tekst czarny/biały wg jasności.
- Profile zostają ekranem Nuvio, dopóki Niunia nie obsłuży PIN-ów.
- Rozmycie plakatu tylko API 31+ (RenderEffect); na JMGO sam gradient.
