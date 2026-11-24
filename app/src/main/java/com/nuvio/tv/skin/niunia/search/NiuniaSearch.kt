package com.nuvio.tv.skin.niunia.search

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
    val restoreId = remember { com.nuvio.tv.skin.niunia.components.NiuniaFocusKeys.searchReturn(NiuniaSearchMemory.lastId, NiuniaSearchMemory.query, viewModel.uiState.value.query) }
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
    // UX JMGO (ręczne testy 24.11): „dun” → „Muriel”, „Cztery noce marzeń” przed „Dune” (kolejność dodatków). Teraz najpierw
    // tytuły równe wpisanemu tekstowi, potem zaczynające się od niego, potem ze słowem od niego; w grupie kolejność dodatków.
    val results: List<Pair<MetaPreview, String>> = remember(state.catalogRows, state.submittedQuery) {
        // Ten sam film z dwóch dodatków (inne id) był dwa razy („Dunkirk” · 2017) — jeden wpis na tytuł, rok i rodzaj.
        val all = state.catalogRows.flatMap { row -> row.items.map { it to row.addonBaseUrl } }.distinctBy { it.first.id }
            .distinctBy { (m, _) -> m.releaseInfo?.take(4)?.takeIf { it.isNotBlank() }?.let { y -> niuniaSearchNorm(m.name) + "|" + y + "|" + m.rawType } ?: m.id }
        val q = niuniaSearchNorm(state.submittedQuery)
        if (q.isBlank()) all else all.sortedBy { niuniaSearchRank(niuniaSearchNorm(it.first.name), q) }
    }
    LaunchedEffect(results.size, state.submittedQuery) {
        if (state.submittedQuery.isNotBlank()) Log.i(TAG, "wyszukiwarka: „${state.submittedQuery}” → ${results.size} wyników")
    }
    val searching = state.query.isNotBlank()
    // Z-19 (P-19): wynik nigdy pusty — przy 0 wynikach zapytanie poprawiane krok po kroku (NiuniaSearchFix).
    var fix by remember { mutableStateOf<NiuniaFixActive?>(null) }
    var exact by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.discoverResults, state.recentSearches, state.suggestions, results.size) {
        NiuniaKnownTitles.add(state.discoverResults.map { it.name } + state.recentSearches + state.suggestions + results.map { it.first.name })
    }
    LaunchedEffect(state.query) {
        if (fix != null && state.query != fix?.corrected) fix = null
        if (exact != null && state.query != exact) exact = null
    }
    LaunchedEffect(state.submittedQuery, state.isSearching, results.isEmpty(), state.error) {
        val q = state.submittedQuery
        if (state.isSearching || q.isBlank() || state.query != q) return@LaunchedEffect
        val f = fix
        if (results.isNotEmpty()) {
            if (f != null && f.corrected == q && !f.done) { Log.i(TAG, "Search: corrected \"${f.original}\" -> \"$q\" (krok ${f.step}): ${results.size} wyników"); fix = f.copy(done = true) }
            return@LaunchedEffect
        }
        if (state.error != null) { Log.w(TAG, "Search: „$q” — błąd wyszukiwania (${state.error}), bez poprawiania"); return@LaunchedEffect }
        if (exact == q) return@LaunchedEffect
        val chain = f?.takeIf { it.corrected == q }
        val original = chain?.original ?: q
        if (chain == null) { if (q.trim().length < 3) return@LaunchedEffect; kotlinx.coroutines.delay(1500) } // pauza w pisaniu
        fun key(x: String) = x.trim().lowercase()
        val tried = (chain?.tried ?: emptySet()) + key(q)
        // Liczenie odległości do kilku tysięcy tytułów poza wątkiem UI.
        val titles = NiuniaKnownTitles.all()
        val local = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { NiuniaSearchFix.localSteps(original, titles) }.filter { key(it.query) !in tried }
        var next = local.firstOrNull { it.step < 4 }
        var tmdbDone = chain?.tmdbDone ?: false
        if (next == null && !tmdbDone) {
            tmdbDone = true
            val base = chain?.takeIf { it.step in 2..3 }?.corrected ?: original
            NiuniaSearchFix.tmdbTitle(ctx, base)?.takeIf { key(it) !in tried }?.let { next = NiuniaFixStep(4, it) }
        }
        if (next == null) next = local.firstOrNull { it.step == 5 }
        val n = next
        if (n == null) { Log.i(TAG, "Search: „$original” — 0 wyników, poprawki wyczerpane (${tried.size} prób)"); return@LaunchedEffect }
        Log.i(TAG, "Search: corrected \"$original\" -> \"${n.query}\" (krok ${n.step}), próba ${tried.size + 1}")
        fix = NiuniaFixActive(original, n.query, n.step, tried + key(n.query), tmdbDone)
        viewModel.onEvent(SearchEvent.QueryChanged(n.query))
        viewModel.onEvent(SearchEvent.SubmitSearch)
    }
    // Pole pokazuje to, co wpisano; wyniki są dla poprawionego zapytania (nagłówek „Wyniki dla …”).
    val shownQuery = fix?.takeIf { state.query == it.corrected }?.original ?: state.query

    Box(Modifier.fillMaxSize().background(NiuniaColor.Bg)) {
        Column(Modifier.fillMaxSize().padding(start = NiuniaSpacing.Space40, end = NiuniaSpacing.Space40, top = NiuniaSize.NavTop + NiuniaSize.H44)) {
            SearchField(shownQuery, ctx.getString(R.string.niunia_ds_search_placeholder))
            Row(Modifier.padding(top = NiuniaSpacing.Space12).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space40)) {
                // Lewa kolumna: klawiatura + lista fraz.
                // UX JMGO (ręczne testy 24.11): ◄ z pierwszej kolumny klawiatury stał w miejscu — teraz otwiera menu jak na innych ekranach.
                val fm = androidx.compose.ui.platform.LocalFocusManager.current
                Column(Modifier.width(NiuniaSize.SplitCol).onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown || e.key != Key.DirectionLeft) false
                    else fm.moveFocus(androidx.compose.ui.focus.FocusDirection.Left) || com.nuvio.tv.skin.niunia.nav.NiuniaNavActions.toMenu("wyszukiwarka ◄ z klawiatury")
                }, verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
                    Keyboard(
                        digits = digits, firstKey = firstKey,
                        onMode = { digits = it; Log.i(TAG, "wyszukiwarka: klawiatura ${if (it) "123" else "abc"}") },
                        onKey = { k -> type(shownQuery + k) },
                        onSpace = { type(shownQuery + " ") },
                        onDelete = { type(shownQuery.dropLast(1)) },
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
                    if (!searching) {
                        // Z-19 (P-18): chip „Odkrywaj: gatunek, rok, katalog” nad rzędami → ekran Odkrywaj.
                        NiuniaButton(onClick = {
                            Log.i(TAG, "wyszukiwarka: Odkrywaj")
                            // UX JMGO 24.11 (F5, pętla 0 ścieżka 19): Wstecz z Odkrywaj wraca na ten chip, nie na klawisz „a”.
                            NiuniaSearchMemory.lastId = com.nuvio.tv.skin.niunia.components.NiuniaFocusKeys.OPENER_DISCOVER; NiuniaSearchMemory.query = state.query
                            val go = com.nuvio.tv.ui.screens.settings.ForkFeatures.open
                            if (go != null) go(com.nuvio.tv.ui.navigation.Screen.Discover.route) else Log.w(TAG, "wyszukiwarka: Odkrywaj: brak nawigatora")
                        }, label = stringResource(R.string.niunia_ke2_discover_chip), icon = "discover", kind = NiuniaButtonKind.Chip,
                            focusRequester = if (restoreId == com.nuvio.tv.skin.niunia.components.NiuniaFocusKeys.OPENER_DISCOVER) restore else null)
                        Browse(state, ctx, viewModel, open, posterOptions, focusFor)
                    }
                    else Results(state.isSearching || (fix != null && fix?.done != true && results.isEmpty()), state.submittedQuery, results, tab, onTab = { tab = it; Log.i(TAG, "wyszukiwarka: zakładka $it") }, open, posterOptions, focusFor,
                        corrected = fix?.takeIf { it.corrected == state.submittedQuery && results.isNotEmpty() }?.let { it.corrected to it.original },
                        offline = state.error != null && results.isEmpty(),
                        onExact = { orig -> Log.i(TAG, "wyszukiwarka: szukaj dokładnie „$orig”"); exact = orig; fix = null; type(orig); viewModel.onEvent(SearchEvent.SubmitSearch) },
                        onRetry = { Log.i(TAG, "wyszukiwarka: spróbuj ponownie"); viewModel.onEvent(SearchEvent.SubmitSearch) })
                }
            }
        }
    }
}

/** Z-19: trwające poprawianie — wpisane [original], wysłane [corrected] (krok [step]), wypróbowane zapytania (bez polskich znaków). */
internal data class NiuniaFixActive(val original: String, val corrected: String, val step: Int, val tried: Set<String>, val tmdbDone: Boolean, val done: Boolean = false)

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
        Row(with(com.nuvio.tv.fork.glass.ForkGlass) { Modifier.width(NiuniaSize.SplitCol).forkGlass(com.nuvio.tv.fork.glass.ForkGlass.Role.Chip, CircleShape, fallback = NiuniaColor.Fill12) }.padding(NiuniaSpacing.Space4)) {
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
    corrected: Pair<String, String>? = null,
    offline: Boolean = false,
    onExact: (String) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    // Z-19: „Szukaj dokładnie »wpisane«” nad zakładkami (▲ z zakładek).
    if (corrected != null) NiuniaButton(onClick = { onExact(corrected.second) }, label = stringResource(R.string.niunia_ke2_search_exact, corrected.second), icon = "search", kind = NiuniaButtonKind.Chip)
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
    if (corrected != null) NiuniaText(stringResource(R.string.niunia_ke2_results_for, corrected.first), variant = NiuniaTextVariant.TitleSection, lines = 1)
    when {
        offline -> Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8)) {
            NiuniaText(stringResource(R.string.niunia_ke2_search_offline), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 2)
            NiuniaButton(onClick = onRetry, label = stringResource(R.string.niunia_ke2_retry), icon = "restart", compact = true)
        }
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

/** Tekst do porównań w Szukaj: małe litery, bez polskich znaków i akcentów, pojedyncze spacje. */
internal fun niuniaSearchNorm(t: String): String =
    java.text.Normalizer.normalize(t.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        .replace('ł', 'l').replace(Regex("[^a-z0-9]+"), " ").trim()

/** 0 = równy, 1 = zaczyna się od, 2 = słowo zaczyna się od, 3 = zawiera, 4 = reszta (dopasowanie dodatku po opisie itp.). */
internal fun niuniaSearchRank(name: String, q: String): Int = when {
    name == q -> 0
    name.startsWith(q) -> 1
    name.split(' ').any { it.startsWith(q) } -> 2
    name.contains(q) -> 3
    else -> 4
}
