# TERAZ — stan pracy i instrukcja „kontynuuj” (aktualizuj po każdym kroku)

Ostatnia aktualizacja: 18.11.2026, wątek „Funkcjonalności z forków niewidoczne w interfejsie”.

## Jak kontynuować
Gdy właściciel napisze „kontynuuj”:
1. `cd "/Users/milajovovich/Desktop/nuvio 2/NuvioFork" && git status --short && git branch --show-current` (oczekiwana gałąź: `hybryda-zrodel`).
2. Przeczytaj ten plik do końca, potem `plan/raporty/audyt-funkcji-ui.md`, `plan/raporty/kicia-funkcje.md`, `plan/raporty/wybor-zrodel-porownanie.md`.
3. Rób „Kolejkę” od pierwszego niezrobionego punktu. Po każdym punkcie: commit + zaktualizuj ten plik (sekcje „Stan” i „Kolejka”).
4. Pracuj po polsku, krótko, autonomicznie; pytaj tylko o rzeczy z listy „Wymaga zgody”.
5. Gdy kontekst zbliża się do limitu (ok. 95%): przerwij, zapisz tu dokładnie, w którym miejscu punktu jesteś, commit.

## Wymaga zgody właściciela
- **Kompilacja** (Gradle) i emulator: właściciel 18.11 kazał NIE kompilować do odwołania; automatyczna ochrona też to blokowała. Zapytaj, zanim zbudujesz. Nie sprawdzaj funkcji przez build, jeśli da się w kodzie.
- Push do GitHuba.

## Zasady (nadrzędne; pełne w `../CLAUDE.md`)
- Funkcja liczy się jako zrobiona, gdy da się jej użyć w aplikacji (wejście w UI, domyślnie włączona), nie gdy przechodzą testy logiki.
- KE, Echo, Reshaped, baza: kopie 1:1 z przypiętym SHA + cienkie adaptery; zmiany w ich plikach jako łatki (`fork-tools/patches/<seria>/`), `bash fork-tools/check.sh` przed commitem. Limit plików z wpięciami: 50 (właściciel: sztuczny; przy hybrydzie podnieś do 65 w `fork-tools/check.sh`).
- **Kicia** (`~/Desktop/nuvio-tv-kicia`, gałąź `feat/auto-odtwarzanie` 34c80c6) jest zamrożona: jej kod można dowolnie dopasować (nie trzeba aktualizacji 1:1).
- Nie pytaj o rzeczy oczywiste; dźwięk `afplay -v 10 /System/Library/Sounds/Sosumi.aiff`, gdy potrzebna decyzja.

## Stan (gałęzie w NuvioFork)
- `aktualizacja-1118`: nowa baza NuvioTV 2b02219d5 + forki; ostatnia kompilacja OK (STATUS c052bd4ed). Od niej wszystko dalej NIEKOMPILOWANE.
- `skin-kicia` (od aktualizacja-1118):
  - skórka Kicia `skin/kicia` (kopia `ui/forge` z Kicii 34c80c6, adapter `KiciaSkin.kt`), domyślny wygląd (SkinPreferences), przełącznik tylko w Ustawieniach (kategoria forka „Wygląd”);
  - wpięcia skin: home, odtwarzacz (PlayerChrome z args), szukaj, biblioteka, YouTube, profile, kreator 1–2;
  - etap 3: łatki Kicii w 26+2 plikach Nuvio/Reshaped (`fork-tools/patches/kicia/*.patch`, check.sh je cofa), bramkowane `skin/kicia/ForgeUi.ON` (= wybrana skórka KICIA); motyw (Color/Type/ShapeTokens) owinięty warunkiem; czcionka Satoshi (AppFont.SATOSHI);
  - start HLS od dowolnej klatki I (PlayerMediaSourceFactory, LiveTvPreview) — działa zawsze;
  - domyślnie włączone funkcje forków (`ForkFeatures.ON_BY_DEFAULT`) i Reshaped Live TV + AutoSync (`FirstRunDefaults.reshapedOn`);
  - launcher: `<queries>` LEANBACK_LAUNCHER w `app/src/full/AndroidManifest.xml`;
  - raporty: `plan/raporty/{audyt-funkcji-ui,kicia-funkcje,wybor-zrodel-porownanie}.md`.
- `kicia-zrodla-wip`: odłożone kopie logiki źródeł Kicii (nie używać wprost; materiał do punktu 4 kolejki).
- `hybryda-zrodel` (od skin-kicia, ROBOCZA): KE 0a87a08 1:1 — ranking i ocena wydań + strażnik tytułu, dekodery, limit rozdzielczości, preferowane źródło (pliki w `core/stream`, `core/player/{StreamQualityRank,DecodeCapability,BingeGroups,StreamContinuation}.kt`, `data/local/{PreferredSourceDataStore,BoundedPrefs}.kt`, `domain/model/ReleaseTags.kt`), nakładka `domain/model/KeStreamShims.kt` (AIOStreams puste: właściciel go nie używa). BRAK adaptera i wpięć.
- Świadomie NIE przeniesione z KE: `AutoPlaySelection`, `FailedReleases`, prefetch (`StreamPrefetchCache`, `PrefetchSelectionSupplier`), limit łącza (`StreamLimit`, `StreamListView`, `StreamBitrateEstimate`), test łącza (`StreamSweepEngine`, `ServiceNetworkTable`, `PerPlayNetwork`), `MemoryBudget`, `NuvioExoPlayerPerformanceHelper` — zależą od pobierania wieloma połączeniami KE (`PlayerMediaSourceFactory` chunk client, `PlaybackConfigFit`, `PlayerSettingsDataStore` KE), którego baza nie ma. Do opisania właścicielowi jako osobny etap.

## Kolejka (hybryda źródeł, decyzja właściciela 18.11: „Tak, rób hybrydę”)
1. [ ] Adapter forka `fork/sources/KeRanking.kt`: kandydaci → `ContentIdentityGuard.rejections` (odrzuć zły tytuł/rok) → `DecodeCapability` (software-only na koniec) → `resolutionLimitVerdict` (ponad limit TV na koniec) → `StreamQualityRank.rank(filmSizeFloors = film)` → `PreferredSource.preferredFirst`. Wzór: KE `core/player/AutoPlaySelection.kt` (`upstream/ke`). Jedno wpięcie w `core/player/StreamAutoPlaySelector.kt` (plik = Reshaped; łatka w serii kicia/hooked) tak, by tryb „pierwsze źródło” brał listę z rankingu; wyścig Echo (`fork/echo/EchoRaceCandidates.kt`) dostaje tę kolejność.
2. [ ] Preferowane źródło: zapis przy ręcznym wyborze (długie OK w panelu źródeł / StreamScreen), wzór KE `PreferredSource.of`.
3. [ ] Echo: brakujący `fallbackWhenPreferredSourcesEmpty` (Echo `core/player/StreamAutoPlaySelector.kt`, `upstream/echo`).
4. [ ] Kicia (z gałęzi `kicia-zrodla-wip`, dopasować swobodnie, bez kopii Echo/M4): kontrole po otwarciu pliku (`DurationCheck`, `SoundCheck`, `core/streams/smart/AudioCheck` → już jest `fork/sources/AudioCheck`), pamięć złych plików (`SmartBadSources`, `FileInherentFailure`), nauka (`SmartOutcomeStore`, `PlayerSmartLearning`), rozgrzewka (`ForgeWarmUp`), bufor (`PlayerBufferTuning` + łatka `BitrateAwareLoadControl`), P2P (`ForgeP2pFallback` + łatki debrid), „Spróbuj innego źródła” (`PlayerTryAnotherSource`), AVI/WMV przez mpv (łatka `PlayerRuntimeControllerInitialization`, `forgeNeedsMpvContainer`).
5. [ ] Domyślnie włączone (ForkFeatures.ON_BY_DEFAULT), wpisy w kategorii forka, wiersz „Test łącza i raport sprzętu” (`ui/screens/settings/DeviceTestSettingsItems.kt`) dziś MARTWY (`onClick = { }`) — podpiąć do testu prędkości z Ustawień → Zaawansowane.
6. [ ] `check.sh` (limit 65), `fork-tools/pins.env` (KE=0a87a08), `UPSTREAM_SOURCES.md`, raport `plan/raporty/wybor-zrodel-porownanie.md` (co zrobione), commit.
7. [ ] Zapytaj o zgodę na kompilację; po zgodzie: `compileFullDebugKotlin` w tle, poprawki, APK.
8. [ ] Porównanie IPTV (prośba 18.11): Kicia vs obecny fork vs plan (`plan/bloki/B01–B03, B08`). Kicia miała m.in.: szybkie wyświetlanie EPG (wycinek „teraz” przed tygodniem: `LiveTvGuideCache` NOW_FILE_NAME, commit 24e08b945), rozbudowany katalog programów z EPG grupujący w serie i wybierający działające źródło odcinka (wyścig emisji `ForgeAiringRace`), propozycja oglądania aktualnego programu z archiwum (przewijanie, `ForgeArchiveOffer`), drzewo kategorii EPG; NIE miała: pamięci postępu programów i rzędu „Dla Ciebie” (plan B02/B03). Sprawdź w kodzie, co z planu jest w nowym forku, co działa w UI, czego brakuje. Pliki danych Live TV Kicii (LiveTvEpg/Repository/Models/SetupServer/GuideCache) NIE są jeszcze przeniesione (konflikt z łatkami dostawca).
9. [ ] Poprawki z audytu UI (`plan/raporty/audyt-funkcji-ui.md` „Co trzeba dodać”): mikrofon (przechwycenie klawisza Szukaj/Asystent w `fork/arc` RemoteGlobalHook → `VoiceTrigger.fire()`, wzór Kicia `ForgeRemoteHook`), „Lektor” w menu dźwięku, „Napisy nie pasują” w menu napisów, rzędy modułów w układzie Modern, Szukaj/Odkrywaj na pełnym przewodniku (`TvStore` schedule, nie tylko `episodeLog`), YouTube/KE w menu, jedno miejsce na ustawienia forka.
10. [ ] Skórka Kicia, braki: kolejność zakładek w `MainActivity`, „Wstecz na pasek” (`ForgeTabBackToBar`) w `NuvioNavHost`, `ForgeRemoteHook` w `MainActivity.dispatchKeyEvent`.
