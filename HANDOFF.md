# HANDOFF: stan prac i problemy do rozwiązania (dla innego modelu AI)

Data: 16.11.2026. Wszystko lokalnie, nic nie wypchnięte do sieci.
Katalog projektu: `/Users/milajovovich/Desktop/nuvio 2/` (dalej `ROOT`).
Fork (git): `ROOT/NuvioFork`. Klony upstream (tylko do odczytu): `ROOT/upstream/{nuvio,ke,reshaped,arc,echo,one}`.
Łatka Reshaped względem Nuvio `main`: `ROOT/upstream/reshaped.patch` (1076 plików; 839 nowych).
Zasady i preferencje użytkownika: `ROOT/CLAUDE.md`. Pochodzenie plików: `NuvioFork/UPSTREAM_SOURCES.md`. Skrót stanu: `NuvioFork/STATUS.md`.

## Wymagania użytkownika (obowiązujące)
- Baza: NuvioTV `dev`, commit `2c853c1` (w forku `85f9010aa` "Baseline NuvioTV 2c853c1").
- Każdy fork jako osobny moduł (zwykłe katalogi, wyjątek: YouTube wymaga modułów Gradle `mediaservicecore`), cienkie adaptery, minimalna ingerencja w rdzeń (użytkownik: ok. 35 plików), każda funkcja z przełącznikiem, wszystkie ustawienia z dodatkowych forków pod jedną kategorią (obecnie sekcja Reshaped).
- Kod 1:1 z forków, bez przepisywania i optymalizacji. Oficjalny odtwarzacz zostaje.
- Zakres KE zawężony: tylko katalogi IPTV (kraje, kategorie, języki), filtry kanałów w EPG oraz testowanie łącza i sprzętu. IPTV/EPG w całości pochodzi z Reshaped. Reszta KE zaparkowana.
- Pilot Arc ma działać w całej aplikacji, także w IPTV.
- Reshaped: bez aktualizatora (usunięty), z Dolby Vision i HDR10.
- Model rozpoznawania mowy ma obsługiwać wiele języków.
- Nie zajmować się tłumaczeniami. Nie ruszać plików tłumaczeń poza koniecznymi poprawkami duplikatów.

## Środowisko i budowanie
- Mac 8 GB RAM; Gradle `-Xmx3072m` (`NuvioFork/gradle.properties`), przy 6 GB był swap. Pierwsze budowanie pakowania APK raz padło bez przyczyny w kodzie, powtórzenie przeszło.
- `export JAVA_HOME=/opt/homebrew/opt/openjdk@17`
- Kompilacja kodu: `cd NuvioFork && ./gradlew :app:compileFullDebugKotlin -PforgeArm64Only=true --console=plain`
- APK: `./gradlew :app:assembleFullDebug -PforgeArm64Only=true`; wynik: `NuvioFork/app/build/outputs/apk/full/debug/app-full-arm64-v8a-debug.apk`
- Obecne APK (stan gałęzi `ke2`, 105 MB) NIE zawiera pilota Arc ani wyboru modelu mowy.
- Nic nie było uruchamiane na telewizorze ani w emulatorze. "Kompiluje się" to jedyna weryfikacja.

## Gałęzie git (`NuvioFork`)
| Gałąź | Zawartość | Status |
|---|---|---|
| `rs-module` | baza + Reshaped jako moduł | kompiluje, APK zbudowany |
| `yt` | rs-module + YouTube | kompiluje |
| `echo` | yt + `AutoQualityPolicy`, `StreamQuality` (niepodpięte) | kompiluje |
| `ke2` | echo + katalog KE + przełączniki | kompiluje, APK zbudowany |
| `arc-remote` | ke2 + pilot Arc + wybór modelu mowy (Whisper) | zmiany NIEZACOMMITOWANE, kompilacja w toku (ostatni wynik nieznany) |
| `main` | porzucony monolit (cała łatka Reshaped na bazie) | kompiluje, nie używać |
| `ke` | pierwsza próba KE (cały Live TV) | nie kompiluje (1391 błędów), nie używać |
| `wip-all` | surowe kopie Arc/Echo/KE/One | nie kompiluje, archiwum |

## 1. WDROŻONE (kompiluje się)

### Reshaped (gałąź `rs-module`, w `ke2`)
- Nowe pliki (759), głównie: `app/src/main/java/com/nuvio/tv/reshaped/**`, `ui/reshaped/**`, `ui/screens/player/{audiosync,autosync,seekpreview,iec}/**`, `app/src/main/cpp/seekpreview/**`, `app/src/main/res/**` (nowe zasoby), `ffmpeg-decoder-downmix/**`, `app/libs/*.aar` (podmienione `lib-decoder-ffmpeg-release.aar`, `lib-exoplayer-release.aar`).
- Funkcje: IPTV (M3U/Xtream/Stalker), EPG, catch-up, szklana nawigacja, synchronizacja napisów z audio, drugi język, rozpoznawanie mowy (sherpa-onnx), lokalne miniatury podglądu (silnik z Arc: `ui/screens/player/seekpreview/engine`, `ArcPreviewAdapter.kt`), Dolby Vision, HDR10.
- Aktualizator Reshaped USUNIĘTY (pliki zaktualizatora wróciły do wersji bazy `85f9010aa`).
- Zmodyfikowane pliki rdzenia względem bazy: 71 (limit użytkownika ok. 35 przekroczony). Pełna lista poniżej w sekcji "Pliki rdzenia zmienione".

### Nuvio One: YouTube (gałąź `yt`, w `ke2`)
- `NuvioFork/mediaservicecore/**` (5 modułów Gradle: mediaserviceinterfaces, youtubeapi, sharedutils, commons-io-2.8.0, j2v8), `settings.gradle.kts` (include), `app/build.gradle.kts` (2 zależności).
- `app/src/main/java/com/nuvio/tv/youtube/**` (25 plików), `ui/screens/settings/YouTubeSettingsContent.kt`, `YouTubeTabOrderEditor.kt`, `ui/screens/player/YouTubePageScrollUi.kt`, `res/drawable-nodpi/onetube_mark_red.png`.
- Adaptery (puste lub cienkie): `ui/screens/player/seekpreview/YouTubeStoryboardAdapter.kt` (tylko sprzątanie cache), `ui/screens/settings/DebugReportDialogAdapter.kt` (zamyka dialog), `com/nuvio/tv/OneAdapter.kt` (`LocalOpenSidebar`), `livetv/record/RecordingResume.kt` (punkty wznowienia).
- Trasa `youtube` w `ui/navigation/Screen.kt` i `NuvioNavHost.kt` (komentarz "Nuvio One hook"); odtwarza przez oficjalny odtwarzacz (`youtubePlayerRoute`).

### KE: katalog IPTV (gałąź `ke2`)
- Źródło danych: repozytorium `Arshad-Kamal/Nuvio-KE-channels` (`pool/streams.tsv`, `monitor/state.tsv`, `monitor/pack.json`) plus metadane iptv-org (`https://iptv-org.github.io/api/`: kanały, kategorie, języki).
- Pliki (skopiowane 1:1 z `upstream/ke/app/src/main/java/com/nuvio/tv/livetv/`): `livetv/{model,parse,data,source}/**` (bez odtwarzacza, bez ekranów KE). Dodatkowe: `core/network/tls/AddonCertFailure.kt`, `core/network/CancellableCall.kt`, `data/local/LocalStoreJson.kt` (obcięty), `assets/livetv/broadcasters.m3u`.
- Adaptery: `livetv/source/LiveSourceManager.kt` (zastępuje KE-owy: pobieranie packu, zapis wyników skanu), `livetv/source/SourceKind.kt`, `livetv/player/PlaybackActivity.kt` (skan czeka, gdy odtwarzacz otwarty; używa `PlayerSessionTracker`), `core/di/KeLiveTvModule.kt` (kwalifikatory `@Validated`, `@Download`), `core/di/KeJsonGuard.kt`.
- Most do Reshaped: `livetv/bridge/KeCatalogViewModel.kt`: pobiera pack, buduje playlistę M3U per kraj (filtry kategoria i język), dodaje ją jako źródło przez `LiveTvRepository.importPlaylist(...)`. Skan łączy: `PackScanner.start(...)`.
- Ustawienia: `ui/screens/settings/KeCatalogSettingsItems.kt`, `ForkFeatures.kt`; podpięte w `NuvioReshapedSettingsContent.kt`.
- NIEZWERYFIKOWANE w działaniu: czy parser Reshaped poprawnie czyta wygenerowane `#EXTVLCOPT`; czy `PlayerSessionTracker.isSessionOpen` istnieje w forku (kompiluje się, więc istnieje).

### Echo (gałąź `echo`, w `ke2`): tylko czysta logika
- `core/streams/AutoQualityPolicy.kt`, `core/streams/StreamQuality.kt`. NIEPODPIĘTE do niczego, więc funkcja nie działa.

### Przełączniki w ustawieniach (jedna kategoria)
- `NuvioReshapedSettingsContent.kt` zawiera wpięcia: `keCatalogSettingsItems()`, `youtubeSettingsItems()` (+ w `arc-remote`: `remoteButtonsSettingsItems()`, `asrModelSettingsItems()`). Stan przełączników: `ui/screens/settings/ForkFeatures.kt` (SharedPreferences `fork_features`).
- Uwaga: przełączniki tylko pokazują/ukrywają wiersze ustawień. Nie wyłączają kodu modułów Reshaped ani YouTube na poziomie działania.

### W toku, gałąź `arc-remote`, NIEZACOMMITOWANE, niezweryfikowana kompilacja
- Pilot Arc: `data/local/RemoteButtons.kt` (1:1), `NightModeLevel.kt`, `RemoteButtonStore.kt` (adapter, SharedPreferences `arc_remote_buttons`), `ui/screens/player/{RemotePressTracker,RemoteButtonLabels,RemotePlayerHook}.kt`, `ui/screens/settings/{PlaybackRemoteButtons,RemoteButtonsSettingsItems}.kt`, `res/values/arc_remote_strings.xml`; hook w `PlayerScreen.kt` obok hooka Live TV z Reshaped (`arcRemote.onKey(...)`). Hook obejmuje odtwarzacz i Live TV (jeden `onPreviewKeyEvent`). NIE ma hooka globalnego w `MainActivity` (ekrany poza odtwarzaczem), więc "w całej aplikacji" jest spełnione tylko częściowo.
- Pilot: zadania bez odpowiednika w forku nic nie robią: `NEXT_CHAPTER`, `PREVIOUS_CHAPTER`, `SHOW_DETAILS`, `NIGHT_MODE`, `NEXT_AUDIO`, `JUMP_TO_PERCENT`, `REPORT_MARK`, `EXTERNAL`, `SUBTITLES_*`. `STATS` → `PlayerEvent.OnTogglePlayerStatsHud`.
- Model mowy: `ui/screens/player/audiosync/asr/AsrModelChoice.kt` (angielski zipformer + Whisper tiny/base/small, pliki z `huggingface.co/csukuangfj/sherpa-onnx-whisper-*`, sumy SHA-256 policzone lokalnie), zmiany w `AsrModel.kt` i `SherpaSpeechToText.kt` (Whisper: `OfflineWhisperModelConfig`, `language=""`, `enableTokenTimestamps=true`), ustawienie `AsrModelSettingsItems.kt`. NIEPRZETESTOWANE: czy znaczniki czasu tokenów Whisper w sherpa-onnx 1.13.8 wystarczają `WordAnchorMatcher`; czy tokeny Whisper (BPE z "Ġ"/spacją) są poprawnie sklejane w `SherpaSpeechToText.mergeTokens`; wydajność na telewizorze (Whisper base ~161 MB, small ~376 MB).

## 2. ZAPARKOWANE / PROBLEMATYCZNE (z przyczynami i ścieżkami)

### A. Echo: Auto Quality, wyścig i przełączanie źródeł
- Źródła: `ROOT/upstream/echo/app/src/main/java/com/nuvio/tv/`:
  `core/player/AutoPlayRace.kt`, `core/streams/AutoQualityPolicy.kt` (już w forku), `core/streams/StreamQuality.kt` (już), `ui/screens/player/PlayerRuntimeControllerAutoQuality.kt`, `…AutoQualityHotSwap.kt`, `…SourceSwitch.kt`, `…QualityFallback.kt`, `…AutoPlayHedge.kt`, `data/local/BetaPerformanceSettingsDataStore.kt`, `ui/screens/settings/BetaPerformanceSettingsContent.kt`. Wywołania są też w Echo-owych: `ui/screens/player/PlayerRuntimeController.kt`, `PlayerRuntimeControllerStreams.kt`, `PlayerRuntimeControllerPlaybackEvents.kt`, `PlayerScreen.kt`, `core/player/StreamAutoPlaySelector.kt`, `ui/screens/stream/StreamScreenViewModel.kt`.
- Problem: 563 błędów po skopiowaniu (log przykładowy). Pliki używają ~40 pól stanu `PlayerRuntimeController` istniejących tylko w Echo: `sourceSwitch`, `sourceSwitchJob`, `sourceSwitchWatchJob`, `sourceSwitchGuardJob`, `autoQualityActive`, `autoQualityPolicy`, `autoQualityHotSwap`, `autoQualityPendingJob`, `autoQualityLastWasDowngrade`, `autoQualityUserChoice`, `autoQualityAvailableCache`, `autoQualitySourcesRequested`, `autoQualitySeenStreamUrl`, `qualityOverlayJob`, `keepLanguageOnNextSource`, `silentSourceKeys`, `hotSwapFailedSourceKeys`, `sourceFallbackTriedUrls`, `sourceFallbackVideoKey`, `autoPlayHedgeJob`, `autoPlayHedgeUrl`, `pendingAudioLanguageSwitch`, `lastBandwidthEstimateBps`, `playerBandwidthMeter`, `currentStreamQuality`. Do tego typy: `BetaFeatures`, `NewLookNotifications`, `NotificationStyle`, `ViewedThresholdUnit`, `AudioLanguage`, `AudioTrackProbe`, `StreamPlayback`, `PlaybackIntroState`, `FileKeyframes`, a w `AutoPlayRace.kt` metoda `StreamAutoPlaySelector.raceCandidates(...)` istniejąca tylko w selektorze Echo. Reshaped ma własną, inną wersję `PlayerRuntimeController`, `StreamAutoPlaySelector` i auto-recovery źródeł (`PlayerAutoSourceRecovery.kt`, `PlayerRuntimeControllerSourceFallback.kt`), więc koliduje i duplikuje funkcję (Reshaped ma "automatic source recovery").
- Zaparkowane, bo 1:1 wymagałoby dopisania ok. 40 pól do kontrolera Reshaped i selektora, czyli ingerencji większej niż cały limit. Rozwiązanie do rozważenia: osobny obiekt stanu (holder) poza kontrolerem plus cienkie hooki w 3 miejscach (wybór źródła, zdarzenia odtwarzania, awaria odtwarzania), albo zastąpienie Echo-owej auto-przełączalności mechanizmem Reshaped z ustawieniami Echo.

### B. KE: testowanie sprzętu i łącza urządzenia
- Źródła: `ROOT/upstream/ke/app/src/main/java/com/nuvio/tv/`: `core/assessment/*.kt` (`DeviceAssessmentEngine` 1415 linii, `DeviceAssessmentApplier`, `DeviceSetupCoordinator`, `DeviceSetupFormula`, `DeviceWelcome`, `PerPlayNetwork`, `PlaybackConfigFit`, `ServiceNetworkTable`, `ServiceNetworkTestRunner`, `ShieldPreset`, `AssessmentModels`, `DeviceFingerprint`), `core/network/StreamSpeedTester.kt`, `core/network/StreamSweepEngine.kt`, `core/network/NetworkTransportReader.kt`, `ui/screens/settings/DeviceAssessmentItems.kt`, `ui/screens/welcome/DeviceWelcome.kt`, `core/player/{PlayerPlaybackNetworking,DeviceQuirks,AudioCapabilityReport,AudioOutputRouteDetector,NuvioExoPlayerPerformanceHelper,MemoryBudget,ParallelRangeDataSource,LastPlaybackDiagnostics}.kt`.
- Problem: 441 błędów. `DeviceAssessmentEngine`/`Applier` czytają i zapisują pola `PlayerSettings`, których nie ma w forku: `deniedCodecHandling`, `allowTrueHdPassthrough`, `allowAc3Passthrough`, `mediaMovieAudioAttributes`, typy `DeniedCodecHandling`, `Dv7HandlingMode`, `FrameRateMatchingMode`, `InternalPlayerEngine`; metoda `setAssessmentRevertSnapshot`; pola `vodCacheStats`, `vodCacheState`, `dvConvertEndedAtMs` w diagnostyce; klasy `PrefetchWindowStore` i `PrewarmWindowReader` (są w KE-owym `core/player/ParallelRangeDataSource.kt`, w forku `ParallelRangeDataSource` leży w pakiecie `ui.screens.player` i jest inną wersją); komponenty UI `SettingsResultLine`, `SettingsValueRow`, `SettingsCard`, `settingsNote`; zasoby `assessment_*` (ponad 100 tekstów w `ROOT/upstream/ke/app/src/main/res/values/strings.xml`); `NuvioTheme.marquee`. Pakiety kolidują: KE trzyma silnik w `com.nuvio.tv.core.player`, fork w `com.nuvio.tv.ui.screens.player`.
- Zaparkowane, bo to nie jest nakładka, tylko tuner zmieniający ustawienia odtwarzacza (część "Applier"). Rozwiązanie do rozważenia: wyciąć wyłącznie części tylko-do-odczytu (sprawdzanie sprzętu: `DeviceAssessmentEngine` bez `Applier`, test prędkości: `StreamSpeedTester`, `ServiceNetworkTestRunner`) i podać wyniki w UI bez zapisu do `PlayerSettings`.

### C. KE: przełączana powłoka, Home, Layout Settings, TV Home
- Nie analizowane. Oryginalne wymaganie: wzorzec `DaoeZ/notflix`. Pytania otwarte: gdzie w `ROOT/upstream/ke` jest przełącznik wyglądu (pliki `ui/newlook/**`? `ui/v2/**`?), jak sięga w `MainActivity.kt`, `NuvioNavHost.kt`, `HomeScreen.kt`, `SettingsScreen.kt`. Reshaped ma własny "Netflix UI" (`netflixUiSettingsItems`, `PillNavSettingsItems.kt`), co może się pokrywać. Decyzja użytkownika: zaparkować.

### D. Arc: kalendarz
- Źródła: `ROOT/upstream/arc/app/src/main/java/com/nuvio/tv/`: `core/calendar/**` (22 pliki), `ui/screens/calendar/**` (11), `ui/screens/settings/CalendarSettingsSection.kt`, `core/di/CalendarModule.kt`, `data/remote/dto/trakt/TraktCalendarDtos.kt`, `data/remote/api/{TvdbApi,TvMazeApi}.kt`, `data/wetrakr/**`, `core/player/ScreensaverController.kt`; punkty wpięcia w Arc: `ui/navigation/{Screen,NuvioNavHost}.kt`, `MainActivity.kt` (linie ok. 364–373, 1001, 1294–1346, 1492–1509, 2927–2959: `CalendarReminderHost`, `LocalCalendarReminderCenter`, pozycja menu), `ui/screens/home/{HomeComingUp,HomeUiState,HomeViewModel,HomeScreen}.kt`, `ui/screens/settings/{LayoutHomeSettings,LayoutSettingsScreen,LayoutSettingsModel,SettingsScreen}.kt`, `core/sync/ProfileSettingsSyncService.kt`, `data/repository/TraktProgressService.kt`, `data/remote/api/TraktApi.kt`.
- Problem: 2172 błędów po skopiowaniu. Przykłady: `CalendarDeviceTier.kt` → `DeviceUiPreferenceStore`, `VisualQualityMode`, `VisualQualityTier`, pakiet `ui.v2`; `CalendarFollowedSourceReader.kt` → `WeTrakrSyncRepository.currentWatchedShowSeeds/currentDroppedShowIds`, `LibraryRepository.currentItemsOrNull`, pola `traktShowId`, `listKeys`, `listedAt`, `SourceType.WETRAKR`, `getNextUpSnapshot(…)` z innymi argumentami; `CalendarRefresher.kt` → `MetaCaller.tmdb`; klienci TMDB/Trakt/TVmaze/TVDB zależą od Arc-owych DTO i API. W Arc kalendarz jest zrośnięty z jego warstwą danych (WeTrakr, biblioteka, Trakt) i z jego UI v2.
- Zaparkowane, bo to wymaga portowania ok. 5 warstw danych Arc. Rozwiązanie do rozważenia: zostawić `core/calendar` jako czysty silnik i napisać cienkie interfejsy źródeł (`CalendarFollowedSourceReader` → własna implementacja czytająca bibliotekę/Trakt forka), pominąć `CalendarDeviceTier` (stała), pominąć WeTrakr.

### E. Arc: wyszukiwarka ustawień
- Źródła: `ROOT/upstream/arc/.../ui/screens/settings/SettingsSearch.kt`, `SettingsSearchContent.kt`, `SettingsSearchIndex.kt`, `SettingsSearchJump.kt` + hook w `SettingsScreen.kt` (ok. linii 525–570).
- Problem: ok. 380 błędów; indeks jest ręczną listą wpisów odwołujących się do ustawień i zasobów Arc (`SettingsSearchIndex.kt` sam ma 337 błędów), a nawigacja "skocz do ustawienia" zakłada strukturę `SettingsCategory`/sekcje Arc.
- Zaparkowane, bo wymagałoby przepisania indeksu pod strukturę ustawień forka (nie 1:1). Rozwiązanie: wygenerować indeks z listy kategorii forka.

### F. Arc: pozostałe pozycje z listy zadania
- Miniatury i "cache przewijania" (cache podglądu przy przewijaniu): ROZWIĄZANE przez Reshaped (silnik jest już w forku). Nie kopiować `upstream/arc/.../core/player/thumbnail/**`.
- Diagnostyka: w forku jest `ui/screens/player/PlayerDebugStatsOverlay.kt` (przełącznik pilota STATS go używa). Arc-owe `PlaybackStatsOverlay.kt`, `PlaybackStatsDv.kt`, `PlaybackStatsPolling.kt`, `ui/v2/diagnostics/**` nie kopiowane.
- Home Catalog Order: już w bazie (`ui/screens/addon/CatalogOrderScreen.kt`, `CatalogOrderViewModel.kt`, trasa `catalog_order`). Nic do dodania.
- Pilot: patrz "W toku".

### G. Reshaped: odstępstwa
- Przekroczony limit plików rdzenia: 71 zmodyfikowanych względem bazy (lista niżej). Część plików wzięta w całości z Reshaped (nie tylko hunki "Nuvio RS hook"), co może nadpisywać nowsze zmiany `dev` w tych plikach: wszystkie w `app/src/main/java/com/nuvio/tv/ui/screens/player/` (ok. 40 plików), `ui/screens/stream/StreamScreenViewModel.kt`, `core/streams/*`, `ui/navigation/Screen.kt`, `ui/screens/settings/AutoSyncSettingsItems.kt`, `ui/screens/player/autosync/AutoSyncPreferences.kt`.
- Pominięto ok. 125 niewymaganych zmian Reshaped (debrid, Trakt, biblioteka, serwer addonów, workflow GitHub, README). Pełny diff: `ROOT/upstream/reshaped.patch`.
- Pominięto 71 plików, które Reshaped oznacza jako "nowe", a które istnieją w `dev` (m.in. testy). Lista: `/tmp/rs/skipped.txt` (może zniknąć po restarcie; odtworzyć z `reshaped.patch`).
- Duplikaty zasobów: usunięto zdublowane wpisy w `values*/strings.xml` i `autosync_strings.xml` (m.in. `values-bg`).
- Reshaped zawiera bibliotekę ffmpeg z `setDeniedTranscodeMimes`; bez podmiany `app/libs/lib-decoder-ffmpeg-release.aar` kompilacja pada.

## Pliki rdzenia zmienione względem bazy (rs-module, 71)
- app/build.gradle.kts
- app/src/full/AndroidManifest.xml
- app/src/main/java/com/nuvio/tv/MainActivity.kt
- app/src/main/java/com/nuvio/tv/NuvioApplication.kt
- app/src/main/java/com/nuvio/tv/core/player/BitrateAwareLoadControl.kt
- app/src/main/java/com/nuvio/tv/core/player/DolbyVisionExtractorsFactory.kt
- app/src/main/java/com/nuvio/tv/core/player/DolbyVisionMatroskaTransformer.kt
- app/src/main/java/com/nuvio/tv/core/player/DoviBridge.kt
- app/src/main/java/com/nuvio/tv/core/player/HevcDvRpuStripper.kt
- app/src/main/java/com/nuvio/tv/core/player/HevcHdr10PlusStripper.kt
- app/src/main/java/com/nuvio/tv/core/player/StreamAutoPlayPolicy.kt
- app/src/main/java/com/nuvio/tv/core/player/StreamAutoPlaySelector.kt
- app/src/main/java/com/nuvio/tv/core/player/dvmkv/MatroskaExtractor.java
- app/src/main/java/com/nuvio/tv/data/local/PlayerSettingsDataStore.kt
- app/src/main/java/com/nuvio/tv/ui/navigation/NuvioNavHost.kt
- app/src/main/java/com/nuvio/tv/ui/navigation/Screen.kt
- app/src/main/java/com/nuvio/tv/ui/screens/home/HomeScreen.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/AudioOutputRouteDetector.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/AudioSelectionOverlay.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/AutoPlaySourceFallback.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/DolbyVisionBaseLayerPolicy.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/GainAudioProcessor.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/NuvioBaseMpvView.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/NuvioExoPlayerPerformanceHelper.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/NuvioMpvSurfaceView.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/ParallelRangeDataSource.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlaybackSpeedAwareAudioRenderer.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlaybackSpeedAwareAudioSink.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerDebugStatsOverlay.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerMediaSourceFactory.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerNavigationArgs.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerPlaybackNetworking.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeController.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerAfrPreflight.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerAudioDelayRoutes.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerAutomaticSubtitleSync.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerEngineFailover.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerErrorRecovery.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerExpiredLinkRefresh.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerInitialization.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerLifecycle.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerMpv.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerMpvEvents.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerObservers.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerPlaybackEvents.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerSourceFallback.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerStartup.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerStreams.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerTorrent.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerTracks.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerScreen.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerViewModel.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/StyledSubtitle.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/AutoSyncPreferences.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/AutoSyncRunPolicy.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/AutoSyncSampledReferenceAligner.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/AutoSyncSidecarBridge.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/AutoSyncTimelineRetime.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/AutomaticSubtitleSync.kt
- app/src/main/java/com/nuvio/tv/ui/screens/player/autosync/EmbeddedSubtitleTimelineLoader.kt
- app/src/main/java/com/nuvio/tv/ui/screens/settings/AutoSyncSettingsItems.kt
- app/src/main/java/com/nuvio/tv/ui/screens/settings/LicensesAttributionsScreen.kt
- app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackAutoPlaySettings.kt
- app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackBufferNetworkSettings.kt
- app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackSettingsModel.kt
- app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackSettingsViewModel.kt
- app/src/main/java/com/nuvio/tv/ui/screens/settings/PlaybackSubtitleSettings.kt
- app/src/main/java/com/nuvio/tv/ui/screens/settings/SettingsCatalog.kt
- app/src/main/java/com/nuvio/tv/ui/screens/settings/SettingsScreen.kt
- app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreen.kt
- app/src/main/java/com/nuvio/tv/ui/screens/stream/StreamScreenViewModel.kt

## Co sprawdzić jako pierwsze (propozycja kolejności)
1. Zacommitować i skompilować `arc-remote`; dodać hook pilota w `MainActivity.dispatchKeyEvent` (ekrany poza odtwarzaczem).
2. Uruchomić APK na emulatorze Android TV / telewizorze: Live TV (Reshaped), katalog KE (import kraju → źródło M3U), YouTube, napisy z audio z modelem Whisper.
3. Echo: zaprojektować holder stanu zamiast ~40 pól w kontrolerze (problem A).
4. KE: wydzielić tylko-do-odczytu testy łącza i sprzętu (problem B).
5. Arc: kalendarz (problem D) z własnymi źródłami danych; wyszukiwarka ustawień (problem E).
6. Ograniczyć liczbę zmodyfikowanych plików rdzenia (problem G), zastępując całe pliki Reshaped punktami wpięcia, jeśli to możliwe.
