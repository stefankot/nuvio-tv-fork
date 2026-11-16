package com.nuvio.tv.core.apikeys

import com.nuvio.tv.BuildConfig

/** Echo shim: Echo reads the TMDB key from ApiKeys; the fork has it in BuildConfig (local.properties). */
object ApiKeys {
    val tmdb: String get() = BuildConfig.TMDB_API_KEY
}
