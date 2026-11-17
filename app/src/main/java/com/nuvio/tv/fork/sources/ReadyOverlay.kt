package com.nuvio.tv.fork.sources

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nuvio.tv.domain.model.AddonStreams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Linia „Gotowe · 1080p · lektor PL” (B04-6): zasilana z preloadingu źródeł na ekranie szczegółów, czyszczona przy wyjściu z ekranu. */
object ReadyState {
    private val _line = MutableStateFlow<String?>(null)
    val line: StateFlow<String?> = _line

    fun update(groups: List<AddonStreams>) {
        val best = ReadyPick.best(groups)
        val info = ReadyPick.info(groups)
        _line.value = if (best == null || info == null) null
        else SourceLanguage.readyLine(info, listOfNotNull(best.name, best.title, best.description).joinToString(" "))
    }

    fun clear() { _line.value = null }
}

@Composable
fun ReadyOverlay() {
    val text by ReadyState.line.collectAsState()
    val t = text ?: return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomStart) {
        Text(t, color = Color.White, modifier = Modifier.padding(start = 48.dp, bottom = 24.dp).background(Color(0xAA000000)).padding(horizontal = 12.dp, vertical = 6.dp))
    }
}
