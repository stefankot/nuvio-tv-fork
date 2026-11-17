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
import com.nuvio.tv.fork.registry.ForkRegistry

/** Główny wyłącznik każdego modułu i pozycje ustawień modułów (wspólna kategoria forka). */
internal fun LazyListScope.forkModuleSettingsItems() {
    item(key = "fork_module_switches") {
        val context = LocalContext.current
        val reg = remember(context) { ForkRegistry.forContext(context) }
        reg.allModules().forEach { m ->
            val k = ForkRegistry.switchKey(m.id)
            var on by remember { mutableStateOf(ForkFeatures.enabled(context, k, m.defaultEnabled)) }
            ToggleSettingsItem(
                icon = Icons.Default.Tune, title = m.title, subtitle = "", isChecked = on,
                onCheckedChange = { on = it; ForkFeatures.set(context, k, it) },
            )
            if (on) m.actions().forEach { a ->
                SettingsActionRow(title = a.title, subtitle = a.description, onClick = { a.run(context) })
            }
            if (on) m.settings().forEach { s ->
                var v by remember { mutableStateOf(ForkFeatures.enabled(context, s.key, s.defaultValue)) }
                ToggleSettingsItem(
                    icon = Icons.Default.Tune, title = s.title, subtitle = s.description, isChecked = v,
                    onCheckedChange = { v = it; ForkFeatures.set(context, s.key, it) },
                )
            }
        }
    }
}
