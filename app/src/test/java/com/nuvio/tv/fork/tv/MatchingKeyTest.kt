package com.nuvio.tv.fork.tv

import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.liveTvGuideMatchingKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MatchingKeyTest {
    private fun ch(i: Int) = LiveTvChannel(id = "c$i", name = "Kanal $i", streamUrl = "u$i", guideKey = "g$i")

    @Test fun `ten sam klucz dla dwoch kolejnosci`() {
        val a = (1..50).map(::ch)
        assertEquals(liveTvGuideMatchingKey(a), liveTvGuideMatchingKey(a.reversed()))
    }

    @Test fun `zmiana nazwy zmienia klucz`() {
        val a = (1..5).map(::ch)
        val b = a.toMutableList().also { it[2] = it[2].copy(name = "Inna") }
        assertNotEquals(liveTvGuideMatchingKey(a), liveTvGuideMatchingKey(b))
    }
}
