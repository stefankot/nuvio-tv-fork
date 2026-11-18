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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var enabled = true
    @Volatile private var decode: DecodeCapability = DecodeCapability.UNKNOWN
    @Volatile private var tvCap: ReleaseResolution? = null

    /** Tytuł, którego dotyczy bieżące szukanie źródeł (ustawiane przez ekran źródeł / odtwarzacz); null = bez strażnika. */
    @Volatile var identity: ContentIdentity? = null

    /** Preferowane źródło bieżącego serialu (KE PreferredSourceDataStore); null = brak. */
    @Volatile var preferred: PreferredSource? = null

    /** Raz po starcie: przełącznik, dekodery urządzenia (zapytanie trwa), największa rozdzielczość telewizora. */
    fun warm(context: Context) {
        val app = context.applicationContext
        enabled = ForkFeatures.enabled(app, KEY, true)
        tvCap = tvMaxResolution(app)
        scope.launch { decode = runCatching { DecodeCapability.current(app) }.getOrDefault(DecodeCapability.UNKNOWN) }
    }

    fun order(streams: List<Stream>): List<Stream> {
        if (!enabled || streams.size < 2) return streams
        return runCatching {
            val id = identity
            val rejected = id?.let { ContentIdentityGuard.rejections(streams, it).keys }.orEmpty()
            val pool = streams.filterNot { it in rejected }.ifEmpty { streams }
            val ranked = StreamQualityRank.rank(pool, filmSizeFloors = id?.film == true)
            val cap = tvCap; val dc = decode
            val (fits, demoted) = ranked.partition { s ->
                val facts = ReleaseFactsParser.factsFor(s)
                dc.verdict(facts) != DecodeVerdict.SOFTWARE && (cap == null || !isOverResolutionCap(facts.resolution, cap))
            }
            val ordered = fits + demoted
            preferred?.preferredFirst(ordered)?.first ?: ordered
        }.getOrDefault(streams)
    }
}
