package com.nuvio.tv.skin.kicia.nav

import com.nuvio.tv.ui.navigation.Screen

/**
 * Screens that draw their own header at the top (YouTube's feed tabs, Discover's Type/Catalog/Genre fields):
 * with the mockup's top bar they start below it instead of under it. Search is handled by Reshaped's own
 * Netflix-look rule. Used by the one-line hook in PillNavScaffold.
 */
internal val ForgeBelowBarRoutes: Set<String> = setOf(Screen.YouTube.route, Screen.Discover.route)

internal fun forgeBelowBar(route: String?): Boolean = route != null && route in ForgeBelowBarRoutes
