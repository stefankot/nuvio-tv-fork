package com.nuvio.tv.skin.kicia.catalog

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.kicia.components.ForgeAvatar
import com.nuvio.tv.skin.kicia.components.ForgeBadge
import com.nuvio.tv.skin.kicia.components.ForgeBadgeKind
import com.nuvio.tv.skin.kicia.components.ForgeCardCaption
import com.nuvio.tv.skin.kicia.components.ForgeButton
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.components.ForgeForced
import com.nuvio.tv.skin.kicia.components.ForgeIconButton
import com.nuvio.tv.skin.kicia.components.ForgePillSelect
import com.nuvio.tv.skin.kicia.components.ForgeSheetRow
import com.nuvio.tv.skin.kicia.components.ForgeSkeleton
import com.nuvio.tv.skin.kicia.components.ForgeTagged
import com.nuvio.tv.skin.kicia.components.ForgeToastPill
import com.nuvio.tv.skin.kicia.nav.ForgeNavTab
import com.nuvio.tv.skin.kicia.nav.forgeTag
import com.nuvio.tv.skin.kicia.nav.forgeTagRoot
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/**
 * v7.4 katalog komponentów (debug): the mockup's page `00-komponenty`, the same components in the same states and places,
 * so `narzedzia/roznice.py 00 <strona>` can compare them. Start:
 * `adb shell am start -n app.nuvioforge.tv.debug/com.nuvio.tv.skin.kicia.catalog.ForgeCatalogActivity -e page przyciski`
 */
class ForgeCatalogActivity : ComponentActivity() {
    private var page by mutableStateOf("przyciski")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        page = intent?.getStringExtra("page") ?: page
        setContent { ForgeCatalog(page) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        page = intent.getStringExtra("page") ?: page
    }
}

private class State(val caption: String, val forced: ForgeForced? = null, val enabled: Boolean = true, val loading: Boolean = false, val selected: Boolean = false)

private val Rest = State("spoczynek")
private val Focus = State("fokus", forced = ForgeForced.Focus)
private val Pressed = State("wciśnięty", forced = ForgeForced.Pressed)
private val Disabled = State("wyłączony", enabled = false)
private val Loading = State("ładowanie", loading = true)
private val Selected = State("wybrany", selected = true)
private val Five = listOf(Rest, Focus, Pressed, Disabled, Loading)
private val Six = listOf(Rest, Focus, Selected, Pressed, Disabled, Loading)

@Composable
fun ForgeCatalog(page: String) {
    Box(Modifier.fillMaxSize().background(ForgeColors.Bg).forgeTagRoot()) {
        // The first focusable element takes the real focus on TV: give it to an invisible one, so every sample shows only its forced state.
        val parking = remember { FocusRequester() }
        Box(Modifier.size(1.dp).focusRequester(parking).focusable())
        androidx.compose.runtime.LaunchedEffect(page) { if (!page.startsWith("nakladki-")) runCatching { parking.requestFocus() } }
        when (page) {
            "nakladki-okno" -> com.nuvio.tv.skin.kicia.components.ForgeDialog(
                visible = true, title = "Usunąć z listy?", message = "Tytuł zniknie z Twojej listy. Postęp oglądania zostanie zachowany.",
                buttons = listOf(com.nuvio.tv.skin.kicia.components.ForgeDialogButton("Anuluj") {}, com.nuvio.tv.skin.kicia.components.ForgeDialogButton("Usuń", primary = true) {}),
                onDismiss = {}, initialFocusIndex = 1,
            )
            "nakladki-popover" -> com.nuvio.tv.skin.kicia.components.ForgePopover(onDismiss = {}, x = 40.dp, y = 286.dp) {
                ForgeSheetRow("Polski", selected = true, trailingIcon = Icons.Filled.Check) {}
                ForgeSheetRow("Angielski") {}
                ForgeSheetRow("Wyłączone") {}
            }
            "nakladki-panel" -> com.nuvio.tv.skin.kicia.components.ForgeSideSheet(visible = true, onDismiss = {}, title = "Ustawienia") {
                listOf("Wygląd", "Odtwarzanie", "Język i napisy", "Konta i profile", "Dodatki", "O aplikacji").forEach {
                    ForgeSheetRow(it, tile = true, trailingIcon = Icons.Filled.KeyboardArrowRight) {}
                }
            }
        }
        if (!page.startsWith("nakladki-")) Column(Modifier.padding(start = 40.dp, top = 24.dp, end = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (page) {
                "plakat-pozioma" -> Cards()
                "kanal" -> Channels()
                "dalej-dzien" -> NextAndDays()
                "pole-epg-postep" -> Keys()
                "pasek-plakietki" -> BarAndBadges()
                "wiersze" -> Rows()
                "awatary" -> Avatars()
                else -> Buttons()
            }
        }
    }
}

@Composable
private fun Group(name: String, modifier: Modifier = Modifier, gap: Dp = 8.dp, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        Text(name, style = ForgeText.captionBold.copy(color = ForgeColors.Text3), maxLines = 1)
        content()
    }
}

/** A component with its state written under it (the mockup's K.podpisany). */
@Composable
private fun Captioned(caption: String, tag: String, forced: ForgeForced? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ForgeTagged(tag, forced, content)
        Text(caption, style = ForgeText.caption.copy(color = ForgeColors.Text3), maxLines = 1)
    }
}

@Composable
private fun StateRow(states: List<State>, tag: String, first: Int, gap: Dp = 12.dp, item: @Composable (State) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
        states.forEachIndexed { i, s -> Captioned(s.caption, "$tag.${first + i}", s.forced) { item(s) } }
    }
}

@Composable
private fun Buttons() {
    Group("Przycisk główny") {
        StateRow(Five, "przycisk", 0) { s -> ForgeButton("Odtwórz", icon = Icons.Filled.PlayArrow, primary = true, enabled = s.enabled, loading = s.loading) {} }
    }
    Group("Przycisk dodatkowy") {
        StateRow(Five, "przycisk", 5) { s -> ForgeButton("Moja lista", icon = Icons.Filled.Add, enabled = s.enabled, loading = s.loading) {} }
    }
    Group("Przycisk mały") {
        StateRow(Five, "przycisk", 10) { s -> ForgeButton("Zwiastun", icon = Icons.Filled.PlayArrow, small = true, enabled = s.enabled, loading = s.loading) {} }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Group("Przycisk okrągły 44") {
            StateRow(Six, "przycisk", 15, gap = 8.dp) { s -> ForgeIconButton(Icons.Filled.Add, "plus", active = s.selected, enabled = s.enabled, loading = s.loading) {} }
        }
        Group("Przycisk okrągły 36") {
            StateRow(Six, "przycisk", 21, gap = 8.dp) { s -> ForgeIconButton(Icons.Filled.Add, "plus", size = 36.dp, active = s.selected, enabled = s.enabled, loading = s.loading) {} }
        }
    }
    Group("Pigułka wyboru") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(Rest, Focus, State("wybrana", selected = true), Pressed, State("wyłączona", enabled = false)).forEachIndexed { i, s ->
                Captioned(s.caption, "pigulka.$i", s.forced) { ForgePillSelect("Napisy", selected = s.selected, enabled = s.enabled) {} }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ForgeSkeleton(80.dp, 36.dp, 18.dp, Modifier.forgeTag("szkielet.0"))
                Text("ładowanie", style = ForgeText.caption.copy(color = ForgeColors.Text3))
            }
        }
    }
}

@Composable
private fun Tray(tag: String, content: @Composable () -> Unit) {
    Row(
        Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(ForgeColors.Tray).forgeTag(tag).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Composable
private fun Tab(text: String?, tag: String, selected: Boolean = false, forced: ForgeForced? = null, enabled: Boolean = true) {
    val requester = remember { FocusRequester() }
    Box {
        ForgeTagged(tag, forced) {
            ForgeNavTab(selected = selected, enabled = false, requester = requester, onFocused = {}, onOk = {}, round = text == null, dimmed = !enabled) { fg ->
                if (text == null) Icon(Icons.Outlined.Search, contentDescription = "Szukaj", tint = fg, modifier = Modifier.width(20.dp).height(20.dp))
                else Text(text, style = ForgeText.button.copy(color = fg))
            }
        }
    }
}

@Composable
private fun Round36(tag: String, icon: androidx.compose.ui.graphics.vector.ImageVector, name: String, selected: Boolean = false, forced: ForgeForced? = null) {
    ForgeTagged(tag, forced) { ForgeIconButton(icon, name, size = 36.dp, active = selected) {} }
}

@Composable
private fun BarAndBadges() {
    Group("Nagłówek sekcji (20/700, 40 nad, 16 pod)") {
        Box(Modifier.padding(bottom = 16.dp)) { Text("Nagłówek sekcji", style = ForgeText.head, modifier = Modifier.forgeTag("naglowek-sekcji.0")) }
    }
    Group("Pasek: Taca i Zakładka") {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            val states = listOf(Rest, State("wybrana", selected = true), Focus, State("wciśnięta", forced = ForgeForced.Pressed), State("wyłączona", enabled = false))
            states.forEachIndexed { i, s ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Tray("taca.$i") { Tab("Home", "zakladka.$i", selected = s.selected, forced = s.forced, enabled = s.enabled) }
                    Text(s.caption, style = ForgeText.caption.copy(color = ForgeColors.Text3))
                }
            }
        }
    }
    Group("Pasek: Taca i Zakładka · całość z Awatarem 44") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Tray("taca.5") {
                Tab(null, "zakladka.5"); Tab("Home", "zakladka.6", selected = true); Tab("Live TV", "zakladka.7"); Tab("YouTube", "zakladka.8"); Tab("Biblioteka", "zakladka.9")
            }
            Tray("taca.6") {
                Round36("przycisk.0", Icons.Outlined.Bedtime, "Tryb nocny"); Round36("przycisk.1", Icons.Outlined.Settings, "Ustawienia")
            }
            ForgeTagged("awatar.0") { ForgeFocusBox(radius = 22.dp, enabled = false) { ForgeAvatar("Maja", 44.dp) } }
        }
    }
    Group("Pasek: Tryb nocny (Przycisk okrągły 36, przed Ustawieniami w tacy przy zegarze): wyłączony · włączony (wybrany, ikona wypełniona) · fokus") {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Tray("taca.7") { Round36("przycisk.2", Icons.Outlined.Bedtime, "Tryb nocny") }; Text("wyłączony", style = ForgeText.caption.copy(color = ForgeColors.Text3)) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Tray("taca.8") { Round36("przycisk.3", Icons.Filled.Bedtime, "Tryb nocny", selected = true) }; Text("włączony", style = ForgeText.caption.copy(color = ForgeColors.Text3)) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Tray("taca.9") { Round36("przycisk.4", Icons.Filled.Bedtime, "Tryb nocny", selected = true, forced = ForgeForced.Focus) }; Text("włączony, fokus", style = ForgeText.caption.copy(color = ForgeColors.Text3)) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        Group("Plakietka") {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf(
                    Triple("Na żywo", ForgeBadgeKind.Live, "Na żywo"), Triple("Archiwum", ForgeBadgeKind.Archive, "Archiwum"),
                    Triple("Niedostępne", ForgeBadgeKind.Unavailable, "Niedostępne"), Triple("Wkrótce", ForgeBadgeKind.Soon, "Wkrótce"), Triple("jakość", ForgeBadgeKind.Quality, "4K"),
                ).forEachIndexed { i, (caption, kind, text) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ForgeBadge(text, kind, Modifier.forgeTag("plakietka.$i"))
                        Text(caption, style = ForgeText.caption.copy(color = ForgeColors.Text3))
                    }
                }
            }
        }
        Group("Komunikat") { ForgeToastPill("Brak połączenia. Pokazuję zapisane.", Modifier.forgeTag("komunikat.0")) }
    }
}

@Composable
private fun CatalogRow(tag: String, text: String, forced: ForgeForced? = null, enabled: Boolean = true, selected: Boolean = false, detail: String? = null,
                       chevron: Boolean = true, switch: Boolean? = null, description: String? = null, rail: Boolean = false) {
    ForgeTagged(tag, forced) {
        ForgeSheetRow(text, tile = true, enabled = enabled, selected = selected, detail = detail, switch = switch, description = description, rail = rail,
            trailingIcon = if (chevron && detail == null && switch == null && !rail) Icons.Filled.KeyboardArrowRight else null) {}
    }
}

@Composable
private fun Rows() {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Group("Wiersz 44", Modifier.width(280.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CatalogRow("wiersz.0", "spoczynek"); CatalogRow("wiersz.1", "fokus", forced = ForgeForced.Focus)
                CatalogRow("wiersz.2", "wybrany", selected = true, detail = "Polski"); CatalogRow("wiersz.3", "wciśnięty", forced = ForgeForced.Pressed)
                CatalogRow("wiersz.4", "wyłączony", enabled = false)
                ForgeSkeleton(280.dp, 44.dp, 8.dp, Modifier.forgeTag("szkielet.0"))
            }
        }
        Group("Wiersz z przełącznikiem", Modifier.width(280.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CatalogRow("wiersz.5", "włączony", switch = true); CatalogRow("wiersz.6", "wyłączony", switch = false)
                CatalogRow("wiersz.7", "fokus", switch = true, forced = ForgeForced.Focus)
                CatalogRow("wiersz.8", "nieaktywny, wł.", switch = true, enabled = false); CatalogRow("wiersz.9", "nieaktywny, wył.", switch = false, enabled = false)
            }
        }
        Column(Modifier.width(280.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Group("Wiersz 56") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CatalogRow("wiersz.10", "Dwie linie", description = "Opis pod tytułem")
                    CatalogRow("wiersz.11", "Dwie linie w fokusie", description = "Opis pod tytułem", forced = ForgeForced.Focus)
                }
            }
            Group("Wiersz 36 (szyna)") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CatalogRow("wiersz.12", "Szyna", rail = true); CatalogRow("wiersz.13", "Szyna w fokusie", rail = true, forced = ForgeForced.Focus)
                    CatalogRow("wiersz.14", "Szyna wybrana", rail = true, selected = true)
                }
            }
        }
    }
}

@Composable
private fun Avatars() {
    var n = 0
    listOf(44.dp, 72.dp, 120.dp).forEach { size ->
        Group("Awatar ${size.value.toInt()}", gap = 24.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val states = listOf(Rest to "", Focus to "", Pressed to "", Disabled to "", State("inicjał") to "Maja") + if (size < 120.dp) listOf(State("inicjał, fokus", forced = ForgeForced.Focus) to "Maja") else emptyList()
                states.forEach { (s, name) ->
                    val tag = "awatar.${n++}"
                    Captioned(s.caption, tag, s.forced) {
                        ForgeFocusBox(radius = size / 2, enabled = false, modifier = Modifier.alpha(if (s.enabled) 1f else .5f)) { ForgeAvatar(name.ifEmpty { "M" }, size) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Cards() {
    Group("Karta/Plakat", gap = 24.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(Rest, Focus, Pressed, Disabled).forEachIndexed { i, s ->
                Captioned(s.caption, "karta.$i", s.forced) { com.nuvio.tv.skin.kicia.components.ForgePosterCard(onOk = {}) { } }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ForgeSkeleton(130.dp, 194.dp, 8.dp, Modifier.forgeTag("szkielet.0")); Text("ładowanie", style = ForgeText.caption.copy(color = ForgeColors.Text3))
            }
            Captioned("bez obrazu", "karta.4") {
                com.nuvio.tv.skin.kicia.components.ForgePosterCard(onOk = {}) { Text("Bez obrazu", style = ForgeText.label.copy(color = ForgeColors.Text2), modifier = Modifier.align(Alignment.Center)) }
            }
        }
    }
    Group("Karta/Pozioma", gap = 24.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(Rest, Focus, Pressed, Disabled).forEachIndexed { i, s ->
                Captioned(s.caption, "karta.${5 + i}", s.forced) {
                    com.nuvio.tv.skin.kicia.components.ForgeCard(160.dp, 114.dp, enabled = s.enabled, onOk = {}) {
                        if (i == 1) ForgeCardCaption("Resident Evil", "Dziś, 20:15", null) else ForgeCardCaption("Resident Evil", "S1, O3 · zostało 19 min", .4f)
                    }
                }
            }
        }
    }
}

@Composable
private fun Spacer16() = Box(Modifier.height(16.dp))

@Composable
private fun Keys() {
    Box(Modifier.height(88.dp)) // Komórka EPG: na ekranie 06
    Group("Klawisz") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("a" to Rest, "b" to Focus, "c" to Pressed, "d" to Disabled).forEachIndexed { i, (label, s) ->
                Captioned(s.caption, "klawisz.$i", s.forced) {
                    Box(Modifier.alpha(if (s.enabled) 1f else .5f)) { com.nuvio.tv.skin.kicia.keyboard.ForgeKeyCap(label, 36.dp, 36.dp, flat = true) {} }
                }
            }
        }
    }
    Group("Pole tekstowe") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Captioned("puste", "pole.0") { com.nuvio.tv.skin.kicia.components.ForgeField("", "Szukaj") {} }
                Captioned("wypełnione", "pole.1") { com.nuvio.tv.skin.kicia.components.ForgeField("Pan Tadeusz", "Szukaj") {} }
                Captioned("fokus", "pole.2", ForgeForced.Focus) { com.nuvio.tv.skin.kicia.components.ForgeField("Pan Tadeusz", "Szukaj") {} }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Captioned("wciśnięte", "pole.3", ForgeForced.Pressed) { com.nuvio.tv.skin.kicia.components.ForgeField("Pan Tadeusz", "Szukaj") {} }
                Captioned("wyłączone", "pole.4") { com.nuvio.tv.skin.kicia.components.ForgeField("Pan Tadeusz", "Szukaj", enabled = false) {} }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { ForgeSkeleton(200.dp, 44.dp, 22.dp, Modifier.forgeTag("szkielet.1")); Text("ładowanie", style = ForgeText.caption.copy(color = ForgeColors.Text3)) }
            }
        }
    }
}

@Composable
private fun Channels() {
    Group("Karta/Kanał (bez fokusu przyciemniona 30%)", gap = 24.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                listOf(Rest, Focus, Pressed).forEachIndexed { i, s ->
                    Captioned(s.caption, "karta.$i", s.forced) { com.nuvio.tv.skin.kicia.components.ForgeChannelCard("2", "TVP 2", "Fakty dnia", live = true, onOk = {}) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Captioned("wyłączony", "karta.3") { com.nuvio.tv.skin.kicia.components.ForgeChannelCard("2", "TVP 2", "Fakty dnia", live = true, enabled = false, onOk = {}) }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { ForgeSkeleton(240.dp, 135.dp, 8.dp, Modifier.forgeTag("szkielet.0")); Text("ładowanie", style = ForgeText.caption.copy(color = ForgeColors.Text3)) }
            }
        }
    }
}

@Composable
private fun NextAndDays() {
    Group("Karta/Dalej", gap = 24.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(Rest, Focus, Pressed, Disabled).forEachIndexed { i, s ->
                Captioned(s.caption, "karta.$i", s.forced) { com.nuvio.tv.skin.kicia.components.ForgeNextCard("21:30", "Prognoza pogody", enabled = s.enabled, onOk = {}) }
            }
        }
    }
    Box(Modifier.height(8.dp))
    Group("Karta/Dzień", gap = 24.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(Rest, Selected, Focus, Pressed, Disabled).forEachIndexed { i, s ->
                Captioned(s.caption, "dzien.$i", s.forced) { com.nuvio.tv.skin.kicia.components.ForgeDayCard("śr", "12", selected = s.selected, enabled = s.enabled) {} }
            }
        }
    }
}
