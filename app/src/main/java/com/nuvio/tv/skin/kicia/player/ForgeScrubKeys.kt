package com.nuvio.tv.skin.kicia.player

import android.os.SystemClock
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.nuvio.tv.skin.kicia.input.isOkKeyCode
import kotlinx.coroutines.delay

/** Observable wrapper around [ForgeScrubDriver] for Compose. Create it with [rememberForgeScrub]. */
@Stable
class ForgeScrubController internal constructor(
    internal val driver: ForgeScrubDriver,
    private val stateHolder: () -> ForgeScrubState,
    private val holdingHolder: () -> Boolean,
    internal val setHolding: (Boolean) -> Unit,
) {
    val state: ForgeScrubState get() = stateHolder()
    val holding: Boolean get() = holdingHolder()
}

@Composable
fun rememberForgeScrub(
    positionMs: Long,
    durationMs: Long,
    edgeMs: Long = durationMs,
    onSeek: (Long) -> Unit,
): ForgeScrubController {
    var state by remember { mutableStateOf(ForgeScrubState(positionMs, durationMs, edgeMs)) }
    var holding by remember { mutableStateOf(false) }
    val seek by rememberUpdatedState(onSeek)
    val driver = remember { ForgeScrubDriver(state, { seek(it) }, { state = it }) }
    LaunchedEffect(positionMs, durationMs, edgeMs) {
        if (!driver.isHolding) driver.updatePlayback(positionMs, durationMs, edgeMs)
    }
    // The remote only auto-repeats after ~500 ms, so a timer paces the steps (every 200 ms).
    LaunchedEffect(holding) {
        while (holding) {
            delay(ForgeScrubRates.TickMs)
            driver.tick(SystemClock.uptimeMillis())
        }
    }
    return remember(driver) { ForgeScrubController(driver, { state }, { holding }, { holding = it }) }
}

/**
 * Key rules of the bar: Left/Right step or hold, Up passes through (to CC), OK confirms the preview
 * (or calls [onTogglePause] when there is none), Back cancels the preview. Down and everything else pass through.
 */
fun Modifier.forgeScrubKeys(
    controller: ForgeScrubController,
    onTogglePause: () -> Unit,
    // forge hook v7.5 (owner 13.11, test: one ▲ did not reach CC): explicit targets for ▲ and ▼; true = moved
    onUp: (() -> Boolean)? = null,
    onDown: (() -> Boolean)? = null,
): Modifier = onPreviewKeyEvent { event ->
    val code = event.key.nativeKeyCode
    val down = event.type == KeyEventType.KeyDown
    val up = event.type == KeyEventType.KeyUp
    when {
        code == AndroidKeyEvent.KEYCODE_DPAD_LEFT || code == AndroidKeyEvent.KEYCODE_DPAD_RIGHT -> {
            if (down) {
                controller.driver.arrowDown(code == AndroidKeyEvent.KEYCODE_DPAD_RIGHT, event.nativeKeyEvent.eventTime)
                controller.setHolding(true)
            } else if (up) {
                controller.driver.arrowUp()
                controller.setHolding(false)
            }
            true
        }
        // forge hook v7.5 (owner 13.11): Up always leaves the bar for the CC / audio row (ZACHOWANIE 5.8); it no longer opens the preview marker.
        code == AndroidKeyEvent.KEYCODE_DPAD_UP -> if (event.type == KeyEventType.KeyDown && onUp != null) onUp() else false
        code == AndroidKeyEvent.KEYCODE_DPAD_DOWN && onDown != null -> if (event.type == KeyEventType.KeyDown) onDown() else false
        isOkKeyCode(code) -> {
            if (up) {
                if (!controller.driver.ok()) onTogglePause()
            }
            true
        }
        code == AndroidKeyEvent.KEYCODE_BACK && controller.state.inPreview -> {
            if (up) controller.driver.back()
            true
        }
        else -> false
    }
}
