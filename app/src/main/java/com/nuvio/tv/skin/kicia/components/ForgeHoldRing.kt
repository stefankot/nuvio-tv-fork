package com.nuvio.tv.skin.kicia.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Makieta 7.7 A6: the hold ring on an element with a long OK. 120 ms after OK goes down a 24 dp circle appears at the
 * element's top right corner (−8, −8): fill rgba(0,0,0,.38), a white 4 dp arc that fills linearly in 400 ms. It goes
 * when OK is released or once the long OK has fired (120 + 400 = 520 ms, the long-OK threshold of [forgeOkKeys]).
 */
@Composable
fun ForgeHoldRing(holding: Boolean, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(holding) {
        progress.snapTo(0f)
        shown = false
        if (!holding) return@LaunchedEffect
        kotlinx.coroutines.delay(120)
        shown = true
        progress.animateTo(1f, tween(400, easing = LinearEasing))
        shown = false
    }
    if (!shown) return
    Canvas(modifier.offset(x = 8.dp, y = (-8).dp).size(24.dp)) {
        drawCircle(Color(0x61000000))
        val stroke = 4.dp.toPx()
        drawCircle(Color.White.copy(alpha = .36f), radius = (size.minDimension - stroke) / 2f, style = Stroke(stroke))
        drawArc(
            Color.White, startAngle = -90f, sweepAngle = 360f * progress.value, useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f), size = Size(size.width - stroke, size.height - stroke), style = Stroke(stroke),
        )
    }
}
