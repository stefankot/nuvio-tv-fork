package com.nuvio.tv.skin.niunia.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.wrapContentHeight
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface NiuniaClockEntryPoint {
    fun playerSettings(): com.nuvio.tv.data.local.PlayerSettingsDataStore
}

/** Ostatnia znana wartość ustawienia (start ekranu bez mignięcia zegara). */
private object NiuniaClockState { @Volatile var last: Boolean = true }

/**
 * Z-15 (P-7): natywne ustawienie Nuvio „Zegar OSD” (Ustawienia → Odtwarzanie). Włączone = godzina na każdym ekranie Niuni
 * i w każdym odtwarzaczu; wyłączone = brak elementu. Zmiana działa bez restartu (Flow ustawień profilu).
 */
@Composable
fun niuniaClockEnabled(): Boolean {
    val app = LocalContext.current.applicationContext
    val flow = remember(app) {
        EntryPointAccessors.fromApplication(app, NiuniaClockEntryPoint::class.java).playerSettings().playerSettings
            .map { it.osdClockEnabled }
            .distinctUntilChanged()
            .catch { e -> Log.w(DS_TAG, "zegar: odczyt ustawienia: ${e.javaClass.simpleName}: ${e.message}"); emit(NiuniaClockState.last) }
    }
    val on by flow.collectAsState(initial = NiuniaClockState.last)
    LaunchedEffect(on) {
        if (NiuniaClockState.last != on) Log.i(DS_TAG, "zegar: ${if (on) "włączony" else "wyłączony"} (ustawienie Nuvio „Zegar OSD”)")
        NiuniaClockState.last = on
    }
    return on
}

/** Godzina 24 h „HH:MM”. */
internal fun niuniaClockText(ms: Long = System.currentTimeMillis()): String {
    val c = java.util.Calendar.getInstance().apply { timeInMillis = ms }
    return "%02d:%02d".format(c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE))
}

/** NNavigation `top-actions` › clock: tekst `type-label`, cyfry tabelaryczne; nie bierze fokusu. Odświeżany co pełną minutę. */
@Composable
fun NiuniaClock(modifier: Modifier = Modifier) {
    if (!niuniaClockEnabled()) return
    val text by produceState(niuniaClockText()) {
        while (true) {
            value = niuniaClockText()
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
        }
    }
    Box(modifier.height(NiuniaSize.H36), contentAlignment = Alignment.Center) {
        Text(text, style = NiuniaType.Label.copy(fontFeatureSettings = "tnum"),
            modifier = Modifier.wrapContentHeight().semantics { contentDescription = "Godzina $text" })
    }
}

/** Zegar sam w prawym górnym rogu (prawa krawędź 40 dp, y 24) — ekrany bez paska akcji: szczegóły, osoba, źródła, odtwarzacze. */
@Composable
fun NiuniaClockCorner(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        NiuniaClock(Modifier.padding(top = NiuniaSize.NavTop, end = 40.dp))
    }
}
