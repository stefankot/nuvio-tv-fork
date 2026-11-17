# B01 Poprawki pod dostawcę, automatyczne EPG i logo (budżet 4 h)

Cel: telewizja działa tak dobrze jak w Kicia, a użytkownik podaje tylko adres listy kanałów.

Skróty: `K` = `../nuvio-tv-kicia/app/src/main/java/com/nuvio/tv`, `J` = `app/src/main/java/com/nuvio/tv`.

Metoda: dla każdego pliku porównaj `diff J/<plik> K/<plik>` i przenieś tylko linie Kicia opisane niżej. Nowy fork ma nowszą wersję Reshaped, więc nie kopiuj całych plików. Każda zmiana dostaje komentarz `// Fork patch: dostawca #<nr>` i osobny plik łatki w `fork-tools/patches/dostawca/` (klasa „łatany” z B00-6). Uwaga na limit 35 plików ręcznie utrzymywanych: ten blok dodaje do niego do 8 plików Live TV; jeśli limit zostałby przekroczony, zapisz to w `STATUS.md` z uzasadnieniem (wymóg `ARCHITECT_PLAN.md` 4.1) i kontynuuj. Odwołania do `com.nuvio.tv.forge.*` zamień na wywołania modułu przez rejestr albo odłóż do B02 (zaznaczone niżej).

Dane dostawcy właściciela (z pomiaru 7.11): lista M3U, 313 kanałów, EPG 51 MB gzip (ok. 346 MB po rozpakowaniu, 3792 kanały), 71% kanałów ma program, archiwum przez atrybut `timeshift` (170 kanałów po 7 dni), strumienie w stylu Flussonic.

## Zadania

- [x] **B01-1 Klucz zapisu przewodnika niezależny od kolejności kanałów (#1).** `reshaped/livetv/LiveTvEpg.kt`, funkcja licząca `matching`: suma po kanałach zamiast łańcucha. Bez tego każdy start czyta cały plik EPG.
  Ukończone, gdy: test JVM pokazuje ten sam klucz dla dwóch kolejności tej samej listy kanałów.

- [ ] **B01-2 Wczytanie w tle po starcie (#2).** `LiveTvRepository.kt`: `warmUp`, `cancelWarmUp`, stałe; wywołanie po starcie aplikacji przez rejestr (nakładka albo inicjalizacja modułu), anulowanie przy starcie odtwarzania. Urządzenia „low memory” pomijają.
  Ukończone, gdy: kompiluje się; pozycja w `DO_SPRAWDZENIA_NA_TV.md` („pierwsze wejście do TV pokazuje program od razu”).

- [x] **B01-3 Dziennik czasu EPG (#11).** Skopiuj `K/reshaped/livetv/LiveTvEpgTrace.kt` i wywołania `LiveTvEpgTrace.step/begin` oraz logi w `LiveTvGuideCache.kt`.
  Ukończone, gdy: kompiluje się.

- [ ] **B01-4 Pierwszeństwo przewodnika dodanego przez użytkownika i uzupełnianie z drugiego przewodnika (#3, #4).** `LiveTvRepository.kt`: ranking (dodany przewodnik wygrywa), `extraLists`, funkcja `forgeEnrichProgrammes` (nazwij `enrichProgrammes`, przenieś do `J/fork/tv/most`). Testy z Kicia, jeśli istnieją, przenieś.
  Ukończone, gdy: test JVM: emisja bez obrazu dostaje obraz z drugiej listy przy tym samym tytule i starcie w granicach 10 minut.

- [x] **B01-5 Numer odcinka i obrazy dla całego okna (#6).** `LiveTvModels.kt` (pole `episode` w `LiveTvProgramme`), `LiveTvEpg.kt` (przekazywanie `episode`, `wantsPicture`, obrazy nie są obcinane), `LiveTvGuideCache.kt` (zapis i odczyt `episode`, podniesienie `VERSION` o 1 względem obecnej wartości w nowym forku).
  Ukończone, gdy: istniejące testy `LiveTvGuideLoadingTest` i `LiveTvArchiveTest` są zielone.

- [x] **B01-6 Szczegóły z EPG (#7).** `LiveTvEpg.kt`: czytanie `sub-title`, `episode-num`, `date`, `country`, `category`, `rating`, `credits`, funkcja `readChildTexts`. Wynik przekazuj przez jedno wywołanie `ForkTvHooks.onProgrammeRead(...)` (interfejs w `J/fork/tv/most`, implementacja pusta do B02).
  Ukończone, gdy: test JVM parsera na małym pliku XMLTV z tymi polami.

- [ ] **B01-7 Zakres przewodnika (#5).** Okno 7 dni wstecz i 8 naprzód oraz zachowanie starszych dni po odświeżeniu, ale według DECYZJE #6: pełne tylko poza „low memory”.
  Ukończone, gdy: test JVM okna dla obu przypadków.

- [ ] **B01-8 Klawisze Live TV (#10).** `ui/reshaped/livetv/LiveTvPlayerOverlay.kt`: klawisze zostają przy Live TV podczas startu i wznawiania strumienia (warunek `currentListUrl`), potwierdzenie zmiany kanału podczas odtwarzania z archiwum (`zapConfirm`, `zapNow`). Okno potwierdzenia zrób z istniejących komponentów aplikacji. Części MiniGuide nie przenoś tutaj (blok B03).
  Ukończone, gdy: kompiluje się; pozycja na liście TV („OK i strzałki podczas startu kanału nie otwierają kontrolek filmu”).

- [ ] **B01-9 Logo kanałów (#8).** Skopiuj `K/ui/forge/livetv/ForgeLogoPack.kt` i `ForgeChannelLogo.kt` do `J/fork/tv/logo` z testami (`ForgeLogoModeTest`, `ForgeLogoFiltersTest`, `ForgeLogoInnerTileTest`). Jedno wpięcie w `LiveTvModels.kt` w miejscu wyboru logo. Kolejność: paczka po nazwie kanału, ikona z EPG, logo z listy, kafel z nazwą.
  Ukończone, gdy: przeniesione testy są zielone.

- [x] **B01-10 Automatyczny drugi przewodnik i lista adresów.** Plik `app/src/main/assets/fork/epg_sources.json` (kraj, adres EPG, wzór logo; na start Polska według DECYZJE #22) z możliwością nadpisania wersją pobraną z repozytorium właściciela (ten sam mechanizm co katalog KE: `J/livetv/source/PackStore.kt`). Po dodaniu źródła moduł rozpoznaje kraj (końcówka `tvg-id`, nazwy grup) i dodaje przewodnik uzupełniający bez pytania. Użytkownik może go usunąć w ustawieniach Advanced.
  Ukończone, gdy: test JVM rozpoznania kraju na próbce 20 kanałów; brak adresów w kodzie Kotlin.

- [x] **B01-11 Strona konfiguracji z telefonu (#9) i kontrola pokrycia.** `LiveTvSetupServer.kt`: pola EPG i logo (opcjonalne, zwinięte pod „Zaawansowane”). Po pierwszym wczytaniu jeden komunikat „N z M kanałów ma program”; kanały bez programu na końcu każdej grupy. Lista „Kanały bez programu” w ustawieniach Advanced modułu.
  Ukończone, gdy: test JVM sortowania; kompiluje się.

## Po bloku

`scripts/fork/hooks.sh` musi wypisać wszystkie `fork-patch: dostawca`. Dopisz je do `plan/WPIECIA.txt`. W `STATUS.md` dodaj listę łatek, które warto zgłosić autorowi Reshaped (#1, #2, #5).
