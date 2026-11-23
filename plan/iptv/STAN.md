# IPTV (Live TV) — stan (23.11)

## Zgłoszenie właściciela (23.11)
Start ekranu IPTV ~100 s (cel < 1 s), ulubione wolno i bez miniaturek, nazwy z dopiskami („Strong8K PL … HD”), zawieszenie przy
odtwarzaniu kanału, „Kanał nie odpowiada” bez wyjścia, gdy kanał już leciał. Prośba o przemyślenie całej architektury
(kolejni dostawcy FlixNest: Trex, Dream 4K, DINO, Neo 4K Pro — w 95% te same kanały).

Decyzja właściciela (23.11, ten wątek): „zajmij się wszystkimi poważnymi błędami, ale nie przebudową całego silnika teraz”.
**23.11 wieczór — WZNOWIONA przez kopię**: „Skopiuj zatem optymalizacje, ale nie usuń funkcji związanych z catch-up”
(po porównaniu forków `FORKI.md`). Projekt: `ARCHITEKTURA.md` (silnik przewodnika z NuvioPlusIPTV: SQLite + strumieniowy XMLTV).

## Pomiary JMGO 23.11 17:45–18:01 (APK napisyai5 z poprawkami 9055fee9f; `.stage/jmgo/iptv-pomiar-{1,2}`, `iptv-test-zapisu`)
- Pomiar 1 (pierwsze wejście po instalacji, nowy klucz zapisu, bez próbki): ekran 0,15 s, kanały hls.gd (326) 0,8 s, wszystkie
  59 323 kanały 6,6 s, start przewodnika 3,3 s, **program „teraz” po ~66 s**, pełny przewodnik ~3 min; sterta 40 → 204–230 →
  363–424 MB; **0 ANR**. Zapis przewodnika nie powstał (aplikację zamknął pomiar 2, ~40 s po końcu odczytu).
- Pomiar 2: próbka startowa 160 kanałów w 27 ms, ekran z próbką od razu, ale **próbka nadpisana wersją „przewodnik dla 0”**
  (zapis przed przewodnikiem + `IOException` przy dwóch zapisach naraz) — poprawione (`TvSnapshot`, bez builda); program ~64 s,
  szczyt sterty 469 MB; 0 ANR.
- Test zapisu (bez zamykania aplikacji; Home, 75 s, ponowne wejście): lista 5 s, wycinek „teraz” 15 s, pełny zapis ~26 s.
- Wniosek: zapis działa tylko, gdy pełny odczyt (~3 min) skończy się na ekranie telewizji; nawet wtedy 15–26 s. Obecny silnik
  nie zejdzie do < 1 s → baza przewodnika (`ARCHITEKTURA.md`).
- Niesprawdzone jeszcze: włączenie kanału w trakcie wczytywania (ANR), okno „Kanał nie odpowiada”, nazwy, ulubione.

## Etap 1 nowego silnika (23.11 ~18:10, commity c7b1874a9 kopia 1:1, 806d78413 wpięcie) — build `iptvdb1` v7a
- Kopie 1:1 NuvioPlusIPTV `e5643387` w `fork/tv/plus/` (EpgDatabase, XmltvParser, ProgrammeIdentityPool, SqliteCompact, PlusModels, PlusShims).
- `fork/tv/plus/GuideDbBridge` + łatka `dostawca/LiveTvRepository/15-baza-przewodnika` (2 linie: `startEpg` w `whileSeen`, `loadHistory`):
  start = ulubione + kanały próbki z bazy (−1 h…+6 h, z opisami) → pobranie starych plików jak Reshaped → do bazy tylko pliki
  zmienione (rozmiar+czas) albo z nowymi kanałami listy → linki kanałów regułami Reshaped (id > nazwa > nazwa z krajem; ranking
  GuidePriority/dodany przewodnik/kolejność źródła) → okno −3 h…+6 h, potem −12 h…+36 h („light”) → kategorie Kici z bazy.
  Catch-up: `loadHistory` czyta minione dni z bazy (7 dni w bazie), Xtream `pastProgrammes` bez zmian; Niunia woła
  `GuideDbBridge.need` (wiersz przewodnika cofnięty > 11 h, odtwarzacz) → `requestHistory`. Logi `ForkTvPlus`.
- Przełącznik „Przewodnik w bazie (szybki start)” (Telewizja, Zaawansowane, domyślnie tak); wyłączony = stary silnik.
- **iptvdb1 NA JMGO** (23.11 18:26 Mac; BUILD SUCCESSFUL 13 min 47 s; v7a md5 `9733678de93a78cad4eb5a35c08c44f2` zgodny, `compile -m speed`;
  kopia `.stage/apk/iptvdb1-v7a.apk`). Pomiar 1 (`.stage/jmgo/iptv-pomiar-db1`, pusta baza): import 5 plików 4 min 20 s
  (FlixNest 29 271 kanałów / 560 662 programy w ~95 s — DWA RAZY, ten sam plik pod dwoma adresami), linki 29 392 z 52 454 kluczy
  (jak dotąd ~29 tys.) w 19 s, okno −12…+36 h = 554 088 programów, kategorie 11 237 w 14 s; sterta 200–450 MB; 0 ANR.
  Pomiar 2 (`iptv-pomiar-db2`, baza pełna): **ulubione + próbka (153 kanały) z bazy w 237 ms** (3,8 s po wejściu — tyle Reshaped
  czyta listę kanałów; próbka startowa pokazuje ekran w 0,1 s), sterta stabilnie 260–290 MB (stary: 365–470), 0 ANR.
  Błędy adaptera (poprawione w 62685f992, build `iptvdb2`): każde wejście czytało przewodniki ponownie („N nowych kanałów listy” —
  wymagane kanały bez programów nie trafiały do bazy jako znane), drugi import tego samego pliku, okno „teraz” wszystkich kanałów
  dopiero po sprawdzeniu plików (teraz od razu z bazy).
- **iptvdb2 NA JMGO** (23.11 ~19:03 Mac; BUILD SUCCESSFUL 11 min 57 s; md5 `9eedcf5eb16add12df5cc31ff34ceb05` zgodny, `compile -m speed`;
  `.stage/apk/iptvdb2-v7a.apk`). Pomiar 1 (`iptv-pomiar-db3`, jednorazowe uzupełnienie bazy po iptvdb1): ulubione z bazy 0,4 s,
  „teraz” 28 846 kanałów po 8,8 s, sterta 290–310 MB, 0 ANR; zdublowany przewodnik NIE rozpoznany (bajty plików różne).
  **Pomiar 2 — stan ustalony** (`iptv-pomiar-db4`): ekran z próbką 0,6 s, program z bazy dla 159 kanałów (ulubione + pierwszy ekran)
  3,5 s po wejściu (zapytanie 259 ms; resztę czeka `startEpg` na listę kanałów Reshaped), wszystkie 59 323 kanały 8,4 s, „teraz”
  dla 28 846 kanałów 11 s, **wszystkie 5 plików „bez zmian” — zero czytania XML**, pełne okno (540 tys. programów) 72 s; sterta
  stabilnie 276–280 MB, szczyt 406 MB; 0 ANR.
  Test ręczny: włączenie kanału (RT HD) w trakcie importu — gra, 0 ANR; odtwarzacz dociąga archiwum catch-up z bazy
  (`archiwum kanału z bazy: 147 programów`); Wstecz ×2 → lista z fokusem na kanale. Nazwy: „Strong8K · PL: TVN STYLE ᴴᴰ (NA)”
  zostawała jako „Strong8K · PL: TVN STYLE” (separator „·”) — poprawione w 0187deb32 (+ test), zdublowany przewodnik
  rozpoznawany po liście kanałów — build `iptvdb3`.
- Ryzyka do sprawdzenia na JMGO: czas pierwszego importu 5 plików do bazy (strumieniowo, ale 600 tys.+ wierszy na słabym CPU),
  liczba kanałów z programem po linkach vs. dotychczasowe ~29 tys. (`ForkTvPlus: linki: N z M`), opis programu w Niuni dla
  kanałów spoza priorytetu (okno „light” bez opisów), dni w przyszłości > +36 h w wyborze dnia (tylko dla kanałów, które je mają).
- Do zrobienia potem: wzbogacanie z drugiego przewodnika (łatka dostawca #4 — dziś tylko w starym silniku), opis na żądanie
  (`EpgDatabase.program`), `TvTextIndex`/szukanie z bazy (`findByWords`), kanał kanoniczny (`ChannelDupes` na wspólnym gid),
  VOD (`OnDemandDatabase`) — `ARCHITEKTURA.md`.

## Przyczyny (zbadane z logów JMGO, bez nowego pomiaru — urządzenie było zajęte)
- **100 s**: każdy pomiar 23.11 (`.stage/jmgo/pamiec-20261123-1013`, `-1056`, `anr-20261123`) ma `saved programmes: miss (no file)`.
  Klucz zapisu przewodnika (`LiveTvGuideCache.key` × `liveTvGuideMatchingKey`) zawiera WSZYSTKIE kanały (klucze przewodnika,
  nazwy, archiwum), a lista Xtream dostawcy zmienia się co kilka godzin → zapis nie pasuje → 5 przewodników XMLTV od zera.
  Oś czasu ram1: lista 1–3 s po wejściu, pierwszy program „teraz” po 67 s, pełny przewodnik po ~130 s, sterta do 476 MB
  (ciągłe GC, ekran ospały). Próbka startowa `TvSnapshot` pomagała tylko dla 160 kanałów i bez obrazków.
- **Zawieszenie przy włączaniu kanału**: ANR 20:28 i 20:35 (czas TV) na ram1 (`.stage/jmgo/iptv-20261123/anr-all.txt`).
  Wątek główny: `LiveTvPlayerState.switchTo` → `prepareForPlayback` → `TvBridge.onPlay` → `TvStore.onWatched` czeka na blokadę;
  trzyma ją `TvStore.updateSchedule` (z `TvBridge.onGuideRead` przy każdej publikacji przewodnika), który liczył
  `ShowIdentity.normalizeTitle` (wyrażenia regularne) dla ~600 tys. programów pod blokadą. Drugi ANR: kompozycja przy pełnej stercie.
- **„Kanał nie odpowiada” bez wyjścia**: w części to samo zawieszenie (Spróbuj ponownie → `switchTo` → blokada); poza tym okno
  zostawało, gdy obraz ruszył później, a Wstecz wyrzucało do listy.
- **Ulubione bez miniaturek**: próbka startowa zapisywała programy bez obrazków (i była pokazywana do czasu przewodnika, czyli ~70 s);
  ulubiony kanał bez programu „teraz” nie miał kafelka wcale. Przewodnik FlixNest dla polskich kanałów sam nie ma obrazków
  (tylko tytuł+opis) — okładki TMDB w kafelkach Niuni to nowa funkcja (`fork/tv/ProgramCard`, `TvTmdb` gotowe, niewpięte).

## Zrobione (commit 9055fee9f, NIESKOMPILOWANE do czasu wspólnego buildu)
1. `TvStore.updateSchedule`: liczenie bez blokady (tytuły przez pamięć `TvBridge.programKey`), pod blokadą tylko podmiana.
2. `ForgeChannelFailed` (Kicia): gdy kanał gra pod oknem (`PlayerBridge.current().isPlaying`, bez błędu) — okno znika samo
   (sprawdzane co 1 s), Wstecz i OK („Spróbuj ponownie”) tylko je chowają, bez restartu i bez wychodzenia z kanału.
3. `fork/tv/guide/GuideReuse` + łatka `dostawca/LiveTvRepository/14-zapis-przewodnika`: klucz zapisu tylko z adresów przewodników,
   id źródeł i okna. Przełącznik „Szybki start przewodnika” (Telewizja, Zaawansowane, domyślnie tak). Pierwsze wejście po instalacji
   jeszcze czyta wszystko (nowy klucz), kolejne — zapis od razu. Kanały dodane u dostawcy dostają program przy następnym pobraniu
   przewodnika (co 12 h albo „Odśwież”).
4. `fork/tv/names/ChannelNames` („Same nazwy kanałów”, Telewizja, domyślnie tak): „PL: Polsat Cafe HD” → „Polsat Cafe”,
   „Strong8K: PL| TVN 7 FHD” → „TVN 7”, „Active Family HD (1080i)” → „Active Family”; tylko wyświetlanie w Niuni (kafelki, logo
   bez obrazka, opis pod przewodnikiem, odtwarzacz). Test `ChannelNamesTest`.
5. Ulubione: każdy ulubiony kanał ma kafelek od razu (logo i nazwa, gdy brak programu; z obrazkiem — na początku);
   `TvSnapshot` v2 z obrazkami programów (`snapshot_v2.bin.gz`; v1 zostaje nieczytany).

## Do sprawdzenia na JMGO po wspólnym buildzie (gdy nikt nie ogląda)
- `fork-tools/test/jmgo_pamiec.sh 240 reshaped_live_tv` DWA razy (pierwszy: zapis pod nowym kluczem; drugi: w logu
  `ForkTvGuide: zapis przewodnika: klucz z 5 przewodników…`, `saved programmes` bez „miss”, `now slice used`, `ForkNiunia: live tv:
  filtr=wszystkie, kafelki=N` z N>0 w ciągu kilku s od wejścia). Zapisać czasy: lista, program „teraz”, pełny przewodnik, sterta.
- Włączenie kanału w trakcie wczytywania przewodnika — 0 ANR (`dumpsys dropbox | grep data_app_anr`).
- Martwy kanał → „Kanał nie odpowiada”; gdy obraz ruszy — okno znika samo.
- Nazwy w przewodniku i kafelkach bez „PL:” i „HD”; Ulubione — kafelki od razu.

## Kierunek przebudowy (gdy właściciel ją wznowi)
Dane telewizji na dysku w SQLite (`android.database.sqlite`, FTS4; zgoda 22.11): kanał kanoniczny = grupa duplikatów u dostawców
(`fork/tv/dupes/ChannelDupes` + `DupeMemory`, gotowe, niewpięte), warianty jakości pod spodem, start najlepszej kopii i przełączenie
na następną; import XMLTV strumieniowo do bazy, przyrostowo, po kluczu kanału przewodnika (niezależnie od listy kanałów);
w RAM tylko strona ekranu + gotowa tabela „ekran startowy” (każda kategoria i ulubione: ~10 kanałów, program ±12 h, obrazki),
przeliczana w tle. UI Niuni (`NiuniaLiveTv`, `NiuniaLiveOrder`, `ForgeEpgCategories.filter`) czyta strony zapytaniami.
Szczegółowy projekt jeszcze nienapisany (`plan/iptv/ARCHITEKTURA.md` — do zrobienia przy wznowieniu).
