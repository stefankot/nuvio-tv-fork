package com.nuvio.tv.skin.api

import com.nuvio.tv.skin.kit.KitSkin

object SkinRegistry {
    fun skinFor(id: SkinId): Skin? = when (id) {
        SkinId.KIT -> KitSkin
        SkinId.KICIA -> com.nuvio.tv.skin.kicia.KiciaSkin
        else -> null
    }
}
