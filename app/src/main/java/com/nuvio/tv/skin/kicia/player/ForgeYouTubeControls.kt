package com.nuvio.tv.skin.kicia.player

import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.nuvio.tv.skin.kicia.components.ForgeButton
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.components.ForgeIconButton
import com.nuvio.tv.skin.kicia.components.ForgeSheetRow
import com.nuvio.tv.skin.kicia.components.ForgeSideSheet
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.skin.kicia.youtube.ForgeYouTubePanelRequests
import com.nuvio.tv.skin.kicia.youtube.ForgeYouTubeSession
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.PlayerViewModel
import com.nuvio.tv.youtube.YouTubePlayerViewModel

/**
 * S12 YouTube player controls of the mockup (c08), for YouTube videos in place of the film controls: title and
 * channel at the top, the clock; "4:56 · chapter" and the length under the bar; one row with the channel
 * [avatar][Opis][Subskrybuj], in the middle [⏮][❚❚][⏭] (chapters; ⏭ at the last one plays the next video) and on
 * the right [👍][👎][💬][🔖][CC][⚙]. Same player state and actions; the YouTube panel does comments and saving.
 */
@Composable
internal fun ForgeYouTubeControls(
    session: ForgeYouTubeSession,
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    playPauseFocusRequester: FocusRequester,
    progressBarFocusRequester: FocusRequester,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onShowSubtitleDialog: () -> Unit,
    onShowSpeedDialog: () -> Unit,
    onShowStreamInfo: () -> Unit,
    onResetHideTimer: () -> Unit,
    onSubtitleClearanceChanged: (Float) -> Unit,
) {
    val yt: YouTubePlayerViewModel = hiltViewModel()
    val ytState by yt.state.collectAsState()
    LaunchedEffect(session.videoId) { yt.load(session.videoId) }
    val info = ytState.info
    val timeline by viewModel.playbackTimeline.collectAsState()
    val scrub = rememberForgeScrub(positionMs = timeline.currentPosition, durationMs = timeline.duration, onSeek = { onSeekTo(it); onResetHideTimer() })
    var barFocused by remember { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    var controlsTopPx by remember { mutableFloatStateOf(0f) }
    var rootHeightPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(controlsTopPx, rootHeightPx) {
        if (rootHeightPx > 0f && controlsTopPx > 0f) onSubtitleClearanceChanged((rootHeightPx - controlsTopPx).coerceAtLeast(0f))
    }
    // Owner 9.11: ◀▶ must always seek, so the focus starts on the bar (▼ goes to the buttons, ▲ back).
    LaunchedEffect(Unit) {
        // PlayerScreen puts the focus on ❚❚ when the controls appear (a few frames later), so ask after it.
        kotlinx.coroutines.delay(250)
        var ok = false
        repeat(6) { if (!ok) { ok = runCatching { progressBarFocusRequester.requestFocus() }.isSuccess; if (!ok) kotlinx.coroutines.delay(80) } }
    }
    val chapters = info?.chapters.orEmpty()
    val likeFocus = remember { FocusRequester() } // makieta 7.7b H3
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { ForgeYouTubePanelRequests.barFocused = false } } // test 15.11 (B-3)
    val position = scrub.state.markerMs ?: scrub.state.positionMs
    val chapter = forgeChapterAt(chapters, position)

    // Any key keeps the controls up (they closed on their own while walking between buttons).
    Box(Modifier.onPreviewKeyEvent { if (it.type == KeyEventType.KeyDown) onResetHideTimer(); false }.onGloballyPositioned { rootHeightPx = it.size.height.toFloat() }) {
        ForgePlayerScaffold(
            visible = true,
            title = info?.title ?: uiState.title,
            subtitle = listOfNotNull(info?.author, info?.subscribers?.takeIf { it.isNotBlank() }).joinToString(" · ").ifBlank { null },
            endsAt = null,
            endActions = {}, // makieta 7.7b H3: Napisy and the rest moved to the one row above the bar
        ) {
            Box(Modifier.onGloballyPositioned { controlsTopPx = it.positionInRoot().y })
            // makieta 7.7b H3: ALL buttons above the bar in one row under the title (gap 12, from x 40): Lubię to, Do kanału
            // (Awatar 44), Napisy, Jakość, Więcej. No ❚❚ and no „Do początku” (OK on the bar pauses; long OK there = from the start).
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                com.nuvio.tv.skin.kicia.components.ForgeButton(
                    text = "Lubię to" + (info?.likeCount?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                    icon = if (info?.liked == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, small = true, focusRequester = likeFocus,
                    onOk = { yt.toggleLike(); onResetHideTimer() },
                )
                val channelId = info?.channelId
                if (channelId != null) com.nuvio.tv.skin.kicia.components.ForgeFocusBox(
                    radius = 22.dp, onOk = { session.openChannel(channelId, info?.author.orEmpty()) },
                ) { _ -> com.nuvio.tv.skin.kicia.components.ForgeAvatar(name = info?.author.orEmpty(), size = 44.dp, imageUrl = info?.authorImage) }
                ForgeIconButton(icon = Icons.Outlined.ClosedCaption, contentDescription = "Napisy", size = 36.dp, onOk = onShowSubtitleDialog)
                com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Jakość", icon = Icons.Outlined.Settings, small = true, onOk = { moreOpen = true; onResetHideTimer() })
                com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Więcej", icon = Icons.Outlined.MoreHoriz, small = true, onOk = { ForgeYouTubePanelRequests.open(); onResetHideTimer() })
            }
            ForgeScrubBar(
                state = scrub.state,
                focused = barFocused,
                modifier = Modifier.fillMaxWidth().height(16.dp).testTag("postep.0") // v7.4: y 412, 16
                    .focusRequester(progressBarFocusRequester)
                    .focusRequester(playPauseFocusRequester) // v7.4: OK on the bar pauses; no ❚❚ in the row
                    .onFocusChanged { barFocused = it.isFocused; ForgeYouTubePanelRequests.barFocused = it.isFocused; if (it.isFocused) onResetHideTimer() }
                    .forgeScrubKeys(scrub, onTogglePause = { onPlayPause(); onResetHideTimer() },
                        onUp = { runCatching { likeFocus.requestFocus() }.isSuccess }, // makieta 7.7b H3: ▲ from the bar = the button row
                        onDown = { ForgeYouTubePanelRequests.open(); true }) // makieta 7.7b H3: ▼ from the bar = the next videos
                    .focusable(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, start = 24.dp)) { // v7.4: czasy 12, y 432
                Text(forgeTime(position) + (chapter?.let { " · $it" } ?: ""), style = ForgeText.caption, maxLines = 1, modifier = Modifier.testTag("odtw-czas.0"))
                Spacer(Modifier.weight(1f))
                Text(if (info?.isLive == true) "Na żywo" else "−" + forgeTime((scrub.state.durationMs - scrub.state.positionMs).coerceAtLeast(0)), style = ForgeText.caption, modifier = Modifier.testTag("odtw-czas.1"))
            }
        }
        ForgeSideSheet(visible = moreOpen, onDismiss = { moreOpen = false }, title = "Jakość") {
            ForgeSheetRow(text = "Szybkość") { moreOpen = false; onShowSpeedDialog() }
            ForgeSheetRow(text = "Informacje") { moreOpen = false; onShowStreamInfo() }
        }
    }
}

/** The chapter playing at [ms] (chapters as title to start), or null. */
internal fun forgeChapterAt(chapters: List<Pair<String, Long>>, ms: Long): String? =
    chapters.lastOrNull { it.second <= ms }?.first

/** ⏮: the start of this chapter, or of the previous one within its first 3 s; 0 without chapters. */
internal fun forgePrevChapterStart(chapters: List<Pair<String, Long>>, ms: Long): Long {
    val starts = chapters.map { it.second }.sorted()
    val current = starts.lastOrNull { it <= ms } ?: return 0L
    return if (ms - current > 3_000) current else starts.lastOrNull { it < current } ?: 0L
}

/** ⏭: the next chapter's start, or null at the last one (then the next video plays). */
internal fun forgeNextChapterStart(chapters: List<Pair<String, Long>>, ms: Long): Long? =
    chapters.map { it.second }.sorted().firstOrNull { it > ms + 500 }
