package com.nuvio.tv.fork.buildage

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildAgeTest {
    @Test fun `wiek builda`() {
        assertEquals("0m", age(-5_000))
        assertEquals("0m", age(59_000))
        assertEquals("45m", age(45 * 60_000L))
        assertEquals("1h", age(60 * 60_000L))
        assertEquals("47h", age(47 * 3_600_000L + 59 * 60_000L))
        assertEquals("2d", age(48 * 3_600_000L))
    }
}
