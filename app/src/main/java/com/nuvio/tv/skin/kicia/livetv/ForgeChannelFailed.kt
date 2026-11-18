package com.nuvio.tv.skin.kicia.livetv

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
