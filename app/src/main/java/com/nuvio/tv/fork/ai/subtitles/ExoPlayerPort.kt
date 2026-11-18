@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.nuvio.tv.fork.ai.subtitles

import androidx.media3.common.text.Cue
import androidx.media3.extractor.text.CuesWithTiming
import com.nuvio.tv.domain.model.Subtitle
import com.nuvio.tv.ui.screens.player.PlayerEvent
import com.nuvio.tv.ui.screens.player.PlayerRuntimeController
import com.nuvio.tv.ui.screens.player.commitPreparedSidecarSubtitle
import com.nuvio.tv.ui.screens.player.currentSidecarGenerationFor
import com.nuvio.tv.ui.screens.player.disableSubtitles
import com.nuvio.tv.ui.screens.player.downloadSubtitleBody
import com.nuvio.tv.ui.screens.player.onEvent
import com.nuvio.tv.ui.screens.player.parseSidecarTimedCuesRobust
import com.nuvio.tv.ui.screens.player.startSidecarAddonSubtitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

/**
 * ExoPlayer adapter on the existing buffer-preserving sidecar layer: no setMediaSource, the film keeps playing
 * and buffering. The translated set gets its own key (`ai://…`); every later commit names that key and its
 * generation, so a late result can never land on another subtitle, episode or player. MPV is not supported
 * (its renderer is separate): [AiSubtitles.availableFor] reports it to the user instead.
 */
class ExoPlayerPort(private val controller: PlayerRuntimeController, private val docKeyProvider: () -> String) : PlayerPort {
    private val videoAtStart = controller.currentVideoId
    override val materialId: String = listOf(controller.contentId ?: controller.title, controller.currentVideoId ?: "").joinToString("|")
    override val title: String = controller.title + (controller.currentEpisodeTitle?.let { " — $it" } ?: "")
    private var generation: Long? = null
    private val url get() = "ai://pl/${docKeyProvider()}.srt"

    private suspend fun <T> main(block: () -> T): T = withContext(Dispatchers.Main.immediate) { block() }

    private fun Subtitle.option() = SubtitleOption(id, url, lang, "$addonName ${getDisplayLanguage()}".trim())

    override suspend fun positionMs(): Long = main { controller._exoPlayer?.currentPosition ?: 0L }
    override suspend fun options(): List<SubtitleOption> = main { controller._uiState.value.addonSubtitles.filterNot { it.url.startsWith("ai://") }.map { it.option() } }
    override suspend fun selected(): SubtitleOption? = main { controller._uiState.value.selectedAddonSubtitle?.takeUnless { it.url.startsWith("ai://") }?.option() }
    override suspend fun audioLanguages(): List<String> = main {
        val tracks = controller._uiState.value.audioTracks
        // The selected track first; a dub does not establish the original, so it is only a hint for the source choice.
        (tracks.filter { it.isSelected } + tracks).mapNotNull { it.language?.takeIf { l -> l.isNotBlank() && l != "und" } }.distinct()
    }

    override suspend fun displayedCues(option: SubtitleOption): List<DocCue>? = main {
        if (controller.activeSidecarSubtitleKey != option.url || controller.sidecarTimedCues.isEmpty()) null
        else toDocCues(controller.sidecarTimedCues) // includes AutoSync retiming already applied
    }

    override suspend fun loadCues(option: SubtitleOption): List<DocCue> {
        val sub = main { controller._uiState.value.addonSubtitles.firstOrNull { it.url == option.url } } ?: return emptyList()
        val raw = com.nuvio.tv.fork.subs.TextSubtitleFormats.toSrt(controller.downloadSubtitleBody(sub.url, sub.lang, sub.headers)) // MPL2/MicroDVD → SRT
        if (raw.length > 8 * 1024 * 1024) throw BlockFailedException("Plik napisów za duży")
        return withContext(Dispatchers.Default) { toDocCues(parseSidecarTimedCuesRobust(raw, sub.url).cues) }
    }

    override suspend fun publish(cues: List<DocCue>, label: String): PublishResult = main {
        if (controller._exoPlayer == null || controller.currentVideoId != videoAtStart) return@main PublishResult.PLAYER_GONE
        val key = url
        val gen = generation
        if (gen == null) {
            // First publication: same steps as the base add-on selection, with the text supplied in memory.
            val synthetic = Subtitle(id = "ai_pl_${docKeyProvider()}", url = key, lang = "pl", addonName = label, addonLogo = null)
            controller.disableSubtitles()
            controller._uiState.update { it.copy(selectedAddonSubtitle = synthetic, selectedSubtitleTrackIndex = -1) }
            val srt = toSrt(cues)
            if (!controller.startSidecarAddonSubtitle(synthetic) { srt }) return@main PublishResult.USER_SWITCHED
            generation = controller.currentSidecarGenerationFor(key)
            return@main if (generation == null) PublishResult.USER_SWITCHED else PublishResult.OK
        }
        if (controller.currentSidecarGenerationFor(key) != gen) return@main PublishResult.USER_SWITCHED
        // Until the first parse finished, that job would overwrite our cues: wait for it.
        if (controller.sidecarTimedCues.isEmpty()) return@main PublishResult.RETRY_LATER
        if (controller.commitPreparedSidecarSubtitle(key, key, toTimed(cues), gen)) PublishResult.OK else PublishResult.USER_SWITCHED
    }

    override suspend fun restore(option: SubtitleOption) = main {
        val sub = controller._uiState.value.addonSubtitles.firstOrNull { it.url == option.url } ?: return@main
        controller.onEvent(PlayerEvent.OnSelectAddonSubtitle(sub))
    }

    companion object {
        fun toDocCues(timed: List<CuesWithTiming>): List<DocCue> {
            val out = ArrayList<DocCue>(timed.size)
            for (t in timed.sortedBy { it.startTimeUs }) {
                if (t.durationUs == androidx.media3.common.C.TIME_UNSET || t.durationUs < 1000) continue
                val text = t.cues.mapNotNull { it.text?.toString()?.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
                if (text.isBlank()) continue
                val start = t.startTimeUs / 1000
                out += DocCue("c%05d".format(java.util.Locale.ROOT, out.size + 1), start, start + t.durationUs / 1000, text)
            }
            return out
        }

        fun toTimed(cues: List<DocCue>): List<CuesWithTiming> = cues.map { c ->
            CuesWithTiming(listOf(Cue.Builder().setText(c.text).build()), c.startMs * 1000, (c.endMs - c.startMs) * 1000)
        }

        fun toSrt(cues: List<DocCue>): String = SubtitleExport.toSrt(cues.map { ValidatedCue(SourceCue(it.id, it.startMs, it.endMs, it.text), it.text) })
    }
}
