One extra: DebugReportDialog, YouTubeStoryboardFrameSource
One extra: YouTubePageScrollUi, RecordingResume, onetube_mark_red.png; adapters OneAdapter.kt, YouTubeStoryboardAdapter.kt, DebugReportDialogAdapter.kt
Echo: tylko AutoQualityPolicy/StreamQuality (czysta logika), AutoPlayRace/HotSwap/SourceSwitch/QualityFallback wymagają selektora i stanu odtwarzacza Echo - ZAPARKOWANE
- SettingsSearchOverlay: usunięto argument imeAction (fork nie ma go w NuvioTextField); SettingsSearch: usunięto CloudStream, YOUTUBE, SIDE_MENU, ON_DEMAND, SPORTS, CALENDAR, PlaybackSection.CAPABILITIES; LIVE_TV→NUVIO_RESHAPED.

## Echo A1 (kopie 1:1 z UP/echo @ e952419)
Skopiowane: PlayerRuntimeControllerSourceSwitch/QualityFallback/AudioMenu.kt, QualitySelectionOverlay.kt, AudioLanguageMenuContent.kt, core/player/AutoPlayRace.kt, AudioTrackProbe.kt, core/streams/StreamAudio.kt, core/intro/PlaybackIntroState.kt.
Dozwolone zmiany: QualitySelectionOverlay.kt: usunięta funkcja rememberBetaPerformanceSettings + importy EntryPoint (shim w fork/echo/shims/BetaSettings.kt).
Shimy Echo: BetaPerformanceSettings/BetaFeatures (ForkFeatures, domyślnie wyłączone).
