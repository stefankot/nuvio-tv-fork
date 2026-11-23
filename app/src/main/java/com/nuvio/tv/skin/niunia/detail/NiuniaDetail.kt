@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.nuvio.tv.skin.niunia.detail

import com.nuvio.tv.skin.niunia.components.NiuniaBlockKind
import com.nuvio.tv.skin.niunia.components.NiuniaButtonKind
import com.nuvio.tv.skin.niunia.components.NiuniaCastCard
import com.nuvio.tv.skin.niunia.components.NiuniaEpisodeCard
import com.nuvio.tv.skin.niunia.components.NiuniaFact
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.focusGroup
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
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
    // UX 22.11: długie OK na „Podobne” = menu opcji bazy (okno rysuje MetaDetailsScreen dla viewModel.posterOptions).
    val posterOptions: (com.nuvio.tv.domain.model.MetaPreview, String?) -> Unit = remember(viewModel) {
        { item, addon -> Log.i(TAG, "szczegóły: długie OK → opcje ${item.id}"); viewModel.posterOptions.show(item, addon) }
    }
    val series = meta.type == ContentType.SERIES || meta.videos.isNotEmpty()
    val playFocus = remember { FocusRequester() }
    val belowFocus = remember { FocusRequester() }
    var focusedRow by remember { mutableIntStateOf(0) }
    // UX 22.11 (ścieżka 19): po Wstecz ze strony osoby albo tytułu z „Podobne” fokus wracał na „Odtwórz” — teraz na ten sam
    // element (klucz zapisany przed przejściem; lista pod hero sama wraca do zapamiętanego miejsca).
    var returnTo by androidx.compose.runtime.saveable.rememberSaveable(meta.id) { mutableStateOf<String?>(null) }
    val returnFocus = remember { FocusRequester() }
    LaunchedEffect(meta.id) {
        Log.i(TAG, "szczegóły: ${meta.rawType} „${meta.name}” (sezony=${uiState.seasons.size}, odcinki=${uiState.episodesForSeason.size}, podobne=${uiState.moreLikeThis.size}, obsada=${meta.castMembers.size}, wznów=${uiState.nextToWatch?.isResume == true})")
        val back = returnTo
        if (back != null) {
            repeat(6) { attempt ->
                kotlinx.coroutines.delay(80)
                try {
                    if (returnFocus.requestFocus(androidx.compose.ui.focus.FocusDirection.Enter)) { Log.i(TAG, "szczegóły: powrót fokusu → $back"); returnTo = null; return@LaunchedEffect }
                    if (attempt == 5) Log.w(TAG, "szczegóły: powrót fokusu na $back: element nie przyjął fokusu — fokus na Odtwórz")
                } catch (e: IllegalStateException) { if (attempt == 5) Log.w(TAG, "szczegóły: powrót fokusu na $back: ${e.javaClass.simpleName}: ${e.message} — fokus na Odtwórz") }
            }
            returnTo = null
        }
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

    val aboutFocus = remember { FocusRequester() }
    val episodeReturn = remember { FocusRequester() }
    val firstEpisode = remember { FocusRequester() }
    var episodeOpen by remember { mutableStateOf<Video?>(null) }
    // UX 22.11: długie OK na odcinku = okno opcji odcinka z Nuvio (Odtwórz, obejrzany/nieobejrzany, sezon, poprzednie odcinki).
    var optionsEpisode by remember { mutableStateOf<Video?>(null) }
    var lastEpisodeId by remember { mutableStateOf<String?>(null) }
    var fullDescription by remember { mutableStateOf(false) }
    // Z-13: nagrody tytułu (źródło: NiuniaAwardsRepo; bez źródła pusto — pasek się nie rysuje, lista pokazuje stan pusty).
    val awards by androidx.compose.runtime.produceState(emptyList<com.nuvio.tv.skin.niunia.awards.NiuniaAward>(), meta.id) {
        value = com.nuvio.tv.skin.niunia.awards.NiuniaAwardsRepo.load(meta.id, meta.rawType, meta.imdbId)
    }
    val extras: com.nuvio.tv.skin.niunia.player.NiuniaPlayerExtrasViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val extrasState by extras.state.collectAsState()
    fun focusAbout(why: String) {
        Log.i(TAG, "szczegóły: $why → Obsada i opis (DetailMore)")
        try { aboutFocus.requestFocus() } catch (e: IllegalStateException) {
            Log.w(TAG, "szczegóły: $why: ${e.javaClass.simpleName}: ${e.message} — fokus na stronę pod hero")
            try { belowFocus.requestFocus() } catch (e2: IllegalStateException) { Log.w(TAG, "szczegóły: $why: ${e2.javaClass.simpleName}: ${e2.message}") }
        }
    }

    Box(Modifier.fillMaxSize()) {
    // Strona odcinka nad stroną serialu: fokus nie może wejść pod spód (test JMGO build147: ▲ z „Wznów” trafiał w hero pod spodem).
    Box(Modifier.fillMaxSize().focusProperties { enter = { if (episodeOpen != null) FocusRequester.Cancel else FocusRequester.Default } }.focusGroup()) {
    // Właściciel 20.11: hero poza listą. ▼ z hero: napisy hero wyjeżdżają, tło mocno rozmyte (Android 11: przyciemnione),
    // strona pod hero wjeżdża od dołu — logo programu na środku (przypięte), niżej rzędy wg makiet Episodes i DetailMore.
    com.nuvio.tv.skin.niunia.components.NiuniaHeroPage(
        background = meta.backdropUrl ?: meta.poster,
        heroFocus = playFocus,
        what = "szczegóły",
        hero = {
            Hero(meta, uiState, series, heroVideo, awards, playFocus, play,
                onLibrary = { Log.i(TAG, "szczegóły: biblioteka (było ${uiState.isInLibrary})"); viewModel.onEvent(MetaDetailsEvent.OnToggleLibrary) },
                onAbout = { focusAbout(if (series) "O serialu" else "O filmie") },
                onFocus = { focusedRow = 0 },
                // UX 22.11: długie OK na „Do biblioteki” = wybór list (Trakt/MDBList), jak w Nuvio.
                onLibraryLong = { Log.i(TAG, "szczegóły: długie OK → listy biblioteki"); viewModel.onEvent(MetaDetailsEvent.OnLibraryLongPress) })
        },
        belowFocusRequester = belowFocus,
        belowEnter = if (series && uiState.episodesForSeason.isNotEmpty()) firstEpisode else null,
        below = {
            PageLogo(meta)
            Box(Modifier.padding(top = 144.dp).fillMaxSize().clipToBounds()) {
                com.nuvio.tv.skin.niunia.components.NiuniaVerticalScroll(pivot = 50.dp) {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)) {
                        // Episodes: pigułki sezonów (wybrany 24%), potem rząd odcinków (EpisodeCard).
                        if (series && uiState.seasons.isNotEmpty()) {
                            // Test JMGO build159: ▲ z odcinka trafiało w najbliższą pigułkę („Sezon 2”), nie w wybrany sezon.
                            val selectedPill = FocusRequester()
                            item(key = "seasons") {
                                com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                                    LazyRow(contentPadding = PaddingValues(horizontal = NiuniaSpacing.Space40), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4)) {
                                        items(uiState.seasons) { sn ->
                                            NiuniaButton(
                                                onClick = { Log.i(TAG, "szczegóły: sezon $sn"); viewModel.onEvent(MetaDetailsEvent.OnSeasonSelected(sn)) },
                                                label = if (sn == 0) stringResource(R.string.niunia_detail_specials) else stringResource(R.string.niunia_detail_season, sn),
                                                kind = NiuniaButtonKind.Chip, selected = sn == uiState.selectedSeason,
                                                focusRequester = if (sn == uiState.selectedSeason) selectedPill else null,
                                                onFocus = { if (it) focusedRow = 1 },
                                                // UX 22.11: długie OK na sezonie = cały sezon obejrzany / nieobejrzany (jak w Nuvio).
                                                onLongClick = {
                                                    val eps = meta.videos.filter { (it.season ?: 0) == sn && it.episode != null }
                                                    val all = eps.isNotEmpty() && eps.all { (sn to (it.episode ?: 0)) in uiState.watchedEpisodes }
                                                    Log.i(TAG, "szczegóły: długie OK → sezon $sn ${if (all) "nieobejrzany" else "obejrzany"} (${eps.size} odc.)")
                                                    viewModel.onEvent(if (all) MetaDetailsEvent.OnMarkSeasonUnwatched(sn) else MetaDetailsEvent.OnMarkSeasonWatched(sn))
                                                    android.widget.Toast.makeText(context, context.getString(if (all) R.string.niunia_detail_season_unwatched else R.string.niunia_detail_season_watched, sn), android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                            item(key = "episodes") {
                                com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                                    LazyRow(Modifier.onPreviewKeyEvent { e ->
                                        if (e.type != androidx.compose.ui.input.key.KeyEventType.KeyDown || e.key != androidx.compose.ui.input.key.Key.DirectionUp) return@onPreviewKeyEvent false
                                        try { selectedPill.requestFocus(); true } catch (ex: IllegalStateException) { Log.w(TAG, "szczegóły: ▲ do sezonu: ${ex.message}"); false }
                                    }, contentPadding = rowPadding(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20)) {
                                        itemsIndexed(uiState.episodesForSeason, key = { _, it -> it.id }) { idx, video ->
                                            val key = (video.season ?: 0) to (video.episode ?: 0)
                                            val wp = uiState.episodeProgressMap[key]
                                            val watched = key in uiState.watchedEpisodes
                                            val m = niuniaEpisodeModel(video, wp?.position, wp?.duration, watched)
                                            NiuniaEpisodeCard(
                                                image = video.thumbnail, eyebrow = stringResource(R.string.niunia_detail_episode, video.episode ?: 0),
                                                title = video.title, description = video.overview, meta = m.meta, caption = m.caption, progress = m.progress, play = true,
                                                current = video.id == heroVideo?.id,
                                                modifier = (if (video.id == lastEpisodeId) Modifier.focusRequester(episodeReturn) else Modifier)
                                                    .then(if (idx == 0) Modifier.focusRequester(firstEpisode) else Modifier),
                                                onFocus = { if (it) focusedRow = 1 },
                                                onClick = { Log.i(TAG, "szczegóły: odcinek S${video.season}:O${video.episode} → strona odcinka"); lastEpisodeId = video.id; episodeOpen = video },
                                                onLongClick = { Log.i(TAG, "szczegóły: długie OK → opcje odcinka S${video.season}:O${video.episode}"); lastEpisodeId = video.id; optionsEpisode = video },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Zwiastuny (TrailerCard): OK = zwiastun na pełnym ekranie (ta sama nakładka co w odtwarzaczu Niuni).
                        val trailers = meta.trailers.filter { !it.ytId.isNullOrBlank() }.take(8)
                        if (trailers.isNotEmpty()) {
                            item(key = "trailers") {
                                RowTitle(stringResource(R.string.niunia_ds_trailers), on = focusedRow == 4)
                                com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                                    LazyRow(contentPadding = rowPadding(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20)) {
                                        itemsIndexed(trailers) { i, t ->
                                            NiuniaEpisodeCard(
                                                image = "https://img.youtube.com/vi/${t.ytId}/hqdefault.jpg", trailer = true, play = true,
                                                title = t.name?.takeIf { it.isNotBlank() } ?: stringResource(R.string.niunia_ds_trailer_n, i + 1),
                                                onFocus = { if (it) focusedRow = 4 },
                                                onClick = {
                                                    Log.i(TAG, "szczegóły: zwiastun ${i + 1}/${trailers.size}")
                                                    // UX 22.11: wczytanie trwa kilka sekund — od razu znak, że coś się dzieje.
                                                    if (!extrasState.trailerLoading) android.widget.Toast.makeText(context, R.string.niunia_pinfo_trailer_loading, android.widget.Toast.LENGTH_SHORT).show()
                                                    extras.playTrailer(meta.name, meta.releaseInfo, meta.apiType, t.ytId)
                                                },
                                            )
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
                                            // Właściciel 22.11: podobne z Trakt/Simkl/TMDB są poziome — plakat pionowy zamiast kadrowania do pionu.
                                            val (art, fallback) = androidx.compose.runtime.remember(item) { com.nuvio.tv.skin.niunia.components.niuniaPortrait(item, "") }
                                            val back = "podobne:${item.rawType}_${item.id}"
                                            NiuniaPoster(
                                                modifier = if (returnTo == back) Modifier.focusRequester(returnFocus) else Modifier,
                                                art = art, fallback = fallback, title = item.name, logo = item.logo, meta = NiuniaLabels.meta(item),
                                                label = NiuniaLabels.label(context, item, 0, rankingRow = false), baked = art != null,
                                                award = com.nuvio.tv.skin.niunia.components.niuniaPosterAward(item.id),
                                                onFocus = { if (it) focusedRow = 2 },
                                                onOk = { Log.i(TAG, "szczegóły: podobne → ${item.id}"); returnTo = back; onNavigateToDetail(item.id, item.rawType, null) },
                                                onLongOk = { posterOptions(item, null) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // DetailMore: Obsada i twórcy (CastCard), fokus startowy tej części = pierwsza osoba.
                        val cast = meta.castMembers.ifEmpty { meta.cast.map { MetaCastMember(name = it) } }
                        if (cast.isNotEmpty()) {
                            item(key = "cast") {
                                RowTitle(stringResource(R.string.niunia_detail_cast), on = focusedRow == 3)
                                com.nuvio.tv.skin.niunia.components.NiuniaRowScroll {
                                    LazyRow(contentPadding = rowPadding(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20)) {
                                        itemsIndexed(cast.take(20), key = { i, it -> "${i}_${it.name}" }) { i, person ->
                                            val back = "osoba:$i"
                                            NiuniaCastCard(
                                                name = person.name, role = person.character, photo = person.photo,
                                                modifier = (if (i == 0) Modifier.focusRequester(aboutFocus) else Modifier)
                                                    .then(if (returnTo == back) Modifier.focusRequester(returnFocus) else Modifier),
                                                onFocus = { if (it) focusedRow = 3 },
                                                onClick = {
                                                    val id = person.tmdbId
                                                    if (id != null) { Log.i(TAG, "szczegóły: osoba → ${person.name}"); returnTo = back; onNavigateToCastDetail(id, person.name, false) }
                                                    else { Log.i(TAG, "szczegóły: ${person.name} bez id TMDB — brak strony osoby"); com.nuvio.tv.fork.ForkLog.say(context, TAG, context.getString(R.string.niunia_ds_no_play)) }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "about") {
                            AboutSection(meta, series, uiState, full = fullDescription, onMore = { fullDescription = !fullDescription; Log.i(TAG, "szczegóły: opis ${if (fullDescription) "cały" else "skrócony"}") },
                                onFocus = { focusedRow = 5 }, active = focusedRow == 5)
                        }
                        // Z-13: „Nagrody i nominacje” na samym dole (wiersze z fokusem — pilot dochodzi do końca strony).
                        item(key = "awards") {
                            com.nuvio.tv.skin.niunia.components.NiuniaAwardsList(awards,
                                Modifier.padding(start = NiuniaSpacing.Space40, end = NiuniaSpacing.Space40, top = NiuniaSpacing.Space12, bottom = NiuniaSpacing.Space40),
                                onFocus = { if (it) focusedRow = 6 })
                        }
                    }
                }
            }
        },
    )
    }
    // Episode: strona odcinka nad stroną serialu; Wstecz → rząd odcinków (fokus na tym samym odcinku).
    episodeOpen?.let { ep ->
        EpisodePage(meta, ep, uiState,
            onPlay = { Log.i(TAG, "odcinek: odtwórz S${ep.season}:O${ep.episode}"); playEpisode(ep) },
            onAbout = { episodeOpen = null; focusAbout("O odcinku") },
            onBack = {
                Log.i(TAG, "odcinek: Wstecz → odcinki")
                episodeOpen = null
            })
    }
    optionsEpisode?.let { ep ->
        val sn = ep.season ?: 0
        val en = ep.episode ?: 0
        val wp = uiState.episodeProgressMap[sn to en]
        val seasonEps = uiState.episodesForSeason
        com.nuvio.tv.ui.screens.detail.EpisodeOptionsOverlay(
            episode = ep,
            isWatched = wp?.isCompleted() == true || (sn to en) in uiState.watchedEpisodes,
            blurUnwatchedEpisodes = uiState.blurUnwatchedEpisodes,
            style = uiState.episodeOptionsOverlayStyle,
            isPending = "${ep.id}:${ep.season ?: -1}:${ep.episode ?: -1}" in uiState.episodeWatchedPendingKeys,
            isSeasonFullyWatched = seasonEps.isNotEmpty() && seasonEps.all { (sn to (it.episode ?: 0)) in uiState.watchedEpisodes },
            hasPreviousEpisodes = seasonEps.any { (it.episode ?: Int.MAX_VALUE) < en },
            onDismiss = { Log.i(TAG, "odcinek: opcje zamknięte"); optionsEpisode = null },
            onNavigateEpisode = { step ->
                val i = seasonEps.indexOfFirst { it.id == ep.id }
                seasonEps.getOrNull(i + step)?.let { optionsEpisode = it; lastEpisodeId = it.id }
            },
            onPlay = { Log.i(TAG, "odcinek: opcje → odtwórz S$sn:O$en"); optionsEpisode = null; playEpisode(ep) },
            onToggleWatched = { Log.i(TAG, "odcinek: opcje → obejrzany przełączony S$sn:O$en"); optionsEpisode = null; viewModel.onEvent(MetaDetailsEvent.OnToggleEpisodeWatched(ep)) },
            onMarkSeasonWatched = { Log.i(TAG, "odcinek: opcje → sezon $sn obejrzany"); optionsEpisode = null; viewModel.onEvent(MetaDetailsEvent.OnMarkSeasonWatched(sn)) },
            onMarkSeasonUnwatched = { Log.i(TAG, "odcinek: opcje → sezon $sn nieobejrzany"); optionsEpisode = null; viewModel.onEvent(MetaDetailsEvent.OnMarkSeasonUnwatched(sn)) },
            onMarkPreviousEpisodesWatched = { Log.i(TAG, "odcinek: opcje → poprzednie obejrzane S$sn:O$en"); optionsEpisode = null; viewModel.onEvent(MetaDetailsEvent.OnMarkPreviousEpisodesWatched(ep)) },
        )
    }
    LaunchedEffect(optionsEpisode) {
        if (optionsEpisode == null && episodeOpen == null && lastEpisodeId != null) {
            kotlinx.coroutines.delay(80)
            try { episodeReturn.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odcinek: powrót fokusu po opcjach: ${e.javaClass.simpleName}: ${e.message}") }
        }
    }
    LaunchedEffect(episodeOpen) {
        if (episodeOpen == null && lastEpisodeId != null) {
            kotlinx.coroutines.delay(80)
            try { episodeReturn.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odcinek: powrót fokusu: ${e.javaClass.simpleName}: ${e.message}") }
        }
    }
    extrasState.trailer?.let { t -> com.nuvio.tv.skin.niunia.player.NiuniaTrailerOverlay(t) { extras.closeTrailer() } }
    LaunchedEffect(extrasState.message) { extrasState.message?.let { com.nuvio.tv.fork.ForkLog.say(context, TAG, it); extras.clearMessage() } }
    }
}

/** Dane karty odcinka: „39m” (zostało) albo „65m” (długość), postęp %, meta „2 wrz 2022 · 65 min”. */
internal data class NiuniaEpisodeModel(val caption: String?, val progress: Int?, val meta: String?)

internal fun niuniaEpisodeModel(video: Video, positionMs: Long?, durationMs: Long?, watched: Boolean): NiuniaEpisodeModel {
    val dur = durationMs?.takeIf { it > 0 }
    val pos = positionMs?.takeIf { it > 0 }
    val inProgress = !watched && dur != null && pos != null && pos < dur
    val left = if (inProgress) ((dur!! - pos!!) / 60_000L).toInt().takeIf { it > 0 } else null
    val caption = left?.let { "${it}m" } ?: video.runtime?.let { "${it}m" }
    val progress = if (watched) 100 else if (inProgress) ((pos!!.toFloat() / dur!!) * 100).toInt().coerceIn(0, 100) else null
    val meta = listOfNotNull(niuniaDate(video.released), video.runtime?.let { "$it min" }).joinToString(" · ").ifBlank { null }
    return NiuniaEpisodeModel(caption, progress, meta)
}

/** DetailMore „Opis”: panel z opisem (3 linie, „Więcej” = cały) + Informacje + Języki (tylko pola, które są). */
@Composable
private fun AboutSection(meta: Meta, series: Boolean, uiState: MetaDetailsUiState, full: Boolean, onMore: () -> Unit, onFocus: () -> Unit, active: Boolean) {
    val ctx = LocalContext.current
    RowTitle(stringResource(R.string.niunia_ds_description), on = active)
    Row(Modifier.padding(start = NiuniaSpacing.Space40, end = NiuniaSpacing.Space40, top = NiuniaSpacing.Space12, bottom = NiuniaSpacing.Space40),
        horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space40), verticalAlignment = Alignment.Top) {
        NiuniaContentBlock(NiuniaBlockKind.Panel) {
            NText(meta.description?.takeIf { it.isNotBlank() } ?: meta.name, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = if (full) Int.MAX_VALUE else 3)
            // UX 22.11: po rozwinięciu napis „Mniej” (było stale „Więcej” — nie było wiadomo, że OK zwija).
            NiuniaButton(onClick = onMore, label = stringResource(if (full) R.string.niunia_ds_less else R.string.niunia_player_more), kind = NiuniaButtonKind.Chip, selected = full,
                description = stringResource(if (full) R.string.niunia_ds_show_less else R.string.niunia_ds_show_full), onFocus = { if (it) onFocus() })
        }
        val episodes = meta.videos.count { (it.season ?: 0) > 0 }
        val info = listOfNotNull(
            niuniaDate(meta.released)?.let { ctx.getString(R.string.niunia_ds_premiere, it) },
            if (series) listOfNotNull(
                uiState.seasons.count { it > 0 }.takeIf { it > 0 }?.let { n -> when { n == 1 -> ctx.getString(R.string.niunia_ds_seasons_one); n % 10 in 2..4 && n % 100 !in 12..14 -> ctx.getString(R.string.niunia_ds_seasons_few, n); else -> ctx.getString(R.string.niunia_ds_seasons_many, n) } },
                episodes.takeIf { it > 0 }?.let { ctx.getString(R.string.niunia_ds_episodes_count, it) },
            ).joinToString(" · ").ifBlank { null } else niuniaRuntimeLabel(ctx, meta.runtime), // test JMGO build159: było „137”
            (if (series) meta.writer.ifEmpty { meta.director } else meta.director).take(2).joinToString(", ").ifBlank { null }
                ?.let { ctx.getString(if (series) R.string.niunia_ds_creators_line else R.string.niunia_ds_director_line, it) },
            (meta.networks + meta.productionCompanies).map { it.name }.distinct().take(2).joinToString(" · ").ifBlank { null },
        )
        if (info.isNotEmpty()) NiuniaFact(stringResource(R.string.niunia_detail_info), info, Modifier.weight(1f))
        val lang = meta.language?.takeIf { it.isNotBlank() }?.let { niuniaLanguageName(it) }
        if (lang != null) NiuniaFact(stringResource(R.string.niunia_ds_languages), listOf(ctx.getString(R.string.niunia_ds_original, lang)), Modifier.weight(1f))
    }
}

/**
 * Strona odcinka (makieta `Episode`, NHero immersive na całą planszę): logo małe, tytuł odcinka 32/38, „Serial · gatunki”,
 * „**S1, E1 ·** opis” (2 linie), data · długość ze znacznikami, Wznów (etykieta, postęp, „39m”) / Odtwórz — fokus startowy,
 * „O odcinku” → Obsada i opis. Wstecz → rząd odcinków.
 */
@Composable
private fun EpisodePage(meta: Meta, ep: Video, uiState: MetaDetailsUiState, onPlay: () -> Unit, onAbout: () -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val focus = remember { FocusRequester() }
    androidx.activity.compose.BackHandler { onBack() }
    LaunchedEffect(ep.id) {
        Log.i(TAG, "odcinek: S${ep.season}:O${ep.episode} „${ep.title}”, fokus startowy → Wznów/Odtwórz")
        kotlinx.coroutines.delay(60)
        try { focus.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odcinek: fokus: ${e.javaClass.simpleName}: ${e.message}") }
    }
    val wp = uiState.episodeProgressMap[(ep.season ?: 0) to (ep.episode ?: 0)]
    val watched = ((ep.season ?: 0) to (ep.episode ?: 0)) in uiState.watchedEpisodes
    val m = niuniaEpisodeModel(ep, wp?.position, wp?.duration, watched)
    val resume = m.progress != null && m.progress in 1..99
    Box(Modifier.fillMaxSize().focusGroup()) {
        com.nuvio.tv.skin.niunia.components.NiuniaHeroBackdrop(com.nuvio.tv.skin.niunia.components.NiuniaHeroKind.Immersive, ep.thumbnail ?: meta.backdropUrl ?: meta.poster)
        Column(Modifier.align(Alignment.BottomStart).padding(start = NiuniaSpacing.Space40, end = NiuniaSpacing.Space40, bottom = NiuniaSpacing.Space40),
            verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space16)) {
            NiuniaContentBlock(NiuniaBlockKind.Info) {
                NiuniaLogo(meta.logo, meta.name, NiuniaLogoSize.Small)
                NText(ep.title.ifBlank { ctx.getString(R.string.niunia_detail_episode, ep.episode ?: 0) }, variant = NiuniaTextVariant.TitleScreen, lines = 2)
                NText(listOfNotNull(ctx.getString(R.string.niunia_player_series), meta.genres.take(2).joinToString(" · ").ifBlank { null }).joinToString(" · "), variant = NiuniaTextVariant.BodySmall, lines = 1)
                val text = ep.overview?.takeIf { it.isNotBlank() }
                if (text != null) NText("**S${ep.season}, E${ep.episode} ·** $text", variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 2)
                NiuniaMetaLine(m.meta) { meta.ageRating?.takeIf { it.isNotBlank() }?.let { NiuniaTag(it, NiuniaTagKind.Age) } }
            }
            NiuniaCollection(NiuniaCollectionKind.Actions) {
                if (resume) NiuniaButton(onClick = onPlay, label = stringResource(R.string.niunia_detail_resume), icon = "play", labelFirst = true,
                    progress = m.progress!! / 100f, trailing = m.caption, focusRequester = focus,
                    description = stringResource(R.string.niunia_ds_resume_left, m.caption ?: ""))
                else NiuniaButton(onClick = onPlay, label = stringResource(R.string.niunia_detail_play), icon = "play", focusRequester = focus)
                NiuniaButton(onClick = onAbout, icon = "about", description = stringResource(R.string.niunia_ds_about_episode))
            }
        }
    }
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
    awards: List<com.nuvio.tv.skin.niunia.awards.NiuniaAward>,
    playFocus: FocusRequester,
    onPlay: () -> Unit,
    onLibrary: () -> Unit,
    onAbout: () -> Unit,
    onFocus: () -> Unit,
    onLibraryLong: (() -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val next = uiState.nextToWatch
    val resume = next?.takeIf { it.isResume }?.watchProgress
    // Serial: kadry odcinków (Detail); film: kadry zwiastunów (Movie). Tylko obraz — ▼ przenosi na stronę pod hero.
    val stills: List<Pair<String?, String>> = if (series) uiState.episodesForSeason.take(5).map { it.thumbnail to it.title }
        else meta.trailers.filter { !it.ytId.isNullOrBlank() }.take(4).map { "https://img.youtube.com/vi/${it.ytId}/hqdefault.jpg" to (it.name ?: meta.name) }
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
                    (ep?.runtime?.let { "$it min" } ?: niuniaRuntimeLabel(ctx, meta.runtime)),
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
                    // Jeden przycisk (nie if/else): dane „Wznów” przychodzą po pierwszej klatce, a podmiana kompozycji gubiła fokus startowy.
                    val resumeOn = resume != null && left != null
                    val label = when {
                        resumeOn -> "${left}m"
                        series && heroVideo != null -> stringResource(R.string.niunia_detail_play_episode, heroVideo.season ?: 1, heroVideo.episode ?: 1)
                        else -> stringResource(R.string.niunia_detail_play)
                    }
                    NiuniaButton(
                        onClick = onPlay, label = label, icon = "play", compact = resumeOn,
                        progress = if (resumeOn) resume!!.progressPercentage.coerceIn(0f, 1f) else null,
                        description = if (resumeOn) stringResource(R.string.niunia_ds_resume_left, listOfNotNull(epLabel, "$left min").joinToString(", ")) else label,
                        focusRequester = playFocus, onFocus = { if (it) onFocus() },
                    )
                    NiuniaButton(onClick = onLibrary, icon = if (uiState.isInLibrary) "check" else "plus", onLongClick = onLibraryLong,
                        description = stringResource(if (uiState.isInLibrary) R.string.niunia_pinfo_in_library else R.string.niunia_ds_add_library), onFocus = { if (it) onFocus() })
                    NiuniaButton(onClick = onAbout, icon = "about",
                        description = stringResource(if (series) R.string.niunia_ds_about_series else R.string.niunia_ds_about_movie), onFocus = { if (it) onFocus() })
                }
                Credits(meta, series, awards, Modifier)
            }
            if (stills.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space20)) {
                stills.forEach { (img, t) -> NiuniaMediaCard(NiuniaCardPreset.StillCard, img, onClick = null, title = t, focusable = false) }
            }
        }
    }
}

/** Kod języka („en”, „pl-PL”) → nazwa po polsku („angielski”); pełna nazwa zostaje bez zmian. */
internal fun niuniaLanguageName(raw: String): String {
    val t = raw.trim()
    if (t.length > 5 || t.contains(' ')) return t
    val name = java.util.Locale(t.substringBefore('-').substringBefore('_').lowercase()).getDisplayLanguage(java.util.Locale("pl", "PL"))
    return if (name.isBlank() || name.equals(t, ignoreCase = true)) t else name
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
    } else niuniaRuntimeLabel(ctx, meta.runtime).takeIf { !series },
).joinToString(" · ")

/** „107” → „1 godz. 47 min”; nieznany zapis zostaje. */
internal fun niuniaRuntimeLabel(ctx: android.content.Context, raw: String?): String? =
    com.nuvio.tv.skin.niunia.home.niuniaRuntime(raw)?.let { (h, m) -> if (h > 0) ctx.getString(R.string.niunia_hours_minutes, h, m) else ctx.getString(R.string.niunia_minutes, m) }
        ?: raw?.takeIf { it.isNotBlank() }

/** Aktorzy do linii „Obsada”: bez twórców i reżyserów (TMDB podaje ich na początku listy), najpierw osoby z rolą. */
internal fun niuniaStars(meta: Meta, n: Int = 3): List<String> {
    val crew = (meta.writer + meta.director).toSet()
    val members = meta.castMembers.filter { it.name !in crew }
    val withRole = members.filter { !it.character.isNullOrBlank() }
    return (withRole.ifEmpty { members }.map { it.name }.ifEmpty { meta.cast.filter { it !in crew } }).take(n)
}

/** NContentBlock `credits`: „Obsada **…**”, „Twórcy/Reżyseria **…**”, 14/20 w 50%, nazwiska pogrubione białe. */
@Composable
private fun Credits(meta: Meta, series: Boolean, awards: List<com.nuvio.tv.skin.niunia.awards.NiuniaAward>, modifier: Modifier) {
    val makers = (if (series) meta.writer.ifEmpty { meta.director } else meta.director).take(2)
    val stars = niuniaStars(meta)
    if (stars.isEmpty() && makers.isEmpty() && awards.isEmpty()) return
    NiuniaContentBlock(NiuniaBlockKind.Credits, modifier) {
        // Z-13: pasek nagród na górze bloku obsady, 8 dp nad „Obsada / Twórcy” (blok ma odstęp 4).
        com.nuvio.tv.skin.niunia.components.NiuniaAwardsStrip(awards, Modifier.padding(bottom = NiuniaSpacing.Space4))
        if (stars.isNotEmpty()) NText("${stringResource(R.string.niunia_ds_cast)} **${stars.joinToString(", ")}**", variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 2)
        if (makers.isNotEmpty()) NText("${stringResource(if (series) R.string.niunia_detail_created_by else R.string.niunia_detail_directed_by)} **${makers.joinToString(", ")}**",
            variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 2)
        val studios = (meta.networks + meta.productionCompanies).map { it.name }.distinct().take(2)
        if (!series && studios.isNotEmpty()) NText(studios.joinToString(" · "), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 1)
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
