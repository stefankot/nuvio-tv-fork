package com.nuvio.tv.ui.screens.player.audiosync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioSpotBudgetTest {
    @Test
    fun aRebuiltSamplerWaitsAndSharesTheBytesAlreadyRead() {
        val budget = AudioSpotBudget()
        assertEquals(150L, budget.begin(150L))
        assertNull(budget.begin(150L))
        budget.finish(110L)
        assertEquals(0L, budget.begin(64L))
        assertEquals(40L, budget.begin(150L))
        budget.finish(45L)
        assertEquals(0L, budget.begin(150L))
    }
}
