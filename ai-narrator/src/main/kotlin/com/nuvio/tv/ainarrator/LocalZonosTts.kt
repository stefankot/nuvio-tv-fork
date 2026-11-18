package com.nuvio.tv.ainarrator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Adapter to Zyphra's locally hosted ZONOS3-Mini HTTP API.
 * The Hugging Face demo is NOT used as an inference service.
 * A separate local machine/server runs ZONOS3 and hosts its model weights.
 */
internal class LocalZonosTts {
    private val automaticSpeakers = ConcurrentHashMap<String, String>()

    suspend fun synthesize(
        endpoint: String,
        accessToken: String,
        speakerId: String,
        text: String
    ): ByteArray = withContext(Dispatchers.IO) {
        val base = ZonosEndpoint.parse(endpoint)
        require(text.isNotBlank() && text.length <= 380)
        // Never send subtitles to arbitrary public hosts or follow server redirects.
        require(ZonosEndpoint.isPrivateHost(base)) { "Serwer musi być w sieci lokalnej" }
        val effectiveSpeaker = speakerId.trim().takeIf { it.isNotEmpty() }
            ?: automaticSpeakers[base.toString()]
            ?: fetchFirstSpeaker(base, accessToken).also { automaticSpeakers[base.toString()] = it }
        val body = JSONObject()
            .put("text", text)
            .put("language", "pl")
            .put("text_normalization", true)
            .put("speaker_embedding_id", effectiveSpeaker)
            .put("short_text", "off")
            .put("stream", false)
            .put("format", "wav")
        val request = open(base.resolve("/v2/generate").toURL(), accessToken, "POST")
        try {
            request.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            request.doOutput = true
            request.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            if (request.responseCode !in 200..299) {
                throw IllegalStateException("ZONOS3 HTTP ${request.responseCode}")
            }
            val wav = readBounded(request, MAX_WAV_BYTES)
            require(WavValidation.isWav(wav)) { "ZONOS3 nie zwrócił WAV" }
            wav
        } finally {
            request.disconnect()
        }
    }

    private fun fetchFirstSpeaker(base: URI, token: String): String {
        val request = open(base.resolve("/v2/speakers").toURL(), token, "GET")
        try {
            if (request.responseCode !in 200..299) {
                throw IllegalStateException("ZONOS3 speakers HTTP ${request.responseCode}")
            }
            val response = String(readBounded(request, 256 * 1024), Charsets.UTF_8)
            val speakers = JSONObject(response).optJSONArray("speakers")
                ?: throw IllegalStateException("Serwer nie udostępnia głosów")
            for (i in 0 until speakers.length()) {
                val id = speakers.optJSONObject(i)?.optString("id").orEmpty()
                if (id.isNotBlank()) return id
            }
            throw IllegalStateException("Brak domyślnego głosu ZONOS3")
        } finally {
            request.disconnect()
        }
    }

    private fun open(url: URL, token: String, method: String): HttpURLConnection =
        (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 4_000
            readTimeout = 30_000
            instanceFollowRedirects = false
            useCaches = false
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }

    private fun readBounded(connection: HttpURLConnection, maxBytes: Int): ByteArray {
        val expected = connection.contentLengthLong
        if (expected > maxBytes) throw IllegalStateException("Odpowiedź ZONOS3 za duża")
        val out = ByteArrayOutputStream(if (expected > 0L) expected.toInt() else 8192)
        connection.inputStream.use { stream ->
            val buf = ByteArray(8192)
            while (true) {
                val count = stream.read(buf)
                if (count < 0) break
                if (out.size() + count > maxBytes) throw IllegalStateException("Odpowiedź ZONOS3 za duża")
                out.write(buf, 0, count)
            }
        }
        return out.toByteArray()
    }

    private companion object { const val MAX_WAV_BYTES = 8_000_000 }
}

/** Input validation is pure; resolving the private host occurs only on the IO dispatcher. */
internal object ZonosEndpoint {
    fun parse(input: String): URI {
        val uri = try { URI(input.trim()) } catch (_: Exception) {
            throw IllegalArgumentException("Nieprawidłowy adres serwera")
        }
        require(uri.scheme?.lowercase() in setOf("http", "https")) { "Wymagany adres HTTP(S)" }
        require(!uri.host.isNullOrBlank() && uri.userInfo == null && uri.rawQuery == null && uri.rawFragment == null) {
            "Nieprawidłowy adres serwera"
        }
        require(uri.path.isNullOrEmpty() || uri.path == "/") { "Podaj adres bazowy serwera bez ścieżki" }
        require(uri.port in 1..65535 || (uri.port == -1 && uri.scheme.equals("https", true))) {
            "Podaj numer portu serwera"
        }
        return URI(uri.scheme.lowercase(), null, uri.host, uri.port, "/", null, null)
    }

    fun isPrivateHost(uri: URI): Boolean =
        runCatching {
            InetAddress.getAllByName(uri.host).let { addresses ->
                addresses.isNotEmpty() && addresses.all {
                    it.isSiteLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress
                }
            }
        }.getOrDefault(false)
}

internal object WavValidation {
    fun isWav(data: ByteArray): Boolean =
        data.size in 44..8_000_000 &&
            data.copyOfRange(0, 4).contentEquals(byteArrayOf(82, 73, 70, 70)) &&
            data.copyOfRange(8, 12).contentEquals(byteArrayOf(87, 65, 86, 69))
}
