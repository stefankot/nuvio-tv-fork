@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.nuvio.tv.skin.niunia.livetv

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nuvio.tv.R
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaButtonKind
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaPanelOverlay
import com.nuvio.tv.skin.niunia.components.NiuniaRowKind
import com.nuvio.tv.skin.niunia.components.NiuniaTrailing
import com.nuvio.tv.skin.niunia.components.niuniaTrapArrows
import com.nuvio.tv.skin.niunia.components.niuniaPanelTrap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private const val TAG = "ForkNiunia"

/**
 * Z-21 (P-25, makieta `LiveTVRegion`): chip „Kraj i język” na końcu filtrów i panel z chipami Kraj / Język.
 * Wybór zawęża listę kanałów i przewodnik (rząd kafelków, siatka, przełączanie kanałów); kategorie zostają chipami.
 * Jeden wybór naraz (kraj albo język), „Wszystkie …” zdejmuje filtr. Zapis na tym TV (SharedPreferences).
 *
 * Kraj kanału: jak przy „kraju IPTV” ([com.nuvio.tv.fork.tv.country.TvCountry.countryOf]: z nazwy, inaczej kraj listy).
 * Język kanału: z kraju (lista niżej) — playlisty nie podają języka kanału (decyzja w `plan/DECYZJE.md`, 24.11).
 * Liczenie (do 60 tys. kanałów) i filtrowanie w tle, nigdy na wątku UI.
 */
internal object NiuniaLiveRegion {
    private const val PREFS = "niunia_live_region"
    enum class Tab { COUNTRY, LANGUAGE }
    data class Choice(val tab: Tab, val code: String) { val active: Boolean get() = code.isNotEmpty() }
    data class Option(val code: String, val name: String, val count: Int)

    val choice = mutableStateOf(Choice(Tab.COUNTRY, ""))
    @Volatile private var loaded = false

    fun ensure(c: Context) {
        if (loaded) return
        val p = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        choice.value = Choice(if (p.getString("tab", "") == "language") Tab.LANGUAGE else Tab.COUNTRY, p.getString("code", "") ?: "")
        loaded = true
        if (choice.value.active) Log.i(TAG, "live tv: kraj i język z zapisu → ${label(choice.value)}")
    }

    fun set(c: Context, v: Choice) {
        choice.value = v
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("tab", if (v.tab == Tab.LANGUAGE) "language" else "country").putString("code", v.code).apply()
        Log.i(TAG, "live tv: kraj i język → ${label(v)}")
    }

    private val PL = Locale("pl")

    /** Kraj (kod jak w nazwach kanałów: pl, uk, de…) → język (ISO 639-1). */
    private val LANGUAGE_OF: Map<String, String> = (
        "pl:pl de:de at:de ch:de li:de uk:en gb:en us:en ca:en au:en ie:en nz:en fr:fr be:fr lu:fr mc:fr it:it sm:it " +
            "es:es mx:es ar:es co:es cl:es pe:es ve:es pt:pt br:pt nl:nl ua:uk ru:ru by:be cz:cs sk:sk hu:hu ro:ro md:ro bg:bg " +
            "hr:hr rs:sr ba:bs si:sl mk:mk al:sq xk:sq gr:el cy:el tr:tr se:sv no:no dk:da fi:fi is:is ee:et lv:lv lt:lt " +
            "il:he ae:ar sa:ar eg:ar ma:ar dz:ar tn:ar iq:ar sy:ar lb:ar jo:ar kw:ar qa:ar ir:fa af:ps pk:ur in:hi bd:bn " +
            "cn:zh tw:zh hk:zh jp:ja kr:ko vn:vi th:th id:id my:ms ph:tl ge:ka am:hy az:az kz:kk"
        ).split(' ').associate { it.substringBefore(':') to it.substringAfter(':') }

    fun languageOf(country: String?): String? = country?.let { LANGUAGE_OF[it] }

    fun countryName(code: String): String {
        val iso = if (code == "uk") "GB" else code.uppercase()
        return Locale("", iso).getDisplayCountry(PL).takeIf { it.isNotBlank() && it != iso }?.replaceFirstChar { it.titlecase(PL) } ?: code.uppercase()
    }

    fun languageName(code: String): String =
        Locale(code).getDisplayLanguage(PL).takeIf { it.isNotBlank() && it != code }?.replaceFirstChar { it.titlecase(PL) } ?: code.uppercase()

    fun label(v: Choice): String = when {
        !v.active -> "wszystkie"
        v.tab == Tab.COUNTRY -> "kraj ${countryName(v.code)}"
        else -> "język ${languageName(v.code)}"
    }

    /** Kraj każdego kanału listy [input] (ta sama kolejność) i dostępne kraje / języki z liczbą kanałów, od najliczniejszych. */
    class Index(val input: List<LiveTvChannel>, private val codes: Array<String?>, val countries: List<Option>, val languages: List<Option>) {
        /** null = bez filtra; pusta lista = żaden kanał nie pasuje. */
        fun filter(v: Choice): List<LiveTvChannel>? {
            if (!v.active) return null
            val out = ArrayList<LiveTvChannel>()
            for (i in input.indices) {
                val cc = codes[i]
                if (if (v.tab == Tab.COUNTRY) cc == v.code else languageOf(cc) == v.code) out += input[i]
            }
            return out
        }
    }

    fun index(input: List<LiveTvChannel>, providers: Collection<String>, listCountry: (String) -> String?): Index {
        val codes = arrayOfNulls<String>(input.size)
        val byCountry = HashMap<String, Int>()
        val byLanguage = HashMap<String, Int>()
        input.forEachIndexed { i, ch ->
            // Test JMGO tv2c: z nazw wychodziły też „GMT”, „BK” — tylko kody krajów ISO (i „uk”).
            val cc = com.nuvio.tv.fork.tv.country.TvCountry.countryOf(ch, providers, listCountry)?.takeIf { it == "uk" || it.uppercase() in ISO }
            codes[i] = cc
            if (cc != null) {
                byCountry[cc] = (byCountry[cc] ?: 0) + 1
                languageOf(cc)?.let { byLanguage[it] = (byLanguage[it] ?: 0) + 1 }
            }
        }
        // UX JMGO (ręczne testy 24.11): „Polska” była za „Stany Zjednoczone” — aplikacja jest po polsku, więc Polska i język
        // polski zawsze na górze (projektor ma ustawiony język systemu „en” bez kraju, więc nie z ustawień systemu).
        fun top(m: Map<String, Int>, name: (String) -> String) =
            m.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.key == "pl" }.thenByDescending { it.value })
                .take(MAX_OPTIONS).map { Option(it.key, name(it.key), it.value) }
        return Index(input, codes, top(byCountry, ::countryName), top(byLanguage, ::languageName))
    }

    private const val MAX_OPTIONS = 30
    private val ISO: Set<String> = Locale.getISOCountries().toHashSet()
}

/** Wynik filtra w tle: lista kanałów po wyborze kraju lub języka (bez wyboru albo bez trafień — [input] bez zmian) i indeks dla panelu. */
internal class NiuniaRegionResult(val input: List<LiveTvChannel>, val shown: List<LiveTvChannel>, val index: NiuniaLiveRegion.Index?)

@Composable
internal fun rememberNiuniaRegion(input: List<LiveTvChannel>, providers: List<String>, choice: NiuniaLiveRegion.Choice): NiuniaRegionResult {
    var result by remember { mutableStateOf(NiuniaRegionResult(input, input, null)) }
    LaunchedEffect(input, choice) {
        // Do czasu wyniku: bez filtra tylko wtedy, gdy nic nie wybrano (wybrany kraj nie miga całą listą przy zmianie listy).
        if (!choice.active && result.input !== input) result = NiuniaRegionResult(input, input, result.index?.takeIf { it.input === input })
        if (input.isEmpty()) return@LaunchedEffect
        val t0 = android.os.SystemClock.elapsedRealtime()
        result = try {
            withContext(Dispatchers.Default) {
                val idx = result.index?.takeIf { it.input === input } ?: run {
                    com.nuvio.tv.fork.tv.index.TvIndexStore.awaitLoaded()
                    val lists = com.nuvio.tv.fork.tv.guide.ExtraGuides.countryBySource
                    NiuniaLiveRegion.index(input, providers) { lists[it] }
                }
                val f = idx.filter(choice)
                if (f != null && f.isEmpty()) Log.w(TAG, "live tv: ${NiuniaLiveRegion.label(choice)} — żaden kanał nie pasuje, pokazuję wszystkie")
                NiuniaRegionResult(input, f?.ifEmpty { null } ?: input, idx)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "live tv: kraj i język: ${e.javaClass.simpleName}: ${e.message}")
            NiuniaRegionResult(input, input, null)
        }
        Log.i(TAG, "live tv: ${NiuniaLiveRegion.label(choice)}: ${result.shown.size} z ${input.size} kanałów (${android.os.SystemClock.elapsedRealtime() - t0} ms)")
    }
    return result
}

/**
 * Panel „Kraj i język” (NOverlay panel z makiety LiveTVRegion): chipy Kraj / Język, lista z „Wszystkie …” na górze i
 * zaznaczeniem `check`, podpis „Dotyczy listy kanałów i przewodnika”. OK na pozycji = wybór i zamknięcie, Wstecz = zamknięcie.
 */
@Composable
internal fun NiuniaLiveRegionPanel(index: NiuniaLiveRegion.Index?, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val cur by NiuniaLiveRegion.choice
    var tab by remember { mutableStateOf(cur.tab) }
    val countryChip = remember { FocusRequester() }
    val checkedRow = remember { FocusRequester() }
    val listState = rememberLazyListState()
    BackHandler { Log.i(TAG, "live tv: panel kraj i język zamknięty (Wstecz)"); onClose() }
    val options = if (tab == NiuniaLiveRegion.Tab.COUNTRY) index?.countries else index?.languages
    val checked = if (cur.active && cur.tab == tab) (options?.indexOfFirst { it.code == cur.code } ?: -1) + 1 else 0 // 0 = „Wszystkie …”
    LaunchedEffect(Unit) {
        delay(60)
        try { countryChip.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "live tv: fokus panelu kraj i język: ${e.javaClass.simpleName}: ${e.message}") }
    }
    // Fokus na zaznaczonej pozycji, gdy lista jest gotowa (raz, przy otwarciu); zmiana chipa zostawia fokus na chipie.
    LaunchedEffect(options != null) {
        if (options == null) return@LaunchedEffect
        try {
            listState.scrollToItem(checked.coerceAtLeast(0))
            delay(60)
            checkedRow.requestFocus()
            Log.i(TAG, "live tv: panel kraj i język, fokus → pozycja $checked")
        } catch (e: IllegalStateException) { Log.w(TAG, "live tv: fokus zaznaczonej pozycji: ${e.javaClass.simpleName}: ${e.message}") }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        NiuniaPanelOverlay(stringResource(R.string.niunia_tv1_region),
            Modifier.padding(top = 40.dp).niuniaPanelTrap("live tv: panel kraj i język")) {
            Row(Modifier.padding(start = 8.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NiuniaButton(onClick = { tab = NiuniaLiveRegion.Tab.COUNTRY; Log.i(TAG, "live tv: panel → Kraj") }, label = stringResource(R.string.niunia_tv1_country),
                    kind = NiuniaButtonKind.Chip, selected = tab == NiuniaLiveRegion.Tab.COUNTRY, focusRequester = countryChip)
                NiuniaButton(onClick = { tab = NiuniaLiveRegion.Tab.LANGUAGE; Log.i(TAG, "live tv: panel → Język") }, label = stringResource(R.string.niunia_tv1_language),
                    kind = NiuniaButtonKind.Chip, selected = tab == NiuniaLiveRegion.Tab.LANGUAGE)
            }
            if (options == null) NiuniaListRow(stringResource(R.string.niunia_tv1_region_counting), onClick = null, enabled = false)
            else {
                val all = stringResource(if (tab == NiuniaLiveRegion.Tab.COUNTRY) R.string.niunia_tv1_all_countries else R.string.niunia_tv1_all_languages)
                val rows = listOf(NiuniaLiveRegion.Option("", all, 0)) + options
                LazyColumn(Modifier.heightIn(max = 340.dp), state = listState) {
                    itemsIndexed(rows, key = { _, o -> "${tab}_${o.code}" }) { i, o ->
                        NiuniaListRow(
                            o.name,
                            onClick = { NiuniaLiveRegion.set(ctx, NiuniaLiveRegion.Choice(tab, o.code)); onClose() },
                            kind = NiuniaRowKind.Track,
                            supporting = if (o.count > 0) pluralStringResource(R.plurals.niunia_tv1_channels, o.count, o.count) else null,
                            trailing = if (i == checked) NiuniaTrailing.Check else NiuniaTrailing.None,
                            focusRequester = if (i == checked) checkedRow else null,
                        )
                    }
                }
            }
            NiuniaListRow(stringResource(R.string.niunia_tv1_region_note), onClick = null, enabled = false)
        }
    }
}
