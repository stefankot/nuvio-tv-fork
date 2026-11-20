package com.nuvio.tv.skin.kicia

/** Switches for the new look, so a part can be turned off in one place if it misbehaves. */
object ForgeUi {
    /**
     * Nowy fork: wygląd Kicii działa tylko przy wybranej skórce Kicia (Ustawienia → Wygląd). W Kicii te przełączniki
     * były stałymi `true`; tu czytają wybór skórki, więc łatki w plikach Nuvio/Reshaped nic nie zmieniają bez skórki.
     */
    val ON: Boolean get() = com.nuvio.tv.skin.api.SkinPreferences.skin.value == com.nuvio.tv.skin.api.SkinId.KICIA

    /** The top bar of the mockup (avatar, search, tabs with counter, screensaver, settings, clock). */
    val NEW_NAV: Boolean get() = ON

    /** Home in the look of the mockup (hero text, wide cards, native new episodes and trailer). */
    val NEW_HOME: Boolean get() = ON

    /** Profile picker with round avatars and the four-wheel PIN dialog. */
    val NEW_PROFILES: Boolean get() = ON

    /** Szukaj of the mockup (S02): pill field, Polish on-screen keyboard, recent pills, result shelves, Odkrywaj grid. */
    val NEW_SEARCH: Boolean get() = ON

    /** Biblioteka of the mockup (S05): sub-tabs with counts, focused title above shelves, Historia grid. */
    val NEW_LIBRARY: Boolean get() = ON

    /** Player controls of the mockup (S04): header, ScrubBar, centred play / Napisy / Dźwięk / "…" row. Films and episodes only. */
    val NEW_PLAYER: Boolean get() = ON

    /** Settings colours of the mockup (S13): rows on surface2, green switches. Layout stays Nuvio's. */
    val NEW_SETTINGS: Boolean get() = ON // owner 8.11: mockup look, Nuvio's structure (categories) + the fork's own section

    /** YouTube of the mockup (S10): icon rail + 3-column grid of 242 dp cards, on Nuvio One's view model. */
    val NEW_YOUTUBE: Boolean get() = ON

    /** Home's "YouTube dla Ciebie" shelf (a second YouTube feed loader on Home; turn off if the TV struggles). */
    val HOME_YOUTUBE: Boolean get() = ON

    /** Live TV channel card of the mockup (S09 MiniGuide look); zapping and keys stay Reshaped's. */
    val NEW_MINIGUIDE: Boolean get() = ON || com.nuvio.tv.skin.niunia.live.NiuniaLive.active() // Fork 20.11 (B3-2): Niunia rysuje swój odtwarzacz IPTV na tej samej ścieżce

    /** Live TV opens on the guide with recently watched programmes above it (mockup S07 variant A, the default). */
    val GUIDE_A: Boolean get() = ON

    /** Live TV opens on the Freely-style front page "Przeglądaj" (mockup S06, variant B; an option in the mockup). */
    const val TV_BROWSE_B = false

    /** Detail screen hero as the mockup draws it (S03): text title, plain "Gotowe" line, one meta line, no ratings row. */
    val NEW_DETAIL: Boolean get() = ON

    /** Screensaver as the mockup (S14): clock and date lower-left (y 380), no slide title. */
    val SAVER_MOCKUP: Boolean get() = ON

    /** First run as the mockup's wizard card (S16): Nuvio's steps (mode, layout) + "Źródła". */
    val NEW_WIZARD: Boolean get() = ON

    /**
     * Owner 9.11 ("EPG loads at every start and costs a lot"): false = nothing of Live TV / YouTube is loaded in the background
     * after start (no guide read, no channel frames); Live TV loads its channels and guide when its tab is opened.
     * Home's "Na żywo" and "Nowe odcinki" shelves then fill after the first visit to Live TV. true = the old behaviour.
     */
    const val EAGER_BACKGROUND_LOAD = false // v7 WY-307

    /**
     * Owner 9.11: true = a frame is grabbed from the live stream ONLY when the EPG has no picture of what is on; cards show the EPG's
     * picture first, then a grabbed frame.
     */
    val CHANNEL_FRAMES: Boolean get() = ON // owner 9.11 (later): only for channels whose guide has no picture of the current programme

    /** Szczegóły (mockup S03, owner 9.11): Obsada / Zwiastuny / Podobne / ... one under the other instead of behind tabs. */
    val NEW_DETAIL_STACK: Boolean get() = ON

    /** Owner 9.11: false = the focused programme card does not play its channel live (one decoder less on a weak TV). */
    const val LIVE_CARD_PREVIEW = false
}
