package com.nuvio.tv.fork.voice

import android.app.Activity
import android.content.Context
import android.view.KeyEvent
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
        "guide.show" to com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE, "now.list" to com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE,
    )

    @Volatile var lastMessage: String? = null

    fun run(context: Context, call: VoiceCommandCall): Boolean {
        if (call.id == "subs.mismatch") {
            val msg = com.nuvio.tv.fork.sources.PlayerBridge.subtitleMismatch() ?: return false
            lastMessage = msg; return true
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
