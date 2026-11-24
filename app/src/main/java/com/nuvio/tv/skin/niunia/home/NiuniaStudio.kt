package com.nuvio.tv.skin.niunia.home

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.core.tmdb.TmdbEntityMediaType
import com.nuvio.tv.core.tmdb.TmdbEntityRailType
import com.nuvio.tv.skin.niunia.components.NiuniaBlockKind
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaCollectionHeader
import com.nuvio.tv.skin.niunia.components.NiuniaContentBlock
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.ui.screens.tmdb.TmdbEntityBrowseUiState
import com.nuvio.tv.ui.screens.tmdb.TmdbEntityBrowseViewModel

private const val TAG = "ForkNiunia"

/**
 * Z-14 (makieta Studio, trasa `tmdb_entity_browse`): studio / sieć z TMDB w stylu Niuni — nagłówek jak strona osoby
 * (logo albo inicjały, nazwa, rodzaj i kraj, opis), rzędy Popularne / Najwyżej oceniane / Najnowsze (filmy i seriale),
 * doczytywanie rzędu 4 pozycje przed końcem. Dane i ich wczytywanie: [TmdbEntityBrowseViewModel] bazy.
 */
@Composable
internal fun NiuniaStudio(
    onBack: () -> Unit,
    onNavigateToDetail: (String, String, String?) -> Unit,
    viewModel: TmdbEntityBrowseViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val posterOptions = com.nuvio.tv.skin.niunia.components.rememberNiuniaPosterOptions { id, t, url -> onNavigateToDetail(id, t, url) }
    val first = remember { FocusRequester() }
    BackHandler { Log.i(TAG, "studio: Wstecz"); onBack() }
    Box(Modifier.fillMaxSize().background(NiuniaColor.Bg)) {
        com.nuvio.tv.skin.niunia.components.NiuniaClockCorner() // Z-15
        when (val s = state) {
            TmdbEntityBrowseUiState.Loading -> NiuniaText(stringResource(R.string.niunia_tv2_cat_loading), Modifier.padding(start = NiuniaDims.Gutter, top = 80.dp), variant = NiuniaTextVariant.Body, tone = NiuniaTone.Secondary)
            is TmdbEntityBrowseUiState.Error -> {
                LaunchedEffect(s.message) { Log.w(TAG, "studio: błąd: ${s.message}") }
                NiuniaContentBlock(NiuniaBlockKind.Empty, Modifier.padding(start = NiuniaDims.Gutter, top = 80.dp)) {
                    NiuniaText(s.message, variant = NiuniaTextVariant.Body, tone = NiuniaTone.Secondary, lines = 3)
                    NiuniaButton(onClick = { viewModel.retry() }, label = stringResource(R.string.niunia_tv1_error_retry), icon = "restart", focusRequester = first)
                }
                LaunchedEffect(Unit) { kotlinx.coroutines.delay(80); try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "studio: fokus: ${e.message}") } }
            }
            is TmdbEntityBrowseUiState.Success -> {
                val h = s.data.header
                val rails = s.data.rails.filter { it.items.isNotEmpty() }
                LaunchedEffect(h.id, rails.isNotEmpty()) {
                    Log.i(TAG, "studio: ${h.name} (${h.kind}), rzędy=${rails.size}")
                    if (rails.isEmpty()) return@LaunchedEffect
                    kotlinx.coroutines.delay(80)
                    try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "studio: fokus startowy: ${e.javaClass.simpleName}: ${e.message}") }
                }
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = NiuniaSpacing.Space40, bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space24)) {
                    item(key = "head") {
                        Row(Modifier.padding(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20), verticalAlignment = Alignment.CenterVertically) {
                            if (h.logo != null) Box(Modifier.size(120.dp, 80.dp).background(NiuniaColor.White.copy(alpha = 0.92f), androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(12.dp), contentAlignment = Alignment.Center) {
                                AsyncImage(model = h.logo, contentDescription = h.name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                            } else Box(Modifier.size(80.dp).background(NiuniaColor.Fill24, CircleShape), contentAlignment = Alignment.Center) {
                                NiuniaText(h.name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }, variant = NiuniaTextVariant.TitleSection)
                            }
                            NiuniaContentBlock(NiuniaBlockKind.Info, Modifier.widthIn(max = 600.dp)) {
                                NiuniaText(h.name, variant = NiuniaTextVariant.TitleScreen, lines = 1)
                                NiuniaText(listOfNotNull(stringResource(if (h.kind.name.contains("NETWORK", true)) R.string.niunia_tv2_studio_network else R.string.niunia_tv2_studio_company), h.secondaryLabel, h.originCountry).joinToString(" · "),
                                    variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 1)
                                h.description?.takeIf { it.isNotBlank() }?.let { NiuniaText(it, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 2) }
                            }
                        }
                    }
                    rails.forEachIndexed { ri, rail ->
                        item(key = "rail_${rail.mediaType}_${rail.railType}") {
                            val title = stringResource(when (rail.railType) { TmdbEntityRailType.POPULAR -> R.string.niunia_tv2_studio_popular; TmdbEntityRailType.TOP_RATED -> R.string.niunia_tv2_studio_top; TmdbEntityRailType.RECENT -> R.string.niunia_tv2_studio_recent }) +
                                " · " + stringResource(if (rail.mediaType == TmdbEntityMediaType.MOVIE) R.string.niunia_ke_lib_movies else R.string.niunia_ke_lib_series)
                            Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
                                NiuniaCollectionHeader(title, active = true, modifier = Modifier.padding(start = NiuniaDims.Gutter).height(22.dp))
                                LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                                    itemsIndexed(rail.items, key = { i, it -> "${i}_${it.id}" }) { i, item ->
                                        if (i >= rail.items.size - 4 && rail.hasMore && !rail.isLoading) LaunchedEffect(rail.items.size) { Log.i(TAG, "studio: doczytuję ${rail.railType}"); viewModel.loadMoreRail(rail.mediaType, rail.railType) }
                                        val (art, fallback) = remember(item) { com.nuvio.tv.skin.niunia.components.niuniaPortrait(item, "") }
                                        NiuniaPoster(
                                            art = art, fallback = fallback, title = item.name, logo = item.logo, meta = com.nuvio.tv.skin.niunia.NiuniaLabels.meta(item), label = null, baked = art != null,
                                            modifier = if (ri == 0 && i == 0) Modifier.focusRequester(first) else Modifier,
                                            onOk = { Log.i(TAG, "studio: OK → ${item.id}"); onNavigateToDetail(item.id, item.rawType, null) },
                                            onLongOk = { posterOptions(item, null) },
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
}
