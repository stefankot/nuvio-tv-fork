package com.nuvio.tv.ui.screens.player.seekpreview

import android.content.Context
import java.io.File

// Nuvio One adapter: only the sprite cache cleanup; storyboard frame source is not ported.
internal object YouTubeStoryboardCache {
    private const val DIR = "youtube-storyboards"
    fun cleanup(context: Context) { runCatching { File(context.cacheDir, DIR).deleteRecursively() } }
    fun directory(context: Context, videoId: String): File = File(File(context.cacheDir, DIR), videoId)
}
