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

    fun run(context: Context, call: VoiceCommandCall): Boolean {
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
