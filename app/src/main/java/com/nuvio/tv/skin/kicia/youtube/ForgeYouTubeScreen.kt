package com.nuvio.tv.skin.kicia.youtube

import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.nuvio.tv.skin.kicia.components.ForgeCard
import com.nuvio.tv.skin.kicia.components.ForgeIconButton
import com.nuvio.tv.skin.kicia.keyboard.ForgeKeyboardLayout
import com.nuvio.tv.skin.kicia.keyboard.ForgeOnScreenKeyboard
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.youtube.YouTubeViewModel
import com.nuvio.tv.youtube.YtItem
import com.nuvio.tv.youtube.YtKind
import com.nuvio.tv.youtube.YtPlayable
import com.nuvio.tv.youtube.YtTab

// Owner 10.11: the rail's icons are centred under the avatar (centre x 60 = 40 dp margin + half the 40 dp avatar); the first column of
// videos starts where the top bar's tab pill starts (x 88 = avatar 40..80 + 8), so the grid sits in one line under the bar.
private val YtRailItemWidth = 36.dp // v7.4: round buttons 36
private val YtRailInset = 62.dp - YtRailItemWidth / 2 // v7.4: rail x 44-80
internal val YtGridStart = 104.dp // v7.4: grid from x 104

/** The rail of the mockup (S10): search, then the feeds, each with its icon and Polish name. */
internal val ForgeYtRail: List<Pair<YtTab, Pair<ImageVector, String>>> = listOf(
    YtTab.HOME to (Icons.Outlined.Home to "Główna"),
    YtTab.SUBSCRIPTIONS to (Icons.Outlined.Subscriptions to "Subskrypcje"),
    YtTab.HISTORY to (Icons.Outlined.History to "Historia"),
    YtTab.PLAYLISTS to (Icons.AutoMirrored.Outlined.PlaylistPlay to "Playlisty"),
    YtTab.LIVE to (Icons.Outlined.LiveTv to "Na żywo"),
    YtTab.MUSIC to (Icons.Outlined.MusicNote to "Muzyka"),
    YtTab.GAMING to (Icons.Outlined.SportsEsports to "Gry"),
    YtTab.NEWS to (Icons.Outlined.Newspaper to "Wiadomości"),
    YtTab.SPORTS to (Icons.Outlined.EmojiEvents to "Sport"),
    YtTab.SHORTS to (Icons.Outlined.SmartDisplay to "Shorts"),
    YtTab.CHANNELS to (Icons.Outlined.People to "Kanały"),
)

/** One flat list of cards from a feed: rows interleaved into the grid of the mockup, each video once. */
internal fun forgeYtCards(rows: List<List<YtItem>>, grid: List<YtItem>): List<YtItem> {
    if (grid.isNotEmpty()) return grid
    val seen = HashSet<String>()
    val out = ArrayList<YtItem>()
    for (row in rows) for (item in row) {
        val key = item.videoId ?: (item.kind.name + item.title)
        if (seen.add(key)) out += item
    }
    return out
}

/**
 * S10 YouTube of the mockup: a narrow icon rail on the left (search + feeds) and a 3-column grid of 242 dp
 * cards (thumbnail with duration / "Na żywo", title in two lines, channel · views). Everything comes from
 * Nuvio One's own [YouTubeViewModel] (feeds, channels, playlists, playback lookup); this only draws it.
 */
@Composable
fun ForgeYouTubeScreen(
    onPlay: (YtPlayable) -> Unit,
    viewModel: YouTubeViewModel = forgeSharedYouTubeViewModel(),
) {
    com.nuvio.tv.skin.kicia.nav.ForgeTabBackToBar() // forge hook v7.4 (ZACHOWANIE): Wstecz z treści zakładki → pasek; własne handlery ekranu, złożone później, wygrywają
    val state by viewModel.state.collectAsState()
    val error by viewModel.error.collectAsState()
    val resolving by viewModel.resolving.collectAsState()
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val cards = remember(state.rows, state.grid) { forgeYtCards(state.rows.map { it.items }, state.grid) }
    val rows = remember(cards) { cards.chunked(3) }
    val listState = rememberLazyListState()

    BackHandler(enabled = state.subTitle != null && !searchOpen) { viewModel.back() }
    // A video picked on Home's "YouTube dla Ciebie" shelf.
    val pending by ForgeYouTubeLaunch.pending.collectAsState()
    LaunchedEffect(pending) {
        ForgeYouTubeLaunch.take()?.let { item -> viewModel.activate(item, listOf(item), onPlay) }
    }
    val pendingQuery by ForgeYouTubeLaunch.query.collectAsState()
    LaunchedEffect(pendingQuery) { ForgeYouTubeLaunch.takeSearch()?.let { q -> viewModel.search(q) } } // fork 19.11: szukaj z ekranu Szukaj
    LaunchedEffect(listState, state.canLoadMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to listState.layoutInfo.totalItemsCount }
            .collect { (last, total) -> if (last != null && total > 0 && last >= total - 2 && state.canLoadMore) viewModel.loadMore() }
    }

    // Back from a video: the focus returns to its card (it used to land on the rail).
    var lastVideoKey by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    val lastVideo = remember { androidx.compose.ui.focus.FocusRequester() }
    val firstVideo = remember { androidx.compose.ui.focus.FocusRequester() } // v7.4 (12): ▼ z paska = pierwsza karta
    LaunchedEffect(Unit) {
        if (lastVideoKey != null) {
            var ok = false
            repeat(6) { if (!ok) { ok = runCatching { lastVideo.requestFocus() }.isSuccess; if (!ok) kotlinx.coroutines.delay(80) } }
        }
    }
    Box(Modifier.fillMaxSize().background(ForgeColors.Bg)) {
        Row(Modifier.fillMaxSize()) {
            // Rail (76 dp): search, then the feeds. OK switches the feed.
            // Entering the rail (← from the grid) lands on the open feed, not on whatever item is level with the card.
            val railFocus = remember { List(ForgeYtRail.size + 1) { androidx.compose.ui.focus.FocusRequester() } }
            val signInFocus = remember { androidx.compose.ui.focus.FocusRequester() }
            val signInShown = !searchOpen && !(error != null && cards.isEmpty()) && !(state.loading && cards.isEmpty()) &&
                !state.signedIn && state.subTitle == null && (state.tab.needsSignIn || cards.isEmpty())
            val railActive = if (searchOpen) 0 else (ForgeYtRail.indexOfFirst { it.first == state.tab && state.subTitle == null } + 1).coerceAtLeast(0)
            Column(
                Modifier.width(YtGridStart).fillMaxHeight().zIndex(1f) // v7.4: nazwa ikony przy fokusie leży nad siatką
                    .focusProperties {
                        @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                        enter = { dir ->
                            when {
                                signInShown -> signInFocus // the sign-in screen is entered on its button (owner 10.11)
                                // v7.4 (12): ▼ from the bar lands on the first card (the remembered one after a return), not on the rail
                                dir == androidx.compose.ui.focus.FocusDirection.Down && !searchOpen && cards.isNotEmpty() -> if (lastVideoKey != null) lastVideo else firstVideo
                                else -> railFocus[railActive]
                            }
                        }
                    }
                    .focusGroup()
                    .verticalScroll(rememberScrollState())
                    .padding(start = YtRailInset, top = 16.dp, bottom = 30.dp), // icons centred under the avatar (x 60), the first one from y ~84
                verticalArrangement = Arrangement.spacedBy(4.dp), // v7.4: pitch 40
                horizontalAlignment = Alignment.Start,
            ) {
                // Mockup b37: bare icons ~35 dp apart, the open feed on a glass pill, the focused one white.
                ForgeYtRailItem(icon = Icons.Outlined.Search, label = "Szukaj w YouTube", active = searchOpen, onOk = { searchOpen = true }, requester = railFocus[0], tag = "przycisk.0")
                ForgeYtRail.forEachIndexed { index, (tab, look) ->
                    ForgeYtRailItem(
                        requester = railFocus[index + 1],
                        icon = look.first,
                        label = look.second,
                        active = !searchOpen && state.tab == tab && state.subTitle == null,
                        onOk = { searchOpen = false; viewModel.selectTab(tab) },
                        tag = "przycisk.${index + 1}", // v7.4 (12)
                    )
                }
            }
            if (searchOpen) {
                // S11 (c11): field, keyboard 9 columns + suggestions, a row of videos under them.
                val suggestions by viewModel.suggestions.collectAsState()
                LaunchedEffect(query) { viewModel.suggest(query) }
                ForgeYtSearchPanel(
                    query = query,
                    onQuery = { query = it },
                    suggestions = suggestions,
                    onSearch = { q -> if (q.isNotBlank()) { query = q; viewModel.search(q) }; searchOpen = false },
                    videos = cards.take(6),
                    onVideo = { item -> viewModel.activate(item, cards, onPlay) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            } else
            Column(Modifier.weight(1f).fillMaxHeight()) {
                // The feed has no heading in the mockup (the rail says which one is open); a channel page or search keeps its title.
                (state.subTitle ?: ForgeYtRail.firstOrNull { it.first == state.tab }?.second?.second?.takeIf { !searchOpen })?.let { Text(it, style = ForgeText.head, maxLines = 1, modifier = Modifier.padding(start = 0.dp, top = 8.dp, bottom = 0.dp).testTag("naglowek-sekcji.0")) } // v7.4: section header with the open feed
                when {
                    error != null && cards.isEmpty() -> Text(error.orEmpty(), style = ForgeText.body, modifier = Modifier.padding(12.dp))
                    state.loading && cards.isEmpty() -> Text("Wczytuję…", style = ForgeText.body, modifier = Modifier.padding(12.dp))
                    // Owner 10.11: no account = a sign-in screen (code to enter at youtube.com/activate), not "Brak filmów".
                    !state.signedIn && state.subTitle == null && (state.tab.needsSignIn || cards.isEmpty()) ->
                        ForgeYtSignIn(focus = signInFocus, onSignedIn = { viewModel.selectTab(state.tab) })
                    cards.isEmpty() -> Text("Brak filmów.", style = ForgeText.body, modifier = Modifier.padding(12.dp))
                    else -> LazyColumn(
                        state = listState,
                        // v7.4 (12): 16 dp room on the left, so the focused card of the first column grows over the gap (x 87.8) instead of being clipped
                        modifier = Modifier.offset(x = (-16).dp),
                        contentPadding = PaddingValues(start = 16.dp, end = 24.dp, top = 16.dp, bottom = 60.dp), // v7.4: header y 76, first row y 118
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        itemsIndexed(rows, key = { i, row -> "r${i}_${row.first().videoId ?: row.first().title}" }) { ri, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(ForgeDims.Gap)) {
                                row.forEachIndexed { ci, item ->
                                    val itemKey = item.videoId ?: item.title
                                    YtCard(
                                        item, resolving = resolving == item.videoId,
                                        onFocused = { viewModel.prefetch(item); lastVideoKey = itemKey },
                                        focusRequester = if (itemKey == lastVideoKey) lastVideo else if (lastVideoKey == null && ri == 0 && ci == 0) firstVideo else null,
                                        index = ri * 3 + ci, // v7.4 (12): karta.N, yt-tytul.N
                                    ) {
                                        viewModel.activate(item, cards, onPlay)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (searchOpen) BackHandler { searchOpen = false }
    }
}

@Composable
private fun YtCard(item: YtItem, resolving: Boolean, onFocused: () -> Unit, focusRequester: androidx.compose.ui.focus.FocusRequester? = null, index: Int = -1, onOk: () -> Unit) {
    fun tag(t: String): Modifier = if (index >= 0) Modifier.testTag("$t.$index") else Modifier
    Column(Modifier.width(ForgeDims.YouTubeCard)) {
        ForgeCard(
            width = ForgeDims.YouTubeCard,
            height = 146.dp, // v7.4: 259 x 146
            modifier = tag("karta"),
            focusRequester = focusRequester,
            onFocusChange = { if (it) onFocused() },
            onOk = onOk,
        ) {
            item.thumbnail?.takeIf { it.isNotBlank() }?.let { url ->
                AsyncImage(model = url, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            when {
                item.isLive -> Text(
                    "Na żywo", style = ForgeText.captionBold.copy(color = ForgeColors.OnFocus),
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(10.dp)).background(ForgeColors.Live).padding(horizontal = 8.dp, vertical = 2.dp),
                )
                !item.badge.isNullOrBlank() -> com.nuvio.tv.skin.kicia.components.ForgeBadge(item.badge.orEmpty(), com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Quality, Modifier.align(Alignment.BottomEnd).padding(8.dp)) // v7.4: czas = Plakietka w rogu
            }
            if (item.percentWatched in 1..100) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(ForgeColors.Surface3)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(item.percentWatched / 100f).background(ForgeColors.Live))
                }
            }
            if (resolving) {
                Text("Wczytuję…", style = ForgeText.captionBold, modifier = Modifier.align(Alignment.Center))
            }
        }
        Text(item.title, style = ForgeText.label, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp).fillMaxWidth().then(tag("yt-tytul"))) // v7.4 (12): tytuł 14/20 do 2 linii (1 linia = 20)
        val meta = listOfNotNull(item.author, item.meta ?: item.postTime).filter { it.isNotBlank() }.joinToString(" · ")
        if (meta.isNotBlank()) Text(meta, style = ForgeText.caption, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().then(tag("yt-linia"))) // v7.4 (12): linia 12/16, 70%
        if (item.kind != YtKind.VIDEO) Text(if (item.kind == YtKind.CHANNEL) "Kanał" else "Playlista", style = ForgeText.caption)
    }
}

/**
 * Home's "YouTube dla Ciebie" shelf (mockup S01): the first cards of the YouTube feed as wide cards.
 * OK opens the YouTube tab, which plays the video.
 */
@Composable
fun ForgeHomeYouTubeShelf(
    onFocused: () -> Unit,
    viewModel: YouTubeViewModel = forgeSharedYouTubeViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val cards = remember(state.rows, state.grid) { forgeYtCards(state.rows.map { it.items }, state.grid).filter { it.kind == YtKind.VIDEO }.take(15) }
    if (cards.isEmpty()) return
    com.nuvio.tv.skin.kicia.components.ForgeShelf(title = "YouTube dla Ciebie") {
        items(cards.size, key = { "yt_" + (cards[it].videoId ?: it.toString()) }) { i ->
            val item = cards[i]
            com.nuvio.tv.skin.kicia.components.ForgeWideCard(
                onFocusChange = { if (it) onFocused() },
                onOk = {
                    ForgeYouTubeLaunch.ask(item)
                    com.nuvio.tv.skin.kicia.nav.ForgeNavRequests.open(com.nuvio.tv.ui.navigation.Screen.YouTube.route)
                },
            ) {
                item.thumbnail?.takeIf { it.isNotBlank() }?.let { url ->
                    AsyncImage(model = url, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(0.4f to androidx.compose.ui.graphics.Color.Transparent, 1f to androidx.compose.ui.graphics.Color.Black.copy(alpha = .8f))))
                Text(item.title, style = ForgeText.label, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 12.dp, bottom = 10.dp))
            }
        }
    }
}

/** S11 YouTube search of the mockup (c11). */
@Composable
private fun ForgeYtSearchPanel(
    query: String,
    onQuery: (String) -> Unit,
    suggestions: List<String>,
    onSearch: (String) -> Unit,
    videos: List<YtItem>,
    onVideo: (YtItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstKey = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstKey.requestFocus() } }
    Column(modifier.padding(start = 12.dp, top = 30.dp, end = ForgeDims.ContentEnd)) {
        // Field: a glass pill with the search icon and what is typed.
        Row(
            Modifier.fillMaxWidth().height(44.dp).background(ForgeColors.Surface, androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(Icons.Outlined.Search, null, tint = ForgeColors.Text, modifier = Modifier.size(20.dp))
            Text(query.ifEmpty { "Szukaj w YouTube" }, style = ForgeText.button.copy(color = if (query.isEmpty()) ForgeColors.Text2 else ForgeColors.Text), maxLines = 1, modifier = Modifier.padding(start = 12.dp))
        }
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ForgeOnScreenKeyboard(
                text = query,
                onTextChange = onQuery,
                onSearch = { onSearch(query) },
                layout = ForgeKeyboardLayout.YouTube9,
                firstKeyFocus = firstKey,
                bare = true,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val rows = (listOfNotNull(query.takeIf { it.isNotBlank() }) + suggestions.filter { !it.equals(query, ignoreCase = true) }).take(6)
                rows.forEach { s ->
                    com.nuvio.tv.skin.kicia.components.ForgeFocusBox(radius = 16.5.dp, modifier = Modifier.height(com.nuvio.tv.skin.kicia.tokens.ForgeDims.PillSelectHeight), scaleOnFocus = false, onOk = { onSearch(s) }) { focused ->
                        Row(
                            Modifier.height(com.nuvio.tv.skin.kicia.tokens.ForgeDims.PillSelectHeight).background(if (focused) ForgeColors.Focus else androidx.compose.ui.graphics.Color.Transparent, androidx.compose.foundation.shape.RoundedCornerShape(16.5.dp)).padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.Icon(Icons.Outlined.Search, null, tint = if (focused) ForgeColors.OnFocus else ForgeColors.Text, modifier = Modifier.size(20.dp))
                            Text(s, style = ForgeText.button.copy(color = if (focused) ForgeColors.OnFocus else ForgeColors.Text), maxLines = 1, modifier = Modifier.padding(start = 14.dp))
                        }
                    }
                }
            }
        }
        if (videos.isNotEmpty()) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.padding(top = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(ForgeDims.Gap),
            ) {
                items(videos.size) { i ->
                    val item = videos[i]
                    YtCard(item, resolving = false, onFocused = {}, onOk = { onVideo(item) })
                }
            }
        }
    }
}

@Composable
private fun ForgeYtRailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onOk: () -> Unit, requester: androidx.compose.ui.focus.FocusRequester, tag: String = "") {
    var railFocused by remember { androidx.compose.runtime.mutableStateOf(false) }
    Box {
        com.nuvio.tv.skin.kicia.components.ForgeFocusBox(radius = 18.dp, modifier = Modifier.size(width = YtRailItemWidth, height = 36.dp).focusRequester(requester).then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier), scaleOnFocus = false, onFocusChange = { railFocused = it }, onOk = onOk) { focused ->
            Box(
                Modifier.fillMaxSize().background(
                    when { focused -> ForgeColors.Focus; active -> ForgeColors.Surface3; else -> androidx.compose.ui.graphics.Color.Transparent },
                    androidx.compose.foundation.shape.CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(icon, contentDescription = label, tint = if (focused) ForgeColors.OnFocus else ForgeColors.Text, modifier = Modifier.size(24.dp)) // v7.4: icon 66%
            }
        }
        // v7.4 (12, szyna-nazwa): the focused icon shows its name beside it (x 88, 36 high), over the grid, outside the layout
        if (railFocused) Text(
            label, style = ForgeText.label.copy(color = ForgeColors.OnFocus), maxLines = 1,
            modifier = Modifier.offset(x = YtRailItemWidth + 8.dp).wrapContentWidth(unbounded = true).height(36.dp)
                .background(ForgeColors.Focus, androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp).wrapContentHeight(Alignment.CenterVertically).testTag("szyna-nazwa.0"),
        )
    }
}

/**
 * One YouTube feed for the whole app (owner 9.11: preloaded at start): the YouTube tab, the Home shelf and the
 * background preload share the activity's view model, so the tab opens on a feed that is already there.
 */
@Composable
internal fun forgeSharedYouTubeViewModel(): YouTubeViewModel {
    var c: android.content.Context = androidx.compose.ui.platform.LocalContext.current
    while (c !is androidx.activity.ComponentActivity && c is android.content.ContextWrapper) c = c.baseContext
    val activity = c as? androidx.activity.ComponentActivity
    return if (activity != null) hiltViewModel(viewModelStoreOwner = activity) else hiltViewModel()
}

/** The sign-in screen of the YouTube tab: a button, then the QR and the code to enter on youtube.com/activate (phone or computer). */
@Composable
private fun ForgeYtSignIn(focus: androidx.compose.ui.focus.FocusRequester, onSignedIn: () -> Unit) {
    val account: com.nuvio.tv.youtube.YouTubeAccountViewModel = hiltViewModel()
    val acc by account.state.collectAsState()
    var initial by remember { androidx.compose.runtime.mutableStateOf(true) }
    LaunchedEffect(Unit) { account.refresh() }
    LaunchedEffect(acc.signedIn) { if (acc.signedIn) onSignedIn() }
    // No grab of the focus on entry (the bar's own ▼ lands here through the rail's enter rule); only when the button is swapped.
    LaunchedEffect(acc.code == null) { if (initial) initial = false else runCatching { focus.requestFocus() } }
    val qr = remember(acc.activateLink) { acc.activateLink?.let { runCatching { com.nuvio.tv.core.qr.QrCodeGenerator.generate(it, 400) }.getOrNull() } }
    Column(Modifier.fillMaxSize().padding(start = 12.dp, end = ForgeDims.ContentEnd), verticalArrangement = Arrangement.Center) {
        Text("Zaloguj się do YouTube", style = ForgeText.title)
        Text(
            "Zaloguj się, aby zobaczyć polecane, subskrypcje i historię.",
            style = ForgeText.body, modifier = Modifier.padding(top = 8.dp).width(520.dp),
        )
        Spacer(Modifier.height(20.dp))
        if (acc.code != null) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                if (qr != null) androidx.compose.foundation.Image(
                    bitmap = qr.asImageBitmap(), contentDescription = "Kod QR logowania",
                    modifier = Modifier.size(160.dp).clip(RoundedCornerShape(8.dp)),
                )
                Column {
                    Text("Zeskanuj kod QR telefonem albo wejdź na ${acc.url} i wpisz kod:", style = ForgeText.body, modifier = Modifier.width(420.dp))
                    Text(acc.code.orEmpty(), style = ForgeText.display, modifier = Modifier.padding(vertical = 12.dp))
                    com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Anuluj", focusRequester = focus, onOk = { account.cancelSignIn() })
                }
            }
        } else {
            com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Zaloguj się", primary = true, focusRequester = focus, onOk = { account.startSignIn() })
            acc.error?.let { Text(it, style = ForgeText.caption.copy(color = ForgeColors.Live), modifier = Modifier.padding(top = 10.dp)) }
        }
    }
}
