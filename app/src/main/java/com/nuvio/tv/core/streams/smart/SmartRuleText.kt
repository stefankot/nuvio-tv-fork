package com.nuvio.tv.core.streams.smart

/**
 * forge (owner 13.11, variant A): the automatic choice written as a Regex the viewer can read and correct.
 * Settings show it under „Reguła wyboru”; editing it saves the edited text and switches the mode to Regex.
 * It is a readable approximation of the ranker (audio first, then resolution by preset), not the ranker itself.
 */
object SmartRuleText {
    fun regexFor(preset: SmartPreset, audioLanguage: String?): String {
        val audio = audioPart(audioLanguage)
        val res = when (preset) {
            SmartPreset.SAVER -> "1080p|720p"
            SmartPreset.BALANCED -> "2160p|4k|1080p"
            SmartPreset.BEST -> "2160p|4k|remux|1080p"
        }
        return "(?i)^" + (audio?.let { "(?=.*($it))" } ?: "") + "(?=.*($res))"
    }

    private fun audioPart(language: String?): String? {
        val code = language?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        return when (code.take(2)) {
            "pl" -> "lektor|dubbing|pldub|pl\\.dub|polish"
            "en" -> "english|eng"
            else -> code.take(2)
        }
    }
}
