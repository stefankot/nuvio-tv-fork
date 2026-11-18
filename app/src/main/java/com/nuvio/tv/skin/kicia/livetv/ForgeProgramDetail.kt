package com.nuvio.tv.skin.kicia.livetv

import androidx.activity.compose.BackHandler
import com.nuvio.tv.skin.kicia.nav.forgeTagRoot
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.kicia.components.ForgeAutoLogo
import com.nuvio.tv.skin.kicia.components.ForgeButton
import com.nuvio.tv.skin.kicia.components.ForgeIconButton
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.nuvio.tv.skin.kicia.components.ForgeWideCard
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/** Other airings of the same title on the channel, and what comes next on it (mockup S08). */
internal data class ForgeAirings(val others: List<LiveTvProgramme>, val next: List<LiveTvProgramme>)

internal fun forgeAirings(schedule: List<LiveTvProgramme>, programme: LiveTvProgramme, nowMs: Long, windowMs: Long = 7L * 86_400_000): ForgeAirings {
    val title = programme.title.trim().lowercase()
    val inWindow = schedule.filter { it.stopEpochMs > nowMs - windowMs && it.startEpochMs < nowMs + windowMs }
    val others = inWindow.filter { it.title.trim().lowercase() == title && it.startEpochMs != programme.startEpochMs }.sortedBy { it.startEpochMs }
    val next = inWindow.filter { it.startEpochMs >= programme.stopEpochMs }.sortedBy { it.startEpochMs }.take(12)
    return ForgeAirings(others, next)
}

/**
 * S08 Szczegóły programu (mockup), a full-screen layer over Live TV: channel logo, title, time · channel ·
 * "Na żywo", whether it can be watched from the start, the description, [Oglądaj na żywo][Od początku] and
 * "Inne emisje" / "Dalej na {kanał}" from the channel's own schedule.
 */
@Composable
fun ForgeProgramDetail(
    channel: LiveTvChannel,
    programme: LiveTvProgramme,
    onWatchLive: () -> Unit,
    onWatchFromStart: (() -> Unit)?,
    onDismiss: () -> Unit,
    /** v6: the MiniGuide's "Info i odcinki" opens as a drawer under the picture, not a full screen. */
    compact: Boolean = false,
) {
    if (compact) { ForgeProgramDetailV6(channel, programme, onWatchLive, onWatchFromStart, onDismiss, compact = true); return }
    ForgeProgramCard(channel, programme, onWatchLive, onWatchFromStart, onDismiss)
}

/**
 * v7.4 (08 Info i odcinki): the programme's card in the look of Szczegóły (spec 04). Hero from the bottom, x 40: title 40/700
 * (y 278), meta 14, description 14 (3 lines, 300), availability badges, buttons y 440 (main, "Od początku" with archive only,
 * "Więcej odcinków", ★). ▼ goes to the section "Odcinki i emisje": the hero gives way to the title centred at y 140,
 * the section header at y 248, cards 160 x 114 at y 294 with the airing in the background at x 40.
 */
@Composable
private fun ForgeProgramCard(
    channel: LiveTvChannel,
    programme: LiveTvProgramme,
    onWatchLive: () -> Unit,
    onWatchFromStart: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val now = remember { System.currentTimeMillis() }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val airings = remember(channel.guideKey, programme) { forgeAirings(LiveTvRepository.schedule(channel.guideKey), programme, now) }
    val live = programme.startEpochMs <= now && now < programme.stopEpochMs
    val future = programme.startEpochMs > now
    val liveTv by LiveTvRepository.uiState.collectAsState()
    val metaVm: com.nuvio.tv.fork.tv.TvStoreMetaViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val extra = remember(programme) { com.nuvio.tv.fork.tv.EpgExtras.get(programme.title, programme.startEpochMs) }
    LaunchedEffect(programme.title) { metaVm.load(programme.title, extra?.year) }
    val tmdb by metaVm.meta.collectAsState()
    val favorite = channel.streamUrl in liveTv.favoriteUrls
    LaunchedEffect(Unit) { com.nuvio.tv.fork.tv.ForgeReminders.load(ctx.applicationContext) }
    val reminders by com.nuvio.tv.fork.tv.ForgeReminders.list.collectAsState()
    val reminded = reminders.any { it.channelUrl == channel.streamUrl && it.startMs == programme.startEpochMs }

    // One row in time order: past airings, the one in the background, future ones (spec 08). Without other airings: "Emisje" = what follows.
    val episodes = remember(airings, programme) {
        if (airings.others.isNotEmpty()) (airings.others + programme).distinctBy { it.startEpochMs }.sortedBy { it.startEpochMs }
        else listOf(programme) + airings.next
    }
    val current = episodes.indexOfFirst { it.startEpochMs == programme.startEpochMs }.coerceAtLeast(0)
    val hasEpisodes = episodes.any { forgeEpisodeLabel(it.episode) != null }

    val main = remember { FocusRequester() }
    val currentCard = remember { FocusRequester() }
    var lastButton by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(main) }
    var inSections by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var moreOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { main.requestFocus() } }
    BackHandler {
        when {
            moreOpen -> { moreOpen = false }
            inSections -> runCatching { lastButton.requestFocus() }
            else -> onDismiss()
        }
    }
    val p by androidx.compose.animation.core.animateFloatAsState(if (inSections || moreOpen) 1f else 0f, com.nuvio.tv.skin.kicia.tokens.ForgeMotion.scrollSpec(), label = "forgeProgramSections")
    val sectionsShown by androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { p > 0.5f } } // which state the layout tags describe
    fun tagIf(on: Boolean, tag: String): Modifier = if (on) Modifier.testTag(tag) else Modifier

    Box(Modifier.fillMaxSize().background(ForgeColors.Bg).forgeTagRoot()) { // v7.4: tagi widoczne w uiautomator (warstwa nad przewodnikiem)
        // Tło tytułu: the programme's picture (the guide's first, TMDB's only when the guide has none), gradient to bg at the bottom.
        (programme.image?.takeIf { it.isNotBlank() } ?: tmdb?.backdropUrl)?.let {
            AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(40.dp))
        }
        Box(Modifier.fillMaxSize().background(ForgeColors.HeroScrimHorizontal))
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(0.83f to androidx.compose.ui.graphics.Color.Transparent, 1f to ForgeColors.Bg)))

        // Hero (fades out when the focus goes down to the sections)
        Column(
            Modifier.padding(start = ForgeDims.ContentStart).height(484.dp).width(600.dp)
                .graphicsLayer { alpha = 1f - p; translationY = -60.dp.toPx() * p },
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(programme.title, style = ForgeText.display, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 560.dp).then(tagIf(!sectionsShown, "t-40.0")))
            val meta = listOfNotNull(
                channel.name,
                forgeTvSlot(programme.startEpochMs, programme.stopEpochMs).replace("—", "–"),
                forgeDayLabel(programme.startEpochMs, now).takeIf { !it.equals("dziś", ignoreCase = true) },
                forgeEpisodeShort(programme.episode ?: extra?.episode),
                tmdb?.genres?.firstOrNull(),
            ).joinToString(" · ")
            Text(meta, style = ForgeText.label5, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp).then(tagIf(!sectionsShown, "t-14.0")))
            (programme.description?.takeIf { it.isNotBlank() } ?: tmdb?.overview)?.let {
                Text(it.replace('\n', ' '), style = ForgeText.label5, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp).width(300.dp).then(tagIf(!sectionsShown, "t-14.1")))
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                forgeAvailability(channel, programme, now).forEachIndexed { i, (label, kind) ->
                    com.nuvio.tv.skin.kicia.components.ForgeBadge(label, kind, tagIf(!sectionsShown, "plakietka.$i"))
                }
            }
            com.nuvio.tv.skin.kicia.components.ForgeSpreadRow(
                Modifier.padding(top = 12.dp).focusProperties { down = currentCard }.focusGroup(), gap = 12.dp, // v7.4: sąsiedzi odsuwają się przy fokusie (--rozsun)
            ) {
                Item(0) { when {
                    future && reminded -> ForgeButton(text = "Usuń przypomnienie", icon = Icons.Filled.Notifications, primary = true, focusRequester = main, modifier = Modifier.testTag("przycisk.0").onFocusChanged { if (it.isFocused) lastButton = main },
                        onOk = { com.nuvio.tv.fork.tv.ForgeReminders.toggle(ctx.applicationContext, com.nuvio.tv.fork.tv.ForgeReminder(channel.streamUrl, programme.startEpochMs, programme.title, channel.name)) })
                    future -> ForgeButton(text = "Przypomnij", icon = Icons.Filled.Notifications, primary = true, focusRequester = main, modifier = Modifier.testTag("przycisk.0").onFocusChanged { if (it.isFocused) lastButton = main },
                        onOk = {
                            com.nuvio.tv.fork.tv.ForgeReminders.toggle(ctx.applicationContext, com.nuvio.tv.fork.tv.ForgeReminder(channel.streamUrl, programme.startEpochMs, programme.title, channel.name))
                            com.nuvio.tv.skin.kicia.components.ForgeToast.show("Przypomnienie ustawione")
                        })
                    !live && onWatchFromStart != null -> ForgeButton(text = "Odtwórz z archiwum", icon = Icons.Filled.PlayArrow, primary = true, focusRequester = main, modifier = Modifier.testTag("przycisk.0").onFocusChanged { if (it.isFocused) lastButton = main }, onOk = onWatchFromStart)
                    else -> ForgeButton(text = "Oglądaj na żywo", icon = Icons.Filled.PlayArrow, primary = true, focusRequester = main, modifier = Modifier.testTag("przycisk.0").onFocusChanged { if (it.isFocused) lastButton = main }, onOk = onWatchLive)
                } }
                var nextTag = 1
                if (live && onWatchFromStart != null) {
                    val r = remember { FocusRequester() }
                    val t = nextTag++
                    Item(t) { ForgeButton(text = "Od początku", icon = Icons.Filled.SkipPrevious, focusRequester = r, modifier = Modifier.testTag("przycisk.$t").onFocusChanged { if (it.isFocused) lastButton = r }, onOk = onWatchFromStart) }
                }
                if (episodes.size > 1) {
                    val r = remember { FocusRequester() }
                    val t = nextTag++
                    Item(t) { ForgeButton(text = "Więcej odcinków", focusRequester = r, modifier = Modifier.testTag("przycisk.$t").onFocusChanged { if (it.isFocused) lastButton = r }, onOk = { moreOpen = true }) }
                }
                val star = remember { FocusRequester() }
                Item(nextTag) { ForgeIconButton(
                    icon = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Ulubione", active = favorite, focusRequester = star,
                    modifier = Modifier.testTag("przycisk-okragly.0").onFocusChanged { if (it.isFocused) lastButton = star },
                    onOk = { LiveTvRepository.toggleFavorite(channel) },
                ) }
            }
        }
        if (channel.catchup == null) {
            Text("Ten kanał nie ma archiwum, więc nie można oglądać od początku.", style = ForgeText.caption.copy(color = ForgeColors.Text3),
                modifier = Modifier.padding(start = ForgeDims.ContentStart, top = 492.dp).graphicsLayer { alpha = 1f - p })
        }

        // Sections state: the title centred at y 140, then "Odcinki i emisje" (y 248) and the cards (y 294)
        Text(programme.title, style = ForgeText.display, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 140.dp).widthIn(max = 880.dp).graphicsLayer { alpha = p }.then(tagIf(sectionsShown, "t-40.0")))
        Column(
            Modifier.fillMaxWidth().padding(top = 248.dp).graphicsLayer { alpha = p; translationY = 300.dp.toPx() * (1f - p) }
                .onFocusChanged { inSections = it.hasFocus },
        ) {
            Text(if (hasEpisodes) "Odcinki i emisje" else "Emisje", style = ForgeText.head, modifier = Modifier.padding(start = ForgeDims.ContentStart).then(tagIf(sectionsShown, "naglowek-sekcji.0")))
            Spacer(Modifier.height(8.dp)) // v7.4 (08): karty y 294 = nagłówek 248 + 26 + 8 + 12 (miejsce na fokus 1.125)
            if (moreOpen) {
                // "Więcej odcinków": the same section at full height, 4 columns
                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                    contentPadding = PaddingValues(start = ForgeDims.ContentStart, end = ForgeDims.ContentEnd, top = 4.dp, bottom = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(episodes.size) { i ->
                        ForgeAiringCard(channel, episodes[i], now, i, if (i == current) currentCard else null, onWatchLive, onWatchFromStart.takeIf { episodes[i].startEpochMs == programme.startEpochMs }, upTo = lastButton, tagged = sectionsShown)
                    }
                }
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(50); runCatching { currentCard.requestFocus() } }
            } else {
                LazyRow(
                    state = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = current),
                    contentPadding = PaddingValues(start = ForgeDims.ContentStart, end = ForgeDims.ContentEnd, top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth().testTag("polka.0"),
                ) {
                    items(episodes.size, key = { episodes[it].startEpochMs }) { i ->
                        ForgeAiringCard(channel, episodes[i], now, i, if (i == current) currentCard else null, onWatchLive, onWatchFromStart.takeIf { episodes[i].startEpochMs == programme.startEpochMs }, upTo = lastButton, tagged = sectionsShown)
                    }
                }
            }
        }
    }
}

/** v7.4 (08): Karta/Odcinek of an airing: picture 160 x 114 (no picture: the channel's logo on a dark surface), under it "S24 · O4302" 16/700, title 14/700, day and time 12, availability badge. */
@Composable
private fun ForgeAiringCard(
    channel: LiveTvChannel,
    p: LiveTvProgramme,
    now: Long,
    index: Int,
    focusRequester: FocusRequester?,
    onWatchLive: () -> Unit,
    onFromStart: (() -> Unit)?,
    upTo: FocusRequester,
    tagged: Boolean,
) {
    fun tagIf(tag: String): Modifier = if (tagged) Modifier.testTag(tag) else Modifier
    val isLive = p.startEpochMs <= now && now < p.stopEpochMs
    Column(Modifier.width(ForgeDims.Wide)) {
        com.nuvio.tv.skin.kicia.components.ForgeFocusBox(
            radius = ForgeDims.RadiusCard,
            focusRequester = focusRequester,
            modifier = Modifier.size(ForgeDims.Wide, ForgeDims.WideHeight).focusProperties { up = upTo }.then(tagIf("karta.$index")),
            onOk = { if (isLive) onWatchLive() else onFromStart?.invoke() },
        ) { _ ->
            Box(Modifier.fillMaxSize().background(ForgeColors.Surface2)) {
                val image = p.image?.takeIf { it.isNotBlank() }
                if (image != null) AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else forgeChannelLogo(channel)?.takeIf { it.isNotBlank() }?.let {
                    ForgeAutoLogo(url = it, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(width = 86.dp, height = 46.dp))
                }
            }
        }
        Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp)) {
            Text(forgeEpisodeShort(p.episode)?.replace(" ", " · ") ?: forgeTvTime(p.startEpochMs), style = ForgeText.shelfTitle, maxLines = 1, modifier = tagIf("t-16.$index"))
            Text(p.title, style = ForgeText.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = tagIf("t-14.$index"))
            Text(forgeDayLabel(p.startEpochMs, now) + " " + forgeTvTime(p.startEpochMs), style = ForgeText.caption, maxLines = 1, modifier = tagIf("t-12.$index"))
            forgeAvailability(channel, p, now).firstOrNull()?.let { (label, kind) ->
                com.nuvio.tv.skin.kicia.components.ForgeBadge(label, kind, Modifier.padding(top = 4.dp).then(tagIf("plakietka.$index")))
            }
        }
    }
}

/** v7.4 (08): availability badges of an airing: Na żywo / W archiwum / Niedostępne / Wkrótce (+ W archiwum when the same episode is there). */
internal fun forgeAvailability(channel: LiveTvChannel, p: LiveTvProgramme, now: Long): List<Pair<String, com.nuvio.tv.skin.kicia.components.ForgeBadgeKind>> = when {
    p.startEpochMs > now -> listOf("Wkrótce" to com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Soon)
    p.stopEpochMs > now -> listOf("Na żywo" to com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Live)
    com.nuvio.tv.reshaped.livetv.LiveTvCatchupLinks.isPlayable(channel.catchup, p, now) -> listOf("W archiwum" to com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Archive)
    else -> listOf("Niedostępne" to com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Unavailable)
}

/** v7.4 (08): "Sezon 24 · Odcinek 4308" -> "S24 O4308", "Odcinek 6009" -> "O6009". */
internal fun forgeEpisodeShort(raw: String?): String? {
    val label = forgeEpisodeLabel(raw) ?: return null
    val s = Regex("Sezon (\\d+)").find(label)?.groupValues?.get(1)
    val e = Regex("Odcinek (\\d+)").find(label)?.groupValues?.get(1)
    return listOfNotNull(s?.let { "S$it" }, e?.let { "O$it" }).joinToString(" ").takeIf { it.isNotEmpty() }
}

@Composable
private fun ForgeProgramDetailV6(
    channel: LiveTvChannel,
    programme: LiveTvProgramme,
    onWatchLive: () -> Unit,
    onWatchFromStart: (() -> Unit)?,
    onDismiss: () -> Unit,
    /** v6: the MiniGuide's "Info i odcinki" opens as a drawer under the picture, not a full screen. */
    compact: Boolean = false,
) {
    BackHandler(onBack = onDismiss)
    val now = remember { System.currentTimeMillis() }
    val airings = remember(channel.guideKey, programme) { forgeAirings(LiveTvRepository.schedule(channel.guideKey), programme, now) }
    val live = programme.startEpochMs <= now && now < programme.stopEpochMs
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    val liveTv by LiveTvRepository.uiState.collectAsState()
    // TMDB fills what the guide lacks (poster, cast, rating); strict title match, nothing shown when TMDB is not sure.
    val metaVm: com.nuvio.tv.fork.tv.TvStoreMetaViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    // What the guide itself knows beyond the description (episode, age, year, categories, credits), filled while the EPG is read.
    val extra = remember(programme) { com.nuvio.tv.fork.tv.EpgExtras.get(programme.title, programme.startEpochMs) }
    LaunchedEffect(programme.title) { metaVm.load(programme.title, extra?.year) }
    val tmdb by metaVm.meta.collectAsState()
    val favorite = channel.streamUrl in liveTv.favoriteUrls
    Box(Modifier.fillMaxSize().background(ForgeColors.Bg)) {
        // Blurred picture of the programme behind (mockup d06); EPG pictures only, never a guess.
        (programme.image?.takeIf { it.isNotBlank() } ?: tmdb?.backdropUrl)?.let { // TMDB's picture only when the guide has none
            AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(40.dp))
        }
        Box(Modifier.fillMaxSize().background(ForgeColors.HeroScrimHorizontal))
        tmdb?.posterUrl?.let { poster ->
            AsyncImage(model = poster, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = ForgeDims.ContentEnd).size(ForgeDims.Poster, ForgeDims.PosterHeight).clip(androidx.compose.foundation.shape.RoundedCornerShape(ForgeDims.RadiusCard)))
        }
        Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(start = ForgeDims.ContentStart, top = if (compact) 20.dp else 72.dp, end = ForgeDims.ContentEnd)) {
            if (false && !compact) { // v7.4 (08): no channel logo above the title; the drawer (340 dp) keeps its room for the episodes below
            Box(Modifier.size(width = 120.dp, height = 54.dp)) {
                val url = forgeChannelLogo(channel)
                if (!url.isNullOrBlank()) ForgeAutoLogo(url = url, contentDescription = channel.name, modifier = Modifier.fillMaxSize())
                else Text(channel.name, style = ForgeText.head)
            }
            Spacer(Modifier.height(20.dp))
            }
            Text(programme.title, style = ForgeText.display, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(560.dp)) // v7.4: 40/44
            Row(Modifier.padding(top = 6.dp)) {
                Text(channel.name + " · " + forgeTvSlot(programme.startEpochMs, programme.stopEpochMs) + " · " + forgeDayLabel(programme.startEpochMs, now), style = ForgeText.label5) // v7.4: meta 14
                if (live) com.nuvio.tv.skin.kicia.components.ForgeBadge("Na żywo", com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Live, Modifier.padding(start = 12.dp)) // v7.4: Plakietka
            }
            if (onWatchFromStart != null) {
                Text("▶ Dostępne od początku (archiwum" + (channel.catchup?.days?.takeIf { it > 0 }?.let { " $it dni" } ?: "") + ")", style = ForgeText.body.copy(color = ForgeColors.Ok), modifier = Modifier.padding(top = 4.dp))
            }
            (forgeEpisodeLabel(programme.episode) ?: forgeEpisodeLabel(extra?.episode))?.let {
                Text(it, style = ForgeText.label.copy(color = ForgeColors.Ok), modifier = Modifier.padding(top = 6.dp))
            }
            extra?.let { x ->
                x.subTitle?.let { Text(it, style = ForgeText.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp).width(560.dp)) }
                x.copy(episode = null).infoLine.takeIf { it.isNotBlank() }?.let { Text(it, style = ForgeText.caption, modifier = Modifier.padding(top = 4.dp)) }
            }
            tmdb?.let { m ->
                listOfNotNull(m.genres.take(2).joinToString(", ").takeIf { it.isNotBlank() }, m.year?.toString(), m.ratingText)
                    .joinToString(" · ").takeIf { it.isNotBlank() }
                    ?.let { Text(it, style = ForgeText.caption, modifier = Modifier.padding(top = 6.dp)) }
            }
            (programme.description?.takeIf { it.isNotBlank() } ?: tmdb?.overview)?.let {
                Text(it, style = ForgeText.label5, maxLines = if (compact) 2 else 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = if (compact) 6.dp else 8.dp).width(300.dp)) // v7.4: 14/20, 300 dp
            }
            Row(Modifier.padding(top = if (compact) 10.dp else 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (programme.startEpochMs > now) {
                    // A programme still to come (S08 state "Przyszły"): the main button reminds.
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    LaunchedEffect(Unit) { com.nuvio.tv.fork.tv.ForgeReminders.load(ctx.applicationContext) }
                    val reminders by com.nuvio.tv.fork.tv.ForgeReminders.list.collectAsState()
                    val on = reminders.any { it.channelUrl == channel.streamUrl && it.startMs == programme.startEpochMs }
                    ForgeButton(text = if (on) "Przypomnę" else "Przypomnij mi", icon = androidx.compose.material.icons.Icons.Filled.Notifications, primary = true, focusRequester = first,
                        onOk = { com.nuvio.tv.fork.tv.ForgeReminders.toggle(ctx.applicationContext, com.nuvio.tv.fork.tv.ForgeReminder(channel.streamUrl, programme.startEpochMs, programme.title, channel.name)) })
                    ForgeButton(text = "Oglądaj kanał", icon = Icons.Filled.PlayArrow, onOk = onWatchLive)
                } else
                ForgeButton(text = if (live) "Oglądaj na żywo" else "Oglądaj kanał", icon = Icons.Filled.PlayArrow, primary = true, focusRequester = first, onOk = onWatchLive)
                if (onWatchFromStart != null) ForgeButton(text = "Oglądaj od początku", icon = Icons.Outlined.Replay, onOk = onWatchFromStart)
                ForgeIconButton(
                    icon = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Ulubione",
                    active = favorite,
                    onOk = { LiveTvRepository.toggleFavorite(channel) },
                )
            }
            // Cast: TMDB with photos when it matched, otherwise the guide's own credits (text as written, no photo).
            val guideCredits = extra?.credits.orEmpty().filter { it.first == "actor" || it.first == "director" || it.first == "presenter" }.map { (role, text) ->
                val dash = text.lastIndexOf(" - ")
                com.nuvio.tv.fork.tv.TvStorePerson(null, if (dash > 0) text.substring(dash + 3) else text, if (dash > 0) text.substring(0, dash) else role, null)
            }
            (tmdb?.let { (it.directors + it.cast) }?.takeIf { it.isNotEmpty() } ?: guideCredits).takeIf { it.isNotEmpty() && !compact }?.let { people ->
                Spacer(Modifier.height(24.dp))
                Text("Obsada i twórcy", style = ForgeText.shelfTitle)
                Spacer(Modifier.height(ForgeDims.ShelfHeaderToCards))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(people.size) { i -> ForgePersonChip(people[i]) }
                }
            }
            Spacer(Modifier.height(if (compact) 14.dp else 32.dp))
            val shelf = if (airings.others.isNotEmpty()) "Inne emisje" to airings.others else "Dalej na ${channel.name}" to airings.next
            if (shelf.second.isEmpty()) {
                Text("Brak danych przewodnika o kolejnych emisjach tego programu.", style = ForgeText.caption.copy(color = ForgeColors.Text3))
                Spacer(Modifier.height(24.dp))
            } else {
                Text(shelf.first, style = ForgeText.head)
                Spacer(Modifier.height(ForgeDims.ShelfHeaderToCards))
                LazyRow(contentPadding = PaddingValues(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(ForgeDims.Gap)) {
                    items(shelf.second, key = { it.startEpochMs }) { p ->
                        ForgeWideCard(onOk = onWatchLive) {
                            forgeChannelLogo(channel)?.takeIf { it.isNotBlank() }?.let { // mockup d06: the channel's logo on each card
                                ForgeAutoLogo(url = it, contentDescription = null, modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp).size(width = 86.dp, height = 46.dp))
                            }
                            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                                Text(forgeDayLabel(p.startEpochMs, now) + " " + forgeTvTime(p.startEpochMs), style = ForgeText.caption)
                                forgeEpisodeLabel(p.episode)?.let { Text(it, style = ForgeText.caption.copy(color = ForgeColors.Ok), maxLines = 1) }
                                Text(p.title, style = ForgeText.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The guide's episode label as "Sezon 3 · Odcinek 168" / "Odcinek 6009": understands S3E168, E6009, "3x168" and xmltv_ns "2.167.0/1". */
internal fun forgeEpisodeLabel(raw: String?): String? {
    val t = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    Regex("(?i)^s\\s*(\\d+)\\s*e\\s*(\\d+)").find(t)?.let { return "Sezon ${it.groupValues[1].toInt()} · Odcinek ${it.groupValues[2].toInt()}" }
    Regex("(?i)^(\\d+)\\s*x\\s*(\\d+)").find(t)?.let { return "Sezon ${it.groupValues[1].toInt()} · Odcinek ${it.groupValues[2].toInt()}" }
    Regex("(?i)^e\\s*(\\d+)").find(t)?.let { return "Odcinek ${it.groupValues[1].toInt()}" }
    Regex("^(\\d*)\\s*\\.\\s*(\\d*)").find(t)?.let { m ->
        val season = m.groupValues[1].toIntOrNull()?.plus(1); val ep = m.groupValues[2].toIntOrNull()?.plus(1)
        return listOfNotNull(season?.let { "Sezon $it" }, ep?.let { "Odcinek $it" }).joinToString(" · ").takeIf { it.isNotEmpty() }
    }
    return t.takeIf { it.all { c -> c.isDigit() } }?.let { "Odcinek ${it.toInt()}" }
}

/** "dziś", "jutro", "wczoraj" or "pon. 10.11". */
internal fun forgeDayLabel(ms: Long, nowMs: Long, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String {
    val d = java.time.Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    val today = java.time.Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    return when (d) {
        today -> "dziś"
        today.plusDays(1) -> "jutro"
        today.minusDays(1) -> "wczoraj"
        else -> listOf("pon.", "wt.", "śr.", "czw.", "pt.", "sob.", "niedz.")[d.dayOfWeek.value - 1] + " %d.%02d".format(d.dayOfMonth, d.monthValue)
    }
}

/** A cast member: TMDB photo when there is one, initials otherwise; the role under the name. */
@Composable
internal fun ForgePersonChip(p: com.nuvio.tv.fork.tv.TvStorePerson) {
    // v6 S20: OK on a person (TMDB id known) opens Nuvio's person screen (photo, biography, filmography).
    com.nuvio.tv.skin.kicia.components.ForgeFocusBox(
        radius = 12.dp,
        modifier = Modifier.width(92.dp),
        enabled = p.id != null,
        onOk = { p.id?.let { com.nuvio.tv.skin.kicia.nav.ForgeNavRequests.open(com.nuvio.tv.ui.navigation.Screen.CastDetail.createRoute(it, p.name)) } },
    ) { _ ->
        Column(Modifier.width(92.dp).padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(androidx.compose.foundation.shape.CircleShape).background(ForgeColors.Surface3), contentAlignment = Alignment.Center) {
                if (p.photoUrl != null) AsyncImage(model = p.photoUrl, contentDescription = p.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Text(p.name.split(' ').mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString(""), style = ForgeText.label)
            }
            Text(p.name, style = ForgeText.captionBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            p.role?.let { Text(it, style = ForgeText.caption.copy(color = ForgeColors.Text3), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}
