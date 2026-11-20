package com.nuvio.tv.skin.niunia.player

import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.MetaCastMember
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.domain.model.MetaPreview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import com.nuvio.tv.skin.niunia.components.niuniaFocusable
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaDims
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpace
import com.nuvio.tv.skin.niunia.tokens.NiuniaText
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState

private const val TAG = "ForkNiunia"

/** Odcinki bieżącego sezonu z danych odtwarzacza (puste, dopóki odtwarzacz ich nie wczytał). */
internal fun niuniaSeasonEpisodes(ui: PlayerUiState): List<Video> {
    val season = ui.episodesSelectedSeason ?: ui.currentSeason
    return ui.episodes.ifEmpty { ui.episodesAll.filter { it.season == season } }
}

/**
 * Rząd pod odtwarzaczem (canvas `PlayerSeries`, decyzja 20.11): kafle 16:9 200×113, 4 + kawałek piątego.
 * Serial: odcinki sezonu (bieżący „Teraz”, postęp na każdym). ▲ wraca do przycisków, ▼ otwiera szczegóły.
 */
@Composable
internal fun NiuniaPlayerRow(
    ui: PlayerUiState,
    episodes: List<Video>,
    focus: FocusRequester,
    onEvent: (PlayerEvent) -> Unit,
    onUp: () -> Boolean,
    onDown: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val current = episodes.indexOfFirst { it.season == ui.currentSeason && it.episode == ui.currentEpisode }.coerceAtLeast(0)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = current)
    LazyRow(
        state = state,
        modifier = modifier.onPreviewKeyEvent {
            if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (it.key) {
                Key.DirectionUp -> onUp()
                Key.DirectionDown -> onDown()
                else -> false
            }
        },
        contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter),
        horizontalArrangement = Arrangement.spacedBy(NiuniaSpace.TileGap),
    ) {
        itemsIndexed(episodes, key = { _, v -> v.id }) { i, v ->
            EpisodeTile(ui, v, focus = if (i == current) focus else null) {
                Log.i(TAG, "odtwarzacz: rząd → odcinek S${v.season}:O${v.episode}")
                onEvent(PlayerEvent.OnEpisodeSelected(v))
            }
        }
    }
}

/** Kafel odcinka = NMediaCard StillCard (makieta PlayerSeries): ▶, postęp, „E3 · Adar”, plakietka „Teraz”; rozmiar z decyzji 200×113. */
@Composable
private fun EpisodeTile(ui: PlayerUiState, v: Video, focus: FocusRequester?, onOk: () -> Unit) {
    val now = v.season == ui.currentSeason && v.episode == ui.currentEpisode
    val progress = ui.episodeWatchProgressMap[(v.season ?: 0) to (v.episode ?: 0)]?.progressPercentage
    com.nuvio.tv.skin.niunia.components.NiuniaMediaCard(
        preset = com.nuvio.tv.skin.niunia.components.NiuniaCardPreset.StillCard,
        image = v.thumbnail ?: ui.backdrop,
        onClick = onOk,
        width = NiuniaSpace.TileW, height = NiuniaSpace.TileH,
        caption = listOfNotNull(v.episode?.let { "E$it" }, v.title.takeIf { it.isNotBlank() }).joinToString(" · "),
        play = true,
        progress = progress?.takeIf { it > 0f }?.let { (it * 100).toInt() },
        badge = if (now) stringResource(R.string.niunia_pinfo_now) else null, badgeStrong = true,
        title = v.title,
        focusRequester = focus,
    )
}

/**
 * Szczegóły w odtwarzaczu (▼▼; canvas `PlayerFilmInfo`, `PlayerSeriesInfo`; WPIECIE.md „Po wdrożeniu” pkt 4, canvas v88):
 * mocno rozmyte tło z obrazu programu (API 31+; starsze — mocne przyciemnienie), logo na środku u góry (jedyny centrowany
 * element), nagłówek od lewej (40 dp, szer. 680): linia meta → opis max 3 linie. Sekcje: najpierw co obejrzeć (Odcinki
 * z pigułkami sezonów w linii tytułu), potem ludzie (Obsada i twórcy — inicjały), na końcu fakty (Informacje, siatka 3 kolumn).
 * Odstępy: tytuł → treść 34 dp, po treści 36 dp. Fokus po wejściu: pierwszy element pierwszej sekcji.
 * Tylko pola, które odtwarzacz ma w [PlayerUiState] (bez akcji „Do biblioteki”/„Zwiastun” i „Podobne” — brak tych danych w odtwarzaczu).
 */
@Composable
internal fun NiuniaPlayerDetails(
    ui: PlayerUiState,
    series: Boolean,
    onEvent: (PlayerEvent) -> Unit,
    extras: NiuniaExtrasState = NiuniaExtrasState(),
    onLibrary: () -> Unit = {},
    onTrailer: () -> Unit = {},
) {
    val episodes = niuniaSeasonEpisodes(ui)
    val cast = ui.castMembers.take(20)
    val similar = extras.similar
    val first = remember { FocusRequester() }
    val fallback = remember { FocusRequester() }
    val hasEpisodes = series && episodes.isNotEmpty()
    LaunchedEffect(Unit) {
        Log.i(TAG, "odtwarzacz: szczegóły (serial=$series, odcinki=${episodes.size}, obsada=${cast.size}, podobne=${similar.size}, opis=${!ui.description.isNullOrBlank()})")
        kotlinx.coroutines.delay(60)
        val ok = try { (if (hasEpisodes || cast.isNotEmpty() || similar.isNotEmpty()) first else fallback).requestFocus(); true } catch (e: IllegalStateException) {
            Log.w(TAG, "odtwarzacz: fokus szczegółów: ${e.javaClass.simpleName}: ${e.message}"); false
        }
        if (!ok) try { fallback.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: fokus zapasowy szczegółów: ${e.message}") }
    }
    Box(Modifier.fillMaxSize()) {
        NiuniaDeepBackground(ui.backdrop)
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = NiuniaSpace.AfterRow),
        ) {
            // 1. Logo na środku (jedyny wyjątek od „bez centrowania”); bez logo — tytuł tekstem od lewej.
            val title = if (series) ui.contentName ?: ui.title else ui.title
            if (ui.logo != null) {
                Box(Modifier.fillMaxWidth().padding(top = 28.dp), contentAlignment = Alignment.TopCenter) {
                    AsyncImage(model = ui.logo, contentDescription = title, contentScale = ContentScale.Fit,
                        modifier = Modifier.widthIn(max = 320.dp).heightIn(max = 64.dp))
                }
            } else {
                Text(title, style = NiuniaText.Logo.copy(fontSize = 32.sp, lineHeight = 38.sp), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = NiuniaDims.Gutter, top = 28.dp).width(680.dp))
            }
            // 2. Nagłówek od lewej: meta → opis max 3 linie → [+ Do biblioteki] [▶ Zwiastun].
            Column(Modifier.padding(start = NiuniaDims.Gutter, top = 20.dp).width(680.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val meta = listOfNotNull(
                    stringResource(if (series) R.string.niunia_player_series else R.string.niunia_player_movie),
                    ui.releaseYear?.takeIf { it.isNotBlank() },
                    if (series) "S${ui.currentSeason}:O${ui.currentEpisode}" else null,
                    ui.currentEpisodeTitle?.takeIf { series && it.isNotBlank() },
                ).joinToString(" · ")
                Text(meta, style = NiuniaText.Meta.copy(fontSize = 14.sp), color = NiuniaColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                ui.description?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = NiuniaText.Body, color = NiuniaColors.Text70, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.nuvio.tv.skin.niunia.components.NiuniaButton(
                        onClick = { Log.i(TAG, "odtwarzacz: szczegóły → biblioteka (było ${extras.inLibrary})"); onLibrary() },
                        label = stringResource(if (extras.inLibrary) R.string.niunia_pinfo_in_library else R.string.niunia_pinfo_add_library),
                        icon = if (extras.inLibrary) "check" else "plus", small = true, focusRequester = fallback)
                    com.nuvio.tv.skin.niunia.components.NiuniaButton(
                        onClick = { Log.i(TAG, "odtwarzacz: szczegóły → zwiastun"); onTrailer() },
                        label = stringResource(if (extras.trailerLoading) R.string.niunia_pinfo_trailer_loading else R.string.niunia_pinfo_trailer),
                        icon = "play-small", small = true)
                }
            }
            Spacer(Modifier.height(NiuniaSpace.AfterRow))
            // 3. Sekcje: co obejrzeć → ludzie → podobne → fakty.
            if (hasEpisodes) {
                Row(Modifier.padding(start = NiuniaDims.Gutter).height(22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(stringResource(R.string.niunia_pinfo_episodes))
                    ui.episodesAvailableSeasons.forEach { s ->
                        SeasonPill(s, selected = s == (ui.episodesSelectedSeason ?: ui.currentSeason)) {
                            Log.i(TAG, "odtwarzacz: szczegóły → sezon $s"); onEvent(PlayerEvent.OnEpisodeSeasonSelected(s))
                        }
                    }
                }
                NiuniaPlayerRow(ui, episodes, focus = first, onEvent = onEvent, onUp = { false }, onDown = { false },
                    modifier = Modifier.padding(top = NiuniaSpace.TitleToRow - 22.dp))
                Spacer(Modifier.height(NiuniaSpace.AfterRow))
            }
            if (cast.isNotEmpty()) {
                SectionTitle(stringResource(R.string.niunia_pinfo_cast), Modifier.padding(start = NiuniaDims.Gutter))
                LazyRow(
                    modifier = Modifier.padding(top = NiuniaSpace.TitleToRow - 22.dp),
                    contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter),
                    horizontalArrangement = Arrangement.spacedBy(com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing.Space20),
                ) {
                    items(cast.size) { i ->
                        com.nuvio.tv.skin.niunia.components.NiuniaCastCard(cast[i].name, cast[i].character, cast[i].photo, onClick = { Log.i(TAG, "odtwarzacz: szczegóły → osoba ${cast[i].name} (bez przejścia z odtwarzacza)") },
                            small = true, focusRequester = if (i == 0 && !hasEpisodes) first else null)
                    }
                }
                Spacer(Modifier.height(NiuniaSpace.AfterRow))
            }
            if (similar.isNotEmpty()) {
                SectionTitle(stringResource(R.string.niunia_pinfo_similar), Modifier.padding(start = NiuniaDims.Gutter))
                NiuniaSimilarRow(similar, focus = if (!hasEpisodes && cast.isEmpty()) first else remember { FocusRequester() }, captions = true,
                    onUp = { false }, onDown = { false }, modifier = Modifier.padding(top = NiuniaSpace.TitleToRow - 22.dp))
                Spacer(Modifier.height(NiuniaSpace.AfterRow))
            }
            InfoGrid(ui, series, extras.similarSource)
        }
    }
}

/**
 * Rząd „Podobne” (film, pkt 3): kafle 16:9 200×113 (tło z TMDB, bez tła — plakat), 4 + kawałek piątego.
 * OK otwiera stronę tytułu (trasa szczegółów; odtwarzacz zostaje na stosie). ▲/▼ jak rząd odcinków.
 */
@Composable
internal fun NiuniaSimilarRow(
    items: List<MetaPreview>,
    focus: FocusRequester,
    onUp: () -> Boolean,
    onDown: () -> Boolean,
    modifier: Modifier = Modifier,
    captions: Boolean = true,
) {
    LazyRow(
        modifier = modifier.onPreviewKeyEvent {
            if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (it.key) { Key.DirectionUp -> onUp(); Key.DirectionDown -> onDown(); else -> false }
        },
        contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter),
        horizontalArrangement = Arrangement.spacedBy(NiuniaSpace.TileGap),
    ) {
        itemsIndexed(items, key = { _, m -> "${m.rawType}_${m.id}" }) { i, m ->
            com.nuvio.tv.skin.niunia.components.NiuniaMediaCard(
                preset = com.nuvio.tv.skin.niunia.components.NiuniaCardPreset.StillCard,
                image = m.background ?: m.poster, width = NiuniaSpace.TileW, height = NiuniaSpace.TileH,
                caption = if (captions) m.name else null, title = m.name,
                focusRequester = if (i == 0) focus else null,
                onClick = {
                    val nav = com.nuvio.tv.skin.kicia.youtube.ForgeYouTubeHost.nav
                    Log.i(TAG, "odtwarzacz: podobne → ${m.rawType} ${m.id} (nawigacja=${nav != null})")
                    try { nav?.navigate(com.nuvio.tv.ui.navigation.Screen.Detail.createRoute(m.id, m.rawType)) } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "odtwarzacz: podobne — trasa: ${e.javaClass.simpleName}: ${e.message}")
                    }
                },
            )
        }
    }
}

/** Zwiastun na pełnym ekranie nad odtwarzaczem (film główny wstrzymany); Wstecz lub koniec zwiastuna zamyka. */
@Composable
internal fun NiuniaTrailerOverlay(source: com.nuvio.tv.data.trailer.TrailerPlaybackSource, onClose: () -> Unit) {
    androidx.activity.compose.BackHandler { Log.i(TAG, "odtwarzacz: zwiastun zamknięty (Wstecz)"); onClose() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { try { focus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: fokus zwiastuna: ${e.message}") } }
    Box(Modifier.fillMaxSize().background(Color.Black).focusRequester(focus).focusable()) {
        com.nuvio.tv.ui.components.TrailerPlayer(
            trailerUrl = source.videoUrl, trailerAudioUrl = source.audioUrl, isPlaying = true,
            onEnded = { Log.i(TAG, "odtwarzacz: zwiastun skończony"); onClose() },
            onError = { Log.w(TAG, "odtwarzacz: zwiastun — błąd odtwarzania"); onClose() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    com.nuvio.tv.skin.niunia.components.NiuniaCollectionHeader(text, active = true, modifier = modifier)
}

/** Pigułka sezonu = NButton chip (wybrany 24%, fokus biały). */
@Composable
private fun SeasonPill(season: Int, selected: Boolean, onOk: () -> Unit) {
    com.nuvio.tv.skin.niunia.components.NiuniaButton(
        onClick = onOk, kind = com.nuvio.tv.skin.niunia.components.NiuniaButtonKind.Chip, selected = selected,
        label = if (season == 0) stringResource(R.string.niunia_detail_specials) else stringResource(R.string.niunia_detail_season, season),
    )
}

/** Osoba: kółko z inicjałami (bez zdjęć), imię i rola — wszystko od lewej (pkt 3). */
@Composable
private fun PersonInitials(person: MetaCastMember, focus: FocusRequester?) {
    var focused by remember { mutableStateOf(false) }
    Column(Modifier.width(110.dp)) {
        Box(
            Modifier.size(96.dp).niuniaFocusable(CircleShape, focusRequester = focus, onFocus = { focused = it }, onOk = {}).clip(CircleShape).background(NiuniaColors.Surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials(person.name), style = NiuniaText.Logo.copy(fontSize = 28.sp, lineHeight = 34.sp), color = NiuniaColors.Text50)
        }
        Text(person.name, style = NiuniaText.Meta.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), color = if (focused) NiuniaColors.Text else NiuniaColors.Text70,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        person.character?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = NiuniaText.Body.copy(fontSize = 12.sp, lineHeight = 16.sp), color = NiuniaColors.Text50, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun initials(name: String): String = name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }

/** Informacje: siatka 3 kolumn, etykieta wersalikami 12 sp + wartość 14 sp; puste pola pomijane. */
@Composable
private fun InfoGrid(ui: PlayerUiState, series: Boolean, dataSource: String? = null) {
    val fields = listOfNotNull(
        stringResource(R.string.niunia_pinfo_label_type) to stringResource(if (series) R.string.niunia_player_series else R.string.niunia_player_movie),
        ui.releaseYear?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.niunia_pinfo_label_year) to it },
        if (series && ui.currentSeason != null) stringResource(R.string.niunia_pinfo_label_episode) to "S${ui.currentSeason}:O${ui.currentEpisode}" else null,
        (ui.currentStreamAddonName ?: ui.currentStreamName)?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.niunia_pinfo_label_source) to it },
        dataSource?.let { stringResource(R.string.niunia_pinfo_label_data) to it },
    )
    SectionTitle(stringResource(R.string.niunia_pinfo_info), Modifier.padding(start = NiuniaDims.Gutter))
    Column(Modifier.padding(start = NiuniaDims.Gutter, top = NiuniaSpace.TitleToRow - 22.dp).width(880.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        fields.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.width(277.dp)) {
                        Text(label, style = NiuniaText.Caption.copy(fontSize = 12.sp, lineHeight = 16.sp), color = NiuniaColors.Text50, maxLines = 1)
                        Text(value, style = NiuniaText.Meta, color = NiuniaColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
