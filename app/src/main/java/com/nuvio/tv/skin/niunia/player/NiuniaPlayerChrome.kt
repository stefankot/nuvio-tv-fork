package com.nuvio.tv.skin.niunia.player

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.skin.kicia.player.ForgeSheetOpen
import com.nuvio.tv.skin.kicia.player.forgeScrubKeys
import com.nuvio.tv.skin.kicia.player.formatScrubTime
import com.nuvio.tv.skin.kicia.player.rememberForgeScrub
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpace
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.PlayerViewModel

private const val TAG = "ForkNiunia"

/**
 * Nakładka odtwarzacza Niunii (canvas: Player): gradient u dołu, nad nim etykieta FILM/SERIAL, logo tytułu (albo tytuł),
 * linia „rok · S1:O2 · tytuł odcinka”, pasek postępu 8 dp z kółkiem 16 dp i czasami po bokach, pod nim przyciski:
 * okrągły ❚❚, Informacje, Dźwięk, Napisy, Szybkość, Więcej (szklany panel po prawej).
 * Klawisze paska = te same co w Kicii (◀▶ przewija, przytrzymanie przyspiesza, OK pauza), dane i akcje = [PlayerUiState] / [PlayerEvent].
 */
@Composable
fun NiuniaPlayerChrome(
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    playPauseFocusRequester: FocusRequester,
    progressBarFocusRequester: FocusRequester,
    onEvent: (PlayerEvent) -> Unit,
    onSubtitleClearanceChanged: (Float) -> Unit,
) {
    val context = LocalContext.current
    val timeline by viewModel.playbackTimeline.collectAsState()
    val resetHide = { viewModel.scheduleHideControls(); viewModel.onUserInteraction() }
    val scrub = rememberForgeScrub(positionMs = timeline.currentPosition, durationMs = timeline.duration, onSeek = { onEvent(PlayerEvent.OnSeekTo(it)); resetHide() })
    var barFocused by remember { mutableStateOf(false) }
    val firstButton = remember { FocusRequester() }
    var moreOpen by remember { mutableStateOf(false) }
    var controlsTopPx by remember { mutableFloatStateOf(0f) }
    var rootHeightPx by remember { mutableFloatStateOf(0f) }
    val series = uiState.currentSeason != null && uiState.currentEpisode != null
    // WPIECIE.md „Po wdrożeniu” pkt 4: ▼ z przycisków = rząd pod odtwarzaczem, kolejne ▼ = szczegóły; BACK cofa krok po kroku.
    var level by remember { mutableIntStateOf(0) }
    val episodes = niuniaSeasonEpisodes(uiState)
    val hasRow = series && episodes.isNotEmpty()
    val rowFocus = remember { FocusRequester() }
    var moved by remember { mutableStateOf(false) }
    fun go(to: Int) {
        moved = true
        Log.i(TAG, "odtwarzacz: poziom $level → $to (rząd=${if (hasRow) episodes.size else 0})")
        level = to
    }
    LaunchedEffect(level) {
        if (!moved) return@LaunchedEffect
        kotlinx.coroutines.delay(60)
        try {
            when (level) {
                0 -> firstButton.requestFocus()
                1 -> rowFocus.requestFocus()
            }
        } catch (e: IllegalStateException) { if (level != 2) Log.w(TAG, "odtwarzacz: fokus poziomu $level: ${e.message}") }
    }
    val controlsShift by animateDpAsState(if (level >= 1) -(NiuniaSpace.TileH + 22.dp + NiuniaSpace.AfterRow - 26.dp) else 0.dp, label = "niuniaControlsShift")
    val rowShift by animateDpAsState(if (level >= 1) 0.dp else NiuniaSpace.TileH + 22.dp + NiuniaSpace.AfterRow - NiuniaSpace.TilePeek, label = "niuniaRowShift")

    LaunchedEffect(controlsTopPx, rootHeightPx) {
        if (rootHeightPx > 0f && controlsTopPx > 0f) onSubtitleClearanceChanged((rootHeightPx - controlsTopPx).coerceAtLeast(0f))
    }
    // Fokus startuje na pasku (◀▶ zawsze przewija); PlayerScreen kładzie fokus na „❚❚” kilka klatek później, więc pytamy po nim.
    LaunchedEffect(Unit) {
        Log.i(TAG, "odtwarzacz: nakładka (serial=$series, logo=${uiState.logo != null})")
        kotlinx.coroutines.delay(250)
        var ok = false
        repeat(6) {
            if (!ok) {
                ok = try { progressBarFocusRequester.requestFocus(); true } catch (e: IllegalStateException) { false }
                if (!ok) kotlinx.coroutines.delay(80)
            }
        }
        if (!ok) Log.w(TAG, "odtwarzacz: pasek postępu nie dostał fokusu")
    }

    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { if (it.type == KeyEventType.KeyDown) resetHide(); false }
            .onGloballyPositioned { rootHeightPx = it.size.height.toFloat() },
    ) {
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(300.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xBF000000)))))
        Column(Modifier.align(Alignment.BottomStart).offset(y = controlsShift).padding(start = 40.dp, end = 40.dp, bottom = 46.dp).fillMaxWidth()) {
            // Tytuł (canvas: 40, 330).
            Text(stringResource(if (series) R.string.niunia_player_series else R.string.niunia_player_movie).uppercase(), style = NiuniaText.Caption.copy(fontSize = 12.sp, lineHeight = 16.sp), color = NiuniaColors.Text70)
            val title = if (series) uiState.contentName ?: uiState.title else uiState.title
            if (uiState.logo != null) {
                AsyncImage(model = uiState.logo, contentDescription = title, contentScale = ContentScale.Fit, alignment = Alignment.CenterStart,
                    modifier = Modifier.padding(top = 8.dp, bottom = 6.dp).width(300.dp).height(64.dp))
            } else {
                Text(title, style = NiuniaText.Logo.copy(fontSize = 32.sp, lineHeight = 38.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(520.dp))
            }
            val meta = listOfNotNull(
                uiState.releaseYear?.takeIf { it.isNotBlank() && !series },
                if (series) "S${uiState.currentSeason}:O${uiState.currentEpisode}" else null,
                uiState.currentEpisodeTitle?.takeIf { series && it.isNotBlank() },
                speedLabel(uiState.playbackSpeed).takeIf { uiState.playbackSpeed != 1f },
            ).joinToString(" · ")
            if (meta.isNotEmpty()) Text(meta, style = NiuniaText.Body.copy(fontSize = 13.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(20.dp).onGloballyPositioned { controlsTopPx = it.positionInRoot().y })
            // Pasek postępu (canvas: y 420, 8 dp, kółko 16 dp).
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(formatScrubTime(scrub.state.markerMs ?: scrub.state.positionMs), style = NiuniaText.Caption.copy(fontSize = 12.sp), color = NiuniaColors.Text)
                val duration = scrub.state.durationMs.coerceAtLeast(1L).toFloat()
                val played = (scrub.state.positionMs / duration).coerceIn(0f, 1f)
                val marker = scrub.state.markerMs?.let { (it / duration).coerceIn(0f, 1f) }
                Canvas(
                    Modifier
                        .weight(1f)
                        .height(16.dp)
                        .focusRequester(progressBarFocusRequester)
                        .focusRequester(playPauseFocusRequester)
                        .onFocusChanged { barFocused = it.isFocused; if (it.isFocused) resetHide() }
                        .forgeScrubKeys(scrub, onTogglePause = { onEvent(PlayerEvent.OnPlayPause); resetHide() },
                            onUp = { false },
                            onDown = { try { firstButton.requestFocus(); true } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: ▼ z paska: ${e.message}"); false } })
                        .focusable(),
                ) {
                    val h = (if (barFocused) 8.dp else 6.dp).toPx()
                    val y = (size.height - h) / 2f
                    drawRoundRect(Color.White.copy(alpha = 0.24f), Offset(0f, y), Size(size.width, h), CornerRadius(h / 2f))
                    drawRoundRect(Color.White, Offset(0f, y), Size(size.width * played, h), CornerRadius(h / 2f))
                    val knob = (marker ?: played) * size.width
                    if (barFocused) drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(knob, size.height / 2f))
                }
                Text("−" + formatScrubTime((scrub.state.durationMs - scrub.state.positionMs).coerceAtLeast(0)), style = NiuniaText.Caption.copy(fontSize = 12.sp), color = NiuniaColors.Text70)
            }
            // Przyciski (canvas: y 454, odstęp 12).
            Row(
                Modifier.padding(top = NiuniaSpace.PlayerGap).onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown) { go(if (hasRow) 1 else 2); true } else false
                },
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallButton(if (uiState.isPlaying) "❚❚" else "▶", round = true, focusRequester = firstButton) { onEvent(PlayerEvent.OnPlayPause); resetHide() }
                SmallButton(stringResource(R.string.niunia_player_info)) { onEvent(PlayerEvent.OnShowStreamInfo); resetHide() }
                SmallButton(stringResource(R.string.niunia_player_audio)) { onEvent(PlayerEvent.OnShowAudioOverlay); resetHide() }
                SmallButton(stringResource(R.string.niunia_player_subtitles)) { onEvent(PlayerEvent.OnShowSubtitleOverlay); resetHide() }
                if (series) SmallButton(stringResource(R.string.niunia_player_episodes)) { onEvent(PlayerEvent.OnShowEpisodesPanel); resetHide() }
                SmallButton(stringResource(R.string.niunia_player_speed, speedLabel(uiState.playbackSpeed))) { onEvent(PlayerEvent.OnShowSpeedDialog); resetHide() }
                SmallButton(stringResource(R.string.niunia_player_more)) { moreOpen = true; Log.i(TAG, "odtwarzacz: panel Więcej"); resetHide() }
            }
        }

        // „Więcej” — szklany panel po prawej (canvas .glass: 360 szer., od y 40, promień 28 po lewej).
        // Rząd odcinków: wystaje u dołu, ▼ go podnosi (4 kafle 16:9 i kawałek piątego).
        if (hasRow) {
            NiuniaPlayerRow(
                uiState, episodes, focus = rowFocus, onEvent = { onEvent(it); resetHide() },
                onUp = { go(0); true }, onDown = { go(2); true },
                modifier = Modifier.align(Alignment.BottomStart).offset(y = rowShift).padding(bottom = NiuniaSpace.AfterRow),
            )
        }
        if (level == 2) NiuniaPlayerDetails(uiState, series, onEvent = { onEvent(it); resetHide() })
        BackHandler(enabled = level > 0 && !moreOpen) { go(if (level == 2 && hasRow) 1 else 0) }

        SideEffect { ForgeSheetOpen.open = moreOpen || level > 0 }
        DisposableEffect(Unit) { onDispose { ForgeSheetOpen.open = false } }
        LaunchedEffect(moreOpen, level) { while (moreOpen || level > 0) { resetHide(); kotlinx.coroutines.delay(2_000) } }
        AnimatedVisibility(moreOpen, modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp), enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
            BackHandler { moreOpen = false }
            MorePanel(series = series, speed = uiState.playbackSpeed, close = { moreOpen = false }, onEvent = onEvent, toast = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() })
        }
    }
}

@Composable
private fun MorePanel(series: Boolean, speed: Float, close: () -> Unit, onEvent: (PlayerEvent) -> Unit, toast: (String) -> Unit) {
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: fokus panelu: ${e.message}") } }
    val shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    Column(
        Modifier
            .width(360.dp)
            .height(400.dp)
            .clip(shape)
            .background(Color(0xE61E1E22))
            .border(1.dp, NiuniaColors.Line, shape)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.niunia_player_more), style = NiuniaText.Button.copy(fontSize = 20.sp, lineHeight = 26.sp), color = NiuniaColors.Text, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
        PanelRow(stringResource(R.string.niunia_player_sources), focusRequester = first) { close(); onEvent(PlayerEvent.OnShowSourcesPanel) }
        PanelRow(stringResource(R.string.niunia_player_quality)) { close(); onEvent(PlayerEvent.OnShowQualityOverlay) }
        PanelRow(stringResource(R.string.niunia_player_try_other)) { close(); toast(com.nuvio.tv.fork.sources.TryAnotherSource.run()) }
        PanelRow(stringResource(R.string.niunia_player_subs_mismatch)) {
            close(); toast(com.nuvio.tv.fork.sources.PlayerBridge.subtitleMismatch() ?: ctx.getString(R.string.niunia_player_no_subs))
        }
        if (com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(ctx, "module_lektor")) {
            val state = if (com.nuvio.tv.fork.lektor.LektorEngine.enabled) R.string.niunia_player_on else R.string.niunia_player_off
            PanelRow(stringResource(R.string.niunia_player_lektor), detail = stringResource(state)) { close(); toast(com.nuvio.tv.fork.lektor.LektorController.toggle(ctx)) }
            // Fork 20.11: regulacja lektora w trakcie filmu (OK = następna wartość, panel zostaje otwarty).
            var lektorRev by remember { mutableStateOf(0) }
            val m = com.nuvio.tv.fork.lektor.LektorModule
            PanelRow(stringResource(R.string.lektor_volume_row), detail = lektorRev.let { m.volumeLabel(ctx) }) { m.cycleVolume(ctx); lektorRev++ }
            PanelRow(stringResource(R.string.lektor_duck_row), detail = lektorRev.let { m.duckLabel(ctx) }) { m.cycleDuck(ctx); lektorRev++ }
        }
        PanelRow(stringResource(R.string.niunia_player_net_test), detail = com.nuvio.tv.fork.net.KeNetwork.status.ifEmpty { null }) {
            close()
            val url = com.nuvio.tv.fork.sources.PlayerBridge.streamUrl()
            if (url == null) toast(ctx.getString(R.string.niunia_player_net_test_idle))
            else {
                toast(ctx.getString(R.string.niunia_player_net_test_start))
                com.nuvio.tv.fork.net.KeNetwork.test(ctx, url, emptyMap()) { msg -> Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show() }
            }
        }
        if (series) PanelRow(stringResource(R.string.niunia_player_episodes)) { close(); onEvent(PlayerEvent.OnShowEpisodesPanel) }
        PanelRow(stringResource(R.string.niunia_player_speed, speedLabel(speed))) { close(); onEvent(PlayerEvent.OnShowSpeedDialog) }
    }
}

/** Wiersz panelu (canvas .row: 44 wys., promień 12; fokus = białe tło, ciemny tekst). */
@Composable
private fun PanelRow(text: String, detail: String? = null, focusRequester: FocusRequester? = null, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .niuniaFocusable(shape, scale = 1f, lift = false, focusRequester = focusRequester, onFocus = { focused = it }, onOk = onOk)
            .background(if (focused) NiuniaColors.Focus else Color.Transparent, shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = NiuniaText.Meta, color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (detail != null) Text(detail, style = NiuniaText.Body, color = if (focused) NiuniaColors.OnFocus.copy(alpha = 0.7f) else NiuniaColors.Text50, maxLines = 1)
    }
}

/** Przycisk mały (canvas .btn.sec.sm: 34 wys.; .rnd 40×40). */
@Composable
private fun SmallButton(text: String, round: Boolean = false, focusRequester: FocusRequester? = null, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = if (round) CircleShape else RoundedCornerShape(50)
    Box(
        (if (round) Modifier.size(40.dp) else Modifier.height(34.dp).padding(horizontal = 0.dp))
            .niuniaFocusable(shape, scale = 1.06f, lift = false, focusRequester = focusRequester, onFocus = { focused = it }, onOk = onOk)
            .background(if (focused) NiuniaColors.Focus else NiuniaColors.Btn, shape)
            .padding(horizontal = if (round) 0.dp else 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = NiuniaText.Button.copy(fontSize = 14.sp, lineHeight = 20.sp), color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text, maxLines = 1)
    }
}

private fun speedLabel(speed: Float): String =
    ("%.2f".format(java.util.Locale("pl"), speed)).trimEnd('0').trimEnd(',') + "×"
