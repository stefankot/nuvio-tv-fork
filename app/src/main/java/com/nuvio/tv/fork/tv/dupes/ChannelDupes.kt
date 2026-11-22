package com.nuvio.tv.fork.tv.dupes

import java.text.Normalizer

/**
 * Duplikaty kanałów z wielu dostawców (właściciel 22.11: „5 dostawców, kanały w 99% wspólne; TVN HD, UHD, 4K itd. mają być
 * JEDNYM kanałem, a wybranie go uruchamia stream najlepszej jakości — preferencyjnie 4K; kolejność kopii wg jakości
 * i najszybszej odpowiedzi; pamięć wyniku np. 4 godziny, albo dopiero gdy pierwsza kopia nie odpowiada np. 3 s”).
 *
 * Tu tylko logika bez Androida (testy JVM): klucz grupy, ocena jakości, kolejność kopii. Pamięć wyników: [DupeMemory].
 */
object ChannelDupes {
    /** Jedna kopia kanału: nazwa z listy, adres, źródło (kolejność źródeł = [sourceOrder]). */
    data class Copy(val name: String, val url: String, val sourceId: String, val sourceOrder: Int = 0)

    /**
     * Tekst do porównań: NFKD (znaki w indeksie górnym „ᴿᴬᵂ”, „ⱽᴵᴾ”, „ᵁᴴᴰ” → zwykłe litery), małe litery, ł → l,
     * bez znaków diakrytycznych.
     */
    fun plain(s: String): String {
        val d = Normalizer.normalize(s.replace('ł', 'l').replace('Ł', 'L'), Normalizer.Form.NFKD).lowercase()
        val out = StringBuilder(d.length)
        for (c in d) if (Character.getType(c) != Character.NON_SPACING_MARK.toInt()) out.append(c)
        return out.toString()
    }

    private val QUALITY_WORDS = setOf(
        "4k", "8k", "uhd", "2160p", "fhd", "1080p", "1080", "hd", "720p", "sd", "576p", "hevc", "h265", "h264", "hdr",
        "raw", "vip", "50fps", "60fps", "fps", "backup", "alt", "+1",
    )
    private val BRACKET_COUNTRY = Regex("""\(([a-z]{2,3})\)""")
    private val SEPARATORS = charArrayOf(' ', '\t', '_', '-', '|', ':', '.', ',', '/', '(', ')', '[', ']', '•', '·', '★', '*')

    /**
     * Kraj z początku nazwy („PL:”, „PL |”, „[PL]”, „PL - ”) albo z nawiasu („TVN (PL)”); null = brak w nazwie.
     * [n] po [plain]. Przedrostek dostawcy („strong8k · ”) musi być już zdjęty.
     */
    internal fun country(n: String): String? {
        val t = n.trimStart('[', ' ')
        var i = 0
        while (i < t.length && t[i] in 'a'..'z') i++
        if (i in 2..3) {
            var j = i
            if (j < t.length && t[j] == ']') j++
            while (j < t.length && t[j] == ' ') j++
            if (j < t.length && (t[j] == ':' || t[j] == '|' || (t[j] == '-' && j > i))) return t.substring(0, i)
            if (n.trimStart().startsWith("[") && j > i && t[i] == ']') return t.substring(0, i)
        }
        if ('(' in n) BRACKET_COUNTRY.find(n)?.let { return it.groupValues[1] }
        return null
    }

    /** Nazwa bez przedrostka dostawcy: „Strong8K · DSTV: TVN AFRICA” → „DSTV: TVN AFRICA” (gdy przedrostek to nazwa źródła). */
    internal fun withoutProvider(name: String, providerNames: Collection<String>): String {
        val dot = name.indexOf(" · ")
        if (dot <= 0) return name
        val head = plain(name.substring(0, dot)).trim()
        return if (providerNames.any { plain(it).trim() == head }) name.substring(dot + 3) else name
    }

    /**
     * Klucz grupy: kraj + nazwa bez kraju, przedrostka dostawcy, nawiasów i oznaczeń jakości. Kraj zostaje w kluczu
     * („PA: TVN” ≠ „PL: TVN”); lista bez kraju w nazwach dostaje [listCountry] (np. hls.gd → „pl”).
     */
    fun groupKey(name: String, providerNames: Collection<String> = emptyList(), listCountry: String? = null): String {
        val n = plain(withoutProvider(name, providerNames))
        val c = country(n) ?: listCountry?.lowercase() ?: ""
        var t = n
        // zdejmij kraj z początku
        run {
            val s = t.trimStart('[', ' ')
            var i = 0
            while (i < s.length && s[i] in 'a'..'z') i++
            if (i in 2..3 && s.substring(0, i) == c) {
                var j = i
                if (j < s.length && s[j] == ']') j++
                while (j < s.length && s[j] == ' ') j++
                if (j < s.length && (s[j] == ':' || s[j] == '|' || s[j] == '-')) t = s.substring(j + 1)
                else if (t.trimStart().startsWith("[")) t = s.substring(j)
            }
        }
        // nawiasy: kraj w nawiasie już w [c]; reszta nawiasów to zwykle jakość/opis
        val sb = StringBuilder()
        var depth = 0
        for (ch in t) when {
            ch == '(' || ch == '[' -> { depth++; sb.append(' ') }
            (ch == ')' || ch == ']') && depth > 0 -> { depth--; sb.append(' ') }
            depth == 0 -> sb.append(ch)
        }
        val all = sb.toString().split(*SEPARATORS).filter { it.isNotEmpty() }
        // „50 fps” = jakość: liczba przed „fps” też odpada (sama liczba zostaje — „TVN 24”).
        val words = all.filterIndexed { i, w -> w !in QUALITY_WORDS && !(i + 1 < all.size && all[i + 1] == "fps" && w.all(Char::isDigit)) }
        val base = words.joinToString(" ").ifEmpty { n.trim() }
        return "$c|$base"
    }

    /**
     * Ocena jakości z nazwy i adresu (większa = lepsza): 4K/UHD 40, FHD 30, HD 20, nieznana 15, SD 10;
     * HEVC/H265 +3, HDR +2, 50/60 fps +1, RAW +1; kopia zapasowa („backup”, „alt”) −5.
     */
    fun quality(name: String, url: String = ""): Int {
        val words = (plain(name) + " " + plain(url.substringAfterLast('/'))).split(*SEPARATORS).filter { it.isNotEmpty() }.toSet()
        var q = when {
            words.any { it == "4k" || it == "8k" || it == "uhd" || it == "2160p" || it == "2160" } -> 40
            words.any { it == "fhd" || it == "1080p" || it == "1080" } -> 30
            words.any { it == "hd" || it == "720p" || it == "720" } -> 20
            words.any { it == "sd" || it == "576p" || it == "480p" } -> 10
            else -> 15
        }
        if (words.any { it == "hevc" || it == "h265" }) q += 3
        if ("hdr" in words) q += 2
        if (words.any { it == "50fps" || it == "60fps" } || ("fps" in words && (("50" in words) || ("60" in words)))) q += 1
        if ("raw" in words) q += 1
        if (words.any { it == "backup" || it == "alt" }) q -= 5
        return q
    }

    /**
     * Kolejność kopii jednego kanału: najpierw ta, która w ciągu [DupeMemory.VALID_MS] ruszyła (ostatnia wygrana),
     * potem bez świeżej porażki, potem jakość (4K pierwsze), potem szybkość odpowiedzi z pamięci, potem kolejność źródeł.
     */
    fun order(copies: List<Copy>, memory: DupeMemory, now: Long): List<Copy> =
        copies.sortedWith(
            compareBy<Copy>(
                { if (memory.recentWin(it.url, now)) 0 else 1 },
                { if (memory.recentFailure(it.url, now)) 1 else 0 },
                { -quality(it.name, it.url) },
                { memory.firstByteMs(it.url, now) ?: Long.MAX_VALUE },
                { it.sourceOrder },
            ),
        )

    /** Grupy duplikatów w kolejności pierwszego wystąpienia (lista z polskimi pierwszymi zostaje w tej kolejności). */
    fun <T> groups(items: List<T>, key: (T) -> String): List<List<T>> {
        val map = LinkedHashMap<String, MutableList<T>>()
        for (it in items) map.getOrPut(key(it)) { ArrayList(2) }.add(it)
        return map.values.toList()
    }
}
