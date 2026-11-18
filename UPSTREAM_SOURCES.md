One extra: DebugReportDialog, YouTubeStoryboardFrameSource
One extra: YouTubePageScrollUi, RecordingResume, onetube_mark_red.png; adapters OneAdapter.kt, YouTubeStoryboardAdapter.kt, DebugReportDialogAdapter.kt
Echo: tylko AutoQualityPolicy/StreamQuality (czysta logika), AutoPlayRace/HotSwap/SourceSwitch/QualityFallback wymagają selektora i stanu odtwarzacza Echo - ZAPARKOWANE
- SettingsSearchOverlay: usunięto argument imeAction (fork nie ma go w NuvioTextField); SettingsSearch: usunięto CloudStream, YOUTUBE, SIDE_MENU, ON_DEMAND, SPORTS, CALENDAR, PlaybackSection.CAPABILITIES; LIVE_TV→NUVIO_RESHAPED.

## Echo A1 (kopie 1:1 z UP/echo @ e952419)
Skopiowane: PlayerRuntimeControllerSourceSwitch/QualityFallback/AudioMenu.kt, QualitySelectionOverlay.kt, AudioLanguageMenuContent.kt, core/player/AutoPlayRace.kt, AudioTrackProbe.kt, core/streams/StreamAudio.kt, core/intro/PlaybackIntroState.kt.
Dozwolone zmiany: QualitySelectionOverlay.kt: usunięta funkcja rememberBetaPerformanceSettings + importy EntryPoint (shim w fork/echo/shims/BetaSettings.kt).
Shimy Echo: BetaPerformanceSettings/BetaFeatures (ForkFeatures, domyślnie wyłączone).

## Skórka Kicia (18.11.2026)
- Źródło: lokalny stary fork `nuvio-tv-kicia-poprawki`, gałąź `fix/zgloszenia-jmgo`, commit `429d32225b325bd9f6f1bf7b4e640a27654d0bb8`.
- `app/src/main/java/com/nuvio/tv/ui/forge/**` → `skin/kicia/**` (1:1, zmiana pakietu `com.nuvio.tv.ui.forge` → `com.nuvio.tv.skin.kicia`; odwołania `com.nuvio.tv.forge.tv.ForgeTv` → `com.nuvio.tv.fork.tv.TvStore`, `ForgeEpgExtras` → `EpgExtras`, `forge.tv.*` → `fork.tv.*`, `forge.ready.*` → `fork.sources.*`).
- `ui/screens/settings/ForgeSettingsContent.kt`, `ForgeNightSettings.kt` → `skin/kicia/settings/`.
- `res/values{,-pl}/forge_tv_strings.xml` → `skin_kicia_strings.xml`; `res/font/satoshi_{bold,medium}.ttf`.
- Adapter: `skin/kicia/KiciaSkin.kt` (jedyny plik pisany od nowa).
