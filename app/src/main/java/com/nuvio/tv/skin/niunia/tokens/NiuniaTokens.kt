package com.nuvio.tv.skin.niunia.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tokeny skórki Niunia (projekt właściciela, canvas „Nuvio TV Apple TV Style App”, plik nuvio.css).
 * Plansza 960x540 dp: 1 px makiety = 1 dp = 1 sp.
 */
object NiuniaColors {
    val Bg = Color(0xFF2C2C2E)
    val Surface = Color(0xFF3A3A3C)
    val Surface2 = Color(0xFF3D3D3F)
    val Focus = Color(0xFFF2F2F2)
    val OnFocus = Color(0xFF111111)
    val Text = Color(0xFFFFFFFF)
    val Text70 = Color(0xB3FFFFFF)
    val Text50 = Color(0x80FFFFFF)
    val Fill12 = Color(0x1FFFFFFF)
    val Fill24 = Color(0x3DFFFFFF)
    val Fill36 = Color(0x5CFFFFFF)
    val Line = Color(0x1FFFFFFF)
    val Live = Color(0xFFE5484D)
    val Chrome = Color(0x9E28282A)
    val Btn = Color(0x8C161618)
    val Badge = Color(0x80000000)
    val ScrimLeft = Color(0xE618181A)
}

object NiuniaDims {
    val Gutter = 40.dp
    val RowGap = 20.dp
    val RadiusCard = 12.dp
    val RadiusTab = 8.dp
    val PosterW = 130.dp
    val PosterH = 195.dp
    val WideW = 160.dp
    val WideH = 114.dp
    val SpotW = 280.dp
    val SpotH = 350.dp
    val ButtonH = 40.dp
    const val ScaleFocus = 1.10f
    const val ScaleButton = 1.06f
}

object NiuniaText {
    val Logo = TextStyle(fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, color = NiuniaColors.Text)
    val Row = TextStyle(fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
    val Body = TextStyle(fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = NiuniaColors.Text70)
    val Meta = TextStyle(fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, color = NiuniaColors.Text)
    val Button = TextStyle(fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold)
    val Tab = TextStyle(fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold)
    val PosterTitle = TextStyle(fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, color = NiuniaColors.Text, letterSpacing = 0.8.sp)
    // SKIN.md 20.11: stopka plakatu 12/16 bold, biel 90 % z miękkim cieniem; plakat „spotlight” 14/18.
    val PosterMeta = TextStyle(fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xE6FFFFFF),
        shadow = androidx.compose.ui.graphics.Shadow(Color(0x80000000), androidx.compose.ui.geometry.Offset(0f, 1f), 4f))
    val PosterMetaLarge = PosterMeta.copy(fontSize = 14.sp, lineHeight = 18.sp)
    val Caption = TextStyle(fontFamily = com.nuvio.tv.skin.kicia.tokens.SatoshiFamily, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, color = NiuniaColors.Text70)
}
