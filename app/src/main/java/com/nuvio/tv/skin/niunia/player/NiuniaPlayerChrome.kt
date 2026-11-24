@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.nuvio.tv.skin.niunia.player

import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaButtonKind
import com.nuvio.tv.skin.niunia.components.NiuniaCollection
import com.nuvio.tv.skin.niunia.components.NiuniaCollectionKind
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaPanelOverlay
import com.nuvio.tv.skin.niunia.components.NiuniaPlayerInfoBlock
import com.nuvio.tv.skin.niunia.components.NiuniaSeekBar
import com.nuvio.tv.skin.niunia.components.NiuniaSeekState
import com.nuvio.tv.skin.niunia.components.NiuniaSurface
import com.nuvio.tv.skin.niunia.components.NiuniaSurfaceKind
import com.nuvio.tv.skin.niunia.components.NiuniaTrailing
import com.nuvio.tv.skin.niunia.components.niuniaTrapArrows
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import kotlinx.coroutines.launch

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
import androidx.compose.ui.focus.focusProperties
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
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.PlayerViewModel
import com.nuvio.tv.ui.screens.player.loadEpisodesIfNeeded

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
    // Paczka „oczywiste” pkt 1: archiwum IPTV — panel podaje całe nagranie (24 h), długość = program z EPG (okno odtwarzania start–koniec).
    val archiveLength = niuniaArchiveLengthMs(viewModel.controller.currentStreamUrl, timeline.duration)
    val scrub = rememberForgeScrub(positionMs = timeline.currentPosition, durationMs = archiveLength ?: timeline.duration, onSeek = { onEvent(PlayerEvent.OnSeekTo(it)); resetHide() })
    var barFocused by remember { mutableStateOf(false) }
    val firstButton = remember { FocusRequester() }
    var moreOpen by remember { mutableStateOf(false) }
    var panelOpen by remember { mutableStateOf(false) }
    var panelTab by remember { mutableIntStateOf(-1) }
    val audioButton = remember { FocusRequester() }
    val lektorButton = remember { FocusRequester() }
    val moreButton = remember { FocusRequester() }
    val lektorOn = remember { com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(context, "module_lektor") }
    // Po zamknięciu panelu fokus wraca na „Dźwięk” (element, z którego wszedł) — bez tego fokus spadał na korzeń i nakładka znikała.
    var panelWasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(panelOpen) {
        if (panelOpen) { panelWasOpen = true; return@LaunchedEffect }
        if (!panelWasOpen) return@LaunchedEffect
        kotlinx.coroutines.delay(60)
        // Test JMGO build155: „Więcej opcji” zamyka panel i otwiera „Więcej” — fokus zostaje w nowym panelu (był zabierany na „Napisy”).
        if (moreOpen) { panelWasOpen = false; Log.i(TAG, "odtwarzacz: panel → Więcej, fokus w panelu Więcej"); return@LaunchedEffect }
        try { (when (panelTab) { 2 -> audioButton; 1 -> if (lektorOn) lektorButton else firstButton; else -> firstButton }).requestFocus(); Log.i(TAG, "odtwarzacz: panel zamknięty, fokus → ${when (panelTab) { 2 -> "Napisy"; 1 -> "Lektor"; else -> "Dźwięk" }}") } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: fokus po panelu: ${e.javaClass.simpleName}: ${e.message}") }
        // Test JMGO ke2: pierwsza prośba ginie, bo panel (exit = Cancel) jeszcze znika — druga po animacji wyjścia.
        kotlinx.coroutines.delay(450)
        if (!panelOpen && !moreOpen) try { (when (panelTab) { 2 -> audioButton; 1 -> if (lektorOn) lektorButton else firstButton; else -> firstButton }).requestFocus(); Log.i(TAG, "odtwarzacz: fokus po zamknięciu panelu (ponowienie)") } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: fokus po panelu (ponowienie): ${e.javaClass.simpleName}: ${e.message}") }
    }
    // Z-14 (PlayerMore): po zamknięciu panelu Więcej fokus wraca na przycisk Więcej.
    var moreWasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(moreOpen) {
        if (moreOpen) { moreWasOpen = true; return@LaunchedEffect }
        if (!moreWasOpen || panelOpen) return@LaunchedEffect
        moreWasOpen = false
        kotlinx.coroutines.delay(60)
        try { moreButton.requestFocus(); Log.i(TAG, "odtwarzacz: Więcej zamknięte, fokus → Więcej") } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: fokus po Więcej: ${e.javaClass.simpleName}: ${e.message}") }
        kotlinx.coroutines.delay(450)
        if (!moreOpen && !panelOpen) try { moreButton.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: fokus po Więcej (ponowienie): ${e.javaClass.simpleName}: ${e.message}") }
    }
    var okDownAt by remember { mutableStateOf(0L) }
    var okLong by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var controlsTopPx by remember { mutableFloatStateOf(0f) }
    var rootHeightPx by remember { mutableFloatStateOf(0f) }
    val series = uiState.currentSeason != null && uiState.currentEpisode != null
    // WPIECIE.md „Po wdrożeniu” pkt 4: ▼ z przycisków = rząd pod odtwarzaczem, kolejne ▼ = szczegóły; BACK cofa krok po kroku.
    var level by remember { mutableIntStateOf(0) }
    val episodes = niuniaSeasonEpisodes(uiState)
    // Test JMGO build155 (PlayerSeries): baza wczytuje odcinki dopiero po otwarciu swojego panelu odcinków, więc rząd Niuni był pusty
    // i ▼ skakało od razu do szczegółów — nakładka serialu prosi o odcinki sama (to samo wczytanie co panel bazy).
    LaunchedEffect(viewModel.controller.contentId, series) {
        if (series && uiState.episodesAll.isEmpty() && !uiState.isLoadingEpisodes) {
            Log.i(TAG, "odtwarzacz: wczytuję odcinki serialu (S${uiState.currentSeason})")
            viewModel.controller.loadEpisodesIfNeeded()
        }
    }
    LaunchedEffect(uiState.episodesError) { uiState.episodesError?.let { Log.w(TAG, "odtwarzacz: odcinki: $it") } }
    // Pkt 3 (20.11): film — ▼ podnosi rząd „Podobne”, dane z TMDB (TVDB nie ma podobnych), biblioteka i zwiastun.
    val extras: NiuniaPlayerExtrasViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val extrasState by extras.state.collectAsState()
    LaunchedEffect(viewModel.controller.contentId) {
        extras.load(viewModel.controller.contentId, viewModel.controller.contentType ?: if (series) "series" else "movie",
            uiState.contentName ?: uiState.title, uiState.releaseYear, null, uiState.backdrop, uiState.logo, uiState.description)
    }
    LaunchedEffect(extrasState.message) { extrasState.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); extras.clearMessage() } }
    val similar = extrasState.similar
    // Z-22 (P-34): plik z rozdziałami (≥ 2) → pod paskiem rząd „Rozdziały” zamiast „Podobne” (film); bez rozdziałów zostaje „Podobne”.
    val chapters = if (uiState.chaptersEnabled) uiState.chapters else emptyList()
    val chapterRow = !series && chapters.size >= 2
    val hasRow = (series && episodes.isNotEmpty()) || (!series && (chapterRow || similar.isNotEmpty()))
    val rowFocus = remember { FocusRequester() }
    var moved by remember { mutableStateOf(false) }
    fun go(to: Int) {
        moved = true
        Log.i(TAG, "odtwarzacz: poziom $level → $to (rząd=${if (!hasRow) 0 else if (series) episodes.size else similar.size}${if (series) " odcinków" else " podobnych"})")
        level = to
    }
    LaunchedEffect(level) {
        if (!moved) return@LaunchedEffect
        kotlinx.coroutines.delay(60)
        try {
            when (level) {
                0 -> progressBarFocusRequester.requestFocus()
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
        // Makieta `Player` (layout player-peek): scrim od dołu, wiersz „informacje | napisy · jakość · dźwięk”, 30 dp, pasek postępu.
        NiuniaSurface(NiuniaSurfaceKind.ScrimPlayer, Modifier.fillMaxSize())
        Column(Modifier.align(Alignment.BottomStart).offset(y = controlsShift).padding(start = NiuniaSpacing.Space40, end = NiuniaSpacing.Space40, bottom = 46.dp).fillMaxWidth()) {
            val title = if (series) uiState.contentName ?: uiState.title else uiState.title
            val meta = listOfNotNull(
                uiState.releaseYear?.takeIf { it.isNotBlank() && !series },
                if (series) "S${uiState.currentSeason}:O${uiState.currentEpisode}" else null,
                uiState.currentEpisodeTitle?.takeIf { series && it.isNotBlank() },
                speedLabel(uiState.playbackSpeed).takeIf { uiState.playbackSpeed != 1f },
                // Z-22 (P-33): „kończy się o 22:42” = teraz + pozostały czas (z prędkością); liczone przy każdej zmianie pozycji.
                niuniaEndsAt(timeline.duration, timeline.currentPosition, uiState.playbackSpeed)?.let { stringResource(R.string.niunia_ke2_ends_at, it) },
            ).joinToString(" · ")
            val quality = niuniaQualityGlyph(uiState.streamInfoData?.videoHeight, uiState.currentStreamName)
            // Napisy filmu nad całym blokiem sterowania (informacje i ikony stoją obok siebie nad paskiem).
            Row(Modifier.fillMaxWidth().onGloballyPositioned { controlsTopPx = it.positionInRoot().y }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                NiuniaPlayerInfoBlock(
                    eyebrow = stringResource(if (series) R.string.niunia_player_series else R.string.niunia_player_movie),
                    logo = uiState.logo, title = title, meta = meta.ifBlank { null }, quality = quality,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Row(
                    Modifier.niuniaTrapArrows("odtwarzacz: ikony", setOf(androidx.compose.ui.focus.FocusDirection.Left, androidx.compose.ui.focus.FocusDirection.Right, androidx.compose.ui.focus.FocusDirection.Up)).onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown) {
                            try { progressBarFocusRequester.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: ▼ z ikon: ${e.message}") }
                            true
                        } else false
                    },
                    horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12), verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Z-6 (makieta Player / PlayerAudio / PlayerSubs): Dźwięk → Lektor → Napisy w kolejności zakładek panelu, każdy otwiera
                    // panel na swojej zakładce (przycisk otwartej zakładki aktywny). Bez przycisku jakości „UHD”: jakość ręcznie w panelu
                    // Więcej i na pilocie (decyzja właściciela 22.11), znacznik jakości zostaje w opisie. Z-14 (PlayerMore): przycisk Więcej.
                    NiuniaButton(onClick = { panelTab = 0; panelOpen = true; Log.i(TAG, "odtwarzacz: panel → Dźwięk"); resetHide() }, icon = "audio",
                        description = stringResource(R.string.niunia_player_audio), focusRequester = firstButton, active = panelOpen && panelTab == 0)
                    if (lektorOn) NiuniaButton(onClick = { panelTab = 1; panelOpen = true; Log.i(TAG, "odtwarzacz: panel → Lektor"); resetHide() }, icon = "voice-over",
                        description = stringResource(R.string.niunia_ds_tab_lektor), focusRequester = lektorButton, active = panelOpen && panelTab == 1)
                    NiuniaButton(onClick = { panelTab = 2; panelOpen = true; Log.i(TAG, "odtwarzacz: panel → Napisy"); resetHide() }, icon = "subtitles",
                        description = stringResource(R.string.niunia_player_subtitles), active = panelOpen && panelTab == 2, focusRequester = audioButton)
                    NiuniaButton(onClick = { panelOpen = false; moreOpen = true; Log.i(TAG, "odtwarzacz: przycisk Więcej → panel Więcej"); resetHide() }, icon = "more",
                        description = stringResource(R.string.niunia_player_more), active = moreOpen, focusRequester = moreButton)
                }
            }
            Spacer(Modifier.height(NiuniaSpace.PlayerGap))
            val duration = scrub.state.durationMs.coerceAtLeast(1L)
            // Paczka „oczywiste” pkt 4: miniatury przy przewijaniu (moduł Reshaped seekpreview) — pozycja znacznika jak w pasku bazy.
            val marker = scrub.state.markerMs
            LaunchedEffect(marker) {
                if (archiveLength == null) viewModel.controller.pendingPreviewSeekPosition = marker
                if (marker != null) Log.d(TAG, "odtwarzacz: miniatura przy ${marker / 1000} s")
            }
            if (marker != null && archiveLength == null) com.nuvio.tv.ui.screens.player.seekpreview.SeekPreviewAboveProgressBar(viewModel)
            NiuniaSeekBar(
                // 22.11: granice rozdziałów na pasku (Exo: rozdziały MKV, mpv: lista), gdy ustawienie „Rozdziały” jest włączone.
                state = NiuniaSeekState.of(scrub.state.positionMs, duration).copy(
                    chapters = if (uiState.chaptersEnabled) uiState.chapters.map { it.startMs.toFloat() / duration } else emptyList()),
                start = formatScrubTime(scrub.state.markerMs ?: scrub.state.positionMs),
                end = "\u2212" + formatScrubTime((scrub.state.durationMs - scrub.state.positionMs).coerceAtLeast(0)),
                focused = barFocused,
                marker = scrub.state.markerMs?.let { (it.toFloat() / duration).coerceIn(0f, 1f) },
                trackModifier = Modifier
                    .focusRequester(progressBarFocusRequester)
                    .focusRequester(playPauseFocusRequester)
                    .onFocusChanged { barFocused = it.isFocused; if (it.isFocused) resetHide() }
                    .onPreviewKeyEvent { e ->
                        // Długie OK na pasku (520 ms) = szczegóły filmu/serialu (makieta: long-href PlayerFilmInfo); krótkie = pauza (pasek).
                        val ok = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
                        if (!ok) return@onPreviewKeyEvent false
                        when (e.type) {
                            KeyEventType.KeyDown -> {
                                if (okDownAt == 0L) {
                                    okDownAt = System.currentTimeMillis(); okLong = false
                                    val mark = okDownAt
                                    scope.launch {
                                        kotlinx.coroutines.delay(com.nuvio.tv.skin.niunia.tokens.NiuniaMotion.LongPress.toLong())
                                        if (okDownAt == mark) { okLong = true; Log.i(TAG, "długie OK → pasek: szczegóły"); go(2) }
                                    }
                                }
                                false
                            }
                            KeyEventType.KeyUp -> { val consumed = okLong; okDownAt = 0L; okLong = false; consumed }
                            else -> false
                        }
                    }
                    .forgeScrubKeys(scrub, onTogglePause = { Log.i(TAG, "OK → pasek: pauza/odtwarzanie"); onEvent(PlayerEvent.OnPlayPause); resetHide() },
                        onUp = { try { firstButton.requestFocus(); true } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: ▲ z paska: ${e.message}"); false } },
                        onDown = { go(if (hasRow) 1 else 2); true })
                    .focusable(),
            )
        }

        // „Więcej” — szklany panel po prawej (canvas .glass: 360 szer., od y 40, promień 28 po lewej).
        // Rząd odcinków: wystaje u dołu, ▼ go podnosi (4 kafle 16:9 i kawałek piątego).
        if (hasRow && series) {
            Column(Modifier.align(Alignment.BottomStart).offset(y = rowShift).padding(bottom = NiuniaSpacing.Space16)) {
                NiuniaPlayerRow(
                    uiState, episodes, focus = rowFocus, onEvent = { onEvent(it); resetHide() },
                    onUp = { go(0); true }, onDown = { go(2); true },
                )
                // Makieta PlayerSeries: „▼ Szczegóły serialu” od lewej (decyzja 20.11) — to samo co ▼ z rzędu.
                NiuniaButton(onClick = { Log.i(TAG, "odtwarzacz: Szczegóły serialu"); go(2) }, label = stringResource(R.string.niunia_ds_series_details),
                    icon = "chevron-down", kind = NiuniaButtonKind.Chip,
                    modifier = Modifier.padding(start = NiuniaSpacing.Space40, top = NiuniaSpacing.Space16))
            }
        } else if (hasRow && chapterRow) {
            NiuniaChapterRow(
                chapters, timeline.currentPosition, focus = rowFocus, onUp = { go(0); true }, onDown = { go(2); true },
                onSeek = { ms -> Log.i(TAG, "odtwarzacz: rozdział → ${ms / 1000} s"); onEvent(PlayerEvent.OnSeekTo(ms)); resetHide() },
                modifier = Modifier.align(Alignment.BottomStart).offset(y = rowShift).padding(bottom = NiuniaSpace.AfterRow),
            )
        } else if (hasRow) {
            NiuniaSimilarRow(
                similar, focus = rowFocus, onUp = { go(0); true }, onDown = { go(2); true },
                modifier = Modifier.align(Alignment.BottomStart).offset(y = rowShift).padding(bottom = NiuniaSpace.AfterRow),
            )
        }
        val trailerOpen = extrasState.trailer != null
        if (level == 2) NiuniaPlayerDetails(
            uiState, series, onEvent = { onEvent(it); resetHide() }, extras = extrasState,
            onLibrary = { extras.toggleLibrary() },
            onTrailer = {
                if (uiState.isPlaying) onEvent(PlayerEvent.OnPlayPause)
                extras.playTrailer(uiState.contentName ?: uiState.title, uiState.releaseYear, viewModel.controller.contentType ?: if (series) "series" else "movie")
            },
        )
        BackHandler(enabled = level > 0 && !moreOpen && !panelOpen && !trailerOpen) { go(if (level == 2 && hasRow) 1 else 0) }
        extrasState.trailer?.let { t -> NiuniaTrailerOverlay(t) { extras.closeTrailer() } }

        SideEffect { ForgeSheetOpen.open = moreOpen || panelOpen || level > 0 || trailerOpen }
        // Wstecz krok po kroku przez wpięcie w PlayerScreen.handleBackPress (makieta Player: panel → odtwarzacz; ▼▼ → ▼ → pasek).
        val hasRowNow by androidx.compose.runtime.rememberUpdatedState(hasRow)
        DisposableEffect(Unit) {
            NiuniaPlayerBack.handler = {
                when {
                    extras.state.value.trailer != null -> { Log.i(TAG, "Wstecz → odtwarzacz (zwiastun zamknięty)"); extras.closeTrailer(); true }
                    moreOpen -> { moreOpen = false; Log.i(TAG, "Wstecz → odtwarzacz (Więcej zamknięte)"); true }
                    panelOpen -> { panelOpen = false; Log.i(TAG, "Wstecz → odtwarzacz (panel zamknięty)"); true }
                    level > 0 -> { val to = if (level == 2 && hasRowNow) 1 else 0; Log.i(TAG, "Wstecz → poziom $to"); go(to); true }
                    else -> false
                }
            }
            onDispose { NiuniaPlayerBack.handler = null }
        }
        DisposableEffect(Unit) { onDispose { ForgeSheetOpen.open = false } }
        LaunchedEffect(moreOpen, panelOpen, level) { while (moreOpen || panelOpen || level > 0) { resetHide(); kotlinx.coroutines.delay(2_000) } }
        AnimatedVisibility(panelOpen, modifier = Modifier.align(Alignment.TopEnd), enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
            BackHandler { panelOpen = false; Log.i(TAG, "Wstecz → odtwarzacz (panel zamknięty)") }
            NiuniaAudioPanel(
                uiState = uiState,
                initialTab = panelTab,
                close = { panelOpen = false },
                onEvent = { onEvent(it); resetHide() },
                openMore = { panelOpen = false; moreOpen = true },
                toast = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
            )
        }
        AnimatedVisibility(moreOpen, modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp), enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
            BackHandler { moreOpen = false }
            NiuniaMorePanel(series = series, speed = uiState.playbackSpeed, close = { moreOpen = false }, onEvent = onEvent, toast = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() })
        }
    }
}

/**
 * Panel „Audio, lektor i napisy” (makieta `Player`, NOverlay panel): zakładki Audio · Lektor · Napisy (decyzja 20.11).
 * Lektor: włącz/wyłącz, głośność lektora i ściszanie filmu (OK = następna wartość, panel zostaje otwarty); fokus startowy
 * „Głośność lektora”. Audio: ścieżka dźwiękowa i więcej opcji (źródła, jakość, test łącza). Napisy: wybór napisów.
 */
@Composable
internal fun NiuniaAudioPanel(uiState: PlayerUiState, initialTab: Int = -1, close: () -> Unit, onEvent: (PlayerEvent) -> Unit, openMore: () -> Unit, toast: (String) -> Unit) {
    val ctx = LocalContext.current
    val lektorOn = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(ctx, "module_lektor")
    var tab by remember { mutableIntStateOf(if (initialTab >= 0) initialTab else if (lektorOn) 1 else 0) }
    val first = remember { FocusRequester() }
    LaunchedEffect(tab) {
        Log.i(TAG, "odtwarzacz: panel, zakładka $tab, fokus startowy")
        try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: fokus panelu: ${e.javaClass.simpleName}: ${e.message}") }
    }
    NiuniaPanelOverlay(stringResource(R.string.niunia_ds_panel_title), Modifier.niuniaTrapArrows("odtwarzacz: panel", setOf(
        androidx.compose.ui.focus.FocusDirection.Left, androidx.compose.ui.focus.FocusDirection.Right, androidx.compose.ui.focus.FocusDirection.Up, androidx.compose.ui.focus.FocusDirection.Down))
        // Test JMGO build155: ▼ z ostatniego wiersza („Wybierz napisy”) przenosiło fokus na przycisk „Napisy” pod panelem.
        .focusProperties { exit = { androidx.compose.ui.focus.FocusRequester.Cancel } }.focusGroup()) {
        NiuniaCollection(NiuniaCollectionKind.Chips, Modifier.padding(bottom = NiuniaSpacing.Space8)) {
            NiuniaButton(onClick = { tab = 0 }, label = stringResource(R.string.niunia_ds_tab_audio), kind = NiuniaButtonKind.Chip, selected = tab == 0)
            if (lektorOn) NiuniaButton(onClick = { tab = 1 }, label = stringResource(R.string.niunia_ds_tab_lektor), kind = NiuniaButtonKind.Chip, selected = tab == 1)
            NiuniaButton(onClick = { tab = 2 }, label = stringResource(R.string.niunia_player_subtitles), kind = NiuniaButtonKind.Chip, selected = tab == 2)
        }
        when (tab) {
            1 -> {
                var rev by remember { mutableIntStateOf(0) }
                val m = com.nuvio.tv.fork.lektor.LektorModule
                val on = rev.let { com.nuvio.tv.fork.lektor.LektorEngine.enabled }
                NiuniaListRow(stringResource(R.string.niunia_ds_tab_lektor), onClick = { toast(com.nuvio.tv.fork.lektor.LektorController.toggle(ctx)); rev++ },
                    value = stringResource(if (on) R.string.niunia_player_on else R.string.niunia_player_off))
                NiuniaListRow(stringResource(R.string.lektor_volume_row), onClick = { m.cycleVolume(ctx); rev++ }, value = rev.let { m.volumeLabel(ctx) }, focusRequester = first)
                NiuniaListRow(stringResource(R.string.lektor_duck_row), onClick = { m.cycleDuck(ctx); rev++ }, value = rev.let { m.duckLabel(ctx) })
            }
            2 -> NiuniaSubtitlesTab(uiState, first, onEvent, toast, more = { close(); onEvent(PlayerEvent.OnShowSubtitleOverlay) })
            else -> {
                NiuniaListRow(stringResource(R.string.niunia_ds_audio_track), onClick = { close(); onEvent(PlayerEvent.OnShowAudioOverlay) }, trailing = NiuniaTrailing.Chevron, focusRequester = first)
                NiuniaListRow(stringResource(R.string.niunia_ds_more_options), onClick = openMore, trailing = NiuniaTrailing.Chevron)
            }
        }
    }
}

/**
 * Zakładka „Napisy” (makieta `PlayerSubs`): Napisy AI · polski (stan tłumaczenia, OK = tłumacz / pokaż oryginał),
 * napisy wbudowane, pliki z dodatków (najpierw polskie), „Wyłączone”, na końcu pełny wybór napisów Nuvio.
 * Bieżące napisy mają ✓; fokus startowy na bieżących (albo na Napisach AI).
 */
@Composable
internal fun NiuniaSubtitlesTab(uiState: PlayerUiState, first: FocusRequester, onEvent: (PlayerEvent) -> Unit, toast: (String) -> Unit, more: () -> Unit) {
    val ctx = LocalContext.current
    val aiOn = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(ctx, "module_ai_subtitles", true)
    val ai by com.nuvio.tv.fork.ai.subtitles.AiSubtitles.state.collectAsState()
    val selectedAddon = uiState.selectedAddonSubtitle
    val embedded = uiState.subtitleTracks
    val addons = remember(uiState.addonSubtitles, uiState.selectedAddonSubtitle) {
        val top = uiState.addonSubtitles.sortedByDescending { com.nuvio.tv.fork.ai.subtitles.AuxiliaryLanguagePolicy.primaryLanguage(it.lang) == "pl" }.take(6)
        // Test JMGO build151: wybrany plik spoza pierwszych 6 nie miał wiersza (brak ✓ i fokusu w panelu).
        val chosen = uiState.selectedAddonSubtitle
        if (chosen != null && top.none { it.id == chosen.id }) top + chosen else top
    }
    val off = selectedAddon == null && embedded.none { it.isSelected || it.index == uiState.selectedSubtitleTrackIndex }
    val aiPublished = ai?.published == true
    // Test JMGO build151 (B3-9): gdy żaden wiersz nie dostał [first], fokus zostawał na pasku pod panelem.
    // Jeden cel fokusu, zawsze istniejący wiersz: AI (pokazane) → bieżące wbudowane → bieżący plik → AI/„Wyłączone” → pierwszy wiersz.
    val selEmbedded = embedded.firstOrNull { it.isSelected || it.index == uiState.selectedSubtitleTrackIndex }
    val focusKey = when {
        aiOn && aiPublished -> "ai"
        selectedAddon == null && selEmbedded != null -> "e${selEmbedded.index}"
        selectedAddon != null && addons.any { it.id == selectedAddon.id } -> "a${selectedAddon.id}"
        aiOn -> "ai"
        off -> "off"
        embedded.isNotEmpty() -> "e${embedded.first().index}"
        addons.isNotEmpty() -> "a${addons.first().id}"
        else -> "off"
    }
    LaunchedEffect(embedded.size, addons.size) { Log.i(TAG, "odtwarzacz: napisy — wbudowane=${embedded.size}, z dodatków=${uiState.addonSubtitles.size}, AI=${if (aiPublished) "pokazane" else if (com.nuvio.tv.fork.ai.subtitles.AiSubtitles.running) "w toku" else "nie"}") }
    if (aiOn) {
        val running = com.nuvio.tv.fork.ai.subtitles.AiSubtitles.running
        val reason = com.nuvio.tv.fork.ai.subtitles.AiSubtitles.unavailableReason(ctx)
        NiuniaListRow(
            stringResource(R.string.niunia_ds_subs_ai),
            supporting = when {
                aiPublished -> stringResource(R.string.niunia_ds_subs_ai_shown)
                running -> ai?.let { "${it.doneBlocks}/${it.totalBlocks}" } ?: stringResource(R.string.niunia_ds_subs_ai_running)
                reason != null -> reason
                else -> stringResource(R.string.niunia_ds_subs_ai_start)
            },
            trailing = if (aiPublished) NiuniaTrailing.Check else NiuniaTrailing.None,
            focusRequester = if (focusKey == "ai") first else null,
            onClick = {
                when {
                    aiPublished -> toast(com.nuvio.tv.fork.ai.subtitles.AiSubtitles.showOriginal())
                    running -> toast(ctx.getString(R.string.niunia_ds_subs_ai_running))
                    reason != null -> toast(reason)
                    else -> toast(com.nuvio.tv.fork.ai.subtitles.AiSubtitles.start(ctx, com.nuvio.tv.fork.ai.subtitles.AiMode.WATCH, retranslate = false))
                }
            },
        )
    }
    embedded.forEach { t ->
        val sel = t.isSelected || t.index == uiState.selectedSubtitleTrackIndex
        NiuniaListRow(
            listOfNotNull(t.name.ifBlank { null } ?: t.language, stringResource(R.string.niunia_ds_subs_embedded)).joinToString(" · "),
            trailing = if (sel && selectedAddon == null) NiuniaTrailing.Check else NiuniaTrailing.None,
            focusRequester = if (focusKey == "e${t.index}") first else null,
            onClick = { onEvent(PlayerEvent.OnSelectSubtitleTrack(t.index)) },
        )
    }
    addons.forEach { sub ->
        val sel = selectedAddon?.id == sub.id
        NiuniaListRow(
            listOf(niuniaLangName(sub.lang), stringResource(R.string.niunia_ds_subs_file)).joinToString(" · "),
            supporting = sub.addonName,
            trailing = if (sel) NiuniaTrailing.Check else NiuniaTrailing.None,
            focusRequester = if (focusKey == "a${sub.id}") first else null,
            onClick = { onEvent(PlayerEvent.OnSelectAddonSubtitle(sub)) },
        )
    }
    NiuniaListRow(stringResource(R.string.niunia_ds_subs_off), trailing = if (off && !aiPublished) NiuniaTrailing.Check else NiuniaTrailing.None,
        focusRequester = if (focusKey == "off") first else null, onClick = { onEvent(PlayerEvent.OnDisableSubtitles) })
    // Z-22 (P-36): „Opóźnienie napisów −0,5 s”: ◄ ► co 0,1 s (opóźnienie Nuvio), OK = 0.
    if (!off || aiPublished) NiuniaListRow(
        stringResource(R.string.niunia_ke2_sub_delay), value = niuniaDelayLabel(uiState.subtitleDelayMs),
        modifier = Modifier.onPreviewKeyEvent { e ->
            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val d = when (e.key) { Key.DirectionLeft -> -100; Key.DirectionRight -> 100; else -> return@onPreviewKeyEvent false }
            Log.i(TAG, "odtwarzacz: opóźnienie napisów ${uiState.subtitleDelayMs} → ${uiState.subtitleDelayMs + d} ms")
            onEvent(PlayerEvent.OnAdjustSubtitleDelay(d, showOverlay = false)); true
        },
        onClick = { Log.i(TAG, "odtwarzacz: opóźnienie napisów → 0"); onEvent(PlayerEvent.OnResetSubtitleDelay(showOverlay = false)) },
    )
    NiuniaListRow(stringResource(R.string.niunia_ds_subtitles_choose), trailing = NiuniaTrailing.Chevron,
        onClick = more)
}

/** Napis w kółku przycisku jakości z grającego strumienia: UHD / HD / SD; null = nieznana. */
internal fun niuniaQualityGlyph(videoHeight: Int?, streamName: String?): String? {
    if (videoHeight != null && videoHeight > 0) return when { videoHeight >= 1800 -> "UHD"; videoHeight >= 700 -> "HD"; else -> "SD" }
    val n = streamName?.lowercase() ?: return null
    return when {
        Regex("""2160|\b4k\b|uhd""").containsMatchIn(n) -> "UHD"
        Regex("""1080|720|\bhd\b""").containsMatchIn(n) -> "HD"
        Regex("""480|360|\bsd\b""").containsMatchIn(n) -> "SD"
        else -> null
    }
}

@Composable
internal fun NiuniaMorePanel(series: Boolean, speed: Float, close: () -> Unit, onEvent: (PlayerEvent) -> Unit, toast: (String) -> Unit) {
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    // Test JMGO build157: otwarty z panelu audio („Więcej opcji”) tracił fokus, gdy znikał panel audio (koniec animacji) —
    // prośba o fokus od razu i ponownie po animacji, aż się uda.
    LaunchedEffect(Unit) {
        var ok = false
        for (wait in longArrayOf(0, 120, 250, 400)) {
            kotlinx.coroutines.delay(wait)
            ok = try { first.requestFocus(); true } catch (e: IllegalStateException) { false }
        }
        Log.i(TAG, "odtwarzacz: panel Więcej, fokus startowy ${if (ok) "Źródła" else "brak"}")
    }
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
            .focusProperties { exit = { FocusRequester.Cancel } } // ▼▲ na krańcach nie wychodzą pod panel (test JMGO build155)
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

/** Nazwa języka po polsku z kodu 2- albo 3-literowego („pol”, „pl”, „pt-BR”); test JMGO build151: „pol” dawał „po”. */
internal fun niuniaLangName(code: String): String {
    val c = code.trim().lowercase().substringBefore('-').substringBefore('_')
    if (c.isEmpty()) return code
    val two = if (c.length == 3) (java.util.Locale.getISOLanguages().firstOrNull { l ->
        try { java.util.Locale(l).isO3Language == c } catch (e: java.util.MissingResourceException) { false }
    } ?: c) else c
    val name = java.util.Locale(two).getDisplayLanguage(java.util.Locale("pl"))
    return if (name.isBlank() || name.equals(two, ignoreCase = true)) code else name
}


/** Długość programu z EPG dla archiwum IPTV, gdy strumień panelu jest dłuższy (np. 24 h); null = zwykły film. */
internal fun niuniaArchiveLengthMs(url: String?, streamMs: Long): Long? {
    val w = com.nuvio.tv.reshaped.livetv.LiveTvPlaybackRegistry.replayWindow(url) ?: return null
    val epg = (w.endMs - w.startMs).takeIf { it > 0L } ?: return null
    if (streamMs in 1..(epg + 60_000L)) return null
    if (archiveLogged != url) { archiveLogged = url; android.util.Log.i(TAG, "odtwarzacz: archiwum — długość z EPG ${epg / 60_000} min zamiast strumienia ${streamMs / 60_000} min") }
    return epg
}
private var archiveLogged: String? = null


/** Z-22 (P-33): godzina końca „HH:MM”; null przy nieznanej długości. */
internal fun niuniaEndsAt(durationMs: Long, positionMs: Long, speed: Float, now: Long = System.currentTimeMillis()): String? {
    if (durationMs <= 0L) return null
    val left = ((durationMs - positionMs).coerceAtLeast(0L) / speed.coerceAtLeast(0.25f)).toLong()
    return com.nuvio.tv.skin.niunia.components.niuniaClockText(now + left)
}

/**
 * Z-22 (P-34): rząd „Rozdziały” (NMediaCard ChapterCard: czas, „Rozdział N” albo nazwa z pliku, „teraz” przy bieżącym);
 * pierwszy rozdział 0:00 z podpisem „od początku” zastępuje osobny przycisk. OK = skok do rozdziału.
 */
@Composable
internal fun NiuniaChapterRow(
    chapters: List<com.nuvio.tv.ui.screens.player.PlayerChapter>,
    positionMs: Long,
    focus: FocusRequester,
    onUp: () -> Boolean,
    onDown: () -> Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = chapters.indexOfLast { it.startMs <= positionMs }
    LaunchedEffect(chapters.size) { Log.i(TAG, "odtwarzacz: rząd Rozdziały (${chapters.size}) zamiast Podobne") }
    Column(modifier) {
        NiuniaShelfTitle(stringResource(R.string.niunia_ke2_chapters), modifier = Modifier.padding(start = NiuniaDims.Gutter))
        androidx.compose.foundation.lazy.LazyRow(
            Modifier.padding(top = NiuniaSpace.TitleToRow - 22.dp).onPreviewKeyEvent {
                if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (it.key) { Key.DirectionUp -> onUp(); Key.DirectionDown -> onDown(); else -> false }
            },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = NiuniaDims.Gutter),
            horizontalArrangement = Arrangement.spacedBy(NiuniaSpace.TileGap),
        ) {
            items(chapters.size) { i ->
                val ch = chapters[i]
                var focused by remember { mutableStateOf(false) }
                Box(Modifier.niuniaFocusable(androidx.compose.foundation.shape.RoundedCornerShape(NiuniaDims.RadiusCard), scale = 1f, lift = false,
                    focusRequester = if (i == (if (current >= 0) current else 0)) focus else null, onFocus = { focused = it }, onOk = { onSeek(ch.startMs) })) {
                    NiuniaTextTile(
                        top = formatScrubTime(ch.startMs),
                        middle = ch.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.niunia_ke2_chapter_n, i + 1),
                        bottomLeft = if (i == 0) stringResource(R.string.niunia_ke2_chapter_start) else null,
                        bottomRight = if (i == current) stringResource(R.string.niunia_ke2_chapter_now) else null,
                        focused = focused,
                    )
                }
            }
        }
    }
}

/** Z-22 (P-36): „−0,5 s” / „0 s” / „+0,3 s”. */
internal fun niuniaDelayLabel(ms: Int): String {
    if (ms == 0) return "0 s"
    val v = "%.1f".format(java.util.Locale("pl", "PL"), kotlin.math.abs(ms) / 1000.0)
    return (if (ms < 0) "\u2212" else "+") + v + " s"
}
