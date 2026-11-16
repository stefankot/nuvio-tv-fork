@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.livetv.bridge.KeCatalogViewModel

/** YouTube (Nuvio One) switch + open row. */
internal fun LazyListScope.youtubeSettingsItems() {
    item(key = "fork_youtube") {
        val context = LocalContext.current
        var on by remember { mutableStateOf(ForkFeatures.enabled(context, "fork_youtube")) }
        androidx.compose.foundation.layout.Column(
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(com.nuvio.tv.ui.theme.NuvioTheme.spacing.md)
        ) {
            ToggleSettingsItem(
                icon = Icons.Default.Language,
                title = "YouTube (Nuvio One)",
                subtitle = "Przeglądanie i odtwarzanie YouTube",
                isChecked = on,
                onCheckedChange = { on = it; ForkFeatures.set(context, "fork_youtube", it) },
            )
            if (on) NavigationSettingsItem(Icons.Default.Language, "Otwórz YouTube", "", onClick = { ForkFeatures.open?.invoke("youtube") })
            if (on) YouTubeSettingsContent()
        }
    }
}

/** KE catalog switches/rows, shown in the one fork settings category. */
internal fun LazyListScope.keCatalogSettingsItems() {
    item(key = "fork_ke_catalog") {
        val context = LocalContext.current
        var on by remember { mutableStateOf(ForkFeatures.enabled(context, "fork_ke_catalog")) }
        ToggleSettingsItem(
            icon = Icons.Default.Language,
            title = "Katalog IPTV (KE)",
            subtitle = "Kanały wg kraju, kategorii i języka z repozytorium KE + test łączy",
            isChecked = on,
            onCheckedChange = { on = it; ForkFeatures.set(context, "fork_ke_catalog", it) },
        )
    }
    item(key = "fork_ke_catalog_body") {
        val context = LocalContext.current
        if (!ForkFeatures.enabled(context, "fork_ke_catalog")) return@item
        val vm: KeCatalogViewModel = hiltViewModel()
        val ui by vm.ui.collectAsStateWithLifecycle()
        androidx.compose.foundation.layout.Column(
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(com.nuvio.tv.ui.theme.NuvioTheme.spacing.md)
        ) {
            NavigationSettingsItem(Icons.Default.Language, "Pobierz / odśwież katalog", ui.message, onClick = { vm.refresh() })
            NavigationSettingsItem(Icons.Default.Language, "Kategoria: ${ui.category ?: "wszystkie"}", "Kliknij, aby zmienić filtr", onClick = { vm.cycleCategory() })
            NavigationSettingsItem(Icons.Default.Language, "Język: ${ui.language ?: "wszystkie"}", "Kliknij, aby zmienić filtr", onClick = { vm.cycleLanguage() })
            ui.countries.forEach { (code, n) ->
                NavigationSettingsItem(Icons.Default.Language, "Dodaj kraj ${code?.uppercase() ?: "—"} ($n kanałów)", "Dodaje źródło do Live TV z filtrami powyżej", onClick = { vm.importCountry(code) })
                NavigationSettingsItem(Icons.Default.Language, "Test łączy ${code?.uppercase() ?: "—"}", "Sprawdza, które łącza działają na tym urządzeniu", onClick = { vm.startScan(code) })
            }
        }
    }
}
