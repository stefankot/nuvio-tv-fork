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
