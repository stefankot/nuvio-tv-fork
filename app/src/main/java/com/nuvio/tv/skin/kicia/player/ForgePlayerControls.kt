package com.nuvio.tv.skin.kicia.player

import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.MoreHoriz
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.components.ForgeIconButton
import com.nuvio.tv.skin.kicia.components.ForgeSheetRow
import com.nuvio.tv.skin.kicia.components.ForgeSideSheet
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.PlayerViewModel

/**
 * S04 player controls of the mockup, drawn in place of Reshaped's overlay (same state and actions):
 * title / "S2:O4 · title" on the left, clock and "Koniec o HH:MM" on the right, the ScrubBar across
 * (tap ±10 s, hold speeds up, ↑ preview marker, OK pause/confirm), times under it and a centred row:
 * large play/pause, Napisy, Dźwięk, "…" (Źródła, Odcinki, Informacje, Szybkość). Names under icons on focus.
 */
@Composable
fun ForgePlayerControls(
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    playPauseFocusRequester: FocusRequester,
    progressBarFocusRequester: FocusRequester,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onShowSubtitleDialog: () -> Unit,
    onShowAudioDialog: () -> Unit,
    onShowSourcesPanel: () -> Unit,
    onShowEpisodesPanel: () -> Unit,
    onShowStreamInfo: () -> Unit,
    onShowSpeedDialog: () -> Unit,
    onResetHideTimer: () -> Unit,
    onSubtitleClearanceChanged: (Float) -> Unit,
) {
    // S12: YouTube videos get the YouTube controls of the mockup.
    com.nuvio.tv.skin.kicia.youtube.LocalForgeYouTubeSession.current?.let { session ->
        ForgeYouTubeControls(session, uiState, viewModel, playPauseFocusRequester, progressBarFocusRequester, onPlayPause, onSeekTo,
            onShowSubtitleDialog, onShowSpeedDialog, onShowStreamInfo, onResetHideTimer, onSubtitleClearanceChanged)
        return
    }
    val timeline by viewModel.playbackTimeline.collectAsState()
    val scrub = rememberForgeScrub(
        positionMs = timeline.currentPosition,
        durationMs = timeline.duration,
        onSeek = { onSeekTo(it); onResetHideTimer() },
    )
    var barFocused by remember { mutableStateOf(false) }
    val ccFocus = remember { androidx.compose.ui.focus.FocusRequester() } // forge hook v7.5: ▲ from the bar (ZACHOWANIE 5.8)
    val firstButtonFocus = remember { androidx.compose.ui.focus.FocusRequester() } // ▼ from the bar (5.9)
    // forge fix 14.11 (ZACHOWANIE 5.13): Back from a panel opened from the row comes back to its button (it landed on the bar)
    val rowFocus = remember { listOf(firstButtonFocus) + List(3) { androidx.compose.ui.focus.FocusRequester() } }
    val panelOpen = uiState.showEpisodesPanel || uiState.showSourcesPanel || uiState.showSpeedDialog || uiState.showStreamInfoOverlay
    LaunchedEffect(panelOpen, uiState.showControls) {
        val index = ForgeControlsReturn.index
        if (panelOpen || !uiState.showControls || index < 0) return@LaunchedEffect
        kotlinx.coroutines.delay(320) // after PlayerScreen's own 250 ms request for the bar
        ForgeControlsReturn.index = -1
        runCatching { rowFocus[index].requestFocus() }
    }
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

    val series = uiState.currentSeason != null && uiState.currentEpisode != null
    val title = if (series) uiState.contentName ?: uiState.title else uiState.title
    val subtitle = forgePlayerSubtitle(uiState.releaseYear, uiState.currentSeason, uiState.currentEpisode, uiState.currentEpisodeTitle)
    val endsAt = forgeEndsAt(System.currentTimeMillis(), timeline.duration - timeline.currentPosition, uiState.playbackSpeed)

    // Any key keeps the controls up (they closed on their own while walking between buttons).
    Box(Modifier.onPreviewKeyEvent { if (it.type == KeyEventType.KeyDown) onResetHideTimer(); false }.onGloballyPositioned { rootHeightPx = it.size.height.toFloat() }) {
        ForgePlayerScaffold(
            visible = true, title = title, subtitle = subtitle, endsAt = endsAt,
            endActions = { // v7.4 (05): Napisy i Dźwięk jako Przyciski okrągłe 36 przy tytule
                ForgeSubtitleButton(modifier = Modifier.focusRequester(ccFocus), onOk = onShowSubtitleDialog) // owner 9.11: CC doubles as the AutoSync state (dots / check / "!")
                ForgeIconButton(icon = Icons.Outlined.Headphones, contentDescription = "Dźwięk", size = 36.dp, onOk = onShowAudioDialog)
            },
        ) {
            Box(Modifier.onGloballyPositioned { controlsTopPx = it.positionInRoot().y })
            ForgeScrubBar(
                state = scrub.state,
                focused = barFocused,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp) // v7.4: Pasek postępu y 412, 16 wysokości
                    .testTag("postep.0")
                    .focusRequester(progressBarFocusRequester)
                    .focusRequester(playPauseFocusRequester) // v7.4: no ❚❚ in the row; OK on the bar pauses (5.5), the player's "focus ❚❚" lands here
                    .onFocusChanged { barFocused = it.isFocused; if (it.isFocused) onResetHideTimer() }
                    .forgeScrubKeys(scrub, onTogglePause = { onPlayPause(); onResetHideTimer() },
                        onUp = { runCatching { ccFocus.requestFocus() }.isSuccess },
                        onDown = { runCatching { firstButtonFocus.requestFocus() }.isSuccess })
                    .focusable(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, start = 24.dp)) { // v7.4: czasy 12, y 432; bieżący od x 64
                Text(forgeTime(scrub.state.markerMs ?: scrub.state.positionMs), style = ForgeText.caption, modifier = Modifier.testTag("odtw-czas.0"))
                Spacer(Modifier.weight(1f))
                Text("−" + forgeTime((scrub.state.durationMs - scrub.state.positionMs).coerceAtLeast(0)), style = ForgeText.caption, modifier = Modifier.testTag("odtw-czas.1"))
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { // v7.4 (05): Przyciski małe z ikoną, y 460
                com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Informacje", icon = Icons.Outlined.Info, small = true, focusRequester = firstButtonFocus, onOk = { ForgeControlsReturn.index = 0; onShowStreamInfo(); onResetHideTimer() })
                if (series) com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Odcinki", icon = Icons.Outlined.VideoLibrary, small = true, focusRequester = rowFocus[1], onOk = { ForgeControlsReturn.index = 1; onShowEpisodesPanel(); onResetHideTimer() })
                // Nowy fork (właściciel 18.11): wybór tylko rozdzielczości jak w Echo (lista jakości), kodek i rozmiar automatycznie;
                // długie OK = pełna lista źródeł. Presety Kicii (Oszczędny/Zbalansowany/Najlepsza) usunięte.
                com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Jakość", icon = Icons.Outlined.Layers, small = true, focusRequester = rowFocus[2],
                    onLongOk = { ForgeControlsReturn.index = 2; onShowSourcesPanel(); onResetHideTimer() },
                    onOk = { ForgeControlsReturn.index = 2; viewModel.onEvent(com.nuvio.tv.ui.screens.player.PlayerEvent.OnShowQualityOverlay); onResetHideTimer() })
                com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Szybkość", icon = Icons.Outlined.Speed, small = true, focusRequester = rowFocus[3], onOk = { ForgeControlsReturn.index = 3; onShowSpeedDialog(); onResetHideTimer() })
            }
        }
        ForgeSideSheet(visible = moreOpen, onDismiss = { moreOpen = false }, title = "Więcej") {
            ForgeSheetRow(text = "Źródła") { moreOpen = false; onShowSourcesPanel() }
            val ctx = androidx.compose.ui.platform.LocalContext.current
            // Kicia „Spróbuj innego źródła” (fork/sources/TryAnotherSource): następne z rankingu, obecne odłożone na 30 min.
            ForgeSheetRow(text = "Spróbuj innego źródła") {
                moreOpen = false
                android.widget.Toast.makeText(ctx, com.nuvio.tv.fork.sources.TryAnotherSource.run(), android.widget.Toast.LENGTH_SHORT).show()
            }
            // Audyt UI p. 2: wejścia do modułów forka, które wcześniej miały tylko głos.
            ForgeSheetRow(text = "Napisy nie pasują") {
                moreOpen = false
                android.widget.Toast.makeText(ctx, com.nuvio.tv.fork.sources.PlayerBridge.subtitleMismatch() ?: "Brak napisów", android.widget.Toast.LENGTH_SHORT).show()
            }
            if (com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(ctx, "module_lektor")) ForgeSheetRow(text = "Lektor (czyta polskie napisy)", detail = if (com.nuvio.tv.fork.lektor.LektorEngine.enabled) "Włączony" else "Wyłączony") {
                moreOpen = false
                android.widget.Toast.makeText(ctx, com.nuvio.tv.fork.lektor.LektorController.toggle(ctx), android.widget.Toast.LENGTH_SHORT).show()
            }
            if (series) ForgeSheetRow(text = "Odcinki") { moreOpen = false; onShowEpisodesPanel() }
            ForgeSheetRow(text = "Informacje") { moreOpen = false; onShowStreamInfo() }
            ForgeSheetRow(text = "Szybkość", detail = forgeSpeed(uiState.playbackSpeed)) { moreOpen = false; onShowSpeedDialog() }
        }
    }
}

/** forge fix 14.11: the row button a panel was opened from (-1 = none), kept outside the composition the panel may replace. */
internal object ForgeControlsReturn { @Volatile var index: Int = -1 }

/** "2024" for a film; "S2:O4 · Woe's Hollow" for an episode. */
internal fun forgePlayerSubtitle(year: String?, season: Int?, episode: Int?, episodeTitle: String?): String? =
    if (season != null && episode != null) {
        listOfNotNull("S$season:O$episode", episodeTitle?.takeIf { it.isNotBlank() }).joinToString(" · ")
    } else year?.takeIf { it.isNotBlank() }

/** "Koniec o 22:49" from the time left (adjusted for the playback speed); null when the length is unknown. */
internal fun forgeEndsAt(nowMs: Long, leftMs: Long, speed: Float = 1f, zone: java.util.TimeZone = java.util.TimeZone.getDefault()): String? {
    if (leftMs <= 0) return null
    val end = nowMs + (leftMs / speed.coerceAtLeast(0.25f)).toLong()
    val cal = java.util.Calendar.getInstance(zone).apply { timeInMillis = end }
    return "Koniec o %02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
}

/** 1:03:08 / 42:51 */
internal fun forgeTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

private fun forgeSpeed(speed: Float): String = if (speed == 1f) "Normalna" else "×" + ("%.2f".format(java.util.Locale("pl"), speed)).trimEnd('0').trimEnd(',')
