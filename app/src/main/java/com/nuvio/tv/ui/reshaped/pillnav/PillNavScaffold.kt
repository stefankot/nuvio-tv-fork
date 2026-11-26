package com.nuvio.tv.ui.reshaped.pillnav

import androidx.compose.foundation.background // forge
import android.util.Log
import androidx.activity.compose.BackHandler
import com.nuvio.tv.skin.kicia.nav.forgeTagRoot
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import com.nuvio.tv.skin.kicia.components.forgeHazeSource
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import com.nuvio.tv.DrawerItem
import com.nuvio.tv.LocalContentFocusRequester
import com.nuvio.tv.LocalSidebarExpanded
import com.nuvio.tv.ui.navigation.NuvioNavHost
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.screens.home.HomeViewModel

private const val PROFILE_ENTRY_KEY = "pill_nav_profile"

/**
 * Third navigation layout next to the legacy and modern sidebars: the content fills the screen and a glass
 * pill menu floats at the top centre on root screens. D-pad Up from the content reaches the pill (always on
 * the selected item), Left/Right walk it, Center/Enter opens, Down returns to the content. Back on a root
 * screen moves focus to the pill, and Back on the pill exits, like the sidebar.
 *
 * TV layout: the pill floats in the header band root screens already keep free when their built-in headers are
 * hidden (the same band the modern sidebar's floating pill uses), so screens keep their normal size and nothing
 * is inset. It tucks away (translate + fade on a graphics layer, no relayout, no blur) when focus moves down into
 * scrolling content and comes back on the way up; on Search it stays tucked away unless it has focus, like the
 * modern sidebar's pill, so it never covers the search field. [topBannerVisible] lets D-pad Up leave the pill
 * for the update banner above the navigation area.
 */
@Composable
internal fun PillNavScaffold(
    longPressBackHeld: MutableState<Boolean>,
    navController: NavHostController,
    startDestination: String,
    currentRoute: String?,
    rootRoutes: Set<String>,
    drawerItems: List<DrawerItem>,
    selectedDrawerRoute: String?,
    blurEnabled: Boolean,
    activeProfileName: String,
    activeProfileColorHex: String,
    activeProfileAvatarImageUrl: String?,
    showProfileSelector: Boolean,
    onSwitchProfile: () -> Unit,
    onNavigate: (String) -> Unit,
    onExitApp: () -> Unit,
    topBannerVisible: Boolean = false,
) {
    val showBar = currentRoute in rootRoutes
    // Settings and Live TV keep the pill in their header band; every other root screen scrolls under it, so it tucks away.
    // In the Netflix look the pill stays put on Home (whose page starts below its band) and on Search
    // (Nuvio's own screen, moved down below the band here).
    val netflixUi = false // Fork 26.11: wygląd Netflix z Reshaped usunięty razem z bazą 1.2.0-beta.4 (pliki bazy 1:1)
    // forge: with the mockup's top bar, Search sits below the bar like in the Netflix look (field at y 84 dp)
    val netflixSearch = (currentRoute == Screen.Search.route && (netflixUi || com.nuvio.tv.skin.kicia.ForgeUi.NEW_NAV)) ||
        (com.nuvio.tv.skin.kicia.ForgeUi.NEW_NAV && com.nuvio.tv.skin.kicia.nav.forgeBelowBar(currentRoute)) // forge hook: screens with their own header sit below the bar
    val netflixHome = currentRoute == Screen.Home.route && netflixUi
    val autoHide = showBar && currentRoute != Screen.Settings.route &&
        currentRoute != com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE && !netflixSearch && !netflixHome
    val hiddenUnlessFocused = currentRoute == Screen.Search.route && !netflixSearch
    val state = remember { PillNavBarState() }
    // Liquid glass on capable TVs with Nuvio's blur setting on. The screen is recorded only while the pill is on
    // it, never during playback; the pill slides out over the last recording, which is then dropped.
    val glassBackdrop = rememberPillGlassBackdrop(blurEnabled)
    val forgeHaze = com.nuvio.tv.skin.kicia.components.rememberForgeHazeState() // forge (owner 10.11): backdrop blur under the top bar
    val forgeSoft = com.nuvio.tv.skin.kicia.components.rememberForgeSoftBlur() // the same for Android 11 and below, in software
    run {
        val soft = forgeSoft
        val density = LocalDensity.current
        val direction = LocalLayoutDirection.current
        LaunchedEffect(soft, showBar) {
            if (soft == null || !showBar || !com.nuvio.tv.skin.kicia.ForgeUi.NEW_NAV) return@LaunchedEffect
            val stripPx = with(density) { 110.dp.roundToPx() }
            while (true) {
                delay(com.nuvio.tv.skin.kicia.components.ForgeSoftBlur.IntervalMs)
                soft.refresh(density, direction, stripPx)
            }
        }
    }
    val glassDensity = LocalDensity.current
    val glassLayoutDirection = LocalLayoutDirection.current
    LaunchedEffect(showBar, glassBackdrop) {
        if (showBar || glassBackdrop == null) return@LaunchedEffect
        delay(400)
        glassBackdrop.clear(glassDensity, glassLayoutDirection)
    }
    val focusRequesters = remember { HashMap<String, FocusRequester>() }
    val requesterFor = remember<(String) -> FocusRequester> {
        { key: String -> focusRequesters.getOrPut(key) { FocusRequester() } }
    }
    val focusManager = LocalFocusManager.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val keyboardController = LocalSoftwareKeyboardController.current
    val contentFocusRequester = remember { FocusRequester() }
    var pendingBarFocus by remember { mutableStateOf(false) }
    var forgeExitDialog by remember { mutableStateOf(false) } // forge hook v7.4: Okno wyjścia (16)
    var pendingContentFocus by remember { mutableStateOf(false) }

    val showProfile = showProfileSelector && activeProfileName.isNotEmpty()
    val entries = remember(drawerItems, showProfile, activeProfileName) {
        buildList {
            drawerItems.filter { it.route != Screen.Settings.route }.forEach { item ->
                add(PillNavEntry(item.route, PillNavEntryKind.Tab, item.route, item.label, item.iconRes, item.icon))
            }
            drawerItems.firstOrNull { it.route == Screen.Settings.route }?.let { item ->
                add(PillNavEntry(item.route, PillNavEntryKind.Settings, item.route, item.label, item.iconRes, item.icon))
            }
            if (showProfile) {
                add(PillNavEntry(PROFILE_ENTRY_KEY, PillNavEntryKind.Profile, null, activeProfileName))
            }
        }
    }
    val selectedKey = selectedDrawerRoute?.takeIf { route -> entries.any { it.key == route } }

    LaunchedEffect(showBar) {
        if (!showBar) {
            pendingBarFocus = false
            pendingContentFocus = false
        }
    }

    // A new root screen starts with the pill visible, like the phone on a tab change. Coming back to the same
    // root screen from a detail screen keeps it tucked away if it was, since that screen returns scrolled.
    val lastRootRoute = remember { arrayOfNulls<String>(1) }
    LaunchedEffect(currentRoute, showBar) {
        if (!showBar || currentRoute == lastRootRoute[0]) return@LaunchedEffect
        lastRootRoute[0] = currentRoute
        state.show()
    }

    val requestBarFocus = {
        state.show()
        pendingBarFocus = true
    }

    BackHandler(enabled = showBar && !state.hasFocus) {
        requestBarFocus()
    }

    // forge hook v7.4 (1.23): after Back from a tab to Home the Home tab keeps focus. The new screen takes focus for
    // itself during its first frames, so focus is put back on the tab for up to 1.5 s, until it has held for 400 ms.
    var forgeBackHomeFocus by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(forgeBackHomeFocus) {
        if (forgeBackHomeFocus == 0) return@LaunchedEffect
        val home = Screen.Home.route
        val deadline = android.os.SystemClock.uptimeMillis() + 1_500L
        var heldSince = 0L
        while (android.os.SystemClock.uptimeMillis() < deadline) {
            kotlinx.coroutines.delay(50)
            val now = android.os.SystemClock.uptimeMillis()
            android.util.Log.d("ForgeNav", "1.23 t=${now - (deadline - 1_500L)} hasFocus=${state.hasFocus} key=${state.focusedKey}") // forge v7.4: diagnostyka
            if (state.hasFocus && state.focusedKey == home) {
                if (heldSince == 0L) heldSince = now else if (now - heldSince >= 400L) break
            } else {
                heldSince = 0L
                runCatching { requesterFor(home).requestFocus() }
            }
        }
    }


    LaunchedEffect(pendingBarFocus, showBar, selectedKey, entries) {
        if (!pendingBarFocus) return@LaunchedEffect
        if (!showBar) {
            pendingBarFocus = false
            return@LaunchedEffect
        }
        val key = selectedKey ?: entries.firstOrNull()?.key
        if (key == null) {
            pendingBarFocus = false
            return@LaunchedEffect
        }
        repeat(2) { withFrameNanos { } }
        runCatching { requesterFor(key).requestFocus() }
        pendingBarFocus = false
    }

    // Reselecting the current tab hands focus back to its content, like the sidebar does.
    LaunchedEffect(pendingContentFocus) {
        if (!pendingContentFocus) return@LaunchedEffect
        repeat(2) { withFrameNanos { } }
        runCatching { contentFocusRequester.requestFocus() }
        withFrameNanos { }
        if (state.hasFocus) focusManager.moveFocus(FocusDirection.Down)
        pendingContentFocus = false
    }

    // Focus recovery, the pill's counterpart of the sidebar's: if the focused item disappears (e.g. Discover
    // was removed from the navigation) while the pill owns focus, land on the selected or first item.
    LaunchedEffect(entries, state.hasFocus, selectedKey) {
        if (!showBar || !state.hasFocus) return@LaunchedEffect
        val focusedKey = state.focusedKey
        if (focusedKey != null && entries.any { it.key == focusedKey }) return@LaunchedEffect
        val fallback = selectedKey ?: entries.firstOrNull()?.key ?: return@LaunchedEffect
        repeat(2) { withFrameNanos { } }
        runCatching { requesterFor(fallback).requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .forgeTagRoot() // forge hook v7.4: testTag -> resource-id (pomiar geometrii)
            .then(if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_NAV && netflixSearch) Modifier.background(com.nuvio.tv.skin.kicia.tokens.ForgeColors.Bg) else Modifier) // forge hook: no black band above screens below the bar
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) glassBackdrop?.poke()
                // Swallow the rest of a long-press Back until it is released so it cannot also exit the app.
                if (longPressBackHeld.value && keyEvent.key == Key.Back) {
                    if (keyEvent.type == KeyEventType.KeyUp) longPressBackHeld.value = false
                    return@onPreviewKeyEvent true
                }
                false
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (netflixSearch) Modifier.padding(top = if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_NAV) androidx.compose.ui.unit.Dp(66f) else PillNavTokens.barTopGap + PillNavTokens.barHeight) else Modifier)
                .pillGlassSource(glassBackdrop?.takeIf { showBar })
                .forgeHazeSource(if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_NAV) forgeHaze else null)
                .then(if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_NAV && forgeSoft != null) with(forgeSoft) { Modifier.source() } else Modifier)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Back) {
                        // Long-press Back on a root screen jumps straight to the pill, past the screens' own Back.
                        if (
                            keyEvent.type == KeyEventType.KeyDown &&
                            showBar &&
                            keyEvent.nativeKeyEvent.isLongPress
                        ) {
                            if (!longPressBackHeld.value) {
                                longPressBackHeld.value = true
                                requestBarFocus()
                            }
                            return@onPreviewKeyEvent true
                        }
                        return@onPreviewKeyEvent false
                    }
                    // Hide-on-scroll: moving down into the content tucks the pill away, moving up brings it back.
                    if (autoHide && keyEvent.type == KeyEventType.KeyDown) {
                        when (keyEvent.key) {
                            Key.DirectionDown -> state.hide()
                            Key.DirectionUp -> state.show()
                            else -> Unit
                        }
                    }
                    false
                }
                .onKeyEvent { keyEvent ->
                    // Up that nothing in the content used: go to the pill (focus search lands on the selected
                    // item, the only one focusable from outside); fall back to requesting it directly.
                    if (
                        showBar &&
                        keyEvent.type == KeyEventType.KeyDown &&
                        keyEvent.key == Key.DirectionUp
                    ) {
                        if (!focusManager.moveFocus(FocusDirection.Up)) requestBarFocus()
                        true
                    } else {
                        false
                    }
                }
        ) {
            CompositionLocalProvider(
                LocalSidebarExpanded provides state.hasFocus,
                LocalContentFocusRequester provides contentFocusRequester,
                com.nuvio.tv.skin.kicia.nav.LocalForgeBarFocus provides (if (showBar && com.nuvio.tv.skin.kicia.ForgeUi.NEW_NAV && !state.hasFocus) requestBarFocus else null), // forge hook v7.4: Back from a tab's content -> bar
            ) {
                NuvioNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    // Only root screens keep their header band free for the pill; others show their own titles.
                    hideBuiltInHeaders = showBar,
                )
            }
        }
        // forge hook v7.4 (1.19, 1.23): registered after NuvioNavHost, so Back on the bar wins over the NavHost's own pop
        // (the pop restored the old focus in Home's hero instead of keeping it on the Home tab)
        BackHandler(enabled = showBar && state.hasFocus) {
            if (longPressBackHeld.value) return@BackHandler
            // forge hook v7.4 (01, 1.19-1.20): Back on the bar goes to the Home tab; on Home it asks "Zamknąć Nuvio?"
            if (com.nuvio.tv.skin.kicia.ForgeUi.NEW_NAV) {
                val home = Screen.Home.route
                if (currentRoute != home && entries.any { it.key == home }) {
                    com.nuvio.tv.skin.kicia.nav.ForgeNavState.homeKeepsBarFocus = true // forge hook v7.4 (1.23)
                    onNavigate(home)
                    navigateToPillRoute(navController, currentRoute, home)
                    runCatching { requesterFor(home).requestFocus() }
                    forgeBackHomeFocus += 1 // forge hook v7.4 (1.23): Home's hero grabs focus once it composes; keep it on the tab
                } else {
                    forgeExitDialog = true
                }
                return@BackHandler
            }
            onExitApp()
        }

        // Slides in from the top edge when a root screen appears and out when a detail screen opens; the
        // enter/exit only moves and fades the pill's layer.
        AnimatedVisibility(
            visible = showBar && entries.isNotEmpty(),
            modifier = Modifier.align(Alignment.TopCenter),
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing),
                initialOffsetY = { -it },
            ) + fadeIn(animationSpec = tween(durationMillis = 260)),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                targetOffsetY = { -it },
            ) + fadeOut(animationSpec = tween(durationMillis = 160)),
        ) {
            CompositionLocalProvider(
                com.nuvio.tv.skin.kicia.components.LocalForgeNavHaze provides forgeHaze,
                com.nuvio.tv.skin.kicia.components.LocalForgeNavSoftBlur provides forgeSoft,
            ) {
            PillNavigationBar(
                entries = entries,
                selectedKey = selectedKey,
                state = state,
                requesterFor = requesterFor,
                frosted = blurEnabled,
                // While it animates out on a detail screen the pill must not be a focus target.
                interactive = showBar,
                hidden = hiddenUnlessFocused || (autoHide && state.hiddenByScroll) || (currentRoute == com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE && com.nuvio.tv.skin.kicia.nav.ForgeNavState.liveTvLifted), // forge hook v7.4 (6.2)
                canLeaveUp = topBannerVisible,
                isRtl = isRtl,
                activeProfileColorHex = activeProfileColorHex,
                activeProfileAvatarImageUrl = activeProfileAvatarImageUrl,
                onEntryClick = { entry ->
                    val route = entry.route
                    if (entry.kind == PillNavEntryKind.Profile || route == null) {
                        onSwitchProfile()
                    } else {
                        keyboardController?.hide()
                        val reselect = currentRoute == route
                        onNavigate(route)
                        navigateToPillRoute(navController, currentRoute, route)
                        // forge hook v7.4 (1.15): also for long OK on Tryb nocny the focus goes into the content first; Settings then moves it to the asked category (with retries)
                        pendingContentFocus = reselect
                    }
                },
                onExitDown = {
                    if (!focusManager.moveFocus(FocusDirection.Down)) {
                        runCatching { contentFocusRequester.requestFocus() }
                    }
                },
                onExitUp = { focusManager.moveFocus(FocusDirection.Up) },
                backdrop = glassBackdrop,
                // The Netflix look uses clear glass: no grey or black body, just the bend, rim and lens.
                clear = netflixUi,
            )
            }
        }
        // forge hook v7.4 (16): Okno wyjścia; "Zostań" has the focus, "Zamknij" leaves to the TV's home screen
        run {
            val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
            com.nuvio.tv.skin.kicia.components.ForgeDialog(
                visible = forgeExitDialog,
                title = "Zamknąć Nuvio?",
                message = "Odtwarzanie i pobieranie w tle zostaną zatrzymane.",
                buttons = listOf(
                    com.nuvio.tv.skin.kicia.components.ForgeDialogButton("Zostań", primary = true) { forgeExitDialog = false },
                    com.nuvio.tv.skin.kicia.components.ForgeDialogButton("Zamknij") { forgeExitDialog = false; activity?.finishAndRemoveTask() },
                ),
                onDismiss = { forgeExitDialog = false },
                initialFocusIndex = 0,
            )
        }
        com.nuvio.tv.skin.kicia.nav.ForgeNightOverlay() // forge hook: Tryb nocny (v7 01)
        com.nuvio.tv.skin.kicia.components.ForgeToastHost() // forge hook: Komunikat (v7.4)
    }
}

/** Same navigation the sidebar performs (MainActivity.navigateToDrawerRoute), kept here so the hook stays small. */
private fun navigateToPillRoute(
    navController: NavHostController,
    currentRoute: String?,
    targetRoute: String,
) {
    if (currentRoute == targetRoute) {
        if (targetRoute == Screen.Home.route) {
            val homeEntry = try {
                navController.getBackStackEntry(Screen.Home.route)
            } catch (_: IllegalArgumentException) {
                return
            }
            ViewModelProvider(homeEntry)[HomeViewModel::class.java].requestScrollToTop()
        }
        return
    }
    if (com.nuvio.tv.fork.nav.ForkHomeNav.handle(navController, currentRoute, targetRoute)) return // Fork hook: Home z paska zdejmuje stos (zgłoszenie 1)
    try {
        navController.navigate(targetRoute) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    } catch (e: IllegalArgumentException) {
        Log.w("NuvioPillNav", "Route not found in nav graph: $targetRoute", e)
    }
}
