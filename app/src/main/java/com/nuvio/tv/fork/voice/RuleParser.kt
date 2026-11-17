package com.nuvio.tv.fork.voice

/** Polecenie głosowe: identyfikator wzorca + parametry. */
data class VoiceCommandCall(val id: String, val params: Map<String, String> = emptyMap(), val confidence: Float = 1f, val needsConfirm: Boolean = false)

/** Kontekst rozmowy: bieżący ekran/kanał oraz słowniki encji. */
data class VoiceContext(
    val screen: String = "",
    val channels: EntityIndex = EntityIndex(emptyList()),
    val titles: EntityIndex = EntityIndex(emptyList()),
)

interface CommandParser {
    suspend fun parse(text: String, context: VoiceContext): VoiceCommandCall?
}

/** Parser regułowy: wzorce z parametrami; szerokie („włącz X”) sprawdzane na końcu i rozstrzygane encjami. */
class RuleParser(private val patterns: List<CommandPattern> = Patterns.all) : CommandParser {
    private val broad = setOf("channel.play", "play.title")
    private val ordered = patterns.filter { it.id !in broad } + patterns.filter { it.id in broad }

    override suspend fun parse(text: String, context: VoiceContext): VoiceCommandCall? = parseNow(text, context)

    fun parseNow(text: String, context: VoiceContext = VoiceContext()): VoiceCommandCall? {
        val n = Text.norm(text)
        if (n.isEmpty()) return null
        val d = Text.digits(n)
        var channelHit: VoiceCommandCall? = null
        var titleHit: VoiceCommandCall? = null
        for (p in ordered) {
            for (rx in p.regexes) {
                val m = rx.find(n) ?: rx.find(d) ?: continue
                val params = paramNames(rx).mapNotNull { name -> runCatching { m.groups[name]?.value }.getOrNull()?.let { name to it } }.toMap()
                val call = VoiceCommandCall(p.id, params, 1f, p.needsConfirm)
                when (p.id) {
                    "channel.play" -> channelHit = channelHit ?: call
                    "play.title" -> titleHit = titleHit ?: call
                    else -> return call
                }
            }
        }
        // Szerokie wzorce: kanał, jeśli encja kanału pasuje, inaczej tytuł.
        val ch = channelHit?.params?.get("kanal")?.let { context.channels.match(it).firstOrNull() }
        if (channelHit != null && ch != null && (titleHit == null || ch.score >= 0.8f)) return channelHit.copy(params = channelHit.params + ("kanal_id" to ch.id), confidence = ch.score)
        if (titleHit != null) {
            val t = titleHit.params["tytul"]?.let { context.titles.match(it).firstOrNull() }
            return titleHit.copy(params = titleHit.params + listOfNotNull(t?.let { "tytul_id" to it.id }), confidence = t?.score ?: 0.6f)
        }
        return null
    }

    private fun paramNames(rx: Regex): List<String> = Regex("\\(\\?<([a-z_]+)>").findAll(rx.pattern).map { it.groupValues[1] }.toList()
}
