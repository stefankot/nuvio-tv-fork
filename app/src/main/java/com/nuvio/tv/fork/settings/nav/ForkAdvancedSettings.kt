package com.nuvio.tv.fork.settings.nav

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.local.PlayerPreference
import com.nuvio.tv.data.local.PlayerSettings
import com.nuvio.tv.fork.registry.ForkRegistry
import com.nuvio.tv.fork.settings.SimpleLevel
import com.nuvio.tv.fork.settings.UiLevel
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.screens.party.PartyViewModel
import com.nuvio.tv.ui.screens.settings.*
import kotlinx.coroutines.launch

private fun sectionIcon(id: String): ImageVector = when (id) {
    "konto" -> Icons.Outlined.AccountCircle
    "wyglad" -> Icons.Outlined.Palette
    "odtwarzanie" -> Icons.Outlined.PlayCircle
    "napisy" -> Icons.Outlined.Subtitles
    "tv" -> Icons.Outlined.LiveTv
    "zrodla" -> Icons.Outlined.Extension
    else -> Icons.Outlined.Settings
}

/**
 * Tryb Zaawansowany: szyna (Szukaj + kategorie [SettingsTree]) i panel podkategorii. OK na podkategorii:
 * kategoria Nuvio → [onOpenCategory] (pełny ekran, Wstecz wraca tutaj); części (Reshaped, moduły) i poziom → w panelu.
 */
@Composable
internal fun ForkAdvancedSettings(onOpenCategory: (String) -> Unit, onSearch: () -> Unit) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val profileVm: ProfileSettingsViewModel = hiltViewModel()
    val partyVm: PartyViewModel = hiltViewModel()
    val primary by profileVm.isPrimaryProfileActive.collectAsStateWithLifecycle()
    val party by partyVm.party.available.collectAsStateWithLifecycle()
    val registry = remember { ForkRegistry.forContext(c) }
    val sections = remember(primary, party) {
        val cats = visibleSettingsCategories(primary, false, BuildConfig.IS_DEBUG_BUILD, party).map { it.name }.toSet()
        val mods = registry.moduleIds().toSet()
        SettingsTree.visible(cats).map { s ->
            s.copy(leaves = s.leaves.filter { l -> val t = l.target; t !is NavTarget.Parts || t.items.isNotEmpty() || t.modules.any { it in mods } })
        }.filter { it.leaves.isNotEmpty() }
    }
    var selected by rememberSaveable { mutableIntStateOf(ForkSettingsNav.lastSection) }
    var openLeaf by rememberSaveable { mutableStateOf<String?>(null) }
    var lastRow by rememberSaveable { mutableIntStateOf(ForkSettingsNav.lastRow) }
    val section = sections[selected.coerceIn(0, sections.lastIndex)]
    val leaf = section.leaves.firstOrNull { it.id == openLeaf }
    val railRequesters = remember(sections) { List(sections.size + 1) { FocusRequester() } }
    val rowRequesters = remember(section) { List(section.leaves.size) { FocusRequester() } }
    val detailFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { Log.i(ForkSettingsNav.TAG, "tryb zaawansowany: ${sections.size} kategorii, ${sections.sumOf { it.leaves.size }} podkategorii"); withFrameNanos { }; runCatching { (if (ForkSettingsNav.lastRow >= 0 && ForkSettingsNav.backFromCategory) rowRequesters.getOrNull(lastRow) ?: railRequesters[selected + 1] else railRequesters[selected + 1]).requestFocus(); ForkSettingsNav.backFromCategory = false }.onFailure { Log.w(ForkSettingsNav.TAG, "fokus szyny: ${it.javaClass.simpleName}: ${it.message}") } }
    LaunchedEffect(openLeaf) {
        if (openLeaf == null) return@LaunchedEffect
        withFrameNanos { }
        var ok = false
        repeat(20) { if (!ok) { ok = runCatching { detailFocus.requestFocus() }.getOrDefault(false); if (!ok) withFrameNanos { } } }
        if (!ok) { Log.w(ForkSettingsNav.TAG, "fokus panelu $openLeaf: brak, przesuwam w prawo"); focusManager.moveFocus(FocusDirection.Right) }
    }
    BackHandler(enabled = leaf != null) {
        openLeaf = null
        scope.launch { withFrameNanos { }; runCatching { rowRequesters.getOrNull(lastRow)?.requestFocus() } }
    }

    Row(Modifier.fillMaxSize().background(settingsLook().bg).padding(top = 84.dp, start = 40.dp, end = 40.dp)) {
        Column(Modifier.width(216.dp).focusGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (ForkSettingsNav.railSearch(c)) {
                NavRailItem("Szukaj", Icons.Outlined.Search, selected = false, focusRequester = railRequesters[0], onOk = onSearch,
                    modifier = Modifier.focusProperties { right = rowRequesters.firstOrNull() ?: FocusRequester.Default })
                Spacer(Modifier.height(8.dp))
            }
            sections.forEachIndexed { i, s ->
                NavRailItem(
                    s.title, sectionIcon(s.id), selected = i == selected,
                    focusRequester = railRequesters[i + 1],
                    modifier = Modifier.focusProperties { right = if (openLeaf != null) detailFocus else rowRequesters.firstOrNull() ?: FocusRequester.Default },
                    onFocus = { if (selected != i) { selected = i; openLeaf = null } },
                    onOk = { runCatching { (if (openLeaf != null) detailFocus else rowRequesters.firstOrNull())?.requestFocus() } },
                )
            }
        }
        Spacer(Modifier.width(40.dp))
        Column(Modifier.weight(1f).focusGroup()) {
            Text(if (leaf == null) section.title else section.title + "  ›  " + leaf.title, style = ForgeText.head, maxLines = 1, modifier = Modifier.height(26.dp))
            Spacer(Modifier.height(16.dp))
            if (leaf == null) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    section.leaves.forEachIndexed { i, l ->
                        item(key = section.id + l.id) {
                            NavRow(
                                l.title, l.description, focusRequester = rowRequesters[i],
                                modifier = Modifier.focusProperties { left = railRequesters[selected + 1] },
                                onOk = { lastRow = i; ForkSettingsNav.lastSection = selected; ForkSettingsNav.lastRow = i; open(c, l, onOpenCategory) { openLeaf = l.id } },
                            )
                        }
                    }
                }
            } else {
                LeafDetail(leaf, Modifier.focusRequester(detailFocus).focusGroup())
            }
        }
    }
}

private fun open(c: android.content.Context, l: NavLeaf, onOpenCategory: (String) -> Unit, inPanel: () -> Unit) {
    com.nuvio.tv.fork.family.FamilyUsage.feature("ustawienia:" + l.id)
    when (val t = l.target) {
        is NavTarget.Category -> onOpenCategory(t.name)
        is NavTarget.Route -> {
            val r = if (t.route == "addons") com.nuvio.tv.ui.navigation.Screen.AddonManager.route else t.route
            Log.i(ForkSettingsNav.TAG, "otwieram ekran $r")
            val nav = ForkFeatures.open
            if (nav == null) { Log.w(ForkSettingsNav.TAG, "brak nawigacji dla $r"); com.nuvio.tv.fork.ForkLog.say(c, ForkSettingsNav.TAG, "Nie udało się otworzyć") } else nav(r)
        }
        else -> { Log.i(ForkSettingsNav.TAG, "panel ${l.id}"); inPanel() }
    }
}

@Composable
private fun LeafDetail(leaf: NavLeaf, modifier: Modifier) {
    when (val t = leaf.target) {
        is NavTarget.Level -> LevelPicker(modifier)
        is NavTarget.Parts -> {
            val vm: PlaybackSettingsViewModel = hiltViewModel()
            val ps by vm.playerSettings.collectAsStateWithLifecycle(initialValue = PlayerSettings())
            LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                t.modules.forEach { moduleItems(it) }
                t.items.forEach { itemsGroup(it, ps) }
                item(key = "dol") { Spacer(Modifier.height(48.dp)) }
            }
        }
        else -> Unit
    }
}

/** Jedna grupa pozycji Reshaped (te same funkcje, co w kategorii „Nuvio Reshaped”). */
internal fun LazyListScope.itemsGroup(g: ItemsGroup, ps: PlayerSettings) {
    val internal = ps.playerPreference != PlayerPreference.EXTERNAL
    when (g) {
        ItemsGroup.AUTOSYNC -> autoSyncSettingsItems(enabled = internal)
        ItemsGroup.AUTOSYNC_BUBBLE -> autoSyncBubbleSettingsItems(enabled = internal)
        ItemsGroup.SOURCE_RECOVERY -> autoSourceRecoverySettingsItems(ps)
        ItemsGroup.FORMATS -> reshapedFormatsSettingsItems(ps)
        ItemsGroup.SEEKR -> seekrKeySettingsItems()
        ItemsGroup.SEEK_PREVIEW -> seekPreviewSettingsItems()
        ItemsGroup.SUBTITLE_FONT -> subtitleFontSettingsItems()
        ItemsGroup.CONNECTION_SPEED -> connectionSpeedSettingsItems()
        ItemsGroup.PILL_NAV -> pillNavSettingsItems()
        ItemsGroup.NETFLIX_UI -> netflixUiSettingsItems()
        ItemsGroup.LIVE_TV -> liveTvSettingsItems()
        ItemsGroup.RESHAPED_SYNC -> reshapedSyncSettingsItems()
        ItemsGroup.KE_CATALOG -> keCatalogSettingsItems()
        ItemsGroup.DEVICE_TEST -> deviceTestSettingsItems()
        ItemsGroup.FORK_SWITCHES -> forkSwitchesSettingsItems()
        ItemsGroup.SKIN -> skinSettingsItems()
        ItemsGroup.ECHO -> echoPlaybackSettingsItems()
        ItemsGroup.YOUTUBE -> youtubeSettingsItems()
        ItemsGroup.REMOTE_BUTTONS -> remoteButtonsSettingsItems()
        ItemsGroup.ASR -> asrModelSettingsItems()
    }
}

/** Wyłącznik jednego modułu z rejestru, jego czynności i pozycje (jak w „Wszystkich modułach”). */
private fun LazyListScope.moduleItems(id: String) {
    item(key = "modul_$id") {
        val c = LocalContext.current
        val m = remember(id) { ForkRegistry.forContext(c).allModules().firstOrNull { it.id == id } } ?: return@item
        val k = ForkRegistry.switchKey(m.id)
        var on by remember { mutableStateOf(ForkFeatures.enabled(c, k, m.defaultEnabled)) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ToggleSettingsItem(icon = Icons.Default.Tune, title = m.title, subtitle = if (on) "Włączone" else "Wyłączone", isChecked = on,
                onCheckedChange = { on = it; ForkFeatures.set(c, k, it); Log.i(ForkSettingsNav.TAG, "moduł ${m.id}: ${if (it) "włączony" else "wyłączony"}") })
            if (on) m.actions().forEach { a -> SettingsActionRow(title = a.title, subtitle = a.description, onClick = { com.nuvio.tv.fork.family.FamilyUsage.feature("akcja:" + a.title); a.run(c) }) }
            if (on) m.settings().forEach { s ->
                var v by remember(s.key) { mutableStateOf(ForkFeatures.enabled(c, s.key, s.defaultValue)) }
                ToggleSettingsItem(icon = Icons.Default.Tune, title = s.title, subtitle = s.description, isChecked = v,
                    onCheckedChange = { v = it; ForkFeatures.set(c, s.key, it) })
            }
        }
    }
}

/** Wybór poziomu: NOOB (jeden ekran), Prosty (8 krótkich kategorii), Zaawansowany (to drzewo). */
@Composable
internal fun LevelPicker(modifier: Modifier = Modifier, current: UiLevel = UiLevel.ADVANCED, onChanged: () -> Unit = {}) {
    val c = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LevelLabels.forEach { (level, title, desc) ->
            NavRow(title, desc, value = if (level == current) "Teraz" else null, chevron = false, onOk = {
                if (level == current) return@NavRow
                Log.i(ForkSettingsNav.TAG, "poziom ustawień: $current → $level")
                if (level == UiLevel.SIMPLE) SimpleLevel.toSimple(c) else scope.launch { SimpleLevel.set(c, level); onChanged() }
            })
        }
    }
}

internal val LevelLabels = listOf(
    Triple(UiLevel.SIMPLE, "NOOB", "Tylko najważniejsze, wszystko na jednym ekranie"),
    Triple(UiLevel.ESSENTIAL, "Prosty", "Najczęstsze ustawienia w kilku kategoriach"),
    Triple(UiLevel.ADVANCED, "Zaawansowany", "Wszystko: Reshaped, Forge i moduły w kategoriach"),
)
