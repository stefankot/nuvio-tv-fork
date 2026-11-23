# IPTV — nowy silnik przewodnika (projekt, 23.11)

Decyzja właściciela (23.11 wieczór): „Skopiuj zatem optymalizacje, ale nie usuń funkcji związanych z catch-up”.
Źródło kopii: homelessbrian/NuvioPlusIPTV @ `e5643387f7029374f7deffe1618a544b3a4396a3` (porównanie: `FORKI.md`).

## Dlaczego obecny silnik nie da < 1 s (pomiary JMGO 23.11 17:45–18:01, `STAN.md`)
Reshaped czyta 5 przewodników XMLTV (~29 tys. kanałów, okno 7 dni wstecz / 8 naprzód) do jednej mapy w RAM: program „teraz”
po ~65 s, całość po ~3 min, sterta 40 → 470 MB. Zapis na dysk powstaje dopiero po pełnym odczycie (wyjście wcześniej = brak zapisu),
a nawet odczyt zapisu to 15 s („teraz”) i ~26 s (całość). Każde pobranie przewodnika (co ~10 h) = znowu pełne parsowanie.

## Cel
Pierwszy rysunek z programem < 1 s, sterta w Live TV ≪ 300 MB, przewodnik czytany z XML tylko, gdy plik się zmienił,
catch-up i archiwum działają jak dziś.

## Kopie 1:1 (pakiet `com.nuvio.tv.fork.tv.plus`, zmiana tylko `package`/`import`)
- `livetv/data/EpgDatabase.kt` → `fork/tv/plus/EpgDatabase.kt` (baza `livetv_guide2.db` → nazwa bez kolizji: `fork_tv_guide.db`,
  jedyna dozwolona zmiana poza pakietem; zapisana w `UPSTREAM_SOURCES.md`).
- `livetv/data/XmltvParser.kt`, `ProgrammeIdentityPool.kt`, `SqliteCompact.kt` → `fork/tv/plus/`.
- Z `livetv/model/LiveTvModels.kt` tylko `EpgProgram`, `SUB_TITLE_MARK`, `subTitle`, `EpgAssignment` → `fork/tv/plus/PlusModels.kt`.

## Adaptery forka (nowe pliki, `fork/tv/plus/`)
1. `GuideDbImport` — po pobraniu plików przez Reshaped (`guide_<hash>.xml.gz`): dla pliku o zmienionym znaczniku (rozmiar+czas)
   strumieniowo `XmltvParser.parse(onPrograms = writer.add)` → `EpgDatabase.startSource(...)`. Okno zapisu jak dziś (7 dni wstecz
   dla kanałów z catch-up, 8 naprzód). W tle, `Dispatchers.IO`, przerywane tylko przy odtwarzaniu filmu; trwa po wyjściu z ekranu.
   Pamięć: jeden kanał naraz. Sterta > 85 % → przewodnik na później (jak NuvioPlusIPTV).
2. `GuideDbLinks` — kanał Reshaped (`guideKey`) → (plik, id XMLTV): najpierw `tvg-id`, potem nazwa (`liveTvNameKey`, ranking
   „własny przewodnik listy wygrywa” jak `LiveTvGuideRequest`), `tvg-shift`. `applyGuide(...)` raz na zmianę listy/przewodnika.
   Duplikaty u 5 dostawców FlixNest dzielą jeden `gid` → jedna kopia programu.
3. `GuideDbWindow` — odczyt: priorytet (ulubione + pierwsze kanały kategorii z ekranu, `TvSnapshot`) −1 h…+4 h natychmiast,
   reszta −3 h…+12 h „light” w tle; konwersja `EpgProgram` → `LiveTvProgramme` (tytuł, czasy, odcinek, obrazek).
   Opis programu: `program(ch, start)` dla kanału w nagłówku/odtwarzaczu.

## Wpięcie w Reshaped (łatka `dostawca/LiveTvRepository/15-baza-przewodnika`)
- `startEpg`: gdy moduł włączony i baza gotowa (odcisk = pliki + linki) → `GuideDbWindow` zamiast `LiveTvEpg`/`LiveTvGuideCache`;
  `publishGuide` bez zmian (TvBridge, kategorie, indeks, UI dostają tę samą mapę, tylko mniejszą).
  Baza niegotowa (pierwszy raz) → wycinek „teraz” jak dziś + import w tle; po imporcie okno z bazy.
- **Catch-up bez zmian w działaniu**: `requestHistory(channel)` (dziś `LiveTvArchive`) czyta minione dni kanału z bazy
  `channelRange(ch, now − dni archiwum, now)`; linki catch-up (`LiveTvCatchupLinks`), „od początku”, `pastProgrammes` z Xtream
  zostają. Archiwum plikowe zostaje jako zapas (wyłączony moduł = dokładnie jak dziś).
- Wyłączony przełącznik „Szybki przewodnik (baza)” (Telewizja, domyślnie włączony) = obecny silnik.

## Etapy (każdy: build v7a, pomiar `jmgo_pamiec.sh 240 reshaped_live_tv`, wpis w `STAN.md`)
1. Kopie + `GuideDbImport` + `GuideDbLinks` + `GuideDbWindow` + łatka 15; test JVM linków; pomiar: 2. wejście < 1 s z programem.
2. Catch-up z bazy (`requestHistory`), opis z `program()`, szukanie programów (`TvTextIndex`) z bazy (`findByWords`).
3. Kanał kanoniczny (`ChannelDupes`) na wspólnym `gid` — jeden wiersz na kanał, start najlepszej kopii, zapasowa przy błędzie.
4. VOD dostawcy: `OnDemandDatabase` (kopia) jako katalog/źródło — tylko na polecenie właściciela.
Później do rozważenia (nowe funkcje z `FORKI.md`): pauza/cofanie bez catch-up (`LiveTvTimeshift`), nagrywanie.

## Etap 1c — pozostałe optymalizacje NuvioPlusIPTV (właściciel 23.11 ~20:00: „Masz skopiować wszystkie”)
Przegląd `.stage/forki/NuvioPlusIPTV/src/app/src/main/java/com/nuvio/tv/livetv/` (e5643387). Zrobione: baza przewodnika,
strumieniowy XMLTV, import tylko zmienionych plików, priorytet (ulubione + ekran), okno przy wejściu, wiersze na żądanie,
kategorie w tle, próg pamięci (częściowo: brak), duplikaty jednym `gid`. DO ZROBIENIA (kolejność wg zysku):
1. **Zapisana lista kanałów** (`data/LiveTvRepository.kt` 810–1055: `channels_cache3.bin`, `loadSavedChannels`, zapis binarny,
   „Live TV appears straight away instead of re-reading every playlist file”): u nas Reshaped czyta 59 tys. kanałów 3,6–8,4 s
   przed startem przewodnika. Kopia formatu + adapter `LiveTvChannel` (łatka w miejscu odczytu listy Reshaped `LiveTvListCache`).
2. **Praca w tle o niskim priorytecie, wstrzymywana przy oglądaniu** (`data/BackgroundWork.kt`, 72/152: wątki z priorytetem 9,
   `browsingRead` z punktami kontrolnymi — odczyt z bazy oddaje kursor, gdy rusza film). U nas: `fullPass`, import, kategorie.
3. **Próg pamięci** (`LiveTvRepository.kt` ~2500: >85 % sterty → GC, nadal >85 % → przewodnik na później) — w `GuideDbBridge.importFile`.
4. **Zapas godzin przy dociąganiu** (`LiveTvRepository.kt` 1392–1400: okno +kilka h, żeby przewijanie nie pytało co 30 s).
5. Przebudowa listy wyświetlanej raz dla całej aplikacji (`LiveTvChannelStore.kt`: pamięć nazw po edycji, „rename took 20–30 s”).
6. Szukaj programów z bazy (`EpgDatabase.findByWords`) zamiast `TvBridge.lastKept` (pełna mapa w RAM, ~80 MB).

## Etap 3 — VOD (właściciel 23.11 ~20:00: „mechanizm który obsługuje VOD, bo mam 160k materiałów wideo”)
NuvioPlusIPTV `livetv/ondemand/` (~7 tys. linii): `OnDemandDatabase` (SQLite `livetv_on_demand.db`, tabela `items`
(pl, kind, id, name, norm, icon, cat, ext, tmdb, year, rating, added, genre…), indeksy po kategorii/nazwie/tmdb/dacie,
`m3u_episodes`, `matches`, `posters`; strony zapytaniami), `OnDemandRepository` (import przyrostowy — tylko zmienione kategorie,
po kategorii gdy panel ucina listę, niski priorytet w tle, wynik „aside at once”), `OnDemandSearch`, `OnDemandStreams`,
`VodPictures` (plakaty), `VodLanguageTags`, `OnDemandAdultGuard`; UI `OnDemandScreen/HomeScreen/SeeAll/ViewModel`.
Plan: kopia 1:1 warstwy danych do `fork/tv/plus/ondemand/` (pakiet, zależności → adaptery: playlisty z `LiveTvRepository` źródeł,
HTTP z `LiveTvHttp`), UI w Niuni (kategorie, strony, szukanie, plakaty TMDB), odtwarzanie przez odtwarzacz Nuvio; filmy i odcinki
także jako źródła przy tytułach (jak dawniej „obok Torbox”). Lista M3U VOD: właściciel dodaje ją sam w aplikacji (QR / telefon,
„Źródła i EPG”) — wykonawca NIE wpisuje adresów z tokenem konta (zasady bezpieczeństwa, jak hasła dostawców); `LiveVod` trzyma
je poza Live TV, On Demand importuje do bazy.

### VOD — zależności kopii (zbadane 23.11 ~20:20, NuvioPlusIPTV e5643387 `livetv/ondemand/`, 6 469 linii)
Warstwa danych (do kopii 1:1 → `fork/tv/plus/ondemand/`): `OnDemandDatabase` 1153, `OnDemandRepository` 1216, `OnDemandStreams` 244,
`OnDemandLanguages` 216, `OnDemandSearch` 166, `VodLanguageTags` 84, `VodPictures` 83, `VodMatchJson` 66, `OnDemandAdultGuard` 54.
UI (raczej Niunia od nowa, wzór): `OnDemandViewModel` 1349, `OnDemandHomeScreen` 656, `ManageVodGroupsPanel` 399, `OnDemandSeeAll` 206,
`OnDemandProviderDialog` 200, `OnDemandHome` 188, `OnDemandPlay` 92, `VodSortPicker` 90.
Zależności spoza pakietu (adaptery/shimy w `fork/tv/plus/`): `LiveTvLoadReport.add` → `PlusShims.report`; `BackgroundWork.{quiet,
awaitIdle,awaitNoHeavy}` → wątek niskiego priorytetu `GuideDbBridge.background` + czekanie, gdy gra odtwarzacz (`PlayerBridge`);
`LiveTvPreferences` + `PlaylistSource` (lista playlist) → źródła Reshaped (`LiveTvRepository` `_uiState.sources`, typ M3U/Xtream,
login Xtream z adresu jak `LiveTvCatchupLinks.xtreamLogin`); `LiveTvRepository.isVodEntry` → `fork/tv/vod/LiveVod`; `M3uParser` →
`LiveTvPlaylistParser` Reshaped albo kopia `data/M3uParser.kt`; `ForkHttp`/`ForkDns` → `LiveTvHttp`; `boundedCache` (kopia
`data/BoundedCache.kt`); `SqliteCompact` (już skopiowany); `StalkerGateway` (portale Stalker — pominąć, brak u właściciela);
`ParentalControls.isAdult/isLocked` → stała „nie” albo kontrola rodzicielska forka; `LiveTvPosterResolver.Hit` + `TmdbRecommendationResult`
→ TMDB forka (`fork/tv/TvTmdb`). Odtwarzanie: `OnDemandStreams` → `Stream` Nuvio (odtwarzacz bazy) i źródła przy tytułach.
