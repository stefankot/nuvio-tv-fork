package com.nuvio.tv.skin.kicia.livetv

import android.util.Log
import android.view.KeyEvent
import androidx.compose.runtime.Composable
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
    val channel = mutableStateOf<LiveTvChannel?>(null)

    /**
     * UX 22.11: the window is drawn inside the player, whose key handler sees every key first, so the window's own
     * BackHandler never got the first Back (it took two). Called first by the Live TV key handler while the window is
     * up: Back (on release) closes the window and leaves the player for the channel list — behind it there is no
     * picture; OK and the arrows go on to the window's buttons (false). Returns whether the key was used.
     */
    fun onKey(event: KeyEvent, exit: (() -> Unit)?): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_BACK && event.keyCode != KeyEvent.KEYCODE_ESCAPE) return false
        if (event.action == KeyEvent.ACTION_UP) {
            Log.i("ForkLiveTv", "kanał nie odpowiada: Wstecz → okno zamknięte, powrót do listy (${channel.value?.name})")
            channel.value = null
            exit?.invoke()
        }
        return true
    }
}

@Composable
internal fun ForgeChannelFailedDialog(state: LiveTvPlayerState) {
    val failed = ForgeChannelFailed.channel.value
    ForgeDialog(
        visible = failed != null,
        title = "Kanał nie odpowiada",
        message = "Kanał ${failed?.name.orEmpty()} nie wysyła obrazu. Sprawdź połączenie z internetem albo włącz inny kanał.",
        buttons = listOf(
            ForgeDialogButton("Spróbuj ponownie", primary = true) {
                val channel = ForgeChannelFailed.channel.value
                ForgeChannelFailed.channel.value = null
                channel?.let { state.switchTo(it, force = true) }
            },
            ForgeDialogButton("Następny kanał") {
                ForgeChannelFailed.channel.value = null
                state.zapNow(1)
            },
        ),
        onDismiss = { ForgeChannelFailed.channel.value = null },
    )
}
