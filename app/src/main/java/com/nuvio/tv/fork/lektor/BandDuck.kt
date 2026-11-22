package com.nuvio.tv.fork.lektor

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow

/**
 * Ściszanie pasmowe pod lektorem (22.11, właściciel: „tło ma być słychać lepiej niż aktorów, a lektor wyraźny”).
 *
 * Zamiast ściszać cały film jednakowo, dzielimy dźwięk na trzy pasma (filtry jednobiegunowe, suma = oryginał):
 * bas (< 250 Hz) i góra (> 3,5 kHz) — muzyka i efekty — ściszane słabo (40% głębokości), pasmo mowy (250 Hz–3,5 kHz)
 * mocniej (150% głębokości). Przy 5.1 kanał centralny (dialogi) w całości jak pasmo mowy, LFE jak tło.
 *
 * Tryb nocny Nuvio działa PRZED mikserem lektora i podbija ciche fragmenty (czyli mowę aktorów) o 6/10/15 dB —
 * wtedy pasmo mowy dostaje dodatkowe −2/−4/−6 dB, żeby lektor nie ginął pod podbitymi dialogami.
 */
class BandDuck {
    private var rate = 0
    private var channels = 0
    private var aLow = 0f
    private var aHigh = 0f
    private var low = FloatArray(0)
    private var high = FloatArray(0)
    /** Wzmocnienie pasma mowy i tła przy pełnym ściszeniu (liniowo). */
    var midFloor = 1f; private set
    var sideFloor = 1f; private set

    fun configure(sampleRate: Int, channelCount: Int) {
        if (sampleRate == rate && channelCount == channels) return
        rate = sampleRate; channels = channelCount
        aLow = coefficient(LOW_HZ, sampleRate)
        aHigh = coefficient(HIGH_HZ, sampleRate)
        low = FloatArray(channelCount); high = FloatArray(channelCount)
    }

    fun reset() { low.fill(0f); high.fill(0f) }

    /** [depthDb] = ściszanie z ustawień (np. −6), [nightExtraDb] = dodatkowe ściszenie mowy przy trybie nocnym (≥ 0). */
    fun setDepth(depthDb: Float, nightExtraDb: Float) {
        midFloor = dbToLinear(depthDb * MID_SHARE - if (depthDb < 0f) nightExtraDb else 0f)
        sideFloor = dbToLinear(depthDb * SIDE_SHARE)
    }

    /** Próbka kanału [c]; [frac] 0..1 = jak głęboko w ściszeniu jesteśmy (0 = bez lektora, 1 = w środku kwestii). */
    fun process(x: Float, c: Int, frac: Float): Float {
        val gm = 1f - frac * (1f - midFloor)
        val gs = 1f - frac * (1f - sideFloor)
        if (channels >= 6) when (c) {
            CENTER -> return x * gm
            LFE -> return x * gs
        }
        val l = low[c] + aLow * (x - low[c]); low[c] = l
        val h = high[c] + aHigh * (x - high[c]); high[c] = h
        return l * gs + (h - l) * gm + (x - h) * gs
    }

    companion object {
        const val LOW_HZ = 250f
        const val HIGH_HZ = 3_500f
        const val MID_SHARE = 1.5f
        const val SIDE_SHARE = 0.4f
        private const val CENTER = 2 // układ 5.1/7.1 Androida: FL, FR, FC, LFE, …
        private const val LFE = 3

        fun coefficient(hz: Float, sampleRate: Int): Float = (1.0 - exp(-2.0 * PI * hz / sampleRate)).toFloat()
        fun dbToLinear(db: Float): Float = 10f.pow(db / 20f)

        /** Dodatkowe ściszenie mowy przy trybie nocnym (klucz `NightMode.settingValue`). */
        fun nightExtraDb(nightKey: String): Float = when (nightKey) { "light" -> 2f; "medium" -> 4f; "strong" -> 6f; else -> 0f }
    }
}
