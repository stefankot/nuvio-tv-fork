package com.nuvio.tv.skin.kicia.search

import androidx.compose.material.icons.outlined.Mood
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Mic
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.components.ForgeIconButton
import com.nuvio.tv.skin.kicia.components.ForgePillSelect
import com.nuvio.tv.skin.kicia.components.ForgePosterCard
import com.nuvio.tv.skin.kicia.components.ForgeSheetRow
import com.nuvio.tv.skin.kicia.components.ForgeSideSheet
import com.nuvio.tv.skin.kicia.input.ForgeRemoteHook
import com.nuvio.tv.skin.kicia.keyboard.ForgeOnScreenKeyboard
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.screens.search.SearchEvent
import com.nuvio.tv.ui.screens.search.SearchViewModel

/**
 * S02 Szukaj (mockup): microphone + pill field at y 84, the 12-column Polish on-screen keyboard on OK,
 * "Ostatnio szukane" as pills, results as shelves ("Filmy i seriale"), and "Odkrywaj" (Typ / Katalog /
 * Gatunek + a grid of posters) always underneath. All data and searching come from Nuvio's own
 * [SearchViewModel]; this only draws it.
 *
 * TODO(scalanie): "Telewizja" and "YouTube" result shelves need search sources outside SearchViewModel. Fork 19.11: fork/discover/SearchExtras.
 */
@Composable
fun ForgeSearchScreen(
    viewModel: SearchViewModel,
    onNavigateToDetail: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    com.nuvio.tv.skin.kicia.nav.ForgeTabBackToBar() // Kicia v7.4: Wstecz z treści → pasek (własne BackHandlery niżej wygrywają)
    val state by viewModel.uiState.collectAsState()
    var keyboardOpen by remember { mutableStateOf(false) }
    var sheet by remember { mutableStateOf<DiscoverSheet?>(null) }
    val fieldFocus = remember { FocusRequester() }
    val micFocus = remember { FocusRequester() }
    val firstKey = remember { FocusRequester() }
    val listState = rememberLazyListState()

    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) {
            viewModel.onEvent(SearchEvent.QueryChanged(text))
            viewModel.onEvent(SearchEvent.SubmitSearch)
        }
    }
    fun listen() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Słucham…")
        runCatching { voice.launch(intent) }
    }
    // The remote's voice / assistant button opens Search and starts listening.
    LaunchedEffect(Unit) {
        if (ForgeRemoteHook.consumeVoiceWanted()) listen()
        ForgeRemoteHook.voiceRequests.collect { listen() }
    }
    LaunchedEffect(Unit) {
        viewModel.ensureDiscoverLoaded() // Odkrywaj: catalogs and the first page of the grid
        runCatching { firstKey.requestFocus() } // v7.4 (09): from the bar the focus lands on the key "a" (before: on the microphone)
    }
    val discoverOn = state.discoverLocation != com.nuvio.tv.domain.model.DiscoverLocation.OFF
    LaunchedEffect(keyboardOpen) { if (keyboardOpen) runCatching { firstKey.requestFocus() } }

    // Load more of the Discover grid near its end.
    LaunchedEffect(listState, state.discoverHasMore) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index to listState.layoutInfo.totalItemsCount }
            .collect { (last, total) ->
                if (last != null && total > 0 && last >= total - 3 && state.discoverHasMore && !state.discoverLoadingMore) {
                    viewModel.onEvent(SearchEvent.LoadNextDiscoverResults)
                }
            }
    }

    val query = state.query
    val results = remember(state.catalogRows) { forgeMergeResults(state.catalogRows.map { row -> row.items.map { it to row.addonBaseUrl } }) }
    val discoverRows = remember(state.discoverResults) { state.discoverResults.chunked(6) }
    val selectedCatalog = state.discoverCatalogs.firstOrNull { it.key == state.selectedDiscoverCatalogKey }
    val discoverBaseUrl = selectedCatalog?.addonBaseUrl.orEmpty()

    Box(modifier.fillMaxSize().background(ForgeColors.Bg)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(start = 266.dp), // v7.4: results start at x 306 (keyboard column 40-266, W-88)
            // The bar sits above (Search starts 66 dp down); the field lands at y 84.
            contentPadding = PaddingValues(top = 30.dp, bottom = 60.dp), // v7.4: field at y 96
            verticalArrangement = Arrangement.spacedBy(16.dp), // v7.4: gaps from the mockup (field 140 -> header 156)
        ) {
            item(key = "field") {
                Row(
                    Modifier.padding(horizontal = ForgeDims.ContentStart),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    com.nuvio.tv.skin.kicia.components.ForgeTagged("pole") {
                        com.nuvio.tv.skin.kicia.components.ForgeField(query, "Filmy i seriale", Modifier.weight(1f), fill = true, focusRequester = fieldFocus) { keyboardOpen = true } // v7.4: komponent Pole
                    }
                    ForgeIconButton(icon = Icons.Outlined.Mic, contentDescription = "Szukaj głosowo", focusRequester = micFocus, modifier = Modifier.testTag("przycisk.0"), onOk = { listen() }) // v7.4 (09)
                }
            }

            if (query.isBlank()) {
                // Fork 19.11: „Na co masz ochotę?” (nastroje, fork/picks) przeniesione z „Narzędzi”.
                if (com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(androidx.compose.ui.platform.LocalContext.current, "module_picks", true)) item(key = "fork_picks") {
                    Row(Modifier.padding(horizontal = ForgeDims.ContentStart)) {
                        ForgePillSelect(text = "Na co masz ochotę?", selected = false, bare = false, icon = Icons.Outlined.Mood,
                            onOk = { com.nuvio.tv.ui.screens.settings.ForkFeatures.open?.invoke(com.nuvio.tv.fork.picks.PicksModule.ROUTE) })
                    }
                }
                if (state.recentSearches.isNotEmpty()) {
                    item(key = "recent") {
                        Column {
                            Text("Ostatnio szukane", style = ForgeText.shelfTitle.copy(color = ForgeColors.Text2), modifier = Modifier.padding(start = ForgeDims.ContentStart).testTag("szuk-etykieta.0")) // v7.4 (09)
                            Spacer(Modifier.height(4.dp)) // v7.4: pills at y 186
                            LazyRow(
                                // UX: ◀▶ stay in the row (→ on the last pill used to drop to the row below), ▲ goes to the field.
                                modifier = Modifier
                                    .focusProperties {
                                        @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                                        exit = { dir ->
                                            when (dir) {
                                                androidx.compose.ui.focus.FocusDirection.Left, androidx.compose.ui.focus.FocusDirection.Right -> FocusRequester.Cancel
                                                androidx.compose.ui.focus.FocusDirection.Up -> fieldFocus
                                                else -> FocusRequester.Default
                                            }
                                        }
                                    }
                                    .focusGroup(),
                                contentPadding = PaddingValues(horizontal = ForgeDims.ContentStart, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(state.recentSearches, key = { it }) { recent ->
                                    ForgePillSelect(
                                        text = recent,
                                        selected = false,
                                        bare = true, // v7.4: pigułka bez tła i bez ikony
                                        onLongOk = { viewModel.onEvent(SearchEvent.RemoveRecentSearch(recent)) },
                                        onOk = {
                                            viewModel.onEvent(SearchEvent.QueryChanged(recent))
                                            viewModel.onEvent(SearchEvent.SubmitSearch)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                when {
                    results.isNotEmpty() -> item(key = "results") {
                        Column {
                            Text("Filmy i seriale", style = ForgeText.shelfTitle.copy(color = ForgeColors.Text2), modifier = Modifier.padding(start = ForgeDims.ContentStart).testTag("polka-tytul.0")) // v7.4 (09)
                            Spacer(Modifier.height(ForgeDims.ShelfHeaderToCards))
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = ForgeDims.ContentStart, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(ForgeDims.Gap),
                            ) {
                                itemsIndexed(results, key = { _, it -> it.first.id }) { i, (meta, baseUrl) ->
                                    SearchPoster(meta, tag = "karta.$i") { onNavigateToDetail(meta.id, meta.apiType, baseUrl) } // v7.4 (09)
                                }
                            }
                        }
                    }
                    state.isSearching -> item(key = "searching") {
                        Text("Szukam…", style = ForgeText.body, modifier = Modifier.padding(start = ForgeDims.ContentStart))
                    }
                    state.submittedQuery.isNotBlank() -> item(key = "none") {
                        Text("Nic nie znaleziono dla „${state.submittedQuery}”", style = ForgeText.head, modifier = Modifier.padding(start = ForgeDims.ContentStart))
                    }
                }
                // Fork 19.11: półki „Telewizja” i „YouTube” (TODO scalanie wyżej), fork/discover/SearchExtras.
                item(key = "fork_extras") { com.nuvio.tv.fork.discover.SearchExtras(state.submittedQuery.ifBlank { query }, Modifier.padding(start = ForgeDims.ContentStart)) }
            }

            // Odkrywaj: always underneath (unless switched off in Nuvio's settings).
            if (discoverOn) item(key = "discover") {
                Column(Modifier.padding(top = 4.dp)) { // v7.4 (09): pigułki filtrów y 246, „Odkrywaj” y 306; the filter pills first, then the header "Odkrywaj" (24 / 24 / 24 apart)
                    Row(
                        Modifier.padding(horizontal = ForgeDims.ContentStart),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ForgePillSelect(text = "Typ: " + forgeTypeLabel(state.selectedDiscoverType), bare = true, selected = false, onOk = { sheet = DiscoverSheet.Type })
                        ForgePillSelect(text = "Katalog: " + (selectedCatalog?.catalogName ?: "—"), bare = true, selected = false, onOk = { sheet = DiscoverSheet.Catalog })
                        ForgePillSelect(text = "Gatunek: " + (state.selectedDiscoverGenre ?: "wszystkie"), bare = true, selected = false, onOk = { sheet = DiscoverSheet.Genre })
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Odkrywaj", style = ForgeText.shelfTitle.copy(color = ForgeColors.Text2), modifier = Modifier.padding(start = ForgeDims.ContentStart).testTag(if (results.isNotEmpty() && query.isNotBlank()) "polka-tytul.1" else "polka-tytul.0")) // v7.4 (09)
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (state.discoverLoading && discoverRows.isEmpty()) {
                item(key = "discover_loading") {
                    Text("Wczytuję…", style = ForgeText.body, modifier = Modifier.padding(start = ForgeDims.ContentStart))
                }
            }
            val cardsBefore = if (query.isNotBlank()) results.size else 0 // v7.4 (09): karta.N continues after the results
            itemsIndexed(discoverRows, key = { i, row -> "d_${i}_${row.firstOrNull()?.id}" }) { rowIndex, row ->
                Row(
                    Modifier.padding(horizontal = ForgeDims.ContentStart).padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(ForgeDims.Gap),
                ) {
                    row.forEachIndexed { col, meta -> SearchPoster(meta, tag = "karta.${cardsBefore + rowIndex * 6 + col}") { onNavigateToDetail(meta.id, meta.apiType, discoverBaseUrl) } }
                }
            }
        }

        // On-screen keyboard (KBD): bottom, 22 dp above the edge, x 16-944.
        run { // v6 (mockup S02): the board stands on the left from the start; OK on the field still jumps into it
            if (keyboardOpen) BackHandler { keyboardOpen = false; if (query.isNotBlank()) viewModel.onEvent(SearchEvent.SubmitSearch); runCatching { fieldFocus.requestFocus() } }
            ForgeOnScreenKeyboard(
                text = query,
                onTextChange = { viewModel.onEvent(SearchEvent.QueryChanged(it)) },
                onSearch = {
                    viewModel.onEvent(SearchEvent.SubmitSearch)
                    keyboardOpen = false
                    runCatching { fieldFocus.requestFocus() }
                },
                firstKeyFocus = firstKey,
                layout = com.nuvio.tv.skin.kicia.keyboard.ForgeKeyboardLayout.Search6,
                // Mockup S02: the board is a panel 280 dp wide from y 96 to the bottom edge, its keys start at x 40, y 150, on the screen's own background.
                modifier = Modifier.align(Alignment.TopStart).padding(top = 28.dp).fillMaxHeight().width(280.dp).background(ForgeColors.Bg).padding(start = 40.dp), // v7.4: tabs abc/123 at y 96, keys from y 144
            )
        }

        // Discover filter panels.
        val open = sheet
        ForgeSideSheet(
            visible = open != null,
            onDismiss = { sheet = null },
            title = when (open) {
                DiscoverSheet.Type -> "Typ"
                DiscoverSheet.Catalog -> "Katalog"
                DiscoverSheet.Genre -> "Gatunek"
                null -> ""
            },
        ) {
            when (open) {
                DiscoverSheet.Type -> state.discoverCatalogs.map { it.type }.distinct().forEach { type ->
                    ForgeSheetRow(text = forgeTypeLabel(type), detail = if (type == state.selectedDiscoverType) "✓" else null) {
                        viewModel.onEvent(SearchEvent.SelectDiscoverType(type)); sheet = null
                    }
                }
                DiscoverSheet.Catalog -> state.discoverCatalogs.filter { it.type == state.selectedDiscoverType }.forEach { catalog ->
                    ForgeSheetRow(text = catalog.catalogName, detail = catalog.addonName) {
                        viewModel.onEvent(SearchEvent.SelectDiscoverCatalog(catalog.key)); sheet = null
                    }
                }
                DiscoverSheet.Genre -> {
                    ForgeSheetRow(text = "Wszystkie", detail = if (state.selectedDiscoverGenre == null) "✓" else null) {
                        viewModel.onEvent(SearchEvent.SelectDiscoverGenre(null)); sheet = null
                    }
                    selectedCatalog?.genres.orEmpty().forEach { genre ->
                        ForgeSheetRow(text = genre, detail = if (genre == state.selectedDiscoverGenre) "✓" else null) {
                            viewModel.onEvent(SearchEvent.SelectDiscoverGenre(genre)); sheet = null
                        }
                    }
                }
                null -> Unit
            }
        }
    }
}

private enum class DiscoverSheet { Type, Catalog, Genre }

@Composable
private fun SearchPoster(meta: MetaPreview, tag: String = "", onOk: () -> Unit) {
    ForgePosterCard(onOk = onOk, modifier = if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier) {
        Text(
            meta.name,
            style = ForgeText.label,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center).padding(12.dp),
        )
        com.nuvio.tv.skin.kicia.components.ForgePosterImage(meta.poster ?: meta.background, meta.rawPosterUrl, meta.name)
    }
}

/** "movie" / "series" / anything else as the mockup words it. */
internal fun forgeTypeLabel(type: String): String = when (type.lowercase()) {
    "movie" -> "Filmy"
    "series" -> "Seriale"
    "tv", "channel" -> "Telewizja"
    else -> type.replaceFirstChar { it.uppercase() }
}

/**
 * One "Filmy i seriale" shelf from the per-addon result rows: rows interleaved (first of each, then second of
 * each…) so every addon's best hits come first, each title once.
 */
internal fun <T> forgeMergeResults(rows: List<List<Pair<T, String>>>, idOf: (T) -> String): List<Pair<T, String>> {
    val seen = HashSet<String>()
    val out = ArrayList<Pair<T, String>>()
    val longest = rows.maxOfOrNull { it.size } ?: 0
    for (i in 0 until longest) for (row in rows) {
        val item = row.getOrNull(i) ?: continue
        if (seen.add(idOf(item.first))) out += item
    }
    return out
}

internal fun forgeMergeResults(rows: List<List<Pair<MetaPreview, String>>>): List<Pair<MetaPreview, String>> =
    forgeMergeResults(rows) { it.id }
