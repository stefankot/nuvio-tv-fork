package com.nuvio.tv.skin.niunia

import com.nuvio.tv.skin.niunia.components.NiuniaFocusKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** UX JMGO 24.11 (F3/F5/F6): pamięć fokusu po kluczu, nie po indeksie. */
class NiuniaFocusKeysTest {
    private val row = listOf("cw:a", "cw:b", "cw:c", "cw:d")

    @Test fun keyFound() = assertEquals(2, NiuniaFocusKeys.restoreIndex(row, "cw:c", 0))

    @Test fun survivesAppendedData() = assertEquals(2, NiuniaFocusKeys.restoreIndex(row + listOf("cw:e", "cw:f"), "cw:c", 2))

    @Test fun survivesReorder() {
        // „Oglądaj dalej” po obejrzeniu: oglądana pozycja wskakuje na początek — fokus idzie za nią, nie zostaje na indeksie 2.
        assertEquals(0, NiuniaFocusKeys.restoreIndex(listOf("cw:c", "cw:a", "cw:b", "cw:d"), "cw:c", 2))
    }

    @Test fun removedItemGoesToNearestNeighbour() {
        assertEquals("sąsiad na tym samym miejscu", 2, NiuniaFocusKeys.restoreIndex(listOf("cw:a", "cw:b", "cw:d"), "cw:c", 2))
        assertEquals("usunięty ostatni → nowy ostatni", 2, NiuniaFocusKeys.restoreIndex(listOf("cw:a", "cw:b", "cw:c"), "cw:d", 3))
        assertEquals("indeks ujemny → pierwszy", 0, NiuniaFocusKeys.restoreIndex(row, "x", -5))
    }

    @Test fun emptyInput() = assertEquals(-1, NiuniaFocusKeys.restoreIndex(emptyList<String>(), "cw:a", 0))

    @Test fun resolveAcrossRows() {
        val rows = listOf("cw:" to row, "r:top:" to listOf("r:top:1", "r:top:2", "r:top:__more"))
        assertEquals("r:top:2", NiuniaFocusKeys.resolve(rows, "r:top:2", 1))
        assertEquals("plakat usunięty z rzędu → sąsiad", "r:top:__more", NiuniaFocusKeys.resolve(rows, "r:top:9", 2))
        assertEquals("cw:a", NiuniaFocusKeys.resolve(rows, "cw:zzz", 0))
        assertNull("cały rząd zniknął", NiuniaFocusKeys.resolve(rows, "r:gone:1", 0))
        assertNull(NiuniaFocusKeys.resolve(rows, null, 0))
    }

    @Test fun twentyThousandItemsIsFast() {
        val big = List(20_000) { "r:x:$it" }
        val t0 = System.nanoTime()
        repeat(50) { NiuniaFocusKeys.restoreIndex(big, "r:x:19999", 0) }
        val ms = (System.nanoTime() - t0) / 1_000_000 / 50.0
        assert(ms < 20.0) { "restoreIndex na 20 tys.: $ms ms" }
    }
}
