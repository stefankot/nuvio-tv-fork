package com.nuvio.tv.skin.kicia

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Settings that exist only in this fork (Settings → Nuvio Forge), kept in the app's preferences. */
object ForgePrefs {
    private const val FILE = "forge_prefs"
    private var prefs: SharedPreferences? = null

    private val _tvBrowseB = MutableStateFlow(false)
    /** Live TV opens on "Przeglądaj" (mockup variant B) instead of the guide with recent programmes (A, default). */
    val tvBrowseB: StateFlow<Boolean> = _tvBrowseB.asStateFlow()

    private val _homeYouTube = MutableStateFlow(true)
    /** Home shows the "YouTube dla Ciebie" shelf. */
    val homeYouTube: StateFlow<Boolean> = _homeYouTube.asStateFlow()

    private val _reminderCard = MutableStateFlow(true)
    /** The card that announces a reminded programme over the Live TV picture. */
    val reminderCard: StateFlow<Boolean> = _reminderCard.asStateFlow()

    private val _logoMode = MutableStateFlow(0)
    /** Channel logos: 0 = guide's (EPG.OVH) then playlist's, 1 = guide's only, 2 = playlist's only, 3 = own address pattern. */
    val logoMode: StateFlow<Int> = _logoMode.asStateFlow()

    private val _logoPattern = MutableStateFlow(DEFAULT_LOGO_PATTERN)
    /** Own logo address; {name} becomes the channel's guide name (spaces as +), e.g. https://epg.ovh/logo/TVP+1.png. */
    val logoPattern: StateFlow<String> = _logoPattern.asStateFlow()

    @Volatile private var appContext: Context? = null

    fun ensure(context: Context) {
        if (prefs != null) return
        appContext = context.applicationContext
        com.nuvio.tv.skin.kicia.livetv.ForgeLogoPack.ensure(context)
        com.nuvio.tv.skin.kicia.livetv.ForgeEpgCategories.ensure(context) // owner 13.11: programme categories kept between starts
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs = p
        _tvBrowseB.value = p.getBoolean("tv_browse_b", false)
        _homeYouTube.value = p.getBoolean("home_youtube", ForgeUi.HOME_YOUTUBE)
        _reminderCard.value = p.getBoolean("reminder_card", true)
        _logoMode.value = p.getInt("logo_mode", 0)
        _logoPattern.value = p.getString("logo_pattern", DEFAULT_LOGO_PATTERN) ?: DEFAULT_LOGO_PATTERN
        // A ZIP package set earlier (before it was unpacked) is fetched now.
        if (com.nuvio.tv.skin.kicia.livetv.ForgeLogoPack.isZip(_logoPattern.value) && com.nuvio.tv.skin.kicia.livetv.ForgeLogoPack.size == 0) {
            com.nuvio.tv.skin.kicia.livetv.ForgeLogoPack.install(context, _logoPattern.value)
        }
    }

    fun setTvBrowseB(value: Boolean) { _tvBrowseB.value = value; prefs?.edit()?.putBoolean("tv_browse_b", value)?.apply() }
    fun setHomeYouTube(value: Boolean) { _homeYouTube.value = value; prefs?.edit()?.putBoolean("home_youtube", value)?.apply() }
    fun setLogoMode(value: Int) { _logoMode.value = value; prefs?.edit()?.putInt("logo_mode", value)?.apply() }
    fun setLogoPattern(value: String) {
        val v = value.trim().ifEmpty { DEFAULT_LOGO_PATTERN }; _logoPattern.value = v; prefs?.edit()?.putString("logo_pattern", v)?.apply()
        // A ZIP package (e.g. https://epg.ovh/logo.zip) is downloaded and unpacked; the channels then find their logos in it.
        if (com.nuvio.tv.skin.kicia.livetv.ForgeLogoPack.isZip(v)) appContext?.let { com.nuvio.tv.skin.kicia.livetv.ForgeLogoPack.install(it, v) }
    }
    const val DEFAULT_LOGO_PATTERN = "https://epg.ovh/logo/{name}.png"
    fun setReminderCard(value: Boolean) { _reminderCard.value = value; prefs?.edit()?.putBoolean("reminder_card", value)?.apply() }
}

@Composable
private fun ensureForgePrefs() {
    val context = LocalContext.current
    remember { ForgePrefs.ensure(context) }
}

/** Live TV layout: true = Przeglądaj first (variant B), false = guide with recent programmes (variant A). */
@Composable
fun rememberForgeTvBrowseB(): Boolean {
    ensureForgePrefs()
    val value by ForgePrefs.tvBrowseB.collectAsState()
    return value
}

@Composable
fun rememberForgeHomeYouTube(): Boolean {
    ensureForgePrefs()
    val value by ForgePrefs.homeYouTube.collectAsState()
    return value
}

@Composable
fun rememberForgeReminderCard(): Boolean {
    ensureForgePrefs()
    val value by ForgePrefs.reminderCard.collectAsState()
    return value
}

@Composable
fun rememberForgeLogoMode(): Int {
    ensureForgePrefs()
    val value by ForgePrefs.logoMode.collectAsState()
    return value
}

@Composable
fun rememberForgeLogoPattern(): String {
    ensureForgePrefs()
    val value by ForgePrefs.logoPattern.collectAsState()
    return value
}
