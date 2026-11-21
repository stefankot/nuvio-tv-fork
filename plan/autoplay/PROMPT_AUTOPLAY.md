# Prompt: analiza i naprawa autoodtwarzania i zmiany źródła (Nuvio TV fork)

Skopiuj wszystko poniżej linii do innego modelu z dostępem do GitHuba.

---

## Rola i cel

Jesteś doświadczonym programistą Android/Kotlin (Media3/ExoPlayer, Jetpack Compose for TV). Przeanalizuj i napraw **automatyczny wybór źródła przy starcie (autoplay)** oraz **automatyczną zmianę źródła w trakcie odtwarzania** w prywatnym forku Nuvio TV. Właściciel ocenia to tak: „bardzo źle działa, nieużywalne — w tym stanie lepiej, żeby tego nie było wcale”.

Cel: start filmu ma być szybki i przewidywalny. Pierwsza próba ma trafiać w źródło, które naprawdę gra obraz i dźwięk. Zmiana źródła w trakcie ma zachodzić tylko wtedy, gdy jest konieczna, i tylko na źródło sprawdzone. Jeśli nie da się tego zrobić dobrze, zostaw tryb ręczny (lista źródeł) jako domyślny, a autoplay za przełącznikiem.

## Repozytorium

- `https://github.com/stefankot/nuvio-tv-fork` (prywatne), gałąź **`claude/paczka-oczywiste`**. To najnowszy stan, zainstalowany na TV jako build162o.
- Aktywna gałąź równoległego wątku (skórka Niunia): `claude/nuvio-tv-handoff-batch-1-4537d7`. Nie scalaj do `main`, nie rób `push --force`. Pracuj na nowej gałęzi od `claude/paczka-oczywiste`, np. `claude/autoplay-fix`, i otwórz PR do `claude/paczka-oczywiste`.
- Zasady projektu: `ZASADY.md` (obowiązkowe), mapa dokumentów: `plan/README.md`. Najważniejsze:
  - kod bazy i kopii z forków zmieniaj minimalnie;
  - nowe rzeczy w osobnych plikach `fork/…`;
  - wpięcia w bazę to jedna instrukcja z komentarzem `// Fork hook: <moduł>` plus wiersz w `fork-tools/hooks.tsv`;
  - `bash fork-tools/check.sh` musi przejść (limit plików z wpięciami: 65, teraz 64);
  - każda funkcja za przełącznikiem `ForkFeatures`;
  - logi `android.util.Log` z tagiem `Fork<Moduł>`, bez połykania wyjątków;
  - teksty dla użytkownika po polsku.
- Kopie upstream są przypięte w `UPSTREAM_SOURCES.md` (SHA), np. Echo `e952419`, KE `0a87a08`, Kicia `34c80c6`.

## Urządzenie i środowisko

- Główny TV: JMGO, Android 11, armeabi-v7a, słaby procesor, MediaTek, dekoder sprzętowy `OMX.MS.*`, sieć ~8 MB/s.
- Źródła głównie z debridu TorBox przez dodatki Stremio: StremThru Torz, Torrentio TB, Comet/ElfHosted TB, MediaFusion, TorrentsDB. To często 600–800 źródeł na tytuł.
- Profil chce polskiego dźwięku (`pl_if_good`), inaczej polskich napisów lub lektora AI.

## Co dziś jest w kodzie (punkty wejścia)

Autoplay i wyścig źródeł (kopia Echo):
- `app/src/main/java/com/nuvio/tv/core/player/AutoPlayRace.kt` — wyścig: sprawdzenie pierwszych bajtów kilku źródeł, wygrywa pierwsze, które „dało wideo”; HLS, zakres bajtów.
- `ui/screens/player/PlayerRuntimeControllerAutoPlayHedge.kt`, `PlayerAutoSourceRecovery.kt`, `PlayerRuntimeControllerErrorRecovery.kt`, `PlayerRuntimeControllerSourceFallback.kt`, `PlayerRuntimeControllerSourceSwitch.kt`, `PlayerRuntimeControllerQualityFallback.kt`, `PlayerRuntimeControllerStreams.kt` — odzyskiwanie po błędzie lub przekroczeniu czasu startu (`AUTO_SOURCE_RECOVERY: startup_timeout / startup_error / trying next eligible source`), zmiana źródła (`SOURCE_SWITCH`), zmiana jakości.
- `ui/screens/stream/StreamScreenViewModel.kt`, `ui/navigation/SourceSelectionRestorePolicy.kt` — ekran źródeł i powrót do niego.

Echo w forku:
- `fork/echo/EchoRaceHook.kt`, `EchoRaceCandidates.kt`, `EchoSourcePreload.kt`, `EchoAutoQualityGuard.kt` (auto jakość w dół tylko po zacięciu), `AutoQualityBackground.kt`, `FastStart.kt` (start po 1,5 s bufora).

Ranking i kontrole forka:
- `fork/sources/KeRanking.kt` — kolejność: polski dźwięk, napisy, „gotowe”, obcy dubbing, wolny pośrednik 4K (`SlowProxy4k.kt`); log `ForkRanking`.
- `StartupChecks.kt` — po starcie: zła długość (`WRONG_DURATION`), dekoder, brak dźwięku.
- `TryAnotherSource.kt`, `SourceReady.kt`, `ReadyPick.kt`, `TrackScout.kt` — sonda ścieżek (języki audio/napisów).
- `Placeholders.kt` (NOWE 21.11) — klip „not cached yet” poniżej 80 MB lub krótszy niż 3 min przegrywa wyścig i spada na koniec; wpięcia w `AutoPlayRace.probe` i `pick`.
- `kicia/SmartBadSources.kt` — pamięć złych źródeł.

Przełączniki: `ui/screens/settings/ForkFeatures.kt` (`ON_BY_DEFAULT`: `echo_source_fallback`, `echo_auto_quality`, `echo_hot_swap`, `echo_race`, `echo_fast_start` …).

## Objawy na TV (JMGO, 21.11, build161o i build162o)

1. **„Godzilla Minus Zero” (2026, tt34384661):** wszystkie czołowe źródła TorBox to klipy zastępcze 17–174 KB (30 s). Wyścig to rozpoznał („lost … placeholder clip”), mimo to włączył #0. Potem 3× `WRONG_DURATION`, `trying next` i koniec z błędem. Źródła z polskim lektorem niżej na liście nie zostały spróbowane.
2. **„Godzilla II” (tt3741700):**
   - wyścig „wygrał” klip „not cached yet” w 1,9 s; `startup_timeout` po 15 s;
   - potem ruszył 4K REMUX 57 GB z polskim lektorem — przy 7,7 MB/s bufor 1–2 s;
   - po ok. 14 s `SOURCE_SWITCH` na Comet 1080p, który znów był klipem „not cached yet” (widoczny napis na ekranie);
   - wybór trwał bardzo długo.
3. **build162o, z poprawką Placeholders:**
   - film A otworzył się **z dźwiękiem, bez obrazu**;
   - film B to seria błędów (`startup_timeout`, `trying next` …), a na końcu ekran listy źródeł;
   - wyścig dwa razy wybrał „StremThru Torz 4k”, mimo że zaraz potem był `startup_timeout`;
   - HEVC 4K na słabym dekoderze. Log: `plan/autoplay/log-jmgo-b162o.txt` (adresy i klucze usunięte).
4. Wcześniej (`plan/TERAZ.md`): auto jakość schodziła 4K→1080p bez zacięcia i gubiła napisy. Poprawiono to `EchoAutoQualityGuard` (w dół tylko po `REBUFFER`).

Wnioski wstępne (zweryfikuj):
- wyścig mierzy tylko pierwsze bajty, nie to, czy dekoder i łącze uniosą plik;
- „źródło zastępcze” rozpoznawane za późno;
- odzyskiwanie poddaje się po kilku próbach i nie idzie w dół listy do źródeł już sprawdzonych;
- zmiana źródła w trakcie wybiera niesprawdzone źródła;
- 4K/REMUX/HEVC wygrywa ranking, choć urządzenie (ok. 8 MB/s, słaby dekoder) go nie uciągnie;
- „dźwięk bez obrazu” to prawdopodobnie brak sprzętowego dekodera dla profilu HEVC/DV albo wyścig przełączeń powierzchni.

## Zadanie

1. **Analiza obecnego kodu:** pełny przepływ od „Odtwórz” do pierwszej klatki i od zacięcia do zmiany źródła. Narysuj go w tekście (kto wywołuje kogo, limity czasu, ile prób, skąd lista kandydatów). Wskaż konkretne przyczyny każdego objawu, z plikami i liniami.
2. **Porównanie z innymi forkami Nuvio, które mają tę funkcję.** Przeczytaj ich kod na GitHubie i opisz:
   - jak wybierają źródło na start (wyścig? sonda? próg rozmiaru lub bitrate?);
   - jak wykrywają „not cached / placeholder”;
   - jak ograniczają 4K/bitrate do możliwości łącza i dekodera;
   - kiedy i na co zmieniają źródło w trakcie;
   - ile prób i jakie limity czasu.

   Przynajmniej:
   - **Echo** (skąd pochodzi `AutoPlayRace`, sprawdź nowsze commity niż `e952419`);
   - **Arc**;
   - **Nuvio Reshaped**;
   - **KE**;
   - oficjalne **NuvioTV / NuvioMedia**;
   - inne forki z autoplay, które znajdziesz (szukaj `AutoPlayRace`, `autoPlay`, `streamAutoPlay`, `nextSource`, `hot swap`).

   Adresy repozytoriów i przypięte SHA są w `UPSTREAM_SOURCES.md` i `fork-tools/pins.env`. Zrób tabelę porównawczą.
3. **Nowy algorytm (projekt przed kodem).** Wymagania:
   - **Przed startem:** odrzuć klipy zastępcze (rozmiar z `Content-Range`/`Content-Length`, czas trwania, znaczniki nazw „not cached”, „⏳”, „download”). Odrzuć pliki, których łącze nie uniesie: szacowany bitrate = rozmiar / oczekiwana długość, porównany ze zmierzoną prędkością; `fork/net/KeNetwork` ma pomiar łącza. Odrzuć pliki, których dekoder nie uniesie (`core/player/DecodeCapability`: HEVC/DV/AV1, 4K na Androidzie 11/armv7).
   - **Wyścig:** tylko spośród przefiltrowanych, z sensownym limitem; zwycięzca to źródło, które da pierwszą klatkę, a nie pierwsze bajty. Rozważ krótkie, ograniczone w czasie przygotowanie kilku kandydatów albo test prędkości odczytu.
   - **Odzyskiwanie przy starcie:** jedna wspólna kolejka sprawdzonych kandydatów. Bez powtórek złych źródeł (`SmartBadSources`, `Placeholders`). Twardy limit łącznego czasu (np. 20–25 s), potem lista źródeł z krótkim komunikatem po polsku.
   - **W trakcie:** zmiana tylko po prawdziwym zacięciu (rebuffer powyżej progu), tylko na kandydata już sprawdzonego jako grający, z zachowaniem pozycji, języka dźwięku i napisów. Nigdy na „not cached”.
   - **Dźwięk bez obrazu:** wykryj (dźwięk gra, brak `onRenderedFirstFrame` przez X s) i traktuj jak błąd dekodera, a nie sukces.
   - **Polski dźwięk:** źródło z polskim lektorem lub dubbingiem, które gra, wygrywa z lepszą jakością bez polskiego (profil `pl_if_good`).
   - **Diagnostyka:** każda decyzja w jednym czytelnym logu (`ForkAutoplay`: kandydaci, odrzuceni z powodem, zwycięzca, czasy).
4. **Implementacja** według zasad z `ZASADY.md` (nowy moduł w `fork/…`, minimalne wpięcia, przełącznik domyślnie włączony tylko wtedy, gdy działa lepiej niż ręczny wybór). Testy JVM czystej logiki: filtr, kolejność, limity, w stylu istniejących w `app/src/test/java/com/nuvio/tv/fork/…`.
5. **Nie uruchamiaj** Gradle na komputerze właściciela. W PR opisz, jak to sprawdzić na TV: tytuły z objawów i oczekiwane linie logu.

## Czego oczekuję na koniec

- Raport (Markdown, w PR albo `plan/autoplay/ANALIZA.md`):
  - przepływ obecny;
  - przyczyny objawów;
  - tabela porównawcza forków;
  - nowy algorytm (pseudokod);
  - ryzyka.
- PR do `claude/paczka-oczywiste` z kodem, testami, wpisami w `fork-tools/hooks.tsv` i `plan/DECYZJE.md`, z wynikiem `check.sh` w opisie.
- Jeśli coś wymaga decyzji właściciela, wypisz to na końcu raportu jako krótką listę pytań — nie zgaduj w ciemno.
