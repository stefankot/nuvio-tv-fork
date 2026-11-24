package com.nuvio.tv.skin.niunia.library

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.LibraryEntry
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.skin.niunia.components.NiuniaBlockKind
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaButtonKind
import com.nuvio.tv.skin.niunia.components.NiuniaCollection
import com.nuvio.tv.skin.niunia.components.NiuniaCollectionHeader
import com.nuvio.tv.skin.niunia.components.NiuniaCollectionKind
import com.nuvio.tv.skin.niunia.components.NiuniaContentBlock
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaPanelOverlay
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.components.NiuniaTrailing
import com.nuvio.tv.skin.niunia.components.niuniaTrapArrows
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.ui.screens.library.LibraryTypeTab
import com.nuvio.tv.ui.screens.library.LibraryUiState
import com.nuvio.tv.ui.screens.library.LibraryViewModel
import com.nuvio.tv.ui.screens.library.LibraryWatchedFilter

private const val TAG = "ForkNiunia"
private const val DAY_MS = 86_400_000L

/**
 * Biblioteka z treścią (Z-14, makiety LibraryShelves i LibrarySort; wzór: Nuvio KE 1.2.2):
 * zakładki typu (Wszystko / Filmy / Seriale) + chip „Sortuj i filtruj”, fakty tytułu z fokusem, półki „Ostatnio dodane (N w tym miesiącu)”,
 * „Jeszcze nieobejrzane”, „Obejrzane”. Filtr (gatunek, rok, obejrzane) albo wyłączone półki → siatka plakatów jak dotąd.
 * Panel „Sortuj i filtruj” (po prawej): OK na wierszu przełącza wartość na następną, Wstecz zamyka.
 */
@Composable
internal fun NiuniaLibraryContent(
    state: LibraryUiState,
    vm: LibraryViewModel,
    posterOptions: (MetaPreview, String?) -> Unit,
    onNavigateToDetail: (String, String, String?) -> Unit,
) {
    val ctx = LocalContext.current
    var shelvesOn by rememberSaveable { mutableStateOf(true) }
    var panel by remember { mutableStateOf(false) }
    // Z-14 (LibraryHistory): zakładka „Historia” obok zakładek typu.
    var historyTab by rememberSaveable { mutableStateOf(false) }
    val filtered = state.selectedGenre != null || state.selectedYear != null || state.selectedWatchedFilter != LibraryWatchedFilter.ALL
    val items = state.visibleItems
    androidx.compose.runtime.LaunchedEffect(items.size) { com.nuvio.tv.skin.niunia.search.NiuniaKnownTitles.add(items.map { it.name }) } // Z-19: słownik Szukaj
    val posterPattern = remember(state.customPosterUrlPattern, state.customPosterEnabledScreens) {
        com.nuvio.tv.core.poster.patternForScreen(state.customPosterUrlPattern, com.nuvio.tv.core.poster.CustomPosterScreen.LIBRARY, state.customPosterEnabledScreens)
    }
    fun watched(e: LibraryEntry) = if (e.type == "series") e.id in state.watchedSeriesIds else e.id in state.watchedMovieIds
    val now = remember { System.currentTimeMillis() }
    val monthStart = remember { java.util.Calendar.getInstance().apply { set(java.util.Calendar.DAY_OF_MONTH, 1); set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0) }.timeInMillis }
    val shelfList = remember(items, state.watchedMovieIds, state.watchedSeriesIds) {
        val recent = items.filter { it.listedAt > 0L }.sortedByDescending { it.listedAt }.take(20)
        listOfNotNull(
            recent.takeIf { it.isNotEmpty() }?.let { Triple(ctx.getString(R.string.niunia_ke_shelf_recent), ctx.getString(R.string.niunia_ke_shelf_recent_sub, items.count { e -> e.listedAt >= monthStart }), it) },
            items.filterNot { watched(it) }.takeIf { it.isNotEmpty() }?.let { Triple(ctx.getString(R.string.niunia_ke_shelf_unwatched), "· ${it.size}", it) },
            items.filter { watched(it) }.takeIf { it.isNotEmpty() }?.let { Triple(ctx.getString(R.string.niunia_ke_shelf_watched), "· ${it.size}", it) },
        )
    }
    val shelves = shelvesOn && !filtered && shelfList.isNotEmpty()
    LaunchedEffect(shelves, items.size, shelfList.size) {
        Log.i(TAG, "biblioteka: ${if (shelves) "półki ${shelfList.joinToString { "${it.first}=${it.third.size}" }}" else "siatka ${items.size} (filtr=$filtered, półki=$shelvesOn)"}")
    }
    var focusedEntry by remember { mutableStateOf<LibraryEntry?>(null) }
    // Test JMGO ke2: po zamknięciu panelu fokus wracał na „Wszystko” — wraca na chip „Sortuj i filtruj”.
    val sortChip = remember { FocusRequester() }
    var panelWasOpen by remember { mutableStateOf(false) }
    LaunchedEffect(panel) {
        if (panel) { panelWasOpen = true; return@LaunchedEffect }
        if (!panelWasOpen) return@LaunchedEffect
        kotlinx.coroutines.delay(80)
        try { sortChip.requestFocus(); Log.i(TAG, "biblioteka: panel zamknięty, fokus → Sortuj i filtruj") } catch (e: IllegalStateException) { Log.w(TAG, "biblioteka: fokus po panelu: ${e.javaClass.simpleName}: ${e.message}") }
    }
    val first = remember { FocusRequester() }
    com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(first)

    fun facts(e: LibraryEntry): String {
        val added = e.listedAt.takeIf { it > 0L }?.let { t ->
            val days = ((now - t) / DAY_MS).toInt()
            when {
                days <= 0 -> ctx.getString(R.string.niunia_ke_added_today)
                days == 1 -> ctx.getString(R.string.niunia_ke_added_yesterday)
                days < 30 -> ctx.getString(R.string.niunia_ke_added_days, days)
                else -> ctx.getString(R.string.niunia_ke_added_date, java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale("pl", "PL")).format(java.util.Date(t)))
            }
        }
        return listOfNotNull(
            ctx.getString(if (e.type == "series") R.string.niunia_ke_series else R.string.niunia_ke_movie),
            e.releaseInfo?.takeIf { it.isNotBlank() }, added,
            ctx.getString(if (watched(e)) R.string.niunia_ke_ep_watched else R.string.niunia_ke_ep_unwatched),
        ).joinToString(" · ")
    }

    @Composable
    fun Poster(entry: LibraryEntry, modifier: Modifier) {
        val rating = entry.imdbRating?.takeIf { it > 0f }?.let { "★ %.1f".format(java.util.Locale.US, it) }
        val (art, fallback) = remember(entry, posterPattern) {
            com.nuvio.tv.skin.niunia.components.niuniaPortrait(entry.toMetaPreview().copy(imdbId = entry.imdbId), posterPattern)
        }
        NiuniaPoster(
            art = art, fallback = fallback, title = entry.name, logo = entry.logo,
            meta = listOfNotNull(com.nuvio.tv.skin.niunia.components.NiuniaGenres.pl(entry.genres.firstOrNull()), rating).joinToString(" · ").ifBlank { null },
            label = null, baked = art != null,
            onOk = { Log.i(TAG, "biblioteka: OK → ${entry.type} ${entry.id}"); onNavigateToDetail(entry.id, entry.type, entry.addonBaseUrl) },
            onLongOk = { posterOptions(entry.toMetaPreview().copy(imdbId = entry.imdbId), entry.addonBaseUrl) },
            modifier = modifier,
            onFocus = { if (it) focusedEntry = entry },
        )
    }

    Box(Modifier.fillMaxSize().background(NiuniaColors.Bg)) {
        Column(Modifier.fillMaxSize().padding(top = 76.dp)) {
            // Zakładki typu + „Sortuj i filtruj”; ▲ z tego rzędu = menu (jak dotąd z pierwszego rzędu plakatów).
            Row(
                Modifier.padding(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter).onPreviewKeyEvent { e ->
                    if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionUp) com.nuvio.tv.skin.niunia.nav.NiuniaNavActions.toMenu("biblioteka ▲ z zakładek") else false
                },
                horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12), verticalAlignment = Alignment.CenterVertically,
            ) {
                val tabs = listOf(LibraryTypeTab.All) + state.availableTypeTabs.filter { it.key != LibraryTypeTab.ALL_KEY }
                val selected = state.selectedTypeTab?.key ?: LibraryTypeTab.ALL_KEY
                NiuniaCollection(NiuniaCollectionKind.Tabs) {
                    if (tabs.size > 1) tabs.forEach { t ->
                        NiuniaButton(onClick = { Log.i(TAG, "biblioteka: zakładka ${t.key}"); historyTab = false; vm.onSelectTypeTab(t) },
                            label = niuniaTypeTabLabel(t, stringResource(R.string.niunia_ke_lib_all), stringResource(R.string.niunia_ke_lib_movies), stringResource(R.string.niunia_ke_lib_series)),
                            kind = NiuniaButtonKind.Tab, selected = !historyTab && selected == t.key)
                    }
                    NiuniaButton(onClick = { Log.i(TAG, "biblioteka: zakładka Historia"); historyTab = true }, label = stringResource(R.string.niunia_tv2_hist_tab),
                        kind = NiuniaButtonKind.Tab, selected = historyTab)
                }
                if (!historyTab) NiuniaButton(onClick = { Log.i(TAG, "biblioteka: Sortuj i filtruj → panel"); panel = true }, label = stringResource(R.string.niunia_ke_lib_sort),
                    kind = NiuniaButtonKind.Chip, selected = filtered || !shelvesOn, focusRequester = sortChip)
            }
            if (historyTab) {
                NiuniaLibraryHistory(first, onOpen = { w -> onNavigateToDetail(w.contentId, w.contentType, null) })
                return@Column
            }
            val fe = focusedEntry
            if (fe != null) NiuniaContentBlock(NiuniaBlockKind.Info, Modifier.padding(start = NiuniaDims.Gutter, top = NiuniaSpacing.Space16, end = NiuniaDims.Gutter)) {
                NiuniaText(fe.name, variant = NiuniaTextVariant.TitleSection, lines = 1)
                NiuniaText(facts(fe), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 1)
            }
            if (shelves) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = NiuniaSpacing.Space16, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space24)) {
                    shelfList.forEachIndexed { si, (title, sub, list) ->
                        item(key = "shelf_$si") {
                            Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
                                NiuniaCollectionHeader(title, active = focusedEntry != null && focusedEntry in list, modifier = Modifier.padding(start = NiuniaDims.Gutter).height(22.dp), sub = sub)
                                LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                                    itemsIndexed(list, key = { _, e -> "${si}_${e.type}_${e.id}" }) { i, e ->
                                        Poster(e, if (si == 0 && i == 0) Modifier.focusRequester(first) else Modifier)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(NiuniaDims.PosterW),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter, top = NiuniaSpacing.Space16, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    itemsIndexed(items, key = { _, it -> "${it.type}_${it.id}" }) { index, entry ->
                        Poster(entry, if (index == 0) Modifier.focusRequester(first) else Modifier)
                    }
                }
            }
        }
        if (panel) Box(Modifier.align(Alignment.TopEnd).padding(top = 40.dp)) {
            BackHandler { Log.i(TAG, "biblioteka: panel zamknięty (Wstecz)"); panel = false }
            SortPanel(state, vm, shelvesOn, onShelves = { shelvesOn = it })
        }
    }
}

/** Panel „Sortuj i filtruj” (makieta LibrarySort): Kolejność, Gatunek, Rok, Obejrzane (OK = następna wartość), Półki (przełącznik). */
@Composable
private fun SortPanel(state: LibraryUiState, vm: LibraryViewModel, shelvesOn: Boolean, onShelves: (Boolean) -> Unit) {
    val firstRow = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60)
        try { firstRow.requestFocus(); Log.i(TAG, "biblioteka: panel, fokus → Kolejność") } catch (e: IllegalStateException) { Log.w(TAG, "biblioteka: fokus panelu: ${e.javaClass.simpleName}: ${e.message}") }
    }
    fun <T> next(list: List<T>, cur: T): T = if (list.isEmpty()) cur else list[(list.indexOf(cur) + 1).mod(list.size)]
    NiuniaPanelOverlay(stringResource(R.string.niunia_ke_lib_sort),
        Modifier.niuniaTrapArrows("biblioteka: panel", setOf(FocusDirection.Left, FocusDirection.Right, FocusDirection.Up, FocusDirection.Down)).focusGroup()) {
        val sorts = state.availableSortOptions.ifEmpty { listOf(state.selectedSortOption) }
        NiuniaListRow(stringResource(R.string.niunia_ke_lib_order), value = stringResource(state.selectedSortOption.labelResId), trailing = NiuniaTrailing.Chevron, focusRequester = firstRow,
            onClick = { val n = next(sorts, state.selectedSortOption); Log.i(TAG, "biblioteka: kolejność → ${n.key}"); vm.onSelectSortOption(n) })
        val genres = listOf<String?>(null) + state.availableGenres.map { it.key }
        NiuniaListRow(stringResource(R.string.niunia_ke_lib_genre), value = state.availableGenres.firstOrNull { it.key == state.selectedGenre }?.label ?: stringResource(R.string.niunia_ke_lib_any),
            trailing = NiuniaTrailing.Chevron, onClick = { val n = next(genres, state.selectedGenre); Log.i(TAG, "biblioteka: gatunek → $n"); vm.onSelectGenre(n) })
        val years = listOf<String?>(null) + state.availableYears.map { it.key }
        NiuniaListRow(stringResource(R.string.niunia_ke_lib_year), value = state.availableYears.firstOrNull { it.key == state.selectedYear }?.label ?: stringResource(R.string.niunia_ke_lib_any),
            trailing = NiuniaTrailing.Chevron, onClick = { val n = next(years, state.selectedYear); Log.i(TAG, "biblioteka: rok → $n"); vm.onSelectYear(n) })
        NiuniaListRow(stringResource(R.string.niunia_ke_lib_watched), value = stringResource(state.selectedWatchedFilter.labelResId), trailing = NiuniaTrailing.Chevron,
            onClick = { val n = next(LibraryWatchedFilter.values().toList(), state.selectedWatchedFilter); Log.i(TAG, "biblioteka: obejrzane → ${n.key}"); vm.onSelectWatchedFilter(n) })
        NiuniaListRow(stringResource(R.string.niunia_ke_lib_shelves), supporting = stringResource(R.string.niunia_ke_lib_shelves_sub),
            trailing = if (shelvesOn) NiuniaTrailing.SwitchOn else NiuniaTrailing.SwitchOff,
            onClick = { Log.i(TAG, "biblioteka: półki → ${!shelvesOn}"); onShelves(!shelvesOn) })
    }
}

/** Zakładka typu po polsku (test JMGO ke2: baza dawała „Movie (108)”): „Filmy (108)”, „Seriale (36)”, inne bez zmian. */
internal fun niuniaTypeTabLabel(t: LibraryTypeTab, all: String, movies: String, series: String): String {
    if (t.key == LibraryTypeTab.ALL_KEY) return all
    val count = Regex("""\((\d+)\)""").find(t.label)?.groupValues?.get(1)
    val name = when (t.key.lowercase()) { "movie", "movies", "film" -> movies; "series", "tv", "show" -> series; else -> t.label.substringBefore(" (").ifBlank { t.label } }
    return if (count != null) "$name ($count)" else name
}
