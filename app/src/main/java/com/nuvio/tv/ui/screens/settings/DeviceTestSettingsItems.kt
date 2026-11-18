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
    // Wiersz usunięty 18.11: był martwy (onClick pusty); test prędkości jest w tej samej sekcji (connectionSpeedSettingsItems).
    // Test łącza KE (per usługa, limit łącza): plan/TERAZ.md W1.
}
