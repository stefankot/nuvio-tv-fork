package com.nuvio.tv.skin.niunia.home

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.focusable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.skin.niunia.components.NiuniaBlockKind
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaContentBlock
import com.nuvio.tv.skin.niunia.components.NiuniaLogo
import com.nuvio.tv.skin.niunia.components.NiuniaLogoSize
import com.nuvio.tv.skin.niunia.components.NiuniaMetaLine
import com.nuvio.tv.skin.niunia.components.NiuniaTag
import com.nuvio.tv.skin.niunia.components.NiuniaTagKind
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaOpacity
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import kotlinx.coroutines.launch

private const val TAG = "ForkNiunia"

/**
 * Z-12 (makieta Pager, `NCollection kind="pager"` + `NHero frame="card"`): OK na plakacie w rzędzie Głównej otwiera karty
 * tytułów w kolejności rzędu, na karcie klikniętego plakatu. Karta 840 × 520 (`pager-card-w`), 20 dp od góry, sąsiednie
 * wystają po 40 dp (przygaszone). W karcie: chip „#N w …” (rzędy rankingowe), logo, gatunki + średnia, opis (3 linie),
 * data · czas, przyciski Odtwórz (fokus) · + · (i). ◄ z pierwszego przycisku / ► z ostatniego przesuwa karuzelę o kartę
 * (bez zawijania) i stawia fokus na Odtwórz nowej karty. (i) = strona tytułu, Odtwórz = strona tytułu z odtwarzaniem
 * od razu (`playOnLoad`), Wstecz = rząd (fokus na tym samym plakacie). Dane: tylko z rzędu (bez dodatkowych zapytań).
 * Android 11: karta bez rozmycia, przyciemnienie jak `scrim-detail-flat`.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NiuniaTitlePager(
    items: List<MetaPreview>,
    start: Int,
    rowName: String,
    ranking: Boolean,
    inLibrary: (MetaPreview) -> Boolean,
    onPlay: (MetaPreview) -> Unit,
    onDetails: (MetaPreview) -> Unit,
    onLibrary: (MetaPreview) -> Unit,
    onClose: (Int) -> Unit,
) {
    val pager = rememberPagerState(initialPage = start.coerceIn(0, (items.size - 1).coerceAtLeast(0))) { items.size }
    val scope = rememberCoroutineScope()
    val plays = remember(items.size) { List(items.size) { FocusRequester() } }
    // UX 25.11 (test uxr6): w czasie przewijania przyciski starej karty się wyłączały i fokus spadał na plakat Głównej pod
    // kartą — po Wstecz zaznaczenie wracało na złą kartę. Na czas przewijania fokus trzyma niewidoczny punkt w karcie.
    val holder = remember { FocusRequester() }
    BackHandler { Log.i(TAG, "karta tytułu: Wstecz → rząd (karta ${pager.currentPage + 1})"); onClose(pager.currentPage) }
    fun go(to: Int) {
        if (to !in items.indices) { Log.i(TAG, "karta tytułu: krawędź karuzeli (${pager.currentPage + 1}/${items.size})"); return }
        val from = pager.currentPage
        try { holder.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "karta tytułu: fokus w czasie przewijania: ${e.javaClass.simpleName}: ${e.message}") }
        scope.launch {
            pager.animateScrollToPage(to)
            Log.i(TAG, "[Nuvio:NCollection] pager: card ${from + 1} -> ${to + 1} / ${items.size} (${items[to].name})")
            try { plays[to].requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "karta tytułu: fokus Odtwórz: ${e.javaClass.simpleName}: ${e.message}") }
        }
    }
    LaunchedEffect(Unit) {
        Log.i(TAG, "karta tytułu: rząd „$rowName”, karta ${pager.currentPage + 1}/${items.size}")
        kotlinx.coroutines.delay(120)
        try { plays[pager.currentPage].requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "karta tytułu: fokus startowy: ${e.javaClass.simpleName}: ${e.message}") }
    }
    // ▲▼ nic nie robią (fokus nie wychodzi pod kartę na Główną).
    Box(Modifier.fillMaxSize().zIndex(6f).background(NiuniaColor.Bg).onPreviewKeyEvent { e -> e.type == KeyEventType.KeyDown && (e.key == Key.DirectionUp || e.key == Key.DirectionDown) }) {
        Box(Modifier.size(1.dp).focusRequester(holder).focusable())
        HorizontalPager(
            state = pager, pageSize = PageSize.Fixed(NiuniaSize.PagerCardW), pageSpacing = NiuniaSpacing.Space20,
            contentPadding = PaddingValues(horizontal = 60.dp), userScrollEnabled = false, beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize().padding(top = NiuniaSpacing.Space20),
        ) { page ->
            val item = items[page]
            val active = page == pager.currentPage
            Card(item, page, ranking, rowName, active, inLibrary(item), plays[page],
                onLeft = { go(page - 1) }, onRight = { go(page + 1) },
                onPlay = { onPlay(item) }, onDetails = { onDetails(item) }, onLibrary = { onLibrary(item) })
        }
    }
}

@Composable
private fun Card(
    item: MetaPreview, index: Int, ranking: Boolean, rowName: String, active: Boolean, inLibrary: Boolean, play: FocusRequester,
    onLeft: () -> Unit, onRight: () -> Unit, onPlay: () -> Unit, onDetails: () -> Unit, onLibrary: () -> Unit,
) {
    val shape = RoundedCornerShape(topStart = NiuniaRadius.Radius28, topEnd = NiuniaRadius.Radius28)
    Box(Modifier.size(NiuniaSize.PagerCardW, 520.dp).clip(shape).background(NiuniaColor.Surface).then(if (active) Modifier else Modifier.alpha(NiuniaOpacity.OpacityDisabled))) {
        AsyncImage(model = item.background ?: item.landscapePoster ?: item.poster, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        // scrim-detail (od lewej) + scrim-detail-bottom; na Androidzie 11 pełniejsze tło od lewej (scrim-detail-flat).
        val flat = android.os.Build.VERSION.SDK_INT < 31
        val left = if (flat) NiuniaColor.Bg else NiuniaColor.Bg.copy(alpha = 0.85f)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to left, (if (flat) 0.45f else 0.35f) to left.copy(alpha = if (flat) 0.92f else 0.6f), 0.7f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to NiuniaColor.Bg.copy(alpha = 0.92f))))
        Column(Modifier.align(Alignment.BottomStart).padding(NiuniaSpacing.Space20).padding(bottom = NiuniaSpacing.Space20), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space16)) {
            NiuniaContentBlock(NiuniaBlockKind.Info) {
                if (ranking) NiuniaTag(stringResource(R.string.niunia_tv2_pager_rank, index + 1, rowName), NiuniaTagKind.Chip)
                NiuniaLogo(item.logo, item.name, NiuniaLogoSize.Hero)
                NiuniaMetaLine(item.genres.take(3).mapNotNull { com.nuvio.tv.skin.niunia.components.NiuniaGenres.pl(it) }.distinct().joinToString(" · ").ifBlank { null }) {
                    com.nuvio.tv.skin.niunia.components.NiuniaRatingAverage(com.nuvio.tv.skin.niunia.components.niuniaScores(item.imdbRating, null))
                }
                item.description?.takeIf { it.isNotBlank() }?.let { NiuniaText(it, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 3) }
                // UX JMGO (ręczne testy 24.11): „1h47min” z dodatku → „1 godz. 47 min” jak na banerze i stronie tytułu.
                val dateLine = listOfNotNull(item.releaseInfo?.takeIf { it.isNotBlank() },
                    com.nuvio.tv.skin.niunia.detail.niuniaRuntimeLabel(androidx.compose.ui.platform.LocalContext.current, item.runtime)).joinToString(" · ")
                if (dateLine.isNotBlank()) NiuniaText(dateLine, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 1)
            }
            // ◄ z Odtwórz / ► z (i) = sąsiednia karta; przyciski kart nieaktywnych bez fokusu.
            Row(
                Modifier.focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12), verticalAlignment = Alignment.CenterVertically,
            ) {
                NiuniaButton(onClick = onPlay, label = stringResource(R.string.niunia_detail_play), icon = "play", focusRequester = play, haze = true, enabled = active,
                    modifier = Modifier.onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionLeft) { onLeft(); true } else false })
                NiuniaButton(onClick = onLibrary, icon = if (inLibrary) "check" else "plus", haze = true, enabled = active,
                    description = stringResource(if (inLibrary) R.string.niunia_pinfo_in_library else R.string.niunia_ds_add_library))
                NiuniaButton(onClick = onDetails, icon = "about", haze = true, enabled = active,
                    description = stringResource(if (item.rawType == "series") R.string.niunia_ds_about_series else R.string.niunia_ds_about_movie),
                    modifier = Modifier.onPreviewKeyEvent { e -> if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight) { onRight(); true } else false })
            }
        }
    }
}
