package com.nuvio.tv.skin.kicia.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/** One row of the basic settings: [target] = name of the full-settings category it opens (null = the "show all" row). */
internal data class ForgeBasicRow(val title: String, val description: String? = null, val target: String?)

/** One group of rows; [header] only when the panel has two groups (spec 15: WYGLĄD / STRONA GŁÓWNA). */
internal data class ForgeBasicGroup(val header: String?, val rows: List<ForgeBasicRow>)

internal data class ForgeBasicCategory(val name: String, val icon: ImageVector, val groups: List<ForgeBasicGroup>)

/** v7.4 (15): the 8 categories of the basic settings, each fitting one screen (up to 7 rows). */
internal val ForgeBasicCategories: List<ForgeBasicCategory> = listOf(
    ForgeBasicCategory("Konto i profile", Icons.Outlined.AccountCircle, listOf(ForgeBasicGroup(null, listOf(
        ForgeBasicRow("Konto", target = "ACCOUNT"),
        ForgeBasicRow("Zarządzaj profilami", target = "PROFILES"),
        ForgeBasicRow("Wspólne oglądanie", target = "WATCH_PARTY"),
    )))),
    ForgeBasicCategory("Wygląd", Icons.Outlined.Palette, listOf(
        ForgeBasicGroup("WYGLĄD", listOf(
            ForgeBasicRow("Motyw kolorystyczny", target = "APPEARANCE"),
            ForgeBasicRow("Tryb AMOLED", target = "APPEARANCE"),
            ForgeBasicRow("Język aplikacji", target = "APPEARANCE"),
        )),
        ForgeBasicGroup("STRONA GŁÓWNA", listOf(
            ForgeBasicRow("Układ strony głównej", target = "LAYOUT"),
            ForgeBasicRow("Półka „YouTube dla Ciebie”", target = "FORGE"),
        )),
    )),
    ForgeBasicCategory("Odtwarzanie", Icons.Outlined.PlayCircle, listOf(ForgeBasicGroup(null, listOf(
        ForgeBasicRow("Wybór źródła", target = "PLAYBACK"),
        ForgeBasicRow("Następny odcinek automatycznie", target = "PLAYBACK"),
        ForgeBasicRow("Język dźwięku", target = "PLAYBACK"),
        ForgeBasicRow("Język napisów", target = "PLAYBACK"),
        ForgeBasicRow("Napisy wymuszone", target = "PLAYBACK"),
    )))),
    ForgeBasicCategory("YouTube", Icons.Outlined.VideoLibrary, listOf(ForgeBasicGroup(null, listOf(
        ForgeBasicRow("Konto", target = "YOUTUBE"),
        ForgeBasicRow("Jakość domyślna", target = "YOUTUBE"),
        ForgeBasicRow("Napisy automatyczne", target = "YOUTUBE"),
        ForgeBasicRow("Następny film automatycznie", target = "YOUTUBE"),
        ForgeBasicRow("Ukryj Shorts", target = "YOUTUBE"),
    )))),
    ForgeBasicCategory("Treści", Icons.Outlined.Extension, listOf(ForgeBasicGroup(null, listOf(
        ForgeBasicRow("Dodatki", target = "CONTENT_DISCOVERY"),
        ForgeBasicRow("Serwery mediów", target = "INTEGRATION"),
    )))),
    ForgeBasicCategory("Śledzenie", Icons.Outlined.Sync, listOf(ForgeBasicGroup(null, listOf(
        ForgeBasicRow("Trakt", target = "TRACKING"),
        ForgeBasicRow("Simkl", target = "TRACKING"),
    )))),
    ForgeBasicCategory("O aplikacji", Icons.Outlined.Info, listOf(ForgeBasicGroup(null, listOf(
        ForgeBasicRow("Sprawdź aktualizacje", target = "ABOUT"),
        ForgeBasicRow("Instaluj aktualizacje automatycznie", target = "ABOUT"),
        ForgeBasicRow("Polityka prywatności", target = "ABOUT"),
    )))),
    ForgeBasicCategory("Wszystkie ustawienia", Icons.Outlined.Tune, listOf(ForgeBasicGroup(null, listOf(
        ForgeBasicRow("Pokaż wszystkie ustawienia", "Ponad 200 opcji dla zaawansowanych. Twoje wartości zostają.", target = null),
        ForgeBasicRow("Najprostsze ustawienia", "Tylko najważniejsze opcje na jednym ekranie", target = "FORK_SIMPLE"), // fork 19.11: powrót do najprostszych
    )))),
)

/**
 * v7.4 (15) Ustawienia podstawowe: the layout of spec 14 (rail x 40-256 from y 84, rows 36; panel x 296-920, title 20/700 y 84,
 * rows 56 from y 126, gap 4) with 8 short categories. A row opens its category in the full settings ([onOpen]); the last
 * category asks "Pokazać wszystkie ustawienia?" and switches the version ([onShowAll]).
 */
@Composable
fun ForgeBasicSettings(
    onOpen: (String) -> Unit,
    onShowAll: () -> Unit,
) {
    var selected by remember { mutableIntStateOf(0) }
    var askAll by remember { mutableStateOf(false) }
    val forkCtx = androidx.compose.ui.platform.LocalContext.current // fork 19.11: „Najprostsze ustawienia”
    val railRequesters = remember { List(ForgeBasicCategories.size) { FocusRequester() } }
    val firstRow = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { railRequesters[0].requestFocus() } }
    val category = ForgeBasicCategories[selected]

    Row(Modifier.fillMaxSize().background(ForgeColors.Bg).padding(top = 84.dp, start = 40.dp, end = 40.dp)) {
        // Rail
        Column(Modifier.width(216.dp).focusGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ForgeBasicCategories.forEachIndexed { i, c ->
                ForgeFocusBox(
                    radius = 8.dp,
                    modifier = Modifier.fillMaxWidth().height(36.dp).testTag("wiersz.$i").focusProperties { right = firstRow }, // ▲ z pierwszej: pasek (zakładka Ustawienia)
                    focusRequester = railRequesters[i],
                    scaleOnFocus = false,
                    onFocusChange = { if (it) selected = i },
                    onOk = { runCatching { firstRow.requestFocus() } },
                ) { focused ->
                    Row(
                        Modifier.fillMaxSize().background(
                            when { focused -> ForgeColors.Focus; i == selected -> ForgeColors.Surface3; else -> androidx.compose.ui.graphics.Color.Transparent },
                            RoundedCornerShape(8.dp),
                        ).padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val fg = if (focused) ForgeColors.OnFocus else ForgeColors.Text
                        Icon(c.icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(c.name, style = ForgeText.label.copy(color = fg), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("wiersz-tytul.$i"))
                    }
                }
            }
        }
        Spacer(Modifier.width(40.dp))
        // Panel
        Column(Modifier.weight(1f).focusGroup()) {
            Text(category.name, style = ForgeText.head, maxLines = 1, modifier = Modifier.height(26.dp).testTag("naglowek-sekcji.0"))
            Spacer(Modifier.height(16.dp))
            var n = 0
            category.groups.forEachIndexed { gi, group ->
                if (group.header != null) {
                    if (gi > 0) Spacer(Modifier.height(44.dp)) // v7.4 (15): STRONA GŁÓWNA y 392 (ostatni wiersz WYGLĄD kończy się na 348)
                    Text(group.header, style = ForgeText.head, maxLines = 1, modifier = Modifier.height(26.dp).testTag("naglowek-sekcji.${gi + 1}"))
                    Spacer(Modifier.height(20.dp)) // v7.4 (15): nagłówek grupy y 126 → wiersze y 172
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    group.rows.forEach { row ->
                        val index = n++
                        ForgeFocusBox(
                            radius = 8.dp,
                            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("wiersz.${ForgeBasicCategories.size + index}")
                                .then(if (index == 0) Modifier.focusRequester(firstRow) else Modifier)
                                .focusProperties { left = railRequesters[selected] },
                            scaleOnFocus = false,
                            onOk = { if (row.target == "FORK_SIMPLE") com.nuvio.tv.fork.settings.SimpleLevel.toSimple(forkCtx) else if (row.target != null) onOpen(row.target) else askAll = true },
                        ) { focused ->
                            Row(
                                Modifier.fillMaxSize().background(if (focused) ForgeColors.Focus else ForgeColors.Surface, RoundedCornerShape(8.dp))
                                    .padding(start = 16.dp, end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.title, style = ForgeText.shelfTitle.copy(color = if (focused) ForgeColors.OnFocus else ForgeColors.Text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (row.description != null) Text(
                                        row.description, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        style = ForgeText.label5.copy(color = if (focused) ForgeColors.OnFocus.copy(alpha = .7f) else ForgeColors.Text2),
                                        modifier = Modifier.testTag("wiersz-opis.$index"),
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                                    tint = if (focused) ForgeColors.OnFocus else ForgeColors.Text2,
                                    modifier = Modifier.padding(start = 12.dp).size(16.dp).testTag("wiersz-szewron.$index"),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    com.nuvio.tv.skin.kicia.components.ForgeDialog(
        visible = askAll,
        title = "Pokazać wszystkie ustawienia?",
        message = "Zobaczysz pełną listę ustawień. Twoje wartości zostają, a wrócić możesz w kategorii Zaawansowane.",
        buttons = listOf(
            com.nuvio.tv.skin.kicia.components.ForgeDialogButton("Anuluj") { askAll = false },
            com.nuvio.tv.skin.kicia.components.ForgeDialogButton("Pokaż", primary = true) { askAll = false; onShowAll() },
        ),
        onDismiss = { askAll = false },
        initialFocusIndex = 1,
    )
}
