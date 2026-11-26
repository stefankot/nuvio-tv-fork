# Funkcje z Reshaped w forku, których nie ma w oficjalnym Nuvio (26.11.2026)

Powód: autor zakończył rozwój Nuvio Reshaped. Ten spis mówi, co w forku pochodzi tylko z Reshaped (nikt tego nie będzie już rozwijał).

## Co z czym porównano
- Fork: `skin-niunia` (HEAD `fd93cf797`). Reshaped przypięty `0ca5a08c2` (17.11, „1.2.0-beta.2-autosync.5”), `fork-tools/pins.env`.
- Oficjalne Nuvio (NuvioMedia/NuvioTV): najnowsze wydanie stabilne **1.1.1** (21.11) i najnowsza beta **1.2.0-beta.4** (24.11).
- Metoda: zmiany Reshaped = różnica względem wspólnego punktu z Nuvio (`3ff5e2b41`, 17.11): 770 nowych plików i 167 zmienionych. Dla każdego pliku: czy jest w forku i czy jest w 1.2.0-beta.4. Do tego teksty ustawień obu wersji i opisy commitów Reshaped (548 własnych commitów). Bez uruchamiania aplikacji.

## A. Tylko w Reshaped — brak w oficjalnym Nuvio (ani 1.1.1, ani 1.2.0-beta.4)

1. **Telewizja na żywo (Live TV / IPTV)** — `reshaped/livetv/`, `ui/reshaped/livetv/`. Listy M3U, Xtream i Stalker, wiele źródeł; przewodnik programowy (XMLTV) w siatce; archiwum (catch-up do tygodnia, przewijanie kanału wstecz, „Od początku”); kategorie (kolejność, zmiana nazw, ukrywanie kanałów); ulubione i własne listy kanałów; podgląd kanału; karta „Teraz / Następnie”; szybkie przełączanie kanałów; dopasowanie odświeżania ekranu; powrót do transmisji na żywo po zatrzymaniu; konfiguracja źródeł z telefonu (kod QR); osobny User-Agent dla źródła. W forku włączone domyślnie; fork ma na tym własne nakładki (przewodnik w bazie SQLite, Niunia, Kicia).
2. **Synchronizacja przez konto Google (Dysk Google)** — `reshaped/sync/`. Ustawienia Reshaped oraz Live TV (źródła, listy, ulubione) takie same na kilku telewizorach i telefonie.
3. **Podgląd klatek przy przewijaniu (miniatury)** — `ui/screens/player/seekpreview/` + kod natywny `cpp/seekpreview/` (silnik z Nuvio Arc). Miniatury z serwisu Seekr (z własnym kluczem, wpisywanym z telefonu), miniatury robione na urządzeniu z tego, co już się pobrało, przygotowanie przed startem, mniejsza kopia pliku, miniatury następnego odcinka, pasek klatek jak w Netflixie, ręczna korekta zgrania podglądu, miniatury dla serwerów multimediów (Jellyfin/Emby/Silo) i Usenetu.
4. **Formaty dźwięku i obrazu** („Sound and picture formats”, z Nuvio Arc) — `iec/`, `core/player/{AudioPassthroughPolicy,DeniedTranscodePlanner,SurroundFormatResolver,Hdr10SeiInjector}.kt` i in. Bezstratne przesyłanie TrueHD/DTS-HD do amplitunera własną drogą aplikacji; wybór, co umie amplituner (AC-3, E-AC-3, TrueHD, DTS, DTS-HD); zamiana wyłączonych formatów na Dolby Digital 5.1; tryb „dźwięk przez TV do soundbara (ARC)”; liczba kanałów po dekodowaniu; automatyczny dobór dźwięku przestrzennego; zapamiętywanie formatów odrzuconych przez amplituner i zacięć trybu tunelowego; domyślnie najlepsza bezstratna ścieżka dźwięku; dopisywanie danych HDR10 przy Dolby Vision profil 8; raport możliwości dźwięku i dodatkowe wiersze statystyk. (Oficjalne ma tylko automatyczny passthrough, „Wymuś AC-3” i obsługę Dolby Vision profil 7.)
5. **Kolejność źródeł według prędkości łącza** („Match streams to connection”) — `core/connection/`. Mierzy prędkość z własnych odtworzeń i przesuwa za ciężkie źródła na koniec.
6. **Ranking reguł Regex** — `core/streams/ranking/`, `RankedRegexDialog`, `StreamRegexPresets`. Do 20 reguł z priorytetami i gotowe zestawy. Oficjalne ma jedno pole Regex. Fork z tego korzysta (zestawy reguł kreatora źródeł).
7. **Strona główna w stylu Netflixa** — `ui/reshaped/netflix/`. Duży baner u góry, kafelek rozszerza się z opisem.
8. **Menu „pigułka” u góry ekranu** — `ui/reshaped/pillnav/`. Pływające menu z efektem szkła zamiast bocznego paska. (Oficjalne „Top Bar” dotyczy tylko wyglądu ekranu ustawień.) Fork podpina pod nie menu Niuni.
9. **Dymki AutoSync** — `autosync/bubble/`. Postęp i wynik synchronizacji napisów w małym dymku oraz opcja „pokazuj tylko błędy”.
10. **Logi diagnostyczne na telefon** — `reshaped/debuglog/`. Kod QR, telefon w tej samej sieci pobiera logi (z ukryciem danych wrażliwych).
11. **Wpisywanie tekstu z telefonu** — `reshaped/phoneentry/`. Kod QR, tekst (np. klucz) wpisany na telefonie trafia do TV. Fork używa tego do klucza Gemini.
12. **Dodatki do wzmocnienia głośności** — `ui/reshaped/volumeboost/`. Pasek 100–200% i miękki ogranicznik zniekształceń. (Samo wzmocnienie jest w oficjalnym.)
13. **Drobne rzeczy techniczne** — osobne połączenia dla równoległego pobierania (`HttpLanes`), instalacja plików mpv bez nadpisywania czcionek (`MpvAssets`), zachowanie stylu napisów ASS z plików MKV, jednorazowe przeniesienie ustawień ze starej wersji Reshaped (`ReshapedMigration*`), poprawka fokusu w wyszukiwarce (`SearchViewModel`).

## B. Z Reshaped, ale oficjalne Nuvio ma już własną wersję
Fork trzyma wersję Reshaped; oficjalna działa podobnie, ale kod jest inny.
- **AutoSync napisów** (tolerancja, dokładne szukanie) — oficjalnie od 1.1.x.
- **Synchronizacja napisów z dźwiękiem** (AudioSync, model mowy) — oficjalnie od 1.2.0-beta (w 1.1.1 brak). Fork ma wersję Reshaped + własne poprawki (Whisper, Codex).
- **Własna czcionka napisów przesyłana z telefonu** — oficjalnie od 1.2.0-beta (do 10 MB; Reshaped: 20 MB i pobieranie z adresu). W 1.1.1 brak.
- **Automatyczne przejście na inne źródło, gdy start się nie uda** — oficjalne ma „Fallback to next matching source”; Reshaped ma własną wersję (wolny start, błąd otwarcia, zachowana kolejność). W forku włączona.
- **Wzmocnienie głośności, Dolby Vision profil 7** — w oficjalnym są; Reshaped zmieniał tylko szczegóły.

## C. Z Reshaped, ale już usunięte z forka
- Aktualizator Reshaped i kanał „Tester” (fork używa aktualizatora Nuvio).
