package com.nuvio.tv.skin.api

import com.nuvio.tv.skin.kit.KitSkin

object SkinRegistry {
    fun skinFor(id: SkinId): Skin? = when (id) {
        SkinId.KIT -> KitSkin
        else -> null
    }
}
