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
