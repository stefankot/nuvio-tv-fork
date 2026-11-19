package com.nuvio.tv.fork.echo

import android.util.Log
import com.nuvio.tv.ui.screens.player.sourceSwitch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fork 19.11 (build115, JMGO: „aplikacja się zamknęła” = ANR): Echo Auto Quality przeliczało języki ścieżek
 * wszystkich źródeł (StreamAudioParser, regex ICU) na wątku głównym przy każdej zmianie listy źródeł — na JMGO
 * ponad 5 s. Tu liczone w tle; takt automatycznej jakości czeka (pomija decyzję), aż wynik będzie gotowy.
 */
object AutoQualityBackground {
    private const val TAG = "ForkEcho"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var pending: Pair<Any, Any?>? = null

    /** Uruchamia [compute] w tle dla danej listy źródeł i języka (raz na parę); [onReady] dostaje wynik. */
    fun <Q> start(streams: Any, language: Any?, compute: () -> Q, onReady: (Q) -> Unit) {
        val key = streams to language
        val p = pending
        if (p != null && p.first === streams && p.second == language) return
        pending = key
        scope.launch {
            val t0 = System.currentTimeMillis()
            runCatching(compute)
                .onSuccess { onReady(it); Log.i(TAG, "auto jakość: języki źródeł policzone w tle w ${System.currentTimeMillis() - t0} ms") }
                .onFailure { Log.w(TAG, "auto jakość w tle: ${it.javaClass.simpleName}: ${it.message?.take(200)}") }
            if (pending === key) pending = null
        }
    }

    /**
     * Wiele rdzeni (19.11, pomiar JMGO: do 14,5 s na jednym wątku): filtr źródeł po języku na wszystkich rdzeniach
     * (`parallelStream`, kolejność zachowana). Parser języków ma zsynchronizowaną pamięć podręczną — bezpieczny wątkowo.
     * Przy błędzie równoległym: wpis w logu i zwykły filtr.
     */
    fun <T> parallelFilter(items: List<T>, keep: (T) -> Boolean): List<T> {
        if (items.size < 8) return items.filter(keep)
        val t0 = System.currentTimeMillis()
        return runCatching { items.parallelStream().filter { keep(it) }.collect(java.util.stream.Collectors.toList()) as List<T> }
            .onSuccess { Log.i("ForkEcho", "auto jakość: filtr języka ${items.size} źródeł na ${Runtime.getRuntime().availableProcessors()} rdzeniach w ${System.currentTimeMillis() - t0} ms") }
            .getOrElse { e -> Log.w("ForkEcho", "parallelFilter: ${e.javaClass.simpleName}: ${e.message?.take(200)}"); items.filter(keep) }
    }
}

/** Fork 19.11: czy trwa przełączanie źródła Echo (auto jakość / zmiana dźwięku) — wtedy hook „polski dźwięk” nie szuka dalej. */
internal fun isSourceSwitching(c: com.nuvio.tv.ui.screens.player.PlayerRuntimeController): Boolean = c.sourceSwitch != null
