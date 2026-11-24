package com.nuvio.tv.fork.perf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 24.11 UX JMGO: wpisy logu ForkFocus (FocusTraceLine) — format czytany przez fork-tools/uitest/sciezki_jmgo.py. */
class ForkFocusTraceTest {

    @Test fun dtFocusFromRecentKey() {
        assertEquals(42L, FocusTraceLine.dtFocus(keyAt = 1000, now = 1042, window = 1500))
    }

    @Test fun dtFocusWithoutKeyOrOldKeyIsMinusOne() {
        assertEquals(-1L, FocusTraceLine.dtFocus(0, 5000, 1500))
        assertEquals("klawisz sprzed okna (powrót fokusu po danych)", -1L, FocusTraceLine.dtFocus(1000, 2600, 1500))
        assertEquals("zegar wstecz", -1L, FocusTraceLine.dtFocus(3000, 2000, 1500))
    }

    @Test fun labelIsOneShortLine() {
        assertEquals("Breaking Bad S1 E1", FocusTraceLine.label("  Breaking Bad\nS1\t E1 "))
        assertEquals(48, FocusTraceLine.label("x".repeat(200)).length)
        assertTrue(FocusTraceLine.label("x".repeat(200)).endsWith("…"))
        assertEquals("?", FocusTraceLine.label("   "))
    }

    @Test fun routeWithoutArguments() {
        assertEquals("detail", FocusTraceLine.route("detail/{itemId}/{itemType}?addonBaseUrl={addonBaseUrl}"))
        assertEquals("reshaped_live_tv", FocusTraceLine.route("reshaped_live_tv"))
        assertEquals("?", FocusTraceLine.route(""))
    }

    @Test fun focusLineParsableByScript() {
        val l = FocusTraceLine.focus("Odtwórz", intArrayOf(96, 700, 360, 780), 38, 61, "DPAD_DOWN")
        assertEquals("focus=Odtwórz bounds=[96,700][360,780] dtFocus=38 dtFrame=61 key=DPAD_DOWN", l)
        // ten sam wzorzec co w skrypcie ścieżek
        val m = Regex("focus=(.*?) bounds=(\\S+) dtFocus=(-?\\d+) dtFrame=(\\d+)").find(l)!!
        assertEquals("Odtwórz", m.groupValues[1]); assertEquals("38", m.groupValues[3]); assertEquals("61", m.groupValues[4])
    }

    @Test fun focusLineWithoutKeyHasNoKeyName() {
        val l = FocusTraceLine.focus("Kanał 1", null, -1, 120, "DPAD_DOWN")
        assertEquals("focus=Kanał 1 bounds=? dtFocus=-1 dtFrame=120", l)
        assertFalse(l.contains("key="))
    }

    @Test fun keyAndScreenLines() {
        assertEquals("key=DPAD_RIGHT rep=3 t=123456", FocusTraceLine.key("DPAD_RIGHT", 3, 123456))
        assertEquals("screen=home firstFocus=840", FocusTraceLine.screen("home", 840))
        assertEquals("screen=? firstFocus=5", FocusTraceLine.screen(null, 5))
    }
}
