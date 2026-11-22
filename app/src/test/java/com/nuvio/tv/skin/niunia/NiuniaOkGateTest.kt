package com.nuvio.tv.skin.niunia

import com.nuvio.tv.skin.niunia.components.NiuniaOkGate
import com.nuvio.tv.skin.niunia.components.NiuniaOkGate.Down
import com.nuvio.tv.skin.niunia.components.NiuniaOkGate.Up
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** UX 22.11: OK / długie OK Niuni (NiuniaOkGate). */
class NiuniaOkGateTest {

    @Test fun shortPressClicks() {
        val g = NiuniaOkGate()
        assertEquals(Down.PRESS, g.down(1000, 0, systemLong = false, hasLong = true))
        assertEquals(Up.CLICK, g.up())
    }

    @Test fun timerFiresLongAndReleaseDoesNotClick() {
        val g = NiuniaOkGate()
        g.down(1000, 0, false, true)
        assertTrue(g.timer(g.downAt))
        assertFalse("drugi raz nie", g.timer(1000))
        assertEquals(Up.AFTER_LONG, g.up())
    }

    @Test fun systemLongFlagFiresLongOnRepeat() {
        val g = NiuniaOkGate()
        g.down(1000, 0, false, true)
        assertEquals(Down.LONG, g.down(1500, 1, systemLong = true, hasLong = true))
        assertEquals(Down.HELD, g.down(1550, 2, systemLong = false, hasLong = true))
        assertEquals(Up.AFTER_LONG, g.up())
    }

    @Test fun withoutLongActionHoldIsClickOnRelease() {
        val g = NiuniaOkGate()
        g.down(1000, 0, false, hasLong = false)
        assertEquals(Down.HELD, g.down(1500, 1, systemLong = true, hasLong = false))
        assertEquals(Up.CLICK, g.up())
    }

    @Test fun orphanReleaseIsIgnored() {
        // Długie OK otworzyło okno: puszczenie trafia do nowego elementu bez jego wciśnięcia.
        assertEquals(Up.IGNORE, NiuniaOkGate().up())
    }

    @Test fun repeatsHeldFromElsewhereAreIgnored() {
        val g = NiuniaOkGate()
        assertEquals(Down.IGNORE, g.down(2000, 3, systemLong = false, hasLong = true))
        assertEquals(Up.IGNORE, g.up())
    }

    @Test fun focusLossResetsSoNextPressClicks() {
        val g = NiuniaOkGate()
        g.down(1000, 0, false, true)
        assertTrue(g.timer(g.downAt)) // długie OK przeniosło fokus, puszczenie trafiło gdzie indziej
        g.reset()                     // utrata fokusu
        assertEquals(Down.PRESS, g.down(5000, 0, false, true))
        assertEquals(Up.CLICK, g.up())
    }

    @Test fun staleTimerFromEarlierPressDoesNothing() {
        val g = NiuniaOkGate()
        g.down(1000, 0, false, true)
        val first = g.downAt
        g.up()
        g.down(1200, 0, false, true)
        assertFalse(g.timer(first))
        assertEquals(Up.CLICK, g.up())
    }
}
