package com.nuvio.tv.skin.niunia.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nuvio.tv.domain.model.MDBListRatings
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType
import java.util.Locale
import kotlin.math.cbrt
import kotlin.math.pow

private const val TAG = "NiuniaRatings"

/** Jedna ocena serwisu: nazwa („IMDb”), wartość na skali 10 i tekst jak u serwisu („8.2”, „91%”, „84”). */
data class NiuniaScore(val source: String, val value10: Double, val label: String)

/**
 * Oceny ze wszystkich serwisów (Z-8/Z-10, makieta `NRatings`): IMDb z metadanych + MDBList (TMDB, Trakt, RT, Metacritic, Letterboxd, MAL).
 * Skale MDBList: imdb/tmdb/mal 0–10, trakt/tomatoes/audience/metacritic 0–100, letterboxd 0–5 (×2). Brak danych → pusta lista.
 */
fun niuniaScores(imdb: Float?, mdb: MDBListRatings?): List<NiuniaScore> {
    val out = mutableListOf<NiuniaScore>()
    val imdbV = mdb?.imdb ?: imdb?.toDouble()
    fun add(src: String, v: Double?, scale: Double, label: (Double) -> String) {
        if (v == null || v <= 0.0) return
        out += NiuniaScore(src, (v * scale).coerceIn(0.0, 10.0), label(v))
    }
    val one = { v: Double -> String.format(Locale.US, "%.1f", v) }
    val int = { v: Double -> v.toInt().toString() }
    add("IMDb", imdbV, 1.0, one)
    add("TMDB", mdb?.tmdb, if ((mdb?.tmdb ?: 0.0) > 10.0) 0.1 else 1.0) { v -> one(if (v > 10.0) v / 10.0 else v) } // test JMGO ke2: było „72.0”
    add("Trakt", mdb?.trakt, if ((mdb?.trakt ?: 0.0) > 10.0) 0.1 else 1.0) { v -> if (v > 10.0) "${v.toInt()}%" else one(v) }
    add("RT", mdb?.tomatoes, 0.1) { v -> "${v.toInt()}%" }
    add("Metacritic", mdb?.metacritic, 0.1, int)
    add("Letterboxd", mdb?.letterboxd, if ((mdb?.letterboxd ?: 0.0) <= 5.0) 2.0 else 1.0, one)
    add("MAL", mdb?.mal, 1.0, one)
    return out
}

/** Średnia ze wszystkich serwisów, zaokrąglona do 0,1 (Z-10); null, gdy brak ocen. */
fun niuniaAverage(scores: List<NiuniaScore>): Double? =
    if (scores.isEmpty()) null else Math.round(scores.sumOf { it.value10 } / scores.size * 10.0) / 10.0

/** „8,4” po polsku. */
fun niuniaScoreText(v: Double): String = String.format(Locale("pl", "PL"), "%.1f", v)

/** Kolor skali ocen (Z-8): ≤4 rating-1 · 5,5 rating-2 · 7 rating-3 · ≥8,5 rating-4, pomiędzy interpolacja w OKLab. */
fun niuniaRatingColor(v: Double): Color {
    val stops = listOf(4.0 to NiuniaColor.Rating1, 5.5 to NiuniaColor.Rating2, 7.0 to NiuniaColor.Rating3, 8.5 to NiuniaColor.Rating4)
    if (v <= stops.first().first) return stops.first().second
    if (v >= stops.last().first) return stops.last().second
    val i = stops.indexOfLast { it.first <= v }
    val (a, ca) = stops[i]; val (b, cb) = stops[i + 1]
    return oklabMix(ca, cb, ((v - a) / (b - a)).toFloat())
}

private fun lin(c: Float) = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
private fun gam(c: Float) = if (c <= 0.0031308f) 12.92f * c else 1.055f * c.pow(1f / 2.4f) - 0.055f
private fun toOklab(c: Color): FloatArray {
    val r = lin(c.red); val g = lin(c.green); val b = lin(c.blue)
    val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
    val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
    val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
    return floatArrayOf(0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s, 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s, 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s)
}
private fun oklabMix(a: Color, b: Color, t: Float): Color {
    val x = toOklab(a); val y = toOklab(b)
    val L = x[0] + (y[0] - x[0]) * t; val A = x[1] + (y[1] - x[1]) * t; val B = x[2] + (y[2] - x[2]) * t
    val l = (L + 0.3963377774f * A + 0.2158037573f * B).pow(3); val m = (L - 0.1055613458f * A - 0.0638541728f * B).pow(3); val s = (L - 0.0894841775f * A - 1.2914855480f * B).pow(3)
    fun c(v: Float) = gam(v).coerceIn(0f, 1f)
    return Color(c(4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s), c(-1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s), c(-0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s))
}

/**
 * `NRatings kind="average"` (Z-10): jedna kapsułka „★ 8,4” (gwiazdka 12, caption 12/16 700, tekst on-focus, tło w kolorze skali,
 * wysokość tag-h 20, promień 6), bez fokusu. Opis dostępności z listą serwisów. Brak ocen → nic.
 */
@Composable
fun NiuniaRatingAverage(scores: List<NiuniaScore>, modifier: Modifier = Modifier) {
    val avg = niuniaAverage(scores) ?: return
    val text = niuniaScoreText(avg)
    val desc = "Średnia ocen $text z ${scores.size} serwis${if (scores.size == 1) "u" else "ów"} (" + scores.joinToString(", ") { "${it.source} ${it.label}" } + ")"
    LaunchedEffect(desc) { Log.i(TAG, "average: $text from ${scores.size} (${scores.joinToString(", ") { it.source + " " + it.label }})") }
    Row(
        modifier.height(NiuniaSize.TagH).clip(RoundedCornerShape(NiuniaRadius.Radius6)).background(niuniaRatingColor(avg))
            .padding(horizontal = NiuniaSpacing.Space4 + 2.dp).semantics { contentDescription = desc },
        horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4), verticalAlignment = Alignment.CenterVertically,
    ) {
        NiuniaIcon("star", tint = NiuniaColor.OnFocus, size = 12.dp)
        Text(text, style = NiuniaType.Caption, color = NiuniaColor.OnFocus, maxLines = 1)
    }
}

/**
 * `NRatings kind="row"` (Z-8): rząd ocen bez fokusu — nazwa serwisu (`caption-regular` 12/16 `text-70`) + wartość na kapsułce
 * (`tag-h` 20, promień 6, `caption` 12/16 700, tekst `on-focus`) w kolorze skali ocen; odstęp 12, zawijanie 8; przecinek dziesiętny.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun NiuniaRatingsRow(scores: List<NiuniaScore>, modifier: Modifier = Modifier) {
    if (scores.isEmpty()) return
    LaunchedEffect(scores) { Log.i(TAG, "row: " + scores.joinToString(", ") { it.source + " " + it.label }) }
    androidx.compose.foundation.layout.FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8)) {
        scores.forEach { s ->
            Row(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4), verticalAlignment = Alignment.CenterVertically) {
                Text(s.source, style = NiuniaType.CaptionRegular, color = NiuniaColor.Text70, maxLines = 1)
                Row(Modifier.height(NiuniaSize.TagH).clip(RoundedCornerShape(NiuniaRadius.Radius6)).background(niuniaRatingColor(s.value10)).padding(horizontal = NiuniaSpacing.Space4 + 2.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(s.label.replace('.', ','), style = NiuniaType.Caption, color = NiuniaColor.OnFocus, maxLines = 1)
                }
            }
        }
    }
}
