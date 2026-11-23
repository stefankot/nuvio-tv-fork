package com.nuvio.tv.skin.kicia.livetv

import android.util.Log
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.skin.kicia.components.ForgeDialog
import com.nuvio.tv.skin.kicia.components.ForgeDialogButton
import com.nuvio.tv.ui.reshaped.livetv.LiveTvPlayerState

/**
 * Makieta 07, stan `blad-kanalu`: okno „Kanał nie odpowiada” (Spróbuj ponownie = fokus, Następny kanał). Shown by the
 * channel start guard (LiveTvPlayerState.watchLiveStart) when a channel fails twice: an error or no data, a quiet
 * restart, the same again.
 */
internal object ForgeChannelFailed {
    private const val TAG = "ForkLiveTv"
    val channel = mutableStateOf<LiveTvChannel?>(null)

    /** The window's button with the focus: 0 = Spróbuj ponownie, 1 = Następny kanał. */
    val selected = mutableIntStateOf(0)
    private var okDown = false

    /** The window just came up: the first button, and an OK pressed before it does not count. */
    fun shown() { selected.intValue = 0; okDown = false }

    fun retry(state: LiveTvPlayerState) {
        val ch = channel.value
        Log.i(TAG, "kanał nie odpowiada: Spróbuj ponownie (${ch?.name})")
        channel.value = null
        ch?.let { state.switchTo(it, force = true) }
    }

    fun next(state: LiveTvPlayerState) {
        Log.i(TAG, "kanał nie odpowiada: Następny kanał (po ${channel.value?.name})")
        channel.value = null
        state.zapNow(1)
    }

    /**
     * UX 22.11/23.11: the window is drawn inside the player, whose key handlers see every key first — the first Back took
     * two presses, and in Niunia ◀▶ and OK went to the player's own controls (they never reached the buttons). Called
     * first by the Live TV key handler while the window is up, it runs the window itself: Back (on release) closes it and
     * leaves the player for the channel list (behind it there is no picture), ◀▶ pick a button (the screen focus follows),
     * OK (on release) runs it, ▲▼ and the channel keys do nothing. Other keys (volume) go on. Returns whether the key was used.
     */
    fun onKey(state: LiveTvPlayerState, event: KeyEvent, exit: (() -> Unit)?): Boolean {
        val down = event.action == KeyEvent.ACTION_DOWN
        when (event.keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> if (!down) {
                Log.i(TAG, "kanał nie odpowiada: Wstecz → okno zamknięte, powrót do listy (${channel.value?.name})")
                channel.value = null
                exit?.invoke()
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> if (down) selected.intValue = 0
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (down) selected.intValue = 1
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> when {
                down && event.repeatCount == 0 -> okDown = true
                !down && okDown -> { okDown = false; if (selected.intValue == 0) retry(state) else next(state) }
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_CHANNEL_DOWN -> Unit
            else -> return false
        }
        return true
    }
}

@Composable
internal fun ForgeChannelFailedDialog(state: LiveTvPlayerState) {
    val failed = ForgeChannelFailed.channel.value
    LaunchedEffect(failed) { if (failed != null) ForgeChannelFailed.shown() }
    ForgeDialog(
        visible = failed != null,
        title = "Kanał nie odpowiada",
        message = "Kanał ${failed?.name.orEmpty()} nie wysyła obrazu. Sprawdź połączenie z internetem albo włącz inny kanał.",
        buttons = listOf(
            ForgeDialogButton("Spróbuj ponownie", primary = true) { ForgeChannelFailed.retry(state) },
            ForgeDialogButton("Następny kanał") { ForgeChannelFailed.next(state) },
        ),
        onDismiss = { ForgeChannelFailed.channel.value = null },
        focusIndex = ForgeChannelFailed.selected.intValue,
    )
}
