package com.nuvio.tv.fork.voice

import com.nuvio.tv.fork.ai.Embedder
import com.nuvio.tv.fork.ai.EmbedderUnavailable
import com.nuvio.tv.fork.ai.VectorMath

/** Silniki mowy na tekst (B07-6). */
enum class SpeechEngine { WHISPER_SMALL, WHISPER_BASE, SYSTEM, API }

object SpeechEngineChoice {
    /** Wybór automatyczny: ≥3 GB RAM i pobrany model → Whisper small; 2 GB → systemowe (Whisper base tylko na życzenie); klucz API → API (chyba że użytkownik wybrał inaczej). */
    fun auto(totalRamMb: Int, whisperSmallDownloaded: Boolean, userPick: SpeechEngine? = null, hasApiKey: Boolean = false, whisperBaseRequested: Boolean = false): SpeechEngine = when {
        userPick != null -> userPick
        hasApiKey && totalRamMb < 3000 -> SpeechEngine.API
        totalRamMb >= 3000 && whisperSmallDownloaded -> SpeechEngine.WHISPER_SMALL
        whisperBaseRequested -> SpeechEngine.WHISPER_BASE
        else -> SpeechEngine.SYSTEM
    }
}

/** Filtr klawisza usługi ułatwień dostępu (B07-2): przejmuje tylko wybrany klawisz, resztę przepuszcza. */
object KeyFilter {
    const val KEYCODE_ASSIST = 219
    const val KEYCODE_VOICE_ASSIST = 231

    fun shouldIntercept(keyCode: Int, chosen: Int = KEYCODE_ASSIST, serviceEnabled: Boolean = true): Boolean =
        serviceEnabled && keyCode == chosen
}

/** Progi pewności i potwierdzenia (B07-7). */
object Confidence {
    const val SURE = 0.85f
    const val MIN_GAP = 0.05f

    sealed interface Decision {
        data class Run(val call: VoiceCommandCall) : Decision
        data class Suggest(val options: List<VoiceCommandCall>) : Decision
        data class Confirm(val call: VoiceCommandCall) : Decision
        object Nothing : Decision
    }

    /** Pewne i jedno → wykonaj; skutki trudne do cofnięcia → potwierdzenie; niepewne lub dwa blisko siebie → do trzech propozycji. */
    fun decide(candidates: List<VoiceCommandCall>): Decision {
        val sorted = candidates.sortedByDescending { it.confidence }
        val top = sorted.firstOrNull() ?: return Decision.Nothing
        val second = sorted.getOrNull(1)
        val unsure = top.confidence < SURE || (second != null && top.confidence - second.confidence < MIN_GAP)
        return when {
            unsure -> Decision.Suggest(sorted.take(3))
            top.needsConfirm || top.id in HARD -> Decision.Confirm(top)
            else -> Decision.Run(top)
        }
    }

    val HARD = setOf("profile.switch", "app.close")
}

/** Trzecia implementacja parsera: dopasowanie znaczeniowe na [Embedder] (przykłady wzorców liczone raz). */
class SemanticParser(
    private val embedder: Embedder,
    private val patterns: List<CommandPattern> = Patterns.all,
    private val threshold: Float = 0.62f,
) : CommandParser {
    private var cache: List<Pair<CommandPattern, List<FloatArray>>>? = null

    private suspend fun examples(): List<Pair<CommandPattern, List<FloatArray>>> = cache ?: run {
        val all = patterns.flatMap { p -> p.examples.map { p to it } }
        val vecs = embedder.embed(all.map { it.second }).map { VectorMath.truncateNormalize(it) }
        patterns.map { p -> p to all.indices.filter { all[it].first === p }.map { vecs[it] } }.also { cache = it }
    }

    override suspend fun parse(text: String, context: VoiceContext): VoiceCommandCall? {
        val q = try { VectorMath.truncateNormalize(embedder.embed(listOf(text)).first()) } catch (_: EmbedderUnavailable) { return null }
        val scored = examples().map { (p, vs) -> p to (vs.maxOfOrNull { VectorMath.cosine(q, it) } ?: -1f) }.sortedByDescending { it.second }
        val (best, score) = scored.firstOrNull() ?: return null
        if (score < threshold) return null
        val second = scored.getOrNull(1)?.second ?: 0f
        // Mała różnica między dwoma najlepszymi: niepewność (dymek z propozycjami zdecyduje).
        val conf = if (score - second < Confidence.MIN_GAP) minOf(score, Confidence.SURE - 0.01f) else score
        return VoiceCommandCall(best.id, emptyMap(), conf, best.needsConfirm)
    }
}

/** Kolejność rozumienia: reguły → dopasowanie znaczeniowe → model przez API (tylko z kluczem). Zwraca kandydatów dla [Confidence]. */
class ParserChain(private val parsers: List<CommandParser>) : CommandParser {
    override suspend fun parse(text: String, context: VoiceContext): VoiceCommandCall? {
        for (p in parsers) p.parse(text, context)?.let { return it }
        return null
    }
}
