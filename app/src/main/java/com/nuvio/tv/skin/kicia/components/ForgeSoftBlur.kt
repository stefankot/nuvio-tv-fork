package com.nuvio.tv.skin.kicia.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

/**
 * Backdrop blur for Android 11 and below (owner 10.11: the JMGO is API 30, where RenderEffect — and so Haze — does not exist).
 * The screen under the top bar is recorded into a layer; every [IntervalMs] the strip under the bar is rendered to a small bitmap, blurred in
 * software (1/8 size, three box-blur passes: close to a Gaussian) and kept; the bar draws it behind its tint. Costs one small bitmap every half second.
 */
@Stable
internal class ForgeSoftBlur(private val layer: GraphicsLayer, private val strip: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
    var contentSize by mutableStateOf(IntSize.Zero)
    var blurred by mutableStateOf<ImageBitmap?>(null)
        private set
    var blurredSize by mutableStateOf(IntSize.Zero)
        private set

    /** The content's own drawing goes through the recording layer. */
    fun Modifier.source(): Modifier = onGloballyPositioned { origin = it.positionInRoot(); contentSize = it.size }
        .drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
        }

    suspend fun refresh(density: Density, direction: LayoutDirection, stripHeightPx: Int) {
        val width = contentSize.width
        if (width <= 0 || stripHeightPx <= 0) return
        val result = runCatching {
            strip.record(density, direction, IntSize(width, stripHeightPx)) { drawLayer(layer) }
            val raw = strip.toImageBitmap().asAndroidBitmap()
            val soft = raw.copy(Bitmap.Config.ARGB_8888, false) ?: return
            val w = (width / 8).coerceAtLeast(8)
            val h = (stripHeightPx / 8).coerceAtLeast(4)
            val small = Bitmap.createScaledBitmap(soft, w, h, true)
            val px = IntArray(w * h)
            small.getPixels(px, 0, w, 0, 0, w, h)
            repeat(3) { boxBlur(px, w, h, 3) }
            small.setPixels(px, 0, w, 0, 0, w, h)
            small.asImageBitmap()
        }.getOrNull() ?: return
        blurred = result
        blurredSize = IntSize(width, stripHeightPx)
    }

    companion object {
        const val IntervalMs = 500L
    }
}

/** A horizontal then a vertical box blur of radius [r] on ARGB pixels. */
private fun boxBlur(px: IntArray, w: Int, h: Int, r: Int) {
    val tmp = IntArray(px.size)
    fun pass(src: IntArray, dst: IntArray, len: Int, count: Int, stride: Int, step: Int) {
        for (line in 0 until count) {
            val base = line * stride
            for (i in 0 until len) {
                var a = 0; var rr = 0; var g = 0; var b = 0; var n = 0
                for (k in -r..r) {
                    val j = (i + k).coerceIn(0, len - 1)
                    val c = src[base + j * step]
                    a += c ushr 24; rr += (c shr 16) and 0xFF; g += (c shr 8) and 0xFF; b += c and 0xFF; n++
                }
                dst[base + i * step] = ((a / n) shl 24) or ((rr / n) shl 16) or ((g / n) shl 8) or (b / n)
            }
        }
    }
    pass(px, tmp, w, h, w, 1)       // rows
    pass(tmp, px, h, w, 1, w)       // columns
}

@Composable
internal fun rememberForgeSoftBlur(): ForgeSoftBlur? {
    if (forgeBlurAvailable()) return null
    val layer = rememberGraphicsLayer()
    val strip = rememberGraphicsLayer()
    return remember(layer, strip) { ForgeSoftBlur(layer, strip) }
}

internal val LocalForgeNavSoftBlur = staticCompositionLocalOf<ForgeSoftBlur?> { null }

/** The blurred strip behind the node at its own position (drawn first; the tint goes on top). */
@Composable
internal fun Modifier.forgeSoftBackdrop(soft: ForgeSoftBlur?): Modifier {
    if (soft == null) return this
    var position by remember { mutableStateOf(Offset.Zero) }
    return this.onGloballyPositioned { position = it.positionInRoot() }.drawBehind {
        val image = soft.blurred ?: return@drawBehind
        drawImage(
            image = image,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset((soft.origin.x - position.x).roundToInt(), (soft.origin.y - position.y).roundToInt()),
            dstSize = soft.blurredSize,
            filterQuality = FilterQuality.High,
        )
    }
}
