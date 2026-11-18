package com.nuvio.tv.fork.sources

import androidx.media3.common.MimeTypes
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Kicia (hook v7.4, właściciel 13.11): pliki AVI/WMV/ASF grają w mpv — Media3 czyta tylko proste AVI i nie czyta WMV.
 * Tylko ten plik (bez trwałej zmiany silnika). Kopia `forgeNeedsMpvContainer` z Kicii 34c80c6; wpięcie `Fork hook: kicia-mpv`
 * w PlayerRuntimeControllerInitialization. Przełącznik [KEY] (domyślnie włączony).
 */
object MpvContainers {
    const val KEY = "kicia_mpv_containers"
    @Volatile var on = true

    fun warm(c: android.content.Context) { on = ForkFeatures.enabled(c, KEY, true) }

    fun needsMpv(url: String, filename: String?, mimeType: String? = null): Boolean {
        if (!on) return false
        if (mimeType == MimeTypes.VIDEO_AVI || mimeType?.contains("ms-wmv") == true || mimeType?.contains("ms-asf") == true) return true
        val name = Regex("(?i)\\.(avi|wmv|asf)$")
        return name.containsMatchIn(filename.orEmpty().trim()) || name.containsMatchIn(url.substringBefore('?').substringBefore('#'))
    }
}
