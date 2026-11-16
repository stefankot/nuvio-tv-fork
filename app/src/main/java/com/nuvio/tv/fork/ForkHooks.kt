package com.nuvio.tv.fork

import android.app.Activity
import android.view.KeyEvent
import com.nuvio.tv.fork.arc.RemoteGlobalHook

/** Jedyne punkty wpięcia forka w pliki rdzenia. Każda funkcja wraca natychmiast, gdy funkcja jest wyłączona. */
object ForkHooks {
    /** MainActivity.dispatchKeyEvent: pilot Arc poza odtwarzaczem. true = klawisz obsłużony. */
    fun onGlobalKey(activity: Activity, event: KeyEvent): Boolean = RemoteGlobalHook.onKey(activity, event)
}
