package com.nuvio.tv.ainarrator

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Only this class knows the external TTS wire format. Never logs credentials or subtitles. */
internal class GeminiNarratorTts {
    suspend fun synthesize(apiKey: String, model: NarratorModel, voice: String, text: String, style: String): ByteArray =
        withContext(Dispatchers.IO) {
            require(apiKey.isNotBlank())
            require(text.isNotBlank() && text.length <= 1_200) // fork 19.11: scena (kilka kwestii) w jednym zapytaniu
            val part = JSONObject().put("text", text).put("speechMetadata", JSONObject().put("style", style))
            val payload = JSONObject()
                .put("contents", JSONArray().put(JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(part))))
                .put("generationConfig", JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put("seed", 7) // fork 19.11: stałe ziarno — ta sama barwa głosu w każdej kwestii
                    .put("speechConfig", JSONObject()
                        .put("voiceConfig", JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", voice)))))
            val connection = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/${model.apiId}:generateContent"
            ).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000 // fork 19.11: kwestia po 15 s i tak jest spóźniona (było 35 s)
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("x-goog-api-key", apiKey)
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                if (status !in 200..299) {
                    // Do not expose server error bodies: they can contain sensitive request data.
                    throw IllegalStateException("TTS HTTP $status")
                }
                val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                    val body = StringBuilder()
                    val buf = CharArray(8192)
                    while (true) {
                        val count = reader.read(buf)
                        if (count < 0) break
                        body.append(buf, 0, count)
                        if (body.length > 12_000_000) throw IllegalStateException("TTS response too large")
                    }
                    body.toString()
                }
                val candidates = JSONObject(response).optJSONArray("candidates")
                    ?: throw IllegalStateException("No TTS candidates")
                val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                var wav: ByteArray? = null
                for (index in 0 until parts.length()) {
                    val inline = parts.getJSONObject(index).optJSONObject("inlineData")
                    val value = inline?.optString("data").orEmpty()
                    if (value.isNotEmpty()) {
                        val bytes = Base64.decode(value, Base64.DEFAULT)
                        // fork: older TTS models return raw PCM (audio/L16;rate=24000) instead of WAV.
                        wav = if (WavValidation.isWav(bytes)) bytes else PcmWav.wrap(bytes, inline?.optString("mimeType").orEmpty())
                        break
                    }
                }
                val result = wav ?: throw IllegalStateException("No audio in TTS response")
                if (!WavValidation.isWav(result)) {
                    throw IllegalStateException("Unsupported TTS output; expected WAV")
                }
                result
            } finally {
                connection.disconnect()
            }
        }
}

internal class NarratorAudioCache(private val directory: File, private val maxBytes: Long = 96L * 1024 * 1024) {
    init { directory.mkdirs() }
    private val sha256Key = Regex("[a-f0-9]{64}")

    fun find(key: String, touch: Boolean = false): File? {
        if (!sha256Key.matches(key)) return null
        return File(directory, "$key.wav")
            .takeIf { it.isFile && it.length() > 44 }
            ?.also { if (touch) it.setLastModified(System.currentTimeMillis()) }
    }

    @Synchronized fun store(key: String, audio: ByteArray): File {
        require(sha256Key.matches(key))
        require(WavValidation.isWav(audio)) { "Invalid WAV cache payload" }
        val dest = File(directory, "$key.wav")
        if (dest.isFile && dest.length() > 44) return dest
        val temp = File.createTempFile("$key.", ".tmp", directory)
        try {
            temp.writeBytes(audio)
            if (!temp.renameTo(dest)) {
                if (!dest.isFile) throw IllegalStateException("Unable to commit narrator cache")
            }
        } finally {
            temp.delete()
        }
        trim()
        return dest
    }

    @Synchronized private fun trim() {
        val files = directory.listFiles { f -> f.isFile && f.extension == "wav" }
            ?.sortedByDescending(File::lastModified) ?: return
        var total = 0L
        files.forEach { file ->
            total += file.length()
            if (total > maxBytes) file.delete()
        }
    }

    @Synchronized fun clear() {
        directory.listFiles()?.filter { it.isFile && (it.extension == "wav" || it.extension == "tmp") }
            ?.forEach(File::delete)
    }
}

/** Fork: wraps 16-bit mono PCM in a WAV header (rate taken from the mime type, default 24 kHz). */
internal object PcmWav {
    fun wrap(pcm: ByteArray, mimeType: String): ByteArray {
        if (!mimeType.contains("L16", ignoreCase = true) && !mimeType.contains("pcm", ignoreCase = true)) return pcm
        val rate = Regex("rate=(\\d+)").find(mimeType)?.groupValues?.get(1)?.toIntOrNull() ?: 24_000
        val header = java.nio.ByteBuffer.allocate(44).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVE".toByteArray())
            .put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2)
            .putShort(2).putShort(16).put("data".toByteArray()).putInt(pcm.size)
        return header.array() + pcm
    }
}
