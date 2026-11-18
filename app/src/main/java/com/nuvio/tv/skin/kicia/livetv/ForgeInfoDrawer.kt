package com.nuvio.tv.skin.kicia.livetv

import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.kicia.components.ForgeAutoLogo
import com.nuvio.tv.skin.kicia.components.ForgeButton
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.components.ForgeWideCard
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/** Season and episode numbers out of the guide's label ("S3E168", "E6009", "3x168", xmltv_ns "2.167."); null parts when the label lacks them. */
internal fun forgeEpisodeNumbers(raw: String?): Pair<Int?, Int?>? {
    val t = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    Regex("(?i)^s\\s*(\\d+)\\s*e\\s*(\\d+)").find(t)?.let { return it.groupValues[1].toInt() to it.groupValues[2].toInt() }
    Regex("(?i)^(\\d+)\\s*x\\s*(\\d+)").find(t)?.let { return it.groupValues[1].toInt() to it.groupValues[2].toInt() }
    Regex("(?i)^e\\s*(\\d+)").find(t)?.let { return null to it.groupValues[1].toInt() }
    Regex("^(\\d*)\\s*\\.\\s*(\\d*)").find(t)?.let { m ->
        val s = m.groupValues[1].toIntOrNull()?.plus(1); val e = m.groupValues[2].toIntOrNull()?.plus(1)
        return if (s == null && e == null) null else s to e
    }
    return t.takeIf { it.all { c -> c.isDigit() } }?.let { null to it.toInt() }
}

/** "Sezon 3, odcinek 1" / "Odcinek 6009" (mockup v6.2, Freely). */
internal fun forgeSeasonEpisode(raw: String?): String? {
    val (s, e) = forgeEpisodeNumbers(raw) ?: return null
    return when {
        s != null && e != null -> "Sezon $s, odcinek $e"
        e != null -> "Odcinek $e"
        s != null -> "Sezon $s"
        else -> null
    }
}

private enum class DrawerMode { Info, More }

/**
 * v6.2 (mockup S09, owner's Freely screenshots 10.11): the MiniGuide's "Info i odcinki" drawer. Title with the channel logo and a line,
 * a row of buttons, "Sezon 3, odcinek 1", CHANNEL, DAY, HOURS, the description, LIVE / length on the right, and "Więcej z tego serialu"
 * (the same title's other airings, past and ahead, with their episode numbers). "Więcej odcinków" = season tabs and the episodes of one.
 */
@Composable
fun ForgeInfoDrawer(
    channel: LiveTvChannel,
    programme: LiveTvProgramme,
    onWatchLive: () -> Unit,
    onWatchFromStart: ((LiveTvProgramme) -> Unit)?,
    /** OK on an airing (a card): live now → the channel, past → the archive, ahead → a reminder. */
    onPlayAiring: (LiveTvProgramme) -> Unit,
    /** The airing the player is on right now: the replayed one, else what is live on this channel; null when the player is elsewhere. */
    playing: LiveTvProgramme? = null,
    onDismiss: () -> Unit,
) {
    var mode by remember { mutableStateOf(DrawerMode.Info) }
    var shown by remember(programme) { mutableStateOf(programme) }
    BackHandler { if (mode == DrawerMode.More) mode = DrawerMode.Info else onDismiss() }
    val now = remember { System.currentTimeMillis() }
    val liveTv by LiveTvRepository.uiState.collectAsState()
    val favorite = channel.streamUrl in liveTv.favoriteUrls
    val titleKey = programme.title.trim().lowercase()
    val sameTitle = remember(channel.guideKey, programme) {
        LiveTvRepository.schedule(channel.guideKey).filter { it.title.trim().lowercase() == titleKey }.sortedBy { it.startEpochMs }
    }
    // Airings with a season / episode number; without any number all of them count, so "Więcej odcinków" still lists them.
    val numbered = remember(sameTitle) { sameTitle.filter { forgeEpisodeNumbers(it.episode) != null }.ifEmpty { sameTitle } }
    val others = sameTitle // the open and the playing airings stay in the rows: they are marked there
    val live = shown.startEpochMs <= now && now < shown.stopEpochMs
    val canStart = onWatchFromStart != null && channel.catchup != null && shown.startEpochMs <= now
    val first = remember { FocusRequester() }
    LaunchedEffect(mode) { runCatching { first.requestFocus() } }
    val logo = forgeChannelLogo(channel)

    Box(Modifier.fillMaxSize().background(if (mode == DrawerMode.Info) androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.72f) else ForgeColors.Bg)) { // v7.4 (08): the video plays on under glass
        Column(Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(start = ForgeDims.ContentStart, end = ForgeDims.ContentEnd, top = if (mode == DrawerMode.Info) 270.dp else 16.dp)) {
            // v7.4 (08): title 40/44, meta 14 (channel · hours · episode), description 14 in 3 lines (300 dp), availability badge
            Text(programme.title, style = ForgeText.display, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
            if (mode == DrawerMode.Info) {
                Text(
                    listOfNotNull(channel.name, forgeTvSlot(shown.startEpochMs, shown.stopEpochMs), forgeSeasonEpisode(shown.episode)).joinToString(" · "),
                    style = ForgeText.label5, maxLines = 1, modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    shown.description?.takeIf { it.isNotBlank() } ?: "Przewodnik nie ma opisu tego programu.",
                    style = ForgeText.label5, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp).width(300.dp),
                )
                Spacer(Modifier.height(8.dp))
                val past = shown.stopEpochMs <= now
                val soon = shown.startEpochMs > now
                com.nuvio.tv.skin.kicia.components.ForgeBadge(
                    when { soon -> "Wkrótce"; past && channel.catchup != null -> "Archiwum"; past -> "Niedostępne w archiwum"; else -> "Na żywo" },
                    when { soon -> com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Soon; past && channel.catchup != null -> com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Archive; past -> com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Unavailable; else -> com.nuvio.tv.skin.kicia.components.ForgeBadgeKind.Live },
                )
            }
            Spacer(Modifier.height(16.dp))
            if (mode == DrawerMode.Info) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var firstSet = false
                    fun req(): FocusRequester? = if (!firstSet) { firstSet = true; first } else null
                    if (live) ForgeButton(text = "Oglądaj na żywo", icon = Icons.Filled.PlayArrow, primary = true, focusRequester = req(), onOk = onWatchLive)
                    if (canStart) ForgeButton(text = "Od początku", icon = androidx.compose.material.icons.Icons.Filled.SkipPrevious, focusRequester = req(), onOk = { onWatchFromStart?.invoke(shown) })
                    if (numbered.size > 1 || sameTitle.size > 1) ForgeButton(text = "Więcej odcinków", icon = Icons.Filled.Info, focusRequester = req(), onOk = { mode = DrawerMode.More })
                    com.nuvio.tv.skin.kicia.components.ForgeIconButton(
                        icon = if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = "Ulubione", active = favorite,
                        focusRequester = req(), onOk = { LiveTvRepository.toggleFavorite(channel) },
                    ) // v7.4: round ★
                }
            } else {
                SeasonTabs(numbered, shown, first) { picked -> shown = picked }
            }
            Spacer(Modifier.height(12.dp))
            if (mode == DrawerMode.More) {
                EpisodeRow(channel, logo, numbered, shown, now, playing, onFocus = { shown = it }, onPlay = onPlayAiring)
                Spacer(Modifier.height(8.dp))
            }
            // Season and episode, channel · day · hours, description; on the right LIVE and the length (the "Więcej odcinków" view only; the hero has it above).
            if (mode == DrawerMode.More) Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f).padding(end = 40.dp)) {
                    forgeSeasonEpisode(shown.episode)?.let { Text(it, style = ForgeText.label) }
                    Text(
                        (channel.name + ", " + forgeDayLabel(shown.startEpochMs, now) + ", " + forgeTvSlot(shown.startEpochMs, shown.stopEpochMs)).uppercase(),
                        style = ForgeText.caption, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    val description = shown.description?.takeIf { it.isNotBlank() }
                    if (description != null) Text(description, style = ForgeText.body, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    else Text("Przewodnik nie ma opisu tego programu.", style = ForgeText.caption.copy(color = ForgeColors.Text3), modifier = Modifier.padding(top = 4.dp))
                }
                Column(Modifier.width(120.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (live) Text("NA ŻYWO", style = ForgeText.captionBold.copy(color = ForgeColors.Live))
                    Text(((shown.stopEpochMs - shown.startEpochMs) / 60_000).toInt().toString() + " MIN", style = ForgeText.captionBold)
                }
            }
            if (mode == DrawerMode.Info) {
                Spacer(Modifier.height(12.dp))
                // Owner 10.11: "Więcej z tego serialu" lists only what can be played (started already), oldest first by season and episode;
                // what is still to come sits under "Emisje w przyszłości".
                // One card per episode: the airings of one episode (same season and number, else the same description) are merged and
                // the card leads to the newest of them that has aired; it is marked when any of them is live or playing.
                val ready = others.filter { it.startEpochMs <= now }
                    .groupBy { p -> forgeEpisodeKey(p) }
                    .values.map { group -> group.maxBy { it.startEpochMs } }
                    .sortedWith(forgeEpisodeOrder)
                val ahead = others.filter { it.startEpochMs > now }.sortedWith(forgeEpisodeOrder)
                if (ready.isEmpty() && ahead.isEmpty()) {
                    Text("Brak innych emisji tego programu w przewodniku.", style = ForgeText.caption.copy(color = ForgeColors.Text3))
                }
                if (ready.isNotEmpty()) AiringShelf("Więcej z tego serialu", ready, channel, logo, now, playing, onFocus = { shown = it }, onPlay = onPlayAiring, upTo = if (numbered.isEmpty()) first else null) // 8.4: ▲ wraca na przycisk główny
                if (ahead.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    AiringShelf("Emisje w przyszłości", ahead, channel, logo, now, playing, onFocus = { shown = it }, onPlay = onPlayAiring)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** What makes airings the same episode: season and number, else the description, else (unique) the start. */
private fun forgeEpisodeKey(p: LiveTvProgramme): String =
    forgeEpisodeNumbers(p.episode)?.let { "s${it.first}e${it.second}" } ?: p.description?.takeIf { it.isNotBlank() } ?: "t${p.startEpochMs}"

/** Oldest first by season, then episode number; airings without numbers follow by their start. */
private val forgeEpisodeOrder: Comparator<LiveTvProgramme> = compareBy<LiveTvProgramme>(
    { forgeEpisodeNumbers(it.episode)?.first ?: 0 },
    { forgeEpisodeNumbers(it.episode)?.second ?: Int.MAX_VALUE },
    { it.startEpochMs },
)

@Composable
private fun AiringShelf(
    title: String, list: List<LiveTvProgramme>, channel: LiveTvChannel, logo: String?, now: Long, playing: LiveTvProgramme?,
    onFocus: (LiveTvProgramme) -> Unit, onPlay: (LiveTvProgramme) -> Unit,
    upTo: FocusRequester? = null,
) {
    Text(title, style = ForgeText.shelfTitle)
    Spacer(Modifier.height(4.dp))
    // Owner 12.11: ▼ lands on the airing that plays (else the newest one, the last of the list), the row scrolled to it
    val target = forgePlayingIndex(list, playing, now)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (target - 1).coerceAtLeast(0))
    val targetFocus = remember { FocusRequester() }
    LazyRow(
        state = listState, contentPadding = PaddingValues(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(ForgeDims.Gap),
        modifier = Modifier.focusProperties {
            @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
            enter = { _ -> if (target >= 0) targetFocus else FocusRequester.Default }
        }.focusGroup(),
    ) {
        itemsIndexed(list, key = { _, it -> it.startEpochMs }) { i, p -> AiringCard(channel, logo, p, now, playing != null && p.startEpochMs <= now && forgeEpisodeKey(playing) == forgeEpisodeKey(p), onFocus = { onFocus(p) }, onOk = { onPlay(p) }, focusRequester = if (i == target) targetFocus else null, upTo = upTo) }
    }
}

/**
 * A card of an airing. Marks (owner 10.11): a red "NA ŻYWO" pill when it is on the air now, and for the one the player is on a green frame and a
 * white "▶ Odtwarzane" pill (live or from the archive).
 */
@Composable
private fun AiringCard(channel: LiveTvChannel, logo: String?, p: LiveTvProgramme, now: Long, isPlaying: Boolean, onFocus: () -> Unit, onOk: () -> Unit, focusRequester: FocusRequester? = null, upTo: FocusRequester? = null) {
    val isLive = p.startEpochMs <= now && now < p.stopEpochMs
    ForgeWideCard(modifier = if (upTo != null) Modifier.focusProperties { up = upTo } else Modifier, focusRequester = focusRequester, onFocusChange = { if (it) onFocus() }, onOk = onOk) {
        val image = p.image?.takeIf { it.isNotBlank() }
        if (image != null) AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else if (!logo.isNullOrBlank()) ForgeAutoLogo(url = logo, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(86.dp, 40.dp))
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(0.4f to androidx.compose.ui.graphics.Color.Transparent, 1f to androidx.compose.ui.graphics.Color.Black.copy(alpha = .78f))))
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            val (s, e) = forgeEpisodeNumbers(p.episode) ?: (null to null)
            Text(if (e != null) "Odcinek $e" else p.title, style = ForgeText.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(forgeDayLabel(p.startEpochMs, now) + " " + forgeTvTime(p.startEpochMs) + (s?.let { " · Sezon $it" } ?: ""), style = ForgeText.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isPlaying) Box(Modifier.fillMaxSize().border(3.dp, ForgeColors.Ok, androidx.compose.foundation.shape.RoundedCornerShape(ForgeDims.RadiusCard)))
        Row(Modifier.align(Alignment.TopStart).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (isPlaying) Text(
                "▶ Odtwarzane", style = ForgeText.captionBold.copy(color = androidx.compose.ui.graphics.Color(0xFF1B1B1E)),
                modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp)).background(ForgeColors.Ok).padding(horizontal = 7.dp, vertical = 1.dp),
            )
            if (isLive) Text(
                "NA ŻYWO", style = ForgeText.captionBold.copy(color = androidx.compose.ui.graphics.Color(0xFF1A0B09)),
                modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp)).background(ForgeColors.Live).padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
    }
}

/** Season tabs of the same title's numbered airings ("Sezon 1", "Sezon 2"...); an airing without a season sits under "Odcinki". */
@Composable
private fun SeasonTabs(numbered: List<LiveTvProgramme>, shown: LiveTvProgramme, first: FocusRequester, onPick: (LiveTvProgramme) -> Unit) {
    val seasons = remember(numbered) { numbered.map { forgeEpisodeNumbers(it.episode)?.first }.distinct().sortedBy { it ?: 0 } }
    val current = forgeEpisodeNumbers(shown.episode)?.first
    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        seasons.forEachIndexed { i, season ->
            val on = season == current
            ForgeFocusBox(radius = 8.dp, modifier = Modifier.height(34.dp), focusRequester = if (i == 0) first else null, scaleOnFocus = false, onOk = {
                numbered.firstOrNull { forgeEpisodeNumbers(it.episode)?.first == season }?.let(onPick)
            }) { focused ->
                Column(Modifier.padding(horizontal = 4.dp)) {
                    Text(season?.let { "Sezon $it" } ?: "Odcinki", style = ForgeText.label.copy(color = if (on || focused) ForgeColors.Text else ForgeColors.Text3))
                    Box(Modifier.padding(top = 4.dp).height(3.dp).fillMaxWidth().background(if (on) ForgeColors.Text else androidx.compose.ui.graphics.Color.Transparent))
                }
            }
        }
    }
}

/** The episodes of the shown programme's season, each with "Odcinek N" under it; the shown one is brought to the left. */
@Composable
private fun EpisodeRow(channel: LiveTvChannel, logo: String?, numbered: List<LiveTvProgramme>, shown: LiveTvProgramme, now: Long, playing: LiveTvProgramme?, onFocus: (LiveTvProgramme) -> Unit, onPlay: (LiveTvProgramme) -> Unit) {
    val season = forgeEpisodeNumbers(shown.episode)?.first
    val episodes = remember(numbered, season) {
        numbered.filter { forgeEpisodeNumbers(it.episode)?.first == season && it.startEpochMs <= now }
            .sortedByDescending { it.startEpochMs }.distinctBy { forgeEpisodeKey(it) }.sortedWith(forgeEpisodeOrder) // one per episode: the newest airing
    }
    val listState = rememberLazyListState()
    // Owner 12.11: ▼ lands on the episode that plays (else the newest aired, the last of the list), not on the first one
    val target = forgePlayingIndex(episodes, playing, now)
    val targetFocus = remember { FocusRequester() }
    LaunchedEffect(episodes, target) {
        listState.scrollToItem((target - 1).coerceAtLeast(0))
    }
    LazyRow(
        state = listState, contentPadding = PaddingValues(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(ForgeDims.Gap),
        modifier = Modifier.focusProperties {
            @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
            enter = { _ -> if (target >= 0) targetFocus else FocusRequester.Default }
        }.focusGroup(),
    ) {
        itemsIndexed(episodes, key = { _, it -> it.startEpochMs }) { i, p ->
            Column {
                ForgeWideCard(focusRequester = if (i == target) targetFocus else null, onFocusChange = { if (it) onFocus(p) }, onOk = { onPlay(p) }) {
                    val image = p.image?.takeIf { it.isNotBlank() }
                    if (image != null) AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else if (!logo.isNullOrBlank()) ForgeAutoLogo(url = logo, contentDescription = null, modifier = Modifier.align(Alignment.Center).size(86.dp, 40.dp))
                    if (playing != null && forgeEpisodeKey(playing) == forgeEpisodeKey(p)) Box(Modifier.fillMaxSize().border(3.dp, ForgeColors.Ok, androidx.compose.foundation.shape.RoundedCornerShape(ForgeDims.RadiusCard)))
                    Row(Modifier.align(Alignment.TopStart).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (playing != null && forgeEpisodeKey(playing) == forgeEpisodeKey(p)) Text("▶ Odtwarzane", style = ForgeText.captionBold.copy(color = androidx.compose.ui.graphics.Color(0xFF1B1B1E)),
                            modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp)).background(ForgeColors.Ok).padding(horizontal = 7.dp, vertical = 1.dp))
                        if (p.startEpochMs <= now && now < p.stopEpochMs) Text("NA ŻYWO", style = ForgeText.captionBold.copy(color = androidx.compose.ui.graphics.Color(0xFF1A0B09)),
                            modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp)).background(ForgeColors.Live).padding(horizontal = 7.dp, vertical = 1.dp))
                    }
                }
                Text(forgeEpisodeNumbers(p.episode)?.second?.let { "Odcinek $it" } ?: p.title, style = ForgeText.label, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/**
 * Owner 12.11: the card the episode rows of the MiniGuide's Info start on: the airing that plays (same start, else the same episode),
 * else the newest one that has aired (the list runs oldest first, so the last of them); -1 for an empty list.
 */
internal fun forgePlayingIndex(list: List<LiveTvProgramme>, playing: LiveTvProgramme?, now: Long): Int {
    if (list.isEmpty()) return -1
    if (playing != null) {
        list.indexOfFirst { it.startEpochMs == playing.startEpochMs }.takeIf { it >= 0 }?.let { return it }
        list.indexOfLast { it.startEpochMs <= now && forgeEpisodeKey(it) == forgeEpisodeKey(playing) }.takeIf { it >= 0 }?.let { return it }
    }
    return list.indexOfLast { it.startEpochMs <= now }.takeIf { it >= 0 } ?: list.lastIndex
}
