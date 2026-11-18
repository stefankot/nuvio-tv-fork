package com.nuvio.tv.skin.kicia.nav

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Lets a screen ask the top bar to switch to another tab (e.g. a channel tile on Home opens Live TV)
 * without that screen knowing about the navigation controller. The bar collects [requests].
 */
object ForgeNavRequests {
    private val flow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val requests: SharedFlow<String> = flow

    fun open(route: String) {
        flow.tryEmit(route)
    }
}
