package com.nuvio.tv.fork.tv.logo

import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.map
import com.nuvio.tv.reshaped.livetv.LiveTvRepository

/**
 * The logo of a channel for the Forge screens: the guide's own (EPG.OVH keeps one clean transparent PNG per channel at
 * epg.ovh/logo/, owner 9.11: the playlist's logos looked bad), the playlist's only when the guide has none.
 * Read when a screen composes; the guide's logos arrive with the first EPG load.
 */
fun forgeChannelLogo(
    channel: LiveTvChannel,
    guideLogos: Map<String, String> = LiveTvRepository.uiState.value.guideLogos,
): String? {
    // Owner 10.11: the playlist's logos sit in ugly frames. The EPG.OVH set (the guide's own icon, else the same
    // address built from the channel's guide name) always comes first; the playlist's only when it is chosen (mode 2).
    val guide = (guideLogos[channel.guideKey]?.takeIf { it.isNotBlank() } ?: epgOvhLogo(channel))?.replace("http://epg.ovh/", "https://epg.ovh/")
    val list = channel.logoUrl?.takeIf { it.isNotBlank() }
    // A logo package (ZIP) the owner gave, once unpacked, is the first choice for every channel it has (modes other than "playlist only").
    val pack = ForgeLogoPack.fileFor(channel)
    return when (ForkLogoPrefs.mode) {
        2 -> list ?: pack ?: guide
        3 -> pack ?: ownLogo(channel) ?: guide
        else -> pack ?: guide
    }
}

/** The channel's name as the EPG.OVH files are named (case matters there): the playlist's, without "HD"/"4K" and a "PL:" tag. */
internal fun forgeLogoName(channel: LiveTvChannel): String? = epgOvhName(channel)

private fun epgOvhName(channel: LiveTvChannel): String? =
    (channel.tvgName?.takeIf { it.isNotBlank() } ?: channel.name)
        .replace(Regex("^[A-Za-z]{2,3}\\s*[:|]\\s*"), "")
        .replace(Regex("\\s+(FHD|UHD|HD|SD|4K|HEVC|H265)$", RegexOption.IGNORE_CASE), "")
        .trim().takeIf { it.isNotEmpty() }

private fun epgOvhLogo(channel: LiveTvChannel): String? =
    epgOvhName(channel)?.let { "https://epg.ovh/logo/" + java.net.URLEncoder.encode(it, "UTF-8") + ".png" }

/** The owner's address pattern with {name} = the channel's guide name (or its own name), URL-encoded (space = +). */
private fun ownLogo(channel: LiveTvChannel): String? {
    val pattern = ForkLogoPrefs.pattern
    val name = epgOvhName(channel) ?: channel.name
    return pattern.takeIf { it.startsWith("http") && !ForgeLogoPack.isZip(it) }?.replace("{name}", java.net.URLEncoder.encode(name, "UTF-8"))
}

/**
 * Subscribes the calling screen to the guide's logos: they arrive with the EPG read, after the first composition, and the screens
 * that only receive the channel list would otherwise keep the playlist's logo until the next unrelated recomposition.
 */
@androidx.compose.runtime.Composable
internal fun rememberForgeLogoVersion(): Int {
    val flow = androidx.compose.runtime.remember { LiveTvRepository.uiState.map { it.guideLogos.size } }
    val pack = ForgeLogoPack.version.collectAsState().value
    return flow.collectAsState(initial = LiveTvRepository.uiState.value.guideLogos.size).value + pack
}

/** Tryb logo (zastępuje ForgePrefs z Kicia): 1 = paczka, potem przewodnik; 2 = z listy; 3 = własny wzór adresu. */
object ForkLogoPrefs {
    @Volatile var mode: Int = 1
    @Volatile var pattern: String = ""
    /** Wczytuje zapisane ustawienia (prefs fork_features). */
    fun load(c: android.content.Context) {
        mode = com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, "tv_logo_mode", "1").toIntOrNull() ?: 1
        pattern = com.nuvio.tv.ui.screens.settings.ForkFeatures.str(c, "tv_logo_pattern", "")
    }

    /** Własny adres logo ({name} = nazwa kanału) wprowadzony np. ze strony na telefonie; tryb 3. */
    fun setPattern(c: android.content.Context, p: String) {
        com.nuvio.tv.ui.screens.settings.ForkFeatures.setStr(c, "tv_logo_pattern", p)
        com.nuvio.tv.ui.screens.settings.ForkFeatures.setStr(c, "tv_logo_mode", "3")
        load(c)
    }

    /** Wyłącznik modułu: wyłączony = logo jak w Reshaped. */
    @Volatile var enabled: Boolean = true

    fun resolve(channel: LiveTvChannel, guideLogos: Map<String, String>): String? =
        if (enabled) forgeChannelLogo(channel, guideLogos)
        else channel.logoUrl?.takeIf(String::isNotBlank) ?: guideLogos[channel.guideKey]
}
