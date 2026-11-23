# IPTV w innych forkach — porównanie (23.11, zlecenie właściciela)

Pytanie właściciela: jak inne forki obsługują dużo kanałów i VOD (160 tys. pozycji); woli skopiować gotowe rozwiązanie.
Kopie (shallow, tylko do odczytu, poza gitem): `.stage/forki/<nazwa>/src`.

| Fork | Wersja sprawdzona | IPTV | Przewodnik (EPG) | Kanały | VOD |
|---|---|---|---|---|---|
| ysosrs123/NuvioTV-Fork (Arc) | `90fd0be` 18.11 | brak | — | — | — |
| DavidVamaiotu/NuvioTV-Reshaped | tag `1.2.0-beta.3-autosync.3` (`78b8904`, 20.11) | tak (nasza baza Live TV) | cały w RAM (mapa programów), zapis gz | RAM | odsiew z listy |
| Arshad-Kamal/Nuvio-KE | `36f601f` 19.11 (1.2.2) | tak (Pluto/Plex/M3U, „paczki”) | tylko dziś+jutro, JSON w RAM | RAM | brak |
| zoga2o3-ops/Nuvio-One | `3dfa4b0` 20.11 | tak — ten sam kod co niżej (starszy) | SQLite | RAM + zapis | SQLite |
| homelessbrian/NuvioPlusIPTV | `e5643387f7029374f7deffe1618a544b3a4396a3` 23.11 | **tak, najpełniejsze** | **SQLite** | RAM + zapis | **SQLite, strony** |

Reshaped: katalog `reshaped/livetv` w najnowszym tagu identyczny z naszą kopią (`upstream/reshaped` 0ca5a08c2) — nic nowego.
KE: przewodnik tylko na 2 dni, cały w pamięci — dobre dla małych list, nie dla 59 tys. kanałów.

## NuvioPlusIPTV (= Nuvio One, licencja GPL-3 jak Nuvio) — jak to robi
- `livetv/data/EpgDatabase.kt` (1174 linii, `android.database.sqlite`, WAL): tabela `listings(gid, start, stop, title, descr, cat, ep, icon…)`
  z jednym indeksem `(gid, start)`; `guide_ch` = kanał przewodnika (plik + id XMLTV), `chan` = kanał z listy → kanał przewodnika
  (+ przesunięcie czasu). Duplikaty (HD/SD/zapasowe, ten sam kanał u dwóch dostawców) dzielą JEDNĄ kopię programu.
  `guide_src.stamp` — plik przewodnika, który się nie zmienił, nie jest czytany ponownie.
- `XmltvParser.kt` (435 linii): XmlPullParser, oddaje programy kanału, gdy kończy się jego blok (`onPrograms`) — prosto do bazy,
  bez całego przewodnika w pamięci; teksty `intern()`, opisy do 800 znaków.
- Wejście do Live TV: `preReadGuide` — kanały priorytetowe (ostatnia grupa + ulubione, ≤ 600) od −1 h do +4 h, „od razu”;
  potem reszta okna −3 h…+12 h „light” (bez opisów i obsady); szczegóły programu (`program(ch, start)`) dopiero dla oglądanego.
  Archiwum kanału: `channelRange(ch, from, to)` na żądanie.
- Przed każdym kolejnym przewodnikiem: sterta > 85 % → GC, nadal > 85 % → przewodnik pominięty (stary zostaje), bez zawieszenia.
- Odczyt przewodnika trwa dalej po wyjściu z ekranu („next time it opens straight away”), wstrzymywany podczas oglądania filmu.
- VOD: `ondemand/OnDemandDatabase.kt` — tabela `items` (100 000+ tytułów u dostawcy), czytana stronami; import przyrostowy
  (zmienione kategorie, nie cały katalog), pobieranie po kategorii, gdy panel ucina dużą listę; dopasowanie TMDB, plakaty, języki.

## Czego oni mają, a my nie (IPTV)
- **Pauza i cofanie kanału na żywo bez catch-up dostawcy** (`player/LiveTvTimeshift.kt`, `TsSegmenter`, `HlsPieces`): po pauzie kanał
  nagrywa się do pamięci podręcznej, odtwarzanie idzie za „na żywo”, „Na żywo” wraca.
- **Nagrywanie** (`record/*`: harmonogram, folder sieciowy SMB, Dysk, oglądanie w trakcie nagrywania).
- **Multiview** — do 4 kanałów naraz.
- **Sport** — mecze z przewodnika na Twoich kanałach; **Kalendarz** seriali/filmów/meczów; przypomnienia; kontrola rodzicielska.
- **Kanały 24/7** — okładki i opisy z TMDB („24/7 Seinfeld” → serial).
- **VOD dostawcy w bazie** (filmy i seriale Xtream/M3U jako katalog „On Demand”, strony, szukanie, TMDB).
- Naprawa „krzywych” playlist HLS (`LiveHlsRepair`), menu kopii zapasowych kanału (`BackupChannelMenu`), portale Stalker, VPN.

## Catch-up — porównanie
U nas (Reshaped + fork): formaty `default/append/shift(timeshift)/flussonic(fs)/xc`, strefa czasowa panelu Xtream, powtórka jako HLS
(przewijanie), archiwum minionych dni na dysku (`LiveTvArchive`), dociąganie starszych programów z Xtream (`pastProgrammes`),
„od początku”. NuvioPlusIPTV ma te same formaty plus `catchup-correction` (poprawka godzin z listy) i catch-up portali Stalker.
**Lepszego catch-up nie mają**; nowością jest pauza/cofanie kanałów BEZ catch-up (timeshift lokalny) i nagrywanie.
