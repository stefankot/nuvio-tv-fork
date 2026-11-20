package com.nuvio.tv.skin.niunia.detail

import com.nuvio.tv.skin.niunia.components.NiuniaBlockKind
import com.nuvio.tv.skin.niunia.components.NiuniaCardPreset
import com.nuvio.tv.skin.niunia.components.NiuniaCollection
import com.nuvio.tv.skin.niunia.components.NiuniaCollectionKind
import com.nuvio.tv.skin.niunia.components.NiuniaContentBlock
import com.nuvio.tv.skin.niunia.components.NiuniaLogo
import com.nuvio.tv.skin.niunia.components.NiuniaLogoSize
import com.nuvio.tv.skin.niunia.components.NiuniaMediaCard
import com.nuvio.tv.skin.niunia.components.NiuniaMetaLine
import com.nuvio.tv.skin.niunia.components.NiuniaSurface
import com.nuvio.tv.skin.niunia.components.NiuniaSurfaceKind
import com.nuvio.tv.skin.niunia.components.NiuniaTag
import com.nuvio.tv.skin.niunia.components.NiuniaTagKind
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.components.NiuniaText as NText
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing

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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
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
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpace
import kotlinx.coroutines.launch
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
    val belowFocus = remember { FocusRequester() }
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

    // Właściciel 20.11: hero poza listą (był przesunięty o ujemny margines). ▼ z hero: napisy hero wyjeżdżają do góry, tło dostaje
    // bardzo mocne rozmycie, a strona pod hero wjeżdża od dołu — na górze logo programu na środku (przypięte), niżej rzędy.
    com.nuvio.tv.skin.niunia.components.NiuniaHeroPage(
        background = meta.backdropUrl ?: meta.poster,
        heroFocus = playFocus,
        what = "szczegóły",
        hero = {
            Hero(meta, uiState, series, heroVideo, playFocus, play,
                onLibrary = { Log.i(TAG, "szczegóły: biblioteka (było ${uiState.isInLibrary})"); viewModel.onEvent(MetaDetailsEvent.OnToggleLibrary) },
                onAbout = {
                    Log.i(TAG, "szczegóły: O ${if (series) "serialu" else "filmie"} → strona pod hero")
                    try { belowFocus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "szczegóły: O serialu: ${e.javaClass.simpleName}: ${e.message}") }
                },
                onFocus = { focusedRow = 0 })
        },
        belowFocusRequester = belowFocus,
        below = {
    PageLogo(meta)
    Box(Modifier.padding(top = 144.dp).fillMaxSize().clipToBounds()) {
    com.nuvio.tv.skin.niunia.components.NiuniaVerticalScroll(pivot = 50.dp) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)) {
        if (series && uiState.seasons.isNotEmpty()) {
            item(key = "episodes") {
                com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                    SeasonTabs(uiState.seasons, uiState.selectedSeason, onFocus = { focusedRow = 1 }) {
                        Log.i(TAG, "szczegóły: sezon $it"); viewModel.onEvent(MetaDetailsEvent.OnSeasonSelected(it))
                    }
                }
                com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                LazyRow(contentPadding = rowPadding(), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
                    items(uiState.episodesForSeason, key = { it.id }) { video ->
                        val progress = uiState.episodeProgressMap[(video.season ?: 0) to (video.episode ?: 0)]?.progressPercentage
                        val watched = (video.season ?: 0) to (video.episode ?: 0) in uiState.watchedEpisodes
                        EpisodeCard(video, progress, watched, onFocus = { focusedRow = 1 }, onOk = { Log.i(TAG, "szczegóły: odcinek S${video.season}:O${video.episode}"); playEpisode(video) })
                    }
                }
                }
            }
        }
        if (uiState.moreLikeThis.isNotEmpty()) {
            item(key = "more") {
                RowTitle(stringResource(R.string.niunia_detail_more_like), on = focusedRow == 2)
                com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                LazyRow(contentPadding = rowPadding(), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
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
        }
        val cast = meta.castMembers.ifEmpty { meta.cast.map { MetaCastMember(name = it) } }
        if (cast.isNotEmpty()) {
            item(key = "cast") {
                RowTitle(stringResource(R.string.niunia_detail_cast), on = focusedRow == 3)
                com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                LazyRow(contentPadding = rowPadding(), horizontalArrangement = Arrangement.spacedBy(NiuniaDims.RowGap)) {
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
        }
        item(key = "info") { InfoPanel(meta) }
    }
    }
    }
        },
    )
}

/** Logo programu na środku u góry części pod hero (jedyny wyjątek od „bez centrowania”), do 64 dp wysokości, 40 dp odstępu niżej. */
@Composable
private fun PageLogo(meta: Meta) {
    if (meta.logo != null) {
        Box(Modifier.fillMaxWidth().padding(top = NiuniaSpace.TopAfterHero, bottom = 40.dp), contentAlignment = Alignment.TopCenter) {
            AsyncImage(model = meta.logo, contentDescription = meta.name, contentScale = ContentScale.Fit,
                modifier = Modifier.width(320.dp).height(64.dp))
        }
    } else {
        Box(Modifier.fillMaxWidth().padding(top = NiuniaSpace.TopAfterHero, bottom = 40.dp).height(64.dp), contentAlignment = Alignment.Center) {
            Text(meta.name, style = NiuniaText.Logo.copy(fontSize = 32.sp, lineHeight = 38.sp), maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 160.dp))
        }
    }
}

private fun rowPadding() = PaddingValues(start = NiuniaDims.Gutter, end = NiuniaDims.Gutter, top = NiuniaSpace.TitleToRow - 22.dp, bottom = NiuniaSpace.AfterRow)

/**
 * Hero strony filmu/serialu wg makiety `Detail` (NHero immersive, `peek`): chip „Obejrzyj z lektorem” (gdy moduł lektora
 * jest włączony), logo, linia „Serial · gatunki · N sezonów”, opis odcinka z pogrubionym „S1, E1 · tytuł:”, linia daty i
 * długości ze znacznikami, wiersz akcji (Wznów z postępem i „39m” / Odtwórz — fokus startowy; biblioteka; O serialu) z obsadą
 * i twórcami po prawej, u dołu wystające kadry odcinków (▼ → strona pod hero = `Episodes`).
 */
@Composable
private fun Hero(
    meta: Meta,
    uiState: MetaDetailsUiState,
    series: Boolean,
    heroVideo: Video?,
    playFocus: FocusRequester,
    onPlay: () -> Unit,
    onLibrary: () -> Unit,
    onAbout: () -> Unit,
    onFocus: () -> Unit,
) {
    val ctx = LocalContext.current
    val next = uiState.nextToWatch
    val resume = next?.takeIf { it.isResume }?.watchProgress
    val stills = if (series) uiState.episodesForSeason.take(5) else emptyList()
    Box(Modifier.fillMaxSize()) { // tło rysuje NiuniaHeroPage
        NiuniaSurface(NiuniaSurfaceKind.ScrimHero, Modifier.fillMaxSize())
        Column(
            Modifier.align(Alignment.BottomStart).offset(y = if (stills.isNotEmpty()) NiuniaSize.Peek else -NiuniaSpacing.Space40)
                .padding(horizontal = NiuniaSpacing.Space40).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space16),
        ) {
            NiuniaContentBlock(NiuniaBlockKind.Info) {
                if (com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(ctx, "module_lektor")) NiuniaTag(stringResource(R.string.niunia_ds_watch_lektor), NiuniaTagKind.Chip)
                NiuniaLogo(meta.logo, meta.name, NiuniaLogoSize.Hero)
                NText(niuniaDetailMetaLine(ctx, meta, series, uiState.seasons.size), variant = NiuniaTextVariant.BodySmall, lines = 1)
                val ep = heroVideo?.takeIf { series }
                val description = (ep?.overview?.takeIf { it.isNotBlank() } ?: meta.description)?.takeIf { it.isNotBlank() }
                val prefix = ep?.let { v -> listOfNotNull("S${v.season}, E${v.episode}", v.title.takeIf { it.isNotBlank() }).joinToString(" · ") }
                if (description != null) NText(if (prefix != null) "**$prefix:** $description" else description, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 3)
                val dateLine = listOfNotNull(
                    niuniaDate(ep?.released ?: meta.released),
                    (ep?.runtime?.let { "$it min" } ?: meta.runtime?.takeIf { it.isNotBlank() }),
                ).joinToString(" · ")
                NiuniaMetaLine(dateLine.ifBlank { null }) {
                    meta.imdbRating?.takeIf { it > 0f }?.let { NiuniaTag("IMDb %.1f".format(java.util.Locale.US, it), NiuniaTagKind.Quality) }
                    meta.ageRating?.takeIf { it.isNotBlank() }?.let { NiuniaTag(it, NiuniaTagKind.Age) }
                }
            }
            NiuniaCollection(NiuniaCollectionKind.SplitRow) {
                Row(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12), verticalAlignment = Alignment.CenterVertically) {
                    val left = resume?.let { p -> ((p.duration - p.position).coerceAtLeast(0) / 60_000L).toInt() }?.takeIf { it > 0 }
                    val epLabel = next?.let { n -> if (n.nextSeason != null && n.nextEpisode != null) "S${n.nextSeason}, E${n.nextEpisode}" else null }
                    if (resume != null && left != null) {
                        NiuniaButton(
                            onClick = onPlay, label = "${left}m", icon = "play", compact = true, progress = resume.progressPercentage.coerceIn(0f, 1f),
                            description = stringResource(R.string.niunia_ds_resume_left, listOfNotNull(epLabel, "$left min").joinToString(", ")),
                            focusRequester = playFocus, onFocus = { if (it) onFocus() },
                        )
                    } else {
                        val label = if (series && heroVideo != null) stringResource(R.string.niunia_detail_play_episode, heroVideo.season ?: 1, heroVideo.episode ?: 1) else stringResource(R.string.niunia_detail_play)
                        NiuniaButton(onClick = onPlay, label = label, icon = "play", focusRequester = playFocus, onFocus = { if (it) onFocus() })
                    }
                    NiuniaButton(onClick = onLibrary, icon = if (uiState.isInLibrary) "check" else "plus",
                        description = stringResource(if (uiState.isInLibrary) R.string.niunia_pinfo_in_library else R.string.niunia_ds_add_library), onFocus = { if (it) onFocus() })
                    NiuniaButton(onClick = onAbout, icon = "about",
                        description = stringResource(if (series) R.string.niunia_ds_about_series else R.string.niunia_ds_about_movie), onFocus = { if (it) onFocus() })
                }
                Credits(meta, series, Modifier)
            }
            if (stills.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20)) {
                stills.forEach { v ->
                    NiuniaMediaCard(NiuniaCardPreset.StillCard, v.thumbnail, onClick = null, title = v.title, focusable = false,
                        description = stringResource(R.string.niunia_detail_episode, v.episode ?: 0))
                }
            }
        }
    }
}

/** „2 wrz 2022” z daty ISO (pierwsze 10 znaków); inaczej null. */
internal fun niuniaDate(iso: String?): String? {
    val d = iso?.take(10) ?: return null
    val m = Regex("""(\d{4})-(\d{2})-(\d{2})""").matchEntire(d) ?: return null
    val months = listOf("sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru")
    val (y, mo, day) = m.destructured
    return "${day.toInt()} ${months.getOrNull(mo.toInt() - 1) ?: return null} $y"
}

/** „Serial · Fantasy · Dramat · 2 sezony” / „Film · Przygodowy · 2026 · 2 godz. 53 min”. */
internal fun niuniaDetailMetaLine(ctx: android.content.Context, meta: Meta, series: Boolean, seasons: Int): String = listOfNotNull(
    ctx.getString(if (series) R.string.niunia_player_series else R.string.niunia_player_movie),
    meta.genres.take(2).joinToString(" · ").ifBlank { null },
    meta.releaseInfo?.takeIf { it.isNotBlank() && !series },
    if (series && seasons > 0) when {
        seasons == 1 -> ctx.getString(R.string.niunia_ds_seasons_one)
        seasons % 10 in 2..4 && seasons % 100 !in 12..14 -> ctx.getString(R.string.niunia_ds_seasons_few, seasons)
        else -> ctx.getString(R.string.niunia_ds_seasons_many, seasons)
    } else meta.runtime?.takeIf { it.isNotBlank() && !series },
).joinToString(" · ")

/** NContentBlock `credits`: „Obsada **…**”, „Twórcy/Reżyseria **…**”, 14/20 w 50%, nazwiska pogrubione białe. */
@Composable
private fun Credits(meta: Meta, series: Boolean, modifier: Modifier) {
    val stars = meta.castMembers.map { it.name }.ifEmpty { meta.cast }.take(3)
    val makers = (if (series) meta.writer.ifEmpty { meta.director } else meta.director).take(2)
    if (stars.isEmpty() && makers.isEmpty()) return
    NiuniaContentBlock(NiuniaBlockKind.Credits, modifier) {
        if (stars.isNotEmpty()) NText("${stringResource(R.string.niunia_ds_cast)} **${stars.joinToString(", ")}**", variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 2)
        if (makers.isNotEmpty()) NText("${stringResource(if (series) R.string.niunia_detail_created_by else R.string.niunia_detail_directed_by)} **${makers.joinToString(", ")}**",
            variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 2)
    }
}

/** canvas .tabs: pigułki sezonów (wybrany = białe 24%, fokus = białe tło). */
@Composable
private fun SeasonTabs(seasons: List<Int>, selected: Int, onFocus: () -> Unit = {}, onSelect: (Int) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = NiuniaDims.Gutter), horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        items(seasons) { s ->
            var focused by remember { mutableStateOf(false) }
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .height(34.dp)
                    .niuniaFocusable(shape, scale = NiuniaDims.ScaleButton, lift = false, onFocus = { focused = it; if (it) onFocus() }, onOk = { onSelect(s) })
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
    Column(Modifier.width(130.dp), horizontalAlignment = Alignment.Start) { // pkt 3: kółko, imię i rola od lewej
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
    // Tytuł 22 dp; rząd 34 dp od góry tytułu, po rzędzie 36 dp (rowPadding).
    Text(text, style = NiuniaText.Row, color = if (on) NiuniaColors.Text else NiuniaColors.Text50, maxLines = 1, modifier = Modifier.padding(start = NiuniaDims.Gutter).height(22.dp))
}
