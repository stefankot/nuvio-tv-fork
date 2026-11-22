package com.nuvio.tv.fork.tv.dupes

/**
 * Pamięć wyników kopii kanałów (właściciel 22.11: „cache np. 4 godziny”): która kopia ruszyła, jak szybko dała pierwsze
 * dane, która zawiodła. Wpis ważny [VALID_MS]; starsze nie wpływają na kolejność i wypadają przy zapisie.
 * Bez Androida — zapis/odczyt jako tekst ([encode]/[decode]), plik trzyma moduł (filesDir).
 * W tekście są tylko adresy kopii po skrócie ([keyOf]) — bez loginów i haseł z adresów (ZASADY §3/§6).
 */
class DupeMemory(private val entries: MutableMap<String, Entry> = HashMap()) {
    data class Entry(val okAtMs: Long = 0, val firstByteMs: Long = 0, val failAtMs: Long = 0)

    companion object {
        const val VALID_MS = 4L * 60 * 60 * 1000

        /** Skrót adresu (bez części z loginem/hasłem w czytelnej postaci). */
        fun keyOf(url: String): String = Integer.toHexString(url.hashCode()) + ":" + url.length

        fun decode(text: String): DupeMemory {
            val m = HashMap<String, Entry>()
            text.lineSequence().forEach { line ->
                val p = line.split('\t')
                if (p.size == 4) {
                    val ok = p[1].toLongOrNull() ?: return@forEach
                    val fb = p[2].toLongOrNull() ?: return@forEach
                    val fail = p[3].toLongOrNull() ?: return@forEach
                    m[p[0]] = Entry(ok, fb, fail)
                }
            }
            return DupeMemory(m)
        }
    }

    @Synchronized fun onSuccess(url: String, firstByteMs: Long, now: Long) {
        entries[keyOf(url)] = Entry(okAtMs = now, firstByteMs = firstByteMs, failAtMs = 0)
    }

    @Synchronized fun onFailure(url: String, now: Long) {
        val old = entries[keyOf(url)] ?: Entry()
        entries[keyOf(url)] = old.copy(failAtMs = now)
    }

    @Synchronized fun recentWin(url: String, now: Long): Boolean =
        entries[keyOf(url)]?.let { it.okAtMs > 0 && now - it.okAtMs < VALID_MS && it.failAtMs < it.okAtMs } == true

    @Synchronized fun recentFailure(url: String, now: Long): Boolean =
        entries[keyOf(url)]?.let { it.failAtMs > 0 && now - it.failAtMs < VALID_MS && it.failAtMs >= it.okAtMs } == true

    @Synchronized fun firstByteMs(url: String, now: Long): Long? =
        entries[keyOf(url)]?.takeIf { it.okAtMs > 0 && now - it.okAtMs < VALID_MS }?.firstByteMs

    /** Tekst do zapisu: tylko wpisy jeszcze ważne. */
    @Synchronized fun encode(now: Long): String = entries.entries
        .filter { (_, e) -> now - maxOf(e.okAtMs, e.failAtMs) < VALID_MS }
        .joinToString("\n") { (k, e) -> "$k\t${e.okAtMs}\t${e.firstByteMs}\t${e.failAtMs}" }

    @Synchronized fun size(): Int = entries.size
}
