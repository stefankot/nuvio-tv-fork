package com.nuvio.tv.fork

import com.nuvio.tv.fork.images.ForkImageQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageQualityTest {
    @Test fun `TMDB i metahub w najwiekszym rozmiarze`() {
        assertEquals("https://image.tmdb.org/t/p/original/abc.jpg", ForkImageQuality.upgrade("https://image.tmdb.org/t/p/w1280/abc.jpg"))
        assertEquals("https://image.tmdb.org/t/p/original/x.png", ForkImageQuality.upgrade("https://image.tmdb.org/t/p/h632/x.png"))
        assertEquals("https://images.metahub.space/background/large/tt0137523/img", ForkImageQuality.upgrade("https://images.metahub.space/background/medium/tt0137523/img"))
        assertNull(ForkImageQuality.upgrade("https://image.tmdb.org/t/p/original/abc.jpg"))
        assertNull(ForkImageQuality.upgrade("https://example.com/a.jpg"))
    }
}
