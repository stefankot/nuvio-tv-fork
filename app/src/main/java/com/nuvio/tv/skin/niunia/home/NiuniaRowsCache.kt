package com.nuvio.tv.skin.niunia.home

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.domain.model.CatalogRow
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.model.MetaPreview
import com.nuvio.tv.domain.model.PosterShape
import com.nuvio.tv.ui.screens.home.HomeUiState
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

/**
 * Pamięć rzędów Głównej (właściciel 25.11: „dodaj cache” — dodatek katalogów odpowiadał 429 „za dużo zapytań” i rzędy
 * znikały). Udane rzędy zapisywane na dysku w tle (3 s po wczytaniu, tylko gdy coś się zmieniło); gdy po wczytaniu rzędu
 * z dodatku brakuje (błąd, 429, brak sieci), na jego miejsce wchodzi ostatnia zapisana kopia. Po restarcie tylko odczyt.
 * Wyłączone (klucz [KEY]) = stan bez zmian, nic nie zapisuje.
 */
internal object NiuniaRowsCache {
    const val KEY = "niunia_rows_cache"
    private const val TAG = "ForkNiunia"
    private const val FILE = "niunia_rows_cache.json"
    private const val MAX_ROWS = 30
    private const val MAX_ITEMS = 40

    private fun key(r: CatalogRow) = "${r.addonId}|${r.catalogId}|${r.rawType}"

    @Composable
    fun withCache(state: HomeUiState): HomeUiState {
        val ctx = LocalContext.current
        val on = remember { ForkFeatures.enabled(ctx, KEY, true) }
        if (!on) return state
        var cached by remember { mutableStateOf<List<CatalogRow>>(emptyList()) }
        LaunchedEffect(Unit) { cached = load(ctx) }
        LaunchedEffect(state.isLoading, state.catalogRows) {
            if (state.isLoading || state.catalogRows.none { it.items.isNotEmpty() }) return@LaunchedEffect
            delay(3_000)
            save(ctx, state.catalogRows, cached)?.let { cached = it }
        }
        return remember(state, cached) { merge(state, cached) }
    }

    /** Brakujące (lub puste) rzędy z dodatków → zapisane kopie; w czasie wczytywania stan bez zmian. */
    private fun merge(state: HomeUiState, cached: List<CatalogRow>): HomeUiState {
        if (cached.isEmpty() || state.isLoading || state.catalogRows.any { it.isLoading }) return state
        val present = state.catalogRows.filter { it.items.isNotEmpty() }.map(::key).toSet()
        val missing = cached.filter { key(it) !in present }
        if (missing.isEmpty()) return state
        Log.i(TAG, "ekran główny: ${missing.size} rzędów z pamięci (dodatek nie odpowiedział): ${missing.joinToString { it.catalogName }}")
        return state.copy(catalogRows = state.catalogRows.filter { it.items.isNotEmpty() } + missing)
    }

    private fun file(c: Context) = File(c.filesDir, FILE)

    /** Bieżące rzędy + wcześniej zapisane, których teraz brak; zapis tylko przy zmianie. Zwraca nową listę albo null. */
    private suspend fun save(c: Context, rows: List<CatalogRow>, old: List<CatalogRow>): List<CatalogRow>? = withContext(Dispatchers.IO) {
        val now = rows.filter { it.items.isNotEmpty() }.map { it.copy(items = it.items.take(MAX_ITEMS)) }
        val keys = now.map(::key).toSet()
        val union = (now + old.filter { key(it) !in keys }).take(MAX_ROWS)
        val sig = { l: List<CatalogRow> -> l.map { key(it) + ":" + it.items.joinToString(",") { m -> m.id } } }
        if (sig(union) == sig(old)) return@withContext null
        try {
            val json = JSONArray().apply { union.forEach { put(rowJson(it)) } }.toString()
            val f = file(c); val tmp = File(f.path + ".tmp")
            tmp.writeText(json)
            if (!tmp.renameTo(f)) { Log.w(TAG, "pamięć rzędów: nie udało się podmienić pliku"); return@withContext null }
            Log.i(TAG, "pamięć rzędów: zapisano ${union.size} rzędów (${json.length / 1024} KB)")
            union
        } catch (e: IOException) { Log.w(TAG, "pamięć rzędów: zapis: ${e.javaClass.simpleName}: ${e.message}"); null }
        catch (e: JSONException) { Log.w(TAG, "pamięć rzędów: zapis: ${e.javaClass.simpleName}: ${e.message}"); null }
    }

    private suspend fun load(c: Context): List<CatalogRow> = withContext(Dispatchers.IO) {
        val f = file(c)
        if (!f.isFile) { Log.i(TAG, "pamięć rzędów: brak zapisu"); return@withContext emptyList() }
        try {
            val arr = JSONArray(f.readText())
            val out = (0 until arr.length()).mapNotNull { row(arr.getJSONObject(it)) }
            Log.i(TAG, "pamięć rzędów: wczytano ${out.size} rzędów")
            out
        } catch (e: IOException) { Log.w(TAG, "pamięć rzędów: odczyt: ${e.javaClass.simpleName}: ${e.message}"); emptyList() }
        catch (e: JSONException) { Log.w(TAG, "pamięć rzędów: odczyt: ${e.javaClass.simpleName}: ${e.message}"); emptyList() }
    }

    private fun rowJson(r: CatalogRow) = JSONObject()
        .put("addonId", r.addonId).put("addonName", r.addonName).put("addonBaseUrl", r.addonBaseUrl)
        .put("catalogId", r.catalogId).put("catalogName", r.catalogName).put("type", r.type.name).put("rawType", r.rawType)
        .put("items", JSONArray().apply { r.items.forEach { put(itemJson(it)) } })

    private fun itemJson(m: MetaPreview) = JSONObject()
        .put("id", m.id).put("type", m.type.name).put("rawType", m.rawType).put("name", m.name)
        .put("poster", m.poster).put("posterShape", m.posterShape.name).put("background", m.background).put("logo", m.logo)
        .put("description", m.description).put("releaseInfo", m.releaseInfo).put("imdbRating", m.imdbRating?.toDouble())
        .put("genres", JSONArray(m.genres)).put("runtime", m.runtime).put("imdbId", m.imdbId)
        .put("landscapePoster", m.landscapePoster).put("sourceAddonBaseUrl", m.sourceAddonBaseUrl)

    private fun JSONObject.str(k: String): String? = if (isNull(k) || !has(k)) null else optString(k).takeIf { it.isNotEmpty() }

    private inline fun <reified E : Enum<E>> enumOf(name: String?, what: String): E? = try {
        name?.let { enumValueOf<E>(it) }
    } catch (e: IllegalArgumentException) { Log.w(TAG, "pamięć rzędów: $what „$name”: ${e.javaClass.simpleName}: ${e.message}"); null }

    private fun row(o: JSONObject): CatalogRow? {
        val type = enumOf<ContentType>(o.str("type"), "rodzaj rzędu") ?: return null
        val items = o.optJSONArray("items") ?: return null
        val metas = (0 until items.length()).mapNotNull { item(items.getJSONObject(it)) }
        if (metas.isEmpty()) return null
        return CatalogRow(
            addonId = o.optString("addonId"), addonName = o.optString("addonName"), addonBaseUrl = o.optString("addonBaseUrl"),
            catalogId = o.optString("catalogId"), catalogName = o.optString("catalogName"), type = type,
            rawType = o.str("rawType") ?: type.toApiString(), items = metas, hasMore = false,
        )
    }

    private fun item(o: JSONObject): MetaPreview? {
        val type = enumOf<ContentType>(o.str("type"), "rodzaj") ?: return null
        val id = o.str("id") ?: return null
        val genres = o.optJSONArray("genres")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
        return MetaPreview(
            id = id, type = type, rawType = o.str("rawType") ?: type.toApiString(), name = o.optString("name"),
            poster = o.str("poster"), posterShape = enumOf<PosterShape>(o.str("posterShape"), "kształt") ?: PosterShape.values().first(),
            background = o.str("background"), logo = o.str("logo"), description = o.str("description"), releaseInfo = o.str("releaseInfo"),
            imdbRating = if (o.isNull("imdbRating") || !o.has("imdbRating")) null else o.optDouble("imdbRating").toFloat(),
            genres = genres, runtime = o.str("runtime"), imdbId = o.str("imdbId"), landscapePoster = o.str("landscapePoster"),
            sourceAddonBaseUrl = o.str("sourceAddonBaseUrl"),
        )
    }
}
