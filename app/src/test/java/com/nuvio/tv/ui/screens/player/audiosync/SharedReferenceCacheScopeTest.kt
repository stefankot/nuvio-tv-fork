package com.nuvio.tv.ui.screens.player.audiosync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedReferenceCacheScopeTest {
    @Test
    fun sameUrlCannotShareDialogueAcrossContentLanguagesProfilesOrHeaders() {
        val key = SharedReferenceCache.Key(1, "example-title", "https://example.com/ref.srt", "pl", mapOf("X-Language" to "pl"))
        val cues = listOf(Triple(0L, 1000L, "Przykład"))
        SharedReferenceCache.put(key, cues)
        assertEquals(cues, SharedReferenceCache.get(key.copy(headers = key.headers.toMap())))
        assertNull(SharedReferenceCache.get(key.copy(profileId = 2)))
        assertNull(SharedReferenceCache.get(key.copy(content = "other-title")))
        assertNull(SharedReferenceCache.get(key.copy(language = "en")))
        assertNull(SharedReferenceCache.get(key.copy(headers = emptyMap())))
    }
}
