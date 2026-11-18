package com.nuvio.tv.skin.kicia.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.screens.player.autosync.bubble.AutoSyncBubbleKind
import com.nuvio.tv.ui.screens.player.autosync.bubble.AutoSyncBubbleMessage
import com.nuvio.tv.ui.screens.player.autosync.bubble.AutoSyncBubbleToasts
import kotlinx.coroutines.delay

/**
 * The subtitles button of the player (the "CC" place) that also shows AutoSync / AudioSync progress (owner 9.11):
 * idle = CC, working = three pulsing dots on a blue tint, done = check on green, failed = "!" on amber, plus a short
 * caption above. It reads Nuvio's own AutoSync message stream, so no AutoSync logic is duplicated. The old bubble
 * is not drawn in the Forge player (see AutoSyncBubbleToastHost), this button replaces it.
 */
@Composable
fun ForgeSubtitleButton(modifier: Modifier = Modifier, onOk: () -> Unit) {
    val message by AutoSyncBubbleToasts.current.collectAsState()
    var shown by remember { mutableStateOf<AutoSyncBubbleMessage?>(null) }
    LaunchedEffect(message) {
        val m = message
        shown = m
        if (m != null) {
            // The bubble used to clear its own message; without it the button clears the state itself.
            delay(
                when (m.kind) {
                    AutoSyncBubbleKind.Working -> 30_000L
                    AutoSyncBubbleKind.Success -> 5_000L
                    AutoSyncBubbleKind.Failure -> 7_000L
                },
            )
            if (shown?.id == m.id) shown = null
        }
    }
    val kind = shown?.kind
    val size = 36.dp // v7.4 (05/13): Przycisk okrągły 36 przy tytule
    ForgeFocusBox(radius = size / 2, modifier = modifier.size(size), onOk = onOk) { focused ->
        val tint = when (kind) {
            AutoSyncBubbleKind.Working -> Color(0x6178AAFF)
            AutoSyncBubbleKind.Success -> ForgeColors.Ok
            AutoSyncBubbleKind.Failure -> ForgeColors.Gold
            null -> ForgeColors.Surface
        }
        val bg by animateColorAsState(if (focused) ForgeColors.Focus else tint, tween(220), label = "subBg")
        val fg = when {
            focused -> ForgeColors.OnFocus
            kind == AutoSyncBubbleKind.Success -> Color(0xFF06240F)
            kind == AutoSyncBubbleKind.Failure -> Color(0xFF3A2A00)
            else -> ForgeColors.Text
        }
        Box(Modifier.size(size).clip(RoundedCornerShape(size / 2)).background(bg), contentAlignment = Alignment.Center) {
            val iconSize = size * ForgeDims.IconRatio
            when (kind) {
                AutoSyncBubbleKind.Working -> PulsingDots(color = fg, modifier = Modifier.size(iconSize))
                AutoSyncBubbleKind.Success -> Icon(Icons.Filled.Check, contentDescription = "Napisy dopasowane", tint = fg, modifier = Modifier.size(iconSize))
                AutoSyncBubbleKind.Failure -> Icon(Icons.Outlined.ErrorOutline, contentDescription = "Nie udało się dopasować napisów", tint = fg, modifier = Modifier.size(iconSize))
                null -> Icon(Icons.Outlined.ClosedCaption, contentDescription = "Napisy", tint = fg, modifier = Modifier.size(iconSize))
            }
        }
        val caption = when (kind) {
            AutoSyncBubbleKind.Working -> "Dopasowuję napisy…"
            AutoSyncBubbleKind.Success -> shown?.headline?.takeIf { it.isNotBlank() } ?: "Napisy dopasowane"
            AutoSyncBubbleKind.Failure -> "Nie udało się dopasować napisów"
            null -> null
        }
        if (caption != null) {
            Box(
                Modifier.requiredWidth(260.dp).offset(x = (size - 260.dp) / 2, y = -(size + 4.dp)),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text(
                    caption,
                    style = ForgeText.captionBold.copy(
                        color = when (kind) {
                            AutoSyncBubbleKind.Success -> ForgeColors.Ok
                            AutoSyncBubbleKind.Failure -> ForgeColors.Gold
                            else -> ForgeColors.Text
                        },
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.wrapContentSize().clip(RoundedCornerShape(8.dp)).background(Color(0xEB28282A)).padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

/** Three dots that pulse one after another (the "matching in progress" bar). */
@Composable
private fun PulsingDots(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "subDots")
    val a0 by t.animateFloat(.25f, 1f, infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "d0")
    val a1 by t.animateFloat(.25f, 1f, infiniteRepeatable(tween(500, delayMillis = 160, easing = LinearEasing), RepeatMode.Reverse), label = "d1")
    val a2 by t.animateFloat(.25f, 1f, infiniteRepeatable(tween(500, delayMillis = 320, easing = LinearEasing), RepeatMode.Reverse), label = "d2")
    Canvas(modifier) {
        val r = size.minDimension * 0.11f
        val y = size.height / 2f
        drawCircle(color.copy(alpha = a0), r, Offset(size.width * 0.2f, y))
        drawCircle(color.copy(alpha = a1), r, Offset(size.width * 0.5f, y))
        drawCircle(color.copy(alpha = a2), r, Offset(size.width * 0.8f, y))
    }
}
