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

    fun primaryLanguage(tag: String): String =
        tag.trim().lowercase(java.util.Locale.ROOT).substringBefore('-').substringBefore('_')

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
