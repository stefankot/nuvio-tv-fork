package com.nuvio.tv.skin.niunia.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.skin.niunia.components.NiuniaBlockKind
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaButtonKind
import com.nuvio.tv.skin.niunia.components.NiuniaContentBlock
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaPanelOverlay
import com.nuvio.tv.skin.niunia.components.NiuniaRowKind
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.components.NiuniaTrailing
import com.nuvio.tv.skin.niunia.components.niuniaTrapArrows
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.PostPlayMode

private const val TAG = "ForkNiunia"

/**
 * Z-14 (makiety PlayerLoading, PlayerError, PlayerSources, PlayerNext): nakładki odtwarzacza w stylu Niuni zamiast
 * nakładek bazy. Wpięcie: `PlayerScreen.kt` → [com.nuvio.tv.fork.ForkHooks.skinPlayerOverlay] → `Skin.PlayerOverlay`.
 * [kind]: "loading" · "error" · "sources" · "next". true = narysowane (baza wtedy nie rysuje swojej).
 * Argumenty z bazy w [args]: "visible", "onEvent", "exit", "focus", "mode", "filename".
 */
@Composable
internal fun niuniaPlayerOverlay(kind: String, uiState: PlayerUiState, args: Map<String, Any?>): Boolean {
    @Suppress("UNCHECKED_CAST")
    val onEvent = args["onEvent"] as? ((PlayerEvent) -> Unit) ?: return false
    @Suppress("UNCHECKED_CAST")
    val exit = args["exit"] as? (() -> Unit)
    // Test JMGO tv2c: w odtwarzaczu TV na żywo ekran wczytywania filmu nakładał się na nakładkę kanału — Live TV zostaje przy
    // własnych oknach (strażnik kanału, „… nie odpowiada”), bez nakładek filmu.
    if ((kind == "loading" || kind == "error") && com.nuvio.tv.reshaped.livetv.LiveTvPlaybackRegistry.isLiveTv(uiState.currentStreamUrl)) return false
    return when (kind) {
        "loading" -> { if (args["visible"] == true) NiuniaPlayerLoading(uiState, onEvent, exit); true }
        "error" -> { uiState.error?.let { NiuniaPlayerError(uiState, it, onEvent, exit) }; true }
        "sources" -> { NiuniaPlayerSources(uiState, onEvent, args["focus"] as? FocusRequester, args["filename"] as? String, exit); true }
        "next" -> {
            val mode = args["mode"] as? PostPlayMode.AutoPlay ?: return false // „Czy oglądasz dalej?” zostaje oknem bazy
            NiuniaPlayerNext(uiState, mode, onEvent, args["focus"] as? FocusRequester)
            true
        }
        else -> false
    }
}

/** PlayerLoading: logo, wskaźnik, „Buforuję… 42%”, etap, źródło; przyciski Inne źródło (fokus) · Anuluj. */
@Composable
private fun NiuniaPlayerLoading(uiState: PlayerUiState, onEvent: (PlayerEvent) -> Unit, exit: (() -> Unit)?) {
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        Log.i(TAG, "odtwarzacz: wczytywanie (ekran Niuni)")
        kotlinx.coroutines.delay(120)
        try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: fokus wczytywania: ${e.javaClass.simpleName}: ${e.message}") }
    }
    Box(Modifier.fillMaxSize().zIndex(2f).background(NiuniaColor.Bg)) {
        uiState.backdrop?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(NiuniaColor.Scrim))
        Column(Modifier.align(Alignment.Center).width(560.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
            if (uiState.logo != null) AsyncImage(model = uiState.logo, contentDescription = uiState.title, contentScale = ContentScale.Fit, modifier = Modifier.width(320.dp).heightIn(max = 110.dp))
            else NiuniaText(uiState.contentName ?: uiState.title, variant = NiuniaTextVariant.TitleScreen, lines = 2)
            androidx.compose.material3.CircularProgressIndicator(color = NiuniaColor.White, strokeWidth = 3.dp, modifier = Modifier.padding(top = NiuniaSpacing.Space8).size(32.dp))
            val pct = uiState.loadingProgress?.let { (it * 100).toInt().coerceIn(0, 100) }
            NiuniaText(if (pct != null) stringResource(R.string.niunia_pl1_buffering_pct, pct) else stringResource(R.string.niunia_pl1_buffering), variant = NiuniaTextVariant.TitleSection, lines = 1)
            uiState.loadingMessage?.takeIf { it.isNotBlank() }?.let { NiuniaText(it, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 2) }
            uiState.currentStreamName?.takeIf { it.isNotBlank() }?.let { NiuniaText(it.replace('\n', ' '), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 1) }
            Row(Modifier.padding(top = NiuniaSpacing.Space12).focusGroup(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
                NiuniaButton(onClick = {
                    Log.i(TAG, "odtwarzacz: wczytywanie → Inne źródło")
                    android.widget.Toast.makeText(ctx, com.nuvio.tv.fork.sources.TryAnotherSource.run(), android.widget.Toast.LENGTH_SHORT).show()
                }, label = stringResource(R.string.niunia_pl1_other_source), focusRequester = first)
                NiuniaButton(onClick = { Log.i(TAG, "odtwarzacz: wczytywanie → Anuluj"); exit?.invoke() }, label = stringResource(R.string.niunia_pl1_cancel))
            }
        }
    }
}

/** Przyczyna błędu prostymi słowami (z komunikatu odtwarzacza). */
internal fun niuniaErrorReason(ctx: android.content.Context, raw: String): String {
    val m = raw.lowercase()
    return ctx.getString(when {
        "403" in m || "expired" in m || "forbidden" in m -> R.string.niunia_pl1_err_expired
        "404" in m || "not found" in m || "410" in m -> R.string.niunia_pl1_err_gone
        "timeout" in m || "timed out" in m || "unable to connect" in m || "unknownhost" in m || "network" in m -> R.string.niunia_pl1_err_network
        "decoder" in m || "codec" in m || "unsupported" in m || "format" in m -> R.string.niunia_pl1_err_format
        else -> R.string.niunia_pl1_err_generic
    })
}

/** PlayerError: tytuł, przyczyna, źródło i kod; Inne źródło (fokus) · Spróbuj ponownie · (Odtwarzacz MPV) · Wróć. */
@Composable
private fun NiuniaPlayerError(uiState: PlayerUiState, message: String, onEvent: (PlayerEvent) -> Unit, exit: (() -> Unit)?) {
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(message) {
        Log.w(TAG, "odtwarzacz: błąd (ekran Niuni): ${message.take(160)}")
        kotlinx.coroutines.delay(120)
        try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: fokus okna błędu: ${e.javaClass.simpleName}: ${e.message}") }
    }
    Box(Modifier.fillMaxSize().zIndex(3f).background(NiuniaColor.Bg), contentAlignment = Alignment.Center) {
        uiState.backdrop?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(NiuniaColor.Scrim))
        NiuniaContentBlock(NiuniaBlockKind.Empty) {
            NiuniaText(stringResource(R.string.niunia_pl1_err_title), variant = NiuniaTextVariant.TitleSection, lines = 2)
            NiuniaText(niuniaErrorReason(ctx, message), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 3)
            NiuniaText(listOfNotNull(uiState.currentStreamName?.replace('\n', ' ')?.take(60), message.replace('\n', ' ').take(90)).joinToString(" · "),
                variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 2)
            Row(Modifier.padding(top = NiuniaSpacing.Space12).focusGroup(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
                NiuniaButton(onClick = {
                    Log.i(TAG, "odtwarzacz: błąd → Inne źródło")
                    android.widget.Toast.makeText(ctx, com.nuvio.tv.fork.sources.TryAnotherSource.run(), android.widget.Toast.LENGTH_SHORT).show()
                }, label = stringResource(R.string.niunia_pl1_other_source), compact = true, focusRequester = first)
                NiuniaButton(onClick = { Log.i(TAG, "odtwarzacz: błąd → Spróbuj ponownie"); onEvent(PlayerEvent.OnRetry) }, label = stringResource(R.string.niunia_tv1_error_retry), icon = "restart")
                if (uiState.showSwitchToMpvErrorAction) NiuniaButton(onClick = { Log.i(TAG, "odtwarzacz: błąd → odtwarzacz MPV"); onEvent(PlayerEvent.OnSwitchToMpvPlayer) },
                    label = stringResource(R.string.niunia_pl1_mpv))
                NiuniaButton(onClick = { Log.i(TAG, "odtwarzacz: błąd → Wróć"); exit?.invoke() }, label = stringResource(R.string.niunia_pl1_back))
            }
        }
    }
}

/**
 * PlayerSources: panel „Zmień źródło” po prawej (jak panele odtwarzacza): chipy dodatków z liczbą, lista źródeł
 * (obecne z `check` — po nazwie pliku albo adresie), OK = przełącz. Fokus startowy: obecne źródło, inaczej pierwsze.
 */
@Composable
private fun NiuniaPlayerSources(uiState: PlayerUiState, onEvent: (PlayerEvent) -> Unit, focus: FocusRequester?, filename: String?, exit: (() -> Unit)?) {
    val ctx = LocalContext.current
    val requester = focus ?: remember { FocusRequester() }
    val streams = uiState.sourceFilteredStreams
    fun current(s: Stream) = (s.url != null && s.url == uiState.currentStreamUrl) || (filename != null && s.behaviorHints?.filename == filename)
    val start = streams.indexOfFirst { current(it) }.coerceAtLeast(0)
    val list = rememberLazyListState()
    LaunchedEffect(streams.size, uiState.isLoadingSourceStreams) {
        Log.i(TAG, "odtwarzacz: Zmień źródło — ${streams.size} źródeł (wczytywanie=${uiState.isLoadingSourceStreams})")
        if (streams.isEmpty()) return@LaunchedEffect
        try { list.scrollToItem(start); kotlinx.coroutines.delay(80); requester.requestFocus() }
        catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: fokus listy źródeł: ${e.javaClass.simpleName}: ${e.message}") }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        NiuniaPanelOverlay(stringResource(R.string.niunia_pl1_sources_title),
            Modifier.niuniaTrapArrows("odtwarzacz: zmień źródło", setOf(FocusDirection.Left, FocusDirection.Right, FocusDirection.Up, FocusDirection.Down)).focusGroup()) {
            if (uiState.sourceAvailableAddons.size > 1) androidx.compose.foundation.lazy.LazyRow(Modifier.padding(bottom = NiuniaSpacing.Space12), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8)) {
                item {
                    NiuniaButton(onClick = { onEvent(PlayerEvent.OnSourceAddonFilterSelected(null)) }, label = stringResource(R.string.niunia_pl1_all_count, uiState.sourceAllStreams.size),
                        kind = NiuniaButtonKind.Chip, selected = uiState.sourceSelectedAddonFilter == null)
                }
                itemsIndexed(uiState.sourceAvailableAddons) { _, a ->
                    NiuniaButton(onClick = { Log.i(TAG, "odtwarzacz: źródła, dodatek $a"); onEvent(PlayerEvent.OnSourceAddonFilterSelected(a)) },
                        label = "$a · ${uiState.sourceAllStreams.count { it.addonName == a }}", kind = NiuniaButtonKind.Chip, selected = uiState.sourceSelectedAddonFilter == a)
                }
            }
            when {
                streams.isEmpty() && uiState.isLoadingSourceStreams -> NiuniaListRow(stringResource(R.string.niunia_pl1_sources_loading), onClick = null, enabled = false)
                streams.isEmpty() -> {
                    NiuniaListRow(uiState.sourceStreamsError ?: stringResource(R.string.niunia_pl1_sources_none), onClick = null, enabled = false)
                    NiuniaListRow(stringResource(R.string.niunia_tv1_error_retry), icon = "restart", focusRequester = requester, onClick = { onEvent(PlayerEvent.OnReloadSourceStreams) })
                }
                else -> LazyColumn(Modifier.heightIn(max = 400.dp), state = list) {
                    itemsIndexed(streams, key = { i, s -> "${i}_${s.url ?: s.getDisplayName()}" }) { i, s ->
                        val now = current(s)
                        NiuniaListRow(
                            com.nuvio.tv.skin.niunia.stream.niuniaStreamLabel(s, ctx), kind = NiuniaRowKind.Track,
                            supporting = listOf(com.nuvio.tv.skin.niunia.stream.niuniaStreamSupporting(s), if (now) ctx.getString(R.string.niunia_pl1_playing_now) else null)
                                .filter { !it.isNullOrBlank() }.joinToString(" · ").ifEmpty { null },
                            trailing = if (now) NiuniaTrailing.Check else NiuniaTrailing.None,
                            focusRequester = if (i == start) requester else null,
                            onClick = { Log.i(TAG, "odtwarzacz: źródło → ${s.getDisplayName().take(60)}"); onEvent(PlayerEvent.OnSourceStreamSelected(s)) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * PlayerNext (koniec odcinka): karta po prawej u dołu — „Następny odcinek za 10 s”, kadr, „S1 E3 · tytuł”, opis (2 linie),
 * Odtwórz następny (fokus, [focus] z bazy) · Oglądaj napisy końcowe (zamyka kartę). Odcinek przed premierą: komunikat bazy.
 */
@Composable
private fun NiuniaPlayerNext(uiState: PlayerUiState, mode: PostPlayMode.AutoPlay, onEvent: (PlayerEvent) -> Unit, focus: FocusRequester?) {
    val ep = mode.nextEpisode
    val requester = focus ?: remember { FocusRequester() }
    LaunchedEffect(ep.videoId) { Log.i(TAG, "odtwarzacz: następny odcinek S${ep.season} E${ep.episode} (karta Niuni, odliczanie ${mode.countdownSec})") }
    Box(Modifier.fillMaxSize().zIndex(2.1f), contentAlignment = Alignment.BottomEnd) {
        val shape = RoundedCornerShape(NiuniaRadius.Radius28)
        Column(
            with(com.nuvio.tv.fork.glass.ForkGlass) { Modifier.padding(end = NiuniaSpacing.Space40, bottom = if (uiState.showControls) 122.dp else 30.dp).width(420.dp)
                .forkGlass(com.nuvio.tv.fork.glass.ForkGlass.Role.Panel, shape, fallback = NiuniaColor.Panel, fallbackBorder = NiuniaColor.Fill12) }
                .padding(NiuniaSpacing.Space20).focusGroup(),
            verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space8),
        ) {
            val eyebrow = when {
                mode.searching -> stringResource(R.string.niunia_pl1_next_searching)
                mode.countdownSec != null -> stringResource(R.string.niunia_pl1_next_in, mode.countdownSec)
                else -> stringResource(R.string.niunia_pl1_next)
            }
            NiuniaText(eyebrow, variant = NiuniaTextVariant.Eyebrow, tone = NiuniaTone.Secondary)
            if (ep.thumbnail != null) AsyncImage(model = ep.thumbnail, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp).background(NiuniaColor.Surface, RoundedCornerShape(NiuniaRadius.Radius16)))
            NiuniaText("S${ep.season} E${ep.episode} · ${ep.title}", variant = NiuniaTextVariant.Label, lines = 1)
            if (!ep.hasAired) NiuniaText(ep.unairedMessage ?: stringResource(R.string.niunia_pl1_next_unaired), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 2)
            else ep.overview?.takeIf { it.isNotBlank() }?.let { NiuniaText(it, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 2) }
            Row(Modifier.padding(top = NiuniaSpacing.Space8), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
                if (ep.hasAired) NiuniaButton(onClick = { Log.i(TAG, "odtwarzacz: Odtwórz następny"); onEvent(PlayerEvent.OnPlayNextEpisode) },
                    label = stringResource(R.string.niunia_pl1_next_play), icon = "play", focusRequester = requester)
                NiuniaButton(onClick = { Log.i(TAG, "odtwarzacz: napisy końcowe (karta zamknięta)"); onEvent(PlayerEvent.OnDismissNextEpisodeCard) },
                    label = stringResource(R.string.niunia_pl1_next_credits), focusRequester = if (!ep.hasAired) requester else null)
            }
        }
    }
}
