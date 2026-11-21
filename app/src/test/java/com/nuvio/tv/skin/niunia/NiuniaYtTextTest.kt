package com.nuvio.tv.skin.niunia

import com.nuvio.tv.skin.niunia.player.NiuniaYtText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Metadane YouTube po polsku (test JMGO build151: „2.6M views · Published on Nov 13, 2026”). */
class NiuniaYtTextTest {
    @Test fun views() {
        assertEquals("2,6 mln wyśw.", NiuniaYtText.views("2.6M views"))
        assertEquals("491 tys. wyśw.", NiuniaYtText.views("491K views"))
        assertEquals("1 234 wyśw.", NiuniaYtText.views("1,234 views"))
        assertEquals("brak", NiuniaYtText.views("brak"))
        assertNull(NiuniaYtText.views(" "))
    }

    @Test fun dates() {
        assertEquals("13 lis 2026", NiuniaYtText.date("Published on Nov 13, 2026"))
        assertEquals("1 maj 2025", NiuniaYtText.date("May 1, 2025"))
        assertEquals("7 dni temu", NiuniaYtText.date("7 days ago"))
        assertEquals("1 dzień temu", NiuniaYtText.date("1 day ago"))
        assertEquals("3 miesiące temu", NiuniaYtText.date("3 months ago"))
        assertEquals("5 lat temu", NiuniaYtText.date("5 years ago"))
        assertEquals("22 lata temu", NiuniaYtText.date("22 years ago"))
        assertEquals("12 lat temu", NiuniaYtText.date("12 years ago"))
        assertEquals("4 godz. temu", NiuniaYtText.date("4 hours ago"))
        assertEquals("wczoraj", NiuniaYtText.date("wczoraj"))
    }
}

/** Gatunki po polsku (test JMGO build152: „Action”, „Drama” w Szukaj i Bibliotece). */
class NiuniaGenresTest {
    @Test fun genres() {
        assertEquals("Akcja", com.nuvio.tv.skin.niunia.components.NiuniaGenres.pl("Action"))
        assertEquals("Sci-fi i fantasy", com.nuvio.tv.skin.niunia.components.NiuniaGenres.pl("Sci-Fi &amp; Fantasy"))
        assertEquals("Komedia", com.nuvio.tv.skin.niunia.components.NiuniaGenres.pl("Komedia"))
        assertNull(com.nuvio.tv.skin.niunia.components.NiuniaGenres.pl(" "))
    }

    @Test
    fun subscribersAndLikesInPolish() {
        assertEquals("4,34 mln subskrybentów", NiuniaYtText.subscribers("4.34M subscribers"))
        assertEquals("512 tys. subskrybentów", NiuniaYtText.subscribers("512K subscribers"))
        assertEquals("1 subskrybent", NiuniaYtText.subscribers("1 subscriber"))
        assertEquals("3 subskrybenci", NiuniaYtText.subscribers("3 subscribers"))
        assertEquals("31 tys.", NiuniaYtText.count("31K"))
        assertEquals("1,2 mln", NiuniaYtText.count("1.2M"))
        assertEquals("Lubię to", NiuniaYtText.count("Lubię to"))
    }
}
