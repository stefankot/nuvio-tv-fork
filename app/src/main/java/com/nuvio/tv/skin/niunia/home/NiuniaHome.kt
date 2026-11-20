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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.focus.focusRequester
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
import com.nuvio.tv.skin.niunia.components.NiuniaBlockKind
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaButtonKind
import com.nuvio.tv.skin.niunia.components.NiuniaCardPreset
import com.nuvio.tv.skin.niunia.components.NiuniaCollection
import com.nuvio.tv.skin.niunia.components.NiuniaCollectionHeader
import com.nuvio.tv.skin.niunia.components.NiuniaCollectionKind
import com.nuvio.tv.skin.niunia.components.NiuniaContentBlock
import com.nuvio.tv.skin.niunia.components.NiuniaDots
import com.nuvio.tv.skin.niunia.components.NiuniaLogo
import com.nuvio.tv.skin.niunia.components.NiuniaLogoSize
import com.nuvio.tv.skin.niunia.components.NiuniaMediaCard
import com.nuvio.tv.skin.niunia.components.NiuniaMetaLine
import com.nuvio.tv.skin.niunia.components.NiuniaScreens
import com.nuvio.tv.skin.niunia.components.NiuniaSurface
import com.nuvio.tv.skin.niunia.components.NiuniaSurfaceKind
import com.nuvio.tv.skin.niunia.components.NiuniaTag
import com.nuvio.tv.skin.niunia.components.NiuniaTagKind
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import androidx.compose.foundation.layout.offset
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpace
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
    onToggleLibrary: (MetaPreview, String) -> Unit = { _, _ -> },
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
    // Powrót fokusu na element, z którego się wyszło (focus-and-keys.md pkt 5): po Wstecz ze strony tytułu i po wyjściu z menu.
    val restoreFocus = remember { FocusRequester() }
    val memory = NiuniaHomeMemory
    var memDown by remember { mutableStateOf(memory.down && memory.key != null) }
    com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(if (memDown) restoreFocus else if (heroes.isNotEmpty()) heroFocus else null)
    remember { memory.restorePending = memDown; 0 }
    fun rememberFocus(key: String, row: Int, index: Int) { memory.key = key; memory.row = row; memory.index = index; memory.down = true; memDown = true }
    val posterPattern = remember(uiState.customPosterUrlPattern, uiState.customPosterEnabledScreens) {
        com.nuvio.tv.core.poster.patternForScreen(uiState.customPosterUrlPattern, com.nuvio.tv.core.poster.CustomPosterScreen.HOME, uiState.customPosterEnabledScreens)
    }
    val hasHero = heroes.isNotEmpty()
    val hero = heroes.getOrNull(heroIndex % heroes.size.coerceAtLeast(1))
    LaunchedEffect(heroes.size, uiState.catalogRows.size) {
        Log.i(TAG, "ekran główny: hero=${heroes.size}, dalej oglądaj=${uiState.continueWatchingItems.size}, rzędy=${uiState.catalogRows.size}")
    }

    // Właściciel 20.11: hero poza listą (był przesunięty o ujemny margines), ▼ = animacja: hero wyjeżdża, tło rozmyte, rzędy wjeżdżają.
    val rows: @Composable () -> Unit = {
        com.nuvio.tv.skin.niunia.components.NiuniaVerticalScroll {
            val colState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = if (memory.down) memory.row.coerceAtLeast(0) else 0)
            LazyColumn(state = colState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 48.dp)) {
                var firstBelow = true // pierwszy rząd 40 dp od góry
                if (uiState.continueWatchingItems.isNotEmpty()) {
                    val top = firstBelow; firstBelow = false
                    item(key = "niunia_cw") {
                        RowTitle(stringResource(R.string.niunia_row_continue), on = focusedRow == -2, first = top)
                        com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                            val st = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = if (memory.down && memory.key?.startsWith("cw:") == true) memory.index else 0)
                            LazyRow(state = st, contentPadding = rowPadding(), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                                itemsIndexed(uiState.continueWatchingItems, key = { _, it -> cwKey(it) }) { i, cw ->
                                    val k = "cw:" + cwKey(cw)
                                    ContinueCard(cw, modifier = restoreModifier(k, restoreFocus), onFocus = { if (it) { focusedRow = -2; rememberFocus(k, 0, i) } },
                                        onOk = { Log.i(TAG, "rzędy: Oglądaj dalej → ${niuniaCwContentId(cw)}"); onContinueWatchingClick(cw) })
                                }
                            }
                        }
                    }
                }
                uiState.catalogRows.forEachIndexed { rowIndex, row ->
                    if (row.items.isEmpty()) return@forEachIndexed
                    val top = firstBelow; firstBelow = false
                    item(key = "niunia_row_${row.addonId}_${row.catalogId}_${row.rawType}") {
                        val ranking = NiuniaLabels.isRankingRow(row.catalogName)
                        RowTitle(row.catalogName, on = focusedRow == rowIndex, first = top)
                        com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                            val rowKey = "r:${row.addonId}_${row.catalogId}_${row.rawType}:"
                            val st = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = if (memory.down && memory.key?.startsWith(rowKey) == true) memory.index else 0)
                            val colIndex = rowIndex + if (uiState.continueWatchingItems.isNotEmpty()) 1 else 0
                            LazyRow(state = st, contentPadding = rowPadding(), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                                itemsIndexed(row.items, key = { _, it -> it.id }) { i, item ->
                                    val k = rowKey + item.id
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
                                        modifier = restoreModifier(k, restoreFocus),
                                        onFocus = { if (it) { focusedRow = rowIndex; onItemFocus(item); rememberFocus(k, colIndex, i) } },
                                        onOk = { Log.i(TAG, "rzędy: OK → strona tytułu ${item.id}"); onNavigateToDetail(item.id, item.rawType, row.addonBaseUrl) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (hero == null) {
        Box(Modifier.fillMaxSize().background(NiuniaColors.Bg)) { rows() }
        return
    }
    com.nuvio.tv.skin.niunia.components.NiuniaHeroPage(
        background = hero.background ?: hero.landscapePoster ?: hero.poster,
        heroFocus = heroFocus,
        what = "ekran główny",
        hero = {
            val addon = uiState.catalogRows.firstOrNull { r -> r.items.any { it.id == hero.id } }?.addonBaseUrl ?: uiState.catalogRows.firstOrNull()?.addonBaseUrl ?: ""
            val open = { Log.i(TAG, "ekran główny: hero → strona tytułu ${hero.id}"); onNavigateToDetail(hero.id, hero.rawType, addon) }
            Hero(
                item = hero, index = heroIndex, count = heroes.size, focus = heroFocus,
                inLibrary = uiState.posterLibraryMembership[com.nuvio.tv.ui.screens.home.homeItemStatusKey(hero.id, hero.apiType)] == true,
                peek = uiState.continueWatchingItems,
                takeFocus = !memDown,
                onHeroFocus = { memory.down = false; memDown = false },
                onPlay = {
                    // Odtwórz: tytuł z „Oglądaj dalej” wznawia się od razu; inny — strona tytułu z fokusem na Odtwórz (bez nowego wpięcia, plan/DECYZJE.md).
                    val cw = uiState.continueWatchingItems.firstOrNull { c -> niuniaCwContentId(c) == hero.id }
                    if (cw != null) { Log.i(TAG, "ekran główny: Odtwórz → wznów ${hero.id}"); onContinueWatchingClick(cw) } else open()
                },
                onOpen = open,
                onLibrary = { Log.i(TAG, "ekran główny: biblioteka ${hero.id}"); onToggleLibrary(hero, addon) },
                onNext = { heroIndex = (heroIndex + 1) % heroes.size; Log.i(TAG, "ekran główny: następny tytuł ${heroIndex + 1}/${heroes.size}") },
            )
        },
        below = { rows() },
    )
}

private fun cwKey(cw: ContinueWatchingItem): String = when (cw) {
    is ContinueWatchingItem.InProgress -> "p_${cw.progress.contentId}_${cw.progress.videoId}"
    is ContinueWatchingItem.NextUp -> "n_${cw.info.contentId}_${cw.info.videoId}"
}

/** Tytuł rzędu: 22 dp wysokości; rząd zaczyna się 34 dp od góry tytułu, po rzędzie 36 dp (decyzja właściciela). */
@Composable
private fun RowTitle(text: String, on: Boolean, first: Boolean = false) {
    NiuniaCollectionHeader(text, active = on, modifier = Modifier.padding(start = NiuniaSpacing.Space40, top = if (first) NiuniaSpace.TopAfterHero else 0.dp).height(22.dp))
}

private fun niuniaCwContentId(cw: ContinueWatchingItem): String = when (cw) {
    is ContinueWatchingItem.InProgress -> cw.progress.contentId
    is ContinueWatchingItem.NextUp -> cw.info.contentId
}

private fun rowPadding() = PaddingValues(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter, top = NiuniaSpace.TitleToRow - 22.dp, bottom = NiuniaSpace.AfterRow)

/**
 * Hero Głównej wg makiety `Main` (NHero immersive, wysokość planszy, `peek`): logo, linia meta ze znacznikiem oceny, opis 2 linie,
 * akcje (Odtwórz — fokus startowy, długie OK = strona tytułu; + biblioteka; i = szczegóły; › następny tytuł), kropki karuzeli,
 * u dołu wystający rząd „Oglądaj dalej” (56 dp pod krawędzią; ▼ przenosi do rzędów — stan `HomeRows`).
 * Tło (obraz + rozmycie po ▼) rysuje NiuniaHeroPage.
 */
@Composable
private fun Hero(
    item: MetaPreview,
    index: Int,
    count: Int,
    focus: FocusRequester,
    inLibrary: Boolean,
    peek: List<ContinueWatchingItem>,
    takeFocus: Boolean,
    onHeroFocus: () -> Unit,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
    onLibrary: () -> Unit,
    onNext: () -> Unit,
) {
    LaunchedEffect(Unit) {
        if (!takeFocus) return@LaunchedEffect
        Log.i(TAG, "ekran główny: fokus startowy → ${NiuniaScreens.Main.initial}")
        try { focus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "fokus hero: ${e.javaClass.simpleName}: ${e.message}") }
    }
    val ctx = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        NiuniaSurface(NiuniaSurfaceKind.ScrimHero, Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.BottomStart).offset(y = if (peek.isNotEmpty()) NiuniaSize.Peek else -NiuniaSpacing.Space40)
                .padding(horizontal = NiuniaSpacing.Space40),
            verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space16),
        ) {
            NiuniaContentBlock(NiuniaBlockKind.Info) {
                NiuniaLogo(item.logo, item.name, NiuniaLogoSize.Hero)
                val type = if (item.rawType == "series") ctx.getString(R.string.niunia_player_series) else ctx.getString(R.string.niunia_player_movie)
                val meta = listOfNotNull(type, item.genres.take(2).joinToString(" · ").ifBlank { null }, item.releaseInfo?.takeIf { it.isNotBlank() }, niuniaRuntime(item.runtime)?.let { (h, m) -> if (h > 0) ctx.getString(R.string.niunia_hours_minutes, h, m) else ctx.getString(R.string.niunia_minutes, m) } ?: item.runtime?.takeIf { it.isNotBlank() })
                    .joinToString(" · ")
                NiuniaMetaLine(meta, tone = NiuniaTone.Primary) {
                    item.imdbRating?.takeIf { it > 0f }?.let { NiuniaTag("IMDb %.1f".format(java.util.Locale.US, it), NiuniaTagKind.Quality) }
                    item.ageRating?.takeIf { it.isNotBlank() }?.let { NiuniaTag(it, NiuniaTagKind.Age) }
                }
                if (!item.description.isNullOrBlank()) NiuniaText(item.description, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 2)
            }
            NiuniaCollection(NiuniaCollectionKind.Actions) {
                NiuniaButton(onClick = onPlay, label = stringResource(R.string.niunia_detail_play), icon = "play", focusRequester = focus, onLongClick = onOpen, onFocus = { if (it) onHeroFocus() })
                NiuniaButton(onClick = onLibrary, icon = if (inLibrary) "check" else "plus",
                    description = stringResource(if (inLibrary) R.string.niunia_pinfo_in_library else R.string.niunia_ds_add_library))
                NiuniaButton(onClick = onOpen, icon = "info", description = stringResource(R.string.niunia_ds_details))
                if (count > 1) NiuniaButton(onClick = onNext, icon = "chevron-right", kind = NiuniaButtonKind.Bare, description = stringResource(R.string.niunia_ds_next_featured))
            }
            if (count > 1) NiuniaDots(count.coerceAtMost(10), index % count.coerceAtMost(10))
            if (peek.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
                    NiuniaCollectionHeader(stringResource(R.string.niunia_row_continue), active = false)
                    Row(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20)) {
                        peek.take(5).forEach { ContinueCard(it, focusable = false, onFocus = {}, onOk = {}) }
                    }
                }
            }
        }
    }
}

/** Karta „Oglądaj dalej” = NMediaCard ContinueWatchingCard: ▶, postęp, „S1, E1 · 39m” (zostało). */
@Composable
private fun ContinueCard(cw: ContinueWatchingItem, modifier: Modifier = Modifier, focusable: Boolean = true, onFocus: (Boolean) -> Unit, onOk: () -> Unit) {
    val ctx = LocalContext.current
    val m = remember(cw) { niuniaContinueModel(cw) }
    NiuniaMediaCard(
        preset = NiuniaCardPreset.ContinueWatchingCard,
        image = m.image,
        onClick = onOk,
        modifier = modifier,
        title = m.title,
        caption = m.caption,
        progress = m.progress,
        play = m.progress != null,
        badge = if (m.nextUp) ctx.getString(R.string.niunia_label_new).takeIf { m.newSeason } else null,
        description = listOfNotNull(m.title, m.caption).joinToString(", "),
        focusable = focusable,
        onFocus = onFocus,
    )
}

/** Dane karty „Oglądaj dalej” (czysta funkcja, test w NiuniaDsTest). */
internal data class NiuniaContinueModel(val title: String, val caption: String?, val image: String?, val progress: Int?, val nextUp: Boolean, val newSeason: Boolean)

internal fun niuniaContinueModel(cw: ContinueWatchingItem): NiuniaContinueModel = when (cw) {
    is ContinueWatchingItem.InProgress -> {
        val p = cw.progress
        val fraction = p.progressPercent?.div(100f) ?: if (p.duration > 0) p.position.toFloat() / p.duration else 0f
        val leftMin = if (p.duration > 0) ((p.duration - p.position).coerceAtLeast(0) / 60_000L).toInt() else null
        val ep = if (p.season != null && p.episode != null) "S${p.season}, E${p.episode}" else null
        val caption = listOfNotNull(ep ?: p.name, leftMin?.takeIf { it > 0 }?.let { "${it}m" }).joinToString(" · ")
        NiuniaContinueModel(p.name, caption, cw.episodeThumbnail ?: p.backdrop ?: p.poster, (fraction * 100).toInt().coerceIn(0, 100), nextUp = false, newSeason = false)
    }
    is ContinueWatchingItem.NextUp -> NiuniaContinueModel(
        cw.info.name, "S${cw.info.season}, E${cw.info.episode}", cw.info.thumbnail ?: cw.info.backdrop ?: cw.info.poster, null,
        nextUp = true, newSeason = cw.info.isNewSeasonRelease,
    )
}

/** Długość z dodatku („148”, „148 min”, „2h 28min”) → (godziny, minuty); null = nieznany zapis. */
internal fun niuniaRuntime(raw: String?): Pair<Int, Int>? {
    val t = raw?.trim()?.lowercase() ?: return null
    Regex("""^(\d+)\s*(min|m)?$""").matchEntire(t)?.let { val m = it.groupValues[1].toInt(); return if (m <= 0) null else m / 60 to m % 60 }
    Regex("""^(\d+)\s*h(?:\s*(\d+)\s*(?:min|m)?)?$""").matchEntire(t)?.let { return it.groupValues[1].toInt() to (it.groupValues[2].toIntOrNull() ?: 0) }
    return null
}

/** Ostatni element rzędów Głównej z fokusem (przeżywa przejście na stronę tytułu i z powrotem). */
internal object NiuniaHomeMemory {
    /** Stan Compose: requester powrotu fokusu przechodzi na ostatni element (wyjście z menu). */
    var key: String? by androidx.compose.runtime.mutableStateOf(null)
    /** Jednorazowy powrót fokusu po wejściu na ekran (Wstecz ze strony tytułu). */
    var restorePending: Boolean = false
    var row: Int = 0
    var index: Int = 0
    var down: Boolean = false
}

/** Element zapamiętany w [NiuniaHomeMemory] dostaje [restore] i fokus po wejściu na ekran. */
@Composable
private fun restoreModifier(key: String, restore: FocusRequester): Modifier {
    if (!NiuniaHomeMemory.down || NiuniaHomeMemory.key != key) return Modifier
    LaunchedEffect(key) {
        if (!NiuniaHomeMemory.restorePending) return@LaunchedEffect
        NiuniaHomeMemory.restorePending = false
        kotlinx.coroutines.delay(80)
        try { restore.requestFocus(); Log.i(TAG, "ekran główny: fokus wraca na $key") } catch (e: IllegalStateException) { Log.w(TAG, "ekran główny: powrót fokusu: ${e.javaClass.simpleName}: ${e.message}") }
    }
    return Modifier.focusRequester(restore)
}
