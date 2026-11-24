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

    /** The window's button with the focus: 0 = Spróbuj ponownie, 1 = Następny kanał (Niunia: [buttons]). */
    val selected = mutableIntStateOf(0)

    enum class Action { RETRY, NEXT, LIST }

    /**
     * Z-14 (Niunia, makieta PlayerLiveError): Następny kanał (fokus) · Spróbuj ponownie · Lista kanałów; Kicia bez zmian:
     * Spróbuj ponownie · Następny kanał.
     */
    fun buttons(): List<Action> =
        if (com.nuvio.tv.skin.api.SkinPreferences.skin.value == com.nuvio.tv.skin.api.SkinId.NIUNIA) listOf(Action.NEXT, Action.RETRY, Action.LIST)
        else listOf(Action.RETRY, Action.NEXT)

    /** Runs a button: [exit] leaves the player for the channel list (null — the window only closes). */
    fun run(state: LiveTvPlayerState, action: Action, exit: (() -> Unit)?) {
        when (action) {
            Action.RETRY -> if (playingBehind()) {
                // 23.11: „Spróbuj ponownie”, a kanał już gra — bez restartu, okno znika.
                Log.i(TAG, "kanał nie odpowiada: OK, kanał już gra → okno zamknięte bez restartu (${channel.value?.name})")
                channel.value = null
            } else retry(state)
            Action.NEXT -> next(state)
            Action.LIST -> {
                Log.i(TAG, "kanał nie odpowiada: Lista kanałów (${channel.value?.name})")
                channel.value = null
                exit?.invoke()
            }
        }
    }
    private var okDown = false

    /**
     * 23.11 (właściciel): kanał mimo wszystko ruszył i gra pod oknem — wtedy okno nie zatrzymuje (zamyka się samo,
     * Wstecz i OK tylko je chowają). Stan odtwarzacza z [com.nuvio.tv.fork.sources.PlayerBridge].
     */
    fun playingBehind(): Boolean = com.nuvio.tv.fork.sources.PlayerBridge.current()?.let { it.isPlaying && it.error == null } == true

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
                if (playingBehind()) {
                    // 23.11 (właściciel: „okno nie pozostawia możliwości wyjścia, gdy kanał już leciał w tle”): obraz jest — tylko zamknij okno.
                    Log.i(TAG, "kanał nie odpowiada: Wstecz → okno zamknięte, kanał gra dalej (${channel.value?.name})")
                    channel.value = null
                } else {
                    Log.i(TAG, "kanał nie odpowiada: Wstecz → okno zamknięte, powrót do listy (${channel.value?.name})")
                    channel.value = null
                    exit?.invoke()
                }
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> if (down) selected.intValue = (selected.intValue - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (down) selected.intValue = (selected.intValue + 1).coerceAtMost(buttons().lastIndex)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> when {
                down && event.repeatCount == 0 -> okDown = true
                !down && okDown -> {
                    okDown = false
                    buttons().getOrNull(selected.intValue)?.let { run(state, it, exit) }
                }
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
    LaunchedEffect(failed) {
        if (failed == null) return@LaunchedEffect
        ForgeChannelFailed.shown()
        // 23.11: obraz ruszył po pokazaniu okna — okno znika samo (sprawdzane co sekundę, póki jest na ekranie).
        while (ForgeChannelFailed.channel.value === failed) {
            kotlinx.coroutines.delay(1_000L)
            if (ForgeChannelFailed.channel.value === failed && ForgeChannelFailed.playingBehind()) {
                Log.i("ForkLiveTv", "kanał nie odpowiada: ${failed.name} jednak gra → okno zamknięte samo")
                ForgeChannelFailed.channel.value = null
            }
        }
    }
    if (com.nuvio.tv.skin.api.SkinPreferences.skin.value == com.nuvio.tv.skin.api.SkinId.NIUNIA) {
        if (failed != null) com.nuvio.tv.skin.niunia.live.NiuniaPlayerLiveError(state, failed) // Z-14 PlayerLiveError
        return
    }
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
