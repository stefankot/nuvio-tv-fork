package com.nuvio.tv.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class NuvioRadiusTokens(
    val none: Dp,
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val panel: Dp,
    val full: Dp
)

@Immutable
data class NuvioShapeTokens(
    val posterCard: Shape,
    val backdropCard: Shape,
    val collectionCard: Shape,
    val button: Shape,
    val iconButton: Shape,
    val chip: Shape,
    val badge: Shape,
    val dialog: Shape,
    val sidePanel: Shape,
    val sidebar: Shape,
    val navItem: Shape,
    val progress: Shape,
    val slider: Shape,
    val field: Shape,
    val menu: Shape,
    val circle: Shape
)

object NuvioRadii {
    val tokens = NuvioRadiusTokens(
        none = 0.dp,
        xxs = 2.dp,
        xs = 4.dp,
        sm = 8.dp,
        md = if (com.nuvio.tv.skin.kicia.ForgeUi.ON) 8.dp else 12.dp, // forge hook v7.4: radii 8 / 16 / full
        lg = if (com.nuvio.tv.skin.kicia.ForgeUi.ON) 16.dp else 14.dp, // forge hook v7.4
        xl = 16.dp,
        xxl = if (com.nuvio.tv.skin.kicia.ForgeUi.ON) 16.dp else 20.dp, // forge hook v7.4
        panel = if (com.nuvio.tv.skin.kicia.ForgeUi.ON) 16.dp else 28.dp, // forge hook v7.4
        full = 999.dp
    )
}

object NuvioShapes {
    val tokens = NuvioShapeTokens(
        posterCard = RoundedCornerShape(NuvioRadii.tokens.md),
        backdropCard = RoundedCornerShape(NuvioRadii.tokens.xl),
        collectionCard = RoundedCornerShape(NuvioRadii.tokens.xl),
        button = RoundedCornerShape(NuvioRadii.tokens.md),
        iconButton = RoundedCornerShape(NuvioRadii.tokens.md),
        chip = RoundedCornerShape(NuvioRadii.tokens.full),
        badge = RoundedCornerShape(NuvioRadii.tokens.xs),
        dialog = RoundedCornerShape(NuvioRadii.tokens.xl),
        sidePanel = RoundedCornerShape(NuvioRadii.tokens.xxl),
        sidebar = RoundedCornerShape(30.dp),
        navItem = RoundedCornerShape(NuvioRadii.tokens.full),
        progress = RoundedCornerShape(NuvioRadii.tokens.xxs),
        slider = RoundedCornerShape(NuvioRadii.tokens.full),
        field = RoundedCornerShape(NuvioRadii.tokens.md),
        menu = RoundedCornerShape(NuvioRadii.tokens.lg),
        circle = CircleShape
    )
}
