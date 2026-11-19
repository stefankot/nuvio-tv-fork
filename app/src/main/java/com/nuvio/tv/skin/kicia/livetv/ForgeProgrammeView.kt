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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import com.nuvio.tv.skin.kicia.components.forgeCardShadow
import com.nuvio.tv.skin.kicia.components.forgeEdge
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
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
    /** makieta 7.7 F29: the phrase typed in „Szukaj” (system keyboard); filters with the category and the sort. */
    var phrase by mutableStateOf("")
    /** makieta 7.7b H11: the sort chosen in „Sortuj” (replaces the 7.6 switch Archiwum / Wkrótce). */
    var sort by mutableStateOf(ForgeProgrammeSort.NEWEST)
    /** 7.6 callers: Wkrótce = [ForgeProgrammeSort.SOON], anything else resets to [ForgeProgrammeSort.NEWEST]. */
    var upcoming: Boolean
        get() = sort == ForgeProgrammeSort.SOON
        set(v) { sort = if (v) ForgeProgrammeSort.SOON else ForgeProgrammeSort.NEWEST }
    /** JMGO uwaga 2: the tile under the focus; the guide header describes it instead of the grid's programme. */
    var focusedTile by mutableStateOf<Pair<LiveTvChannel, LiveTvProgramme>?>(null)
    fun open(key: String) { selection = key }
    fun close() { selection = null; focused = false; focusedTile = null; phrase = "" }
    val active: Boolean get() = selection != null
}

private val FORGE_MARKS = Regex("\\p{M}+")

/** makieta 7.7 F29: a title for matching — lower case, without Polish marks. */
internal fun forgeFold(t: String): String =
    FORGE_MARKS.replace(java.text.Normalizer.normalize(t.lowercase(), java.text.Normalizer.Form.NFD), "").replace('ł', 'l')

/** makieta 7.7 F27: the „Ulubione” entry of the programme tree (programmes followed with „+” on 08). */
internal const val FORGE_PROG_FAVOURITES = "prog:ulubione"

/** makieta 7.7 F26 / 7.7b H11: the five sorts of the programme view. */
internal enum class ForgeProgrammeSort(val label: String) {
    NEWEST("Najnowsze"), RECOMMENDED("Polecane"), POPULAR("Popularne"), LIVE("Na żywo"), SOON("Wkrótce"),
}

/** One programme of the list: its freshest airing and how many airings it has. */
internal data class ProgrammeEntry(val channel: LiveTvChannel, val programme: LiveTvProgramme, val airings: Int)

private const val DAY_MS = 86_400_000L
private const val MAX_ENTRIES = 300

/** JMGO uwaga 1: the header counts every programme of the category; the grid shows the first [MAX_ENTRIES]. */
internal class Entries(val shown: List<ProgrammeEntry>, val total: Int)

internal fun buildEntries(channels: List<LiveTvChannel>, key: String, now: Long, sort: ForgeProgrammeSort, phrase: String = ""): Entries {
    val upcoming = sort == ForgeProgrammeSort.SOON
    val favourites = com.nuvio.tv.fork.tv.TvStore.favoriteShows.value
    // makieta 7.7 F29: the phrase matches the title anywhere, case and Polish letters ignored, so a recurring programme is found whole
    // test 15.11 (JMGO): a Regex per title over ~100 000 airings kept „Wczytuję programy…” for minutes — one pattern, one result per title
    val cache = HashMap<String, Boolean>()
    val favCache = HashMap<String, Boolean>()
    val needle = forgeFold(phrase.trim())
    val groups = LinkedHashMap<String, MutableList<Pair<LiveTvChannel, LiveTvProgramme>>>()
    val seen = HashSet<String>()
    channels.forEach { ch ->
        if (!seen.add(ch.guideKey)) return@forEach
        LiveTvRepository.schedule(ch.guideKey).forEach { p ->
            if (p.startEpochMs > now + 2 * DAY_MS) return@forEach
            // Past airings count only where they can be played again.
            if (p.stopEpochMs <= now && !LiveTvCatchupLinks.isPlayable(ch.catchup, p, now)) return@forEach
            if (key == FORGE_PROG_FAVOURITES) { if (!favCache.getOrPut(p.title) { com.nuvio.tv.fork.tv.ShowIdentity.normalizeTitle(p.title) in favourites }) return@forEach } // makieta 7.7 F27; fork 19.11: raz na tytuł (7 regex na każdą emisję = wolne Ulubione)
            else if (!ForgeEpgCategories.programmeMatches(ch.guideKey, p, key)) return@forEach
            if (needle.isNotEmpty() && !cache.getOrPut(p.title) { forgeFold(p.title).contains(needle) }) return@forEach
            val id = p.title.trim().lowercase() // v7.6: ALL airings and episodes of a programme in one tile
            groups.getOrPut(id) { ArrayList() } += ch to p
        }
    }
    fun started(p: LiveTvProgramme) = p.startEpochMs <= now
    val all = groups.values.mapNotNull { airings ->
        // Archiwum: the latest airing that has started (on now or in the archive); Wkrótce: the next one that has not started.
        val best = when (sort) {
            ForgeProgrammeSort.SOON -> airings.filter { !started(it.second) }.minByOrNull { it.second.startEpochMs }
            ForgeProgrammeSort.LIVE -> airings.firstOrNull { started(it.second) && now < it.second.stopEpochMs } // only what airs now
            else -> airings.filter { started(it.second) }.maxByOrNull { it.second.startEpochMs }
        }
        best?.let { ProgrammeEntry(it.first, it.second, airings.size) }
    }.sortedWith(when (sort) {
        ForgeProgrammeSort.SOON -> compareBy { it.programme.startEpochMs }
        ForgeProgrammeSort.POPULAR -> compareByDescending<ProgrammeEntry> { it.airings }.thenByDescending { it.programme.startEpochMs } // the number of airings
        ForgeProgrammeSort.RECOMMENDED -> compareBy { it.programme.title.lowercase() } // makieta: AI later; a fixed order for now
        else -> compareByDescending { it.programme.startEpochMs }
    })
    return Entries(all.take(MAX_ENTRIES), groups.size) // forge fix 14.11 (JMGO): every programme of the category, as the tree counts
}

/** Drawn over the grid's place (hook in LiveTvScreen) while a programme category is chosen. */
@Composable
internal fun ForgeProgrammeRows(uiState: LiveTvUiState, onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit, modifier: Modifier = Modifier) {
    val key = ForgeProgrammeView.selection ?: return
    val context = LocalContext.current
    val minute = LiveTvClock.nowEpochMs() / 60_000
    val tree by produceState(ForkProgrammeCache.treeOrNull(uiState.channels, uiState.guideVersion) ?: emptyList(), uiState.channels, uiState.guideVersion, ForgeEpgCategories.programmeVersion()) { value = ForkProgrammeCache.tree(uiState.channels, uiState.guideVersion) } // fork 19.11: pamięć, poza wątkiem głównym
    val entries by produceState(ForkProgrammeCache.entriesOrNull(uiState.channels, uiState.guideVersion, key, ForgeProgrammeView.sort, ForgeProgrammeView.phrase), key, uiState.channels, uiState.guideVersion, ForgeEpgCategories.programmeVersion(), minute / 5, ForgeProgrammeView.sort, ForgeProgrammeView.phrase) {
        // fork 19.11: lista z pamięci (rozgrzewka w tle po wejściu w Live TV); „Wczytuję…” tylko gdy jej jeszcze nie ma
        value = ForkProgrammeCache.entriesOrNull(uiState.channels, uiState.guideVersion, key, ForgeProgrammeView.sort, ForgeProgrammeView.phrase)
        value = ForkProgrammeCache.entries(uiState.channels, uiState.guideVersion, key, ForgeProgrammeView.sort, ForgeProgrammeView.phrase)
    }
    val chosenRow = remember { FocusRequester() }
    val firstTile = remember { FocusRequester() }
    // Opening: the focus goes to the chosen category in the tree.
    LaunchedEffect(Unit) { repeat(10) { kotlinx.coroutines.delay(60); if (runCatching { chosenRow.requestFocus() }.getOrDefault(false)) return@LaunchedEffect } }
    val close = {
        ForgeProgrammeView.close()
        ForgeGuideFocus.lastTool?.let { runCatching { it.requestFocus() } }
    }
    var searchOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) } // makieta 7.7b H11
    val sortPill = remember { FocusRequester() }
    var sortWasOpen by remember { mutableStateOf(false) }
    var typing by remember { mutableStateOf(false) } // makieta 7.7 F29
    var typedOnce by remember { mutableStateOf(false) }
    val searchPill = remember { FocusRequester() }
    // v7.6 (06): ▲ from the header goes back to the navigation bar (the tree and the tiles keep it tucked away)
    val focusBar = com.nuvio.tv.skin.kicia.nav.LocalForgeBarFocus.current
    Column(
        modifier
            .background(ForgeColors.Bg)
            .onFocusChanged { ForgeProgrammeView.focused = it.hasFocus }
            .onPreviewKeyEvent { e ->
                val k = e.nativeKeyEvent
                if (k.keyCode != android.view.KeyEvent.KEYCODE_BACK && k.keyCode != android.view.KeyEvent.KEYCODE_ESCAPE) return@onPreviewKeyEvent false
                // test 15.11 (JMGO): Back while typing the phrase ends the typing only, it used to close the whole view
                if (k.action == android.view.KeyEvent.ACTION_UP) { if (typing) typing = false else close() }
                true
            }
            .focusProperties { ForgeGuideFocus.lastTool?.let { up = it } }
            .focusGroup(),
    ) {
        // v7.6 (owner 13.11): header — Wróć, the category with its count, Szukaj, and the switch Archiwum / Wkrótce
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp)
                .onPreviewKeyEvent { e ->
                    val k = e.nativeKeyEvent
                    if (k.keyCode != android.view.KeyEvent.KEYCODE_DPAD_UP || focusBar == null) return@onPreviewKeyEvent false
                    if (k.action == android.view.KeyEvent.ACTION_DOWN) focusBar.invoke()
                    true
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            com.nuvio.tv.skin.kicia.components.ForgeIconButton(
                icon = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć", size = 36.dp, onOk = { close() },
            )
            Spacer(Modifier.width(12.dp))
            // makieta 7.7b H12: the title without a count; H14: Szukaj first, then Sortuj, both at the right edge
            Text(if (key == FORGE_PROG_FAVOURITES) "Ulubione" else ForgeEpgCategories.nameOf(key), style = ForgeText.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            // makieta 7.7 F29: „Szukaj” opens Android's keyboard; the phrase then stands in the same place as a field
            if (typing) ForgeProgrammePhraseField(onDone = { typing = false })
            else com.nuvio.tv.skin.kicia.components.ForgePillSelect(
                text = ForgeProgrammeView.phrase.ifBlank { "Szukaj" }, selected = ForgeProgrammeView.phrase.isNotBlank(), bare = false,
                icon = androidx.compose.material.icons.Icons.Filled.Search, focusRequester = searchPill,
                onOk = { typing = true },
            )
            Spacer(Modifier.width(8.dp))
            // makieta 7.7b H11: one small button „Sortuj: X”; OK = a Popover with the five sorts (✓ at the chosen one)
            com.nuvio.tv.skin.kicia.components.ForgePillSelect(
                text = "Sortuj: " + ForgeProgrammeView.sort.label, selected = false, bare = false, focusRequester = sortPill,
                icon = androidx.compose.material.icons.Icons.Filled.FilterList, onOk = { sortOpen = true },
            )
        }
        // test 15.11 (JMGO): after the Popover the focus comes back to „Sortuj” (it fell to the guide's „Szukaj programu”)
        // test 15.11 (JMGO): the pill exists only once the field is gone — the focus is asked for after that frame
        LaunchedEffect(typing) { if (typing) typedOnce = true else if (typedOnce) { kotlinx.coroutines.delay(60); runCatching { searchPill.requestFocus() } } }
        LaunchedEffect(sortOpen) { if (sortOpen) sortWasOpen = true else if (sortWasOpen) { kotlinx.coroutines.delay(50); runCatching { sortPill.requestFocus() } } }
        if (sortOpen) com.nuvio.tv.skin.kicia.components.ForgePopover(onDismiss = { sortOpen = false }, x = 680.dp, y = 56.dp) {
            ForgeProgrammeSort.entries.forEach { option ->
                com.nuvio.tv.skin.kicia.components.ForgeSheetRow(
                    text = option.label, detail = if (option == ForgeProgrammeView.sort) "✓" else null,
                    onOk = { ForgeProgrammeView.sort = option; sortOpen = false },
                )
            }
        }
        if (searchOpen) ForgeTvSearch(channels = uiState.channels, onPlay = onPlay, onClose = { searchOpen = false }, host = "programy")
        ForgeTvSearchReturn("programy") { searchOpen = true } // forge fix 14.11 (JMGO A3): back from the details or the player = the results again
        Row(Modifier.weight(1f).fillMaxWidth()) {
        // Left: the tree (Wiersz 36, 240 wide); the chosen branch shows its leaves under it.
        val openBranch = key.removePrefix(ForgeEpgCategories.PROG_PREFIX).substringBefore('/').takeIf { it != "*" }
        val favShows by com.nuvio.tv.fork.tv.TvStore.favoriteShows.collectAsState()
        val treeRows = remember(tree, openBranch) {
            buildList {
                add(Triple(ForgeEpgCategories.PROG_ALL, "Wszystkie programy", 0))
                add(Triple(FORGE_PROG_FAVOURITES, "Ulubione", 0)) // makieta 7.7 F27: second in the tree
                tree.forEach { n ->
                    add(Triple(n.key, n.name, 0))
                    if (n.key.removePrefix(ForgeEpgCategories.PROG_PREFIX) == openBranch) n.children.forEach { add(Triple(it.key, it.name, 1)) }
                }
            }
        }
        LazyColumn(
            Modifier.width(240.dp).fillMaxHeight().focusProperties { right = firstTile; up = searchPill }.focusGroup(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(treeRows, key = { it.first }) { (k, name, depth) ->
                val count = if (k == FORGE_PROG_FAVOURITES) favShows.size else if (k == ForgeEpgCategories.PROG_ALL) tree.sumOf { it.count } else (tree.firstOrNull { it.key == k }?.count ?: tree.flatMap { it.children }.firstOrNull { it.key == k }?.count ?: 0)
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
            val list = entries?.shown
            when {
                list == null -> Text("Wczytuję programy…", style = ForgeText.body.copy(color = ForgeColors.Text3))
                list.isEmpty() -> Text(when {
                    key == FORGE_PROG_FAVOURITES -> "Nie masz jeszcze ulubionych programów. Dodaj program przyciskiem + na jego stronie." // makieta 7.7 F27
                    ForgeProgrammeView.phrase.isNotBlank() -> "Nic nie znaleziono dla «" + ForgeProgrammeView.phrase.trim() + "» w tej kategorii." // makieta 7.7 F29
                    ForgeProgrammeView.upcoming -> "Brak zapowiedzi w tej kategorii."
                    else -> "Brak programów w tej kategorii."
                }, style = ForgeText.body.copy(color = ForgeColors.Text3))
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    modifier = Modifier.fillMaxSize().focusProperties { left = chosenRow; up = searchPill }.focusGroup(),
                    contentPadding = PaddingValues(top = 8.dp, end = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(list, key = { it.programme.title.trim().lowercase() }) { e ->
                        ProgrammeTile(e, if (e === list.first()) firstTile else null, onFocus = { ForgeProgrammeView.focusedTile = e.channel to e.programme }, onDetails = { ForgeGuideDetail.request = e.channel to e.programme }) {
                            val now = LiveTvClock.nowEpochMs()
                            val p = e.programme
                            when {
                                p.startEpochMs <= now && p.stopEpochMs > now -> onPlay(e.channel, null)
                                p.stopEpochMs <= now -> onPlay(e.channel, p)
                                else -> {
                                    com.nuvio.tv.fork.tv.Reminders.toggle(context.applicationContext, com.nuvio.tv.fork.tv.Reminder(e.channel.streamUrl, p.startEpochMs, p.title, e.channel.name))
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
}

/** Karta/Odcinek 160 x 114 with the programme's picture (else the channel's logo), badge, title and "channel · when" under it. */
@Composable
internal fun ProgrammeTile(e: ProgrammeEntry, focusRequester: FocusRequester?, onFocus: () -> Unit, onDetails: () -> Unit, onOk: () -> Unit) {
    val now = LiveTvClock.nowEpochMs()
    val p = e.programme
    val live = p.startEpochMs <= now && p.stopEpochMs > now
    val past = p.stopEpochMs <= now
    Column {
      Box { // makieta 7.7 F28 / 7.7b H13: a recurring programme = a stack of covers, visible but quiet
        if (e.airings > 1) listOf(Triple(8.dp, 16.dp, .5f), Triple(4.dp, 8.dp, .3f)).forEach { (up, inset, dim) ->
            val shape = androidx.compose.foundation.shape.RoundedCornerShape(com.nuvio.tv.skin.kicia.tokens.ForgeDims.RadiusCard)
            Box(
                Modifier.align(Alignment.TopCenter).offset(y = -up).width(160.dp - inset * 2).height(114.dp)
                    .forgeCardShadow(shape)
                    .clip(shape).background(ForgeColors.CardBase)
                    .forgeEdge(shape),
            ) {
                p.image?.takeIf { it.isNotBlank() }?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
            }
        }
        ForgeWideCard(focusRequester = focusRequester, onFocusChange = { if (it) onFocus() }, onOk = onOk) { _ ->
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
                if (e.airings > 1) ForgeBadge("${e.airings} odc.", ForgeBadgeKind.OnImage, Modifier.align(Alignment.TopEnd).padding(8.dp)) // makieta 7.7 F28 + H18
            }
        }
      }
        Spacer(Modifier.height(4.dp))
        // v7.6 (owner 13.11): the text under the picture is its own focus target: OK = "Info i odcinki" (the newest airing under the focus); the picture OK = play
        com.nuvio.tv.skin.kicia.components.ForgeFocusBox(radius = 8.dp, scaleOnFocus = false, onFocusChange = { if (it) onFocus() }, onOk = onDetails, modifier = Modifier.width(160.dp)) { _ ->
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(p.title, style = ForgeText.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // JMGO uwaga 3: the channel on its own line, then when (and how many airings): nothing cut mid-word
                Text(e.channel.name, style = ForgeText.caption.copy(color = ForgeColors.Text3), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    forgeWhen(p.startEpochMs, now), // makieta 7.7 F28: no „· N emisji” (the stack and „N odc.” say it)
                    style = ForgeText.caption.copy(color = ForgeColors.Text3), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
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


/** Makieta 7.7 F29: the phrase field of the programme view, with Android's own keyboard (shown at once). */
@Composable
private fun ForgeProgrammePhraseField(onDone: () -> Unit) {
    val focus = remember { FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() }; keyboard?.show() }
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(com.nuvio.tv.skin.kicia.tokens.ForgeDims.RadiusButton)
    androidx.compose.foundation.text.BasicTextField(
        value = ForgeProgrammeView.phrase,
        onValueChange = { ForgeProgrammeView.phrase = it },
        singleLine = true,
        textStyle = ForgeText.label.copy(color = ForgeColors.OnFocus),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { keyboard?.hide(); onDone() }, onDone = { keyboard?.hide(); onDone() }),
        modifier = Modifier.width(240.dp).height(36.dp).clip(shape).background(ForgeColors.Focus).padding(horizontal = 16.dp, vertical = 8.dp)
            .focusRequester(focus)
            .onPreviewKeyEvent { e ->
                val k = e.nativeKeyEvent
                if (k.keyCode == android.view.KeyEvent.KEYCODE_BACK && k.action == android.view.KeyEvent.ACTION_UP) { keyboard?.hide(); onDone(); true } else false
            },
    )
}
