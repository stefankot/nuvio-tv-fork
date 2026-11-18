package com.nuvio.tv.skin.kicia.livetv

import androidx.compose.material.icons.outlined.Explore
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.fork.tv.TvStore
import com.nuvio.tv.fork.tv.RecentShow
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvUiState
import com.nuvio.tv.skin.kicia.components.ForgeAutoLogo
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.reshaped.livetv.FILTER_ALL
import com.nuvio.tv.ui.reshaped.livetv.FILTER_FAVORITES
import com.nuvio.tv.ui.reshaped.livetv.FILTER_LIST_PREFIX
import com.nuvio.tv.ui.reshaped.livetv.FILTER_SOURCE_GROUP_PREFIX
import com.nuvio.tv.ui.reshaped.livetv.FILTER_SOURCE_PREFIX
import com.nuvio.tv.ui.reshaped.livetv.LiveTvGuideState

/**
 * S07 guide, variant A of the mockup (the default, `e02`): above the grid the row of recently watched
 * programmes (196×110, y 84) and the line [● Dziś, niedz. 8 lis ▾][Wszystkie kanały]  Tytuł · 20:30—21:15 · ▶ od początku
 * (y 206). Drawn in place of Reshaped's guide header (picture, description, preview); the grid stays Reshaped's.
 */
@Composable
internal fun ForgeGuideHeaderA(
    guide: LiveTvGuideState,
    uiState: LiveTvUiState,
    gridFocused: Boolean,
    clock: State<Long>,
    filterKey: String,
    onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit,
    modifier: Modifier = Modifier,
    /** ↓ from the bar goes to the grid (S07: the recent row is reached with ↑ from the day line). */
    gridFocus: androidx.compose.ui.focus.FocusRequester? = null,
    /** Picks the channel list the guide shows (the "Wszystkie kanały" pill; replaces Reshaped's side menu). */
    onFilter: ((String) -> Unit)? = null,
    /** v7.4 (6.2): the tools row has the focus (the screen moves its content up). */
    onToolsFocus: ((Boolean) -> Unit)? = null,
) {
    var filterSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var searchOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var daySheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val recentShows by TvStore.recent.collectAsStateWithLifecycle()
    val byUrl = remember(uiState.channels) { uiState.channels.associateBy { it.streamUrl } }
    val byGuide = remember(uiState.channels) { HashMap<String, LiveTvChannel>().also { m -> uiState.channels.forEach { m.putIfAbsent(it.guideKey, it) } } }
    // Owner 10.11: only what is on LIVE now — first the favourite channels, then the recently watched ones.
    val shown = remember(recentShows, uiState.channels, uiState.currentProgrammes, uiState.favoriteUrls, byUrl, byGuide) {
        fun entry(ch: LiveTvChannel): Pair<RecentShow, LiveTvChannel>? {
            val p = uiState.currentProgrammes[ch.guideKey] ?: return null
            return RecentShow(
                key = ch.streamUrl, title = p.title, channelName = ch.name, channelUrl = ch.streamUrl, guideKey = ch.guideKey,
                logoUrl = ch.logoUrl, imageUrl = p.image, lastViewedMs = p.startEpochMs,
            ) to ch
        }
        val favourites = uiState.channels.filter { it.streamUrl in uiState.favoriteUrls }.mapNotNull(::entry)
        val recent = recentShows.sortedByDescending { it.lastViewedMs }
            .mapNotNull { byUrl[it.channelUrl] ?: byGuide[it.guideKey] }
            .distinctBy { it.streamUrl }
            .filter { it.streamUrl !in uiState.favoriteUrls }
            .mapNotNull(::entry)
        (favourites + recent).take(24)
    }
    val nowMs = clock.value
    // fork (19.11): „Dla Ciebie” (fork/tv) w tej półce zamiast osobnego rzędu, który ucinał przyciski pod półką.
    val forYouAll = com.nuvio.tv.fork.tv.ekrany.rememberForYouItems()
    val forYou = remember(forYouAll, shown) { val onShelf = shown.map { it.second.guideKey }.toSet(); forYouAll.filter { it.channel.guideKey !in onShelf }.take(12) }
    // makieta 7.7b H16: „Kontynuuj oglądanie” first — programmes left part way in the archive (still in it), newest first;
    // OK = resume from where it was left. They lead the shelf (the separate row and its title come with the layout pass).
    val resumeAll by TvStore.resume.collectAsStateWithLifecycle()
    val resumes = remember(resumeAll, byUrl, nowMs / 60_000) {
        // test 15.11 (JMGO): „resumes=0 of 6 (started=2)” — the channel is found by its guide key too, and the archive
        // window is the channel's days counted on the device clock (the guide clock may run apart)
        val deviceNow = System.currentTimeMillis()
        resumeAll.filter { it.started }.distinctBy { it.channelUrl + "|" + it.title + "|" + it.episode }.mapNotNull { r -> // test 15.11: one card per programme
            val ch = byUrl[r.channelUrl] ?: byGuide[r.guideKey]
            val days = ch?.catchup?.days
            val ok = ch != null && days != null && deviceNow - r.positionMs <= days * 86_400_000L + 86_400_000L
            if (!ok) android.util.Log.i("TvStore", "shelf skip '${r.title}': channel=${ch != null} catchupDays=$days")
            if (ok) r to ch!! else null
        }.take(12).also { android.util.Log.i("TvStore", "shelf resumes=${it.size} of ${resumeAll.size} (started=${resumeAll.count { r -> r.started }})") }
    }
    // A channel tile on Home asked for this channel: start it as soon as the list is here.
    val pendingPlay by ForgePendingPlay.request.collectAsStateWithLifecycle()
    // makieta 7.7 G31: a resume from Home; test 15.11 (JMGO): it waits for the guide (before it the archive fell back to live)
    androidx.compose.runtime.LaunchedEffect(pendingPlay, byUrl, uiState.isEpgLoading, uiState.currentProgrammes.isEmpty()) {
        if (pendingPlay?.programme != null && (uiState.isEpgLoading || uiState.currentProgrammes.isEmpty())) return@LaunchedEffect
        ForgePendingPlay.take(byUrl, byGuide = byGuide)?.let { onPlay(it, ForgePendingPlay.lastProgramme) }
    }
    // Frames of what is on (owner 8.11): the recent row's channels first, then the guide rows around the focus.
    val frames by ForgeChannelSnapshots.frames.collectAsStateWithLifecycle()
    val snapChannels = remember(shown, guide.channels, guide.row) {
        val near = guide.channels.drop((guide.row - 2).coerceAtLeast(0)).take(8)
        (shown.map { it.second } + near).distinctBy { it.guideKey }.take(14)
    }
    Box(Modifier.requiredSize(0.dp)) { ForgeSnapshotter(snapChannels) } // invisible, takes no room
    // v7.4 (6.2, 6.7, W-68): ▼ from the shelf and ▲ from the grid land on the tool used last (default "Kategorie"), never on the days
    val searchTool = remember { androidx.compose.ui.focus.FocusRequester() }
    val categoriesTool = remember { androidx.compose.ui.focus.FocusRequester() }
    var lastToolIsSearch by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) } // v7.6: default "Szukaj programu"
    val lastTool = if (lastToolIsSearch) searchTool else categoriesTool
    val firstRecent = remember { androidx.compose.ui.focus.FocusRequester() }
    androidx.compose.runtime.SideEffect { ForgeGuideFocus.lastTool = lastTool }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { ForgeGuideFocus.lastTool = null } }
    Column(modifier.fillMaxWidth()) {
        // v7.4 (06): content from y 68 (below the bar): title "Na żywo" y 76, cards y 110 (160x114, focus 180x128), tools y 252, programme header y 292, grid y 341.
        Spacer(Modifier.height(8.dp))
        if (shown.isNotEmpty() || resumes.isNotEmpty() || forYou.isNotEmpty()) {
            // v7.6 (owner 13.11): no shelf title "Na żywo"; the cards say it with a blinking red dot
            Spacer(Modifier.height(5.dp))
            // makieta 7.7b H16: a resume card added in front would sit left of the screen (the row keeps its first visible card)
            val shelfState = androidx.compose.foundation.lazy.rememberLazyListState()
            androidx.compose.runtime.LaunchedEffect(resumes.size) { if (resumes.isNotEmpty()) shelfState.scrollToItem(0) }
            LazyRow(
                state = shelfState,
                modifier = Modifier.height(128.dp).offset(x = (-10).dp) // v7.4: the first card at x 40, the focused one grows to x 30
                    // Owner 9.11: ↓ from the top bar lands on these previews, not straight in the guide grid.
                    .focusProperties {
                        gridFocus?.let { down = it } // v7.6 (owner 13.11): ▼ from the shelf goes straight to the guide; ▲ from the guide lands on "Szukaj programu"
                        @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                        enter = { _ -> firstRecent } // v7.4 (6.1): ▼ z paska zawsze na pierwszą kartę, nie na najbliższą
                    }
                    .focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(start = 10.dp, end = 58.dp), // 10 dp = the focused card grows 10 dp to the left
            ) {
                itemsIndexed(resumes, key = { _, it -> "resume|" + it.first.key }) { i, (r, channel) ->
                    ForgeRecentProgrammeCard(
                        show = RecentShow(
                            key = r.key, title = r.title, channelName = channel.name + " · zostało " + r.leftMinutes + " min", channelUrl = channel.streamUrl,
                            guideKey = channel.guideKey, logoUrl = channel.logoUrl, imageUrl = r.image, lastViewedMs = r.updatedMs,
                        ),
                        frame = r.image,
                        episode = r.episode, // fork W8
                        channel = channel,
                        live = false,
                        nowMs = nowMs,
                        progress = r.fraction,
                        onClick = { onPlay(channel, LiveTvProgramme(r.title, r.positionMs, r.stopMs, image = r.image, episode = r.episode)) },
                        onLongClick = { ForgeGuideDetail.open(channel, LiveTvProgramme(r.title, r.startMs, r.stopMs, image = r.image, episode = r.episode)) },
                        modifier = Modifier.focusProperties { gridFocus?.let { down = it } }.then(if (i == 0) Modifier.focusRequester(firstRecent) else Modifier),
                    )
                }
                itemsIndexed(forYou, key = { i, it -> "foryou|$i|" + it.item.programKey + "|" + it.item.guideKey }) { i, r ->
                    val p = r.programme
                    ForgeRecentProgrammeCard(
                        show = RecentShow(
                            key = "foryou|" + r.item.programKey, title = r.item.title,
                            channelName = com.nuvio.tv.fork.tv.ekrany.forYouBadgeLabel(r.item.badge) + " · " + r.channel.name,
                            channelUrl = r.channel.streamUrl, guideKey = r.channel.guideKey, logoUrl = r.channel.logoUrl,
                            imageUrl = p?.image, lastViewedMs = r.item.startMs,
                        ),
                        frame = p?.image?.takeIf { it.isNotBlank() } ?: frames[r.channel.guideKey]?.second,
                        episode = p?.episode, // fork W8
                        channel = r.channel,
                        live = r.item.badge == com.nuvio.tv.fork.tv.Badge.LIVE,
                        nowMs = nowMs,
                        onClick = { onPlay(r.channel, p) },
                        onLongClick = { com.nuvio.tv.ui.screens.settings.ForkFeatures.open?.invoke("fork_program/" + android.net.Uri.encode(r.item.programKey)) },
                        modifier = Modifier.focusProperties { gridFocus?.let { down = it } }.then(if (i == 0 && resumes.isEmpty()) Modifier.focusRequester(firstRecent) else Modifier),
                    )
                }
                itemsIndexed(shown, key = { _, it -> it.first.key }) { cardIndex, (show, channel) ->
                    val onNow = uiState.currentProgrammes[channel.guideKey]
                    val live = onNow != null && forgeSameTitle(onNow.title, show.title)
                    ForgeRecentProgrammeCard(
                        show = show,
                        frame = onNow?.image?.takeIf { it.isNotBlank() } ?: frames[channel.guideKey]?.second, // owner 9.11: the EPG's picture first, a stream frame only when the EPG has none
                        episode = onNow?.episode, // fork W8
                        channel = channel,
                        live = live,
                        nowMs = nowMs,
                        progress = if (live && onNow != null && onNow.stopEpochMs > onNow.startEpochMs) ((nowMs - onNow.startEpochMs).toFloat() / (onNow.stopEpochMs - onNow.startEpochMs)).coerceIn(0f, 1f) else 0f,
                        onClick = { onPlay(channel, null) },
                        onLongClick = { TvStore.remove(show.key) },
                        modifier = Modifier.testTag("karta.$cardIndex").focusProperties { gridFocus?.let { down = it } }.then(if (cardIndex == 0 && resumes.isEmpty() && forYou.isEmpty()) Modifier.focusRequester(firstRecent) else Modifier), // v7.4 (06, 6.2: ▼ na ostatnie narzędzie)
                        badgeTag = "plakietka.$cardIndex",
                    )
                }
            }
            Spacer(Modifier.height(21.dp))
        } else {
            Spacer(Modifier.height(5.dp + 128.dp + 21.dp))
        }
        // [● Dziś, niedz. 8 lis ▾] [Wszystkie kanały]  info
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(com.nuvio.tv.skin.kicia.tokens.ForgeDims.PillSelectHeight).onFocusChanged { onToolsFocus?.invoke(it.hasFocus) }) { // v7.4 (6.2)
            // v7 (06, W-57/W-58): left "Szukaj" and "Kategorie: ...", the day button on the right.
            com.nuvio.tv.skin.kicia.components.ForgePillSelect(
                text = "Szukaj programu", selected = false, bare = false, modifier = Modifier.testTag("przycisk-maly.0"), // v7.4 (06); v7.6: opens the programme view (search is its button)
                focusRequester = searchTool, onFocusChange = { if (it) lastToolIsSearch = true }, // v7.4 (6.2)
                icon = Icons.Filled.Search,
                onOk = { ForgeProgrammeView.upcoming = false; ForgeProgrammeView.open(ForgeEpgCategories.PROG_ALL) },
            )
            Spacer(Modifier.width(12.dp))
            com.nuvio.tv.skin.kicia.components.ForgePillSelect(
                text = "Kategorie: " + forgeFilterLabel(filterKey, uiState), // v7.6: channel categories only (programmes: "Szukaj programu")
                selected = false, bare = false, modifier = Modifier.testTag("przycisk-maly.1"), // v7.4 (06); owner 13.11: buttons with a fill like "Dziś" (mockup: przycisk-maly, --wyp-12)
                focusRequester = categoriesTool, onFocusChange = { if (it) lastToolIsSearch = false }, // v7.4 (6.2)
                icon = Icons.Outlined.FormatListBulleted, // v7.4: list icon, no chevron
                onOk = { if (onFilter != null) filterSheet = true },
            )
            // Fork 19.11 (właściciel: „Narzędzia” w najodpowiedniejsze miejsca): „Odkrywaj telewizję” obok narzędzi przewodnika.
            if (com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(androidx.compose.ui.platform.LocalContext.current, "module_discover", true)) {
                Spacer(Modifier.width(12.dp))
                com.nuvio.tv.skin.kicia.components.ForgePillSelect(
                    text = "Odkrywaj", selected = false, bare = false, icon = Icons.Outlined.Explore,
                    onOk = { com.nuvio.tv.ui.screens.settings.ForkFeatures.open?.invoke("fork_discover") },
                )
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            com.nuvio.tv.skin.kicia.components.ForgeFocusBox(radius = 18.dp, modifier = Modifier.height(com.nuvio.tv.skin.kicia.tokens.ForgeDims.PillSelectHeight).testTag("przycisk-maly.2").focusRequester(ForgeGuideFocus.rememberDayRow()), scaleOnFocus = false, onOk = { daySheet = true }) { focused ->
                val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
                Row(
                    Modifier.height(com.nuvio.tv.skin.kicia.tokens.ForgeDims.PillSelectHeight).clip(RoundedCornerShape(18.dp)).background(if (focused) ForgeColors.Focus else ForgeColors.Surface).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.CalendarToday, null, tint = fg, modifier = Modifier.padding(end = 6.dp).size(16.dp)) // v7.4
                    Text(forgeGuideDayShort(guide.anchorMs, nowMs), style = ForgeText.label.copy(color = fg))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        // v7.4: header of the programme under the focus: title 20/700 + availability badge, then "hh:mm–hh:mm · description" (14, 70%)
        val tile = ForgeProgrammeView.focusedTile.takeIf { ForgeProgrammeView.active && ForgeProgrammeView.focused } // JMGO uwaga 2
        val hChannel = tile?.first ?: guide.channel
        val hProgramme = tile?.second ?: (if (gridFocused) guide.selected() else null) ?: hChannel?.guideKey?.let(uiState.currentProgrammes::get)
        if (hProgramme != null && hChannel != null) {
            val past = hProgramme.stopEpochMs <= nowMs
            val future = hProgramme.startEpochMs > nowMs
            Row(Modifier.height(26.dp).padding(start = 0.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(hProgramme.title, style = ForgeText.title.copy(fontSize = 20.sp, lineHeight = 26.sp), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).testTag("opis-tytul.0")) // v7.4 (06)
                val (label, kind) = when {
                    future -> "Wkrótce" to com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Soon
                    past && hChannel.catchup != null -> "Archiwum" to com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Archive
                    past -> "Niedostępne w archiwum" to com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Unavailable
                    else -> "Na żywo" to com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Live
                }
                if (kind == com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Live) com.nuvio.tv.skin.kicia.components.ForgeLiveDot(Modifier.padding(start = 12.dp).testTag("opis-pl.0")) // v7.6: a dot instead of the badge
                else com.nuvio.tv.skin.kicia.components.ForgeBadge(label, kind, Modifier.padding(start = 12.dp).testTag("opis-pl.0")) // v7.4 (06)
            }
            Text(
                forgeTvSlot(hProgramme.startEpochMs, hProgramme.stopEpochMs).replace("—", "–") + (hProgramme.description?.takeIf { it.isNotBlank() }?.let { " · " + it.replace('\n', ' ') } ?: ""),
                style = ForgeText.caption.copy(fontSize = 14.sp, lineHeight = 20.sp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(20.dp).fillMaxWidth().testTag("opis-meta.0"), // v7.4 (06)
            )
        }
    }
    if (searchOpen) ForgeTvSearch(channels = uiState.channels, onPlay = onPlay, onClose = { searchOpen = false }, host = "naglowek")
    ForgeTvSearchReturn("naglowek") { searchOpen = true } // forge fix 14.11 (JMGO A3): back from the details or the player = the results again
    ForgeGuideDetailHost(onPlay)
    if (filterSheet && onFilter != null) ForgeFilterSheet(uiState, filterKey, onPick = onFilter, onDismiss = { filterSheet = false })
    if (daySheet) ForgeDaySheet(guide.anchorMs, nowMs, onPick = { offset ->
        if (offset == 0) guide.backToNow() else guide.forgeJumpTo(forgeGuideDayAnchor(nowMs, offset))
    }, onDismiss = { daySheet = false })
}

/** Card of a recently watched programme (S07 A, S06): logo on top, "Na żywo" when on now, ▶ title, "dziś 20:10 · kanał". */
@Composable
internal fun ForgeRecentProgrammeCard(
    show: RecentShow,
    frame: Any?,
    episode: String? = null, // fork W8: obraz odcinka z TVDB, gdy przewodnik go nie ma
    channel: LiveTvChannel,
    live: Boolean,
    nowMs: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    badgeTag: String = "", // v7.4 (06)
) {
    rememberForgeLogoVersion() // forge: redraw when the guide's logos arrive
    val shape = RoundedCornerShape(8.dp)
    var cardFocused by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = Modifier.padding(vertical = 7.dp).width(160.dp).height(114.dp)
            .onFocusChanged { cardFocused = it.hasFocus }.then(modifier),
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(containerColor = ForgeColors.Surface2, focusedContainerColor = ForgeColors.Surface2),
        scale = CardDefaults.scale(focusedScale = 1.125f),
        border = CardDefaults.border(focusedBorder = Border.None), // v7.4: focus is never an outline (scale 1.125 + shadow)
    ) {
        Box(Modifier.fillMaxSize()) {
            val image: Any? = frame ?: show.imageUrl?.takeIf { it.isNotBlank() } ?: com.nuvio.tv.fork.tvdb.rememberTvdbCardImage(show.title, episode) // fork W8
            if (image != null) {
                AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            // Owner 9.11: the focused card plays its channel live, muted (whatever is on the channel now).
            if (cardFocused && com.nuvio.tv.skin.kicia.ForgeUi.LIVE_CARD_PREVIEW) ForgeLiveCardVideo(channel, Modifier.fillMaxSize())
            val logo = forgeChannelLogo(channel) ?: show.logoUrl
            if (!logo.isNullOrBlank() && !(cardFocused && com.nuvio.tv.skin.kicia.ForgeUi.LIVE_CARD_PREVIEW)) {
                // Freely: the logo in the top right corner of the picture, in a soft shadow; "Na żywo" top left.
                com.nuvio.tv.skin.kicia.components.ForgeLogoWithShadow(url = logo, contentDescription = channel.name, modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp).size(54.dp, 28.dp))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = .78f))))
            if (live) com.nuvio.tv.skin.kicia.components.ForgeLiveDot(Modifier.padding(10.dp).then(if (badgeTag.isNotEmpty()) Modifier.testTag(badgeTag) else Modifier)) // v7.6: blinking red dot// v7.4
            if (live) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = .24f))) { Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color.White)) }
            Column(Modifier.align(Alignment.BottomStart).padding(start = 8.dp, end = 8.dp, bottom = 12.dp)) {
                Text(show.title, style = ForgeText.label.copy(fontSize = 14.sp, lineHeight = 20.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(channel.name, style = ForgeText.caption.copy(fontSize = 12.sp, lineHeight = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

internal fun forgeSameTitle(a: String, b: String): Boolean = a.trim().equals(b.trim(), ignoreCase = true)

/** v7 (W-82): "Dziś" or "czw 12". */
internal fun forgeGuideDayShort(ms: Long, nowMs: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String {
    val d = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    if (d == java.time.Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()) return "Dziś"
    return listOf("pon", "wt", "śr", "czw", "pt", "sob", "niedz")[d.dayOfWeek.value - 1] + " " + d.dayOfMonth
}

/** "Dziś, niedz. 8 lis" / "Wczoraj, sob. 7 lis" / "pon. 10 lis". */
internal fun forgeGuideDay(ms: Long, nowMs: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String {
    val d = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    val today = java.time.Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    val day = listOf("pon.", "wt.", "śr.", "czw.", "pt.", "sob.", "niedz.")[d.dayOfWeek.value - 1]
    val month = listOf("sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru")[d.monthValue - 1]
    val date = "$day ${d.dayOfMonth} $month"
    return when (d) {
        today -> "Dziś, $date"
        today.minusDays(1) -> "Wczoraj, $date"
        today.plusDays(1) -> "Jutro, $date"
        else -> date
    }
}

/** The name of the channel list the guide shows. */
internal fun forgeFilterLabel(key: String, uiState: LiveTvUiState): String = when {
    key == FILTER_ALL -> "Wszystkie kanały"
    key == FILTER_FAVORITES -> "Ulubione"
    key.startsWith(FILTER_LIST_PREFIX) -> key.removePrefix(FILTER_LIST_PREFIX).let { id -> uiState.customLists.firstOrNull { it.id == id }?.name ?: "Lista" }
    key.startsWith(FILTER_SOURCE_GROUP_PREFIX) -> key.removePrefix(FILTER_SOURCE_GROUP_PREFIX).substringAfter('\u0000')
    key.startsWith(FILTER_SOURCE_PREFIX) -> key.removePrefix(FILTER_SOURCE_PREFIX).let { id -> uiState.sources.firstOrNull { it.id == id }?.label ?: "Źródło" }
    ForgeEpgCategories.isKey(key) -> ForgeEpgCategories.nameOf(key)
    else -> key
}

/** One row of the category panel: key, name, count, and the branch it opens (second level of the tree). */
internal data class ForgeFilterRow(val key: String, val name: String, val count: Int, val opens: String? = null)

/**
 * Rows of the category panel (mockup v7.4, 06 / 6.11; owner 13.11). [programmes] = tab "Programy", [branch] = the opened
 * branch (its key). Kanały: all, favourites, the viewer's lists, then the channel tree; Programy: all programmes, then
 * the programme tree. Inside a branch: "Wszystkie: …" and its leaves.
 */
internal fun forgeFilterRows(uiState: LiveTvUiState, programmes: Boolean, branch: String?): List<ForgeFilterRow> = buildList {
    val tree = if (programmes) ForgeEpgCategories.programmeTree(uiState.channels) else ForgeEpgCategories.channelTree(uiState.channels, uiState.hiddenGroups)
    if (branch != null) {
        val node = tree.firstOrNull { it.key == branch } ?: return@buildList
        add(ForgeFilterRow(node.key, "Wszystkie: " + node.name, node.count))
        node.children.forEach { add(ForgeFilterRow(it.key, it.name, it.count)) }
        return@buildList
    }
    if (programmes) {
        add(ForgeFilterRow(ForgeEpgCategories.PROG_ALL, "Wszystkie programy", tree.sumOf { it.count }))
    } else {
        add(ForgeFilterRow(FILTER_ALL, "Wszystkie kanały", uiState.channels.size))
        if (uiState.favoriteUrls.isNotEmpty()) add(ForgeFilterRow(FILTER_FAVORITES, "Ulubione", uiState.channels.count { it.streamUrl in uiState.favoriteUrls }))
        uiState.customLists.forEach { add(ForgeFilterRow(FILTER_LIST_PREFIX + it.id, it.name, it.urls.size)) }
    }
    tree.forEach { n -> add(ForgeFilterRow(n.key, n.name, n.count, opens = n.key.takeIf { n.children.isNotEmpty() })) }
}

/** The list picker over the whole screen (its own window, so the guide's layout never clips it). */
@Composable
internal fun ForgeFilterSheet(
    uiState: LiveTvUiState,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    /** v7.6: opened from the Live TV search (its button „Kategoria: …”): the „Programy” tab first. */
    startOnProgrammes: Boolean = false,
    /** v7.6: a programme category goes here instead of opening the programme view (the search decides). */
    onProgrammePick: ((String) -> Unit)? = null,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val firstRow = remember { androidx.compose.ui.focus.FocusRequester() }
        val shownSelection = if (onProgrammePick != null) selected else ForgeProgrammeView.selection ?: selected
        var programmes by remember { androidx.compose.runtime.mutableStateOf(startOnProgrammes || ForgeProgrammeView.selection != null) }
        val branchOf = { key: String -> if ((ForgeEpgCategories.isKey(key) || ForgeEpgCategories.isProgrammeKey(key)) && '/' in key) key.substringBefore('/') else null }
        var branch by remember { androidx.compose.runtime.mutableStateOf(branchOf(shownSelection)) }
        var returnTo by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
        val rows = remember(programmes, branch, uiState.customLists, uiState.hiddenGroups, uiState.favoriteUrls.size, uiState.guideVersion, uiState.channels, ForgeEpgCategories.programmeVersion()) { forgeFilterRows(uiState, programmes, branch) }
        val focusKey = returnTo?.takeIf { k -> rows.any { it.key == k } } ?: shownSelection.takeIf { k -> rows.any { it.key == k } } ?: rows.firstOrNull()?.key
        // The rows are composed a moment after the panel (and again on each level and tab); the focus goes to the chosen one as soon as it exists.
        androidx.compose.runtime.LaunchedEffect(programmes, branch, rows.size) { repeat(10) { kotlinx.coroutines.delay(60); if (runCatching { firstRow.requestFocus() }.getOrDefault(false)) return@LaunchedEffect } }
        val title = branch?.let { ForgeEpgCategories.nameOf(it) } ?: "Kategorie"
        com.nuvio.tv.skin.kicia.components.ForgeSideSheet(visible = true, onDismiss = onDismiss, title = title, initialFocus = firstRow) { // v7.4 (06): Panel kategorii
            // After the sheet's own Back (composed first), so inside a branch Back goes up a level (6.11: W = poziom wyżej).
            androidx.activity.compose.BackHandler(enabled = branch != null) { returnTo = branch; branch = null }
            if (branch == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.nuvio.tv.skin.kicia.components.ForgePillSelect(text = "Kanały", selected = !programmes, bare = true, onOk = { programmes = false; returnTo = null })
                    com.nuvio.tv.skin.kicia.components.ForgePillSelect(text = "Programy", selected = programmes, bare = true, onOk = { programmes = true; returnTo = null })
                }
                Spacer(Modifier.height(8.dp))
            }
            val listState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = rows.indexOfFirst { it.key == focusKey }.coerceAtLeast(0))
            androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rows, key = { it.key + "|" + (it.opens ?: "") }) { row ->
                    com.nuvio.tv.skin.kicia.components.ForgeSheetRow(
                        text = row.name, detail = row.count.toString(), tile = true, focusRequester = if (row.key == focusKey) firstRow else null,
                        trailingIcon = if (row.opens != null) Icons.Filled.ChevronRight else if (row.key == shownSelection) Icons.Filled.Check else null,
                    ) {
                        when {
                            row.opens != null -> { returnTo = null; branch = row.opens }
                            ForgeEpgCategories.isProgrammeKey(row.key) && onProgrammePick != null -> { onDismiss(); onProgrammePick(row.key) }
                            ForgeEpgCategories.isProgrammeKey(row.key) -> { ForgeProgrammeView.open(row.key); onDismiss() }
                            else -> { ForgeProgrammeView.close(); onPick(row.key); onDismiss() }
                        }
                    }
                }
            }
        }
    }
}

/**
 * v7 (06): the day picker = 5 day cards (56x64, +-2 around the chosen day), left/right moves the window over -7..+7,
 * OK picks. The card of the chosen day is filled 24%; the focused one `#EBEBE8`.
 */
@Composable
internal fun ForgeDaySheet(anchorMs: Long, nowMs: Long, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val current = forgeDayOffset(anchorMs, nowMs)
        var center by remember { androidx.compose.runtime.mutableStateOf(current.coerceIn(-5, 5)) }
        val focusChosen = remember { androidx.compose.ui.focus.FocusRequester() }
        androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { focusChosen.requestFocus() } }
        androidx.activity.compose.BackHandler(onBack = onDismiss)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp)).background(ForgeColors.Sheet).padding(16.dp)
                    .onPreviewKeyEvent { e ->
                        if (e.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) false
                        else when (e.key) {
                            androidx.compose.ui.input.key.Key.DirectionLeft -> if (center > -5) { center--; true } else false
                            androidx.compose.ui.input.key.Key.DirectionRight -> if (center < 5) { center++; true } else false
                            else -> false
                        }
                    }
                    .focusGroup(),
            ) {
                for (d in (center - 2)..(center + 2)) {
                    val day = d.coerceIn(-7, 7)
                    val ms = forgeGuideDayAnchor(nowMs, day)
                    val zone = java.time.ZoneId.systemDefault()
                    val date = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
                    val wd = listOf("pon", "wt", "śr", "czw", "pt", "sob", "niedz")[date.dayOfWeek.value - 1]
                    com.nuvio.tv.skin.kicia.components.ForgeFocusBox(
                        radius = 8.dp,
                        modifier = Modifier.size(56.dp, 64.dp).then(if (d == center) Modifier.focusRequester(focusChosen) else Modifier),
                        scaleOnFocus = false,
                        onOk = { onPick(day); onDismiss() },
                    ) { focused ->
                        val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
                        Column(
                            Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                .background(if (focused) ForgeColors.Focus else if (day == current) ForgeColors.Surface3 else ForgeColors.Surface2),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            Text(if (day == 0) "Dziś" else wd, style = ForgeText.caption.copy(color = fg))
                            Text(date.dayOfMonth.toString(), style = ForgeText.title.copy(color = fg, fontSize = 20.sp, lineHeight = 26.sp))
                        }
                    }
                }
            }
        }
    }
}

/** Whole days between [ms] and [nowMs] in the device's zone (0 = today). */
internal fun forgeDayOffset(ms: Long, nowMs: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Int {
    val a = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    val b = java.time.Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    return java.time.temporal.ChronoUnit.DAYS.between(b, a).toInt()
}

/** Long OK on a programme in the guide (S07 → S08). */
internal object ForgeGuideDetail {
    var request by androidx.compose.runtime.mutableStateOf<Pair<LiveTvChannel, LiveTvProgramme>?>(null)
    fun open(channel: LiveTvChannel, programme: LiveTvProgramme) { request = channel to programme }
}

@Composable
private fun ForgeGuideDetailHost(onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit) {
    val (channel, programme) = ForgeGuideDetail.request ?: return
    val close = { ForgeGuideDetail.request = null }
    val now = System.currentTimeMillis()
    androidx.compose.ui.window.Dialog(onDismissRequest = close, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        ForgeProgramDetail(
            channel = channel,
            programme = programme,
            onWatchLive = { close(); onPlay(channel, null) },
            onWatchFromStart = if (channel.catchup != null && programme.startEpochMs < now) ({ close(); onPlay(channel, programme) }) else null,
            onDismiss = close,
            onPlayAiring = { airing -> close(); onPlay(channel, airing) }, // forge fix 15.11 (JMGO): any archived episode plays
        )
    }
}
