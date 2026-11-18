@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.nuvio.tv.skin.kicia.components

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/**
 * Card with the shared focus look (v6: scale 1.125 + shadow, no outline).
 * Sizes follow the grid: poster 130x194, wide 160x114, channel tile 96x96, YouTube 242 wide.
 * The image / text goes into [content]; the card clips only its own content, never the outline.
 */
@Composable
fun ForgeCard(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onLongOk: (() -> Unit)? = null,
    enabled: Boolean = true,
    onOk: () -> Unit,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(ForgeDims.RadiusCard)
    ForgeFocusBox(
        radius = ForgeDims.RadiusCard,
        modifier = modifier.size(width, height).graphicsLayer { alpha = if (enabled) 1f else .5f },
        enabled = enabled,
        outline = true,
        focusRequester = focusRequester,
        onFocusChange = onFocusChange,
        onLongOk = onLongOk,
        onOk = onOk,
    ) { focused ->
        val cardHaze = rememberForgeHazeState() // owner 10.11: the footer blurs the card's own picture
        androidx.compose.runtime.CompositionLocalProvider(LocalForgeCardHaze provides cardHaze) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(width, height).forgeCardShadow(shape).clip(shape).background(ForgeColors.CardBase), // makieta 7.7 A7: stały cień karty
            ) {
                content(focused)
            }
        }
    }
}

@Composable
fun ForgePosterCard(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onLongOk: (() -> Unit)? = null,
    onOk: () -> Unit,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) = ForgeCard(ForgeDims.Poster, ForgeDims.PosterHeight, modifier, focusRequester, onFocusChange, onLongOk, onOk = onOk, content = content)

@Composable
fun ForgeWideCard(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onLongOk: (() -> Unit)? = null,
    onOk: () -> Unit,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) = ForgeCard(ForgeDims.Wide, ForgeDims.WideHeight, modifier, focusRequester, onFocusChange, onLongOk, onOk = onOk, content = content)

/**
 * A shelf: heading (16/22 bold), 16 dp gap, a row of cards starting at the 40 dp content edge,
 * 20 dp between cards. The row has vertical padding so the enlarged focused card and its outline
 * are not cut off by the row's own bounds.
 */
@Composable
fun ForgeShelf(
    title: String,
    modifier: Modifier = Modifier,
    /** More room under the heading for cards that grow a lot on focus (the big posters would cover it). */
    extraGap: androidx.compose.ui.unit.Dp = 0.dp,
    tag: String = "", // v7.4: testTag tytułu (polka-tytul.N)
    /** test 15.11 (B-1): Home scrolls the first shelf back before it moves the focus there. */
    state: androidx.compose.foundation.lazy.LazyListState? = null,
    content: LazyListScope.() -> Unit,
) {
    // v6: the title of the shelf that holds focus is white, the others 50% (colour change 240 ms).
    var active by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val titleColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (active) ForgeColors.Text else ForgeColors.Text3,
        animationSpec = androidx.compose.animation.core.tween(240),
        label = "forgeShelfTitle",
    )
    Column(modifier = modifier.fillMaxWidth().onFocusChanged { active = it.hasFocus }.then(if (tag.startsWith("polka-tytul.")) Modifier.testTag("polka." + tag.substringAfter('.')) else Modifier)) { // v7.4: półka = polka.N
        Text(
            text = title,
            style = ForgeText.shelfTitle.copy(color = titleColor),
            modifier = Modifier.fillMaxWidth().padding(start = ForgeDims.ContentStart).then(if (tag.isEmpty()) Modifier else Modifier.testTag(tag)), // v7.4: tytuł półki = blok 920 dp
        )
        Spacer(Modifier.height(ForgeDims.ShelfHeaderToCards - ShelfBleed + extraGap))
        // A page that scrolls itself (Home) turns off bring-into-view for its column; rows keep the app's own.
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.foundation.gestures.LocalBringIntoViewSpec provides
                (LocalForgeRowBringIntoView.current ?: androidx.compose.foundation.gestures.LocalBringIntoViewSpec.current),
            LocalForgeScaleOrigin provides androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.25f), // v7.4: .polka .sk { transform-origin: 50% 25% }
        ) {
            LazyRow(
                state = state ?: androidx.compose.foundation.lazy.rememberLazyListState(),
                contentPadding = PaddingValues(
                    start = ForgeDims.ContentStart,
                    end = ForgeDims.ContentEnd,
                    top = ShelfBleed,
                    bottom = ShelfBleed,
                ),
                horizontalArrangement = Arrangement.spacedBy(ForgeDims.Gap),
                content = content,
            )
        }
    }
}

/** Room above and below a row for the focus scale (1.07), the 3 dp outline and the shadow. */
private val ShelfBleed = 10.dp

/** Bring-into-view for shelf rows, set by a page that turns it off for its own column (see ForgeHomeContent). */
@Suppress("DEPRECATION")
val LocalForgeRowBringIntoView = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.foundation.gestures.BringIntoViewSpec?> { null }

/* ===== v6: shared caption / blur-like footer / rating for cards with text on the image (docs/makieta-tv/v6-appletv/PROMPT §5) ===== */

/**
 * The lower part of a card under its caption: a Gaussian blur of the card's own picture (backdrop filter, Android 12+; the picture is the haze
 * source) faded in from the top by a mask, under a dark gradient. Below Android 12 only the gradient remains.
 */
@Composable
fun BoxScope.ForgeCardFooterScrim(heightFraction: Float = .52f) {
    val haze = LocalForgeCardHaze.current
    val gradient = androidx.compose.ui.graphics.Brush.verticalGradient(
        0f to androidx.compose.ui.graphics.Color.Transparent,
        0.55f to androidx.compose.ui.graphics.Color.Black.copy(alpha = if (haze != null) .28f else .45f),
        1f to androidx.compose.ui.graphics.Color.Black.copy(alpha = if (haze != null) .6f else .78f),
    )
    androidx.compose.foundation.layout.Box(
        Modifier
            .align(androidx.compose.ui.Alignment.BottomCenter)
            .fillMaxWidth()
            .fillMaxHeight(heightFraction)
            .then(
                if (haze != null) {
                    Modifier
                        .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            // The blur fades in from the top: full from 45% of the height, none at the top edge.
                            drawRect(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    0f to androidx.compose.ui.graphics.Color.Transparent,
                                    0.45f to androidx.compose.ui.graphics.Color.Black,
                                    1f to androidx.compose.ui.graphics.Color.Black,
                                ),
                                blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
                            )
                        }
                        .hazeEffect(state = haze) {
                            blurRadius = 22.dp
                            noiseFactor = 0.02f
                            inputScale = dev.chrisbanes.haze.HazeInputScale.Fixed(0.5f)
                        }
                } else Modifier
            )
            .background(gradient),
    )
}

/**
 * Small caption hugging the bottom edge (title 12 bold, line 12 regular; 7 dp from the edge). [progress] draws the tiny 26x2 dp bar
 * next to the line (Continue watching); [ratingPercent] adds the Rotten Tomatoes score (shown only when present).
 */
/** v7.4 podpis karty: tytuł 14/20 700, linia 12/16 70%, odstęp 8; Pasek postępu 4 dp na całą szerokość przy dolnej krawędzi. */
@Composable
fun BoxScope.ForgeCardCaption(
    title: String,
    line: String? = null,
    progress: Float? = null,
    ratingPercent: Int? = null,
    modifier: Modifier = Modifier,
    bottom: Dp = 12.dp,
) {
    Column(
        modifier
            .align(androidx.compose.ui.Alignment.BottomStart)
            .fillMaxWidth()
            .background(androidx.compose.ui.graphics.Brush.verticalGradient(0f to androidx.compose.ui.graphics.Color.Transparent, 1f to androidx.compose.ui.graphics.Color.Black.copy(alpha = .72f))) // v7.4: .karta-podpis gradient do --podpis-gora
            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = bottom), // v7.4: Pozioma 12 (z paskiem postępu i bez), Kanał 8
    ) {
        Text(title, style = ForgeText.label, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        if (line != null || ratingPercent != null) {
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                if (line != null) Text(line, style = ForgeText.caption, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (ratingPercent != null) {
                    Spacer(Modifier.size(6.dp))
                    Text("\uD83C\uDF45 $ratingPercent%", style = ForgeText.captionBold)
                }
            }
        }
    }
    if (progress != null) {
        androidx.compose.foundation.layout.Box(
            Modifier.align(androidx.compose.ui.Alignment.BottomCenter).fillMaxWidth().height(4.dp).background(ForgeColors.Surface3),
        ) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).background(androidx.compose.ui.graphics.Color.White))
        }
    }
}
@Composable
fun ForgeBigCard(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onLongOk: (() -> Unit)? = null,
    onOk: () -> Unit,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val shape = RoundedCornerShape(ForgeDims.RadiusBigCard)
    ForgeFocusBox(
        radius = ForgeDims.RadiusBigCard,
        modifier = modifier.size(ForgeDims.BigCard, ForgeDims.BigCardHeight),
        outline = true,
        focusRequester = focusRequester,
        onFocusChange = onFocusChange,
        onLongOk = onLongOk,
        onOk = onOk,
    ) { focused ->
        val cardHaze = rememberForgeHazeState()
        androidx.compose.runtime.CompositionLocalProvider(LocalForgeCardHaze provides cardHaze) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(ForgeDims.BigCard, ForgeDims.BigCardHeight).clip(shape).background(ForgeColors.CardBase),
            ) { content(focused) }
        }
    }
}

/**
 * v7.4 rozsunięcie grupy: the neighbours of the focused element move away by half of what the focus scale adds to its width
 * (the mockup's --rozsun), so a focused button never covers the next one. Wrap every element of the row in [ForgeSpreadScope.Item].
 */
class ForgeSpreadScope {
    internal var focused by androidx.compose.runtime.mutableIntStateOf(-1)
    internal val widths = androidx.compose.runtime.mutableStateMapOf<Int, Int>()

    @Composable
    fun Item(index: Int, content: @Composable () -> Unit) {
        val target = if (focused < 0 || index == focused) 0f else (widths[focused] ?: 0) * (ForgeDims.FocusScale - 1f) / 2f * (if (index > focused) 1f else -1f)
        val dx by androidx.compose.animation.core.animateFloatAsState(target, com.nuvio.tv.skin.kicia.tokens.ForgeMotion.focusSpec(), label = "forgeSpread")
        androidx.compose.foundation.layout.Box(
            Modifier
                .onFocusChanged { if (it.hasFocus) focused = index else if (focused == index) focused = -1 }
                .onSizeChanged { widths[index] = it.width }
                .graphicsLayer { translationX = dx },
        ) { content() }
    }
}

@Composable
fun ForgeSpreadRow(modifier: Modifier = Modifier, gap: Dp = 12.dp, alignEnd: Boolean = false, content: @Composable ForgeSpreadScope.() -> Unit) {
    val scope = androidx.compose.runtime.remember { ForgeSpreadScope() }
    androidx.compose.foundation.layout.Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(gap, if (alignEnd) androidx.compose.ui.Alignment.End else androidx.compose.ui.Alignment.Start),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) { scope.content() }
}


/** v7.4 Karta/Kanał 240 × 135: numer i Plakietka w lewym górnym rogu (12, 12), podpis na dole (8); bez fokusu przyciemniona 30%. */
@Composable
fun ForgeChannelCard(
    number: String,
    name: String,
    programme: String?,
    live: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onLongOk: (() -> Unit)? = null,
    onOk: () -> Unit,
    picture: @Composable BoxScope.() -> Unit = {},
) {
    ForgeCard(ForgeDims.ChannelCard, ForgeDims.ChannelCardHeight, modifier, focusRequester, onFocusChange, onLongOk, enabled, onOk) { focused ->
        picture()
        if (!focused) androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .3f)))
        androidx.compose.foundation.layout.Row(
            Modifier.align(androidx.compose.ui.Alignment.TopStart).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(number, style = ForgeText.captionBold.copy(color = ForgeColors.Text2), maxLines = 1)
            if (live) ForgeBadge("Na żywo", ForgeBadgeKind.Live)
        }
        ForgeCardCaption(name, programme, bottom = 8.dp)
    }
}

/** v7.4 Karta/Dalej 160 × 90: „21:30 · Tytuł” przy dolnej krawędzi (odstęp 12); bez fokusu przyciemniona 30%. */
@Composable
fun ForgeNextCard(
    time: String,
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onOk: () -> Unit,
    picture: @Composable BoxScope.() -> Unit = {},
) {
    ForgeCard(ForgeDims.NextCard, ForgeDims.NextCardHeight, modifier, focusRequester, null, null, enabled, onOk) { focused ->
        picture()
        if (!focused) androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = .3f)))
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontSize = 12.sp, color = ForgeColors.Text2)); append("$time · "); pop(); append(title)
            },
            style = ForgeText.label, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomStart).padding(12.dp),
        )
    }
}

/** v7.4 Karta/Dzień 56 × 64: nazwa dnia 12/16 (70%) nad numerem 20/26; bez tła w spoczynku, wybrany 24%, fokus wypełniony. */
@Composable
fun ForgeDayCard(
    name: String,
    number: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onOk: () -> Unit,
) {
    ForgeFocusBox(
        radius = ForgeDims.RadiusCard,
        modifier = modifier.size(ForgeDims.DayCard, ForgeDims.DayCardHeight).graphicsLayer { alpha = if (enabled) 1f else .5f },
        scaleOnFocus = false,
        enabled = enabled,
        focusRequester = focusRequester,
        onOk = onOk,
    ) { focused ->
        Column(
            Modifier.size(ForgeDims.DayCard, ForgeDims.DayCardHeight).clip(RoundedCornerShape(ForgeDims.RadiusCard))
                .background(if (focused) ForgeColors.Focus else if (selected) ForgeColors.Surface3 else androidx.compose.ui.graphics.Color.Transparent),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Text(name, style = ForgeText.caption.copy(color = if (focused) ForgeColors.OnFocus else ForgeColors.Text2), maxLines = 1)
            Text(number, style = ForgeText.head.copy(color = if (focused) ForgeColors.OnFocus else ForgeColors.Text), maxLines = 1)
        }
    }
}
