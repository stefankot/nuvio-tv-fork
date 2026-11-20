package com.nuvio.tv.fork.yt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeTextSourceTest {
    @Test fun `id filmu z lokalnego adresu mpd`() {
        assertEquals("dQw4w9WgXcQ", YouTubeTextSource.videoId("http://127.0.0.1:8123/yt/dQw4w9WgXcQ.mpd"))
        assertNull(YouTubeTextSource.videoId("https://example.com/video.mp4"))
        assertNull(YouTubeTextSource.videoId(null))
    }
}
