package com.nuvio.tv.skin.api

/** NUVIO = dzisiejsza aplikacja, NETFLIX = wygląd Reshaped, KIT = własny projekt właściciela, KICIA = wygląd starego forka (nuvio-tv-kicia). */
enum class SkinId(val key: String) {
    NUVIO("nuvio"), NETFLIX("netflix"), KIT("kit"), KICIA("kicia");

    companion object {
        fun fromKey(key: String?): SkinId = entries.firstOrNull { it.key == key } ?: NUVIO
    }
}
