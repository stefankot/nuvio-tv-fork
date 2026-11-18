// Kopia z Kicii 34c80c6 (gałąź kicia-zrodla-wip, core/player/recovery), dopasowana do forka: tylko pakiet
package com.nuvio.tv.fork.sources.kicia

/** forge M3c K5: whether the file being started has sound the device plays. */
enum class SoundVerdict { OK, WAIT, SILENT }

/**
 * Rule from Echo `hasPlayableAudio` + `SILENT_GRACE_MS` (e952419, `PlayerRuntimeControllerSourceSwitch.kt`): sound
 * plays when a track is listed and one is selected. A file with video that has none [SILENT_GRACE_MS] after its first
 * frame is silent. Nothing known about its tracks = not judged (OK).
 */
fun judgeSound(
    hasVideo: Boolean,
    audioTrackCount: Int,
    selectedAudioIndex: Int,
    tracksKnown: Boolean,
    msSinceFirstFrame: Long?
): SoundVerdict {
    if (!hasVideo) return SoundVerdict.OK
    if (audioTrackCount > 0 && selectedAudioIndex >= 0) return SoundVerdict.OK
    val since = msSinceFirstFrame ?: return SoundVerdict.OK
    if (since < SILENT_GRACE_MS) return SoundVerdict.WAIT
    return if (tracksKnown || audioTrackCount > 0) SoundVerdict.SILENT else SoundVerdict.OK
}

const val SILENT_GRACE_MS = 2_000L
