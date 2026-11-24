package com.nuvio.tv.skin.niunia.live

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.nuvio.tv.R
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.skin.kicia.livetv.ForgeChannelFailed
import com.nuvio.tv.skin.niunia.components.NiuniaBlockKind
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaContentBlock
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.ui.reshaped.livetv.LiveTvPlayerState

private const val TAG = "ForkNiunia"

/**
 * Z-14 (makieta `PlayerLiveError`): zamiast okna bazy „Kanał nie odpowiada” — ekran na środku: „TVN nie odpowiada”,
 * przyczyna, przyciski Następny kanał (fokus) · Spróbuj ponownie · Lista kanałów. Klawisze obsługuje
 * [ForgeChannelFailed.onKey] (odtwarzacz widzi je pierwszy): ◄► wybierają przycisk, fokus ekranu idzie za wyborem,
 * OK uruchamia, Wstecz zamyka (gdy kanał jednak gra) albo wraca do listy. Okno znika samo, gdy obraz ruszy.
 * Bez zdań „sprawdziłem N strumieni” i „pominę go przez 24 godziny” z makiety — fork tego dziś nie robi.
 */
@Composable
internal fun NiuniaPlayerLiveError(state: LiveTvPlayerState, channel: LiveTvChannel) {
    val name = com.nuvio.tv.fork.tv.names.ChannelNames.shown(channel.name)
    val buttons = remember { ForgeChannelFailed.buttons() }
    val requesters = remember { buttons.map { FocusRequester() } }
    val selected = ForgeChannelFailed.selected.intValue.coerceIn(0, buttons.lastIndex)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(channel) {
        Log.i(TAG, "odtwarzacz TV: „$name nie odpowiada” (ekran Niuni)")
        com.nuvio.tv.skin.niunia.livetv.NiuniaLiveStatus.failed(ctx, channel.streamUrl, name) // Z-21 (P-28): „Niedostępny” w przewodniku na 24 h
    }
    LaunchedEffect(selected) {
        kotlinx.coroutines.delay(60)
        try { requesters[selected].requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "odtwarzacz TV: fokus przycisku okna błędu: ${e.javaClass.simpleName}: ${e.message}") }
    }
    Box(Modifier.fillMaxSize().zIndex(5f).background(NiuniaColor.Scrim), contentAlignment = Alignment.Center) {
        NiuniaContentBlock(NiuniaBlockKind.Empty) {
            NiuniaText(stringResource(R.string.niunia_tv1_error_title, name), variant = NiuniaTextVariant.TitleSection, lines = 2)
            NiuniaText(stringResource(R.string.niunia_tv1_error_body), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Secondary, lines = 3)
            NiuniaText(stringResource(R.string.niunia_tv1_error_tries), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 2)
            Row(Modifier.padding(top = NiuniaSpacing.Space12).focusGroup(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
                buttons.forEachIndexed { i, a ->
                    val (label, icon) = when (a) {
                        ForgeChannelFailed.Action.NEXT -> stringResource(R.string.niunia_tv1_error_next) to "chevron-down"
                        ForgeChannelFailed.Action.RETRY -> stringResource(R.string.niunia_tv1_error_retry) to "restart"
                        ForgeChannelFailed.Action.LIST -> stringResource(R.string.niunia_tv1_error_list) to null
                    }
                    // Zwykle OK łapie odtwarzacz ([ForgeChannelFailed.onKey]); kliknięcie tylko na wypadek innej drogi klawisza.
                    NiuniaButton(onClick = { ForgeChannelFailed.selected.intValue = i; ForgeChannelFailed.run(state, a, null) }, label = label, icon = icon,
                        compact = a == ForgeChannelFailed.Action.NEXT, focusRequester = requesters[i])
                }
            }
        }
    }
}
