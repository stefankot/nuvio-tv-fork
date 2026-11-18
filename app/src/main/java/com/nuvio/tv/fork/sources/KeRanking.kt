package com.nuvio.tv.fork.sources

import android.content.Context
import com.nuvio.tv.core.player.DecodeCapability
import com.nuvio.tv.core.player.DecodeVerdict
import com.nuvio.tv.core.player.StreamQualityRank
import com.nuvio.tv.core.stream.ContentIdentity
import com.nuvio.tv.core.stream.ContentIdentityGuard
import com.nuvio.tv.core.stream.PreferredSource
import com.nuvio.tv.core.stream.ReleaseFactsParser
import com.nuvio.tv.core.stream.isOverResolutionCap
import com.nuvio.tv.core.stream.tvMaxResolution
import com.nuvio.tv.domain.model.ReleaseResolution
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Hybryda wyboru źródła (18.11.2026): jedyne miejsce, które składa kod KE (kopie 1:1 w `core/stream`, `core/player`)
 * w kolejność kandydatów auto-odtwarzania. Wzór: KE `core/player/AutoPlaySelection.kt` (0a87a08), bez części
 * zależnych od pobierania wieloma połączeniami KE (limit łącza, test łącza, prefetch).
 *
 * Kolejność: zły tytuł/rok odpada (ContentIdentityGuard) → ranking jakości KE (TRaSH) → na koniec to, czego urządzenie
 * nie dekoduje sprzętowo albo co przekracza rozdzielczość telewizora → na początek preferowane źródło serialu.
 * Używają: StreamAutoPlaySelector (tryb „pierwsze źródło”, wpięcie `Fork hook: ke-rank`) i wyścig Echo.
 */
object KeRanking {
    const val KEY = "ke_ranking"
    /** Jedyny wybór jakości w ustawieniach (właściciel 18.11): oszczędzanie łącza. */
    const val SAVER_KEY = "data_saver"
    @Volatile private var saver = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var enabled = true
    @Volatile private var decode: DecodeCapability = DecodeCapability.UNKNOWN
    @Volatile private var tvCap: ReleaseResolution? = null

    /** Tytuł, którego dotyczy bieżące szukanie źródeł; null = bez strażnika. Ważny [TTL_MS] od [begin]. */
    @Volatile private var identityRaw: ContentIdentity? = null
    /** Preferowane źródło bieżącego serialu (KE PreferredSourceDataStore); null = brak. */
    @Volatile private var preferredRaw: PreferredSource? = null
    @Volatile private var setAtMs = 0L
    /** Po tym czasie dane ekranu źródeł nie dotyczą już wyboru (np. następny odcinek w odtwarzaczu, inny tytuł). */
    private const val TTL_MS = 90_000L
    private fun fresh() = android.os.SystemClock.elapsedRealtime() - setAtMs < TTL_MS
    val identity: ContentIdentity? get() = identityRaw?.takeIf { fresh() }
    val preferred: PreferredSource? get() = preferredRaw?.takeIf { fresh() }

    @dagger.hilt.EntryPoint @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface Deps { fun preferredSources(): com.nuvio.tv.data.local.PreferredSourceDataStore }

    /** Ekran źródeł: początek szukania źródeł dla tytułu (wpięcie `Fork hook: ke-rank` w StreamScreenViewModel.loadStreams). */
    fun begin(context: Context, type: String?, title: String?, year: String?, season: Int?, episode: Int?, videoId: String?, showId: String?) {
        identityRaw = ContentIdentity.of(type, title, year, season, episode, videoId)
        ForeignDub.setTitle(title) // fork 19.11: słowa tytułu nie są językiem dubbingu
        preferredRaw = null
        setAtMs = android.os.SystemClock.elapsedRealtime()
        val app = context.applicationContext
        if (showId.isNullOrBlank()) return
        scope.launch {
            preferredRaw = runCatching {
                dagger.hilt.android.EntryPointAccessors.fromApplication(app, Deps::class.java).preferredSources().observe(showId).first()
            }.getOrNull()
        }
    }

    /** Ręczny wybór źródła na ekranie źródeł: zdejmij blokadę złego źródła; w serialu zapamiętaj je jako preferowane. */
    fun onManualPick(context: Context, showId: String?, stream: Stream) {
        StartupChecks.clearFor(stream)
        rememberManualPick(context, showId, stream)
    }

    /** Ręczny wybór źródła (długie OK / panel): zapamiętaj je jako preferowane dla serialu (KE PreferredSource.of). */
    fun rememberManualPick(context: Context, showId: String?, stream: Stream) {
        if (showId.isNullOrBlank()) return
        val app = context.applicationContext
        scope.launch {
            runCatching {
                dagger.hilt.android.EntryPointAccessors.fromApplication(app, Deps::class.java).preferredSources().set(showId, PreferredSource.of(stream))
            }
        }
    }

    /**
     * Odpowiedź na pytanie „polski lektor/dubbing czy jakość obrazu?” (fork/sources/SourceChoice): tylko reguły polskiego
     * dźwięku z jej zestawu (`source_presets.json`, testy B04-1: „PL subs” to nie polski dźwięk). Null = bez preferencji
     * języka (odpowiedź „oryginał” albo brak odpowiedzi); jakość układa wtedy sam ranking KE.
     */
    @Volatile private var polishRules: SourcePreset? = null

    /** Język napisów = polski (ustawienia odtwarzacza): źródła z polskimi napisami w nazwie wyżej, po polskim dźwięku (W4). */
    @Volatile private var polishSubs = false
    private val PL_SUBS = Regex("""(?i)\b(?:pl[ ._-]?subs?|subs?[ ._-]?pl|napisy(?:[ ._-]?pl)?|plsub)\b""")
    private fun subsTier(s: Stream): Int =
        if (!polishSubs) 0 else if (PL_SUBS.containsMatchIn(listOfNotNull(s.name, s.title, s.description).joinToString(" "))) 0 else 1

    private fun polishTier(s: Stream): Int {
        val p = polishRules ?: return 0
        val text = listOfNotNull(s.name, s.title, s.description).joinToString(" ")
        val audio = SourcePresets.tierOf(p, text) ?: Int.MAX_VALUE
        // P3 (18.11): działa polski głos lektora i napisy mają być po polsku → źródło z polskimi napisami równie dobre
        // jak polski dźwięk (lektor przeczyta). Brak głosu (JMGO) → zostaje sam polski dźwięk.
        if (polishSubs && com.nuvio.tv.fork.lektor.LektorVoice.ready == true && PL_SUBS.containsMatchIn(text)) return 0
        return audio
    }

    /** Raz po starcie i po każdej zmianie odpowiedzi: przełącznik, język, dekodery urządzenia, rozdzielczość telewizora. */
    @Volatile private var appCtx: Context? = null

    fun warm(context: Context) {
        val app = context.applicationContext
        appCtx = app
        enabled = ForkFeatures.enabled(app, KEY, true)
        saver = ForkFeatures.enabled(app, SAVER_KEY, false)
        val answer = SourceChoice.answer(app)
        polishRules = SourceChoice.presets(app).firstOrNull { it.id == answer }
            ?.let { preset -> preset.copy(rules = preset.rules.filter { "lektor" in it }) }?.takeIf { it.rules.isNotEmpty() }
        com.nuvio.tv.skin.kicia.KiciaSkin.audioLanguage = if (polishRules != null) "pl" else null
        scope.launch {
            polishSubs = runCatching {
                dagger.hilt.android.EntryPointAccessors.fromApplication(app, SourceChoice.Deps::class.java).player().playerSettings.first().subtitleStyle.preferredLanguage == "pl"
            }.getOrDefault(false)
        }
        // Dawniej odpowiedź włączała tryb Regex, który omija ranking KE: jednorazowo przestaw na „pierwsze źródło”.
        if (answer.isNotEmpty() && answer != SourcePresets.CUSTOM) scope.launch { runCatching { SourceChoice.useRanking(app) } }
        tvCap = tvMaxResolution(app)
        com.nuvio.tv.fork.lektor.LektorVoice.probe(app)
        StartupChecks.warm(app)
        MpvContainers.warm(app)
        scope.launch { decode = runCatching { DecodeCapability.current(app) }.getOrDefault(DecodeCapability.UNKNOWN) }
    }

    /** Fork 19.11: dlaczego wygrało dane źródło (logcat `ForkRanking`), pierwsze 6 pozycji z powodami. */
    private fun logTop(list: List<Stream>) = runCatching {
        android.util.Log.i("ForkRanking", "order: ${list.size} źródeł, polski=${polishRules?.id ?: "-"} napisyPL=$polishSubs oszczędz=$saver preferowane=${preferred != null}")
        list.take(6).forEachIndexed { i, st ->
            val f = ReleaseFactsParser.factsFor(st)
            android.util.Log.i("ForkRanking", "#$i ${st.addonName} | ${f.resolution} | gotowe=${SourceReady.tier(st) == 0} obcyDub=${ForeignDub.tier(st) == 1} pl=${polishTier(st)} pref=${preferred?.matches(st) == true} | " +
                listOfNotNull(st.name, st.title).joinToString(" ").replace('\n', ' ').take(140))
        }
    }

    fun order(streams: List<Stream>): List<Stream> {
        if (!enabled || streams.size < 2) return streams
        return runCatching {
            val id = identity
            val rejected = id?.let { ContentIdentityGuard.rejections(streams, it).keys }.orEmpty()
            val pool = streams.filterNot { it in rejected }.ifEmpty { streams }
            val ranked = StreamQualityRank.rank(pool, filmSizeFloors = id?.film == true)
            val cap = tvCap; val dc = decode
            // W1: limit łącza KE (StreamLimit) — źródła ponad zmierzoną prędkość usługi na koniec (nie znikają).
            val over = appCtx?.let { com.nuvio.tv.fork.net.KeNetwork.overLimit(it, ranked, runtimeMinutes = null, episode = id?.film == false) }.orEmpty()
            val (fits, demoted) = ranked.partition { s ->
                if (s in over) return@partition false
                val facts = ReleaseFactsParser.factsFor(s)
                dc.verdict(facts) != DecodeVerdict.SOFTWARE && (cap == null || !isOverResolutionCap(facts.resolution, cap))
            }
            // Polski dźwięk według odpowiedzi właściciela przed jakością; w obrębie szczebla zostaje kolejność KE (stabilne).
            // Oszczędzanie łącza: w obrębie dekodowanych sprzętowo najpierw najlepszy kodek (AV1 > HEVC > AVC), potem najmniejszy plik.
            val fitsOrdered = if (!saver) fits else fits.sortedWith(compareBy<Stream>({ ReleaseFactsParser.factsFor(it).encode.ordinal }, { ReleaseFactsParser.factsFor(it).size ?: Long.MAX_VALUE }))
            val ordered = (fitsOrdered + demoted).let { list -> if (polishRules == null && !polishSubs) list else list.sortedWith(compareBy(::polishTier, ::subsTier)) }
            // Kicia: źródła, które niedawno nie wystartowały albo nie przeszły kontroli pliku, na koniec (StartupChecks).
            val withBad = StartupChecks.demoteBad(ordered)
            // Fork 19.11 (JMGO: hiszpańskie „Castellano” 720p wygrało z angielskim 1080p, potem niegotowy plik TB⬇️):
            // zapamiętany ręczny wybór (dodatek + rozdzielczość) nie może przebić języka ani gotowości pliku —
            // niegotowe (SourceReady) i obcy dubbing (ForeignDub) zawsze na koniec, kolejność w środku zostaje.
            val picked = preferred?.preferredFirst(withBad)?.first ?: withBad
            picked.sortedWith(compareBy(SourceReady::tier, ForeignDub::tier)).also { logTop(it) }
        }.getOrDefault(streams)
    }
}
