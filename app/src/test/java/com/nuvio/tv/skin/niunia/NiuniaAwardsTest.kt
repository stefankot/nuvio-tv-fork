package com.nuvio.tv.skin.niunia

import com.nuvio.tv.skin.niunia.awards.NiuniaAward
import com.nuvio.tv.skin.niunia.awards.NiuniaAwardBodies
import com.nuvio.tv.skin.niunia.awards.NiuniaAwardResult.Nominated
import com.nuvio.tv.skin.niunia.awards.NiuniaAwardResult.Pending
import com.nuvio.tv.skin.niunia.awards.NiuniaAwardResult.Won
import com.nuvio.tv.skin.niunia.awards.NiuniaAwardWords
import com.nuvio.tv.skin.niunia.awards.NiuniaAwardsStripPlan
import com.nuvio.tv.skin.niunia.awards.niuniaAwardBadge
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsData
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsListGroups
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsSplit
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsStrip
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsStripDescription
import com.nuvio.tv.skin.niunia.awards.niuniaAwardsSummary
import com.nuvio.tv.skin.niunia.awards.niuniaPlural
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Z-13: wartości z ZMIANY-DO-WDROZENIA.md (tablice Detail = Rings of Power, Awards12AngryMen). */
class NiuniaAwardsTest {
    private val w = NiuniaAwardWords("nagroda", "nagrody", "nagród", "nominacja", "nominacje", "nominacji", "gala", "gale", "gal")

    private fun a(body: String, year: Int, result: com.nuvio.tv.skin.niunia.awards.NiuniaAwardResult, name: String? = null, short: String? = null, cat: String = "Kategoria") =
        NiuniaAward(body, year, cat, result, name = name, short = short)

    /** Rings of Power (Detail): ADG 2 W, VES 3 W + 4 N, BAFTA 1 W, Emmy 7 N, Saturn 3 N, SAG 1 N, CCA 1 N. */
    private val rings = List(2) { a("adg", 2023, Won) } + List(3) { a("ves", 2023, Won) } + List(4) { a("ves", 2023, Nominated) } +
        a("bafta", 2025, Won) + List(7) { a("emmy", 2023, Nominated) } + List(3) { a("saturn", 2024, Nominated) } +
        a("sag", 2023, Nominated) + a("cca", 2023, Nominated)

    /** 12 Angry Men: Berlinale 1 W, BAFTA 1 W + 1 N, Oscar 3 N, Złoty Glob 4 N, WGA 1 W, Edgar / Étoiles / NBR po 1 W (spoza rejestru). */
    private val angry = listOf(a("berlinale", 1957, Won), a("bafta", 1958, Won), a("bafta", 1958, Nominated)) + List(3) { a("oscar", 1958, Nominated) } +
        List(4) { a("globe", 1958, Nominated) } + a("wga", 1958, Won) + a("edgar", 1958, Won, "Edgar Awards", "Edgar") +
        a("etoiles", 1958, Won, "Étoiles de cristal", "Étoiles") + a("nbr", 1957, Won, "National Board of Review", "NBR")

    private fun line(p: NiuniaAwardsStripPlan) = p.shown.joinToString(" · ") { g ->
        if (g.wins.isNotEmpty()) "${g.body.label} ${g.wins.size}" else "${g.body.label} ${g.noms.size} (nom.)"
    } + " · „${p.rest}”"

    @Test fun `odmiana nagroda nagrody nagrod`() {
        val f = { n: Int -> niuniaPlural(n, "nagroda", "nagrody", "nagród") }
        assertEquals("1 nagroda", f(1))
        assertEquals("2 nagrody", f(2))
        assertEquals("4 nagrody", f(4))
        assertEquals("5 nagród", f(5))
        assertEquals("11 nagród", f(11))
        assertEquals("12 nagród", f(12))
        assertEquals("14 nagród", f(14))
        assertEquals("21 nagród", f(21))
        assertEquals("22 nagrody", f(22))
        assertEquals("26 nagród", f(26))
        assertEquals("112 nagród", f(112))
        assertEquals("0 nagród", f(0))
        assertEquals("5 nominacji", niuniaPlural(5, "nominacja", "nominacje", "nominacji"))
        assertEquals("3 nominacje", niuniaPlural(3, "nominacja", "nominacje", "nominacji"))
    }

    @Test fun `sortowanie gal poziom wygrane rejestr`() {
        assertEquals(listOf("bafta", "emmy", "ves", "adg", "saturn", "sag", "cca"), niuniaAwardsData(rings).groups.map { it.body.key })
        assertEquals(listOf("berlinale", "bafta", "oscar", "globe", "wga", "edgar", "etoiles", "nbr"), niuniaAwardsData(angry).groups.map { it.body.key })
        // ta sama liczba wygranych i poziom → kolejność rejestru (Oscar przed Emmy), potem liczba wszystkich
        val tie = listOf(a("emmy", 2020, Nominated), a("oscar", 2020, Nominated), a("cannes", 2020, Nominated), a("cannes", 2020, Nominated))
        assertEquals(listOf("oscar", "cannes", "emmy"), niuniaAwardsData(tie).groups.map { it.body.key })
        val reg = NiuniaAwardBodies.registry.map { it.key }
        assertEquals(listOf("oscar", "cannes", "berlinale", "venice", "sundance", "emmy", "globe", "bafta"), reg.take(8))
        assertEquals(999, NiuniaAwardBodies.rank("edgar"))
    }

    @Test fun `pasek Rings of Power`() {
        val d = niuniaAwardsData(rings)
        assertEquals("BAFTA 1 · Emmy 7 (nom.) · VES 3 · „2 nagrody”", line(niuniaAwardsStrip(d, w)))
        assertEquals("6 nagród · 16 nominacji", niuniaAwardsSummary(d.wins, d.noms, w))
        assertTrue(niuniaAwardsStripDescription(d, w).startsWith("6 nagród · 16 nominacji (BAFTA 1 nagroda, Emmy 7 nominacji, Visual Effects Society 3 nagrody"))
    }

    @Test fun `pasek 12 Angry Men`() {
        assertEquals("Berlinale 1 · BAFTA 1 · Oscar 3 (nom.) · „4 nagrody”", line(niuniaAwardsStrip(niuniaAwardsData(angry), w)))
    }

    @Test fun `reszta bez wygranych to nominacje, same festiwale to tylko liczba`() {
        val noms = List(2) { a("oscar", 2020, Won) } + a("emmy", 2020, Won) + a("bafta", 2020, Won) + List(5) { a("saturn", 2020, Nominated) }
        assertEquals("„5 nominacji”", line(niuniaAwardsStrip(niuniaAwardsData(noms), w)).substringAfterLast(" · "))
        val fest = listOf(a("gdynia", 2021, Won, "Festiwal w Gdyni", "Gdynia"), a("tff", 2021, Won, "Tribeca", "Tribeca"))
        val p = niuniaAwardsStrip(niuniaAwardsData(fest), w)
        assertTrue(p.shown.isEmpty())
        assertEquals("2 nagrody", p.rest)
        assertNull(niuniaAwardsStrip(niuniaAwardsData(listOf(a("oscar", 2020, Won))), w).rest)
    }

    @Test fun `lista festiwale w jednej grupie i kolumny`() {
        val g = niuniaAwardsListGroups(niuniaAwardsData(angry))
        assertEquals(listOf("berlinale", "bafta", "oscar", "globe", "wga", null), g.map { it.body?.key })
        val tail = g.last()
        assertEquals(3, tail.galas)
        assertEquals(listOf("Edgar Awards", "Étoiles de cristal", "National Board of Review"), tail.rows.map { it.via })
        // wygrane pierwsze w grupie
        assertEquals(listOf(Won, Nominated), g[1].rows.map { it.award.result })
        // jedna gala spoza rejestru zostaje sama
        assertEquals("gdynia", niuniaAwardsListGroups(niuniaAwardsData(listOf(a("gdynia", 2021, Won, "Festiwal w Gdyni")))).single().body?.key)
        // kolumny: grupy w kolejności, wagi możliwie równe
        val split = niuniaAwardsSplit(niuniaAwardsListGroups(niuniaAwardsData(rings)))
        assertTrue(split in 1..6)
        assertEquals(0, niuniaAwardsSplit(emptyList()))
    }

    @Test fun `gala spoza rejestru bez nazwy i plakietka`() {
        val b = NiuniaAwardBodies.of("xyzfest")
        assertEquals("XYZF", b.short)
        assertEquals(4, b.tier)
        assertNull(niuniaAwardBadge(niuniaAwardsData(emptyList()), w))
        val badge = niuniaAwardBadge(niuniaAwardsData(rings), w)!!
        assertEquals("6 nagród", badge.text)
        assertEquals("bafta", badge.body?.key)
        val nom = niuniaAwardBadge(niuniaAwardsData(List(4) { a("oscar", 2020, Nominated) } + a("emmy", 2026, Pending)), w)!!
        assertEquals("5 nominacji", nom.text)
        assertTrue(nom.nomination)
    }
}
