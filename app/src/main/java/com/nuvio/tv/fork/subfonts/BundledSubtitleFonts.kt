package com.nuvio.tv.fork.subfonts

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.nuvio.tv.core.player.SubtitleFontStore
import com.nuvio.tv.core.player.SubtitleFonts
import com.nuvio.tv.fork.ForkLog
import com.nuvio.tv.fork.registry.ForkRegistry
import com.nuvio.tv.fork.sources.SourceChoice
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Czcionki napisów dołączone do aplikacji (24.11, właściciel: „Zainstaluj te fonty jako fonty napisów do wyboru:
 * Atkinson Hyperlegible and Tiresias. Ustaw Atkinson jako domyślny”).
 *
 * Pliki w `assets/fork/subtitle-fonts/` (źródła i licencje: `UPSTREAM_SOURCES.md`, `assets/licenses/`). Wgrywane tym samym
 * mechanizmem co czcionka dodana przez użytkownika (`core/player/SubtitleFontStore.import` z Nuvio), więc są na liście
 * „Czcionka” w wyglądzie napisów i w panelu napisów odtwarzacza, działają w ExoPlayer i mpv — bez zmian w kodzie bazy.
 * - Włączone (domyślnie): brakujące czcionki wgrane raz; czcionki usuniętej ręcznie z listy moduł nie wgrywa ponownie.
 *   Raz na profil Atkinson zostaje wybrana — chyba że profil ma już własną, dodaną przez użytkownika czcionkę.
 * - Wyłączone: czcionki wgrane przez moduł znikają z listy, profil z jedną z nich wraca do czcionki domyślnej; ponowne
 *   włączenie wgrywa je i wybiera Atkinson od nowa. Wyłączone i nic nie wgrane — od razu powrót, bez odczytów.
 * Tiresias: tylko odmiana LPfont ma polskie litery (ą, ę, ł…) — pozostałe wolne odmiany RNIB ich nie mają.
 */
object BundledSubtitleFonts {
    const val KEY = "subfonts_bundled"
    private const val TAG = "ForkSubFonts"
    private const val PREFS = "fork_features" // te same prefs co ForkFeatures
    private const val K_IDS = "subfonts_ids" // „nazwa=id” czcionek wgranych przez moduł
    private const val K_OWNED = "subfonts_owned" // id utworzone przez moduł (nie wcześniejsza kopia użytkownika)
    private const val K_REMOVED = "subfonts_removed" // id usunięte po wyłączeniu: profil z nimi wraca do domyślnej
    private const val K_CHOSEN = "subfonts_chosen_" // + id profilu: Atkinson już wybrana raz

    data class Bundled(val name: String, val asset: String)

    val ATKINSON = Bundled("Atkinson Hyperlegible", "fork/subtitle-fonts/atkinson_hyperlegible_next.ttf")
    val TIRESIAS = Bundled("Tiresias", "fork/subtitle-fonts/tiresias_lpfont.ttf")
    val ALL = listOf(ATKINSON, TIRESIAS)

    private val mutex = Mutex()

    private fun prefs(c: Context): SharedPreferences = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun moduleKey() = ForkRegistry.switchKey(SubtitleFontsModule.id)

    fun isEnabled(c: Context) =
        ForkFeatures.enabled(c, moduleKey(), SubtitleFontsModule.defaultEnabled) && ForkFeatures.enabled(c, KEY, true)

    /** ForkHooks.moduleOverlays: przy starcie, zmianie profilu i przełącznika wgrywa albo usuwa czcionki. */
    suspend fun watch(c: Context) {
        val app = c.applicationContext
        val deps = EntryPointAccessors.fromApplication(app, SourceChoice.Deps::class.java)
        val switches = callbackFlow {
            val p = prefs(app)
            val l = SharedPreferences.OnSharedPreferenceChangeListener { _, k -> if (k == KEY || k == moduleKey()) trySend(Unit) }
            p.registerOnSharedPreferenceChangeListener(l)
            trySend(Unit)
            awaitClose { p.unregisterOnSharedPreferenceChangeListener(l) }
        }
        combine(deps.profiles().activeProfileId, switches) { pid, _ -> pid }.collect { pid ->
            withContext(Dispatchers.IO) {
                try {
                    mutex.withLock { sync(app, pid) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "profil $pid: ${e.javaClass.simpleName}: ${e.message?.take(200)}")
                }
            }
        }
    }

    private suspend fun sync(app: Context, pid: Int) {
        if (!isEnabled(app)) {
            uninstall(app, pid)
            return
        }
        val store = SubtitleFontStore.get(app)
        val ids = install(app, store)
        chooseAtkinson(app, store, pid, ids)
    }

    /** Wgrywa brakujące czcionki; zwraca nazwa → id tych, które są na liście. */
    private fun install(app: Context, store: SubtitleFontStore): Map<String, String> {
        val p = prefs(app)
        val ids = readIds(p).toMutableMap()
        val owned = p.getStringSet(K_OWNED, emptySet()).orEmpty().toMutableSet()
        val listed = store.fonts.value.map { it.id }.toSet()
        var changed = false
        for (f in ALL) {
            val known = ids[f.name]
            if (known != null) {
                if (known !in listed) Log.i(TAG, "${f.name}: usunięta z listy przez użytkownika — nie wgrywam ponownie")
                continue
            }
            try {
                val bytes = app.assets.open(f.asset).use { it.readBytes() }
                val before = store.fonts.value.map { it.id }.toSet()
                val font = store.import(f.name + ".ttf", bytes)
                ids[f.name] = font.id
                if (font.id !in before) owned += font.id
                changed = true
                Log.i(TAG, "${f.name}: wgrana do czcionek napisów (${bytes.size / 1024} KB, ${font.id})" +
                    if (font.id in before) ", była już na liście" else "")
            } catch (e: Exception) {
                Log.w(TAG, "${f.name}: nie wgrana: ${e.javaClass.simpleName}: ${e.message?.take(200)}")
                ForkLog.say(app, TAG, "Nie udało się dodać czcionki napisów ${f.name}")
            }
        }
        if (changed) p.edit().putString(K_IDS, ids.entries.joinToString(";") { "${it.key}=${it.value}" }).putStringSet(K_OWNED, owned).apply()
        if (p.contains(K_REMOVED)) p.edit().remove(K_REMOVED).apply()
        val now = store.fonts.value.map { it.id }.toSet()
        return ids.filterValues { it in now }
    }

    /** Raz na profil: Atkinson jako czcionka napisów, chyba że profil ma własną czcionkę dodaną przez użytkownika. */
    private suspend fun chooseAtkinson(app: Context, store: SubtitleFontStore, pid: Int, ids: Map<String, String>) {
        val p = prefs(app)
        val flag = K_CHOSEN + pid
        val atkinson = ids[ATKINSON.name] ?: return
        if (p.getBoolean(flag, false)) return
        val deps = EntryPointAccessors.fromApplication(app, SourceChoice.Deps::class.java)
        if (deps.profiles().activeProfileId.value != pid) return // profil zmienił się w międzyczasie — zrobi to następny przebieg
        val player = deps.player()
        val current = player.playerSettings.first().subtitleStyle.fontId
        val userOwn = SubtitleFonts.isCustom(current) && current !in ids.values && store.fonts.value.any { it.id == current }
        if (userOwn) {
            Log.i(TAG, "profil $pid: zostaje czcionka dodana przez użytkownika ($current)")
        } else if (current != atkinson) {
            player.setSubtitleFontId(atkinson)
            Log.i(TAG, "profil $pid: czcionka napisów $current → ${ATKINSON.name}")
        }
        p.edit().putBoolean(flag, true).apply()
    }

    /** Wyłączone: usuwa czcionki wgrane przez moduł; profil z jedną z nich wraca do domyślnej. */
    private suspend fun uninstall(app: Context, pid: Int) {
        val p = prefs(app)
        val ids = readIds(p)
        val removed = p.getStringSet(K_REMOVED, emptySet()).orEmpty()
        if (ids.isEmpty() && removed.isEmpty()) return
        val gone = removed + ids.values
        if (ids.isNotEmpty()) {
            val store = SubtitleFontStore.get(app)
            val owned = p.getStringSet(K_OWNED, emptySet()).orEmpty()
            ids.values.filter { it in owned }.forEach { id ->
                try {
                    store.remove(id)
                    Log.i(TAG, "usunięta z czcionek napisów: $id")
                } catch (e: Exception) {
                    Log.w(TAG, "usuwanie $id: ${e.javaClass.simpleName}: ${e.message?.take(200)}")
                }
            }
            val e = p.edit().remove(K_IDS).remove(K_OWNED).putStringSet(K_REMOVED, gone)
            p.all.keys.filter { it.startsWith(K_CHOSEN) }.forEach { e.remove(it) }
            e.apply()
        }
        val deps = EntryPointAccessors.fromApplication(app, SourceChoice.Deps::class.java)
        if (deps.profiles().activeProfileId.value != pid) return
        val player = deps.player()
        val current = player.playerSettings.first().subtitleStyle.fontId
        if (current in gone) {
            player.setSubtitleFontId(SubtitleFonts.DEFAULT)
            Log.i(TAG, "profil $pid: czcionka napisów wróciła do domyślnej")
        }
    }

    private fun readIds(p: SharedPreferences): Map<String, String> =
        p.getString(K_IDS, null).orEmpty().split(';').mapNotNull { s ->
            val i = s.indexOf('=')
            if (i <= 0) null else s.substring(0, i) to s.substring(i + 1)
        }.toMap()
}
