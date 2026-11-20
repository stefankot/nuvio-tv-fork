package com.nuvio.tv.skin.niunia.player

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.skin.kicia.player.ForgeSheetOpen
import com.nuvio.tv.skin.kicia.player.forgeScrubKeys
import com.nuvio.tv.skin.kicia.player.formatScrubTime
import com.nuvio.tv.skin.kicia.player.rememberForgeScrub
import com.nuvio.tv.skin.kicia.youtube.ForgeYouTubeSession
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpace
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.PlayerViewModel
import com.nuvio.tv.youtube.YouTubePlayerViewModel
import com.nuvio.tv.youtube.YtItem
import com.nuvio.tv.youtube.YtKind
import com.nuvio.tv.youtube.YtVideoInfo

private const val TAG = "ForkNiunia"

/** Materiał z YouTube (karta YouTube, zwiastuny): id treści `youtube:` albo sesja YouTube trasy odtwarzacza. */
internal fun niuniaIsYouTube(contentId: String?, session: ForgeYouTubeSession?): Boolean =
    session != null || com.nuvio.tv.youtube.YouTubeProgress.isYouTubeId(contentId)

/** „Więcej z kanału”: filmy tego kanału z propozycji YouTube najpierw, potem pozostałe propozycje (bez kanałów i Shorts). */
internal fun niuniaMoreFromChannel(info: YtVideoInfo?, related: List<YtItem>): List<YtItem> {
    val videos = related.filter { it.kind == YtKind.VIDEO && it.videoId != null && !it.isShorts }
    val same = videos.filter { v -> info != null && ((info.channelId != null && v.channelId == info.channelId) || (info.author.isNotBlank() && v.author == info.author)) }
    return (same + (videos - same.toSet())).take(12)
}

/**
 * Odtwarzacz YouTube Niuni (canvas `PlayerLeanback`, SKIN.md „Player YouTube”): nad paskiem od lewej mały szary napis
 * wersalikami = kanał, tytuł 24 pogrubiony do 2 linii, szara linia „wyświetlenia · kiedy · długość” + znacznik jakości;
 * po prawej [Subskrybuj], awatar kanału i okrągłe: napisy, podoba się, nie podoba się, opis, zapisz, ustawienia;
 * pasek przez całą szerokość z przerwami rozdziałów (część obejrzana czerwona, gałka biała), pod nim nazwa rozdziału od lewej;
 * na dole wystają miniatury innych filmów kanału (z fokusem jasna, pozostałe 55 %).
 * Fokus startuje na pasku. ▼ z paska podnosi rząd „Więcej z kanału”, ▼ z rzędu = szczegóły (`PlayerLeanbackInfo`). BACK krok po kroku.
 */
@Composable
fun NiuniaYouTubeChrome(
    uiState: PlayerUiState,
    viewModel: PlayerViewModel,
    session: ForgeYouTubeSession?,
    progressBarFocusRequester: FocusRequester,
    playPauseFocusRequester: FocusRequester,
    onEvent: (PlayerEvent) -> Unit,
    onSubtitleClearanceChanged: (Float) -> Unit,
) {
    val context = LocalContext.current
    val yt: YouTubePlayerViewModel = hiltViewModel()
    val ytState by yt.state.collectAsState()
    val videoId = session?.videoId ?: viewModel.controller.contentId?.removePrefix(com.nuvio.tv.youtube.YouTubeProgress.ID_PREFIX)
    LaunchedEffect(videoId) {
        if (videoId != null) { yt.load(videoId); yt.loadRelated() }
        Log.i(TAG, "youtube: nakładka (film=${videoId != null}, sesja=${session != null})")
    }
    LaunchedEffect(ytState.message) {
        ytState.message?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show(); yt.clearMessage() }
    }
    val info = ytState.info
    val more = remember(info, ytState.related) { niuniaMoreFromChannel(info, ytState.related) }
    val timeline by viewModel.playbackTimeline.collectAsState()
    val resetHide = { viewModel.scheduleHideControls(); viewModel.onUserInteraction() }
    val scrub = rememberForgeScrub(positionMs = timeline.currentPosition, durationMs = timeline.duration, onSeek = { onEvent(PlayerEvent.OnSeekTo(it)); resetHide() })
    var barFocused by remember { mutableStateOf(false) }
    var level by remember { mutableIntStateOf(0) }
    var moreOpen by remember { mutableStateOf(false) }
    var moved by remember { mutableStateOf(false) }
    val firstButton = remember { FocusRequester() }
    val rowFocus = remember { FocusRequester() }
    var rootHeightPx by remember { mutableFloatStateOf(0f) }
    var controlsTopPx by remember { mutableFloatStateOf(0f) }
    val hasRow = more.isNotEmpty()
    fun go(to: Int) {
        moved = true
        Log.i(TAG, "youtube: poziom $level → $to (więcej z kanału=${more.size})")
        level = to
    }
    LaunchedEffect(level) {
        if (!moved) return@LaunchedEffect
        kotlinx.coroutines.delay(60)
        try {
            when (level) { 0 -> progressBarFocusRequester.requestFocus(); 1 -> rowFocus.requestFocus() }
        } catch (e: IllegalStateException) { if (level != 2) Log.w(TAG, "youtube: fokus poziomu $level: ${e.message}") }
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(250)
        var ok = false
        repeat(6) {
            if (!ok) {
                ok = try { progressBarFocusRequester.requestFocus(); true } catch (e: IllegalStateException) { false }
                if (!ok) kotlinx.coroutines.delay(80)
            }
        }
        if (!ok) Log.w(TAG, "youtube: pasek nie dostał fokusu")
    }
    LaunchedEffect(controlsTopPx, rootHeightPx) {
        if (rootHeightPx > 0f && controlsTopPx > 0f) onSubtitleClearanceChanged((rootHeightPx - controlsTopPx).coerceAtLeast(0f))
    }
    // ▼ podnosi wszystko tak, że rząd ma 36 dp do dołu ekranu (canvas: rząd od y 448, 115 wys.).
    val lift by animateDpAsState(if (level >= 1) (-(448 + 113 + 36 - 540)).dp else 0.dp, label = "niuniaYtLift")

    val duration = scrub.state.durationMs.coerceAtLeast(1L)
    val chapters = info?.chapters.orEmpty()
    val position = scrub.state.markerMs ?: scrub.state.positionMs
    val chapterNow = chapters.lastOrNull { it.second <= position }?.first

    Box(
        Modifier.fillMaxSize()
            .onPreviewKeyEvent { if (it.type == KeyEventType.KeyDown) resetHide(); false }
            .onGloballyPositioned { rootHeightPx = it.size.height.toFloat() },
    ) {
        NiuniaPlayerScrim(from = 0.30f)
        Box(Modifier.fillMaxSize().offset(y = lift)) {
            // Tytuł (canvas: 40, 232, szer. 420).
            Column(Modifier.offset(x = 40.dp, y = 232.dp).width(420.dp)) {
                Text((info?.author ?: "").uppercase(), style = NiuniaText.Caption.copy(fontSize = 12.sp, lineHeight = 16.sp), color = NiuniaColors.Text70, maxLines = 1)
                Text(info?.title?.takeIf { it.isNotBlank() } ?: uiState.title, style = NiuniaText.Logo.copy(fontSize = 24.sp, lineHeight = 30.sp), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val meta = listOfNotNull(NiuniaYtText.views(info?.viewCount), NiuniaYtText.date(info?.publishedDate), niuniaMinutes(context, timeline.duration)).joinToString(" · ")
                    if (meta.isNotEmpty()) Text(meta, style = NiuniaText.Body.copy(fontSize = 13.sp, lineHeight = 18.sp), color = NiuniaColors.Text70, maxLines = 1)
                    niuniaQuality(uiState)?.let { NiuniaChip(it, filled = true) }
                }
            }
            // Przyciski po prawej (canvas: right 48, y 328).
            Row(
                Modifier.align(Alignment.TopEnd).offset(x = (-48).dp, y = 326.dp).onPreviewKeyEvent {
                    if (it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown) {
                        try { progressBarFocusRequester.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "youtube: ▼ z przycisków: ${e.message}") }
                        true
                    } else false
                },
                horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                NiuniaPillButton(stringResource(if (info?.subscribed == true) R.string.niunia_yt_subscribed else R.string.niunia_yt_subscribe), focusRequester = firstButton) {
                    Log.i(TAG, "youtube: subskrypcja (było ${info?.subscribed})"); yt.toggleSubscribe(); resetHide()
                }
                Spacer(Modifier.width(8.dp))
                NiuniaChannelAvatar(info, 40)
                NiuniaIconButton(Icons.Outlined.ClosedCaption, stringResource(R.string.niunia_player_subtitles)) { onEvent(PlayerEvent.OnShowSubtitleOverlay); resetHide() }
                NiuniaIconButton(if (info?.liked == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, stringResource(R.string.niunia_yt_like)) { Log.i(TAG, "youtube: podoba się"); yt.toggleLike(); resetHide() }
                NiuniaIconButton(if (info?.disliked == true) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown, stringResource(R.string.niunia_yt_dislike)) { Log.i(TAG, "youtube: nie podoba się"); yt.toggleDislike(); resetHide() }
                NiuniaIconButton(Icons.Outlined.Description, stringResource(R.string.niunia_yt_description)) { go(2) }
                NiuniaIconButton(Icons.Outlined.PlaylistAdd, stringResource(R.string.niunia_yt_save)) {
                    Log.i(TAG, "youtube: zapisz na playliście → panel 1Tube"); com.nuvio.tv.skin.kicia.youtube.ForgeYouTubePanelRequests.open()
                }
                NiuniaIconButton(Icons.Outlined.Settings, stringResource(R.string.niunia_yt_settings)) { moreOpen = true; Log.i(TAG, "youtube: panel Więcej"); resetHide() }
            }
            // Pasek (canvas: .pline y 398) i nazwa rozdziału od lewej (y 420).
            Row(
                Modifier.offset(x = 40.dp, y = 392.dp).width(880.dp).onGloballyPositioned { controlsTopPx = it.positionInRoot().y },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(formatScrubTime(position), style = NiuniaText.Caption.copy(fontSize = 12.sp), color = NiuniaColors.Text)
                NiuniaTrack(
                    played = scrub.state.positionMs / duration.toFloat(), color = NiuniaPlayState.YOUTUBE.color(), focused = barFocused,
                    gaps = chapters.drop(1).map { it.second / duration.toFloat() }, knob = position / duration.toFloat(), knobColor = NiuniaColors.Text,
                    modifier = Modifier.weight(1f)
                        .focusRequester(progressBarFocusRequester)
                        .focusRequester(playPauseFocusRequester)
                        .onFocusChanged { barFocused = it.isFocused; if (it.isFocused) resetHide() }
                        .forgeScrubKeys(scrub, onTogglePause = { onEvent(PlayerEvent.OnPlayPause); resetHide() },
                            onUp = { try { firstButton.requestFocus(); true } catch (e: IllegalStateException) { Log.w(TAG, "youtube: ▲ z paska: ${e.message}"); false } },
                            onDown = { go(if (hasRow) 1 else 2); true })
                        .focusable(),
                )
                Text(formatScrubTime(scrub.state.durationMs), style = NiuniaText.Caption.copy(fontSize = 12.sp), color = NiuniaColors.Text70)
            }
            chapterNow?.let {
                Text(it, style = NiuniaText.Body.copy(fontSize = 13.sp, lineHeight = 18.sp), color = NiuniaColors.Text70, maxLines = 1,
                    modifier = Modifier.offset(x = 40.dp, y = 420.dp))
            }
            // Rząd „Więcej z kanału” (canvas: .rail y 448).
            if (hasRow) {
                NiuniaYtRow(more, rowFocus, dimUnfocused = level == 0, modifier = Modifier.offset(y = 448.dp).onPreviewKeyEvent {
                    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (it.key) { Key.DirectionUp -> { go(0); true }; Key.DirectionDown -> { go(2); true }; else -> false }
                }) { item ->
                    Log.i(TAG, "youtube: rząd → ${item.videoId}"); session?.playVideo?.invoke(item, ytState.related)
                }
            }
        }
        if (level == 2) {
            LaunchedEffect(Unit) { yt.loadComments() }
            NiuniaYouTubeDetails(info, more, ytState.comments, position, chapters,
                onSeek = { ms -> Log.i(TAG, "youtube: szczegóły → rozdział $ms ms"); onEvent(PlayerEvent.OnSeekTo(ms)); go(0) },
                onPlay = { item -> Log.i(TAG, "youtube: szczegóły → ${item.videoId}"); session?.playVideo?.invoke(item, ytState.related) },
                onSubscribe = { yt.toggleSubscribe() }, onLike = { yt.toggleLike() },
                onAllComments = { Log.i(TAG, "youtube: wszystkie komentarze → panel 1Tube"); com.nuvio.tv.skin.kicia.youtube.ForgeYouTubePanelRequests.open() },
                quality = niuniaQuality(uiState), minutes = niuniaMinutes(context, timeline.duration))
        }
        BackHandler(enabled = level > 0 && !moreOpen) { go(if (level == 2 && hasRow) 1 else 0) }
        SideEffect { ForgeSheetOpen.open = moreOpen || level > 0 }
        DisposableEffect(Unit) { onDispose { ForgeSheetOpen.open = false } }
        LaunchedEffect(moreOpen, level) { while (moreOpen || level > 0) { resetHide(); kotlinx.coroutines.delay(2_000) } }
        AnimatedVisibility(moreOpen, modifier = Modifier.align(Alignment.TopEnd).padding(top = 40.dp), enter = slideInHorizontally { it }, exit = slideOutHorizontally { it }) {
            BackHandler { moreOpen = false }
            NiuniaMorePanel(series = false, speed = uiState.playbackSpeed, close = { moreOpen = false }, onEvent = onEvent,
                toast = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() })
        }
    }
}

@Composable
private fun NiuniaYtRow(items: List<YtItem>, focus: FocusRequester, dimUnfocused: Boolean, modifier: Modifier = Modifier, captions: Boolean = false, onPlay: (YtItem) -> Unit) {
    LazyRow(modifier = modifier, contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(NiuniaSpace.TileGap)) {
        itemsIndexed(items, key = { i, v -> "${v.videoId}_$i" }) { i, v ->
            var focused by remember { mutableStateOf(false) }
            Box(Modifier.niuniaFocusable(RoundedCornerShape(NiuniaDims.RadiusCard), scale = 1f, lift = false, focusRequester = if (i == 0) focus else null, onFocus = { focused = it }, onOk = { onPlay(v) })) {
                NiuniaWideTile(v.thumbnail, if (captions) v.title else null, focused, dim = dimUnfocused, progress = v.percentWatched.takeIf { it > 0 }?.let { it / 100f })
            }
        }
    }
}

@Composable
private fun NiuniaChannelAvatar(info: YtVideoInfo?, size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(NiuniaColors.Fill24), contentAlignment = Alignment.Center) {
        if (info?.authorImage != null) AsyncImage(model = info.authorImage, contentDescription = info.author, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(niuniaInitials(info?.author ?: ""), style = NiuniaText.Button.copy(fontSize = 14.sp), color = NiuniaColors.Text)
    }
}

/** „21 min” / „1 godz. 4 min” z długości filmu. */
internal fun niuniaMinutes(context: android.content.Context, durationMs: Long): String? {
    if (durationMs <= 0L) return null
    val min = ((durationMs + 30_000) / 60_000).toInt()
    return if (min >= 60) context.getString(R.string.niunia_hours_minutes, min / 60, min % 60) else context.getString(R.string.niunia_minutes, min.coerceAtLeast(1))
}

/** Znacznik jakości z grającego źródła („4K”, „HD”) — tylko gdy nazwa źródła to mówi. */
internal fun niuniaQuality(ui: PlayerUiState): String? {
    val s = listOfNotNull(ui.currentStreamName, ui.currentStreamDescription).joinToString(" ").lowercase()
    return when {
        "2160" in s || "4k" in s || "uhd" in s -> "4K"
        "1080" in s || "720" in s || " hd" in s -> "HD"
        else -> null
    }
}

/**
 * Szczegóły YouTube (▼▼, canvas `PlayerLeanbackInfo`, WPIECIE pkt 4): rozmyte tło, od lewej etykieta „YOUTUBE”, tytuł,
 * meta, karta kanału + [Subskrybuj] + [👍 liczba]. Sekcje: Rozdziały (kafle: czas, nazwa, „teraz”; OK przewija) →
 * Więcej z kanału → Komentarze (karty + [Wszystkie]). Bez opisu (YtVideoInfo go nie ma). Odstępy 34/36 dp.
 */
@Composable
private fun NiuniaYouTubeDetails(
    info: YtVideoInfo?,
    more: List<YtItem>,
    comments: List<com.nuvio.tv.youtube.YtComment>,
    positionMs: Long,
    chapters: List<Pair<String, Long>>,
    onSeek: (Long) -> Unit,
    onPlay: (YtItem) -> Unit,
    onSubscribe: () -> Unit,
    onLike: () -> Unit,
    onAllComments: () -> Unit,
    quality: String?,
    minutes: String?,
) {
    val first = remember { FocusRequester() }
    val moreFocus = remember { FocusRequester() }
    val fallback = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        Log.i(TAG, "youtube: szczegóły (rozdziały=${chapters.size}, więcej=${more.size}, komentarze=${comments.size})")
        kotlinx.coroutines.delay(60)
        try { (if (chapters.isNotEmpty() || more.isNotEmpty()) first else fallback).requestFocus() } catch (e: IllegalStateException) {
            Log.w(TAG, "youtube: fokus szczegółów: ${e.message}")
            try { fallback.requestFocus() } catch (e2: IllegalStateException) { Log.w(TAG, "youtube: fokus zapasowy: ${e2.message}") }
        }
    }
    Box(Modifier.fillMaxSize()) {
        NiuniaDeepBackground(info?.videoId?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = NiuniaSpace.AfterRow)) {
            Text(stringResource(R.string.niunia_yt_label), style = NiuniaText.Caption.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp), color = NiuniaColors.Text70,
                modifier = Modifier.padding(start = NiuniaDims.Gutter, top = 28.dp))
            Column(Modifier.padding(start = NiuniaDims.Gutter, top = 8.dp).width(680.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(info?.title ?: "", style = NiuniaText.Logo.copy(fontSize = 24.sp, lineHeight = 30.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(listOfNotNull(NiuniaYtText.views(info?.viewCount), NiuniaYtText.date(info?.publishedDate), minutes).joinToString(" · "), style = NiuniaText.Body, color = NiuniaColors.Text70, maxLines = 1)
                    quality?.let { NiuniaChip(it, filled = true) }
                }
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NiuniaChannelAvatar(info, 40)
                    Column(Modifier.focusRequester(fallback).focusable()) {
                        Text(info?.author ?: "", style = NiuniaText.Meta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), maxLines = 1)
                        info?.subscribers?.let { Text(it, style = NiuniaText.Caption.copy(fontSize = 12.sp), color = NiuniaColors.Text50, maxLines = 1) }
                    }
                    Spacer(Modifier.width(8.dp))
                    NiuniaPillButton(stringResource(if (info?.subscribed == true) R.string.niunia_yt_subscribed else R.string.niunia_yt_subscribe)) { onSubscribe() }
                    NiuniaPillButton(info?.likeCount ?: "", icon = if (info?.liked == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp) { onLike() }
                }
            }
            Spacer(Modifier.height(NiuniaSpace.AfterRow))
            if (chapters.isNotEmpty()) {
                NiuniaShelfTitle(stringResource(R.string.niunia_yt_chapters), modifier = Modifier.padding(start = NiuniaDims.Gutter))
                val current = chapters.indexOfLast { it.second <= positionMs }
                LazyRow(Modifier.padding(top = NiuniaSpace.TitleToRow - 22.dp), contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(NiuniaSpace.TileGap)) {
                    itemsIndexed(chapters) { i, (title, start) ->
                        var focused by remember { mutableStateOf(false) }
                        Box(Modifier.niuniaFocusable(RoundedCornerShape(NiuniaDims.RadiusCard), scale = 1f, lift = false, focusRequester = if (i == 0) first else null, onFocus = { focused = it }, onOk = { onSeek(start) })) {
                            NiuniaTextTile(formatScrubTime(start), title, null, if (i == current) stringResource(R.string.niunia_yt_now) else null, focused)
                        }
                    }
                }
                Spacer(Modifier.height(NiuniaSpace.AfterRow))
            }
            if (more.isNotEmpty()) {
                NiuniaShelfTitle(stringResource(R.string.niunia_yt_more), modifier = Modifier.padding(start = NiuniaDims.Gutter))
                NiuniaYtRow(more, if (chapters.isEmpty()) first else moreFocus, dimUnfocused = false, captions = true,
                    modifier = Modifier.padding(top = NiuniaSpace.TitleToRow - 22.dp), onPlay = onPlay)
                Spacer(Modifier.height(NiuniaSpace.AfterRow))
            }
            if (comments.isNotEmpty()) {
                NiuniaShelfTitle(stringResource(R.string.niunia_yt_comments), modifier = Modifier.padding(start = NiuniaDims.Gutter))
                LazyRow(Modifier.padding(top = NiuniaSpace.TitleToRow - 22.dp), contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    itemsIndexed(comments.take(10)) { _, c ->
                        var focused by remember { mutableStateOf(false) }
                        Column(
                            Modifier.width(236.dp).niuniaFocusable(RoundedCornerShape(16.dp), scale = 1.04f, lift = false, onFocus = { focused = it }, onOk = onAllComments)
                                .background(if (focused) NiuniaColors.Fill24 else androidx.compose.ui.graphics.Color(0x12FFFFFF), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Text(c.author, style = NiuniaText.Meta.copy(fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(c.text, style = NiuniaText.Body.copy(fontSize = 13.sp, lineHeight = 18.sp), color = NiuniaColors.Text70, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    item { NiuniaPillButton(stringResource(R.string.niunia_yt_all)) { onAllComments() } }
                }
            }
        }
    }
}
