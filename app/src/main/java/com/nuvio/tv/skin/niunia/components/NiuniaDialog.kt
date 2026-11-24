package com.nuvio.tv.skin.niunia.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaRadius
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing

private const val TAG = "ForkNiunia"

/** Jedna pozycja okna akcji: ikona (opcjonalnie), etykieta, strzałka „dalej”, akcja. */
/** [dismiss] = false: akcja sama prowadzi dalej (np. okno list), okno nie jest zamykane osobno. */
data class NiuniaDialogAction(val label: String, val icon: String? = null, val chevron: Boolean = false, val dismiss: Boolean = true, val onClick: () -> Unit)

/**
 * `NOverlay kind="dialog"` (Z-14, makieta TitleActions / EpisodeActions): okno na środku sceny, szerokość `dialog-w`,
 * promień 28, tło `panel`, przyciemnienie `scrim` za oknem. Fokus zostaje w oknie, Wstecz zamyka.
 * Okno to osobne okno systemu: puszczenie OK po długim OK, które je otworzyło, nie trafia w pierwszą pozycję
 * (pozycje Niuni klikają tylko po własnym wciśnięciu, [NiuniaOkGate]).
 */
@Composable
fun NiuniaDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    LaunchedEffect(title) { Log.i(TAG, "okno: „$title” otwarte") }
    Dialog(onDismissRequest = { Log.i(TAG, "okno: „$title” zamknięte (Wstecz)"); onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(NiuniaColor.Scrim), contentAlignment = Alignment.Center) {
            val shape = RoundedCornerShape(NiuniaRadius.Radius28)
            Column(
                with(com.nuvio.tv.fork.glass.ForkGlass) { Modifier.width(NiuniaSize.DialogW).forkGlass(com.nuvio.tv.fork.glass.ForkGlass.Role.Dialog, shape, fallback = NiuniaColor.Panel, fallbackBorder = NiuniaColor.Fill12) }
                    .padding(NiuniaSpacing.Space24).focusGroup(),
                verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space16),
                content = content,
            )
        }
    }
}

/**
 * Okno akcji (TitleActions / EpisodeActions): nadtytuł, tytuł, stan w jednej linii i lista akcji (wiersze `setting` z ikoną).
 * Fokus startowy na pozycji [focusIndex]. Każdy wybór zamyka okno i zapisuje się w logu.
 */
@Composable
fun NiuniaActionsDialog(
    eyebrow: String?,
    title: String,
    state: String?,
    actions: List<NiuniaDialogAction>,
    onDismiss: () -> Unit,
    focusIndex: Int = 0,
) {
    val first = remember { FocusRequester() }
    NiuniaDialog(title, onDismiss) {
        NiuniaContentBlock(NiuniaBlockKind.Info) {
            if (!eyebrow.isNullOrBlank()) NiuniaText(eyebrow, variant = NiuniaTextVariant.Eyebrow, tone = NiuniaTone.Secondary)
            NiuniaText(title, variant = NiuniaTextVariant.TitleSection, lines = 2)
            if (!state.isNullOrBlank()) NiuniaText(state, variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 2)
        }
        Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4)) {
            actions.forEachIndexed { i, a ->
                NiuniaListRow(
                    a.label,
                    onClick = { Log.i(TAG, "okno „$title”: wybrano „${a.label}”"); a.onClick(); if (a.dismiss) onDismiss() }, // najpierw akcja (stan okna jeszcze jest), potem zamknięcie
                    kind = NiuniaRowKind.Setting,
                    icon = a.icon,
                    trailing = if (a.chevron) NiuniaTrailing.Chevron else NiuniaTrailing.None,
                    focusRequester = if (i == focusIndex.coerceIn(0, actions.lastIndex)) first else null,
                )
            }
        }
    }
    LaunchedEffect(actions.size) {
        kotlinx.coroutines.delay(80)
        try { first.requestFocus(); Log.i(TAG, "okno „$title”: fokus startowy → „${actions.getOrNull(focusIndex)?.label}”") }
        catch (e: IllegalStateException) { Log.w(TAG, "okno „$title”: fokus startowy: ${e.javaClass.simpleName}: ${e.message}") }
    }
}
