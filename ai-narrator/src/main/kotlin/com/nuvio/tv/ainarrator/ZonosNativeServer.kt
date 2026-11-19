package com.nuvio.tv.ainarrator

import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

/**
 * The worker executable is NEVER shipped in NuvioTV.
 * A separately signed, user-installed ARM32 companion holds the native runtime
 * in the OS-managed nativeLibraryDir and executes it in a different application process.
 *
 * Models in Nuvio's private filesDir are passed as time-limited, read-only
 * content:// URIs. The companion verifies/copies each once to its private storage.
 * Do not attempt exec() or dlopen() of a downloaded ELF from writable filesDir.
 */
internal class ZonosNativeServer(context: Context, private val memory: ZonosMemoryGuard) {
    private val app = context.applicationContext
    private val installer = ZonosArm32Installer(app)
    private val tts = LocalZonosTts()
    @Volatile private var endpoint: String? = null
    @Volatile private var token: String? = null
    private var authorizedUris: Pair<Uri, Uri>? = null

    fun isPackaged(): Boolean = installer.installedAndTrusted()

    private fun workerIntent(action: String): Intent = Intent(action)
        .setComponent(ComponentName(ZonosArm32Installer.WORKER_PACKAGE,
            "com.nuvio.tv.zonos32.ZonosWorkerService"))

    private suspend fun ensureStarted(paths: ZonosModelPaths): String {
        if (!isPackaged()) throw IOException("Silnik ARM32 nie jest zainstalowany lub podpis jest niezgodny")
        val existing = endpoint
        if (existing != null && isHealthy(existing)) return existing
        val guard = memory.check(starting = true, offline = true)
        if (!guard.allowed) throw IOException("Zbyt mało wolnej RAM: " + guard.decision)
        return withTimeout(120_000L) {
            suspendCancellableCoroutine { continuation ->
                val authority = app.packageName + ".fileprovider"
                val first = FileProvider.getUriForFile(app, authority, paths.backbone)
                val second = FileProvider.getUriForFile(app, authority, paths.decoder)
                val request = workerIntent(ACTION_START)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .putExtra(EXTRA_BACKBONE_URI, first)
                    .putExtra(EXTRA_DECODER_URI, second)
                    .apply {
                        clipData = ClipData.newRawUri("zonos3-backbone", first).apply {
                            addItem(ClipData.Item(second))
                        }
                    }
                val receiver = object : ResultReceiver(Handler(Looper.getMainLooper())) {
                    override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                        if (!continuation.isActive) return
                        if (resultCode != RESULT_READY) {
                            continuation.resumeWith(Result.failure(IOException(
                                resultData?.getString(EXTRA_ERROR) ?: "Błąd uruchomienia silnika ARM32")))
                            return
                        }
                        val port = resultData?.getInt(EXTRA_PORT, -1) ?: -1
                        val secret = resultData?.getString(EXTRA_TOKEN).orEmpty()
                        if (port !in 1..65535 || secret.length < 24) {
                            continuation.resumeWith(Result.failure(IOException("Nieprawidłowa odpowiedź silnika")))
                            return
                        }
                        endpoint = "http://127.0.0.1:$port"
                        token = secret
                        continuation.resume(endpoint!!)
                    }
                }
                request.putExtra(EXTRA_RECEIVER, receiver)
                try {
                    app.grantUriPermission(ZonosArm32Installer.WORKER_PACKAGE, first,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    app.grantUriPermission(ZonosArm32Installer.WORKER_PACKAGE, second,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    authorizedUris = first to second
                    if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(request)
                    else app.startService(request)
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWith(Result.failure(
                        IOException("Nie można uruchomić serwisu ARM32: " + error.javaClass.simpleName)))
                }
                continuation.invokeOnCancellation { stop() }
            }
        }
    }

    suspend fun synthesize(paths: ZonosModelPaths, text: String): ByteArray = withContext(Dispatchers.IO) {
        val host = ensureStarted(paths)
        val secret = token ?: throw IOException("Brak tokenu lokalnego procesu")
        if (!memory.check(starting = false, offline = true).allowed) {
            stop()
            throw IOException("Zatrzymano ZONOS3 z powodu małej ilości RAM")
        }
        val wav = tts.synthesize(host, secret, "", text)
        if (!memory.check(starting = false, offline = true).allowed) {
            stop()
            throw IOException("Zatrzymano ZONOS3 z powodu małej ilości RAM")
        }
        wav
    }

    private fun isHealthy(host: String): Boolean {
        val secret = token ?: return false
        return runCatching {
            val conn = URL("$host/v2/health").openConnection() as HttpURLConnection
            conn.connectTimeout = 400
            conn.readTimeout = 400
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("Authorization", "Bearer " + secret)
            try { conn.responseCode in 200..299 } finally { conn.disconnect() }
        }.getOrDefault(false)
    }

    @Synchronized fun stop() {
        endpoint = null
        token = null
        if (installer.installedAndTrusted()) {
            runCatching { app.startService(workerIntent(ACTION_STOP)) }
        }
        authorizedUris?.let { (first, second) ->
            runCatching { app.revokeUriPermission(first, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            runCatching { app.revokeUriPermission(second, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        }
        authorizedUris = null
    }

    fun deleteCompanionModels() {
        if (installer.installedAndTrusted()) {
            runCatching { app.startService(workerIntent(ACTION_CLEAR_MODELS)) }
        }
    }

    companion object {
        const val ACTION_START = "com.nuvio.tv.zonos32.START"
        const val ACTION_STOP = "com.nuvio.tv.zonos32.STOP"
        const val ACTION_CLEAR_MODELS = "com.nuvio.tv.zonos32.CLEAR_MODELS"
        const val EXTRA_BACKBONE_URI = "com.nuvio.tv.zonos32.backbone_uri"
        const val EXTRA_DECODER_URI = "com.nuvio.tv.zonos32.decoder_uri"
        const val EXTRA_RECEIVER = "com.nuvio.tv.zonos32.receiver"
        const val EXTRA_PORT = "port"
        const val EXTRA_TOKEN = "token"
        const val EXTRA_ERROR = "error"
        const val RESULT_READY = 1
        const val RESULT_ERROR = 2
    }
}
