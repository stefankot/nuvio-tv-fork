package com.nuvio.tv.skin.kicia.livetv

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.reshaped.livetv.LiveTvCatchupLinks
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.reshaped.livetv.LiveTvUiState
import com.nuvio.tv.skin.kicia.components.ForgeAutoLogo
import com.nuvio.tv.skin.kicia.components.ForgeBadge
import com.nuvio.tv.skin.kicia.components.ForgeBadgeKind
import com.nuvio.tv.skin.kicia.components.ForgeSheetRow
import com.nuvio.tv.skin.kicia.components.ForgeToast
import com.nuvio.tv.skin.kicia.components.ForgeWideCard
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Owner 13.11 (layout added to the mockup as 06 `programy-kategorii`): picking in the "Programy" tab of Kategorie shows,
 * in place of the EPG grid, the category tree on the left (Wiersz 36 rail, branches open their leaves) and on the right
 * every programme of the chosen category as tiles, one tile per programme (its airings merged), freshest airing first:
 * on now, then the archive newest first, then later airings soonest first.
 * OK on a tile: on now = the channel live, past with an archive = that recording (all airings race), later = a reminder.
 * Back (or ◀ at the tree) closes the view.
 */
internal object ForgeProgrammeView {
    var selection by mutableStateOf<String?>(null)
        private set
    var focused by mutableStateOf(false)
    fun open(key: String) { selection = key }
    fun close() { selection = null; focused = false }
    val active: Boolean get() = selection != null
}

/** One programme of the list: its freshest airing and how many airings it has. */
private data class ProgrammeEntry(val channel: LiveTvChannel, val programme: LiveTvProgramme, val airings: Int)

private const val DAY_MS = 86_400_000L
private const val MAX_ENTRIES = 300

private fun buildEntries(channels: List<LiveTvChannel>, key: String, now: Long): List<ProgrammeEntry> {
    val groups = LinkedHashMap<String, MutableList<Pair<LiveTvChannel, LiveTvProgramme>>>()
    val seen = HashSet<String>()
    channels.forEach { ch ->
        if (!seen.add(ch.guideKey)) return@forEach
        LiveTvRepository.schedule(ch.guideKey).forEach { p ->
            if (p.startEpochMs > now + 2 * DAY_MS) return@forEach
            // Past airings count only where they can be played again.
            if (p.stopEpochMs <= now && !LiveTvCatchupLinks.isPlayable(ch.catchup, p, now)) return@forEach
            if (!ForgeEpgCategories.programmeMatches(ch.guideKey, p, key)) return@forEach
            val id = p.title.trim().lowercase() + "|" + (p.episode?.trim()?.lowercase() ?: ("@" + p.startEpochMs))
            groups.getOrPut(id) { ArrayList() } += ch to p
        }
    }
    fun started(p: LiveTvProgramme) = p.startEpochMs <= now
    return groups.values.map { airings ->
        // The freshest airing: the latest that has started, else the next one.
        val best = airings.filter { started(it.second) }.maxByOrNull { it.second.startEpochMs } ?: airings.minBy { it.second.startEpochMs }
        ProgrammeEntry(best.first, best.second, airings.size)
    }.sortedWith(compareBy<ProgrammeEntry> { if (started(it.programme)) 0 else 1 }
        .thenBy { if (started(it.programme)) -it.programme.startEpochMs else it.programme.startEpochMs })
        .take(MAX_ENTRIES)
}

/** Drawn over the grid's place (hook in LiveTvScreen) while a programme category is chosen. */
@Composable
internal fun ForgeProgrammeRows(uiState: LiveTvUiState, onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit, modifier: Modifier = Modifier) {
    val key = ForgeProgrammeView.selection ?: return
    val context = LocalContext.current
    val minute = LiveTvClock.nowEpochMs() / 60_000
    val tree = remember(uiState.channels, uiState.guideVersion, ForgeEpgCategories.programmeVersion()) { ForgeEpgCategories.programmeTree(uiState.channels) }
    val entries by produceState<List<ProgrammeEntry>?>(null, key, uiState.channels, uiState.guideVersion, ForgeEpgCategories.programmeVersion(), minute / 5) {
        value = null
        value = withContext(Dispatchers.Default) { runCatching { buildEntries(uiState.channels, key, LiveTvClock.nowEpochMs()) }.getOrDefault(emptyList()) }
    }
    val chosenRow = remember { FocusRequester() }
    val firstTile = remember { FocusRequester() }
    // Opening: the focus goes to the chosen category in the tree.
    LaunchedEffect(Unit) { repeat(10) { kotlinx.coroutines.delay(60); if (runCatching { chosenRow.requestFocus() }.getOrDefault(false)) return@LaunchedEffect } }
    val close = {
        ForgeProgrammeView.close()
        ForgeGuideFocus.lastTool?.let { runCatching { it.requestFocus() } }
    }
    Row(
        modifier
            .background(ForgeColors.Bg)
            .onFocusChanged { ForgeProgrammeView.focused = it.hasFocus }
            .onPreviewKeyEvent { e ->
                val k = e.nativeKeyEvent
                if (k.keyCode != android.view.KeyEvent.KEYCODE_BACK && k.keyCode != android.view.KeyEvent.KEYCODE_ESCAPE) return@onPreviewKeyEvent false
                if (k.action == android.view.KeyEvent.ACTION_UP) close()
                true
            }
            .focusProperties { ForgeGuideFocus.lastTool?.let { up = it } }
            .focusGroup(),
    ) {
        // Left: the tree (Wiersz 36, 240 wide); the chosen branch shows its leaves under it.
        val openBranch = key.removePrefix(ForgeEpgCategories.PROG_PREFIX).substringBefore('/').takeIf { it != "*" }
        val treeRows = remember(tree, openBranch) {
            buildList {
                add(Triple(ForgeEpgCategories.PROG_ALL, "Wszystkie programy", 0))
                tree.forEach { n ->
                    add(Triple(n.key, n.name, 0))
                    if (n.key.removePrefix(ForgeEpgCategories.PROG_PREFIX) == openBranch) n.children.forEach { add(Triple(it.key, it.name, 1)) }
                }
            }
        }
        LazyColumn(
            Modifier.width(240.dp).fillMaxHeight().focusProperties { right = firstTile }.focusGroup(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(treeRows, key = { it.first }) { (k, name, depth) ->
                val count = if (k == ForgeEpgCategories.PROG_ALL) tree.sumOf { it.count } else (tree.firstOrNull { it.key == k }?.count ?: tree.flatMap { it.children }.firstOrNull { it.key == k }?.count ?: 0)
                ForgeSheetRow(
                    text = name, detail = count.toString(), rail = true, selected = k == key,
                    focusRequester = if (k == key) chosenRow else null,
                    modifier = Modifier.padding(start = (depth * 16).dp),
                ) { ForgeProgrammeView.open(k) }
            }
        }
        Spacer(Modifier.width(24.dp))
        // Right: the programmes of the category, freshest airing first.
        Column(Modifier.weight(1f).fillMaxHeight()) {
            val list = entries
            Text(
                ForgeEpgCategories.nameOf(key) + (list?.let { " · " + it.size } ?: ""),
                style = ForgeText.title, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            when {
                list == null -> Text("Wczytuję programy…", style = ForgeText.body.copy(color = ForgeColors.Text3))
                list.isEmpty() -> Text("Brak programów w tej kategorii.", style = ForgeText.body.copy(color = ForgeColors.Text3))
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    modifier = Modifier.fillMaxSize().focusProperties { left = chosenRow }.focusGroup(),
                    contentPadding = PaddingValues(top = 8.dp, end = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(list, key = { it.channel.guideKey + "|" + it.programme.startEpochMs }) { e ->
                        ProgrammeTile(e, if (e === list.first()) firstTile else null) {
                            val now = LiveTvClock.nowEpochMs()
                            val p = e.programme
                            when {
                                p.startEpochMs <= now && p.stopEpochMs > now -> onPlay(e.channel, null)
                                p.stopEpochMs <= now -> onPlay(e.channel, p)
                                else -> {
                                    com.nuvio.tv.fork.tv.ForgeReminders.toggle(context.applicationContext, com.nuvio.tv.fork.tv.ForgeReminder(e.channel.streamUrl, p.startEpochMs, p.title, e.channel.name))
                                    ForgeToast.show("Przypomnienie: " + p.title + " · " + forgeTvTime(p.startEpochMs), top = true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Karta/Odcinek 160 x 114 with the programme's picture (else the channel's logo), badge, title and "channel · when" under it. */
@Composable
private fun ProgrammeTile(e: ProgrammeEntry, focusRequester: FocusRequester?, onOk: () -> Unit) {
    val now = LiveTvClock.nowEpochMs()
    val p = e.programme
    val live = p.startEpochMs <= now && p.stopEpochMs > now
    val past = p.stopEpochMs <= now
    Column {
        ForgeWideCard(focusRequester = focusRequester, onOk = onOk) { _ ->
            Box(Modifier.fillMaxSize().background(ForgeColors.CardBase)) {
                val image = p.image?.takeIf { it.isNotBlank() }
                val logo = forgeChannelLogo(e.channel)
                if (image != null) AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else if (!logo.isNullOrBlank()) ForgeAutoLogo(url = logo, contentDescription = null, modifier = Modifier.align(Alignment.Center).padding(28.dp))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to Color.Black.copy(alpha = .6f))))
                when {
                    live -> ForgeBadge("Na żywo", ForgeBadgeKind.Live, Modifier.align(Alignment.TopStart).padding(8.dp))
                    past -> ForgeBadge("Archiwum", ForgeBadgeKind.Archive, Modifier.align(Alignment.TopStart).padding(8.dp))
                    else -> ForgeBadge(forgeTvTime(p.startEpochMs), ForgeBadgeKind.Soon, Modifier.align(Alignment.TopStart).padding(8.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(p.title, style = ForgeText.label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(160.dp).padding(horizontal = 8.dp))
        Text(
            e.channel.name + " · " + forgeWhen(p.startEpochMs, now) + (if (e.airings > 1) " · " + e.airings + " emisje" else ""),
            style = ForgeText.caption.copy(color = ForgeColors.Text3), maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(160.dp).padding(horizontal = 8.dp),
        )
    }
}

/** "dziś 20:00", "wczoraj 18:30", "pt 21:15". */
private fun forgeWhen(ms: Long, now: Long): String {
    val zone = java.util.TimeZone.getDefault()
    fun day(t: Long) = (t + zone.getOffset(t)) / DAY_MS
    val d = day(ms) - day(now)
    val wd = listOf("niedz", "pon", "wt", "śr", "czw", "pt", "sob")[java.util.Calendar.getInstance(zone).apply { timeInMillis = ms }.get(java.util.Calendar.DAY_OF_WEEK) - 1]
    val label = when (d) { 0L -> "dziś"; -1L -> "wczoraj"; 1L -> "jutro"; else -> wd }
    return label + " " + forgeTvTime(ms)
}
