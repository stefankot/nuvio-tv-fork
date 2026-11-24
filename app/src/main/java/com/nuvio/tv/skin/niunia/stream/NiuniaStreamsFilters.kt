package com.nuvio.tv.skin.niunia.stream

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaPanelOverlay
import com.nuvio.tv.skin.niunia.components.NiuniaRowKind
import com.nuvio.tv.skin.niunia.components.NiuniaTrailing
import com.nuvio.tv.skin.niunia.components.niuniaTrapArrows

private const val TAG = "ForkNiunia"

/**
 * Z-14 (makieta StreamsFilters): panel filtrów źródeł — sekcje Rozdzielczość, Dźwięk, HDR, Kodek, Dodatek; zaznaczenie `check`,
 * przyciski „Zapisz jako filtr” (filtr zostaje na tym TV dla następnych tytułów) i „Wyczyść”. W sekcji wybory łączy „lub”,
 * między sekcjami „i”. Rozpoznawanie z tekstu źródła (nazwa, tytuł, opis, plik), jak szybkie filtry.
 */
internal object NiuniaStreamFilters {
    private const val PREFS = "niunia_stream_filters"
    data class Option(val section: Int, val key: String, val label: String, val regex: Regex?)

    val SECTIONS = listOf(R.string.niunia_tv2_f_resolution, R.string.niunia_tv2_f_audio, R.string.niunia_tv2_f_hdr, R.string.niunia_tv2_f_codec, R.string.niunia_tv2_f_addon)
    val OPTIONS = listOf(
        Option(0, "2160", "4K", Regex("""2160p|\b4k\b|\buhd\b""")),
        Option(0, "1080", "1080p", Regex("""1080p""")),
        Option(0, "720", "720p", Regex("""720p""")),
        Option(1, "atmos", "Dolby Atmos", Regex("""atmos""")),
        Option(1, "dts", "DTS", Regex("""\bdts""")),
        Option(1, "51", "5.1", Regex("""5[ .]1|ddp?5|eac3""")),
        Option(2, "dv", "Dolby Vision", Regex("""dolby ?vision|\bdovi\b|\bdv\b""")),
        Option(2, "hdr10", "HDR10", Regex("""hdr10|\bhdr\b""")),
        Option(3, "hevc", "HEVC", Regex("""hevc|x265|h\.?265""")),
        Option(3, "av1", "AV1", Regex("""\bav1\b""")),
        Option(3, "avc", "H.264", Regex("""x264|h\.?264|\bavc\b""")),
    )

    fun saved(c: Context): Set<String> = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getStringSet("on", emptySet()) ?: emptySet()

    fun save(c: Context, on: Set<String>) {
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet("on", on).apply()
        Log.i(TAG, "źródła: filtr zapisany (${on.size} wyborów)")
    }

    /** [on]: klucze opcji i „addon:<nazwa>”. */
    fun matches(s: Stream, on: Set<String>): Boolean {
        if (on.isEmpty()) return true
        val text = niuniaStreamText(s)
        for (sec in 0..3) {
            val chosen = OPTIONS.filter { it.section == sec && it.key in on }
            if (chosen.isNotEmpty() && chosen.none { it.regex!!.containsMatchIn(text) }) return false
        }
        val addons = on.filter { it.startsWith("addon:") }.map { it.removePrefix("addon:") }
        return addons.isEmpty() || s.addonName in addons
    }
}

@Composable
internal fun NiuniaStreamsFiltersPanel(addons: List<String>, on: Set<String>, onChange: (Set<String>) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    BackHandler { Log.i(TAG, "źródła: panel filtrów zamknięty (Wstecz)"); onClose() }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60)
        try { first.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "źródła: fokus panelu filtrów: ${e.javaClass.simpleName}: ${e.message}") }
    }
    fun toggle(k: String) { val n = if (k in on) on - k else on + k; Log.i(TAG, "źródła: filtr $k → ${k in n}"); onChange(n) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        NiuniaPanelOverlay(stringResource(R.string.niunia_tv2_f_title),
            Modifier.padding(top = 40.dp).niuniaTrapArrows("źródła: panel filtrów", setOf(FocusDirection.Left, FocusDirection.Right, FocusDirection.Up, FocusDirection.Down)).focusGroup()) {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                NiuniaStreamFilters.SECTIONS.forEachIndexed { sec, title ->
                    val opts = if (sec == 4) addons.map { NiuniaStreamFilters.Option(4, "addon:$it", it, null) } else NiuniaStreamFilters.OPTIONS.filter { it.section == sec }
                    if (opts.isEmpty()) return@forEachIndexed
                    item(key = "h$sec") { NiuniaListRow(ctx.getString(title), onClick = null, kind = NiuniaRowKind.Header) }
                    items(opts, key = { it.key }) { o ->
                        NiuniaListRow(o.label, kind = NiuniaRowKind.Track, trailing = if (o.key in on) NiuniaTrailing.Check else NiuniaTrailing.None,
                            focusRequester = if (o.key == NiuniaStreamFilters.OPTIONS.first().key) first else null, onClick = { toggle(o.key) })
                    }
                }
            }
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NiuniaButton(onClick = { NiuniaStreamFilters.save(ctx, on); onClose() }, label = stringResource(R.string.niunia_tv2_f_save), small = true)
                NiuniaButton(onClick = { Log.i(TAG, "źródła: filtry wyczyszczone"); onChange(emptySet()); NiuniaStreamFilters.save(ctx, emptySet()) }, label = stringResource(R.string.niunia_tv2_f_clear), small = true)
            }
        }
    }
}
