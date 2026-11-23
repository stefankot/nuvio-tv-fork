package com.nuvio.tv.fork.tv.names

import android.content.Context
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Nazwa kanału na ekranie bez dopisków (właściciel 23.11: „absolutnie niepotrzebne są dopiski: Strong8K PL (nazwa
 * kanału) HD — zostaw tylko nazwę kanału”). Listy IPTV nazywają kanały np. „PL: Polsat Cafe HD”, „Strong8K: PL| TVN 7 FHD”,
 * „Active Family HD (1080i)”, „[PL] TVP 1 4K”; na ekranie zostaje „Polsat Cafe”, „TVN 7”, „Active Family”, „TVP 1”.
 * Zmienia tylko to, co widać — dane kanału (nazwa w liście, szukanie, przewodnik) zostają bez zmian.
 * Wyłączone ([KEY]) — nazwy jak w liście dostawcy.
 */
object ChannelNames {
    const val KEY = "tv_plain_names"
    private const val TAG = "ForkTvNames"
    private const val MEMO_MAX = 20_000

    @Volatile var enabled = true
        private set
    private val memo = ConcurrentHashMap<String, String>()

    fun start(c: Context) {
        enabled = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, KEY, true)
        memo.clear()
        Log.i(TAG, "nazwy kanałów bez dopisków dostawcy, kraju i jakości: ${if (enabled) "tak" else "nie"}")
    }

    /** Nazwa do pokazania (pamięć wyników; ekrany pytają o te same nazwy w każdej klatce). */
    fun shown(name: String): String {
        if (!enabled) return name
        memo[name]?.let { return it }
        val out = clean(name)
        if (memo.size > MEMO_MAX) memo.clear()
        memo[name] = out
        return out
    }

    // „Strong8K: PL| …”, „Strong8K PL …” — nazwa dostawcy przed znacznikiem kraju.
    private val PROVIDER = Regex("""^\s*[\p{L}\p{N}+._-]{2,24}\s*[:|]?\s+(?=[A-Z]{2,3}\s*[:|\]\-–])""")
    // „PL: ”, „PL| ”, „PL - ”, „[PL] ”, „(PL) ”, „|PL| ”, „PL ▎”.
    private val COUNTRY_HEAD = Regex("""^\s*(?:\|\s*[A-Z]{2,3}\s*\||\[\s*[A-Z]{2,3}\s*]|\(\s*[A-Z]{2,3}\s*\)|[A-Z]{2,3}\s*[:|]|[A-Z]{2,3}\s+[-–]\s)\s*""")
    // Na końcu: jakość, rozdzielczość, klatki, kodek (osobne słowo, także w nawiasie: „HD (1080i)”, „FHD 50fps”).
    private val QUALITY_TAIL = Regex(
        """\s*[\(\[]?\s*(?<![\p{L}\p{N}])(?:F?HD|UHD|SD|HQ|LQ|4K|8K|HDR(?:10)?|HEVC|H\.?26[45]|\d{3,4}\s*[pi](?:\s*\d{2,3}\s*fps)?|\d{2,3}\s*fps|RAW|VIP|BACKUP|ᴴᴰ|ᶠᴴᴰ|ᵁᴴᴰ|ˢᴰ|ᴿᴬᵂ)\s*[\)\]]?\s*$""",
        RegexOption.IGNORE_CASE,
    )
    // Kraj po nazwie: „TVN (PL)”, „TVN [PL]”, „TVN | PL”.
    private val COUNTRY_TAIL = Regex("""\s*(?:[\(\[]\s*[A-Z]{2,3}\s*[\)\]]|[|:]\s*[A-Z]{2,3})\s*$""")
    private val TRIM_CHARS = charArrayOf(' ', '|', ':', '-', '–', '·', '•', '_', '/', ',', '.')

    /** Czyszczenie (bez pamięci; testy JVM). */
    internal fun clean(name: String): String {
        var s = name.trim()
        if (s.isEmpty()) return name
        try {
            s = s.replace(PROVIDER, "")
            s = s.replace(COUNTRY_HEAD, "")
            // Kilka dopisków na końcu („HD (1080i)”, „FHD 50fps”, „HD (PL)”) — po jednym; sama nazwa nigdy nie znika.
            repeat(5) {
                val t = s.trimEnd(*TRIM_CHARS)
                val m = QUALITY_TAIL.find(t) ?: COUNTRY_TAIL.find(t) ?: return@repeat
                val cut = t.substring(0, m.range.first).trimEnd(*TRIM_CHARS)
                if (cut.isEmpty()) return@repeat
                s = cut
            }
            s = s.trim(*TRIM_CHARS).replace(Regex("""\s{2,}"""), " ")
        } catch (e: Exception) {
            Log.w(TAG, "nazwa „${name.take(40)}”: ${e.javaClass.simpleName}: ${e.message}")
            return name
        }
        return s.ifEmpty { name.trim() }
    }
}
