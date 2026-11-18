package com.nuvio.tv.skin.kicia.components

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Makieta 7.7 A8: every UI element (buttons, Pigułki, Pola, Wiersze, Panele, Okna, Popovery, Komunikaty) has an inner 1 dp edge, white 12%. */
val ForgeEdgeColor = Color(0x1FFFFFFF)

fun Modifier.forgeEdge(shape: Shape): Modifier = border(1.dp, ForgeEdgeColor, shape)

/** Makieta 7.7 A7: every card (poster, wide, episode, channel, YouTube) casts `0 2 10 rgba(0,0,0,.55)`, focused or not. */
fun Modifier.forgeCardShadow(shape: Shape): Modifier = shadow(
    elevation = 6.dp, shape = shape, clip = false,
    ambientColor = Color(0x8C000000), spotColor = Color(0x8C000000),
)
