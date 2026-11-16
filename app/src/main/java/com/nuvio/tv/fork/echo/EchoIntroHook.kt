package com.nuvio.tv.fork.echo

import android.view.KeyEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import com.nuvio.tv.core.intro.PlaybackIntroState
import com.nuvio.tv.ui.screens.player.PlaybackIntroHost
import com.nuvio.tv.ui.screens.settings.ForkFeatures

/** Echo A6: wpięcie intro odtwarzania (kod Echo 1:1 w PlaybackIntro*.kt). Przełącznik: echo_playback_intro. */
object EchoIntroHook {
    @Composable
    private fun enabled(): Boolean = ForkFeatures.enabled(LocalContext.current, "echo_playback_intro")

    @Composable
    fun Wrap(content: @Composable () -> Unit) {
        if (!enabled()) { content(); return }
        Box(
            modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                if (!PlaybackIntroState.active.value) return@onPreviewKeyEvent false
                if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK &&
                    event.nativeKeyEvent.action == KeyEvent.ACTION_UP
                ) PlaybackIntroState.finish()
                true
            }
        ) {
            content()
            PlaybackIntroHost()
        }
    }

    @Composable
    fun BeforePlayer(autoPlayDecided: Boolean, showDirectAutoPlayOverlay: Boolean) {
        val on = enabled()
        var startedHere by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(autoPlayDecided, showDirectAutoPlayOverlay) {
            if (on && !startedHere && autoPlayDecided && showDirectAutoPlayOverlay) {
                startedHere = true
                PlaybackIntroState.startBeforePlayer()
            }
        }
    }

    @Composable
    fun OnPlayerOpen() {
        val on = enabled()
        LaunchedEffect(Unit) { if (on) PlaybackIntroState.startOnPlayerOpen() }
    }
}
