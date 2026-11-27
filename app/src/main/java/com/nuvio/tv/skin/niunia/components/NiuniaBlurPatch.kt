package com.nuvio.tv.skin.niunia.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.transformations
import com.nuvio.tv.fork.images.NativeBlur
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Przegląd 27.11 (właściciel, ~16:55: „rozmycia przykrywają za dużo powierzchni zdjęć wszędzie — ustaw absolutnie minimalne
 * powierzchnie niezbędne dla czytelności małych napisów”): łata rozmycia tylko pod blokiem drobnego tekstu. Pod elementem
 * rysowany jest rozmyty WYCINEK zdjęcia, które leży pod spodem (ta sama gotowa rozmyta kopia co tło pod hero — liczona raz,
 * pamięć Coil + dysk), z miękkimi brzegami [feather] albo przycięty do [shape] (panel = wycinek rozmytego obrazu za nim).
 * Zdjęcie pod spodem: na cały ekran ([frame] = null) albo w ramce [frame] (np. karta), zawsze kadrowane jak `ContentScale.Crop`.
 * Bez rozmycia na żywo — działa na Androidzie 11. Klucz `niunia_blur_scrims` wyłącza (wołający wraca do dawnego wyglądu).
 */
class NiuniaBlurFrame {
    var coords: LayoutCoordinates? by mutableStateOf(null)
}

/** Ramka zdjęcia dla łat [niuniaBlurPatch] z [NiuniaBlurFrame] (element, w którym zdjęcie wypełnia całość jak Crop). */
fun Modifier.niuniaBlurFrame(frame: NiuniaBlurFrame): Modifier = onGloballyPositioned { frame.coords = it }

/** Czy łaty rozmycia są włączone (klucz forka + gotowe rozmycie na tym urządzeniu). */
@Composable
fun niuniaBlurPatchesOn(): Boolean {
    val ctx = LocalContext.current
    return remember { ForkFeatures.enabled(ctx, "niunia_blur_scrims", true) && NativeBlur.enabled(ctx) }
}

@Composable
fun Modifier.niuniaBlurPatch(
    url: String?,
    frame: NiuniaBlurFrame? = null,
    feather: Dp = 20.dp,
    tint: Color = Color(0x33000000),
    shape: Shape? = null,
    radius: Dp = 60.dp,
): Modifier {
    val on = niuniaBlurPatchesOn()
    if (url.isNullOrBlank() || !on) return this
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val cfg = LocalConfiguration.current
    val px = with(density) { radius.toPx() }
    val req = remember(url, px) {
        // Rozmiar = ekran (jak tło hero), żeby trafić w gotowe rozmycie w pamięci Coil; inaczej odczyt z dysku.
        val w = (cfg.screenWidthDp * density.density).toInt().coerceAtLeast(1)
        val h = (cfg.screenHeightDp * density.density).toInt().coerceAtLeast(1)
        ImageRequest.Builder(ctx).data(url).size(w, h).transformations(NativeBlur(px, diskKey = url)).build()
    }
    val painter = rememberAsyncImagePainter(req)
    var pos by remember { mutableStateOf(Offset.Unspecified) }
    var frameSize by remember { mutableStateOf(IntSize.Zero) }
    if (!patchLogged) { patchLogged = true; Log.i("ForkNiunia", "łata rozmycia: tylko pod drobnym tekstem (zamiast rozmycia dużej części zdjęcia)") }
    val tintFilter = if (tint.alpha > 0f) ColorFilter.tint(tint, BlendMode.SrcAtop) else null
    return this
        .onGloballyPositioned { c ->
            val f = frame?.coords?.takeIf { it.isAttached } ?: c.findRootCoordinates()
            pos = try { f.localPositionOf(c, Offset.Zero) } catch (e: IllegalArgumentException) { Log.w("ForkNiunia", "łata rozmycia: ${e.javaClass.simpleName}: ${e.message}"); Offset.Unspecified }
            frameSize = f.size
        }
        .drawBehind {
            val p = pos
            val isz = painter.intrinsicSize
            if (p == Offset.Unspecified || frameSize == IntSize.Zero || isz == Size.Unspecified || isz.width <= 0f || isz.height <= 0f) return@drawBehind
            val fw = frameSize.width.toFloat(); val fh = frameSize.height.toFloat()
            val s = maxOf(fw / isz.width, fh / isz.height)
            val dw = isz.width * s; val dh = isz.height * s
            val ox = (fw - dw) / 2f - p.x; val oy = (fh - dh) / 2f - p.y
            if (shape != null) {
                val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }
                clipPath(path) { translate(ox, oy) { with(painter) { draw(Size(dw, dh), colorFilter = tintFilter) } } }
                return@drawBehind
            }
            val f = feather.toPx()
            val l = -f; val t = -f; val r = size.width + f; val b = size.height + f
            drawContext.canvas.saveLayer(Rect(l, t, r, b), Paint())
            clipRect(l, t, r, b) { translate(ox, oy) { with(painter) { draw(Size(dw, dh), colorFilter = tintFilter) } } }
            if (f > 0f) {
                val kx = (f / (r - l)).coerceIn(0f, .5f); val ky = (f / (b - t)).coerceIn(0f, .5f)
                drawRect(Brush.horizontalGradient(0f to Color.Transparent, kx to Color.Black, 1f - kx to Color.Black, 1f to Color.Transparent, startX = l, endX = r),
                    topLeft = Offset(l, t), size = Size(r - l, b - t), blendMode = BlendMode.DstIn)
                drawRect(Brush.verticalGradient(0f to Color.Transparent, ky to Color.Black, 1f - ky to Color.Black, 1f to Color.Transparent, startY = t, endY = b),
                    topLeft = Offset(l, t), size = Size(r - l, b - t), blendMode = BlendMode.DstIn)
            }
            drawContext.canvas.restore()
        }
}

@Volatile private var patchLogged = false
