package com.nuvio.tv.fork.devprefs

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder

/**
 * Serwer ustawień do testów (właściciel 19.11: „tak się nie da pracować” — klikanie pilotem po ustawieniach).
 * Tylko build debug, tylko 127.0.0.1:8765 (z Maca przez `adb forward`, niewidoczny w sieci domowej).
 * Czyta i zmienia ustawienia profilu (DataStore z ProfileDataStoreFactory) oraz SharedPreferences (`sp:<nazwa>`)
 * na żywo, bez restartu. Mac: `fork-tools/prefs.sh`. W logach tylko nazwy magazynów i kluczy, nigdy wartości.
 *
 * GET  /stores                      magazyny (aktywny profil)
 * GET  /get?store=S[&profile=N]     klucze magazynu: {klucz: {t, v}}
 * GET  /set?store=S&key=K&value=V[&type=bool|int|long|float|double|string|set][&profile=N]   (set: V = tablica JSON)
 * GET  /del?store=S&key=K[&profile=N]
 * GET  /dump[?profile=N]            wszystkie magazyny naraz
 * POST /apply[?profile=N]           treść = wynik /dump (całość lub część); zapisuje tylko różnice
 */
object DevPrefsServer {
    private const val TAG = "ForkDevPrefs"
    const val KEY = "fork_dev_prefs_server"
    const val PORT = 8765

    /** Magazyny z własnym `preferencesDataStore` — drugi DataStore na ten sam plik wywróciłby aplikację. */
    private val standalone = setOf(
        "app_onboarding", "auth_session_notice_store", "debug_settings", "device_local_player_prefs", "member_access",
        "profile_lock_state", "profile_settings", "sentry_settings", "torrent_settings", "tv_channel_prefs",
    )

    @EntryPoint @InstallIn(SingletonComponent::class)
    interface Deps { fun factory(): ProfileDataStoreFactory; fun profiles(): ProfileManager }

    @Volatile private var started = false

    /** ForkHooks.moduleOverlays: raz na proces. Wyłączony przełącznik albo build bez „debug” = nic nie robi. */
    fun start(c: Context) {
        if (started) return
        if (!com.nuvio.tv.BuildConfig.APPLICATION_ID.contains("debug")) return
        val app = c.applicationContext
        if (!ForkFeatures.enabled(app, KEY, default = true)) { Log.i(TAG, "wyłączony w ustawieniach"); return }
        started = true
        Thread({ serve(app) }, "ForkDevPrefs").apply { isDaemon = true }.start()
    }

    private fun serve(app: Context) {
        val server = try {
            ServerSocket(PORT, 4, InetAddress.getByName("127.0.0.1"))
        } catch (e: Exception) {
            Log.w(TAG, "start serwera: ${e.javaClass.simpleName}: ${e.message}"); started = false; return
        }
        Log.i(TAG, "serwer ustawień: 127.0.0.1:$PORT")
        while (true) {
            val s = try { server.accept() } catch (e: Exception) { Log.w(TAG, "accept: ${e.javaClass.simpleName}: ${e.message}"); continue }
            try { handle(app, s) } catch (e: Exception) {
                Log.w(TAG, "zapytanie: ${e.javaClass.simpleName}: ${e.message}")
                runCatching { reply(s, 500, JSONObject().put("error", "${e.javaClass.simpleName}: ${e.message}")) }
                    .onFailure { Log.w(TAG, "odpowiedź błędu: ${it.javaClass.simpleName}: ${it.message}") }
            } finally {
                runCatching { s.close() }.onFailure { Log.w(TAG, "close: ${it.javaClass.simpleName}: ${it.message}") }
            }
        }
    }

    private fun handle(app: Context, s: Socket) {
        s.soTimeout = 10_000
        val r = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
        val line = r.readLine() ?: return
        val (method, target) = line.split(" ").let { it.getOrElse(0) { "" } to it.getOrElse(1) { "/" } }
        var length = 0
        while (true) {
            val h = r.readLine() ?: break
            if (h.isEmpty()) break
            if (h.startsWith("Content-Length:", ignoreCase = true)) length = h.substringAfter(':').trim().toIntOrNull() ?: 0
        }
        val body = if (length > 0) CharArray(length).let { buf -> var n = 0; while (n < length) { val k = r.read(buf, n, length - n); if (k < 0) break; n += k }; String(buf, 0, n) } else ""
        val path = target.substringBefore('?')
        val q = target.substringAfter('?', "").split('&').filter { it.isNotEmpty() }.associate {
            URLDecoder.decode(it.substringBefore('='), "UTF-8") to URLDecoder.decode(it.substringAfter('=', ""), "UTF-8")
        }
        val deps = EntryPointAccessors.fromApplication(app, Deps::class.java)
        val profile = q["profile"]?.toIntOrNull() ?: deps.profiles().activeProfileId.value
        Log.i(TAG, "$method $path store=${q["store"] ?: "-"} key=${q["key"] ?: "-"} profil=$profile")
        val out: JSONObject = when (path) {
            "/stores" -> JSONObject().put("profile", profile)
                .put("datastore", JSONArray(dataStoreNames(app, profile)))
                .put("shared", JSONArray(sharedNames(app).map { "sp:$it" }))
            "/get" -> read(app, deps, profile, need(q, "store"))
            "/set" -> { write(app, deps, profile, need(q, "store"), need(q, "key"), q["type"], need(q, "value")); JSONObject().put("ok", true) }
            "/del" -> { remove(app, deps, profile, need(q, "store"), need(q, "key")); JSONObject().put("ok", true) }
            "/dump" -> JSONObject().also { o ->
                (dataStoreNames(app, profile) + sharedNames(app).map { "sp:$it" }).forEach { o.put(it, read(app, deps, profile, it)) }
            }
            "/apply" -> JSONObject().put("changed", apply(app, deps, profile, JSONObject(body)))
            else -> { reply(s, 404, JSONObject().put("error", "nieznana ścieżka $path")); return }
        }
        reply(s, 200, out)
    }

    private fun need(q: Map<String, String>, k: String) = q[k] ?: throw IllegalArgumentException("brak parametru $k")

    private fun reply(s: Socket, code: Int, o: JSONObject) {
        val b = o.toString(1).toByteArray(Charsets.UTF_8)
        s.getOutputStream().apply {
            write("HTTP/1.1 $code OK\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${b.size}\r\nConnection: close\r\n\r\n".toByteArray())
            write(b); flush()
        }
    }

    // --- magazyny ---

    private fun dataStoreNames(app: Context, profile: Int): List<String> {
        val suffix = Regex("_p\\d+$")
        return File(app.filesDir, "datastore").listFiles().orEmpty()
            .map { it.name }.filter { it.endsWith(".preferences_pb") }.map { it.removeSuffix(".preferences_pb") }
            .filter { if (profile == 1) !suffix.containsMatchIn(it) else it.endsWith("_p$profile") }
            .map { if (profile == 1) it else it.removeSuffix("_p$profile") }
            .filter { it !in standalone }.sorted()
    }

    private fun sharedNames(app: Context): List<String> =
        File(app.applicationInfo.dataDir, "shared_prefs").listFiles().orEmpty()
            .map { it.name }.filter { it.endsWith(".xml") }.map { it.removeSuffix(".xml") }.sorted()

    private fun store(deps: Deps, profile: Int, name: String): DataStore<Preferences> {
        require(name !in standalone) { "magazyn $name ma własny DataStore — tylko przez ekran aplikacji" }
        return deps.factory().get(profile, name)
    }

    private fun typeOf(v: Any?): String = when (v) {
        is Boolean -> "bool"; is Int -> "int"; is Long -> "long"; is Float -> "float"; is Double -> "double"
        is Set<*> -> "set"; else -> "string"
    }

    private fun jsonValue(v: Any?): Any? = if (v is Set<*>) JSONArray(v.toList()) else v

    private fun read(app: Context, deps: Deps, profile: Int, name: String): JSONObject {
        val o = JSONObject()
        if (name.startsWith("sp:")) {
            app.getSharedPreferences(name.removePrefix("sp:"), Context.MODE_PRIVATE).all.toSortedMap().forEach { (k, v) ->
                o.put(k, JSONObject().put("t", typeOf(v)).put("v", jsonValue(v)))
            }
        } else {
            val prefs = runBlocking(Dispatchers.IO) { store(deps, profile, name).data.first() }
            prefs.asMap().entries.sortedBy { it.key.name }.forEach { (k, v) ->
                o.put(k.name, JSONObject().put("t", typeOf(v)).put("v", jsonValue(v)))
            }
        }
        return o
    }

    private fun parse(type: String, raw: Any?): Any = when (type) {
        "bool" -> raw.toString().let { it == "true" || it == "1" || it == "wł" }
        "int" -> raw.toString().toInt()
        "long" -> raw.toString().toLong()
        "float" -> raw.toString().toFloat()
        "double" -> raw.toString().toDouble()
        "set" -> (if (raw is JSONArray) raw else JSONArray(raw.toString())).let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
        "string" -> raw.toString()
        else -> throw IllegalArgumentException("nieznany typ $type")
    }

    private fun existingType(app: Context, deps: Deps, profile: Int, name: String, key: String): String? =
        read(app, deps, profile, name).optJSONObject(key)?.optString("t")

    private fun write(app: Context, deps: Deps, profile: Int, name: String, key: String, type: String?, raw: Any?) {
        val t = type ?: existingType(app, deps, profile, name, key)
            ?: throw IllegalArgumentException("klucz $key nie istnieje — podaj type")
        val v = parse(t, raw)
        if (name.startsWith("sp:")) {
            val e = app.getSharedPreferences(name.removePrefix("sp:"), Context.MODE_PRIVATE).edit()
            @Suppress("UNCHECKED_CAST")
            when (v) {
                is Boolean -> e.putBoolean(key, v); is Int -> e.putInt(key, v); is Long -> e.putLong(key, v)
                is Float -> e.putFloat(key, v); is Double -> e.putFloat(key, v.toFloat())
                is Set<*> -> e.putStringSet(key, v as Set<String>); else -> e.putString(key, v.toString())
            }
            check(e.commit()) { "zapis SharedPreferences nieudany" }
        } else runBlocking(Dispatchers.IO) {
            store(deps, profile, name).edit { p ->
                @Suppress("UNCHECKED_CAST")
                when (v) {
                    is Boolean -> p[booleanPreferencesKey(key)] = v; is Int -> p[intPreferencesKey(key)] = v
                    is Long -> p[longPreferencesKey(key)] = v; is Float -> p[floatPreferencesKey(key)] = v
                    is Double -> p[doublePreferencesKey(key)] = v
                    is Set<*> -> p[stringSetPreferencesKey(key)] = v as Set<String>
                    else -> p[stringPreferencesKey(key)] = v.toString()
                }
                Unit
            }
        }
        Log.i(TAG, "zmieniono $name/$key (typ $t, profil $profile)")
    }

    private fun remove(app: Context, deps: Deps, profile: Int, name: String, key: String) {
        if (name.startsWith("sp:")) {
            check(app.getSharedPreferences(name.removePrefix("sp:"), Context.MODE_PRIVATE).edit().remove(key).commit()) { "usunięcie nieudane" }
        } else runBlocking(Dispatchers.IO) {
            store(deps, profile, name).edit { p -> p.asMap().keys.filter { it.name == key }.forEach { p.remove(it) } }
        }
        Log.i(TAG, "usunięto $name/$key (profil $profile)")
    }

    /** Wgrywa zrzut: tylko klucze, których wartość się różni. Zwraca liczbę zmian. */
    private fun apply(app: Context, deps: Deps, profile: Int, dump: JSONObject): Int {
        var changed = 0
        dump.keys().forEach { name ->
            if (name.removePrefix("sp:") in standalone) { Log.i(TAG, "pomijam $name (własny DataStore)"); return@forEach }
            val now = read(app, deps, profile, name)
            val want = dump.getJSONObject(name)
            want.keys().forEach { key ->
                val w = want.getJSONObject(key)
                val cur = now.optJSONObject(key)
                if (cur == null || cur.optString("t") != w.getString("t") || cur.opt("v").toString() != w.opt("v").toString()) {
                    write(app, deps, profile, name, key, w.getString("t"), w.opt("v")); changed++
                }
            }
        }
        Log.i(TAG, "wgrano zrzut: $changed zmian (profil $profile)")
        return changed
    }
}
