package com.nuvio.tv.ui.screens.player

import android.util.Log
import com.nuvio.tv.data.local.StreamAutoPlayMode
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.flow.first

/**
 * forge (owner 13.11, P2P as a quiet fallback): in automatic mode, when the debrid provider cannot give the link for a
 * torrent file (error, stale, not cached), the same file is played over P2P instead of failing. With the viewer's P2P
 * consent this is silent apart from a toast; without it the window „Dostawca nie odpowiada” asks once per title, during
 * the film (owner 13.11, makieta 7.5 05 state `p2p`): „Przełącz na P2P” gives the consent and plays the same file.
 */
internal suspend fun PlayerRuntimeController.forgeP2pFallback(stream: Stream, reason: String): Stream? {
    if (streamAutoPlayModeSetting != StreamAutoPlayMode.SMART) return null
    if (stream.infoHash.isNullOrBlank()) return null
    if (contentType in listOf("tv", "channel", "cloud")) return null
    // granted: the window just said yes; the consent is saved asynchronously, so do not read it back yet
    val allowed = stream.infoHash in ForgeP2pConsent.granted || runCatching { torrentSettings.settings.first().p2pEnabled }.getOrDefault(false)
    Log.i("SmartRecovery", "p2p-fallback reason=$reason consent=$allowed")
    if (!allowed) {
        if (ForgeP2pConsent.askedFor.add(stream.infoHash!!)) ForgeP2pConsent.pending.value = ForgeP2pConsent.Ask(stream, this, null)
        return null
    }
    com.nuvio.tv.skin.kicia.components.ForgeToast.show("Oglądam dalej innym sposobem", top = true, note = "Serwis przestał wysyłać ten plik, więc pobieram go od innych użytkowników (P2P).")
    return stream
}

/** The open consent question; shown by [ForgeP2pConsentDialog] in the player. */
internal object ForgeP2pConsent {
    data class Ask(val stream: Stream, val player: PlayerRuntimeController, val positionMs: Long?)
    val pending = androidx.compose.runtime.mutableStateOf<Ask?>(null)
    val askedFor = java.util.Collections.synchronizedSet(HashSet<String>())
    val granted = java.util.Collections.synchronizedSet(HashSet<String>())
}

@androidx.compose.runtime.Composable
internal fun ForgeP2pConsentDialog() {
    val ask = ForgeP2pConsent.pending.value
    com.nuvio.tv.skin.kicia.components.ForgeDialog(
        visible = ask != null,
        title = "Film się zatrzymał",
        message = "Mogę dokończyć go innym sposobem, od tego samego miejsca.",
        note = "Serwis, z którego oglądasz, przestał wysyłać ten plik. Inny sposób (P2P) pobiera go od innych użytkowników.",
        buttons = listOf(
            com.nuvio.tv.skin.kicia.components.ForgeDialogButton("Oglądaj dalej", primary = true) {
                val a = ForgeP2pConsent.pending.value ?: return@ForgeDialogButton
                ForgeP2pConsent.pending.value = null
                a.stream.infoHash?.let { ForgeP2pConsent.granted.add(it) }
                a.player.torrentSettings.setP2pEnabled(true)
                a.player.autoSourceRecovery?.cancel()
                a.player.switchToSourceStream(a.stream, resumeSavedProgress = a.positionMs == null, startPositionMs = a.positionMs)
            },
            com.nuvio.tv.skin.kicia.components.ForgeDialogButton("Inne źródło") {
                val a = ForgeP2pConsent.pending.value
                ForgeP2pConsent.pending.value = null
                a?.player?.showSourcesPanel()
            },
        ),
        onDismiss = { ForgeP2pConsent.pending.value = null },
    )
}

/**
 * forge (owner 13.11, M3): the debrid provider drops the file during the film (an HTTP error after the first frame).
 * In automatic mode the same file (its torrent) plays on over P2P from the same spot: at once with consent, else after
 * the window. True when this took over the error.
 */
internal fun PlayerRuntimeController.forgeP2pOnProviderFailure(httpStatus: Int?): Boolean {
    if (streamAutoPlayModeSetting != StreamAutoPlayMode.SMART || httpStatus == null) return false
    if (contentType in listOf("tv", "channel", "cloud")) return false // Live TV and archive have their own recovery
    if (!hasRenderedFirstFrame || _uiState.value.isTorrentStream) return false
    val playing = forgeSelectedStream ?: smartTrySession?.current ?: return false
    val hash = playing.infoHash?.takeIf { it.isNotBlank() } ?: return false
    val torrent = playing.copy(url = null, externalUrl = null)
    val at = currentPlaybackPositionMs()
    Log.i("SmartRecovery", "p2p-fallback reason=provider-http-$httpStatus at=${at ?: -1}")
    // SmartDeviceProbe mirrors the viewer's P2P consent here (no DataStore read on the player's error callback)
    val allowed = hash in ForgeP2pConsent.granted || com.nuvio.tv.core.streams.smart.SmartPolicyHolder.current.p2pAllowed
    if (!allowed) {
        if (!ForgeP2pConsent.askedFor.add(hash)) return false
        ForgeP2pConsent.pending.value = ForgeP2pConsent.Ask(torrent, this, at)
        setPlaybackPaused(true)
        return true
    }
    com.nuvio.tv.skin.kicia.components.ForgeToast.show("Oglądam dalej innym sposobem", top = true, note = "Serwis przestał wysyłać ten plik, więc pobieram go od innych użytkowników (P2P).")
    forgeSelectedStream = torrent
    switchToSourceStream(torrent, resumeSavedProgress = false, startPositionMs = at)
    return true
}
