@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.nuvio.tv.skin.niunia.livetv

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.nuvio.tv.skin.niunia.components.niuniaOk
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.fork.tv.AlertKind
import com.nuvio.tv.fork.tv.TvStore
import com.nuvio.tv.reshaped.livetv.LiveTvCatchupLinks
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories
import com.nuvio.tv.skin.kicia.livetv.forgeChannelLogo
import com.nuvio.tv.skin.kicia.livetv.rememberForgeLogoVersion
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import com.nuvio.tv.ui.reshaped.livetv.LiveTvScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

private const val TAG = "ForkNiunia"
private const val HALF_MS = 30 * MIN_MS
private val HALF_DP = 220.dp // canvas: 220 dp na pół godziny (20:30 → 214, 21:00 → 434)
private val GRID_X = 214.dp // canvas: komórki od x 214, kolumna kanału 40..206
private val CH_W = 166.dp
private val CELL_H = 44.dp
private val ROW_H = 48.dp
private const val WINDOW_MS = 105 * MIN_MS // 746 dp do prawej krawędzi ≈ 1 h 42 min
private const val PAST_DAYS = 7

/** Ekrany Live TV Niuni w jednej trasie (Wstecz wraca do przewodnika). */
private enum class LiveMode { GUIDE, DISCOVER, SEARCH }

/** Wejście do Live TV od razu w trybie Szukaj (z wyników głównej wyszukiwarki, zakładka TV). Jednorazowe. */
internal object NiuniaLiveStart {
    @Volatile var openSearch: Boolean = false
    fun consumeSearch(): Boolean = openSearch.also { openSearch = false }
}

/**
 * Live TV Niuni (canvas v4: `LiveTV`, `LiveTVFav`, `LiveTVRow`, `LiveTVDiscover`, `LiveTVSearch`; decyzje właściciela 20.11).
 * U góry po prawej Szukaj · Dziś · Odkrywaj; rząd kafelków i linia filtrów (Wszystkie — fokus na wejściu, Ulubione,
 * Oglądaj dalej, kategorie z EPG) zmieniają jednocześnie kafelki i przewodnik. Przewodnik jak u nadawców: numer + logo,
 * komórki w jednej linii, ▶ przy trwającym, fokus = biała obwódka, podziałka co 30 min, czerwona linia „teraz”;
 * w rzędzie z fokusem końce osi przechodzą w tło ze strzałkami ‹ ›. Pod przewodnikiem opis programu z fokusem.
 * Dane: [LiveTvRepository], [TvStore], [ForgeEpgCategories] — bez nowych źródeł.
 */
@Composable
fun NiuniaLiveTv(onPlay: (String) -> Unit, viewModel: LiveTvScreenModel = hiltViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    remember(viewModel) { viewModel.ensureLoaded() }
    // Test JMGO build151: bez tego (robiły to tylko ekrany Kici) indeks kategorii programów był pusty → „Kategorie · 0”.
    remember { com.nuvio.tv.skin.kicia.ForgePrefs.ensure(context) }
    rememberForgeLogoVersion()
    val repoState by LiveTvRepository.uiState.collectAsState()
    val favShows by TvStore.favoriteShows.collectAsState()
    val alerts by TvStore.alerts.collectAsState()
    val recent by TvStore.recent.collectAsState()
    val resume by TvStore.resume.collectAsState()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); now = System.currentTimeMillis() } }
    // 23.11 (TvRam, właściciel: „mała próbka startowa, żeby ekran rysował się szybko”): dopóki pełna lista się wczytuje,
    // ekran pokazuje zapisaną próbkę (pierwsze kanały, ulubione, „Dla Ciebie”) z programem na teraz.
    // JMGO 23.11 („brak programu na wszystkich kanałach przez długi czas”): także po wczytaniu listy, dopóki przewodnik
    // nie dojdzie, „teraz na antenie” kanałów z próbki pochodzi z niej.
    val snap = if (com.nuvio.tv.fork.tv.ram.TvRam.enabled) com.nuvio.tv.fork.tv.ram.TvSnapshot.data else null
    val snapshot = if (snap != null && repoState.shownChannels.isEmpty() && !repoState.isLoaded) snap else null
    val snapNow = snap != null && repoState.currentProgrammes.isEmpty()
    val state = remember(repoState, snapshot, snapNow, now / MIN_MS) {
        if (snapshot == null && !snapNow) repoState else repoState.copy(
            channels = snapshot?.channels ?: repoState.channels,
            shownChannels = snapshot?.channels ?: repoState.shownChannels,
            currentProgrammes = if (snapNow) snap!!.currentProgrammes(now) else repoState.currentProgrammes,
            favoriteUrls = repoState.favoriteUrls.ifEmpty { snap!!.favoriteUrls },
        ).also { Log.i(TAG, "live tv: próbka startowa — ${if (snapshot != null) "${snapshot.channels.size} kanałów, " else ""}program na teraz, dopóki przewodnik się wczytuje") }
    }
    var mode by remember { mutableStateOf(if (NiuniaLiveStart.consumeSearch()) LiveMode.SEARCH.also { Log.i(TAG, "live tv: start w trybie Szukaj") } else LiveMode.GUIDE) }
    // Paczka „oczywiste” pkt 2: po powrocie z odtwarzacza ten sam filtr i dzień (lista kanałów jak przed wyjściem).
    var filter by remember { mutableStateOf<NiuniaLiveFilter>(NiuniaLiveReturn.restoredFilter() ?: NiuniaLiveFilter.All) }
    var dayOffset by remember { mutableIntStateOf(NiuniaLiveReturn.restoredDay()) }
    val returnTo = remember { NiuniaLiveReturn.consume() }
    var dayPicker by remember { mutableStateOf(false) }
    var launching by remember { mutableStateOf(false) }
    val allFocus = remember { FocusRequester() }
    com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(if (mode == LiveMode.GUIDE) allFocus else null)

    // 22.11: prawdziwa długość archiwum źródeł (raz na dobę, w tle) — etykieta „Dostępny jeszcze …”.
    LaunchedEffect(state.channels.size) { com.nuvio.tv.fork.tv.CatchupDepth.probeStale(context, state.channels) }
    // 22.11: najpierw kanały polskojęzyczne, potem zagraniczne. JMGO 22.11 (ANR przy 59 349 kanałach): duża lista w tle.
    val input = state.shownChannels
    val hiddenGroups = state.hiddenGroups
    // 22.11 (właściciel): „kraj IPTV” — najpierw kanały kraju (domyślnie Polska) albo tylko one; czytane przy wejściu.
    val countryMode = remember { com.nuvio.tv.fork.tv.country.TvCountry.mode(context) }
    fun compute(): NiuniaLiveOrder.Prepared {
        val providers = state.sources.map { it.name }.filter { it.isNotBlank() }
        val lists = com.nuvio.tv.fork.tv.guide.ExtraGuides.countryBySource
        return NiuniaLiveOrder.prepare(input, hiddenGroups, countryMode, arrange = { list ->
            com.nuvio.tv.fork.tv.country.TvCountry.arrange(list, countryMode, providers, { lists[it] }) {
                Log.w(TAG, "live tv: żaden kanał nie jest z kraju ${com.nuvio.tv.fork.tv.country.TvCountry.nameOf(countryMode.country)} — pokazuję wszystkie")
            }.also { Log.i(TAG, "live tv: kraj IPTV ${countryMode}: ${it.size} z ${list.size} kanałów") }
        }) { NiuniaLiveFilters.categories(context, it, hiddenGroups) }
    }
    var prepared by remember {
        mutableStateOf(NiuniaLiveOrder.cached(input, hiddenGroups, countryMode) ?: if (input.size <= NiuniaLiveOrder.SYNC_LIMIT) compute()
        else NiuniaLiveOrder.unsorted(input, hiddenGroups, countryMode))
    }
    LaunchedEffect(input, hiddenGroups, countryMode) {
        if (prepared.sorted && prepared.isFor(input, hiddenGroups, countryMode)) return@LaunchedEffect
        NiuniaLiveOrder.cached(input, hiddenGroups, countryMode)?.let { prepared = it; return@LaunchedEffect }
        if (input.size <= NiuniaLiveOrder.SYNC_LIMIT) {
            prepared = compute()
            return@LaunchedEffect
        }
        // 23.11 etap 1c: ostatni wynik z dysku — ta sama lista: gotowe; zmieniona: ostatnia kolejność od razu, liczenie niżej.
        withContext(Dispatchers.IO) { NiuniaLiveOrder.fromDisk(context, input, hiddenGroups, countryMode) }?.let { prepared = it; if (it.sorted) return@LaunchedEffect }
        // Do czasu wyniku zostaje poprzednia lista (np. 326 kanałów z zapisanej listy), a przy pierwszym wejściu — kolejność listy.
        if (prepared.input.isEmpty()) prepared = NiuniaLiveOrder.unsorted(input, hiddenGroups, countryMode)
        val t0 = android.os.SystemClock.elapsedRealtime()
        prepared = try {
            withContext(Dispatchers.Default) {
                // 22.11: najpierw zapisane indeksy z dysku (kategorie, kraj) — po restarcie bez liczenia od nowa.
                com.nuvio.tv.fork.tv.index.TvIndexStore.awaitLoaded()
                ForgeEpgCategories.awaitStored()
                compute()
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "live tv: kolejność kanałów: ${e.javaClass.simpleName}: ${e.message}")
            NiuniaLiveOrder.unsorted(input, hiddenGroups, countryMode)
        }
        Log.i(TAG, "live tv: kolejność i kategorie ${input.size} kanałów w tle w ${android.os.SystemClock.elapsedRealtime() - t0} ms")
        val done = prepared
        withContext(Dispatchers.IO) { NiuniaLiveOrder.saveToDisk(context, done) }
    }
    val shown = prepared.shown
    val categories = prepared.categories
    // Z-21 (P-25): chip „Kraj i język” — filtr listy kanałów i przewodnika liczony w tle; Z-14/P-26: opcje kanału na długim OK.
    remember { NiuniaLiveRegion.ensure(context); NiuniaGuideChoice.ensure(context) }
    val regionChoice by NiuniaLiveRegion.choice
    val providerNames = remember(state.sources) { state.sources.map { it.name }.filter { it.isNotBlank() } }
    val region = rememberNiuniaRegion(shown, providerNames, regionChoice)
    val base = region.shown
    var regionPanel by remember { mutableStateOf(false) }
    var optionsFor by remember { mutableStateOf<LiveTvChannel?>(null) }
    var optionsFrom by remember { mutableStateOf<String?>(null) }
    val regionChip = remember { FocusRequester() }
    val optionsReturn = remember { FocusRequester() }
    fun refocus(r: FocusRequester, what: String) {
        scope.launch {
            delay(80)
            try { r.requestFocus(); Log.i(TAG, "live tv: fokus wraca na $what") }
            catch (e: IllegalStateException) {
                Log.w(TAG, "live tv: fokus na $what: ${e.javaClass.simpleName}: ${e.message}")
                try { allFocus.requestFocus() } catch (e2: IllegalStateException) { Log.w(TAG, "live tv: fokus „Wszystkie”: ${e2.message}") }
            }
        }
    }
    // 23.11 (TvRam): próbka startowa z pełnej listy — w tle, najwyżej raz na 10 min (albo gdy zmieni się pierwsza strona).
    LaunchedEffect(prepared, snapshot == null, state.currentProgrammes.isNotEmpty()) {
        if (snapshot != null || !prepared.sorted || state.currentProgrammes.isEmpty() || prepared.shown.isEmpty()) return@LaunchedEffect
        // Etap 1b: pierwsza strona + po 10 kanałów każdej kategorii (przełączenie kategorii od razu, zanim wczyta się lista).
        val perCategory = try {
            prepared.categories.flatMap { cat -> ForgeEpgCategories.filter(prepared.shown, cat.key, state.hiddenChannelKeys, "")?.take(com.nuvio.tv.fork.tv.ram.TvSnapshot.PER_CATEGORY).orEmpty() }
        } catch (e: Exception) {
            Log.w(TAG, "live tv: próbka kategorii: ${e.javaClass.simpleName}: ${e.message}"); emptyList()
        }
        val first = (prepared.shown.take(com.nuvio.tv.fork.tv.ram.TvSnapshot.FIRST_PAGE) + perCategory).distinctBy { it.streamUrl }
        val all = repoState.channels
        val fav = repoState.favoriteUrls
        val alertKeys = alerts.map { it.airing.guideKey }
        withContext(Dispatchers.IO) {
            val d = com.nuvio.tv.fork.tv.most.TvBridge.data
            val t = System.currentTimeMillis()
            val extra = alertKeys + d?.records()?.filter { it.state == com.nuvio.tv.fork.tv.WatchState.IN_PROGRESS }?.map { it.guideKey }.orEmpty() +
                d?.let { com.nuvio.tv.fork.tv.ChannelStats.usualForDayPart(it.sessions(), com.nuvio.tv.fork.tv.DayPart.of(t), t) }.orEmpty()
            com.nuvio.tv.fork.tv.ram.TvSnapshot.save(first, all, fav, extra) { LiveTvRepository.schedule(it) }
        }
    }
    val filtered = remember(base, filter, state.favoriteUrls, favShows, alerts) {
        when (val f = filter) {
            is NiuniaLiveFilter.Category -> ForgeEpgCategories.filter(base, f.key, state.hiddenChannelKeys, "") ?: base
            else -> base
        }
    }
    val tiles = remember(filter, filtered, base, state.currentProgrammes, now / (5 * MIN_MS), recent, resume, favShows, alerts) {
        when (filter) {
            NiuniaLiveFilter.All -> recentTiles(base, state.currentProgrammes, now)
            NiuniaLiveFilter.Favourites -> favouriteTiles(base, state.favoriteUrls, state.currentProgrammes, now)
            NiuniaLiveFilter.Continue -> continueTiles(base, now)
            is NiuniaLiveFilter.Category -> categoryTiles(filtered, state.currentProgrammes, now)
        }
    }
    val guideChannels = remember(filter, filtered, base, tiles, state.favoriteUrls) {
        when (filter) {
            NiuniaLiveFilter.Favourites -> favouriteChannels(base, state.favoriteUrls, tiles)
            NiuniaLiveFilter.Continue -> tiles.map { it.channel }.distinctBy { it.streamUrl }.ifEmpty { base }
            else -> filtered
        }
    }
    val numbers = prepared.numbers
    val newKeys = remember(alerts) { alerts.filter { it.kind == AlertKind.NEW_EPISODE }.mapTo(HashSet()) { it.airing.guideKey + "|" + it.airing.startMs } }
    LaunchedEffect(filter.id, tiles.size, guideChannels.size) {
        Log.i(TAG, "live tv: filtr=${filterName(filter)}, kafelki=${tiles.size}, kanały=${guideChannels.size}")
    }

    fun play(channel: LiveTvChannel, programme: LiveTvProgramme?, list: List<LiveTvChannel>) {
        if (launching) return
        val t = System.currentTimeMillis()
        if (programme != null && programme.startEpochMs > t) {
            Log.i(TAG, "live tv: „${programme.title}” jeszcze nie leci (${hhmm(programme.startEpochMs)})")
            Toast.makeText(context, context.getString(R.string.niunia_tv_not_yet, whenLabel(programme.startEpochMs, t)), Toast.LENGTH_SHORT).show()
            return
        }
        val past = programme != null && programme.stopEpochMs <= t
        if (past && !LiveTvCatchupLinks.isPlayable(channel.catchup, programme!!, t)) {
            Log.i(TAG, "live tv: „${programme.title}” nie ma w archiwum kanału ${channel.name}")
            Toast.makeText(context, R.string.niunia_tv_no_archive, Toast.LENGTH_SHORT).show()
            return
        }
        launching = true
        scope.launch {
            try {
                LiveTvRepository.setZapList(list.takeIf { l -> l.any { it.streamUrl == channel.streamUrl } } ?: shown, folderKey = (filter as? NiuniaLiveFilter.Category)?.key)
                val route = if (past) com.nuvio.tv.ui.reshaped.livetv.liveTvCatchupRoute(channel, programme!!, viewModel.profileId)
                else com.nuvio.tv.ui.reshaped.livetv.liveTvPlayerRoute(channel, viewModel.profileId)
                if (route == null) {
                    Log.w(TAG, "live tv: archiwum ${channel.name} „${programme?.title}” — brak linku")
                    Toast.makeText(context, R.string.niunia_tv_no_archive, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                Log.i(TAG, "live tv: odtwarzam ${channel.name}" + (if (past) " archiwum „${programme?.title}”" else " na żywo"))
                viewModel.launchedUrl = channel.streamUrl
                NiuniaLiveReturn.commit(filter, dayOffset)
                NiuniaLivePreview.release("pełny ekran") // P-32: jeden dekoder — podgląd zwolniony przed odtwarzaczem
                onPlay(route)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "live tv: start kanału ${channel.id}: ${e.javaClass.simpleName}: ${e.message}")
                Toast.makeText(context, context.getString(R.string.niunia_live_play_failed), Toast.LENGTH_SHORT).show()
            } finally {
                launching = false
            }
        }
    }

    if (snapshot == null && (!state.hasSource || (state.isLoaded && shown.isEmpty()))) {
        // UX 22.11 (test emulatora): napis odsyłał do „Ustawienia → Telewizja”, a w Niuni takiej pozycji nie było —
        // teraz przycisk od razu otwiera okno źródeł Nuvio (M3U / Xtream / Stalker, adres EPG, telefon przez QR).
        var sources by remember { mutableStateOf(false) }
        val addFocus = remember { FocusRequester() }
        com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(if (!state.hasSource) addFocus else null)
        Box(Modifier.fillMaxSize().background(NiuniaColors.Bg).padding(start = NiuniaDims.Gutter), contentAlignment = Alignment.CenterStart) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(if (!state.hasSource) R.string.niunia_live_no_source else R.string.niunia_live_empty), style = NiuniaText.Meta, color = NiuniaColors.Text70)
                if (!state.hasSource) com.nuvio.tv.skin.niunia.components.NiuniaButton(
                    onClick = { Log.i(TAG, "live tv: pusto → okno źródeł"); sources = true },
                    label = stringResource(R.string.niunia_live_add_source), icon = "plus", focusRequester = addFocus,
                )
            }
        }
        if (sources) com.nuvio.tv.ui.reshaped.livetv.LiveTvSourceDialog(onDismiss = { sources = false })
        return
    }

    BackHandler(enabled = mode != LiveMode.GUIDE || dayPicker) {
        Log.i(TAG, "live tv: Wstecz → przewodnik")
        if (dayPicker) dayPicker = false else mode = LiveMode.GUIDE
    }

    Box(Modifier.fillMaxSize().background(NiuniaColors.Bg)) {
        when (mode) {
            LiveMode.GUIDE -> Guide(
                tiles = tiles, filter = filter, categories = categories, channels = guideChannels, numbers = numbers,
                now = now, dayOffset = dayOffset, guideVersion = state.guideVersion, newKeys = newKeys, allFocus = allFocus, returnTo = returnTo,
                regionActive = regionChoice.active, regionChip = regionChip, onRegion = { Log.i(TAG, "live tv: Kraj i język → panel"); regionPanel = true },
                optionsReturn = optionsReturn, optionsFrom = optionsFrom,
                onOptions = { ch, from -> Log.i(TAG, "live tv: długie OK → opcje kanału ${ch.name}"); optionsFrom = from; optionsFor = ch },
                onFilter = { f ->
                    if (f is NiuniaLiveFilter.Category) NiuniaLiveFilters.used(context, f.key)
                    filter = f
                },
                onPlay = { ch, p -> play(ch, p, guideChannels) },
            )
            LiveMode.DISCOVER -> NiuniaLiveDiscover(shown, state.favoriteUrls, state.currentProgrammes, now) { ch, p -> NiuniaLiveReturn.fromOther(); play(ch, p, shown) }
            LiveMode.SEARCH -> NiuniaLiveSearch(shown, state.currentProgrammes, now) { ch, p -> NiuniaLiveReturn.fromOther(); play(ch, p, shown) }
        }
        TopBar(
            mode = mode, dayLabel = dayLabel(dayOffset, now),
            onSearch = { Log.i(TAG, "live tv: Szukaj"); mode = LiveMode.SEARCH },
            onDay = { dayPicker = true },
            onDiscover = { Log.i(TAG, "live tv: Odkrywaj"); mode = LiveMode.DISCOVER },
        )
        if (dayPicker) DayPicker(dayOffset, now) { d ->
            Log.i(TAG, "live tv: dzień ${dayLabel(d, now)}")
            dayOffset = d; dayPicker = false; mode = LiveMode.GUIDE
        }
        if (regionPanel && mode == LiveMode.GUIDE) NiuniaLiveRegionPanel(region.index) { regionPanel = false; refocus(regionChip, "chip Kraj i język") }
        val opt = optionsFor
        if (opt != null && mode == LiveMode.GUIDE) NiuniaLiveOptionsPanel(opt, state.channels, now,
            onPlay = { c, p -> play(c, p, guideChannels) },
            onClose = { optionsFor = null; refocus(optionsReturn, "kanał ${opt.name}") })
    }
}

private fun filterName(f: NiuniaLiveFilter) = when (f) {
    NiuniaLiveFilter.All -> "wszystkie"
    NiuniaLiveFilter.Favourites -> "ulubione"
    NiuniaLiveFilter.Continue -> "oglądaj dalej"
    is NiuniaLiveFilter.Category -> f.name
}

private fun dayLabel(offset: Int, now: Long): String = when (offset) {
    0 -> "Dziś"
    -1 -> "Wczoraj"
    1 -> "Jutro"
    else -> whenLabel(now + offset * DAY_MS, now).substringBefore(' ')
}

private fun windowStartFor(t: Long): Long = Calendar.getInstance().apply {
    timeInMillis = t; set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); set(Calendar.MINUTE, if (get(Calendar.MINUTE) >= 30) 30 else 0)
}.timeInMillis

private fun dpFor(ms: Long): Dp = HALF_DP * (ms.toFloat() / HALF_MS)

@Composable
private fun Guide(
    tiles: List<NiuniaLiveTile>,
    filter: NiuniaLiveFilter,
    categories: List<NiuniaLiveFilter.Category>,
    channels: List<LiveTvChannel>,
    numbers: Map<String, Int>,
    now: Long,
    dayOffset: Int,
    guideVersion: Int,
    newKeys: Set<String>,
    allFocus: FocusRequester,
    returnTo: NiuniaLiveReturn.Entry? = null,
    regionActive: Boolean,
    regionChip: FocusRequester,
    onRegion: () -> Unit,
    optionsReturn: FocusRequester,
    optionsFrom: String?,
    onOptions: (LiveTvChannel, String) -> Unit,
    onFilter: (NiuniaLiveFilter) -> Unit,
    onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit,
) {
    val minStart = remember(now) { windowStartFor(now) - PAST_DAYS * DAY_MS }
    val backToGuide = returnTo as? NiuniaLiveReturn.Entry.Guide
    var winStart by remember(dayOffset) { mutableLongStateOf(backToGuide?.startMs?.let { maxOf(minStart, windowStartFor(it)) } ?: windowStartFor(now + dayOffset * DAY_MS)) }
    val focusTime = remember(dayOffset) { mutableLongStateOf(backToGuide?.startMs ?: maxOf(now + dayOffset * DAY_MS, 0L)) }
    val tilesState = androidx.compose.foundation.lazy.rememberLazyListState()
    val channelsState = androidx.compose.foundation.lazy.rememberLazyListState()
    val restoreFocus = remember { FocusRequester() }
    val restoreTileKey = (returnTo as? NiuniaLiveReturn.Entry.Tile)?.key?.takeIf { k -> tiles.any { it.key == k } }
    val restoreChannel = backToGuide?.streamUrl?.takeIf { u -> channels.any { it.streamUrl == u } }
    var info by remember { mutableStateOf<Pair<LiveTvChannel, LiveTvProgramme?>?>(null) }
    val tilesFocus = remember { FocusRequester() }
    var userKey by remember { mutableStateOf(false) }
    // Z-21 (P-32): Wstecz z dowolnego miejsca przewodnika = fokus na wybranym chipie filtra, przewodnik wraca do „teraz”
    // i pierwszego kanału; drugie Wstecz (z chipa) = menu (obsługuje PillNavScaffold).
    val selectedChip = remember { FocusRequester() }
    var gridFocused by remember { mutableStateOf(false) }
    val guideScope = androidx.compose.runtime.rememberCoroutineScope()
    BackHandler(enabled = gridFocused) {
        Log.i(TAG, "live tv: Wstecz z przewodnika → chip ${filter.javaClass.simpleName}, „teraz”, 1. kanał")
        winStart = windowStartFor(now)
        focusTime.longValue = now
        guideScope.launch {
            try { channelsState.scrollToItem(0) } catch (e: IllegalStateException) { Log.w(TAG, "live tv: przewiń na 1. kanał: ${e.javaClass.simpleName}: ${e.message}") }
            try { (if (filter == NiuniaLiveFilter.All) allFocus else selectedChip).requestFocus() }
            catch (e: IllegalStateException) { Log.w(TAG, "live tv: fokus na chip: ${e.javaClass.simpleName}: ${e.message}"); try { allFocus.requestFocus() } catch (e2: IllegalStateException) { Log.w(TAG, "live tv: fokus „Wszystkie”: ${e2.message}") } }
        }
    }
    LaunchedEffect(Unit) {
        // Paczka „oczywiste” pkt 2: Wstecz z odtwarzacza → fokus na kafelku albo kanale, z którego program był włączony.
        if (restoreTileKey != null || restoreChannel != null) {
            try {
                if (restoreTileKey != null) tilesState.scrollToItem(tiles.indexOfFirst { it.key == restoreTileKey }.coerceAtLeast(0))
                else channelsState.scrollToItem(channels.indexOfFirst { it.streamUrl == restoreChannel }.coerceAtLeast(0))
                delay(80)
                restoreFocus.requestFocus()
                Log.i(TAG, "live tv: fokus wraca na " + (restoreTileKey?.let { "kafel $it" } ?: "kanał $restoreChannel"))
                // UX 22.11 (test emulatora): po powrocie z kanału fokus po chwili lądował na „Szukaj” u góry (przebudowa ekranu
                // po odtwarzaczu) — dopóki użytkownik nic nie naciśnie, ponów fokus na kanale/kafelku.
                for (wait in longArrayOf(600, 900)) {
                    delay(wait)
                    if (userKey) break
                    try { restoreFocus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "live tv: ponowny fokus: ${e.javaClass.simpleName}: ${e.message}"); break }
                }
                return@LaunchedEffect
            } catch (e: IllegalStateException) {
                Log.w(TAG, "live tv: powrót fokusu: ${e.javaClass.simpleName}: ${e.message}")
            }
        } else if (returnTo != null) Log.i(TAG, "live tv: miejsca powrotu już nie ma na liście ($returnTo)")
        delay(80)
        try { allFocus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "live tv: fokus „Wszystkie”: ${e.javaClass.simpleName}: ${e.message}") }
    }
    // Z-21 (P-32, makieta LiveTVRow): fokus w przewodniku = podgląd kanału na żywo u góry po prawej i opis programu po lewej
    // zamiast rzędu kafelków (jeden odtwarzacz, bez dźwięku; przełącznik niunia_live_preview).
    val previewCtx = LocalContext.current
    val previewOn = remember { NiuniaLivePreview.enabled(previewCtx) }
    val previewFor = info?.takeIf { gridFocused && previewOn }
    if (previewFor != null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) { NiuniaLivePreviewBox(previewFor.first, previewFor.second) }
    // Test JMGO build141: nagłówek przewijał się pod przyciski u góry — kafelki, filtry i podziałka stoją, przewijają się tylko kanały.
    Column(Modifier.fillMaxSize().onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown) userKey = true; false }) {
        Box(Modifier.fillMaxWidth().height(236.dp)) {
            if (previewFor != null) {
                val (pc, pp) = previewFor
                Column(Modifier.padding(start = NiuniaDims.Gutter, top = 74.dp).width(420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.nuvio.tv.skin.niunia.components.NiuniaText(pp?.title ?: com.nuvio.tv.fork.tv.names.ChannelNames.shown(pc.name), variant = com.nuvio.tv.skin.niunia.components.NiuniaTextVariant.TitleScreen, lines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pp != null && isLive(pp, now)) com.nuvio.tv.skin.niunia.components.NiuniaTag(stringResource(R.string.niunia_live_live), com.nuvio.tv.skin.niunia.components.NiuniaTagKind.Live)
                        com.nuvio.tv.skin.niunia.components.NiuniaText(listOfNotNull(pp?.let { "${whenLabel(it.startEpochMs, now)} – ${hhmm(it.stopEpochMs)}" }, com.nuvio.tv.fork.tv.names.ChannelNames.shown(pc.name)).joinToString(" · "),
                            variant = com.nuvio.tv.skin.niunia.components.NiuniaTextVariant.LabelSmall, lines = 1)
                    }
                    pp?.description?.takeIf { it.isNotBlank() }?.let { com.nuvio.tv.skin.niunia.components.NiuniaText(it, variant = com.nuvio.tv.skin.niunia.components.NiuniaTextVariant.BodySmall, tone = com.nuvio.tv.skin.niunia.components.NiuniaTone.Secondary, lines = 2) }
                }
            } else if (tiles.isEmpty()) {
                // UX 22.11 (test emulatora, lista bez przewodnika): „Nic tu jeszcze nie ma.” nad pełną listą kanałów myliło —
                // gdy żaden kanał nie ma programu, podpowiedź, skąd go wziąć.
                val noGuide = remember(guideVersion, channels.size) { LiveTvRepository.uiState.value.currentProgrammes.isEmpty() && channels.isNotEmpty() }
                Text(stringResource(if (noGuide) R.string.niunia_tv_rail_no_guide else R.string.niunia_tv_rail_empty), style = NiuniaText.Body, color = NiuniaColors.Text50, modifier = Modifier.padding(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter, top = 128.dp))
            } else com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                LazyRow(Modifier.padding(top = 74.dp).focusRequester(tilesFocus).focusRestorer(), state = tilesState, contentPadding = PaddingValues(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter, top = 6.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                    items(tiles, key = { it.key }) { t ->
                        NiuniaLiveTileCard(t, longBadge = true, focusRequester = if (t.key == restoreTileKey) restoreFocus else if (optionsFrom == "tile:" + t.key) optionsReturn else null,
                            onFocus = { if (it) info = t.channel to t.programme }, onLongOk = { onOptions(t.channel, "tile:" + t.key) },
                            onOk = { NiuniaLiveReturn.fromTile(t.key); onPlay(t.channel, t.programme) })
                    }
                }
            }
        }
        com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
            // Test JMGO build151 (B3-3): ▲ z „Wszystkie” otwierał menu zamiast kafli nad pigułkami → wprost na kafle (ostatni z fokusem).
            // UX 22.11 (emulator, ścieżka 29): ▲ z pierwszego kanału trafiał w pigułkę najbliżej środka („Informacje”) zamiast
            // w ostatnio wybraną — teraz ostatnia pigułka z fokusem, na początku „Wszystkie”.
            LazyRow(Modifier.fillMaxWidth().height(56.dp).focusRestorer(allFocus).onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown || e.key != Key.DirectionUp || tiles.isEmpty()) return@onPreviewKeyEvent false
                try { tilesFocus.requestFocus(); true } catch (ex: IllegalStateException) { Log.w(TAG, "live tv: ▲ na kafle: ${ex.javaClass.simpleName}: ${ex.message}"); false }
            }, contentPadding = PaddingValues(start = NiuniaDims.Gutter, end = 120.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top) {
                item { FilterPill(stringResource(R.string.niunia_live_all), filter == NiuniaLiveFilter.All, allFocus) { onFilter(NiuniaLiveFilter.All) } }
                item { FilterPill(stringResource(R.string.niunia_tv_favourites), filter == NiuniaLiveFilter.Favourites, if (filter == NiuniaLiveFilter.Favourites) selectedChip else null) { onFilter(NiuniaLiveFilter.Favourites) } }
                item { FilterPill(stringResource(R.string.niunia_tv_continue), filter == NiuniaLiveFilter.Continue, if (filter == NiuniaLiveFilter.Continue) selectedChip else null) { onFilter(NiuniaLiveFilter.Continue) } }
                if (categories.isNotEmpty()) item { Box(Modifier.padding(horizontal = 4.dp, vertical = 7.dp).width(1.dp).height(20.dp).background(NiuniaColors.Line)) }
                items(categories, key = { it.key }) { c -> val sel = (filter as? NiuniaLiveFilter.Category)?.key == c.key; FilterPill(c.name, sel, if (sel) selectedChip else null) { onFilter(c) } }
                // Z-21 (P-25): „Kraj i język” na końcu filtrów; wybrany, gdy filtr kraju albo języka działa.
                item(key = "__region") {
                    com.nuvio.tv.skin.niunia.components.NiuniaButton(onClick = onRegion, label = stringResource(R.string.niunia_tv1_region), icon = "globe",
                        kind = com.nuvio.tv.skin.niunia.components.NiuniaButtonKind.Chip, selected = regionActive, focusRequester = regionChip)
                }
            }
        }
        Ruler(winStart, now)
        if (channels.isEmpty()) {
            Text(stringResource(R.string.niunia_live_empty), style = NiuniaText.Body, color = NiuniaColors.Text50, modifier = Modifier.padding(start = NiuniaDims.Gutter, top = 12.dp))
        }
        Box(Modifier.fillMaxWidth().weight(1f).onFocusChanged { gridFocused = it.hasFocus }) { com.nuvio.tv.skin.niunia.components.NiuniaVerticalScroll(pivot = ROW_H) {
            LazyColumn(Modifier.fillMaxSize(), state = channelsState, contentPadding = PaddingValues(bottom = 56.dp)) {
                items(channels, key = { "${it.sourceId}_${it.id}" }) { ch ->
                    GuideRow(
                        ch, numbers[ch.streamUrl] ?: 0, winStart, minStart, now, guideVersion, focusTime, newKeys,
                        onWindow = { winStart = it },
                        onFocus = { p -> info = ch to p },
                        onOk = { p -> NiuniaLiveReturn.fromGuide(ch.streamUrl, p?.startEpochMs); onPlay(ch, p) },
                        onLong = { onOptions(ch, "ch:" + ch.streamUrl) },
                        requester = if (ch.streamUrl == restoreChannel) restoreFocus else if (optionsFrom == "ch:" + ch.streamUrl) optionsReturn else null,
                    )
                }
            }
        } }
    }
    // Opis programu z fokusem (canvas: linia pod przewodnikiem); przy podglądzie (P-32) opis jest u góry.
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
        if (previewFor != null) return@Box
        val (ch, p) = info ?: return@Box
        Row(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, NiuniaColors.Bg, NiuniaColors.Bg)))
                .padding(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter, top = 18.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (p != null && isLive(p, now)) com.nuvio.tv.skin.niunia.components.NiuniaTag(stringResource(R.string.niunia_live_live), com.nuvio.tv.skin.niunia.components.NiuniaTagKind.Live) // NTag live-badge (5,2:1)
            val text = if (p == null) com.nuvio.tv.fork.tv.names.ChannelNames.shown(ch.name) else listOf(p.title, com.nuvio.tv.fork.tv.names.ChannelNames.shown(ch.name), "${hhmm(p.startEpochMs)} – ${hhmm(p.stopEpochMs)}").joinToString(" · ")
            Text(text, style = NiuniaText.Body, color = NiuniaColors.Text70, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Pigułka filtra (canvas `.pill` 34 dp: zwykła = szary napis, wybrana = białe 24 %, fokus = białe tło). */
@Composable
private fun FilterPill(text: String, selected: Boolean, requester: FocusRequester? = null, onOk: () -> Unit) {
    // NButton chip (design system v2): 36 dp, wybrany 24 %, fokus biały bez skali.
    com.nuvio.tv.skin.niunia.components.NiuniaButton(onClick = onOk, label = text, kind = com.nuvio.tv.skin.niunia.components.NiuniaButtonKind.Chip,
        selected = selected, focusRequester = requester)
}

/** Podziałka co 30 min (słabsza kreska) i znacznik „teraz” nad czerwoną linią. */
@Composable
private fun Ruler(winStart: Long, now: Long) {
    Box(Modifier.fillMaxWidth().height(24.dp).clipToBounds()) {
        var t = winStart
        while (t < winStart + WINDOW_MS) {
            Box(Modifier.offset(x = GRID_X + dpFor(t - winStart), y = 2.dp).width(1.dp).height(14.dp).background(NiuniaColors.Line))
            Text(hhmm(t), style = NiuniaText.Caption.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium), color = NiuniaColors.Text70,
                modifier = Modifier.offset(x = GRID_X + dpFor(t - winStart) + 5.dp))
            t += HALF_MS
        }
        if (now in winStart until winStart + WINDOW_MS) {
            val x = GRID_X + dpFor(now - winStart)
            Box(Modifier.offset(x = x - 5.dp, y = 12.dp).size(12.dp, 12.dp).clip(TriangleShape).background(NiuniaColors.Live))
        }
    }
}

private val TriangleShape = androidx.compose.foundation.shape.GenericShape { size, _ ->
    moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, size.height); close()
}

/**
 * Rząd kanału: jeden element z fokusem (◄► chodzą po programach i przewijają wspólną oś czasu, ▲▼ zmieniają kanał
 * na ten sam czas — [focusTime]). OK = trwający na żywo, miniony z archiwum.
 */
@Composable
private fun GuideRow(
    ch: LiveTvChannel,
    number: Int,
    winStart: Long,
    minStart: Long,
    now: Long,
    guideVersion: Int,
    focusTime: androidx.compose.runtime.MutableLongState,
    newKeys: Set<String>,
    onWindow: (Long) -> Unit,
    onFocus: (LiveTvProgramme?) -> Unit,
    onOk: (LiveTvProgramme?) -> Unit,
    onLong: () -> Unit,
    requester: FocusRequester? = null,
) {
    val winEnd = winStart + WINDOW_MS
    // Z-21 (P-26/P-27): przewodnik wybrany ręcznie w opcjach kanału ("" = bez przewodnika).
    val choiceV = NiuniaGuideChoice.version.intValue
    val gkey = remember(ch.guideKey, choiceV) { NiuniaGuideChoice.keyFor(ch) }
    val gch = remember(ch, gkey) { if (gkey == ch.guideKey) ch else ch.copy(guideKey = gkey) }
    // 23.11 (TvRam): dopóki przewodnik się wczytuje — program z próbki startowej (kanały pierwszej strony).
    // 23.11 etap 1b (przewodnik w bazie): wiersz na ekranie prosi bazę o pełny program kanału; po paczce [dbRows] rośnie.
    val dbCtx = LocalContext.current
    val dbRows by com.nuvio.tv.fork.tv.plus.GuideDbBridge.version.collectAsState()
    LaunchedEffect(gkey) { if (gkey.isNotEmpty()) com.nuvio.tv.fork.tv.plus.GuideDbBridge.want(dbCtx, listOf(gkey)) }
    val schedule = remember(gkey, guideVersion, dbRows) {
        if (gkey.isEmpty()) emptyList()
        else LiveTvRepository.schedule(gkey).ifEmpty { if (com.nuvio.tv.fork.tv.ram.TvRam.enabled) com.nuvio.tv.fork.tv.ram.TvSnapshot.programmesFor(gkey) else emptyList() }
    }
    val visible = remember(schedule, winStart) { schedule.filter { it.stopEpochMs > winStart && it.startEpochMs < winEnd } }
    // 23.11 (przewodnik w bazie, GuideDbBridge): cofnięty w czasie wiersz kanału z catch-up dociąga minione dni z bazy.
    val needCtx = LocalContext.current
    LaunchedEffect(gkey, winStart) { if (gkey.isNotEmpty()) com.nuvio.tv.fork.tv.plus.GuideDbBridge.need(needCtx, gch, winStart) }
    var focused by remember { mutableStateOf(false) }
    var sel by remember { mutableIntStateOf(-1) }
    val logo = forgeChannelLogo(ch)

    fun pick(t: Long): Int = schedule.indexOfFirst { t >= it.startEpochMs && t < it.stopEpochMs }.takeIf { it >= 0 }
        ?: schedule.indexOfFirst { it.startEpochMs >= t }.takeIf { it >= 0 } ?: (schedule.size - 1)

    fun show(i: Int) {
        val p = schedule[i]
        var w = winStart
        while (p.startEpochMs >= w + WINDOW_MS - HALF_MS / 2) w += HALF_MS
        while (p.stopEpochMs <= w + MIN_MS && w > minStart) w -= HALF_MS
        if (p.startEpochMs < w && p.stopEpochMs <= w + HALF_MS && w > minStart) w = maxOf(minStart, windowStartFor(p.startEpochMs))
        if (w != winStart) onWindow(w)
        sel = i
        focusTime.longValue = maxOf(p.startEpochMs, w)
        onFocus(p)
    }

    Box(
        Modifier.fillMaxWidth().height(ROW_H)
            .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
            .onFocusChanged { st ->
                if (st.isFocused != focused) {
                    focused = st.isFocused
                    if (focused) {
                        if (schedule.isEmpty()) onFocus(null) else { sel = pick(maxOf(focusTime.longValue, winStart)); onFocus(schedule[sel]) }
                    }
                }
            }
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown || schedule.isEmpty()) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionRight -> {
                        if (sel < schedule.size - 1) { show(sel + 1); true } else false
                    }
                    Key.DirectionLeft -> {
                        if (sel > 0 && schedule[sel - 1].stopEpochMs > minStart) { show(sel - 1); true } else false
                    }
                    else -> false
                }
            }
            // UX 22.11: długie OK na wierszu kanału = ulubione tak/nie; OK/długie OK jak w całej Niuni (niuniaOk: zegar 520 ms
            // albo flaga systemu, puszczenie po długim OK nie klika).
            // Z-14 (LiveTVOptions): długie OK = opcje kanału (ulubione, przewodnik, ramówka, ukryj); wcześniej tylko ulubione.
            .niuniaOk(ch.name, onLongClick = onLong, onClick = { onOk(schedule.getOrNull(sel)) }),
    ) {
        // Kanał: numer + logo (epg.ovh), bez logo — nazwa pogrubiona.
        Row(
            Modifier.padding(start = NiuniaDims.Gutter).size(CH_W, CELL_H).background(if (focused) NiuniaColors.Fill24 else NiuniaColors.Surface, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(if (number > 0) number.toString() else "", style = NiuniaText.Meta.copy(fontSize = 15.sp), color = NiuniaColors.Text70, modifier = Modifier.width(24.dp), maxLines = 1)
            // Z-21 (P-28): „Niedostępny” pod logo kanału przez 24 h po oknie „… nie odpowiada”.
            val statusV = NiuniaLiveStatus.version.intValue
            val down = remember(ch.streamUrl, statusV) { NiuniaLiveStatus.unavailable(dbCtx, ch.streamUrl) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(if (down) 22.dp else 30.dp), contentAlignment = Alignment.Center) {
                    NiuniaChannelLogo(ch) // Paczka „oczywiste” pkt 5: logo z zapasem, na końcu nazwa
                }
                if (down) Text(stringResource(R.string.niunia_tv2_unavailable), style = com.nuvio.tv.skin.niunia.tokens.NiuniaType.CaptionRegular, color = if (focused) NiuniaColors.Text else NiuniaColors.Text50, maxLines = 1)
            }
        }
        Box(Modifier.padding(start = GRID_X).fillMaxSize().clipToBounds()) {
            if (visible.isEmpty()) {
                // 23.11: w trakcie wczytywania przewodnika „Wczytuję program…”, nie „Brak programu”.
                val loadingGuide = LiveTvRepository.uiState.value.isEpgLoading
                Cell(stringResource(if (loadingGuide) R.string.niunia_live_guide_loading else R.string.niunia_live_no_guide), 0.dp, dpFor(WINDOW_MS), live = false, focused = focused, cut = false, new = false)
            }
            // Test JMGO build152: strzałka ‹ (Edge) zasłaniała początek tytułu programu, który zaczyna się równo z oknem.
            val leftEdge = focused && schedule.any { it.startEpochMs < winStart } && winStart > minStart
            visible.forEach { p ->
                val start = maxOf(p.startEpochMs, winStart)
                val stop = minOf(p.stopEpochMs, winEnd)
                val i = schedule.indexOf(p)
                // Z-1: komórka „teraz” ma u dołu czerwoną linię upływu liczoną od początku widocznej części.
                val frac = if (isLive(p, now) && stop > start) ((now - start).toFloat() / (stop - start)).coerceIn(0f, 1f) else null
                Cell(p.title, dpFor(start - winStart), (dpFor(stop - start) - 4.dp).coerceAtLeast(4.dp), live = isLive(p, now),
                    focused = focused && i == sel, cut = focused && (p.startEpochMs < winStart || leftEdge && p.startEpochMs <= winStart), new = (gkey + "|" + p.startEpochMs) in newKeys,
                    elapsed = frac)
            }
            if (now in winStart until winEnd) Box(Modifier.offset(x = dpFor(now - winStart) - 1.dp).width(2.dp).height(ROW_H).background(NiuniaColors.Live))
            if (focused) {
                // Końce osi przechodzą w tło ze strzałkami, gdy dalej są programy (canvas LiveTVRow).
                if (leftEdge) Edge(left = true)
                if (schedule.any { it.stopEpochMs > winEnd }) Box(Modifier.align(Alignment.CenterEnd)) { Edge(left = false) }
            }
        }
    }
}

@Composable
private fun Edge(left: Boolean) {
    val bg = NiuniaColors.Bg
    Box(
        Modifier.size(64.dp, CELL_H).background(Brush.horizontalGradient(if (left) listOf(bg, bg.copy(alpha = 0f)) else listOf(bg.copy(alpha = 0f), bg))),
        contentAlignment = if (left) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Icon(if (left) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = NiuniaColors.Focus,
            modifier = Modifier.padding(horizontal = 4.dp).size(26.dp))
    }
}

/** Komórka programu (canvas `.ecell`, Z-1): jedna linia, bez ikony ▶; trwający = biały napis i czerwona linia upływu u dołu;
 *  fokus = białe tło (`focus`) i ciemny napis, bez skali, linia zostaje. */
@Composable
private fun Cell(title: String, x: Dp, w: Dp, live: Boolean, focused: Boolean, cut: Boolean, new: Boolean, elapsed: Float? = null) {
    val shape = RoundedCornerShape(6.dp)
    Box(Modifier.offset(x = x).size(w, CELL_H).clip(shape).background(if (focused) NiuniaColors.Focus else NiuniaColors.Surface, shape)) {
        Row(
            Modifier.fillMaxSize().padding(start = if (cut) 40.dp else 12.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (w > 28.dp) Text(title, style = com.nuvio.tv.skin.niunia.tokens.NiuniaType.BodySmall,
                color = when { focused -> com.nuvio.tv.skin.niunia.tokens.NiuniaColor.OnFocus; live -> NiuniaColors.Text; else -> NiuniaColors.Text70 },
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (new && w > 120.dp) com.nuvio.tv.skin.niunia.components.NiuniaTag(stringResource(R.string.niunia_tv_new_chip), com.nuvio.tv.skin.niunia.components.NiuniaTagKind.Chip)
        }
        if (elapsed != null) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(elapsed).height(3.dp).background(NiuniaColors.Live))
    }
}

/** Prawy górny róg, w linii zwiniętej nawigacji: Szukaj · Dziś · Odkrywaj (ikona + napis; bieżący ekran podświetlony). */
@Composable
private fun TopBar(mode: LiveMode, dayLabel: String, onSearch: () -> Unit, onDay: () -> Unit, onDiscover: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = com.nuvio.tv.skin.niunia.tokens.NiuniaSize.NavTop, end = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        TopButton(Icons.Outlined.Search, stringResource(R.string.niunia_tv_search), mode == LiveMode.SEARCH, onSearch)
        TopButton(Icons.Outlined.CalendarToday, dayLabel, false, onDay)
        TopButton(Icons.Outlined.Explore, stringResource(R.string.niunia_tv_discover), mode == LiveMode.DISCOVER, onDiscover)
        com.nuvio.tv.skin.niunia.components.NiuniaClock() // Z-15: zegar na końcu paska akcji
    }
}

@Composable
private fun TopButton(icon: ImageVector, text: String, on: Boolean, onOk: () -> Unit) {
    // NButton small z ikoną (36 dp); bieżący tryb = wypełnienie 36 % (`active`).
    val name = when (icon) {
        Icons.Outlined.Search -> "search"
        Icons.Outlined.Explore -> "discover"
        else -> "calendar"
    }
    com.nuvio.tv.skin.niunia.components.NiuniaButton(onClick = onOk, label = text, icon = name, small = true, active = on)
}

/** „Dziś”: lista dni otwierana przyciskiem (tydzień archiwum wstecz + 3 dni przodu, canvas `.day`). */
@Composable
private fun DayPicker(current: Int, now: Long, onPick: (Int) -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "live tv: fokus dnia: ${e.javaClass.simpleName}: ${e.message}") } }
    Box(Modifier.fillMaxSize().background(Color(0xB3000000)).padding(top = 80.dp), contentAlignment = Alignment.TopEnd) {
        Row(with(com.nuvio.tv.fork.glass.ForkGlass) { Modifier.padding(end = NiuniaDims.Gutter).forkGlass(com.nuvio.tv.fork.glass.ForkGlass.Role.Menu, RoundedCornerShape(16.dp), fallback = NiuniaColors.Surface2, layer = LAYER_BACK) }.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (-PAST_DAYS..3).forEach { d ->
                var focused by remember { mutableStateOf(false) }
                val shape = RoundedCornerShape(NiuniaDims.RadiusCard)
                val day = Calendar.getInstance().apply { timeInMillis = now + d * DAY_MS }
                Column(
                    Modifier.size(56.dp, 60.dp)
                        .niuniaFocusable(shape, scale = NiuniaDims.ScaleButton, lift = false, focusRequester = if (d == current) first else null, onFocus = { focused = it }) { onPick(d) }
                        .background(when { focused -> NiuniaColors.Focus; d == current -> NiuniaColors.Fill24; else -> NiuniaColors.Surface }, shape),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val c = if (focused) NiuniaColors.OnFocus else NiuniaColors.Text
                    Text(dayLabel(d, now), style = NiuniaText.Caption, color = c, maxLines = 1)
                    Text(day.get(Calendar.DAY_OF_MONTH).toString(), style = NiuniaText.Button.copy(fontSize = 20.sp, lineHeight = 26.sp), color = c)
                }
            }
        }
    }
}

/** Wspólne dla Odkrywaj i Szukaj: obliczenie poza wątkiem UI z logiem błędu. */
@Composable
internal fun <T> rememberComputed(vararg keys: Any?, empty: T, what: String, block: suspend () -> T): T {
    var value by remember { mutableStateOf(empty) }
    LaunchedEffect(*keys) {
        value = try {
            withContext(Dispatchers.Default) { block() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "live tv: $what: ${e.javaClass.simpleName}: ${e.message}")
            empty
        }
    }
    return value
}

/** Rząd kafelków z tytułem (Odkrywaj, wyniki Szukaj „Rzędy”). */
@Composable
internal fun NiuniaLiveShelf(title: String, tiles: List<NiuniaLiveTile>, longBadge: Boolean, first: FocusRequester? = null, onPlay: (LiveTvChannel, LiveTvProgramme?) -> Unit) {
    var on by remember { mutableStateOf(false) }
    Column(Modifier.padding(bottom = 16.dp)) {
        Text(title, style = NiuniaText.Row, color = if (on) NiuniaColors.Text else NiuniaColors.Text50, modifier = Modifier.padding(start = NiuniaDims.Gutter))
        com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
            LazyRow(contentPadding = PaddingValues(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter, top = 10.dp, bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap),
                modifier = Modifier.onFocusChanged { on = it.hasFocus }) {
                itemsIndexed(tiles, key = { _, t -> t.key }) { i, t ->
                    NiuniaLiveTileCard(t, longBadge, focusRequester = if (i == 0) first else null) { onPlay(t.channel, t.programme) }
                }
            }
        }
    }
}
