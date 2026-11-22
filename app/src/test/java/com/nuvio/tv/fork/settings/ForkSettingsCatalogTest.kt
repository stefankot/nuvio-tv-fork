package com.nuvio.tv.fork.settings

import com.nuvio.tv.ui.screens.settings.SettingsCategory
import com.nuvio.tv.ui.screens.settings.visibleSettingsCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Test 22.11: kategorie ustawień z kategoriami forka (zastępuje wykluczony test bazy — fork/test/wykluczone-testy-bazy.txt). */
class ForkSettingsCatalogTest {
    @Test fun `kolejność kategorii z kategoriami forka`() {
        assertEquals(
            listOf(
                SettingsCategory.ACCOUNT, SettingsCategory.PROFILES, SettingsCategory.APPEARANCE, SettingsCategory.NIGHT_MODE,
                SettingsCategory.LAYOUT, SettingsCategory.CONTENT_DISCOVERY, SettingsCategory.PLAYBACK, SettingsCategory.NUVIO_RESHAPED,
                SettingsCategory.YOUTUBE, SettingsCategory.FORGE, SettingsCategory.INTEGRATION, SettingsCategory.TRACKING,
                SettingsCategory.ADVANCED, SettingsCategory.ABOUT, SettingsCategory.DEBUG,
            ),
            visibleSettingsCategories(isPrimaryProfile = true, isEssentialMode = false, isDebugBuild = true),
        )
    }

    @Test fun `kategorie forka widać też w profilu dodatkowym i w trybie podstawowym`() {
        val c = visibleSettingsCategories(isPrimaryProfile = false, isEssentialMode = true, isDebugBuild = true)
        assertTrue(c.containsAll(listOf(SettingsCategory.NIGHT_MODE, SettingsCategory.YOUTUBE, SettingsCategory.FORGE)))
        assertFalse(SettingsCategory.ACCOUNT in c)
        assertFalse("debug ukryty w trybie podstawowym", SettingsCategory.DEBUG in c)
    }
}
