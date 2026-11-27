package com.nuvio.tv.skin.niunia.youtube

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nuvio.tv.R
import com.nuvio.tv.skin.niunia.components.NiuniaActionsDialog
import com.nuvio.tv.skin.niunia.components.NiuniaButton
import com.nuvio.tv.skin.niunia.components.NiuniaDialogAction
import com.nuvio.tv.skin.niunia.components.NiuniaListRow
import com.nuvio.tv.skin.niunia.components.NiuniaRowKind
import com.nuvio.tv.skin.niunia.components.NiuniaText
import com.nuvio.tv.skin.niunia.components.NiuniaTextVariant
import com.nuvio.tv.skin.niunia.components.NiuniaTone
import com.nuvio.tv.skin.niunia.search.Keyboard
import com.nuvio.tv.skin.niunia.search.SearchField
import com.nuvio.tv.skin.niunia.tokens.NiuniaColor
import com.nuvio.tv.skin.niunia.tokens.NiuniaSize
import com.nuvio.tv.skin.niunia.tokens.NiuniaSpacing
import com.nuvio.tv.youtube.YouTubeViewModel
import kotlinx.coroutines.delay

private const val TAG = "ForkNiunia"

/**
 * Przegląd ekranów 27.11, ekran 24 (właściciel: „wyszukiwarka YouTube nie działa”): Szukaj w YouTube przenosiło do
 * wspólnej wyszukiwarki filmów i seriali. Teraz osobne okno nad YouTube: pole „Szukaj w YouTube”, klawiatura,
 * podpowiedzi z YouTube, pod klawiaturą ostatnie wyszukiwania (długie OK = „Usuń z historii”). Wyniki — w siatce
 * YouTube (filmy, kanały, playlisty) przez [YouTubeViewModel.search]; Wstecz z wyników wraca do strony YouTube.
 */
@Composable
internal fun NiuniaYouTubeSearch(viewModel: YouTubeViewModel, onSearch: (String) -> Unit, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var digits by remember { mutableStateOf(false) }
    var askRemove by remember { mutableStateOf<String?>(null) }
    val firstKey = remember { FocusRequester() }
    val suggestions by viewModel.suggestions.collectAsState()
    remember { NiuniaYtRecent.load(ctx) }
    BackHandler { Log.i(TAG, "youtube szukaj: Wstecz → zamknięte"); onClose() }
    LaunchedEffect(Unit) {
        delay(60)
        try { firstKey.requestFocus() } catch (e: IllegalStateException) { Log.w(TAG, "youtube szukaj: fokus klawiatury: ${e.javaClass.simpleName}: ${e.message}") }
    }
    LaunchedEffect(query) { if (query.isNotBlank()) viewModel.suggest(query) }
    fun submit(q: String) {
        val t = q.trim()
        if (t.isEmpty()) return
        Log.i(TAG, "youtube szukaj: szukam (${t.length} znaków)")
        NiuniaYtRecent.add(ctx, t)
        onSearch(t)
    }

    Column(Modifier.fillMaxSize().background(NiuniaColor.Bg).padding(start = NiuniaSpacing.Space40, end = NiuniaSpacing.Space40, top = NiuniaSize.NavTop + NiuniaSize.H44)) {
        SearchField(query, stringResource(R.string.niunia_review_yt_search_hint))
        Row(Modifier.padding(top = NiuniaSpacing.Space12).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space40)) {
            Column(Modifier.width(NiuniaSize.SplitCol), verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space12)) {
                Keyboard(
                    digits = digits, firstKey = firstKey,
                    onMode = { digits = it },
                    onKey = { k -> query += k },
                    onSpace = { query += " " },
                    onDelete = { query = query.dropLast(1) },
                    onClear = { query = "" },
                )
                NiuniaButton(onClick = { submit(query) }, label = stringResource(R.string.niunia_ke2_yt_search), icon = "search", small = true)
                val recent = NiuniaYtRecent.items.toList()
                val phrases = if (query.isBlank()) recent.take(5) else suggestions.take(5)
                if (phrases.isNotEmpty()) Column(verticalArrangement = Arrangement.spacedBy(NiuniaSpacing.Space4)) {
                    NiuniaText(stringResource(if (query.isBlank()) R.string.niunia_search_recent else R.string.niunia_ds_search_suggestions),
                        Modifier.padding(horizontal = NiuniaSpacing.Space8), variant = NiuniaTextVariant.CaptionRegular, tone = NiuniaTone.Tertiary)
                    phrases.forEach { ph ->
                        NiuniaListRow(ph, onClick = { submit(ph) }, kind = NiuniaRowKind.Suggestion, icon = if (query.isBlank()) "history" else "search",
                            onLongClick = if (query.isBlank()) ({ askRemove = ph }) else null)
                    }
                }
            }
            Column(Modifier.padding(top = NiuniaSpacing.Space8)) {
                NiuniaText(stringResource(R.string.niunia_review_yt_search_tip), variant = NiuniaTextVariant.BodySmall, tone = NiuniaTone.Tertiary, lines = 3)
            }
        }
    }
    askRemove?.let { q ->
        NiuniaActionsDialog(
            eyebrow = stringResource(R.string.niunia_search_recent), title = q, state = null,
            actions = listOf(NiuniaDialogAction(stringResource(R.string.niunia_tv2_hist_remove)) { NiuniaYtRecent.remove(ctx, q) }),
            onDismiss = { askRemove = null },
        )
    }
}

/** Ostatnie wyszukiwania YouTube (osobne od filmów i Live TV), zapisane na dysku; najnowsze pierwsze. */
internal object NiuniaYtRecent {
    private const val PREFS = "niunia_yt_recent_searches"
    private const val KEY = "queries"
    private const val MAX = 8
    val items = mutableStateListOf<String>()
    @Volatile private var loaded = false

    fun load(c: Context) {
        if (loaded) return
        loaded = true
        try {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)?.split('\n')?.filter { it.isNotBlank() }?.let { items.addAll(it) }
        } catch (e: Exception) { Log.w(TAG, "youtube szukaj: historia odczyt: ${e.javaClass.simpleName}: ${e.message}") }
    }

    fun add(c: Context, q: String) {
        items.removeAll { it.equals(q, ignoreCase = true) }
        items.add(0, q)
        while (items.size > MAX) items.removeAt(items.lastIndex)
        save(c)
    }

    fun remove(c: Context, q: String) {
        items.removeAll { it.equals(q, ignoreCase = true) }
        Log.i(TAG, "youtube szukaj: usunięto wpis z historii (zostało ${items.size})")
        save(c)
    }

    private fun save(c: Context) {
        try { c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, items.joinToString("\n")).apply() }
        catch (e: Exception) { Log.w(TAG, "youtube szukaj: historia zapis: ${e.javaClass.simpleName}: ${e.message}") }
    }
}
