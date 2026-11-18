@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.nuvio.tv.skin.kicia.livetv

import androidx.compose.ui.focus.focusProperties

import androidx.compose.foundation.focusGroup

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.fork.tv.AlertKind
import com.nuvio.tv.fork.tv.EpisodeAlert
import com.nuvio.tv.fork.tv.TvStore
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.ui.reshaped.livetv.LiveTvPillButton
import com.nuvio.tv.ui.theme.NuvioTheme

private val CardShape = RoundedCornerShape(10.dp)
private val Plaque = Color(0xEBFFFFFF)
private val NewColor = Color(0xFF6EE7B7)
private val ArchiveColor = Color(0xFF8AB4F8)
private val UpcomingColor = Color(0xFFB9BFCB)

/**
 * The TV tab's front page, after Freely: new episodes of the shows the viewer watches (told from the
 * guide), the channels watched last and the programmes watched last. The full guide is one button away.
 */
@Composable
fun ForgeTvBrowse(
    channels: List<LiveTvChannel>,
    onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
    topPadding: androidx.compose.ui.unit.Dp = 24.dp,
) {
    rememberForgeLogoVersion() // forge: redraw when the guide's logos arrive
    val context = LocalContext.current
    val alerts by TvStore.alerts.collectAsStateWithLifecycle()
    val recentChannels by TvStore.recentChannels.collectAsStateWithLifecycle()
    val recentShows by TvStore.recent.collectAsStateWithLifecycle()
    val byUrl = remember(channels) { channels.associateBy { it.streamUrl } }
    val byGuide = remember(channels) { HashMap<String, LiveTvChannel>().also { m -> channels.forEach { m.putIfAbsent(it.guideKey, it) } } }

    // A channel tile on Home asked for this channel: start it as soon as the list is here.
    val pendingPlay by ForgePendingPlay.request.collectAsStateWithLifecycle()
    LaunchedEffect(pendingPlay, byUrl) {
        ForgePendingPlay.take(byUrl)?.let { onPlay(it, ForgePendingPlay.lastProgramme) } // makieta 7.7 G31: a resume from Home
    }

    val shownAlerts = remember(alerts, byGuide) {
        alerts.filter { byGuide.containsKey(it.airing.guideKey) }
            .sortedWith(compareBy({ it.kind != AlertKind.NEW_EPISODE }, { it.airing.startMs }))
            .take(12)
    }
    val shownChannels = remember(recentChannels, byUrl) { recentChannels.mapNotNull { r -> byUrl[r.streamUrl]?.let { r to it } } }
    val shownShows = remember(recentShows, byUrl, byGuide) {
        recentShows.mapNotNull { s -> (byUrl[s.channelUrl] ?: byGuide[s.guideKey])?.let { s to it } }
    }

    // ↓ from the bar: the scaffold's fallback target is the first tile (the focus search found nothing, as on Home).
    val scaffoldTarget = com.nuvio.tv.LocalContentFocusRequester.current
    val firstFocus = remember(scaffoldTarget) { if (scaffoldTarget == FocusRequester.Default) FocusRequester() else scaffoldTarget }
    // v6 S17 "Teraz na żywo": what the guide says is on air now, channels with a picture or description first (from the EPG alone).
    val liveState by com.nuvio.tv.reshaped.livetv.LiveTvRepository.uiState.collectAsStateWithLifecycle()
    val onAirNow = remember(liveState.currentProgrammes, channels) {
        channels.mapNotNull { ch -> liveState.currentProgrammes[ch.guideKey]?.takeIf { it.title.isNotBlank() }?.let { ch to it } }
            .sortedByDescending { (_, p) -> (if (!p.image.isNullOrBlank()) 2 else 0) + (if (!p.description.isNullOrBlank()) 1 else 0) }
            .take(20)
    }
    val onAirAll = remember(liveState.currentProgrammes, channels) {
        channels.mapNotNull { ch -> liveState.currentProgrammes[ch.guideKey]?.takeIf { it.title.isNotBlank() }?.let { ch to it } }
    }
    val hasAny = shownAlerts.isNotEmpty() || shownChannels.isNotEmpty() || shownShows.isNotEmpty() || onAirNow.isNotEmpty()
    // Hero (mockup S06): what the focused tile or card is, large above the rows.
    var hero by remember { mutableStateOf<TvHero?>(null) }
    // S08: long OK on a programme card opens its details.
    var detail by remember { mutableStateOf<Pair<LiveTvChannel, LiveTvProgramme>?>(null) }
    var searching by remember { mutableStateOf(false) } // v6 S18: Live TV search (forge hook)
    fun heroOn(h: TvHero) = Modifier.onFocusChanged { if (it.isFocused) hero = h }
    LaunchedEffect(hasAny) { runCatching { firstFocus.requestFocus() } }

    Box(
        modifier.fillMaxSize()
            // ↓ from the bar lands on the first tile (the focus search found nothing in this page otherwise).
            .focusProperties {
                @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                enter = { if (hasAny) firstFocus else FocusRequester.Default }
            }
            .focusGroup(),
    ) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 0.dp, top = topPadding, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            // ▲ from the tiles enters the row on the open pill, „Przeglądaj” (not the nearest one).
            val browsePill = remember { FocusRequester() }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.focusProperties { enter = { browsePill } }.focusGroup(),
            ) {
                LiveTvPillButton(
                    text = stringResource(R.string.forge_tv_browse),
                    onClick = {},
                    selected = true,
                    modifier = (if (!hasAny) Modifier.focusRequester(firstFocus) else Modifier).focusRequester(browsePill),
                )
                LiveTvPillButton(text = stringResource(R.string.forge_tv_guide), onClick = onOpenGuide)
                LiveTvPillButton(text = "Szukaj", onClick = { searching = true }) // forge hook S18
            }
        }
        hero?.let { h ->
            item(key = "hero") {
                androidx.compose.foundation.layout.Column(Modifier.padding(start = 34.dp).height(130.dp)) {
                    Text(h.title, style = com.nuvio.tv.skin.kicia.tokens.ForgeText.display, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Row(Modifier.padding(top = 6.dp)) {
                        Text(h.meta, style = com.nuvio.tv.skin.kicia.tokens.ForgeText.body, maxLines = 1)
                        if (h.live) Text("  ·  Na żywo", style = com.nuvio.tv.skin.kicia.tokens.ForgeText.body.copy(color = com.nuvio.tv.skin.kicia.tokens.ForgeColors.Live))
                    }
                }
            }
        }

        if (!hasAny) {
            item { EmptyState() }
        }

        if (shownAlerts.isNotEmpty()) {
            item {
                Section(stringResource(R.string.forge_tv_new_episodes), stringResource(R.string.forge_tv_new_episodes_note)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 24.dp, top = 6.dp, bottom = 6.dp)) {
                        items(shownAlerts, key = { it.id }) { alert ->
                            val channel = byGuide.getValue(alert.airing.guideKey)
                            val badge = badgeFor(alert.kind)
                            MediaCard(
                                title = alert.showTitle,
                                subtitle = DateUtils.formatDateTime(
                                    context, alert.airing.startMs,
                                    DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY or DateUtils.FORMAT_SHOW_TIME,
                                ) + " · " + channel.name,
                                imageUrl = null,
                                logoUrl = forgeChannelLogo(channel),
                                badge = badge.first,
                                badgeColor = badge.second,
                                modifier = (if (alert === shownAlerts.first()) Modifier.focusRequester(firstFocus) else Modifier).then(heroOn(TvHero(alert.showTitle, channel.name, live = false))),
                                onClick = { onAlert(alert, channel, onPlay, onOpenGuide) },
                                onLongClick = { detail = channel to LiveTvProgramme(alert.airing.title, alert.airing.startMs, alert.airing.stopMs, alert.airing.description) },
                            )
                        }
                    }
                }
            }
        }

        if (onAirNow.isNotEmpty()) {
            item {
                Section("Teraz na żywo", "Programy emitowane w tej chwili (z przewodnika EPG)") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 24.dp, top = 6.dp, bottom = 6.dp)) {
                        items(onAirNow, key = { "now_" + it.first.streamUrl }) { (channel, prog) ->
                            MediaCard(
                                title = prog.title,
                                subtitle = channel.name,
                                imageUrl = prog.image,
                                logoUrl = forgeChannelLogo(channel),
                                badge = "NA ŻYWO",
                                badgeColor = com.nuvio.tv.skin.kicia.tokens.ForgeColors.Live,
                                modifier = (if (shownAlerts.isEmpty() && onAirNow.first().first === channel) Modifier.focusRequester(firstFocus) else Modifier).then(heroOn(TvHero(prog.title, channel.name, live = true))),
                                onClick = { onPlay(channel, null) },
                                onLongClick = { detail = channel to prog },
                            )
                        }
                    }
                }
            }
        }

        // v6 S17 "Gatunki i tematy": the on-air programmes of the three biggest channel groups of the playlist (≥ 3 each).
        // Owner 13.11: by the EPG category of the programme on air; the playlist groups only while the EPG gives no categories.
        val epgCats = onAirAll.associate { (ch, p) -> ch.streamUrl to ForgeEpgCategories.programmeBranchName(ch.guideKey, p) }
        val useEpg = epgCats.values.any { it != null }
        val groupRows = onAirAll.filter { if (useEpg) epgCats[it.first.streamUrl] != null else it.first.group.isNotBlank() }
            .groupBy { if (useEpg) epgCats.getValue(it.first.streamUrl)!! else it.first.group }
            .filterValues { it.size >= 3 }.entries.sortedByDescending { it.value.size }.take(3)
        groupRows.forEach { (groupName, rows) ->
            item(key = "group_$groupName") {
                Section(groupName, null) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 24.dp, top = 6.dp, bottom = 6.dp)) {
                        items(rows.take(15), key = { "grp_" + groupName + it.first.streamUrl }) { (channel, prog) ->
                            MediaCard(
                                title = prog.title, subtitle = channel.name, imageUrl = prog.image, logoUrl = forgeChannelLogo(channel),
                                badge = null, badgeColor = Color.Unspecified,
                                modifier = heroOn(TvHero(prog.title, channel.name, live = true)),
                                onClick = { onPlay(channel, null) },
                                onLongClick = { detail = channel to prog },
                            )
                        }
                    }
                }
            }
        }

        if (shownChannels.isNotEmpty()) {
            item {
                Section(stringResource(R.string.forge_tv_recent_channels), null) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(end = 24.dp, top = 6.dp, bottom = 6.dp)) {
                        items(shownChannels, key = { it.first.streamUrl }) { (recent, channel) ->
                            ChannelTile(
                                name = recent.name,
                                logoUrl = forgeChannelLogo(channel) ?: recent.logoUrl,
                                modifier = (if (shownAlerts.isEmpty() && onAirNow.isEmpty() && recent === shownChannels.first().first) Modifier.focusRequester(firstFocus) else Modifier).then(heroOn(TvHero(recent.name, recent.group.ifBlank { "Kanał" }, live = true))),
                                onClick = { onPlay(channel, null) },
                                onLongClick = { TvStore.removeChannel(recent.streamUrl) },
                            )
                        }
                    }
                }
            }
        }

        if (shownShows.isNotEmpty()) {
            item {
                Section(stringResource(R.string.forge_tv_recent_programmes), stringResource(R.string.forge_tv_recent_programmes_note)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(end = 24.dp, top = 6.dp, bottom = 6.dp)) {
                        items(shownShows, key = { it.first.key }) { (show, channel) ->
                            MediaCard(
                                title = show.title,
                                subtitle = channel.name,
                                imageUrl = show.imageUrl,
                                logoUrl = forgeChannelLogo(channel) ?: show.logoUrl,
                                badge = null,
                                badgeColor = Color.Unspecified,
                                modifier = (if (shownAlerts.isEmpty() && onAirNow.isEmpty() && shownChannels.isEmpty() && show === shownShows.first().first) Modifier.focusRequester(firstFocus) else Modifier).then(heroOn(TvHero(show.title, channel.name, live = false))),
                                width = 150.dp,
                                height = 84.dp,
                                onClick = { onPlay(channel, null) },
                                onLongClick = { TvStore.remove(show.key) },
                            )
                        }
                    }
                }
            }
        }
    }
    if (searching) ForgeTvSearch(channels = channels, onPlay = onPlay, onClose = { searching = false }, host = "przegladaj")
    ForgeTvSearchReturn("przegladaj") { searching = true } // forge fix 14.11 (JMGO A3): back from the details or the player = the results again
    detail?.let { (ch, p) ->
        val now = System.currentTimeMillis()
        ForgeProgramDetail(
            channel = ch,
            programme = p,
            onWatchLive = { detail = null; onPlay(ch, null) },
            onWatchFromStart = if (ch.catchup != null && p.startEpochMs < now) ({ detail = null; onPlay(ch, p) }) else null,
            onDismiss = { detail = null },
            onPlayAiring = { airing -> detail = null; onPlay(ch, airing) }, // forge fix 15.11 (JMGO): any archived episode plays
        )
    }
    }
}

@Composable
private fun badgeFor(kind: AlertKind): Pair<String, Color> = when (kind) {
    AlertKind.NEW_EPISODE -> stringResource(R.string.forge_tv_badge_new) to NewColor
    AlertKind.MISSED_IN_ARCHIVE -> stringResource(R.string.forge_tv_badge_archive) to ArchiveColor
    AlertKind.UPCOMING -> stringResource(R.string.forge_tv_badge_upcoming) to UpcomingColor
}

/** An airing that has started plays (live, or from the archive once over); one still to come opens the guide. */
private fun onAlert(
    alert: EpisodeAlert,
    channel: LiveTvChannel,
    onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit,
    onOpenGuide: () -> Unit,
    nowMs: Long = System.currentTimeMillis(),
) {
    val a = alert.airing
    when {
        a.startMs > nowMs -> onOpenGuide()
        a.stopMs <= nowMs -> onPlay(channel, LiveTvProgramme(a.title, a.startMs, a.stopMs, a.description))
        else -> onPlay(channel, null)
    }
}

@Composable
private fun Section(title: String, note: String?, content: @Composable () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(bottom = 6.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = NuvioTheme.colors.TextPrimary, fontWeight = FontWeight.SemiBold)
            if (note != null) {
                Spacer(Modifier.width(10.dp))
                Text(text = note, style = MaterialTheme.typography.labelMedium, color = NuvioTheme.colors.TextPrimary.copy(alpha = 0.55f))
            }
        }
        content()
    }
}

@Composable
private fun MediaCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    logoUrl: String?,
    badge: String?,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 196.dp,
    height: androidx.compose.ui.unit.Dp = 110.dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.width(width).height(height),
        shape = CardDefaults.shape(CardShape),
        colors = CardDefaults.colors(containerColor = Color.White.copy(alpha = 0.10f)),
        scale = CardDefaults.scale(focusedScale = 1.05f),
        border = CardDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = CardShape)),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else if (!logoUrl.isNullOrBlank()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    com.nuvio.tv.skin.kicia.components.ForgeAutoLogo( // black-and-white logo, no plate (filter per logo)
                        url = logoUrl,
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                    )
                }
            }
            Box(
                Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))),
            )
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF06251A),
                    modifier = Modifier.padding(8.dp).clip(RoundedCornerShape(6.dp)).background(badgeColor).padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
            Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.82f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ChannelTile(
    name: String,
    logoUrl: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(104.dp)) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier.size(com.nuvio.tv.skin.kicia.tokens.ForgeDims.ChannelTile),
            shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
            colors = CardDefaults.colors(containerColor = Color.White.copy(alpha = 0.10f)),
            scale = CardDefaults.scale(focusedScale = 1.1f),
            border = CardDefaults.border(focusedBorder = Border(BorderStroke(2.dp, Color.White), shape = RoundedCornerShape(12.dp))),
        ) {
            Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
                if (!logoUrl.isNullOrBlank()) {
                    com.nuvio.tv.skin.kicia.components.ForgeAutoLogo(url = logoUrl, contentDescription = name, modifier = Modifier.fillMaxSize())
                } else {
                    Text(text = name.take(2).uppercase(), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = NuvioTheme.colors.TextPrimary.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp).fillMaxWidth(),
        )
    }
}

@Composable
private fun EmptyState() {
    Column(Modifier.padding(top = 40.dp).fillMaxWidth(0.6f)) {
        Text(text = stringResource(R.string.forge_tv_empty_title), style = MaterialTheme.typography.titleLarge, color = NuvioTheme.colors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(R.string.forge_tv_empty_body), style = MaterialTheme.typography.bodyMedium, color = NuvioTheme.colors.TextPrimary.copy(alpha = 0.7f))
    }
}

/** What the hero of the browse screen shows for the focused item. */
private data class TvHero(val title: String, val meta: String, val live: Boolean)
