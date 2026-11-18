package com.nuvio.tv.skin.kicia.tokens

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nuvio.tv.R

/** Satoshi, only weights 500 and 700 (owner's decision: the light weight was unreadable on TV). */
val SatoshiFamily = FontFamily(
    Font(R.font.satoshi_medium, FontWeight.Medium),
    Font(R.font.satoshi_bold, FontWeight.Bold),
)

/** Size / line height (sp) / weight of one text style. Kept as data so a test can police the rules. */
data class ForgeTextSpec(val size: Float, val line: Float, val weight: Int)

/**
 * Type scale from LAYOUT_SPEC §1.2. Hard rules: nothing under 12 sp, nothing lighter than 500.
 */
object ForgeType {
    val Display = ForgeTextSpec(40f, 44f, 700)
    val Title = ForgeTextSpec(32f, 38f, 700)
    val Head = ForgeTextSpec(20f, 26f, 700)
    val ShelfTitle = ForgeTextSpec(16f, 22f, 700) // v6: shelf title
    val Body = ForgeTextSpec(16f, 22f, 500) // v7.4: treść 16/22
    val Label = ForgeTextSpec(14f, 20f, 700) // v7.4: 14/20
    val Label5 = ForgeTextSpec(14f, 20f, 500)
    val Caption = ForgeTextSpec(12f, 16f, 500)
    val CaptionBold = ForgeTextSpec(12f, 16f, 700)
    val Button = ForgeTextSpec(16f, 22f, 700) // v7.4: przycisk i zakładka 16/22
    val SaverClock = ForgeTextSpec(96f, 100f, 700)

    val all: Map<String, ForgeTextSpec> = mapOf(
        "display" to Display, "title" to Title, "head" to Head, "shelfTitle" to ShelfTitle, "body" to Body, "label" to Label,
        "label5" to Label5, "caption" to Caption, "captionBold" to CaptionBold, "button" to Button,
        "saverClock" to SaverClock,
    )
}

private fun ForgeTextSpec.style(color: androidx.compose.ui.graphics.Color, tracking: Float = 0f) = TextStyle(
    fontFamily = SatoshiFamily,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = if (weight >= 700) FontWeight.Bold else FontWeight.Medium,
    color = color,
    letterSpacing = tracking.em,
    // v7.4: the line box is exactly the token's line height (CSS line-height): no trimming of the first and last line, no font padding
    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.None),
    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
)

object ForgeText {
    val display = ForgeType.Display.style(ForgeColors.Text, tracking = -0.01f)
    val title = ForgeType.Title.style(ForgeColors.Text)
    val head = ForgeType.Head.style(ForgeColors.Text)
    val shelfTitle = ForgeType.ShelfTitle.style(ForgeColors.Text)
    val shelfTitleInactive = ForgeType.ShelfTitle.style(ForgeColors.Text3)
    val body = ForgeType.Body.style(ForgeColors.Text2)
    val label = ForgeType.Label.style(ForgeColors.Text)
    val label5 = ForgeType.Label5.style(ForgeColors.Text2)
    val caption = ForgeType.Caption.style(ForgeColors.Text2)
    val captionBold = ForgeType.CaptionBold.style(ForgeColors.Text)
    val button = ForgeType.Button.style(ForgeColors.Text)
    val saverClock = ForgeType.SaverClock.style(ForgeColors.Text)
}
