package com.nuvio.tv.skin.niunia.components

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.pow

/**
 * Kolory plakatu liczone z jego pikseli (decyzja właściciela 19.11):
 *  - [tab]: średnia pikseli POD etykietą = środek górnej krawędzi widocznego kadru (50% szer. × 9% wys.),
 *  - [tabText]: #111 albo #fff wg luminancji [tab] (próg 0,22),
 *  - [dominant]: kolor dolnej części kadru (najliczniejszy z domieszką nasycenia), przyciemniony do v ≤ 0,42 — do gradientów.
 * Liczy na małej kopii obrazu (30×45, bez sprzętowej bitmapy), wynik w pamięci podręcznej.
 */
data class ArtColors(val tab: Color, val tabText: Color, val dominant: Color)

private const val TAG = "ForkNiunia"
private val cache = LruCache<String, ArtColors>(400)
val NiuniaFallbackColors = ArtColors(Color(0xFFDDDDDD), Color(0xFF111111), Color(0xFF333333))

@Composable
fun rememberArtColors(url: String?, cardAspect: Float, fallback: String? = null): ArtColors {
    val context = LocalContext.current
    val key = "$url@$cardAspect"
    var colors by remember(key) { mutableStateOf(cache.get(key) ?: NiuniaFallbackColors) }
    LaunchedEffect(key) {
        if (url.isNullOrBlank() || cache.get(key) != null) return@LaunchedEffect
        val result = try {
            val request = ImageRequest.Builder(context).data(url).size(60, 90).allowHardware(false)
                .memoryCacheKey("niunia-sample:$url")
                .apply { if (!fallback.isNullOrBlank() && fallback != url) memoryCacheKeyExtras(mapOf(com.nuvio.tv.core.image.CustomPosterFallbackInterceptor.FALLBACK_URL_KEY to fallback)) }
                .build()
            SingletonImageLoader.get(context).execute(request)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e // kafel zniknął z ekranu (przewijanie) — to nie błąd, log zalewał ForkNiunia (test JMGO build155)
        } catch (e: Exception) {
            Log.w(TAG, "próbka koloru: ${e.javaClass.simpleName}: ${e.message}"); null
        }
        val bitmap = (result as? SuccessResult)?.image?.toBitmap()
        if (bitmap == null) {
            Log.w(TAG, "próbka koloru: brak obrazu (${result?.javaClass?.simpleName})"); return@LaunchedEffect
        }
        val computed = withContext(Dispatchers.Default) { sample(bitmap, cardAspect) }
        cache.put(key, computed)
        colors = computed
    }
    return colors
}

/** Kadr jak ContentScale.Crop dla karty o proporcji [cardAspect] (szer./wys.). */
internal fun sample(bmp: Bitmap, cardAspect: Float): ArtColors {
    val w = bmp.width; val h = bmp.height
    val imgAspect = w.toFloat() / h
    val cw: Float; val ch: Float
    if (imgAspect > cardAspect) { ch = h.toFloat(); cw = h * cardAspect } else { cw = w.toFloat(); ch = w / cardAspect }
    val x0 = (w - cw) / 2f; val y0 = (h - ch) / 2f
    val tab = average(bmp, x0 + cw * .25f, y0, x0 + cw * .75f, y0 + max(1f, ch * .09f))
    val low = average(bmp, x0, y0 + ch * .62f, x0 + cw, y0 + ch)
    val tabText = niuniaTabTextColor(tab)
    return ArtColors(tab, tabText, darken(low, .42f))
}

private fun average(bmp: Bitmap, l: Float, t: Float, r: Float, b: Float): Color {
    var rs = 0L; var gs = 0L; var bs = 0L; var n = 0
    val x1 = l.toInt().coerceIn(0, bmp.width - 1); val x2 = r.toInt().coerceIn(x1 + 1, bmp.width)
    val y1 = t.toInt().coerceIn(0, bmp.height - 1); val y2 = b.toInt().coerceIn(y1 + 1, bmp.height)
    for (y in y1 until y2) for (x in x1 until x2) {
        val p = bmp.getPixel(x, y)
        rs += (p shr 16) and 0xFF; gs += (p shr 8) and 0xFF; bs += p and 0xFF; n++
    }
    if (n == 0) return Color(0xFF333333)
    return Color((rs / n).toInt(), (gs / n).toInt(), (bs / n).toInt())
}

/** Tekst etykiety na kolorze spod niej (WPIECIE.md §5): jasne tło (luminancja > 0,22) → czarny #111, ciemne → biały. */
internal fun niuniaTabTextColor(tab: Color): Color = if (luminance(tab) > .22f) Color(0xFF111111) else Color(0xFFFFFFFF)

private fun luminance(c: Color): Float {
    fun lin(v: Float) = if (v <= .04045f) v / 12.92f else ((v + .055f) / 1.055f).toDouble().pow(2.4).toFloat()
    return .2126f * lin(c.red) + .7152f * lin(c.green) + .0722f * lin(c.blue)
}

private fun darken(c: Color, maxV: Float): Color {
    val m = maxOf(c.red, c.green, c.blue)
    if (m <= maxV || m == 0f) return c
    val k = maxV / m
    return Color(c.red * k, c.green * k, c.blue * k)
}
