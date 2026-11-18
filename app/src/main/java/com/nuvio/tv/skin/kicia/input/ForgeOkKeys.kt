package com.nuvio.tv.skin.kicia.input

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

internal fun isOkKeyCode(code: Int): Boolean =
    code == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
        code == AndroidKeyEvent.KEYCODE_ENTER ||
        code == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER ||
        code == AndroidKeyEvent.KEYCODE_BUTTON_A

/**
 * Hooks OK / long OK on a focusable element. [onOk] fires on key-up of a short press,
 * [onLongOk] fires once while the key is still held, as soon as 520 ms have passed.
 * With no [onLongOk] a long hold still counts as a normal OK on release.
 */
fun Modifier.forgeOkKeys(
    enabled: Boolean = true,
    onLongOk: (() -> Unit)? = null,
    onOk: () -> Unit,
): Modifier = composed {
    val tracker = remember { ForgeOkTracker() }
    onPreviewKeyEvent { event ->
        if (!enabled || !isOkKeyCode(event.key.nativeKeyCode)) return@onPreviewKeyEvent false
        val time = event.nativeKeyEvent.eventTime
        when (event.type) {
            KeyEventType.KeyDown -> {
                if (tracker.down(time, event.nativeKeyEvent.isLongPress) == ForgeOkTracker.Result.LONG_PRESS) onLongOk?.invoke()
                true
            }
            KeyEventType.KeyUp -> {
                when (tracker.up(time)) {
                    ForgeOkTracker.Result.CLICK -> onOk()
                    ForgeOkTracker.Result.LONG_PRESS -> if (onLongOk != null) onLongOk() else onOk()
                    ForgeOkTracker.Result.NONE -> Unit
                }
                true
            }
            else -> false
        }
    }
}
