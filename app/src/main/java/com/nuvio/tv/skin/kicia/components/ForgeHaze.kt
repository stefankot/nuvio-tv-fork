package com.nuvio.tv.skin.kicia.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Backdrop blur of the mockup (`backdrop-filter: blur(28px)`): what lies behind a glass surface is blurred (Gaussian) and tinted.
 * It rides on Haze's RenderEffect, so it exists from Android 12 (API 31); below that a [HazeState] is null and the surface keeps only its tint.
 */
internal fun forgeBlurAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
internal fun rememberForgeHazeState(): HazeState? = remember { if (forgeBlurAvailable()) HazeState() else null }

/** The bar's haze state: the screens below are its source (set in the pill scaffold), the pill the effect. */
internal val LocalForgeNavHaze = staticCompositionLocalOf<HazeState?> { null }

/** The state of the card being drawn: its picture is the source, the caption's footer the effect. */
internal val LocalForgeCardHaze = staticCompositionLocalOf<HazeState?> { null }

internal fun Modifier.forgeHazeSource(state: HazeState?): Modifier = if (state != null) hazeSource(state = state) else this

/** A blurred (when possible) glass surface tinted white at [tintAlpha]; a dark tint would be [Color.Black]. */
internal fun Modifier.forgeGlass(state: HazeState?, tint: Color = Color.White, tintAlpha: Float = 0.16f, radius: Dp = 28.dp): Modifier =
    (if (state != null) hazeEffect(state = state) {
        blurRadius = radius
        noiseFactor = 0.03f
        inputScale = HazeInputScale.Fixed(0.5f)
    } else this).background(tint.copy(alpha = tintAlpha))
