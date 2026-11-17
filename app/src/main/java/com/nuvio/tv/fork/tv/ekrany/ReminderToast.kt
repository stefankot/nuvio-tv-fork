@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.fork.tv.ekrany

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nuvio.tv.fork.tv.Reminders
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.delay

/** Karta przypomnienia nad obrazem (B03-8): prawy górny róg na 6 s, nie bierze fokusu. Jedno ogłoszenie na przypomnienie. */
@Composable
fun ReminderToast() {
    val context = LocalContext.current
    if (!ForkFeatures.enabled(context, "module_tv", true) || !ForkFeatures.enabled(context, "tv_reminders", true)) return
    LaunchedEffect(Unit) { Reminders.load(context.applicationContext) }
    val reminders by Reminders.list.collectAsState()
    var shown by remember { mutableStateOf<com.nuvio.tv.fork.tv.Reminder?>(null) }
    val announced = remember { HashSet<String>() }
    LaunchedEffect(reminders) {
        while (true) {
            val due = MiniGuideLogic.dueReminder(reminders, announced, System.currentTimeMillis())
            if (due != null) { announced += due.key; shown = due; delay(6_000L); shown = null }
            delay(15_000L)
        }
    }
    shown?.let { r ->
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.TopEnd) {
            Surface {
                Text("Za chwilę: ${r.title} · ${MiniGuideLogic.time(r.startMs)} · ${r.channelName}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            }
        }
    }
}
