package com.nuvio.tv.skin.kicia.settings

import com.nuvio.tv.ui.screens.settings.*

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.nuvio.tv.skin.kicia.ForgePrefs
import com.nuvio.tv.skin.kicia.rememberForgeHomeYouTube
import com.nuvio.tv.skin.kicia.rememberForgeReminderCard
import com.nuvio.tv.skin.kicia.rememberForgeTvBrowseB
import com.nuvio.tv.ui.theme.NuvioTheme

/**
 * Settings → Nuvio Forge: only what this fork adds on top of Nuvio / Reshaped (owner 8.11: the rest of Settings
 * stays as the system has it). Drawn with Nuvio's own settings rows.
 */
@Composable
internal fun ForgeSettingsContent(initialFocusRequester: FocusRequester? = null) {
    val browseB = rememberForgeTvBrowseB()
    val homeYouTube = rememberForgeHomeYouTube()
    val reminderCard = rememberForgeReminderCard()
    val context = androidx.compose.ui.platform.LocalContext.current
    // System states change in the system's own screens: read again whenever Settings comes back.
    var tick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) tick++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val autostart = androidx.compose.runtime.remember(tick) { com.nuvio.tv.skin.kicia.ForgeSystemOptions.autostart(context) }
    val canStart = androidx.compose.runtime.remember(tick) { com.nuvio.tv.skin.kicia.ForgeSystemOptions.canStartFromBackground(context) }
    val homeOn = androidx.compose.runtime.remember(tick) { com.nuvio.tv.skin.kicia.ForgeSystemOptions.homeEnabled(context) }
    val isHome = androidx.compose.runtime.remember(tick) { com.nuvio.tv.skin.kicia.ForgeSystemOptions.isDefaultHome(context) }
    val isAssistant = androidx.compose.runtime.remember(tick) { com.nuvio.tv.skin.kicia.ForgeSystemOptions.isAssistant(context) }
    var showNoEpg by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val liveTvState by com.nuvio.tv.reshaped.livetv.LiveTvRepository.uiState.collectAsState()
    // Channels the guide has no programme for (spec IPTV: "kanały bez EPG"); counted from the guide as it is now.
    val noEpg = androidx.compose.runtime.remember(liveTvState.guideVersion, liveTvState.channels) {
        liveTvState.channels.filter { c -> com.nuvio.tv.reshaped.livetv.LiveTvRepository.schedule(c.guideKey).isEmpty() }
    }
    if (showNoEpg) ForgeNoEpgDialog(noEpg.map { it.name }, onDismiss = { showNoEpg = false })
    var editLogo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (editLogo) ForgeLogoPatternDialog(onDismiss = { editLogo = false })
    var showSources by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (showSources) com.nuvio.tv.ui.reshaped.livetv.LiveTvSourceDialog(onDismiss = { showSources = false })
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md),
    ) {
        SettingsDetailHeader(
            title = "Nuvio Forge",
            subtitle = "Ustawienia dodane w tej wersji (wygląd z makiety)",
        )
        SettingsGroupCard(title = "System") {
            SettingsToggleRow(
                title = "Uruchamiaj z telewizorem",
                subtitle = if (autostart && !canStart) "Włączone, ale telewizor wymaga jeszcze zgody „Wyświetlanie nad innymi aplikacjami” (wiersz niżej)" else "Nuvio otwiera się samo po włączeniu telewizora",
                checked = autostart,
                onToggle = { com.nuvio.tv.skin.kicia.ForgeSystemOptions.setAutostart(context, !autostart); tick++ },
                modifier = if (initialFocusRequester != null) Modifier.focusRequester(initialFocusRequester) else Modifier,
            )
            if (autostart && !canStart) {
                SettingsActionRow(
                    title = "Zgoda na start po włączeniu",
                    subtitle = "Otwiera ustawienia systemu: włącz „Wyświetlanie nad innymi aplikacjami” dla Nuvio",
                    value = "Brak",
                    onClick = { com.nuvio.tv.skin.kicia.ForgeSystemOptions.openOverlayPermission(context) },
                )
            }
            SettingsToggleRow(
                title = "Nuvio jako ekran główny (launcher)",
                subtitle = when {
                    isHome -> "Nuvio jest ekranem głównym: przycisk Home otwiera Nuvio"
                    homeOn -> "Gotowe do wyboru: naciśnij Home na pilocie i wybierz Nuvio (na Google TV może być potrzebne wyłączenie launchera Google)"
                    else -> "Przycisk Home otwiera Nuvio zamiast ekranu Google TV"
                },
                checked = homeOn,
                onToggle = { com.nuvio.tv.skin.kicia.ForgeSystemOptions.setHome(context, !homeOn); tick++ },
            )
            SettingsActionRow(
                title = "Nuvio jako asystent",
                subtitle = "Przycisk Asystenta na pilocie otwiera Szukaj w Nuvio z mikrofonem. Wybór w ustawieniach systemu (wyłącza Asystenta Google)",
                value = if (isAssistant) "Włączone" else "Wyłączone",
                onClick = { com.nuvio.tv.skin.kicia.ForgeSystemOptions.openAssistantSettings(context) },
            )
            SettingsActionRow( // fork 19.11 (właściciel): powrót z wersji średniej/rozszerzonej do najprostszych
                title = "Tryb NOOB",
                subtitle = "Tylko najważniejsze opcje na jednym ekranie (z niego wrócisz do pełnych)",
                onClick = { com.nuvio.tv.fork.settings.SimpleLevel.toSimple(context) },
            )
        }
        SettingsGroupCard(title = "Live TV") {
            // Nuvio's own source window (M3U / Xtream / Stalker + EPG link): the only place to type the guide address.
            SettingsActionRow(
                title = "Źródła i EPG",
                subtitle = "Listy kanałów oraz adres EPG, np. https://epg.ovh/pl.gz (okno źródeł Nuvio)",
                value = "Otwórz",
                onClick = { showSources = true },
            )
            // Owner 9.11: the playlist's logos are often poor, so the source of channel logos can be chosen and overridden.
            val logoMode = com.nuvio.tv.skin.kicia.rememberForgeLogoMode()
            val logoPattern = com.nuvio.tv.skin.kicia.rememberForgeLogoPattern()
            SettingsActionRow(
                title = "Logotypy kanałów",
                subtitle = "Skąd brać logo: z EPG (epg.ovh/logo, zalecane), z listy M3U lub spod własnego adresu",
                value = listOf("Z EPG, potem M3U", "Tylko z EPG", "Tylko z listy M3U", "Własny adres")[logoMode.coerceIn(0, 3)],
                onClick = { ForgePrefs.setLogoMode((logoMode + 1) % 4) },
            )
            SettingsActionRow(
                title = "Własny adres logotypów",
                subtitle = "Wzór z {name} = nazwa kanału z EPG, np. $logoPattern (włącz „Własny adres” wyżej)",
                value = "Zmień",
                onClick = { editLogo = true },
            )
            SettingsActionRow(
                title = "Kanały bez programu EPG",
                subtitle = "Kanały z listy, dla których EPG nie ma żadnych programów (nazwy z listy nie pasują do EPG albo kanału tam nie ma)",
                value = if (liveTvState.guideVersion == 0) "EPG nie wczytane" else "${noEpg.size} z ${liveTvState.channels.size}",
                onClick = { showNoEpg = true },
            )
            SettingsActionRow(
                title = "Układ Live TV",
                subtitle = if (browseB) "Najpierw strona Przeglądaj, przewodnik osobno" else "Ostatnio oglądane programy nad siatką przewodnika",
                value = if (browseB) "B: Przeglądaj + Przewodnik" else "A: Przewodnik z ostatnimi",
                onClick = { ForgePrefs.setTvBrowseB(!browseB) },
            )
            SettingsToggleRow(
                title = "Karta przypomnień",
                subtitle = "Gdy program z „Przypomnij mi” się zaczyna, karta w rogu obrazu",
                checked = reminderCard,
                onToggle = { ForgePrefs.setReminderCard(!reminderCard) },
            )
        }
        SettingsGroupCard(title = "Home") {
            SettingsToggleRow(
                title = "Półka „YouTube dla Ciebie”",
                subtitle = "Filmy z YouTube na ekranie głównym (więcej danych i pamięci)",
                checked = homeYouTube,
                onToggle = { ForgePrefs.setHomeYouTube(!homeYouTube) },
            )
        }
    }
}

/** Small window with one text field for the own logo address (Nuvio's own text field, so the remote and the phone keyboard work). */
@Composable
private fun ForgeLogoPatternDialog(onDismiss: () -> Unit) {
    var text by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(ForgePrefs.logoPattern.value) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxSize().background(com.nuvio.tv.skin.kicia.tokens.ForgeColors.Bg).padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            androidx.compose.material3.Text("Własny adres logotypów", style = com.nuvio.tv.skin.kicia.tokens.ForgeText.title)
            androidx.compose.material3.Text("{name} zastąpi nazwa kanału z EPG (spacje jako +). Domyślnie: " + ForgePrefs.DEFAULT_LOGO_PATTERN, style = com.nuvio.tv.skin.kicia.tokens.ForgeText.body)
            com.nuvio.tv.ui.reshaped.livetv.LiveTvTextField(text, { text = it }, ForgePrefs.DEFAULT_LOGO_PATTERN)
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Zapisz", primary = true, onOk = { ForgePrefs.setLogoPattern(text); ForgePrefs.setLogoMode(3); onDismiss() })
                com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Anuluj", onOk = onDismiss)
            }
        }
    }
}

/** The channels without a programme in the guide, by name (the first 80), so the owner can match them by hand or hide them. */
@Composable
private fun ForgeNoEpgDialog(names: List<String>, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxSize().background(com.nuvio.tv.skin.kicia.tokens.ForgeColors.Bg).padding(40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.material3.Text("Kanały bez programu EPG: ${names.size}", style = com.nuvio.tv.skin.kicia.tokens.ForgeText.title)
            androidx.compose.material3.Text(
                if (names.isEmpty()) "Każdy kanał z listy ma program w EPG."
                else names.take(80).joinToString("  ·  ") + if (names.size > 80) "  …" else "",
                style = com.nuvio.tv.skin.kicia.tokens.ForgeText.body,
            )
            com.nuvio.tv.skin.kicia.components.ForgeButton(text = "Zamknij", primary = true, onOk = onDismiss)
        }
    }
}
