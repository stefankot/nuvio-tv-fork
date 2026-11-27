package com.nuvio.tv.skin.niunia.player

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.core.streams.AudioLanguage
import com.nuvio.tv.core.streams.AudioMenuOption
import com.nuvio.tv.core.streams.StreamQuality
import com.nuvio.tv.core.streams.StreamQualityMenu
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaPanelOverlay
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.components.NiuniaTrailing
import com.nuvio.tv.skin.niunia.components.niuniaPanelTrap
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.TrackInfo

/*
 * Przegląd ekranów 27.11, ekrany 05 i 06 (plan/ux/przeglad-ekranow/NOTATKA.md):
 *  - „Dźwięk” = prosta lista ścieżek pliku (język + krótki opis, ✓ bieżąca), „Polski” z innego wydania, gdy pliku brak polskiej
 *    ścieżki (to samo zdarzenie co wiersz „Polski” w menu Echo → Dublagem, potem przełączenie źródła), reszta ustawień zwinięta
 *    w wierszu „Więcej ustawień dźwięku” (otwiera dotychczasowe okno z opóźnieniem, wzmocnieniem, trybem nocnym).
 *  - „Jakość” = panel Niuni zamiast okna Echo (te same zdarzenia: OnQualitySelected / OnAutoQualitySelected), po polsku;
 *    długie OK na jakości = lista źródeł w tej jakości, OK na źródle = przełącz (OnSourceStreamSelected).
 * Wyłącznik: klucz ForkFeatures `niunia_player_panels` (wyłączony = stare wiersze i okna Echo).
 */
private const val TAG = "ForkNiunia"
internal const val NIUNIA_PANELS_KEY = "niunia_player_panels"

internal fun niuniaPanelsOn(ctx: android.content.Context): Boolean =
    com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(ctx, NIUNIA_PANELS_KEY, true)

private fun TrackInfo.isPl(): Boolean = com.nuvio.tv.fork.subs.PolishFirst.isPl(language) ||
    Regex("""\b(pol|polski|polish|lektor|dubbing pl)\b""", RegexOption.IGNORE_CASE).containsMatchIn(name)

/** Krótki opis ścieżki: rodzaj (lektor / dubbing / komentarz) i układ kanałów. */
@Composable
private fun trackDetail(t: TrackInfo): String? {
    val n = t.name.lowercase()
    val kind = when {
        "lektor" in n || "voice" in n || "voiceover" in n -> stringResource(R.string.niunia_qa_kind_lektor)
        "dub" in n -> stringResource(R.string.niunia_qa_kind_dub)
        "comment" in n || "komentarz" in n -> stringResource(R.string.niunia_qa_kind_comment)
        else -> null
    }
    val ch = when (t.channelCount) { 1 -> "mono"; 2 -> "stereo"; 6 -> "5.1"; 8 -> "7.1"; else -> null }
    return listOfNotNull(kind, ch).joinToString(" · ").ifEmpty { null }
}

/**
 * Zakładka „Dźwięk” panelu „Audio, lektor i napisy”: ścieżki pliku (polskie na górze), „Polski” z innego wydania,
 * „Więcej ustawień dźwięku” (dotychczasowe okno), „Więcej opcji”. Fokus startowy na bieżącej ścieżce.
 */
@Composable
internal fun NiuniaAudioTracksTab(
    uiState: PlayerUiState,
    first: FocusRequester,
    onEvent: (PlayerEvent) -> Unit,
    close: () -> Unit,
    openMore: () -> Unit,
) {
    val tracks = uiState.audioTracks
    val isSel: (TrackInfo) -> Boolean = { it.isSelected || it.index == uiState.selectedAudioTrackIndex }
    val ordered = remember(tracks) { tracks.filter { it.isPl() } + tracks.filterNot { it.isPl() } }
    val hasPl = ordered.any { it.isPl() }
    val live = com.nuvio.tv.reshaped.livetv.LiveTvPlaybackRegistry.isLiveTv(uiState.currentStreamUrl)
    val offerPl = !hasPl && !live && tracks.isNotEmpty()
    val focusIndex = ordered.firstOrNull(isSel)?.index ?: ordered.firstOrNull()?.index
    LaunchedEffect(tracks.size) {
        Log.i(TAG, "odtwarzacz: Dźwięk — ścieżek ${tracks.size} (polskich ${ordered.count { it.isPl() }}), bieżąca ${ordered.firstOrNull(isSel)?.let { "${it.index}:${it.language}" } ?: "?"}, Polski z innego wydania=${offerPl}")
        // „Polski” z innego wydania szuka wśród źródeł tego filmu — wczytaj je z wyprzedzeniem (bez okna).
        if (offerPl && uiState.sourceAllStreams.isEmpty() && !uiState.isLoadingSourceStreams) {
            Log.i(TAG, "odtwarzacz: Dźwięk — wczytuję źródła dla „Polski”"); onEvent(PlayerEvent.OnReloadSourceStreams)
        }
    }
    // Test JMGO prz04: Dublagem dołączył polską ścieżkę przy otwartym panelu — lista się przebudowała („Polski z innego wydania”
    // zniknął) i fokus przepadł (▲▼ nic nie zaznaczały). Wiersze mają stałe klucze, a gdy fokus zniknie — wraca na bieżącą ścieżkę.
    var focusedRow by remember { mutableStateOf<String?>(null) }
    val rowsKey = ordered.joinToString(",") { "${it.index}:${isSel(it)}" } + "|$offerPl"
    LaunchedEffect(rowsKey) {
        kotlinx.coroutines.delay(150)
        // Wiersz z fokusem usunięty z listy (np. „Polski z innego wydania”) nie zawsze zgłasza utratę fokusu — sprawdzamy, czy jeszcze jest.
        val alive = ordered.map { "t${it.index}" } + listOfNotNull("pl".takeIf { offerPl }) + listOf("more", "opts")
        if (focusedRow != null && focusedRow in alive) return@LaunchedEffect
        try { first.requestFocus(); Log.i(TAG, "odtwarzacz: Dźwięk — lista ścieżek zmieniona, fokus wraca na bieżącą") }
        catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: Dźwięk — fokus po zmianie listy: ${e.javaClass.simpleName}: ${e.message}") }
    }
    fun track(key: String): (Boolean) -> Unit = { on -> if (on) focusedRow = key else if (focusedRow == key) focusedRow = null }
    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
        ordered.forEach { t -> androidx.compose.runtime.key(t.index) {
            val sel = isSel(t)
            val lang = t.language?.takeIf { it.isNotBlank() && !it.equals("und", true) }?.let { niuniaLangName(it) }
            NiuniaListRow(
                lang ?: t.name.ifBlank { stringResource(R.string.niunia_qa_audio_unknown) },
                supporting = trackDetail(t),
                trailing = if (sel) NiuniaTrailing.Check else NiuniaTrailing.None,
                focusRequester = if (t.index == focusIndex) first else null,
                onFocus = track("t${t.index}"),
                onClick = {
                    Log.i(TAG, "odtwarzacz: Dźwięk → ścieżka ${t.index} (${t.language})")
                    onEvent(PlayerEvent.OnSelectAudioTrack(t.index))
                },
            )
        } }
        if (offerPl) NiuniaListRow(
            stringResource(R.string.niunia_qa_audio_pl),
            supporting = stringResource(R.string.niunia_qa_audio_pl_other),
            onFocus = track("pl"),
            onClick = {
                Log.i(TAG, "odtwarzacz: Dźwięk → Polski z innego wydania (źródeł ${uiState.sourceAllStreams.size})")
                close()
                onEvent(PlayerEvent.OnAudioMenuLanguageSelected(AudioMenuOption(language = AudioLanguage.fromCode("pl"), isOriginal = false, sourceCount = 1)))
            },
        )
        NiuniaListRow(
            stringResource(R.string.niunia_qa_audio_more), trailing = NiuniaTrailing.Chevron,
            focusRequester = if (focusIndex == null) first else null,
            onFocus = track("more"),
            onClick = { Log.i(TAG, "odtwarzacz: Dźwięk → Więcej ustawień dźwięku"); close(); onEvent(PlayerEvent.OnShowAudioOverlay) },
        )
        NiuniaListRow(stringResource(R.string.niunia_ds_more_options), onClick = openMore, trailing = NiuniaTrailing.Chevron, onFocus = track("opts"))
    }
}

private fun StreamQuality.descRes(): Int = when (this) {
    StreamQuality.P2160 -> R.string.niunia_qa_desc_2160
    StreamQuality.P1440 -> R.string.niunia_qa_desc_1440
    StreamQuality.P1080 -> R.string.niunia_qa_desc_1080
    StreamQuality.P720 -> R.string.niunia_qa_desc_720
    StreamQuality.P480, StreamQuality.P360 -> R.string.niunia_qa_desc_sd
    StreamQuality.P240, StreamQuality.P144 -> R.string.niunia_qa_desc_low
}

/**
 * Panel „Jakość” w stylu Niuni: Automatycznie (gdy włączone), jakości od najlepszej (✓ bieżąca, „Teraz” / „Źródła: N”).
 * OK = przełącz na tę jakość od tego samego miejsca; długie OK = lista źródeł tej jakości (OK = przełącz na to źródło).
 */
@Composable
internal fun NiuniaQualityPanel(uiState: PlayerUiState, onEvent: (PlayerEvent) -> Unit, close: () -> Unit) {
    val ctx = LocalContext.current
    val info = uiState.streamInfoData
    // Diagnostyka 27.11: currentStreamQuality potrafiło podać 4K dla pliku 1080p — najpierw rozmiar obrazu z dekodera.
    // Test JMGO prz04: bez okna Echo `currentStreamQuality` jest puste, a rozmiar obrazu bywa nieznany — wtedy z nazwy źródła („Comet 1080p”).
    val current = remember(info?.videoWidth, info?.videoHeight, uiState.currentStreamQuality, uiState.currentStreamName) {
        val w = info?.videoWidth; val h = info?.videoHeight
        (if (w != null && h != null) StreamQuality.fromVideoSize(w, h) else null) ?: uiState.currentStreamQuality
            ?: com.nuvio.tv.core.streams.StreamQualityParser.parse(uiState.currentStreamName, info?.filename)
    }
    val streams = uiState.sourceAllStreams
    val options = remember(streams, current) { StreamQualityMenu.buildOptions(streams, current) }
    val autoActive = uiState.autoQualityEnabled && uiState.autoQualityActive
    var pick by remember { mutableStateOf<StreamQuality?>(null) }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        Log.i(TAG, "odtwarzacz: Jakość (Niunia) — źródeł ${streams.size}, teraz ${current?.label ?: "?"}, auto=${uiState.autoQualityEnabled}/${uiState.autoQualityActive}")
        if (streams.isEmpty() && !uiState.isLoadingSourceStreams) { Log.i(TAG, "odtwarzacz: Jakość — wczytuję źródła"); onEvent(PlayerEvent.OnReloadSourceStreams) }
    }
    LaunchedEffect(pick, options.isEmpty(), uiState.isLoadingSourceStreams) {
        for (wait in longArrayOf(80, 200, 400)) {
            kotlinx.coroutines.delay(wait)
            try { first.requestFocus(); break } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz: Jakość, fokus (${wait} ms): ${e.javaClass.simpleName}: ${e.message}") }
        }
    }
    val q = pick
    NiuniaPanelOverlay(
        if (q == null) stringResource(R.string.niunia_qa_quality_title) else stringResource(R.string.niunia_qa_pick_title, q.label),
        Modifier.niuniaPanelTrap("odtwarzacz: jakość"),
    ) {
        if (q == null) {
            NiuniaText(stringResource(R.string.niunia_qa_quality_hint), Modifier.padding(start = NiuniaSpacing.Space12, end = NiuniaSpacing.Space12, bottom = NiuniaSpacing.Space8),
                variant = NiuniaTextVariant.Caption, tone = NiuniaTone.Secondary)
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                if (uiState.autoQualityEnabled) NiuniaListRow(
                    stringResource(R.string.niunia_qa_auto),
                    supporting = if (autoActive) current?.label else stringResource(R.string.niunia_qa_auto_desc),
                    trailing = if (autoActive) NiuniaTrailing.Check else NiuniaTrailing.None,
                    focusRequester = if (autoActive) first else null,
                    onClick = { Log.i(TAG, "odtwarzacz: Jakość → Automatycznie"); onEvent(PlayerEvent.OnAutoQualitySelected); close() },
                )
                val focusQ = if (autoActive) null else options.firstOrNull { it.isCurrent }?.quality ?: options.firstOrNull()?.quality
                options.forEach { o ->
                    NiuniaListRow(
                        o.quality.label,
                        supporting = stringResource(o.quality.descRes()),
                        value = if (o.isCurrent) stringResource(R.string.niunia_qa_playing) else stringResource(R.string.niunia_qa_sources, o.sourceCount),
                        trailing = if (o.isCurrent && !autoActive) NiuniaTrailing.Check else NiuniaTrailing.None,
                        focusRequester = if (o.quality == focusQ) first else null,
                        onClick = {
                            if (o.isCurrent && !autoActive) { Log.i(TAG, "odtwarzacz: Jakość → ${o.quality.label} (już gra)"); close() }
                            else { Log.i(TAG, "odtwarzacz: Jakość → ${o.quality.label} (${o.sourceCount} źródeł)"); onEvent(PlayerEvent.OnQualitySelected(o.quality)); close() }
                        },
                        onLongClick = { Log.i(TAG, "odtwarzacz: Jakość — długie OK → źródła ${o.quality.label}"); pick = o.quality },
                    )
                }
                if (options.isEmpty()) {
                    if (uiState.isLoadingSourceStreams) NiuniaListRow(stringResource(R.string.niunia_qa_loading), onClick = null, enabled = false)
                    else NiuniaListRow(stringResource(R.string.niunia_qa_none), onClick = null, enabled = false)
                    NiuniaListRow(stringResource(R.string.niunia_tv1_error_retry), icon = "restart", focusRequester = if (!uiState.autoQualityEnabled) first else null,
                        onClick = { Log.i(TAG, "odtwarzacz: Jakość → wczytaj źródła ponownie"); onEvent(PlayerEvent.OnReloadSourceStreams) })
                }
            }
        } else {
            val list = remember(streams, q) { StreamQualityMenu.rankForQuality(streams, q, uiState.currentStreamAddonName) }
            NiuniaListRow(stringResource(R.string.niunia_qa_back), icon = "chevron-left", focusRequester = if (list.isEmpty()) first else null,
                onClick = { Log.i(TAG, "odtwarzacz: Jakość — powrót z listy źródeł"); pick = null })
            if (list.isEmpty()) NiuniaListRow(stringResource(R.string.niunia_qa_none_q), onClick = null, enabled = false)
            else LazyColumn(Modifier.heightIn(max = 400.dp)) {
                itemsIndexed(list, key = { i, s -> "${i}_${s.url ?: s.getDisplayName()}" }) { i, s ->
                    // Adres grającego pliku bywa już rozwiązany (inny niż w liście) — wtedy po nazwie i dodatku.
                    val now = (s.url != null && s.url == uiState.currentStreamUrl) ||
                        (s.name != null && s.name == uiState.currentStreamName && s.addonName == uiState.currentStreamAddonName)
                    NiuniaListRow(
                        com.nuvio.tv.skin.niunia.stream.niuniaStreamLabel(s, ctx),
                        supporting = com.nuvio.tv.skin.niunia.stream.niuniaStreamSupporting(s).ifEmpty { null },
                        trailing = if (now) NiuniaTrailing.Check else NiuniaTrailing.None,
                        focusRequester = if (i == 0) first else null,
                        onClick = {
                            Log.i(TAG, "odtwarzacz: Jakość ${q.label} → źródło ${i + 1}/${list.size} (${s.addonName})")
                            onEvent(PlayerEvent.OnSourceStreamSelected(s)); close()
                        },
                    )
                }
            }
        }
    }
}
