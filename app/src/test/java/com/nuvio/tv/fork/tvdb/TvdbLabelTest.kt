package com.nuvio.tv.fork.tvdb

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Test 22.11: sezon i odcinek z etykiety programu w przewodniku. */
class TvdbLabelTest {
    @Test fun `formaty SxxEyy, 1x02, E12`() {
        assertEquals(3 to 168, Tvdb.parseLabel("S3E168"))
        assertEquals(1 to 2, Tvdb.parseLabel("s01 e02"))
        assertEquals(2 to 5, Tvdb.parseLabel("2x05"))
        assertEquals(null to 6009, Tvdb.parseLabel("E6009"))
    }

    @Test fun `xmltv_ns liczony od zera`() {
        assertEquals(1 to 3, Tvdb.parseLabel("0.2."))
        assertEquals(null to 5, Tvdb.parseLabel(".4."))
    }

    @Test fun `same cyfry, puste i śmieci`() {
        assertEquals(null to 42, Tvdb.parseLabel("42"))
        assertNull(Tvdb.parseLabel(null))
        assertNull(Tvdb.parseLabel("  "))
        assertNull(Tvdb.parseLabel("odcinek specjalny"))
    }
}
