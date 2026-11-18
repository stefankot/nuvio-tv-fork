package com.nuvio.tv.ainarrator

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URL

/**
 * ZONOS3 runs in an on-device, local-only arm64 child process, never on a LAN server.
 *
 * A working Android/arm64 PIE executable must be packaged as:
 * nativeLibraryDir/libzonos3_server.so
 * The upstream Linux release is NOT Android-compatible; no shell download or chmod of a
 * downloaded executable is allowed. A GGUF download does NOT provide the executable.
 */
internal class ZonosNativeServer(context: Context, private val memory: ZonosMemoryGuard) {
    private val executable = File(context.applicationInfo.nativeLibraryDir, "libzonos3_server.so")
    private val log = File(context.applicationContext.cacheDir, "zonos3-local.log")
    private val api = LocalZonosTts()
    private val accessToken = java.util.UUID.randomUUID().toString() + java.util.UUID.randomUUID().toString()

    @Volatile private var process: Process? = null
    @Volatile private var endpoint: String? = null

    fun isPackaged(): Boolean = executable.isFile && executable.canExecute()

    private suspend fun ensureStarted(paths: ZonosModelPaths): String = withContext(Dispatchers.IO) {
        synchronized(this@ZonosNativeServer) {
            val active = process
            if (active != null && active.isAlive && endpoint != null) return@withContext endpoint!!
        }
        if (!isPackaged()) throw IOException("Brak zgodnego pliku ARM64 w APK")
        val verdict = memory.check(starting = true, offline = true)
        if (!verdict.allowed) throw IOException("Za mało pamięci RAM: " + verdict.decision)
        val port = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { it.localPort }
        val candidateUrl = "http://127.0.0.1:$port"
        val args = listOf(
            executable.absolutePath,
            paths.backbone.absolutePath,
            "--zac", paths.decoder.absolutePath,
            "--cpu", "--batch", "1", "--zac-threads", "1",
            "--host", "127.0.0.1", "--port", port.toString()
        )
        val builder = ProcessBuilder(args)
            .redirectErrorStream(true)
            .redirectOutput(log)
        builder.environment()["ZONOS3_ACCESS_TOKEN"] = accessToken
        val candidate = builder.start()
        synchronized(this@ZonosNativeServer) {
            process = candidate
            endpoint = candidateUrl
        }
        try {
            repeat(100) {
                if (!candidate.isAlive) throw IOException("Natywny silnik zakończył działanie")
                if (!memory.check(starting = false, offline = true).allowed) {
                    throw IOException("Przerwano uruchamianie: za mało RAM")
                }
                if (isHealthy(candidateUrl)) return@withContext candidateUrl
                delay(250L)
            }
            throw IOException("Silnik nie wystartował w ciągu 25 sekund")
        } catch (error: Exception) {
            stop()
            throw error
        }
    }

    suspend fun synthesize(paths: ZonosModelPaths, text: String): ByteArray {
        val localUrl = ensureStarted(paths)
        val wav = api.synthesize(localUrl, accessToken, "", text)
        if (!memory.check(starting = false, offline = true).allowed) {
            stop()
            throw IOException("Synteza zatrzymana: systemowi brakuje pamięci")
        }
        return wav
    }

    private fun isHealthy(endpoint: String): Boolean = runCatching {
        val connection = URL("$endpoint/v2/health").openConnection() as HttpURLConnection
        connection.connectTimeout = 200
        connection.readTimeout = 200
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Authorization", "Bearer " + accessToken)
        try { connection.responseCode in 200..299 } finally { connection.disconnect() }
    }.getOrDefault(false)

    @Synchronized fun stop() {
        endpoint = null
        val old = process
        process = null
        if (old != null) {
            runCatching { old.destroy() }
            // A hung native worker must not survive the user's cancellation.
            runCatching { if (old.isAlive) old.destroyForcibly() }
        }
    }
}
