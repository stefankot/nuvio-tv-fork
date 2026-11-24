package com.nuvio.tv.skin.niunia

import com.nuvio.tv.skin.niunia.components.NiuniaFocusMemory
import com.nuvio.tv.skin.niunia.components.NiuniaFocusMemory.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * UX JMGO 24.11, pętla 0 (ścieżki 19, 22, 49; F5 „Wstecz przywraca fokus na element, który otworzył ekran, z tym samym przewinięciem”):
 * Biblioteka, Odkrywaj i Szukaj po powrocie ze strony tytułu oddawały fokus pierwszemu klawiszowi/chipowi.
 */
class NiuniaFocusMemoryTest {
    private val grid = listOf("g:movie_a", "g:movie_b", "g:series_c", "g:series_d")
    private val rows = listOf("s0:" to listOf("s0:movie_a", "s0:movie_b"), "s1:" to listOf("s1:series_c", "s1:series_d", "s1:series_e"))

    @Test fun nothingSavedNothingRestored() {
        assertNull(NiuniaFocusMemory.target(null, listOf("g:" to grid)))
    }

    @Test fun backFromTitleReturnsToTheSameItem() {
        val e = Entry("g:series_c", 2, outer = 4, outerOffset = 30)
        assertEquals("g:series_c", NiuniaFocusMemory.target(e, listOf("g:" to grid)))
    }

    @Test fun survivesAppendedAndReorderedData() {
        val e = Entry("g:series_c", 2)
        assertEquals("g:series_c", NiuniaFocusMemory.target(e, listOf("g:" to (listOf("g:new") + grid + listOf("g:x")))))
    }

    @Test fun removedItemGoesToNeighbourAtTheSamePlace() {
        val e = Entry("g:series_c", 2)
        assertEquals("g:series_d", NiuniaFocusMemory.target(e, listOf("g:" to listOf("g:movie_a", "g:movie_b", "g:series_d"))))
        assertEquals("usunięty ostatni → nowy ostatni", "g:series_d",
            NiuniaFocusMemory.target(Entry("g:movie_z", 9), listOf("g:" to grid)))
    }

    @Test fun otherLayoutIsNotRestored() {
        // Zapis z siatki, a ekran pokazuje półki (przełączone w panelu): nic nie przywracamy, fokus domyślny.
        assertNull(NiuniaFocusMemory.target(Entry("g:series_c", 2), rows))
        // I odwrotnie: zapis z półki, ekran pokazuje siatkę.
        assertNull(NiuniaFocusMemory.target(Entry("s1:series_d", 1), listOf("g:" to grid)))
    }

    @Test fun shelfItemFoundInItsOwnShelf() {
        assertEquals("s1:series_d", NiuniaFocusMemory.target(Entry("s1:series_d", 1), rows))
        assertEquals("rząd skrócony → ostatni z rzędu", "s1:series_e",
            NiuniaFocusMemory.target(Entry("s1:series_z", 7), rows))
        assertNull("cała półka zniknęła", NiuniaFocusMemory.target(Entry("s2:x", 0), rows))
    }

    @Test fun scrollIsRestoredOnlyForTheShelfTheItemBelongsTo() {
        val e = Entry("s1:series_d", 1, outer = 1, outerOffset = 12, inner = 3, innerOffset = 40)
        val t = NiuniaFocusMemory.target(e, rows)
        assertEquals(1 to 12, NiuniaFocusMemory.outerStart(e, t))
        assertEquals(3 to 40, NiuniaFocusMemory.innerStart(e, t, "s1:"))
        assertEquals("inna półka zostaje na początku", 0 to 0, NiuniaFocusMemory.innerStart(e, t, "s0:"))
        assertEquals("brak celu → bez przewijania", 0 to 0, NiuniaFocusMemory.outerStart(e, null))
        assertEquals(0 to 0, NiuniaFocusMemory.outerStart(null, null))
    }

    @Test fun saveAndReadPerScreen() {
        NiuniaFocusMemory.clear("t:a"); NiuniaFocusMemory.clear("t:b")
        NiuniaFocusMemory.save("t:a", Entry("k1", 1))
        NiuniaFocusMemory.save("t:b", Entry("k2", 2))
        NiuniaFocusMemory.save("t:a", Entry("k3", 3))
        assertEquals("k3", NiuniaFocusMemory.entry("t:a")?.key)
        assertEquals("k2", NiuniaFocusMemory.entry("t:b")?.key)
        NiuniaFocusMemory.clear("t:a")
        assertNull(NiuniaFocusMemory.entry("t:a"))
        assertEquals("k2", NiuniaFocusMemory.entry("t:b")?.key)
        NiuniaFocusMemory.clear("t:b")
    }

    @Test fun emptyScreenHasNothingToRestore() {
        assertNull(NiuniaFocusMemory.target(Entry("g:a", 0), listOf("g:" to emptyList())))
        assertNull(NiuniaFocusMemory.target(Entry("g:a", 0), emptyList()))
    }
}
