package com.nuvio.tv.ui.screens.player

import android.util.Log
import com.nuvio.tv.core.autoquality.AutoQualityFlags
import com.nuvio.tv.domain.model.Stream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * forge M4c E (N10): warming up before Play. The file's host is resolved through the player's own OkHttp DNS, and for
 * https the TLS handshake is done ahead on a bare socket with the player's own SSLSocketFactory: its session lands in
 * that factory's session cache, so the player's first connection resumes it (one round trip less). No HTTP request
 * (also no HEAD / Range), no byte of the file, nothing to debrid: only sources that already have an address. Once per
 * host, one at a time, a few seconds at most.
 */
internal object ForgeWarmUp {
    private val warmed = java.util.Collections.synchronizedSet(HashSet<String>())

    fun warm(scope: CoroutineScope, stream: Stream?) {
        if (!AutoQualityFlags.PREFETCH_ENABLED || stream == null) return
        val url = stream.getStreamUrl()?.toHttpUrlOrNull() ?: return
        if (!warmed.add(url.host)) return
        scope.launch(Dispatchers.IO) {
            val client = PlayerPlaybackNetworking.playbackHttpClient
            val addresses = runCatching { client.dns.lookup(url.host) }.getOrNull() ?: return@launch
            Log.i("AutoQuality", "warm-up dns ok (${addresses.size})")
            if (!url.isHttps) return@launch
            synchronized(this@ForgeWarmUp) {
                runCatching {
                    java.net.Socket().use { raw ->
                        raw.connect(java.net.InetSocketAddress(addresses.first(), url.port), WARM_TIMEOUT_MS)
                        raw.soTimeout = WARM_TIMEOUT_MS
                        val tls = client.sslSocketFactory.createSocket(raw, url.host, url.port, true) as javax.net.ssl.SSLSocket
                        tls.sslParameters = tls.sslParameters.apply { serverNames = listOf(javax.net.ssl.SNIHostName(url.host)) }
                        tls.startHandshake()
                        tls.close()
                    }
                }.onSuccess { Log.i("AutoQuality", "warm-up tls ok") }
                    .onFailure { Log.i("AutoQuality", "warm-up tls skipped: ${it.javaClass.simpleName}") }
            }
        }
    }

    private const val WARM_TIMEOUT_MS = 3_000
}
