package com.nuvio.tv.fork.images

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.util.Log
import coil3.size.Size
import coil3.transform.Transformation

/**
 * Przegląd 27.11 (CAŁA APLIKACJA, płynność JMGO): przejście ostry obraz → rozmycie „wpieczone” w kanał alfa małej, już
 * rozmytej bitmapy (liczone raz procesorem, wynik w pamięci Coil) zamiast warstwy pozaekranowej na cały ekran z maską
 * rysowaną w każdej klatce (pomiar prz09: zmiana tytułu na hero p95 40 ms z warstwą, 30 ms bez niej).
 * [fromLeft] = true: pełna od lewej do [a], zanika do zera w [b] (ułamki szerokości).
 * [fromLeft] = false: zero od góry do [a], narasta do pełnej w [b] i zostaje do dołu (ułamki wysokości).
 * Używać po [NativeBlur] w tym samym żądaniu (bitmapa ma wtedy kilkadziesiąt–kilkaset pikseli).
 */
class AlphaMask(private val fromLeft: Boolean, private val a: Float, private val b: Float) : Transformation() {
    override val cacheKey: String = "fork_alpha_mask_${if (fromLeft) "l" else "b"}_${(a * 100).toInt()}_${(b * 100).toInt()}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val out = try {
            input.copy(Bitmap.Config.ARGB_8888, true)
        } catch (e: Exception) {
            Log.w(TAG, "maska rozmycia: kopia ${input.width}×${input.height}: ${e.javaClass.simpleName}: ${e.message} — bez maski")
            null
        } ?: return input
        val w = out.width.toFloat()
        val h = out.height.toFloat()
        val shader = if (fromLeft) LinearGradient(0f, 0f, w, 0f, intArrayOf(Color.BLACK, Color.BLACK, Color.TRANSPARENT, Color.TRANSPARENT),
            floatArrayOf(0f, a, b, 1f), Shader.TileMode.CLAMP)
        else LinearGradient(0f, 0f, 0f, h, intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.BLACK, Color.BLACK),
            floatArrayOf(0f, a, b, 1f), Shader.TileMode.CLAMP)
        Canvas(out).drawRect(0f, 0f, w, h, Paint().apply { this.shader = shader; xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN) })
        if (!logged) { logged = true; Log.i(TAG, "maska rozmycia w obrazie (${out.width}×${out.height}, ${if (fromLeft) "z lewej" else "od dołu"} ${(a * 100).toInt()}–${(b * 100).toInt()} %) zamiast warstwy na cały ekran") }
        return out
    }

    private companion object {
        const val TAG = "ForkImages"
        @Volatile var logged = false
    }
}
