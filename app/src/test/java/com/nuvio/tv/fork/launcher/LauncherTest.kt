package com.nuvio.tv.fork.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherTest {
    @Test fun `4 nacisniecia nic 5 w 3 s otwiera 5 w 6 s nic`() {
        var c = BackPressCounter(); assertFalse((0..3).any { c.press(it * 500L) }); assertTrue(c.press(2_000))
        c = BackPressCounter(); assertFalse((0..4).any { c.press(it * 1_500L) })
    }
    @Test fun `blokada i pin`() {
        val c = BackPressCounter()
        assertEquals(ExitDecision.NOTHING, ExitPolicy.onHomeBack(false, c, 0, false))
        repeat(4) { assertEquals(ExitDecision.NOTHING, ExitPolicy.onHomeBack(true, c, it * 100L, true)) }
        assertEquals(ExitDecision.ASK_PIN, ExitPolicy.onHomeBack(true, c, 500, true))
        repeat(4) { ExitPolicy.onHomeBack(true, c, 1_000L + it, false) }
        assertEquals(ExitDecision.OPEN_EXIT_SCREEN, ExitPolicy.onHomeBack(true, c, 1_100, false))
    }
    @Test fun `poziomy blokady`() {
        assertFalse(LockLevels.backLockOn(LockLevel.OFF)); assertTrue(LockLevels.backLockOn(LockLevel.SOFT)); assertTrue(LockLevels.autostart(LockLevel.FULL, false))
        assertFalse(LockLevels.homeAllowed(false))
    }
    @Test fun `trasa startowa`() {
        assertEquals("home", StartRoute.resolve(StartPage.HOME, null, true)); assertEquals("tv", StartRoute.resolve(StartPage.TV, null, true))
        assertEquals("tv_channel", StartRoute.resolve(StartPage.LAST_CHANNEL, "u", true)); assertEquals("tv", StartRoute.resolve(StartPage.LAST_CHANNEL, null, true))
        assertEquals("home", StartRoute.resolve(StartPage.TV, null, false))
    }
    @Test fun `lista aplikacji bez Nuvio i posortowana`() {
        val apps = listOf(AppEntry("Zeta", "z"), AppEntry("nuvio", "me"), AppEntry("alfa", "a"), AppEntry("Alfa2", "a"))
        assertEquals(listOf("a", "z"), AppList.filterSort(apps, "me").map { it.packageName })
    }
}
