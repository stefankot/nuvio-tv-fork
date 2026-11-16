package com.nuvio.tv.skin.api

/** NUVIO = dzisiejsza aplikacja, NETFLIX = wygląd Reshaped, KIT = własny projekt właściciela. */
enum class SkinId(val key: String) {
    NUVIO("nuvio"), NETFLIX("netflix"), KIT("kit");

    companion object {
        fun fromKey(key: String?): SkinId = entries.firstOrNull { it.key == key } ?: NUVIO
    }
}
