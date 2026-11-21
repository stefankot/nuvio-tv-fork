package com.nuvio.tv.ui.screens.player

import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.security.SecureRandom
import java.util.Base64
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.min
import okhttp3.Dispatcher
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response as OkHttpResponse

data class DubbingRelaySource(
    val url: String,
    val requestHeaders: Map<String, String> = emptyMap(),
    val responseHeaders: Map<String, String> = emptyMap(),
)

data class DubbingRelayEndpoints(
    val mainPlayerUrl: String,
    val analysisUrl: String,
)

/**
 * Capability-protected loopback relay for source B.
 *
 * Playback and analysis are independent local readers, while seekable upstreams have at most one
 * remote request in flight. A small LRU reuses nearby ranges without a desktop-sized memory cost.
 */
class DubbingSourceRelay(
    private val source: DubbingRelaySource,
    client: OkHttpClient = defaultClient(),
    token: String = randomToken(),
) : NanoHTTPD(LOOPBACK_HOST, 0), AutoCloseable {
    private val httpClient = client.newBuilder()
        .dispatcher(Dispatcher())
        .followRedirects(false)
        .followSslRedirects(false)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val capabilityToken = token
    private val rangeSource = SharedRangeSource()

    @Volatile private var resolvedUpstreamUrl: String? = null
    @Volatile private var endpoints: DubbingRelayEndpoints? = null

    init {
        require(source.url.startsWith("http://", true) || source.url.startsWith("https://", true))
        require(capabilityToken.isNotBlank() && '/' !in capabilityToken)
    }

    @Synchronized
    fun startRelay(): DubbingRelayEndpoints {
        endpoints?.let { return it }
        start(SOCKET_READ_TIMEOUT, false)
        val base = "http://$LOOPBACK_HOST:$listeningPort/v1/$capabilityToken"
        return DubbingRelayEndpoints("$base/main", "$base/analysis").also { endpoints = it }
    }

    override fun serve(session: IHTTPSession): Response {
        if (session.method != Method.GET && session.method != Method.HEAD) {
            return plain(Response.Status.METHOD_NOT_ALLOWED, "Method not allowed").apply {
                addHeader("Allow", "GET, HEAD")
            }
        }
        val expectedPrefix = "/v1/$capabilityToken/"
        val role = session.uri.removePrefix(expectedPrefix)
        if (!session.uri.startsWith(expectedPrefix) || role !in ALLOWED_ROLES) {
            return plain(Response.Status.NOT_FOUND, "Not found")
        }
        return runCatching { rangeSource.response(session) ?: proxy(session) }
            .getOrElse { plain(BAD_GATEWAY_STATUS, "Upstream unavailable") }
    }

    private fun proxy(session: IHTTPSession): Response = executeUpstream(
        method = session.method,
        range = session.headers["range"],
        ifRange = session.headers["if-range"],
    ).toRelayResponse(session.method == Method.HEAD)

    private fun executeUpstream(
        method: Method,
        range: String? = null,
        ifRange: String? = null,
    ): OkHttpResponse {
        var currentUrl = resolvedUpstreamUrl ?: source.url
        val originalOrigin = URI(source.url).origin()
        var redirects = 0
        while (true) {
            val currentOrigin = URI(currentUrl).origin()
            val builder = Request.Builder().url(currentUrl)
            if (method == Method.HEAD) builder.head() else builder.get()
            source.requestHeaders.forEach { (name, value) ->
                if (name.isSafeRequestHeader() &&
                    (currentOrigin == originalOrigin || !name.isSensitiveRequestHeader())
                ) runCatching { builder.header(name, value) }
            }
            range?.let { builder.header("Range", it) }
            ifRange?.let { builder.header("If-Range", it) }

            val upstream = httpClient.newCall(builder.build()).execute()
            val location = upstream.header("Location")
            if (upstream.code in REDIRECT_CODES && location != null) {
                if (redirects++ >= MAX_REDIRECTS) {
                    upstream.close()
                    throw IOException("Too many redirects")
                }
                val resolved = upstream.request.url.resolve(location)
                upstream.close()
                if (resolved == null || resolved.scheme !in setOf("http", "https")) {
                    throw IOException("Invalid redirect")
                }
                currentUrl = resolved.toString()
                continue
            }
            if (currentUrl != source.url) resolvedUpstreamUrl = currentUrl
            return upstream
        }
    }

    private fun OkHttpResponse.toRelayResponse(headOnly: Boolean): Response {
        val status = Response.Status.lookup(code) ?: Response.Status.INTERNAL_ERROR
        val contentType = header("Content-Type") ?: "application/octet-stream"
        val relayResponse = if (headOnly) {
            val length = header("Content-Length")?.toLongOrNull()
                ?: body.contentLength().takeIf { it >= 0L }
                ?: 0L
            close()
            newFixedLengthResponse(status, contentType, ByteArrayInputStream(ByteArray(0)), length)
        } else {
            val input = ClosingInput(body.byteStream(), this)
            val length = body.contentLength()
            if (length >= 0L) newFixedLengthResponse(status, contentType, input, length)
            else newChunkedResponse(status, contentType, input)
        }
        copyResponseHeaders(headers, relayResponse)
        return relayResponse
    }

    private fun copyResponseHeaders(
        headers: Headers,
        response: Response,
        additionallyBlocked: Set<String> = emptySet(),
    ) {
        val overridden = source.responseHeaders.keys.map { it.lowercase(Locale.US) }.toSet()
        headers.names().forEach { name ->
            val normalized = name.lowercase(Locale.US)
            if (name.isSafeResponseHeader() && normalized !in additionallyBlocked && normalized !in overridden) {
                headers.values(name).forEach { response.addHeader(name, it) }
            }
        }
        source.responseHeaders.forEach { (name, value) ->
            if (name.isSafeResponseOverride()) response.addHeader(name, value)
        }
        response.addHeader("Cache-Control", "no-store")
    }

    override fun close() {
        rangeSource.close()
        stop()
        endpoints = null
        resolvedUpstreamUrl = null
        httpClient.dispatcher.cancelAll()
        httpClient.dispatcher.executorService.shutdownNow()
        httpClient.connectionPool.evictAll()
    }

    private fun plain(status: Response.IStatus, message: String): Response =
        newFixedLengthResponse(status, MIME_PLAINTEXT, message).apply { addHeader("Cache-Control", "no-store") }

    private inner class SharedRangeSource {
        private val upstreamGate = ReentrantLock(true)
        private val cache = object : LinkedHashMap<Long, ByteArray>(MAX_CACHE_CHUNKS + 1, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, ByteArray>?): Boolean =
                size > MAX_CACHE_CHUNKS
        }
        @Volatile private var metadata: RangeMetadata? = null
        @Volatile private var rangeUnavailable = false
        @Volatile private var closed = false

        fun response(session: IHTTPSession): Response? {
            if (rangeUnavailable) return null
            // Retryable failures are not evidence that the provider lacks byte-range support.
            // In particular, signed providers may briefly return 409 while releasing their
            // previous reader. Keep the relay in its serialized range mode and let the local
            // client retry instead of permanently falling back to concurrent direct proxies.
            val info = metadata ?: probe()
            if (!info.acceptsRanges || info.length <= 0L) {
                rangeUnavailable = true
                return null
            }
            val rangeHeader = session.headers["range"]
            val requested = parseRange(rangeHeader, info.length)
                ?: if (rangeHeader == null) ByteRange(0L, info.length - 1L)
                else return rangeNotSatisfiable(info.length)
            val responseLength = requested.endInclusive - requested.start + 1L
            val status = if (rangeHeader == null) Response.Status.OK else Response.Status.PARTIAL_CONTENT
            val input = if (session.method == Method.HEAD) {
                ByteArrayInputStream(ByteArray(0))
            } else {
                chunk(requested.start / CHUNK_SIZE)
                RangeInput(requested.start, responseLength)
            }
            return newFixedLengthResponse(status, info.contentType, input, responseLength).apply {
                addHeader("Accept-Ranges", "bytes")
                if (status == Response.Status.PARTIAL_CONTENT) {
                    addHeader("Content-Range", "bytes ${requested.start}-${requested.endInclusive}/${info.length}")
                }
                copyResponseHeaders(info.headers, this, CACHED_ENTITY_HEADERS)
            }
        }

        fun close() {
            closed = true
            synchronized(cache) { cache.clear() }
        }

        private fun probe(): RangeMetadata = upstreamGate.withLock {
            metadata?.let { return it }
            check(!closed)
            executeWithRetry("bytes=0-0").use { response ->
                val contentRange = parseContentRange(response.header("Content-Range"))
                val acceptsRanges = response.code == 206 &&
                    contentRange?.first == 0L &&
                    contentRange.second >= 0L &&
                    contentRange.third > contentRange.second
                val length = contentRange?.third
                    ?: response.header("Content-Length")?.toLongOrNull()
                    ?: response.body.contentLength().takeIf { it >= 0L }
                    ?: -1L
                if (acceptsRanges) readExactly(response.body.byteStream(), 1)
                return RangeMetadata(
                    length = length,
                    contentType = response.header("Content-Type") ?: "application/octet-stream",
                    headers = response.headers,
                    acceptsRanges = acceptsRanges,
                ).also { metadata = it }
            }
        }

        private fun chunk(index: Long): ByteArray {
            synchronized(cache) { cache[index]?.let { return it } }
            return upstreamGate.withLock {
                synchronized(cache) { cache[index]?.let { return@withLock it } }
                check(!closed)
                val info = metadata ?: probe()
                val start = index * CHUNK_SIZE
                if (start >= info.length) return@withLock ByteArray(0)
                val end = min(info.length - 1L, start + CHUNK_SIZE - 1L)
                executeWithRetry("bytes=$start-$end").use { response ->
                    if (response.code != 206) throw IOException("Range request returned HTTP ${response.code}")
                    val contentRange = parseContentRange(response.header("Content-Range"))
                    if (contentRange == null || contentRange.first != start ||
                        contentRange.second < end || contentRange.third != info.length
                    ) throw IOException("Invalid Content-Range")
                    // Some providers honour only the requested start. Bound the read to one chunk.
                    val bytes = readExactly(response.body.byteStream(), (end - start + 1L).toInt())
                    synchronized(cache) { cache[index] = bytes }
                    bytes
                }
            }
        }

        private fun readExactly(input: InputStream, length: Int): ByteArray {
            val result = ByteArray(length)
            var offset = 0
            while (offset < length) {
                val count = input.read(result, offset, length - offset)
                if (count < 0) throw IOException("Incomplete range response")
                if (count > 0) offset += count
            }
            return result
        }

        private fun executeWithRetry(range: String): OkHttpResponse {
            var lastFailure: IOException? = null
            repeat(UPSTREAM_ATTEMPTS) { attempt ->
                if (attempt > 0) Thread.sleep(UPSTREAM_RETRY_DELAYS_MS[attempt - 1])
                val response = try {
                    executeUpstream(Method.GET, range)
                } catch (error: IOException) {
                    lastFailure = error
                    return@repeat
                }
                if (response.code !in RETRYABLE_CODES) return response
                lastFailure = IOException("Upstream returned HTTP ${response.code}")
                response.close()
            }
            throw lastFailure ?: IOException("Upstream unavailable")
        }

        private inner class RangeInput(
            private var position: Long,
            private var remaining: Long,
        ) : InputStream() {
            private var streamClosed = false

            override fun read(): Int {
                val one = ByteArray(1)
                return if (read(one, 0, 1) < 0) -1 else one[0].toInt() and 0xff
            }

            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (streamClosed) throw IOException("Stream closed")
                if (remaining <= 0L) return -1
                if (length == 0) return 0
                val data = chunk(position / CHUNK_SIZE)
                val chunkOffset = (position % CHUNK_SIZE).toInt()
                if (chunkOffset >= data.size) return -1
                val count = min(min(length.toLong(), remaining), (data.size - chunkOffset).toLong()).toInt()
                data.copyInto(buffer, offset, chunkOffset, chunkOffset + count)
                position += count
                remaining -= count
                return count
            }

            override fun close() {
                streamClosed = true
            }
        }
    }

    private fun rangeNotSatisfiable(length: Long): Response =
        newFixedLengthResponse(RANGE_NOT_SATISFIABLE_STATUS, MIME_PLAINTEXT, "Invalid range").apply {
            addHeader("Content-Range", "bytes */$length")
            addHeader("Cache-Control", "no-store")
        }

    private fun parseRange(value: String?, length: Long): ByteRange? {
        if (value == null) return ByteRange(0L, length - 1L)
        val match = RANGE_REGEX.matchEntire(value.trim()) ?: return null
        val startText = match.groupValues[1]
        val endText = match.groupValues[2]
        if (startText.isEmpty()) {
            val suffixLength = endText.toLongOrNull()?.coerceAtMost(length) ?: return null
            if (suffixLength <= 0L) return null
            return ByteRange(length - suffixLength, length - 1L)
        }
        val start = startText.toLongOrNull() ?: return null
        if (start < 0L || start >= length) return null
        val end = endText.toLongOrNull()?.coerceAtMost(length - 1L) ?: (length - 1L)
        if (end < start) return null
        return ByteRange(start, end)
    }

    private fun parseContentRange(value: String?): Triple<Long, Long, Long>? {
        val match = value?.let(CONTENT_RANGE_REGEX::matchEntire) ?: return null
        return Triple(
            match.groupValues[1].toLongOrNull() ?: return null,
            match.groupValues[2].toLongOrNull() ?: return null,
            match.groupValues[3].toLongOrNull() ?: return null,
        )
    }

    private class ClosingInput(input: InputStream, private val response: OkHttpResponse) : FilterInputStream(input) {
        override fun close() = try {
            super.close()
        } finally {
            response.close()
        }
    }

    private fun URI.origin(): String {
        val port = if (port >= 0) port else if (scheme.equals("https", true)) 443 else 80
        return "${scheme.lowercase(Locale.US)}://${host.orEmpty().lowercase(Locale.US)}:$port"
    }

    private fun String.isSensitiveRequestHeader() = lowercase(Locale.US) in SENSITIVE_REQUEST_HEADERS
    private fun String.isSafeRequestHeader() = lowercase(Locale.US) !in BLOCKED_REQUEST_HEADERS
    private fun String.isSafeResponseHeader() = lowercase(Locale.US) !in BLOCKED_RESPONSE_HEADERS
    private fun String.isSafeResponseOverride() = lowercase(Locale.US) !in PROTECTED_RESPONSE_HEADERS

    private data class RangeMetadata(
        val length: Long,
        val contentType: String,
        val headers: Headers,
        val acceptsRanges: Boolean,
    )

    private data class ByteRange(val start: Long, val endInclusive: Long)

    companion object {
        private const val LOOPBACK_HOST = "127.0.0.1"
        private const val MAX_REDIRECTS = 5
        private const val CHUNK_SIZE = 1024L * 1024L
        private const val MAX_CACHE_CHUNKS = 12
        private const val UPSTREAM_ATTEMPTS = 4
        private val UPSTREAM_RETRY_DELAYS_MS = longArrayOf(250L, 750L, 1_500L)
        private val RETRYABLE_CODES = setOf(409, 429, 502, 503, 504)
        private val ALLOWED_ROLES = setOf("main", "analysis")
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
        private val RANGE_REGEX = Regex("bytes=(\\d*)-(\\d*)", RegexOption.IGNORE_CASE)
        private val CONTENT_RANGE_REGEX = Regex("bytes (\\d+)-(\\d+)/(\\d+)", RegexOption.IGNORE_CASE)
        private val CACHED_ENTITY_HEADERS = setOf("content-range", "accept-ranges")
        private val SENSITIVE_REQUEST_HEADERS = setOf("authorization", "cookie", "proxy-authorization")
        private val BLOCKED_REQUEST_HEADERS = setOf(
            "connection", "content-length", "host", "proxy-connection", "transfer-encoding", "upgrade"
        )
        private val BLOCKED_RESPONSE_HEADERS = setOf(
            "connection", "content-length", "keep-alive", "proxy-authenticate", "proxy-connection",
            "te", "trailer", "transfer-encoding", "upgrade"
        )
        private val PROTECTED_RESPONSE_HEADERS = BLOCKED_RESPONSE_HEADERS + setOf(
            "content-range", "location", "set-cookie", "www-authenticate"
        )
        private val BAD_GATEWAY_STATUS = status(502, "502 Bad Gateway")
        private val RANGE_NOT_SATISFIABLE_STATUS = status(416, "416 Range Not Satisfiable")

        private fun status(code: Int, description: String) = object : Response.IStatus {
            override fun getRequestStatus() = code
            override fun getDescription() = description
        }

        private fun randomToken(): String = ByteArray(32)
            .also(SecureRandom()::nextBytes)
            .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
