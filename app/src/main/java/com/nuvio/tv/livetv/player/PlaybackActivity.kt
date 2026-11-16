package com.nuvio.tv.livetv.player

import com.nuvio.tv.core.player.PlayerSessionTracker
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

// KE adapter: a background scan waits while the fork's player is open.
@Singleton
class PlaybackActivity @Inject constructor() {
    val playing: Flow<Boolean> = PlayerSessionTracker.isSessionOpen
}
