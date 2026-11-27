package com.nuvio.tv.fork.tv.country

import java.util.Locale

/**
 * Przegląd ekranów 27.11, ekrany 15 i 16: przedrostki list IPTV znaczą coś innego niż kody ISO.
 *  - „AR:” = kanały arabskie (nie Argentyna); Argentyna tylko z „ARG” / „ARGENTINA”,
 *  - „LAT:”/„LATAM:”/„LATINO” = Ameryka Łacińska (język hiszpański), „EX-YU”/„YU” = Bałkany, „AF:” = Afryka,
 *  - kody trzyliterowe ISO 3166 alpha-3 („IND:”, „USA:”, „GBR:”) → dwuliterowe,
 *  - słowa w grupach i nazwach („INDIA |”, „HINDI”, „TAMIL”…) → kraj.
 * Wynik: kod jak w nazwach kanałów (pl, uk, in…) albo region spoza ISO ([PSEUDO]).
 */
object IptvPrefixes {
    const val ARAB = "arab"
    const val LATAM = "latam"
    const val EXYU = "exyu"
    const val AFRICA = "afr"

    /** Regiony list IPTV spoza ISO: nazwa w panelu „Kraj i język” i język. */
    val PSEUDO: Map<String, String> = mapOf(ARAB to "Kraje arabskie", LATAM to "Ameryka Łacińska", EXYU to "Bałkany (ex-YU)", AFRICA to "Afryka")
    val PSEUDO_LANGUAGE: Map<String, String> = mapOf(ARAB to "ar", LATAM to "es")

    private val TWO: Map<String, String> = mapOf("ar" to ARAB, "lat" to LATAM, "yu" to EXYU, "ex" to EXYU, "af" to AFRICA, "gb" to "uk", "en" to "uk")

    private val ALPHA3: Map<String, String> = Locale.getISOCountries().mapNotNull { cc ->
        runCatching { Locale("", cc).isO3Country.lowercase() to (if (cc == "GB") "uk" else cc.lowercase()) }
            .onFailure { android.util.Log.w("ForkTvCountry", "alpha-3 $cc: ${it.javaClass.simpleName}: ${it.message}") }.getOrNull()
    }.toMap()

    private val WORDS: Map<String, String> = (
        "india:in hindi:in tamil:in telugu:in malayalam:in kannada:in punjabi:in marathi:in gujarati:in bengali:in bangla:bd " +
            "arab:$ARAB arabic:$ARAB arabia:$ARAB latino:$LATAM latam:$LATAM latin:$LATAM latina:$LATAM exyu:$EXYU ex-yu:$EXYU balkan:$EXYU " +
            "africa:$AFRICA poland:pl polska:pl polish:pl argentina:ar usa:us america:us canada:ca germany:de deutsch:de deutschland:de " +
            "france:fr french:fr italia:it italy:it italian:it espana:es spain:es spanish:es portugal:pt brasil:br brazil:br " +
            "turkey:tr turkiye:tr turkish:tr greece:gr greek:gr russia:ru russian:ru ukraine:ua romania:ro hungary:hu czech:cz " +
            "slovakia:sk netherlands:nl dutch:nl belgium:be sweden:se norway:no denmark:dk finland:fi pakistan:pk iran:ir persian:ir " +
            "afghanistan:af albania:al albanian:al croatia:hr serbia:rs bosnia:ba bulgaria:bg mexico:mx colombia:co chile:cl " +
            "peru:pe philippines:ph pinoy:ph thailand:th vietnam:vn indonesia:id malaysia:my china:cn chinese:cn japan:jp korea:kr " +
            "israel:il ireland:ie australia:au caribbean:$LATAM kurdish:iq kurdistan:iq"
        ).split(' ').associate { it.substringBefore(':') to it.substringAfter(':') }

    /** Surowy kod z nazwy (2–3 litery, małe) → kod forka. Nieznane 3-literowe zostają (odfiltruje je panel). */
    fun normalize(raw: String?): String? {
        val r = raw?.lowercase()?.takeIf { it.isNotEmpty() } ?: return raw
        TWO[r]?.let { return it }
        if (r.length == 3) return WORDS[r] ?: ALPHA3[r] ?: r
        return r
    }

    private val WORD_HEAD = Regex("""^\s*[\[(|]?\s*([A-Za-z][A-Za-z-]{2,13})\s*(?:[\]):|\-–]|\s+\|)""")

    /** Słowo na początku nazwy albo grupy („INDIA | Star Plus”, „HINDI: …”, „[ARABIC] …”) → kraj; null = brak w słowniku. */
    fun wordCountry(text: String): String? {
        val w = WORD_HEAD.find(text)?.groupValues?.get(1)?.lowercase() ?: return null
        return WORDS[w] ?: WORDS[w.replace("-", "")]
    }

    /** Jedno słowo grupy („Hindi Movies”, „Arabic News”): pierwsze słowo z listy, gdy grupa nie ma znacznika kraju. */
    fun groupWord(group: String): String? {
        val first = group.trim().trimStart('[', '(', '|', ' ').takeWhile { it.isLetter() || it == '-' }.lowercase()
        if (first.length < 4) return null
        return WORDS[first]
    }
}
