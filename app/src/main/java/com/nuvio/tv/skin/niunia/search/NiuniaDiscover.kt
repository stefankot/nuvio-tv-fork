package com.nuvio.tv.skin.niunia.search

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.skin.niunia.components.NiuniaBlockKind
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaButtonKind
import com.nuvio.tv.skin.niunia.components.NiuniaContentBlock
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.domain.model.DiscoverLocation
import com.nuvio.tv.ui.screens.search.SearchEvent
import com.nuvio.tv.ui.screens.search.SearchViewModel

private const val TAG = "ForkNiunia"

/**
 * Z-14 (makieta Discover, trasa `discover`): chipy Filmy / Seriale, Katalog (dodatku), Gatunek; siatka plakatów po 6,
 * doczytywanie 10 przed końcem; OK = strona tytułu, długie OK = okno akcji Niuni. Dane i filtry: [SearchViewModel] bazy.
 * Odkrywanie wyłączone w ustawieniach Nuvio → stan pusty z wyjaśnieniem.
 */
@Composable
internal fun NiuniaDiscover(viewModel: SearchViewModel, onNavigateToDetail: (String, String, String) -> Unit) {
    val ui by viewModel.uiState.collectAsState()
    val posterOptions = com.nuvio.tv.skin.niunia.components.rememberNiuniaPosterOptions { id, t, url -> onNavigateToDetail(id, t, url) }
    val firstChip = remember { FocusRequester() }
    // UX JMGO 24.11 (F5, pętla 0 ścieżka 49): Wstecz ze strony tytułu wraca na ten sam kafelek z tym samym przewinięciem
    // (wcześniej: fokus na pierwszym chipie „Seriale”, siatka od góry). Pamięć po kluczu: NiuniaFocusMemory.
    val restore = com.nuvio.tv.skin.niunia.components.rememberNiuniaFocusRestore("odkrywaj") {
        listOf("d:" to ui.discoverResults.mapIndexed { i, it -> "d:${i}_${it.id}" })
    }
    val (gridI, gridO) = restore.outerStart()
    val grid = rememberLazyGridState(gridI, gridO)
    LaunchedEffect(ui.discoverLocation) { if (ui.discoverLocation != DiscoverLocation.OFF) viewModel.ensureDiscoverLoaded() }
    LaunchedEffect(Unit) {
        if (restore.active) return@LaunchedEffect // powrót ze strony tytułu: fokus oddaje rememberNiuniaFocusRestore
        kotlinx.coroutines.delay(100)
        try { firstChip.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odkrywaj: fokus startowy: ${e.javaClass.simpleName}: ${e.message}") }
    }
    val catalogs = ui.discoverCatalogs.filter { it.type == ui.selectedDiscoverType }
    val catalog = catalogs.firstOrNull { it.key == ui.selectedDiscoverCatalogKey } ?: catalogs.firstOrNull()
    LaunchedEffect(ui.selectedDiscoverType, catalog?.key, ui.selectedDiscoverGenre, ui.discoverResults.size) {
        Log.i(TAG, "odkrywaj: ${ui.selectedDiscoverType}, katalog=${catalog?.catalogName}, gatunek=${ui.selectedDiscoverGenre ?: "wszystkie"}, wyniki=${ui.discoverResults.size}")
    }
    LaunchedEffect(grid, ui.discoverResults.size) {
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (ui.discoverResults.isNotEmpty() && last >= ui.discoverResults.size - 10 && ui.discoverHasMore && !ui.discoverLoadingMore && !ui.discoverLoading) {
                Log.i(TAG, "odkrywaj: doczytuję (${ui.discoverResults.size})")
                viewModel.onEvent(SearchEvent.LoadNextDiscoverResults)
            }
        }
    }
    Box(Modifier.fillMaxSize().background(NiuniaColor.Bg)) {
        com.nuvio.tv.skin.niunia.components.NiuniaClockCorner() // Z-15
        Column(Modifier.fillMaxSize().padding(top = NiuniaSpacing.Space40), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
            NiuniaText(stringResource(R.string.niunia_tv2_discover_title), Modifier.padding(start = NiuniaDims.Gutter), variant = NiuniaTextVariant.TitleScreen, lines = 1)
            if (ui.discoverLocation == DiscoverLocation.OFF) {
                NiuniaContentBlock(NiuniaBlockKind.Empty, Modifier.padding(start = NiuniaDims.Gutter)) {
                    NiuniaText(stringResource(R.string.discover_disabled_subtitle), variant = NiuniaTextVariant.Body, tone = NiuniaTone.Secondary, lines = 3)
                }
                return@Column
            }
            LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8)) {
                items(listOf("movie", "series")) { t ->
                    NiuniaButton(onClick = { Log.i(TAG, "odkrywaj: typ → $t"); viewModel.onEvent(SearchEvent.SelectDiscoverType(t)) },
                        label = stringResource(if (t == "movie") R.string.niunia_ke_lib_movies else R.string.niunia_ke_lib_series), kind = NiuniaButtonKind.Chip,
                        selected = ui.selectedDiscoverType == t, focusRequester = if (t == ui.selectedDiscoverType) firstChip else null)
                }
                items(catalogs, key = { it.key }) { c ->
                    NiuniaButton(onClick = { Log.i(TAG, "odkrywaj: katalog → ${c.catalogName}"); viewModel.onEvent(SearchEvent.SelectDiscoverCatalog(c.key)) },
                        label = c.catalogName + if (catalogs.count { it.catalogName == c.catalogName } > 1) " · ${c.addonName}" else "", kind = NiuniaButtonKind.Chip, selected = c.key == catalog?.key)
                }
            }
            val genres = catalog?.genres.orEmpty()
            if (genres.isNotEmpty()) LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8)) {
                items(listOf<String?>(null) + genres) { g ->
                    NiuniaButton(onClick = { Log.i(TAG, "odkrywaj: gatunek → ${g ?: "wszystkie"}"); viewModel.onEvent(SearchEvent.SelectDiscoverGenre(g)) },
                        label = g?.let { com.nuvio.tv.skin.niunia.components.NiuniaGenres.pl(it) ?: it } ?: stringResource(R.string.niunia_tv2_discover_all_genres),
                        kind = NiuniaButtonKind.Chip, selected = ui.selectedDiscoverGenre == g)
                }
            }
            when {
                ui.discoverResults.isEmpty() && ui.discoverLoading -> NiuniaText(stringResource(R.string.niunia_tv2_cat_loading), Modifier.padding(start = NiuniaDims.Gutter), variant = NiuniaTextVariant.Body, tone = NiuniaTone.Secondary)
                ui.discoverResults.isEmpty() -> NiuniaContentBlock(NiuniaBlockKind.Empty, Modifier.padding(start = NiuniaDims.Gutter)) {
                    NiuniaText(stringResource(R.string.niunia_tv2_cat_empty), variant = NiuniaTextVariant.Body, tone = NiuniaTone.Secondary, lines = 2)
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(6), state = grid, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter, top = 8.dp, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap), verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    itemsIndexed(ui.discoverResults, key = { i, it -> "${i}_${it.id}" }) { idx, item ->
                        val k = "d:${idx}_${item.id}"
                        val (art, fallback) = remember(item) { com.nuvio.tv.skin.niunia.components.niuniaPortrait(item, "") }
                        val url = catalog?.addonBaseUrl ?: ""
                        NiuniaPoster(
                            art = art, fallback = fallback, title = item.name, logo = item.logo, meta = com.nuvio.tv.skin.niunia.NiuniaLabels.meta(item), label = null, baked = art != null,
                            onOk = { Log.i(TAG, "odkrywaj: OK → ${item.id}"); onNavigateToDetail(item.id, item.rawType, url) },
                            onLongOk = { posterOptions(item, url) },
                            modifier = restore.modifier(k),
                            onFocus = { if (it) restore.save(k, idx, grid.firstVisibleItemIndex, grid.firstVisibleItemScrollOffset) },
                        )
                    }
                }
            }
        }
    }
}
