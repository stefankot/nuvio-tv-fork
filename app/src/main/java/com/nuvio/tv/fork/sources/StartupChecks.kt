package com.nuvio.tv.fork.sources

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.nuvio.tv.core.streams.smart.ReleaseParser
import com.nuvio.tv.core.util.parseRuntimeMinutes
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.fork.sources.kicia.BadReason
import com.nuvio.tv.fork.sources.kicia.DurationSanity
import com.nuvio.tv.fork.sources.kicia.DurationVerdict
import com.nuvio.tv.fork.sources.kicia.SmartBadSources
import com.nuvio.tv.fork.sources.kicia.SoundVerdict
import com.nuvio.tv.fork.sources.kicia.judgeDuration
import com.nuvio.tv.fork.sources.kicia.judgeSound
import com.nuvio.tv.ui.screens.player.PlayerRuntimeController
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Kontrole Kicii po otwarciu pliku (punkt 4 kolejki), podpięte w automatyczne przełączanie źródeł bazy
 * (`PlayerAutoSourceRecovery`, wpięcie `Fork hook: kicia-checks`):
 *  - zły czas trwania (Kicia `DurationCheck`: film ±10%, odcinek 2×; dwa zgodne „złe” pliki = zły opis tytułu, gra dalej),
 *  - brak dźwięku, który urządzenie odtwarza (Kicia `SoundCheck`, wzór Echo: 2 s po pierwszej klatce),
 *  - pamięć złych źródeł (Kicia `SmartBadSources`): źródło, które nie wystartowało albo nie przeszło kontroli,
 *    trafia na koniec kolejności rankingu (KeRanking) na 30 min, zły plik na 24 h; ręczny wybór zdejmuje blokadę.
 * Przełącznik: [KEY] w kategorii forka (domyślnie włączony).
 */
object StartupChecks {
    const val KEY = "kicia_startup_checks"
    private const val TAG = "ForkStartupChecks"
    @Volatile private var on = true

    fun warm(context: Context) {
        SmartBadSources.init(context.applicationContext)
        on = ForkFeatures.enabled(context, KEY, true)
    }

    /** Ranking: zablokowane źródła na koniec (nigdy nie usuwane). Stabilnie. */
    fun demoteBad(streams: List<Stream>): List<Stream> {
        if (!on) return streams
        val (bad, good) = streams.partition { runCatching { SmartBadSources.isBlocked(it) }.getOrDefault(false) }
        return if (bad.isEmpty()) streams else good + bad
    }

    /** „Spróbuj innego źródła”: pominięte przez widza odłożone na 30 min (Kicia USER_SKIP). */
    fun markSkipped(stream: Stream) { if (on) runCatching { SmartBadSources.mark(stream, BadReason.USER_SKIP) } }

    /** Ręczny wybór źródła: zdejmij jego blokadę. */
    fun clearFor(stream: Stream) = runCatching { SmartBadSources.clearFor(stream) }

    /** Ile źródeł jest teraz odłożonych (wiersz w ustawieniach). */
    fun badCount(): Int = runCatching { SmartBadSources.liveCount() }.getOrDefault(0)
    fun clearAll() = runCatching { SmartBadSources.clearAll() }

    sealed interface Verdict {
        data object Ok : Verdict
        /** Jeszcze nie wiadomo (np. czekanie na dźwięk po pierwszej klatce): nie potwierdzaj startu. */
        data object Wait : Verdict
        data class Bad(val reason: String) : Verdict
    }

    /** Jedno automatyczne uruchomienie tytułu (wiele prób). */
    class Session {
        private val sanity = DurationSanity()
        private val wrongDuration = mutableListOf<Stream>()
        private var current: Stream? = null
        private var durationChecked = false
        private var soundOk = false
        private var firstFrameAtMs: Long? = null
        private var badReason: BadReason? = null

        /** Nowa próba: [stream] = źródło, które za chwilę gra (null = nieznane, wtedy kontrole czasu i pamięć pomijane). */
        fun attempt(stream: Stream?) {
            current = stream; durationChecked = false; soundOk = false; firstFrameAtMs = null; badReason = null
        }

        fun verdict(player: PlayerRuntimeController): Verdict {
            if (!on) return Verdict.Ok
            return runCatching { check(player) }.getOrElse { Verdict.Ok }
        }

        private fun check(player: PlayerRuntimeController): Verdict {
            badReason?.let { return Verdict.Bad(it.name) }
            val now = SystemClock.elapsedRealtime()
            if (player.hasRenderedFirstFrame && firstFrameAtMs == null) firstFrameAtMs = now
            val source = current
            val timeline = player._playbackTimeline.value
            if (source != null && !durationChecked && !sanity.disabled && timeline.duration > 0) {
                durationChecked = true
                val seekable = !timeline.isLive && player._exoPlayer?.isCurrentMediaItemSeekable != false
                val episode = player.currentEpisode != null
                val expectedSec = expectedSec(player)
                val text = listOfNotNull(source.name, source.title, source.description, source.behaviorHints?.filename).joinToString(" ")
                val edition = runCatching { ReleaseParser.parse(text).edition }.getOrNull()
                if (judgeDuration(timeline.duration.takeIf { seekable }, expectedSec, episode, edition) == DurationVerdict.WRONG) {
                    if (!sanity.onWrong(com.nuvio.tv.fork.sources.kicia.badSourceKey(source), timeline.duration)) {
                        wrongDuration += source
                        return bad(BadReason.WRONG_DURATION, "duration file=${timeline.duration} expected_s=$expectedSec")
                    }
                    // Dwa zgodne pliki: to opis tytułu jest zły, nie pliki.
                    (wrongDuration + source).forEach { SmartBadSources.clearFor(it) }
                    wrongDuration.clear()
                    Log.i(TAG, "duration metadata wrong, playing")
                }
            }
            if (soundOk) return Verdict.Ok
            val state = player._uiState.value
            val sound = judgeSound(
                hasVideo = player.currentStreamHasVideoTrack,
                audioTrackCount = state.audioTracks.size,
                selectedAudioIndex = state.selectedAudioTrackIndex,
                tracksKnown = player._exoPlayer?.currentTracks?.groups?.isNotEmpty() == true,
                msSinceFirstFrame = firstFrameAtMs?.let { now - it },
            )
            return when (sound) {
                SoundVerdict.OK -> { if (firstFrameAtMs != null) soundOk = true; Verdict.Ok }
                SoundVerdict.WAIT -> Verdict.Wait
                SoundVerdict.SILENT -> bad(BadReason.AUDIO_MISSING, "silent tracks=${state.audioTracks.size}")
            }
        }

        private fun bad(reason: BadReason, log: String): Verdict {
            badReason = reason
            Log.i(TAG, "${reason.name} $log")
            com.nuvio.tv.fork.family.FamilyStats.badSource(reason.name, PlayerBridge.current()?.currentStreamName) // panel Rodzina
            return Verdict.Bad(reason.name)
        }

        /**
         * Próba się skończyła bez potwierdzonego startu: zapamiętaj źródło. Powód: z kontroli; błąd dekodera/kontenera
         * (Kicia FileInherentFailure) = zły plik, odkładany 24 h u każdego dodatku; inaczej „nie wystartowało” (30 min).
         */
        fun onLeaving(player: PlayerRuntimeController) {
            if (!on) return
            val source = current ?: return
            val code = player.lastPlaybackIssueError?.errorCode
            val reason = badReason ?: if (code != null && com.nuvio.tv.fork.sources.kicia.isFileInherentPlaybackError(code)) BadReason.DECODER else BadReason.NO_START
            runCatching { SmartBadSources.mark(source, reason) }
        }

        /** Czas tytułu z metadanych w pamięci (bez zapytań): film = runtime, odcinek = runtime odcinka. */
        private fun expectedSec(player: PlayerRuntimeController): Long? {
            val type = player.contentType ?: return null
            val id = player.contentId ?: return null
            val meta = runCatching { player.metaRepository.getCachedMeta(type, id) }.getOrNull() ?: return null
            val ep = player.currentEpisode
            val minutes = if (ep != null) meta.videos.firstOrNull { it.season == player.currentSeason && it.episode == ep }?.runtime
                else parseRuntimeMinutes(meta.runtime)
            return minutes?.takeIf { it > 0 }?.times(60L)
        }
    }
}
