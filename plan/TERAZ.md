# TERAZ — bieżący stan i kolejka („kontynuuj” zaczyna tutaj)

Zasady: `ZASADY.md`. Mapa dokumentów: `plan/README.md`. Historia wątków 1–13: `plan/archiwum/TERAZ-historia-watki-1-13.md` (tylko `grep`, nie czytaj całości). Sekcje z 22.11 (build163–169c, scalenie, aktualizacja bazy): `plan/archiwum/TERAZ-historia-22.11.md`.
Utrzymanie pliku: nowy wątek dopisuje sekcję „NAJPIERW” na górze, a sekcje nieaktualne przenosi na koniec pliku historii w archiwum. Cel: ten plik ma mieć poniżej ok. 20 KB.

## NAJPIERW (22.11 ~19:10, wątek „Live TV przy 252 tys. kanałów + build171”) — po „kontynuuj” rób to, po kolei, bez pytań
Polecenie właściciela 22.11 nadal obowiązuje: testy jednostkowe dla każdej funkcji, poprawki dużymi paczkami (nigdy build dla jednej zmiany), każdą kompilację testować ≥ 10 min na JMGO.

STAN:
- **build171 NA JMGO** (commit 2ffee0953 = f9fe533a7 + dokumenty, v7a, md5 39da94c67a31c8bd55597646c73593b7 zgodny po `adb pull`, `compile -m speed`). Testy JVM: 4668, 1 nieudany — `ProfileSettingsPluginSyncTest > independent profiles…` (test bazy, MockK `Dispatchers.getIO()`); uruchomiony sam (`test171`) PRZECHODZI → niestabilny w pełnym przebiegu, nie z paczki.
- **Przyczyna problemów Live TV z build169c** (ustalona simpleperfem i śladem ANR z dropboxa): na JMGO są 3 źródła — hls.gd (M3U, 326), live.flixnest.app Xtream (59 023) i live.flixnest.app M3U (193 478) = 252 827 pozycji, bez ukrytych grup (właściciel ma aplikację FlixNest — źródła zostają, DECYZJE 22.11). (1) `fork/tv/guide/EpgSources.matches` kompilował `Regex("[^\\p{L}\\p{N}]+")` na kanał × podpowiedź (~1,3 mln kompilacji ICU na każde przeładowanie listy) → wątek w tle 16 min CPU, odśmiecanie „NativeAlloc” co ~2,5 s, prawdopodobnie też wolne ekrany (rozgrzewka Live TV rusza przy starcie, JMGO ma 3,8 GB). (2) ANR 01:18 i 23:12: `ForgeEpgCategories.polishFirst` na wątku UI. (3) „Brak programu”: przewodnik uzupełniający epg.ovh wykrywany dla wszystkich list razem — po dodaniu FlixNest udział PL < 30% → hls.gd bez epg.ovh.
- W build171 (f9fe533a7): EpgSources bez kompilacji na kanał (252 827 kanałów: 925 ms), Niunia Live TV liczy kolejność/kategorie/numery w tle z pamięcią wyniku (`NiuniaLiveOrder`), `norm()` bez Regex, CatchupDepth poza UI. Sprawdzone ręcznie na JMGO 01:41–01:46: bez ANR, fokus chodzi, kafelki=15, programy CNBC/EuroNews/BBC widoczne; kolejność 252 827 kanałów w tle 143 s; jedno przycięcie 1,4 s przy wejściu (mapa numerów i kafelki dla 252 tys. na wątku UI).
- `fork-tools/test/jmgo_sesja.sh` liczy teraz ANR także z dropboxa (logcat się przewija — build169c miał „ANR: 0” przy dwóch ANR) i zapisuje `exit-info.txt`.
- Wypchnięte na GitHuba 22.11 ~19:00 na polecenie właściciela (skin-niunia = cf0fef9a6 i nowsze); wcześniej `git push` zablokował automatyczny filtr uprawnień.

PACZKA DO NASTĘPNEGO BUILDA (zacommitowane 1650ce2e7, NIESKOMPILOWANE): przewodnik uzupełniający osobno dla każdej listy (`EpgSources.detectPerList` — hls.gd znów dostanie epg.ovh), `ForgeEpgCategories` z jednym Matcherem na regułę i wątek (cel: 143 s → kilkadziesiąt s); testy EpgSourcesTest +2, EpgChannelOrderTest +1.

WYNIK OBCHODU build171 (jmgo_sesja.sh 10): WYNIK_OBCHODU

KOLEJKA (po kolei):
1. Błędy z obchodu build171 (wyżej) → do paczki.
2. Przycięcie 1,4 s przy wejściu do Live TV: `NiuniaLiveOrder.unsorted` (mapa 252 tys. numerów) i `recentTiles`/`categoryTiles` (mapy po całej liście) na wątku UI — przenieść do `NiuniaLiveOrder` (mapy byUrl/byGuide raz na listę) albo liczyć numery leniwie.
3. Jeden build całej paczki: `bash fork-tools/build.sh build172 :app:testFullDebugUnitTest :app:assembleFullDebug -PforkAbi=armeabi-v7a --continue` (przed buildem `df -h ~`; 22.11 było 11 GB — build171 przeszedł). Cel testów: 0 nieudanych poza niestabilnym `ProfileSettingsPluginSyncTest` (jeśli znów padnie — dopisać do `fork-tools/test/wykluczone-testy-bazy.txt` z powodem „niestabilny: statyczny mock Dispatchers”).
4. Instalacja na JMGO tylko gdy nikt nie ogląda (`dumpsys media_session` bez `state=3`), md5 po `adb pull`, `compile -m speed`.
5. Test ≥ 10 min `bash fork-tools/test/jmgo_sesja.sh 10` + ręcznie Live TV: log `ForkTv: przewodnik uzupełniający: PL … z 3 list`, w Źródłach hls.gd „guide for ~189”, programy przy polskich kanałach, czas logu `kolejność i kategorie … w tle`, liczba GC/min w logcat; potem `plan/DO_SPRAWDZENIA_NA_TV.md` (sekcja build171 i starsze: autoplay Regex, przejście na inne źródło, AutoSync, Dublagem, lektor).
6. Wolne ekrany (Kalendarz 8,1 / YouTube 8,0 / Główna 6,3 s w build169c) — porównać z raportem crawl.py po build171/172; jeśli nadal > 5 s i przyczyna nieoczywista → `plan/BLOKADY.md`.
7. Pozostałe koszty dużej listy w Reshaped (poza forkiem, tylko gdy test pokaże problem): `LiveTvGuideRequest.from` (Regex na nazwę kanału, 252 tys.), czytanie przewodnika FlixNest (`LiveTvEpg`), pamięć ~1 GB PSS.

## Polski dźwięk z innego wydania na JMGO (21.11)

APK ARM32 z `21087fcbc` jest na projektorze. Zawiera poprawkę Auto Quality i automatyczny dobór audio oparty na Dublagem. Próby ExoPlayer na JMGO potwierdziły 4K z polskim AC3 5.1, korektę +600 ms, przewijanie, odrzucenie złego dopasowania i powrót do oryginalnego audio po utracie pliku. [Opis zmian, testów i przypięcia upstreamu](autoplay/POLSKI_DZWIEK_DUBLAGEM.md).

## Jakość po włączeniu lektora na JMGO (21.11)

APK ARM32 z poprawką `ff8a3906a` przetestowany na projektorze. 4K pozostało po krótkich przerwach i przewinięciu. Długi zastój dał wznowienie w 1080p od 17,831 s. [Pomiary i opis](autoplay/AUTOJAKOSC_PO_LEKTORZE.md).

## Autoodtwarzanie na JMGO (21.11, po buildzie 162o)

Na JMGO jest APK ARM32 z poprawką autoodtwarzania i napisów (`25b549162`, scalenie `2f6fab0a1`, MD5 `7e3a155cc2304eba31be558fe8ec9049`). Na fizycznym projektorze sprawdzono zmianę źródła przy braku obrazu, wznowienie po awarii HLS, wyczerpanie prób i użycie polskich napisów z innego wydania. [Opis zmian i pomiary](autoplay/AUTOODTWARZANIE_I_NAPISY.md). Kolejka paczki poniżej pozostaje w historii wątku.

## OTWARTE Z WCZEŚNIEJSZYCH WĄTKÓW (zebrane 19.11 przy archiwizacji; przed robieniem sprawdź w kodzie lub logu, czy nadal aktualne)
- Czeka na decyzję właściciela: emocje lektora (Gemini TTS Tier 2 — formularz niewysłany / ElevenLabs / zostaje Chirp); 4K na ekranie 1080p (`KeRanking.tvCap` celowo odsuwa 4K); ZONOS3 (lokalny głos, `ai-narrator/`, nieskompilowany); kod bazy: `CollectionsDataStore.parseCollections` (7% wątku głównego) i `UpdateBannerHost` (5%; aktualizator miał być usunięty); W1 (test/limit łącza KE) nierozpoczęty; przenoszenie czegokolwiek z innych forków tylko po potwierdzeniu.
- Zaległe: serwer logów (`server/family-sync/panel_api.php`, `panel/app.js` — wgrać przez DirectAdmin, właściciel musi być zalogowany); `ForkLog.say` przy czynnościach, które „nic nie robią”; szum `ForkTv TvStore.kt JsonDecodingException EOF` (pusty plik = pusta lista, bez logu).
- Niesprawdzone na TV: okno napisów na starcie filmu (lektor auto), Baahubali ▲ (rząd kolekcji), Calendar Wstecz, kafle bez ikonki typu, zmiana ścieżki dźwięku YouTube.
- Odłożone (Kicia, głębokie zmiany odtwarzacza): nauka `SmartOutcomeStore`, rozgrzewka `ForgeWarmUp`, bufor `PlayerBufferTuning`, ciche P2P, restart w mpv po błędzie kontenera.
- Nieśledzone `plan/raporty/jmgo-*.log` — nie commitować bez przejrzenia (mogą zawierać treści odpowiedzi OkHttp).
