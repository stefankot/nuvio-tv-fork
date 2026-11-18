package com.nuvio.tv.skin.kicia.tokens

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Typography

/**
 * v7.4 phase 2: Nuvio's own screens (the ones the mockup does not draw) read their text styles from the Material typography. This aligns that
 * typography with the Forge scale in one place: line heights 12/16, 14/20, 16/22, 20/26, 32/38, 40/44, no letter spacing, weights 500 and 700 only.
 */
@OptIn(ExperimentalTvMaterial3Api::class)
fun forgeAlignTypography(t: Typography): Typography = t.copy(
    displayLarge = align(t.displayLarge), displayMedium = align(t.displayMedium), displaySmall = align(t.displaySmall),
    headlineLarge = align(t.headlineLarge), headlineMedium = align(t.headlineMedium), headlineSmall = align(t.headlineSmall),
    titleLarge = align(t.titleLarge), titleMedium = align(t.titleMedium), titleSmall = align(t.titleSmall),
    bodyLarge = align(t.bodyLarge), bodyMedium = align(t.bodyMedium), bodySmall = align(t.bodySmall),
    labelLarge = align(t.labelLarge), labelMedium = align(t.labelMedium), labelSmall = align(t.labelSmall),
)

private fun align(s: TextStyle): TextStyle {
    val line = when (s.fontSize.value.toInt()) {
        12 -> 16f
        14 -> 20f
        16 -> 22f
        20 -> 26f
        32 -> 38f
        40 -> 44f
        else -> s.lineHeight.value
    }
    val weight = when {
        s.fontWeight == null -> FontWeight.Medium
        s.fontWeight!! >= FontWeight.SemiBold -> FontWeight.Bold
        else -> FontWeight.Medium
    }
    return s.copy(lineHeight = if (s.lineHeight.isSpecified) line.sp else s.lineHeight, fontWeight = weight, letterSpacing = 0.sp)
}

private val androidx.compose.ui.unit.TextUnit.isSpecified: Boolean get() = this != androidx.compose.ui.unit.TextUnit.Unspecified
