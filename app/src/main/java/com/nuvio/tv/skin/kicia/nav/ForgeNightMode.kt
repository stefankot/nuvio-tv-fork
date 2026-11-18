package com.nuvio.tv.skin.kicia.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * v7 (spec 01): Tryb nocny. On = a warm layer over the whole board (blue-light filter); it never takes focus or touches.
 * The state lives in this object, so it survives a change of screen (not an app restart).
 */
object ForgeNightMode {
    var enabled by mutableStateOf(false)
    fun toggle() { enabled = !enabled; com.nuvio.tv.skin.kicia.components.ForgeToast.show(if (enabled) "Tryb nocny włączony" else "Tryb nocny wyłączony") } // v7.4: Komunikat

    /** v7.4 Harmonogram (14): presets as (from, to) in minutes of the day; index 0 = off. Saved in forge_prefs. */
    val presets: List<Pair<Int, Int>?> = listOf(null, 22 * 60 to 6 * 60, 21 * 60 to 7 * 60, 23 * 60 to 7 * 60)
    var scheduleIndex by mutableStateOf(0)
    private var loaded = false

    fun load(context: android.content.Context) {
        if (loaded) return
        loaded = true
        scheduleIndex = context.applicationContext.getSharedPreferences("forge_prefs", android.content.Context.MODE_PRIVATE).getInt("night_schedule", 0).coerceIn(0, presets.lastIndex)
    }

    fun cycleSchedule(context: android.content.Context) {
        scheduleIndex = (scheduleIndex + 1) % presets.size
        context.applicationContext.getSharedPreferences("forge_prefs", android.content.Context.MODE_PRIVATE).edit().putInt("night_schedule", scheduleIndex).apply()
    }

    fun scheduleLabel(): String = presets[scheduleIndex]?.let { (from, to) -> "%02d:%02d–%02d:%02d".format(from / 60, from % 60, to / 60, to % 60) } ?: "Wyłączony"

    fun scheduledNow(nowMinutes: Int): Boolean {
        val (from, to) = presets[scheduleIndex] ?: return false
        return if (from <= to) nowMinutes in from until to else nowMinutes >= from || nowMinutes < to
    }
}

@Composable
fun ForgeNightOverlay() {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { ForgeNightMode.load(context) }
    // The schedule is looked at once a minute.
    val minute by androidx.compose.runtime.produceState(initialValue = currentMinuteOfDay()) {
        while (true) { kotlinx.coroutines.delay(30_000L); value = currentMinuteOfDay() }
    }
    if (ForgeNightMode.enabled || ForgeNightMode.scheduledNow(minute)) Box(Modifier.fillMaxSize().background(com.nuvio.tv.skin.kicia.tokens.ForgeColors.NightTint)) // v7.4: --tryb-nocny-barwa
}

private fun currentMinuteOfDay(): Int {
    val c = java.util.Calendar.getInstance()
    return c.get(java.util.Calendar.HOUR_OF_DAY) * 60 + c.get(java.util.Calendar.MINUTE)
}
