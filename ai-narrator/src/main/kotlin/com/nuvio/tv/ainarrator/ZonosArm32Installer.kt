package com.nuvio.tv.ainarrator

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.FileProvider
import com.nuvio.tv.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

internal enum class Arm32InstallPhase {
    UNSUPPORTED, NOT_PUBLISHED, NOT_INSTALLED, DOWNLOADING,
    AWAITING_INSTALL, INSTALLED, ERROR
}

internal data class Arm32InstallStatus(
    val phase: Arm32InstallPhase,
    val receivedBytes: Long = 0L,
    val expectedBytes: Long = 0L,
    val message: String = ""
)

/**
 * Downloads a SEPARATELY SIGNED Android 32-bit companion APK.
 * No native binary is in Nuvio's base APK; no exec or dlopen from writable filesDir.
 * PackageInstaller (with user approval) puts code in the OS-managed executable location.
 */
internal class ZonosArm32Installer(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val apk = File(app.cacheDir, "zonos32/zonos3-worker-armeabi-v7a.apk")
    private var download: Job? = null
    private val _status = MutableStateFlow(readStatus())
    val status: StateFlow<Arm32InstallStatus> = _status

    fun refresh() {
        val latest = readStatus()
        if (latest.phase == Arm32InstallPhase.INSTALLED ||
            latest.phase == Arm32InstallPhase.UNSUPPORTED ||
            (_status.value.phase != Arm32InstallPhase.DOWNLOADING &&
                _status.value.phase != Arm32InstallPhase.AWAITING_INSTALL)) {
            _status.value = latest
        }
    }

    fun supportedAbi(): Boolean = Build.SUPPORTED_32_BIT_ABIS.contains("armeabi-v7a")

    fun installedAndTrusted(): Boolean = try {
        app.packageManager.checkSignatures(app.packageName, WORKER_PACKAGE) ==
            PackageManager.SIGNATURE_MATCH && supportedAbi()
    } catch (_: Exception) { false }

    private fun readStatus(): Arm32InstallStatus {
        val phase = when {
            !supportedAbi() -> Arm32InstallPhase.UNSUPPORTED
            installedAndTrusted() -> Arm32InstallPhase.INSTALLED
            BuildConfig.ZONOS_ARM32_APK_URL.isBlank() ||
                !BuildConfig.ZONOS_ARM32_APK_SHA256.matches(Regex("[a-fA-F0-9]{64}")) ->
                Arm32InstallPhase.NOT_PUBLISHED
            else -> Arm32InstallPhase.NOT_INSTALLED
        }
        return Arm32InstallStatus(phase)
    }

    /** Invoked once the user enables ZONOS3 in settings (no background install). */
    fun downloadOnFeatureEnable() {
        if (download?.isActive == true || !supportedAbi() || installedAndTrusted()) return
        if (_status.value.phase == Arm32InstallPhase.AWAITING_INSTALL) return
        val url = BuildConfig.ZONOS_ARM32_APK_URL
        val expectedSha = BuildConfig.ZONOS_ARM32_APK_SHA256.lowercase(Locale.ROOT)
        if (url.isBlank() || !expectedSha.matches(Regex("[a-f0-9]{64}"))) {
            _status.value = Arm32InstallStatus(Arm32InstallPhase.NOT_PUBLISHED,
                message = "Brak wydanego i podpisanego pakietu ARM32; nie można go pobrać")
            return
        }
        download = scope.launch {
            try {
                _status.value = Arm32InstallStatus(Arm32InstallPhase.DOWNLOADING)
                val downloaded = downloadApk(URL(url))
                if (!checkDigest(downloaded, expectedSha)) {
                    downloaded.delete()
                    throw IOException("Niezgodna suma SHA-256 pliku APK")
                }
                verifyApkSigner(downloaded)
                _status.value = Arm32InstallStatus(Arm32InstallPhase.AWAITING_INSTALL,
                    message = "Pakiet zweryfikowany; potwierdź instalację w systemie")
            } catch (e: CancellationException) {
                _status.value = Arm32InstallStatus(Arm32InstallPhase.NOT_INSTALLED,
                    message = "Pobieranie anulowano")
                throw e
            } catch (e: Exception) {
                _status.value = Arm32InstallStatus(Arm32InstallPhase.ERROR,
                    message = e.message ?: "Błąd pobierania silnika")
            }
        }
    }

    fun cancel() { download?.cancel() }

    /**
     * Must be invoked via actual user interaction. The system installer controls consent;
     * the user is not asked to disable Android's security checks.
     */
    fun openInstaller(): Boolean {
        if (_status.value.phase != Arm32InstallPhase.AWAITING_INSTALL) return false
        verifyApkSigner(apk)
        val uri = FileProvider.getUriForFile(app, app.packageName + ".fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            app.startActivity(intent)
        } catch (e: Exception) {
            _status.value = Arm32InstallStatus(Arm32InstallPhase.ERROR,
                message = "Nie znaleziono instalatora Android: " + e.javaClass.simpleName)
            return false
        }
        return true
    }

    private suspend fun downloadApk(start: URL): File = withContext(Dispatchers.IO) {
        var current = start
        var redirects = 0
        apk.parentFile?.mkdirs()
        val partial = File(apk.parentFile, apk.name + ".part")
        try {
            while (true) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                require(Arm32GitHubSource.isApproved(current, redirects > 0)) {
                    "Pakiet ARM32 musi pochodzić z GitHub Releases użytkownika stefankot"
                }
                val conn = current.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 12_000
                conn.readTimeout = 35_000
                conn.setRequestProperty("Accept-Encoding", "identity")
                try {
                    val code = conn.responseCode
                    if (code in 300..399) {
                        val location = conn.getHeaderField("Location")
                            ?: throw IOException("Brak adresu przekierowania")
                        current = URL(current, location)
                        if (++redirects > 7) throw IOException("Za dużo przekierowań")
                        continue
                    }
                    if (code != 200) throw IOException("Silnik HTTP " + code)
                    val expected = conn.contentLengthLong
                    if (expected > MAX_APK_BYTES) throw IOException("Pakiet silnika zbyt duży")
                    var copied = 0L
                    FileOutputStream(partial).buffered().use { output ->
                        conn.inputStream.use { input ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                                val n = input.read(buffer)
                                if (n < 0) break
                                copied += n
                                if (copied > MAX_APK_BYTES) throw IOException("Przekroczony rozmiar APK")
                                output.write(buffer, 0, n)
                                if (copied % (1024 * 1024) < n) {
                                    _status.value = Arm32InstallStatus(
                                        Arm32InstallPhase.DOWNLOADING, copied, expected)
                                }
                            }
                        }
                    }
                    if (copied < 20_000L) throw IOException("Niekompletny APK")
                    if (expected >= 0 && expected != copied) throw IOException("Niepełny plik silnika")
                    if (apk.exists() && !apk.delete()) throw IOException("Nie można zastąpić APK")
                    if (!partial.renameTo(apk)) throw IOException("Błąd zapisu APK")
                    return@withContext apk
                } finally { conn.disconnect() }
            }
        } finally { partial.delete() }
        @Suppress("UNREACHABLE_CODE")
        apk
    }

    private fun checkDigest(file: File, expected: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 255) } == expected
    }

    @Suppress("DEPRECATION")
    private fun verifyApkSigner(file: File) {
        if (Build.VERSION.SDK_INT < 28) throw IOException("Wymagany Android 9 lub nowszy")
        val flags = PackageManager.GET_SIGNING_CERTIFICATES
        val archive = app.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IOException("Nie można odczytać podpisanego pakietu")
        if (archive.packageName != WORKER_PACKAGE) throw IOException("Zła nazwa pakietu silnika")
        if (archive.applicationInfo == null) throw IOException("Pakiet nie zawiera komponentu Android")
        val appInfo = app.packageManager.getPackageInfo(app.packageName, flags)
        val own = appInfo.signingInfo?.apkContentsSigners?.map { it.toByteArray().toList() }?.toSet()
        val downloaded = archive.signingInfo?.apkContentsSigners?.map { it.toByteArray().toList() }?.toSet()
        if (own.isNullOrEmpty() || downloaded.isNullOrEmpty() || own != downloaded) {
            throw IOException("Pakiet ARM32 nie jest podpisany tym samym kluczem co NuvioTV")
        }
    }

    fun close() { cancel(); scope.cancel() }

    companion object {
        const val WORKER_PACKAGE = "com.nuvio.tv.zonos32"
        private const val MAX_APK_BYTES = 256L * 1024L * 1024L
    }
}

/** Strict GitHub provenance. Public release or authenticated delivery is required for private repos. */
internal object Arm32GitHubSource {
    fun isApproved(url: URL, redirect: Boolean): Boolean {
        if (!url.protocol.equals("https", ignoreCase = true) || url.userInfo != null ||
            url.port != -1 || url.host.isBlank()) return false
        val host = url.host.lowercase(Locale.ROOT)
        if (!redirect) {
            return host == "github.com" &&
                url.path.startsWith("/stefankot/") &&
                url.path.contains("/releases/download/") &&
                url.path.endsWith(".apk")
        }
        return (host == "github.com" && url.path.startsWith("/stefankot/")) ||
            host == "release-assets.githubusercontent.com" ||
            host == "objects.githubusercontent.com"
    }
}
