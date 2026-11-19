package com.nuvio.tv.fork.tvdb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.fork.logged

/** W8: zapasowy obraz karty programu (półki Live TV i Home) z TVDB: zdjęcie odcinka, inaczej tło/plakat serialu. Null do czasu odpowiedzi. */
@Composable
fun rememberTvdbCardImage(title: String, episode: String?): String? {
    val c = LocalContext.current
    val url by produceState<String?>(null, title, episode) { value = runCatching { Tvdb.cardImage(c.applicationContext, title, episode) }.logged("ForkTvdb", "TvdbCompose.kt:12").getOrNull() }
    return url
}
