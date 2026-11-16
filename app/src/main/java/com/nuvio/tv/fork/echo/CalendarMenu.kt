package com.nuvio.tv.fork.echo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.nuvio.tv.R
import com.nuvio.tv.ui.screens.settings.ForkFeatures
import com.nuvio.tv.DrawerItem

const val CALENDAR_ROUTE = "calendar"

/** Pozycja menu Kalendarz (Echo). null, gdy przełącznik `fork_calendar` jest wyłączony. */
@Composable
fun rememberCalendarMenuItem(): DrawerItem? {
    val context = LocalContext.current
    val label = stringResource(R.string.nav_calendar)
    return remember(label) {
        if (ForkFeatures.enabled(context, "fork_calendar", default = true))
            DrawerItem(route = CALENDAR_ROUTE, label = label, icon = Icons.Default.CalendarMonth)
        else null
    }
}
