@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language

/**
 * B: test łącza i raport sprzętu. Gotowe w bazie (`AdvancedSettingsContent`: test prędkości,
 * karta diagnostyki). Nie kopiujemy KE; ten wiersz tylko wskazuje, gdzie to jest.
 */
internal fun LazyListScope.deviceTestSettingsItems() {
    item(key = "fork_device_test") {
        NavigationSettingsItem(
            Icons.Default.Language,
            "Test łącza i raport sprzętu",
            "Ustawienia → Zaawansowane (Advanced): test prędkości połączenia i raport o sprzęcie",
            onClick = { }
        )
    }
}
