package com.nuvio.tv.fork

import android.app.Activity
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import com.nuvio.tv.fork.arc.RemoteGlobalHook
import com.nuvio.tv.skin.api.SkinRegistry
import com.nuvio.tv.skin.api.rememberSkinId
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState

/** Jedyne punkty wpięcia forka w pliki rdzenia. Każda funkcja wraca natychmiast, gdy funkcja jest wyłączona. */
object ForkHooks {
    /** MainActivity.dispatchKeyEvent: pilot Arc poza odtwarzaczem. true = klawisz obsłużony. */
    fun onGlobalKey(activity: Activity, event: KeyEvent): Boolean = RemoteGlobalHook.onKey(activity, event)

    /** HomeScreen / trasy: true = aktywny skin narysował ekran tej trasy. */
    @Composable
    fun skinScreen(route: String, args: Map<String, Any?>): Boolean {
        val skin = SkinRegistry.skinFor(rememberSkinId()) ?: return false
        return skin.screen(route)?.Content(args) ?: false
    }

    /** PlayerScreen: true = aktywny skin narysował nakładkę (sterowanie i panele) odtwarzacza. */
    @Composable
    fun skinPlayerChrome(uiState: PlayerUiState, onEvent: (PlayerEvent) -> Unit): Boolean {
        val skin = SkinRegistry.skinFor(rememberSkinId()) ?: return false
        return skin.PlayerChrome(uiState, onEvent)
    }
}
