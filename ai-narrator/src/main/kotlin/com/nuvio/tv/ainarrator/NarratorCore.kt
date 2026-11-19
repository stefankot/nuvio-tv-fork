package com.nuvio.tv.ainarrator

import java.security.MessageDigest
import java.util.Locale

/** Independent contracts: no Nuvio, ExoPlayer or mpv types may enter this file. */
enum class NarratorModel(val apiId: String) {
    FLASH("gemini-3.8-flash-tts"),
    FLASH_LITE("gemini-3.8-flash-lite-tts")
}

enum class NarratorBackend { GEMINI, ZONOS_LOCAL }

data class NarratorLine(val startMs: Long, val endMs: Long, val text: String) {
    init { require(startMs >= 0 && endMs > startMs) }
}

data class NarratorFrame(
    val mediaId: String,
    val positionMs: Long,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val visibleText: String,
    val timedLines: List<NarratorLine> = emptyList()
)

data class NarratorOptions(
    val enabled: Boolean = false,
    val backend: NarratorBackend = NarratorBackend.GEMINI,
    val localEndpoint: String = "",
    val localSpeakerId: String = "",
    val model: NarratorModel = NarratorModel.FLASH_LITE,
    val voice: String = "Kore",
    val narratorVolume: Float = 0.65f,
    val monthlyUsdLimit: Double = 2.0,
    val prefetchMs: Long = 10_000L
)

object NarratorText {
    private val htmlTags = Regex("<[^>]+>")
    private val assTags = Regex("""\{\\[^}]*\}""") // fork: Android (ICU) wymaga \} — bez tego ExceptionInInitializerError na TV
    private val whitespace = Regex("""\s+""")
    private val nonDialogue = Regex("""^\[[^\]]+\]$""") // fork: ICU nie przyjmuje [^]] ani gołego ]

    fun clean(raw: String): String = raw
        .replace(htmlTags, "")
        .replace(assTags, "")
        .replace(whitespace, " ")
        .trim()
        .take(380)

    fun eligible(text: String): Boolean {
        val value = clean(text)
        return value.isNotBlank() && value.any(Char::isLetter) &&
            !nonDialogue.matches(value) &&
            value.count(Char::isLetter) >= 2
    }

    /** Avoid dramatic acting: lexical context modifies only phrasing/intonation. */
    fun style(current: String, previous: String?): String {
        val text = clean(current)
        val previousQuestion = previous?.trim()?.endsWith("?") == true
        // Fork 19.11 (właściciel): klasyczny polski lektor filmowy — styl, nie naśladowanie konkretnej osoby. Jedna stała
        // instrukcja (ta sama barwa i dynamika w każdej kwestii); pytania tylko lekko podniesione na końcu.
        val inflection = if (text.endsWith("?")) " Pytanie tylko minimalnie podnieś na końcu." else ""
        if (previousQuestion) Unit
        return "Klasyczny polski lektor filmowy czytający tłumaczenie na tle oryginalnej ścieżki: niski, spokojny męski głos, " +
            "równy i beznamiętny, prawie płaski ton, bez grania emocji i bez odgrywania postaci. Tempo szybkie i stałe, " +
            "zdania czytane płynnie, bez dramatycznych pauz. Bardzo wyraźna dykcja, lekki dystans, rzeczowo. " +
            "Nie dodawaj ani nie pomijaj słów.$inflection"
    }

    fun fingerprint(model: NarratorModel, voice: String, text: String, style: String): String =
        fingerprint("${model.apiId}:$voice", text, style)

    /** Backend and local server identity are part of the cache key; tokens never are. */
    fun fingerprint(identity: String, text: String, style: String): String {
        val raw = "$identity\u0000$text\u0000$style"
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    }
}

object NarratorPlanner {
    /** A seek detector must not confuse pause/buffering with a timeline discontinuity. */
    fun isDiscontinuity(previousMs: Long, currentMs: Long, elapsedMs: Long): Boolean =
        previousMs >= 0L && (currentMs < previousMs - 750L ||
            currentMs - previousMs > elapsedMs.coerceAtLeast(0L) + 1_750L)

    fun current(lines: List<NarratorLine>, positionMs: Long): NarratorLine? =
        lines.firstOrNull { positionMs in it.startMs until it.endMs && NarratorText.eligible(it.text) }

    fun upcoming(lines: List<NarratorLine>, positionMs: Long, horizonMs: Long): List<NarratorLine> =
        lines.asSequence()
            .filter { it.endMs > positionMs && it.startMs <= positionMs + horizonMs && NarratorText.eligible(it.text) }
            .take(8)
            .toList()

    fun stillAudible(line: NarratorLine, positionMs: Long, toleranceMs: Long = 180L): Boolean =
        positionMs >= line.startMs - toleranceMs && positionMs < line.endMs - toleranceMs
}

/** Prices per million input/audio-output tokens, not per generated character. */
object NarratorPrices {
    fun rate(model: NarratorModel, year: Int, month: Int): Pair<Double, Double> {
        val afterPromotion = year > 2026 || (year == 2026 && month > 12)
        return when (model) {
            NarratorModel.FLASH -> if (afterPromotion) 1.0 to 18.0 else 0.5 to 9.0
            NarratorModel.FLASH_LITE -> if (afterPromotion) 1.0 to 12.0 else 0.5 to 6.0
        }
    }

    fun estimatedUsd(model: NarratorModel, textLength: Int, seconds: Double, year: Int, month: Int): Double {
        val (input, output) = rate(model, year, month)
        // The result is an estimate, NOT a guarantee or a measured API charge.
        val estimatedInputTokens = (textLength / 3.0) + 80.0
        val estimatedAudioTokens = seconds * 25.0
        return (estimatedInputTokens * input + estimatedAudioTokens * output) / 1_000_000.0
    }
}
