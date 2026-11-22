# Plan testów (aktualny 22.11.2026; poprzednia wersja z 20.11 w historii gita)

Właściciel 22.11: „zaprojektuj testy jednostkowe dla każdej funkcjonalności w moim Nuvio; iteracyjnie poprawiaj błędy dużymi
paczkami; nie kompiluj paczki z pojedynczą zmianą; testuj co najmniej 10 minut jedną kompilację”.

## Jak to działa (jedna paczka = jeden build)
1. Wszystkie poprawki z kolejki → commit (bez buildu dla jednej poprawki).
2. Jeden build: `bash fork-tools/build.sh <nazwa> :app:testFullDebugUnitTest :app:assembleFullDebug -PforkAbi=armeabi-v7a`
   (testy JVM + APK na JMGO naraz). Wyniki testów: `app/build/test-results/testFullDebugUnitTest/*.xml`,
   zbiorczo `python3 -I fork-tools/test/junit_wyniki.py` (lista nieudanych z pierwszą linią błędu).
3. Instalacja na JMGO (md5 po `adb pull`, `compile -m speed`), potem co najmniej 10 minut testu na TV:
   obchód pilotem `DEV=192.168.31.252:5555 python3 fork-tools/uitest/crawl.py` (trasy domyślne) + logi `Fork*` i bufor crash.
4. Błędy z testów JVM i z TV → poprawki do następnej paczki (kolejka w `plan/TERAZ.md`).

Pokrycie: `python3 -I fork-tools/merge/coverage.py .` — pliki forka bez żadnego testu (22.11: 252 pliki, 102 z testem przed paczką testów).

## Funkcje → testy jednostkowe (JVM)
Testy sprawdzają czystą logikę (bez TV, bez sieci). Ekrany Compose i odtwarzanie — obchód pilotem na JMGO.

| Funkcja | Co sprawdzamy | Plik testu |
|---|---|---|
| Live TV: archiwum IPTV | ile godzin program jest jeszcze w archiwum (deklarowane/sprawdzone dni), program trwający, przeterminowany, klucz źródła | `fork/tv/LiveTvLogicTest` (NOWE) |
| Live TV: przypomnienia | zapis/odczyt bez utraty pól (tab, nowa linia), uszkodzone wiersze | `fork/tv/LiveTvLogicTest` (NOWE) |
| Live TV: kolejność przewodników | epg.ovh przed tv.team, ranga dla repozytorium, obce hosty | `fork/tv/LiveTvLogicTest` (NOWE) |
| Live TV: duże listy (252 tys. kanałów, JMGO 22.11) | przewodnik uzupełniający po słowach grupy (separatory, wielkość liter, całe słowa, remis, 250 tys. kanałów < 5 s); pierwszy kanał z archiwum każdego źródła; Niunia: kolejność polskie→zagraniczne, numery, pamięć wyniku tylko dla tej samej listy, norm() bez Regex | `fork/tv/EpgSourcesTest` (+5), `fork/tv/LiveTvLogicTest` (+1), `skin/niunia/NiuniaLiveOrderTest` (NOWE, 5) |
| Live TV: EPG, kanały, przewodnik, alerty, historia | 22 pliki | `fork/tv/*` (było) |
| Live TV: logo kanałów | paczka zip, logo przy wyłączonym module | `fork/tv/LogoAndCondenseTest` (NOWE) + `fork/tv/logo/*` (było) |
| Niunia: Live TV | powrót z odtwarzacza na kafel/komórkę (raz), stan „na żywo”, postęp, serial | `skin/niunia/NiuniaLiveAndPlayerTest` (NOWE) |
| Niunia: odtwarzacz | Wstecz krok po kroku (nakładka decyduje, błąd nie wywraca) | `skin/niunia/NiuniaLiveAndPlayerTest` (NOWE) |
| Niunia: plakaty | plakat pionowy ze wzoru, zapas (oryginał, Cinemeta po IMDb — 22.11) | `skin/niunia/NiuniaLogicTest` (poprawione 22.11) |
| Niunia: tokeny, DS, etykiety, nagrody, YouTube | 55 testów | `skin/niunia/*` (było) |
| Rodzina: szyfrowanie | SHA-256 wzorzec, kod parowania 6 cyfr, klucz z kodu, AES-GCM tam i z powrotem, zmieniony szyfrogram/zły klucz | `fork/family/FamilyCryptoTest` (NOWE) |
| Źródła: obcy dubbing | Castellano/TRUEFRENCH na koniec, z angielskim/MULTI/PL zostaje, słowo z tytułu filmu | `fork/sources/SourceTiersTest` (NOWE) |
| Źródła: gotowe przed niegotowymi | torrent bez debrid, NOT_CACHED, ⬇/⏳/[RD download] | `fork/sources/SourceTiersTest` (NOWE) |
| Odtwarzacz: AVI/WMV w mpv | rozszerzenie w adresie/nazwie/typie, nie w zapytaniu, wyłącznik | `fork/sources/SourceTiersTest` (NOWE) |
| Źródła: zestawy reguł, język, napisy | reguły Regex, kreator | `fork/sources/*` (było) |
| Napisy: polskie najpierw | rozpoznanie polskiego kodu | `fork/subs/PolishFirstTest` (NOWE) |
| Napisy: MPL2/MicroDVD | konwersja do SRT | `fork/subs/TextSubtitleFormatsTest` (było) |
| Napisy AI: automat | brak polskich, polskie dobre, wybrany polski bez AutoSync, błąd obcego pliku | `fork/ai/subtitles/AiAutoTest` (NOWE) |
| Napisy AI: MKV | EBML (id, rozmiar, nieznany rozmiar, liczby), tekst ASS/SRT bez stylów, kody języków | `fork/ai/subtitles/MkvSubtitleTextTest` (NOWE) |
| Napisy AI: tłumaczenie, kontrakty, audyt | 5 plików | `fork/ai/*` (było) |
| Lektor: głośność | 100% = dawne 30%, skala rosnąca 10–300, ×2 = +10 dB, miękki ogranicznik, opisy ustawień | `fork/lektor/LektorVolumeTest` (NOWE) |
| Lektor: tempo mowy | uczy się tylko z sensownych nagrań | `fork/tv/LogoAndCondenseTest` (NOWE) |
| Lektor: silnik, mikser, ściszanie pasmowe | 4 pliki | `fork/lektor/*` (było) |
| Dublagem (polski dźwięk z innego wydania) | wybór ścieżki, czas | `fork/dublagem/DubbingSelectionTest`, `DubbingTimingTest`, `DubbingAudioSyncDspTest` (było) |
| TVDB | sezon/odcinek z etykiety (SxxEyy, 1x02, E12, xmltv_ns, cyfry) | `fork/tvdb/TvdbLabelTest` (NOWE) |
| YouTube: panel dźwięku | jeden zestaw na język (AAC przed Opus), najlepsza jakość, myślniki Unicode, zły manifest | `fork/yt/YouTubeAudioSlimTest` (NOWE) |
| Głos, odkrywanie, launcher, noc, polecane, rejestr, ustawienia, built-1h, obrazki | po 1–5 plików | (było) |

## Bez testów JVM (tylko obchód na TV)
Ekrany Compose (Niunia, Kicia, ustawienia), rodzina (synchronizacja sieciowa), sieć KE, serwer ustawień, odłączony kod
wyboru źródła (Echo, KE, Kicia — decyzja 22.11: autoplay jak w Reshaped, do usunięcia).
