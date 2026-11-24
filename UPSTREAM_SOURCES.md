One extra: DebugReportDialog, YouTubeStoryboardFrameSource
One extra: YouTubePageScrollUi, RecordingResume, onetube_mark_red.png; adapters OneAdapter.kt, YouTubeStoryboardAdapter.kt, DebugReportDialogAdapter.kt
Echo: tylko AutoQualityPolicy/StreamQuality (czysta logika), AutoPlayRace/HotSwap/SourceSwitch/QualityFallback wymagają selektora i stanu odtwarzacza Echo - ZAPARKOWANE
- SettingsSearchOverlay: usunięto argument imeAction (fork nie ma go w NuvioTextField); SettingsSearch: usunięto CloudStream, YOUTUBE, SIDE_MENU, ON_DEMAND, SPORTS, CALENDAR, PlaybackSection.CAPABILITIES; LIVE_TV→NUVIO_RESHAPED.

## Echo A1 (kopie 1:1 z UP/echo @ e952419)
Skopiowane: PlayerRuntimeControllerSourceSwitch/QualityFallback/AudioMenu.kt, QualitySelectionOverlay.kt, AudioLanguageMenuContent.kt, core/player/AutoPlayRace.kt, AudioTrackProbe.kt, core/streams/StreamAudio.kt, core/intro/PlaybackIntroState.kt.
Dozwolone zmiany: QualitySelectionOverlay.kt: usunięta funkcja rememberBetaPerformanceSettings + importy EntryPoint (shim w fork/echo/shims/BetaSettings.kt).
Shimy Echo: BetaPerformanceSettings/BetaFeatures (ForkFeatures, domyślnie wyłączone).

## Skórka Kicia (18.11.2026)
- Źródło: lokalny stary fork `~/Desktop/nuvio-tv-kicia`, gałąź `feat/auto-odtwarzanie` (najnowsza, 15.11), commit `34c80c6395e5d5666f6c3b2f507b6626c3ed86ea` (zawiera `fix/zgloszenia-jmgo` 429d322 + 168 commitów, makieta 7.6–7.7).
- `app/src/main/java/com/nuvio/tv/ui/forge/**` → `skin/kicia/**` (1:1, zmiana pakietu `com.nuvio.tv.ui.forge` → `com.nuvio.tv.skin.kicia`; odwołania `com.nuvio.tv.forge.tv.ForgeTv` → `com.nuvio.tv.fork.tv.TvStore`, `ForgeEpgExtras` → `EpgExtras`, `forge.tv.*` → `fork.tv.*`, `forge.ready.*` → `fork.sources.*`).
- `ui/screens/settings/ForgeSettingsContent.kt`, `ForgeNightSettings.kt` → `skin/kicia/settings/`.
- `res/values{,-pl}/forge_tv_strings.xml` → `skin_kicia_strings.xml`; `res/font/satoshi_{bold,medium}.ttf`.
- Adapter: `skin/kicia/KiciaSkin.kt` (jedyny plik pisany od nowa).

## Hybryda źródeł (18.11.2026)
- KE `upstream/ke` @ `0a87a08` (1:1): `core/stream/*` (ReleaseFactsParser, TrashReleaseGroups, ContentIdentityGuard, ResolutionLimit, PreferredSource, TextSizeParser…), `core/player/{StreamQualityRank,DecodeCapability,BingeGroups,StreamContinuation}.kt`, `data/local/{PreferredSourceDataStore,BoundedPrefs}.kt`, `domain/model/ReleaseTags.kt`. Nakładka `domain/model/KeStreamShims.kt` (pola AIOStreams puste).
- Echo `upstream/echo` @ `e952419`: `fallbackWhenPreferredSourcesEmpty` (rozszerzenie w `fork/echo/EchoRaceCandidates.kt`).
- Kicia `34c80c6` (zamrożona, dopasowywana swobodnie): `fork/sources/kicia/{DurationCheck,SoundCheck,SmartBadSources,FileInherentFailure}.kt` (z `core/player/recovery`, zmieniony pakiet; SmartBadSources bez SmartOutcomeStore), `core/streams/smart/ReleaseInfo.kt` (wyświetlanie plakietek), `forgeNeedsMpvContainer` → `fork/sources/MpvContainers.kt`, `PlayerTryAnotherSource` → `fork/sources/TryAnotherSource.kt` (uproszczone, bez sesji Echo Kicii).
- Adaptery forka: `fork/sources/KeRanking.kt`, `StartupChecks.kt`, `TryAnotherSource.kt`, `MpvContainers.kt`. Wpięcia: `fork-tools/hooks.tsv` (seria sources).

## Dublagem audio (21.11.2026)

- [NuvioTV-Dublagem](https://github.com/SaladinoGW/NuvioTV-Dublagem/tree/c683a2b6a84bf4af0b4b9b08d9faf1d7c0d33447) przypięty do `c683a2b6a84bf4af0b4b9b08d9faf1d7c0d33447`. Pliki źródłowe zachowują ścieżki i pakiety z Dublagem. Commit importu `dcaf5936d` przechowuje ich wersje 1:1, z dwoma zestawami importowanych testów i biblioteką FFT z forka.
- Lokalne rozszerzenia analizatora i modelu żądania są zapisane w `fork-tools/patches/dublagem/01-background-analysis.patch`. Ograniczają zasięg i koszt analizy w tle, zachowując domyślne zachowanie Dublagem dla dotychczasowych wywołań.
- Automatyczny dobór i cykl życia sesji są w `fork/dublagem/`. Integracja z bazowym odtwarzaczem jest w `fork-tools/hooks.tsv`. Aktualizacja forka Dublagem wymaga aktualizacji importu na nowych plikach, nałożenia łatki analizatora i ponownego zbudowania adaptera.

## Silnik przewodnika NuvioPlusIPTV (23.11.2026, właściciel: „Skopiuj zatem optymalizacje, ale nie usuń funkcji związanych z catch-up”)
- Źródło: [homelessbrian/NuvioPlusIPTV](https://github.com/homelessbrian/NuvioPlusIPTV/tree/e5643387f7029374f7deffe1618a544b3a4396a3) @ `e5643387f7029374f7deffe1618a544b3a4396a3` (GPL-3; ten sam kod Live TV co zoga2o3-ops/Nuvio-One).
- `app/src/main/java/com/nuvio/tv/livetv/data/{EpgDatabase,XmltvParser,ProgrammeIdentityPool,SqliteCompact}.kt` → `app/src/main/java/com/nuvio/tv/fork/tv/plus/` (1:1).
  Zmiany: pakiet `com.nuvio.tv.livetv.data` → `com.nuvio.tv.fork.tv.plus`, importy modeli, `LiveTvLoadReport.add` → `PlusShims.report` (log `ForkTvPlus`), `LiveTvSearchBridge.smallWords` → `PlusShims.smallWords` (kopia 1:1 z `livetv/ui/LiveTvSearch.kt`).
- `livetv/model/LiveTvModels.kt` → `fork/tv/plus/PlusModels.kt`: tylko `EpgProgram`, `SUB_TITLE_MARK`, `subTitle`, `descriptionText`, `EpgAssignment` (1:1).
- Adaptery forka (pisane od nowa) w `fork/tv/plus/`; projekt: `plan/iptv/ARCHITEKTURA.md`, porównanie forków: `plan/iptv/FORKI.md`.

## Czcionki napisów Atkinson i Tiresias (24.11.2026, właściciel: „Zainstaluj te fonty jako fonty napisów do wyboru… Ustaw Atkinson jako domyślny”)
- Atkinson Hyperlegible Next (Braille Institute, OFL 1.1): `upstream/arc` @ `343b27591` `app/src/main/res/font/atkinson_hyperlegible_next_variable.ttf` → `app/src/main/assets/fork/subtitle-fonts/atkinson_hyperlegible_next.ttf` (1:1, md5 `4cb8eaa42c20e9c16b242d2fd5be4d7d`); licencja `assets/licenses/atkinson_hyperlegible_next_OFL.txt` (1:1 z Arc).
- Tiresias LPfont (RNIB, GPL-3+ z wyjątkiem dla czcionek): pakiet Debiana `fonts-tiresias_0.1-6_all.deb` (deb.debian.org, sha256 `d810a936…b3fc38`) `usr/share/fonts/truetype/tiresias/tiresias_lpfont.ttf` → `assets/fork/subtitle-fonts/tiresias_lpfont.ttf` (1:1, md5 `d4be7993d1e17cd760d9e42f839bcc21`); licencja `assets/licenses/tiresias_COPYRIGHT.txt`. Inne wolne odmiany (Infofont, PCfont, Signfont) nie mają polskich liter; Screenfont nie jest wolna.
- Kod forka (pisany od nowa): `fork/subfonts/` (wgranie przez `core/player/SubtitleFontStore.import` z Nuvio).
