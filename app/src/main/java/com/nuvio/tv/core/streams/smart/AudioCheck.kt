package com.nuvio.tv.core.streams.smart

import com.nuvio.tv.domain.model.normalizeLanguageCode

/** forge (N7): does the opened file really carry the audio its name promised? Pure. */
enum class AudioVerdict { OK, MISSING, NOT_JUDGED }

/**
 * [wanted] is the viewer's language, [claimedTier] what the release name promised ([SmartSourceRanker.audioTier]),
 * [trackLanguages] the languages of the audio tracks the player found. Only a file that promised the language is
 * judged; tracks without a language tell nothing.
 */
fun judgeAudio(wanted: String?, claimedTier: Int, trackLanguages: List<String?>): AudioVerdict {
    val want = audioLanguageKey(wanted) ?: return AudioVerdict.NOT_JUDGED
    if (claimedTier < SmartSourceRanker.AUDIO_LANGUAGE || trackLanguages.isEmpty()) return AudioVerdict.NOT_JUDGED
    val known = trackLanguages.mapNotNull(::audioLanguageKey).filter { it != "und" }
    if (known.isEmpty()) return AudioVerdict.NOT_JUDGED
    return if (want in known) AudioVerdict.OK else AudioVerdict.MISSING
}

/** "pl", "pol", "Polish", "pl-PL" → "pl", with the project's language normalizer. */
private fun audioLanguageKey(value: String?): String? {
    val base = value?.trim()?.substringBefore('-')?.substringBefore('_')?.takeIf { it.isNotBlank() } ?: return null
    return normalizeLanguageCode(base)
}
