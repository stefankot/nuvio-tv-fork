package com.nuvio.tv.skin.kicia.library

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/** Historia (owner 9.11): watched titles on a screen of their own, opened from the avatar menu. Same data as the old Biblioteka tab. */
@Composable
fun ForgeHistoryScreen(
    onNavigateToDetail: (String, String, String?) -> Unit,
    historyViewModel: ForgeHistoryViewModel = hiltViewModel(),
    metaViewModel: com.nuvio.tv.skin.kicia.home.ForgeHeroMetaViewModel = hiltViewModel(),
) {
    val history by historyViewModel.history.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(ForgeColors.Bg).padding(top = 40.dp)) { // v7.4 (11): full screen without the bar, title at y 40
        Text("Historia", style = ForgeText.title, modifier = Modifier.padding(start = ForgeDims.ContentStart).height(38.dp)) // v7.4 (11): 32/38, meta y 78
        val n = history.size
        Text(
            "$n " + if (n == 1) "pozycja" else if (n % 10 in 2..4 && n % 100 !in 12..14) "pozycje" else "pozycji",
            style = ForgeText.label5, modifier = Modifier.padding(start = ForgeDims.ContentStart).testTag("glowka-meta.0"),
        )
        HistoryGrid(history, metaViewModel, onOpen = { item -> onNavigateToDetail(item.contentId, item.contentType, null) }, onRemove = historyViewModel::remove)
    }
}
