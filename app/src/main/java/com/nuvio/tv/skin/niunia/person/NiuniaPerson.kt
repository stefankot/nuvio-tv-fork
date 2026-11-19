package com.nuvio.tv.skin.niunia.person

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
        Log.i(TAG, "osoba: ${person.tmdbId} (seriale=${series.size}, filmy=${movies.size}, biografia=${!person.biography.isNullOrBlank()})")
        try { firstFocus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "osoba: fokus startowy: ${e.javaClass.simpleName}: ${e.message}") }
    }

    LazyColumn(Modifier.fillMaxSize().background(NiuniaColors.Bg), contentPadding = PaddingValues(top = 58.dp, bottom = 48.dp)) {
        item(key = "head") {
            Row(Modifier.padding(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                Box(Modifier.size(124.dp).clip(CircleShape).background(NiuniaColors.Surface2), contentAlignment = Alignment.Center) {
                    Text(initials(person.name), style = NiuniaText.Logo.copy(fontSize = 32.sp, lineHeight = 38.sp), color = NiuniaColors.Text50)
                    if (person.profilePhoto != null) AsyncImage(model = person.profilePhoto, contentDescription = person.name, contentScale = ContentScale.Crop, modifier = Modifier.size(124.dp))
                }
                Column(Modifier.width(520.dp).padding(top = 22.dp)) {
                    Text(person.name, style = NiuniaText.Logo.copy(fontSize = 28.sp, lineHeight = 34.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val line = listOfNotNull(
                        person.knownFor?.takeIf { it.isNotBlank() },
                        person.placeOfBirth?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.niunia_person_born_in, it) },
                    ).joinToString(" · ")
                    if (line.isNotEmpty()) Text(line, style = NiuniaText.Body.copy(fontSize = 13.sp), color = NiuniaColors.Text50, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    val bio = person.biography?.takeIf { it.isNotBlank() }
                    if (bio != null) {
                        var focused by remember { mutableStateOf(false) }
                        val shape = RoundedCornerShape(12.dp)
                        Column(
                            Modifier
                                .padding(top = 8.dp)
                                .niuniaFocusable(shape, scale = 1f, lift = false, focusRequester = firstFocus, onFocus = { focused = it; if (it) focusedRow = -1 }, onOk = {
                                    Log.i(TAG, "osoba: biografia ${if (bioExpanded) "zwinięta" else "rozwinięta"}"); onBioExpanded(!bioExpanded)
                                })
                                .background(if (focused) NiuniaColors.Fill12 else androidx.compose.ui.graphics.Color.Transparent, shape)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(bio, style = NiuniaText.Body.copy(fontSize = 13.sp, lineHeight = 18.sp), maxLines = if (bioExpanded) 30 else 3, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(if (bioExpanded) R.string.niunia_person_less else R.string.niunia_person_more).uppercase(),
                                style = NiuniaText.Caption.copy(fontWeight = FontWeight.Bold), color = NiuniaColors.Text, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
        listOf(R.string.niunia_person_series to series, R.string.niunia_person_movies to movies).forEachIndexed { rowIndex, (title, list) ->
            if (list.isEmpty()) return@forEachIndexed
            item(key = "row_$rowIndex") {
                Text(stringResource(title), style = NiuniaText.Row, color = if (focusedRow == rowIndex) NiuniaColors.Text else NiuniaColors.Text50, modifier = Modifier.padding(start = NiuniaDims.Gutter, top = 18.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                    items(list, key = { "${it.rawType}_${it.id}" }) { item ->
                        NiuniaPoster(
                            art = item.poster, title = item.name, logo = item.logo, meta = NiuniaLabels.meta(item),
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

private fun year(item: MetaPreview): Int = (item.releaseInfo ?: item.released)?.take(4)?.toIntOrNull() ?: 0

private fun initials(name: String): String = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
