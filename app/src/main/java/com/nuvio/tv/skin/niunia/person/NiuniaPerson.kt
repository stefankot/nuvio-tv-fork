package com.nuvio.tv.skin.niunia.person

import com.nuvio.tv.skin.niunia.components.niuniaOk

import androidx.compose.ui.focus.focusRequester

import androidx.compose.foundation.lazy.itemsIndexed

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PersonDetail
import com.nuvio.tv.skin.niunia.NiuniaLabels
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText

private const val TAG = "ForkNiunia"

/**
 * Strona osoby Niunii (canvas: Person): okrągłe zdjęcie 124 (albo inicjały), imię 28/700, linia „dziedzina · urodzony w …”,
 * biografia w 3 liniach (OK rozwija / zwija), potem rzędy „Seriale” i „Filmy” (od najnowszych) jako plakaty Niunii.
 * Dane: [PersonDetail] z ekranu osoby Nuvio; Wstecz obsługuje ekran Nuvio (najpierw zwija biografię).
 */
@Composable
fun NiuniaPerson(
    person: PersonDetail,
    bioExpanded: Boolean,
    onBioExpanded: (Boolean) -> Unit,
    onNavigateToDetail: (String, String, String?) -> Unit,
) {
    val context = LocalContext.current
    val series = remember(person.tvCredits) { person.tvCredits.distinctBy { it.id }.sortedByDescending { year(it) } }
    val movies = remember(person.movieCredits) { person.movieCredits.distinctBy { it.id }.sortedByDescending { year(it) } }
    var focusedRow by remember { mutableIntStateOf(-1) }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(person.tmdbId) {
        // Makieta Person: fokus startowy na pierwszym plakacie (Seriale, inaczej Filmy); biografia (▲) rozwija się OK.
        Log.i(TAG, "osoba: ${person.tmdbId} (seriale=${series.size}, filmy=${movies.size}, biografia=${!person.biography.isNullOrBlank()}), fokus startowy → 1. plakat")
        kotlinx.coroutines.delay(60)
        try { firstFocus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "osoba: fokus startowy: ${e.javaClass.simpleName}: ${e.message}") }
    }
    com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(firstFocus)

    // Układ stack-tight: od góry 40 dp, odstęp 24 między blokami.
    // Przewijanie tylko, gdy element wychodzi poza ekran (test JMGO build147: nagłówek z imieniem uciekał za górną krawędź).
    com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
    LazyColumn(Modifier.fillMaxSize().background(com.nuvio.tv.skin.niunia.tokens.NiuniaColor.Bg), contentPadding = PaddingValues(top = 40.dp, bottom = 48.dp)) {
        item(key = "head") {
            // NContentBlock profile-head: awatar 120, obok imię 32/38, linia 14/20 w 50%, biografia 3 linie w 70%.
            Row(Modifier.padding(horizontal = com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space40), horizontalArrangement = Arrangement.spacedBy(com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space24),
                verticalAlignment = Alignment.CenterVertically) {
                com.nuvio.tv.skin.niunia.components.NiuniaAvatar(com.nuvio.tv.skin.niunia.components.niuniaInitials(person.name), com.nuvio.tv.skin.niunia.tokens.NiuniaSize.Avatar120, photo = person.profilePhoto)
                com.nuvio.tv.skin.niunia.components.NiuniaContentBlock(com.nuvio.tv.skin.niunia.components.NiuniaBlockKind.Wide) {
                    com.nuvio.tv.skin.niunia.components.NiuniaText(person.name, variant = com.nuvio.tv.skin.niunia.components.NiuniaTextVariant.TitleScreen, lines = 1)
                    val line = listOfNotNull(
                        person.knownFor?.takeIf { it.isNotBlank() }?.let { niuniaDepartment(it) }, // test JMGO build159: „Acting”
                        person.placeOfBirth?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.niunia_person_born_in, it) },
                    ).joinToString(" · ")
                    if (line.isNotEmpty()) com.nuvio.tv.skin.niunia.components.NiuniaText(line, variant = com.nuvio.tv.skin.niunia.components.NiuniaTextVariant.BodySmall, tone = com.nuvio.tv.skin.niunia.components.NiuniaTone.Tertiary, lines = 1)
                    val bio = person.biography?.takeIf { it.isNotBlank() }
                    if (bio != null) {
                        var focused by remember { mutableStateOf(false) }
                        Column(
                            Modifier
                                .niuniaOk("biografia", onFocus = { focused = it; if (it) focusedRow = -1 }, onClick = {
                                    Log.i(TAG, "osoba: biografia ${if (bioExpanded) "zwinięta" else "rozwinięta"}"); onBioExpanded(!bioExpanded)
                                })
                                .clip(RoundedCornerShape(com.nuvio.tv.skin.niunia.tokens.NiuniaRadius.Radius16))
                                .background(if (focused) com.nuvio.tv.skin.niunia.tokens.NiuniaColor.Fill12 else androidx.compose.ui.graphics.Color.Transparent)
                                .padding(horizontal = if (focused) com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space8 else 0.dp, vertical = com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space4),
                        ) {
                            com.nuvio.tv.skin.niunia.components.NiuniaText(bio, variant = com.nuvio.tv.skin.niunia.components.NiuniaTextVariant.BodySmall, tone = com.nuvio.tv.skin.niunia.components.NiuniaTone.Secondary, lines = if (bioExpanded) 30 else 3)
                            if (focused) com.nuvio.tv.skin.niunia.components.NiuniaText(stringResource(if (bioExpanded) R.string.niunia_person_less else R.string.niunia_person_more),
                                variant = com.nuvio.tv.skin.niunia.components.NiuniaTextVariant.Caption, upper = true)
                        }
                    }
                }
            }
        }
        listOf(R.string.niunia_person_series to series, R.string.niunia_person_movies to movies).forEachIndexed { rowIndex, (title, list) ->
            if (list.isEmpty()) return@forEachIndexed
            item(key = "row_$rowIndex") {
                com.nuvio.tv.skin.niunia.components.NiuniaCollectionHeader(stringResource(title), active = focusedRow == rowIndex, modifier = Modifier.padding(start = NiuniaDims.Gutter, top = 24.dp))
                val firstRow = rowIndex == (if (series.isNotEmpty()) 0 else 1)
                LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                    itemsIndexed(list, key = { _, it -> "${it.rawType}_${it.id}" }) { i, item ->
                        NiuniaPoster(
                            modifier = if (firstRow && i == 0) Modifier.focusRequester(firstFocus) else Modifier,
                            art = com.nuvio.tv.skin.niunia.components.niuniaRealPoster(item).first, fallback = com.nuvio.tv.skin.niunia.components.niuniaRealPoster(item).second, title = item.name, logo = item.logo, meta = NiuniaLabels.meta(item),
                            label = NiuniaLabels.label(context, item, 0, rankingRow = false), baked = item.poster != null,
                            onFocus = { if (it) focusedRow = rowIndex },
                            onOk = { Log.i(TAG, "osoba: otwieram ${item.rawType} ${item.id}"); onNavigateToDetail(item.id, item.rawType, null) },
                        )
                    }
                }
            }
        }
    }
    }
}

private fun year(item: MetaPreview): Int = (item.releaseInfo ?: item.released)?.take(4)?.toIntOrNull() ?: 0

private fun initials(name: String): String = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/** Dział z TMDB („Acting”, „Directing”…) po polsku; nieznany zostaje. */
@Composable
private fun niuniaDepartment(raw: String): String = when (raw.trim().lowercase()) {
    "acting" -> stringResource(com.nuvio.tv.R.string.niunia_dept_acting)
    "directing" -> stringResource(com.nuvio.tv.R.string.niunia_role_director)
    "writing" -> stringResource(com.nuvio.tv.R.string.niunia_role_writer)
    "production" -> stringResource(com.nuvio.tv.R.string.niunia_role_producer)
    else -> raw
}
