package com.nuvio.tv.skin.niunia.settings

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.R
import com.nuvio.tv.data.local.PlayerSettings
import com.nuvio.tv.fork.registry.ForkRegistry
import com.nuvio.tv.fork.settings.SimpleLevel
import com.nuvio.tv.fork.settings.UiLevel
import com.nuvio.tv.fork.settings.nav.itemsGroup
import com.nuvio.tv.fork.settings.tree.SettingsTreeJump
import com.nuvio.tv.fork.settings.tree.SettingsTreeRuntime
import com.nuvio.tv.fork.settings.tree.SettingsTreeSearch
import com.nuvio.tv.fork.settings.tree.SettingsTreeSpec
import com.nuvio.tv.fork.settings.tree.TreeCategory
import com.nuvio.tv.fork.settings.tree.TreeItem
import com.nuvio.tv.fork.settings.tree.TreeLevel
import com.nuvio.tv.fork.settings.tree.TreeMode
import com.nuvio.tv.fork.settings.tree.TreeTarget
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaRowKind
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.components.NiuniaTrailing
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.ui.screens.party.PartyViewModel
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import com.nuvio.tv.ui.screens.settings.PlaybackSettingsViewModel
import com.nuvio.tv.ui.screens.settings.ProfileSettingsViewModel
import com.nuvio.tv.ui.screens.settings.visibleSettingsCategories
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = SettingsTreeRuntime.TAG

/** Wiersz panelu: nagłówek grupy, pozycja, „Więcej (n)”/„Mniej”, „Pokaż wszystkie ustawienia”. */
private sealed interface PanelRow {
    val key: String
    data class Head(val title: String) : PanelRow { override val key get() = "h:$title" }
    data class Item(val item: TreeItem) : PanelRow { override val key get() = item.id }
    data class More(val group: String, val count: Int, val open: Boolean) : PanelRow { override val key get() = "more:$group" }
    data object ShowAll : PanelRow { override val key get() = "show_all" }
}

/**
 * Ustawienia Niuni (23.11): jedno drzewo ([SettingsTreeSpec]) dla wszystkich poziomów. Po lewej „Szukaj” i 7 kategorii
 * (fokus startowy na kategorii, panel zmienia się przy przejściu ▲▼), po prawej grupy z pozycjami; „Więcej (n)” rozwija
 * rzadsze ustawienia w tej samej grupie; tryb prosty = tylko pozycje NOOB. Na dole opis pozycji z fokusem.
 * ◄► szyna ↔ panel, Wstecz: szukanie/panel części → zamknij, panel → szyna, szyna → menu Niuni.
 */
@Composable
fun NiuniaSettingsTree(navigate: (String) -> Unit) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val profileVm: ProfileSettingsViewModel = hiltViewModel()
    val partyVm: PartyViewModel = hiltViewModel()
    val primary by profileVm.isPrimaryProfileActive.collectAsStateWithLifecycle()
    val party by partyVm.party.available.collectAsStateWithLifecycle()
    val levelChanges by SimpleLevel.changes.collectAsState()
    var rev by remember { mutableIntStateOf(0) }
    var treeRev by remember { mutableIntStateOf(0) }
    // UX JMGO (ręczne testy 24.11): „Rozmiar napisów” pokazywał „—”, dopóki nie wybrano go tutaj — odczyt z odtwarzacza.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if ((ForkFeatures.str(c, "tree_subtitle_size", "-1").toIntOrNull() ?: -1) >= 0) return@LaunchedEffect
        try {
            val ch = com.nuvio.tv.fork.sources.SourceChoice.subtitleSizeChoice(c)
            ForkFeatures.setStr(c, "tree_subtitle_size", com.nuvio.tv.fork.sources.SubtitleSizeChoice.entries.indexOf(ch).toString())
            Log.i(TAG, "rozmiar napisów z odtwarzacza: ${ch.title}"); rev++
        } catch (e: Exception) { Log.w(TAG, "rozmiar napisów z odtwarzacza: ${e.javaClass.simpleName}: ${e.message}") }
    }
    val visibleCats = remember(primary, party) { visibleSettingsCategories(primary, false, BuildConfig.IS_DEBUG_BUILD, party).map { it.name }.toSet() }
    val full = remember(visibleCats, treeRev) { SettingsTreeRuntime.tree(c, visibleCats) }
    var showFull by rememberSaveable { mutableStateOf(false) } // wynik szukania spoza trybu prostego: pełne drzewo na tę wizytę
    val mode = remember(levelChanges, showFull) { if (SimpleLevel.isSimple(c) && !showFull) TreeMode.SIMPLE else TreeMode.FULL }
    val tree = remember(full, mode) { SettingsTreeSpec.forMode(full, mode) }
    var selected by rememberSaveable { mutableIntStateOf(SettingsTreeRuntime.lastCategory) }
    val sel = selected.coerceIn(0, (tree.size - 1).coerceAtLeast(0))
    val cat = tree.getOrNull(sel) ?: return
    var expanded by rememberSaveable { mutableStateOf(listOf<String>()) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var partsOpen by rememberSaveable { mutableStateOf<String?>(null) }
    var focusedDesc by remember { mutableStateOf<String?>(null) }
    var panelFocused by remember { mutableStateOf(false) }
    var tvSources by remember { mutableStateOf(false) }
    var pendingFocus by remember { mutableStateOf(SettingsTreeRuntime.lastItem) }

    val railRequesters = remember(tree.size) { List(tree.size + 1) { FocusRequester() } }
    val rows = remember(cat, mode, expanded) {
        buildList {
            cat.groups.forEach { g ->
                val key = cat.id + "/" + g.id
                val items = SettingsTreeSpec.itemsFor(g, mode, key in expanded)
                if (items.isEmpty() && SettingsTreeSpec.moreCount(g, mode) == 0) return@forEach
                add(PanelRow.Head(g.title))
                items.forEach { add(PanelRow.Item(it)) }
                SettingsTreeSpec.moreCount(g, mode).takeIf { it > 0 }?.let { add(PanelRow.More(key, it, key in expanded)) }
            }
            if (mode == TreeMode.SIMPLE) add(PanelRow.ShowAll)
        }
    }
    val rowRequesters = remember(rows) { rows.associate { it.key to FocusRequester() } }
    val firstRow = rows.firstOrNull { it !is PanelRow.Head }?.key
    // Osobna lista na kategorię: wspólna trzymała przewinięcie z poprzedniej kategorii, pierwszy wiersz nowej nie był narysowany
    // i ► z szyny nie wchodził do panelu (JMGO ust3).
    val listState = remember(cat.id) { androidx.compose.foundation.lazy.LazyListState() }
    val partsFocus = remember { FocusRequester() }

    if (tvSources) com.nuvio.tv.ui.reshaped.livetv.LiveTvSourceDialog(onDismiss = { tvSources = false; pendingFocus = SettingsTreeRuntime.lastItem })
    com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(railRequesters.getOrNull(sel + 1))

    LaunchedEffect(Unit) {
        Log.i(TAG, "ekran: tryb ${mode.name}, kategoria ${cat.title}, ${rows.count { it is PanelRow.Item }} pozycji w panelu, fokus → " +
            (pendingFocus?.let { "pozycja $it" } ?: "kategoria"))
        if (pendingFocus == null) {
            withFrameNanos { }
            try { railRequesters[sel + 1].requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "fokus szyny: ${e.javaClass.simpleName}: ${e.message}") }
        }
    }
    // Powrót na pozycję (po ekranie, oknie, kategorii Nuvio, wyniku szukania): przewiń do niej, potem fokus.
    LaunchedEffect(pendingFocus, rows, searching, partsOpen) {
        val id = pendingFocus ?: return@LaunchedEffect
        if (searching || partsOpen != null) return@LaunchedEffect
        val idx = rows.indexOfFirst { it.key == id }
        if (idx < 0) { pendingFocus = null; return@LaunchedEffect }
        try { listState.scrollToItem((idx - 1).coerceAtLeast(0)) } catch (e: IllegalStateException) { Log.w(TAG, "przewinięcie: ${e.javaClass.simpleName}: ${e.message}") }
        var ok = false
        repeat(20) { if (!ok) { withFrameNanos { }; ok = try { rowRequesters[id]?.requestFocus() == true } catch (e: IllegalStateException) { false } } }
        Log.i(TAG, "fokus na pozycji $id: ${if (ok) "tak" else "NIE"}")
        pendingFocus = null
    }
    LaunchedEffect(partsOpen) {
        if (partsOpen == null) return@LaunchedEffect
        var ok = false
        repeat(20) { if (!ok) { withFrameNanos { }; ok = try { partsFocus.requestFocus() == true } catch (e: IllegalStateException) { false } } }
        if (!ok) { Log.w(TAG, "panel części: brak fokusu, przesuwam w prawo"); focusManager.moveFocus(FocusDirection.Right) }
    }
    BackHandler(enabled = searching) { searching = false; Log.i(TAG, "szukanie: zamknięte"); scope.launch { withFrameNanos { }; runCatching { railRequesters[0].requestFocus() }.onFailure { Log.w(TAG, "fokus Szukaj: ${it.javaClass.simpleName}: ${it.message}") } } }
    BackHandler(enabled = !searching && partsOpen != null) { pendingFocus = partsOpen; partsOpen = null }
    BackHandler(enabled = !searching && partsOpen == null && panelFocused) {
        try { railRequesters[sel + 1].requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "Wstecz → szyna: ${e.javaClass.simpleName}: ${e.message}") }
    }

    fun mark(item: TreeItem) { SettingsTreeRuntime.lastItem = item.id; SettingsTreeRuntime.lastCategory = sel }

    fun activate(item: TreeItem) {
        mark(item)
        com.nuvio.tv.fork.family.FamilyUsage.feature("ustawienia:" + item.id)
        when (val t = item.target) {
            is TreeTarget.Toggle -> {
                val on = !ForkFeatures.enabled(c, t.key, t.default)
                ForkFeatures.set(c, t.key, on); Log.i(TAG, "${item.title}: ${t.key} = $on"); rev++
                if (t.key == SettingsTreeSpec.KEY && !on) com.nuvio.tv.fork.ForkLog.say(c, TAG, "Dawne ustawienia pokażą się po ponownym wejściu")
            }
            is TreeTarget.ModuleSwitch -> {
                val k = ForkRegistry.switchKey(t.module); val on = !ForkFeatures.enabled(c, k, t.default)
                ForkFeatures.set(c, k, on); Log.i(TAG, "moduł ${t.module}: ${if (on) "włączony" else "wyłączony"}"); treeRev++
            }
            is TreeTarget.Action -> { Log.i(TAG, "czynność ${t.module}: ${t.title}"); if (SettingsTreeRuntime.runAction(c, t.module, t.title)) scope.launch { delay(400); rev++ } }
            is TreeTarget.Route -> { Log.i(TAG, "ekran ${t.route} (${item.title})"); navigate(t.route) }
            is TreeTarget.Base -> SettingsTreeRuntime.openBase(t.category)
            is TreeTarget.Playback -> { Log.i(TAG, "Odtwarzanie, sekcja ${t.section}"); SettingsTreeJump.set(t.section); navigate(com.nuvio.tv.ui.navigation.Screen.PlaybackSettings.route) }
            is TreeTarget.Parts -> { Log.i(TAG, "panel części ${item.id}: ${t.groups}"); partsOpen = item.id }
            is TreeTarget.Special -> special(c, t.id, navigate, openSources = { tvSources = true }, setLevel = { simple ->
                if (simple) SimpleLevel.toSimple(c) else fullLevel(c)
                showFull = false; Log.i(TAG, "tryb ustawień → ${if (simple) "prosty" else "pełny"}")
            }).also { scope.launch { delay(400); rev++ } }
        }
    }

    Row(
        Modifier.fillMaxSize().background(NiuniaColor.Bg).padding(start = NiuniaSpacing.Space40, end = NiuniaSpacing.Space40),
        horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space40),
    ) {
        // Szyna: Szukaj + kategorie.
        Column(Modifier.width(RAIL_W).fillMaxHeight().padding(top = NiuniaSize.ContentTop).focusGroup(), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4)) {
            NiuniaListRow(stringResource(R.string.niunia_tv_search), kind = NiuniaRowKind.Nav, icon = "search", selected = searching,
                focusRequester = railRequesters[0],
                onFocus = { if (it) { focusedDesc = null; panelFocused = false } },
                onClick = { Log.i(TAG, "szukanie: otwarte"); searching = true; partsOpen = null })
            Spacer(Modifier.height(NiuniaSpacing.Space12))
            tree.forEachIndexed { i, k ->
                NiuniaListRow(k.title, kind = NiuniaRowKind.Nav, icon = k.icon, selected = i == sel && !searching,
                    focusRequester = railRequesters[i + 1],
                    modifier = Modifier.focusProperties { right = if (partsOpen != null) partsFocus else firstRow?.let { rowRequesters[it] } ?: FocusRequester.Default },
                    onFocus = { f ->
                        if (f) {
                            panelFocused = false; focusedDesc = k.description
                            if (selected != i || searching || partsOpen != null) { selected = i; searching = false; partsOpen = null; SettingsTreeRuntime.lastCategory = i }
                        }
                    },
                    onClick = { firstRow?.let { k -> runCatching { rowRequesters[k]?.requestFocus() }.onFailure { Log.w(TAG, "fokus panelu: ${it.javaClass.simpleName}: ${it.message}") } } })
            }
        }
        // Panel.
        Column(Modifier.weight(1f).fillMaxHeight().padding(top = NiuniaSize.ContentTop)) {
            when {
                searching -> SearchPanel(full, query, onQuery = { query = it }, onOpen = { hit ->
                    val inView = SettingsTreeSpec.forMode(full, mode).indexOfFirst { it.id == hit.category.id }
                    val visibleNow = inView >= 0 && (mode == TreeMode.FULL || hit.item.level == TreeLevel.NOOB)
                    if (!visibleNow) showFull = true
                    val target = SettingsTreeSpec.forMode(full, if (visibleNow) mode else TreeMode.FULL).indexOfFirst { it.id == hit.category.id }
                    if (hit.item.level == TreeLevel.MORE) expanded = (expanded + (hit.category.id + "/" + hit.group.id)).distinct()
                    Log.i(TAG, "szukanie: „$query” → ${hit.category.title} › ${hit.group.title} › ${hit.item.title}${if (!visibleNow) " (pełne drzewo)" else ""}")
                    selected = target.coerceAtLeast(0); SettingsTreeRuntime.lastCategory = selected
                    searching = false; pendingFocus = hit.item.id
                })
                partsOpen != null -> {
                    val item = cat.groups.flatMap { it.items }.firstOrNull { it.id == partsOpen }
                    val groups = (item?.target as? TreeTarget.Parts)?.groups.orEmpty()
                    NiuniaText(cat.title + "  ›  " + (item?.title ?: ""), variant = NiuniaTextVariant.TitleSection, lines = 1)
                    Spacer(Modifier.height(NiuniaSpacing.Space12))
                    val vm: PlaybackSettingsViewModel = hiltViewModel()
                    val ps by vm.playerSettings.collectAsStateWithLifecycle(initialValue = PlayerSettings())
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().focusRequester(partsFocus).focusGroup()
                        .focusProperties { left = railRequesters[sel + 1] },
                        verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12),
                        contentPadding = PaddingValues(bottom = NiuniaSpacing.Space40)) {
                        groups.forEach { itemsGroup(it, ps) }
                    }
                }
                else -> {
                    NiuniaText(cat.title, variant = NiuniaTextVariant.TitleSection, lines = 1)
                    Spacer(Modifier.height(NiuniaSpacing.Space8))
                    LazyColumn(
                        Modifier.weight(1f).fillMaxWidth().onFocusChanged { panelFocused = it.hasFocus },
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4),
                        contentPadding = PaddingValues(bottom = NiuniaSpacing.Space24),
                    ) {
                        items(rows.size, key = { rows[it].key }) { i ->
                            val r = rows[i]
                            // ◄ zawsze na kategorię; ▼ z ostatniej pozycji zostaje w panelu (wcześniej skakał na szynę i zmieniał kategorię).
                            val left = Modifier.focusProperties { left = railRequesters[sel + 1]; if (i == rows.lastIndex) down = FocusRequester.Cancel }
                            when (r) {
                                is PanelRow.Head -> NiuniaListRow(r.title, onClick = null, kind = NiuniaRowKind.Header)
                                is PanelRow.Item -> {
                                    val v = remember(rev, r.item.id) { value(c, r.item) }
                                    NiuniaListRow(r.item.title, kind = NiuniaRowKind.Setting, value = v.text, trailing = v.trailing, enabled = v.enabled,
                                        modifier = left, focusRequester = rowRequesters[r.key],
                                        onFocus = { if (it) focusedDesc = r.item.description },
                                        onClick = { activate(r.item) })
                                }
                                is PanelRow.More -> NiuniaListRow(
                                    if (r.open) stringResource(R.string.niunia_ds_less) else stringResource(R.string.settings_tree_more, r.count),
                                    kind = NiuniaRowKind.Setting, trailing = NiuniaTrailing.None, modifier = left, focusRequester = rowRequesters[r.key],
                                    onFocus = { if (it) focusedDesc = c.getString(R.string.settings_tree_more_desc) },
                                    onClick = {
                                        expanded = if (r.open) expanded - r.group else expanded + r.group
                                        Log.i(TAG, "grupa ${r.group}: ${if (r.open) "zwinięta" else "rozwinięta (${r.count})"}")
                                    })
                                PanelRow.ShowAll -> NiuniaListRow(stringResource(R.string.settings_tree_show_all), kind = NiuniaRowKind.Setting,
                                    trailing = NiuniaTrailing.Chevron, modifier = left, focusRequester = rowRequesters[r.key],
                                    onFocus = { if (it) focusedDesc = c.getString(R.string.settings_tree_show_all_desc) },
                                    onClick = { Log.i(TAG, "tryb prosty → pełny (Pokaż wszystkie)"); fullLevel(c) })
                            }
                        }
                    }
                    // Opis pozycji z fokusem (jedno zdanie).
                    Box(Modifier.fillMaxWidth().height(DESC_H).padding(top = NiuniaSpacing.Space8)) {
                        focusedDesc?.takeIf { it.isNotBlank() }?.let { NiuniaText(it, variant = NiuniaTextVariant.CaptionRegular, tone = NiuniaTone.Secondary, lines = 2) }
                    }
                }
            }
        }
    }
}

private val RAIL_W = androidx.compose.ui.unit.Dp(240f)
private val DESC_H = androidx.compose.ui.unit.Dp(48f)

/** Szukanie: pole, klawiatura Niuni, wyniki (cała treść drzewa, także ukryta w trybie prostym i pod „Więcej”). */
@Composable
private fun SearchPanel(full: List<TreeCategory>, query: String, onQuery: (String) -> Unit,
                        onOpen: (com.nuvio.tv.fork.settings.tree.TreeHit) -> Unit) {
    val index = remember(full) { SettingsTreeSearch.index(full) }
    val hits = remember(index, query) { SettingsTreeSearch.search(index, query) }
    val firstKey = remember { FocusRequester() }
    var digits by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { withFrameNanos { }; runCatching { firstKey.requestFocus() }.onFailure { Log.w(TAG, "fokus klawiatury: ${it.javaClass.simpleName}: ${it.message}") } }
    LaunchedEffect(query) { if (query.isNotBlank()) { delay(600); Log.i(TAG, "szukanie: „$query” → ${hits.size} wyników") } }
    NiuniaText(query.ifEmpty { stringResource(R.string.settings_tree_search_hint) }, variant = NiuniaTextVariant.TitleSection,
        tone = if (query.isEmpty()) NiuniaTone.Tertiary else NiuniaTone.Primary, lines = 1)
    Spacer(Modifier.height(NiuniaSpacing.Space12))
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space24)) {
        Column(Modifier.width(NiuniaSize.SplitCol)) {
            com.nuvio.tv.skin.niunia.search.Keyboard(digits = digits, firstKey = firstKey, onMode = { digits = it },
                onKey = { onQuery(query + it) }, onSpace = { onQuery("$query ") }, onDelete = { onQuery(query.dropLast(1)) }, onClear = { onQuery("") })
        }
        Column(Modifier.weight(1f)) {
            when {
                query.isBlank() -> Unit
                hits.isEmpty() -> NiuniaText(stringResource(R.string.settings_tree_search_none, query), variant = NiuniaTextVariant.Body, tone = NiuniaTone.Secondary)
                else -> {
                    NiuniaText(stringResource(R.string.settings_tree_search_count, hits.size), variant = NiuniaTextVariant.CaptionRegular, tone = NiuniaTone.Tertiary)
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4)) {
                        items(hits.size, key = { hits[it].item.id }) { i ->
                            val h = hits[i]
                            NiuniaListRow(h.item.title, kind = NiuniaRowKind.Track, supporting = SettingsTreeSearch.subtitle(h),
                                trailing = NiuniaTrailing.Chevron, onClick = { onOpen(h) })
                        }
                    }
                }
            }
        }
    }
}

private class RowValue(val text: String?, val trailing: NiuniaTrailing, val enabled: Boolean = true)

/** Pełne drzewo bez zmiany trybu Nuvio (Podstawowy/Zaawansowany) — zmienia tylko widok ustawień forka. */
private fun fullLevel(c: Context) {
    ForkFeatures.setStr(c, com.nuvio.tv.fork.settings.FirstRunDefaults.LEVEL_KEY, UiLevel.ADVANCED.name)
    SimpleLevel.changes.value++
}

private fun forge(c: Context) = c.getSharedPreferences("forge_prefs", Context.MODE_PRIVATE)
private fun switch(on: Boolean) = RowValue(null, if (on) NiuniaTrailing.SwitchOn else NiuniaTrailing.SwitchOff)

private val LOGO = listOf("Z programu, potem z listy", "Tylko z programu", "Tylko z listy", "Własny adres")
private val ANSWERS = listOf(
    com.nuvio.tv.fork.sources.SourcePresets.PL_ALWAYS to "Zawsze po polsku",
    com.nuvio.tv.fork.sources.SourcePresets.PL_IF_GOOD to "Po polsku przy dobrej jakości",
    com.nuvio.tv.fork.sources.SourcePresets.ORIGINAL to "Oryginał z napisami",
)
private val SIZES = listOf("Napisy: małe" to "Małe", "Napisy: średnie" to "Średnie", "Napisy: duże" to "Duże")
private val NIGHT = listOf("OFF" to ("Tryb nocny: wyłączony" to "Wyłączony"), "SCHEDULE" to ("Tryb nocny: według godzin 22:00–07:00" to "Według godzin"), "ON" to ("Tryb nocny: włączony" to "Włączony"))
private val START = listOf("HOME" to ("Po uruchomieniu pokaż: ekran główny" to "Ekran główny"), "TV" to ("Po uruchomieniu pokaż: telewizję" to "Telewizja"),
    "LAST_CHANNEL" to ("Po uruchomieniu pokaż: ostatni kanał" to "Ostatni kanał"))

/** Wartość wiersza: przełącznik, tekst wyboru albo strzałka (ekran, okno, panel). */
private fun value(c: Context, item: TreeItem): RowValue = when (val t = item.target) {
    is TreeTarget.Toggle -> switch(ForkFeatures.enabled(c, t.key, t.default))
    is TreeTarget.ModuleSwitch -> switch(ForkFeatures.enabled(c, ForkRegistry.switchKey(t.module), t.default))
    is TreeTarget.Action -> RowValue(null, NiuniaTrailing.None)
    is TreeTarget.Special -> when (t.id) {
        "level" -> RowValue(c.getString(if (SimpleLevel.isSimple(c)) R.string.settings_tree_level_simple else R.string.settings_tree_level_full), NiuniaTrailing.None)
        "logo_mode" -> RowValue(LOGO[forge(c).getInt("logo_mode", 0).coerceIn(0, 3)], NiuniaTrailing.None)
        "home_youtube" -> switch(forge(c).getBoolean("home_youtube", true))
        "reminder_card" -> switch(forge(c).getBoolean("reminder_card", true))
        "tv_layout" -> RowValue(if (forge(c).getBoolean("tv_browse_b", false)) "Przeglądaj i przewodnik" else "Przewodnik z ostatnimi", NiuniaTrailing.None)
        "sources_answer" -> com.nuvio.tv.fork.sources.SourceChoice.answer(c).let { a ->
            RowValue(ANSWERS.firstOrNull { it.first == a }?.second ?: if (a.isEmpty()) c.getString(R.string.settings_tree_unknown) else "Własne", NiuniaTrailing.None)
        }
        "subtitle_size" -> RowValue(SIZES.getOrNull(ForkFeatures.str(c, "tree_subtitle_size", "-1").toIntOrNull() ?: -1)?.second ?: c.getString(R.string.settings_tree_unknown), NiuniaTrailing.None)
        "night_mode" -> RowValue(NIGHT.firstOrNull { it.first == ForkFeatures.str(c, "night_mode", "OFF") }?.second?.second ?: "Wyłączony", NiuniaTrailing.None)
        "start_page" -> RowValue(START.firstOrNull { it.first == ForkFeatures.str(c, "launcher_start_page", "HOME") }?.second?.second ?: "Ekran główny", NiuniaTrailing.None)
        // Szkło (fork/glass): na API < 33 wyszarzone z dopiskiem „Niedostępne na tym telewizorze”.
        "glass" -> RowValue(com.nuvio.tv.fork.glass.ForkGlass.label(c), NiuniaTrailing.None, enabled = com.nuvio.tv.fork.glass.ForkGlass.supported)
        "start_permission" -> RowValue(if (com.nuvio.tv.skin.kicia.ForgeSystemOptions.canStartFromBackground(c)) "Jest" else "Brak", NiuniaTrailing.Chevron)
        else -> RowValue(null, NiuniaTrailing.Chevron)
    }
    else -> RowValue(null, NiuniaTrailing.Chevron)
}

/** Pozycje „specjalne”: okna, wybory z wartością (kolejne OK = następna wartość). */
private fun special(c: Context, id: String, navigate: (String) -> Unit, openSources: () -> Unit, setLevel: (Boolean) -> Unit) {
    fun cycle(options: List<Pair<String, Pair<String, String>>>, current: String) {
        val i = options.indexOfFirst { it.first == current }
        val next = options[(i + 1).mod(options.size)]
        Log.i(TAG, "wybór $id: $current → ${next.first}")
        SettingsTreeRuntime.runAction(c, if (id == "night_mode") "night" else "launcher", next.second.first)
    }
    when (id) {
        "tv_sources" -> { Log.i(TAG, "okno źródeł telewizji"); openSources() }
        "tv_search" -> { com.nuvio.tv.skin.niunia.livetv.NiuniaLiveStart.openSearch = true; navigate(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE) }
        "level" -> setLevel(!SimpleLevel.isSimple(c))
        "logo_mode" -> { val m = (forge(c).getInt("logo_mode", 0) + 1) % 4; forge(c).edit().putInt("logo_mode", m).apply(); com.nuvio.tv.skin.kicia.ForgePrefs.setLogoMode(m) }
        "home_youtube" -> { val v = !forge(c).getBoolean("home_youtube", true); forge(c).edit().putBoolean("home_youtube", v).apply(); com.nuvio.tv.skin.kicia.ForgePrefs.setHomeYouTube(v) }
        "reminder_card" -> { val v = !forge(c).getBoolean("reminder_card", true); forge(c).edit().putBoolean("reminder_card", v).apply(); com.nuvio.tv.skin.kicia.ForgePrefs.setReminderCard(v) }
        "tv_layout" -> { val v = !forge(c).getBoolean("tv_browse_b", false); forge(c).edit().putBoolean("tv_browse_b", v).apply(); com.nuvio.tv.skin.kicia.ForgePrefs.setTvBrowseB(v) }
        "sources_answer" -> {
            val i = ANSWERS.indexOfFirst { it.first == com.nuvio.tv.fork.sources.SourceChoice.answer(c) }
            val next = ANSWERS[(i + 1).mod(ANSWERS.size)]
            Log.i(TAG, "polski czy jakość → ${next.first}")
            SettingsTreeRuntime.runAction(c, "sources", next.second)
        }
        "subtitle_size" -> {
            val i = ((ForkFeatures.str(c, "tree_subtitle_size", "-1").toIntOrNull() ?: -1) + 1).mod(SIZES.size)
            if (SettingsTreeRuntime.runAction(c, "sources", SIZES[i].first)) ForkFeatures.setStr(c, "tree_subtitle_size", i.toString())
        }
        "night_mode" -> cycle(NIGHT, ForkFeatures.str(c, "night_mode", "OFF"))
        "start_page" -> cycle(START, ForkFeatures.str(c, "launcher_start_page", "HOME"))
        "start_permission" -> com.nuvio.tv.skin.kicia.ForgeSystemOptions.openOverlayPermission(c)
        "glass" -> com.nuvio.tv.fork.glass.ForkGlass.cycle(c)
        else -> Log.w(TAG, "nieznana pozycja specjalna $id")
    }
}
