package com.nuvio.tv.skin.niunia.home

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import com.nuvio.tv.domain.model.legacyKey
import com.nuvio.tv.skin.niunia.components.NiuniaBlockKind
import com.nuvio.tv.skin.niunia.components.NiuniaContentBlock
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.ui.screens.home.HomeEvent
import com.nuvio.tv.ui.screens.home.HomeViewModel

private const val TAG = "ForkNiunia"

/**
 * Z-14 (makieta CatalogAll, trasa `catalog_see_all`): cały katalog rzędu Głównej — nagłówek „nazwa · dodatek · liczba”,
 * siatka plakatów po 6, doczytywanie 10 pozycji przed końcem, OK = strona tytułu, długie OK = okno akcji Niuni.
 * Pusty katalog: stan pusty. Wyniki Szukaj i katalogi serwera zostają w ekranie bazy (wpięcie zwraca false).
 */
@Composable
internal fun NiuniaCatalogAll(
    catalogId: String,
    addonId: String,
    type: String,
    viewModel: HomeViewModel,
    onNavigateToDetail: (String, String, String) -> Unit,
    onBack: () -> Unit,
) {
    val rows by viewModel.fullCatalogRows.collectAsState()
    val key = com.nuvio.tv.domain.model.catalogRowLegacyKey(addonId, type, catalogId)
    val row = rows.find { it.legacyKey() == key }
    val posterOptions = com.nuvio.tv.skin.niunia.components.rememberNiuniaPosterOptions { id, t, url -> onNavigateToDetail(id, t, url) }
    val first = remember { FocusRequester() }
    val grid = rememberLazyGridState()
    BackHandler { Log.i(TAG, "katalog: Wstecz"); onBack() }
    LaunchedEffect(key, row != null) {
        if (row == null) { Log.i(TAG, "katalog: $key — wczytuję"); viewModel.requestLazyCatalogLoad(key) }
        else Log.i(TAG, "katalog: ${row.catalogName} (${row.addonName}) ${row.items.size} pozycji")
    }
    LaunchedEffect(row?.items?.isNotEmpty()) {
        if (row?.items?.isNotEmpty() != true) return@LaunchedEffect
        kotlinx.coroutines.delay(80)
        try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "katalog: fokus startowy: ${e.javaClass.simpleName}: ${e.message}") }
    }
    LaunchedEffect(grid, row?.items?.size) {
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            val r = row ?: return@collect
            if (r.items.isNotEmpty() && last >= r.items.size - 10 && r.hasMore && !r.isLoading) {
                Log.i(TAG, "katalog: doczytuję (${r.items.size})")
                viewModel.onEvent(HomeEvent.OnLoadMoreCatalog(r.catalogId, r.addonId, r.apiType))
            }
        }
    }
    Box(Modifier.fillMaxSize().background(NiuniaColor.Bg)) {
        com.nuvio.tv.skin.niunia.components.NiuniaClockCorner() // Z-15
        Column(Modifier.fillMaxSize().padding(top = NiuniaSpacing.Space40), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space16)) {
            NiuniaContentBlock(NiuniaBlockKind.Info, Modifier.padding(start = NiuniaDims.Gutter)) {
                NiuniaText(row?.catalogName ?: stringResource(R.string.niunia_tv2_cat_loading), variant = NiuniaTextVariant.TitleScreen, lines = 1)
                if (row != null) NiuniaText(listOfNotNull(row.addonName.takeIf { it.isNotBlank() }, row.items.size.toString() + if (row.hasMore) "+" else "").joinToString(" · "),
                    variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 1)
            }
            when {
                row == null || (row.items.isEmpty() && row.isLoading) -> Unit
                row.items.isEmpty() -> NiuniaContentBlock(NiuniaBlockKind.Empty, Modifier.padding(start = NiuniaDims.Gutter)) {
                    NiuniaText(stringResource(R.string.niunia_tv2_cat_empty), variant = NiuniaTextVariant.Body, tone = NiuniaTone.Secondary, lines = 2)
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(6), state = grid, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter, top = 8.dp, bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap), verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    itemsIndexed(row.items, key = { i, it -> "${i}_${it.id}" }) { i, item ->
                        val (art, fallback) = remember(item) { com.nuvio.tv.skin.niunia.components.niuniaPortrait(item, "") }
                        NiuniaPoster(
                            art = art, fallback = fallback, title = item.name, logo = item.logo, meta = com.nuvio.tv.skin.niunia.NiuniaLabels.meta(item), label = null, baked = art != null,
                            modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                            onOk = { Log.i(TAG, "katalog: OK → ${item.id}"); onNavigateToDetail(item.id, item.rawType, row.addonBaseUrl) },
                            onLongOk = { posterOptions(item, row.addonBaseUrl) },
                        )
                    }
                }
            }
        }
    }
}
