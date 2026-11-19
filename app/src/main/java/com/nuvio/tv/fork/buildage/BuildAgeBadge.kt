package com.nuvio.tv.fork.buildage

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val TAG = "ForkBuildAge"
private const val ASSET = "fork/build_time.txt" // zapisuje fork-tools/build.sh (sekundy od 1970) przy każdym buildzie

/**
 * Dyskretny napis w prawym dolnym rogu: „built-1h” = ile minęło od kompilacji tego builda (tylko build debug).
 * Właściciel 20.11: żeby na TV od razu było widać, czy działa świeży build. Wyłącznik: ForkFeatures `fork_build_age`.
 */
@Composable
fun BuildAgeBadge() {
    val c = LocalContext.current
    if (!com.nuvio.tv.BuildConfig.DEBUG) return
    val enabled = remember(c) { com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, "fork_build_age", default = true) }
    if (!enabled) return
    val builtMs = remember(c) { readBuildTime(c) } ?: return
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); now = System.currentTimeMillis() } }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Text("built-" + age(now - builtMs), color = Color(0x59FFFFFF), fontSize = 10.sp, modifier = Modifier.padding(end = 8.dp, bottom = 4.dp))
    }
}

private fun readBuildTime(c: Context): Long? = try {
    c.assets.open(ASSET).bufferedReader().use { it.readText().trim().toLong() * 1000L }.also { Log.i(TAG, "czas kompilacji: $it") }
} catch (e: Exception) {
    Log.w(TAG, "brak czasu kompilacji ($ASSET): ${e.javaClass.simpleName}: ${e.message}"); null
}

internal fun age(ms: Long): String {
    val m = (ms / 60_000L).coerceAtLeast(0)
    return when {
        m < 60 -> "${m}m"
        m < 48 * 60 -> "${m / 60}h"
        else -> "${m / (24 * 60)}d"
    }
}
