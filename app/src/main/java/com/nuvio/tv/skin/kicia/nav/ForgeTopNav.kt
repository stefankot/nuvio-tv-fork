package com.nuvio.tv.skin.kicia.nav

import androidx.compose.material.icons.outlined.Apps
import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.nuvio.tv.skin.kicia.components.forgeGlass
import com.nuvio.tv.skin.kicia.components.forgeSoftBackdrop
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE
import com.nuvio.tv.ui.components.ProfileAvatarCircle
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.components.ForgeSheetRow
import com.nuvio.tv.skin.kicia.components.ForgeSideSheet
import com.nuvio.tv.skin.kicia.player.ForgeClock
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeMotion
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.reshaped.pillnav.PillNavBarState
import com.nuvio.tv.ui.reshaped.pillnav.PillNavEntry
import com.nuvio.tv.ui.reshaped.pillnav.PillNavEntryKind
import com.nuvio.tv.ui.screens.ambient.LocalAmbientIdleController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tabs of the bar in the mockup's order: search, Home, Live TV, YouTube, Biblioteka; any other tab after them.
 * Discover is not on the bar in the mockup (it lives in Search as "Odkrywaj").
 */
internal fun forgeNavTabs(entries: List<PillNavEntry>): List<PillNavEntry> {
    val order = listOf(Screen.Search.route, Screen.Home.route, LIVE_TV_ROUTE, Screen.YouTube.route, Screen.Library.route)
    val byKey = entries.associateBy { it.key }
    val ordered = order.mapNotNull { byKey[it] }
    return ordered + entries.filter { it.kind == PillNavEntryKind.Tab && it !in ordered && it.key != Screen.Discover.route }
}

/** The tab's name as the mockup writes it ("Home", not the app's own word for it). */
internal fun forgeTabLabel(entry: PillNavEntry): String = if (entry.key == Screen.Home.route) "Home" else entry.label

private const val AvatarKey = "forge_nav_avatar"
private const val NightKey = "forge_nav_night"
private const val SaverKey = "forge_nav_saver"
private const val SwitchDelayMs = 250L

/** Dark base under the glass: the mockup blurs the picture behind the bar; without a blur this keeps it legible. */
private val GlassBase = Color(0xA6000000) // ~65% black: the title behind the bar must not show through

/**
 * The top bar of the mockup (NAV), drawn in place of Reshaped's pill. All of Reshaped's behaviour around it
 * is kept (hide on scroll, Back, focus hand-over, navigation): this only replaces what is drawn, and it
 * takes the same inputs the pill took.
 *
 * Layout, y 27-67 dp, content edge x 58: avatar, glass pill [search icon | tabs | Biblioteka + counter],
 * on the right the screensaver and settings icons and the clock. Left/Right on a tab switches to it after a
 * short pause (no OK); Down leaves into the content; avatar, screensaver and settings need OK.
 * The avatar opens "Przełącz profil" / "Usługi i dodatki".
 */
@Composable
internal fun ForgeTopNav(
    entries: List<PillNavEntry>,
    selectedKey: String?,
    state: PillNavBarState,
    requesterFor: (String) -> FocusRequester,
    interactive: Boolean,
    hidden: Boolean,
    canLeaveUp: Boolean,
    isRtl: Boolean,
    activeProfileColorHex: String,
    activeProfileAvatarImageUrl: String?,
    onEntryClick: (PillNavEntry) -> Unit,
    onExitDown: () -> Unit,
    onExitUp: () -> Unit,
    modifier: Modifier = Modifier,
    navViewModel: ForgeNavViewModel = hiltViewModel(),
) {
    val libraryCount by navViewModel.libraryCount.collectAsStateWithLifecycle()
    val ambient = LocalAmbientIdleController.current
    val byKey = remember(entries) { entries.associateBy { it.key } }
    val tabs = remember(entries) { forgeNavTabs(entries) }
    val settings = entries.firstOrNull { it.kind == PillNavEntryKind.Settings }
    val profile = entries.firstOrNull { it.kind == PillNavEntryKind.Profile }
    // Focus walk order for Left/Right: avatar, tabs, screensaver, settings.
    val order = remember(tabs, settings) { listOf(AvatarKey) + tabs.map { it.key } + NightKey + listOfNotNull(settings?.key) }
    val entryKeyForEntry = selectedKey?.takeIf { it in byKey } ?: entries.firstOrNull()?.key
    val barFocused = state.hasFocus
    var menuOpen by remember { mutableStateOf(false) }
    var focusedSlot by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // v7.4 (1.13): Back from Historia opened from the avatar menu shows the menu again, focus on "Historia"
    LaunchedEffect(interactive) {
        if (interactive && ForgeNavState.reopenAvatarMenu) { ForgeNavState.reopenAvatarMenu = false; menuOpen = true }
    }

    val searchEntry = byKey[Screen.Search.route]

    // Cold start from the assistant: the flag is set before the bar exists, so open Search once.
    LaunchedEffect(searchEntry) {
        if (com.nuvio.tv.skin.kicia.input.ForgeRemoteHook.peekVoiceWanted() && searchEntry != null && selectedKey != Screen.Search.route) {
            onEntryClick(searchEntry)
        }
    }

    // The remote's voice button (when the system passes it on): open Search; the Search screen starts listening.
    LaunchedEffect(searchEntry, selectedKey) {
        com.nuvio.tv.skin.kicia.input.ForgeRemoteHook.voiceRequests.collect {
            if (searchEntry != null && selectedKey != Screen.Search.route) {
                com.nuvio.tv.skin.kicia.input.ForgeRemoteHook.requestVoiceOnSearch()
                onEntryClick(searchEntry)
            }
        }
    }

    // Another screen asked for a tab (e.g. a channel tile on Home opens Live TV).
    LaunchedEffect(byKey, selectedKey) {
        ForgeNavRequests.requests.collect { route ->
            val entry = byKey[route]
            if (entry != null && route != selectedKey) onEntryClick(entry)
        }
    }

    // Tabs switch when the user pauses on them, so sweeping across the bar does not load every screen it passes.
    // The screen that appears tends to take focus for itself; while the user is still walking the bar the focus is
    // put back on the tab (for up to 3 s, until Down is pressed) so Left/Right keep moving along the bar.
    var leftForContent by remember { mutableStateOf(false) }
    LaunchedEffect(state.focusedKey) {
        leftForContent = false
        val key = state.focusedKey ?: return@LaunchedEffect
        val entry = byKey[key] ?: return@LaunchedEffect
        if (!state.hasFocus || entry.kind != PillNavEntryKind.Tab || key == selectedKey) return@LaunchedEffect
        delay(SwitchDelayMs)
        if (!state.hasFocus) return@LaunchedEffect
        onEntryClick(entry)
        val deadline = SystemClock.uptimeMillis() + 3_000L
        while (SystemClock.uptimeMillis() < deadline && !leftForContent) {
            delay(100)
            if (!state.hasFocus && !leftForContent) runCatching { requesterFor(key).requestFocus() }
        }
    }

    // Hide on scroll, like Reshaped's pill: the layer moves up and fades, nothing is relaid out.
    val hideFraction = animateFloatAsState(
        targetValue = if (hidden && !barFocused) 1f else 0f,
        animationSpec = ForgeMotion.scrollSpec(),
        label = "forgeNavHide",
    )
    val hideDistancePx = with(LocalDensity.current) { 90.dp.toPx() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = -hideFraction.value * hideDistancePx
                alpha = (1f - 1.25f * hideFraction.value).coerceIn(0f, 1f)
            }
            .padding(start = ForgeDims.ContentStart, end = ForgeDims.ContentEnd, top = 16.dp) // v7: pasek y=16 dp
            .height(ForgeDims.NavPillHeight)
            .onFocusChanged { state.hasFocus = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> { if (canLeaveUp) onExitUp(); true }
                    Key.DirectionDown -> { leftForContent = true; onExitDown(); true }
                    Key.DirectionLeft, Key.DirectionRight -> {
                        val index = order.indexOf(focusedSlot)
                        val forward = (event.key == Key.DirectionRight) != isRtl
                        val target = order.getOrNull(if (forward) index + 1 else index - 1)
                        if (index >= 0 && target != null) runCatching { requesterFor(target).requestFocus() }
                        true
                    }
                    else -> false
                }
            }
            // forge hook v7.4 (1.21, ZACHOWANIE): focus coming up from the content always lands on the open tab, not on the nearest button
            .focusProperties {
                @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                enter = { _ -> entryKeyForEntry?.let { requesterFor(it) } ?: androidx.compose.ui.focus.FocusRequester.Default }
            }
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar
        NavRound(
            slot = AvatarKey, enabled = interactive && barFocused, requester = requesterFor(AvatarKey),
            onFocused = { focusedSlot = AvatarKey }, onOk = { menuOpen = true },
            onLongOk = { profile?.let(onEntryClick) }, // v7.4 (1.17): długie OK = Przełącz profil
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape)) {
                if (profile == null && activeProfileAvatarImageUrl.isNullOrBlank()) {
                    // Single profile: no name or picture to show, so a plain person on the profile colour.
                    val tint = runCatching { Color(android.graphics.Color.parseColor(activeProfileColorHex)) }.getOrDefault(ForgeColors.Accent)
                    Box(Modifier.size(44.dp).background(tint), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Person, contentDescription = "Profil", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                } else {
                    // Initial underneath, picture on top (a blank or failing address leaves the initial, not an empty circle).
                    ProfileAvatarCircle(name = profile?.label.orEmpty(), colorHex = "#3A3A3C", size = 44.dp) // v7.4: awatar neutralny #3A3A3C jak w makiecie
                    activeProfileAvatarImageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                        coil3.compose.AsyncImage(
                            model = url,
                            contentDescription = profile?.label,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.size(44.dp).clip(CircleShape),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(12.dp))

        // Glass pill with the search icon and the tabs
        Row(
            modifier = Modifier
                .height(ForgeDims.NavPillHeight)
                .clip(RoundedCornerShape(ForgeDims.RadiusNavPill))
                // Owner 10.11: white with opacity and a Gaussian blur of what lies behind (the mockup's backdrop-filter), not a dark base.
                .forgeSoftBackdrop(com.nuvio.tv.skin.kicia.components.LocalForgeNavSoftBlur.current) // API < 31: the strip under the bar blurred in software
                .forgeGlass(com.nuvio.tv.skin.kicia.components.LocalForgeNavHaze.current, tint = ForgeColors.Tray, tintAlpha = 1f) // v7.4: --taca rgba(28,28,30,.72)
                .forgeTag("taca.0")
                .padding(horizontal = 4.dp), // v7.4: taca z odstępem 4
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { entry ->
                val isSearch = entry.key == Screen.Search.route
                ForgeNavTab(
                    selected = entry.key == selectedKey && !(barFocused && (focusedSlot == AvatarKey || focusedSlot == NightKey || focusedSlot == settings?.key)), // owner 10.11: the open tab's glass goes while the focus is on the round buttons
                    enabled = interactive && (barFocused || entry.key == entryKeyForEntry),
                    requester = requesterFor(entry.key),
                    onFocused = { state.focusedKey = entry.key; focusedSlot = entry.key },
                    onOk = { if (entry.key != selectedKey) onEntryClick(entry) }, // forge hook v7.4 (03.8): OK na otwartej zakładce zostawia fokus na pasku
                    // v7.4 (1.16): długie OK = odśwież ekran, fokus zostaje na zakładce
                    onLongOk = { onEntryClick(entry); scope.launch { delay(200); runCatching { requesterFor(entry.key).requestFocus() } } },
                    tag = "zakladka.${tabs.indexOf(entry)}",
                    round = isSearch, // v7.4: Szukaj = zakładka 36 x 36
                ) { fg ->
                    if (isSearch) {
                        Icon(Icons.Outlined.Search, contentDescription = entry.label, tint = fg, modifier = Modifier.size(20.dp))
                    } else {
                        Text(forgeTabLabel(entry), style = ForgeText.button.copy(color = fg))
                        if (false && entry.key == Screen.Library.route && libraryCount > 0) { // v7.4: makieta nie ma licznika na zakładce
                            Box(
                                Modifier
                                    .padding(start = 8.dp)
                                    .height(22.dp)
                                    .defaultMinSize(minWidth = 22.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(ForgeColors.Ok)
                                    .padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (libraryCount > 99) "99+" else libraryCount.toString(),
                                    style = ForgeText.captionBold.copy(color = ForgeColors.OnFocus),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        // v7 (01): second tray with the round settings button (the screensaver has no button, W-75; Tryb nocny: later).
        if (settings != null) {
            Spacer(Modifier.width(20.dp))
            Row(
                modifier = Modifier
                    .height(ForgeDims.NavPillHeight)
                    .clip(RoundedCornerShape(ForgeDims.RadiusNavPill))
                    .forgeSoftBackdrop(com.nuvio.tv.skin.kicia.components.LocalForgeNavSoftBlur.current)
                    .forgeGlass(com.nuvio.tv.skin.kicia.components.LocalForgeNavHaze.current, tint = ForgeColors.Tray, tintAlpha = 1f) // v7.4: --taca rgba(28,28,30,.72)
                    .forgeTag("pasek-druga")
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ForgeNavTab(
                    selected = ForgeNightMode.enabled,
                    enabled = interactive && barFocused,
                    requester = requesterFor(NightKey),
                    onFocused = { focusedSlot = NightKey },
                    onOk = { ForgeNightMode.toggle() },
                    onLongOk = { ForgeNavState.settingsCategory = "NIGHT_MODE"; onEntryClick(settings) }, // v7.4 (1.15)
                    tag = "o-36.0", restBg = ForgeColors.Surface,
                    round = true,
                ) { fg -> Icon(if (ForgeNightMode.enabled) Icons.Filled.Bedtime else Icons.Outlined.Bedtime, contentDescription = "Tryb nocny", tint = fg, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(8.dp)) // v7.4: przyciski okrągłe x 527 i 571 (odstęp 8 wg renderu makiety)
                ForgeNavTab(
                    selected = settings.key == selectedKey,
                    enabled = interactive && (barFocused || settings.key == entryKeyForEntry), // forge hook v7.4 (01): ▲ z Ustawień wchodzi na ten przycisk
                    requester = requesterFor(settings.key),
                    onFocused = { state.focusedKey = settings.key; focusedSlot = settings.key },
                    onOk = { onEntryClick(settings) },
                    tag = "o-36.1", restBg = ForgeColors.Surface,
                    round = true,
                ) { fg -> Icon(Icons.Outlined.Settings, contentDescription = "Ustawienia", tint = fg, modifier = Modifier.size(24.dp)) }
            }
        }

        Spacer(Modifier.weight(1f))

        ForgeClock(Modifier.forgeTag("zegar"))
    }

    // Profile menu (mockup PMENU): Moja lista (= the Biblioteka tab), Przełącz profil, Usługi i dodatki.
    if (menuOpen) {
        Dialog(
            onDismissRequest = { menuOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            com.nuvio.tv.skin.kicia.components.ForgePopover(onDismiss = { menuOpen = false }, x = 40.dp, y = 68.dp) {
                ForgeSheetRow(text = "Historia", icon = androidx.compose.material.icons.Icons.Outlined.History) {
                    menuOpen = false
                    ForgeNavState.reopenAvatarMenu = true
                    onEntryClick(PillNavEntry("forge_history", PillNavEntryKind.Tab, "forge_history", "Historia"))
                }
                // Fork 19.11: „Aplikacje” (fork/launcher) przeniesione z „Narzędzi”.
                if (com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(androidx.compose.ui.platform.LocalContext.current, "module_launcher", true)) {
                    ForgeSheetRow(text = "Aplikacje", icon = androidx.compose.material.icons.Icons.Outlined.Apps) {
                        menuOpen = false
                        com.nuvio.tv.ui.screens.settings.ForkFeatures.open?.invoke(com.nuvio.tv.fork.launcher.LauncherModule.APPS_ROUTE)
                    }
                }
                if (profile != null) {
                    ForgeSheetRow(text = "Przełącz profil", icon = androidx.compose.material.icons.Icons.Outlined.Person) {
                        menuOpen = false
                        onEntryClick(profile)
                    }
                }
                settings?.let { st ->
                    ForgeSheetRow(text = "Ustawienia", icon = androidx.compose.material.icons.Icons.Outlined.Settings) {
                        menuOpen = false
                        onEntryClick(st)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, description: String) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(GlassBase).background(ForgeColors.Surface), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = description, tint = ForgeColors.Text, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun NavRound(
    slot: String,
    enabled: Boolean,
    requester: FocusRequester,
    onFocused: () -> Unit,
    onOk: () -> Unit,
    onLongOk: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    ForgeFocusBox(
        radius = 22.dp,
        modifier = Modifier.size(44.dp).forgeTag("awatar"),
        onLongOk = onLongOk,
        outline = true,
        enabled = enabled,
        focusRequester = requester,
        onFocusChange = { has -> if (has) onFocused() },
        tapEnabled = true,
        onOk = onOk,
    ) { _ -> content() }
}

@Composable
internal fun ForgeNavTab(
    selected: Boolean,
    enabled: Boolean,
    requester: FocusRequester,
    onFocused: () -> Unit,
    onOk: () -> Unit,
    round: Boolean = false,
    onLongOk: (() -> Unit)? = null,
    tag: String = "",
    restBg: Color = Color.Transparent,
    dimmed: Boolean = false, // v7.4: wyłączona = krycie .5
    content: @Composable RowScope.(foreground: Color) -> Unit,
) {
    ForgeFocusBox(
        radius = 18.dp,
        modifier = Modifier.height(36.dp).graphicsLayer { alpha = if (dimmed) .5f else 1f }.forgeTag(tag),
        onLongOk = onLongOk,
        scaleOnFocus = false, // v6: nav pills do not scale (1.125 would overlap neighbours)
        enabled = enabled,
        focusRequester = requester,
        onFocusChange = { has -> if (has) onFocused() },
        tapEnabled = true,
        onOk = onOk,
    ) { focusedNow ->
        val bg = when {
            focusedNow -> ForgeColors.Focus
            selected -> ForgeColors.Surface3 // v7.4: wybrana 24%
            else -> restBg // v7.4: przycisk okrągły w spoczynku 12%
        }
        val fg = when {
            focusedNow -> ForgeColors.OnFocus
            selected -> ForgeColors.Text
            else -> ForgeColors.Text2
        }
        Row(
            (if (round) Modifier.size(36.dp) else Modifier.height(36.dp)).clip(RoundedCornerShape(18.dp)).background(bg).padding(horizontal = if (round) 0.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) { content(fg) }
    }
}
