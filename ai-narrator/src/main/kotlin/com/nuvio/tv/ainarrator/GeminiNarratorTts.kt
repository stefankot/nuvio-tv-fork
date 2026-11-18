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
            require(text.isNotBlank() && text.length <= 380)
            val part = JSONObject().put("text", text).put("speechMetadata", JSONObject().put("style", style))
            val payload = JSONObject()
                .put("contents", JSONArray().put(JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(part))))
                .put("generationConfig", JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put("speechConfig", JSONObject()
                        .put("voiceConfig", JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", voice)))))
            val connection = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/${model.apiId}:generateContent"
            ).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 10_000
                connection.readTimeout = 35_000
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
                    val value = parts.getJSONObject(index).optJSONObject("inlineData")?.optString("data").orEmpty()
                    if (value.isNotEmpty()) {
                        wav = Base64.decode(value, Base64.DEFAULT)
                        break
                    }
                }
                val result = wav ?: throw IllegalStateException("No audio in TTS response")
                if (result.size !in 44..8_000_000 || String(result, 0, 4, Charsets.US_ASCII) != "RIFF" ||
                    String(result, 8, 4, Charsets.US_ASCII) != "WAVE") {
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
    fun find(key: String): File? = File(directory, "$key.wav")
        .takeIf { key.matches(Regex("[a-f0-9]{64}")) && it.isFile && it.length() > 44 }
        ?.also { it.setLastModified(System.currentTimeMillis()) }

    @Synchronized fun store(key: String, audio: ByteArray): File {
        require(key.matches(Regex("[a-f0-9]{64}")))
        val dest = File(directory, "$key.wav")
        val temp = File(directory, "$key.tmp")
        temp.writeBytes(audio)
        if (!temp.renameTo(dest)) {
            temp.delete()
            throw IllegalStateException("Unable to write narrator cache")
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

    fun clear() {
        directory.listFiles()?.filter { it.isFile && (it.extension == "wav" || it.extension == "tmp") }
            ?.forEach(File::delete)
    }
}
