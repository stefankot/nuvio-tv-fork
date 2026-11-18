package com.nuvio.tv.skin.kicia.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
