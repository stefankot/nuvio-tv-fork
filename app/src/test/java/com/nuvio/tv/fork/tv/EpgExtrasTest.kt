package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.most.ForkTvHooks
import com.nuvio.tv.fork.tv.most.ProgrammeExtra
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpgExtrasTest {
    @After fun reset() { ForkTvHooks.sink = null }

    @Test fun `tabela zapamietuje i pomija puste`() {
        EpgExtras.put("Serial", 1L, ProgrammeExtra(episode = "S1E2", age = "12", year = 2020, categories = listOf("dramat")))
        EpgExtras.put("Pusty", 2L, ProgrammeExtra())
        assertEquals("S1E2 · wiek 12 · 2020 · dramat", EpgExtras.get(" serial ", 1L)?.infoLine)
        assertNull(EpgExtras.get("Pusty", 2L))
    }

    @Test fun `punkt wpiecia oddaje dane wlasciwemu odbiorcy`() {
        var got: String? = null
        ForkTvHooks.sink = { t, _, _ -> got = t }
        ForkTvHooks.onProgrammeRead("X", 1L, ProgrammeExtra(episode = "E1"))
        assertEquals("X", got)
    }
}
