package com.nuvio.tv.skin.kicia.livetv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.nuvio.tv.reshaped.livetv.LiveTvCatchupLinks
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.kicia.components.ForgeBadge
import com.nuvio.tv.skin.kicia.components.ForgeBadgeKind
import com.nuvio.tv.skin.kicia.components.ForgeButton
import com.nuvio.tv.skin.kicia.components.ForgeField
import com.nuvio.tv.skin.kicia.components.ForgeLiveDot
import com.nuvio.tv.skin.kicia.components.ForgePillSelect
import com.nuvio.tv.skin.kicia.components.ForgeSheetRow
import com.nuvio.tv.skin.kicia.keyboard.ForgeKeyboardLayout
import com.nuvio.tv.skin.kicia.keyboard.ForgeOnScreenKeyboard
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import kotlinx.coroutines.Dispatchers
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.nuvio.tv.fork.logged

/**
 * v7.6 (mockup 06, state „szukaj”, owner 13.11): Live TV search. The board on the left; on the right the field
 * „Kanał, numer lub program”, the button „Kategoria: …” and the results: „Ostatnio szukane” while nothing is typed,
 * else „Kanały” (name or number, 4) and „Programy” (the whole guide, 8; title, „kanał · dzień hh:mm–hh:mm · kategoria”,
 * availability badge), or „Nic nie znaleziono dla «…».” with „Wyczyść”. OK on a channel plays it; OK on a programme
 * opens Info i odcinki. A programme category other than „Wszystkie programy” opens the programme view instead (7.6b).
 * Drawn in its own window, so the guide's layout (the programme view sits in the grid's place) never clips it.
 * (Before: v6 S18, only programmes on now and channel names.)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ForgeTvSearch(
    channels: List<LiveTvChannel>,
    onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit,
    onClose: () -> Unit,
    /** forge fix 14.11 (JMGO A3): the screen this search belongs to (see [ForgeTvSearchReturn]); null = no return. */
    host: String? = null,
) {
    // JMGO test 14.11 (A3): only the user closing the search ends the way back to it
    val userClose = { ForgeTvSearchMemory.reopenHost = null; ForgeTvSearchMemory.focusKey = null; onClose() }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = userClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BackHandler(onBack = userClose)
        val state by LiveTvRepository.uiState.collectAsStateWithLifecycle()
        var query by remember { mutableStateOf(ForgeTvSearchMemory.lastQuery) }
        var category by remember { mutableStateOf(ForgeEpgCategories.PROG_ALL) }
        var filterOpen by remember { mutableStateOf(false) }
        val firstKey = remember { FocusRequester() }
        val returnTo = remember { FocusRequester() } // forge fix 14.11 (JMGO A3): the result picked before, when coming back
        val returnKey = remember { ForgeTvSearchMemory.focusKey }
        LaunchedEffect(Unit) {
            if (returnKey != null) repeat(50) { delay(100); if (runCatching { returnTo.requestFocus() }.getOrDefault(false)) return@LaunchedEffect }
            repeat(10) { delay(60); if (runCatching { firstKey.requestFocus() }.getOrDefault(false)) return@LaunchedEffect }
        }
        val leaveTo = { key: String -> if (host != null) { ForgeTvSearchMemory.reopenHost = host; ForgeTvSearchMemory.focusKey = key } }
        val q = query.trim()
        val setQuery = { text: String -> query = text; ForgeTvSearchMemory.lastQuery = text }
        // The mockup waits 250 ms after the last letter before searching again.
        val results by produceState<ForgeTvSearchResults?>(null, q, category, channels, state.guideVersion) {
            if (q.isNotEmpty()) delay(250)
            value = withContext(Dispatchers.Default) {
                runCatching { forgeTvSearch(q, category, channels, state.currentProgrammes, LiveTvClock.nowEpochMs()) }.logged("ForkKiciaLivetv", "ForgeTvSearch.kt:100").getOrNull()
            }
        }
        val keep = { if (q.isNotEmpty()) ForgeTvSearchMemory.add(q) }
        Box(Modifier.fillMaxSize().background(ForgeColors.Bg).padding(start = 40.dp, top = 40.dp, end = 40.dp, bottom = 24.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.width(380.dp)) {
                    ForgeOnScreenKeyboard(
                        text = query, onTextChange = setQuery, onSearch = keep,
                        layout = ForgeKeyboardLayout.Search6, firstKeyFocus = firstKey, // docked: „abc | 123” (channel numbers), as Szukaj 09
                    )
                }
                Column(Modifier.fillMaxWidth()) {
                    // mockup (ukladSzukania): the field, the tools under it, then the results
                    ForgeField(text = query, placeholder = "Kanał, numer lub program", modifier = Modifier.fillMaxWidth(), fill = true, onOk = { runCatching { firstKey.requestFocus() } })
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ForgePillSelect(
                            text = "Kategoria: " + if (category == ForgeEpgCategories.PROG_ALL) "Wszystkie programy" else ForgeEpgCategories.nameOf(category),
                            selected = false, bare = false, icon = Icons.Outlined.FormatListBulleted,
                            onOk = { filterOpen = true },
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    val r = results
                    when {
                        q.isEmpty() && category == ForgeEpgCategories.PROG_ALL -> {
                            if (ForgeTvSearchMemory.recent.isNotEmpty()) {
                                Text("Ostatnio szukane", style = ForgeText.shelfTitle.copy(color = ForgeColors.Text2))
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ForgeTvSearchMemory.recent.forEach { past ->
                                        ForgePillSelect(text = past, selected = false, bare = false, onOk = { setQuery(past) })
                                    }
                                }
                            } else {
                                Text("Wpisz nazwę kanału, jego numer albo tytuł programu.", style = ForgeText.body.copy(color = ForgeColors.Text3))
                            }
                        }
                        r == null -> Text("Szukam…", style = ForgeText.body.copy(color = ForgeColors.Text3))
                        r.channels.isEmpty() && r.programmes.isEmpty() -> Column {
                            Text(if (q.isEmpty()) "Brak programów w tej kategorii." else "Nic nie znaleziono dla «$q».", style = ForgeText.body)
                            Spacer(Modifier.height(12.dp))
                            ForgeButton(text = "Wyczyść", onOk = { setQuery(""); category = ForgeEpgCategories.PROG_ALL; runCatching { firstKey.requestFocus() } })
                        }
                        else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (r.channels.isNotEmpty()) {
                                item(key = "h-ch") { Text("Kanały", style = ForgeText.shelfTitle.copy(color = ForgeColors.Text2), modifier = Modifier.padding(bottom = 4.dp)) }
                                items(r.channels, key = { "ch|" + it.channel.streamUrl }) { hit ->
                                    val rowKey = "ch|" + hit.channel.streamUrl
                                    ForgeSheetRow(
                                        text = hit.number.toString() + "   " + hit.channel.name,
                                        description = hit.now?.let { "Teraz: " + it.title },
                                        modifier = if (rowKey == returnKey) Modifier.focusRequester(returnTo) else Modifier,
                                    ) { keep(); leaveTo(rowKey); onPlay(hit.channel, null); onClose() }
                                }
                            }
                            if (r.programmes.isNotEmpty()) {
                                item(key = "h-pr") { Text("Programy", style = ForgeText.shelfTitle.copy(color = ForgeColors.Text2), modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                                // makieta 7.7b H15: the programmes as a grid of tiles with a picture (as the programme view), up to 8;
                                // the picture OK = the player (F30: live → the channel, archive → from the start, future → a reminder),
                                // the text block = 08
                                item(key = "pr-grid") {
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                                        r.programmes.take(8).forEach { hit ->
                                            val rowKey = "pr|" + hit.channel.streamUrl + "|" + hit.programme.startEpochMs
                                            val now = LiveTvClock.nowEpochMs()
                                            val airings = LiveTvRepository.schedule(hit.channel.guideKey).count { it.title.trim().equals(hit.programme.title.trim(), ignoreCase = true) }
                                            ProgrammeTile(
                                                ProgrammeEntry(hit.channel, hit.programme, airings.coerceAtLeast(1)),
                                                focusRequester = if (rowKey == returnKey) returnTo else null,
                                                onFocus = {},
                                                onDetails = { keep(); leaveTo(rowKey); ForgeGuideDetail.open(hit.channel, hit.programme); onClose() },
                                                onOk = {
                                                    keep(); leaveTo(rowKey)
                                                    when {
                                                        hit.programme.startEpochMs > now -> {
                                                            com.nuvio.tv.fork.tv.Reminders.toggle(context.applicationContext, com.nuvio.tv.fork.tv.Reminder(hit.channel.streamUrl, hit.programme.startEpochMs, hit.programme.title, hit.channel.name))
                                                            com.nuvio.tv.skin.kicia.components.ForgeToast.show("Przypomnę: " + hit.programme.title + " · " + forgeTvTime(hit.programme.startEpochMs))
                                                        }
                                                        hit.programme.stopEpochMs > now -> { onPlay(hit.channel, null); onClose() }
                                                        else -> { onPlay(hit.channel, hit.programme); onClose() }
                                                    }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (filterOpen) ForgeFilterSheet(
            state, category,
            onPick = { key -> com.nuvio.tv.skin.kicia.components.ForgeToast.show("Kategoria kanałów: " + ForgeEpgCategories.nameOf(key)) },
            onDismiss = { filterOpen = false },
            startOnProgrammes = true,
            onProgrammePick = { key ->
                if (key == ForgeEpgCategories.PROG_ALL) {
                    category = key
                } else {
                    keep()
                    ForgeProgrammeView.open(key) // v7.6b: a chosen programme category = the programme view (one place)
                    userClose()
                }
            },
        )
    }
}

/** What was searched in this run of the app (mockup: „Ostatnio szukane”), newest first. */
internal object ForgeTvSearchMemory {
    var lastQuery = ""
    val recent = mutableStateListOf<String>()
    fun add(q: String) {
        recent.removeAll { it.equals(q, ignoreCase = true) }
        recent.add(0, q)
        while (recent.size > 8) recent.removeAt(recent.lastIndex)
    }

    /**
     * forge fix 14.11 (JMGO A3): which screen's search a result was opened from, so leaving the details or the player
     * comes back to the results, on that result ([focusKey]), not to the guide under them.
     */
    var reopenHost by mutableStateOf<String?>(null)
    var focusKey: String? = null
}

/**
 * forge fix 14.11 (JMGO A3): put in each screen that opens the search ([host] names it): once the details opened from a
 * result are closed, or the screen is back from the player, the search opens again.
 */
@Composable
internal fun ForgeTvSearchReturn(host: String, onReopen: () -> Unit) {
    val pending = ForgeTvSearchMemory.reopenHost == host
    val detailOpen = ForgeGuideDetail.request != null
    // JMGO test 14.11: „Oglądaj” closed the details and reopened the search under the player starting; that screen state was lost
    // with the player, so back from it showed the guide. Only a resumed screen (not under the player) takes the return, a moment later.
    val resumed = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle.currentStateAsState().value.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
    // The return stays until the user closes the search (Back): „Odtwórz z archiwum” races the airings for seconds before the
    // player opens, so a search reopened meanwhile was lost with the screen; a rebuilt screen opens it again.
    LaunchedEffect(pending, detailOpen, resumed) {
        if (pending && !detailOpen && resumed) {
            delay(400)
            onReopen()
        }
    }
}

internal class ForgeTvChannelHit(val channel: LiveTvChannel, val number: Int, val now: LiveTvProgramme?)
internal class ForgeTvProgrammeHit(
    val channel: LiveTvChannel,
    val programme: LiveTvProgramme,
    val time: String,
    val category: String?,
    val badge: Pair<String, ForgeBadgeKind>,
    /** forge fix 14.11 (JMGO C2): how many airings of this programme were found. */
    val airings: Int = 1,
)
internal class ForgeTvSearchResults(val channels: List<ForgeTvChannelHit>, val programmes: List<ForgeTvProgrammeHit>)

/**
 * The mockup's search: channels by name or number (only with a query; 4), programmes of the whole guide by title within
 * the programme [category] (8): on now first, then the soonest to come, then the latest that can be replayed. Past
 * programmes count only where they can be played again.
 */
internal fun forgeTvSearch(
    query: String,
    category: String,
    channels: List<LiveTvChannel>,
    current: Map<String, LiveTvProgramme>,
    now: Long,
    schedule: (String) -> List<LiveTvProgramme> = LiveTvRepository::schedule,
    inCategory: (LiveTvChannel, LiveTvProgramme) -> Boolean = { ch, p -> ForgeEpgCategories.programmeMatches(ch.guideKey, p, category) },
    categoryName: (LiveTvChannel, LiveTvProgramme) -> String? = { ch, p -> ForgeEpgCategories.programmeBranchName(ch.guideKey, p) },
    /** 22.11 (właściciel): limity nie są celowe — Niunia pokazuje 30 kanałów i 50 tytułów; ekran Kici zostaje przy 4/12. */
    maxChannels: Int = 4,
    maxProgrammes: Int = FORGE_TV_SEARCH_PROGRAMMES,
    /** Kanał po kluczu przewodnika dla [channels] (Niunia ma go z góry); null = policzony tu (zapamiętany dla tej listy). */
    byKey: Map<String, LiveTvChannel>? = null,
): ForgeTvSearchResults {
    val index = com.nuvio.tv.fork.tv.index.TvTextIndex
    val q = ForgeEpgCategories.norm(query.trim())
    val anyCategory = category == ForgeEpgCategories.PROG_ALL
    if (q.isEmpty() && anyCategory) return ForgeTvSearchResults(emptyList(), emptyList())
    val chHits = if (q.isEmpty()) emptyList() else forgeTvChannelHits(q, channels, current, maxChannels)
    fun rank(p: LiveTvProgramme): Long = when {
        p.startEpochMs <= now && p.stopEpochMs > now -> 0L
        p.stopEpochMs <= now -> 1L + (now - p.startEpochMs).coerceAtMost(Long.MAX_VALUE / 4)
        else -> Long.MAX_VALUE / 2 + (p.startEpochMs - now)
    }
    // forge fix 14.11 (JMGO C2): one row per programme (all its airings and episodes, as in the programme view); a title
    // whose word starts with the text first, then: on now, the newest that can be replayed, the soonest to come.
    // 22.11: per tytuł tylko najlepsza emisja i licznik (bez listy wszystkich emisji w pamięci).
    class Group(var best: Pair<LiveTvChannel, LiveTvProgramme>, var count: Int, val wordStart: Boolean)
    val groups = HashMap<String, Group>()
    fun add(title: String, ch: LiveTvChannel, p: LiveTvProgramme) {
        if (p.stopEpochMs <= now && !LiveTvCatchupLinks.isPlayable(ch.catchup, p, now)) return
        if (!anyCategory && !inCategory(ch, p)) return
        val g = groups[title]
        if (g == null) groups[title] = Group(ch to p, 1, forgeWordStarts(title, q))
        else { g.count++; if (rank(p) < rank(g.best.second)) g.best = ch to p }
    }
    if (q.isNotEmpty() && index.ready()) {
        // 22.11: spis tytułów liczony w tle (TvTextIndex) — przy zapytaniu bez normalizacji całego przewodnika.
        val keyMap = byKey ?: forgeChannelsByKey(channels)
        val matched = index.titles(q).let { all -> if (q.length < 2) all.filter { forgeWordStarts(it.first, q) } else all }
        for ((title, keys) in matched) for (key in keys) {
            val ch = keyMap[key] ?: continue
            for (p in schedule(key)) if (index.titleOf(p.title) == title) add(title, ch, p)
        }
    } else {
        val seen = HashSet<String>()
        channels.forEach { ch ->
            if (!seen.add(ch.guideKey)) return@forEach
            schedule(ch.guideKey).forEach { p ->
                val title = index.titleOf(p.title)
                if (q.isNotEmpty() && !title.contains(q)) return@forEach
                add(title, ch, p)
            }
        }
    }
    val prHits = groups.values.sortedWith(compareBy<Group>({ if (it.wordStart) 0 else 1 }, { rank(it.best.second) }))
        .take(maxProgrammes)
        .map { it.best to it.count }
        .map { (best, count) ->
            val (ch, p) = best
            val badge = when {
                p.startEpochMs > now -> "Wkrótce" to ForgeBadgeKind.Soon
                p.stopEpochMs <= now -> "Archiwum" to ForgeBadgeKind.Archive
                else -> "Na żywo" to ForgeBadgeKind.Live
            }
            val time = forgeTvSearchDay(p.startEpochMs, now) + " " + forgeTvTime(p.startEpochMs) + "–" + forgeTvTime(p.stopEpochMs) +
                (if (count > 1) " · " + count + (if (count < 5) " emisje" else " emisji") else "")
            ForgeTvProgrammeHit(ch, p, time, categoryName(ch, p), badge, count)
        }
    return ForgeTvSearchResults(chHits, prHits)
}

/**
 * Kanały do frazy [q] (już po norm): numer, potem nazwa bez kraju/jakości równa frazie, zaczynająca się od niej, słowo od niej,
 * na końcu zawierająca; przy równej ocenie kolejność listy (polskie pierwsze). Ten sam kanał w kilku jakościach/źródłach raz.
 * Teksty z TvTextIndex (liczone raz na nazwę, w tle).
 */
internal fun forgeTvChannelHits(q: String, channels: List<LiveTvChannel>, current: Map<String, LiveTvProgramme>, max: Int): List<ForgeTvChannelHit> {
    val index = com.nuvio.tv.fork.tv.index.TvTextIndex
    val number = q.toIntOrNull()
    val scored = ArrayList<Pair<Int, Int>>() // ocena, pozycja
    for (i in channels.indices) {
        val ch = channels[i]
        val score = if (number != null && number == i + 1) 0 else {
            val n = index.nameOf(ch.name)
            if (!n.contains(q)) continue
            val k = index.keyOf(ch.name)
            when {
                k == q -> 1
                k.startsWith(q) -> 2
                forgeWordStarts(n, q) -> 3
                else -> 4
            }
        }
        scored += score to i
    }
    scored.sortWith(compareBy({ it.first }, { it.second }))
    val seen = HashSet<String>()
    val out = ArrayList<ForgeTvChannelHit>()
    for ((_, i) in scored) {
        if (out.size >= max) break
        val ch = channels[i]
        if (!seen.add(index.sameChannelKey(ch.name))) continue
        out += ForgeTvChannelHit(ch, i + 1, current[ch.guideKey])
    }
    return out
}

private class ForgeKeyMemo(val list: List<LiveTvChannel>, val map: Map<String, LiveTvChannel>)
@Volatile private var forgeKeyMemo: ForgeKeyMemo? = null

/** Pierwszy kanał listy po kluczu przewodnika, zapamiętany dla ostatniej listy (ten sam obiekt). */
internal fun forgeChannelsByKey(channels: List<LiveTvChannel>): Map<String, LiveTvChannel> =
    forgeKeyMemo?.takeIf { it.list === channels }?.map
        ?: HashMap<String, LiveTvChannel>(channels.size * 2).also { m -> channels.forEach { m.putIfAbsent(it.guideKey, it) } }
            .also { forgeKeyMemo = ForgeKeyMemo(channels, it) }

/** forge fix 14.11 (JMGO C2): programmes (titles) shown by the search. */
internal const val FORGE_TV_SEARCH_PROGRAMMES = 12

/** Whether a word of [title] (both normalised) starts with [q]; an empty [q] counts. */
internal fun forgeWordStarts(title: String, q: String): Boolean =
    q.isEmpty() || title.startsWith(q) || title.split(' ', '-', ':', '.', ',', '(', '"', '„').any { it.startsWith(q) }

/** "dziś", "wczoraj", "jutro" or the weekday, as the mockup's DANE.nazwaDnia. */
internal fun forgeTvSearchDay(ms: Long, now: Long, zone: java.util.TimeZone = java.util.TimeZone.getDefault()): String {
    val dayMs = 24L * 60 * 60 * 1000
    fun day(t: Long) = Math.floorDiv(t + zone.getOffset(t), dayMs)
    return when (day(ms) - day(now)) {
        0L -> "dziś"
        -1L -> "wczoraj"
        1L -> "jutro"
        else -> listOf("niedz.", "pon.", "wt.", "śr.", "czw.", "pt.", "sob.")[java.util.Calendar.getInstance(zone).apply { timeInMillis = ms }.get(java.util.Calendar.DAY_OF_WEEK) - 1]
    }
}
