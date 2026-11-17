package com.nuvio.tv.fork.tv.ekrany

/** Automat „NA ŻYWO / Z OPÓŹNIENIEM” (B03-7). */
enum class LiveMode { LIVE, DELAYED }

data class DelayState(val mode: LiveMode = LiveMode.LIVE, val behindMs: Long = 0L)

object DelayedPlayback {
    const val EDGE_MS = 15_000L
    private val STEPS = longArrayOf(10_000, 30_000, 60_000, 120_000)

    /** Krok przewijania zależny od tego, jak długo klawisz jest trzymany (indeks powtórzenia). */
    fun stepMs(repeat: Int): Long = when {
        repeat < 3 -> STEPS[0]; repeat < 8 -> STEPS[1]; repeat < 16 -> STEPS[2]; else -> STEPS[3]
    }

    /** Pauza lub przewinięcie w tył: tylko na kanale z archiwum przechodzi w Z OPÓŹNIENIEM, inaczej bez zmian. */
    fun pauseOrRewind(s: DelayState, hasArchive: Boolean, rewindMs: Long = 0L): DelayState =
        if (!hasArchive) s else DelayState(LiveMode.DELAYED, s.behindMs + rewindMs)

    /** Przewijanie w obie strony; po dogonieniu krawędzi powrót do NA ŻYWO. */
    fun seek(s: DelayState, deltaMs: Long): DelayState {
        if (s.mode == LiveMode.LIVE) return s
        val behind = (s.behindMs - deltaMs).coerceAtLeast(0L)
        return if (behind <= EDGE_MS) DelayState(LiveMode.LIVE, 0L) else DelayState(LiveMode.DELAYED, behind)
    }

    /** Czas pozostawania w tyle: oglądanie z opóźnieniem trwa w czasie rzeczywistym, więc zapas rośnie podczas pauzy. */
    fun elapsedPause(s: DelayState, ms: Long) = if (s.mode == LiveMode.DELAYED) s.copy(behindMs = s.behindMs + ms) else s

    fun backToLive() = DelayState(LiveMode.LIVE, 0L)

    /** „−12 min” albo „−45 s”. */
    fun label(s: DelayState): String? = if (s.mode == LiveMode.LIVE) null else {
        val sec = s.behindMs / 1000
        if (sec >= 60) "−${(sec + 30) / 60} min" else "−$sec s"
    }
}
