package com.nuvio.tv.fork.lektor

import android.content.Context
import android.util.Log
import com.nuvio.tv.fork.ai.subtitles.GeminiProvider
import com.nuvio.tv.fork.ai.subtitles.ModelRequest
import java.io.File

/**
 * Fork 20.11 (właściciel: „chcę surowe zapytania do Gemini, tak jak zostały wysłane, najlepiej 3”): 3 ostatnie treści
 * zapytań każdego rodzaju w `Android/data/<pakiet>/files/lektor-zapytania/<rodzaj>-<1..3>.json`. Tylko treść (JSON) — klucz API idzie
 * w nagłówku i tu nie trafia. Nie do logcat (tam bez tekstu dialogów).
 */
object LektorDump {
    private const val KEEP = 3
    private val counters = HashMap<String, Int>()
    @Volatile private var dir: File? = null

    // Pamięć zewnętrzna aplikacji (/sdcard/Android/data/<pakiet>/files): da się ją pobrać przez adb (aplikacja nie jest „debuggable”).
    fun init(c: Context) { if (dir == null) dir = File(c.applicationContext.getExternalFilesDir(null) ?: c.applicationContext.filesDir, "lektor-zapytania") }

    fun save(kind: String, body: String) {
        val d = dir ?: return
        runCatching {
            d.mkdirs()
            val n = synchronized(counters) { val v = (counters[kind] ?: 0) % KEEP + 1; counters[kind] = v; v }
            File(d, "$kind-$n.json").writeText(body)
        }.onFailure { Log.w("ForkLektor", "zapis zapytania $kind: ${it.javaClass.simpleName}: ${it.message?.take(120)}") }
    }

    /** Zapisuje treść zapytania modelu tekstowego i zwraca je bez zmian. */
    fun text(c: Context, kind: String, provider: GeminiProvider, r: ModelRequest): ModelRequest { init(c); save(kind, provider.buildBody(r)); return r }
}
