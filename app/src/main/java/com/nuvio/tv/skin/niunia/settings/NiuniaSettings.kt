package com.nuvio.tv.skin.niunia.settings

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.skin.niunia.components.NiuniaIcon
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaRowKind
import com.nuvio.tv.skin.niunia.components.NiuniaTrailing
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.ui.screens.settings.ForkFeatures

private const val TAG = "ForkNiunia"

/** Wiersz ustawień NOOB: nagłówek (bez akcji) albo pozycja z wartością i strzałką. */
sealed interface NiuniaSettingsItem {
    data class Header(val title: String) : NiuniaSettingsItem
    data class Row(val key: String, val label: String, val value: String? = null, val chevron: Boolean = true, val action: () -> Unit) : NiuniaSettingsItem
}

/**
 * Ustawienia Niuni — tylko poziom NOOB (decyzja właściciela 20.11, makieta `Settings`, NScreen settings): po lewej sylwetka
 * profilu 240 dp, po prawej (szer. 400 od x 520) jedna lista NListRow `setting` (36 dp, tło `surface`) z nagłówkami grup.
 * Fokus startowy: „Profile”. Pozycje prowadzą do istniejących ekranów Nuvio i forka; przełączniki modułów NOOB zostają.
 */
@Composable
fun NiuniaSettingsNoob(items: List<NiuniaSettingsItem>, footer: @Composable () -> Unit) {
    val first = remember { FocusRequester() }
    com.nuvio.tv.skin.niunia.nav.NiuniaRegisterContentFocus(first)
    LaunchedEffect(Unit) {
        Log.i(TAG, "ustawienia NOOB: ${items.count { it is NiuniaSettingsItem.Row }} pozycji, fokus startowy → ${(items.firstOrNull { it is NiuniaSettingsItem.Row } as? NiuniaSettingsItem.Row)?.label}")
        androidx.compose.runtime.withFrameNanos { }
        try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "ustawienia NOOB: fokus: ${e.javaClass.simpleName}: ${e.message}") }
    }
    val firstRow = items.indexOfFirst { it is NiuniaSettingsItem.Row }
    Row(
        Modifier.fillMaxSize().background(NiuniaColor.Bg).padding(start = NiuniaSpacing.Space40, end = NiuniaSpacing.Space40),
        horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space40),
    ) {
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            NiuniaIcon("profile", size = NiuniaSize.Avatar240, tint = NiuniaColor.Fill24)
        }
        LazyColumn(
            Modifier.width(NiuniaSize.PanelW).fillMaxHeight(),
            contentPadding = PaddingValues(top = NiuniaSize.ContentTop, bottom = NiuniaSpacing.Space40),
            verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4),
        ) {
            items(items.size) { i ->
                when (val it = items[i]) {
                    is NiuniaSettingsItem.Header -> NiuniaListRow(it.title, onClick = null, kind = NiuniaRowKind.Header)
                    is NiuniaSettingsItem.Row -> NiuniaListRow(
                        it.label, kind = NiuniaRowKind.Setting, value = it.value,
                        trailing = if (it.chevron) NiuniaTrailing.Chevron else NiuniaTrailing.None,
                        focusRequester = if (i == firstRow) first else null,
                        onClick = { Log.i(TAG, "ustawienia NOOB: ${it.key}"); it.action() },
                    )
                }
            }
            item { footer() }
        }
    }
}

/** Przełącznik modułu forka jako wiersz z wartością Włączone/Wyłączone (OK zmienia od razu). */
@Composable
fun NiuniaSettingsToggle(key: String, title: String, default: Boolean) {
    val c = LocalContext.current
    var rev by remember { mutableIntStateOf(0) }
    val on = rev.let { ForkFeatures.enabled(c, key, default) }
    NiuniaListRow(title, kind = NiuniaRowKind.Setting, value = if (on) "Włączone" else "Wyłączone", onClick = {
        ForkFeatures.set(c, key, !on); rev++; Log.i(TAG, "ustawienia NOOB: $key = ${!on}")
    })
}
