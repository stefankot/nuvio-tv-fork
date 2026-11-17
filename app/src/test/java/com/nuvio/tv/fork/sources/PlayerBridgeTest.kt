package com.nuvio.tv.fork.sources

import com.nuvio.tv.domain.model.Subtitle
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerUiState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerBridgeTest {
    @After fun reset() = PlayerBridge.clear()

    private fun sub(id: String, lang: String = "pol") = Subtitle(id = id, url = "u$id", lang = lang, addonName = "A$id", addonLogo = null)

    @Test fun `bez odtwarzacza brak komunikatu`() {
        PlayerBridge.clear()
        assertNull(PlayerBridge.subtitleMismatch())
    }

    @Test fun `napisy nie pasuja - kolejne pliki w tym samym jezyku i propozycja po trzech`() {
        val subs = listOf(sub("1"), sub("2"), sub("3"), sub("x", "eng"))
        val events = ArrayList<PlayerEvent>()
        var state = PlayerUiState(addonSubtitles = subs, selectedAddonSubtitle = subs[0])
        PlayerBridge.set(state) { events += it }
        val m1 = PlayerBridge.subtitleMismatch()
        assertTrue(m1, m1!!.startsWith("Inne napisy"))
        val selected = events.filterIsInstance<PlayerEvent.OnSelectAddonSubtitle>().single().subtitle
        assertEquals("pol", selected.lang)
        assertTrue(selected.id != "1")
        assertTrue(events.any { it is PlayerEvent.OnReloadSubtitleAutoSyncCues })
        PlayerBridge.subtitleMismatch()
        val third = PlayerBridge.subtitleMismatch()
        assertTrue(third, third!!.contains("wyłączyć"))
    }

    @Test fun `brak innych napisow`() {
        PlayerBridge.set(PlayerUiState(addonSubtitles = emptyList())) { }
        assertEquals("Brak innych napisów", PlayerBridge.subtitleMismatch())
    }
}
