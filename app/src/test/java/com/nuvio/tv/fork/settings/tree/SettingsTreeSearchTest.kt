package com.nuvio.tv.fork.settings.tree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Szukanie w ustawieniach (23.11). Dawne szukanie znało tylko angielskie nazwy kategorii Nuvio: „napisy”, „pilot”, „lektor”
 * nic nie znajdowały. Tu każde polskie słowo prowadzi do właściwej pozycji drzewa (także ukrytej w trybie prostym i pod Więcej).
 */
class SettingsTreeSearchTest {
    private val index = SettingsTreeSearch.index(SettingsTreeSpecTest.full)
    private fun ids(q: String) = SettingsTreeSearch.search(index, q).map { it.item.id }
    private fun first(q: String) = SettingsTreeSearch.search(index, q).firstOrNull()

    private fun finds(q: String, id: String, top: Int = 5) {
        val r = ids(q)
        assertTrue("„$q” → $id w pierwszych $top, jest: ${r.take(top)}", id in r.take(top))
    }

    @Test fun `polskie słowa trafiają w pozycje`() {
        finds("napisy", "subtitles", top = 3)
        finds("rozmiar napisów", "subtitle_size", top = 1)
        finds("lektor", "lektor", top = 1)
        finds("pilot", "remote")
        finds("tryb nocny", "night_mode", top = 1)
        finds("dźwięk", "audio")
        finds("kraj", "m:tv:a:Zmień kraj IPTV")
        finds("następny odcinek", "up_next", top = 1)
        finds("czołówki", "skip", top = 1)
        finds("skórka", "skin", top = 1)
        finds("profile", "profiles", top = 1)
        finds("dodatki", "addons", top = 1)
        finds("aktualizacje", "about")
        finds("napisy ai", "m:ai_subtitles:a:AI napisy: klucz, model, limity, zapisane", top = 3)
    }

    @Test fun `bez polskich znaków i wielkich liter`() {
        assertEquals(ids("dźwięk"), ids("dzwiek"))
        assertEquals(ids("Czołówki"), ids("czolowki"))
        finds("jezyk aplikacji", "theme")
    }

    @Test fun `angielskie i techniczne słowa też działają`() {
        finds("hdr", "video")
        finds("dolby", "formats")
        finds("trakt", "tracking", top = 1)
        finds("whisper", "asr")
        finds("m3u", "tv_sources")
        finds("bufor", "buffer")
    }

    @Test fun `nazwa wiersza z wnętrza ekranu prowadzi do jego pozycji z opisem „zawiera”`() {
        val h = SettingsTreeSearch.search(index, "tunelowane").first { it.item.id == "audio" }
        assertEquals("Tunelowane odtwarzanie", h.inner)
        assertTrue(SettingsTreeSearch.subtitle(h).contains("zawiera: Tunelowane odtwarzanie"))
        finds("amoled", "theme")
        finds("debrid", "services")
    }

    @Test fun `ukryte w trybie prostym i pod Więcej też są znajdowane, z dopiskiem Więcej`() {
        val h = first("torrenty")!!
        assertEquals("p2p", h.item.id)
        assertEquals(TreeLevel.MORE, h.item.level)
        assertTrue(SettingsTreeSearch.subtitle(h).startsWith("Oglądanie › Włączanie filmu › Więcej"))
    }

    @Test fun `puste i bezsensowne zapytanie nic nie zwraca, wyników najwyżej LIMIT`() {
        assertTrue(ids("").isEmpty())
        assertTrue(ids("   ").isEmpty())
        assertTrue(ids("qqxzv").isEmpty())
        assertTrue(ids("a").size <= SettingsTreeSearch.LIMIT)
    }
}
