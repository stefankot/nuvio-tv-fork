package com.nuvio.tv.skin.kicia.livetv

import com.nuvio.tv.fork.tv.EpgExtras
import com.nuvio.tv.reshaped.livetv.LiveTvCatchupLinks
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvClock
import com.nuvio.tv.reshaped.livetv.LiveTvHttp
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import com.nuvio.tv.reshaped.livetv.LiveTvReplay
import com.nuvio.tv.reshaped.livetv.LiveTvRepository
import com.nuvio.tv.ui.screens.player.PlayerMediaSourceFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import com.nuvio.tv.fork.logged

/**
 * Owner 13.11:
 * 1. Archive programmes often do not load. Every airing of the same episode that the providers keep
 *    (any channel with an archive) is asked for at once; the first whose stream really answers
 *    (playlist → segment, or the first bytes of a TS) plays. [pick]
 * 2. While a programme plays live, an earlier airing of the same episode is looked for in the
 *    background, checked the same way, and offered (an archive copy can skip the adverts). [findEarlier]
 */
internal object ForgeAiringRace {
    data class Win(val channel: LiveTvChannel, val programme: LiveTvProgramme, val replay: LiveTvReplay)

    private const val MAX_CANDIDATES = 6
    private const val RACE_MS = 12_000L

    private val probeClient: OkHttpClient by lazy {
        LiveTvHttp.client.newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    /** Same title and, as far as the guide tells, the same episode (label, episode title or description). */
    fun sameEpisode(a: LiveTvProgramme, b: LiveTvProgramme): Boolean {
        if (!forgeSameTitle(a.title, b.title)) return false
        val ea = a.episode?.trim()?.lowercase()
        val eb = b.episode?.trim()?.lowercase()
        if (!ea.isNullOrBlank() && !eb.isNullOrBlank()) return ea == eb
        val sa = EpgExtras.get(a.title, a.startEpochMs)?.subTitle?.trim()
        val sb = EpgExtras.get(b.title, b.startEpochMs)?.subTitle?.trim()
        if (!sa.isNullOrBlank() && !sb.isNullOrBlank()) return sa.equals(sb, ignoreCase = true)
        val da = a.description?.trim()?.take(120)
        val db = b.description?.trim()?.take(120)
        if (!da.isNullOrBlank() && !db.isNullOrBlank()) return da.equals(db, ignoreCase = true)
        return false // a daily show with the same title is not the same episode
    }

    /** Same title, episode and subtitle unknown on one side — only the descriptions agree. */
    private fun descriptionOnly(a: LiveTvProgramme, b: LiveTvProgramme): Boolean {
        if (!a.episode.isNullOrBlank() && !b.episode.isNullOrBlank()) return false
        val sa = EpgExtras.get(a.title, a.startEpochMs)?.subTitle
        val sb = EpgExtras.get(b.title, b.startEpochMs)?.subTitle
        return sa.isNullOrBlank() || sb.isNullOrBlank()
    }

    /** The description of [programme] repeats on other airings of its title in [sched] — a series blurb, not an episode's. */
    private fun genericDescription(sched: List<LiveTvProgramme>, programme: LiveTvProgramme): Boolean {
        val d = programme.description?.trim()?.take(120) ?: return false
        return sched.count { forgeSameTitle(it.title, programme.title) && it.description?.trim()?.take(120).equals(d, ignoreCase = true) } >= 2
    }

    /** Playable archived airings of [programme] on every channel with an archive, newest first, [programme] itself left out. */
    private fun otherAirings(programme: LiveTvProgramme, nowMs: Long, before: Long = Long.MAX_VALUE): List<Pair<LiveTvChannel, LiveTvProgramme>> {
        val out = ArrayList<Pair<LiveTvChannel, LiveTvProgramme>>()
        val seen = HashSet<String>()
        var generic = 0
        LiveTvRepository.uiState.value.channels.forEach { ch ->
            val catchup = ch.catchup ?: return@forEach
            val sched = LiveTvRepository.schedule(ch.guideKey)
            var genericHere: Boolean? = null
            sched.forEach { p ->
                if (p.startEpochMs == programme.startEpochMs && forgeSameTitle(p.title, programme.title)) return@forEach
                if (p.startEpochMs >= before || p.startEpochMs >= nowMs) return@forEach
                if (!LiveTvCatchupLinks.isPlayable(catchup, p, nowMs) || !sameEpisode(p, programme)) return@forEach
                // UX JMGO 25.11 (#51): „Świat według Kiepskich” bez numeru odcinka — ten sam opis przy każdej emisji (opis serialu),
                // więc „wcześniejsza emisja” była innym odcinkiem. Zgodny tylko opis → emisja się liczy, gdy na tym kanale ten opis
                // nie powtarza się przy innych emisjach tego tytułu.
                if (descriptionOnly(p, programme)) {
                    val g = genericHere ?: genericDescription(sched, programme).also { genericHere = it }
                    if (g) { generic++; return@forEach }
                }
                if (seen.add(ch.streamUrl + "|" + p.startEpochMs)) out += ch to p
            }
        }
        if (generic > 0) android.util.Log.i("ForgeAiringRace", "'" + programme.title + "': pominięte emisje ze wspólnym opisem serialu (" + generic + ") — numer odcinka nieznany")
        // Ended airings first (a whole copy), then the newest.
        return out.sortedWith(compareBy<Pair<LiveTvChannel, LiveTvProgramme>> { it.second.stopEpochMs > nowMs }.thenByDescending { it.second.startEpochMs })
    }

    /**
     * Hook for Reshaped's playCatchup: the airing to play. With no other airing nothing changes (no check, no delay);
     * otherwise all of them race and the first that answers wins. When none answers, the chosen one is played as before.
     */
    suspend fun pick(channel: LiveTvChannel, programme: LiveTvProgramme): Triple<LiveTvChannel, LiveTvProgramme, LiveTvReplay?> {
        val now = LiveTvClock.nowEpochMs()
        val others = withContext(Dispatchers.Default) { runCatching { otherAirings(programme, now) }.logged("ForkKiciaLivetv", "ForgeAiringRace.kt:84").getOrDefault(emptyList()) }
        android.util.Log.i("ForgeAiringRace", "pick '" + programme.title + "' ep=" + programme.episode + " on " + channel.name + ": " + others.size + " other airings") // forge JMGO 6 diagnosis
        if (others.isEmpty()) return Triple(channel, programme, LiveTvRepository.catchupChannel(channel, programme))
        com.nuvio.tv.skin.kicia.components.ForgeToast.show("Szukam działającej emisji (" + (others.size.coerceAtMost(MAX_CANDIDATES - 1) + 1) + ")", top = true)
        val won = race(listOf(channel to programme) + others.take(MAX_CANDIDATES - 1))
        if (won != null) return Triple(won.channel, won.programme, won.replay)
        return Triple(channel, programme, LiveTvRepository.catchupChannel(channel, programme))
    }

    /** An earlier airing of [programme] (on now on [channel]) whose archive copy answers, or null. */
    suspend fun findEarlier(channel: LiveTvChannel, programme: LiveTvProgramme): Win? {
        val now = LiveTvClock.nowEpochMs()
        val earlier = withContext(Dispatchers.Default) {
            runCatching { otherAirings(programme, now, before = programme.startEpochMs).filter { it.second.stopEpochMs <= now } }.logged("ForkKiciaLivetv", "ForgeAiringRace.kt:97").getOrDefault(emptyList())
        }
        android.util.Log.i("ForgeAiringRace", "findEarlier '" + programme.title + "' ep=" + programme.episode + " on " + channel.name + ": " + earlier.size + " earlier airings") // forge JMGO 7 diagnosis
        if (earlier.isEmpty()) return null
        return race(earlier.take(MAX_CANDIDATES)).also { android.util.Log.i("ForgeAiringRace", "findEarlier won=" + (it?.channel?.name ?: "none")) }
    }

    /** Asks for every candidate's replay at once and checks its stream; the first that answers wins. */
    private suspend fun race(candidates: List<Pair<LiveTvChannel, LiveTvProgramme>>): Win? = coroutineScope {
        val result = CompletableDeferred<Win?>()
        val left = AtomicInteger(candidates.size)
        val jobs = candidates.map { (ch, p) ->
            launch(Dispatchers.IO) {
                val win = runCatching {
                    val replay = LiveTvRepository.catchupChannel(ch, p) ?: return@runCatching null
                    when {
                        answers(replay.playback) -> Win(ch, p, replay)
                        replay.fallback != null && answers(replay.fallback.playback) -> Win(ch, p, replay.fallback)
                        else -> null
                    }
                }.logged("ForkKiciaLivetv", "ForgeAiringRace.kt:110").getOrNull()
                if (win != null) result.complete(win) else if (left.decrementAndGet() == 0) result.complete(null)
            }
        }
        val out = withTimeoutOrNull(RACE_MS) { result.await() }
        jobs.forEach { it.cancel() }
        out
    }

    /** Whether [playback]'s stream gives data: an HLS playlist down to its first segment, or the first bytes of a stream. */
    internal suspend fun answers(playback: LiveTvChannel): Boolean { // 22.11: też sonda długości archiwum (fork/tv/CatchupDepth)
        if (playback.stalkerCommand != null) return true // resolved by the player itself; cannot be checked here
        val req = PlayerMediaSourceFactory.normalizePlaybackRequest(playback.streamUrl, playback.headers)
        return withContext(Dispatchers.IO) { runCatching { probe(req.url, req.headers, 0) }.logged("ForkKiciaLivetv", "ForgeAiringRace.kt:130").getOrDefault(false) }
    }

    private fun probe(url: String, headers: Map<String, String>, depth: Int): Boolean {
        if (depth > 3) return false
        val request = Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build()
        probeClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return false
            val bytes = response.peekBody(16_384).bytes()
            if (bytes.isEmpty()) return false
            val head = String(bytes, 0, minOf(bytes.size, 16_384), Charsets.UTF_8)
            if (!head.trimStart('﻿', ' ', '\n', '\r').startsWith("#EXTM3U")) return depth > 0 || bytes.size >= 188
            // A playlist: its first entry (a variant or a segment) must answer too.
            val next = head.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.startsWith("#") } ?: return false
            val base = response.request.url
            val target = base.resolve(next)?.toString() ?: next.toHttpUrlOrNull()?.toString() ?: return false
            return probe(target, headers, depth + 1)
        }
    }
}
