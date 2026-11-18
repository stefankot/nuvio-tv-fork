// Źródło: Laking22/Nuvio-Echo @ e952419559bdcc8438504d177ddcf494b85d3a46
// Oryginał: app/src/main/java/com/nuvio/tv/core/streams/StreamQuality.kt
// Licencja: GPL-3.0 (jak Kicia). Skopiowano: 13.11.2026.
// Zmiany Kici oznaczone „// KICIA:”. Rejestr: docs/agent/auto-odtwarzanie/ECHO-PORT.md
// KICIA: pakiet echo
package com.nuvio.tv.core.autoquality.echo

// KICIA: usunięte importy Stream i StreamDebridCacheState — używał ich tylko usunięty parser nazw
/** Resolution buckets shown in the player's quality menu, best first. */
enum class StreamQuality(val height: Int, val label: String) {
    P2160(2160, "4K"),
    P1440(1440, "1440p"),
    P1080(1080, "1080p"),
    P720(720, "720p"),
    P480(480, "480p"),
    P360(360, "360p"),
    P240(240, "240p"),
    P144(144, "144p");

    companion object {
        val menuOrder: List<StreamQuality> = entries.toList()

        /**
         * Bucket for a decoded video size. Uses the long edge as well as the short one so
         * cropped widescreen video (1920x800) still reads as 1080p; the long-edge steps sit near
         * each class's full width (1920, 1280...), so a 1720x720 scope encode stays 720p.
         */
        fun fromVideoSize(width: Int, height: Int): StreamQuality? {
            if (width <= 0 || height <= 0) return null
            val long = maxOf(width, height)
            val short = minOf(width, height)
            return when {
                long >= 3200 || short >= 2000 -> P2160
                long >= 2200 || short >= 1300 -> P1440
                long >= 1800 || short >= 1000 -> P1080
                long >= 1200 || short >= 700 -> P720
                long >= 700 || short >= 460 -> P480
                long >= 560 || short >= 340 -> P360
                long >= 380 || short >= 220 -> P240
                else -> P144
            }
        }
    }
}

// KICIA: usunięty parser nazw — poziom liczy core/autoquality/QualityLevels.kt
