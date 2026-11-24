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

    // --- UX JMGO 24.11: przypadki z pętli na projektorze ---

    @Test fun okHeldFromPreviousScreenThenFreshPressClicks() {
        // OK trzymane na poprzednim ekranie: na nowym elemencie przychodzą same powtórzenia i puszczenie — nic nie robią.
        val g = NiuniaOkGate()
        assertEquals(Down.IGNORE, g.down(1000, 5, systemLong = true, hasLong = true))
        assertEquals(Down.IGNORE, g.down(1050, 6, systemLong = true, hasLong = true))
        assertEquals(Up.IGNORE, g.up())
        assertEquals(Down.PRESS, g.down(2000, 0, false, true))
        assertEquals(Up.CLICK, g.up())
    }

    @Test fun systemFlagOnFirstDownIsLongOnce() {
        val g = NiuniaOkGate()
        assertEquals(Down.LONG, g.down(1000, 0, systemLong = true, hasLong = true))
        assertEquals(Down.HELD, g.down(1050, 1, systemLong = true, hasLong = true))
        assertFalse("zegar po fladze nie odpala drugi raz", g.timer(g.downAt))
        assertEquals(Up.AFTER_LONG, g.up())
    }

    @Test fun timerThenSystemFlagFiresOnlyOnce() {
        val g = NiuniaOkGate()
        g.down(1000, 0, false, true)
        assertTrue(g.timer(g.downAt))
        assertEquals(Down.HELD, g.down(1500, 10, systemLong = true, hasLong = true))
        assertEquals(Up.AFTER_LONG, g.up())
    }

    @Test fun windowOpenedAt520msReleaseOnWindowDoesNotClick() {
        // Długie OK otwiera okno: fokus przechodzi na pierwszy przycisk okna (stara bramka zerowana przez utratę fokusu),
        // puszczenie OK trafia do przycisku okna, który nie widział wciśnięcia — pominięte; potem krótkie OK w oknie działa.
        val poster = NiuniaOkGate(); val button = NiuniaOkGate()
        poster.down(1000, 0, false, true)
        assertTrue(poster.timer(poster.downAt))
        poster.reset() // onFocusChanged(utrata) w niuniaOk
        assertEquals(Down.IGNORE, button.down(1530, 3, systemLong = true, hasLong = false))
        assertEquals(Up.IGNORE, button.up())
        assertEquals(Down.PRESS, button.down(3000, 0, false, false))
        assertEquals(Up.CLICK, button.up())
        // po zamknięciu okna krótkie OK na plakacie od razu działa
        assertEquals(Down.PRESS, poster.down(4000, 0, false, true))
        assertEquals(Up.CLICK, poster.up())
    }
}
