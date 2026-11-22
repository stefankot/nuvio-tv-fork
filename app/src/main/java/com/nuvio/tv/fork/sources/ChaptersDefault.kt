package com.nuvio.tv.fork.sources

import android.content.Context
import android.util.Log
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException

/**
 * Rozdziały (22.11, właściciel: „teraz ich nie ma — natywne nie obsługują czy tylko wyłączone?”).
 * Sprawdzone: oba odtwarzacze je obsługują (ExoPlayer czyta rozdziały Matroska przez HTTP — MatroskaChapterLoader,
 * mpv podaje listę rozdziałów), pasek i panel rozdziałów są gotowe; Nuvio ma tylko domyślnie wyłączone ustawienie
 * „Rozdziały” (`exoChaptersEnabled = false`). Włączamy je raz przy pierwszym starcie tej wersji; późniejszy wybór
 * użytkownika w Ustawienia → Odtwarzanie zostaje (nie nadpisujemy go drugi raz).
 */
object ChaptersDefault {
    private const val DONE = "chapters_default_v1"

    suspend fun ensure(c: Context) {
        if (ForkFeatures.str(c, DONE).isNotEmpty()) return
        try {
            val store = EntryPointAccessors.fromApplication(c.applicationContext, SourceChoice.Deps::class.java).player()
            store.setExoChaptersEnabled(true)
            ForkFeatures.setStr(c, DONE, "on")
            Log.i("ForkSources", "rozdziały: włączone domyślnie (pasek postępu i panel rozdziałów, Exo i mpv)")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("ForkSources", "rozdziały: włączenie nie wyszło: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
