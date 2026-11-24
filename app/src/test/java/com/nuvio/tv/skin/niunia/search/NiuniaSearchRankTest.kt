package com.nuvio.tv.skin.niunia.search

import org.junit.Assert.assertEquals
import org.junit.Test

/** UX JMGO (ręczne testy 24.11): „dun” → „Dune” przed „Muriel” i „Cztery noce marzeń”. */
class NiuniaSearchRankTest {
    private fun order(q: String, names: List<String>) =
        names.sortedBy { niuniaSearchRank(niuniaSearchNorm(it), niuniaSearchNorm(q)) }

    @Test fun `tytuly zaczynajace sie od zapytania sa pierwsze`() {
        val got = order("dun", listOf("Exploits of a Young Don Juan", "Muriel", "Madam Dun", "Dune", "Dunk", "Dun Benit"))
        assertEquals(listOf("Dune", "Dunk", "Dun Benit", "Madam Dun", "Exploits of a Young Don Juan", "Muriel"), got)
    }

    @Test fun `polskie znaki i wielkosc liter nie przeszkadzaja`() {
        assertEquals("smierc czlowieka", niuniaSearchNorm("Śmierć  Człowieka!"))
        assertEquals(0, niuniaSearchRank(niuniaSearchNorm("Łódź"), niuniaSearchNorm("lodz")))
    }
}

class NiuniaGenericEpisodeTitleTest {
    @Test fun `tytuly zastepcze sa ukrywane`() {
        val d = com.nuvio.tv.skin.niunia.detail.niuniaGenericEpisodeTitle("Episode 2", 2)
        assertEquals(true, d)
        assertEquals(true, com.nuvio.tv.skin.niunia.detail.niuniaGenericEpisodeTitle("Odcinek 1", 1))
        assertEquals(false, com.nuvio.tv.skin.niunia.detail.niuniaGenericEpisodeTitle("Pilot", 1))
        assertEquals(false, com.nuvio.tv.skin.niunia.detail.niuniaGenericEpisodeTitle("Episode 2", 3))
    }
}
