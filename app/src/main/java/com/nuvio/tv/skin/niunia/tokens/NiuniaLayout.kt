package com.nuvio.tv.skin.niunia.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Odstępy i kolory stanów Niunii z decyzji właściciela (WPIECIE.md „Po wdrożeniu” pkt 2–4, canvas v83–v88).
 * Osobny plik, żeby nie mieszać z [NiuniaColors]/[NiuniaDims], które zmienia równolegle inny wątek.
 */
object NiuniaSpace {
    /** Tytuł sekcji/rzędu → rząd (góra tytułu → góra kart). */
    val TitleToRow = 34.dp
    /** Po rzędzie (dół kart → góra następnego tytułu). */
    val AfterRow = 36.dp
    /** Pierwszy tytuł rzędu po zejściu pod hero (od góry ekranu). */
    val TopAfterHero = 40.dp
    /** Pasek postępu ↔ rząd przycisków we wszystkich odtwarzaczach. */
    val PlayerGap = 30.dp
    /** Kafel 16:9 pod odtwarzaczem: 4 kafle + kawałek piątego na 960 dp. */
    val TileW = 200.dp
    val TileH = 113.dp
    val TileGap = 20.dp
    /** Ile kafli wystaje u dołu odtwarzacza, zanim ▼ podniesie rząd. */
    val TilePeek = 22.dp
}

object NiuniaStatus {
    /** Kropka „leci teraz” (= [NiuniaColors.Live]). */
    val Live = Color(0xFFE5484D)
    /** Kropka „jest w archiwum”. */
    val Archive = Color(0xFF30D158)
    /** Odtwarzanie z opóźnieniem (IPTV). */
    val Delay = Color(0xFFF5C542)
}
