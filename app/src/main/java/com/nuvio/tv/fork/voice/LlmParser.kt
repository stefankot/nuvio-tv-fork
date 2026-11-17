package com.nuvio.tv.fork.voice

import kotlinx.coroutines.withTimeoutOrNull

/** Dostawca modelu językowego wymienny za interfejsem (pierwsza implementacja: Gemini). */
interface LlmClient {
    /** Zwraca wybór narzędzia (id wzorca + parametry) albo pytanie doprecyzowujące. */
    suspend fun choose(utterance: String, tools: List<LlmTool>, context: List<String>): LlmChoice
}

data class LlmTool(val id: String, val examples: List<String>)

sealed interface LlmChoice {
    data class Tool(val id: String, val params: Map<String, String> = emptyMap()) : LlmChoice
    data class Ask(val question: String) : LlmChoice
}

/** Parser modelowy: prośby swobodne; limit 4 s, nieznane narzędzie odrzucone, wysyłany jest tylko tekst i skrócony kontekst. */
class LlmParser(
    private val client: LlmClient?,
    private val patterns: List<CommandPattern> = Patterns.all,
    private val timeoutMs: Long = TIMEOUT_MS,
) : CommandParser {
    /** Ostatnie pytanie doprecyzowujące (do pokazania w dymku), jeśli model je zadał. */
    @Volatile var lastQuestion: String? = null
        private set

    override suspend fun parse(text: String, context: VoiceContext): VoiceCommandCall? {
        val c = client ?: return null // bez klucza funkcja niewidoczna
        lastQuestion = null
        val tools = patterns.map { LlmTool(it.id, it.examples.take(2)) }
        val choice = try {
            withTimeoutOrNull(timeoutMs) { c.choose(text, tools, listOf(context.screen).filter { it.isNotBlank() }.take(MAX_CONTEXT)) }
        } catch (_: Exception) { null } ?: return null // limit czasu / błąd → wyszukiwanie
        return when (choice) {
            is LlmChoice.Ask -> { lastQuestion = choice.question; null }
            is LlmChoice.Tool -> patterns.firstOrNull { it.id == choice.id }?.let { VoiceCommandCall(it.id, choice.params, 0.8f, it.needsConfirm) }
        }
    }

    companion object { const val TIMEOUT_MS = 4_000L; const val MAX_CONTEXT = 50 }
}
