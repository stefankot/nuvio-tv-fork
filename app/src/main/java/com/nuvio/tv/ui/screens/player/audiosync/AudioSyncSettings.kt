package com.nuvio.tv.ui.screens.player.audiosync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class AudioSamplingMode { FULL, LIMITED, OFF }

/**
 * Settings of the audio subtitle sync fallback (see AutoSync). Persistence is injected by the
 * platform, so Nuvio's own player settings are left untouched.
 */
internal object AudioSyncSettings {
    private val _fallbackEnabled = MutableStateFlow(true)

    /** Sync to the audio when AutoSync keeps a subtitle's original timing. */
    val fallbackEnabled: StateFlow<Boolean> = _fallbackEnabled.asStateFlow()

    private val _samplingOnMobileData = MutableStateFlow(false)

    /** Read short parts of the film ahead of playback on metered networks too. */
    val samplingOnMobileData: StateFlow<Boolean> = _samplingOnMobileData.asStateFlow()

    private val _samplingMode = MutableStateFlow(AudioSamplingMode.FULL)
    val samplingMode: StateFlow<AudioSamplingMode> = _samplingMode.asStateFlow()

    private val _showStatistics = MutableStateFlow(false)

    /** Show the live sync statistics panel in the player's top-left corner. Off by default. */
    val showStatistics: StateFlow<Boolean> = _showStatistics.asStateFlow()

    private var save: (key: String, value: Boolean) -> Unit = { _, _ -> }
    private var saveMode: (String) -> Unit = {}

    fun installPersistence(
        load: (key: String) -> Boolean?,
        save: (key: String, value: Boolean) -> Unit,
        mode: String?,
        saveMode: (String) -> Unit,
    ) {
        this.save = save
        this.saveMode = saveMode
        _fallbackEnabled.value = load(FALLBACK_ENABLED) ?: true
        _samplingOnMobileData.value = load(SAMPLING_ON_MOBILE_DATA) ?: false
        _showStatistics.value = load(SHOW_STATISTICS) ?: false
        _samplingMode.value = AudioSamplingMode.entries.firstOrNull { it.name == mode } ?: AudioSamplingMode.FULL
    }

    fun setFallbackEnabled(enabled: Boolean) {
        _fallbackEnabled.value = enabled
        save(FALLBACK_ENABLED, enabled)
    }

    fun setSamplingOnMobileData(enabled: Boolean) {
        _samplingOnMobileData.value = enabled
        save(SAMPLING_ON_MOBILE_DATA, enabled)
    }

    fun setSamplingMode(mode: AudioSamplingMode) {
        _samplingMode.value = mode
        saveMode(mode.name)
    }

    fun setShowStatistics(enabled: Boolean) {
        _showStatistics.value = enabled
        save(SHOW_STATISTICS, enabled)
    }

    private const val FALLBACK_ENABLED = "audio_sync_fallback_enabled"
    private const val SAMPLING_ON_MOBILE_DATA = "audio_sync_sampling_on_mobile_data"
    private const val SHOW_STATISTICS = "audio_sync_show_statistics"
}
