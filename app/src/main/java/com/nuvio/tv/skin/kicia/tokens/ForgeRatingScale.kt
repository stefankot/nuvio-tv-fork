package com.nuvio.tv.skin.kicia.tokens

import androidx.compose.ui.graphics.Color
import kotlin.math.cbrt
import kotlin.math.pow

/**
 * Rating scale of mockup v7.4 (W-67, `K.kolorOceny` in komponenty.js): red ≤ 4.0 → orange 5.5 →
 * yellow 7.0 → green ≥ 8.5, smooth OKLab interpolation between the points. Text on it is always
 * [ForgeColors.OnFocus] (≥ 4.5:1 on every point).
 */
object ForgeRatingScale {
    private val thresholds = doubleArrayOf(4.0, 5.5, 7.0, 8.5)
    private val labs by lazy {
        listOf(ForgeColors.Rating1, ForgeColors.Rating2, ForgeColors.Rating3, ForgeColors.Rating4).map(::toOklab)
    }

    fun color(value: Double): Color {
        var i = 0
        while (i < 2 && value > thresholds[i + 1]) i++
        val t = ((value - thresholds[i]) / (thresholds[i + 1] - thresholds[i])).coerceIn(0.0, 1.0)
        val a = labs[i]
        val b = labs[i + 1]
        return fromOklab(
            a[0] + (b[0] - a[0]) * t,
            a[1] + (b[1] - a[1]) * t,
            a[2] + (b[2] - a[2]) * t,
        )
    }

    val textColor: Color get() = ForgeColors.OnFocus

    private fun toOklab(c: Color): DoubleArray {
        fun lin(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.04045) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
        }
        val r = lin(c.red)
        val g = lin(c.green)
        val b = lin(c.blue)
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        return doubleArrayOf(
            0.2104542553 * l + 0.793617785 * m - 0.0040720468 * s,
            1.9779984951 * l - 2.428592205 * m + 0.4505937099 * s,
            0.0259040371 * l + 0.7827717662 * m - 0.808675766 * s,
        )
    }

    private fun fromOklab(lab: Double, aa: Double, bb: Double): Color {
        val l = (lab + 0.3963377774 * aa + 0.2158037573 * bb).pow(3)
        val m = (lab - 0.1055613458 * aa - 0.0638541728 * bb).pow(3)
        val s = (lab - 0.0894841775 * aa - 1.291485548 * bb).pow(3)
        fun enc(v: Double): Float {
            val c = v.coerceIn(0.0, 1.0)
            return (if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1 / 2.4) - 0.055).toFloat()
        }
        return Color(
            enc(4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s),
            enc(-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s),
            enc(-0.0041960863 * l - 0.7034186147 * m + 1.707614701 * s),
        )
    }
}
