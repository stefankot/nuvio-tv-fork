package com.nuvio.tv.reshaped.sync

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VerifiedPlaylistCopyTest {
    private val playlist = "#EXTM3U\n#EXTINF:-1,Example\nhttps://example.com/live\n".toByteArray()
    private val hash = MessageDigest.getInstance("SHA-1").digest(playlist).joinToString("") { "%02x".format(it) }

    @Test
    fun checksTheSameBytesInPlainAndCompressedPlaylists() {
        val compressed = ByteArrayOutputStream().also { buffer ->
            GZIPOutputStream(buffer).use { it.write(playlist) }
        }.toByteArray()
        for (bytes in listOf(playlist, compressed)) {
            val result = ByteArrayOutputStream()
            copyVerifiedPlaylist(bytes.inputStream(), result, hash, playlist.size.toLong())
            assertArrayEquals(playlist, result.toByteArray())
        }
    }

    @Test
    fun rejectsAnIncorrectHashAndAnOversizedPlaylist() {
        assertThrows(IOException::class.java) {
            copyVerifiedPlaylist(playlist.inputStream(), ByteArrayOutputStream(), "0".repeat(40), 4096)
        }
        assertThrows(IOException::class.java) {
            copyVerifiedPlaylist(playlist.inputStream(), ByteArrayOutputStream(), hash, playlist.size.toLong() - 1)
        }
    }
}
