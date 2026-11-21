package com.nuvio.tv.ui.screens.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpRangeRepresentationTest {
    @Test
    fun parsesByteRangesAndRejectsInconsistentFileLengths() {
        assertEquals(HttpRangeRepresentation(25, 49, 100), HttpRangeRepresentation.parse("bytes 25-49/100"))
        assertEquals(HttpRangeRepresentation(25, 49, null), HttpRangeRepresentation.parse("bytes 25-49/*"))
        assertNull(HttpRangeRepresentation.parse("bytes 50-25/100"))
        assertNull(HttpRangeRepresentation.parse("bytes 25-100/100"))
    }

    @Test
    fun differentFileVersionsCannotShareParallelChunks() {
        val original = HttpRangeVersion.from(mapOf("ETag" to listOf("\"one\""), "Content-Range" to listOf("bytes 0-49/100")))
        assertFalse(original.accepts(HttpRangeVersion.from(mapOf("etag" to listOf("\"two\"")))))
        assertFalse(original.accepts(HttpRangeVersion.from(mapOf("content-range" to listOf("bytes 0-49/90")))))
        assertTrue(original.accepts(HttpRangeVersion.from(mapOf("ETAG" to listOf("\"one\""), "Content-Range" to listOf("bytes 50-99/100")))))
    }
}
