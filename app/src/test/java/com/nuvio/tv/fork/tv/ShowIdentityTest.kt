package com.nuvio.tv.fork.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShowIdentityTest {
    @Test fun titlesLoseAccentsMarkersAndPunctuation() {
        assertEquals("sprawiedliwi wydzial kryminalny", ShowIdentity.normalizeTitle("Sprawiedliwi - Wydział Kryminalny"))
        assertEquals("wojna domowa", ShowIdentity.normalizeTitle("Wojna domowa (12)"))
        assertEquals("mentalista", ShowIdentity.normalizeTitle("Mentalista S02E05"))
        assertEquals("m jak milosc", ShowIdentity.normalizeTitle("M jak miłość - odc. 1850"))
        assertEquals("lodz", ShowIdentity.normalizeTitle("Łódź"))
    }

    @Test fun aBareTrailingNumberStaysPartOfTheTitle() {
        assertEquals("mentalista 2", ShowIdentity.normalizeTitle("Mentalista 2"))
        assertNotEquals(ShowIdentity.normalizeTitle("Mentalista"), ShowIdentity.normalizeTitle("Mentalista 2"))
    }

    @Test fun markersComeFromTitleOrDescription() {
        assertEquals("e12", ShowIdentity.episodeMarker("Wojna domowa (12)", null))
        assertEquals("s02e05", ShowIdentity.episodeMarker("Serial S02E05", null))
        assertEquals("s03e04", ShowIdentity.episodeMarker("Serial sezon 3 odc. 4", null))
        assertEquals("e07", ShowIdentity.episodeMarker("Serial", "Odcinek 7. Bohaterowie wracają."))
    }

    @Test fun aYearOrABareNumberIsNeverAMarker() {
        assertNull(ShowIdentity.episodeMarker("Film (2024)", null))
        assertNull(ShowIdentity.episodeMarker("Mentalista 2", "Pod mostem znaleziono ciało."))
    }

    @Test fun fingerprintsIgnoreSpacingAndCaseAndSkipShortText() {
        val a = ShowIdentity.fingerprint("Pod mostem w Sacramento znaleziono ciało.")
        val b = ShowIdentity.fingerprint("  pod MOSTEM   w sacramento znaleziono  ciało ")
        assertEquals(a, b)
        assertNotEquals(a, ShowIdentity.fingerprint("CBI otrzymuje anonimową informację o zwłokach."))
        assertEquals("", ShowIdentity.fingerprint("krótki"))
        assertEquals("", ShowIdentity.fingerprint(null))
    }

    @Test fun episodeIdPrefersTheMarker() {
        assertEquals("e05", ShowIdentity.episodeId("Serial (5)", "Jakiś dłuższy opis odcinka numer pięć."))
        assertEquals("", ShowIdentity.episodeId("Serial", null))
    }
}
