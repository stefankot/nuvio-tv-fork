package com.nuvio.tv.fork.arc

import android.app.Activity
import android.view.KeyEvent
import com.nuvio.tv.core.player.PlayerSessionTracker
import com.nuvio.tv.data.local.RemoteButtonJob
import com.nuvio.tv.data.local.RemoteButtonStore
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Pilot Arc w całej aplikacji. W odtwarzaczu (także Live TV) klawisze obsługuje PlayerScreen,
 * więc tu zwracamy false, gdy sesja odtwarzacza jest otwarta. Poza odtwarzaczem działa tylko to,
 * co ma oczywiste znaczenie w całej aplikacji: przycisk przypisany do "Nic" jest wyciszony.
 * Pozostałe zadania (np. napisy, źródła) nie mają poza odtwarzaczem odpowiednika i przepuszczamy je.
 */
object RemoteGlobalHook {
    private const val KEY = "fork_arc_remote"
    @Volatile private var playerOpen = false
    @Volatile private var started = false

    private fun start() {
        if (started) return
        started = true
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            PlayerSessionTracker.isSessionOpen.collect { playerOpen = it }
        }
    }

    fun onKey(activity: Activity, event: KeyEvent): Boolean {
        if (!ForkFeatures.enabled(activity, KEY, default = true)) return false
        start()
        if (playerOpen) return false
        RemoteButtonStore.ensureLoaded(activity)
        val jobs = RemoteButtonStore.map.value.takeOver(event.keyCode, controlsHidden = true) ?: return false
        if (jobs.hold != null) return false
        return jobs.press?.job == RemoteButtonJob.NOTHING
    }
}
