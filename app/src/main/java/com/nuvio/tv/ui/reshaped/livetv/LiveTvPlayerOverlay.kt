@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.reshaped.livetv

import android.view.KeyEvent
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.ProxyHeaders
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBehaviorHints
import com.nuvio.tv.reshaped.livetv.LIVE_TV_UNGROUPED
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvCatchupLinks
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvPlaybackRegistry
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvReplay
import com.nuvio.tv.reshaped.livetv.LiveTvReplayWindow
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.reshaped.livetv.LiveTvPreferences
import com.nuvio.tv.reshaped.livetv.LiveTvUiState
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerMediaSourceFactory
import com.nuvio.tv.ui.screens.player.PlayerRuntimeController
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.onEvent
import com.nuvio.tv.ui.screens.player.switchToSourceStream
import com.nuvio.tv.ui.theme.NuvioTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Live TV inside Nuvio's player: CH+/CH- (and ▲▼ while the controls are hidden) switch channel,
 * ◀ opens the channel list and ◀ again its categories (▶ there lists a channel's programmes), ▶ the player's controls, and a banner shows what is on after each switch. Everything is
 * inert unless the player is playing a Live TV channel.
 */
@Stable
internal class LiveTvPlayerState(
    private val controller: PlayerRuntimeController,
    private val containerFocusRequester: FocusRequester,
    private val scope: CoroutineScope,
) {
    var panelOpen by mutableStateOf(false)
        private set
    /** The categories column beside the channel list (◀ from the list). */
    var foldersOpen by mutableStateOf(false)
        private set
    /** The category the panel lists, or null for the list being zapped. */
    var panelFolderKey by mutableStateOf<String?>(null)
        private set
    /** The channels the panel lists. */
    var panelChannels by mutableStateOf<List<LiveTvChannel>>(emptyList())
        private set
    private var folderJob: Job? = null
    /** The list entry of the channel playing now (a Stalker link differs from its list URL). */
    var currentListUrl by mutableStateOf<String?>(null)
        private set
    /** Bumped on each switch so the banner shows again. */
    var bannerKey by mutableIntStateOf(0)
        private set

    private var switchJob: Job? = null

    /** The channel focused in the panel, which ▶ lists the programmes of. */
    internal var panelFocusedUrl: String? = null
    /** The channel whose programmes the panel lists (▶ from the channel list), or null. */
    var programmesChannel by mutableStateOf<LiveTvChannel?>(null)
        private set
    /** The channel the list comes back to from its programmes, instead of the one playing. */
    internal var panelReturnUrl: String? = null
        private set

    /** Whether the day column beside the programmes has the focus (▶ from the programmes). */
    var programmeDaysFocused by mutableStateOf(false)

    private fun openProgrammes() {
        val url = panelFocusedUrl ?: currentListUrl
        programmeDaysFocused = false
        programmesChannel = panelChannels.firstOrNull { it.streamUrl == url } ?: return
    }

    /** Back to the channel list, on the channel whose programmes were shown. */
    internal fun closeProgrammes() {
        val channel = programmesChannel ?: return
        panelReturnUrl = channel.streamUrl
        programmeDaysFocused = false
        programmesChannel = null
    }

    /** OK on a programme: an ended one plays again where the provider keeps it; anything else plays live. */
    internal fun playProgramme(channel: LiveTvChannel, programme: LiveTvProgramme) {
        val now = LiveTvClock.nowEpochMs()
        if (programme.stopEpochMs <= now && LiveTvCatchupLinks.isPlayable(channel.catchup, programme, now)) {
            playCatchup(channel, programme)
        } else {
            pickFromPanel(channel)
        }
    }

    /** The Now/Next card OK shows over the picture; OK again opens the controls. Never pauses. */
    var infoOpen by mutableStateOf(false)
        private set
    private var infoJob: Job? = null
    /** The release of an OK press Live TV acted on, which must not reach the controls it opened. */
    private var swallowOkRelease = false
    /** OK is down on the bare picture: a short press acts on release, a held one starts the programme over. */
    private var okHeld = false

    internal fun showInfo() { // forge: internal (MiniGuide keeps the card up)
        infoOpen = true
        infoJob?.cancel()
        infoJob = scope.launch {
            // forge fix 14.11 (makieta 7.6, ZACHOWANIE 5.14: „w pauzie zostaje”): paused, the MiniGuide stays and the pause screen waits
            // (it took over after 5 s, and OK on „Info” then resumed instead)
            val until = android.os.SystemClock.uptimeMillis() + INFO_MS
            while (android.os.SystemClock.uptimeMillis() < until || (!controller.uiState.value.isPlaying && controller.uiState.value.error == null)) {
                if (controller.uiState.value.showPauseOverlay && !controller.uiState.value.isPlaying) controller._uiState.update { it.copy(showPauseOverlay = false) }
                delay(250L)
            }
            infoOpen = false
        }
    }

    internal fun hideInfo() {
        infoJob?.cancel()
        infoJob = null
        infoOpen = false
    }

    /** The player, for the details the Now/Next card shows. */
    internal val player: PlayerRuntimeController get() = controller

    /** forge fix 14.11 (JMGO A2): leaves the player (set by the player screen); a long Back and Back in the archive's question use it. */
    internal var exit: (() -> Unit)? = null
    private var backHeldHandled = false

    /**
     * forge fix 14.11 (JMGO A1/A4): pause and resume at once, without the film player's own bar (its range is the whole
     * stream: 24 h ahead live, the archive's whole recording): live it is a time shift, the MiniGuide shows where it stands.
     */
    internal fun togglePause() {
        val playing = controller.uiState.value.isPlaying
        controller.onEvent(PlayerEvent.OnPlayPause, showPlaybackControls = false)
        // „paused” = behind live from a pause on: it stays after resuming, until the channel goes live again
        if (playing && !isCatchup()) com.nuvio.tv.skin.kicia.livetv.ForgeMiniGuideNav.paused = true
        if (!isCatchup()) com.nuvio.tv.skin.kicia.livetv.ForgeMiniGuideNav.onPauseToggled(nowPaused = playing) // forge hook 7.7 E19: the shift in minutes
    }

    /** forge fix 14.11 (JMGO A1): whether the picture stands still now. */
    internal fun isPausedNow(): Boolean = !controller.uiState.value.isPlaying

    /** forge fix 14.11 (JMGO A2): a held Back (repeats, or a late release) leaves the player from any state; true = consumed. */
    private fun forgeLongBack(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_BACK && event.keyCode != KeyEvent.KEYCODE_ESCAPE) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) { backHeldHandled = false; return false }
        val held = (event.action == KeyEvent.ACTION_DOWN && event.repeatCount > 0) ||
            (event.action == KeyEvent.ACTION_UP && event.eventTime - event.downTime >= ViewConfiguration.getLongPressTimeout())
        if (backHeldHandled) { if (event.action == KeyEvent.ACTION_UP) backHeldHandled = false; return true }
        if (!held) return false
        backHeldHandled = event.action == KeyEvent.ACTION_DOWN // the release that follows is swallowed
        hideInfo()
        zapConfirm = null
        exit?.invoke()
        return true
    }

    /** Whether the player is showing a Live TV channel (read on each key; a memory lookup). */
    fun isActive(): Boolean = LiveTvPlaybackRegistry.isLiveTv(controller.currentStreamUrl)

    internal fun syncCurrent() {
        if (currentListUrl == null && isActive()) {
            currentListUrl = LiveTvPlaybackRegistry.listUrlFor(controller.currentStreamUrl)
            bannerKey++
            // forge hook (JMGO 6): a replay opened from the guide gets the same start guard as one started here
            if (isCatchup()) currentChannel()?.let { ch ->
                // forge fix 14.11 (JMGO A5): a programme opened from the guide's archive also stops at its own end
                LiveTvPlaybackRegistry.replayWindow(controller.currentStreamUrl)?.let { window ->
                    val at = window.startMs + REPLAY_BIND_SLACK_MS
                    replayBound = LiveTvRepository.schedule(ch.guideKey).firstOrNull { at >= it.startEpochMs && at < it.stopEpochMs }
                    replayBoundHit = false
                }
                controller.currentStreamUrl.let { watchReplayStart(ch, it, it) }
            }
            else currentChannel()?.let(::watchLiveStart) // forge hook v7.4: the first channel opened gets the same guard
        }
    }

    /** The list zapping moves through: the one the channel was picked from, else every shown channel. */
    internal fun zapList(): List<LiveTvChannel> = zapTarget().first

    /** The list zapping moves through and the category it is (null for a search). */
    private fun zapTarget(): Pair<List<LiveTvChannel>, String?> {
        val picked = LiveTvRepository.zapList
        if (picked.any { it.streamUrl == currentListUrl }) return picked to LiveTvRepository.zapFolderKey
        // Worked out off the main thread whenever the list or what is hidden changes.
        return LiveTvRepository.uiState.value.shownChannels to FILTER_ALL
    }

    /** The category the panel's list is, so the categories open on it; null for a search. */
    var zappedFolderKey by mutableStateOf<String?>(null)
        private set

    /** Called first by the player's key handler; true when the key was Live TV's. */
    fun onPreviewKey(event: KeyEvent, uiState: PlayerUiState): Boolean {
        // Fork patch: dostawca #10 - gdy trwa sesja Live TV (wybrano kanał), klawisze zostają Live TV nawet gdy strumień chwilowo nie jest zarejestrowany
        if (!isActive() && currentListUrl == null) return false
        if (forgeLongBack(event)) return true // forge fix 14.11 (JMGO A2): a long Back always leaves at once
        if (com.nuvio.tv.skin.kicia.livetv.ForgeChannelFailed.channel.value != null) return com.nuvio.tv.skin.kicia.livetv.ForgeChannelFailed.onKey(this, event, exit) // forge fix 22.11 (UX): the „Kanał nie odpowiada” window runs its own keys (Back the first time, ◀▶ OK on its buttons)
        // test 15.11 (B-6): Back on the legend closes the legend only (it also left the player)
        if (com.nuvio.tv.skin.kicia.livetv.ForgeRemoteLegend.pressed != -1 && (event.keyCode == KeyEvent.KEYCODE_BACK || event.keyCode == KeyEvent.KEYCODE_ESCAPE)) {
            if (event.action == KeyEvent.ACTION_UP) com.nuvio.tv.skin.kicia.livetv.ForgeRemoteLegend.dismiss()
            return true
        }
        if (archiveEnd != null || com.nuvio.tv.skin.kicia.livetv.ForgeRemoteLegend.pressed != -1) return false // forge hook 7.7 E25 / H9: the card or the legend own the keys
        val nuvioOverlayOpen = uiState.showEpisodesPanel || uiState.showSourcesPanel ||
            uiState.showAudioOverlay || uiState.showSubtitleOverlay || uiState.showSubtitleStylePanel ||
            uiState.showSpeedDialog || uiState.showSubtitleDelayOverlay || uiState.showSubtitleTimingDialog ||
            uiState.showMoreDialog || uiState.showStreamInfoOverlay
        val down = event.action == KeyEvent.ACTION_DOWN
        if (scrub != null) return onScrubKey(event, down, uiState)
        // forge fix 14.11 (JMGO A1/A4): the remote's play/pause keys pause at once from any state, without the film bar
        if (!nuvioOverlayOpen && uiState.error == null && event.keyCode in FORGE_PAUSE_KEYS) {
            if (down && event.repeatCount == 0) {
                val playing = uiState.isPlaying
                val wanted = when (event.keyCode) { KeyEvent.KEYCODE_MEDIA_PLAY -> !playing; KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP -> playing; else -> true }
                if (wanted) togglePause()
                if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE && !panelOpen) showInfo()
            }
            return true
        }
        // forge fix 14.11 (JMGO A1): over the pause screen OK resumes here too (the player's own resume brings its bar)
        if (!nuvioOverlayOpen && uiState.error == null && uiState.showPauseOverlay && event.keyCode in OK_KEYS && !panelOpen && !infoOpen) { // forge fix 14.11 (JMGO A6): with the MiniGuide up OK is its own
            if (down && event.repeatCount == 0) { togglePause(); swallowOkRelease = true; okHeld = false }
            return true
        }
        if (panelOpen && programmesChannel != null) {
            return when (event.keyCode) {
                // Back to the channels, on the channel whose programmes these are.
                // From the days, back to the programmes first.
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    if (!down) {
                        if (programmeDaysFocused) programmeDaysFocused = false else closeProgrammes()
                    }
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (down && event.repeatCount == 0) {
                        if (programmeDaysFocused) programmeDaysFocused = false else closeProgrammes()
                    }
                    true
                }
                // ▶ to the day column, when the programmes span more than one day.
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (down && event.repeatCount == 0) programmeDaysFocused = true
                    true
                }
                KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN -> true
                else -> false // the list handles the rest
            }
        }
        if (panelOpen && foldersOpen) {
            return when (event.keyCode) {
                // Back to the channels, which show the category last focused.
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (!down) foldersOpen = false
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN -> true
                else -> false // the column handles the rest
            }
        }
        if (panelOpen) {
            return when (event.keyCode) {
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    if (!down) closePanel()
                    true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (down && event.repeatCount == 0) openProgrammes()
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (down && event.repeatCount == 0) foldersOpen = true
                    true
                }
                KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN -> true
                else -> false // the list handles the rest
            }
        }
        when (event.keyCode) {
            KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                if (nuvioOverlayOpen && uiState.error == null) return false
                if (down) zap(if (event.keyCode == KeyEvent.KEYCODE_CHANNEL_UP) -1 else 1)
                return true
            }
        }
        if (event.keyCode in OK_KEYS && !down && swallowOkRelease) {
            swallowOkRelease = false
            return true
        }
        if (event.keyCode !in OK_KEYS) okHeld = false
        // A channel that failed: ▲▼ still zap away from it (most remotes have no CH+/CH-). ◀▶ stay
        // the error screen's, which moves between its buttons.
        if (uiState.error != null && !nuvioOverlayOpen && !isCatchup()) {
            return when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (down) zap(if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) -1 else 1)
                    true
                }
                else -> false
            }
        }
        if (isCatchup()) {
            // A past programme seeks and pauses like a film: only ▲▼ (back to live channels) and
            // Back on the bare picture (back to this channel live) stay Live TV's.
            if (uiState.showControls || nuvioOverlayOpen || uiState.error != null) return false
            // forge (owner 10.11): from the archive too, OK opens the MiniGuide (pause, restart, info) instead of the player's own controls.
            if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE) {
                if (infoOpen) return com.nuvio.tv.skin.kicia.livetv.ForgeMiniGuideNav.onKey(this, event)
                if (event.keyCode in OK_KEYS) { if (!down) { togglePause(); showInfo() }; return true } // forge fix 14.11 (JMGO A1): OK pauses at once (and shows the MiniGuide)
            }
            return when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (down) zap(if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) -1 else 1)
                    true
                }
                // A replay the player cannot seek in (TS, most providers' kind) moves by asking for
                // another replay instead; one that seeks (HLS) seeks like a film.
                in SCRUB_KEYS -> {
                    // ◀ at the very start of one that seeks (a programme started over, which
                    // began early) reaches back past its start with the rewind bar.
                    // forge fix 14.11 (JMGO A4): an HLS replay no longer goes to the film bar (it spans the whole archive, −23:57:22): the rewind bar always
                    val channel = currentChannel() ?: return false
                    if (down && event.repeatCount == 0) openScrub(channel, if (event.keyCode in BACK_SCRUB_KEYS) -1 else 1)
                    channel.catchup != null
                }
                KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                    // forge fix 14.11 (JMGO A2): over the pause screen Back asks too (it opened the film bar first, then took two more Backs)
                    if (uiState.showPauseOverlay && !com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE) return false
                    if (uiState.showPauseOverlay && !down) controller._uiState.update { it.copy(showPauseOverlay = false) }
                    // A channel gone since (removed, or its source edited) leaves Back to the player.
                    val channel = currentChannel() ?: return false
                    if (!down && com.nuvio.tv.fork.tv.LiveNoAsk.back(controller.context, ::hideInfo, exit)) return true // Fork hook: live-no-ask
                    if (!down) { if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE) { if (zapConfirm == null) { hideInfo(); zapConfirm = 0 } } else switchTo(channel) } // forge hook (owner 13.11): Back from the archive asks first, like ▲▼ (step 0 = this channel live)
                    true
                }
                else -> false
            }
        }
        // ▲▼◀ and OK are Live TV's only on the bare picture: never over controls, panels or errors.
        if (uiState.showControls || nuvioOverlayOpen || uiState.error != null || (uiState.showPauseOverlay && !infoOpen)) { // forge fix 14.11 (JMGO A6): the MiniGuide keeps its keys while paused
            if (infoOpen) hideInfo()
            return false
        }
        if (!infoOpen && com.nuvio.tv.skin.kicia.livetv.ForgeArchiveOffer.onKey(this, event)) return true // forge hook (owner 13.11): OK takes the archive offer
        if (infoOpen && com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE) return com.nuvio.tv.skin.kicia.livetv.ForgeMiniGuideNav.onKey(this, event) // forge hook: MiniGuide buttons (S09)
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                // A short press acts on release (info, then the controls); held, the programme on
                // now plays from its start where the provider keeps it. The player never sees OK
                // (which pauses).
                when {
                    down && event.repeatCount == 0 -> okHeld = true
                    down -> if (okHeld && startOverFromHold()) {
                        okHeld = false
                        swallowOkRelease = true
                    }
                    okHeld -> {
                        okHeld = false
                        // Some remotes send no repeats for a held OK, only a late release.
                        val held = event.eventTime - event.downTime >= ViewConfiguration.getLongPressTimeout()
                        when {
                            held && startOverFromHold() -> Unit
                            infoOpen -> {
                                hideInfo()
                                controller.onEvent(PlayerEvent.OnToggleControls)
                            }
                            else -> { togglePause(); showInfo() } // forge fix 14.11 (JMGO A1): OK on the bare picture pauses at once (a time shift) and shows the MiniGuide
                        }
                    }
                }
                true
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                if (!infoOpen) return false
                if (!down) hideInfo()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE && !infoOpen) return forgeHoldKey(event) // forge hook 7.7b H9: short on release, long ≥ 520 ms
                if (down) zap(if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) -1 else 1)
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE && !infoOpen) return forgeHoldKey(event) // forge hook 7.7b H9
                if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE) { if (down && event.repeatCount == 0) showInfo(); return true } // forge hook: hidden UI ◀ = MiniGuide (mockup S09), not the channel list
                // With the info card up, ◀ rewinds a channel the provider keeps (catch-up).
                val rewind = infoOpen && currentChannel()?.catchup != null
                if (down && event.repeatCount == 0) {
                    val channel = currentChannel()
                    if (rewind && channel != null) openScrub(channel, -1) else openPanel()
                }
                true // also swallows the release, which would commit a seek
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                val channel = currentChannel()?.takeIf { it.catchup != null } ?: return false
                if (down && event.repeatCount == 0) openScrub(channel, -1)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE && !infoOpen) return forgeHoldKey(event) // forge hook 7.7b H9
                if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE) { if (down && event.repeatCount == 0) showInfo(); return true } // forge hook: hidden UI ▶ = MiniGuide (mockup S09), not Nuvio's controls
                // A live channel has nothing to seek to: ▶ opens the controls (audio, subtitles).
                if (down && event.repeatCount == 0) {
                    hideInfo()
                    controller.onEvent(PlayerEvent.OnToggleControls)
                }
                true
            }
            else -> false
        }
    }

    /**
     * forge hook 7.7b H9: with the controls hidden, ▲▼◀▶ act on RELEASE (short: ▲▼ zap, ◀▶ MiniGuide); held ≥ 520 ms the
     * long action runs once instead (◀ from the start, ▶ next episode, ▼ Szczegóły i odcinki, ▲ the MiniGuide) and the
     * remote legend may follow.
     */
    private var holdCode = -1
    private var holdSince = 0L
    private var holdFired = false
    private fun forgeHoldKey(event: KeyEvent): Boolean {
        val code = event.keyCode
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                if (event.repeatCount == 0 || holdCode != code) { holdCode = code; holdSince = event.downTime; holdFired = false }
                if (!holdFired && (event.isLongPress || event.eventTime - holdSince >= 520L)) { holdFired = true; forgeLongAction(code) } // test 15.11: the system's long-press flag counts too
            }
            KeyEvent.ACTION_UP -> {
                if (holdCode == code && !holdFired) when (code) {
                    KeyEvent.KEYCODE_DPAD_UP -> zap(-1)
                    KeyEvent.KEYCODE_DPAD_DOWN -> zap(1)
                    else -> showInfo()
                }
                holdCode = -1; holdFired = false
            }
        }
        return true
    }
    private fun forgeLongAction(code: Int) {
        val channel = currentChannel() ?: return
        val now = LiveTvClock.nowEpochMs()
        val shown = replayingProgramme ?: LiveTvRepository.uiState.value.currentProgrammes[channel.guideKey]
        when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> startOverProgramme(channel)?.let {
                playCatchup(channel, it); com.nuvio.tv.skin.kicia.components.ForgeToast.show("Od początku programu", top = true)
            } ?: com.nuvio.tv.skin.kicia.components.ForgeToast.show("Ten kanał nie ma archiwum", top = true)
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val next = shown?.let { p -> LiveTvRepository.schedule(channel.guideKey).filter {
                    it.title.trim().equals(p.title.trim(), ignoreCase = true) && it.startEpochMs > p.startEpochMs && it.stopEpochMs <= now &&
                        LiveTvCatchupLinks.isPlayable(channel.catchup, it, now)
                }.minByOrNull { it.startEpochMs } }
                if (next != null) playCatchup(channel, next)
                else {
                    val after = shown?.let { p -> LiveTvRepository.schedule(channel.guideKey).firstOrNull { it.startEpochMs >= p.stopEpochMs } }
                    com.nuvio.tv.skin.kicia.components.ForgeToast.show("Nie ma jeszcze następnego odcinka", top = true,
                        note = after?.let { "Kolejny program: " + com.nuvio.tv.skin.kicia.livetv.forgeTvTime(it.startEpochMs) + " · " + it.title })
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> shown?.let { com.nuvio.tv.skin.kicia.livetv.ForgeMiniGuideNav.detail = channel to it }
            KeyEvent.KEYCODE_DPAD_UP -> showInfo()
        }
        // test 15.11 (B-4/B-5): no legend over the details (long ▼); after ◀ ▶ it waits for the Komunikat to be read
        if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) scope.launch { kotlinx.coroutines.delay(2_500); com.nuvio.tv.skin.kicia.livetv.ForgeRemoteLegend.offer(code) }
    }

    /** Where the playing channel was in the list last zapped, so a zap skips searching for it. */
    private var zapIndexHint = -1

    /** Fork patch: dostawca #10 - zmiana kanału podczas archiwum czeka na potwierdzenie. */
    internal var zapConfirm by mutableStateOf<Int?>(null)

    internal fun zap(step: Int) { // forge: internal (MiniGuide ▲▼)
        if (isCatchup() && com.nuvio.tv.fork.tv.LiveNoAsk.zapNow(controller.context, step)) return zapNow(step) // Fork hook: live-no-ask
        if (isCatchup()) {
            if (zapConfirm == null) { hideInfo(); zapConfirm = step }
            return
        }
        zapNow(step)
    }

    internal fun zapNow(step: Int) {
        hideInfo()
        val url = currentListUrl
        val picked = LiveTvRepository.zapList
        val pickedHint = zapIndexHint.takeIf { it in picked.indices && picked[it].streamUrl == url }
        val list = if (pickedHint != null || picked.any { it.streamUrl == url }) picked else LiveTvRepository.uiState.value.shownChannels
        if (list.isEmpty()) return
        val index = zapIndexHint.takeIf { it in list.indices && list[it].streamUrl == url } ?: list.indexOfFirst { it.streamUrl == url }
        val next = if (index < 0) 0 else Math.floorMod(index + step, list.size)
        zapIndexHint = next
        com.nuvio.tv.skin.kicia.livetv.ForgeMiniGuideNav.zapAt = android.os.SystemClock.uptimeMillis() // forge hook 7.7b H8: the tile's countdown
        switchTo(list[next])
    }

    /**
     * Gives the key focus back to the player once a failed channel's error screen is gone (its
     * focused button left with it), so the next ▲▼ or OK is not lost and Back stays Live TV's.
     */
    internal suspend fun refocusPlayer() {
        repeat(5) {
            withFrameNanos { }
            if (panelOpen || controller._uiState.value.showControls) return
            if (runCatching { containerFocusRequester.requestFocus() }.isSuccess) return
        }
    }

    private fun openPanel() {
        hideInfo()
        panelFocusedUrl = null
        panelReturnUrl = null
        programmesChannel = null
        folderJob?.cancel()
        panelFolderKey = null
        val (channels, folderKey) = zapTarget()
        panelChannels = channels
        zappedFolderKey = folderKey
        foldersOpen = false
        panelOpen = true
    }

    /** Shows a category's channels in the panel; zapping follows once one of them is picked. */
    internal fun showFolder(key: String) {
        if (key == panelFolderKey) return
        panelFolderKey = key
        panelReturnUrl = null
        folderJob?.cancel()
        folderJob = scope.launch {
            val state = LiveTvRepository.uiState.value
            panelChannels = withContext(Dispatchers.Default) {
                filterChannels(state.channels, state.favoriteUrls, state.hiddenGroups, state.hiddenChannelKeys, key, customLists = state.customLists)
            }
        }
    }

    /** A channel picked from the panel: zapping then stays in the list it was picked from. */
    internal fun pickFromPanel(channel: LiveTvChannel) {
        panelFolderKey?.let { LiveTvRepository.setZapList(panelChannels, it) }
        switchTo(channel)
    }

    /** The rewind bar, while it shows (see [LiveTvScrub]). */
    var scrub by mutableStateOf<LiveTvScrub?>(null)
    /** forge hook v7.4 (07): the rewind runs on the MiniGuide's own bar (the MiniGuide stays, no native bar). */
    var scrubInInfo by mutableStateOf(false)
        private set
    private var scrubJob: Job? = null
    /** OK went down while the rewind bar showed: its release plays the time picked. */
    private var scrubOkDown = false

    /** Whether the replay playing can be sought in by the player itself (HLS with a length). */
    private fun replaySeeks(): Boolean =
        controller._exoPlayer?.let { it.isCurrentMediaItemSeekable && it.duration != androidx.media3.common.C.TIME_UNSET } == true

    /** Whether the replay playing is within a few seconds of its own start: the player's bar can go no further back. */
    private fun atReplayStart(): Boolean =
        controller._exoPlayer?.currentPosition?.let { it < REPLAY_START_MARGIN_MS } == true

    /** Where the picture is in the channel's time: now when live, else the replay's position. */
    private fun playheadMs(now: Long): Long {
        val window = LiveTvPlaybackRegistry.replayWindow(controller.currentStreamUrl) ?: return now
        return (window.startMs + controller.playbackTimeline.value.currentPosition.coerceAtLeast(0L)).coerceAtMost(now)
    }


    /** The earliest time [channel]'s provider keeps, with a minute to spare. */
    private fun earliestMs(channel: LiveTvChannel, now: Long): Long =
        now - (channel.catchup?.days ?: 0) * 24L * 60 * 60 * 1000 + 60_000L

    /** Shows the rewind bar on where the picture is, moved one [direction] step. */
    internal fun openScrub(channel: LiveTvChannel, direction: Int, keepInfo: Boolean = false) { // forge: internal (MiniGuide bar)
        if (channel.catchup == null) return
        // Its older days, for stepping back past what the guide keeps in memory.
        LiveTvRepository.requestHistory(channel)
        // forge hook v7.4 (07): from the MiniGuide's bar the MiniGuide stays and its bar moves; otherwise the native rewind bar
        scrubInInfo = keepInfo
        if (keepInfo) showInfo() else hideInfo()
        val now = LiveTvClock.nowEpochMs()
        val at = playheadMs(now)
        val from = LiveTvScrubSteps.rangeStart(LiveTvRepository.schedule(channel.guideKey), at, earliestMs(channel, now))
        scrub = LiveTvScrub(channel, from.coerceAtMost(at), at)
        stepScrub(direction, 0)
    }

    /**
     * Moves the picked time; ◀ at the bar's start reaches back to the show before. A held ◀ stops
     * at the start of a show (it would race on through the show before): a fresh press goes on.
     */
    private fun stepScrub(direction: Int, repeatCount: Int) {
        val bar = scrub ?: return
        val now = LiveTvClock.nowEpochMs()
        val schedule = LiveTvRepository.schedule(bar.channel.guideKey)
        if (direction < 0 && bar.targetMs <= bar.fromMs && repeatCount == 0) {
            val earlier = LiveTvScrubSteps.rangeStart(schedule, bar.fromMs - 1, earliestMs(bar.channel, now))
            if (earlier < bar.fromMs) bar.fromMs = earlier
        }
        var target = bar.targetMs + direction * LiveTvScrubSteps.stepMs(repeatCount)
        if (direction < 0 && repeatCount > 0) target = LiveTvScrubSteps.heldBackTo(schedule, bar.targetMs, target)
        bar.targetMs = target.coerceIn(bar.fromMs, now)
        // A key held down again keeps the bar up.
        scrubJob?.cancel()
        scrubJob = null
    }

    /** As the player's own bar does after ◀▶ are let go: plays from the time picked once the keys rest. */
    private fun settleScrub() {
        scrubJob?.cancel()
        scrubJob = scope.launch {
            delay(SCRUB_SETTLE_MS)
            commitScrub()
        }
    }

    private fun closeScrub() {
        scrubJob?.cancel()
        scrubJob = null
        scrubOkDown = false
        scrub = null
        scrubInInfo = false // forge hook v7.4 (07)
    }

    /** Plays from the picked time: a replay from there, or the channel live when that is now. */
    private fun commitScrub() {
        val bar = scrub ?: return
        closeScrub()
        val channel = bar.channel
        val now = LiveTvClock.nowEpochMs()
        if (now - bar.targetMs < LiveTvScrubSteps.LIVE_MARGIN_MS) {
            if (isCatchup()) switchTo(channel)
            return
        }
        val start = bar.targetMs
        // forge fix 14.11 (JMGO A4): within a replay the player can seek in (HLS with a length), ◀▶ seeks instead of loading the stream again
        val window = LiveTvPlaybackRegistry.replayWindow(controller.currentStreamUrl)
        val length = controller._exoPlayer?.duration ?: 0L
        if (isCatchup() && channel.streamUrl == currentListUrl && window != null && replaySeeks() && start >= window.startMs && start < window.startMs + length - REPLAY_END_HOLD_MS) {
            switchJob?.cancel()
            replayBound = replayBound?.takeIf { start >= it.startEpochMs && start < it.stopEpochMs }
            replayBoundHit = false
            controller._exoPlayer?.seekTo(start - window.startMs)
            return
        }
        switchJob?.cancel()
        switchJob = scope.launch {
            val replay = LiveTvRepository.replayChannel(channel, start)
            if (replay == null) {
                android.widget.Toast.makeText(controller.context, R.string.live_tv_catchup_failed, android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            currentListUrl = channel.streamUrl
            val programme = LiveTvRepository.schedule(channel.guideKey).firstOrNull { start >= it.startEpochMs && start < it.stopEpochMs }
            replayTitleProgramme = programme
            // forge fix 14.11 (JMGO A5): a rewind of live runs on (back to the archive's limit, forward to now); within a programme picked from the archive it still ends there
            replayBound = replayBound?.takeIf { start >= it.startEpochMs && start < it.stopEpochMs }
            replayBoundHit = false
            controller._uiState.update { it.copy(title = programme?.title ?: channel.name, logo = LiveTvRepository.uiState.value.logoFor(channel)) }
            playReplay(channel, replay)
        }
    }

    /** Keys while the rewind bar shows: ◀▶ move (played from once let go), OK plays at once, Back leaves it. */
    private fun onScrubKey(event: KeyEvent, down: Boolean, uiState: PlayerUiState): Boolean {
        if (panelOpen || uiState.showControls) {
            closeScrub()
            return false
        }
        when (event.keyCode) {
            in SCRUB_KEYS -> if (down) { stepScrub(if (event.keyCode in BACK_SCRUB_KEYS) -1 else 1, event.repeatCount); if (scrubInInfo) showInfo() } else settleScrub() // forge hook v7.4 (07): the MiniGuide stays up while it rewinds
            in OK_KEYS -> if (down && event.repeatCount == 0) {
                scrubOkDown = true
            } else if (!down && scrubOkDown) {
                commitScrub()
            }
            KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> if (down) commitScrub()
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> if (!down) closeScrub()
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                // Leaves the bar; the key then zaps as usual.
                closeScrub()
                return onPreviewKey(event, uiState)
            }
        }
        return true
    }

    /** Whether the player shows a past programme (catch-up) rather than the live channel. */
    internal fun isCatchup(): Boolean = LiveTvPlaybackRegistry.isCatchup(controller.currentStreamUrl)

    /** The list entry of the channel playing (live or catch-up). */
    private fun currentChannel(): LiveTvChannel? =
        currentListUrl?.let { url -> LiveTvRepository.uiState.value.channels.firstOrNull { it.streamUrl == url } }

    /** Starts the programme on now over, when the channel playing can; whether it did. */
    private fun startOverFromHold(): Boolean {
        val channel = currentChannel() ?: return false
        val programme = startOverProgramme(channel) ?: return false
        playCatchup(channel, programme)
        return true
    }

    /** What is on now on [channel], when its provider can play it from the start; null otherwise. */
    internal fun startOverProgramme(channel: LiveTvChannel): LiveTvProgramme? {
        val now = LiveTvClock.nowEpochMs()
        val programme = LiveTvRepository.uiState.value.currentProgrammes[channel.guideKey] ?: return null
        return programme.takeIf { it.startEpochMs < now && now < it.stopEpochMs && LiveTvCatchupLinks.isPlayable(channel.catchup, it, now) }
    }

    /**
     * Plays [channel]'s [programme] again from its start (past, or on now: start over); it stays
     * the channel ▲▼ zap from, and plays on past the programme's end (see [followReplay]).
     */
    internal fun playCatchup(channel: LiveTvChannel, programme: LiveTvProgramme) {
        closePanel()
        hideInfo()
        switchJob?.cancel()
        switchJob = scope.launch {
            // forge fix 15.11 (JMGO): a resume starts inside an airing (no guide entry starts there) — that very airing plays,
            // the race could pick another airing of the episode and the replay then missed its own end
            val resume = LiveTvRepository.schedule(channel.guideKey).none { it.startEpochMs == programme.startEpochMs }
            val (channel, programme, replay) = if (resume) Triple(channel, programme, LiveTvRepository.catchupChannel(channel, programme))
                else com.nuvio.tv.skin.kicia.livetv.ForgeAiringRace.pick(channel, programme) // forge hook (owner 13.11): every airing of this episode races
            if (replay == null) {
                // What plays stays as it was.
                android.widget.Toast.makeText(controller.context, R.string.live_tv_catchup_failed, android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }
            currentListUrl = channel.streamUrl
            bannerKey++
            replayTitleProgramme = programme
            replayBound = programme // forge fix 14.11 (JMGO A5): a programme picked from the archive ends where it ends
            replayBoundHit = false
            controller._uiState.update { it.copy(title = programme.title, logo = LiveTvRepository.uiState.value.logoFor(channel)) }
            playReplay(channel, replay)
        }
    }

    private fun playReplay(channel: LiveTvChannel, replay: LiveTvReplay) {
        val url = PlayerMediaSourceFactory.normalizePlaybackRequest(replay.playback.streamUrl, replay.playback.headers).url
        LiveTvPlaybackRegistry.register(url, listUrl = channel.streamUrl, catchup = true, window = replay.window)
        replayFallback = replay.fallback?.let { url to it }
        controller.switchToSourceStream(channel.toStream(replay.playback))
        watchReplayStart(channel, url, replay.playback.streamUrl)
    }

    /**
     * forge hook (JMGO test 13.11: an archive hung 70 s on „Uruchamianie strumienia…” with no word): a replay that shows no
     * picture in [REPLAY_START_MS] tries its other link; with none, says so and goes back to the channel live.
     */
    private var replayStartJob: Job? = null
    private fun watchReplayStart(channel: LiveTvChannel, url: String, rawUrl: String) {
        replayStartJob?.cancel()
        replayStartJob = scope.launch {
            delay(REPLAY_START_MS)
            val still = controller.currentStreamUrl == url || controller.currentStreamUrl == rawUrl
            if (!still || controller.hasRenderedFirstFrame) return@launch
            android.util.Log.i("ForgeAiringRace", "replay start timeout on ${channel.name}")
            if (playReplayFallback(url)) return@launch
            com.nuvio.tv.skin.kicia.components.ForgeToast.show("Archiwum nie odpowiada — wracam na żywo", top = true)
            replayTitleProgramme = null
            pickFromPanel(channel)
        }
    }

    private val REPLAY_START_MS = 20_000L

    /** The replay playing (its player URL) and the other link to play should the provider fail it. */
    private var replayFallback: Pair<String, LiveTvReplay>? = null

    /** Plays the other link of the replay at [url] when the provider failed it; whether it did. */
    internal fun playReplayFallback(url: String): Boolean {
        val (failed, fallback) = replayFallback ?: return false
        if (failed != url) return false
        val channel = currentChannel() ?: return false
        replayFallback = null
        switchJob?.cancel()
        playReplay(channel, fallback)
        return true
    }

    /** The programme the title shows while a replay plays. */
    private var replayTitleProgramme: LiveTvProgramme? = null
    /** Fork hook: tv - miejsce obrazu w czasie kanału. */
    internal fun playheadEpochMs(): Long = playheadMs(LiveTvClock.nowEpochMs())

    /** Fork patch: dostawca #10 */
    internal val replayingProgramme: LiveTvProgramme? get() = replayTitleProgramme?.takeIf { isCatchup() }
    /** The replay whose end has been acted on, so it is acted on once. */
    private var continuedWindow: LiveTvReplayWindow? = null
    /** forge fix 14.11 (JMGO A5): the programme picked from the archive, whose end the replay stops at; null for a rewind of live. */
    private var replayBound: LiveTvProgramme? = null
    /** forge hook 7.7 E25/H10: the archive programme whose end shows the „Następny odcinek” card; null = no card. */
    internal var archiveEnd by mutableStateOf<LiveTvProgramme?>(null)
    private var endLogMs = 0L

    /** forge 7.7 E25: after [ended], the next episode of the same programme still in the archive, else what followed on the channel. */
    internal fun nextAfterArchiveEnd(ended: LiveTvProgramme): Pair<LiveTvChannel, LiveTvProgramme>? {
        val channel = currentChannel() ?: return null
        val now = LiveTvClock.nowEpochMs()
        val schedule = LiveTvRepository.schedule(channel.guideKey)
        val title = ended.title.trim().lowercase()
        val sameNext = schedule.filter {
            it.title.trim().lowercase() == title && it.startEpochMs > ended.startEpochMs && it.episode != ended.episode &&
                it.stopEpochMs <= now && LiveTvCatchupLinks.isPlayable(channel.catchup, it, now)
        }.minByOrNull { it.startEpochMs }
        val after = schedule.firstOrNull { it.startEpochMs >= ended.stopEpochMs }
        return (sameNext ?: after)?.let { channel to it }
    }

    /** forge 7.7 E25: plays what the card offers (from the archive, or the channel live when it airs now). */
    internal fun playAfterArchiveEnd() {
        val ended = archiveEnd ?: return
        archiveEnd = null
        val (channel, next) = nextAfterArchiveEnd(ended) ?: return
        if (next.stopEpochMs <= LiveTvClock.nowEpochMs()) playCatchup(channel, next) else switchTo(channel, force = true)
    }
    private var replayBoundHit = false

    /**
     * Follows a replay as it plays ([positionMs] into it, [durationMs] long when known): the title
     * becomes the programme it has got to, and near its end the next part plays, or the channel
     * live once the replay has caught up. One map lookup when nothing is replayed.
     */
    internal fun followReplay(positionMs: Long, durationMs: Long) {
        val window = LiveTvPlaybackRegistry.replayWindow(controller.currentStreamUrl) ?: return
        val channel = currentChannel() ?: return
        val at = window.startMs + positionMs
        val shown = replayTitleProgramme
        if (shown == null || at < shown.startEpochMs || at >= shown.stopEpochMs) {
            LiveTvRepository.schedule(channel.guideKey).firstOrNull { at >= it.startEpochMs && at < it.stopEpochMs }?.let { programme ->
                replayTitleProgramme = programme
                if (controller._uiState.value.title != programme.title) controller._uiState.update { it.copy(title = programme.title) }
            }
        }
        (replayBound ?: replayTitleProgramme)?.let { com.nuvio.tv.fork.tv.TvStore.recordReplay(channel, it, at) } // forge hook 7.7 G31/H16: where the archive was left
        // test 15.11 (JMGO): a replay ran past its programme's end without stopping — say once per minute what the end check sees
        if (android.os.SystemClock.elapsedRealtime() - endLogMs > 60_000L) {
            endLogMs = android.os.SystemClock.elapsedRealtime()
            android.util.Log.i("ForgeReplayEnd", "at=$at windowStart=${window.startMs} pos=$positionMs bound=${replayBound?.let { it.title + " " + it.startEpochMs + "-" + it.stopEpochMs }} hit=$replayBoundHit bounded=${window.bounded}")
        }
        // forge fix 14.11 (JMGO A5): a programme picked from the archive stops at its own end (the provider's stream runs on
        // to now, into the next programmes); a seek past the end comes back to it.
        replayBound?.let { bound ->
            val endAt = bound.stopEpochMs - window.startMs
            when {
                at > bound.stopEpochMs + REPLAY_PAST_END_MS -> controller._exoPlayer?.seekTo((endAt - REPLAY_END_HOLD_MS).coerceAtLeast(0L))
                at >= bound.stopEpochMs - REPLAY_END_HOLD_MS -> if (!replayBoundHit) {
                    replayBoundHit = true
                    if (controller.uiState.value.isPlaying) togglePause()
                    // forge hook 7.7 E25/H10: the „Następny odcinek za 10 s” card instead of the MiniGuide
                    if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE && nextAfterArchiveEnd(bound) != null) { hideInfo(); archiveEnd = bound } else showInfo()
                }
                else -> replayBoundHit = false
            }
            return
        }
        // A replay's length is known for HLS; a TS replay is as long as was asked for. One that
        // names no end runs on to live by itself, and is only followed should it stop.
        if (!window.bounded) return
        val length = if (durationMs > 0L) durationMs else window.endMs - window.startMs
        if (positionMs > 0L && positionMs >= length - REPLAY_END_MARGIN_MS) continueReplay(window)
    }

    /** At the end of [window]: the next part of the channel, or the channel live once caught up. */
    internal fun continueReplay(window: LiveTvReplayWindow) {
        if (continuedWindow === window) return
        continuedWindow = window
        val channel = currentChannel() ?: return
        if (LiveTvClock.nowEpochMs() - window.endMs < REPLAY_CAUGHT_UP_MS) {
            replayTitleProgramme = null
            switchTo(channel)
            return
        }
        switchJob?.cancel()
        switchJob = scope.launch {
            // Gone from the provider since (or no longer kept): the channel live.
            val replay = LiveTvRepository.replayChannel(channel, window.endMs) ?: return@launch switchTo(channel)
            playReplay(channel, replay)
        }
    }

    internal fun switchTo(channel: LiveTvChannel, force: Boolean = false) { // forge: force = back to live on the same channel (08 „Oglądaj na żywo”)
        if (!force && channel.streamUrl == currentListUrl && !isCatchup()) {
            closePanel()
            return
        }
        currentListUrl = channel.streamUrl
        bannerKey++
        replayTitleProgramme = null
        replayBound = null // forge fix 14.11 (JMGO A5)
        com.nuvio.tv.skin.kicia.livetv.ForgeMiniGuideNav.paused = false // forge fix 14.11 (JMGO A6): live again, not shifted
        closePanel()
        // The loading screen and pause screen show the player's logo: the new channel's, from the first press.
        controller._uiState.update { it.copy(title = channel.name, logo = LiveTvRepository.uiState.value.logoFor(channel)) }
        // Quick presses land on the last channel only.
        switchJob?.cancel()
        switchJob = scope.launch {
            delay(ZAP_SETTLE_MS)
            val playback = LiveTvRepository.prepareForPlayback(channel)
            LiveTvPlaybackRegistry.register(
                PlayerMediaSourceFactory.normalizePlaybackRequest(playback.streamUrl, playback.headers).url,
                listUrl = channel.streamUrl,
            )
            controller.switchToSourceStream(channel.toStream(playback))
            watchLiveStart(channel) // forge hook v7.4
        }
    }

    /**
     * forge hook v7.4 (test 13.11: TVP 1 HD hung >100 s on „Uruchamianie strumienia…” with no word; owner: no fixed waits,
     * check every second). Each second until the first picture:
     * - the player failed, or no data came in [LIVE_NO_DATA_MS]: the channel starts once more on a fresh address
     *   (the provider starts an idle channel on its first viewer); failing again, it says so;
     * - data is buffered but the decoder gives no picture for [LIVE_NO_PICTURE_MS] after the player is ready: the
     *   device cannot show this channel (e.g. 1080i on the emulator), so it says so at once.
     */
    private var liveStartJob: Job? = null
    private fun watchLiveStart(channel: LiveTvChannel) {
        liveStartJob?.cancel()
        liveStartJob = scope.launch {
            var restarted = false
            var since = android.os.SystemClock.elapsedRealtime()
            var readySince = 0L
            while (true) {
                delay(1_000L)
                if (!isActive() || currentListUrl != channel.streamUrl || isCatchup()) return@launch
                val p = controller._exoPlayer
                if (controller.hasRenderedFirstFrame || p?.isPlaying == true) return@launch
                val now = android.os.SystemClock.elapsedRealtime()
                val failed = p == null || p.playerError != null || p.playbackState == androidx.media3.common.Player.STATE_IDLE
                val noData = (p?.totalBufferedDuration ?: 0L) <= 0L && now - since >= LIVE_NO_DATA_MS
                if (p?.playbackState == androidx.media3.common.Player.STATE_READY) { if (readySince == 0L) readySince = now } else readySince = 0L
                val noPicture = readySince > 0L && now - readySince >= LIVE_NO_PICTURE_MS
                if (!(failed && now - since >= 1_000L) && !noData && !noPicture) continue
                android.util.Log.i("ForgeLiveStart", "${channel.name} failed=$failed noData=$noData noPicture=$noPicture restarted=$restarted after=${now - since}ms")
                if (noPicture) { com.nuvio.tv.skin.kicia.components.ForgeToast.show("Ten kanał nie wyświetla się na tym urządzeniu — wybierz inny", top = true); return@launch }
                if (restarted) { com.nuvio.tv.skin.kicia.livetv.ForgeChannelFailed.channel.value = channel; return@launch } // makieta 07 blad-kanalu
                restarted = true
                since = now
                readySince = 0L
                val playback = LiveTvRepository.prepareForPlayback(channel)
                LiveTvPlaybackRegistry.register(
                    PlayerMediaSourceFactory.normalizePlaybackRequest(playback.streamUrl, playback.headers).url,
                    listUrl = channel.streamUrl,
                )
                controller.switchToSourceStream(channel.toStream(playback))
            }
        }
    }

    /**
     * forge fix 15.11 (JMGO: the TV lost the internet for 2 min while AXN played, Nuvio's raw „Błąd odtwarzania” with a
     * Java exception stayed after the network came back): a live channel that drops while watched starts once more,
     * quietly; failing again, the start guard's „Kanał nie odpowiada” says so. While the start guard still watches, or
     * its window is up, the raw error is only hidden (the guard decides).
     */
    internal fun recoverLiveDrop() {
        val error = controller.uiState.value.error ?: return
        if (!isActive() || isCatchup()) return
        val channel = currentChannel() ?: return
        controller._uiState.update { it.copy(error = null) }
        if (liveStartJob?.isActive == true || com.nuvio.tv.skin.kicia.livetv.ForgeChannelFailed.channel.value != null) return
        android.util.Log.i("ForgeLiveStart", "${channel.name} dropped while watched, restarting: $error")
        switchTo(channel, force = true)
    }

    private val LIVE_NO_DATA_MS = 6_000L
    private val LIVE_NO_PICTURE_MS = 3_000L

    /** Switches the display to the playing channel's frame rate (see [LiveTvFrameRateMatch]). */
    internal suspend fun matchDisplay(fps: Float, raw: Float) = controller.matchDisplayToLiveTrack(fps, raw)

    internal fun closePanel() {
        if (!panelOpen) return
        // The programmes stay as they are while the panel slides away; opening it clears them.
        panelOpen = false
        foldersOpen = false
        runCatching { containerFocusRequester.requestFocus() }
    }

    private fun LiveTvChannel.toStream(playback: LiveTvChannel) = Stream(
        name = name,
        title = name,
        description = group.takeIf(String::isNotBlank),
        url = playback.streamUrl,
        ytId = null,
        infoHash = null,
        fileIdx = null,
        externalUrl = null,
        behaviorHints = StreamBehaviorHints(
            notWebReady = null,
            bingeGroup = null,
            countryWhitelist = null,
            proxyHeaders = ProxyHeaders(request = playback.headers, response = null),
        ),
        addonName = LIVE_TV_ADDON_NAME,
        addonLogo = null,
    )

    private companion object {
        const val ZAP_SETTLE_MS = com.nuvio.tv.skin.kicia.livetv.ForgeMiniGuideNav.ZAP_MS // forge hook 7.7b H8: 500 ms (was 350)
        /** How near its end a replay hands over to what follows, before the player stops at its end. */
        const val REPLAY_END_MARGIN_MS = 1_500L
        /** forge fix 14.11 (JMGO A5): a programme from the archive pauses this near its end; past it by more, it comes back. */
        const val REPLAY_END_HOLD_MS = 2_000L
        const val REPLAY_PAST_END_MS = 5_000L
        /** Providers start a replay a little early: the programme is the one on this long after the replay's start. */
        const val REPLAY_BIND_SLACK_MS = 120_000L
        /** A replay this near to now goes live rather than asking for a few seconds more. */
        const val REPLAY_CAUGHT_UP_MS = 90_000L
        const val INFO_MS = 6_000L
        /** After ◀▶ are let go, the rewind bar waits this long for another press before playing from there. */
        const val SCRUB_SETTLE_MS = 1_200L
        const val REPLAY_START_MARGIN_MS = 5_000L
        val SCRUB_KEYS = intArrayOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)
        val BACK_SCRUB_KEYS = intArrayOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_REWIND)
        val OK_KEYS = intArrayOf(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)
        /** forge fix 14.11 (JMGO A1): the remote's pause keys. */
        val FORGE_PAUSE_KEYS = intArrayOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_SPACE)
    }
}

@Composable
internal fun rememberLiveTvPlayer(controller: PlayerRuntimeController, containerFocusRequester: FocusRequester): LiveTvPlayerState {
    val scope = rememberCoroutineScope()
    return remember(controller) { LiveTvPlayerState(controller, containerFocusRequester, scope) }
}

/** The banner and channel panel, drawn over the player. */
@Composable
internal fun LiveTvPlayerOverlay(state: LiveTvPlayerState, uiState: PlayerUiState) {
    if (!state.isActive()) return
    Box(modifier = Modifier.fillMaxSize().zIndex(3f)) {
        LiveTvPlayerOverlayContent(state, uiState)
    }
}

@Composable
private fun BoxScope.LiveTvPlayerOverlayContent(state: LiveTvPlayerState, uiState: PlayerUiState) {
    LaunchedEffect(Unit) { state.syncCurrent() }
    // Live TV lets go of its channels while unseen (the app in the background for a while):
    // back on a channel, they load again so zapping and the channel list work.
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    DisposableEffect(lifecycleOwner) {
        LiveTvClock.followDeviceHourFormat(context)
        LiveTvRepository.reloadIfReleased()
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_START) LiveTvRepository.reloadIfReleased() }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LiveTvFrameRateMatch(state, uiState)
    LiveTvReplayFollow(state)
    // Only after an error screen: elsewhere the player's own focus handling stands.
    val hasError = uiState.error != null
    var hadError by remember { mutableStateOf(false) }
    LaunchedEffect(hasError) {
        if (hasError) {
            hadError = true
        } else if (hadError) {
            hadError = false
            state.refocusPlayer()
        }
    }
    val liveState by LiveTvRepository.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(liveState.isLoaded, liveState.isLoading, liveState.hasSource) {
        if (!liveState.isLoaded && !liveState.isLoading && !liveState.hasSource) LiveTvRepository.reloadIfReleased()
    }
    val clock = rememberLiveTvMinuteClock()
    // Numbered within the list being zapped (a category keeps its own 1, 2, 3...).
    val zapList = remember(state.currentListUrl, liveState.shownChannels) { state.zapList() }
    val currentIndex = remember(state.currentListUrl, zapList) { zapList.indexOfFirst { it.streamUrl == state.currentListUrl } }
    val current = zapList.getOrNull(currentIndex)

    // Fork hook: tv - pozycja co 10 s (na żywo i z archiwum)
    LaunchedEffect(state.currentListUrl) {
        while (state.currentListUrl != null) {
            delay(10_000L)
            com.nuvio.tv.fork.tv.most.TvBridge.onPosition(state.currentListUrl, state.isCatchup(), state.playheadEpochMs())
        }
    }

    // Fork patch: dostawca #10
    state.zapConfirm?.let { step ->
        com.nuvio.tv.fork.tv.ui.ForkZapConfirm(
            programmeTitle = state.replayingProgramme?.title,
            onStay = { state.zapConfirm = null },
            onChange = { state.zapConfirm = null; state.zapNow(step) },
        )
    }

    var bannerVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.bannerKey) {
        if (state.bannerKey == 0) return@LaunchedEffect
        if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE) { state.showInfo(); return@LaunchedEffect } // forge hook: a switch shows the MiniGuide (S09), not the old banner
        bannerVisible = true
        delay(BANNER_MS)
        bannerVisible = false
    }
    AnimatedVisibility(
        visible = bannerVisible && current != null && !uiState.showControls && !state.panelOpen && !state.infoOpen,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopStart).zIndex(3f),
    ) {
        current?.let { channel ->
            LiveTvBanner(
                channel = channel,
                logo = liveState.logoFor(channel),
                programme = liveState.currentProgrammes[channel.guideKey],
                number = currentIndex + 1,
                clock = clock,
            )
        }
    }

    val details by rememberLiveTvStreamDetails(state.player, state.infoOpen)
    AnimatedVisibility(
        visible = state.infoOpen && current != null && !uiState.showControls && !state.panelOpen,
        enter = slideInVertically { it / 3 } + fadeIn(),
        exit = slideOutVertically { it / 3 } + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter).zIndex(3f),
    ) {
        current?.let { channel ->
            LiveTvInfoCard(
                channel = channel,
                logo = liveState.logoFor(channel),
                details = details,
                now = liveState.currentProgrammes[channel.guideKey],
                number = currentIndex + 1,
                clock = clock,
                startOver = remember(channel, liveState.currentProgrammes, clock.value / 60_000L) { state.startOverProgramme(channel) != null },
                rewind = channel.catchup != null,
                replay = state.replayingProgramme,
                playheadMs = state.playheadEpochMs(),
                scrubMs = state.scrub?.takeIf { state.scrubInInfo }?.targetMs, // forge hook v7.4 (07)
            )
        }
    }

    if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE) com.nuvio.tv.skin.kicia.livetv.ForgeArchiveOfferCard(state) // forge hook (owner 13.11): earlier airing from the archive
    com.nuvio.tv.skin.kicia.livetv.ForgeChannelFailedDialog(state) // forge hook v7.4 (makieta 07 blad-kanalu): the start guard's second failure
    com.nuvio.tv.skin.kicia.livetv.ForgeArchiveEndCard(state) // forge hook 7.7 E25/H10: „Następny odcinek za 10 s”
    com.nuvio.tv.skin.kicia.livetv.ForgeRemoteLegendDialog() // forge hook 7.7b H9: the remote legend after a long press
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { com.nuvio.tv.skin.kicia.livetv.ForgeRemoteLegend.dismiss() } } // test 15.11 (B-6): it stayed for the next channel
    LaunchedEffect(uiState.error) { state.recoverLiveDrop() } // forge fix 15.11 (JMGO): a channel that drops while watched restarts, no raw error
    state.zapConfirm?.let { step ->
        com.nuvio.tv.skin.kicia.livetv.ForgeZapConfirm(
            programmeTitle = state.replayingProgramme?.title,
            toLive = step == 0, // forge: Back (step 0) = this channel live
            logoUrl = com.nuvio.tv.skin.kicia.livetv.forgeCurrentChannelLogo(state), // forge v7.6
            onStay = { state.zapConfirm = null },
            onChange = { state.zapConfirm = null; state.zapNow(step) },
            // forge fix 14.11 (JMGO A2): asked by Back, a second Back leaves (it used to close the question and ask again forever)
            onBack = if (step == 0) ({ state.zapConfirm = null; state.exit?.invoke() }) else null,
        )
    }

    AnimatedVisibility(
        visible = state.scrub != null && !state.scrubInInfo && !uiState.showControls && !state.panelOpen, // forge hook v7.4 (07): no native bar when the MiniGuide rewinds
        // As the player's own seek bar comes and goes.
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150)),
        modifier = Modifier.align(Alignment.BottomCenter).zIndex(3f),
    ) {
        // The last bar stays drawn through the exit animation, after it has closed.
        val shown = remember { arrayOfNulls<LiveTvScrub>(1) }
        state.scrub?.let { shown[0] = it }
        shown[0]?.let { bar -> LiveTvScrubBar(bar) }
    }

    AnimatedVisibility(
        visible = state.panelOpen,
        enter = slideInHorizontally { -it } + fadeIn(),
        exit = slideOutHorizontally { -it } + fadeOut(),
        modifier = Modifier.align(Alignment.CenterStart).zIndex(3f),
    ) {
        LiveTvChannelPanel(state, liveState.currentProgrammes, clock)
    }
    com.nuvio.tv.skin.kicia.livetv.ForgeMiniGuideDetailHost(state) // forge hook: S08 from the MiniGuide

}

/** Near solid, so the banner and info card read clearly over any picture. */
internal val LiveTvCardBackground = Color(0xF0121214)

@Composable
private fun LiveTvBanner(channel: LiveTvChannel, logo: String?, programme: LiveTvProgramme?, number: Int, clock: State<Long>) {
    Row(
        modifier = Modifier
            .padding(start = 48.dp, top = 40.dp)
            .widthIn(max = 640.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(LiveTvCardBackground)
            .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number > 0) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(end = 16.dp),
            )
        }
        LiveTvLogo(url = logo, name = channel.name, width = 88.dp, height = 54.dp)
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (programme != null) {
                Text(
                    text = programme.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    LiveTvProgressBar(
                        programme = programme,
                        clock = clock,
                        fill = Color.White,
                        track = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.width(220.dp),
                    )
                    Text(
                        text = "${LiveTvClock.formatSpan(programme)}  ·  ${liveTvTimeLeft(programme, clock)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.live_tv_player_hint),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.62f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * The Now/Next card OK shows: the channel with its picture and sound (resolution, frame rate,
 * codecs), what is on with how far it has got, and what follows.
 */
@Composable
private fun LiveTvInfoCard(
    channel: LiveTvChannel,
    logo: String?,
    details: LiveTvStreamDetails,
    now: LiveTvProgramme?,
    number: Int,
    clock: State<Long>,
    startOver: Boolean,
    rewind: Boolean,
    replay: LiveTvProgramme? = null,
    playheadMs: Long = 0L,
    scrubMs: Long? = null, // forge hook v7.4 (07)
) {
    // Read once per minute tick: the kept guide is a map lookup.
    val next = remember(channel.guideKey, now, clock.value / 60_000L) { LiveTvRepository.nextProgramme(channel.guideKey) }
    if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_MINIGUIDE) { com.nuvio.tv.skin.kicia.livetv.ForgeMiniGuideCard(channel, logo, now, next, clock.value, startOver, rewind, number, replay, playheadMs, scrubMs); return } // forge hook: MiniGuide (S09)
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .padding(start = 48.dp, end = 48.dp, bottom = 36.dp)
            .widthIn(max = 880.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(LiveTvCardBackground)
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveTvLogo(url = logo, name = channel.name, width = 104.dp, height = 64.dp)
        Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (number > 0) {
                    Text(
                        text = number.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.68f),
                        modifier = Modifier.padding(end = 10.dp),
                    )
                }
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                details.labels.forEach { label ->
                    LiveTvDetailChip(label, modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (now == null) {
                Text(
                    text = stringResource(R.string.live_tv_info_no_guide),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.live_tv_info_now).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black,
                        modifier = Modifier
                            .clip(LiveTvPillShape)
                            .background(Color.White.copy(alpha = 0.92f))
                            .padding(horizontal = 7.dp, vertical = 1.dp),
                    )
                    Text(
                        text = now.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    LiveTvProgressBar(
                        programme = now,
                        clock = clock,
                        fill = Color.White,
                        track = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.width(240.dp),
                    )
                    Text(
                        text = "${LiveTvClock.formatSpan(now)}  ·  ${liveTvTimeLeft(now, clock)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                next?.let {
                    Text(
                        text = stringResource(R.string.live_tv_info_next, LiveTvClock.formatClock(it.startEpochMs)) + "  " + it.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Text(
                text = stringResource(
                    when {
                        rewind && startOver -> R.string.live_tv_info_hint_rewind_start_over
                        rewind -> R.string.live_tv_info_hint_rewind
                        startOver -> R.string.live_tv_info_hint_start_over
                        else -> R.string.live_tv_info_hint
                    },
                ),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.62f),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/** A quiet outlined tag: "1080p", "50 fps". */
@Composable
private fun LiveTvDetailChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = Color.White.copy(alpha = 0.82f),
        maxLines = 1,
        modifier = modifier
            .border(1.dp, Color.White.copy(alpha = 0.24f), LiveTvPillShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun LiveTvChannelPanel(state: LiveTvPlayerState, programmes: Map<String, LiveTvProgramme>, clock: State<Long>) {
    val liveState by LiveTvRepository.uiState.collectAsStateWithLifecycle()
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .background(
                Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.92f),
                    0.85f to Color.Black.copy(alpha = 0.82f),
                    1f to Color.Black.copy(alpha = 0f),
                ),
            ),
    ) {
        AnimatedVisibility(
            visible = state.foldersOpen,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
        ) {
            LiveTvFolderColumn(state, liveState)
        }
        // ▶ on a channel: its programmes in place of the channels, ◀ back.
        AnimatedContent(
            targetState = state.programmesChannel,
            contentKey = { it?.streamUrl },
            transitionSpec = {
                val step = if (targetState != null) 1 else -1
                (fadeIn(tween(220)) + slideInHorizontally(tween(220)) { step * it / 10 }) togetherWith fadeOut(tween(120))
            },
            label = "panel",
        ) { channel ->
            if (channel == null) {
                LiveTvChannelColumn(state, programmes, liveState, clock)
            } else {
                LiveTvProgrammeColumn(state, channel, liveState, clock)
            }
        }
    }
}

/** The categories: focusing one lists its channels beside it (after a short rest, so passing over is cheap). */
@Composable
private fun LiveTvFolderColumn(state: LiveTvPlayerState, liveState: LiveTvUiState) {
    val allLabel = stringResource(R.string.live_tv_all_channels)
    val favoritesLabel = stringResource(R.string.live_tv_favorites)
    val uncategorisedLabel = liveTvGroupLabel(LIVE_TV_UNGROUPED)
    val showAll by LiveTvPreferences.showAll.collectAsStateWithLifecycle()
    val showFavorites by LiveTvPreferences.showFavorites.collectAsStateWithLifecycle()
    val folders = remember(liveState.sources, liveState.groups, liveState.hiddenGroups, liveState.groupNames, liveState.customLists, allLabel, favoritesLabel, uncategorisedLabel, showAll, showFavorites) {
        buildList {
            if (showAll) add(FILTER_ALL to allLabel)
            if (showFavorites) add(FILTER_FAVORITES to favoritesLabel)
            liveState.customLists.forEach { add(FILTER_LIST_PREFIX + it.id to it.name) }
            if (liveState.sources.size > 1) liveState.sources.forEach { add(FILTER_SOURCE_PREFIX + it.id to it.label) }
            liveState.visibleGroups.forEach {
                add(it to (liveTvGroupName(it, liveState.groupNames) ?: if (it == LIVE_TV_UNGROUPED) uncategorisedLabel else it))
            }
        }
    }
    // The panel opens on the zapped list's category (All channels for a search or one gone since).
    val zappedFolder = state.zappedFolderKey?.takeIf { key -> folders.any { it.first == key } } ?: FILTER_ALL
    val startKey = state.panelFolderKey ?: zappedFolder
    val startIndex = folders.indexOfFirst { it.first == startKey }.coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (startIndex - 3).coerceAtLeast(0))
    val startFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        repeat(10) {
            if (runCatching { startFocus.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(16)
        }
    }
    Column(modifier = Modifier.fillMaxHeight().width(250.dp).padding(start = 32.dp, top = 32.dp, end = 8.dp)) {
        Text(
            text = stringResource(R.string.live_tv_categories),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            itemsIndexed(folders, key = { _, folder -> folder.first }) { index, (key, label) ->
                FolderRow(
                    label = label,
                    selected = key == (state.panelFolderKey ?: startKey),
                    onFocused = { state.showFolder(key) },
                    onClick = { state.showFolder(key) },
                    modifier = if (index == startIndex) Modifier.focusRequester(startFocus) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun FolderRow(
    label: String,
    selected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(focused) {
        if (focused && !selected) {
            delay(250) // passing over a category does not re-filter
            onFocused()
        }
    }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        shape = CardDefaults.shape(RoundedCornerShape(10.dp)),
        colors = CardDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent,
            focusedContainerColor = Color.White,
        ),
        scale = CardDefaults.scale(focusedScale = 1.02f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (focused) Color.Black else if (selected) Color.White else Color.White.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun LiveTvChannelColumn(
    state: LiveTvPlayerState,
    programmes: Map<String, LiveTvProgramme>,
    liveState: LiveTvUiState,
    clock: State<Long>,
) {
    val channels = state.panelChannels
    // Back from a channel's programmes: on that channel; otherwise on the one playing.
    val startIndex = remember(channels) {
        val back = state.panelReturnUrl?.let { url -> channels.indexOfFirst { it.streamUrl == url } } ?: -1
        (if (back >= 0) back else channels.indexOfFirst { it.streamUrl == state.currentListUrl }).coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (startIndex - 3).coerceAtLeast(0))
    // A new category starts at its top (or at the channel playing, when it has it).
    LaunchedEffect(channels) {
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == startIndex }) {
            listState.scrollToItem((startIndex - 3).coerceAtLeast(0))
        }
    }
    val currentFocus = remember { FocusRequester() }
    // On opening, and when the categories close, focus goes to the channel playing (or the first).
    LaunchedEffect(state.foldersOpen) {
        if (state.foldersOpen) return@LaunchedEffect
        // The row must be composed before it can take focus.
        repeat(10) {
            if (runCatching { currentFocus.requestFocus() }.isSuccess) return@LaunchedEffect
            delay(16)
        }
    }
    val folderKey = state.panelFolderKey
    val folderLabel = when {
        folderKey == null -> null
        folderKey == FILTER_ALL -> stringResource(R.string.live_tv_all_channels)
        folderKey == FILTER_FAVORITES -> stringResource(R.string.live_tv_favorites)
        folderKey.startsWith(FILTER_LIST_PREFIX) -> liveState.customLists.firstOrNull { FILTER_LIST_PREFIX + it.id == folderKey }?.name
        folderKey.startsWith(FILTER_SOURCE_PREFIX) ->
            liveState.sources.firstOrNull { FILTER_SOURCE_PREFIX + it.id == folderKey }?.label
        // One source's category: its name, as the Live TV screen shows it under the source.
        folderKey.startsWith(FILTER_SOURCE_GROUP_PREFIX) -> (filterFor(folderKey) as LiveTvFilter.SourceGroup).let { group ->
            val source = liveState.sources.firstOrNull { it.id == group.id }?.label
            listOfNotNull(liveTvGroupLabel(group.name, liveState.groupNames), source).joinToString("  ·  ")
        }
        else -> folderKey
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(460.dp)
            .padding(start = if (state.foldersOpen) 8.dp else 32.dp, end = 40.dp, top = 32.dp),
    ) {
        Text(
            text = folderLabel ?: stringResource(R.string.live_tv_player_channels),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.live_tv_player_categories_hint),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = if (state.foldersOpen) 0f else 0.45f),
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            itemsIndexed(channels, key = { _, channel -> channel.id }, contentType = { _, _ -> "channel" }) { index, channel ->
                PanelRow(
                    channel = channel,
                    logo = liveState.logoFor(channel),
                    programme = programmes[channel.guideKey],
                    playing = channel.streamUrl == state.currentListUrl,
                    clock = clock,
                    onClick = { state.pickFromPanel(channel) },
                    onFocused = { state.panelFocusedUrl = channel.streamUrl },
                    modifier = if (index == startIndex) Modifier.focusRequester(currentFocus) else Modifier,
                )
            }
            if (channels.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(
                            if (folderKey == FILTER_FAVORITES) R.string.live_tv_no_favorites else R.string.live_tv_no_channels_found,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelRow(
    channel: LiveTvChannel,
    logo: String?,
    programme: LiveTvProgramme?,
    playing: Boolean,
    clock: State<Long>,
    onClick: () -> Unit,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Card(
        onClick = onClick,
        onLongClick = { LiveTvRepository.toggleFavorite(channel) },
        modifier = modifier.fillMaxWidth().onFocusChanged {
            focused = it.isFocused
            if (it.isFocused) onFocused()
        },
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.White),
        scale = CardDefaults.scale(focusedScale = 1.02f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveTvLogo(url = logo, name = channel.name, width = 60.dp, height = 38.dp)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (playing) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (focused) Color.Black else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                programme?.let {
                    Text(
                        text = it.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (focused) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(modifier = Modifier.padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        LiveTvProgressBar(
                            programme = it,
                            clock = clock,
                            fill = if (focused) Color.Black else Color.White,
                            track = if (focused) Color.Black.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.width(120.dp),
                        )
                        Text(
                            text = liveTvTimeLeft(it, clock),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (focused) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.5f),
                            maxLines = 1,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            if (playing) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (focused) Color.Black else Color(0xFFE50914)),
                )
            }
        }
    }
}

private const val BANNER_MS = 4_000L
