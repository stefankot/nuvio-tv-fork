package com.nuvio.tv.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/** Echo shim: focus highlight for rows and chips (Echo draws glass; here a plain tint). */
internal fun Modifier.selectionSurface(focused: Boolean, shape: Shape, idle: Color = Color.Transparent, lifted: Boolean = true): Modifier =
    if (focused) clip(shape).background(Color.White.copy(alpha = 0.18f)) else clip(shape).background(idle)
