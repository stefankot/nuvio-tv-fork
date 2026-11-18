package com.nuvio.tv.skin.kicia.youtube

import android.view.KeyEvent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.nuvio.tv.youtube.YouTubePlayerPanel
import com.nuvio.tv.youtube.YouTubePlayerViewModel
import com.nuvio.tv.youtube.YtItem

/**
 * What the YouTube panel in the player needs from the app around it: the video it belongs to, and the two things
 * that leave the player (a channel page, another video). The host of the player route provides it for YouTube
 * videos only; everything else sees `null` and the player behaves as before.
 */
class ForgeYouTubeSession(
    val videoId: String,
    val openChannel: (channelId: String, name: String) -> Unit,
    val playVideo: (item: YtItem, related: List<YtItem>) -> Unit,
)

val LocalForgeYouTubeSession = compositionLocalOf<ForgeYouTubeSession?> { null }

/** The panel's open state and the key that opens it: Down while the controls are hidden. */
@Stable
class ForgeYouTubePanel internal constructor(internal val session: ForgeYouTubeSession?) {
    var open by mutableStateOf(false)
        internal set
    internal var scrollFraction by mutableFloatStateOf(0f)
    /** Opened with Down: recommendations first; opened from a button of the controls: the action list. */
    internal var startHome by mutableStateOf(true)

    val active: Boolean get() = session != null

    /** True when the key was used here (the panel opened). While the panel is open its own views take every key. */
    fun onPreviewKey(event: KeyEvent, controlsVisible: Boolean): Boolean {
        if (session == null || open) return false
        // test 15.11 (JMGO, B-3): ▼ from the bar (makieta 7.7b H3) never reached the bar — the player took it first
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN && (!controlsVisible || ForgeYouTubePanelRequests.barFocused)) {
            startHome = true
            open = true
            return true
        }
        return false
    }
}

@Composable
fun rememberForgeYouTubePanel(): ForgeYouTubePanel {
    val session = LocalForgeYouTubeSession.current
    return remember(session?.videoId) { ForgeYouTubePanel(session) }
}

/** The panel, drawn over the player when it is open. [onSeekTo] moves the video (chapters, comment timestamps). */
@Composable
fun ForgeYouTubePanelHost(panel: ForgeYouTubePanel, onSeekTo: (Long) -> Unit) {
    val session = panel.session ?: return
    val context = LocalContext.current
    val viewModel: YouTubePlayerViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    // What a button did (liked, subscribed, "sign in first"), whether or not the panel is open.
    LaunchedEffect(state.message) {
        state.message?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }
    // The controls' Opis / comments / save buttons open the panel on its action list (mockup S12).
    LaunchedEffect(panel) {
        ForgeYouTubePanelRequests.requests.collect { panel.startHome = false; panel.open = true }
    }
    if (!panel.open) return
    Box(Modifier.fillMaxSize()) {
        YouTubePlayerPanel(
            videoId = session.videoId,
            viewModel = viewModel,
            onClose = {
                panel.scrollFraction = 0f
                panel.open = false
            },
            onOpenChannel = session.openChannel,
            onPlayVideo = { item -> session.playVideo(item, viewModel.state.value.related) },
            onSeekTo = onSeekTo,
            startWithRecommendations = panel.startHome,
            scrollFraction = panel.scrollFraction,
            onScrollFractionChange = { panel.scrollFraction = it },
        )
    }
}

/** Asks the open player's YouTube panel to open on its action list (from the S12 controls). */
object ForgeYouTubePanelRequests {
    /** makieta 7.7b H3: the progress bar of the YouTube controls holds the focus (▼ there = the next videos). */
    @Volatile var barFocused: Boolean = false
    internal val requests = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    fun open() { requests.tryEmit(Unit) }
}
