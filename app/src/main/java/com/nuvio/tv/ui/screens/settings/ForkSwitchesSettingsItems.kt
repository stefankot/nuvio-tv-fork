@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector

private fun LazyListScope.forkSwitch(key: String, icon: ImageVector, title: String, subtitle: String) {
    item(key = "fork_switch_$key") {
        val context = LocalContext.current
        var on by remember { mutableStateOf(ForkFeatures.enabled(context, key, default = true)) }
        ToggleSettingsItem(
            icon = icon, title = title, subtitle = subtitle, isChecked = on,
            onCheckedChange = { on = it; ForkFeatures.set(context, key, it) },
        )
    }
}

/** Przełączniki funkcji forka (jedna wspólna kategoria). Zmiany działają po ponownym otwarciu ekranu. */
internal fun LazyListScope.forkSwitchesSettingsItems() {
    forkSwitch("fork_calendar", Icons.Default.CalendarMonth, "Kalendarz (Echo)", "Pozycja Kalendarz w menu")
    forkSwitch("fork_settings_search", Icons.Default.Search, "Wyszukiwarka ustawień", "Przycisk wyszukiwania w Ustawieniach")
    forkSwitch("fork_arc_remote", Icons.Default.Tune, "Pilot Arc poza odtwarzaczem", "Przyciski przypisane do „Nic” są wyciszone w całej aplikacji")
}
