package com.nuvio.tv.skin.kicia.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.components.ForgeButton
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/**
 * S16 "Skonfiguruj dla tego TV" of the mockup (b51): a card x 150–810, y 64–476 on a soft background,
 * "krok i z 3", the step's title, its rows, and [Wstecz][Dalej/Gotowe] at the bottom right.
 * The steps are Nuvio's own first-run steps (mode, Home layout) plus the mockup's "Źródła".
 */
@Composable
fun ForgeWizardScaffold(
    step: Int,
    title: String,
    primaryText: String,
    onPrimary: (() -> Unit)?,
    onBack: (() -> Unit)?,
    primaryFocus: FocusRequester? = null,
    side: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(ForgeColors.Bg)
            .background(Brush.radialGradient(0f to Color(0xFF3A4450), 0.6f to Color(0xFF1C2229), 1f to ForgeColors.Bg, radius = 1200f)),
    ) {
        Column(
            Modifier.padding(start = 150.dp, top = 40.dp).width(660.dp).height(450.dp)
                .clip(RoundedCornerShape(24.dp)).background(ForgeColors.Sheet).padding(36.dp),
        ) {
            Text("Skonfiguruj dla tego TV · krok $step z 3", style = ForgeText.label5)
            Text(title, style = ForgeText.title, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(20.dp))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
                side?.invoke()
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                if (onBack != null) ForgeButton(text = "Wstecz", onOk = onBack)
                if (onPrimary != null) ForgeButton(text = primaryText, primary = true, focusRequester = primaryFocus, onOk = onPrimary)
            }
        }
    }
}

/** A row of the wizard: name on the left, its value and › on the right (56 dp). */
@Composable
fun ForgeWizardRow(
    title: String,
    value: String? = null,
    selected: Boolean = false,
    focusRequester: FocusRequester? = null,
    onOk: () -> Unit,
) {
    ForgeFocusBox(radius = 16.dp, modifier = Modifier.fillMaxWidth().height(56.dp), scaleOnFocus = false, focusRequester = focusRequester, onOk = onOk) { focused ->
        val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
        Row(
            Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                .background(if (focused) ForgeColors.Focus else if (selected) ForgeColors.Surface3 else ForgeColors.Surface2)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = ForgeText.button.copy(color = fg), modifier = Modifier.weight(1f), maxLines = 1)
            if (value != null) Text(value, style = ForgeText.body.copy(color = if (focused) ForgeColors.OnFocus else ForgeColors.Text2), maxLines = 1)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = fg, modifier = Modifier.padding(start = 10.dp).size(20.dp))
        }
    }
}

/** Step 1: Nuvio's experience mode (Essential / Advanced), as wizard rows. */
@Composable
fun ForgeWizardMode(firstFocus: FocusRequester, onEssential: () -> Unit, onAdvanced: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { ForgeWizardState.markSourcesPending(context) } // step 3 follows on Home
    ForgeWizardScaffold(step = 1, title = "Tryb aplikacji", primaryText = "Dalej", onPrimary = null, onBack = null) {
        Text("Wybierz, ile ustawień chcesz widzieć. Zmienisz to później w Ustawieniach.", style = ForgeText.body)
        ForgeWizardRow(title = "Podstawowy", value = "najprostszy, gotowy układ", focusRequester = firstFocus, onOk = onEssential)
        ForgeWizardRow(title = "Zaawansowany", value = "wszystkie opcje i wybór układu", onOk = onAdvanced)
    }
}

/** Step 3 is shown once on Home after Nuvio's first-run steps (set when step 1 is answered). */
object ForgeWizardState {
    private const val FILE = "forge_wizard"
    fun markSourcesPending(context: android.content.Context) =
        context.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE).edit().putBoolean("sources", true).apply()
    fun sourcesPending(context: android.content.Context): Boolean =
        context.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE).getBoolean("sources", false)
    fun done(context: android.content.Context) =
        context.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE).edit().putBoolean("sources", false).apply()
}

/** S16 step 3 "Źródła" (b51): add-ons, the IPTV playlist, the guide, and a QR to add a playlist from the phone. */
@Composable
fun ForgeWizardSourcesHost() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(ForgeWizardState.sourcesPending(context)) }
    if (!open) return
    val live by com.nuvio.tv.reshaped.livetv.LiveTvRepository.uiState.collectAsState()
    val finish = { ForgeWizardState.done(context); open = false }
    val first = androidx.compose.runtime.remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    androidx.compose.ui.window.Dialog(onDismissRequest = finish, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        ForgeWizardScaffold(
            step = 3, title = "Źródła", primaryText = "Gotowe", onPrimary = finish, onBack = null,
            side = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(200.dp)) {
                    ForgeWizardSetupQr()
                }
            },
        ) {
            ForgeWizardRow(title = "Playlista IPTV (M3U)", value = if (live.channels.isEmpty()) "brak" else "${live.channels.size} kanałów", focusRequester = first,
                onOk = { finish(); com.nuvio.tv.skin.kicia.nav.ForgeNavRequests.open(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE) })
            ForgeWizardRow(title = "Przewodnik (XMLTV)", value = if (live.sources.isEmpty()) "brak" else "${live.sources.size} źródła",
                onOk = { finish(); com.nuvio.tv.skin.kicia.nav.ForgeNavRequests.open(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE) })
            ForgeWizardRow(title = "Dodatki", value = "w Ustawieniach", onOk = { finish(); com.nuvio.tv.skin.kicia.nav.ForgeNavRequests.open(com.nuvio.tv.ui.navigation.Screen.Settings.route) })
        }
    }
}

/**
 * The QR of the phone page of Live TV's source setup (M3U link or file, Xtream, Stalker) — now with the guide (EPG) address and the
 * logo package address (owner 10.11). The page runs while this step is open.
 */
@Composable
private fun ForgeWizardSetupQr() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var shown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Pair<String, android.graphics.Bitmap?>?>(null) }
    androidx.compose.runtime.DisposableEffect(Unit) {
        var server: com.nuvio.tv.reshaped.livetv.LiveTvSetupServer? = null
        val ip = com.nuvio.tv.reshaped.net.LanAddress.get(context)
        if (ip != null) {
            server = com.nuvio.tv.reshaped.livetv.LiveTvSetupServer.startOnAvailablePort(context)
            server?.let {
                val url = "http://$ip:${it.listeningPort}/${it.token}/"
                shown = url to com.nuvio.tv.core.qr.QrCodeGenerator.generate(url, 400)
            }
        }
        onDispose { server?.stop() }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val qr = shown?.second
        if (qr != null) androidx.compose.foundation.Image(bitmap = qr.asImageBitmap(), contentDescription = "Kod QR", modifier = Modifier.size(160.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)))
        Text(
            if (shown == null) "Brak sieci — połącz telewizor z Wi‑Fi" else "Zeskanuj telefonem: M3U, EPG i paczka logo",
            style = ForgeText.caption, modifier = Modifier.padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
