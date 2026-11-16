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
    echoSwitch("echo_quality_menu", "Echo: przełączanie źródła w odtwarzaczu", "Menu jakości (przycisk pilota „Quality”) i zmiana źródła z zachowaniem pozycji")
    echoSwitch("echo_auto_quality", "Echo: Auto Quality", "Sam dobiera jakość do łącza (potrzebuje przełączania źródła)")
    echoSwitch("echo_hot_swap", "Echo: hot swap", "Zmiana jakości bez ekranu ładowania")
    echoSwitch("echo_race", "Echo: wyścig źródeł na starcie", "Startuje pierwsze źródło, które zacznie grać")
    echoSwitch("echo_source_fallback", "Echo: następne źródło po błędzie", "Własny mechanizm Echo obok mechanizmu Reshaped")
    echoSwitch("echo_audio_menu", "Echo: menu języka audio", "Języki ze wszystkich źródeł")
    echoSwitch("echo_playback_intro", "Echo: intro przed odtwarzaniem", "Krótkie animowane intro")
}
