# ARCHITECT_PLAN: finishing the NuvioTV fork (revision 2)

Written 2026-11-16 for an autonomous, weaker executor model. The owner is a UI designer, not a programmer.
`ROOT` = `/Users/milajovovich/Desktop/nuvio 2`. `FORK` = `ROOT/NuvioFork`. `SRC` = `FORK/app/src/main/java/com/nuvio/tv`.
`UP` = `ROOT/upstream` (read-only clones: `nuvio`, `ke`, `reshaped`, `arc`, `echo`, `one`).
`MOCK` = `/Users/milajovovich/Desktop/makieta 2/design-system` (the owner's design system; read-only for the executor).

**VERIFIED** = checked in the files on 2026-11-16. **UNVERIFIED** = an assumption the executor must check first.

Owner decisions that this revision implements (2026-11-16):
- D1. Core-file limit counts only hand-maintained files; Reshaped-identical files are tracked separately. **Accepted.**
- D2. Calendar comes from Echo. **Accepted.**
- D3. Echo Auto Quality, source race, hot swap and in-player source/quality choice are **mandatory**. They may not be dropped or simplified.
- D4. Reshaped's Netflix UI is **not** enough. A switchable appearance must exist as a **separate module** that will hold the owner's own design (`MOCK`). Design it now; implementation may come later.
- D5. No TMDB key yet; nothing has been tested on an emulator or device.
- D6. The owner will supply a streaming addon for testing A when emulator or device testing starts. Until then A is reported as "compiles, behaviour unverified".
- D7. Echo's **audio-language menu** and **playback intro** are also wanted (phases A5 and A6). They are no longer stubbed.
- D8. The mockup's token generator may be changed to the skin package (section 2.7.2).
- D9. "My design" must eventually cover **everything, including the player** (section 2.7.4, hook 4).

---

## 1. Summary

### 1.1 Facts the plan is built on

1. VERIFIED: of the 72 modified core source files, **65 are byte-identical to Reshaped's version** (`UP/reshaped` @ `2dd10cd5e`). Only 7 carry our own edits: `app/build.gradle.kts`, `settings.gradle.kts`, `app/src/full/AndroidManifest.xml`, `ui/navigation/Screen.kt`, `ui/navigation/NuvioNavHost.kt`, `ui/screens/home/HomeScreen.kt`, `ui/screens/player/PlayerScreen.kt`.
2. VERIFIED: Echo's calendar is 5 files / 2,265 lines on repositories the fork has; Nuvio One's settings search is 2 files / 453 lines with an automatically built index.
3. VERIFIED: the base already has a link test (`ui/screens/settings/NetworkSettingsScreen.kt`) and a hardware report (`ui/screens/settings/DiagnosticsCard.kt`).
4. VERIFIED: Echo's Auto Quality code is 6 files (2,539 lines) of extension functions on `PlayerRuntimeController`, called from **about 40 places in 10 player files** that in the fork are Reshaped-owned, plus 5 places in `StreamScreenViewModel.kt` for the race. This is the real cost of D3.
5. VERIFIED: the app already switches appearance in exactly two places: `MainActivity.kt` line ~1225 (`PillNavScaffold` replaces the sidebar) and `HomeScreen.kt` line ~377 (`NetflixHomeRoute` replaces Home). The owner's skin module reuses this proven pattern.

### 1.2 Decisions per feature

| Item | Decision | Source | Effort | Risk |
|---|---|---|---|---|
| Arc remote in whole app | finish | `arc-remote` branch | S | low |
| B. Link and hardware test | reuse base, add settings rows | base | S | low |
| E. Settings search | port | Nuvio One | M | medium |
| D. Calendar | port | Echo | L | medium-high |
| **A. Auto Quality, race, hot swap, quality menu, audio-language menu, playback intro** | **port in full, in 6 phases (incl. audio-language menu and playback intro)** | Echo | **XL** | **high** |
| **C. Switchable appearance** | **build the skin module frame now; owner's screens later** | owner's `MOCK` | M now, XL later | medium |
| G. Core-file count | reclassify; contract test | n/a | S | low |
| KE device-assessment tuner, KE shell code, Arc calendar, Arc settings search | not ported (replaced by the rows above) | n/a | n/a | n/a |

### 1.3 Honest warnings for the owner

- **A is the riskiest part of the project.** It can only be proven by playing a title that has several sources of different quality. Until the app is tested on an emulator or TV with a working addon, "compiles" is all that can be claimed for A.
- A forces hook lines into about 11 Reshaped-identical files. They stop being "copy to update" files and become "copy, then re-insert marked lines" files. The contract test (section 4.2) keeps this mechanical, but every future Reshaped update costs more.
- Everything is switchable. Each phase of A has its own switch, default **off** until tested, so a broken phase cannot break normal playback.

---

## 2. Solutions

### 2.0 Rules for the executor

- R1. Work on branch `final` (step 3). One step or phase = one commit. Update `STATUS.md` after each.
- R2. Files in `fork-tools/overrides.txt` are Reshaped-owned. They may be edited **only** by inserting marked hook lines named in this plan. Each insertion: one statement, ending with `// Fork hook: <module>`, calling into `SRC/fork/**` or an Echo extension function; add a row to `fork-tools/hooks.tsv`; move the file from `overrides.txt` to `hooked-overrides.txt`. Never restructure, reformat or delete Reshaped code.
- R3. Copied files stay 1:1 in their original package path. Fix compile errors only by the "symbol closure procedure" (2.6.3) or by edits this plan explicitly allows. Record every allowed edit in `UPSTREAM_SOURCES.md`.
- R4. **Stop rule for optional items (B, D, E):** maximum 4 compile cycles; if more than 40 errors remain, `git reset --hard` to the last good commit, write "PARKED: reason, error count" in `STATUS.md`, continue with the next step.
- R5. **No-drop rule for A:** never park A. If a phase is stuck after 6 compile cycles, commit the work on a side branch `a-phase-N-wip`, write the remaining error list to `FORK/A_BLOCKERS.md` (file, line, symbol, what was tried), play the owner's alert sound once, and continue with the next non-A step. Return to A afterwards.
- R6. Never run Gradle and the emulator together (8 GB RAM): `./gradlew --stop` before the emulator, `adb emu kill` after.
- R7. New strings go to `res/values/<module>_strings.xml`; delete a name from the module file if it already exists in `strings.xml`. No translations.
- R8. `df -h ~` before each build (need about 15 GB; VERIFIED exactly 15 GB free on 2026-11-16).
- R9. A feature that is switched off must cost nothing: every hook goes through a function that returns immediately when its `ForkFeatures` key is off.

### 2.1 Shared infrastructure: `ForkHooks` and `ForkFeatures`

- New `SRC/fork/ForkHooks.kt`, `package com.nuvio.tv.fork`, plain `object ForkHooks`. No Hilt, no `ServiceLoader`.
- `ForkFeatures.kt` (fork-owned): add `enabled(c, key, default)`. VERIFIED today the default is always `false` and switches only hide settings rows. Defaults: existing working features **true**; calendar, search, device test **true**; every Echo key **false**; `skin` = `"nuvio"`.
- Keys: `reshaped_livetv`, `youtube`, `ke_catalog`, `arc_remote`, `settings_search`, `calendar`, `echo_quality_menu`, `echo_auto_quality`, `echo_hot_swap`, `echo_race`, `echo_audio_menu`, `echo_playback_intro`, and string key `skin`.

| `ForkHooks` function | Called from | When off |
|---|---|---|
| `onGlobalKey(activity, event): Boolean` | `MainActivity.dispatchKeyEvent` | `false` |
| `isRouteEnabled(context, route): Boolean` | menu building in `MainActivity`, route blocks in `NuvioNavHost` | `false` |
| `skin(): SkinId` (Compose state) | section 2.7 | `SkinId.NUVIO` |

### 2.2 Arc remote in the whole app

- Hook: `SRC/MainActivity.kt`, `dispatchKeyEvent` (line ~1418), first statement: `if (com.nuvio.tv.fork.ForkHooks.onGlobalKey(this, event)) return true // Fork hook: arc-remote`.
- New `SRC/fork/arc/RemoteGlobalHook.kt`: reads bindings from `RemoteButtonStore`, uses `RemotePressTracker`. Returns `false` at once when a player session is open (`PlayerSessionTracker.isSessionOpen`, as used in `livetv/player/PlaybackActivity.kt`), so keys are never handled twice.
- Read the task enum in `SRC/data/local/RemoteButtons.kt`. Outside the player only tasks with an obvious app-level meaning act; all others return `false`.

### 2.3 B: link and hardware test (reuse base)

- Copy nothing from `UP/ke/.../core/assessment`.
- New `SRC/ui/screens/settings/DeviceTestSettingsItems.kt` (pattern: `KeCatalogSettingsItems.kt`): section "Device and connection test" with rows "Test connection speed" (existing `NetworkSettingsScreen`) and "Hardware report" (existing `diagnosticsCardItems(...)`). Find entry points with `grep -rn "NetworkSettingsScreen(\|diagnosticsCardItems(" FORK/app/src/main/java`. Embed the items if navigation lambdas are not reachable.
- Wire next to `keCatalogSettingsItems()` in `NuvioReshapedSettingsContent.kt`.

### 2.4 E: settings search (from Nuvio One)

- Copy 1:1 into `SRC/ui/screens/settings/`: `SettingsSearch.kt`, `SettingsSearchOverlay.kt` from `UP/one/app/src/main/java/com/nuvio/tv/ui/screens/settings/`.
- VERIFIED in fork: `SettingsSectionSpec`, `PlaybackSection`, `AppFeaturePolicy`. UNVERIFIED: `ui.screens.collection.NuvioTextField`.
- Allowed edits: replace `SettingsCategory.LIVE_TV` with `SettingsCategory.NUVIO_RESHAPED`; remove `SettingsSearchRoute.CloudStream` if the fork has no such destination; remove `PlaybackSection` values the fork lacks.
- Hook in `SettingsScreen.kt` (reference `UP/one/.../SettingsScreen.kt` lines 288 to 290, 407 to 433, 816 to 826, 927 onward): one state line, one rail item `settings_search_row`, one overlay block. Skip One's focus-restore logic if it fails twice.

### 2.5 D: calendar (from Echo)

Copy 1:1: `UP/echo/.../core/calendar/{CalendarModels,CalendarStore,CalendarRepository}.kt`, `UP/echo/.../ui/screens/calendar/{CalendarScreen,CalendarViewModel,CalendarReminderHost}.kt`, all `calendar_*` strings and `nav_calendar` into `res/values/echo_calendar_strings.xml`, drawable `ic_nav_calendar`.

Missing symbols (VERIFIED) and how to supply them:

| Symbol | Supply |
|---|---|
| `TmdbScheduleEpisode`, fields `nextEpisodeToAir`, `lastEpisodeToAir` (`UP/echo/.../data/remote/api/TmdbApi.kt` ~421 to 440) | additive edit of the fork's `TmdbApi.kt` (nullable, default `null`), marked `// Fork hook: echo-calendar`. Also copy any TMDB method `CalendarRepository.kt` calls that the fork lacks |
| `selectionSurface`, `scrollFade`, `BrowsePill`, `underSidebar`, `glassSurface`, `AmbientPageWash`, `NewLookNotifications`, `rememberBetaPerformanceSettings` | shims in `SRC/fork/echo/shims/`, same package and signature as Echo (procedure 2.6.3). `rememberBetaPerformanceSettings` and `NewLookNotifications` are shared with section 2.6: create them once |

Hooks: `Screen.kt` (`data object Calendar : Screen("calendar")`), `NuvioNavHost.kt` (route block, Echo lines 1181 to ~1190; `CalendarReminderHost`, Echo line 89, optional), `MainActivity.kt` (menu entry like the existing `liveTvMenuItem`, shown when `ForkHooks.isRouteEnabled(context, "calendar")`).

D5 consequence: without a TMDB key the calendar may be empty. Add an empty-state check to verification, not a workaround.

### 2.6 A: Echo Auto Quality, race, hot swap, quality menu (mandatory)

#### 2.6.1 Architecture

Echo's code is written as extension functions that read and write about 25 fields of `PlayerRuntimeController` and 8 fields of `PlayerUiState`. Three adapters make it compile 1:1 without rewriting the Reshaped controller:

1. **State holder with extension properties** (no edit to `PlayerRuntimeController.kt`).
   New `SRC/fork/echo/EchoControllerState.kt`, declared in `package com.nuvio.tv.ui.screens.player` so `internal` members are visible:
   - `internal class EchoControllerState` holding every Echo-only field with the exact type and initial value from `UP/echo/.../ui/screens/player/PlayerRuntimeController.kt` lines 412 to 413 and 684 to 729: `autoPlayHedgeJob`, `autoPlayHedgeUrl`, `sourceFallbackVideoKey`, `sourceFallbackTriedUrls`, `qualityOverlayJob`, `pendingAudioLanguageSwitch`, `sourceSwitch`, `sourceSwitchJob`, `sourceSwitchWatchJob`, `sourceSwitchGuardJob`, `autoQualityPolicy`, `autoQualityUserChoice`, `autoQualityJob`, `autoQualityPendingJob`, `autoQualitySeenStreamUrl`, `autoQualityLastWasDowngrade`, `autoQualitySourcesRequested`, `autoQualityAvailableCache`, `silentSourceKeys`, `hotSwapFailedSourceKeys`, `lastBandwidthEstimateBps`, `autoQualityHotSwap`, `playerBandwidthMeter`.
   - `private val echoStates = java.util.WeakHashMap<PlayerRuntimeController, EchoControllerState>()` and `internal val PlayerRuntimeController.echo: EchoControllerState` (synchronized get-or-create).
   - One extension property per field, for example `internal var PlayerRuntimeController.sourceSwitchJob: Job? get() = echo.sourceSwitchJob; set(v) { echo.sourceSwitchJob = v }`. Echo's files then compile unchanged.
   - If a field name already exists in the Reshaped controller with another meaning, do **not** add the extension (a member always wins over an extension). Record the name in `A_BLOCKERS.md`, then check by reading both usages whether the Reshaped member can serve; `sourceFallbackVideoKey` and `sourceFallbackTriedUrls` are the likely cases because Reshaped has its own source fallback.
2. **UI state and events** (additive edit of the base file that declares `PlayerUiState` and `PlayerEvent`; locate with `grep -rn "data class PlayerUiState" SRC`). Add, in one marked block each, with Echo's defaults (`UP/echo/.../PlayerUiState.kt` lines 155 to 189 and 348 to 351): fields `showQualityOverlay`, `currentStreamQuality`, `sourceSwitch`, `autoQualityEnabled`, `autoQualityActive`, `qualityLanguageNotes`, `autoQualityPending`; events `OnShowQualityOverlay`, `OnQualitySelected`, `OnAutoQualitySelected`, `OnCancelAutoQualityChange`.
3. **Settings adapter.** Echo reads flags through `BetaFeatures.settings(context)` (`UP/echo/.../core/player/FastPlaybackStart.kt` line 21) returning `BetaPerformanceSettings` (a 560-line DataStore with many unrelated settings). Do not copy that DataStore or the 766-line `BetaPerformanceSettingsContent.kt`. New `SRC/fork/echo/shims/BetaFeatures.kt` (package `com.nuvio.tv.core.player`): `object BetaFeatures { fun settings(context): BetaPerformanceSettings }`, plus a `data class BetaPerformanceSettings` containing **only the properties the copied files read** (find them with `grep -ohE "settings\.[a-zA-Z]+|BetaFeatures\.settings\([^)]*\)\.[a-zA-Z]+" <copied files> | sort -u`), same names, types and defaults as Echo. Values come from `ForkFeatures` keys (`echo_race` → `raceAutoPlaySources`, `echo_auto_quality` → `autoQuality`, `echo_hot_swap` → `autoQualityHotSwap`, plus `sourceFallback`, `sourceSwitchTimeoutSeconds`, `autoQualityConfirmSeconds`, `autoQualityLanguageLoyal` stored in the same SharedPreferences). New `SRC/ui/screens/settings/EchoPlaybackSettingsItems.kt` shows these as rows in the fork settings category.

Two source-fallback systems will coexist: Reshaped's `tryFallbackToNextSourceAfterError` and Echo's `tryFallbackToNextSource`. Rule: at each error site the Echo call is inserted **before** the Reshaped call and returns `false` at once when `echo_quality_menu` is off or no Echo source switch is in progress; then Reshaped's code runs as today. Never remove the Reshaped call.

#### 2.6.2 Phases (each compiles, commits and is switchable on its own)

**Phase A1: in-player quality menu and position-keeping source switch** (key `echo_quality_menu`)
- Copy 1:1: `PlayerRuntimeControllerSourceSwitch.kt` (482 lines), `PlayerRuntimeControllerQualityFallback.kt` (318), `QualitySelectionOverlay.kt` (203) from `UP/echo/.../ui/screens/player/`; `core/player/AutoPlayRace.kt` (539; SourceSwitch calls `AutoPlayRace.pickForSwitch` / `switchField`).
- Build adapters 1, 2, 3 above.
- Hook lines (Echo reference line → fork file; insert the same statement at the equivalent place):

| Fork file | Echo reference | Statement |
|---|---|---|
| `PlayerRuntimeControllerPlaybackEvents.kt` | 1628 to 1656 | the four `is PlayerEvent.On…Quality…` branches calling `showQualityOverlay()`, `selectStreamQuality(event.quality)`, `selectAutoQuality()`, `cancelAutoQualityChange(byViewer = true)`; `cancelSourceSwitch()`; `dismissQualityOverlay()` on back |
| `PlayerRuntimeControllerInitialization.kt` | 1243, 1339, 1342, 1510, 1678, 1755, 2196 | `isSourceSwitching()` conditions, `rejectUnseekableSwitchPick(...)`, `handleSourceSwitchFailure(...)`, `tryFallbackToNextSource(...)`. Where Echo changes an existing expression, wrap instead of replacing: `if (echoEnabled && isSourceSwitching()) <echo value> else <reshaped value>` |
| `PlayerRuntimeControllerMpv.kt` | 77, 206 | `tryFallbackToNextSource(...)` before the existing error path |
| `PlayerRuntimeControllerMpvEvents.kt` | 272 | same |
| `PlayerRuntimeControllerObservers.kt` | 954 | same |
| `PlayerRuntimeControllerStreams.kt` | 939, 1488 | `isSourceSwitching()` guard, `cancelSourceSwitch()` |
| `PlayerRuntimeControllerLifecycle.kt` | 18 | `cancelSourceSwitch()` on release |
| `PlayerScreen.kt` | 357, 1422, and where Echo composes `QualitySelectionOverlay` (`grep -n "QualitySelectionOverlay\|showQualityOverlay" UP/echo/.../PlayerScreen.kt`) | `abortSourceSwitch()`, `refreshCurrentStreamQuality()`, the overlay composable, and the control button that sends `OnShowQualityOverlay` |

**Phase A2: Auto Quality monitor** (key `echo_auto_quality`)
- Copy 1:1: `PlayerRuntimeControllerAutoQuality.kt` (401).
- Hooks: `PlayerViewModel.kt` (Echo line 154: `controller.startAutoQualityMonitor()`), `PlayerRuntimeControllerLifecycle.kt` (Echo line 19: `cancelAutoQualityChange(byViewer = false)`).
- `lastBandwidthEstimateBps` / `playerBandwidthMeter`: find where Echo assigns them (`grep -rn "playerBandwidthMeter\|lastBandwidthEstimateBps" UP/echo/app/src/main/java`) and insert the same assignment at the equivalent place in the fork (probably `PlayerRuntimeControllerInitialization.kt`, where ExoPlayer is built).

**Phase A3: hot swap** (key `echo_hot_swap`)
- Copy 1:1: `PlayerRuntimeControllerAutoQualityHotSwap.kt` (732).
- Hook: `PlayerRuntimeControllerLifecycle.kt` (Echo line 14: `cancelAutoQualityHotSwap("player released")`), plus any call the compiler reports as the only unresolved use of `startAutoQualityHotSwap` / `handleAutoQualityHotSwapFailure`.
- This file builds a second player and uses `PlayerMediaSourceFactory`, `DoviBridge`, `DolbyVisionBaseLayerPolicy`. Echo imports them from `com.nuvio.tv.core.player`; in the fork some live in `com.nuvio.tv.ui.screens.player`. Allowed edit: fix the `import` lines only. If a constructor or function signature differs, add a bridge function in `SRC/fork/echo/EchoReshapedBridge.kt` with Echo's signature that calls the fork's version.

**Phase A4: source race and hedge at start** (key `echo_race`)
- Copy 1:1: `PlayerRuntimeControllerAutoPlayHedge.kt` (67).
- `StreamAutoPlaySelector.raceCandidates(...)` exists only in Echo's selector (line ~219). Do not replace the Reshaped selector. Add `SRC/fork/echo/EchoRaceCandidates.kt` (package `com.nuvio.tv.core.player`) with an **extension function** `fun StreamAutoPlaySelector.raceCandidates(...)` whose body is copied from Echo. If it calls private members of Echo's selector, copy those private helpers into the same file.
- Hooks in `ui/screens/stream/StreamScreenViewModel.kt` (Echo reference lines 463, 530, 568, 630, 1529): each wrapped as `if (BetaFeatures.settings(ctx).raceAutoPlaySources) { <echo block> } else { <existing code> }`. In `PlayerRuntimeControllerStreams.kt` (Echo 2302 to 2314: `AutoPlayRace.fieldFor` / `pick`) and `PlayerRuntimeControllerInitialization.kt` (Echo 188: `armAutoPlayHedge(url)`).
- This phase changes which stream starts first, so it is the one most able to break normal playback. Keep the default off until checked on a device.

**Phase A5: audio-language menu** (key `echo_audio_menu`; owner decision D7)
- What it does: the audio menu lists languages found across all sources, and choosing one switches to a source that has it, keeping the position. Auto Quality uses the same code to stay in the viewer's language.
- Copy 1:1: `ui/screens/player/PlayerRuntimeControllerAudioMenu.kt` (334 lines), `ui/screens/player/AudioLanguageMenuContent.kt`, `core/player/AudioTrackProbe.kt` (88), `core/streams/StreamAudio.kt` (691; defines `AudioLanguage`, `AudioLanguageMenu`, `AudioMenuOption`, `AudioTrackRef`, `StreamAudioParser`).
- **Do this phase's file copy together with A1**, with its key off: A1's files call `listeningLanguage()`, `keepsLanguage()`, `preferLanguage()`, `qualityLanguageNotes()` and `keepLanguageOnNextSource()` from this file, so copying it removes those errors without stubs. Only the hooks and UI below belong to phase A5.
- State: add `pendingAudioLanguageSwitch` and `keepLanguageOnNextSource` (already listed in 2.6.1) plus any further field the compiler reports (rule 1 of 2.6.3). UI state and events: find Echo's with `grep -n "audioMenu\|AudioMenu\|AudioLanguage" UP/echo/.../ui/screens/player/PlayerUiState.kt` and add them to the same marked block.
- Hooks (find each with `grep -n "<function>(" UP/echo/.../ui/screens/player/<file>`): `PlayerRuntimeControllerTracks.kt` (`rememberListeningLanguage`, `applyPendingAudioLanguage`), `PlayerRuntimeControllerObservers.kt` and `PlayerRuntimeControllerInitialization.kt` (`verifyAudioSwitchDuration`, `applyPendingAudioLanguage`), `PlayerRuntimeControllerPlaybackEvents.kt` (event branches calling `startAudioLanguageMenu`, `switchSourceForAudio`, `showSourcesForAudio`), `PlayerRuntimeControllerMpv.kt`.
- UI: `AudioSelectionOverlay.kt` differs between Echo (708 lines) and the fork (914 lines, Reshaped-owned). Do **not** replace the fork's overlay. Insert one hook that shows Echo's `AudioLanguageMenuContent` as an extra section when `isAudioLanguageMenuEnabled()` is true. `PlayerRuntimeControllerTracks.kt` and `AudioSelectionOverlay.kt` become hooked overrides.
- Echo also touches `PlayerSettingsDataStore.kt` and `PlaybackAudioSettings.kt` for this feature (the list of menu languages). Keep those settings in the Echo settings adapter (`audioMenuLanguages`, default `AudioLanguage.defaultMenuKeys`) instead of editing the fork's DataStore.

**Phase A6: playback intro** (key `echo_playback_intro`; owner decision D7)
- What it does: a short animated intro shown while the stream loads.
- Copy 1:1: `core/intro/PlaybackIntroState.kt` (51 lines, no app imports; **copy it already in A1**, it removes the `PlaybackIntroState` errors in the Auto Quality files), `ui/screens/player/PlaybackIntro.kt` (581). Settings adapter: `playbackIntro`, `introLengthSeconds`.
- Hooks: `NuvioNavHost.kt` (Echo lines 78 to 88: key guard and `PlaybackIntroHost()`), `ui/screens/stream/StreamScreen.kt` (Echo 395 to 401: `startBeforePlayer()`), `PlayerScreen.kt` (Echo 642 to 644: `startOnPlayerOpen()`), `PlayerRuntimeControllerStreams.kt` (Echo 1166, 1255, 2005, 2029, 2059: `PlaybackIntroState.finish()` and the countdown condition). Check `UP/echo/.../PlayerRuntimeControllerMetadata.kt` for one more reference and place it in the fork file that holds the equivalent code.
- `PlaybackIntro.kt` draws with its own `IntroSurfaceView`; if it needs Echo drawables or raw resources, copy exactly those resources.

#### 2.6.3 Symbol closure procedure (use for every unresolved symbol in A and D)

Compile, collect `grep "^e: " /tmp/buildN.log`, and classify each unresolved name. Apply the first rule that matches:

1. It is an Echo-only controller field → add it to `EchoControllerState` and add an extension property.
2. It is a `PlayerUiState` field or `PlayerEvent` → add it to the marked additive block.
3. It is a small self-contained Echo declaration (needs at most 2 further missing symbols) → copy only that declaration into `SRC/fork/echo/shims/`, same package as in Echo.
4. It exists in the fork under another package or name → fix the `import` in the copied file, or add a bridge in `EchoReshapedBridge.kt` with Echo's signature.
5. It belongs to an Echo feature outside D3 and D7 (seek-preview keyframes `FileKeyframes`, Echo seek thumbnails, toast style `NewLookNotifications`, issue reports). The audio-language menu and the playback intro are **not** in this group: copy their real files (phases A5, A6) → write a neutral stub with Echo's signature: flags return `false`, lookups return `null` or an empty list, notifications call the fork's existing player message function if one is found by `grep -rn "showStreamSourceIndicator\|fun showPlayerMessage" SRC`, else do nothing. List every stub in `UPSTREAM_SOURCES.md` under "Echo stubs".
6. None of the above → write it to `A_BLOCKERS.md` and continue with other errors.

Never change logic inside a copied Echo function to make it compile.

#### 2.6.4 Cost

About 14 Reshaped-owned files receive hook lines (R2), 1 base file (`PlayerUiState`) gets an additive block, roughly 65 hook rows in `hooks.tsv` for the six phases. Realistic executor time: 3 to 5 working sessions.

### 2.7 C: switchable appearance as a separate module (design now, screens later)

#### 2.7.1 What the owner's mockup already gives (VERIFIED in `MOCK`)

- `generated/kotlin/*.kt`: 13 token files, currently generated into `package com.nuvio.tv.ui.theme` with the **same file names as the base theme files but different shapes** (for example `NuvioColorRole` enum, `NuvioSpacing.space*`). They were meant to overwrite the base theme. **They must not overwrite it**: the base and Reshaped screens read the old shapes and would stop compiling, and nothing would be switchable.
- `components/`: 16 components (`NButton`, `NCollection`, `NContentBlock`, `NControl`, `NHero`, `NIcon`, `NIndicator`, `NInput`, `NListRow`, `NMedia`, `NMediaCard`, `NNavigation`, `NOverlay`, `NScreen`, `NSurface`, `NTag`, `NText`), as web components.
- `screens/*.json`: 32 screens; `screens/index.json` maps them to routes named exactly like `ui/navigation/Screen.kt`. This is what makes wiring easy.

#### 2.7.2 Module layout

Directory module inside `:app` (a separate Gradle module cannot see the app's ViewModels):

```
SRC/skin/
  api/        SkinId.kt, SkinPreferences.kt, Skin.kt, SkinRegistry.kt      (contract; tiny)
  kit/
    tokens/   the 13 generated files                                       (generated, never hand-edited)
    theme/    KitTheme.kt                                                   (hand-written once)
    components/ NButton.kt … NText.kt                                       (16 composables, hand-written once)
    screens/  KitHomeScreen.kt, KitDetailScreen.kt, …                       (one file per route, added over time)
    KitSkin.kt                                                              (registers what exists)
```

Isolation rules (enforced by `fork-tools/check.sh`):
- `skin/kit/tokens` and `skin/kit/components` import nothing from `com.nuvio.tv.*` except `com.nuvio.tv.skin.*`. They stay portable and mirror `MOCK` one to one.
- Only `skin/kit/screens` and `KitSkin.kt` may import app ViewModels, UI state and domain models.
- Base and Reshaped files reference the skin **only** through `ForkHooks` (three hook surfaces below).

**Change on the mockup side (approved by the owner, D8).** VERIFIED: the package is one constant, `MOCK/scripts/kotlin-tokens.mjs` line 6: `const PKG = 'package com.nuvio.tv.ui.theme';`. Change it to `'package com.nuvio.tv.skin.kit.tokens'`, then run `npm run build` and `npm run check:kotlin` in `MOCK` and confirm both pass. If `check-kotlin.sh` or `scripts/kotlin-stubs/` contain the old package name, update them in the same change. Also correct the sentence in `MOCK/docs/nuvio-mapping.md` that says the generated files overwrite `ui/theme/*`: they now live beside the base theme. Another session may be working in `MOCK`: make this change in one go and do not touch anything else there.

#### 2.7.3 Contract (`skin/api`)

- `enum class SkinId { NUVIO, NETFLIX, KIT }`. `NUVIO` = today's app. `NETFLIX` = sets Reshaped's existing `NetflixUiPreferences` on. `KIT` = the owner's design.
- `SkinPreferences`: same pattern as the existing `ui/reshaped/netflix/NetflixUiPreferences.kt` (SharedPreferences + `StateFlow` + `rememberSkinId()`), so a change applies immediately without restart.
- `interface Skin` with three optional capabilities; a skin implements only what it has, and everything missing falls back to the normal app:

| Capability | Signature idea | Fallback |
|---|---|---|
| Theme | `@Composable fun Theme(content: @Composable () -> Unit)` | base `NuvioTheme` |
| Shell (navigation chrome) | `@Composable fun Scaffold(args: ShellArgs)` where `ShellArgs` carries exactly the parameters `PillNavScaffold` receives today (VERIFIED list: `navController`, `startDestination`, `currentRoute`, `rootRoutes`, `drawerItems`, `selectedDrawerRoute`, profile data, `onSwitchProfile`, `onNavigate`, `onExitApp`, …) | sidebar or pill nav |
| Screens | `fun screen(route: String): SkinScreen?`, where each `SkinScreen` receives the same ViewModel/UI state and navigation lambdas the default screen receives (model: the argument list of `NetflixHomeRoute` in `HomeScreen.kt` line ~378) | default screen for that route |

- Per-screen fallback is the key property: the owner can ship Home alone first, then Detail, and so on. A half-finished skin is always a working app.

#### 2.7.4 Hook surfaces (four, all in files that are already hook files)

| # | File and place | Hook |
|---|---|---|
| 1 | `MainActivity.kt` line ~618, the `NuvioTheme(` call | wrap content: `ForkHooks.SkinTheme { … }` (inside `NuvioTheme`, so base tokens still exist for non-skinned screens) |
| 2 | `MainActivity.kt` line ~1225, before `if (rememberPillNavEnabled())` | `if (ForkHooks.skinScaffold(shellArgs)) { } else if (…existing…)` |
| 3 | `HomeScreen.kt` line ~377, before `if (rememberNetflixUiEnabled())`, and later one line at the top of each route block in `NuvioNavHost.kt` | `if (ForkHooks.skinScreen("home", args)) { } else …existing…` |

| 4 | `PlayerScreen.kt` (already a hook file), around the composable that draws the controls and overlays on top of the video | `if (ForkHooks.skinPlayerChrome(uiState, onEvent)) { } else { …existing controls… }` |

Hook 3 is added per route only when the owner's design for that route exists. Suggested order, following `MOCK/screens/index.json`: `home` → `detail` → `streams` → `search` → `library` → `discover` → settings screens → profile, sign-in and onboarding screens → `player`.

**Player (owner decision D9: the skin covers everything).** The player is split in two layers and only the top one is skinned:
- *Engine* (never skinned): video surface, `PlayerRuntimeController*`, `PlayerViewModel`. Reshaped-owned.
- *Chrome* (skinned through hook 4): transport controls, seek bar, title block, and the panels for sources, quality, audio, subtitles and episodes. The contract is the pair the default chrome already uses: `PlayerUiState` in, `PlayerEvent` out. A kit player screen never calls the controller directly.
- Consequences: (a) every feature the plan adds to the player (Echo quality menu, Auto Quality notice, audio-language menu, Arc remote actions, Reshaped subtitle sync) works in the kit player automatically if its overlay exists there, and falls back to the default overlay if not; apply the same per-piece fallback as for screens, one overlay at a time. (b) Key handling stays in `PlayerScreen.kt` (`onPreviewKeyEvent`, including the Arc remote hook), so the remote keeps working under any skin. (c) Live TV screens come from Reshaped's module (`ui/reshaped/livetv`), not from base routes; skinning them needs a fifth hook inside that module and is the last item.
- Source design: `MOCK/screens/player.json`, `MOCK/screens/streams.json`, `MOCK/docs/player-report.md`.

#### 2.7.5 What is built now and what later

- **Now (step 10):** the generator package change (2.7.2), `skin/api/*`, hooks 1 to 3 (hook 3 for `home` only) and hook 4, the setting row "Appearance: Nuvio / Netflix / My design" in the fork settings category, and an empty `KitSkin` (no theme, no shell, no screens → identical to Nuvio). Result: the switch exists and is proven harmless.
- **Later (owner's request, own plan):** copy tokens; write `KitTheme.kt`; write the 16 `N*` composables from `MOCK/components` and `MOCK/presets`; then screens one route at a time from `MOCK/screens/<name>.json`. Each screen is verified against `MOCK/mockup/review` screenshots on the emulator.
- Theme-only shortcut available early: the base already supports a custom palette (`ui/theme/CustomThemePalette.kt`). `KitTheme` can feed the kit's colours through it, which recolours **every** existing screen before any kit screen exists.

No KE shell code and no notflix code is used: KE retired its own shell styles, and notflix is a whole app, not a module.

---

## 3. Alternative sources

| Source | URL | Relation to our base | Used for |
|---|---|---|---|
| Nuvio `dev` | github.com/NuvioMedia/NuvioTV | is the base (`2c853c1`) | B (speed test, diagnostics) |
| Reshaped | github.com/DavidVamaiotu/NuvioTV-Reshaped | real git fork, merges `dev` the same day | player/IPTV layer, second base |
| Echo | github.com/Laking22/Nuvio-Echo | snapshot repo, last push 2026-11-02 | **A** (only source of Auto Quality / race / hot swap) and **D** |
| Nuvio One | github.com/zoga2o3-ops/Nuvio-One | snapshot repo | YouTube (done), **E** |
| Arc | github.com/ysosrs123/NuvioTV-Fork | tracks official releases | remote only |
| KE | github.com/Arshad-Kamal/Nuvio-KE | snapshot repo | IPTV catalog only |
| notflix | github.com/DaoeZ/notflix | standalone app built on NuvioTV | nothing (pattern studied: full UI replacement, not modular) |
| NuvioPlusIPTV, NuvioTV-Lite, Nuvio-ReSeq, TuvoraTV, NuvioTV-Fast | GitHub forks of NuvioTV | forks | nothing; none has the missing features |

Checked: top 25 forks of `NuvioMedia/NuvioTV` by stars, repository search, local clones. No other public fork has Auto Quality, source race or hot swap, so Echo is the only source for A and its coupling to the player controller cannot be avoided by choosing another fork. Fork descriptions and file lists were checked, not every fork's full source.

---

## 4. Merge strategy

One repository, no submodules, no separate repositories.

### 4.1 File classes

| Class | What | Count after this plan | Update method |
|---|---|---|---|
| **O** override | base file replaced by Reshaped's version, byte-identical | about 48 | copy again from the new Reshaped commit |
| **OH** hooked override | Reshaped's version plus marked hook lines (Echo, remote, search, skin) | about 17 | copy from Reshaped, then re-insert every `hooks.tsv` row for that file |
| **H** hook | base file with our marked edits only | about 9 | take new upstream version, re-insert `hooks.tsv` rows |
| **M** module | new files (Reshaped, YouTube, KE, Arc, Echo, One, `fork/**`, `skin/**`) | n/a | re-copy from pinned clone; adapters are ours |

Count under the owner-approved rule D1 (hand-maintained = OH + H): **about 26**, under the limit of 35. The margin is small: the executor must not add hook files beyond those named in this plan without recording why in `STATUS.md`.

### 4.2 `FORK/fork-tools/` (step 3)

- `pins.env`: `NUVIO=2c853c1 RESHAPED=2dd10cd5e KE=0a87a08 ARC=343b275 ECHO=e952419 ONE=1067d29`.
- `overrides.txt`, `hooked-overrides.txt`: generated, not typed. For each file from `git diff --diff-filter=M --name-only 85f9010aa..HEAD`: identical to `UP/reshaped/<file>` → `overrides.txt`.
- `hooks.tsv`: `file`, `marker text` (the exact hook statement), `anchor` (a nearby unchanged line of the original file), `module`, `purpose`.
- `check.sh`, the contract test, run before every commit. Fails when: (a) an `overrides.txt` file differs from Reshaped at the pinned SHA; (b) a `hooked-overrides.txt` file, **after removing its `hooks.tsv` lines**, differs from Reshaped (proves we changed nothing else); (c) a `hooks.tsv` marker is missing from its file; (d) OH + H exceeds 35; (e) a base file is modified but in no list; (f) a file under `skin/kit/tokens` or `skin/kit/components` imports `com.nuvio.tv.` outside `com.nuvio.tv.skin`.
  To make (b) possible, every Echo hook must be a whole added line or a wrapped expression that the script can reverse; where an expression is wrapped (2.6.2), store the original line in a sixth `hooks.tsv` column.

### 4.3 Vendor branch

Create `vendor/nuvio` at `85f9010aa` (pure Nuvio snapshot). It only ever holds pure Nuvio snapshots.

### 4.4 Update procedure (only when the owner asks)

1. `git -C UP/reshaped fetch --all`; pick newest Reshaped release commit `R2`.
2. `N2 = git -C UP/reshaped merge-base R2 nuvio/dev`. Only ever update the base to `N2`.
3. `git checkout vendor/nuvio`; replace the tree with `git -C UP/nuvio archive N2`; commit.
4. `git checkout final && git merge vendor/nuvio`. Resolve by class: O → overwrite from `R2`; OH → overwrite from `R2`, re-insert hooks by anchor; H → take vendor version, re-insert hooks.
5. Overwrite every O file from `R2` even without conflict; re-copy Reshaped module files.
6. Review `git -C UP/reshaped diff --name-only N2 R2` minus known lists.
7. Update `pins.env`, compile, `check.sh`, emulator smoke test.

Limits: depends on Reshaped continuing to follow `dev`. Echo is a snapshot repo: a newer Echo means re-copying its files and re-running procedure 2.6.3; the adapters and hooks stay. If a Reshaped update moves an anchor line, the hook must be re-placed by hand using the Echo reference line.

### 4.5 Rejected

Hilt multibindings / `ServiceLoader` (KSP hides errors; few hook surfaces); submodules or separate repos (more for a non-technical owner to manage); `git format-patch` series (breaks on line movement; `hooks.tsv` with anchors survives it); splitting `PlayerRuntimeController` (replaced by the extension-property state holder, which needs no edit of that file).

---

## 5. Core-file reduction

No file is rewritten or reverted. The 65 Reshaped-identical files are not reduced: reverting a small diff to the base version can silently remove Reshaped behaviour, and identical files are the cheapest to maintain.

| | Now | After this plan |
|---|---|---|
| H (base + our hooks) | 7 | about 9 (`+ TmdbApi.kt`, `+ PlayerUiState` file) |
| OH (Reshaped + our hooks) | 0 | about 17: `MainActivity.kt`, `SettingsScreen.kt`, `PlayerViewModel.kt`, `StreamScreenViewModel.kt`, `StreamScreen.kt`, `AudioSelectionOverlay.kt`, `PlayerRuntimeControllerTracks.kt`, `PlayerRuntimeController{PlaybackEvents,Initialization,Mpv,MpvEvents,Observers,Streams,Lifecycle}.kt`, and up to 2 the compiler adds |
| O (Reshaped, untouched) | 65 | about 48 |
| Hand-maintained total (H + OH) | 7 | **about 26** (limit 35) |

`PlayerScreen.kt` and `HomeScreen.kt` are already in H.

---

## 6. Verification

Nothing has been run yet (D5). The plan therefore separates "compiles" from "seen working" in every report.

### 6.1 Setup (VERIFIED: emulator binary, AVD `gtv`, `adb` at `/opt/homebrew/bin/adb`)

`./gradlew --stop` → `~/Library/Android/sdk/emulator/emulator -avd gtv -no-snapshot-save -no-boot-anim &` → `adb wait-for-device` → `adb install -r <apk>` → launch with `adb shell monkey -p <applicationId from app/build.gradle.kts> 1`. Evidence per check: `adb exec-out screencap -p > <scratch>/NN.png` (look at it) and `adb logcat -d -s AndroidRuntime:E` (must be empty). Keys: `adb shell input keyevent DPAD_*`, `DPAD_CENTER`, `BACK`. Always end with `adb emu kill`.

Not provable on the emulator: Dolby Vision, HDR10, passthrough audio, TV performance. Report as "compiles, needs a real TV".

### 6.2 Checks

| # | Feature | Do | Expect |
|---|---|---|---|
| 1 | Startup | launch | first screen, empty crash log |
| 2 | Settings category | Settings → Nuvio Reshaped | all fork rows visible |
| 3 | Live TV | add `https://iptv-org.github.io/iptv/countries/pl.m3u`, open a channel | list appears; plays or shows a normal error |
| 4 | KE catalog | add one country | new Live TV source; filters change the list |
| 5 | YouTube | open a video | plays in the normal player |
| 6 | Remote | bind STATS; press in player; press a player-only button on Home | overlay toggles; nothing happens on Home and D-pad still works |
| 7 | Speech model | choose Whisper tiny | downloads, persists after restart |
| 8 | B | Device and connection test | speed test opens; hardware rows shown |
| 9 | E | Settings → Search → "sub" | results; selecting opens the category |
| 10 | D | menu → Calendar | renders; empty state acceptable (no TMDB key) |
| 11 | Skin switch | Appearance → My design → back to Nuvio | no visual change with the empty kit skin, no crash; Netflix option turns the Netflix look on |
| 12 | **All Echo switches off** | play any stream | behaviour identical to before step 11's build (regression guard for the hooks) |
| 13 | A1 | play a title with several sources; open quality menu; pick another quality | menu lists qualities with source counts; playback continues from the same position |
| 14 | A2 | enable Auto Quality; throttle the emulator network (`adb emu network speed gsm`) | pending-change notice appears, then a lower quality plays |
| 15 | A3 | same with hot swap on | switch happens without the loading overlay |
| 16 | A4 | enable race; start a title with autoplay | playback starts; log shows the race picking a winner |
| 17 | A5 | open the audio menu on a title whose sources differ in language; pick another language | menu lists languages across sources; playback continues from the same position in the chosen language |
| 18 | A6 | enable playback intro; start a title | intro plays while loading and ends when the video starts; BACK skips it |

Checks 13 to 18 need a streaming addon with several sources for one title. The owner will supply one when emulator or device testing starts (D6); do not ask again and do not install an addon on your own. Without it, A is reported as "compiles, hooks proven harmless by check 12, behaviour unverified".

---

## 7. Steps

S = under 1 h, M = 1 to 3 h, L = 3 to 6 h, XL = more than one session.

| # | Step | Section | Effort | Risk |
|---|---|---|---|---|
| 1 | Verify `arc-remote` compiles; build APK | n/a | S | low |
| 2 | **First run on emulator**: checks 1 to 5 and 7; fix crashes only | 6 | M | high |
| 3 | Branch `final`, `vendor/nuvio`, `fork-tools/` and `check.sh` | 4.2, 4.3 | S | low |
| 4 | `ForkHooks`, `ForkFeatures` defaults | 2.1 | S | low |
| 5 | Remote in whole app | 2.2 | S | low |
| 6 | B: device and connection test rows | 2.3 | S | low |
| 7 | E: settings search (One) | 2.4 | M | medium |
| 8 | D: calendar (Echo), including the shared Echo shims | 2.5 | L | medium-high |
| 9 | Make switches real (routes and menu entries respect `ForkFeatures`) | 2.1 | S | low |
| 10 | C: mockup generator package change, skin module frame, four hooks, appearance setting, empty `KitSkin` | 2.7.5 | M | medium |
| 11 | APK + emulator pass, checks 1 to 12 → **milestone "everything except A"** | 6 | M | medium |
| 12 | A1: quality menu and position-keeping switch (copy the A5 and A6 files here too, keys off) | 2.6 | XL | high |
| 13 | A2: Auto Quality monitor; A3: hot swap | 2.6 | XL | high |
| 14 | A4: race and hedge; A5: audio-language menu; A6: playback intro; APK; checks 12 to 18 | 2.6 | XL | high |
| 15 | Final documents (`STATUS.md`, `HANDOFF.md`, `UPSTREAM_SOURCES.md`, `../CLAUDE.md`) and feature list: working / partial / blocked | n/a | S | low |

A comes after the milestone on purpose: it is mandatory, but it is also the only work that edits Reshaped-owned player files, so everything else is finished and saved in git first. No push to GitHub in any step.

Later, each on the owner's request: owner's kit theme, components and screens (2.7.5); base update (4.4).

### Not ported

| Item | Reason |
|---|---|
| KE device-assessment tuner | rewrites player settings the fork lacks; base already tests link and hardware |
| KE shell code, notflix code | replaced by the owner's own skin module |
| Arc calendar, Arc settings search | replaced by Echo and One versions |
| Echo `BetaPerformanceSettingsDataStore` and its settings screen | replaced by a thin settings adapter with the same flags |
| Echo features outside D3 and D7 (Echo seek thumbnails, new-look toasts, issue reports) | stubbed neutrally so the mandatory files compile 1:1 |
| YouTube storyboard previews, debug report dialog | stay as empty adapters |

---

## Questions for the owner

All earlier questions are answered (decisions D1 to D9 at the top). Nothing blocks the executor.

One thing the owner must provide later: a streaming addon that returns several sources per title, at the moment emulator or device testing starts (D6). Until then checks 13 to 18 stay "compiles, behaviour unverified".
