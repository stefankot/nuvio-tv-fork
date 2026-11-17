package com.nuvio.tv.fork.lektor

import kotlin.math.pow

/**
 * Obwiednia ściszania oryginału (B09-5): ok. −10 dB podczas kwestii, zejście 150 ms, powrót 300 ms.
 * Czysta funkcja czasu; procesor dźwięku mnoży próbki przez [gain].
 */
class DuckEnvelope(private val depthDb: Float = -10f, private val downMs: Long = 150, private val upMs: Long = 300) {
    private val low = 10f.pow(depthDb / 20f)

    /** Wzmocnienie oryginału w chwili [tMs] przy kwestiach [lines] (posortowanych). */
    fun gain(tMs: Long, lines: List<Line>): Float {
        var g = 1f
        for (l in lines) {
            val v = when {
                tMs < l.startMs - downMs -> 1f
                tMs < l.startMs -> 1f - (1f - low) * (tMs - (l.startMs - downMs)) / downMs
                tMs <= l.endMs -> low
                tMs < l.endMs + upMs -> low + (1f - low) * (tMs - l.endMs) / upMs
                else -> 1f
            }
            if (v < g) g = v
        }
        return g
    }

    /** Miks kanału: oryginał × obwiednia + głos × [voiceGain]. */
    fun mix(original: Float, voice: Float, gain: Float, voiceGain: Float = 1f): Float = (original * gain + voice * voiceGain).coerceIn(-1f, 1f)
}

/** Włączanie lektora (B09-6): widoczność w menu dźwięku i automat szczebli profilu. */
object LektorRules {
    fun visibleInMenu(hasPolishSubs: Boolean, hasPolishAudio: Boolean, voiceAvailable: Boolean) = hasPolishSubs && !hasPolishAudio && voiceAvailable

    enum class Step { POLISH_AUDIO, LEKTOR, POLISH_SUBS, NONE }

    /** Profil „Zawsze po polsku”: polski dźwięk, potem lektor (jeśli dostępny), potem napisy. */
    fun step(alwaysPolish: Boolean, hasPolishAudio: Boolean, hasPolishSubs: Boolean, lektorAvailable: Boolean): Step = when {
        hasPolishAudio -> Step.POLISH_AUDIO
        alwaysPolish && hasPolishSubs && lektorAvailable -> Step.LEKTOR
        hasPolishSubs -> Step.POLISH_SUBS
        else -> Step.NONE
    }

    /** Przy lektorze napisy są domyślnie ukryte. */
    fun subtitlesHidden(lektorOn: Boolean) = lektorOn

    const val DOLBY_NOTE = "Lektor wyłącza przekazywanie Dolby do amplitunera."
}
