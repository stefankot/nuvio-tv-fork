package com.nuvio.tv.skin.niunia

import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.skin.niunia.livetv.NiuniaLiveFilter
import com.nuvio.tv.skin.niunia.livetv.NiuniaLiveReturn
import com.nuvio.tv.skin.niunia.livetv.isLive
import com.nuvio.tv.skin.niunia.livetv.isSeries
import com.nuvio.tv.skin.niunia.livetv.liveFraction
import com.nuvio.tv.skin.niunia.player.NiuniaPlayerBack
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testy 22.11: Live TV Niuni (powrót z odtwarzacza, stan programu) i Wstecz w nakładce odtwarzacza. */
class NiuniaLiveAndPlayerTest {
    @After fun clean() { NiuniaPlayerBack.handler = null; NiuniaLiveReturn.consume() }

    @Test fun `powrót z odtwarzacza na kafel - raz`() {
        NiuniaLiveReturn.fromTile("http://a/1.ts|100")
        NiuniaLiveReturn.commit(NiuniaLiveFilter.Favourites, dayOffset = -1)
        assertEquals(NiuniaLiveFilter.Favourites, NiuniaLiveReturn.restoredFilter())
        assertEquals(-1, NiuniaLiveReturn.restoredDay())
        assertEquals(NiuniaLiveReturn.Entry.Tile("http://a/1.ts|100"), NiuniaLiveReturn.consume())
        assertNull("drugi raz nic", NiuniaLiveReturn.consume())
        assertNull("po zużyciu filtr nie wraca", NiuniaLiveReturn.restoredFilter())
        assertEquals(0, NiuniaLiveReturn.restoredDay())
    }

    @Test fun `powrót do komórki przewodnika i z wyszukiwania`() {
        NiuniaLiveReturn.fromGuide("http://a/2.ts", 500L)
        NiuniaLiveReturn.commit(NiuniaLiveFilter.All, 0)
        assertEquals(NiuniaLiveReturn.Entry.Guide("http://a/2.ts", 500L), NiuniaLiveReturn.consume())
        NiuniaLiveReturn.fromTile("x")
        NiuniaLiveReturn.fromOther()
        NiuniaLiveReturn.commit(NiuniaLiveFilter.All, 0)
        assertNull("Szukaj/Odkrywaj bez miejsca powrotu", NiuniaLiveReturn.consume())
    }

    @Test fun `stan programu - na żywo i postęp`() {
        val p = LiveTvProgramme("Wiadomości", 1_000L, 3_000L)
        assertTrue(isLive(p, 1_000L))
        assertFalse(isLive(p, 3_000L))
        assertFalse(isLive(p, 999L))
        assertEquals(0.5f, liveFraction(p, 2_000L), 0.0001f)
        assertEquals(0f, liveFraction(p, 0L), 0f)
        assertEquals(1f, liveFraction(p, 9_000L), 0f)
        assertEquals("program o zerowej długości", 1f, liveFraction(LiveTvProgramme("x", 5L, 5L), 6L), 0f)
    }

    @Test fun `serial z pola odcinka`() {
        assertFalse(isSeries(null))
        assertTrue(isSeries(LiveTvProgramme("Klan", 0L, 1L, episode = "S3E168")))
    }

    @Test fun `Wstecz w odtwarzaczu - bez nakładki Niuni nic nie robi`() {
        assertFalse(NiuniaPlayerBack.consume())
    }

    @Test fun `Wstecz w odtwarzaczu - nakładka decyduje, błąd nie wywraca odtwarzacza`() {
        var calls = 0
        NiuniaPlayerBack.handler = { calls++; true }
        assertTrue(NiuniaPlayerBack.consume())
        NiuniaPlayerBack.handler = { false }
        assertFalse(NiuniaPlayerBack.consume())
        NiuniaPlayerBack.handler = { throw IllegalStateException("panel zamknięty") }
        assertFalse(NiuniaPlayerBack.consume())
        assertEquals(1, calls)
    }
}
