package com.nuvio.tv.skin.kicia.detail

import com.nuvio.tv.skin.kicia.nav.forgeTag
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/**
 * The one meta line of the detail screen (mockup S03): "Sci-Fi, Przygodowy · 1.03.2024 · 2 h 46 min · IMDb 8,5 · 13+"
 * (series: "… · 2 sezony · …"), IMDb in gold. Drawn in place of Nuvio's row of badges.
 */
@Composable
fun ForgeDetailMetaLine(meta: Meta, tmdbRating: Float?, modifier: Modifier = Modifier) {
    val isSeries = meta.apiType.equals("series", ignoreCase = true) || meta.apiType.equals("tv", ignoreCase = true)
    val seasons = if (isSeries) meta.videos.mapNotNull { it.season }.filter { it > 0 }.distinct().size else 0
    val parts = forgeDetailParts(
        genres = emptyList(), // v6 S03: genres and age moved to the line under the title (ForgeDetailTypeLine)
        date = forgeDetailDate(meta.released) ?: meta.releaseInfo,
        length = if (isSeries) forgeSeasons(seasons) else forgeRuntime(meta.runtime),
        imdb = meta.imdbRating ?: tmdbRating,
        age = null,
    )
    Text(forgeGold(parts), style = ForgeText.body.copy(fontSize = ForgeText.body.fontSize), maxLines = 1, modifier = modifier.padding(top = 2.dp))
}

internal fun forgeDetailParts(genres: List<String>, date: String?, length: String?, imdb: Float?, age: String?): List<String> = buildList {
    if (genres.isNotEmpty()) add(genres.take(2).joinToString(", "))
    date?.takeIf { it.isNotBlank() }?.let(::add)
    length?.let(::add)
    imdb?.takeIf { it > 0f }?.let { add("IMDb " + "%.1f".format(java.util.Locale("pl"), it)) }
    age?.takeIf { it.isNotBlank() }?.let(::add)
}

/** "2024-03-01T00:00:00.000Z" -> "1.03.2024"; anything else -> null. */
internal fun forgeDetailDate(released: String?): String? {
    val m = Regex("""^(\d{4})-(\d{2})-(\d{2})""").find(released ?: return null) ?: return null
    val (y, mo, d) = m.destructured
    return "${d.toInt()}.$mo.$y"
}

/** "166 min" / "166" / "2h 46min" -> "2 g 46 min". */
internal fun forgeRuntime(runtime: String?): String? {
    val r = runtime?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val h = Regex("""(\d+)\s*h""").find(r)?.groupValues?.get(1)?.toInt()
    val m = Regex("""(\d+)\s*m""").find(r)?.groupValues?.get(1)?.toInt()
    val total = when {
        h != null || m != null -> (h ?: 0) * 60 + (m ?: 0)
        else -> r.filter { it.isDigit() }.toIntOrNull() ?: return r
    }
    if (total <= 0) return null
    return if (total >= 60) "${total / 60} g" + (if (total % 60 > 0) " ${total % 60} min" else "") else "$total min" // v7.4: "1 g 45 min"
}

/** "1 sezon", "2 sezony", "5 sezonów". */
internal fun forgeSeasons(n: Int): String? = when {
    n <= 0 -> null
    n == 1 -> "1 sezon"
    n % 10 in 2..4 && n % 100 !in 12..14 -> "$n sezony"
    else -> "$n sezonów"
}

/** The line with "IMDb x,y" in gold. */
internal fun forgeGold(parts: List<String>): AnnotatedString = buildAnnotatedString {
    parts.forEachIndexed { i, p ->
        if (i > 0) append(" · ")
        if (p.startsWith("IMDb ")) {
            pushStyle(SpanStyle(color = ForgeColors.Gold)); append(p); pop()
        } else append(p)
    }
}

/** "Adam Scott" -> "AS"; one word -> its first letter. */
fun forgeInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(1).uppercase()
        else -> (words.first().take(1) + words.last().take(1)).uppercase()
    }
}

private val CastGradients = listOf(
    0xFF2D6CDF to 0xFF5B3FA8, 0xFF5B3FA8 to 0xFF8E3FA0, 0xFF9A3FA0 to 0xFFB2406C,
    0xFFB2405A to 0xFF8F5A2C, 0xFF9A7A2C to 0xFF6E8F2C, 0xFF4E9A2C to 0xFF2C8F5A,
)

/** The gradient behind a cast member's initials (mockup S03), the same for the same name. */
fun forgeCastBrush(name: String): androidx.compose.ui.graphics.Brush {
    val seed = name.foldIndexed(7) { i, acc, c -> acc * 131 + c.code * (i + 3) }
    val (a, b) = CastGradients[Math.floorMod(seed, CastGradients.size)]
    return androidx.compose.ui.graphics.Brush.linearGradient(listOf(androidx.compose.ui.graphics.Color(a), androidx.compose.ui.graphics.Color(b)))
}

/** v7.4 (04) line under the title: "Film · akcja · przygodowy · dramat · 16" (type, up to 3 genres, age) in 14/20, 70%. */
@Composable
fun ForgeDetailTypeLine(meta: Meta, modifier: Modifier = Modifier) {
    val isSeries = meta.apiType.equals("series", ignoreCase = true) || meta.apiType.equals("tv", ignoreCase = true)
    val text = buildList {
        add(if (isSeries) "Serial" else "Film")
        meta.genres.take(3).forEach { add(it.lowercase()) }
        meta.ageRating?.takeIf { it.isNotBlank() }?.let { add(it) }
    }.joinToString(" · ")
    Text(text, style = ForgeText.label5, maxLines = 1, modifier = Modifier.padding(bottom = 8.dp).then(modifier).then(Modifier.forgeTag("t-14.0"))) // v7.4: nazwa z makiety
}
