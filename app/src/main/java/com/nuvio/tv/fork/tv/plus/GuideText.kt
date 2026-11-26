package com.nuvio.tv.fork.tv.plus

import android.util.Log

/**
 * UX JMGO 26.11 (#71, zdjęcia 207–212): przewodnik Nowej TV ma tytuły zakodowane podwójnie („Can&amp;#39;t dance”) —
 * po odczycie XML w tytule zostawało „Can&#39;t dance, don&#39;t ask me” (odtwarzacz, kafelki, przewodnik).
 * Tu resztki encji HTML zamieniane na znaki (najwyżej dwa przebiegi: „&amp;#39;” → „&#39;” → „'”). Tekst bez „&” wraca bez zmian.
 */
internal object GuideText {
    private const val TAG = "ForkGuideDb"
    private val ENTITY = Regex("""&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|amp|quot|apos|lt|gt|nbsp);""")
    @Volatile private var logged = false

    fun clean(text: String?): String? {
        if (text == null || text.indexOf('&') < 0) return text
        var out: String = text
        for (pass in 0 until 2) {
            if (out.indexOf('&') < 0) break
            out = ENTITY.replace(out) { m -> decode(m.groupValues[1]) ?: m.value }
        }
        if (!logged && out != text) { logged = true; Log.i(TAG, "przewodnik: znaki HTML w tekstach zamieniane na litery (np. „&#39;” → „'”)") }
        return out
    }

    private fun decode(e: String): String? {
        val cp = when {
            e.startsWith("#x") || e.startsWith("#X") -> e.substring(2).toIntOrNull(16)
            e.startsWith("#") -> e.substring(1).toIntOrNull()
            else -> return when (e) { "amp" -> "&"; "quot" -> "\""; "apos" -> "'"; "lt" -> "<"; "gt" -> ">"; "nbsp" -> " "; else -> null }
        } ?: return null
        return if (cp > 0 && Character.isValidCodePoint(cp)) String(Character.toChars(cp)) else null
    }
}
