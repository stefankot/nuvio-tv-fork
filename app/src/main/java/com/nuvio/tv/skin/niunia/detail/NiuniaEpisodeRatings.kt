package com.nuvio.tv.skin.niunia.detail

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.Video
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaButtonKind
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.components.niuniaOk
import com.nuvio.tv.skin.niunia.components.niuniaRatingColor
import com.nuvio.tv.skin.niunia.components.niuniaScoreText
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.skin.niunia.tokens.NiuniaType
import kotlin.math.ceil

private const val TAG = "ForkNiunia"

/** Próg „najlepszych 20%”: górne 20% ocen (zaokrąglone w górę), remisy na progu wchodzą. null = brak ocen. */
internal fun niuniaTopThreshold(values: Collection<Double>): Double? {
    if (values.isEmpty()) return null
    val n = ceil(values.size * 0.2).toInt().coerceAtLeast(1)
    return values.sortedDescending()[n - 1]
}

/**
 * Z-8 (makiety Ratings / RatingsBest, `NRatings kind="table"`): tabela sezon × odcinek nad stroną serialu. Źródło: oceny
 * IMDb odcinków z bazy ([com.nuvio.tv.ui.screens.detail.MetaDetailsUiState.episodeImdbRatings]); TMDB i Trakt dla odcinków
 * fork nie ma, więc chip źródła jest jeden. Chip „Tylko najlepsze 20%”: reszta komórek szara i bez fokusu (pilot je
 * przeskakuje). OK = odtwórz odcinek, długie OK = szczegóły odcinka, Wstecz = zamknięcie. Fokus startowy: pierwsza komórka z oceną.
 */
@Composable
internal fun NiuniaEpisodeRatingsTable(
    title: String,
    episodes: List<Video>,
    ratings: Map<Pair<Int, Int>, Double>,
    onPlay: (Video) -> Unit,
    onDetails: (Video) -> Unit,
    onClose: () -> Unit,
) {
    var best by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf<Video?>(null) }
    val first = remember { FocusRequester() }
    BackHandler { Log.i(TAG, "NRatings: tabela zamknięta (Wstecz)"); onClose() }
    val bySeason = remember(episodes) {
        episodes.filter { (it.season ?: 0) > 0 && (it.episode ?: 0) > 0 }.groupBy { it.season!! }.toSortedMap().mapValues { (_, v) -> v.sortedBy { it.episode } }
    }
    fun score(v: Video) = ratings[(v.season ?: 0) to (v.episode ?: 0)]?.takeIf { it > 0.0 }
    val values = remember(bySeason, ratings) { bySeason.values.flatten().mapNotNull { score(it) } }
    val threshold = remember(values) { niuniaTopThreshold(values) }
    val topCount = remember(values, threshold) { threshold?.let { t -> values.count { it >= t } } ?: 0 }
    val average = remember(values) { if (values.isEmpty()) null else values.average() }
    val firstRated = remember(bySeason, ratings, best) { bySeason.values.flatten().firstOrNull { v -> score(v)?.let { !best || (threshold != null && it >= threshold) } == true } }
    LaunchedEffect(values.size, best) {
        Log.i(TAG, "NRatings: table: ${values.size} episodes, source IMDb, top 20% = $topCount (from ${threshold?.let { niuniaScoreText(it) }})" + if (best) ", filtr włączony" else "")
        kotlinx.coroutines.delay(80)
        try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "NRatings: fokus startowy: ${e.javaClass.simpleName}: ${e.message}") }
    }
    val maxEp = bySeason.values.maxOfOrNull { s -> s.maxOf { it.episode ?: 0 } } ?: 0
    Box(Modifier.fillMaxSize().zIndex(4f).background(NiuniaColor.Bg)) {
        Column(Modifier.fillMaxSize().padding(start = NiuniaSpacing.Space40, end = NiuniaSpacing.Space40, top = NiuniaSpacing.Space40), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space16)) {
            NiuniaText(stringResource(R.string.niunia_tv2_ratings_title, title), variant = NiuniaTextVariant.TitleSection, lines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8), verticalAlignment = Alignment.CenterVertically) {
                NiuniaButton(onClick = { Log.i(TAG, "NRatings: źródło IMDb") }, label = "IMDb", kind = NiuniaButtonKind.Chip, selected = true)
                NiuniaButton(onClick = { best = !best; Log.i(TAG, "NRatings: tylko najlepsze 20% → $best") }, label = stringResource(R.string.niunia_tv2_ratings_best), icon = "star",
                    kind = NiuniaButtonKind.Chip, selected = best)
            }
            val summary = when {
                average == null -> stringResource(R.string.niunia_tv2_ratings_none)
                best -> stringResource(R.string.niunia_tv2_ratings_best_summary, topCount, values.size, niuniaScoreText(threshold!!))
                else -> stringResource(R.string.niunia_tv2_ratings_summary, niuniaScoreText(average), niuniaScoreText(threshold!!))
            }
            NiuniaText(summary, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 1)
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space24)) {
                // Siatka: kolumna sezonu 80 (stoi w miejscu) + komórki 56 × 44, odstęp 4; komórki przewijają się w poziomie,
                // całość w pionie, za fokusem. Test JMGO tv2c: przewijanie w prawo chowało etykiety „Sezon N”.
                Row(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(Modifier.width(NiuniaSize.RatingLabelW), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(" ", style = NiuniaType.Caption, maxLines = 1)
                        bySeason.keys.forEach { season ->
                            Box(Modifier.height(NiuniaSize.H44), contentAlignment = Alignment.CenterStart) {
                                Text(stringResource(R.string.niunia_tv2_ratings_season, season), style = NiuniaType.LabelSmall,
                                    color = if (focused?.season == season) NiuniaColor.White else NiuniaColor.Text70, maxLines = 1)
                            }
                        }
                    }
                    Column(Modifier.horizontalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            (1..maxEp).forEach { e -> Text("E$e", style = NiuniaType.Caption, color = NiuniaColor.Text50, textAlign = TextAlign.Center, modifier = Modifier.width(NiuniaSize.RatingCellW), maxLines = 1) }
                        }
                        bySeason.forEach { (_, eps) ->
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                val byEp = eps.associateBy { it.episode ?: 0 }
                                (1..maxEp).forEach { e ->
                                    val v = byEp[e]
                                    if (v == null) Box(Modifier.size(NiuniaSize.RatingCellW, NiuniaSize.H44))
                                    else Cell(v, score(v), enabled = !best || (score(v) != null && threshold != null && score(v)!! >= threshold),
                                        requester = if (v === firstRated) first else null, onFocus = { if (it) focused = v }, onPlay = onPlay, onDetails = onDetails)
                                }
                            }
                        }
                    }
                }
                // Panel wskazanego odcinka (bez fokusu).
                Column(Modifier.width(260.dp).padding(top = NiuniaSpacing.Space20), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8)) {
                    val v = focused
                    if (v != null) {
                        NiuniaText(stringResource(R.string.niunia_tv2_ratings_episode, v.season ?: 0, v.episode ?: 0), variant = NiuniaTextVariant.Eyebrow, tone = NiuniaTone.Secondary)
                        NiuniaText(v.title, variant = NiuniaTextVariant.TitleSection, lines = 2)
                        score(v)?.let { NiuniaText("IMDb ${niuniaScoreText(it)}", variant = NiuniaTextVariant.Label, lines = 1) }
                        v.overview?.takeIf { it.isNotBlank() }?.let { NiuniaText(it, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 4) }
                        NiuniaText(stringResource(R.string.niunia_tv2_ratings_hint), variant = NiuniaTextVariant.Caption, tone = NiuniaTone.Tertiary, lines = 2)
                    }
                }
            }
        }
    }
}

/** Komórka 56 × 44, promień 6, tło w kolorze oceny; fokus: białe tło + obwódka 3 dp, bez skali. Bez oceny: „–” na `surface`. */
@Composable
private fun Cell(v: Video, score: Double?, enabled: Boolean, requester: FocusRequester?, onFocus: (Boolean) -> Unit, onPlay: (Video) -> Unit, onDetails: (Video) -> Unit) {
    var on by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    val bg = when {
        on -> NiuniaColor.Focus
        !enabled || score == null -> NiuniaColor.Surface
        else -> niuniaRatingColor(score)
    }
    Box(
        Modifier.size(NiuniaSize.RatingCellW, NiuniaSize.H44)
            .niuniaOk("S${v.season}E${v.episode}", requester, enabled, onFocus = { on = it; onFocus(it) },
                onLongClick = { Log.i(TAG, "NRatings: długie OK → szczegóły S${v.season}E${v.episode}"); onDetails(v) },
                onClick = { Log.i(TAG, "NRatings: OK → odtwórz S${v.season}E${v.episode}"); onPlay(v) })
            .background(bg, shape)
            .then(if (on) Modifier.border(NiuniaSize.Stroke3, NiuniaColor.White, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(score?.let { niuniaScoreText(it) } ?: "–", style = NiuniaType.Label,
            color = if (on || (enabled && score != null)) NiuniaColor.OnFocus else NiuniaColor.Text50, maxLines = 1)
    }
}
