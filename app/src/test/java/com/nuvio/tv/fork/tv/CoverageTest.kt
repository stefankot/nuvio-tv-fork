package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.guide.Coverage
import org.junit.Assert.assertEquals
import org.junit.Test

class CoverageTest {
    @Test fun `bez programu na koncu grupy`() {
        val items = listOf(Coverage.Item("a", "G1", false), Coverage.Item("b", "G1", true), Coverage.Item("c", "G2", true), Coverage.Item("d", "G2", false))
        val sorted = Coverage.sortedGroupTail(items, { it.group }, { it.hasProgramme })
        assertEquals(listOf("b", "a", "c", "d"), sorted.map { it.key })
    }
    @Test fun `komunikat`() { assertEquals("221 z 313 kanałów ma program", Coverage.message(221, 313)) }
}
