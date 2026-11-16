@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.ui.theme.NuvioTheme

/** Fork adapter: on/off switches (plus an "open" row) for features copied from other forks. */
object ForkFeatures {
    /** Set by NuvioNavHost; opens a route. */
    var open: ((String) -> Unit)? = null
    private fun prefs(c: Context) = c.getSharedPreferences("fork_features", Context.MODE_PRIVATE)
    fun enabled(c: Context, key: String) = prefs(c).getBoolean(key, false)
    fun set(c: Context, key: String, v: Boolean) = prefs(c).edit().putBoolean(key, v).apply()
}

internal fun LazyListScope.forkFeaturesSettingsItems() {
    forkFeatureItem("fork_ke_livetv", "ke_live_tv", "Live TV – katalogi IPTV/EPG (KE)", "Kraje, filtry, źródła M3U", Icons.Default.LiveTv)
}

private fun LazyListScope.forkFeatureItem(key: String, route: String, title: String, subtitle: String, icon: ImageVector, extra: (@androidx.compose.runtime.Composable () -> Unit)? = null) {
    item(key = key) {
        val context = LocalContext.current
        var checked by remember { mutableStateOf(ForkFeatures.enabled(context, key)) }
        Column(verticalArrangement = Arrangement.spacedBy(NuvioTheme.spacing.md)) {
            ToggleSettingsItem(
                icon = icon, title = title, subtitle = subtitle, isChecked = checked,
                onCheckedChange = { checked = it; ForkFeatures.set(context, key, it) },
            )
            if (checked) {
                NavigationSettingsItem(icon = icon, title = "Otwórz: $title", subtitle = "", onClick = { ForkFeatures.open?.invoke(route) })
                extra?.invoke()
            }
        }
    }
}
