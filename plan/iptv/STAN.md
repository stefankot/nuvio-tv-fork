# IPTV (Live TV) — stan (23.11)

## Zgłoszenie właściciela (23.11)
Start ekranu IPTV ~100 s (cel < 1 s), ulubione wolno i bez miniaturek, nazwy z dopiskami („Strong8K PL … HD”), zawieszenie przy
odtwarzaniu kanału, „Kanał nie odpowiada” bez wyjścia, gdy kanał już leciał. Prośba o przemyślenie całej architektury
(kolejni dostawcy FlixNest: Trex, Dream 4K, DINO, Neo 4K Pro — w 95% te same kanały).

Decyzja właściciela (23.11, ten wątek): „zajmij się wszystkimi poważnymi błędami, ale nie przebudową całego silnika teraz”.
**Przebudowa silnika (SQLite) — WSTRZYMANA**, kierunek niżej.

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
