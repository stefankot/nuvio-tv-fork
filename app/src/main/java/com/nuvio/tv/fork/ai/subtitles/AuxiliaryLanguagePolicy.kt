package com.nuvio.tv.fork.ai.subtitles

/** Sources must have been independently checked for language, edit, completeness and quality. */
data class AuxiliarySubtitleCandidate(
    val sourceId: String,
    val languageTag: String,
    val acceptedAfterVerification: Boolean,
    val matchingScore: Int,
)

data class AuxiliarySourceSelection(
    val slavic: AuxiliarySubtitleCandidate?,
    val western: AuxiliarySubtitleCandidate?,
) {
    val complete: Boolean get() = slavic != null && western != null
    val missingCategories: Set<String> get() = buildSet {
        if (slavic == null) add("AUX_SLAVIC")
        if (western == null) add("AUX_WESTERN")
    }
}

/** The LLM may choose *only* a verified available Western candidate, never a language category. */
object AuxiliaryLanguagePolicy {
    val defaultSlavicPriority = listOf("ru", "uk", "cs", "sk", "bg", "sr", "hr", "bs", "sl", "mk", "be")
    private val slavicLanguages = defaultSlavicPriority.toSet()
    val westernLanguages = setOf("en", "de", "fr", "es", "it", "nl", "pt")

    /**
     * Subtitle add-ons deliver ISO 639-1 ("ru"), ISO 639-2/B or /T ("rus", "ger", "deu") or
     * English names ("Russian"); all of them must land in the same category, otherwise the
     * original language or Polish slips through as an auxiliary source.
     */
    fun primaryLanguage(tag: String): String {
        val base = tag.trim().lowercase(java.util.Locale.ROOT).substringBefore('-').substringBefore('_')
            .substringBefore(' ').substringBefore('(')
        return aliases[base] ?: base
    }

    private val aliases: Map<String, String> = buildMap {
        fun alias(code: String, vararg names: String) = names.forEach { put(it, code) }
        alias("pl", "pol", "polish", "polski")
        alias("ru", "rus", "russian")
        alias("uk", "ukr", "ukrainian")
        alias("cs", "ces", "cze", "czech")
        alias("sk", "slk", "slo", "slovak")
        alias("bg", "bul", "bulgarian")
        alias("sr", "srp", "scc", "serbian")
        alias("hr", "hrv", "scr", "croatian")
        alias("bs", "bos", "bosnian")
        alias("sl", "slv", "slovenian", "slovene")
        alias("mk", "mkd", "mac", "macedonian")
        alias("be", "bel", "belarusian")
        alias("en", "eng", "english")
        alias("de", "deu", "ger", "german")
        alias("fr", "fra", "fre", "french")
        alias("es", "spa", "spanish")
        alias("it", "ita", "italian")
        alias("nl", "nld", "dut", "dutch")
        alias("pt", "por", "pob", "portuguese")
    }

    fun chooseForPolish(
        originalLanguage: String,
        candidates: List<AuxiliarySubtitleCandidate>,
        excludedLanguages: Set<String> = emptySet(),
        slavicPriority: List<String> = defaultSlavicPriority,
        modelChosenWesternSourceId: String? = null,
    ): AuxiliarySourceSelection {
        val ranks = slavicPriority.withIndex().associate { (index, language) -> language to index }
        require(slavicPriority.size == defaultSlavicPriority.size && ranks.keys == slavicLanguages) {
            "Slavic priority may be reordered, but no required category language may be added or dropped"
        }
        val original = primaryLanguage(originalLanguage)
        val excluded = excludedLanguages.mapTo(HashSet(excludedLanguages.size), ::primaryLanguage)
        val seenIds = HashSet<String>(candidates.size)
        var slavic: AuxiliarySubtitleCandidate? = null
        var bestSlavicRank = Int.MAX_VALUE
        var western: AuxiliarySubtitleCandidate? = null
        var requestedWestern: AuxiliarySubtitleCandidate? = null

        // One pass, O(n), rather than creating eligible lists and sorting Slavic candidates.
        // Ties intentionally preserve the previous stable-sort and Western comparator behavior.
        for (candidate in candidates) {
            require(seenIds.add(candidate.sourceId)) { "Candidate IDs must be unique" }
            val lang = primaryLanguage(candidate.languageTag)
            if (!candidate.acceptedAfterVerification || candidate.matchingScore < 0 ||
                lang == original || lang == "pl" || lang in excluded
            ) continue

            val rank = ranks[lang]
            if (rank != null && (rank < bestSlavicRank ||
                    (rank == bestSlavicRank &&
                        candidate.matchingScore > (slavic?.matchingScore ?: Int.MIN_VALUE)))
            ) {
                bestSlavicRank = rank
                slavic = candidate
            }
            if (lang in westernLanguages) {
                if (candidate.sourceId == modelChosenWesternSourceId) requestedWestern = candidate
                if (western == null || candidate.matchingScore > western.matchingScore ||
                    (candidate.matchingScore == western.matchingScore && candidate.sourceId < western.sourceId)
                ) western = candidate
            }
        }
        if (modelChosenWesternSourceId != null) {
            western = requestedWestern ?: throw IllegalArgumentException(
                "Model chose an unavailable or ineligible Western source"
            )
        }
        return AuxiliarySourceSelection(slavic, western)
    }
}
