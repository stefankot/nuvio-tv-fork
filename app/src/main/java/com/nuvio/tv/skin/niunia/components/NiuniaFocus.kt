package com.nuvio.tv.skin.niunia.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims

/**
 * Fokus Niunii: powiększenie (karty 1,10 i uniesienie o 4 dp, przyciski 1,06) + miękki cień, bez obwódki.
 * OK pilota = `clickable` (Compose obsługuje DPAD_CENTER i Enter).
 */
@Composable
fun Modifier.niuniaFocusable(
    shape: Shape,
    scale: Float = NiuniaDims.ScaleFocus,
    lift: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFocus: ((Boolean) -> Unit)? = null,
    traceName: String = "niunia",
    onOk: () -> Unit,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val trace = com.nuvio.tv.fork.perf.ForkFocusTrace.on // 24.11 UX JMGO: pomiar fokusu (wyłączony = bez dodatkowych modyfikatorów)
    val pos = remember { arrayOfNulls<androidx.compose.ui.layout.LayoutCoordinates>(1) }
    val s by animateFloatAsState(if (focused) scale else 1f, tween(180), label = "niuniaScale")
    val interaction = remember { MutableInteractionSource() }
    val withRequester = if (focusRequester != null) this.focusRequester(focusRequester) else this
    return withRequester
        .zIndex(if (focused) 1f else 0f)
        .then(if (trace) Modifier.onGloballyPositioned { pos[0] = it } else Modifier)
        .onFocusChanged { st ->
            if (st.isFocused && com.nuvio.tv.fork.perf.ForkFocusTrace.on) com.nuvio.tv.fork.perf.ForkFocusTrace.focused(traceName, niuniaBounds(pos[0]))
            if (st.isFocused != focused) {
                focused = st.isFocused
                onFocus?.invoke(st.isFocused)
            }
        }
        .graphicsLayer {
            scaleX = s; scaleY = s
            translationY = if (lift && focused) -4.dp.toPx() * ((s - 1f) / (scale - 1f).coerceAtLeast(0.001f)) else 0f
        }
        .shadow(if (focused) (if (lift) 18.dp else 10.dp) else (if (lift) 6.dp else 0.dp), shape, clip = false)
        .clickable(interactionSource = interaction, indication = null, onClick = onOk)
}
