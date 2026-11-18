package com.nuvio.tv.skin.kicia.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.nuvio.tv.core.streams.smart.SmartPreset
import com.nuvio.tv.skin.kicia.components.ForgeBadge
import com.nuvio.tv.skin.kicia.components.ForgeBadgeKind
import com.nuvio.tv.skin.kicia.components.ForgeButton
import com.nuvio.tv.skin.kicia.components.ForgeSlider
import com.nuvio.tv.skin.kicia.components.ForgeToast
import com.nuvio.tv.skin.kicia.components.forgeEdge
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerViewModel

/** Makieta 7.7 B1b: a long OK on a level asks PlayerScreen for the source list of that level (read there when the panel opens). */
object ForgeSourcePopoverBridge {
    var pendingListPreset: SmartPreset? = null
}

/**
 * Makieta 7.7 B1/B1b/B2 + 7.7b H1: the „Źródło i jakość” bubble over the button (short OK on the button; a long OK keeps
 * opening the 7.6 panel). At the top the playing file, not focusable: „Teraz gra” 12/70% and a row of Plakietki
 * (resolution, HDR, bitrate, codec, language). Then „Złe źródło – zmień” (start focus; OK = the next source not marked bad,
 * same position; long OK = the panel) and the quality Suwak (◀▶ selects, OK = a new source of that level, long OK = the
 * list of that level).
 */
@Composable
fun ForgeSourcePopover(
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    onOpenPanel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preset by viewModel.smartPreset.collectAsState(initial = SmartPreset.BALANCED)
    val levels = forgePresetLabels
    var selected by remember(preset) { mutableIntStateOf(levels.indexOfFirst { it.first == preset }.coerceAtLeast(0)) }
    val badFocus = remember { FocusRequester() }
    val sliderFocus = remember { FocusRequester() }
    var onSlider by remember { mutableStateOf(false) }
    val guardScope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(Unit) { runCatching { badFocus.requestFocus() } }
    BackHandler(onBack = onClose)
    val playing = viewModel.controller.let { it.forgeSelectedStream ?: it.smartTrySession?.current }
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.width(360.dp).clip(shape).background(ForgeColors.GlassFallback).forgeEdge(shape).padding(16.dp)
            // test 15.11 (JMGO): ▼ from the button left the bubble and it vanished — the focus stays inside it
            .focusProperties { @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class) exit = { FocusRequester.Cancel } }
            // test 15.11 (B-2): should the focus fall out of the open bubble, it goes back to the element that had it
            .onFocusChanged { st -> if (!st.hasFocus) guardScope.launch { kotlinx.coroutines.delay(30); runCatching { (if (onSlider) sliderFocus else badFocus).requestFocus() } } }
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (playing != null) {
            val b = com.nuvio.tv.skin.kicia.detail.forgeFileBadgesFor(playing)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Teraz gra", style = ForgeText.caption.copy(color = ForgeColors.Text2))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOfNotNull(b.resolution, b.hdr, b.bitrate, b.codec, b.language ?: "angielski").forEach { ForgeBadge(it, ForgeBadgeKind.Quality) }
                }
            }
        }
        ForgeButton(
            text = "Złe źródło – zmień", icon = Icons.Filled.Refresh, small = true, focusRequester = badFocus,
            modifier = Modifier.focusProperties { down = sliderFocus },
            onLongOk = { onClose(); onOpenPanel() },
            onOk = { onClose(); viewModel.onEvent(PlayerEvent.OnTryAnotherSource) },
        )
        // test 15.11 (JMGO): OK on the Suwak never arrived through an onPreviewKeyEvent here — the Forge OK handling instead
        ForgeSlider(
            levels = levels.map { it.second }, selected = selected, onSelect = { selected = it }, width = 300.dp,
            focusRequester = sliderFocus,
            modifier = Modifier.onFocusChanged { onSlider = it.hasFocus },
            onLongOk = { onClose(); ForgeSourcePopoverBridge.pendingListPreset = levels[selected].first; onOpenPanel() },
            onOk = {
                val level = levels[selected].first
                onClose()
                ForgeToast.show(if (level == preset) "Wczytuję to źródło od nowa…" else "Szukam źródła: " + levels[selected].second + "…")
                if (level == preset) viewModel.onEvent(PlayerEvent.OnTryAnotherSource) else viewModel.setSmartPreset(level)
            },
        )
    }
}
