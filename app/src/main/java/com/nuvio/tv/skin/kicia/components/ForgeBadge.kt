package com.nuvio.tv.skin.kicia.components

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/** v7.4 Plakietka kinds (SYSTEM): one size, 22 dp, 12/700, full pill. */
enum class ForgeBadgeKind(val bg: Color, val fg: Color) {
    Live(ForgeColors.Live, Color.White),
    Archive(ForgeColors.Ok, ForgeColors.OnFocus),
    Unavailable(Color.White.copy(alpha = .12f), Color.White.copy(alpha = .7f)),
    Soon(Color.White.copy(alpha = .24f), Color.White),
    Quality(Color.White.copy(alpha = .12f), Color.White.copy(alpha = .7f)),
    /** makieta 7.7b H6: „Z przesunięciem −N min”, always yellow. */
    Shift(ForgeColors.Shift, ForgeColors.OnFocus),
    /** makieta 7.7b H18: a badge in a card's corner over the picture (Obejrzane, N odc., Teraz, Niedostępne), readable on a bright frame. */
    OnImage(ForgeColors.BadgeOnImage, Color.White),
}

@Composable
fun ForgeBadge(text: String, kind: ForgeBadgeKind, modifier: Modifier = Modifier) {
    Text(
        text,
        style = ForgeText.captionBold.copy(fontSize = 12.sp, lineHeight = 16.sp, color = kind.fg),
        maxLines = 1,
        modifier = modifier.height(22.dp).clip(CircleShape).background(kind.bg).padding(horizontal = 8.dp, vertical = 3.dp) // v7.4: od-8,
    )
}

/**
 * v7.6 (owner 13.11): "na żywo" as a small red dot that blinks slowly (no text).
 */
@androidx.compose.runtime.Composable
fun ForgeLiveDot(modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "live-dot")
    val alpha by transition.animateFloat(
        initialValue = 1f, targetValue = .25f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(800), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "alpha",
    )
    androidx.compose.foundation.layout.Box(
        modifier.size(12.dp).graphicsLayer { this.alpha = alpha }
            .background(ForgeColors.Live, androidx.compose.foundation.shape.CircleShape),
    )
}
