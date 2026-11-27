package com.nuvio.tv.skin.niunia.youtube

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.skin.kicia.youtube.ForgeYouTubeLaunch
import com.nuvio.tv.skin.kicia.youtube.ForgeYtRail
import com.nuvio.tv.skin.kicia.youtube.forgeSharedYouTubeViewModel
import com.nuvio.tv.skin.kicia.youtube.forgeYtCards
import com.nuvio.tv.skin.niunia.components.NiuniaActionsDialog
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaClock
import com.nuvio.tv.skin.niunia.components.NiuniaDialogAction
import com.nuvio.tv.skin.niunia.components.NiuniaFocusStyle
import com.nuvio.tv.skin.niunia.components.NiuniaTag
import com.nuvio.tv.skin.niunia.components.NiuniaTagKind
import com.nuvio.tv.skin.niunia.components.niuniaFocusLook
import com.nuvio.tv.skin.niunia.components.niuniaOk
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType
import com.nuvio.tv.youtube.YouTubeViewModel
import com.nuvio.tv.youtube.YtItem
import com.nuvio.tv.youtube.YtKind
import com.nuvio.tv.youtube.YtPlayable
import com.nuvio.tv.youtube.YtTab

private const val TAG = "ForkNiunia"

/** Z-23: ikony design systemu dla kanałów szyny (NIcon subscriptions, playlist, live, music, games, news, sport, shorts, channels). */
private fun niuniaYtIcon(tab: YtTab): String = when (tab) {
    YtTab.HOME -> "home"; YtTab.SUBSCRIPTIONS -> "subscriptions"; YtTab.HISTORY -> "history"; YtTab.PLAYLISTS -> "playlist"
    YtTab.LIVE -> "live"; YtTab.MUSIC -> "music"; YtTab.GAMING -> "games"; YtTab.NEWS -> "news"; YtTab.SPORTS -> "sport"
    YtTab.SHORTS -> "shorts"; YtTab.CHANNELS -> "channels"
}

/**
 * Z-23 (makieta `YouTube`, wzór Kicia/Forge S10): NScreen `rail` — po lewej NNavigation `icon-rail` (11 kanałów, otwarty `active`,
 * fokus biały ×1,08 z nazwą w białej pigułce), obok siatka 3 × N kart `YouTubeCard` (259 × 146, czas / „Na żywo”, tytuł 2 linie,
 * „kanał · wyświetlenia”); Shorts: plakaty 5 w rzędzie. U góry po prawej „Szukaj” + zegar. Dane: [YouTubeViewModel] (Nuvio One).
 */
@Composable
fun NiuniaYouTube(onPlay: (YtPlayable) -> Unit, viewModel: YouTubeViewModel = forgeSharedYouTubeViewModel()) {
    val ctx = LocalContext.current
    val state by viewModel.state.collectAsState()
    val error by viewModel.error.collectAsState()
    val resolving by viewModel.resolving.collectAsState()
    val cards = remember(state.rows, state.grid) { forgeYtCards(state.rows.map { it.items }, state.grid) }
    val shorts = state.tab == YtTab.SHORTS && state.subTitle == null
    val perRow = if (shorts) 5 else 3
    val rows = remember(cards, perRow) { cards.chunked(perRow) }
    val listState = rememberLazyListState()
    var menuItem by remember { mutableStateOf<YtItem?>(null) }

    BackHandler(enabled = state.subTitle != null) { Log.i(TAG, "youtube: Wstecz → kanał ${state.tab}"); viewModel.back() }
    val pending by ForgeYouTubeLaunch.pending.collectAsState()
    // UX JMGO (ręczne testy 24.11): powrót z odtwarzacza filmu otwartego z „Oglądaj dalej” → z powrotem na Główną.
    val returning = remember { ForgeYouTubeLaunch.pending.value == null }
    LaunchedEffect(Unit) {
        val to = ForgeYouTubeLaunch.returnRoute ?: return@LaunchedEffect
        if (!returning) return@LaunchedEffect
        val shown = ForgeYouTubeLaunch.playerShown
        ForgeYouTubeLaunch.returnRoute = null; ForgeYouTubeLaunch.playerShown = false
        if (!shown) return@LaunchedEffect
        Log.i(TAG, "youtube: film z „Oglądaj dalej” zamknięty — Wstecz wraca na $to")
        if (!com.nuvio.tv.skin.niunia.nav.NiuniaNavActions.open(to)) Log.w(TAG, "youtube: powrót na $to — brak przejścia")
    }
    // UX 25.11 (test uxr6): film z „Oglądaj dalej” gra w odtwarzaczu bazy, nie w NiuniaYouTubePlayer — znacznik „odtwarzacz
    // się pokazał” ustawiany w chwili uruchomienia, inaczej Wstecz zostawał w zakładce YouTube.
    LaunchedEffect(pending) { ForgeYouTubeLaunch.take()?.let { item -> Log.i(TAG, "youtube: film z innego ekranu → odtwarzacz"); viewModel.activate(item, listOf(item)) { p -> if (ForgeYouTubeLaunch.returnRoute != null) ForgeYouTubeLaunch.playerShown = true; onPlay(p) } } }
    val pendingQuery by ForgeYouTubeLaunch.query.collectAsState()
    LaunchedEffect(pendingQuery) { ForgeYouTubeLaunch.takeSearch()?.let { q -> Log.i(TAG, "youtube: szukaj z ekranu Szukaj"); viewModel.search(q) } }
    // Doczytywanie 2 rzędy przed końcem.
    LaunchedEffect(listState, state.canLoadMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to listState.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 2 && state.canLoadMore) { Log.i(TAG, "youtube: doczytuję (${cards.size} kart)"); viewModel.loadMore() } }
    }
    LaunchedEffect(state.tab, state.subTitle, cards.size, state.loading) {
        Log.i(TAG, "youtube: ${state.subTitle ?: state.tab.name} — kart ${cards.size}${if (state.loading) ", wczytuję" else ""}${error?.let { ", błąd: $it" } ?: ""}")
    }

    // Wstecz z odtwarzacza: fokus na tę samą kartę.
    var lastVideoKey by rememberSaveable { mutableStateOf<String?>(null) }
    var ytSearchOpen by remember { mutableStateOf(false) }
    var ytSearchFocus by remember { mutableStateOf(false) }
    val lastVideo = remember { FocusRequester() }
    val firstVideo = remember { FocusRequester() }
    val railFocus = remember { List(ForgeYtRail.size) { FocusRequester() } }
    val signInFocus = remember { FocusRequester() }
    val signInShown = !(error != null && cards.isEmpty()) && !(state.loading && cards.isEmpty()) &&
        !state.signedIn && state.subTitle == null && (state.tab.needsSignIn || cards.isEmpty())
    val railActive = ForgeYtRail.indexOfFirst { it.first == state.tab }.coerceAtLeast(0)
    val contentTarget = when { signInShown -> signInFocus; cards.isEmpty() -> railFocus[railActive]; lastVideoKey != null -> lastVideo; else -> firstVideo }
    com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(contentTarget)
    LaunchedEffect(Unit) {
        var ok = false
        repeat(8) {
            if (!ok) {
                kotlinx.coroutines.delay(100)
                ok = try { (if (lastVideoKey != null) lastVideo else if (cards.isNotEmpty()) firstVideo else railFocus[railActive]).requestFocus(); true } catch (e: IllegalStateException) { false }
            }
        }
        Log.i(TAG, "youtube: fokus startowy → ${if (!ok) "brak (${cards.size} kart)" else if (lastVideoKey != null) "ostatnia karta" else if (cards.isNotEmpty()) "pierwsza karta" else "szyna"}")
    }
    // Przegląd 27.11, ekran 23 pkt 1a: karty dochodziły ~2 s po wejściu, a fokus zostawał na szynie („Główna”) — gdy widz
    // jeszcze nic nie nacisnął, fokus przechodzi na pierwszy film, gdy tylko karty się pojawią.
    var userKeys by remember { mutableStateOf(false) }
    val startedEmpty = remember { cards.isEmpty() }
    LaunchedEffect(cards.isNotEmpty()) {
        if (!startedEmpty || cards.isEmpty() || userKeys || signInShown || lastVideoKey != null) return@LaunchedEffect
        var ok = false
        repeat(6) {
            if (!ok) {
                kotlinx.coroutines.delay(100)
                ok = try { firstVideo.requestFocus(); true } catch (e: IllegalStateException) { false }
            }
        }
        Log.i(TAG, "youtube: karty doszły (${cards.size}) → fokus ${if (ok) "na pierwszy film" else "bez zmiany (karta jeszcze nie gotowa)"}")
    }

    Box(Modifier.fillMaxSize().background(NiuniaColor.Bg).onPreviewKeyEvent { userKeys = true; false }) {
        // Pasek akcji (NNavigation top-actions): Szukaj + zegar; ▼ = pierwsza karta (po powrocie: ostatnia oglądana).
        Row(Modifier.fillMaxWidth().padding(top = NiuniaSize.NavTop, end = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8, Alignment.End),
            verticalAlignment = Alignment.CenterVertically) {
            NiuniaButton(onClick = {
                // Przegląd 27.11, ekran 24: własna wyszukiwarka YouTube (było: wspólna wyszukiwarka filmów i seriali).
                Log.i(TAG, "youtube: Szukaj → wyszukiwarka YouTube")
                ytSearchOpen = true
            }, label = stringResource(R.string.niunia_ke2_yt_search), icon = "search", small = true,
                modifier = Modifier.focusProperties { down = if (cards.isNotEmpty()) (if (lastVideoKey != null) lastVideo else firstVideo) else railFocus[railActive] })
            NiuniaClock()
        }
        Row(Modifier.fillMaxSize().padding(start = NiuniaDims.Gutter, top = 68.dp)) {
            // Szyna: 36 dp co 4 dp; ◄ z siatki wchodzi na otwarty kanał.
            Column(
                // UX JMGO (ręczne testy 24.11): ◄ na szynie stał w miejscu — menu aplikacji było tylko pod Wstecz. Teraz ◄ = menu.
                Modifier.width(NiuniaSize.H36).fillMaxHeight().zIndex(1f)
                    .onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) com.nuvio.tv.skin.niunia.nav.NiuniaNavActions.toMenu("youtube ◄ z szyny") else false
                    }
                    .focusProperties {
                        @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                        enter = { railFocus[railActive] }
                    }
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4),
            ) {
                ForgeYtRail.forEachIndexed { i, (tab, look) ->
                    RailItem(niuniaYtIcon(tab), look.second, active = state.tab == tab && state.subTitle == null, requester = railFocus[i]) {
                        Log.i(TAG, "youtube: szyna → ${look.second}")
                        lastVideoKey = null
                        viewModel.selectTab(tab)
                    }
                }
            }
            Spacer(Modifier.width(NiuniaSpacing.Space24))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                state.subTitle?.let { Text(it, style = NiuniaType.TitleSection, maxLines = 1, modifier = Modifier.padding(bottom = NiuniaSpacing.Space8)) }
                when {
                    error != null && cards.isEmpty() -> Text(error.orEmpty(), style = NiuniaType.BodySmall, color = NiuniaColor.Text70)
                    state.loading && cards.isEmpty() -> Skeleton(perRow)
                    signInShown -> SignIn(signInFocus) { viewModel.selectTab(state.tab) }
                    cards.isEmpty() -> Text(stringResource(R.string.niunia_search_none, state.subTitle ?: ForgeYtRail.firstOrNull { it.first == state.tab }?.second?.second ?: ""), style = NiuniaType.BodySmall, color = NiuniaColor.Text70)
                    else -> LazyColumn(
                        state = listState,
                        // 12 dp zapasu, żeby karta w fokusie (×1,1) nie była obcięta.
                        modifier = Modifier.offset(x = (-12).dp),
                        contentPadding = PaddingValues(start = 12.dp, end = NiuniaDims.Gutter, top = 12.dp, bottom = 60.dp),
                        verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20),
                    ) {
                        itemsIndexed(rows, key = { i, row -> "r${i}_${row.first().videoId ?: row.first().title}" }) { ri, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20)) {
                                row.forEachIndexed { ci, item ->
                                    val itemKey = item.videoId ?: item.title
                                    YouTubeCard(
                                        item, shorts = shorts, resolving = resolving == item.videoId,
                                        focusRequester = if (itemKey == lastVideoKey) lastVideo else if (lastVideoKey == null && ri == 0 && ci == 0) firstVideo else null,
                                        onFocused = { viewModel.prefetch(item); lastVideoKey = itemKey },
                                        onLong = { Log.i(TAG, "youtube: długie OK → okno „${item.title.take(40)}”"); menuItem = item },
                                    ) {
                                        Log.i(TAG, "youtube: OK → ${item.kind} ${item.videoId ?: item.channelId}")
                                        val mix = item.kind == YtKind.PLAYLIST && item.videoId != null &&
                                            (item.source.playlistId?.startsWith("RD") == true || item.badge?.contains("Mix", ignoreCase = true) == true)
                                        if (mix) Log.i(TAG, "youtube: Mix → odtwarzam pierwszy film (${item.videoId})")
                                        viewModel.activate(if (mix) item.copy(kind = YtKind.VIDEO) else item, cards, onPlay)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (ytSearchOpen) NiuniaYouTubeSearch(viewModel, onSearch = { q ->
            ytSearchOpen = false; lastVideoKey = null; ytSearchFocus = true
            viewModel.search(q)
        }, onClose = { ytSearchOpen = false })
    }
    // Po wyszukaniu fokus na pierwszy wynik, gdy tylko wyniki są.
    LaunchedEffect(ytSearchFocus, cards.isNotEmpty(), state.loading) {
        if (!ytSearchFocus || cards.isEmpty() || state.loading) return@LaunchedEffect
        ytSearchFocus = false
        var ok = false
        repeat(6) { if (!ok) { kotlinx.coroutines.delay(100); ok = try { firstVideo.requestFocus(); true } catch (e: IllegalStateException) { false } } }
        Log.i(TAG, "youtube szukaj: wyniki ${cards.size} → fokus ${if (ok) "na pierwszy" else "bez zmiany"}")
    }
    menuItem?.let { item -> CardMenu(item, viewModel) { menuItem = null } }
}

/** NButton icon 36 na szynie: otwarty kanał `active` (fill-36), fokus biały ×1,08 i nazwa w białej pigułce obok (bez zmiany układu). */
@Composable
private fun RailItem(icon: String, label: String, active: Boolean, requester: FocusRequester, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(Modifier.size(NiuniaSize.H36)) {
        NiuniaButton(onClick = onOk, icon = icon, description = label, active = active, focusRequester = requester, onFocus = { focused = it })
        if (focused) Text(label, style = NiuniaType.LabelSmall, color = NiuniaColor.OnFocus, maxLines = 1,
            modifier = Modifier.align(Alignment.CenterStart).offset(x = NiuniaSize.H36 + NiuniaSpacing.Space8).wrapPill())
    }
}

private fun Modifier.wrapPill(): Modifier = this.zIndex(2f).wrapContentWidth(Alignment.Start, unbounded = true)
    .clip(RoundedCornerShape(50)).background(NiuniaColor.White).padding(horizontal = NiuniaSpacing.Space12, vertical = NiuniaSpacing.Space4)

/** NMediaCard `YouTubeCard`: obraz 259 × 146 (Shorts: plakat), czas albo „Na żywo”, tytuł 2 linie, „kanał · wyświetlenia”. Fokus: karta z obrazem. */
@Composable
private fun YouTubeCard(item: YtItem, shorts: Boolean, resolving: Boolean, focusRequester: FocusRequester?, onFocused: () -> Unit, onLong: () -> Unit, onOk: () -> Unit) {
    val w: Dp = if (shorts) NiuniaDims.PosterW else NiuniaSize.YoutubeW
    val h: Dp = if (shorts) NiuniaDims.PosterH else NiuniaSize.YoutubeH
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(NiuniaRadius.Radius16)
    Column(Modifier.width(w)) {
        Box(
            Modifier.size(w, h)
                .niuniaOk(item.title, focusRequester = focusRequester, onFocus = { focused = it; if (it) onFocused() }, onLongClick = onLong, onClick = onOk)
                .niuniaFocusLook(NiuniaFocusStyle.Lift, focused, shape)
                .clip(shape).background(NiuniaColor.Surface),
        ) {
            item.thumbnail?.takeIf { it.isNotBlank() }?.let { url -> AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            when {
                item.isLive -> NiuniaTag(stringResource(R.string.niunia_ke2_yt_live), NiuniaTagKind.Live, Modifier.align(Alignment.TopStart).padding(NiuniaSpacing.Space8))
                !item.badge.isNullOrBlank() -> NiuniaTag(item.badge.orEmpty(), NiuniaTagKind.Badge, Modifier.align(Alignment.BottomEnd).padding(NiuniaSpacing.Space8))
            }
            if (item.percentWatched in 1..100) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(NiuniaSize.ProgressH).background(NiuniaColor.Fill36)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(item.percentWatched / 100f).background(NiuniaColor.Live))
            }
            if (resolving) Text("…", style = NiuniaType.Label, modifier = Modifier.align(Alignment.Center))
        }
        Text(item.title, style = NiuniaType.LabelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = NiuniaSpacing.Space8))
        val meta = listOfNotNull(item.author, item.meta ?: item.postTime).filter { it.isNotBlank() }.joinToString(" · ")
        if (meta.isNotBlank()) Text(meta, style = NiuniaType.Caption, color = NiuniaColor.Text70, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Wczytywanie: szkielety kart (powierzchnia `surface`, bez treści). */
@Composable
private fun Skeleton(perRow: Int) {
    val shorts = perRow == 5
    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20)) {
                repeat(perRow) {
                    Box(Modifier.size(if (shorts) NiuniaDims.PosterW else NiuniaSize.YoutubeW, if (shorts) NiuniaDims.PosterH else NiuniaSize.YoutubeH)
                        .clip(RoundedCornerShape(NiuniaRadius.Radius16)).background(NiuniaColor.Surface))
                }
            }
        }
    }
}

/** Subskrypcje / Historia / Playlisty bez konta: logowanie kodem (youtube.com/activate) — to samo konto co w Nuvio One. */
@Composable
private fun SignIn(focus: FocusRequester, onSignedIn: () -> Unit) {
    val account: com.nuvio.tv.youtube.YouTubeAccountViewModel = hiltViewModel()
    val acc by account.state.collectAsState()
    LaunchedEffect(Unit) { account.refresh() }
    LaunchedEffect(acc.signedIn) { if (acc.signedIn) { Log.i(TAG, "youtube: zalogowano"); onSignedIn() } }
    val qr = remember(acc.activateLink) {
        acc.activateLink?.let { link ->
            try { com.nuvio.tv.core.qr.QrCodeGenerator.generate(link, 400) } catch (e: Exception) { Log.w(TAG, "youtube: kod QR: ${e.javaClass.simpleName}: ${e.message}"); null }
        }
    }
    Column(Modifier.padding(top = NiuniaSpacing.Space24), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
        Text("Zaloguj się do YouTube", style = NiuniaType.TitleSection)
        Text("Zaloguj się, aby zobaczyć subskrypcje, historię i playlisty.", style = NiuniaType.BodySmall, color = NiuniaColor.Text70)
        if (acc.code != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space24), verticalAlignment = Alignment.CenterVertically) {
                if (qr != null) androidx.compose.foundation.Image(bitmap = qr.asImageBitmap(), contentDescription = "Kod QR logowania",
                    modifier = Modifier.size(140.dp).clip(RoundedCornerShape(NiuniaRadius.Radius6)))
                Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8)) {
                    Text("Zeskanuj kod telefonem albo wejdź na ${acc.url} i wpisz kod:", style = NiuniaType.BodySmall, color = NiuniaColor.Text70, modifier = Modifier.width(360.dp))
                    Text(acc.code.orEmpty(), style = NiuniaType.TitleScreen)
                    NiuniaButton(onClick = { Log.i(TAG, "youtube: logowanie anulowane"); account.cancelSignIn() }, label = "Anuluj", compact = true, focusRequester = focus)
                }
            }
        } else {
            NiuniaButton(onClick = { Log.i(TAG, "youtube: logowanie kodem"); account.startSignIn() }, label = "Zaloguj się", compact = true, focusRequester = focus)
            acc.error?.let { Text(it, style = NiuniaType.Caption, color = NiuniaColor.Live) }
        }
    }
}

/** Długie OK na karcie: okno (Zapisz na później, Kanał, Nie interesuje mnie) — czynności z menu karty Nuvio One. */
@Composable
private fun CardMenu(item: YtItem, viewModel: YouTubeViewModel, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val menu by viewModel.menu.collectAsState()
    LaunchedEffect(item) { viewModel.openMenu(item) }
    LaunchedEffect(menu?.message) { menu?.message?.let { m -> Log.w(TAG, "youtube: okno karty: $m"); com.nuvio.tv.fork.ForkLog.say(ctx, TAG, m) } }
    fun close() { viewModel.closeMenu(); onClose() }
    NiuniaActionsDialog(
        eyebrow = item.author ?: "YouTube",
        title = item.title,
        state = listOfNotNull(item.meta, item.postTime, item.badge).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { null },
        onDismiss = { Log.i(TAG, "youtube: okno karty zamknięte"); close() },
        actions = listOfNotNull(
            if (item.kind == YtKind.VIDEO) NiuniaDialogAction(stringResource(R.string.niunia_ke2_yt_save_later), icon = "plus") {
                val m = viewModel.menu.value
                val row = m?.playlists?.firstOrNull { it.id == "WL" || it.title.contains("later", true) || it.title.contains("później", true) }
                when {
                    m == null || !m.signedIn -> { Log.i(TAG, "youtube: zapisz na później — bez konta"); com.nuvio.tv.fork.ForkLog.say(ctx, TAG, "Zaloguj się do YouTube, aby zapisywać filmy") }
                    row == null -> { Log.w(TAG, "youtube: zapisz na później — brak listy „Do obejrzenia” (${m.playlists?.size ?: "wczytuję"})"); com.nuvio.tv.fork.ForkLog.say(ctx, TAG, "Lista „Do obejrzenia” jeszcze się wczytuje") }
                    else -> { Log.i(TAG, "youtube: zapisz na później → ${row.title}"); viewModel.menuTogglePlaylist(row) }
                }
                close()
            } else null,
            (menu?.item?.channelId ?: item.channelId)?.let { id ->
                NiuniaDialogAction(stringResource(R.string.niunia_ke2_yt_channel) + (item.author?.let { ": $it" } ?: ""), icon = "channels", chevron = true) {
                    Log.i(TAG, "youtube: okno → kanał ${item.author}")
                    viewModel.openChannel(id, item.author ?: "")
                    close()
                }
            },
            NiuniaDialogAction(stringResource(R.string.niunia_ke2_yt_not_interested), dismiss = false) {
                Log.i(TAG, "youtube: okno → nie interesuje mnie")
                viewModel.menuNotInterested()
                onClose()
            },
        ),
    )
}
