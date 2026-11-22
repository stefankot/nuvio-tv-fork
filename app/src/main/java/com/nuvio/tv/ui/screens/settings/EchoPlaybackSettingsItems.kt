@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private fun LazyListScope.echoSwitch(key: String, title: String, subtitle: String) {
    item(key = "fork_echo_$key") {
        val context = LocalContext.current
        var on by remember { mutableStateOf(ForkFeatures.enabled(context, key, default = false)) }
        ToggleSettingsItem(
            icon = Icons.Default.Tune, title = title, subtitle = subtitle, isChecked = on,
            onCheckedChange = { on = it; ForkFeatures.set(context, key, it) },
        )
    }
}

/** Echo: jakość, przełączanie źródeł i menu języka audio. Wszystko domyślnie WYŁĄCZONE (nieprzetestowane). Działa od następnego odtwarzania. */
internal fun LazyListScope.echoPlaybackSettingsItems() {
    item(key = "fork_detail_preload") {
        val context = LocalContext.current
        var on by remember { mutableStateOf(ForkFeatures.enabled(context, "fork_detail_preload", default = true)) }
        ToggleSettingsItem(
            icon = Icons.Default.Tune, title = "Preloading źródeł na ekranie szczegółów",
            subtitle = "Wyszukuje źródła w tle, gdy otwierasz film lub serial, żeby Play startował szybciej",
            isChecked = on, onCheckedChange = { on = it; ForkFeatures.set(context, "fork_detail_preload", it) },
        )
    }
    echoSwitch(com.nuvio.tv.fork.echo.FastStart.KEY, "Echo: szybszy start", "Odtwarzanie rusza po 1,5 s bufora zamiast 5 s (gdy bufor startowy nie był zmieniany ręcznie)")
    echoSwitch("echo_quality_menu", "Echo: przełączanie źródła w odtwarzaczu", "Menu jakości (przycisk pilota „Quality”) i zmiana źródła z zachowaniem pozycji")
    echoSwitch("echo_auto_quality", "Echo: Auto Quality", "Sam dobiera jakość do łącza (potrzebuje przełączania źródła)")
    echoSwitch("echo_hot_swap", "Echo: hot swap", "Zmiana jakości bez ekranu ładowania")
    echoSwitch(com.nuvio.tv.fork.sources.MpvContainers.KEY, "AVI/WMV w odtwarzaczu mpv", "Pliki, których wbudowany odtwarzacz nie czyta (AVI, WMV, ASF), same otwierają się w mpv")
    echoSwitch("echo_audio_menu", "Echo: menu języka audio", "Języki ze wszystkich źródeł")
    // intro Echo usunięte (właściciel 19.11)
    // Właściciel 22.11: wybór źródła tylko natywny (Regex Nuvio) — bez przełączników rankingu, wyścigu, kontroli pliku i limitu łącza.
}
