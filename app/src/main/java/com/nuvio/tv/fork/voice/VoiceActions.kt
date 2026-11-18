package com.nuvio.tv.fork.voice

import android.app.Activity
import android.content.Context
import android.view.KeyEvent
import kotlinx.coroutines.launch
import com.nuvio.tv.fork.night.NightModule
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/** Wykonanie prostych poleceń głosowych (B07): klawisze pilota wstrzyknięte do aktywności i tryb nocny. Zwraca true, gdy polecenie obsłużono. */
object VoiceActions {
    private val keys = mapOf(
        "pause" to KeyEvent.KEYCODE_MEDIA_PAUSE, "play" to KeyEvent.KEYCODE_MEDIA_PLAY,
        "seek.back" to KeyEvent.KEYCODE_MEDIA_REWIND, "seek.forward" to KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
        "back" to KeyEvent.KEYCODE_BACK, "nav.down" to KeyEvent.KEYCODE_DPAD_DOWN, "nav.up" to KeyEvent.KEYCODE_DPAD_UP,
        "channel.next" to KeyEvent.KEYCODE_CHANNEL_UP, "channel.prev" to KeyEvent.KEYCODE_CHANNEL_DOWN,
        "ep.next" to KeyEvent.KEYCODE_MEDIA_NEXT, "ep.prev" to KeyEvent.KEYCODE_MEDIA_PREVIOUS,
    )

    private val routes = mapOf(
        "home" to "home", "library" to "library", "search" to "search", "settings" to "settings", "settings.open" to "settings",
        "tv" to com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE, "apps" to com.nuvio.tv.fork.launcher.LauncherModule.APPS_ROUTE,
        "genre.show" to "fork_discover", "archive.movies" to "fork_discover", "now.kind" to "fork_discover", "followed.news" to "fork_discover", "continue" to "fork_discover",
        "mood.light" to "fork_picks", "mood.heavy" to "fork_picks", "mood.gems" to "fork_picks",
        "guide.show" to com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE, "now.list" to com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE,
    )

    @Volatile var lastMessage: String? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    fun run(context: Context, call: VoiceCommandCall): Boolean {
        if (call.id == "subs.bigger" || call.id == "subs.smaller") {
            val size = if (call.id == "subs.bigger") com.nuvio.tv.fork.sources.SubtitleSizeChoice.LARGE else com.nuvio.tv.fork.sources.SubtitleSizeChoice.SMALL
            scope.launch { com.nuvio.tv.fork.sources.SourceChoice.applySubtitleSize(context, size) }
            lastMessage = "Napisy: " + size.title.lowercase(); return true
        }
        if (call.id == "fav.add" || call.id == "fav.remove") {
            val url = com.nuvio.tv.fork.sources.PlayerBridge.streamUrl() ?: return false
            val repo = com.nuvio.tv.reshaped.livetv.LiveTvRepository
            val ch = repo.uiState.value.channels.firstOrNull { it.streamUrl == url } ?: return false
            val isFav = url in repo.uiState.value.favoriteUrls
            if ((call.id == "fav.add") != isFav) repo.toggleFavorite(ch)
            lastMessage = if (call.id == "fav.add") "Dodano do ulubionych: ${ch.name}" else "Usunięto z ulubionych: ${ch.name}"; return true
        }
        if (call.id == "seek.back" || call.id == "seek.forward") {
            if (!com.nuvio.tv.fork.sources.PlayerBridge.active) return false
            val ms = DurationParser.millis(call.params["czas"].orEmpty()) ?: 60_000L
            val to = (com.nuvio.tv.fork.sources.PlayerBridge.positionMs() + if (call.id == "seek.back") -ms else ms).coerceAtLeast(0L)
            com.nuvio.tv.fork.sources.PlayerBridge.send(com.nuvio.tv.ui.screens.player.PlayerEvent.OnSeekTo(to))
            lastMessage = (if (call.id == "seek.back") "Cofam o " else "Przewijam o ") + (ms / 1000) + " s"; return true
        }
        if (call.id == "channel.number") {
            val n = call.params["n"]?.toIntOrNull() ?: return false
            val ch = com.nuvio.tv.reshaped.livetv.LiveTvRepository.uiState.value.channels.getOrNull(n - 1) ?: return false
            com.nuvio.tv.fork.tv.ekrany.PendingPlay.ask(ch.streamUrl)
            ForkFeatures.open?.invoke(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE); lastMessage = ch.name; return true
        }
        if (call.id == "source.other") {
            lastMessage = com.nuvio.tv.fork.sources.TryAnotherSource.run(); return true
        }
        if (call.id == "subs.mismatch") {
            val msg = com.nuvio.tv.fork.sources.PlayerBridge.subtitleMismatch() ?: return false
            lastMessage = msg; return true
        }
        if (call.id == "lektor.on" || call.id == "lektor.off") {
            lastMessage = if (call.id == "lektor.on") com.nuvio.tv.fork.lektor.LektorController.start(context) else { com.nuvio.tv.fork.lektor.LektorController.stop(); "Lektor wyłączony" }; return true
        }
        if (call.id == "channel.play") {
            val url = call.params["kanal_id"] ?: return false
            com.nuvio.tv.fork.tv.ekrany.PendingPlay.ask(url)
            ForkFeatures.open?.invoke(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE)
            return true
        }
        routes[call.id]?.let { r -> ForkFeatures.open?.invoke(r); return true }
        keys[call.id]?.let { code ->
            val a = context as? Activity ?: return false
            a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code)); a.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
            return true
        }
        val night = when (call.id) { "night.on" -> "ON"; "night.off" -> "OFF"; "night.untilmorning" -> "SCHEDULE"; else -> null } ?: return false
        ForkFeatures.setStr(context, "night_mode", night); NightModule.load(context)
        return true
    }
}
