package com.nuvio.tv.fork.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.DrawerItem
import com.nuvio.tv.fork.registry.ForkRegistry
import com.nuvio.tv.ui.screens.settings.ForkFeatures

const val TOOLS_ROUTE = "fork_tools"

/** Pozycja menu „Narzędzia” (skróty do funkcji forka, także w układzie Home Modern, gdzie nie ma miejsca na rzędy modułów). */
@Composable
fun rememberToolsMenuItem(): DrawerItem? {
    val context = LocalContext.current
    return remember { if (ForkFeatures.enabled(context, "fork_tools_menu", default = false) /* 19.11: pozycje rozmieszczone w aplikacji (Szukaj, Live TV, menu profilu, Ustawienia → Moduły) */) DrawerItem(route = TOOLS_ROUTE, label = "Narzędzia", icon = Icons.Default.Apps) else null }
}

/** Hub: Szukaj w TV, Odkrywaj, nastroje, polecenia głosowe, moduły; tylko włączone moduły. */
@Composable
fun ToolsHubScreen() {
    val c = LocalContext.current
    val reg = remember { ForkRegistry.forContext(c) }
    val ids = remember { reg.moduleIds().filter { ForkFeatures.enabled(c, ForkRegistry.switchKey(it), reg.allModules().first { m -> m.id == it }.defaultEnabled) }.toSet() }
    val entries = remember {
        buildList {
            if ("discover" in ids) { add("Szukaj w telewizji" to "fork_tv_search"); add("Odkrywaj telewizję (rzędy)" to "fork_discover") }
            if ("picks" in ids) add("Na co masz ochotę?" to "fork_picks")
            if ("voice" in ids) add("Sterowanie głosem: przykłady poleceń" to com.nuvio.tv.fork.voice.VOICE_HELP_ROUTE)
            if ("launcher" in ids) add("Aplikacje" to com.nuvio.tv.fork.launcher.LauncherModule.APPS_ROUTE)
            add("Moduły (wszystkie ustawienia)" to MODULES_ROUTE)
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Narzędzia", style = MaterialTheme.typography.headlineMedium) }
        entries.forEach { (title, route) -> item(key = route) { Button(onClick = { ForkFeatures.open?.invoke(route) }) { Text(title) } } }
    }
}
