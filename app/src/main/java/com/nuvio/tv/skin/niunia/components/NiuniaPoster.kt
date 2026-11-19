package com.nuvio.tv.skin.niunia.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText

/**
 * NPoster Niunii (canvas: plansza „Poster variants”).
 * Warstwy: grafika → rozmycie od dołu (API 31+, RenderEffect; starsze: bez rozmycia) → odcień koloru dominującego
 * (rogi dolne + środek + przyciemnienie od dołu) → etykieta u góry w kolorze spod niej → stopka.
 * [baked] = grafika ma już tytuł: stopka bez logo i bez tytułu, rozmycie i odcień niskie.
 * Bez [baked]: logo (gdy jest), inaczej tytuł tekstem.
 */
@Composable
fun NiuniaPoster(
    art: String?,
    title: String,
    logo: String?,
    meta: String?,
    label: String?,
    baked: Boolean,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = NiuniaDims.PosterW,
    height: Dp = NiuniaDims.PosterH,
    large: Boolean = false,
    tagline: String? = null,
    onFocus: ((Boolean) -> Unit)? = null,
    fallback: String? = null,
) {
    val shape = RoundedCornerShape(NiuniaDims.RadiusCard)
    val colors = rememberArtColors(art, width.value / height.value, fallback)
    val context = androidx.compose.ui.platform.LocalContext.current
    val model = androidx.compose.runtime.remember(art, fallback) { niuniaImageRequest(context, art, fallback) }
    Box(
        modifier
            .size(width, height)
            .niuniaFocusable(shape, onFocus = onFocus, onOk = onOk)
            .clip(shape)
            .background(NiuniaColors.Surface)
    ) {
        AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        if (Build.VERSION.SDK_INT >= 31) {
            if (!baked) BlurLayer(model, 6f, .66f, .84f)
            BlurLayer(model, if (baked) 8f else 14f, if (baked) .82f else .78f, if (baked) .97f else .96f)
        }
        Tint(colors.dominant, baked)
        if (label != null) {
            Text(
                label, style = NiuniaText.Tab, color = colors.tabText, maxLines = 1,
                modifier = Modifier.align(Alignment.TopCenter)
                    .background(colors.tab, RoundedCornerShape(bottomStart = NiuniaDims.RadiusTab, bottomEnd = NiuniaDims.RadiusTab))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = if (large) 16.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (!baked) {
                if (!logo.isNullOrBlank()) {
                    AsyncImage(
                        model = logo, contentDescription = title, contentScale = ContentScale.Fit,
                        modifier = Modifier.widthIn(max = if (large) 220.dp else 112.dp).heightIn(max = if (large) 64.dp else 36.dp),
                    )
                } else {
                    Text(title.uppercase(), style = NiuniaText.PosterTitle, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
            if (!meta.isNullOrBlank()) Text(meta, style = NiuniaText.PosterMeta, maxLines = 1)
            if (large && !tagline.isNullOrBlank()) Text(tagline, style = NiuniaText.Body.copy(fontSize = NiuniaText.Caption.fontSize), maxLines = 1)
        }
    }
}

/** Druga kopia grafiki rozmyta i widoczna tylko od [from] (przejście do [to]) wysokości. */
@Composable
private fun BoxScope.BlurLayer(art: Any?, radius: Float, from: Float, to: Float) {
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer {
                renderEffect = BlurEffect(radius.dp.toPx(), radius.dp.toPx(), TileMode.Clamp)
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Transparent, from to Color.Transparent, to to Color.Black, 1f to Color.Black), blendMode = BlendMode.DstIn)
            }
    ) {
        AsyncImage(model = art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun BoxScope.Tint(dom: Color, baked: Boolean) {
    val deep = Color(dom.red * .3f, dom.green * .3f, dom.blue * .3f)
    Box(
        Modifier.matchParentSize().drawBehind {
            val w = size.width; val h = size.height
            val cornerR = w * if (baked) .6f else .75f
            drawRect(Brush.radialGradient(listOf(dom, Color.Transparent), center = Offset(0f, h), radius = cornerR))
            drawRect(Brush.radialGradient(listOf(dom, Color.Transparent), center = Offset(w, h), radius = cornerR))
            if (!baked) drawRect(Brush.radialGradient(listOf(dom.copy(alpha = .35f), Color.Transparent), center = Offset(w / 2, h * .8f), radius = w * .65f))
            drawRect(Brush.verticalGradient(0f to Color.Transparent, (if (baked) .72f else .60f) to Color.Transparent, 1f to deep))
        }
    )
}

/**
 * Zapytanie o grafikę z zapasem: Nuvio podmienia plakaty na wzór z ustawień (np. kaptain.stream), a przy błędzie
 * wraca do oryginału (CustomPosterFallbackInterceptor, klucz w memoryCacheKeyExtras) — tak samo jak ContentCard.
 */
fun niuniaImageRequest(context: android.content.Context, art: String?, fallback: String?): coil3.request.ImageRequest =
    coil3.request.ImageRequest.Builder(context).data(art).apply {
        if (!fallback.isNullOrBlank() && fallback != art) {
            memoryCacheKeyExtras(mapOf(com.nuvio.tv.core.image.CustomPosterFallbackInterceptor.FALLBACK_URL_KEY to fallback))
        }
    }.build()

/**
 * Plakat pionowy pozycji: (grafika, zapas). Pozycje poziome (posterShape ≠ POSTER) dostają pionowy plakat ze wzoru
 * z ustawień (bez tego poziomy obraz był przycinany do pionu); zapas = oryginalny plakat.
 */
fun niuniaPortrait(item: com.nuvio.tv.domain.model.MetaPreview, pattern: String): Pair<String?, String?> {
    if (item.posterShape == com.nuvio.tv.domain.model.PosterShape.POSTER || pattern.isBlank()) return item.poster to item.rawPosterUrl
    val url = try {
        com.nuvio.tv.core.poster.CustomPosterUrlResolver.resolve(
            pattern, com.nuvio.tv.core.poster.CustomPosterUrlResolver.extractIds(item.id, item.imdbId), item.type.toApiString(item.rawType), "poster",
        )
    } catch (e: Exception) {
        android.util.Log.w("ForkNiunia", "plakat pionowy ${item.id}: ${e.javaClass.simpleName}: ${e.message}"); null
    }
    return (url ?: item.poster) to (item.rawPosterUrl ?: item.poster)
}
