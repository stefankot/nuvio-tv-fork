package com.nuvio.tv.fork.ai.subtitles

import com.nuvio.tv.domain.model.Subtitle
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.TrackInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy 22.11: kiedy „Napisy AI, gdy brak dobrych polskich” uznaje polskie napisy za złe. */
class AiAutoTest {
    private fun sub(lang: String) = Subtitle(id = lang, url = "https://s/$lang.srt", lang = lang, addonName = "OpenSubtitles", addonLogo = null)
    private fun track(lang: String?) = TrackInfo(index = 0, name = lang ?: "?", language = lang)

    @Test fun `polski kod w różnych zapisach`() {
        listOf("pl", "pol", "Polish", "pl-PL", "pl_PL", "polski").forEach { assertTrue(it, AiAuto.isPl(it)) }
        listOf(null, "en", "pt-BR", "por").forEach { assertFalse("$it", AiAuto.isPl(it)) }
    }

    @Test fun `brak polskich napisów`() {
        val s = PlayerUiState(addonSubtitles = listOf(sub("en")), subtitleTracks = listOf(track("ita")))
        assertEquals("brak polskich napisów", AiAuto.polishProblem(s))
    }

    @Test fun `polskie wbudowane albo z dodatku są dobre`() {
        assertNull(AiAuto.polishProblem(PlayerUiState(subtitleTracks = listOf(track("pol")))))
        assertNull(AiAuto.polishProblem(PlayerUiState(addonSubtitles = listOf(sub("pl")), selectedAddonSubtitle = sub("pl"))))
    }

    @Test fun `wybrany polski plik bez dopasowania AutoSync`() {
        val s = PlayerUiState(addonSubtitles = listOf(sub("pl")), selectedAddonSubtitle = sub("pl"), subtitleAutoSyncError = "nie pasuje")
        assertEquals("AutoSync nie dopasował polskiego pliku", AiAuto.polishProblem(s))
        // błąd AutoSync obcego pliku nie dotyczy polskich
        val t = PlayerUiState(addonSubtitles = listOf(sub("pl"), sub("en")), selectedAddonSubtitle = sub("en"), subtitleAutoSyncError = "x")
        assertNull(AiAuto.polishProblem(t))
    }
}
