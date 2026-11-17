package com.nuvio.tv.fork.tv.ekrany

import com.nuvio.tv.fork.tv.Badge
import com.nuvio.tv.fork.tv.EpisodeAlert
import com.nuvio.tv.fork.tv.EpisodeAlerts
import com.nuvio.tv.fork.tv.QueueItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenStateTest {
    @Test fun `rzad chowa sie w siatce i wraca z pierwszego wiersza`() {
        val s = GuideHeaderState()
        assertEquals(Zone.ROW, s.downFromNavBar(true)); assertTrue(s.rowVisible)
        s.focusGrid(0); assertFalse(s.rowVisible)
        s.down(); assertEquals(1, s.gridRow); assertEquals(Zone.GRID, s.up()); assertFalse(s.rowVisible)
        assertEquals(Zone.ROW, s.up()); assertTrue(s.rowVisible)
        assertEquals(Zone.CONTROLS, GuideHeaderState().downFromNavBar(false))
    }

    @Test fun `OK na karcie i menu`() {
        val item = QueueItem("p", "P", "ch", 0, Badge.RESUME)
        assertTrue(CardActions.onOk(item, 10, true) is CardAction.Resume)
        assertTrue(CardActions.onOk(item.copy(badge = Badge.NEW_EPISODE, startMs = 1), 10, true) is CardAction.PlayFromStart)
        assertTrue(CardActions.onOk(item.copy(badge = Badge.NEW_EPISODE, startMs = 1), 10, false) is CardAction.PlayLive)
        assertTrue(CardActions.onOk(item.copy(badge = Badge.LIVE), 10, true) is CardAction.PlayLive)
        assertFalse(CardMenu.FROM_START in CardActions.menu(false)); assertTrue(CardMenu.FROM_START in CardActions.menu(true))
    }

    @Test fun `domyslny filtr i sortowanie`() {
        assertEquals(ChannelFilter.Favourites, ChannelFilters.default(5)); assertEquals(ChannelFilter.All, ChannelFilters.default(4))
        val e = ChannelFilters.entries(5, 3, 2, 100, mapOf("Sport" to 10, "Filmy" to 30, "Puste" to 0, "Ukryta" to 50), setOf("Ukryta"))
        assertEquals(listOf("Ulubione", "Twoje kanały", "Ostatnie", "Wszystkie", "Filmy", "Sport"), e.map { it.title })
        val ordered = ChannelFilters.order(listOf(ChannelRef("a", "g", false), ChannelRef("b", "g", true), ChannelRef("c", "g", true)), listOf("c", "b"))
        assertEquals(listOf("c", "b", "a"), ordered.map { it.key })
    }

    @Test fun `automat z opoznieniem`() {
        var s = DelayState()
        assertEquals(LiveMode.LIVE, DelayedPlayback.pauseOrRewind(s, false).mode)
        s = DelayedPlayback.pauseOrRewind(s, true, 12 * 60_000L)
        assertEquals("−12 min", DelayedPlayback.label(s))
        s = DelayedPlayback.seek(s, 30_000L); assertEquals(12 * 60_000L - 30_000, s.behindMs)
        s = DelayedPlayback.seek(s, 12 * 60_000L); assertEquals(LiveMode.LIVE, s.mode); assertNull(DelayedPlayback.label(s))
        assertEquals(listOf(10_000L, 30_000L, 60_000L, 120_000L), listOf(0, 3, 8, 16).map(DelayedPlayback::stepMs))
    }

    @Test fun `alert ogłaszany raz`() {
        val a = EpisodeAlert("id1", "s", "S", com.nuvio.tv.fork.tv.AlertKind.NEW_EPISODE, com.nuvio.tv.fork.tv.Airing("k", "S", 1, 2))
        assertEquals(listOf(a), EpisodeAlerts.fresh(listOf(a), emptySet())); assertTrue(EpisodeAlerts.fresh(listOf(a), setOf("id1")).isEmpty())
    }
}
