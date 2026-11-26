package com.nuvio.tv.fork.tv.country

import android.content.Context
import com.nuvio.tv.fork.tv.dupes.ChannelDupes
import com.nuvio.tv.reshaped.livetv.LiveTvChannel

/**
 * Kraj IPTV (właściciel 22.11): „preferowany kraj — wszystko z tego kraju (dla mnie polskie) na początek każdej listy;
 * konieczny kraj — inne w ogóle się nie pokazują”. Dwa przełączniki w ustawieniach „Telewizja” ([KEY_PREFER] domyślnie
 * włączony — zastępuje „polskie przed zagranicznymi” z 22.11, [KEY_ONLY] domyślnie wyłączony) i kraj ([countryCode],
 * domyślnie pl, zmiana czynnością „Zmień kraj IPTV”).
 *
 * Kraj kanału: z nazwy („PL:”, „PL |”, „[PL]”, „(PL)”, po zdjęciu przedrostka dostawcy „Strong8K · ”), a gdy nazwa go
 * nie ma — kraj całej listy (rozpoznany przy przewodniku uzupełniającym, np. hls.gd → pl). Kraj z nazwy liczony raz na
 * nazwę (pamięć przeżywa przeładowania list — liczone są tylko nowe nazwy).
 */
object TvCountry {
    const val KEY_PREFER = "tv_country_prefer"
    const val KEY_ONLY = "tv_country_only"
    private const val PREFS = "fork_tv_country"
    private const val KEY_CODE = "code"
    val CHOICES = listOf("pl" to "Polska", "de" to "Niemcy", "uk" to "Wielka Brytania", "us" to "USA", "cz" to "Czechy", "ua" to "Ukraina", "fr" to "Francja", "it" to "Włochy", "es" to "Hiszpania")

    /** Ustawienia w chwili liczenia listy (część klucza pamięci wyniku listy). */
    data class Mode(val prefer: Boolean, val only: Boolean, val country: String) {
        val active: Boolean get() = prefer || only
    }

    fun mode(c: Context): Mode = Mode(
        prefer = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, KEY_PREFER, true),
        only = com.nuvio.tv.ui.screens.settings.ForkFeatures.enabled(c, KEY_ONLY, false),
        country = countryCode(c),
    )

    fun countryCode(c: Context): String =
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CODE, "pl") ?: "pl"

    /** Następny kraj z [CHOICES]; zwraca jego nazwę. */
    fun nextCountry(c: Context): String {
        val cur = countryCode(c)
        val i = CHOICES.indexOfFirst { it.first == cur }
        val next = CHOICES[(i + 1).mod(CHOICES.size)]
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CODE, next.first).apply()
        android.util.Log.i("ForkTvCountry", "kraj IPTV: ${next.second}")
        return next.second
    }

    fun nameOf(code: String): String = CHOICES.firstOrNull { it.first == code }?.second ?: code.uppercase()

    /** Kraj z nazwy kanału ("" = brak w nazwie), raz na nazwę — trwała pamięć na dysku (po restarcie tylko odczyt). */
    private val fromName get() = com.nuvio.tv.fork.tv.index.TvIndexStore.country

    fun countryInName(name: String, providers: Collection<String>): String = fromName[name]
        ?: computeCountry(name, providers).also { fromName.put(name, it) }

    /**
     * IPTV #81 (właściciel 26.11: „nie działają kraje i języki — po wyborze pusta lista”): „Strong8K · PL: TVN …” miało kraj
     * tylko wtedy, gdy przedrostek był DOKŁADNIE nazwą źródła — na JMGO nie był, więc z 59 tys. kanałów kraj miało ok. 1,5 tys.
     * (głównie wydarzenia PPV „NEXT | … (US)” bez programu): „Stany Zjednoczone” dawały listę pustych wierszy. Teraz przedrostek
     * dostawcy przed znacznikiem kraju („Strong8K · ”, „Strong8K: ”, „Strong8K ”, jak w [com.nuvio.tv.fork.tv.names.ChannelNames])
     * jest zdejmowany zawsze, a „|PL| …” też daje kraj.
     */
    internal fun computeCountry(name: String, providers: Collection<String>): String {
        ChannelDupes.country(ChannelDupes.plain(ChannelDupes.withoutProvider(name, providers)))?.let { return it }
        fun head(t: String): String? = TAGGED.find(t)?.groupValues?.get(1)?.lowercase() ?: ChannelDupes.country(ChannelDupes.plain(t))
        head(name.trimStart().trimStart('|', ' '))?.let { return it }
        val m = PROVIDER_HEAD.find(name) ?: return ""
        return head(name.substring(m.range.last + 1).trimStart('|', ' ')) ?: ""
    }

    // „PL VIP: CANAL+ SPORT 2”, „UK RAW | …” — kraj, dopisek jakości/pakietu, potem dwukropek albo kreska.
    private const val TAGS = "VIP|RAW|4K|8K|UHD|FHD|HD|HEVC|SD|PPV|HDR|50FPS"
    private val TAGGED = Regex("""^([A-Z]{2})\s+(?:$TAGS)\s*[:|]""")
    private val PROVIDER_HEAD = Regex("""^\s*[\p{L}\p{N}+._-]{2,24}\s*[:|·•]?\s+(?=[A-Z]{2,3}\s*[:|\]\-–]|[A-Z]{2}\s+(?:$TAGS)\s*[:|])""")

    /** Kraj kanału: z nazwy, inaczej z grupy listy („PL | NEWS”), inaczej kraj jego listy ([listCountry] po sourceId); null = nieznany. */
    fun countryOf(ch: LiveTvChannel, providers: Collection<String>, listCountry: (String) -> String?): String? =
        countryInName(ch.name, providers).ifEmpty { null } ?: groupCountry(ch.group) ?: listCountry(ch.sourceId)?.lowercase()

    /** IPTV #81: kraj z nazwy grupy (kilkaset grup na listę — pamięć w procesie). */
    private val byGroup = java.util.concurrent.ConcurrentHashMap<String, String>()
    internal fun groupCountry(group: String): String? {
        if (group.isBlank()) return null
        return (byGroup[group] ?: computeCountry(group, emptyList()).also { byGroup[group] = it }).ifEmpty { null }
    }

    /**
     * Lista wg ustawień: [Mode.only] — tylko kanały kraju (gdy żaden nie pasuje, cała lista zostaje i [onNoneMatch] —
     * lepiej pokazać wszystko niż pusty ekran); [Mode.prefer] — kanały kraju na początku, reszta za nimi; w obu grupach
     * kolejność listy bez zmian. Wyłączone oba — lista bez zmian.
     */
    fun arrange(
        channels: List<LiveTvChannel>,
        mode: Mode,
        providers: Collection<String>,
        listCountry: (String) -> String?,
        onNoneMatch: () -> Unit = {},
    ): List<LiveTvChannel> {
        if (!mode.active || channels.isEmpty()) return channels
        val mine = ArrayList<LiveTvChannel>()
        val rest = ArrayList<LiveTvChannel>()
        for (ch in channels) if (countryOf(ch, providers, listCountry) == mode.country) mine += ch else rest += ch
        if (mode.only) {
            if (mine.isEmpty()) { onNoneMatch(); return channels }
            return mine
        }
        return if (mine.isEmpty() || rest.isEmpty()) channels else mine + rest
    }
}
