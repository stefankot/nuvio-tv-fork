package com.nuvio.tv.skin.niunia.livetv

import android.content.Context
import android.util.Log
import com.nuvio.tv.reshaped.livetv.LiveTvChannel
import com.nuvio.tv.reshaped.livetv.LiveTvProgramme
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * IPTV #84 (JMGO 26.11): pierwsze wejście w Live TV › Odkrywaj po starcie aplikacji to 6,8 s „Wczytuję…” — rzędy były
 * tylko w pamięci procesu ([NiuniaDiscoverCache], #77). ZASADY §2: wynik ciężkiego liczenia na dysku, po restarcie odczyt.
 * Ostatnie rzędy (adres kanału + program) w `fork_tv/discover_v1.json.gz`; przy odczycie kanały z bieżącej listy, bez
 * programów, które już się skończyły („na żywo”) albo już się zaczęły („wkrótce”). Nowe rzędy liczą się w tle jak dotąd.
 */
internal object NiuniaDiscoverDisk {
    private const val TAG = "ForkNiunia"
    private const val VERSION = 1

    private fun file(c: Context) = File(File(c.applicationContext.filesDir, "fork_tv"), "discover_v$VERSION.json.gz")

    fun save(c: Context, rows: List<NiuniaDiscoverRow>) {
        try {
            val out = JSONArray()
            for (r in rows) {
                val tiles = JSONArray()
                for (t in r.tiles) tiles.put(JSONObject().apply {
                    put("u", t.channel.streamUrl); put("m", t.mark.name); put("s", t.series); put("w", t.withChannel)
                    t.progress?.let { put("pr", it.toDouble()) }; t.extra?.let { put("x", it) }; t.expiresHours?.let { put("e", it) }
                    t.programme?.let { p ->
                        put("p", JSONObject().apply {
                            put("t", p.title); put("a", p.startEpochMs); put("b", p.stopEpochMs)
                            p.image?.let { put("i", it) }; p.episode?.let { put("ep", it) }
                        })
                    }
                })
                out.put(JSONObject().put("title", r.title).put("tiles", tiles))
            }
            val f = file(c)
            f.parentFile?.mkdirs()
            val tmp = File(f.path + ".tmp")
            GZIPOutputStream(tmp.outputStream()).bufferedWriter().use { it.write(out.toString()) }
            if (!tmp.renameTo(f)) Log.w(TAG, "live tv: odkrywaj na dysk: nie udało się podmienić pliku")
        } catch (e: Exception) {
            Log.w(TAG, "live tv: odkrywaj na dysk: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    /** Ostatnie rzędy z dysku dla kanałów [channels] (null — brak pliku albo nic nie zostało). */
    fun load(c: Context, channels: List<LiveTvChannel>, now: Long): List<NiuniaDiscoverRow>? {
        val f = file(c)
        if (!f.exists() || channels.isEmpty()) return null
        return try {
            val t0 = android.os.SystemClock.elapsedRealtime()
            val arr = JSONArray(GZIPInputStream(f.inputStream()).bufferedReader().use { it.readText() })
            val byUrl = NiuniaListMaps.byUrl(channels)
            val rows = ArrayList<NiuniaDiscoverRow>()
            for (i in 0 until arr.length()) {
                val r = arr.getJSONObject(i)
                val ta = r.getJSONArray("tiles")
                val tiles = ArrayList<NiuniaLiveTile>()
                for (j in 0 until ta.length()) {
                    val o = ta.getJSONObject(j)
                    val ch = byUrl[o.getString("u")] ?: continue
                    val mark = try { NiuniaTileMark.valueOf(o.getString("m")) } catch (e: IllegalArgumentException) { NiuniaTileMark.NONE }
                    val p = o.optJSONObject("p")?.let { po ->
                        LiveTvProgramme(po.getString("t"), po.getLong("a"), po.getLong("b"), image = po.optString("i").ifEmpty { null }, episode = po.optString("ep").ifEmpty { null })
                    }
                    if (p != null && mark == NiuniaTileMark.LIVE && p.stopEpochMs <= now) continue // już się skończył
                    if (p != null && mark == NiuniaTileMark.SOON && p.startEpochMs <= now) continue // już nie „wkrótce”
                    tiles += NiuniaLiveTile(ch, p, mark, if (o.has("pr")) o.getDouble("pr").toFloat() else null, o.optBoolean("s"),
                        o.optString("x").ifEmpty { null }, o.optBoolean("w", true), if (o.has("e")) o.getInt("e") else null)
                }
                if (tiles.isNotEmpty()) rows += NiuniaDiscoverRow(r.getString("title"), tiles)
            }
            Log.i(TAG, "live tv: odkrywaj z dysku: ${rows.size} rzędów w ${android.os.SystemClock.elapsedRealtime() - t0} ms")
            rows.ifEmpty { null }
        } catch (e: Exception) {
            Log.w(TAG, "live tv: odkrywaj z dysku: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }
}
