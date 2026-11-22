package com.nuvio.tv.fork.images

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Gotowe rozmycie (22.11, właściciel: „rozmycie tła liczyć raz przy wczytaniu i trzymać w cache Coil”).
 * Transformacja Coil: obraz zmniejszony do [scale] i rozmyty natywnie (RenderEffect na RenderNode, rysowany przez
 * HardwareRenderer do ImageReader — GPU, Android 12+). Wynik (mała bitmapa) trafia do pamięci Coil pod kluczem z promieniem,
 * więc przy przewijaniu i powrotach nic nie jest liczone drugi raz — zamiast RenderEffect w każdej klatce.
 * Android 11 i starsze: nie używać (ZASADY: rozmycie tylko natywnie; tam zwykłe tło skórki) — [available] = false.
 */
class NativeBlur(private val radiusPx: Float, private val scale: Float = 0.25f) : Transformation() {
    override val cacheKey: String = "fork_native_blur_${radiusPx.toInt()}_${(scale * 100).toInt()}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        if (Build.VERSION.SDK_INT < 31) return input
        return try { blur(input) } catch (e: Exception) {
            Log.w(TAG, "rozmycie (${input.width}×${input.height}, r=$radiusPx): ${e.javaClass.simpleName}: ${e.message} — obraz bez rozmycia")
            input
        }
    }

    @RequiresApi(31)
    private fun blur(input: Bitmap): Bitmap {
        val w = (input.width * scale).toInt().coerceAtLeast(1)
        val h = (input.height * scale).toInt().coerceAtLeast(1)
        val r = (radiusPx * scale).coerceAtLeast(1f)
        val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 1, HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT)
        val node = RenderNode("forkBlur")
        val renderer = HardwareRenderer()
        try {
            node.setPosition(0, 0, w, h)
            node.setRenderEffect(RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP))
            val canvas = node.beginRecording()
            canvas.drawBitmap(input, null, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
            node.endRecording()
            renderer.setSurface(reader.surface)
            renderer.setContentRoot(node)
            renderer.createRenderRequest().setWaitForPresent(true).syncAndDraw()
            val image = reader.acquireNextImage() ?: throw IllegalStateException("brak klatki")
            try {
                val hb = image.hardwareBuffer ?: throw IllegalStateException("brak bufora")
                try {
                    val hw = Bitmap.wrapHardwareBuffer(hb, ColorSpace.get(ColorSpace.Named.SRGB)) ?: throw IllegalStateException("bitmapa")
                    val out = hw.copy(Bitmap.Config.ARGB_8888, false)
                    hw.recycle()
                    if (logged < 3) { logged++; Log.i(TAG, "rozmycie gotowe raz: ${input.width}×${input.height} → ${w}×${h}, r=${r.toInt()} px (pamięć Coil)") }
                    return out
                } finally { hb.close() }
            } finally { image.close() }
        } finally {
            renderer.destroy(); node.discardDisplayList(); reader.close()
        }
    }

    companion object {
        private const val TAG = "ForkImages"
        const val KEY = "images_preblur"
        @Volatile private var logged = 0
        val available: Boolean get() = Build.VERSION.SDK_INT >= 31
        fun enabled(c: android.content.Context) = available && com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, KEY, true)
    }
}
