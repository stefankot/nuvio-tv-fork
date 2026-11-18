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
            onCheckedChange = { on = it; ForkFeatures.set(context, key, it); com.nuvio.tv.fork.sources.KeRanking.warm(context) },
        )
    }
}

private fun LazyListScope.echoCycle(key: String, title: String, subtitle: String, values: List<Pair<String, String>>) {
    item(key = "fork_echo_cycle_$key") {
        val context = LocalContext.current
        var cur by remember { mutableStateOf(ForkFeatures.str(context, key)) }
        val label = values.firstOrNull { it.first == cur }?.second ?: values.first().second
        NavigationSettingsItem(
            Icons.Default.Tune, "$title: $label", subtitle,
            onClick = {
                val i = values.indexOfFirst { it.first == cur }.coerceAtLeast(0)
                cur = values[(i + 1) % values.size].first
                ForkFeatures.setStr(context, key, cur)
            }
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
    echoCycle("echo_ap_quality", "Auto-play: rozdzielczość", "Kliknij, aby zmienić: dowolna → 4K → 1080p → 720p (dotyczy trybu Auto-play first source)",
        listOf("" to "dowolna", "4k" to "4K", "1080p" to "1080p", "720p" to "720p"))
    echoCycle("echo_ap_codec", "Auto-play: kodek", "Kliknij, aby zmienić: dowolny → AVC (H.264) → HEVC (H.265)",
        listOf("" to "dowolny", "avc" to "AVC", "hevc" to "HEVC"))
    echoSwitch(com.nuvio.tv.fork.net.KeNetwork.KEY, "KE: limit łącza", "Źródła szybsze niż zmierzone łącze na koniec listy (test łącza: odtwarzacz → Więcej → Test łącza)")
    echoSwitch("echo_quality_menu", "Echo: przełączanie źródła w odtwarzaczu", "Menu jakości (przycisk pilota „Quality”) i zmiana źródła z zachowaniem pozycji")
    echoSwitch("echo_auto_quality", "Echo: Auto Quality", "Sam dobiera jakość do łącza (potrzebuje przełączania źródła)")
    echoSwitch("echo_hot_swap", "Echo: hot swap", "Zmiana jakości bez ekranu ładowania")
    echoSwitch(com.nuvio.tv.fork.sources.KeRanking.SAVER_KEY, "Oszczędzanie łącza", "Najmniejszy plik przy najlepszym kodeku, jaki telewizor dekoduje sprzętowo")
    echoSwitch("echo_race", "Echo: wyścig źródeł na starcie", "Startuje pierwsze źródło, które zacznie grać")
    echoSwitch(com.nuvio.tv.fork.sources.StartupChecks.KEY, "Kontrola otwartego pliku", "Zły czas trwania albo brak dźwięku = od razu następne źródło; źródła, które nie zadziałały, idą na koniec listy (30 min, zły plik 24 h)")
    echoSwitch(com.nuvio.tv.fork.sources.MpvContainers.KEY, "AVI/WMV w odtwarzaczu mpv", "Pliki, których wbudowany odtwarzacz nie czyta (AVI, WMV, ASF), same otwierają się w mpv")
    item(key = "fork_bad_sources_clear") {
        var count by remember { mutableStateOf(com.nuvio.tv.fork.sources.StartupChecks.badCount()) }
        NavigationSettingsItem(
            Icons.Default.Tune, "Zapomnij odłożone źródła ($count)", "Źródła odłożone przez kontrolę pliku wracają na zwykłe miejsce",
            onClick = { com.nuvio.tv.fork.sources.StartupChecks.clearAll(); count = com.nuvio.tv.fork.sources.StartupChecks.badCount() }
        )
    }
    echoSwitch("echo_source_fallback", "Echo: następne źródło po błędzie", "Własny mechanizm Echo obok mechanizmu Reshaped")
    echoSwitch("echo_audio_menu", "Echo: menu języka audio", "Języki ze wszystkich źródeł")
    echoSwitch("echo_playback_intro", "Echo: intro przed odtwarzaniem", "Krótkie animowane intro")
}
