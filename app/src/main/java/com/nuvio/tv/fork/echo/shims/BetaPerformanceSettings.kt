package com.nuvio.tv.data.local

import com.nuvio.tv.core.streams.AudioLanguage

/** Echo shim: Echo's NotificationStyle (the fork has no notification styles). */
enum class NotificationStyle(val key: String) {
    ISLAND("island"), PROFILE("profile"), OFF("off");

    companion object {
        fun fromKey(key: String?): NotificationStyle = entries.firstOrNull { it.key == key } ?: ISLAND
    }
}

/**
 * Echo shim: only the properties the copied Echo files read, same names and types as in Echo.
 * Values come from ForkFeatures (see BetaFeatures); every Echo feature is OFF by default.
 */
data class BetaPerformanceSettings(
    val raceAutoPlaySources: Boolean = false,
    val sourceFallback: Boolean = false,
    val sourceSwitchTimeoutSeconds: Int = 8,
    val autoQuality: Boolean = false,
    val autoQualityConfirmSeconds: Int = 5,
    val autoQualityLanguageLoyal: Boolean = true,
    val autoQualityHotSwap: Boolean = false,
    val playbackIntro: Boolean = false,
    val introLengthSeconds: Int = 5,
    val audioLanguageMenu: Boolean = false,
    val audioMenuLanguages: Set<String> = AudioLanguage.defaultMenuKeys,
    val showUnknownAudio: Boolean = true,
    val notificationStyle: NotificationStyle = NotificationStyle.ISLAND
)
