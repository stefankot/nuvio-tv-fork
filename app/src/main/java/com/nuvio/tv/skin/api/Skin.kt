package com.nuvio.tv.skin.api

import androidx.compose.runtime.Composable
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState

/** Ekran skina dla jednej trasy. Zwraca true, gdy narysował ekran (inaczej działa domyślny). */
fun interface SkinScreen {
    @Composable
    fun Content(args: Map<String, Any?>): Boolean
}

/**
 * Skin implementuje tylko to, co ma; reszta wraca do zwykłej aplikacji (ekran po ekranie).
 */
interface Skin {
    val id: SkinId
    fun screen(route: String): SkinScreen? = null

    /** Nakładka odtwarzacza (sterowanie + panele): UiState wchodzi, PlayerEvent wychodzi. true = narysowano. */
    @Composable
    fun PlayerChrome(uiState: PlayerUiState, onEvent: (PlayerEvent) -> Unit, args: Map<String, Any?> = emptyMap()): Boolean = false
}
