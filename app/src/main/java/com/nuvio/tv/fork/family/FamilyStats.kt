package com.nuvio.tv.fork.family

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.nuvio.tv.fork.logged

/**
 * Lekkie, anonimowe statystyki dla panelu „Nuvio Rodzina” (19.11.2026): start filmu (ms), błędy odtwarzania,
 * odłożone złe źródła, awarie aplikacji. Bez adresów strumieni i bez tytułów; źródło = nazwa dodatku.
 * Kolejka w prefs `fork_family_stats` (nie jedzie w zrzucie ustawień), wysyłana przez FamilySync.report.
 */
object FamilyStats {
    private const val PREFS = "fork_family_stats"
    private const val MAX = 200
    @Volatile private var app: Context? = null
    private val urls = Regex("""\w+://\S+""")

    fun install(c: Context) {
        if (app != null) return
        app = c.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                val at = e.stackTrace.firstOrNull { it.className.startsWith("com.nuvio") }?.let { "${it.className.substringAfterLast('.')}:${it.lineNumber}" }
                add(JSONObject().put("k", "crash").put("cls", e.javaClass.simpleName).put("at", at ?: ""), sync = true)
            }.logged("ForkFamily", "FamilyStats.kt:23")
            prev?.uncaughtException(t, e)
        }
        add(JSONObject().put("k", "start"))
    }

    fun play(ms: Long, source: String?) = add(JSONObject().put("k", "play").put("ms", ms).put("src", source.orEmpty().take(60)))
    fun playError(message: String?, source: String?) =
        add(JSONObject().put("k", "err").put("msg", urls.replace(message.orEmpty(), "…").take(120)).put("src", source.orEmpty().take(60)))
    fun badSource(reason: String, source: String?) = add(JSONObject().put("k", "bad").put("reason", reason).put("src", source.orEmpty().take(60)))

    @Synchronized private fun add(e: JSONObject, sync: Boolean = false) {
        val c = app ?: return
        val p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = runCatching { JSONArray(p.getString("q", "[]")) }.logged("ForkFamily", "FamilyStats.kt:40").getOrDefault(JSONArray())
        arr.put(e.put("t", System.currentTimeMillis()))
        val keep = JSONArray(); for (i in maxOf(0, arr.length() - MAX) until arr.length()) keep.put(arr.get(i))
        val ed = p.edit().putString("q", keep.toString())
        if (sync) ed.commit() else ed.apply()
    }

    /** Zabiera kolejkę do wysłania; przy błędzie wysyłki [restore]. */
    @Synchronized fun take(c: Context): JSONArray {
        val p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val arr = runCatching { JSONArray(p.getString("q", "[]")) }.logged("ForkFamily", "FamilyStats.kt:50").getOrDefault(JSONArray())
        p.edit().putString("q", "[]").apply()
        return arr
    }
    @Synchronized fun restore(c: Context, events: JSONArray) {
        val p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = runCatching { JSONArray(p.getString("q", "[]")) }.logged("ForkFamily", "FamilyStats.kt:56").getOrDefault(JSONArray())
        val all = JSONArray(); for (i in 0 until events.length()) all.put(events.get(i)); for (i in 0 until now.length()) all.put(now.get(i))
        val keep = JSONArray(); for (i in maxOf(0, all.length() - MAX) until all.length()) keep.put(all.get(i))
        p.edit().putString("q", keep.toString()).apply()
    }
}
