package com.nuvio.tv.skin.niunia

import com.nuvio.tv.skin.niunia.livetv.NiuniaLiveReturn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** ZASADY §3 (logi bez haseł i tokenów): adres strumienia IPTV ma login i hasło w ścieżce — w logu tylko etykieta (pętla 0 UX JMGO). */
class NiuniaLiveReturnTest {
    private val url = "https://live.example.test/live/uzytkownik/haslo123/42.ts"

    @Test fun labelHidesTheAddress() {
        val l = NiuniaLiveReturn.logLabel(url)
        assertFalse(l, "haslo123" in l || "uzytkownik" in l || "http" in l || "example" in l)
        assertTrue(l, l.startsWith("#"))
    }

    @Test fun labelIsStableAndDistinguishesChannels() {
        assertEquals(NiuniaLiveReturn.logLabel(url), NiuniaLiveReturn.logLabel(url))
        assertNotEquals(NiuniaLiveReturn.logLabel(url), NiuniaLiveReturn.logLabel("$url?x=1"))
    }

    @Test fun tileKeyKeepsProgrammeTimeButNotTheAddress() {
        val l = NiuniaLiveReturn.logLabel("$url|1732470000000")
        assertTrue(l, l.endsWith("|1732470000000"))
        assertFalse(l, "haslo123" in l)
    }

    @Test fun entriesPrintWithoutTheAddress() {
        assertFalse(NiuniaLiveReturn.Entry.Guide(url, 5L).toString().let { "haslo123" in it || "http" in it })
        assertFalse(NiuniaLiveReturn.Entry.Tile("$url|7").toString().let { "haslo123" in it || "http" in it })
        assertTrue(NiuniaLiveReturn.Entry.Guide(url, 5L).toString().contains("startMs=5"))
    }
}
