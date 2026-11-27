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
 * Android 11 i starsze (22.11, właściciel: „Tak, włącz” — zmiana ZASADY §2): to samo rozmycie liczone raz procesorem
 * (stack blur Nuvio, [com.nuvio.tv.ui.util.BlurTransformation]) na obrazie pomniejszonym do [scale] — kilka-kilkanaście ms
 * na obraz, potem gotowa bitmapa z pamięci Coil; w klatkach animacji i przewijania nic nie jest liczone.
 */
class NativeBlur(private val radiusPx: Float, private val scale: Float = 0.25f, private val diskKey: String? = null) : Transformation() {
    override val cacheKey: String = "fork_native_blur_${radiusPx.toInt()}_${(scale * 100).toInt()}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // Przegląd 27.11 (plakaty pkt b): gotowe rozmycie z dysku (po restarcie nie liczone drugi raz).
        val file = diskFile()
        if (file != null) readDisk(file)?.let { return it }
        val out = if (Build.VERSION.SDK_INT < 31) cpu(input, size) else try { blur(input) } catch (e: Exception) {
            Log.w(TAG, "rozmycie GPU (${input.width}×${input.height}, r=$radiusPx): ${e.javaClass.simpleName}: ${e.message} — liczę procesorem")
            cpu(input, size)
        }
        if (file != null && out !== input) saveDisk(file, out)
        return out
    }

    private fun diskFile(): java.io.File? {
        val key = diskKey?.takeIf { it.isNotBlank() } ?: return null
        val d = dir ?: return null
        val md = java.security.MessageDigest.getInstance("SHA-1").digest("$key|$cacheKey".toByteArray())
        return java.io.File(d, md.joinToString("") { "%02x".format(it) } + ".webp")
    }

    private fun readDisk(f: java.io.File): Bitmap? {
        if (!f.exists()) return null
        return try {
            android.graphics.BitmapFactory.decodeFile(f.path)?.also { diskHits++; if (diskHits <= 3 || diskHits % 200 == 0) Log.i(TAG, "rozmycie z dysku (${it.width}×${it.height}), razem $diskHits") }
                ?: run { Log.w(TAG, "rozmycie z dysku: plik nieczytelny — liczę od nowa"); f.delete(); null }
        } catch (e: Exception) { Log.w(TAG, "rozmycie z dysku: ${e.javaClass.simpleName}: ${e.message}"); null }
    }

    private fun saveDisk(f: java.io.File, bmp: Bitmap) {
        try {
            val tmp = java.io.File(f.parentFile, f.name + ".tmp")
            @Suppress("DEPRECATION")
            val fmt = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
            tmp.outputStream().use { bmp.compress(fmt, 80, it) }
            if (!tmp.renameTo(f)) tmp.delete()
            diskSaves++
            if (diskSaves % 500 == 0) trim(f.parentFile)
        } catch (e: Exception) { Log.w(TAG, "zapis rozmycia na dysk: ${e.javaClass.simpleName}: ${e.message}") }
    }

    /** Android 11 (i zapas): pomniejszenie + stack blur, raz na obraz. */
    private suspend fun cpu(input: Bitmap, size: Size): Bitmap = try {
        val w = (input.width * scale).toInt().coerceAtLeast(1)
        val h = (input.height * scale).toInt().coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(input, w, h, true)
        val r = (radiusPx * scale).toInt().coerceIn(1, 60)
        val t0 = android.os.SystemClock.uptimeMillis()
        com.nuvio.tv.ui.util.BlurTransformation(r).transform(small, size).also {
            if (small !== input && small !== it) small.recycle()
            if (logged < 3) { logged++; Log.i(TAG, "rozmycie gotowe raz (procesor): ${input.width}×${input.height} → ${w}×${h}, r=$r px, ${android.os.SystemClock.uptimeMillis() - t0} ms (pamięć Coil)") }
        }
    } catch (e: Exception) {
        Log.w(TAG, "rozmycie procesorem (${input.width}×${input.height}): ${e.javaClass.simpleName}: ${e.message} — obraz bez rozmycia")
        input
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
        const val DISK_KEY = "images_preblur_disk"
        /** Katalog gotowych rozmyć (null = zapis na dysk wyłączony); ustawiany przy pierwszym [enabled]. */
        @Volatile private var dir: java.io.File? = null
        @Volatile private var diskHits = 0
        @Volatile private var diskSaves = 0

        /** Najwyżej ~4000 plików (po kilka KB): przy przekroczeniu usuwa najstarszą 1/4. */
        private fun trim(d: java.io.File?) {
            val files = d?.listFiles { x -> x.name.endsWith(".webp") } ?: return
            if (files.size <= 4000) return
            files.sortedBy { it.lastModified() }.take(files.size / 4).forEach { it.delete() }
            Log.i(TAG, "rozmycia na dysku: usunięto najstarsze ${files.size / 4} z ${files.size}")
        }
        @Volatile private var logged = 0
        /** 22.11: wszystkie wersje Androida (11 — procesorem, raz na obraz). */
        val available: Boolean get() = true
        fun enabled(c: android.content.Context): Boolean {
            val on = available && com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, KEY, true)
            if (on && dir == null && com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, DISK_KEY, true)) {
                dir = java.io.File(c.applicationContext.cacheDir, "fork_blur").apply { if (!exists() && !mkdirs()) Log.w(TAG, "rozmycia na dysku: brak katalogu") }
            }
            return on
        }
    }
}
