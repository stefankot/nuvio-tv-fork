package com.nuvio.tv.skin.niunia.player

import android.util.Log
import android.view.KeyEvent
import com.nuvio.tv.core.streams.smart.HdrKind
import com.nuvio.tv.core.streams.smart.ReleaseParser
import com.nuvio.tv.core.streams.smart.VideoCodec
import com.nuvio.tv.fork.logged
import com.nuvio.tv.ui.screens.player.PlayerUiState

/**
 * Przegląd ekranów 27.11, ekran 26 pkt 1: na ekranie wczytywania ◀▶/OK/Wstecz należą do nakładki.
 * Klawisze brał odtwarzacz pod spodem (przyciski pilota Arc i obsługa strzałek przy schowanych kontrolkach),
 * więc fokus nie ruszał się z „Inne źródło”. Wołane z `ForkHooks.onGlobalKey` (poziom aktywności, przed odtwarzaczem).
 */
internal object NiuniaLoadingKeys {
    private const val TAG = "ForkNiunia"
    @Volatile var handler: ((KeyEvent) -> Boolean)? = null

    fun onKey(e: KeyEvent): Boolean {
        val h = handler ?: return false
        return runCatching { h(e) }.logged(TAG, "NiuniaLoadingKeys.onKey").getOrDefault(false)
    }

    /**
     * Ekran 26 pkt 2: parametry wczytywanego pliku prostymi słowami, z opisu źródła
     * („4K · Dolby Vision · HEVC · 18 Mb/s · 12,4 GB · Polski · napisy PL”).
     */
    fun fileLine(s: PlayerUiState): String? {
        val text = listOfNotNull(s.currentStreamName, s.currentStreamDescription).joinToString("\n")
        if (text.isBlank()) return null
        val info = runCatching { ReleaseParser.parse(text) }.logged(TAG, "fileLine parse").getOrNull() ?: return null
        val parts = mutableListOf<String>()
        info.height?.let { h -> parts += if (h >= 2000) "4K" else "${h}p" }
        when (info.hdr) {
            HdrKind.DOLBY_VISION -> parts += "Dolby Vision"
            HdrKind.HDR10_PLUS -> parts += "HDR10+"
            HdrKind.HDR -> parts += "HDR"
            null -> Unit
        }
        when (info.codec) {
            VideoCodec.HEVC -> parts += "HEVC"
            VideoCodec.AVC -> parts += "H.264"
            VideoCodec.AV1 -> parts += "AV1"
            null -> Unit
        }
        BITRATE.find(text)?.let { m -> parts += "${m.groupValues[1].replace('.', ',')} Mb/s" }
        info.textSizeBytes?.takeIf { it > 0 }?.let { parts += size(it) }
        val langs = info.audioLanguages.map { it.lowercase() }
        when {
            info.lektor -> parts += "lektor PL"
            info.dubbing || "pl" in langs || "pol" in langs -> parts += "polski dźwięk"
            langs.isNotEmpty() -> parts += langs.take(2).joinToString("/") { it.uppercase() }
        }
        if (info.subtitleLanguages.any { it.equals("pl", true) || it.equals("pol", true) }) parts += "napisy PL"
        if (s.isTorrentStream && s.torrentDownloadSpeed > 0) parts += "pobieram ${speed(s.torrentDownloadSpeed)}"
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }

    private val BITRATE = Regex("""(\d+(?:[.,]\d+)?)\s*(?:Mbps|Mb/s|Mbit)""", RegexOption.IGNORE_CASE)

    private fun size(b: Long): String {
        val gb = b / 1_073_741_824.0
        return if (gb >= 1) String.format(java.util.Locale("pl"), "%.1f GB", gb) else "${b / 1_048_576} MB"
    }

    private fun speed(bytesPerSec: Long): String = String.format(java.util.Locale("pl"), "%.1f Mb/s", bytesPerSec * 8 / 1_000_000.0)

    fun log(msg: String) = Log.i(TAG, msg)
}
