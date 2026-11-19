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

    fun w(tag: String, what: String, e: Throwable) {
        Log.w(tag, "$what: ${e.javaClass.simpleName}: ${e.message?.take(200)}")
    }

    /** Błąd odczuwalny przez użytkownika: wpis w logu + krótki komunikat na ekranie (z dowolnego wątku). */
    fun say(context: Context, tag: String, msg: String) {
        Log.w(tag, "ekran: $msg")
        val app = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            runCatching { Toast.makeText(app, msg, Toast.LENGTH_LONG).show() }
                .onFailure { Log.w(tag, "Toast: ${it.javaClass.simpleName}: ${it.message}") }
        }
    }
}
