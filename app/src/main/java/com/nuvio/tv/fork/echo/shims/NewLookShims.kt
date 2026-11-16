package com.nuvio.tv.ui.newlook

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/** Echo shims (Echo "new look" visuals are not ported): neutral versions with Echo's signatures. */
internal fun Modifier.glassSurface(shape: Shape, glints: Boolean = true, shadow: Boolean = true, body: Boolean = true): Modifier =
    this.clip(shape).background(Color.White.copy(alpha = 0.08f))

internal fun Modifier.underSidebar(): Modifier = this

/** Echo toast/island notifications: not ported; posting does nothing. */
object NewLookNotifications {
    enum class Kind { CALENDAR, REMINDER, UPDATE, SAMPLE, PLAYER, QUALITY_UP, QUALITY_DOWN }

    fun post(kind: Kind, title: String, text: String): Long = 0L
}
