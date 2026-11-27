package com.nuvio.tv.skin.niunia.home

import com.nuvio.tv.skin.niunia.components.niuniaPortrait
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
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
    onItemLongPress: (MetaPreview, String) -> Unit = { _, _ -> },
    cwActions: NiuniaCwActions? = null,
    onRetry: () -> Unit = {},
) {
    // Właściciel 25.11 („dodaj cache”): brakujące rzędy z dodatków → ostatnia zapisana kopia (NiuniaRowsCache).
    val rawState = uiState
    @Suppress("NAME_SHADOWING") val uiState = NiuniaRowsCache.withCache(rawState)
    val context = LocalContext.current
    // Z-16 (P-2): długie OK na plakacie = okno akcji Niuni (TitleActions), nigdy akcja od razu; wybór list Trakt zostaje oknem bazy.
    val posterOptions = com.nuvio.tv.skin.niunia.components.rememberNiuniaPosterOptions { id, type, url -> onNavigateToDetail(id, type, url) }
    // UX 22.11: długie OK na „Oglądaj dalej” = menu bazy (szczegóły, od początku, wybierz źródło, obejrzane, usuń).
    var cwMenu by remember { mutableStateOf<ContinueWatchingItem?>(null) }
    // Właściciel 24.11 („na ekranie głównym nie da się otworzyć filmów z YouTube — wyskakuje błąd”): pozycje „Oglądaj dalej”
    // z YouTube (id youtube:…) szły ścieżką filmów i seriali bazy (sprawdzenie źródeł z dodatków → „niedostępne”) — teraz
    // otwierają odtwarzacz YouTube przez ekran YouTube Niuni (ForgeYouTubeLaunch), jak film wybrany na tym ekranie.
    val playCw: (ContinueWatchingItem) -> Unit = { cw -> if (!niuniaOpenYouTubeCw(cw)) onContinueWatchingClick(cw) }
    // Z-12: karta tytułu (rząd + indeks klikniętego plakatu); przełącznik niunia_title_card (domyślnie włączony).
    var pager by remember { mutableStateOf<Pair<com.nuvio.tv.domain.model.CatalogRow, Int>?>(null) }
    // UX JMGO (ręczne testy 24.11): Wstecz z karty przewiniętej na inny tytuł wracał na otwarty plakat — teraz na oglądany
    // (klucz rzędu, indeks): rząd przewija się do niego i daje mu fokus.
    var cardReturn by remember { mutableStateOf<Pair<String, Int>?>(null) }
    val titleCard = remember { com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(context, "niunia_title_card", true) }
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
    // UX JMGO (ręczne testy 24.11): bez baneru (np. dodatek nie odpowiada) nic nie miało fokusu — ani po starcie, ani po
    // wygaszaczu. Wtedy fokus idzie na pierwszy element pierwszego rzędu (firstFocus, LaunchedEffect niżej).
    val firstFocus = remember { FocusRequester() }
    com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(if (memDown) restoreFocus else if (heroes.isNotEmpty()) heroFocus else if (uiState.continueWatchingItems.isNotEmpty() || uiState.catalogRows.any { it.items.isNotEmpty() }) firstFocus else null)
    remember { memory.restorePending = memDown; 0 }
    fun rememberFocus(key: String, row: Int, index: Int) { if (pager != null) return /* UX 25.11: fokus uciekł spod karty tytułu */; memory.key = key; memory.target = key; memory.row = row; memory.index = index; memory.down = true; memDown = true }
    // UX JMGO 24.11 (F5/F6): powrót po kluczu — „Oglądaj dalej” po obejrzeniu zmienia kolejność, usunięty plakat → sąsiad (NiuniaFocusKeys).
    val rowKeys = remember(uiState.continueWatchingItems, uiState.catalogRows) {
        buildList {
            if (uiState.continueWatchingItems.isNotEmpty()) add("cw:" to uiState.continueWatchingItems.map { "cw:" + cwKey(it) })
            uiState.catalogRows.filter { it.items.isNotEmpty() }.forEach { r ->
                val p = "r:${r.addonId}_${r.catalogId}_${r.rawType}:"
                add(p to (r.items.map { p + it.id } + (p + "__more")))
            }
        }
    }
    remember(rowKeys) {
        if (memory.down) {
            val t = com.nuvio.tv.skin.niunia.components.NiuniaFocusKeys.resolve(rowKeys, memory.key, memory.index)
            if (t != null && t != memory.key) Log.i(TAG, "ekran główny: zapamiętany element zniknął/przesunięty — fokus na sąsiada $t")
            memory.target = t ?: memory.key
        }
        0
    }
    // UX JMGO 24.11: onItemFocus (baza: wzbogacenie TMDB → nowy stan całego ekranu) dopiero po 250 ms bez ruchu — nie przy każdym
    // kroku i nigdy w trakcie przytrzymania strzałki (powtórzenia co ~50 ms). Bez stanu Compose: ruch nie przebudowuje ekranu.
    val focusScope = androidx.compose.runtime.rememberCoroutineScope()
    val itemFocusJob = remember { arrayOfNulls<kotlinx.coroutines.Job>(1) }
    fun itemFocusLater(item: MetaPreview) {
        itemFocusJob[0]?.cancel()
        itemFocusJob[0] = focusScope.launch { kotlinx.coroutines.delay(com.nuvio.tv.skin.niunia.components.NiuniaHold.SETTLE_MS); onItemFocus(item) }
    }
    fun initialIndex(prefix: String): Int {
        val t = memory.target ?: return 0
        if (!memory.down || !t.startsWith(prefix)) return 0
        return rowKeys.firstOrNull { it.first == prefix }?.second?.indexOf(t)?.coerceAtLeast(0) ?: 0
    }
    val posterPattern = remember(uiState.customPosterUrlPattern, uiState.customPosterEnabledScreens) {
        com.nuvio.tv.core.poster.patternForScreen(uiState.customPosterUrlPattern, com.nuvio.tv.core.poster.CustomPosterScreen.HOME, uiState.customPosterEnabledScreens)
    }
    val hasHero = heroes.isNotEmpty()
    val hero = heroes.getOrNull(heroIndex % heroes.size.coerceAtLeast(1))
    LaunchedEffect(heroes.size, uiState.catalogRows.size) {
        Log.i(TAG, "ekran główny: hero=${heroes.size}, dalej oglądaj=${uiState.continueWatchingItems.size}, rzędy=${uiState.catalogRows.size}")
        // Z-19: tytuły z Głównej i „Oglądaj dalej” = słownik poprawiania literówek w Szukaj.
        com.nuvio.tv.skin.niunia.search.NiuniaKnownTitles.add(uiState.catalogRows.flatMap { r -> r.items.map { it.name } } + uiState.continueWatchingItems.map { niuniaContinueModel(it).title })
    }

    val firstKey = rowKeys.firstOrNull()?.second?.firstOrNull()
    fun firstModifier(k: String): Modifier = if (k == firstKey) Modifier.focusRequester(firstFocus) else Modifier
    LaunchedEffect(heroes.isEmpty(), firstKey != null) {
        if (heroes.isNotEmpty() || firstKey == null || memDown) return@LaunchedEffect
        kotlinx.coroutines.delay(150)
        try { firstFocus.requestFocus(); Log.i(TAG, "ekran główny: bez baneru — fokus na pierwszy element $firstKey") }
        catch (e: IllegalStateException) { Log.w(TAG, "ekran główny: fokus bez baneru: ${e.javaClass.simpleName}: ${e.message}") }
    }
    // UX JMGO (ręczne testy 24.11): dwa rzędy „TMDB Popular” (filmy i seriale) — przy powtórzonej nazwie dopisany rodzaj.
    val dupNames = remember(uiState.catalogRows) {
        uiState.catalogRows.filter { it.items.isNotEmpty() }.groupingBy { it.catalogName }.eachCount().filterValues { it > 1 }.keys
    }
    fun rowTitle(r: com.nuvio.tv.domain.model.CatalogRow): String = when {
        r.catalogName !in dupNames -> r.catalogName
        r.rawType == "movie" -> context.getString(R.string.niunia_ux_row_movies, r.catalogName)
        r.rawType == "series" -> context.getString(R.string.niunia_ux_row_series, r.catalogName)
        else -> r.catalogName
    }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { com.nuvio.tv.skin.niunia.nav.NiuniaNavChrome.homeRows.value = false } }
    val rowsFailed = !rawState.isLoading && rawState.continueWatchingItems.isNotEmpty() && rawState.catalogRows.all { it.items.isEmpty() }
    LaunchedEffect(rowsFailed) {
        if (!rowsFailed) return@LaunchedEffect
        Log.w(TAG, "ekran główny: rzędy z dodatków puste (błąd: ${uiState.error ?: "brak"}) — komunikat, ponowna próba za 20 s")
        repeat(3) { n ->
            kotlinx.coroutines.delay(20_000L * (n + 1))
            Log.i(TAG, "ekran główny: sama ponowna próba wczytania rzędów (${n + 1}/3)")
            onRetry()
        }
    }

    // Właściciel 20.11: hero poza listą (był przesunięty o ujemny margines), ▼ = animacja: hero wyjeżdża, tło rozmyte, rzędy wjeżdżają.
    val rows: @Composable () -> Unit = {
        com.nuvio.tv.skin.niunia.components.NiuniaVerticalScroll {
            val colState = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = if (memory.down) memory.row.coerceAtLeast(0) else 0)
            // UX JMGO 25.11 (#44, uxr10): ◄ z pierwszego plakatu rzędu katalogu nie otwierał menu (szukanie fokusu nie trafia w pasek) —
            // jak w Bibliotece: nieobsłużone ◄ najpierw próbuje przenieść fokus, a przy lewej krawędzi otwiera menu.
            val edgeFm = androidx.compose.ui.platform.LocalFocusManager.current
            LazyColumn(state = colState, contentPadding = PaddingValues(bottom = 48.dp), modifier = Modifier.fillMaxSize().onKeyEvent { e ->
                if (pager != null || e.type != androidx.compose.ui.input.key.KeyEventType.KeyDown || e.key != androidx.compose.ui.input.key.Key.DirectionLeft) false
                else edgeFm.moveFocus(androidx.compose.ui.focus.FocusDirection.Left) || com.nuvio.tv.skin.niunia.nav.NiuniaNavActions.toMenu("główna ◄ z pierwszego plakatu")
            }) {
                var firstBelow = true // pierwszy rząd 40 dp od góry
                if (uiState.continueWatchingItems.isNotEmpty()) {
                    val top = firstBelow; firstBelow = false
                    item(key = "niunia_cw") {
                        RowTitle(stringResource(R.string.niunia_row_continue), on = focusedRow == -2, first = top)
                        com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                            val st = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = initialIndex("cw:"))
                            // UX JMGO (ręczne testy 24.11): po obejrzeniu tytuł wskakuje na początek rzędu, a rząd zostawał przewinięty —
                            // karta z fokusem była ucięta przy lewej krawędzi. Wtedy rząd wraca na początek.
                            val firstCw = uiState.continueWatchingItems.firstOrNull()?.let { "cw:" + cwKey(it) }
                            LaunchedEffect(firstCw) {
                                if (firstCw == null || !NiuniaHomeMemory.down || NiuniaHomeMemory.target != firstCw) return@LaunchedEffect
                                if (st.firstVisibleItemIndex == 0 && st.firstVisibleItemScrollOffset == 0) return@LaunchedEffect
                                st.scrollToItem(0); Log.i(TAG, "„Oglądaj dalej”: obejrzany tytuł na początku rzędu — rząd przewinięty na początek")
                            }
                            LazyRow(state = st, contentPadding = rowPadding(), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                                itemsIndexed(uiState.continueWatchingItems, key = { _, it -> cwKey(it) }) { i, cw ->
                                    val k = "cw:" + cwKey(cw)
                                    ContinueCard(cw, modifier = restoreModifier(k, restoreFocus).then(firstModifier(k)), onFocus = { if (it) { focusedRow = -2; rememberFocus(k, 0, i) } },
                                        onLongOk = if (cwActions != null) ({ Log.i(TAG, "rzędy: długie OK → opcje „Oglądaj dalej” ${niuniaCwContentId(cw)}"); cwMenu = cw }) else null,
                                        onOk = { Log.i(TAG, "rzędy: Oglądaj dalej → ${niuniaCwContentId(cw)}"); playCw(cw) })
                                }
                            }
                        }
                    }
                }
                // UX JMGO (ręczne testy 24.11): dodatek odpowiadał 429 („za dużo zapytań”) — były tylko „Oglądaj dalej” i pusty ekran
                // bez słowa. Teraz pod „Oglądaj dalej” komunikat z przyciskiem, a ponowna próba idzie sama (LaunchedEffect niżej).
                if (rowsFailed && uiState.catalogRows.all { it.items.isEmpty() }) {
                    item(key = "niunia_rows_failed") { RowsFailed(onRetry) }
                }
                uiState.catalogRows.forEachIndexed { rowIndex, row ->
                    if (row.items.isEmpty()) return@forEachIndexed
                    val top = firstBelow; firstBelow = false
                    item(key = "niunia_row_${row.addonId}_${row.catalogId}_${row.rawType}") {
                        val ranking = NiuniaLabels.isRankingRow(row.catalogName)
                        RowTitle(rowTitle(row), on = focusedRow == rowIndex, first = top)
                        com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                            val rowKey = "r:${row.addonId}_${row.catalogId}_${row.rawType}:"
                            val st = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = initialIndex(rowKey))
                            val colIndex = rowIndex + if (uiState.continueWatchingItems.isNotEmpty()) 1 else 0
                            // #44 (uxr9): restoreFocus nie był jeszcze przypięty do oglądanego plakatu (Compose 1.11 tylko wypisuje
                            // „FocusRequester is not initialized”) — plakat z karty dostaje własny requester, fokus ponawiany co klatkę.
                            val returnFocus = remember { FocusRequester() }
                            LaunchedEffect(cardReturn) {
                                val r = cardReturn ?: return@LaunchedEffect
                                if (r.first != rowKey) return@LaunchedEffect
                                // Stan zerowany dopiero na końcu: zmiana klucza efektu w trakcie anulowała przewijanie (test uxr3).
                                try {
                                    st.scrollToItem((r.second - 1).coerceAtLeast(0))
                                    var n = 0
                                    var ok = false
                                    while (!ok && n < 10) {
                                        androidx.compose.runtime.withFrameNanos { }
                                        n++
                                        ok = returnFocus.requestFocus(androidx.compose.ui.focus.FocusDirection.Enter)
                                    }
                                    if (ok) Log.i(TAG, "karta tytułu: Wstecz → plakat ${r.second + 1} w rzędzie (klatka $n, pierwszy widoczny ${st.firstVisibleItemIndex + 1})")
                                    else Log.w(TAG, "karta tytułu: plakat ${r.second + 1} nie przyjął fokusu przez 10 klatek (cel ${memory.target}, down ${memory.down})")
                                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                catch (e: IllegalStateException) { Log.w(TAG, "karta tytułu: powrót na oglądany plakat: ${e.javaClass.simpleName}: ${e.message}") }
                                // #44: karta znika dopiero teraz — fokus jest już na plakacie, więc Compose nie szuka nowego.
                                pager = null
                                cardReturn = null
                            }
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
                                        modifier = restoreModifier(k, restoreFocus).then(firstModifier(k))
                                            .then(if (cardReturn?.let { it.first == rowKey && it.second == i } == true) Modifier.focusRequester(returnFocus) else Modifier),
                                        award = com.nuvio.tv.skin.niunia.components.niuniaPosterAward(item.id), // Z-13
                                        onFocus = { if (it) { focusedRow = rowIndex; itemFocusLater(item); rememberFocus(k, colIndex, i) } },
                                        onOk = {
                                            if (titleCard) { Log.i(TAG, "rzędy: OK → karta tytułu ${item.id} (${i + 1}/${row.items.size} w „${row.catalogName}”)"); pager = row to i }
                                            else { Log.i(TAG, "rzędy: OK → strona tytułu ${item.id}"); onNavigateToDetail(item.id, item.rawType, row.addonBaseUrl) }
                                        },
                                        onLongOk = { Log.i(TAG, "rzędy: długie OK → okno akcji ${item.id}"); posterOptions(item, row.addonBaseUrl) },
                                    )
                                }
                                // Z-16 (P-4): ostatnia karta rzędu dodatku = „Zobacz wszystko” → cały katalog (CatalogAll).
                                item(key = "niunia_more_${row.addonId}_${row.catalogId}") {
                                    val k = rowKey + "__more"
                                    com.nuvio.tv.skin.niunia.components.NiuniaMoreCard(
                                        rowName = row.catalogName,
                                        modifier = restoreModifier(k, restoreFocus),
                                        onFocus = { if (it) { focusedRow = rowIndex; rememberFocus(k, colIndex, row.items.size) } },
                                        onOk = {
                                            val route = com.nuvio.tv.ui.navigation.Screen.CatalogSeeAll.createRoute(row.catalogId, row.addonId, row.apiType)
                                            Log.i(TAG, "rzędy: Zobacz wszystko → ${row.addonId}/${row.catalogId} (${row.apiType})")
                                            val go = com.nuvio.tv.ui.screens.settings.ForkFeatures.open
                                            if (go != null) go(route) else { Log.w(TAG, "rzędy: Zobacz wszystko: brak nawigatora"); com.nuvio.tv.fork.ForkLog.say(context, TAG, context.getString(R.string.niunia_ke2_retry)) }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    val menuItem = cwMenu
    if (menuItem != null && cwActions != null) {
        fun close(what: String, action: (ContinueWatchingItem) -> Unit) { Log.i(TAG, "„Oglądaj dalej”: $what ${niuniaCwContentId(menuItem)}"); cwMenu = null; action(menuItem) }
        // Z-14 (makieta TitleActions): okno akcji „Oglądaj dalej” w stylu Niuni zamiast okna bazy.
        val ctx = LocalContext.current
        val (cwTitle, cwState, series) = niuniaCwDialogText(ctx, menuItem)
        val resumeMs = (menuItem as? ContinueWatchingItem.InProgress)?.progress?.position?.takeIf { it > 0L }
        // UX 25.11 (#47): film YouTube — „Od początku”, „Wybierz źródło” i „Szczegóły” prowadziły ścieżką filmów z dodatków.
        val yt = niuniaCwContentId(menuItem).startsWith("youtube:")
        com.nuvio.tv.skin.niunia.components.NiuniaActionsDialog(
            eyebrow = ctx.getString(R.string.niunia_ke_cw_eyebrow),
            title = cwTitle,
            state = cwState,
            onDismiss = { cwMenu = null },
            actions = listOfNotNull(
                com.nuvio.tv.skin.niunia.components.NiuniaDialogAction(
                    if (resumeMs != null) ctx.getString(R.string.niunia_ke_resume_at, niuniaClock(resumeMs)) else ctx.getString(R.string.niunia_ke_play), icon = "play",
                ) { close("wznów", playCw) },
                if (resumeMs != null && !yt) com.nuvio.tv.skin.niunia.components.NiuniaDialogAction(ctx.getString(R.string.niunia_ke_from_start), icon = "restart") { close("od początku", cwActions.fromStart) } else null,
                if (cwActions.showManual && !yt) com.nuvio.tv.skin.niunia.components.NiuniaDialogAction(ctx.getString(R.string.niunia_ke_choose_source), icon = "playlist-add", chevron = true) { close("wybierz źródło", cwActions.playManually) } else null,
                if (yt) null else com.nuvio.tv.skin.niunia.components.NiuniaDialogAction(ctx.getString(if (series) R.string.niunia_ke_details_series else R.string.niunia_ke_details_movie), icon = "info", chevron = true) {
                    close("szczegóły") { cw -> onNavigateToDetail(niuniaCwContentId(cw), niuniaCwContentType(cw), "") }
                },
                com.nuvio.tv.skin.niunia.components.NiuniaDialogAction(ctx.getString(R.string.niunia_ke_mark_watched), icon = "check") { close("obejrzane", cwActions.markWatched) },
                com.nuvio.tv.skin.niunia.components.NiuniaDialogAction(ctx.getString(R.string.niunia_ke_remove_cw), icon = "close") { close("usuń", cwActions.remove) },
            ),
        )
    }
    // Z-16 (P-5, makieta HomeEmpty): brak katalogów / błąd → blok pusty z jedną akcją (fokus na przycisku).
    val nothing = !uiState.isLoading && uiState.catalogRows.all { it.items.isEmpty() } && uiState.continueWatchingItems.isEmpty()
    if (nothing) {
        HomeEmpty(error = uiState.error, onRetry = onRetry)
        return
    }
    val pagerOverlay: @Composable () -> Unit = {
        pager?.let { (row, start) ->
            NiuniaTitlePager(
                items = row.items, start = start, rowName = row.catalogName, ranking = NiuniaLabels.isRankingRow(row.catalogName),
                inLibrary = { m -> uiState.posterLibraryMembership[com.nuvio.tv.ui.screens.home.homeItemStatusKey(m.id, m.apiType)] == true },
                onPlay = { m ->
                    val route = com.nuvio.tv.ui.navigation.Screen.Detail.createRoute(m.id, m.rawType, row.addonBaseUrl, playOnLoad = true)
                    val go = com.nuvio.tv.ui.screens.settings.ForkFeatures.open
                    Log.i(TAG, "karta tytułu: Odtwórz ${m.id}")
                    pager = null
                    if (go != null) go(route) else onNavigateToDetail(m.id, m.rawType, row.addonBaseUrl)
                },
                onDetails = { m -> Log.i(TAG, "karta tytułu: (i) → strona tytułu ${m.id}"); pager = null; onNavigateToDetail(m.id, m.rawType, row.addonBaseUrl) },
                onLibrary = { m -> Log.i(TAG, "karta tytułu: biblioteka ${m.id}"); onToggleLibrary(m, row.addonBaseUrl) },
                onClose = { page ->
                    val item = row.items.getOrNull(page)
                    if (page != start && item != null) {
                        // UX JMGO 25.11 (#44): po zamknięciu karty Compose sam dawał fokus lewemu górnemu plakatowi (40 ms po Wstecz),
                        // co nadpisywało pamięć fokusu. Teraz karta zostaje, aż rząd (LaunchedEffect(cardReturn)) da fokus oglądanemu plakatowi.
                        val rowKey = "r:${row.addonId}_${row.catalogId}_${row.rawType}:"
                        val k = rowKey + item.id
                        memory.key = k; memory.target = k; memory.index = page; memory.down = true; memDown = true
                        memory.row = uiState.catalogRows.indexOf(row) + if (uiState.continueWatchingItems.isNotEmpty()) 1 else 0
                        val ret = rowKey to page
                        cardReturn = ret
                        focusScope.launch {
                            kotlinx.coroutines.delay(800)
                            if (cardReturn == ret && pager != null) { Log.w(TAG, "karta tytułu: rząd nie przejął fokusu w 0,8 s — zamykam kartę"); pager = null; cardReturn = null }
                        }
                    } else {
                        pager = null
                        try { restoreFocus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "karta tytułu: powrót fokusu: ${e.javaClass.simpleName}: ${e.message}") }
                    }
                },
            )
        }
    }
    if (hero == null) {
        // UX JMGO (ręczne testy 24.11): bez baneru tytuł pierwszego rzędu wchodził pod pigułkę menu „Główna” — odstęp od paska.
        Box(Modifier.fillMaxSize().background(NiuniaColors.Bg)) { Box(Modifier.padding(top = 56.dp)) { rows() }; pagerOverlay() }
        return
    }
    Box(Modifier.fillMaxSize()) {
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
                    if (cw != null) { Log.i(TAG, "ekran główny: Odtwórz → wznów ${hero.id}"); playCw(cw) } else open()
                },
                onOpen = open,
                onLibrary = { Log.i(TAG, "ekran główny: biblioteka ${hero.id}"); onToggleLibrary(hero, addon) },
                onNext = { heroIndex = (heroIndex + 1) % heroes.size; Log.i(TAG, "ekran główny: następny tytuł ${heroIndex + 1}/${heroes.size}") },
            )
        },
        below = { rows() },
        onDown = { com.nuvio.tv.skin.niunia.nav.NiuniaNavChrome.homeRows.value = it },
        scrim = { NiuniaSurface(NiuniaSurfaceKind.ScrimHero, Modifier.fillMaxSize()) },
        // Przegląd 27.11 (CAŁA APLIKACJA): zamiast szarego ScrimHero z lewej — rozmyta kopia tła z lewej (lekko przyciemniona pod tekst).
        scrimBlurLeft = true, scrimBlurTint = androidx.compose.ui.graphics.Color(0x40000000),
    )
    pagerOverlay()
    }
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

/** Film z YouTube z „Oglądaj dalej” → ekran YouTube Niuni odtwarza go od razu (wznowienie z YouTubeProgress). false = to nie YouTube. */
private fun niuniaOpenYouTubeCw(cw: ContinueWatchingItem): Boolean {
    val id = niuniaCwContentId(cw)
    if (!com.nuvio.tv.youtube.YouTubeProgress.isYouTubeId(id)) return false
    val videoId = id.removePrefix(com.nuvio.tv.youtube.YouTubeProgress.ID_PREFIX).takeIf { it.isNotBlank() } ?: return false
    val go = com.nuvio.tv.ui.screens.settings.ForkFeatures.open ?: run { Log.w(TAG, "„Oglądaj dalej”: YouTube $videoId — brak nawigatora"); return false }
    val m = niuniaContinueModel(cw)
    val item = com.nuvio.tv.youtube.YtItem(
        kind = com.nuvio.tv.youtube.YtKind.VIDEO, videoId = videoId, title = m.title, author = null, thumbnail = m.image, badge = null,
        isLive = false, channelId = null, percentWatched = 0,
        source = com.liskovsoft.youtubeapi.service.data.YouTubeMediaItem.fromIds(videoId, null),
    )
    Log.i(TAG, "„Oglądaj dalej”: YouTube $videoId → odtwarzacz YouTube")
    com.nuvio.tv.skin.kicia.youtube.ForgeYouTubeLaunch.returnRoute = "home"
    com.nuvio.tv.skin.kicia.youtube.ForgeYouTubeLaunch.playerShown = false
    com.nuvio.tv.skin.kicia.youtube.ForgeYouTubeLaunch.ask(item)
    go(com.nuvio.tv.ui.navigation.Screen.YouTube.route)
    return true
}

private fun niuniaCwContentId(cw: ContinueWatchingItem): String = when (cw) {
    is ContinueWatchingItem.InProgress -> cw.progress.contentId
    is ContinueWatchingItem.NextUp -> cw.info.contentId
}

private fun niuniaCwContentType(cw: ContinueWatchingItem): String = when (cw) {
    is ContinueWatchingItem.InProgress -> cw.progress.contentType
    is ContinueWatchingItem.NextUp -> cw.info.contentType
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
    Box(Modifier.fillMaxSize()) { // scrim hero rysuje NiuniaHeroPage (parametr scrim, fork/glass)
        Column(
            Modifier.align(Alignment.BottomStart).offset(y = if (peek.isNotEmpty()) NiuniaSize.Peek else -NiuniaSpacing.Space40)
                .padding(horizontal = NiuniaSpacing.Space40),
            verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space16),
        ) {
            NiuniaContentBlock(NiuniaBlockKind.Info) {
                NiuniaLogo(item.logo, item.name, NiuniaLogoSize.Hero)
                val type = if (item.rawType == "series") ctx.getString(R.string.niunia_player_series) else ctx.getString(R.string.niunia_player_movie)
                val meta = listOfNotNull(type, item.genres.take(2).mapNotNull { com.nuvio.tv.skin.niunia.components.NiuniaGenres.pl(it) }.joinToString(" · ").ifBlank { null }, item.releaseInfo?.takeIf { it.isNotBlank() }, niuniaRuntime(item.runtime)?.let { (h, m) -> if (h > 0) ctx.getString(R.string.niunia_hours_minutes, h, m) else ctx.getString(R.string.niunia_minutes, m) } ?: item.runtime?.takeIf { it.isNotBlank() })
                    .joinToString(" · ")
                NiuniaMetaLine(meta, tone = NiuniaTone.Primary) {
                    // Z-10: na Głównej tylko średnia ocen (zastępuje „16” i „IMDb/TMDB”).
                    com.nuvio.tv.skin.niunia.components.NiuniaRatingAverage(com.nuvio.tv.skin.niunia.components.niuniaScores(item.imdbRating, null))
                }
                if (!item.description.isNullOrBlank()) NiuniaText(item.description, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 2)
            }
            NiuniaCollection(NiuniaCollectionKind.Actions) {
                NiuniaButton(onClick = onPlay, label = stringResource(R.string.niunia_detail_play), icon = "play", focusRequester = focus, onLongClick = onOpen, onFocus = { if (it) onHeroFocus() })
                NiuniaButton(onClick = onLibrary, icon = if (inLibrary) "check" else "plus",
                    description = stringResource(if (inLibrary) R.string.niunia_pinfo_in_library else R.string.niunia_ds_add_library))
                NiuniaButton(onClick = onOpen, icon = "info", description = stringResource(R.string.niunia_ds_details))
                if (count > 1) NiuniaButton(onClick = onNext, icon = "chevron-right", kind = if (com.nuvio.tv.fork.glass.ForkGlass.active != null) NiuniaButtonKind.Icon else NiuniaButtonKind.Bare, /* szkło: „Następny” jak w makiecie */ description = stringResource(R.string.niunia_ds_next_featured))
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
private fun ContinueCard(cw: ContinueWatchingItem, modifier: Modifier = Modifier, focusable: Boolean = true, onFocus: (Boolean) -> Unit, onLongOk: (() -> Unit)? = null, onOk: () -> Unit) {
    val ctx = LocalContext.current
    val m = remember(cw) { niuniaContinueModel(cw) }
    // Z-16 (P-1): jeden stan na kartę: „Nowy sezon” › „Nowy odcinek” › „Premiera jutro” › „Następny: S1 E2” › „Zostało 39 min”.
    val state = remember(cw) { niuniaCwState(ctx, cw) ?: m.caption }
    NiuniaMediaCard(
        preset = NiuniaCardPreset.ContinueWatchingCard,
        image = m.image,
        onClick = onOk,
        modifier = modifier,
        title = m.title,
        caption = state,
        progress = m.progress,
        play = m.progress != null,
        badge = if (m.nextUp) ctx.getString(R.string.niunia_label_new).takeIf { m.newSeason } else null,
        description = listOfNotNull(m.title, m.caption, state?.takeIf { it != m.caption }).joinToString(", "),
        focusable = focusable,
        onFocus = onFocus,
        onLongClick = onLongOk,
    )
}

/** Czynności menu „Oglądaj dalej” (długie OK) — podaje adapter skórki z ekranu głównego bazy. */
class NiuniaCwActions(
    val remove: (ContinueWatchingItem) -> Unit,
    val markWatched: (ContinueWatchingItem) -> Unit,
    val fromStart: (ContinueWatchingItem) -> Unit,
    val playManually: (ContinueWatchingItem) -> Unit,
    val showManual: Boolean,
)

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

/** Z-16 (P-1): stan karty „Oglądaj dalej” wg ważności; null = zostaje podpis z [niuniaContinueModel]. */
internal fun niuniaCwState(ctx: android.content.Context, cw: ContinueWatchingItem, now: Long = System.currentTimeMillis()): String? = when (cw) {
    is ContinueWatchingItem.InProgress -> {
        val p = cw.progress
        val left = if (p.duration > 0) ((p.duration - p.position).coerceAtLeast(0) / 60_000L).toInt() else 0
        if (left > 0) ctx.getString(R.string.niunia_ke2_cw_left, left) else null
    }
    is ContinueWatchingItem.NextUp -> {
        val i = cw.info
        val zone = java.time.ZoneId.systemDefault()
        val day = i.releaseTimestamp?.let { java.time.Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        when {
            i.isNewSeasonRelease && i.hasAired -> ctx.getString(R.string.niunia_ke2_cw_new_season)
            i.isReleaseAlert && i.hasAired -> ctx.getString(R.string.niunia_ke2_cw_new_episode)
            !i.hasAired && day == today.plusDays(1) -> ctx.getString(R.string.niunia_ke2_cw_premiere_tomorrow)
            !i.hasAired && !i.airDateLabel.isNullOrBlank() -> ctx.getString(R.string.niunia_ke2_cw_premiere, i.airDateLabel)
            else -> ctx.getString(R.string.niunia_ke2_cw_next, i.season, i.episode)
        }
    }
}

/** Z-16 (P-5): pusta Główna — „Brak dodatków z katalogami” + „Otwórz dodatki”; błąd katalogów — „Spróbuj ponownie” (odświeża Główną). */
/** Rzędy z dodatków się nie wczytały, a „Oglądaj dalej” jest: krótki tekst i „Spróbuj ponownie” pod rzędem. */
@Composable
private fun RowsFailed(onRetry: () -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.padding(start = NiuniaSpacing.Space40, top = NiuniaSpace.AfterRow), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NiuniaText(ctx.getString(R.string.niunia_ux_rows_failed), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 2)
        NiuniaButton(
            onClick = { Log.i(TAG, "ekran główny: rzędy puste → spróbuj ponownie (przycisk)"); onRetry() },
            label = ctx.getString(R.string.niunia_ke2_retry), icon = "restart", compact = true,
        )
    }
}

@Composable
private fun HomeEmpty(error: String?, onRetry: () -> Unit) {
    val ctx = LocalContext.current
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(focus)
    LaunchedEffect(error) {
        Log.i(TAG, "ekran główny: pusty (${if (error != null) "błąd: $error" else "brak katalogów"}), fokus → przycisk")
        kotlinx.coroutines.delay(80)
        try { focus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "ekran główny: fokus pustego stanu: ${e.javaClass.simpleName}: ${e.message}") }
    }
    Box(Modifier.fillMaxSize().background(NiuniaColors.Bg).padding(start = NiuniaDims.Gutter, top = 120.dp)) {
        com.nuvio.tv.skin.niunia.components.NiuniaContentBlock(com.nuvio.tv.skin.niunia.components.NiuniaBlockKind.Empty) {
            com.nuvio.tv.skin.niunia.components.NiuniaText(ctx.getString(if (error != null) R.string.niunia_ke2_home_error_title else R.string.niunia_ke2_home_empty_title),
                variant = com.nuvio.tv.skin.niunia.components.NiuniaTextVariant.TitleSection)
            com.nuvio.tv.skin.niunia.components.NiuniaText(error ?: ctx.getString(R.string.niunia_ke2_home_empty_text),
                variant = com.nuvio.tv.skin.niunia.components.NiuniaTextVariant.BodySmall, tone = com.nuvio.tv.skin.niunia.components.NiuniaTone.Secondary, lines = 3)
            val route = com.nuvio.tv.ui.navigation.Screen.AddonManager.route
            com.nuvio.tv.skin.niunia.components.NiuniaButton(
                onClick = {
                    if (error != null) { Log.i(TAG, "ekran główny: pusty → spróbuj ponownie"); onRetry(); return@NiuniaButton }
                    Log.i(TAG, "ekran główny: pusty → $route")
                    val go = com.nuvio.tv.ui.screens.settings.ForkFeatures.open
                    if (go != null) go(route) else Log.w(TAG, "ekran główny: brak nawigatora dla $route")
                },
                label = ctx.getString(if (error != null) R.string.niunia_ke2_retry else R.string.niunia_ke2_home_open_addons),
                icon = if (error != null) "restart" else "plus", compact = true, focusRequester = focus,
            )
        }
    }
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
    var key: String? = null
    /**
     * Element, który ma requester powrotu (stan Compose — wyjście z menu): ostatni z fokusem albo jego sąsiad, gdy zniknął.
     * UX JMGO 24.11: czytany przez derivedStateOf w [restoreModifier] — ruch fokusu przebudowuje 2 elementy, nie wszystkie widoczne.
     */
    var target: String? by androidx.compose.runtime.mutableStateOf(null)
    /** Jednorazowy powrót fokusu po wejściu na ekran (Wstecz ze strony tytułu). */
    var restorePending: Boolean = false
    var row: Int = 0
    var index: Int = 0
    var down: Boolean = false
}

/** Element zapamiętany w [NiuniaHomeMemory] dostaje [restore] i fokus po wejściu na ekran. */
@Composable
private fun restoreModifier(key: String, restore: FocusRequester): Modifier {
    val isTarget by remember(key) { androidx.compose.runtime.derivedStateOf { NiuniaHomeMemory.target == key } }
    if (!NiuniaHomeMemory.down || !isTarget) return Modifier
    LaunchedEffect(key) {
        if (!NiuniaHomeMemory.restorePending) return@LaunchedEffect
        NiuniaHomeMemory.restorePending = false
        // Pierwsza klatka, zanim domyślny fokus trafi do menu (wcześniej po 80 ms menu zdążyło się na chwilę otworzyć).
        androidx.compose.runtime.withFrameNanos { }
        val ok = try { restore.requestFocus(); true } catch (e: IllegalStateException) { Log.w(TAG, "ekran główny: powrót fokusu (1. klatka): ${e.javaClass.simpleName}: ${e.message}"); false }
        if (!ok) {
            kotlinx.coroutines.delay(80)
            try { restore.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "ekran główny: powrót fokusu: ${e.javaClass.simpleName}: ${e.message}") }
        }
        Log.i(TAG, "ekran główny: fokus wraca na $key")
    }
    return Modifier.focusRequester(restore)
}

/** Tytuł, linia stanu i „serial?” do okna akcji „Oglądaj dalej” (Z-14): „S1, E1 · tytuł · zostało 39 min”, „Następny odcinek: S1, E4”, „Premiera …”. */
private fun niuniaCwDialogText(ctx: android.content.Context, cw: ContinueWatchingItem): Triple<String, String?, Boolean> = when (cw) {
    is ContinueWatchingItem.InProgress -> {
        val p = cw.progress
        val left = ((p.duration - p.position).coerceAtLeast(0L) / 60_000L).toInt().takeIf { p.duration > 0L && it > 0 }
        val ep = if (p.season != null && p.episode != null) listOfNotNull("S${p.season}, E${p.episode}", p.episodeTitle?.takeIf { it.isNotBlank() }).joinToString(" · ") else null
        Triple(p.name, listOfNotNull(ep, left?.let { ctx.getString(R.string.niunia_ke_left_min, it) }).joinToString(" · ").ifBlank { null }, p.season != null)
    }
    is ContinueWatchingItem.NextUp -> {
        val i = cw.info
        val ep = listOfNotNull("S${i.season}, E${i.episode}", i.episodeTitle?.takeIf { it.isNotBlank() }).joinToString(" · ")
        val head = when {
            !i.hasAired -> ctx.getString(R.string.niunia_ke_airs, i.airDateLabel ?: i.released?.take(10) ?: "")
            i.isNewSeasonRelease -> ctx.getString(R.string.niunia_ke_new_season)
            else -> ctx.getString(R.string.niunia_ke_next_up)
        }
        Triple(i.name, "$head: $ep", true)
    }
}

/** 1:12:04 / 26:00 z milisekund. */
private fun niuniaClock(ms: Long): String {
    val t = ms / 1000; val h = t / 3600; val m = (t % 3600) / 60; val sec = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}
