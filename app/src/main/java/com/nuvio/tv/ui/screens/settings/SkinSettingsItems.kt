@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.skin.api.SkinId
import com.nuvio.tv.skin.api.SkinPreferences
import com.nuvio.tv.skin.api.rememberSkinId

/** Wygląd: Nuvio / Netflix / Mój projekt. Klik przełącza po kolei. */
internal fun LazyListScope.skinSettingsItems() {
    item(key = "fork_skin") {
        val context = LocalContext.current
        val current = rememberSkinId()
        val name = when (current) { SkinId.NUVIO -> "Nuvio"; SkinId.NETFLIX -> "Netflix"; SkinId.KIT -> "Mój projekt"; SkinId.KICIA -> "Kicia"; SkinId.NIUNIA -> "Niunia" }
        NavigationSettingsItem(
            Icons.Default.Palette,
            "Wygląd: $name",
            "Kliknij, aby zmienić: Nuvio → Mój projekt → Kicia → Niunia",
            onClick = { val order = SkinId.entries - SkinId.NETFLIX; SkinPreferences.set(context, order[(order.indexOf(current) + 1) % order.size]) } // Fork 26.11: wygląd Netflix z Reshaped usunięty razem z bazą 1.2.0-beta.4 (pliki bazy 1:1)
        )
    }
}
