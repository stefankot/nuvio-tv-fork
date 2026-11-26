package com.nuvio.tv.skin.api

/** NUVIO = dzisiejsza aplikacja, NETFLIX = wygląd Reshaped, KIT = własny projekt właściciela, KICIA = wygląd starego forka (nuvio-tv-kicia), NIUNIA = projekt właściciela (canvas). */
enum class SkinId(val key: String) {
    NUVIO("nuvio"), NETFLIX("netflix"), KIT("kit"), KICIA("kicia"), NIUNIA("niunia");

    companion object {
        fun fromKey(key: String?): SkinId = entries.firstOrNull { it.key == key }?.takeUnless { it == NETFLIX } ?: NUVIO // Fork 26.11: wygląd Netflix z Reshaped usunięty razem z bazą 1.2.0-beta.4 (pliki bazy 1:1)
    }
}
