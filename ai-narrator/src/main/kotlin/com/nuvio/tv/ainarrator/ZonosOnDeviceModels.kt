package com.nuvio.tv.ainarrator

import android.content.Context
import android.os.StatFs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

internal enum class ModelPhase { CHECKING, MISSING, DOWNLOADING, READY, ERROR }

internal data class ModelDownloadStatus(
    val phase: ModelPhase,
    val downloadedBytes: Long = 0L,
    val expectedBytes: Long = 1_115_476_064L,
    val message: String = ""
)

internal data class ZonosModelPaths(val backbone: File, val decoder: File)

internal data class ZonosModelSpec(val name: String, val sha256: String, val maxBytes: Long) {
    val url: String get() = "https://huggingface.co/Zyphra/ZONOS3-Mini/resolve/7146a686b62bcb0661528fa300984e815e10152b/gguf/" + name
}

/** Exact upstream SHA-256 digests. Only the minimal backbone and audio decoder are downloaded. */
internal object ZonosModelManifest {
    val files = listOf(
        ZonosModelSpec("backbone-ptq.gguf",
            "66fceb6d73a8c30bde80249e4112bb308380ab7f3384fa11c8e4048115e4ceec", 663_150_624L),
        ZonosModelSpec("zac-keep2.gguf",
            "834e5b70b88294afc3c702a478487e0664dcc6b574c16fc6167d72444aabeafd", 452_325_440L)
    )
    fun allowDownloadUrl(url: URL): Boolean {
        val host = url.host.lowercase(Locale.ROOT)
        return url.protocol.equals("https", ignoreCase = true) && url.userInfo == null &&
            (host == "huggingface.co" || host.endsWith(".hf.co") ||
                host.endsWith(".xethub.hf.co"))
    }
}

/**
 * User-initiated, resumable download into PRIVATE persistent Android TV storage.
 * Does not run on launch. Only verified GGUFs may be handed to a native inference engine.
 */
internal class ZonosOnDeviceModels(context: Context) {
    private val dir = File(context.applicationContext.filesDir, "zonos3-mini/v1")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var downloadJob: Job? = null
    private val _status = MutableStateFlow(ModelDownloadStatus(ModelPhase.CHECKING))
    val status: StateFlow<ModelDownloadStatus> = _status

    init {
        dir.mkdirs()
        scope.launch { mutex.withLock { inspectFiles() } }
    }

    fun modelPaths(): ZonosModelPaths? =
        if (_status.value.phase == ModelPhase.READY)
            ZonosModelPaths(File(dir, "backbone-ptq.gguf"), File(dir, "zac-keep2.gguf"))
        else null

    fun downloadOnUserRequest() {
        if (downloadJob?.isActive == true || _status.value.phase == ModelPhase.READY) return
        downloadJob = scope.launch {
            mutex.withLock {
                try {
                    val missing = ArrayList<ZonosModelSpec>()
                    for (spec in ZonosModelManifest.files) if (!isVerified(spec)) missing.add(spec)
                    val required = missing.sumOf {
                        (it.maxBytes - File(dir, it.name + ".part").length()).coerceAtLeast(0L)
                    } + 256L * 1024L * 1024L
                    val available = StatFs(dir.absolutePath).availableBytes
                    if (required > available) {
                        throw IOException("Brak miejsca: potrzebne ok. " + (required / 1_000_000L) +
                            " MB, wolne " + (available / 1_000_000L) + " MB")
                    }
                    _status.value = ModelDownloadStatus(ModelPhase.DOWNLOADING)
                    for (spec in missing) downloadAndVerify(spec)
                    _status.value = ModelDownloadStatus(ModelPhase.READY, message = "Sprawdzono SHA-256 obu modeli")
                } catch (error: kotlinx.coroutines.CancellationException) {
                    _status.value = ModelDownloadStatus(ModelPhase.MISSING, message = "Można wznowić pobieranie")
                    throw error
                } catch (error: Exception) {
                    _status.value = ModelDownloadStatus(ModelPhase.ERROR,
                        message = error.message ?: "Błąd pobierania")
                }
            }
        }
    }

    fun cancelDownload() { downloadJob?.cancel() }

    fun deleteOnUserRequest() {
        cancelDownload()
        scope.launch {
            mutex.withLock {
                for (spec in ZonosModelManifest.files) {
                    File(dir, spec.name).delete()
                    File(dir, spec.name + ".part").delete()
                }
                _status.value = ModelDownloadStatus(ModelPhase.MISSING)
            }
        }
    }

    private suspend fun inspectFiles() {
        _status.value = ModelDownloadStatus(ModelPhase.CHECKING)
        var valid = true
        for (spec in ZonosModelManifest.files) valid = isVerified(spec) && valid
        _status.value = ModelDownloadStatus(if (valid) ModelPhase.READY else ModelPhase.MISSING)
    }

    private suspend fun isVerified(spec: ZonosModelSpec): Boolean {
        val file = File(dir, spec.name)
        return file.isFile && file.length() in 100_000_000L..spec.maxBytes &&
            verifySha256(file, spec.sha256)
    }

    private suspend fun verifySha256(file: File, expected: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count == -1) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 255) } == expected
    }

    private suspend fun downloadAndVerify(spec: ZonosModelSpec) {
        val partial = File(dir, spec.name + ".part")
        val final = File(dir, spec.name)
        var offset = partial.length()
        if (offset > spec.maxBytes) { partial.delete(); offset = 0L }
        var url = URL(spec.url)
        var redirects = 0
        var connection: HttpURLConnection? = null
        try {
            while (true) {
                currentCoroutineContext().ensureActive()
                if (!ZonosModelManifest.allowDownloadUrl(url)) throw IOException("Zabroniony host pobierania")
                val conn = url.openConnection() as HttpURLConnection
                connection = conn
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 12_000
                conn.readTimeout = 35_000
                conn.setRequestProperty("Accept-Encoding", "identity")
                if (offset > 0) conn.setRequestProperty("Range", "bytes=" + offset + "-")
                val code = conn.responseCode
                if (code in 300..399) {
                    val destination = conn.getHeaderField("Location") ?: throw IOException("Brak przekierowania")
                    url = URL(url, destination)
                    conn.disconnect()
                    if (++redirects > 8) throw IOException("Zbyt wiele przekierowań")
                    continue
                }
                if (code != 200 && code != 206) throw IOException("Pobieranie HTTP " + code)
                if (code == 206 && !conn.getHeaderField("Content-Range").orEmpty()
                    .startsWith("bytes " + offset + "-")) {
                    throw IOException("Błędne wznowienie pobierania")
                }
                val append = code == 206 && offset > 0
                if (!append) offset = 0L
                FileOutputStream(partial, append).buffered().use { output ->
                    conn.inputStream.use { input ->
                        val buffer = ByteArray(128 * 1024)
                        var lastReported = offset
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            offset += count
                            if (offset > spec.maxBytes) throw IOException("Model przekracza limit rozmiaru")
                            output.write(buffer, 0, count)
                            if (offset - lastReported >= 4L * 1024L * 1024L) {
                                lastReported = offset
                                val other = ZonosModelManifest.files.filter { it.name != spec.name }.sumOf { item ->
                                    File(dir, item.name).takeIf { it.exists() }?.length()
                                        ?: File(dir, item.name + ".part").length()
                                }
                                _status.value = ModelDownloadStatus(ModelPhase.DOWNLOADING, offset + other)
                            }
                        }
                    }
                }
                break
            }
        } finally {
            connection?.disconnect()
        }
        if (partial.length() !in 100_000_000L..spec.maxBytes || !verifySha256(partial, spec.sha256)) {
            partial.delete()
            throw IOException("Niepoprawna suma SHA-256: " + spec.name)
        }
        if (final.exists() && !final.delete()) throw IOException("Nie można zastąpić modelu")
        if (!partial.renameTo(final)) throw IOException("Nie można zatwierdzić modelu")
    }

    fun close() { scope.cancel() }
}
