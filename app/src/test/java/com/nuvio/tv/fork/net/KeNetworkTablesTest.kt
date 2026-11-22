package com.nuvio.tv.fork.net

import com.nuvio.tv.core.assessment.ServiceNetworkTables
import com.nuvio.tv.core.assessment.ServiceTable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy 22.11: test łącza KE — zapis tabeli serwisu i przekierowań (kopia 1:1 z KE `withTest`). */
class KeNetworkTablesTest {
    private val t1 = ServiceTable(testedAtMs = 100L)
    private val t2 = ServiceTable(testedAtMs = 200L)

    @Test fun `ten sam serwis - tabela zapisana bez aliasu`() {
        val r = ServiceNetworkTables().withTest("cdn.a", "cdn.a", t1)
        assertEquals(t1, r.tables["cdn.a"])
        assertTrue(r.aliases.isEmpty())
    }

    @Test fun `przekierowanie - alias z dodatku na serwujący serwis`() {
        val r = ServiceNetworkTables().withTest("addon.example", "cdn.b", t1)
        assertEquals(t1, r.tables["cdn.b"])
        assertEquals("cdn.b", r.aliases["addon.example"])
        assertFalse("tabela tylko pod serwującym", r.tables.containsKey("addon.example"))
    }

    @Test fun `nowy wynik zastępuje stary, osierocona tabela do ponownego testu znika`() {
        val start = ServiceNetworkTables(tables = mapOf("cdn.old" to t1), aliases = mapOf("addon" to "cdn.old"), retestRequestedAtMs = 150L)
        val r = start.withTest("addon", "cdn.new", t2)
        assertEquals("cdn.new", r.aliases["addon"])
        assertEquals(t2, r.tables["cdn.new"])
        assertFalse("stara tabela czekała na ponowny test i nikt już na nią nie wskazuje", r.tables.containsKey("cdn.old"))
    }

    @Test fun `tabela aktualna (po ponownym teście) zostaje`() {
        val start = ServiceNetworkTables(tables = mapOf("cdn.old" to t2), aliases = mapOf("addon" to "cdn.old"), retestRequestedAtMs = 150L)
        val r = start.withTest("addon", "cdn.new", t1)
        assertTrue(r.tables.containsKey("cdn.old"))
    }
}
