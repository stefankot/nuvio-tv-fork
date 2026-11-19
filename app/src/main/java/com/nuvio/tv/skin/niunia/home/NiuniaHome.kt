package com.nuvio.tv.skin.niunia.home

import com.nuvio.tv.skin.niunia.components.niuniaPortrait
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch
import android.util.Log
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.skin.niunia.NiuniaLabels
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.components.NiuniaRoundButton
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import com.nuvio.tv.ui.screens.home.ContinueWatchingItem
import com.nuvio.tv.ui.screens.home.HomeUiState

private const val TAG = "ForkNiunia"

/**
 * Ekran główny Niunii (canvas: Home + HomePage): hero na całą szerokość z logo, Continue Watching, potem rzędy katalogów
 * jako plakaty NPoster. Tytuł rzędu szary, rząd z fokusem biały. Dane: wyłącznie [HomeUiState].
 */
@Composable
fun NiuniaHome(
    uiState: HomeUiState,
    onNavigateToDetail: (String, String, String) -> Unit,
    onContinueWatchingClick: (ContinueWatchingItem) -> Unit,
    onItemFocus: (MetaPreview) -> Unit,
) {
    val context = LocalContext.current
    // Baner: tylko filmy i seriale z dużym obrazem tła (bez pozycji „narzędziowych” dodatków, np. „Delete all files”).
    // Najpierw źródła wybrane w ustawieniach (heroItems), potem wszystkie rzędy ekranu głównego.
    val heroes = remember(uiState.heroItems, uiState.catalogRows) {
        fun ok(m: MetaPreview) = (m.rawType == "movie" || m.rawType == "series") && !m.background.isNullOrBlank()
        (uiState.heroItems.filter(::ok) + uiState.catalogRows.flatMap { r -> r.items.filter(::ok).take(3) })
            .distinctBy { it.id }.take(8)
    }
    var heroIndex by remember { mutableIntStateOf(0) }
    var focusedRow by remember { mutableIntStateOf(-1) }
    val heroFocus = remember { FocusRequester() }
    val posterPattern = remember(uiState.customPosterUrlPattern, uiState.customPosterEnabledScreens) {
        com.nuvio.tv.core.poster.patternForScreen(uiState.customPosterUrlPattern, com.nuvio.tv.core.poster.CustomPosterScreen.HOME, uiState.customPosterEnabledScreens)
    }
    // Hero ucięty od góry: po powrocie z rzędów (▲) lista przewijała się tylko tyle, by pokazać przycisk.
    // Fokus w hero = lista na samą górę (jak w projekcie: hero zawsze w całości).
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(heroes.size, uiState.catalogRows.size) {
        Log.i(TAG, "ekran główny: hero=${heroes.size}, dalej oglądaj=${uiState.continueWatchingItems.size}, rzędy=${uiState.catalogRows.size}")
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(NiuniaColors.Bg),
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        heroes.getOrNull(heroIndex % heroes.size.coerceAtLeast(1))?.let { hero ->
            item(key = "niunia_hero") {
                Hero(
                    item = hero, index = heroIndex, count = heroes.size, focus = heroFocus,
                    onFocusInside = { if (listState.firstVisibleItemIndex != 0 || listState.firstVisibleItemScrollOffset != 0) scope.launch { listState.animateScrollToItem(0) } },
                    onOpen = { onNavigateToDetail(hero.id, hero.rawType, uiState.catalogRows.firstOrNull()?.addonBaseUrl ?: "") },
                    onNext = { heroIndex = (heroIndex + 1) % heroes.size },
                )
            }
        }
        if (uiState.continueWatchingItems.isNotEmpty()) {
            item(key = "niunia_cw") {
                RowTitle(stringResource(R.string.niunia_row_continue), on = focusedRow == -2)
                LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                    items(uiState.continueWatchingItems, key = { cwKey(it) }) { cw ->
                        ContinueCard(cw, onFocus = { if (it) focusedRow = -2 }, onOk = { onContinueWatchingClick(cw) })
                    }
                }
            }
        }
        uiState.catalogRows.forEachIndexed { rowIndex, row ->
            if (row.items.isEmpty()) return@forEachIndexed
            item(key = "niunia_row_${row.addonId}_${row.catalogId}_${row.rawType}") {
                val ranking = NiuniaLabels.isRankingRow(row.catalogName)
                RowTitle(row.catalogName, on = focusedRow == rowIndex)
                LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                    itemsIndexed(row.items, key = { _, it -> it.id }) { i, item ->
                        val (art, fallback) = remember(item, posterPattern) { niuniaPortrait(item, posterPattern) }
                        NiuniaPoster(
                            art = art,
                            fallback = fallback,
                            title = item.name,
                            logo = item.logo,
                            meta = NiuniaLabels.meta(item),
                            label = NiuniaLabels.label(context, item, i, ranking),
                            // Plakaty z dodatków zwykle mają tytuł w grafice → wariant „baked” (decyzja: plan/DECYZJE.md, Niunia).
                            baked = art != null,
                            onFocus = { if (it) { focusedRow = rowIndex; onItemFocus(item) } },
                            onOk = { onNavigateToDetail(item.id, item.rawType, row.addonBaseUrl) },
                        )
                    }
                }
            }
        }
    }
}

private fun cwKey(cw: ContinueWatchingItem): String = when (cw) {
    is ContinueWatchingItem.InProgress -> "p_${cw.progress.contentId}_${cw.progress.videoId}"
    is ContinueWatchingItem.NextUp -> "n_${cw.info.contentId}_${cw.info.videoId}"
}

@Composable
private fun RowTitle(text: String, on: Boolean) {
    Text(
        text, style = NiuniaText.Row, color = if (on) NiuniaColors.Text else NiuniaColors.Text50, maxLines = 1,
        modifier = Modifier.padding(start = NiuniaDims.Gutter, top = 18.dp),
    )
}

@Composable
private fun Hero(item: MetaPreview, index: Int, count: Int, focus: FocusRequester, onFocusInside: () -> Unit, onOpen: () -> Unit, onNext: () -> Unit) {
    LaunchedEffect(Unit) {
        try { focus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "fokus hero: ${e.javaClass.simpleName}: ${e.message}") }
    }
    Box(Modifier.fillMaxWidth().height(480.dp)) {
        AsyncImage(model = item.background ?: item.landscapePoster ?: item.poster, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to NiuniaColors.ScrimLeft, .38f to Color(0x8C18181A), .68f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(.55f to Color.Transparent, 1f to NiuniaColors.Bg)))
        Column(Modifier.align(Alignment.BottomStart).padding(start = NiuniaDims.Gutter, bottom = 40.dp).width(420.dp)) {
            if (!item.logo.isNullOrBlank()) {
                AsyncImage(model = item.logo, contentDescription = item.name, contentScale = ContentScale.Fit, alignment = Alignment.BottomStart,
                    modifier = Modifier.widthIn(max = 370.dp).heightIn(max = 110.dp))
            } else {
                Text(item.name.uppercase(), style = NiuniaText.Logo, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            val meta = listOfNotNull(item.genres.take(2).joinToString(" · ").ifBlank { null }, item.releaseInfo, item.runtime, item.ageRating).joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, style = NiuniaText.Meta, modifier = Modifier.padding(top = 10.dp), maxLines = 1)
            if (!item.description.isNullOrBlank()) Text(item.description, style = NiuniaText.Body, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 18.dp).onFocusChanged { if (it.hasFocus) onFocusInside() }, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                NiuniaButton(stringResource(R.string.niunia_hero_open), onOpen, primary = true, focusRequester = focus)
                if (count > 1) NiuniaRoundButton("›", stringResource(R.string.niunia_hero_next), onNext)
            }
            if (count > 1) Dots(index % count, count)
        }
    }
}

@Composable
private fun Dots(selected: Int, count: Int) {
    Row(Modifier.padding(top = 14.dp).background(Color(0x40000000), RoundedCornerShape(9.dp)).padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(count.coerceAtMost(10)) { i ->
            Box(Modifier.size(width = if (i == selected) 14.dp else 5.dp, height = 5.dp).background(if (i == selected) Color.White else Color(0x66FFFFFF), RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
private fun ContinueCard(cw: ContinueWatchingItem, onFocus: (Boolean) -> Unit, onOk: () -> Unit) {
    val (image, caption, fraction) = when (cw) {
        is ContinueWatchingItem.InProgress -> {
            val p = cw.progress
            val f = p.progressPercent?.div(100f) ?: if (p.duration > 0) p.position.toFloat() / p.duration else 0f
            val cap = if (p.season != null && p.episode != null) "S${p.season}, E${p.episode}" else p.name
            Triple(cw.episodeThumbnail ?: p.backdrop ?: p.poster, cap, f.coerceIn(0f, 1f))
        }
        is ContinueWatchingItem.NextUp -> Triple(cw.info.thumbnail ?: cw.info.backdrop ?: cw.info.poster, "S${cw.info.season}, E${cw.info.episode}", 0f)
    }
    val shape = RoundedCornerShape(NiuniaDims.RadiusCard)
    Box(Modifier.size(NiuniaDims.WideW, NiuniaDims.WideH).niuniaFocusable(shape, onFocus = onFocus, onOk = onOk).clip(shape).background(NiuniaColors.Surface)) {
        AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        Row(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xA6000000))))
                .padding(start = 8.dp, end = 8.dp, top = 22.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            if (fraction > 0f) {
                Box(Modifier.size(width = 22.dp, height = 3.dp).background(Color(0x59FFFFFF), RoundedCornerShape(2.dp))) {
                    Box(Modifier.size(width = (22 * fraction).dp, height = 3.dp).background(Color.White, RoundedCornerShape(2.dp)))
                }
            }
            Text(caption, style = NiuniaText.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    Spacer(Modifier.width(0.dp))
}
