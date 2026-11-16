package com.nuvio.tv.ui.screens.settings

import android.content.Context

object ForkFeatures {
    /** Set by NuvioNavHost; opens a route. */
    var open: ((String) -> Unit)? = null
    private fun prefs(c: Context) = c.getSharedPreferences("fork_features", Context.MODE_PRIVATE)
    fun enabled(c: Context, key: String, default: Boolean = false) = prefs(c).getBoolean(key, default)
    fun set(c: Context, key: String, v: Boolean) = prefs(c).edit().putBoolean(key, v).apply()
}

