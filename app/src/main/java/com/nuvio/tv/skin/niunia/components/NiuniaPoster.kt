package com.nuvio.tv.skin.niunia.components

import coil3.request.transformations
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    onLongOk: (() -> Unit)? = null,
    award: com.nuvio.tv.skin.niunia.awards.NiuniaAwardBadge? = null,
) {
    val shape = RoundedCornerShape(com.nuvio.tv.skin.niunia.tokens.NiuniaRadius.Radius16) // DS v2: karty 16
    var focused by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val colors = rememberArtColors(art, width.value / height.value, fallback)
    val context = androidx.compose.ui.platform.LocalContext.current
    val model = androidx.compose.runtime.remember(art, fallback) { niuniaImageRequest(context, art, fallback) }
    Box(
        modifier
            .size(width, height)
            // DS v2: uniesienie 4 dp, ×1,1, cień i obwódka 3 dp; OK / długie OK (520 ms) przez niuniaOk.
            .niuniaOk(title, onFocus = { focused = it; onFocus?.invoke(it) }, onLongClick = onLongOk, onClick = onOk)
            .niuniaFocusLook(NiuniaFocusStyle.Lift, focused, shape)
            .clip(shape)
            .background(NiuniaColors.Surface)
    ) {
        AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        // 22.11: na Androidzie 11 też, gdy rozmycie jest gotowe (liczone raz procesorem, pamięć Coil).
        val preblur = androidx.compose.runtime.remember { com.nuvio.tv.fork.images.NativeBlur.enabled(context) }
        if (Build.VERSION.SDK_INT >= 31 || preblur) {
            if (!baked) BlurLayer(model, 6f, .66f, .84f)
            BlurLayer(model, if (baked) 8f else 14f, if (baked) .82f else .78f, if (baked) .97f else .96f)
        }
        Tint(colors.dominant, baked)
        if (label != null) {
            Text(
                label, style = com.nuvio.tv.skin.niunia.tokens.NiuniaType.Caption, color = colors.tabText, maxLines = 1,
                modifier = Modifier.align(Alignment.TopCenter)
                    .background(colors.tab, RoundedCornerShape(bottomStart = com.nuvio.tv.skin.niunia.tokens.NiuniaRadius.Radius6, bottomEnd = com.nuvio.tv.skin.niunia.tokens.NiuniaRadius.Radius6))
                    .padding(horizontal = com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space8, vertical = com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space4),
            )
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = if (large) 16.dp else 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (award != null) NiuniaAwardTag(award) // Z-13: plakietka nagrody na górze stopki, nad logo
            val foot = niuniaPosterFoot(baked, logo)
            if (foot != NiuniaPosterFoot.None) {
                if (foot == NiuniaPosterFoot.Logo) {
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
    // 22.11: gotowe rozmycie z pamięci Coil (liczone raz przy wczytaniu) zamiast RenderEffect w każdej klatce.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { radius.dp.toPx() }
    val pre = androidx.compose.runtime.remember { com.nuvio.tv.fork.images.NativeBlur.enabled(ctx) }
    val model = androidx.compose.runtime.remember(art, pre, px) {
        if (pre && art is coil3.request.ImageRequest) art.newBuilder(ctx).transformations(com.nuvio.tv.fork.images.NativeBlur(px)).build() else art
    }
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer {
                if (!pre) renderEffect = BlurEffect(radius.dp.toPx(), radius.dp.toPx(), TileMode.Clamp)
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.Transparent, from to Color.Transparent, to to Color.Black, 1f to Color.Black), blendMode = BlendMode.DstIn)
            }
    ) {
        AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
 * Plakat pionowy pozycji: (grafika, zapas). Pozycje poziome (posterShape ≠ POSTER, np. „Więcej podobnych” z Trakt/Simkl/TMDB)
 * dostają pionowy plakat: wzór z ustawień → oryginalny plakat pionowy (rawPosterUrl, gdy nie jest obrazem poziomym) →
 * plakat Cinemeta (metahub) po IMDb. Bez tego poziomy obraz był przycinany do pionu. Brak plakatu (biblioteka Trakt/MDBList)
 * → plakat Cinemeta po IMDb. Zapas = to, co zostaje, gdy wybrana grafika się nie wczyta.
 */
fun niuniaPortrait(item: com.nuvio.tv.domain.model.MetaPreview, pattern: String): Pair<String?, String?> {
    val imdb = (item.imdbId ?: item.id.takeIf { it.startsWith("tt") })?.substringBefore(':')?.takeIf { it.startsWith("tt") }
    val metahub = imdb?.let { "https://images.metahub.space/poster/medium/$it/img" }
    if (item.posterShape == com.nuvio.tv.domain.model.PosterShape.POSTER) {
        val art = item.poster?.takeIf { it.isNotBlank() } ?: item.rawPosterUrl?.takeIf { it.isNotBlank() } ?: metahub
        return art to ((item.rawPosterUrl?.takeIf { it.isNotBlank() && it != art }) ?: metahub?.takeIf { it != art })
    }
    val landscape = setOfNotNull(item.poster, item.landscapePoster, item.background)
    val portrait = item.rawPosterUrl?.takeIf { it.isNotBlank() && it !in landscape } ?: metahub
    val custom = if (pattern.isBlank()) null else try {
        com.nuvio.tv.core.poster.CustomPosterUrlResolver.resolve(
            pattern, com.nuvio.tv.core.poster.CustomPosterUrlResolver.extractIds(item.id, item.imdbId), item.type.toApiString(item.rawType), "poster",
        )
    } catch (e: Exception) {
        android.util.Log.w("ForkNiunia", "plakat pionowy ${item.id}: ${e.javaClass.simpleName}: ${e.message}"); null
    }
    return when {
        custom != null -> custom to (portrait ?: item.poster)
        portrait != null -> portrait to (metahub?.takeIf { it != portrait } ?: item.poster)
        else -> item.poster to item.rawPosterUrl
    }
}


/**
 * Paczka „oczywiste” pkt 10 (21.11): pozycja z poziomą grafiką (posterShape ≠ POSTER) bez wzoru plakatów — prawdziwy pionowy
 * plakat (Cinemeta/metahub po IMDb), zapas = oryginał; bez IMDb — oryginał (przycięty jak dawniej, z logiem).
 */
fun niuniaRealPoster(item: com.nuvio.tv.domain.model.MetaPreview): Pair<String?, String?> {
    if (item.posterShape == com.nuvio.tv.domain.model.PosterShape.POSTER) return item.poster to item.rawPosterUrl
    val imdb = (item.imdbId ?: item.id).takeIf { it.startsWith("tt") }?.substringBefore(':')
    if (imdb == null) { if (posterLogged.add(item.id)) android.util.Log.i("ForkNiunia", "plakat pionowy ${item.id}: brak IMDb — grafika pozioma przycięta"); return item.poster to item.rawPosterUrl }
    return "https://images.metahub.space/poster/medium/$imdb/img" to (item.rawPosterUrl ?: item.poster)
}

private val posterLogged = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

/**
 * Z-16 (P-4): NMediaCard preset `MoreCard` — ostatnia karta rzędu dodatku: rozmiar plakatu, tło `surface`,
 * chevron 28 + „Zobacz wszystko” + nazwa rzędu. Fokus jak plakat (×1,1, uniesienie 4, obwódka 3 dp).
 */
@Composable
fun NiuniaMoreCard(
    rowName: String,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = NiuniaDims.PosterW,
    height: Dp = NiuniaDims.PosterH,
    onFocus: ((Boolean) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(com.nuvio.tv.skin.niunia.tokens.NiuniaRadius.Radius16)
    var focused by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val label = androidx.compose.ui.res.stringResource(com.nuvio.tv.R.string.niunia_ke2_see_all)
    Box(
        modifier
            .size(width, height)
            .niuniaOk("$label: $rowName", onFocus = { focused = it; onFocus?.invoke(it) }, onClick = onOk)
            .niuniaFocusLook(NiuniaFocusStyle.Lift, focused, shape)
            .clip(shape)
            .background(NiuniaColors.Surface),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NiuniaIcon("chevron-right", size = 28.dp)
            Text(label, style = com.nuvio.tv.skin.niunia.tokens.NiuniaType.Label, maxLines = 1, textAlign = TextAlign.Center)
            Text(rowName, style = com.nuvio.tv.skin.niunia.tokens.NiuniaType.Caption, color = com.nuvio.tv.skin.niunia.tokens.NiuniaColor.Text70,
                maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        }
    }
}
