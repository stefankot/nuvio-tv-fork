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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.skin.niunia.NiuniaLabels
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaButtonKind
import com.nuvio.tv.skin.niunia.components.NiuniaCardPreset
import com.nuvio.tv.skin.niunia.components.NiuniaCollection
import com.nuvio.tv.skin.niunia.components.NiuniaCollectionHeader
import com.nuvio.tv.skin.niunia.components.NiuniaCollectionKind
import com.nuvio.tv.skin.niunia.components.NiuniaIcon
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaMediaCard
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.components.NiuniaResultCard
import com.nuvio.tv.skin.niunia.components.NiuniaResultThumb
import com.nuvio.tv.skin.niunia.components.NiuniaRowKind
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.components.niuniaTrapArrows
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType
import com.nuvio.tv.ui.screens.search.SearchEvent
import com.nuvio.tv.ui.screens.search.SearchViewModel

private const val TAG = "ForkNiunia"

/** Zakładki wyników (decyzja 20.11, makieta SearchResults). */
internal enum class NiuniaSearchTab { All, Movies, Series, Tv, YouTube, People }

/** Czy wynik pasuje do zakładki (czysta funkcja, test w NiuniaDsTest). */
internal fun niuniaSearchTabMatches(tab: NiuniaSearchTab, rawType: String): Boolean = when (tab) {
    NiuniaSearchTab.All -> true
    NiuniaSearchTab.Movies -> rawType == "movie"
    NiuniaSearchTab.Series -> rawType == "series" || rawType == "anime"
    NiuniaSearchTab.Tv -> rawType == "tv" || rawType == "channel"
    NiuniaSearchTab.YouTube -> rawType.startsWith("youtube")
    NiuniaSearchTab.People -> rawType == "person"
}

/** Podpowiedzi pod klawiaturą: do 3 fraz z podpowiedzi wyszukiwarki, a gdy ich brak — z tytułów wyników zawierających frazę. */
internal fun niuniaSearchSuggestions(query: String, fromEngine: List<String>, titles: List<String>): List<String> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val fromTitles = titles.map { it.lowercase() }.filter { it.contains(q) && it != q }
    return (fromEngine.map { it.lowercase() } + fromTitles).distinct().take(3)
}

/**
 * Wyszukiwarka Niuni wg makiet `Search` i `SearchResults` (NScreen split: lewa kolumna 216 dp, prawa od x 296):
 * u góry pole (32/38, ikona lupy, kursor), z lewej klawiatura design systemu (abc/123, klawisze 36 dp po 6, SPACJA ⌫ WYCZYŚĆ)
 * i lista „Ostatnio wyszukiwane” / „Podpowiedzi”; z prawej bez frazy: „Przeglądaj” (Filmy, Seriale, Live TV, gatunki) i „Polecane”;
 * z frazą: zakładki Wszystko · Filmy · Seriale · TV · YouTube · Osoby i siatka 2 kolumn kart wyników. Dane: [SearchViewModel].
 */
@Composable
fun NiuniaSearch(viewModel: SearchViewModel, onNavigateToDetail: (String, String, String) -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val ctx = LocalContext.current
    var digits by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(NiuniaSearchTab.All) }
    val firstKey = remember { FocusRequester() }
    fun type(q: String) { viewModel.onEvent(SearchEvent.QueryChanged(q)) }
    // UX 22.11: długie OK na plakacie/wyniku = menu opcji (szczegóły, biblioteka, obejrzane).
    val posterOptions = com.nuvio.tv.skin.niunia.components.rememberNiuniaPosterOptions(onNavigateToDetail)
    // UX 22.11 (test emulatora): po Wstecz ze strony tytułu fokus wracał na klawisz „a” — teraz na otwarty wynik/plakat.
    val restore = remember { FocusRequester() }
    val restoreId = remember { NiuniaSearchMemory.lastId?.takeIf { NiuniaSearchMemory.query == viewModel.uiState.value.query } }
    com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(if (restoreId != null) restore else firstKey)
    LaunchedEffect(Unit) {
        viewModel.ensureDiscoverLoaded() // „Przeglądaj” (gatunki) i „Polecane” — jak ekran odkrywania Nuvio (test JMGO build147: puste)
        if (restoreId != null) {
            kotlinx.coroutines.delay(120)
            try { restore.requestFocus(); Log.i(TAG, "wyszukiwarka: powrót — fokus na $restoreId"); return@LaunchedEffect }
            catch (e: IllegalStateException) { Log.w(TAG, "wyszukiwarka: powrót na $restoreId: ${e.javaClass.simpleName}: ${e.message}") }
        }
        Log.i(TAG, "wyszukiwarka: fokus startowy → klawisz a")
        try { firstKey.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "wyszukiwarka: fokus klawiatury: ${e.javaClass.simpleName}: ${e.message}") }
    }
    val open: (String, String, String) -> Unit = { id, type, addon -> NiuniaSearchMemory.lastId = id; NiuniaSearchMemory.query = state.query; onNavigateToDetail(id, type, addon) }
    val focusFor: (String) -> Modifier = { id -> if (id == restoreId) Modifier.focusRequester(restore) else Modifier }
    val results: List<Pair<MetaPreview, String>> = remember(state.catalogRows) {
        state.catalogRows.flatMap { row -> row.items.map { it to row.addonBaseUrl } }.distinctBy { it.first.id }
    }
    LaunchedEffect(results.size, state.submittedQuery) {
        if (state.submittedQuery.isNotBlank()) Log.i(TAG, "wyszukiwarka: „${state.submittedQuery}” → ${results.size} wyników")
    }
    val searching = state.query.isNotBlank()

    Box(Modifier.fillMaxSize().background(NiuniaColor.Bg)) {
        Column(Modifier.fillMaxSize().padding(start = NiuniaSpacing.Space40, end = NiuniaSpacing.Space40, top = NiuniaSize.NavTop + NiuniaSize.H44)) {
            SearchField(state.query, ctx.getString(R.string.niunia_ds_search_placeholder))
            Row(Modifier.padding(top = NiuniaSpacing.Space12).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space40)) {
                // Lewa kolumna: klawiatura + lista fraz.
                Column(Modifier.width(NiuniaSize.SplitCol), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
                    Keyboard(
                        digits = digits, firstKey = firstKey,
                        onMode = { digits = it; Log.i(TAG, "wyszukiwarka: klawiatura ${if (it) "123" else "abc"}") },
                        onKey = { k -> type(state.query + k) },
                        onSpace = { type(state.query + " ") },
                        onDelete = { type(state.query.dropLast(1)) },
                        onClear = { Log.i(TAG, "wyszukiwarka: wyczyść"); type(""); tab = NiuniaSearchTab.All },
                    )
                    val phrases = if (searching) niuniaSearchSuggestions(state.query, state.suggestions, results.map { it.first.name }) else state.recentSearches.take(3)
                    if (phrases.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4)) {
                        NiuniaText(stringResource(if (searching) R.string.niunia_ds_search_suggestions else R.string.niunia_search_recent),
                            Modifier.padding(horizontal = NiuniaSpacing.Space8), variant = NiuniaTextVariant.CaptionRegular, tone = NiuniaTone.Tertiary)
                        phrases.forEach { ph ->
                            NiuniaListRow(ph, onClick = { Log.i(TAG, "wyszukiwarka: fraza „$ph”"); type(ph); viewModel.onEvent(SearchEvent.SubmitSearch) },
                                kind = NiuniaRowKind.Suggestion, icon = if (searching) "search" else "history")
                        }
                    }
                }
                // Prawa kolumna.
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space24)) {
                    if (!searching) Browse(state, ctx, viewModel, open, posterOptions, focusFor)
                    else Results(state.isSearching, state.submittedQuery, results, tab, onTab = { tab = it; Log.i(TAG, "wyszukiwarka: zakładka $it") }, open, posterOptions, focusFor)
                }
            }
        }
    }
}

/** Ostatnio otwarty wynik/plakat wyszukiwarki (powrót fokusu po Wstecz; tylko w pamięci procesu). */
internal object NiuniaSearchMemory {
    var lastId: String? = null
    var query: String = ""
}

/** NInput search: lupa 28, tekst 32/38 (podpowiedź w 50%), kursor 2 dp, linia 1 dp u dołu. */
@Composable
private fun SearchField(query: String, placeholder: String) {
    Column {
        Row(Modifier.height(NiuniaSize.H44), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
            NiuniaIcon("search", size = NiuniaSize.Icon28, tint = NiuniaColor.Text70)
            if (query.isBlank()) Text(placeholder, style = NiuniaType.TitleScreen, color = NiuniaColor.Text50, maxLines = 1, overflow = TextOverflow.Ellipsis)
            else {
                Text(query, style = NiuniaType.TitleScreen, color = NiuniaColor.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Box(Modifier.width(NiuniaSize.CaretW).height(NiuniaSize.Icon28).background(NiuniaColor.White))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(NiuniaColor.Line))
    }
}

/** NCollection search-keyboard: przełącznik abc/123 (segmenty), klawisze 36×36 po 6 w rzędzie (odstęp 4), SPACJA · ⌫ · WYCZYŚĆ. */
@Composable
internal fun Keyboard(digits: Boolean, firstKey: FocusRequester, onMode: (Boolean) -> Unit, onKey: (String) -> Unit, onSpace: () -> Unit, onDelete: () -> Unit, onClear: () -> Unit) {
    val keys = (if (digits) "1234567890" else "abcdefghijklmnopqrstuvwxyz").map { it.toString() }
    Column(Modifier.niuniaTrapArrows("klawiatura", setOf(androidx.compose.ui.focus.FocusDirection.Left, androidx.compose.ui.focus.FocusDirection.Right)),
        verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
        Row(Modifier.width(NiuniaSize.SplitCol).clip(CircleShape).background(NiuniaColor.Fill12).padding(NiuniaSpacing.Space4)) {
            NiuniaButton(onClick = { onMode(false) }, label = stringResource(R.string.niunia_ds_search_kbd_letters), kind = NiuniaButtonKind.Segment, selected = !digits, modifier = Modifier.weight(1f))
            NiuniaButton(onClick = { onMode(true) }, label = stringResource(R.string.niunia_ds_search_kbd_digits), kind = NiuniaButtonKind.Segment, selected = digits, modifier = Modifier.weight(1f))
            // Paczka „oczywiste” pkt 12: wejście do sterowania głosem (moduł fork/voice; wyłączony moduł = brak przycisku).
            val ctx = androidx.compose.ui.platform.LocalContext.current
            if (com.nuvio.tv.fork.voice.VoiceEntry.visible(ctx)) NiuniaButton(onClick = { com.nuvio.tv.fork.voice.VoiceEntry.start(ctx, "wyszukiwarka") },
                label = stringResource(R.string.voice_button), kind = NiuniaButtonKind.Segment, selected = false,
                description = stringResource(R.string.voice_button_description), modifier = Modifier.weight(1f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(NiuniaSize.KeyboardGap)) {
            keys.chunked(6).forEachIndexed { r, line ->
                Row {
                    line.forEachIndexed { c, k ->
                        NiuniaButton(onClick = { onKey(k) }, label = k, kind = NiuniaButtonKind.Key, description = k, focusRequester = if (r == 0 && c == 0) firstKey else null)
                    }
                }
            }
        }
        Row(Modifier.width(NiuniaSize.SplitCol), horizontalArrangement = Arrangement.SpaceBetween) {
            NiuniaButton(onClick = onSpace, label = stringResource(R.string.niunia_search_space), kind = NiuniaButtonKind.KeyWide)
            NiuniaButton(onClick = onDelete, icon = "backspace", kind = NiuniaButtonKind.KeyWide, description = stringResource(R.string.niunia_ds_search_delete))
            NiuniaButton(onClick = onClear, label = stringResource(R.string.niunia_search_clear), kind = NiuniaButtonKind.KeyWide)
        }
    }
}

/** Bez frazy: „Przeglądaj” (karty-etykiety: Filmy, Seriale, Live TV, gatunki odkrywania) i „Polecane” (plakaty z odkrywania). */
@Composable
private fun Browse(state: com.nuvio.tv.ui.screens.search.SearchUiState, ctx: android.content.Context, viewModel: SearchViewModel, onNavigateToDetail: (String, String, String) -> Unit, niuniaSearchPosterOptions: (MetaPreview, String?) -> Unit, focusFor: (String) -> Modifier) {
    var active by remember { mutableIntStateOf(-1) }
    data class Tile(val label: String, val action: () -> Unit)
    // Test JMGO build151: wybrany katalog bywa bez gatunków → gatunki z pierwszego katalogu tego typu (lub dowolnego), który je ma.
    val selected = state.discoverCatalogs.firstOrNull { it.key == state.selectedDiscoverCatalogKey }
    val genreCatalog = selected?.takeIf { it.genres.isNotEmpty() }
        ?: state.discoverCatalogs.firstOrNull { it.genres.isNotEmpty() && it.type == selected?.type }
        ?: state.discoverCatalogs.firstOrNull { it.genres.isNotEmpty() }
    val genres = genreCatalog?.genres.orEmpty().take(6)
    val tiles = listOf(
        Tile(ctx.getString(R.string.niunia_ds_search_tab_movies)) { viewModel.onEvent(SearchEvent.SelectDiscoverType("movie")) },
        Tile(ctx.getString(R.string.niunia_ds_search_tab_series)) { viewModel.onEvent(SearchEvent.SelectDiscoverType("series")) },
        Tile("Live TV") { com.nuvio.tv.skin.niunia.nav.NiuniaNavActions.open(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE) },
    ) + genres.map { g -> Tile(com.nuvio.tv.skin.niunia.components.NiuniaGenres.pl(g) ?: g) {
        if (genreCatalog != null && genreCatalog.key != state.selectedDiscoverCatalogKey) viewModel.onEvent(SearchEvent.SelectDiscoverCatalog(genreCatalog.key))
        viewModel.onEvent(SearchEvent.SelectDiscoverGenre(g))
    } }
    val shownTiles = tiles.distinctBy { it.label } // klucze LazyRow muszą być unikalne (gatunek „Filmy” itp.)
    Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
        NiuniaCollectionHeader(stringResource(R.string.niunia_search_browse), active = active == 0)
        LazyRow(Modifier.niuniaTrapArrows("przeglądaj"), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20), contentPadding = PaddingValues(end = NiuniaSpacing.Space40)) {
            items(shownTiles, key = { it.label }) { t ->
                NiuniaMediaCard(NiuniaCardPreset.PosterCard, image = null, label = t.label, title = t.label,
                    onFocus = { if (it) active = 0 }, onClick = { Log.i(TAG, "wyszukiwarka: przeglądaj → ${t.label}"); t.action() })
            }
        }
    }
    if (state.discoverResults.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
        NiuniaCollectionHeader(stringResource(R.string.niunia_ds_search_recommended), active = active == 1)
        LazyRow(Modifier.niuniaTrapArrows("polecane"), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20), contentPadding = PaddingValues(end = NiuniaSpacing.Space40)) {
            items(state.discoverResults.take(24), key = { it.id }) { item ->
                NiuniaPoster(art = com.nuvio.tv.skin.niunia.components.niuniaRealPoster(item).first, fallback = com.nuvio.tv.skin.niunia.components.niuniaRealPoster(item).second, title = item.name, logo = item.logo, meta = NiuniaLabels.meta(item), label = null, baked = item.poster != null,
                    modifier = focusFor(item.id),
                    onFocus = { if (it) active = 1 },
                    onOk = { Log.i(TAG, "wyszukiwarka: polecane → ${item.id}"); onNavigateToDetail(item.id, item.rawType, "") },
                    onLongOk = { niuniaSearchPosterOptions(item, "") })
            }
        }
    }
}

/** Z frazą: zakładki w pigułce, siatka 2 kolumn kart wyników (280×80, odstęp 20 / 16). */
@Composable
private fun Results(
    searchingNow: Boolean, submitted: String, results: List<Pair<MetaPreview, String>>,
    tab: NiuniaSearchTab, onTab: (NiuniaSearchTab) -> Unit, onNavigateToDetail: (String, String, String) -> Unit,
    niuniaSearchPosterOptions: (MetaPreview, String?) -> Unit,
    focusFor: (String) -> Modifier,
) {
    val labels = listOf(
        NiuniaSearchTab.All to R.string.niunia_ds_search_tab_all, NiuniaSearchTab.Movies to R.string.niunia_ds_search_tab_movies,
        NiuniaSearchTab.Series to R.string.niunia_ds_search_tab_series, NiuniaSearchTab.Tv to R.string.niunia_ds_search_tab_tv,
        NiuniaSearchTab.YouTube to R.string.niunia_ds_search_tab_youtube, NiuniaSearchTab.People to R.string.niunia_ds_search_tab_people,
    )
    NiuniaCollection(NiuniaCollectionKind.Tabs, Modifier.niuniaTrapArrows("zakładki")) {
        labels.forEach { (t, res) -> NiuniaButton(onClick = { onTab(t) }, label = stringResource(res), kind = NiuniaButtonKind.Tab, selected = t == tab) }
    }
    val shown = results.filter { niuniaSearchTabMatches(tab, it.first.rawType) }
    // Zakładka TV: programy i kanały z przewodnika szuka Live TV → Szukaj (ta sama fraza); tu karta przejścia.
    if (tab == NiuniaSearchTab.Tv && submitted.isNotBlank()) {
        NiuniaResultCard(
            title = stringResource(R.string.niunia_ds_search_tv_open, submitted), subtitle = stringResource(R.string.niunia_ds_search_tv_hint),
            image = null, thumb = NiuniaResultThumb.Wide,
            onClick = {
                Log.i(TAG, "wyszukiwarka: TV → Live TV, Szukaj „$submitted”")
                com.nuvio.tv.skin.kicia.livetv.ForgeTvSearchMemory.lastQuery = submitted
                com.nuvio.tv.skin.niunia.livetv.NiuniaLiveStart.openSearch = true
                if (!com.nuvio.tv.skin.niunia.nav.NiuniaNavActions.open(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE)) com.nuvio.tv.skin.niunia.livetv.NiuniaLiveStart.openSearch = false
            },
        )
    }
    when {
        searchingNow && results.isEmpty() -> NiuniaText(stringResource(R.string.niunia_search_searching), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary)
        results.isEmpty() && submitted.isNotBlank() -> NiuniaText(stringResource(R.string.niunia_search_none, submitted), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary)
        shown.isEmpty() && results.isNotEmpty() && tab != NiuniaSearchTab.Tv -> NiuniaText(stringResource(R.string.niunia_ds_search_tab_empty), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary)
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), modifier = Modifier.niuniaTrapArrows("wyniki"),
        contentPadding = PaddingValues(top = NiuniaSpacing.Space8, bottom = NiuniaSpacing.Space40),
        horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space16), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20),
    ) {
        items(shown, key = { it.first.id }) { (item, addon) ->
            NiuniaResultCard(
                modifier = focusFor(item.id),
                title = item.name,
                subtitle = niuniaResultSubtitle(item),
                image = item.poster,
                thumb = if (item.rawType == "tv" || item.rawType == "channel" || item.rawType.startsWith("youtube")) NiuniaResultThumb.Wide else NiuniaResultThumb.Poster,
                onClick = { Log.i(TAG, "wyszukiwarka: wynik → ${item.rawType} ${item.id}"); onNavigateToDetail(item.id, item.rawType, addon) },
                onLongClick = if (item.rawType == "movie" || item.rawType == "series") ({ niuniaSearchPosterOptions(item, addon) }) else null,
            )
        }
    }
}

/** „Film · Przygodowy · 2026” / „Serial · Fantasy · 2022”. */
internal fun niuniaResultSubtitle(item: MetaPreview): String = listOfNotNull(
    when (item.rawType) { "movie" -> "Film"; "series", "anime" -> "Serial"; "tv", "channel" -> "TV"; else -> null },
    com.nuvio.tv.skin.niunia.components.NiuniaGenres.pl(item.genres.firstOrNull()),
    item.releaseInfo?.takeIf { it.isNotBlank() }?.take(4),
).joinToString(" · ")

@Suppress("unused")
private fun highlight(label: String, q: String) = buildAnnotatedString {
    val i = label.lowercase().indexOf(q.lowercase())
    if (i < 0 || q.isBlank()) { append(label); return@buildAnnotatedString }
    append(label.substring(0, i)); withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = NiuniaColor.Text)) { append(label.substring(i, i + q.length)) }; append(label.substring(i + q.length))
}
