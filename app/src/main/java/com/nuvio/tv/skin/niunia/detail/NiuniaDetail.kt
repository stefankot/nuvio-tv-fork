package com.nuvio.tv.skin.niunia.detail

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.domain.model.MetaCastMember
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.skin.niunia.NiuniaLabels
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaPoster
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import com.nuvio.tv.ui.screens.detail.MetaDetailsEvent
import com.nuvio.tv.ui.screens.detail.MetaDetailsUiState
import com.nuvio.tv.ui.screens.detail.MetaDetailsViewModel

private const val TAG = "ForkNiunia"

/**
 * Strona filmu / serialu Niunii (canvas: Detail, Movie, SeriesPage, MoviePage): tło na cały ekran z gradientem z lewej i od dołu,
 * logo (albo tytuł), linia „Serial · gatunki · N sezonów”, opis (3 linie), dla serialu następny odcinek, przyciski
 * (Odtwórz / Wznów z paskiem postępu, + Biblioteka), obsada i twórcy po prawej. Niżej: sezony i odcinki, „Więcej podobnych”, „Obsada”.
 * Dane i akcje = [MetaDetailsUiState] / [MetaDetailsViewModel] Nuvio i funkcje odtwarzania z MetaDetailsScreen (te same sprawdzenia źródeł).
 */
@Composable
fun NiuniaDetail(
    meta: Meta,
    uiState: MetaDetailsUiState,
    viewModel: MetaDetailsViewModel,
    heroVideo: Video?,
    playEpisode: (Video) -> Unit,
    playTitle: (String) -> Unit,
    onNavigateToDetail: (String, String, String?) -> Unit,
    onNavigateToCastDetail: (Int, String, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val series = meta.type == ContentType.SERIES || meta.videos.isNotEmpty()
    val playFocus = remember { FocusRequester() }
    var focusedRow by remember { mutableIntStateOf(0) }

    LaunchedEffect(meta.id) {
        Log.i(TAG, "szczegóły: ${meta.rawType} „${meta.name}” (sezony=${uiState.seasons.size}, odcinki=${uiState.episodesForSeason.size}, podobne=${uiState.moreLikeThis.size}, obsada=${meta.castMembers.size}, wznów=${uiState.nextToWatch?.isResume == true})")
        try { playFocus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "szczegóły: fokus startowy: ${e.javaClass.simpleName}: ${e.message}") }
    }

    val play: () -> Unit = {
        if (heroVideo != null) {
            Log.i(TAG, "szczegóły: odtwórz odcinek S${heroVideo.season}:O${heroVideo.episode}")
            playEpisode(heroVideo)
        } else {
            Log.i(TAG, "szczegóły: odtwórz ${meta.id}")
            playTitle(meta.id)
        }
    }

    LazyColumn(Modifier.fillMaxSize().background(NiuniaColors.Bg), contentPadding = PaddingValues(bottom = 48.dp)) {
        item(key = "hero") {
            Hero(meta, uiState, series, heroVideo, playFocus, play,
                onLibrary = { Log.i(TAG, "szczegóły: biblioteka (było ${uiState.isInLibrary})"); viewModel.onEvent(MetaDetailsEvent.OnToggleLibrary) },
                onFocus = { focusedRow = 0 })
        }
        if (series && uiState.seasons.isNotEmpty()) {
            item(key = "seasons") {
                SeasonTabs(uiState.seasons, uiState.selectedSeason) {
                    Log.i(TAG, "szczegóły: sezon $it"); viewModel.onEvent(MetaDetailsEvent.OnSeasonSelected(it))
                }
            }
            item(key = "episodes_${uiState.selectedSeason}") {
                LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                    items(uiState.episodesForSeason, key = { it.id }) { video ->
                        val progress = uiState.episodeProgressMap[(video.season ?: 0) to (video.episode ?: 0)]?.progressPercentage
                        val watched = (video.season ?: 0) to (video.episode ?: 0) in uiState.watchedEpisodes
                        EpisodeCard(video, progress, watched, onFocus = { focusedRow = 1 }, onOk = { Log.i(TAG, "szczegóły: odcinek S${video.season}:O${video.episode}"); playEpisode(video) })
                    }
                }
            }
        }
        if (uiState.moreLikeThis.isNotEmpty()) {
            item(key = "more") {
                RowTitle(stringResource(R.string.niunia_detail_more_like), on = focusedRow == 2)
                LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                    items(uiState.moreLikeThis, key = { "${it.rawType}_${it.id}" }) { item ->
                        NiuniaPoster(
                            art = item.poster, fallback = item.rawPosterUrl, title = item.name, logo = item.logo, meta = NiuniaLabels.meta(item),
                            label = NiuniaLabels.label(context, item, 0, rankingRow = false), baked = item.poster != null,
                            onFocus = { if (it) focusedRow = 2 },
                            onOk = { onNavigateToDetail(item.id, item.rawType, null) },
                        )
                    }
                }
            }
        }
        val cast = meta.castMembers.ifEmpty { meta.cast.map { MetaCastMember(name = it) } }
        if (cast.isNotEmpty()) {
            item(key = "cast") {
                RowTitle(stringResource(R.string.niunia_detail_cast), on = focusedRow == 3)
                LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                    items(cast.take(20), key = { "${it.name}_${it.character}" }) { person ->
                        PersonCard(person, onFocus = { focusedRow = 3 }, onOk = {
                            val id = person.tmdbId
                            if (id != null) onNavigateToCastDetail(id, person.name, false)
                            else Log.i(TAG, "szczegóły: ${person.name} bez id TMDB — brak strony osoby")
                        })
                    }
                }
            }
        }
        item(key = "info") { InfoPanel(meta) }
    }
}

@Composable
private fun Hero(
    meta: Meta,
    uiState: MetaDetailsUiState,
    series: Boolean,
    heroVideo: Video?,
    playFocus: FocusRequester,
    onPlay: () -> Unit,
    onLibrary: () -> Unit,
    onFocus: () -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(540.dp)) {
        AsyncImage(model = meta.backdropUrl ?: meta.poster, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        // canvas .scrim-l i .scrim-b
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to NiuniaColors.ScrimLeft, 0.55f to Color(0x6618181A), 1f to Color.Transparent)))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(200.dp).background(Brush.verticalGradient(listOf(Color.Transparent, NiuniaColors.Bg))))
        Column(Modifier.offset(x = 40.dp, y = 150.dp).width(430.dp)) {
            if (meta.logo != null) {
                AsyncImage(model = meta.logo, contentDescription = meta.name, contentScale = ContentScale.Fit, alignment = Alignment.CenterStart, modifier = Modifier.width(330.dp).height(96.dp))
            } else {
                Text(meta.name, style = NiuniaText.Logo, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(metaLine(meta, series, uiState.seasons.size), style = NiuniaText.Meta.copy(fontSize = 13.sp), color = NiuniaColors.Text70, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
            val episodeLine = heroVideo?.takeIf { series }?.let { v ->
                listOfNotNull("S${v.season}, O${v.episode}", v.title.takeIf { it.isNotBlank() }).joinToString(" · ")
            }
            if (episodeLine != null) Text(episodeLine, style = NiuniaText.Meta.copy(fontSize = 13.sp), color = NiuniaColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
            val description = (heroVideo?.overview?.takeIf { series && it.isNotBlank() } ?: meta.description)?.takeIf { it.isNotBlank() }
            if (description != null) Text(description, style = NiuniaText.Body.copy(fontSize = 13.sp, lineHeight = 18.sp), maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val next = uiState.nextToWatch
                val playText = when {
                    next?.isResume == true -> stringResource(R.string.niunia_detail_resume)
                    series && heroVideo != null -> stringResource(R.string.niunia_detail_play_episode, heroVideo.season ?: 1, heroVideo.episode ?: 1)
                    else -> stringResource(R.string.niunia_detail_play)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NiuniaButton(text = "▶  $playText", onOk = onPlay, primary = true, focusRequester = playFocus,
                        modifier = Modifier.niuniaRowFocus(onFocus))
                    val progress = next?.takeIf { it.isResume }?.watchProgress?.progressPercentage
                    if (progress != null && progress > 0f) {
                        // canvas .mini: cienki pasek postępu pod przyciskiem
                        Box(Modifier.padding(top = 6.dp).width(120.dp).height(3.dp).clip(RoundedCornerShape(2.dp)).background(NiuniaColors.Fill24)) {
                            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(NiuniaColors.Text))
                        }
                    }
                }
                NiuniaButton(text = if (uiState.isInLibrary) "✓" else "+", onOk = onLibrary, modifier = Modifier.niuniaRowFocus(onFocus))
            }
        }
        Credits(meta, series, Modifier.align(Alignment.TopEnd).offset(x = (-40).dp, y = 410.dp).width(250.dp))
    }
}

private fun Modifier.niuniaRowFocus(onFocus: () -> Unit): Modifier =
    this.onFocusChanged { if (it.hasFocus) onFocus() }

/** canvas .credits: „Występują …” i „Twórcy / Reżyseria …”, 12/16, szare etykiety. */
@Composable
private fun Credits(meta: Meta, series: Boolean, modifier: Modifier) {
    val stars = meta.castMembers.map { it.name }.ifEmpty { meta.cast }.take(3)
    val makers = (if (series) meta.writer.ifEmpty { meta.director } else meta.director).take(2)
    if (stars.isEmpty() && makers.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (stars.isNotEmpty()) CreditLine(stringResource(R.string.niunia_detail_starring), stars.joinToString(", "))
        if (makers.isNotEmpty()) CreditLine(stringResource(if (series) R.string.niunia_detail_created_by else R.string.niunia_detail_directed_by), makers.joinToString(", "))
    }
}

@Composable
private fun CreditLine(label: String, value: String) {
    Text("$label  $value", style = NiuniaText.Caption.copy(fontSize = 12.sp, lineHeight = 16.sp), color = NiuniaColors.Text70, maxLines = 2, overflow = TextOverflow.Ellipsis)
}

private fun metaLine(meta: Meta, series: Boolean, seasons: Int): String = listOfNotNull(
    if (series) "Serial" else "Film",
    meta.genres.take(2).joinToString(" · ").ifBlank { null },
    meta.releaseInfo?.takeIf { it.isNotBlank() },
    if (series && seasons > 0) (if (seasons == 1) "1 sezon" else if (seasons in 2..4) "$seasons sezony" else "$seasons sezonów") else meta.runtime?.takeIf { it.isNotBlank() },
    meta.ageRating?.takeIf { it.isNotBlank() },
    meta.imdbRating?.takeIf { it > 0f }?.let { "IMDb %.1f".format(java.util.Locale.US, it) },
).joinToString(" · ")

/** canvas .tabs: pigułki sezonów (wybrany = białe 24%, fokus = białe tło). */
@Composable
private fun SeasonTabs(seasons: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        items(seasons) { s ->
            var focused by remember { mutableStateOf(false) }
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .height(34.dp)
                    .niuniaFocusable(shape, scale = NiuniaDims.ScaleButton, lift = false, onFocus = { focused = it }, onOk = { onSelect(s) })
                    .background(when { focused -> NiuniaColors.Focus; s == selected -> NiuniaColors.Fill24; else -> Color.Transparent }, shape)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (s == 0) stringResource(R.string.niunia_detail_specials) else stringResource(R.string.niunia_detail_season, s),
                    style = NiuniaText.Button.copy(fontSize = 14.sp), color = when { focused -> NiuniaColors.OnFocus; s == selected -> NiuniaColors.Text; else -> NiuniaColors.Text70 })
            }
        }
    }
}

/** canvas .ep: karta 205×115 (kadr odcinka, czas w rogu, pasek postępu), pod nią „ODCINEK N”, tytuł i opis (2 linie). */
@Composable
private fun EpisodeCard(video: Video, progress: Float?, watched: Boolean, onFocus: () -> Unit, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(NiuniaDims.RadiusCard)
    Column(Modifier.width(205.dp)) {
        Box(
            Modifier
                .size(205.dp, 115.dp)
                .niuniaFocusable(shape, onFocus = { focused = it; if (it) onFocus() }, onOk = onOk)
                .clip(shape)
                .background(NiuniaColors.Surface),
        ) {
            AsyncImage(model = video.thumbnail, contentDescription = video.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            video.runtime?.let { m ->
                Text("${m}m", style = NiuniaText.Caption, color = NiuniaColors.Text, modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).background(NiuniaColors.Badge, RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
            if (watched) Text("✓", style = NiuniaText.Caption, color = NiuniaColors.Text, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).background(NiuniaColors.Badge, CircleShape).padding(horizontal = 6.dp, vertical = 2.dp))
            if (progress != null && progress > 0f && !watched) {
                Box(Modifier.align(Alignment.BottomStart).padding(8.dp).fillMaxWidth(0.6f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(NiuniaColors.Fill36)) {
                    Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(3.dp).background(NiuniaColors.Text))
                }
            }
        }
        Column(Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp)) {
            Text(stringResource(R.string.niunia_detail_episode, video.episode ?: 0).uppercase(), style = NiuniaText.Caption, color = NiuniaColors.Text50)
            Text(video.title, style = NiuniaText.Meta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = if (focused) NiuniaColors.Text else NiuniaColors.Text70, maxLines = 1, overflow = TextOverflow.Ellipsis)
            video.overview?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = NiuniaText.Body.copy(fontSize = 12.sp, lineHeight = 16.sp), color = NiuniaColors.Text50, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** canvas .card.r: okrągłe zdjęcie 130 (albo inicjały), pod nim imię i postać. */
@Composable
private fun PersonCard(person: MetaCastMember, onFocus: () -> Unit, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(130.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(130.dp).niuniaFocusable(CircleShape, onFocus = { focused = it; if (it) onFocus() }, onOk = onOk).clip(CircleShape).background(NiuniaColors.Surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials(person.name), style = NiuniaText.Logo.copy(fontSize = 32.sp, lineHeight = 38.sp), color = NiuniaColors.Text50)
            if (person.photo != null) AsyncImage(model = person.photo, contentDescription = person.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Text(person.name, style = NiuniaText.Meta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = if (focused) NiuniaColors.Text else NiuniaColors.Text70, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        person.character?.takeIf { it.isNotBlank() }?.let { Text(it, style = NiuniaText.Body.copy(fontSize = 12.sp), color = NiuniaColors.Text50, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

private fun initials(name: String): String = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/** canvas .panel „Informacje”: premiera, kraj, język, studio (tylko pola, które są). */
@Composable
private fun InfoPanel(meta: Meta) {
    val rows = listOfNotNull(
        meta.released?.take(10)?.let { R.string.niunia_detail_released to it },
        meta.country?.takeIf { it.isNotBlank() }?.let { R.string.niunia_detail_country to it },
        meta.language?.takeIf { it.isNotBlank() }?.let { R.string.niunia_detail_language to it },
        (meta.networks + meta.productionCompanies).map { it.name }.distinct().take(3).joinToString(", ").ifBlank { null }?.let { R.string.niunia_detail_studio to it },
    )
    if (rows.isEmpty()) return
    RowTitle(stringResource(R.string.niunia_detail_info), on = false)
    Column(
        Modifier.padding(start = NiuniaDims.Gutter, top = 12.dp).width(400.dp).clip(RoundedCornerShape(16.dp)).background(NiuniaColors.Surface).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { (label, value) ->
            Column {
                Text(stringResource(label), style = NiuniaText.Meta.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = NiuniaColors.Text)
                Text(value, style = NiuniaText.Body.copy(fontSize = 13.sp), color = NiuniaColors.Text50, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun RowTitle(text: String, on: Boolean) {
    Text(text, style = NiuniaText.Row, color = if (on) NiuniaColors.Text else NiuniaColors.Text50, maxLines = 1, modifier = Modifier.padding(start = NiuniaDims.Gutter, top = 18.dp))
}
