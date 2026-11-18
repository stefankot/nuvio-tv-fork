# Audyt: czy funkcje z planu da się użyć (18.11.2026, tylko kod, gałąź aktualizacja-1118)

Legenda: **DZIAŁA** = jest wejście w interfejsie i droga do działania · **UKRYTE** = kod działa, ale nie ma jak go uruchomić albo jest domyślnie wyłączony · **PRZERWANE** = droga przerwana w kodzie · **BRAK** = kodu nie ma albo nic go nie wywołuje.

Wpięcia (check.sh) są na miejscu po dzisiejszej aktualizacji bazy; check.sh porównuje jeszcze manifest ze starą bazą (do poprawy w aktualizacji pinów).

## B00 Fundament
- Rejestr modułów, wpięcia, CI, autoaktualizacja: DZIAŁA (infrastruktura, bez UI).
- Ekran „Moduły”: dostępny tylko z Narzędzi / ustawień Prostych, nie jako kategoria w zwykłych Ustawieniach.

## B01 Dostawca, EPG, logo
- Klucz przewodnika, wczytanie w tle, ranking przewodnika, szczegóły XMLTV, okno 7/8 dni, drugi przewodnik, logo, potwierdzenie zmiany kanału: DZIAŁA (w tle, bez osobnego UI).
- `cancelWarmUp` przy starcie odtwarzania: BRAK (nic go nie woła).
- Komunikat „N z M kanałów ma program” i lista „Kanały bez programu” (B01-11): BRAK (Coverage nieużywane).

## B02 Dane TV
- Dziennik odcinków, dziennik oglądania, obserwowane, statystyka: DZIAŁA w tle, ALE dziennik odcinków zapisuje się tylko dla kanałów z archiwum (`TvBridge.onGuideRead`: `if (days.isEmpty()) return`). Bez catch-up wszystko, co z niego korzysta, jest puste.
- Cache kart programu (B02-7): BRAK (ProgramCardCache nieużywany).

## B03 Ekrany TV
- Rząd „Dla Ciebie” nad przewodnikiem, znaczniki w siatce, strona programu, przypomnienia: wpięte.
- Rząd TV na ekranie głównym (B03-9): UKRYTE: rzędy modułów rysują się tylko w układzie Classic; domyślny układ to Modern (`LayoutPreferenceDataStore.kt:162`), hook w Modern wycofany.
- Panel grup kanałów (B03-3), MiniGuide (B03-6), „z opóźnieniem” (B03-7): BRAK UI (logika z testami, nieużywana; w BLOKADY).

## B04 Źródła i napisy
- Pytanie „polski dźwięk czy jakość”, auto-próba źródła, sprawdzenie ścieżek (hook w Tracks), linia „Gotowe”: wpięte.
- „Napisy nie pasują” (B04-8): UKRYTE: brak pozycji w menu napisów; tylko polecenie głosowe (które nie startuje, B07).
- Napisy dla seniorów (B04-9): do sprawdzenia w SourcesModule (pozycja rozmiaru napisów).

## B05 Ustawienia i tryb nocny
- Ekran Prosty, poziom ustawień, tryb nocny (dźwięk przez hook, nakładka): wpięte.

## B06 Launcher
- Aplikacje: było PRZERWANE (brak `<queries>` LEANBACK_LAUNCHER, lista zawsze pusta); poprawione 18.11 w `app/src/full/AndroidManifest.xml`, niesprawdzone na urządzeniu.
- Alias HOME, start z telewizorem, 5×Wstecz: wpięte (manifest forka).

## B06a Skórka KE
- BRAK: nie ma `skin/marquee`, `MarqueeSkin`, komponentów ani ekranu głównego KE. Jest tylko pusta rama `skin/api` + `KitSkin`.

## B06b Silnik znaczeń (AI)
- BRAK w aplikacji: `VectorPack` nieużywany, brak `AiModule`, brak uruchamiania modelu. Skrypt paczki jest, paczki nie ma (BLOKADY: sekret TMDB, licencja Gemma).

## B07 Głos
- Nasłuch, parser reguł, encje, akcje, dymek: kod jest.
- Wywołanie: PRZERWANE. Brak przycisku mikrofonu w aplikacji; brak zadania „Sterowanie głosem” w pilocie Arc (`RemotePlayerHook.kt`: nie ma takiego RemoteButtonJob); asystent systemowy na Google TV zwykle niewybieralny; usługa ułatwień dostępu wymaga ręcznego włączenia w systemie + ukrytego przełącznika `voice_key_service` (domyślnie wyłączony).
- Dopasowanie znaczeniowe (B07-5b) i parser przez API (B07-8): BRAK (SemanticParser, LlmParser nieużywane).

## B08 Odkrywaj i Szukaj
- Szukaj w TV: kanały działają; programy z EPG NIE są przeszukiwane (indeks tylko z dziennika archiwum, B02). Ostatnie wyszukiwania i kafle koszyków: BRAK (RecentSearches nieużywane).
- Odkrywaj: ekran jest, rzędy puste bez archiwum (B02); na ekranie głównym tylko w układzie Classic.

## B09 Lektor
- Plan kwestii, tempo, synteza TTS, miks w procesorze dźwięku: wpięte.
- Włączanie (B09-6): UKRYTE: brak pozycji w menu dźwięku odtwarzacza. Start tylko głosem (nie działa, B07) albo automatycznie przy ukrytym `lektor_auto` (domyślnie wyłączony) i profilu „po polsku”. Przy przekazywaniu Dolby/DTS do amplitunera procesor nie działa.
- Wybór głosu (B09-7): BRAK.

## B10 Nastroje
- Ekran „Na co masz ochotę?”: jest w Narzędziach; zapytanie TMDB używa `BuildConfig.TMDB_API_KEY` (w tej kopii `local.properties` pusty).
- Rzędy nastrojów na ekranie głównym (B10-4): BRAK (PicksModule nie ma `homeRows`).
- Ankieta (B10-6), ranking przez API (B10-8), filtr „zagra po polsku” (B10-5): BRAK (Survey, LlmRank nieużywane).

## B11 Eksperymenty
- Raporty są; kodu zgodnie z planem nie dodano.

## Echo (spoza planu, wspomniane przez właściciela)
- Przełączanie w locie (Auto Quality + hot swap): wpięte, ale UKRYTE: trzy przełączniki domyślnie wyłączone, działa od następnego odtworzenia.

## Wspólna przyczyna
Kryterium „ukończone” w planie to prawie wszędzie test JVM logiki, nie wejście w interfejsie. Wykonawca odkładał każde wpięcie w duży plik ekranu (odtwarzacz, ekran główny Modern, menu napisów/dźwięku) do BLOKADY i liczył zadanie jako zrobione. Wymóg zgodności z forkami (cienkie wpięcia, limit ~35 plików) tego nie wymuszał: brakujące wejścia to pojedyncze linie w plikach, które już mają wpięcia (PlayerScreen, MainActivity, HomeScreen, RemotePlayerHook).
