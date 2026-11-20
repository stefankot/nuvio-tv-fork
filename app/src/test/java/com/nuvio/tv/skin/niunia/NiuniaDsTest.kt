package com.nuvio.tv.skin.niunia

import com.nuvio.tv.skin.niunia.components.NiuniaCardPreset
import com.nuvio.tv.skin.niunia.components.NiuniaCardState
import com.nuvio.tv.skin.niunia.components.NiuniaCardStatus
import com.nuvio.tv.skin.niunia.components.NiuniaDir
import com.nuvio.tv.skin.niunia.components.NiuniaFocusStyle
import com.nuvio.tv.skin.niunia.components.NiuniaScreens
import com.nuvio.tv.skin.niunia.components.NiuniaSeekState
import com.nuvio.tv.skin.niunia.components.NiuniaSeekTone
import com.nuvio.tv.skin.niunia.components.niuniaCardParts
import com.nuvio.tv.skin.niunia.components.niuniaEpgCell
import com.nuvio.tv.skin.niunia.components.niuniaEpgNowX
import com.nuvio.tv.skin.niunia.components.niuniaRichText
import com.nuvio.tv.skin.niunia.components.niuniaTime
import com.nuvio.tv.skin.niunia.detail.niuniaDate
import com.nuvio.tv.skin.niunia.home.niuniaRuntime
import com.nuvio.tv.skin.niunia.player.niuniaQualityGlyph
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaIconPaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Logika komponentów design systemu w skórce Niunia (paczka 1): presety kart, pasek postępu, EPG, mapy fokusu. */
class NiuniaDsTest {

    // ------------------------------------------------------------------ NMediaCard

    @Test fun `rozmiary presetow kart`() {
        assertEquals(130f to 195f, NiuniaCardPreset.PosterCard.let { it.width.value to it.height.value })
        assertEquals(160f to 114f, NiuniaCardPreset.ContinueWatchingCard.let { it.width.value to it.height.value })
        assertEquals(205f to 115f, NiuniaCardPreset.StillCard.let { it.width.value to it.height.value })
        assertEquals(280f to 350f, NiuniaCardPreset.SpotlightCard.let { it.width.value to it.height.value })
        assertEquals(280f to 80f, NiuniaCardPreset.ResultCard.let { it.width.value to it.height.value })
        assertEquals(72f, NiuniaCardPreset.ProfileCard.width.value)
    }

    @Test fun `styl fokusu presetow`() {
        assertEquals(NiuniaFocusStyle.Lift, NiuniaCardPreset.PosterCard.focusStyle)
        assertEquals(NiuniaFocusStyle.Lift, NiuniaCardPreset.StillCard.focusStyle)
        assertEquals(NiuniaFocusStyle.Fill, NiuniaCardPreset.ResultCard.focusStyle)
        assertEquals(NiuniaFocusStyle.Fill, NiuniaCardPreset.ChapterCard.focusStyle)
        assertEquals(NiuniaFocusStyle.Button, NiuniaCardPreset.ProfileCard.focusStyle)
        assertEquals(NiuniaCardPreset.PosterCard, NiuniaCardPreset.of("PosterCard"))
        assertNull(NiuniaCardPreset.of("Nieznany"))
    }

    @Test fun `karta oglądaj dalej - play postep podpis plakietka kropka`() {
        val p = niuniaCardParts(NiuniaCardPreset.ContinueWatchingCard, caption = "S1, E1 · 39m", progress = 40, play = true, badge = "Nowy", status = NiuniaCardStatus.Live)
        assertTrue(p.playIcon); assertEquals(0.4f, p.progress!!, 0.0001f); assertEquals("S1, E1 · 39m", p.caption)
        assertEquals("Nowy", p.badge); assertEquals(NiuniaCardStatus.Live, p.statusDot)
        assertTrue(p.hasCaptionBand); assertTrue(p.hasTopRow); assertFalse(p.posterFoot)
    }

    @Test fun `postep przyciety do zakresu`() {
        assertEquals(1f, niuniaCardParts(NiuniaCardPreset.StillCard, progress = 140).progress!!, 0f)
        assertEquals(0f, niuniaCardParts(NiuniaCardPreset.StillCard, progress = -5).progress!!, 0f)
    }

    @Test fun `plakat ma stopke a nie pasek podpisu`() {
        val p = niuniaCardParts(NiuniaCardPreset.PosterCard, caption = "x", progress = 10, play = true)
        assertTrue(p.posterFoot); assertFalse(p.hasCaptionBand)
    }

    @Test fun `stany ladowania bledu i pustki`() {
        val loading = niuniaCardParts(NiuniaCardPreset.PosterCard, NiuniaCardState.Loading, title = "A", badge = "B")
        assertTrue(loading.shimmer); assertNull(loading.badge); assertFalse(loading.posterFoot)
        val error = niuniaCardParts(NiuniaCardPreset.StillCard, NiuniaCardState.Error, title = "Odyseja", caption = "c")
        assertEquals("Odyseja", error.fallbackLabel); assertNull(error.caption)
        assertTrue(niuniaCardParts(NiuniaCardPreset.LandscapeCard, NiuniaCardState.Empty).emptyText)
        assertNull(niuniaCardParts(NiuniaCardPreset.StillCard, badge = " ").badge)
    }

    // ------------------------------------------------------------------ NInput seek

    @Test fun `pasek na zywo czerwony bez bufora`() {
        val s = NiuniaSeekState(0.5f, NiuniaSeekTone.Live, buffer = 0.8f, edge = 0.9f)
        assertEquals(NiuniaColor.Live, s.fillColor); assertNull(s.bufferShown); assertNull(s.edgeShown)
    }

    @Test fun `pasek z opoznieniem zolty z buforem i krawedzia`() {
        val s = NiuniaSeekState(0.4f, NiuniaSeekTone.Delay, buffer = 0.95f, edge = 0.9f)
        assertEquals(NiuniaColor.Delay, s.fillColor)
        assertEquals(0.9f, s.bufferShown!!, 0f) // bufor nie wychodzi za krawędź „na żywo”
        assertEquals(0.9f, s.edgeShown!!, 0f)
    }

    @Test fun `pasek archiwum i glowny bialy`() {
        assertEquals(NiuniaColor.White, NiuniaSeekState(0.3f, NiuniaSeekTone.Archive).fillColor)
        assertEquals(NiuniaColor.White, NiuniaSeekState(0.3f).fillColor)
        assertNull(NiuniaSeekState(0.3f, buffer = 0.2f).bufferShown) // bufor za pozycją — niewidoczny
        assertEquals(1f, NiuniaSeekState(1.4f).fill, 0f)
        assertEquals(0.25f, NiuniaSeekState.of(30_000, 120_000).fill, 0.0001f)
        assertEquals(0f, NiuniaSeekState.of(5, 0).fill, 0f)
    }

    @Test fun `rozdzialy bez krawedzi i duplikatow`() {
        assertEquals(listOf(0.25f, 0.5f), NiuniaSeekState(0f, chapters = listOf(0.5f, 0f, 0.25f, 1f, 0.5f)).chapterGaps)
    }

    @Test fun `czas na pasku`() {
        assertEquals("52:10", niuniaTime(3_130_000))
        assertEquals("−2:00:50", niuniaTime(7_250_000, remaining = true))
        assertEquals("0:00", niuniaTime(-5))
    }

    // ------------------------------------------------------------------ NEpgGrid

    @Test fun `komorka EPG z czasow programu`() {
        val c = niuniaEpgCell(fromMin = 30f, toMin = 90f, windowStartMin = 0f)!!
        assertEquals(220f, c.x.value, 0.001f)           // 30 min = epg-slot-w
        assertEquals(440f - 4f, c.width.value, 0.001f)  // 60 min minus odstęp 4
        assertFalse(c.clipped)
    }

    @Test fun `komorka przycieta na poczatku okna`() {
        val c = niuniaEpgCell(fromMin = -20f, toMin = 15f, windowStartMin = 0f)!!
        assertEquals(0f, c.x.value, 0f); assertEquals(110f - 4f, c.width.value, 0.001f); assertTrue(c.clipped)
        assertNull(niuniaEpgCell(-60f, -10f, 0f)) // przed oknem — poza siatką
    }

    @Test fun `linia teraz`() {
        assertEquals(110f, niuniaEpgNowX(nowMin = 15f, windowStartMin = 0f).value, 0.001f)
    }

    // ------------------------------------------------------------------ NScreen: mapy fokusu (paczka 1)

    @Test fun `fokus startowy z makiety`() {
        assertEquals("play", NiuniaScreens.Main.initial)
        assertEquals("cw0", NiuniaScreens.HomeRows.initial)
        assertEquals("home", NiuniaScreens.Sidebar.initial)
        assertEquals("resume", NiuniaScreens.Detail.initial)
        assertEquals("seek", NiuniaScreens.Player.initial)
        assertEquals("volume", NiuniaScreens.PlayerPanel.initial)
    }

    @Test fun `bez zawijania na krawedziach`() {
        NiuniaScreens.batch1.forEach { map ->
            map.rows.forEach { row ->
                assertEquals("${map.screen}: ◄ z ${row.first()}", row.first(), map.move(row.first(), NiuniaDir.Left))
                assertEquals("${map.screen}: ► z ${row.last()}", row.last(), map.move(row.last(), NiuniaDir.Right))
            }
            map.rows.first().forEach { assertEquals("${map.screen}: ▲ z $it", it, map.move(it, NiuniaDir.Up)) }
            map.rows.last().forEach { assertEquals("${map.screen}: ▼ z $it", it, map.move(it, NiuniaDir.Down)) }
        }
    }

    @Test fun `kazdy element osiagalny ze startu`() {
        NiuniaScreens.batch1.forEach { map ->
            val seen = mutableSetOf(map.initial); var frontier = listOf(map.initial)
            while (frontier.isNotEmpty()) {
                frontier = frontier.flatMap { id -> NiuniaDir.entries.map { map.move(id, it) } }.filter { seen.add(it) }
            }
            assertEquals("${map.screen}: nieosiągalne ${map.all - seen}", map.all.toSet(), seen)
        }
    }

    @Test fun `cele OK dlugiego OK i Wstecz`() {
        assertEquals("Player", NiuniaScreens.Main.okTarget("play"))
        assertEquals("Movie", NiuniaScreens.Main.longOkTarget("play"))
        assertEquals("Sidebar", NiuniaScreens.Main.okTarget("menu"))
        assertNull(NiuniaScreens.Main.back)
        assertEquals("Main", NiuniaScreens.HomeRows.back)
        assertEquals("Profiles", NiuniaScreens.Sidebar.okTarget("account"))
        assertEquals("PlayerLeanback", NiuniaScreens.Sidebar.okTarget("youtube"))
        assertEquals("DetailMore", NiuniaScreens.Detail.okTarget("about"))
        assertEquals("HomeRows", NiuniaScreens.Detail.back)
        assertEquals("PlayerFilmInfo", NiuniaScreens.Player.longOkTarget("seek"))
        assertEquals("PlayerSubs", NiuniaScreens.Player.okTarget("subtitles"))
        assertEquals("Player", NiuniaScreens.PlayerPanel.back)
    }

    @Test fun `ruch w mapie odtwarzacza`() {
        val p = NiuniaScreens.Player
        assertEquals("subtitles", p.move("seek", NiuniaDir.Up))
        assertEquals("similar", p.move("seek", NiuniaDir.Down))
        assertEquals("seek", p.move("audio", NiuniaDir.Down))
        val m = NiuniaScreens.Main.copy(disabled = setOf("library"))
        assertEquals("info", m.move("play", NiuniaDir.Right)) // wyłączony pomijany
    }

    // ------------------------------------------------------------------ drobne funkcje ekranów

    @Test fun `znak jakosci z wysokosci obrazu albo nazwy strumienia`() {
        assertEquals("UHD", niuniaQualityGlyph(2160, null))
        assertEquals("HD", niuniaQualityGlyph(1080, "4K HDR"))
        assertEquals("SD", niuniaQualityGlyph(480, null))
        assertEquals("UHD", niuniaQualityGlyph(null, "Torrentio 4K DV"))
        assertEquals("HD", niuniaQualityGlyph(0, "WEB-DL 720p"))
        assertNull(niuniaQualityGlyph(null, "Nieznane"))
    }

    @Test fun `data po polsku`() {
        assertEquals("2 wrz 2022", niuniaDate("2022-09-02T00:00:00Z"))
        assertNull(niuniaDate("2022"))
        assertNull(niuniaDate(null))
    }

    @Test fun `dlugosc filmu z dodatku`() {
        assertEquals(2 to 28, niuniaRuntime("148"))
        assertEquals(0 to 45, niuniaRuntime("45 min"))
        assertEquals(2 to 5, niuniaRuntime("2h 5min"))
        assertNull(niuniaRuntime("około dwóch godzin"))
        assertNull(niuniaRuntime(null))
    }

    @Test fun `pogrubienie z gwiazdek`() {
        val t = niuniaRichText("**S1, E1 · Cień:** opis")
        assertEquals("S1, E1 · Cień: opis", t.text)
        assertEquals(1, t.spanStyles.size)
        assertEquals("a **b", niuniaRichText("a **b").text) // niedomknięte — zostaje jak jest
    }

    @Test fun `ikony z makiety sa w zestawie`() {
        listOf("play", "plus", "info", "chevron-right", "check", "about", "subtitles", "audio", "search", "home", "tv", "youtube", "library", "settings", "star", "play-small", "chevron-left")
            .forEach { assertTrue("brak ikony $it", NiuniaIconPaths.all.containsKey(it)) }
    }
}
