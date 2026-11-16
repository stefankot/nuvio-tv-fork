package com.nuvio.tv.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Arc adapter: where the fork keeps the remote-button map (Arc keeps it in DeviceLocalPlayerPreferences). */
object RemoteButtonStore {
    private const val PREFS = "arc_remote_buttons"
    private const val KEY = "remote_buttons"
    private val _map = MutableStateFlow(RemoteButtonMap.DEFAULT)
    val map: StateFlow<RemoteButtonMap> = _map.asStateFlow()
    private var loaded = false

    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        _map.value = RemoteButtonMap.decode(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))
    }

    fun set(context: Context, map: RemoteButtonMap) {
        _map.value = map
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (map == RemoteButtonMap.DEFAULT) remove(KEY) else putString(KEY, map.encode())
        }.apply()
    }
}
