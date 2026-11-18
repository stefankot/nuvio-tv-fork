package com.nuvio.tv.core.player

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.hardware.display.DisplayManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import com.nuvio.tv.core.streams.smart.DecoderCap
import com.nuvio.tv.core.streams.smart.HdrKind
import com.nuvio.tv.core.streams.smart.SmartPolicyHolder
import com.nuvio.tv.core.streams.smart.VideoCodec
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * forge: asks the device what it can decode and how tall its screen is, so automatic source choice needs no
 * technical setup. Nothing is downloaded and nothing is stored: the probe takes milliseconds, runs once per
 * process and again whenever a display is added, removed or changes (HDMI switch, other TV).
 * What the device does not say stays UNKNOWN (null) and is never guessed.
 */
object SmartDeviceProbe {
    private const val TAG = "SmartDevice"
    private val worker = Executors.newSingleThreadExecutor { Thread(it, "smart-device-probe").apply { isDaemon = true } }
    @Volatile private var started = false
    private val consentScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val mimes = mapOf(
        VideoCodec.AVC to "video/avc",
        VideoCodec.HEVC to "video/hevc",
        VideoCodec.AV1 to "video/av01",
    )
    private val sizes = listOf(3840 to 2160, 2560 to 1440, 1920 to 1080, 1280 to 720)

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        com.nuvio.tv.core.streams.smart.SmartStreamRanking.log = { Log.i("SmartPick", it) }
        refresh(app)
        followP2pConsent(app)
        // forge M6 (N6): the link measured last time (24 h, same kind of network) and the memory of results
        runCatching { com.nuvio.tv.core.streams.smart.SmartLinkMeter.restore(app, com.nuvio.tv.core.streams.smart.SmartLinkMeter.networkKind(app)) }
        runCatching { com.nuvio.tv.core.streams.smart.SmartOutcomeStore.init(app.filesDir) }
        runCatching {
            val manager = app.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            manager.registerDisplayListener(object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) = refresh(app)
                override fun onDisplayRemoved(displayId: Int) = refresh(app)
                override fun onDisplayChanged(displayId: Int) {
                    if (displayId == Display.DEFAULT_DISPLAY) refresh(app)
                }
            }, Handler(Looper.getMainLooper()))
        }
        // Another default network makes the measured link meaningless: forget it (no connection is opened here).
        runCatching {
            val connectivity = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                @Volatile private var last: Network? = null
                override fun onAvailable(network: Network) {
                    if (network != last) { last = network; com.nuvio.tv.core.streams.smart.SmartLinkMeter.reset() }
                }
                // forge M3b (N9): a metered network (phone hotspot) makes the session rank as SAVER.
                override fun onCapabilitiesChanged(network: Network, caps: android.net.NetworkCapabilities) {
                    val metered = !caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                    if (com.nuvio.tv.core.streams.smart.SmartPolicyHolder.current.metered != metered) {
                        com.nuvio.tv.core.streams.smart.SmartPolicyHolder.update { it.copy(metered = metered) }
                        Log.i(TAG, "metered=$metered")
                    }
                }
            })
        }
    }

    /**
     * forge (check 13.11): the viewer's P2P consent (Reshaped's torrent settings) feeds the automatic mode, which puts
     * P2P sources last without it, so no pick stops on the "Streaming P2P" dialog. The same DataStore, read only.
     */
    private fun followP2pConsent(app: Context) {
        consentScope.launch {
            runCatching {
                com.nuvio.tv.core.torrent.TorrentSettings(app).settings
                    .map { it.p2pEnabled }
                    .distinctUntilChanged()
                    .collect { allowed ->
                        SmartPolicyHolder.update { it.copy(p2pAllowed = allowed) }
                        Log.i(TAG, "p2p=${if (allowed) "consent" else "no-consent"}")
                    }
            }.onFailure { Log.w(TAG, "p2p consent not read: ${it.javaClass.simpleName}") }
        }
    }

    fun refresh(context: Context) {
        worker.execute {
            val height = runCatching { displayHeight(context) }.getOrNull()
            val decoders = runCatching { decoders() }.getOrNull()
            val lowRam = runCatching {
                (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice
            }.getOrDefault(false)
            val hdr = hdrKinds(DolbyVisionBaseLayerPolicy.displayHdrTypes(context))
            val before = SmartPolicyHolder.current
            SmartPolicyHolder.update { it.copy(displayHeight = height, decoders = decoders, hdr = hdr, lowRam = lowRam) }
            if (before.displayHeight != height || before.decoders != decoders || before.hdr != hdr || before.lowRam != lowRam) {
                Log.i(TAG, "screen=${height ?: "UNKNOWN"} decoders=${decoders?.entries?.joinToString { (codec, cap) ->
                    "$codec:${if (cap.hardware) "hw" else "sw"}/${cap.maxHeight ?: "?"}"
                } ?: "UNKNOWN"} hdr=${hdr?.joinToString(",")?.ifEmpty { "SDR" } ?: "UNKNOWN"} lowRam=$lowRam")
            }
        }
    }

    /** System HDR types → kinds the ranker knows. null = the system does not say; empty = an SDR screen. */
    @Suppress("DEPRECATION")
    private fun hdrKinds(types: IntArray?): Set<HdrKind>? = types?.asList()?.mapNotNull { type ->
        when (type) {
            Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> HdrKind.DOLBY_VISION
            Display.HdrCapabilities.HDR_TYPE_HDR10, Display.HdrCapabilities.HDR_TYPE_HLG -> HdrKind.HDR
            Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> HdrKind.HDR10_PLUS
            else -> null
        }
    }?.toSet()

    /** Tallest mode the connected screen offers; null when the system does not say. */
    private fun displayHeight(context: Context): Int? {
        val manager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager ?: return null
        val display = manager.getDisplay(Display.DEFAULT_DISPLAY) ?: return null
        return display.supportedModes.maxOfOrNull { minOf(it.physicalHeight, it.physicalWidth) }?.takeIf { it > 0 }
    }

    /** Best decoder per codec: hardware before software, then the taller picture. A codec without one is left out. */
    private fun decoders(): Map<VideoCodec, DecoderCap> {
        val infos = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { !it.isEncoder }
        val out = LinkedHashMap<VideoCodec, DecoderCap>()
        for ((codec, mime) in mimes) {
            val best = infos.mapNotNull { info ->
                if (info.supportedTypes.none { it.equals(mime, ignoreCase = true) }) return@mapNotNull null
                val video = runCatching { info.getCapabilitiesForType(mime).videoCapabilities }.getOrNull()
                val maxHeight = video?.let { caps ->
                    sizes.firstOrNull { (w, h) -> runCatching { caps.isSizeSupported(w, h) }.getOrDefault(false) }?.second
                }
                DecoderCap(hardware = isHardware(info), maxHeight = maxHeight)
            }.maxWithOrNull(compareBy<DecoderCap>({ if (it.hardware) 1 else 0 }, { it.maxHeight ?: 0 }))
            if (best != null) out[codec] = best
        }
        return out
    }

    private fun isHardware(info: MediaCodecInfo): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.isHardwareAccelerated
        else {
            val name = info.name.lowercase()
            !(name.startsWith("omx.google.") || name.startsWith("c2.android.") || name.contains(".sw.") || name.contains("ffmpeg"))
        }
}
