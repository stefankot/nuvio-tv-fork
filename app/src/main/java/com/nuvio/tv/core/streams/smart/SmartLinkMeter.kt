package com.nuvio.tv.core.streams.smart

/**
 * forge: how fast this device's link has been, read only from bytes the player already downloaded. No extra
 * connection, nothing on disk, no address: a fixed ring of (bitrate, time) pairs in memory. Pure JVM.
 */
object SmartLinkMeter {
    /** A sample from a smaller transfer is ignored: too short to say anything about the link. */
    const val MIN_BYTES = 512L * 1024
    const val MIN_LOAD_MS = 200
    const val MAX_SAMPLES = 30
    const val MIN_SAMPLES = 5
    const val MAX_AGE_MS = 6L * 60 * 60 * 1000

    private val bps = LongArray(MAX_SAMPLES)
    private val at = LongArray(MAX_SAMPLES)
    private var count = 0
    private var next = 0

    @Synchronized
    fun onSample(bitrateEstimateBps: Long, totalBytesLoaded: Long, totalLoadTimeMs: Int, nowMs: Long) {
        if (bitrateEstimateBps <= 0 || totalBytesLoaded < MIN_BYTES || totalLoadTimeMs < MIN_LOAD_MS) return
        bps[next] = bitrateEstimateBps
        at[next] = nowMs
        next = (next + 1) % MAX_SAMPLES
        if (count < MAX_SAMPLES) count++
    }

    /** 90th percentile of the samples younger than [MAX_AGE_MS]; fewer than [MIN_SAMPLES] = UNKNOWN (null). */
    @Synchronized
    fun estimateBps(nowMs: Long): Long? {
        val valid = LongArray(count)
        var n = 0
        for (i in 0 until count) {
            val age = nowMs - at[i]
            if (age in 0..MAX_AGE_MS) valid[n++] = bps[i]
        }
        if (n < MIN_SAMPLES) return null
        val sorted = valid.copyOf(n).also { it.sort() }
        val index = (Math.ceil(n * 0.9).toInt() - 1).coerceIn(0, n - 1)
        return sorted[index]
    }

    /** Another network: what was measured no longer applies. */
    @Synchronized
    fun reset() {
        count = 0
        next = 0
    }

    // forge M6 (N6): the measurement survives a restart — one number and its time, valid 24 h, dropped on another network.
    const val KEEP_MS = 24L * 60 * 60 * 1000
    private const val PREFS = "forge_smart_link"

    /** Saves the estimate for [network] (a label of the network kind, no address). */
    fun persist(context: android.content.Context, network: String, nowWallMs: Long = System.currentTimeMillis()) {
        val bps = estimateBps(android.os.SystemClock.elapsedRealtime()) ?: return
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE).edit()
            .putLong("bps", bps).putLong("at", nowWallMs).putString("net", network).apply()
    }

    /** Seeds the ring with the saved estimate when it is younger than 24 h and from the same kind of network. */
    fun restore(context: android.content.Context, network: String, nowWallMs: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
        val bps = prefs.getLong("bps", 0L)
        val at = prefs.getLong("at", 0L)
        if (bps <= 0 || nowWallMs - at !in 0..KEEP_MS || prefs.getString("net", null) != network) return
        val now = android.os.SystemClock.elapsedRealtime()
        synchronized(this) { if (count == 0) repeat(MIN_SAMPLES) { onSample(bps, MIN_BYTES, MIN_LOAD_MS, now) } }
    }

    /** "wifi" / "ethernet" / "cellular" / "other": the kind of the active network. */
    fun networkKind(context: android.content.Context): String {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java) ?: return "other"
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "other"
        return when {
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            else -> "other"
        }
    }
}
