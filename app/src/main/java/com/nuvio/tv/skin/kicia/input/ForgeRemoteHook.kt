package com.nuvio.tv.skin.kicia.input

import android.util.Log
import android.view.KeyEvent
import com.nuvio.tv.BuildConfig
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * The single entry point for remote buttons the app takes for itself. MainActivity calls [onKey] first
 * (one line, marked `forge hook`).
 *
 * - Debug builds write every key press to the log under the tag `ForgeKeys`, so the exact code a given
 *   remote sends can be read with `adb logcat -s ForgeKeys`.
 * - A voice key (Search / Assistant / Voice Assistant) is taken ONLY when a screen is listening to
 *   [voiceRequests]; otherwise it passes through untouched, so the button never becomes dead.
 */
object ForgeRemoteHook {
    private const val TAG = "ForgeKeys"

    @Volatile
    var keys: ForgeRemoteKeys = ForgeRemoteKeys()

    private val voice = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the user pressed a voice button; the Search screen collects this and starts listening. */
    val voiceRequests: SharedFlow<Unit> = voice

    @Volatile
    private var voiceWanted = false

    /** The bar opens Search for a voice key and leaves this flag for the Search screen, which starts listening. */
    fun requestVoiceOnSearch() {
        voiceWanted = true
    }

    /** The assistant session asks for listening while the app is already running. */
    fun triggerVoice() {
        voice.tryEmit(Unit)
    }

    /** Looks at the flag without clearing it (the bar uses it on a cold start to open Search). */
    fun peekVoiceWanted(): Boolean = voiceWanted

    /** True once if a voice key asked for listening; read by the Search screen when it opens. */
    fun consumeVoiceWanted(): Boolean = voiceWanted.also { voiceWanted = false }

    fun onKey(event: KeyEvent, redispatch: ((KeyEvent) -> Boolean)? = null): Boolean {
        if (BuildConfig.IS_DEBUG_BUILD) Log.d(TAG, ForgeRemoteKeys.describe(event))
        com.nuvio.tv.skin.kicia.player.ForgePlayerFocusRescue.onKey(event) // player lost the focus (controls hid): give it back first
        // A computer keyboard (emulator, USB/Bluetooth keyboard): Esc works as Back. Remotes never send Esc.
        if (event.keyCode == KeyEvent.KEYCODE_ESCAPE && redispatch != null) {
            redispatch(KeyEvent(event.downTime, event.eventTime, event.action, KeyEvent.KEYCODE_BACK, event.repeatCount, event.metaState))
            return true
        }
        val action = keys.actionFor(event.keyCode, event.scanCode) ?: return false
        if (action == ForgeRemoteAction.VoiceSearch) {
            if (voice.subscriptionCount.value == 0) return false
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) voice.tryEmit(Unit)
            return true
        }
        return false
    }
}
