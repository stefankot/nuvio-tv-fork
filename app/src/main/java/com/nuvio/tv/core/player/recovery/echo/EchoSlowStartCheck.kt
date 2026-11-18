// Pochodzenie: Laking22/Nuvio-Echo @ e952419, app/src/main/java/com/nuvio/tv/ui/screens/player/PlayerRuntimeControllerAutoPlayHedge.kt
// Kopia wprost (M3c, K2 kontrola wolnego startu). Zmiany oznaczone „// KICIA:”. Rejestr: docs/agent/auto-odtwarzanie/ECHO-PORT.md
// KICIA: pakiet echo (osobny katalog na kod Echo)
package com.nuvio.tv.core.player.recovery.echo

import android.os.SystemClock
import android.util.Log
// KICIA: usunięte importy R i flow.update — komunikat i błąd pokazuje nasz ratunek (failAttempt)
import com.nuvio.tv.core.player.recovery.port.EchoStartHost
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// KICIA: TAG zamiast PlayerRuntimeController.TAG (pakiet echo nie importuje odtwarzacza)
private const val TAG = "AutoPlayRace"

/** How long a remembered link has to show its first frame before it's checked. */
private const val SLOW_START_CHECK_MS = 1_500L
// KICIA: usunięte SLOW_PICK_GIVE_UP_MS (6 s) — za ostre dla dużych plików; dalej czeka czas próby K3 (StartupSourceRecovery)

/**
 * Race matching sources, for a remembered link (Reuse last link), which plays at once with no fresh matches to race
 * against: if it hasn't shown a frame within [SLOW_START_CHECK_MS] it's checked, and one that sends no video moves on
 * to the next source at once; one still showing nothing [SLOW_PICK_GIVE_UP_MS] in moves on too, so a bad link can't
 * leave the player loading for minutes. Runs for [url], the link being started, when the source page asked for it; a
 * retry of the same link keeps it.
 */
// KICIA: obiekt z odbiorcą EchoStartHost zamiast rozszerzenia PlayerRuntimeController; zadanie zwracane wołającemu
// (autoPlayHedgeJob/autoPlayHedgeUrl trzyma nasz ratunek)
object EchoSlowStartCheck {
    fun arm(host: EchoStartHost, url: String): Job {
        // KICIA: usunięte AutoPlayRace.takeSlowStartWatch — sprawdzamy każdą próbę startu automatu, nie tylko zapamiętany link
        val headers = host.currentHeaders()
        return host.scope.launch {
            delay(SLOW_START_CHECK_MS)
            if (host.hasStarted() || host.currentUrl() != url) return@launch
            val probeStartedAt = SystemClock.elapsedRealtime() // KICIA: czas sondy do logu
            val delivers = AutoPlayRace.delivers(url, headers)
            host.probed(delivers, SystemClock.elapsedRealtime() - probeStartedAt) // KICIA: wynik sondy do logu i K3
            if (host.hasStarted() || host.currentUrl() != url) return@launch
            if (!delivers) {
                Log.w(TAG, "AUTOPLAY_RACE: the remembered link sends no video; moving on")
                host.failAttempt("NO_START") // KICIA: zamiast moveOnFromPick — następne źródło wybiera nasz ranking
                return@launch
            }
            // KICIA: usunięte czekanie do SLOW_PICK_GIVE_UP_MS i moveOnFromPick — adres dostarcza, dalej czas próby K3
        }
    }
}

// KICIA: usunięte pickStarted — EchoStartHost.hasStarted
// KICIA: usunięte moveOnFromPick — EchoStartHost.failAttempt
