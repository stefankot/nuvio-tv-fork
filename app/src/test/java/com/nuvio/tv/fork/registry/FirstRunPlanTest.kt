package com.nuvio.tv.fork.registry

import com.nuvio.tv.fork.settings.FirstRunPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirstRunPlanTest {
    @Test fun `ustawia domyslne gdy brak zapisu`() {
        val r = FirstRunPlan.decide(FirstRunPlan.Stored(null, null, null, null))
        assertEquals(true, r.tmdbEnabled); assertEquals("pl", r.tmdbLanguage); assertEquals("pl", r.appLocale); assertEquals("SIMPLE", r.level)
    }
    @Test fun `nie nadpisuje zapisanych`() {
        val r = FirstRunPlan.decide(FirstRunPlan.Stored(false, "en", "de", "ADVANCED"))
        assertNull(r.tmdbEnabled); assertNull(r.tmdbLanguage); assertNull(r.appLocale); assertNull(r.level)
    }
}
