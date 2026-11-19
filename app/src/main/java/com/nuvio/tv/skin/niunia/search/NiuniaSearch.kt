package com.nuvio.tv.skin.niunia.search

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.skin.niunia.NiuniaLabels
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import com.nuvio.tv.ui.screens.search.SearchEvent
import com.nuvio.tv.ui.screens.search.SearchViewModel

private const val TAG = "ForkNiunia"
private val LETTERS = ('a'..'z').map { it.toString() }
private val DIGITS = ('1'..'9').map { it.toString() } + "0"

/**
 * Wyszukiwarka Niunii (canvas: Search, SearchResults): z lewej pole i klawiatura liter 6 w rzędzie (abc / 123,
 * SPACJA, ⌫, WYCZYŚĆ), z prawej: bez zapytania — ostatnie wyszukiwania i „Przeglądaj” (plakaty z odkrywania),
 * z zapytaniem — karty wyników (miniatura, tytuł, typ · rok). Dane i zdarzenia: [SearchViewModel] Nuvio.
 */
@Composable
fun NiuniaSearch(viewModel: SearchViewModel, onNavigateToDetail: (String, String, String) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var digits by remember { mutableStateOf(false) }
    val firstKey = remember { FocusRequester() }
    fun type(q: String) = viewModel.onEvent(SearchEvent.QueryChanged(q))
    LaunchedEffect(Unit) {
        try { firstKey.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "fokus klawiatury: ${e.javaClass.simpleName}: ${e.message}") }
    }
    val results: List<Pair<MetaPreview, String>> = remember(state.catalogRows) {
        state.catalogRows.flatMap { row -> row.items.map { it to row.addonBaseUrl } }.distinctBy { it.first.id }
    }
    LaunchedEffect(results.size, state.submittedQuery) {
        if (state.submittedQuery.isNotBlank()) Log.i(TAG, "wyszukiwarka: ${results.size} wyników")
    }

    Row(Modifier.fillMaxSize().background(NiuniaColors.Bg).padding(start = NiuniaDims.Gutter, top = 74.dp, end = NiuniaDims.Gutter)) {
        Column(Modifier.width(220.dp)) {
            Text(
                state.query.ifBlank { stringResource(R.string.niunia_search_hint) },
                style = NiuniaText.Logo.copy(fontSize = 22.sp, lineHeight = 28.sp),
                color = if (state.query.isBlank()) NiuniaColors.Text50 else NiuniaColors.Text,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 18.dp).fillMaxWidth().background(NiuniaColors.Fill12.copy(alpha = .06f), RoundedCornerShape(16.dp)).padding(3.dp)) {
                Seg("abc", !digits, Modifier.weight(1f)) { digits = false }
                Seg("123", digits, Modifier.weight(1f)) { digits = true }
            }
            val keys = if (digits) DIGITS else LETTERS
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                keys.chunked(6).forEachIndexed { r, line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        line.forEachIndexed { c, k ->
                            Key(k, if (r == 0 && c == 0) firstKey else null) { type(state.query + k) }
                        }
                    }
                }
            }
            Row(Modifier.padding(top = 12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SmallKey(stringResource(R.string.niunia_search_space)) { type(state.query + " ") }
                SmallKey("⌫") { type(state.query.dropLast(1)) }
                SmallKey(stringResource(R.string.niunia_search_clear)) { type("") }
            }
        }
        Column(Modifier.padding(start = 36.dp).fillMaxSize()) {
            if (state.query.isBlank()) {
                if (state.recentSearches.isNotEmpty()) {
                    Text(stringResource(R.string.niunia_search_recent), style = NiuniaText.Row, color = NiuniaColors.Text50)
                    Row(Modifier.padding(top = 10.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.recentSearches.take(5).forEach { q -> Chip(q) { type(q) } }
                    }
                }
                Text(stringResource(R.string.niunia_search_browse), style = NiuniaText.Row, color = NiuniaColors.Text50)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(NiuniaDims.PosterW), contentPadding = PaddingValues(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap), verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    items(state.discoverResults.take(24), key = { it.id }) { item ->
                        NiuniaPoster(art = item.poster, fallback = item.rawPosterUrl, title = item.name, logo = item.logo, meta = NiuniaLabels.meta(item), label = null, baked = item.poster != null,
                            onOk = { onNavigateToDetail(item.id, item.rawType, "") })
                    }
                }
            } else {
                Text(stringResource(R.string.niunia_search_top), style = NiuniaText.Row, color = NiuniaColors.Text)
                if (state.isSearching && results.isEmpty()) {
                    Text(stringResource(R.string.niunia_search_searching), style = NiuniaText.Body, modifier = Modifier.padding(top = 14.dp))
                } else if (results.isEmpty() && state.submittedQuery.isNotBlank()) {
                    Text(stringResource(R.string.niunia_search_none, state.submittedQuery), style = NiuniaText.Body, modifier = Modifier.padding(top = 14.dp))
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2), contentPadding = PaddingValues(vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(results, key = { it.first.id }) { (item, addon) -> ResultCard(item) { onNavigateToDetail(item.id, item.rawType, addon) } }
                }
            }
        }
    }
}

@Composable
private fun Seg(text: String, on: Boolean, modifier: Modifier, onOk: () -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Box(modifier.height(26.dp).niuniaFocusable(shape, scale = 1.04f, lift = false, onOk = onOk).background(if (on) NiuniaColors.Fill24 else NiuniaColors.Bg.copy(alpha = 0f), shape),
        contentAlignment = Alignment.Center) {
        Text(text, style = NiuniaText.Caption, color = if (on) NiuniaColors.Text else NiuniaColors.Text70)
    }
}

@Composable
private fun Key(k: String, requester: FocusRequester?, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(9.dp)
    Box(Modifier.size(34.dp).niuniaFocusable(shape, scale = 1.12f, lift = false, focusRequester = requester, onFocus = { focused = it }, onOk = onOk)
        .background(if (focused) NiuniaColors.Focus else NiuniaColors.Bg.copy(alpha = 0f), shape), contentAlignment = Alignment.Center) {
        Text(k, style = NiuniaText.Logo.copy(fontSize = 20.sp, lineHeight = 20.sp), color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text70)
    }
}

@Composable
private fun SmallKey(text: String, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(Modifier.height(28.dp).niuniaFocusable(shape, scale = 1.06f, lift = false, onFocus = { focused = it }, onOk = onOk)
        .background(if (focused) NiuniaColors.Focus else NiuniaColors.Bg.copy(alpha = 0f), shape).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        Text(text, style = NiuniaText.Caption.copy(fontSize = 13.sp), color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text70)
    }
}

@Composable
private fun Chip(text: String, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    Box(Modifier.height(32.dp).niuniaFocusable(shape, scale = 1.06f, lift = false, onFocus = { focused = it }, onOk = onOk)
        .background(if (focused) NiuniaColors.Focus else NiuniaColors.Fill12, shape).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(text, style = NiuniaText.Meta, color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text, maxLines = 1)
    }
}

@Composable
private fun ResultCard(item: MetaPreview, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().height(80.dp).niuniaFocusable(shape, scale = 1.04f, lift = false, onFocus = { focused = it }, onOk = onOk)
            .background(if (focused) NiuniaColors.Focus else NiuniaColors.Fill12.copy(alpha = .1f), shape).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(model = item.poster, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp, 60.dp).clip(RoundedCornerShape(4.dp)).background(NiuniaColors.Surface))
        Column {
            Text(item.name, style = NiuniaText.Meta.copy(fontSize = 15.sp), color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(item.genres.firstOrNull(), item.releaseInfo).joinToString(" · ")
            if (sub.isNotBlank()) Text(sub, style = NiuniaText.Caption.copy(fontSize = 12.sp), color = if (focused) NiuniaColors.OnFocus.copy(alpha = .6f) else NiuniaColors.Text50, maxLines = 1)
        }
    }
}
