package com.nuvio.tv.fork.perf

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Choreographer
import android.view.KeyEvent
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/**
 * Pomiar fokusu pilota (UX JMGO 24.11, `plan/ux/jmgo/PROMPT.md` pkt 3). Przełącznik System › Pomoc › „Pomiar szybkości pilota”
 * (klucz [KEY], domyślnie wyłączony). Wyłączony nic nie kosztuje: każde wywołanie wraca po odczycie jednej flagi [on].
 *
 * Log `ForkFocus` (bez tekstów okien i danych prywatnych — tylko nazwy elementów, jak w `ForkNiunia: OK → …`):
 * - `key=DPAD_RIGHT rep=0 t=<uptime ms>` — klawisz (z ForkHooks.onGlobalKey, bez nowego wpięcia w bazę),
 * - `focus=<nazwa> bounds=[l,t][r,b] dtFocus=<ms od klawisza do onFocusChanged> dtFrame=<ms do końca pierwszej klatki po zmianie>`
 *   (z `niuniaOk` / `niuniaFocusable`; bez klawisza w ostatnich [KEY_WINDOW_MS] ms: dtFocus=-1, dtFrame liczony od zmiany fokusu),
 * - `screen=<trasa> firstFocus=<ms od wejścia na ekran do pierwszego fokusu>` (trasa z nawigatora, ForkFeatures.bind).
 */
object ForkFocusTrace {
    const val KEY = "perf_focus_trace"
    const val TAG = "ForkFocus"
    /** Klawisz starszy niż to okno nie jest przyczyną zmiany fokusu (np. powrót fokusu po wczytaniu danych). */
    const val KEY_WINDOW_MS = 1500L

    @Volatile var on: Boolean = false
        private set
    @Volatile private var keyAt = 0L
    @Volatile private var keyName = ""
    @Volatile private var screen: String? = null
    @Volatile private var screenAt = 0L
    @Volatile private var firstPending = false
    private var prefs: SharedPreferences? = null
    private val watched = java.util.Collections.newSetFromMap(java.util.WeakHashMap<androidx.navigation.NavController, Boolean>())
    private val main by lazy { Handler(Looper.getMainLooper()) }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, k ->
        if (k == KEY) { on = p.getBoolean(KEY, false); Log.i(TAG, "pomiar fokusu: ${if (on) "włączony" else "wyłączony"}") }
    }

    /** ForkHooks.moduleOverlays (raz): stan przełącznika i nasłuch jego zmian. */
    fun init(c: Context) {
        if (prefs != null) return
        on = ForkFeatures.enabled(c, KEY, false)
        prefs = c.applicationContext.getSharedPreferences("fork_features", Context.MODE_PRIVATE).also { it.registerOnSharedPreferenceChangeListener(listener) }
        if (on) Log.i(TAG, "pomiar fokusu: włączony")
    }

    /** Tylko debug (`--ez focus_trace true|false` w intencji com.nuvio.fork.OPEN) — testy bez klikania w ustawieniach. */
    fun set(c: Context, value: Boolean) {
        c.getSharedPreferences("fork_features", Context.MODE_PRIVATE).edit().putBoolean(KEY, value).apply()
        on = value
        Log.i(TAG, "pomiar fokusu: ${if (value) "włączony" else "wyłączony"} (intencja debug)")
    }

    /** ForkFeatures.bind: trasa bieżącego ekranu (słuchacz tylko przy włączonym pomiarze). */
    fun watch(nav: androidx.navigation.NavController) {
        // Słuchacz zawsze (raz na nawigator), żeby włączenie w trakcie działało; przy wyłączonym wraca od razu.
        synchronized(watched) { if (!watched.add(nav)) return }
        nav.addOnDestinationChangedListener { _, dest, _ -> onScreen(dest.route ?: "?") }
    }

    private fun onScreen(route: String) {
        if (!on) return
        screen = FocusTraceLine.route(route); screenAt = SystemClock.uptimeMillis(); firstPending = true
    }

    /** ForkHooks.onGlobalKey: każde wciśnięcie (z powtórzeniami przy przytrzymaniu — rep>0). */
    fun onKey(e: KeyEvent) {
        if (!on || e.action != KeyEvent.ACTION_DOWN) return
        keyAt = e.eventTime
        keyName = KeyEvent.keyCodeToString(e.keyCode).removePrefix("KEYCODE_")
        Log.i(TAG, FocusTraceLine.key(keyName, e.repeatCount, e.eventTime))
    }

    /** niuniaOk / niuniaFocusable: element dostał fokus. [bounds] = l,t,r,b w pikselach okna (null = nieznane). */
    fun focused(name: String, bounds: IntArray?) {
        if (!on) return
        val now = SystemClock.uptimeMillis()
        val k = keyAt
        val dtFocus = FocusTraceLine.dtFocus(k, now, KEY_WINDOW_MS)
        val first = if (firstPending && screenAt > 0) { firstPending = false; now - screenAt } else -1L
        val scr = screen
        val kn = keyName
        try {
            Choreographer.getInstance().postFrameCallback {
                // Wiadomość po klatce: wykona się po rysowaniu tej klatki (pomiar, układ, rysowanie na wątku UI).
                main.post {
                    val end = SystemClock.uptimeMillis()
                    val dtFrame = end - (if (dtFocus >= 0) k else now)
                    Log.i(TAG, FocusTraceLine.focus(name, bounds, dtFocus, dtFrame, kn))
                    if (first >= 0) Log.i(TAG, FocusTraceLine.screen(scr, first))
                }
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "pomiar klatki: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}

/** Czysta logika wpisów `ForkFocus` (testy JVM: `ForkFocusTraceTest`). */
object FocusTraceLine {
    /** dtFocus: ms od klawisza; -1, gdy klawisza nie było albo był dawno (zmiana nie od pilota). */
    fun dtFocus(keyAt: Long, now: Long, window: Long): Long = if (keyAt <= 0L || now < keyAt || now - keyAt > window) -1L else now - keyAt

    /** Nazwa elementu do logu: jedna linia, bez znaków sterujących i spacji na końcach, najwyżej 48 znaków. */
    fun label(name: String): String {
        val one = name.replace(Regex("[\\r\\n\\t]+"), " ").replace(Regex("\\s{2,}"), " ").trim()
        return (if (one.length > 48) one.take(47) + "…" else one).ifEmpty { "?" }
    }

    fun route(r: String): String = r.substringBefore('?').substringBefore('/').take(40).ifEmpty { "?" }

    fun key(name: String, rep: Int, t: Long) = "key=$name rep=$rep t=$t"

    fun focus(name: String, b: IntArray?, dtFocus: Long, dtFrame: Long, key: String): String {
        val bb = if (b != null && b.size == 4) "[${b[0]},${b[1]}][${b[2]},${b[3]}]" else "?"
        return "focus=${label(name)} bounds=$bb dtFocus=$dtFocus dtFrame=$dtFrame" + if (dtFocus >= 0) " key=$key" else ""
    }

    fun screen(route: String?, ms: Long) = "screen=${route ?: "?"} firstFocus=$ms"
}
