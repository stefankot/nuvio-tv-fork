package com.nuvio.tv.skin.niunia.livetv

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.tv.R
import com.nuvio.tv.fork.tv.ShowIdentity
import com.nuvio.tv.fork.tv.TvStore
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories
import com.nuvio.tv.skin.kicia.livetv.ForgeTvSearchMemory
import com.nuvio.tv.skin.kicia.livetv.forgeTvSearch
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import kotlinx.coroutines.delay

private const val TAG = "ForkNiunia"
private val LETTERS = ('a'..'z').map { it.toString() } + listOf("ą", "ć", "ę", "ł", "ń", "ó", "ś", "ź", "ż")
private val DIGITS = ('1'..'9').map { it.toString() } + "0"
private const val FAV_KEY = "\u0000niunia:ulubione"

/** Wiersz drzewa kategorii: poziom 0 = gałąź, 1 = liść. */
private class TreeRow(val key: String, val name: String, val count: Int, val level: Int)

/**
 * Live TV → Szukaj (canvas `LiveTVSearch`): klawiatura jak w głównej wyszukiwarce Niuni, pod nią podpowiedzi albo
 * ostatnio wyszukiwane; obok drzewo kategorii programów z EPG ([ForgeEpgCategories.programmeTree]) z liczbą programów —
 * wpisywanie filtruje drzewo (pasujące + rodzice); po prawej wyniki (programy i kanały) jako Rzędy albo Siatka (zapamiętane).
 * Dopasowanie: [forgeTvSearch] z Kicii (małe litery, bez polskich znaków). Kategoria w drzewie = zaznaczona, nie fokus.
 */
@Composable
internal fun NiuniaLiveSearch(
    channels: List<LiveTvChannel>,
    current: Map<String, LiveTvProgramme>,
    now: Long,
    onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf(ForgeTvSearchMemory.lastQuery) }
    var digits by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(ForgeEpgCategories.PROG_ALL) }
    var grid by remember { mutableStateOf(NiuniaLiveFilters.searchGrid(context)) }
    val favShows by TvStore.favoriteShows.collectAsState()
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(60)
        try { firstKey.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "szukaj tv: fokus klawiatury: ${e.javaClass.simpleName}: ${e.message}") }
    }

    val tree = rememberComputed(channels, empty = emptyList<ForgeEpgCategories.Node>(), what = "drzewo kategorii") { ForgeEpgCategories.programmeTree(channels, now) }
    val total = remember(tree) { tree.sumOf { it.count } }
    val treeRows = remember(tree, query, category, favShows) {
        val q = ForgeEpgCategories.norm(query.trim())
        val out = ArrayList<TreeRow>()
        out += TreeRow(ForgeEpgCategories.PROG_ALL, context.getString(R.string.niunia_tv_all_programmes), total, 0)
        out += TreeRow(FAV_KEY, context.getString(R.string.niunia_tv_favourites), favShows.size, 0)
        tree.forEach { b ->
            val leaves = b.children.filter { q.isEmpty() || ForgeEpgCategories.norm(it.name).contains(q) }
            val branchHit = q.isNotEmpty() && ForgeEpgCategories.norm(b.name).contains(q)
            if (q.isNotEmpty() && !branchHit && leaves.isEmpty()) return@forEach
            out += TreeRow(b.key, b.name, b.count, 0)
            val open = q.isNotEmpty() || category == b.key || category.startsWith(b.key + "/")
            if (open) (if (q.isEmpty() || branchHit && leaves.isEmpty()) b.children else leaves).forEach { out += TreeRow(it.key, it.name, it.count, 1) }
        }
        out
    }
    val matchedCategories = remember(treeRows, query) { if (query.isBlank()) 0 else treeRows.count { it.level >= 0 && it.key != ForgeEpgCategories.PROG_ALL && it.key != FAV_KEY } }

    val results = rememberComputed(query, category, channels, favShows, empty = null as com.nuvio.tv.skin.kicia.livetv.ForgeTvSearchResults?, what = "szukaj") {
        delay(250) // pisanie
        if (category == FAV_KEY) forgeTvSearch(query, "\u0000prog:ulubione", channels, current, now,
            inCategory = { _, p -> ShowIdentity.normalizeTitle(p.title) in favShows })
        else forgeTvSearch(query, category, channels, current, now)
    }
    val programmeTiles = remember(results) { results?.programmes?.map { tileFor(it.channel, it.programme, now) }.orEmpty() }
    val channelTiles = remember(results) { results?.channels?.map { h -> h.now?.let { tileFor(h.channel, it, now) } ?: NiuniaLiveTile(h.channel, null, NiuniaTileMark.LIVE) }.orEmpty() }
    LaunchedEffect(results) {
        results ?: return@LaunchedEffect
        Log.i(TAG, "szukaj tv: „$query” (${ForgeEpgCategories.nameOf(category)}) → programy=${results.programmes.size}, kanały=${results.channels.size}, kategorie=$matchedCategories")
    }
    val suggestions = remember(query, results, treeRows) {
        if (query.isBlank()) ForgeTvSearchMemory.recent.take(3)
        else {
            val q = ForgeEpgCategories.norm(query.trim())
            (programmeTiles.mapNotNull { it.programme?.title?.lowercase() } + channelTiles.map { it.channel.name.lowercase() } + treeRows.drop(2).map { it.name.lowercase() })
                .filter { ForgeEpgCategories.norm(it).let { n -> n != q && n.contains(q) } }.distinct().take(3)
        }
    }

    fun type(q: String) { query = q; ForgeTvSearchMemory.lastQuery = q }
    fun open(ch: LiveTvChannel, p: LiveTvProgramme?) {
        if (query.isNotBlank()) ForgeTvSearchMemory.add(query.trim())
        onPlay(ch, p)
    }

    Box(Modifier.fillMaxSize()) {
        // Pole: tekst + liczniki wyników zamiast podpowiedzi.
        Row(Modifier.padding(start = NiuniaDims.Gutter, top = 84.dp).height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, null, tint = NiuniaColors.Text70, modifier = Modifier.size(24.dp))
            Text(query.ifEmpty { stringResource(R.string.niunia_tv_search_hint) }, style = NiuniaText.Logo.copy(fontSize = 26.sp, lineHeight = 32.sp),
                color = if (query.isEmpty()) NiuniaColors.Text50 else NiuniaColors.Text, maxLines = 1, modifier = Modifier.padding(start = 10.dp))
            if (query.isNotEmpty()) Box(Modifier.padding(start = 2.dp).size(2.dp, 28.dp).background(NiuniaColors.Text))
            if (results != null && (query.isNotBlank() || category != ForgeEpgCategories.PROG_ALL)) Text(
                stringResource(R.string.niunia_tv_search_counts, results.programmes.size, results.channels.size, matchedCategories),
                style = NiuniaText.Body, color = NiuniaColors.Text50, modifier = Modifier.padding(start = 12.dp))
        }
        // Klawiatura (x 40) + podpowiedzi.
        Column(Modifier.padding(start = 37.dp, top = 164.dp).width(224.dp)) {
            Row(Modifier.padding(start = 3.dp).width(216.dp).height(32.dp).background(Color(0x0FFFFFFF), RoundedCornerShape(16.dp)).padding(3.dp)) {
                Seg("abc", !digits, Modifier.weight(1f)) { digits = false }
                Seg("123", digits, Modifier.weight(1f)) { digits = true }
            }
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                (if (digits) DIGITS else LETTERS).chunked(6).forEachIndexed { r, line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        line.forEachIndexed { c, k -> Key(k, if (r == 0 && c == 0) firstKey else null) { type(query + k) } }
                    }
                }
            }
            Row(Modifier.padding(top = 10.dp, start = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallKey(stringResource(R.string.niunia_search_space), null) { type("$query ") }
                SmallKey(null, Icons.AutoMirrored.Outlined.Backspace) { type(query.dropLast(1)) }
                SmallKey(stringResource(R.string.niunia_search_clear), null) { type("") }
            }
            if (suggestions.isNotEmpty()) {
                Text(stringResource(if (query.isBlank()) R.string.niunia_tv_recent_searches else R.string.niunia_tv_suggestions), style = NiuniaText.Caption.copy(fontSize = 12.sp),
                    color = NiuniaColors.Text50, modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 4.dp))
                suggestions.forEach { s -> Suggestion(s, if (query.isBlank()) Icons.Outlined.History else Icons.Outlined.Search) { type(s) } }
            }
        }
        // Drzewo kategorii (x 290).
        Column(Modifier.padding(start = 290.dp, top = 150.dp).width(270.dp)) {
            Text(buildAnnotatedString {
                append(stringResource(R.string.niunia_tv_categories))
                if (query.isNotBlank()) withStyle(SpanStyle(color = NiuniaColors.Text50, fontWeight = FontWeight.Medium, fontSize = 13.sp)) {
                    append(" · " + context.getString(R.string.niunia_tv_categories_match, matchedCategories, tree.sumOf { 1 + it.children.size }))
                }
            }, style = NiuniaText.Row, color = NiuniaColors.Text, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
            LazyColumn(Modifier.fillMaxWidth().height(380.dp), contentPadding = PaddingValues(6.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                items(treeRows, key = { it.key }) { row ->
                    TreeItem(row, query, selected = row.key == category) {
                        Log.i(TAG, "szukaj tv: kategoria ${row.name}")
                        category = row.key
                    }
                }
            }
        }
        // Wyniki (x 584).
        Column(Modifier.padding(start = 584.dp, top = 150.dp, end = NiuniaDims.Gutter).fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.niunia_tv_programmes) + (results?.let { " · ${it.programmes.size}" } ?: ""), style = NiuniaText.Row, color = NiuniaColors.Text50, modifier = Modifier.weight(1f))
                Row(Modifier.width(150.dp).height(32.dp).background(Color(0x0FFFFFFF), RoundedCornerShape(16.dp)).padding(3.dp)) {
                    Seg(stringResource(R.string.niunia_tv_view_rows), !grid, Modifier.weight(1f)) { grid = false; NiuniaLiveFilters.setSearchGrid(context, false); Log.i(TAG, "szukaj tv: widok rzędy") }
                    Seg(stringResource(R.string.niunia_tv_view_grid), grid, Modifier.weight(1f)) { grid = true; NiuniaLiveFilters.setSearchGrid(context, true); Log.i(TAG, "szukaj tv: widok siatka") }
                }
            }
            when {
                results == null -> Unit
                programmeTiles.isEmpty() && channelTiles.isEmpty() -> Text(
                    stringResource(if (query.isBlank() && category == ForgeEpgCategories.PROG_ALL) R.string.niunia_tv_search_start else R.string.niunia_tv_search_none),
                    style = NiuniaText.Body, color = NiuniaColors.Text50, modifier = Modifier.padding(top = 16.dp))
                grid -> LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 14.dp, bottom = 30.dp, end = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(programmeTiles.size + channelTiles.size) { i ->
                        val t = if (i < programmeTiles.size) programmeTiles[i] else channelTiles[i - programmeTiles.size]
                        NiuniaLiveTileCard(t, longBadge = false) { open(t.channel, t.programme) }
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                    if (programmeTiles.isNotEmpty()) item { NiuniaLiveShelfPlain(programmeTiles) { ch, p -> open(ch, p) } }
                    if (channelTiles.isNotEmpty()) item {
                        Text(stringResource(R.string.niunia_tv_channels) + " · ${channelTiles.size}", style = NiuniaText.Row, color = NiuniaColors.Text50, modifier = Modifier.padding(top = 8.dp))
                        NiuniaLiveShelfPlain(channelTiles) { ch, p -> open(ch, p) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NiuniaLiveShelfPlain(tiles: List<NiuniaLiveTile>, onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(top = 10.dp, bottom = 14.dp, end = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        items(tiles, key = { it.key }) { t -> NiuniaLiveTileCard(t, longBadge = false) { onPlay(t.channel, t.programme) } }
    }
}

@Composable
private fun TreeItem(row: TreeRow, query: String, selected: Boolean, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(NiuniaDims.RadiusCard)
    Row(
        Modifier.fillMaxWidth().height(30.dp)
            .niuniaFocusable(shape, scale = 1.02f, lift = false, onFocus = { focused = it }, onOk = onOk)
            .background(when { focused -> NiuniaColors.Focus; selected -> NiuniaColors.Fill24; else -> Color.Transparent }, shape)
            .padding(start = 8.dp + 16.dp * row.level, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val c = if (focused) NiuniaColors.OnFocus else if (row.level == 0 || selected) NiuniaColors.Text else NiuniaColors.Text70
        Text(highlight(row.name, query, focused), style = NiuniaText.Meta, color = c, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text("%,d".format(row.count).replace(',', ' '), style = NiuniaText.Caption.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = if (focused) NiuniaColors.OnFocus.copy(alpha = .6f) else NiuniaColors.Text50)
    }
}

/** Dopasowany fragment pogrubiony na biało (porównanie bez polskich znaków — długość tekstu się nie zmienia). */
private fun highlight(name: String, query: String, focused: Boolean) = buildAnnotatedString {
    val q = ForgeEpgCategories.norm(query.trim())
    val i = if (q.isEmpty()) -1 else ForgeEpgCategories.norm(name).indexOf(q)
    if (i < 0 || i + q.length > name.length) { append(name); return@buildAnnotatedString }
    append(name.substring(0, i))
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text)) { append(name.substring(i, i + q.length)) }
    append(name.substring(i + q.length))
}

@Composable
private fun Suggestion(text: String, icon: ImageVector, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(NiuniaDims.RadiusCard)
    Row(
        Modifier.fillMaxWidth().height(28.dp).niuniaFocusable(shape, scale = 1.02f, lift = false, onFocus = { focused = it }, onOk = onOk)
            .background(if (focused) NiuniaColors.Focus else Color.Transparent, shape).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val c = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text70
        Icon(icon, null, tint = c, modifier = Modifier.size(16.dp))
        Text(text, style = NiuniaText.Meta, color = c, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Seg(text: String, on: Boolean, modifier: Modifier, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(13.dp)
    Box(modifier.height(26.dp).niuniaFocusable(shape, scale = 1.04f, lift = false, onFocus = { focused = it }, onOk = onOk)
        .background(when { focused -> NiuniaColors.Focus; on -> NiuniaColors.Fill24; else -> Color.Transparent }, shape), contentAlignment = Alignment.Center) {
        Text(text, style = NiuniaText.Caption.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = when { focused -> NiuniaColors.OnFocus; on -> NiuniaColors.Text; else -> NiuniaColors.Text70 })
    }
}

@Composable
private fun Key(k: String, requester: FocusRequester?, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(9.dp)
    Box(Modifier.size(34.dp).niuniaFocusable(shape, scale = 1.12f, lift = false, focusRequester = requester, onFocus = { focused = it }, onOk = onOk)
        .background(if (focused) Color.White else Color.Transparent, shape), contentAlignment = Alignment.Center) {
        Text(k, style = NiuniaText.Logo.copy(fontSize = 20.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium), color = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text70)
    }
}

@Composable
private fun SmallKey(text: String?, icon: ImageVector?, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Box(Modifier.height(30.dp).niuniaFocusable(shape, scale = 1.06f, lift = false, onFocus = { focused = it }, onOk = onOk)
        .background(if (focused) NiuniaColors.Focus else NiuniaColors.Fill12, shape).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
        val c = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text70
        if (icon != null) Icon(icon, null, tint = c, modifier = Modifier.size(18.dp))
        if (text != null) Text(text, style = NiuniaText.Caption.copy(fontSize = 12.sp), color = c)
    }
}
