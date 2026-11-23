package com.nuvio.tv.fork.tv

import com.nuvio.tv.fork.tv.vod.LiveVod
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 23.11 (JMGO): filmy i seriale z list IPTV nie są kanałami. */
class LiveVodTest {
    @Test fun `filmy i seriale - pliki, posrednik z rozszerzeniem przed m3u8, sciezki Xtream`() {
        assertTrue(LiveVod.isVodUrl("https://p.example/p/abcdefghijklmnopqrs/abc/123456/Film%20(2020)%3Amkv.m3u8"))
        assertTrue(LiveVod.isVodUrl("https://p.example/p/abcdefghijklmnopqrs/abc/123456/Serial%20S01E02%3AMP4.m3u8?t=1"))
        assertTrue(LiveVod.isVodUrl("http://panel.example:8080/movie/u/p/12345.mkv"))
        assertTrue(LiveVod.isVodUrl("http://panel.example:8080/series/u/p/777.mp4"))
        assertTrue(LiveVod.isVodUrl("http://cdn.example/vod/film.avi"))
    }

    @Test fun `kanaly na zywo zostaja`() {
        assertFalse(LiveVod.isVodUrl("http://panel.example:8080/live/u/p/1234.ts"))
        assertFalse(LiveVod.isVodUrl("https://cdn.example/tvn/index.m3u8"))
        assertFalse(LiveVod.isVodUrl("https://cdn.example/stream/chunklist_w12.m3u8?token=a.mkv"))
        assertFalse(LiveVod.isVodUrl("http://panel.example:8080/u/p/1234"))
        assertFalse(LiveVod.isVodUrl("rtmp://host.example"))
        assertFalse(LiveVod.isVodUrl("https://p.example/p/abc/Kanal%3Ats.m3u8"))
    }
}
