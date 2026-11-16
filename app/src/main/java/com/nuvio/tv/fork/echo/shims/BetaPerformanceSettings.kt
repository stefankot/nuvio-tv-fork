package com.nuvio.tv.data.local

/** Echo shim: only the properties read by the copied Echo files (extended per phase). */
enum class NotificationStyle(val key: String) {
    ISLAND("island"), PROFILE("profile"), OFF("off");

    companion object {
        fun fromKey(key: String?): NotificationStyle = entries.firstOrNull { it.key == key } ?: ISLAND
    }
}

data class BetaPerformanceSettings(
    val notificationStyle: NotificationStyle = NotificationStyle.ISLAND
)
