@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.nuvio.tv.skin.niunia.components

import coil3.request.transformations
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims

private const val TAG = "ForkNiunia"

/**
 * Strona z hero Niuni (właściciel 20.11): hero nie jest elementem przewijanej listy (był wyższy niż obszar listy, więc fokus
 * na przycisku przesuwał go o ujemny margines do góry). Trzy warstwy na cały ekran:
 *  - tło (obraz hero), które po zejściu w dół dostaje bardzo mocne rozmycie (API 31+; starsze — mocne przyciemnienie),
 *  - [hero] (napisy, przyciski) — po ▼ wyjeżdża do góry i znika,
 *  - [below] (strona pod hero) — wjeżdża od dołu.
 * ▼ z hero = fokus w [below]; ▲ z góry [below] i Wstecz = z powrotem na [heroFocus].
 */
@Composable
fun NiuniaHeroPage(
    background: Any?,
    heroFocus: FocusRequester,
    what: String,
    hero: @Composable BoxScope.() -> Unit,
    below: @Composable BoxScope.() -> Unit,
    belowFocusRequester: FocusRequester? = null,
    /** Element, na który ▼ z hero ma trafić (np. 1. odcinek — makieta Episodes); null = pierwszy w stronie pod hero. */
    belowEnter: FocusRequester? = null,
    /** Przyciemnienie hero (scrim) — rysowane z tłem, żeby szkło przycisków (fork/glass) widziało przyciemniony obraz; znika razem z [hero]. */
    scrim: (@Composable BoxScope.() -> Unit)? = null,
    /** Z-2 (hero szczegółów): na API 31+ lewa część obrazu rozmyta (`blur-detail` 36, maska `mask-detail`: pełne do 32 %, zanika do 64 %). */
    detailBlur: Boolean = false,
) {
    var down by remember { mutableStateOf(false) }
    val belowFocus = belowFocusRequester ?: remember { FocusRequester() }
    val t by animateFloatAsState(if (down) 1f else 0f, tween(520, easing = FastOutSlowInEasing), label = "niuniaHeroPage")
    val blurOk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val blurPx = with(LocalDensity.current) { 60.dp.toPx() }
    // 22.11: gotowe rozmycie (liczone raz, w pamięci Coil) zamiast RenderEffect w każdej klatce animacji.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val preblur = remember { com.nuvio.tv.fork.images.NativeBlur.enabled(ctx) }
    val blurred = remember(background, preblur) {
        if (preblur && background != null) coil3.request.ImageRequest.Builder(ctx).data(background)
            .transformations(com.nuvio.tv.fork.images.NativeBlur(blurPx)).build() else null
    }
    BackHandler(enabled = down) {
        Log.i(TAG, "$what: Wstecz → hero")
        try { heroFocus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "$what: fokus hero: ${e.javaClass.simpleName}: ${e.message}") }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(NiuniaColors.Bg)) {
        val h = with(LocalDensity.current) { maxHeight.toPx() }
        // Szkło (fork/glass): obraz, rozmycie, przyciemnienie i scrim to warstwa tła dla przycisków i paneli strony.
        Box(with(com.nuvio.tv.fork.glass.ForkGlass) { Modifier.fillMaxSize().forkGlassBackdrop() }) {
        AsyncImage(
            model = background, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                if (blurOk && blurred == null && t > 0.01f) renderEffect = BlurEffect(blurPx * t, blurPx * t, TileMode.Clamp)
                if (t > 0.01f) { scaleX = 1f + 0.08f * t; scaleY = 1f + 0.08f * t } // brzegi rozmycia poza ekranem
            },
        )
        if (detailBlur && blurOk) {
            val detailPx = with(LocalDensity.current) { 36.dp.toPx() }
            LaunchedEffect(Unit) { Log.i(TAG, "$what: effects: blur (lewa część hero rozmyta, maska do 64 %)") }
            AsyncImage(
                model = background, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer { renderEffect = BlurEffect(detailPx, detailPx, TileMode.Clamp); alpha = 1f - t; compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawRect(androidx.compose.ui.graphics.Brush.horizontalGradient(0f to Color.Black, 0.32f to Color.Black, 0.64f to Color.Transparent),
                            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn)
                    },
            )
        }
        if (blurred != null) AsyncImage(
            model = blurred, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = t; scaleX = 1f + 0.08f * t; scaleY = 1f + 0.08f * t },
        )
        if (scrim != null) Box(Modifier.fillMaxSize().graphicsLayer { alpha = 1f - t; translationY = -h * t }, content = scrim)
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = t }.background(if (blurOk || blurred != null) Color(0x8C000000) else Color(0xE62C2C2E)))
        }
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = 1f - t; translationY = -h * t }
                .onFocusChanged { if (it.hasFocus && down) { down = false; Log.i(TAG, "$what: ▲ → hero") } }
                .focusProperties { exit = { dir -> if (dir == FocusDirection.Down) (belowEnter ?: belowFocus) else FocusRequester.Default } }
                .focusGroup(),
            content = hero,
        )
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { translationY = h * (1f - t) }
                .onFocusChanged { if (it.hasFocus && !down) { down = true; Log.i(TAG, "$what: ▼ → pod hero (rozmycie ${if (blurred != null) "gotowe" else if (blurOk) "tak" else "przyciemnienie"})") } }
                .focusRequester(belowFocus)
                .focusProperties { exit = { dir -> if (dir == FocusDirection.Up) heroFocus else FocusRequester.Default } }
                .focusGroup(),
            content = below,
        )
    }
}

/**
 * Przewijanie list Niuni bez „skakania” (zgłoszenie właściciela 20.11): pionowo rząd z fokusem zawsze w tym samym miejscu
 * ([pivot] od góry — tytuł rzędu nad nim), więc ruch w bok w rzędzie nic nie przewija; poziomo — tylko gdy kafel wychodzi
 * poza margines 40 dp, o tyle, ile trzeba.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NiuniaVerticalScroll(pivot: androidx.compose.ui.unit.Dp = 74.dp, content: @Composable () -> Unit) {
    val base = LocalBringIntoViewSpec.current
    val px = with(LocalDensity.current) { pivot.toPx() }
    val spec = remember(base, px) {
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        object : BringIntoViewSpec {
            override val scrollAnimationSpec = base.scrollAnimationSpec
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = when {
                // Element wyższy niż miejsce pod [pivot] (np. panel informacji): tylko tyle, by był widoczny.
                size > containerSize - px -> when {
                    offset < 0f -> offset
                    offset + size > containerSize -> minOf(offset, offset + size - containerSize)
                    else -> 0f
                }
                else -> offset - px
            }
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}

/** Rząd poziomy w liście [NiuniaVerticalScroll]: przewija tylko, gdy kafel wychodzi poza margines. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NiuniaRowScroll(content: @Composable () -> Unit) {
    val base = LocalBringIntoViewSpec.current
    val gutter = with(LocalDensity.current) { NiuniaDims.Gutter.toPx() }
    val spec = remember(base, gutter) {
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        object : BringIntoViewSpec {
            override val scrollAnimationSpec = base.scrollAnimationSpec
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = when {
                offset < gutter -> offset - gutter
                offset + size > containerSize - gutter -> offset + size - (containerSize - gutter)
                else -> 0f
            }
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}
