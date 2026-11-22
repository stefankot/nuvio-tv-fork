package com.nuvio.tv.fork.lektor

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections

/**
 * Pamięć nagrań lektora (22.11, właściciel: „duży cache z Dyskiem Google, przed generowaniem sprawdzaj Dysk, WAV → Opus”).
 *
 * Było: każda kwestia Gemini jako WAV 16-bit mono 24 kHz (ok. 48 KB/s) w `cacheDir/ai-narrator`, limit 96 MB.
 * Jest: Ogg Opus mono 24 kb/s (ok. 3 KB/s, ~16× mniej) w `filesDir/fork_lektor_opus`, limit 500 MB, oraz kopia
 * w folderze „Nuvio Lektor” na Dysku Google konta zalogowanego w synchronizacji (Ustawienia → Synchronizacja Google).
 * Kolejność przed generowaniem: stary WAV (zostaje, czytany jak dotąd) → Opus na TV → Dysk → dopiero Gemini.
 * Nazwa pliku = odcisk kwestii (model + głos + tekst + styl), więc ta sama kwestia z innego TV tego samego konta Google
 * (np. rodzinne TV zalogowane na konto właściciela) jest pobierana z Dysku, nie generowana drugi raz.
 * Opus wymaga Androida 10+ (koder systemowy); starsze — WAV jak dotąd.
 */
object LektorStore {
    private const val TAG = "ForkLektor"
    const val KEY = "lektor_store_opus"
    const val KEY_DRIVE = "lektor_drive"
    private const val LIMIT_BYTES = 500L * 1024 * 1024
    private const val BITRATE = 24_000
    private val idRegex = Regex("[a-f0-9]{64}")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val misses = Collections.synchronizedSet(HashSet<String>())

    private fun dir(c: Context) = File(c.applicationContext.filesDir, "fork_lektor_opus").apply { mkdirs() }
    private fun enabled(c: Context) = Build.VERSION.SDK_INT >= 29 && ForkFeatures.enabled(c, KEY, true)

    /** Nagranie (WAV) z pamięci Opus albo z Dysku; null = trzeba wygenerować. */
    suspend fun find(c: Context, id: String): ByteArray? {
        if (!enabled(c) || !idRegex.matches(id)) return null
        val f = File(dir(c), "$id.ogg")
        if (f.isFile && f.length() > 0) {
            decodeOrNull(f, "pamięć")?.let { f.setLastModified(System.currentTimeMillis()); return it }
        }
        if (!ForkFeatures.enabled(c, KEY_DRIVE, true) || id in misses) return null
        return try {
            val fileId = LektorDrive.find(c, "$id.ogg") ?: run { misses += id; return null }
            val tmp = File(dir(c), "$id.part")
            LektorDrive.download(c, fileId, tmp)
            if (!tmp.renameTo(f)) throw IOException("rename ${tmp.name}")
            Log.i(TAG, "pamięć: kwestia ${id.take(8)} z Dysku (${f.length() / 1024} KB) — bez generowania")
            trim(c)
            decodeOrNull(f, "Dysk")
        } catch (e: CancellationException) {
            throw e
        } catch (e: LektorDrive.SignedOut) {
            misses += id; null // bez konta Google: tylko pamięć na TV (zalogowanie: Ustawienia → Synchronizacja)
        } catch (e: Exception) {
            Log.w(TAG, "pamięć: Dysk, kwestia ${id.take(8)}: ${e.javaClass.simpleName}: ${e.message}")
            misses += id; null
        }
    }

    /** Zapis nowego nagrania jako Opus (+ wysyłka na Dysk w tle). false = Opus niedostępny, zapisz WAV po staremu. */
    fun put(c: Context, id: String, wav: ByteArray): Boolean {
        if (Build.VERSION.SDK_INT < 29 || !enabled(c) || !idRegex.matches(id)) return false
        val (pcm, rate) = pcmOf(wav) ?: run { Log.w(TAG, "pamięć: kwestia ${id.take(8)}: nagranie nie jest WAV 16-bit mono"); return false }
        val f = File(dir(c), "$id.ogg")
        val tmp = File(dir(c), "$id.tmp")
        return try {
            encode(pcm, rate, tmp)
            if (!tmp.renameTo(f)) throw IOException("rename ${tmp.name}")
            Log.i(TAG, "pamięć: kwestia ${id.take(8)} zapisana jako Opus ${f.length() / 1024} KB (WAV było ${wav.size / 1024} KB)")
            misses -= id
            trim(c)
            if (ForkFeatures.enabled(c, KEY_DRIVE, true)) scope.launch {
                try { LektorDrive.upload(c.applicationContext, f) }
                catch (e: LektorDrive.SignedOut) { Log.i(TAG, "pamięć: Dysk pominięty — brak konta Google w synchronizacji") }
                catch (e: Exception) { Log.w(TAG, "pamięć: wysyłka na Dysk ${id.take(8)}: ${e.javaClass.simpleName}: ${e.message}") }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "pamięć: Opus nie wyszedł (${id.take(8)}, $rate Hz): ${e.javaClass.simpleName}: ${e.message} — zapis WAV")
            tmp.delete(); f.delete(); false
        }
    }

    fun clear(c: Context) { dir(c).listFiles()?.forEach { it.delete() }; misses.clear(); Log.i(TAG, "pamięć Opus wyczyszczona (Dysk bez zmian)") }

    private fun trim(c: Context) {
        val files = dir(c).listFiles { f -> f.extension == "ogg" }?.sortedByDescending { it.lastModified() } ?: return
        var total = 0L
        files.forEach { f -> total += f.length(); if (total > LIMIT_BYTES) f.delete() }
    }

    private fun decodeOrNull(f: File, from: String): ByteArray? = try {
        decode(f)
    } catch (e: Exception) {
        Log.w(TAG, "pamięć: odczyt Opus ($from) ${f.name.take(8)}: ${e.javaClass.simpleName}: ${e.message} — plik usunięty")
        f.delete(); null
    }

    // ------------------------------------------------------------------ WAV ↔ PCM

    /** (próbki, częstotliwość) z WAV 16-bit mono; null = inny format. */
    fun pcmOf(b: ByteArray): Pair<ShortArray, Int>? {
        if (b.size < 44 || String(b, 0, 4, Charsets.US_ASCII) != "RIFF" || String(b, 8, 4, Charsets.US_ASCII) != "WAVE") return null
        val bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 12; var rate = 0; var ch = 0; var bits = 0
        while (pos + 8 <= b.size) {
            val id = String(b, pos, 4, Charsets.US_ASCII); val len = bb.getInt(pos + 4).coerceAtLeast(0); val body = pos + 8
            if (id == "fmt " && body + 16 <= b.size) { ch = bb.getShort(body + 2).toInt(); rate = bb.getInt(body + 4); bits = bb.getShort(body + 14).toInt() }
            if (id == "data") {
                if (ch != 1 || bits != 16 || rate <= 0) return null
                val end = minOf(b.size, body + len)
                val out = ShortArray((end - body) / 2)
                ByteBuffer.wrap(b, body, out.size * 2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(out)
                return out to rate
            }
            pos = body + len + (len and 1)
        }
        return null
    }

    fun wavOf(pcm: ShortArray, rate: Int): ByteArray {
        val data = pcm.size * 2
        val bb = ByteBuffer.allocate(44 + data).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray(Charsets.US_ASCII)).putInt(36 + data).put("WAVE".toByteArray(Charsets.US_ASCII))
        bb.put("fmt ".toByteArray(Charsets.US_ASCII)).putInt(16).putShort(1).putShort(1).putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        bb.put("data".toByteArray(Charsets.US_ASCII)).putInt(data)
        bb.asShortBuffer().put(pcm)
        return bb.array()
    }

    // ------------------------------------------------------------------ Opus (koder systemowy, Android 10+)

    @RequiresApi(29)
    private fun encode(pcm: ShortArray, rate: Int, out: File) {
        val fmt = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_OPUS, rate, 1).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, BITRATE); setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_OPUS)
        val muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)
        val bytes = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN).apply { asShortBuffer().put(pcm) }.array()
        var track = -1; var started = false
        try {
            codec.configure(fmt, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); codec.start()
            val info = MediaCodec.BufferInfo()
            var inPos = 0; var inputDone = false; var guard = 0
            while (guard++ < 100_000) {
                if (!inputDone) {
                    val i = codec.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val buf = codec.getInputBuffer(i)!!; buf.clear()
                        val n = minOf(buf.remaining() and 1.inv(), bytes.size - inPos)
                        buf.put(bytes, inPos, n)
                        val pts = (inPos / 2).toLong() * 1_000_000L / rate
                        inPos += n
                        inputDone = inPos >= bytes.size
                        codec.queueInputBuffer(i, 0, n, pts, if (inputDone) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0)
                    }
                }
                val o = codec.dequeueOutputBuffer(info, 10_000)
                if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) { track = muxer.addTrack(codec.outputFormat); muxer.start(); started = true }
                else if (o >= 0) {
                    val ob = codec.getOutputBuffer(o)!!
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0 && info.size > 0 && started) muxer.writeSampleData(track, ob, info)
                    codec.releaseOutputBuffer(o, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                }
            }
            if (!started) throw IOException("koder nie podał formatu")
        } finally {
            try { codec.stop() } catch (e: IllegalStateException) { Log.w(TAG, "pamięć: koder stop: ${e.message}") }
            codec.release()
            if (started) try { muxer.stop() } catch (e: IllegalStateException) { Log.w(TAG, "pamięć: muxer stop: ${e.message}") }
            muxer.release()
        }
    }

    private fun decode(f: File): ByteArray {
        val ex = MediaExtractor()
        try {
            ex.setDataSource(f.path)
            if (ex.trackCount < 1) throw IOException("brak ścieżki")
            ex.selectTrack(0)
            val fmt = ex.getTrackFormat(0)
            val codec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_AUDIO_OPUS)
            var rate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE); var ch = 1
            val out = ByteArrayOutputStream()
            try {
                codec.configure(fmt, null, null, 0); codec.start()
                val info = MediaCodec.BufferInfo(); var inputDone = false; var guard = 0
                while (guard++ < 100_000) {
                    if (!inputDone) {
                        val i = codec.dequeueInputBuffer(10_000)
                        if (i >= 0) {
                            val n = ex.readSampleData(codec.getInputBuffer(i)!!, 0)
                            if (n < 0) { codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true }
                            else { codec.queueInputBuffer(i, 0, n, ex.sampleTime, 0); ex.advance() }
                        }
                    }
                    val o = codec.dequeueOutputBuffer(info, 10_000)
                    if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        rate = codec.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE); ch = codec.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1)
                    } else if (o >= 0) {
                        val ob = codec.getOutputBuffer(o)!!
                        val chunk = ByteArray(info.size); ob.position(info.offset); ob.get(chunk); out.write(chunk)
                        codec.releaseOutputBuffer(o, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                    }
                }
            } finally {
                try { codec.stop() } catch (e: IllegalStateException) { Log.w(TAG, "pamięć: dekoder stop: ${e.message}") }
                codec.release()
            }
            val raw = out.toByteArray()
            val all = ShortArray(raw.size / 2).also { ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(it) }
            val mono = if (ch == 1) all else ShortArray(all.size / ch) { k -> (0 until ch).sumOf { all[k * ch + it].toInt() }.div(ch).toShort() }
            if (mono.isEmpty()) throw IOException("puste nagranie")
            return wavOf(mono, rate)
        } finally {
            ex.release()
        }
    }
}

/** Folder „Nuvio Lektor” na Dysku konta z synchronizacji Google (zakres drive.file: aplikacja widzi tylko swoje pliki). */
internal object LektorDrive {
    class SignedOut : Exception("Brak konta Google")
    private const val TAG = "ForkLektor"
    private const val FOLDER = "Nuvio Lektor"
    private const val FILES = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id"
    private val lock = Mutex()
    @Volatile private var folderId: String? = null

    private suspend fun <T> call(c: Context, build: (String) -> Request, read: (okhttp3.Response) -> T): T {
        repeat(2) { attempt ->
            val token = com.nuvio.tv.reshaped.sync.GoogleAccount.accessToken(c, forceRefresh = attempt > 0) ?: throw SignedOut()
            val r = withContext(Dispatchers.IO) {
                com.nuvio.tv.reshaped.sync.GoogleAccount.http.newCall(build(token)).execute().use { resp ->
                    when {
                        resp.isSuccessful -> Result.success(read(resp))
                        resp.code == 401 -> null
                        else -> Result.failure(IOException("Drive HTTP ${resp.code}: ${resp.body?.string()?.take(160)}"))
                    }
                }
            }
            if (r != null) return r.getOrThrow()
        }
        throw SignedOut()
    }

    private suspend fun folder(c: Context): String = lock.withLock {
        folderId?.let { return it }
        val prefs = c.getSharedPreferences("fork_lektor_drive", Context.MODE_PRIVATE)
        prefs.getString("folder", null)?.let { folderId = it; return it }
        val url = FILES.toHttpUrl().newBuilder()
            .addQueryParameter("q", "name = '$FOLDER' and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
            .addQueryParameter("fields", "files(id)").addQueryParameter("spaces", "drive").build()
        val found = call(c, { t -> Request.Builder().url(url).header("Authorization", "Bearer $t").build() }) { r ->
            JSONObject(r.body?.string().orEmpty()).optJSONArray("files")?.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }
        }
        val id = found ?: call(c, { t ->
            val meta = JSONObject().put("name", FOLDER).put("mimeType", "application/vnd.google-apps.folder")
            Request.Builder().url("$FILES?fields=id").header("Authorization", "Bearer $t")
                .post(meta.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())).build()
        }) { r -> JSONObject(r.body?.string().orEmpty()).getString("id") }.also { Log.i(TAG, "pamięć: utworzony folder „$FOLDER” na Dysku") }
        prefs.edit().putString("folder", id).apply()
        folderId = id
        id
    }

    /** Id pliku [name] w folderze lektora albo null. */
    suspend fun find(c: Context, name: String): String? {
        val parent = folder(c)
        val url = FILES.toHttpUrl().newBuilder()
            .addQueryParameter("q", "name = '$name' and '$parent' in parents and trashed = false")
            .addQueryParameter("fields", "files(id)").addQueryParameter("pageSize", "1").build()
        return call(c, { t -> Request.Builder().url(url).header("Authorization", "Bearer $t").build() }) { r ->
            JSONObject(r.body?.string().orEmpty()).optJSONArray("files")?.optJSONObject(0)?.optString("id")?.takeIf { it.isNotBlank() }
        }
    }

    suspend fun download(c: Context, id: String, dest: File) {
        call(c, { t -> Request.Builder().url("$FILES/$id?alt=media").header("Authorization", "Bearer $t").build() }) { r ->
            val body = r.body ?: throw IOException("pusty plik")
            dest.outputStream().use { o -> body.byteStream().use { it.copyTo(o) } }
        }
    }

    /** Wysyła plik, jeśli jeszcze go nie ma w folderze. */
    suspend fun upload(c: Context, f: File) {
        if (find(c, f.name) != null) return
        val parent = folder(c)
        call(c, { t ->
            val meta = JSONObject().put("name", f.name).put("parents", org.json.JSONArray().put(parent))
            val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
                .addPart(meta.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
                .addPart(f.asRequestBody("audio/ogg".toMediaType())).build()
            Request.Builder().url(UPLOAD).header("Authorization", "Bearer $t").post(body).build()
        }) { }
        Log.i(TAG, "pamięć: ${f.name.take(8)} wysłana na Dysk (${f.length() / 1024} KB)")
    }
}
