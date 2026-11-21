package com.nuvio.app.features.player

enum class DubbingSyncMode { QUICK, EXTENDED }

data class DubbingSyncRequest(
    val anchorPositionMs: Long,
    val currentOffsetMs: Long = 0L,
    val windowDurationMs: Long = 30_000L,
    val mode: DubbingSyncMode = DubbingSyncMode.QUICK,
    val searchRadiusMs: Long? = null,
)

data class DubbingSyncProgress(
    val fraction: Float,
    val message: String,
)

enum class DubbingSyncConfidence { HIGH, MEDIUM }

sealed interface DubbingSyncResult {
    data class Match(
        val offsetMs: Long,
        val confidence: DubbingSyncConfidence,
        val peakToSidelobeRatio: Float,
        val anchorAgreementMs: Long,
    ) : DubbingSyncResult

    data class NoMatch(val message: String, val canTryExtended: Boolean) : DubbingSyncResult
    data class Failure(val message: String) : DubbingSyncResult
    data object Unsupported : DubbingSyncResult
}
internal object DubbingTiming {
    private const val MAX_OFFSET_MS = 30L * 60L * 1_000L
    fun clampOffset(offsetMs: Long): Long = offsetMs.coerceIn(-MAX_OFFSET_MS, MAX_OFFSET_MS)
}
