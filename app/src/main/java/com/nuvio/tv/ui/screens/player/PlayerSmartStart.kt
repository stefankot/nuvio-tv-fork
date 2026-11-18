package com.nuvio.tv.ui.screens.player

import android.widget.Toast
import com.nuvio.tv.R
import com.nuvio.tv.core.streams.smart.ReleaseParser
import com.nuvio.tv.core.streams.smart.SmartLastFile
import com.nuvio.tv.core.streams.smart.SmartPolicyHolder
import com.nuvio.tv.core.streams.smart.SmartSourceRanker
import com.nuvio.tv.core.streams.smart.SmartStreamRanking
import com.nuvio.tv.data.local.StreamAutoPlayMode

/**
 * forge M3b: the one place the automatic mode reacts to a source's first frame, whether or not the viewer's
 * "Zmieniaj źródło przy problemach" switch runs the startup recovery:
 * - N8: remember the file for this title (the latest first frame wins, so a file the recovery leaves is replaced);
 * - P8: "Brak dźwięku PL — oryginał" once per title, when the file's name promises no audio in the viewer's language;
 * - N9: "Sieć z limitem danych — tryb oszczędny" once per title on a metered network.
 * A source picked by hand in the panel is the viewer's, not the automatic mode's: nothing happens.
 */
internal fun PlayerRuntimeController.onSmartFirstFrame() {
    refreshTryAnotherSource() // the menu row follows the title now playing (next episode, another source)
    if (streamAutoPlayModeSetting != StreamAutoPlayMode.SMART) return
    if (smartTrySession?.pinned == true) return
    // forge (check 13.11): the first start goes through no switch, so forgeSelectedStream is null there and the file
    // was never remembered ("Kontynuuj" → resume=-). Then the automatic mode's own source, else the launch arguments.
    val source = forgeSelectedStream ?: smartTrySession?.current ?: com.nuvio.tv.core.streams.smart.lastFileLaunchStream(
        url = initialStreamUrl, infoHash = navigationArgs.infoHash, fileIdx = navigationArgs.fileIdx,
        filename = navigationArgs.filename, videoHash = navigationArgs.videoHash, videoSize = navigationArgs.videoSize,
        addonName = navigationArgs.addonName,
    )
    if (contentType in listOf("tv", "channel", "cloud")) return
    val policy = SmartPolicyHolder.current
    SmartLastFile.remember(contentId, currentSeason, currentEpisode, source, policy.preset)
    forgeAfterFirstFrame(source, policy) // forge M6 (N6, N12) / M7 (N14)
    val title = "${contentId}:${currentSeason}:${currentEpisode}"
    if (smartNoticesShownFor == title) return
    smartNoticesShownFor = title
    val notices = buildList {
        if (policy.metered) add(context.getString(R.string.forge_smart_metered))
        val wanted = policy.audioLanguage
        if (wanted != null) {
            val candidate = SmartStreamRanking.candidate(source)
            val tier = SmartSourceRanker.audioTier(ReleaseParser.parse(candidate.text, candidate.sizeBytes), wanted)
            if (tier == SmartSourceRanker.AUDIO_SUBTITLES_ONLY || tier == SmartSourceRanker.AUDIO_OTHER) {
                add(context.getString(R.string.forge_smart_audio_original, wanted.uppercase()))
            }
        }
    }
    if (notices.isNotEmpty()) Toast.makeText(context, notices.joinToString("\n"), Toast.LENGTH_LONG).show()
}
