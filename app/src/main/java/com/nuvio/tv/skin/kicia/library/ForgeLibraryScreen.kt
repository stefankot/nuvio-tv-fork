package com.nuvio.tv.skin.kicia.library

import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import com.nuvio.tv.data.local.WatchedItemsPreferences
import com.nuvio.tv.domain.model.LibraryEntry
import com.nuvio.tv.domain.model.WatchedItem
import com.nuvio.tv.skin.kicia.components.ForgeButton
import com.nuvio.tv.skin.kicia.components.ForgePillSelect
import com.nuvio.tv.skin.kicia.components.ForgePosterCard
import com.nuvio.tv.skin.kicia.components.ForgeSheetRow
import com.nuvio.tv.skin.kicia.components.ForgeSideSheet
import com.nuvio.tv.skin.kicia.components.ForgeWideCard
import com.nuvio.tv.skin.kicia.nav.ForgeNavRequests
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.screens.library.LibraryViewModel
import com.nuvio.tv.ui.screens.library.LibraryWatchedFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Sub-tabs of the mockup's Biblioteka (S05). */
enum class ForgeLibraryTab(val label: String) { All("Wszystko"), Movies("Filmy"), Series("Seriale"), History("Historia") }

/** One shelf of the library: a title and its entries (each title is in exactly one shelf). */
data class ForgeLibraryShelf(val title: String, val entries: List<LibraryEntry>)

/**
 * The library split into shelves without repeats (mockup S05): titles still to watch first, watched ones after.
 * TODO(scalanie): "Już dostępne" / "Nowe odcinki" need release data the library does not carry.
 */
fun forgeLibraryShelves(
    entries: List<LibraryEntry>,
    tab: ForgeLibraryTab,
    watchedMovieIds: Set<String>,
    watchedSeriesIds: Set<String>,
): List<ForgeLibraryShelf> {
    val shown = entries.filter {
        when (tab) {
            ForgeLibraryTab.Movies -> it.type == "movie"
            ForgeLibraryTab.Series -> it.type != "movie"
            else -> true
        }
    }.distinctBy { it.type + ":" + it.id }
    fun watched(e: LibraryEntry) = if (e.type == "movie") e.id in watchedMovieIds else e.id in watchedSeriesIds
    val (done, waiting) = shown.partition(::watched)
    return listOfNotNull(
        waiting.takeIf { it.isNotEmpty() }?.let { ForgeLibraryShelf("Nieobejrzane", it) },
        done.takeIf { it.isNotEmpty() }?.let { ForgeLibraryShelf("Obejrzane", it) },
    )
}

/** Count per sub-tab, as the pills show it. */
fun forgeLibraryCount(entries: List<LibraryEntry>, tab: ForgeLibraryTab): Int = when (tab) {
    ForgeLibraryTab.All -> entries.distinctBy { it.type + ":" + it.id }.size
    ForgeLibraryTab.Movies -> entries.filter { it.type == "movie" }.distinctBy { it.id }.size
    ForgeLibraryTab.Series -> entries.filter { it.type != "movie" }.distinctBy { it.id }.size
    ForgeLibraryTab.History -> 0
}

/** History (watched marks with their time), newest first, one card per title. */
@HiltViewModel
class ForgeHistoryViewModel @Inject constructor(private val watched: WatchedItemsPreferences) : ViewModel() {
    /** v7.4 (11): Popover „Usuń z historii”. */
    fun remove(item: WatchedItem) { viewModelScope.launch { runCatching { watched.unmarkAsWatched(item.contentId) } } }
    val history: StateFlow<List<WatchedItem>> = watched.allItems
        .map { items -> items.sortedByDescending { it.watchedAt }.distinctBy { it.contentId }.take(60) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** "dziś 20:14", "wczoraj 21:03", "3 lis 19:40". */
fun forgeWatchedLabel(atMs: Long, nowMs: Long = System.currentTimeMillis(), zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String {
    val at = java.time.Instant.ofEpochMilli(atMs).atZone(zone)
    val today = java.time.Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
    val time = "%02d:%02d".format(at.hour, at.minute)
    return when (at.toLocalDate()) {
        today -> "dziś $time"
        today.minusDays(1) -> "wczoraj $time"
        // Within the week: the day's name, as the mockup writes it ("pt. 21:10").
        in today.minusDays(6)..today.minusDays(2) -> listOf("pon.", "wt.", "śr.", "czw.", "pt.", "sob.", "niedz.")[at.dayOfWeek.value - 1] + " $time"
        else -> "${at.dayOfMonth} ${PolishMonths[at.monthValue - 1]} $time"
    }
}

private val PolishMonths = listOf("sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru")

/**
 * S05 Biblioteka (mockup): sub-tab pills with counts and "Sortuj i filtruj" at y 84, the focused title large
 * above the shelves (posters carry no text), shelves without repeats, Historia as a grid of wide cards, and an
 * empty state with one action. Data: Nuvio's own [LibraryViewModel] (entries, sorting, watched sets).
 */
@Composable
fun ForgeLibraryScreen(
    onNavigateToDetail: (String, String, String?) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    historyViewModel: ForgeHistoryViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    metaViewModel: com.nuvio.tv.skin.kicia.home.ForgeHeroMetaViewModel = hiltViewModel(),
) {
    com.nuvio.tv.skin.kicia.nav.ForgeTabBackToBar() // forge hook v7.4 (ZACHOWANIE): Wstecz z treści zakładki → pasek; własne handlery ekranu, złożone później, wygrywają
    val state by viewModel.uiState.collectAsState()
    val history by historyViewModel.history.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(ForgeLibraryTab.All) }
    var focused by remember { mutableStateOf<LibraryEntry?>(null) }
    var sortOpen by remember { mutableStateOf(false) }
    val firstTab = remember { FocusRequester() }

    val entries = state.visibleItems.ifEmpty { state.allItems }
    val shelves = remember(entries, tab, state.watchedMovieIds, state.watchedSeriesIds) {
        forgeLibraryShelves(entries, tab, state.watchedMovieIds, state.watchedSeriesIds)
    }
    // Owner 10.11: nothing is selected when the screen first appears — no first film on the hero until a card has the focus.
    // v7.4 (10): the title block shows the focused item, at first the first card of the first shelf (render of the mockup); flat background, no picture
    val shownTitle = focused ?: shelves.firstOrNull()?.entries?.firstOrNull()
    // Back from a title: the focus returns to its card (it used to land on the first sub-tab).
    var lastCardKey by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    val lastCard = remember { FocusRequester() }
    val firstCard = remember { FocusRequester() } // v7.4 (10): ▼ z zakładek: karta zapamiętana, przy pierwszym wejściu pierwsza
    // v7.4 (10): long OK on a card = Popover (Odtwórz, Usuń z biblioteki, Oznacz jako obejrzane), 280 wide, under the card up to y 336
    var cardMenu by remember { mutableStateOf<Pair<com.nuvio.tv.domain.model.LibraryEntry, androidx.compose.ui.geometry.Rect?>?>(null) }
    val cardBounds = remember { HashMap<String, androidx.compose.ui.geometry.Rect>() }
    LaunchedEffect(Unit) {
        // Only back from a title the focus returns to its card; a first visit takes no focus (the bar keeps it).
        if (lastCardKey != null) {
            var ok = false
            repeat(6) { if (!ok) { ok = runCatching { lastCard.requestFocus() }.isSuccess; if (!ok) kotlinx.coroutines.delay(80) } }
        }
    }

    Box(modifier.fillMaxSize().background(ForgeColors.Bg)) {
        // Backdrop of the focused title, faint, under a scrim (mockup: the page takes the title's colour).
        if (false && tab != ForgeLibraryTab.History) { // v7.4: flat bg, no title picture
            Crossfade(targetState = shownTitle?.background, label = "forgeLibraryBackdrop") { url ->
                if (!url.isNullOrBlank()) {
                    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Box(Modifier.fillMaxSize().background(ForgeColors.HeroScrimHorizontal))
            Box(Modifier.fillMaxSize().background(ForgeColors.HeroScrimVertical))
            // Owner 9.11: bright artwork (YouTube thumbnails) swallowed the title and pills; extra darkening behind the top band.
            Box(Modifier.fillMaxWidth().height(300.dp).background(androidx.compose.ui.graphics.Brush.verticalGradient(0f to androidx.compose.ui.graphics.Color(0xCC000000), 1f to androidx.compose.ui.graphics.Color.Transparent)))
        }

        Column(Modifier.fillMaxSize().padding(top = 84.dp)) {
            // Sub-tabs + Sortuj i filtruj (y 84).
            // Entering the row (↓ from the bar, ↑ from the grid) lands on the open sub-tab: focus selects a tab,
            // so landing on the pill under the cursor used to switch the Library (e.g. to Historia).
            val tabFocus = remember(firstTab) { List(ForgeLibraryTab.entries.size) { if (it == 0) firstTab else androidx.compose.ui.focus.FocusRequester() } }
            Row(
                Modifier.fillMaxWidth()
                    .focusProperties {
                        @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                        enter = { tabFocus[ForgeLibraryTab.entries.indexOf(tab).coerceAtLeast(0)] }
                    }
                    .focusGroup()
                    .padding(horizontal = ForgeDims.ContentStart),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Owner 9.11: Historia has its own screen (avatar menu), no longer a sub-tab.
                ForgeLibraryTab.entries.filter { it != ForgeLibraryTab.History }.forEachIndexed { i, t ->
                    val count = forgeLibraryCount(entries, t)
                    ForgePillSelect(
                        text = if (t == ForgeLibraryTab.History) t.label else "${t.label} $count", // v7.4 (10): jeden napis „Wszystko 12” jak w makiecie
                        selected = t == tab,
                        bare = true, // v7.4: Pigułka wyboru bez tła w spoczynku
                        modifier = Modifier.testTag("pigulka.$i").focusProperties { down = if (lastCardKey != null) lastCard else firstCard }, // v7.4 (10)
                        focusRequester = tabFocus[i],
                        onFocusChange = { if (it) { tab = t; focused = null } },
                        onOk = { tab = t },
                    )
                }
                Spacer(Modifier.weight(1f))
                com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Sortuj i filtruj", small = true, modifier = Modifier.testTag("przycisk.0"), onOk = { sortOpen = true }) // v7.4: Przycisk mały 12%
            }

            when {
                state.isLoading && entries.isEmpty() -> Text("Wczytuję…", style = ForgeText.body, modifier = Modifier.padding(start = ForgeDims.ContentStart, top = 40.dp))
                tab == ForgeLibraryTab.History -> HistoryGrid(history, metaViewModel, onOpen = { item -> onNavigateToDetail(item.contentId, item.contentType, null) })
                shelves.isEmpty() -> Column(Modifier.padding(start = ForgeDims.ContentStart, top = 60.dp)) {
                    Text("Twoja Biblioteka jest pusta", style = ForgeText.title)
                    Spacer(Modifier.height(8.dp))
                    Text("Przytrzymaj OK na tytule, aby dodać go do Mojej listy.", style = ForgeText.body)
                    Spacer(Modifier.height(24.dp))
                    ForgeButton(text = "Przeglądaj Home", primary = true, onOk = { ForgeNavRequests.open(Screen.Home.route) })
                }
                else -> {
                    // Focused title (title 32/38 + meta), y 136.
                    Column(Modifier.padding(start = ForgeDims.ContentStart, top = 16.dp).height(64.dp)) { // v7.4: title y 136, shelves from y 200
                        Text(shownTitle?.name.orEmpty(), style = ForgeText.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(end = ForgeDims.ContentEnd).fillMaxWidth().height(38.dp).testTag("glowka-tytul.0")) // v7.4 (10): 32/38
                        Text(shownTitle?.let(::forgeLibraryMeta).orEmpty(), style = ForgeText.label5, maxLines = 1, modifier = Modifier.padding(end = ForgeDims.ContentEnd).fillMaxWidth().testTag("glowka-meta.0")) // v7.4: meta 14, 70%, y 174 (tuż pod tytułem 38)
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 60.dp),
                        verticalArrangement = Arrangement.spacedBy(ForgeDims.ShelfGap - 10.dp), // v7.4 (10): 10 dp dolnego marginesu rzędu kart (miejsce na fokus) wlicza się w 40
                    ) {
                        val cardOffsets = shelves.runningFold(0) { acc, sh -> acc + sh.entries.size } // v7.4 (10): karta.N in document order
                        itemsIndexed(shelves, key = { _, it -> it.title }) { shelfIndex, shelf ->
                            Column(Modifier.testTag("polka.$shelfIndex")) {
                                Text(shelf.title, style = ForgeText.shelfTitle.copy(color = ForgeColors.Text2), modifier = Modifier.padding(start = ForgeDims.ContentStart).testTag("polka-tytul.$shelfIndex"))
                                Spacer(Modifier.height(14.dp)) // v7.4: header 24 above the cards (10 more come from the row's padding)
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = ForgeDims.ContentStart, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(ForgeDims.Gap),
                                ) {
                                    itemsIndexed(shelf.entries, key = { _, it -> it.type + ":" + it.id }) { cardIndex, entry ->
                                        val cardKey = entry.type + ":" + entry.id
                                        ForgePosterCard(
                                            modifier = Modifier.testTag("karta.${cardOffsets[shelfIndex] + cardIndex}") // v7.4 (10)
                                                .onGloballyPositioned { cardBounds[cardKey] = it.boundsInRoot() },
                                            onLongOk = { cardMenu = entry to cardBounds[cardKey] },
                                            focusRequester = if (cardKey == lastCardKey) lastCard else if (shelfIndex == 0 && cardIndex == 0) firstCard else null,
                                            onFocusChange = { if (it) { focused = entry; lastCardKey = cardKey } },
                                            onOk = { onNavigateToDetail(entry.id, entry.type, entry.addonBaseUrl) },
                                        ) {
                                            Text(
                                                entry.name, style = ForgeText.label, maxLines = 3, overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(12.dp),
                                            )
                                            com.nuvio.tv.skin.kicia.components.ForgePosterImage(entry.poster ?: entry.background, entry.rawPosterUrl, entry.name)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        cardMenu?.let { (entry, b) ->
            val density = androidx.compose.ui.platform.LocalDensity.current
            val px = with(density) { (b?.left ?: 0f).toDp() }.coerceAtLeast(ForgeDims.ContentStart)
            val py = (with(density) { (b?.bottom ?: 0f).toDp() } + 8.dp).coerceIn(68.dp, 336.dp)
            com.nuvio.tv.skin.kicia.components.ForgePopover(onDismiss = { cardMenu = null }, x = px, y = py, width = 280.dp) {
                com.nuvio.tv.skin.kicia.components.ForgeSheetRow(text = "Odtwórz") { cardMenu = null; onNavigateToDetail(entry.id, entry.type, entry.addonBaseUrl) }
                com.nuvio.tv.skin.kicia.components.ForgeSheetRow(text = "Usuń z biblioteki") { cardMenu = null; metaViewModel.removeFromLibrary(entry) }
                com.nuvio.tv.skin.kicia.components.ForgeSheetRow(text = "Oznacz jako obejrzane") { cardMenu = null; metaViewModel.markWatched(entry) }
            }
        }
        // Sortuj i filtruj: Nuvio's own sort options and watched filter.
        ForgeSideSheet(visible = sortOpen, onDismiss = { sortOpen = false }, title = "Sortuj i filtruj") {
            state.availableSortOptions.forEach { option ->
                ForgeSheetRow(text = stringResource(option.labelResId), detail = if (option == state.selectedSortOption) "✓" else null) {
                    viewModel.onSelectSortOption(option); sortOpen = false
                }
            }
            LibraryWatchedFilter.entries.forEach { filter ->
                ForgeSheetRow(text = forgeWatchedFilterLabel(filter), detail = if (filter == state.selectedWatchedFilter) "✓" else null) {
                    viewModel.onSelectWatchedFilter(filter); sortOpen = false
                }
            }
        }
    }
}

@Composable
internal fun HistoryGrid(history: List<WatchedItem>, metaViewModel: com.nuvio.tv.skin.kicia.home.ForgeHeroMetaViewModel, onOpen: (WatchedItem) -> Unit, onRemove: ((WatchedItem) -> Unit)? = null) {
    // v7.4 (11): long OK = Popover (Wznów, Usuń z historii) under the card
    var menu by remember { mutableStateOf<Pair<WatchedItem, androidx.compose.ui.geometry.Rect?>?>(null) }
    val bounds = remember { HashMap<String, androidx.compose.ui.geometry.Rect>() }
    val images by metaViewModel.images.collectAsStateWithLifecycle()
    if (history.isEmpty()) {
        Text("Tu pojawią się obejrzane tytuły.", style = ForgeText.body, modifier = Modifier.padding(start = ForgeDims.ContentStart, top = 40.dp))
        return
    }
    val now = remember { System.currentTimeMillis() }
    // v7.4 (11): groups "Dziś" / "W tym tygodniu" / "Wcześniej" with a section header 20/700; cards 160x114, 4 per row (pitch 180), 40 between rows
    val groups = remember(history) {
        val cal = java.util.Calendar.getInstance().apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
        val today = cal.timeInMillis
        val week = today - 6 * 24 * 3_600_000L
        listOf(
            "Dziś" to history.filter { it.watchedAt >= today },
            "W tym tygodniu" to history.filter { it.watchedAt in week until today },
            "Wcześniej" to history.filter { it.watchedAt < week },
        ).filter { it.second.isNotEmpty() }
    }
    val offsets = groups.runningFold(0) { acc, g -> acc + g.second.size } // v7.4 (11): karta.N in document order
    LazyColumn(
        contentPadding = PaddingValues(top = 0.dp, bottom = 60.dp),
    ) {
        groups.forEachIndexed { gi, (label, items) ->
            item(key = "h_$label") {
                Text(label, style = ForgeText.head, modifier = Modifier.padding(start = ForgeDims.ContentStart, top = if (gi == 0) 0.dp else 0.dp, bottom = 16.dp).testTag("naglowek-sekcji.$gi"))
            }
            itemsIndexed(items.chunked(4), key = { _, row -> "r_" + row.first().contentId }) { ri, row ->
                Row(Modifier.padding(horizontal = ForgeDims.ContentStart).padding(bottom = ForgeDims.ShelfGap), horizontalArrangement = Arrangement.spacedBy(ForgeDims.Gap)) {
                    row.forEachIndexed { ci, item ->
                        val n = offsets[gi] + ri * 4 + ci
                        Column {
                            // Mockup b23: a landscape picture of the title (its backdrop, looked up once), the poster as a stand-in.
                            LaunchedEffect(item.contentId) { metaViewModel.request(item.contentType, item.contentId) }
                            ForgeWideCard(
                                modifier = Modifier.testTag("karta.$n").onGloballyPositioned { bounds[item.contentId] = it.boundsInRoot() },
                                onLongOk = { menu = item to bounds[item.contentId] },
                                onOk = { onOpen(item) },
                            ) {
                                val picture = images[item.contentId] ?: item.poster?.takeIf { it.isNotBlank() }
                                if (picture == null) Text(item.title, style = ForgeText.label, maxLines = 2, modifier = Modifier.align(Alignment.Center).padding(12.dp))
                                else AsyncImage(model = picture, contentDescription = item.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                            // v7.4 (11): title block 40 high (2 lines of 14/20) at card + 8, the line 8 below it
                            Text(item.title, style = ForgeText.label, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp).width(ForgeDims.Wide).height(40.dp).testTag("karta-opis-tytul.$n"))
                            Text(forgeWatchedLabel(item.watchedAt, now), style = ForgeText.caption, modifier = Modifier.padding(top = 8.dp).width(ForgeDims.Wide).testTag("karta-opis-linia.$n"))
                        }
                    }
                }
            }
        }
    }
    menu?.let { (item, b) ->
        val density = androidx.compose.ui.platform.LocalDensity.current
        val px = with(density) { (b?.left ?: 0f).toDp() }.coerceAtLeast(ForgeDims.ContentStart)
        val below = with(density) { (b?.bottom ?: 0f).toDp() } + 8.dp
        val py = if (below + 104.dp <= 524.dp) below else with(density) { (b?.top ?: 0f).toDp() } - 112.dp
        com.nuvio.tv.skin.kicia.components.ForgePopover(onDismiss = { menu = null }, x = px, y = py.coerceAtLeast(40.dp)) {
            com.nuvio.tv.skin.kicia.components.ForgeSheetRow(text = "Wznów") { menu = null; onOpen(item) }
            if (onRemove != null) com.nuvio.tv.skin.kicia.components.ForgeSheetRow(text = "Usuń z historii") { menu = null; onRemove(item) }
        }
    }
}

/** "2024 · Sci-Fi, Przygodowy · IMDb 8,5" */
fun forgeLibraryMeta(entry: LibraryEntry): String = listOfNotNull(
    entry.releaseInfo?.takeIf { it.isNotBlank() },
    entry.genres.take(2).joinToString(", ").takeIf { it.isNotBlank() },
    entry.imdbRating?.let { "IMDb " + "%.1f".format(java.util.Locale("pl"), it) },
).joinToString(" · ")

private fun forgeWatchedFilterLabel(filter: LibraryWatchedFilter): String = when (filter.name) {
    "ALL" -> "Wszystkie"
    "WATCHED" -> "Obejrzane"
    "UNWATCHED" -> "Nieobejrzane"
    else -> filter.name.lowercase().replaceFirstChar { it.uppercase() }
}
