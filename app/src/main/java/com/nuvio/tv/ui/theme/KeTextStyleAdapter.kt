package com.nuvio.tv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// KE adapter: text styles used by com.nuvio.tv.livetv, missing from the fork's NuvioTextStyleTokens.
private fun ke(size: Int, line: Int, w: FontWeight, ls: TextUnit = 0.sp) =
    TextStyle(fontSize = size.sp, lineHeight = line.sp, fontWeight = w, letterSpacing = ls)

val NuvioTextStyleTokens.meta: TextStyle get() = ke(13, 18, FontWeight.Medium)
val NuvioTextStyleTokens.cardLabel: TextStyle get() = ke(13, 18, FontWeight.SemiBold)
val NuvioTextStyleTokens.techBadge: TextStyle get() = ke(10, 16, FontWeight.Bold, 0.6.sp)
val NuvioTextStyleTokens.caption: TextStyle get() = ke(12, 16, FontWeight.Medium)
val NuvioTextStyleTokens.panelStateTitle: TextStyle get() = ke(24, 30, FontWeight.Bold, (-0.3).sp)
val NuvioTextStyleTokens.eyebrow: TextStyle get() = ke(11, 14, FontWeight.SemiBold, 1.8.sp)
val NuvioTextStyleTokens.panelBody: TextStyle get() = ke(13, 19, FontWeight.Normal)
val NuvioTextStyleTokens.panelPill: TextStyle get() = ke(12, 16, FontWeight.SemiBold)
val NuvioTextStyleTokens.panelButtonSmall: TextStyle get() = ke(13, 18, FontWeight.SemiBold)
val NuvioPrimitives.errorOnImage get() = androidx.compose.ui.graphics.Color(0xFFFF8A80)
val NuvioPrimitives.warningOnTint get() = androidx.compose.ui.graphics.Color(0xFFFFCC80)
