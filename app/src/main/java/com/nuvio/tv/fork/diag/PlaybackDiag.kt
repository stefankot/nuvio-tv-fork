package com.nuvio.tv.fork.diag

import android.content.Context
import android.util.Log
import com.nuvio.tv.ui.screens.player.PlayerUiState
import com.nuvio.tv.ui.screens.player.TrackInfo

/**
 * Diagnostyka odtwarzania do testów (znacznik logcat „ForkDiag”): przy starcie każdego strumienia i przy zmianie
 * wybranej ścieżki zapisuje, co oferuje źródło (dźwięk, napisy, obraz/HDR), co odtwarzacz wybrał i czego chce profil.
 * Linia „mismatch=…” wskazuje, że preferencja nie została spełniona, choć źródło miało pasującą ścieżkę.
 * Wołane z PlayerBridge.set (plik forka; zero zmian w bazie). Czyta: fork-tools/uitest/playdiag.py.
 */
object PlaybackDiag {
    private const val TAG = "ForkDiag"
    private var lastKey: String? = null

    fun onState(c: Context?, s: PlayerUiState) {
        val url = s.currentStreamUrl ?: return
        if (s.audioTracks.isEmpty() && s.subtitleTracks.isEmpty()) return
        val key = "$url|${s.selectedAudioTrackIndex}|${s.selectedSubtitleTrackIndex}|${s.audioTracks.size}|${s.subtitleTracks.size}|${s.isHdrVideo}"
        if (key == lastKey) return
        lastKey = key
        runCatching { log(c, s) }
    }

    fun reset() { lastKey = null }

    private fun t(x: TrackInfo) = "${x.index}:${x.language ?: "?"}/${x.codec ?: "?"}${x.channelCount?.let { "/${it}ch" } ?: ""}${if (x.isForced) "/forced" else ""} «${x.name.take(40)}»"

    private fun isPl(l: String?) = l != null && (l.startsWith("pl", true) || l.startsWith("pol", true))

    private fun log(c: Context?, s: PlayerUiState) {
        val audioSel = s.audioTracks.firstOrNull { it.index == s.selectedAudioTrackIndex }
        val subSel = s.subtitleTracks.firstOrNull { it.index == s.selectedSubtitleTrackIndex }
        val wish = c?.let { runCatching { com.nuvio.tv.fork.sources.SourceChoice.answer(it) }.getOrNull() } ?: "?"
        val wantsPl = wish == "pl_always" || wish == "pl_if_good"
        val plAudio = s.audioTracks.any { isPl(it.language) }
        val plSubs = s.subtitleTracks.any { isPl(it.language) } || s.addonSubtitles.any { isPl(it.lang) }
        val mismatch = buildList {
            if (wantsPl && plAudio && !isPl(audioSel?.language)) add("audio_pl_dostepne_niewybrane")
            if (wantsPl && !plAudio && plSubs && !isPl(subSel?.language)) add("napisy_pl_dostepne_niewlaczone")
            if (audioSel == null && s.audioTracks.isNotEmpty()) add("brak_wybranej_sciezki_audio")
        }
        val host = runCatching { android.net.Uri.parse(s.currentStreamUrl).host }.getOrNull()
        Log.i(TAG, "stream title=«${s.title.take(60)}» name=«${s.currentStreamName?.take(80)}» addon=${s.currentStreamAddonName} host=$host engine=${s.internalPlayerEngine}")
        Log.i(TAG, "video hdr=${s.isHdrVideo} quality=${s.currentStreamQuality?.label} desc=«${s.currentStreamDescription?.replace('\n', ' ')?.take(160)}»")
        Log.i(TAG, "audio offered=[${s.audioTracks.joinToString("; ") { t(it) }}] selected=${audioSel?.let { t(it) }}")
        Log.i(TAG, "subs offered=[${s.subtitleTracks.joinToString("; ") { t(it) }}] addon=${s.addonSubtitles.size}(pl=${s.addonSubtitles.count { isPl(it.lang) }}) selected=${subSel?.let { t(it) }}")
        Log.i(TAG, "wish=$wish mismatch=${if (mismatch.isEmpty()) "none" else mismatch.joinToString(",")}")
    }
}
