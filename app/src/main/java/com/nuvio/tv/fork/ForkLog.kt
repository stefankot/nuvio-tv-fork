package com.nuvio.tv.fork

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

/**
 * Zasada stała właściciela (19.11): wszystkie moduły zapisują do logu, KAŻDY błąd z typem i komunikatem,
 * bez połkniętych wyjątków. Logcat procesu zbiera FullLog (fork/family) i wysyła do panelu.
 *
 * Użycie: `runCatching { … }.logged("ForkXxx", "Plik:linia").getOrNull()` — zachowanie bez zmian, dochodzi tylko wpis.
 * Bez kluczy, tokenów i tekstu dialogów (komunikat wyjątku skracany do 200 znaków).
 */
fun <T> Result<T>.logged(tag: String, what: String): Result<T> = onFailure { e ->
    Log.w(tag, "$what: ${e.javaClass.simpleName}: ${e.message?.take(200)}")
}

object ForkLog {
    fun i(tag: String, msg: String) { Log.i(tag, msg) }

    @Volatile private var lastJankMs = 0L
    @Volatile private var skippedJanks = 0

    /**
     * Optymalizacja logów (19.11, właściciel „Tak, dodaj”): JankStats z bazy pisał długą ścieżkę ekranu przy każdym
     * przycięciu. Teraz tylko klatki ≥ 100 ms i najwyżej 1 wpis / 5 s; pominięte zliczane w jednym wpisie.
     */
    fun jankWorthLogging(isJank: Boolean, frameNanos: Long): Boolean {
        if (!isJank) return false
        if (frameNanos < 100_000_000L) { skippedJanks++; return false }
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastJankMs < 5_000L) { skippedJanks++; return false }
        lastJankMs = now
        if (skippedJanks > 0) Log.i("ForkJank", "pominięte drobne/powtórzone przycięcia: $skippedJanks")
        skippedJanks = 0
        return true
    }

    fun w(tag: String, what: String, e: Throwable) {
        Log.w(tag, "$what: ${e.javaClass.simpleName}: ${e.message?.take(200)}")
    }

    /** Błąd odczuwalny przez użytkownika: wpis w logu + krótki komunikat na ekranie (z dowolnego wątku). */
    fun say(context: Context, tag: String, msg: String) {
        Log.w(tag, "ekran: $msg")
        val app = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            // Szkło włączone i nakładka na ekranie: komunikat jako pigułka ze szkła (fork/glass), inaczej Toast systemu.
            if (com.nuvio.tv.fork.glass.ForkGlass.showToast(msg)) return@post
            runCatching { Toast.makeText(app, msg, Toast.LENGTH_LONG).show() }
                .onFailure { Log.w(tag, "Toast: ${it.javaClass.simpleName}: ${it.message}") }
        }
    }
}
