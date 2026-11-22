# BLOKADY

Zadania, których nie dało się dokończyć. Przeglądaj na początku każdego bloku.

Format wpisu:

```
## YouTube w „Oglądaj dalej” nie odtwarza (21.11, JMGO build157)
OK na pozycji `youtube:<id>` w „Oglądaj dalej” → baza: „odtwarzanie niedostępne” (żaden dodatek nie obsługuje typu youtube); moduł YouTube nie ma odtwarzania po samym identyfikatorze (`YouTubeViewModel.activate` wymaga `YtItem` z obiektem silnika). Do zrobienia w module YouTube (np. `playById`), potem Niunia/Kicia mogą go wołać.

## Angielska nakładka synchronizacji napisów (21.11, JMGO build159)
W odtwarzaczu filmu duża nakładka „Syncing subtitles to audio… / Listened 39s ahead / Speech recognition: not used…” po angielsku na środku ekranu (Reshaped `ui/screens/player/audiosync/SubtitleSyncStatusOverlay.kt`, teksty `audio_sync_strings.xml`). Moduł Reshaped, nie skórka; tłumaczenia zakazane bez zgody (ZASADY §1) — do decyzji właściciela (ukryć poza trybem diagnostycznym?).

## Napisy YouTube podwójne (21.11, test JMGO build155)
Tłumaczone napisy YouTube (Nuvio One + `YouTubeMpdServer`/`YouTubeCaptionTranslation`) pokazują każdą linię dwa razy (tekst wiersza „X\nX”). To moduł YouTube, nie skórka Niunia — zostawione dla wątku YouTube.

## B03-4 (data)
Co próbowałem: ...
Czego brakuje: ...
Co może dostarczyć właściciel: ...
```

## B03-3/6/7/9 (część UI)
Co próbowałem: logika i testy JVM gotowe (ChannelFilters, MiniGuideLogic, DelayedPlayback, ForYouRow, ProgramPage, CellMark, PendingPlay). Rysowanie panelu grup, MiniGuide w LiveTvPlayerOverlay, automat „z opóźnieniem” w odtwarzaczu i wiersze modułów w HomeScreen wymagają zmian w dużych plikach Reshaped/rdzenia bez możliwości sprawdzenia wyglądu.
Czego brakuje: sprawdzenia na telewizorze/emulatorze (po zakończeniu kompilacji), decyzji o wyglądzie (skórka).
Co może dostarczyć właściciel: opinię po zobaczeniu rzędu „Dla Ciebie” i strony programu.


## B04-4 / B04-6 / B04-8 (część w odtwarzaczu i szczegółach)
Co próbowałem: logika i testy (AudioCheck, SourceLanguage.readyLine, ReadyPick, SubtitleQueue). Podpięcie sprawdzenia ścieżek do mechanizmu odzyskiwania źródeł, linia „Gotowe” w ekranie szczegółów i pozycja „Napisy nie pasują” w menu napisów wymagają zmian w dużych plikach odtwarzacza/szczegółów bez wglądu w wygląd.
Czego brakuje: weryfikacji na emulatorze i decyzji o wyglądzie linii.
Co może dostarczyć właściciel: opinię po teście na telewizorze.

## B06b-5 / B06b-2 (paczka wektorów i model)
Co próbowałem: skrypt `scripts/fork/build_vectors.py` z testem na atrapie API (zielony), workflow `fork-vectors.yml` (poprawny YAML), format paczki i matematyka z testami JVM.
Czego brakuje: sekretu `TMDB_API_KEY` w repozytorium (bez niego paczka nie powstaje; aplikacja działa na regułach) oraz uruchomienia modelu na urządzeniu (EmbeddingGemma/LiteRT) — wymaga pobrania modelu i sprawdzenia licencji Gemma (`plan/raporty/licencja-modelu.md`).
Co może dostarczyć właściciel: sekret TMDB w ustawieniach repozytorium; decyzję o redystrybucji modelu.

- B11-3 DubLift: potrzebne od właściciela 5 filmów w dwóch wydaniach (polski lektor/dubbing + oryginał angielski; w tym 2 z różnicą 25/23,976 fps, 1 z innym montażem, 1 z 5.1) do próby dopasowania. Raport: plan/raporty/dublift.md

## B10 (nastroje) — brak klucza TMDB
Co próbowałem: PicksModule (ekran „Na co masz ochotę?”, 5 nastrojów, discover z TMDB, punktacja regułowa) działa na emulatorze do momentu zapytania; bez klucza wynik „Brak klucza TMDB”.
Czego brakuje: `TMDB_API_KEY` w `local.properties` (w tej kopii pusty). Bez niego kalendarz Echo i nastroje nie pobiorą danych z TMDB.
Co może dostarczyć właściciel: darmowy klucz TMDB (v3) w `NuvioFork/local.properties` jako `TMDB_API_KEY=...`.

## Testy Trakt (3) — brak kluczy Trakt
Co ustaliłem: TraktAuthServiceTest (2) i TraktProgressServiceFailureTest (czasem) padają, bo `TraktAuthService.hasRequiredCredentials()` wymaga `TRAKT_CLIENT_ID` i `TRAKT_CLIENT_SECRET` z `local.properties`, a tu są puste; kod i testy są identyczne w bazie, w Reshaped i w forku, więc to nie błąd forka ani repo, tylko brak sekretów w środowisku.
Co może dostarczyć właściciel: klucze aplikacji Trakt (client id/secret) w `NuvioFork/local.properties`, albo zgoda, by testy uznać za zależne od środowiska.

## JMGO — brak połączenia ADB
Co próbowałem: `adb devices` (puste), `adb connect` do 192.168.31.29 i .44 (No route to host).
Czego brakuje: adres IP projektora JMGO z włączonym debugowaniem sieciowym (port 5555) albo kabel USB.
Co może dostarczyć właściciel: IP JMGO (Ustawienia → Sieć) i włączenie debugowania USB/sieciowego.

- 2026-11-17 JMGO H931 (192.168.31.252): instalacja forka niemożliwa: `INSTALL_FAILED_DUPLICATE_PERMISSION` (`com.nuvio.reshaped.tv.permission.MIGRATE` należy do zainstalowanej aplikacji `com.nuvioreshaped.tv`). Poprzednia instalacja forka (podpis inny niż mój build, z 16.11) została odinstalowana, bo blokowała aktualizację. Żeby zmierzyć JMGO: właściciel odinstalowuje `com.nuvioreshaped.tv` na JMGO albo zgadza się na wariant builda z inną nazwą uprawnienia. Pomiary idą na Xiaomi.
- 20.11 Niunia panel lektora w odtwarzaczu (makieta Player): „Głos lektora” i „Ekspresyjność” — brak API (głos tylko w Ustawienia → Moduły → Lektor przez `LektorVoiceChoice.actions()`, poziom emocji nie istnieje w `LektorScript`). Do zrobienia razem z wątkiem lektora.
- 20.11 Niunia odtwarzacz — Wstecz krok po kroku: ROZWIĄZANE (właściciel: „add the Back hook in PlayerScreen”) — wpięcie `// Fork hook: skin` w `PlayerScreen.handleBackPress` → `skin/niunia/player/NiuniaPlayerBack`, łatka `fork-tools/patches/niunia/04-player-back-skin-hook.patch`.
- 20.11 Niunia wyszukiwarka (makieta SearchResults): zakładka TV — karta „Szukaj „…” w telewizji” otwiera Live TV → Szukaj z tą frazą (przewodnik jest danymi ekranu Live TV). Zakładki YouTube i Osoby nie mają jeszcze własnych źródeł (wyszukiwanie YouTube, osoby TMDB) — pokazują tylko wyniki katalogów danego typu albo „Brak wyników w tej kategorii”. Ustawienia NOOB: wiersze „Język” i „Napisy AI” z makiety pominięte (brak ekranów w aplikacji).

- 20.11 (Niunia, test JMGO build152): okno „Brak polskich napisów — Włączyć napisy AI?” (tłumaczenie Gemini, płatne) wyskakuje przy KAŻDYM kanale na żywo i archiwum IPTV, fokus domyślnie na „Napisy AI” (jedno OK = płatne tłumaczenie). To moduł AI napisów (inny wątek, `AiAuto.kt`), nie skórka — do decyzji: nie pytać na Live TV albo fokus domyślnie na „Nie”.
- 20.11 (Niunia): Settings NOOB nie do sprawdzenia na JMGO bez zmiany poziomu ustawień w profilu właściciela → test na emulatorze.
- 22.11 Z-13 „Nagrody i nominacje” (Niunia) — **źródło nagród do wyboru przez właściciela.** Aplikacja nie ma źródła z galą, kategorią, rokiem i wynikiem: TMDB nie podaje nagród, `Meta.awards` z dodatków (Cinemeta/OMDb) to jedno zdanie po angielsku („Won 1 Oscar. 12 wins & 45 nominations total”) — bez kategorii, więc listy z niego nie da się zrobić. Zrobione: model + interfejs `NiuniaAwardsSource` z pustą implementacją `NoAwardsSource` (`skin/niunia/awards/`): pasek w hero się nie rysuje, lista na dole strony pokazuje „Brak nagród i nominacji”, plakietek na plakatach brak. Możliwe źródła: Wikidata (P166 „otrzymane nagrody”, P1411 „nominacje”; darmowe, bez klucza, niepełne dla seriali), Wikipedia (strony „List of accolades…”, trudne do odczytu), zdanie OMDb tylko dla paska (Oscar + liczba reszty). Po wyborze: jedna klasa źródła + `NiuniaAwardsRepo.source = …`, przełącznik w ustawieniach forka.
