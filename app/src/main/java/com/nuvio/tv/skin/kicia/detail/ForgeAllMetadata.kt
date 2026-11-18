package com.nuvio.tv.skin.kicia.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/** Every field the title has, as (label, value) in reading order; empty ones are left out. */
internal fun forgeAllMetadata(meta: Meta, tmdbRating: Float?): List<Pair<String, String>> = buildList {
    fun add(label: String, value: String?) { value?.trim()?.takeIf { it.isNotEmpty() }?.let { add(label to it) } }
    add("Tytuł", meta.name)
    add("Typ", meta.apiType)
    add("Data premiery", forgeDetailDate(meta.released) ?: meta.released)
    add("Rok / wydanie", meta.releaseInfo)
    add("Status", meta.status)
    add("Czas trwania", forgeRuntime(meta.runtime) ?: meta.runtime)
    val seasons = meta.videos.mapNotNull { it.season }.filter { it > 0 }.distinct().size
    if (seasons > 0) add("Sezony", seasons.toString())
    val episodes = meta.videos.count { (it.season ?: 0) > 0 && it.episode != null }
    if (episodes > 0) add("Odcinki", episodes.toString())
    add("Gatunki", meta.genres.joinToString(", "))
    add("Ocena IMDb", meta.imdbRating?.takeIf { it > 0f }?.let { "%.1f".format(java.util.Locale("pl"), it) })
    add("Ocena TMDB", tmdbRating?.takeIf { it > 0f }?.let { "%.1f".format(java.util.Locale("pl"), it) })
    add("Klasyfikacja wiekowa", meta.ageRating)
    add("Kraj", meta.country)
    add("Język", meta.language)
    add("Reżyseria", meta.director.joinToString(", "))
    add("Scenariusz", meta.writer.joinToString(", "))
    add("Obsada", (meta.castMembers.map { m -> m.name + (m.character?.takeIf { it.isNotBlank() }?.let { c -> " ($c)" } ?: "") }.ifEmpty { meta.cast }).joinToString(", "))
    add("Nagrody", meta.awards)
    add("Studia", meta.productionCompanies.joinToString(", ") { it.name })
    add("Sieci", meta.networks.joinToString(", ") { it.name })
    add("Linki", meta.links.joinToString(", ") { it.name })
    add("Zwiastuny", meta.trailers.mapNotNull { it.name?.takeIf { n -> n.isNotBlank() } }.joinToString(", ").ifEmpty { meta.trailerYtIds.size.takeIf { it > 0 }?.toString() })
    add("Daty premier", meta.releaseDates.joinToString("; ") { c ->
        c.countryCode + ": " + c.releaseDates.joinToString(", ") { d -> listOfNotNull(d.releaseDate?.takeIf { it.isNotBlank() }?.take(10), d.certification?.takeIf { it.isNotBlank() }).joinToString(" ") }
    }.takeIf { meta.releaseDates.isNotEmpty() })
    add("Opis", meta.description)
    add("Identyfikator", meta.id)
    add("IMDb ID", meta.imdbId)
    add("Slug", meta.slug)
}

/**
 * "Informacje" in the manner of the Apple TV app's Information shelf: a grey small label over a white value, the short facts in four columns,
 * the long ones (cast, awards, release dates, description...) in two wider columns underneath. A focusable block, so the page scrolls to it.
 */
@Composable
fun ForgeAllMetadata(meta: Meta, tmdbRating: Float?, modifier: Modifier = Modifier) {
    val rows = forgeAllMetadata(meta, tmdbRating)
    if (rows.isEmpty()) return
    val (short, long) = rows.partition { it.second.length <= 48 }
    Column(modifier.fillMaxWidth().padding(start = 40.dp, end = 40.dp, top = 24.dp, bottom = 40.dp)) {
        Text("Informacje", style = ForgeText.head)
        Spacer(Modifier.height(14.dp))
        ForgeFocusBox(radius = 12.dp, modifier = Modifier.fillMaxWidth(), scaleOnFocus = false, onOk = {}) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                short.chunked(4).forEach { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        line.forEach { (label, value) -> InfoCell(label, value, Modifier.weight(1f)) }
                        repeat(4 - line.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                long.chunked(2).forEach { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        line.forEach { (label, value) -> InfoCell(label, value, Modifier.weight(1f)) }
                        if (line.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = ForgeText.caption.copy(color = ForgeColors.Text3))
        Text(value, style = ForgeText.label, maxLines = 8)
    }
}
