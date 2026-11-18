package com.nuvio.tv.skin.kicia.player

import android.view.KeyEvent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged

/**
 * Owner's report 9.11: in the player OK and the arrows sometimes did nothing — when the controls hide
 * themselves the focused button goes away and nothing in the player holds the focus, so no key reaches
 * the player's own key handling. On the next direction/OK key the focus goes back to the player container
 * (whose handler shows the controls, seeks, zaps), before the key is delivered.
 */
object ForgePlayerFocusRescue {
    @Volatile private var playerHasFocus = true
    @Volatile private var rescue: (() -> Unit)? = null

    /** Called by the activity's key hook for every key; never consumes it. */
    fun onKey(event: KeyEvent) {
        if (event.action != KeyEvent.ACTION_DOWN || playerHasFocus) return
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> rescue?.invoke()
        }
    }

    /** On the player's root container: reports whether the player (or anything in it) holds the focus. */
    fun Modifier.forgePlayerFocusRescue(container: FocusRequester): Modifier = composed {
        DisposableEffect(container) {
            val action = { runCatching { container.requestFocus() }; Unit }
            rescue = action
            playerHasFocus = true
            onDispose { if (rescue === action) { rescue = null; playerHasFocus = true } }
        }
        onFocusChanged { playerHasFocus = it.hasFocus }
    }
}
