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
    onOk: () -> Unit,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (focused) scale else 1f, tween(180), label = "niuniaScale")
    val interaction = remember { MutableInteractionSource() }
    val withRequester = if (focusRequester != null) this.focusRequester(focusRequester) else this
    return withRequester
        .zIndex(if (focused) 1f else 0f)
        .onFocusChanged { st ->
            if (st.isFocused != focused) {
                focused = st.isFocused
                onFocus?.invoke(st.isFocused)
            }
        }
        .graphicsLayer {
            scaleX = s; scaleY = s
            translationY = if (lift && focused) -4.dp.toPx() * ((s - 1f) / (scale - 1f).coerceAtLeast(0.001f)) else 0f
        }
        .shadow(if (focused) 18.dp else 6.dp, shape, clip = false)
        .clickable(interactionSource = interaction, indication = null, onClick = onOk)
}
