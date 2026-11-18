package com.nuvio.tv.skin.kicia.components

import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.input.forgeOkKeys
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeMotion

/**
 * Focus look shared by every element: scale 1.07, a 3 dp white outline drawn OUTSIDE the element
 * (never clipped - the parent row must not clip), and a soft shadow.
 * Wide fields pass [scaleOnFocus] = false so they do not cover their neighbours.
 */
/** v7.4 katalog komponentów: a state shown permanently (the mockup's `pokaz-fokus` / `wcisniety`), and the element's name for the measurement. */
enum class ForgeForced { Focus, Pressed }
val LocalForgeForced = androidx.compose.runtime.compositionLocalOf<ForgeForced?> { null }
val LocalForgeTag = androidx.compose.runtime.compositionLocalOf<String?> { null }
/** v7.4: where the focus scale is anchored; shelves use 50% 25% (--zaczep-skali), everything else the centre. */
val LocalForgeScaleOrigin = androidx.compose.runtime.compositionLocalOf { androidx.compose.ui.graphics.TransformOrigin.Center }

/** v7.4: names the first focus box inside [content] (tag = CSS class of the mockup, `class.N` = N-th in document order). */
@Composable
fun ForgeTagged(tag: String, forced: ForgeForced? = null, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalForgeTag provides tag, LocalForgeForced provides forced, content = content)
}

@Composable
fun Modifier.forgeFocusFrame(
    focused: Boolean,
    radius: Dp,
    outline: Boolean = false,
    scaleOnFocus: Boolean = true,
    shadow: Boolean = true, // v7.4: --cien-fokusu na każdym fokusie
    pressed: Boolean = false, // v7.4: --skala-wcisniecia .97 w --czas-wcisniecia
): Modifier {
    val scale by animateFloatAsState(
        targetValue = (if (focused && scaleOnFocus) ForgeDims.FocusScale else 1f) * (if (pressed) ForgeDims.PressScale else 1f),
        animationSpec = if (pressed) androidx.compose.animation.core.tween(ForgeMotion.PressMs) else ForgeMotion.focusSpec(),
        label = "forgeFocusScale",
    )
    val shadowDp = if (focused && shadow) ForgeDims.FocusShadow else 0.dp
    val origin = LocalForgeScaleOrigin.current
    return this
        .graphicsLayer {
            transformOrigin = origin
            scaleX = scale
            scaleY = scale
            shadowElevation = shadowDp.toPx()
            shape = androidx.compose.foundation.shape.RoundedCornerShape(radius)
            clip = false
            ambientShadowColor = Color.Black
            spotShadowColor = ForgeColors.ShadowFocus
        }
        .drawWithContent {
            drawContent()
            if (focused && outline && ForgeDims.FocusBorder.value > 0f) {
                val w = ForgeDims.FocusBorder.toPx()
                drawRoundRect(
                    color = ForgeColors.Focus,
                    topLeft = Offset(-w / 2f, -w / 2f),
                    size = Size(size.width + w, size.height + w),
                    cornerRadius = CornerRadius(radius.toPx() + w / 2f),
                    style = Stroke(width = w),
                )
            }
        }
}

/**
 * A focusable box with the shared focus look and the OK / long OK rule.
 * [content] receives whether the box has focus so it can swap colours (white fill, dark text).
 */
@Composable
fun ForgeFocusBox(
    radius: Dp,
    modifier: Modifier = Modifier,
    outline: Boolean = false,
    scaleOnFocus: Boolean = true,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onLongOk: (() -> Unit)? = null,
    /** Taps work even while the box can't take focus (e.g. the top bar while focus is in the content). */
    tapEnabled: Boolean = enabled,
    onOk: () -> Unit = {},
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val forced = LocalForgeForced.current
    val tag = LocalForgeTag.current
    val shown = focused || forced == ForgeForced.Focus
    Box(
        modifier = modifier
            .forgeFocusFrame(focused = shown, radius = radius, outline = outline, scaleOnFocus = scaleOnFocus, pressed = pressed || forced == ForgeForced.Pressed)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier) // after the frame: the measured bounds include the focus scale
            .onPreviewKeyEvent { e -> // v7.4: wciśnięcie (OK w dół) = skala .97
                if (enabled && com.nuvio.tv.skin.kicia.input.isOkKeyCode(e.key.nativeKeyCode)) pressed = e.type == androidx.compose.ui.input.key.KeyEventType.KeyDown
                false
            }
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .onFocusChanged {
                focused = it.hasFocus
                if (!it.hasFocus) pressed = false
                onFocusChange?.invoke(it.hasFocus)
            }
            .forgeOkKeys(enabled = enabled, onLongOk = onLongOk, onOk = onOk)
            // Mouse / touch (emulator window, phone-style remotes): a tap is OK, a press-and-hold is the long OK.
            .pointerInput(tapEnabled, onOk, onLongOk) {
                if (tapEnabled) {
                    val long = onLongOk
                    detectTapGestures(
                        onTap = { onOk() },
                        onLongPress = if (long != null) { _: androidx.compose.ui.geometry.Offset -> long() } else null,
                    )
                }
            }
            .semantics(mergeDescendants = true) {} // v7.4: the box reads as one element (its text and icon description), for TalkBack and the pilot test
            .focusable(enabled = enabled),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(LocalForgeTag provides null, LocalForgeForced provides null) { content(shown) }
        if (onLongOk != null) ForgeHoldRing(holding = pressed && focused, modifier = Modifier.align(androidx.compose.ui.Alignment.TopEnd)) // makieta 7.7 A6
    }
}
