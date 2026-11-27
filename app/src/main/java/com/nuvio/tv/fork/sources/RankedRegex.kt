package com.nuvio.tv.fork.sources

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.streams.ranking.RankedRegexSettingsCodec
import com.nuvio.tv.core.streams.ranking.StreamRegexRanking
import com.nuvio.tv.data.local.PlayerSettingsDataStore
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.domain.model.Stream
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Ranking reguł Regex (z Reshaped 0ca5a08c2) po aktualizacji bazy do NuvioTV 1.2.0-beta.4 (26.11).
 *
 * Oficjalne Nuvio ma jedno pole „Regex”; Reshaped dodawał do niego listę reguł z priorytetami (kreator źródeł forka
 * zapisuje tam zestaw „polski dźwięk / jakość”). Pliki bazy są teraz 1:1, więc lista żyje tutaj:
 *  - zapis w tym samym miejscu co w Reshaped (DataStore profilu „player_settings”, klucz `stream_auto_play_ranked_regex`),
 *    więc zapisane już reguły działają dalej bez migracji,
 *  - [active]/[pick] woła jedno wpięcie w `StreamAutoPlaySelector` (tryb Regex): najwyższy pasujący priorytet wygrywa,
 *  - do oficjalnego pola „Regex” trafia suma reguł (albo `(?s).*`), żeby oficjalna kontrola „Regex ustawiony” przepuściła
 *    automatyczny wybór; to też zapas, gdy lista nie zdąży się wczytać.
 */
object RankedRegex {
    private const val TAG = "ForkRankedRegex"
    private const val FEATURE = "player_settings"
    private val KEY = stringPreferencesKey("stream_auto_play_ranked_regex")
    /** Dopasowuje wszystko; zawiera literę, bo oficjalna kontrola wymaga litery albo cyfry. */
    const val MATCH_ALL = "(?s).*"

    @EntryPoint @InstallIn(SingletonComponent::class)
    interface Deps { fun factory(): ProfileDataStoreFactory; fun profiles(): ProfileManager; fun player(): PlayerSettingsDataStore }

    @Volatile private var patterns: List<String> = emptyList()
    @Volatile private var started = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun deps(c: Context) = EntryPointAccessors.fromApplication(c.applicationContext, Deps::class.java)

    /** Raz na proces: trzyma w pamięci listę reguł aktywnego profilu (wpięcie w selektorze nie może czekać na dysk). */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(c: Context) {
        if (started) return
        synchronized(this) {
            if (started) return
            started = true
        }
        val d = deps(c)
        scope.launch {
            runCatching {
                d.profiles().activeProfileId
                    .flatMapLatest { pid -> d.factory().get(pid, FEATURE).data.map { RankedRegexSettingsCodec.decode(it[KEY]) } }
                    .collect { list ->
                        if (list != patterns) Log.i(TAG, "reguły aktywnego profilu: ${list.size}")
                        patterns = list
                    }
            }.onFailure { Log.w(TAG, "wczytanie reguł: ${it.javaClass.simpleName}: ${it.message}") }
        }
    }

    /** Wpięcie w `StreamAutoPlaySelector`: true, gdy profil ma listę reguł (wtedy wybór należy do [pick]). */
    fun active(): Boolean = patterns.isNotEmpty()

    /** Najwyższy pasujący priorytet; null = żadna reguła nie pasuje (Nuvio otwiera wtedy ręczny wybór, jak w Reshaped). */
    fun pick(playable: List<Stream>): Stream? {
        val rules = patterns
        val chosen = runCatching { StreamRegexRanking.rank(playable, rules) {}?.firstOrNull() }
            .onFailure { Log.w(TAG, "ranking: ${it.javaClass.simpleName}: ${it.message}") }
            .getOrNull()
        Log.i(TAG, "wybór: ${playable.size} źródeł, ${rules.size} reguł → ${if (chosen == null) "brak dopasowania" else "\"${chosen.name.orEmpty().take(40)}\""}")
        return chosen
    }

    suspend fun get(c: Context): List<String> {
        val d = deps(c)
        return RankedRegexSettingsCodec.decode(d.factory().get(d.profiles().activeProfileId.value, FEATURE).data.first()[KEY])
    }

    /** Zapis listy (jak `setStreamAutoPlayRankedRegex` w Reshaped) i sumy reguł w oficjalnym polu „Regex”. */
    suspend fun set(c: Context, rules: List<String>) {
        val d = deps(c)
        val stored = RankedRegexSettingsCodec.encode(rules)
        d.factory().get(d.profiles().activeProfileId.value, FEATURE).edit { it[KEY] = stored }
        d.player().setStreamAutoPlayRegex(officialField(rules))
        patterns = RankedRegexSettingsCodec.decode(stored)
        Log.i(TAG, "zapisane reguły: ${rules.size}")
    }

    /** Suma reguł dla oficjalnego pola; gdy Kotlin Regex jej nie przyjmie (składnia biblioteki Reshaped) — [MATCH_ALL]. */
    fun officialField(rules: List<String>): String {
        if (rules.isEmpty()) return MATCH_ALL
        val joined = rules.joinToString("|") { "(?:$it)" }
        return if (runCatching { Regex(joined, RegexOption.IGNORE_CASE) }.isSuccess) joined else MATCH_ALL
    }
}
