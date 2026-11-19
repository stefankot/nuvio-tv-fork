package com.nuvio.tv.fork.settings.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nuvio.tv.skin.api.SkinId
import com.nuvio.tv.skin.api.rememberSkinId
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.niunia.tokens.NiuniaColors

/**
 * Wygląd szkieletu ustawień forka (szyna kategorii, wiersze, plakietki) dla aktywnej skórki. Układ jest wspólny;
 * skórka Niunia podmienia tylko kolory i kształty na swoje (canvas .row: promień 12, fokus #F2F2F2; .side-item: pigułka).
 * Pozostałe skórki = dotychczasowy wygląd Kicii (bez zmian).
 */
data class SettingsLook(
    val bg: Color,
    val focus: Color,
    val onFocus: Color,
    /** Wiersz w spoczynku. */
    val row: Color,
    /** Kafel szyny w spoczynku. */
    val railTile: Color,
    /** Zaznaczona (otwarta) kategoria. */
    val selected: Color,
    val text: Color,
    val text2: Color,
    val on: Color,
    val rowRadius: Dp,
    val railRadius: Dp,
) {
    companion object {
        val Kicia = SettingsLook(
            bg = ForgeColors.Bg, focus = ForgeColors.Focus, onFocus = ForgeColors.OnFocus, row = ForgeColors.Surface,
            railTile = ForgeColors.Surface2, selected = ForgeColors.Surface3, text = ForgeColors.Text, text2 = ForgeColors.Text2,
            on = ForgeColors.Switch, rowRadius = 8.dp, railRadius = 8.dp,
        )
        val Niunia = SettingsLook(
            bg = NiuniaColors.Bg, focus = NiuniaColors.Focus, onFocus = NiuniaColors.OnFocus, row = NiuniaColors.Fill12,
            railTile = Color.Transparent, selected = NiuniaColors.Fill24, text = NiuniaColors.Text, text2 = NiuniaColors.Text70,
            on = Color(0xFF30D158), rowRadius = 12.dp, railRadius = 18.dp,
        )
    }
}

/** Wygląd ustawień dla wybranej skórki. */
@Composable
fun settingsLook(): SettingsLook = if (rememberSkinId() == SkinId.NIUNIA) SettingsLook.Niunia else SettingsLook.Kicia
