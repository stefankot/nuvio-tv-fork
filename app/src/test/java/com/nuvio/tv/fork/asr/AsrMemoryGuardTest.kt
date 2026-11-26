package com.nuvio.tv.fork.asr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 26.11 (V9): największy wolny obszar adresów z /proc/self/maps — bez obszaru pod stosem i nad nim. */
class AsrMemoryGuardTest {
    private val mb = 1024L * 1024L

    @Test fun `największa przerwa między obszarami`() {
        val maps = sequenceOf(
            "10000000-12000000 r-xp 00000000 00:00 0 /system/lib/libc.so",
            "12000000-13000000 rw-p 00000000 00:00 0",
            "1f000000-20000000 rw-p 00000000 00:00 0 [anon:dalvik-main space]", // przerwa 192 MB
            "2a000000-2b000000 rw-p 00000000 00:00 0", // przerwa 160 MB
            "be800000-bf000000 rw-p 00000000 00:00 0 [stack]", // przerwa pod stosem się nie liczy
            "ffff0000-ffff1000 r-xp 00000000 00:00 0 [vectors]",
        )
        assertEquals(192 * mb, AsrMemoryGuard.largestGap(maps))
    }

    @Test fun `pusta mapa — nie wiadomo`() {
        assertNull(AsrMemoryGuard.largestGap(emptySequence()))
    }
}
