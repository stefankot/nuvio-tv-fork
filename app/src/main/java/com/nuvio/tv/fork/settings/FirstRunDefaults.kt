package com.nuvio.tv.fork.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.data.local.ProfileDataStoreFactory
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/** Domyślne wartości pierwszego uruchomienia profilu. Nie zmienia tego, co użytkownik już zapisał. */
object FirstRunDefaults {
    @EntryPoint @InstallIn(SingletonComponent::class)
    interface Deps { fun factory(): ProfileDataStoreFactory; fun profiles(): ProfileManager }

    const val LEVEL_KEY = "fork_settings_level"

    suspend fun run(c: Context) {
        val app = c.applicationContext
        val d = EntryPointAccessors.fromApplication(app, Deps::class.java)
        val pid = d.profiles().activeProfileId.value
        val flag = "first_run_done_$pid"
        if (ForkFeatures.enabled(app, flag)) return
        val en = booleanPreferencesKey("tmdb_enabled"); val lang = stringPreferencesKey("tmdb_language")
        d.factory().get(pid, "tmdb_settings").edit { p ->
            val r = FirstRunPlan.decide(FirstRunPlan.Stored(p[en], p[lang], null, null))
            r.tmdbEnabled?.let { p[en] = it }; r.tmdbLanguage?.let { p[lang] = it }
        }
        val locPrefs = app.getSharedPreferences("app_locale", Context.MODE_PRIVATE)
        val r = FirstRunPlan.decide(FirstRunPlan.Stored(true, "x", locPrefs.getString("locale_tag", null), ForkFeatures.str(app, LEVEL_KEY).ifEmpty { null }))
        r.appLocale?.let { locPrefs.edit().putString("locale_tag", it).apply() }
        r.level?.let { ForkFeatures.setStr(app, LEVEL_KEY, it) }
        ForkFeatures.set(app, flag, true)
    }
}
