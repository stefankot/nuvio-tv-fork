package com.nuvio.tv.fork.tv.plus

// Kopia 1:1 z homelessbrian/NuvioPlusIPTV @ e5643387f7029374f7deffe1618a544b3a4396a3, app/src/main/java/com/nuvio/tv/livetv/model/LiveTvModels.kt
// (tylko EpgProgram, SUB_TITLE_MARK, subTitle, descriptionText, EpgAssignment; zmieniony pakiet). UPSTREAM_SOURCES.md: „Silnik przewodnika NuvioPlusIPTV”.

data class EpgProgram(
    val startMs: Long,
    val stopMs: Long,
    val title: String,
    val description: String? = null,
    val category: String? = null,
    val episode: String? = null,
    val icon: String? = null,
    /** Release year from the guide (XMLTV <date>, or "(1990)" in the title/description). */
    val year: Int? = null,
    /** Actors and directors from the guide's <credits>, when it has them. */
    val people: List<String> = emptyList(),
    /** Provider archive identifier, retained through guide storage and time shifts. */
    val providerProgramId: String? = null,
    /** Portal identity which issued the archive id; other guides can still provide the text. */
    val providerSourceIdentity: String? = null
) {
    fun isLive(nowMs: Long): Boolean = nowMs in startMs until stopMs
    fun progress(nowMs: Long): Float {
        val span = (stopMs - startMs).coerceAtLeast(1L)
        return ((nowMs - startMs).toFloat() / span).coerceIn(0f, 1f)
    }
}

/** Starts a description whose first line is the guide's sub-title (see [EpgProgram.subTitle]). */
const val SUB_TITLE_MARK = '⁣'

/** The guide's sub-title (an episode's name, a game's match-up), when the guide has one. */
val EpgProgram.subTitle: String?
    get() {
        val d = description ?: return null
        if (d.isEmpty() || d[0] != SUB_TITLE_MARK) return null
        return d.substring(1).substringBefore('\n').trim().takeIf { it.isNotEmpty() }
    }

/** The description without its sub-title line (null when there's nothing else). */
val EpgProgram.descriptionText: String?
    get() {
        val d = description ?: return null
        if (d.isEmpty() || d[0] != SUB_TITLE_MARK) return d
        return d.substringAfter('\n', "").trim().takeIf { it.isNotEmpty() }
    }

/** A channel's guide, picked by hand: which EPG source, and which channel inside it. */
data class EpgAssignment(val sourceId: String, val xmltvId: String) {
    /** "Unassign" in Assign EPG: the channel gets no guide at all (not even an automatic match). */
    val isNone: Boolean get() = sourceId == NONE

    companion object {
        const val NONE = "__none__"
        val None = EpgAssignment(NONE, "none")
    }
}
