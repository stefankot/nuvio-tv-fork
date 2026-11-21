package com.nuvio.tv.reshaped.sync

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/** Copies into a temporary file, matching the existing Drive ref against the decompressed bytes. */
internal fun copyVerifiedPlaylist(input: InputStream, output: OutputStream, expectedHash: String, maxBytes: Long) {
    val buffered = input.buffered(64 * 1024)
    buffered.mark(2)
    val gzipped = buffered.read() == 0x1f && buffered.read() == 0x8b
    buffered.reset()
    val digest = MessageDigest.getInstance("SHA-1")
    var written = 0L
    (if (gzipped) GZIPInputStream(buffered, 64 * 1024) else buffered).use { body ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = body.read(buffer)
            if (read < 0) break
            written += read
            if (written > maxBytes) throw IOException("Synced playlist is too large")
            digest.update(buffer, 0, read)
            output.write(buffer, 0, read)
        }
    }
    val hash = digest.digest().joinToString("") { "%02x".format(it) }
    if (written == 0L || !hash.equals(expectedHash, ignoreCase = true)) {
        throw IOException("Synced playlist does not match its checksum")
    }
}
