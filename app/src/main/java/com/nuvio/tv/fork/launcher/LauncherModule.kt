@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.fork.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.composable
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.nuvio.tv.fork.registry.ForkModule
import com.nuvio.tv.fork.registry.ModuleAction
import com.nuvio.tv.fork.registry.ModuleRoute
import com.nuvio.tv.fork.registry.ModuleSetting
import com.nuvio.tv.fork.registry.SettingsLevel
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/** Ekran główny telewizora i blokada wyjścia (B06). */
object LauncherModule : ForkModule {
    override val id = "launcher"
    override val title = "Ekran główny i blokada wyjścia"
    override val defaultEnabled = true

    const val APPS_ROUTE = "fork_apps"
    private const val K_LOCK = "launcher_lock"; private const val K_START = "launcher_start_page"

    override fun settings() = listOf(
        ModuleSetting("launcher_autostart", SettingsLevel.SIMPLE, "Telewizja", "Uruchamiaj z telewizorem", "Nuvio startuje po włączeniu telewizora", defaultValue = false),
        ModuleSetting(K_LOCK, SettingsLevel.ESSENTIAL, "Telewizja", "Blokada wyjścia", "Pięć razy Wstecz w 3 sekundy otwiera „Wyjście i ustawienia”", defaultValue = false),
    )

    override fun actions() = listOf(
        ModuleAction("Po uruchomieniu pokaż: ekran główny", SettingsLevel.SIMPLE) { c -> ForkFeatures.setStr(c, K_START, StartPage.HOME.name) },
        ModuleAction("Po uruchomieniu pokaż: telewizję", SettingsLevel.SIMPLE) { c -> ForkFeatures.setStr(c, K_START, StartPage.TV.name) },
        ModuleAction("Po uruchomieniu pokaż: ostatni kanał", SettingsLevel.SIMPLE) { c -> ForkFeatures.setStr(c, K_START, StartPage.LAST_CHANNEL.name) },
        ModuleAction("Nuvio jako ekran główny", SettingsLevel.ESSENTIAL, "Otwiera systemowy wybór ekranu głównego") { c ->
            LauncherOptions.setHome(c, !LauncherOptions.homeEnabled(c))
        },
    )

    override fun routes() = listOf(ModuleRoute(APPS_ROUTE) { composable(APPS_ROUTE) { AppsScreen() } })

    override fun rootOverlays(): List<@Composable () -> Unit> = listOf({ Controller() })

    @Composable
    private fun Controller() {
        val c = LocalContext.current
        var exitOpen by remember { mutableStateOf(false) }
        val counter = remember { BackPressCounter() }
        LaunchedEffect(Unit) {
            LauncherOptions.setAutostart(c, ForkFeatures.enabled(c, "launcher_autostart", false))
            // Trasa startowa: tylko raz na proces.
            if (!started) {
                started = true
                val page = runCatching { StartPage.valueOf(ForkFeatures.str(c, K_START, "HOME")) }.getOrDefault(StartPage.HOME)
                val last = com.nuvio.tv.fork.tv.TvStore.recentChannels.value.firstOrNull()?.streamUrl
                when (StartRoute.resolve(page, last, liveTvAvailable = true)) {
                    "tv_channel" -> { com.nuvio.tv.fork.tv.ekrany.PendingPlay.ask(last!!); ForkFeatures.open?.invoke(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE) }
                    "tv" -> ForkFeatures.open?.invoke(com.nuvio.tv.reshaped.livetv.LIVE_TV_ROUTE)
                }
            }
        }
        val lockOn = ForkFeatures.enabled(c, K_LOCK, false)
        BackHandler(enabled = lockOn && !exitOpen) {
            if (ExitPolicy.onHomeBack(true, counter, System.currentTimeMillis(), hasPin = false) != ExitDecision.NOTHING) exitOpen = true
        }
        if (exitOpen) ExitDialog(onDismiss = { exitOpen = false })
    }

    @Volatile private var started = false

    @Composable
    private fun ExitDialog(onDismiss: () -> Unit) {
        val c = LocalContext.current
        Dialog(onDismissRequest = onDismiss) {
            Surface(Modifier.width(480.dp)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Wyjście i ustawienia", style = MaterialTheme.typography.headlineSmall)
                    Button(onClick = { (c as? Activity)?.finishAffinity() }) { Text("Zamknij aplikację") }
                    Button(onClick = { onDismiss(); com.nuvio.tv.ui.screens.settings.ForkFeatures.open?.invoke(APPS_ROUTE) }) { Text("Aplikacje") }
                    Button(onClick = { onDismiss(); LauncherOptions.openSystemSettings(c) }) { Text("Ustawienia systemu") }
                }
            }
        }
    }

    @Composable
    private fun AppsScreen() {
        val c = LocalContext.current
        val apps = remember { listApps(c) }
        LazyColumn(Modifier.fillMaxSize().padding(48.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Aplikacje", style = MaterialTheme.typography.headlineMedium) }
            item { Button(onClick = { LauncherOptions.openSystemSettings(c) }) { Text("Ustawienia systemu") } }
            items(apps, key = { it.first.packageName }) { (entry, intent) ->
                Button(onClick = { runCatching { c.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }) { Text(entry.label) }
            }
        }
    }

    private fun listApps(c: Context): List<Pair<AppEntry, Intent>> {
        val pm = c.packageManager
        val infos = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
        val entries = infos.map { AppEntry(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
        val sorted = AppList.filterSort(entries, c.packageName)
        return sorted.mapNotNull { e -> pm.getLeanbackLaunchIntentForPackage(e.packageName)?.let { e to it } }
    }
}
