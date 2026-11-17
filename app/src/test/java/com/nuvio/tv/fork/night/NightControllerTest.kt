package com.nuvio.tv.fork.night

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NightControllerTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private fun at(day: Int, h: Int, m: Int = 0) = Calendar.getInstance(utc).apply { clear(); set(2026, Calendar.NOVEMBER, day, h, m) }.timeInMillis
    private fun controller() = NightController(NightSwitch.SCHEDULE, NightSchedule(22 * 60, 7 * 60), Strength.MEDIUM, utc)

    @Test fun `22-07 o 23-30 aktywny a o 8-00 nie`() {
        val c = controller()
        assertTrue(c.active(at(10, 23, 30))); assertEquals(Strength.MEDIUM, c.effective(at(10, 23, 30)))
        assertFalse(c.active(at(11, 8))); assertEquals(Strength.OFF, c.effective(at(11, 8)))
        assertTrue(c.active(at(11, 3)))
    }

    @Test fun `reczne wylaczenie o 23 wraca o 22 nastepnego dnia`() {
        val c = controller()
        c.toggleManually(at(10, 23))
        assertFalse(c.active(at(10, 23, 30))); assertFalse(c.active(at(11, 3)))
        // granica = 07:00 (koniec okna); po niej harmonogram (8:00 nieaktywny), a o 22:00 znów aktywny
        assertFalse(c.active(at(11, 8))); assertTrue(c.active(at(11, 22, 30)))
    }

    @Test fun `tryb wlaczony i wylaczony ignoruje godziny`() {
        assertTrue(NightController(NightSwitch.ON, zone = utc).active(at(10, 12))); assertFalse(NightController(NightSwitch.OFF, zone = utc).active(at(10, 23)))
    }

    @Test fun `granica`() {
        assertEquals(at(11, 7), controller().nextBoundary(at(10, 23))); assertEquals(at(10, 22), controller().nextBoundary(at(10, 8)))
    }
}
