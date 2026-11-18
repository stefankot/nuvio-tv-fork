package com.nuvio.tv.skin.kicia

/** Switches for the new look, so a part can be turned off in one place if it misbehaves. */
object ForgeUi {
    /** The top bar of the mockup (avatar, search, tabs with counter, screensaver, settings, clock). */
    const val NEW_NAV = true

    /** Home in the look of the mockup (hero text, wide cards, native new episodes and trailer). */
    const val NEW_HOME = true

    /** Profile picker with round avatars and the four-wheel PIN dialog. */
    const val NEW_PROFILES = true

    /** Szukaj of the mockup (S02): pill field, Polish on-screen keyboard, recent pills, result shelves, Odkrywaj grid. */
    const val NEW_SEARCH = true

    /** Biblioteka of the mockup (S05): sub-tabs with counts, focused title above shelves, Historia grid. */
    const val NEW_LIBRARY = true

    /** Player controls of the mockup (S04): header, ScrubBar, centred play / Napisy / Dźwięk / "…" row. Films and episodes only. */
    const val NEW_PLAYER = true

    /** Settings colours of the mockup (S13): rows on surface2, green switches. Layout stays Nuvio's. */
    const val NEW_SETTINGS = true // owner 8.11: mockup look, Nuvio's structure (categories) + the fork's own section

    /** YouTube of the mockup (S10): icon rail + 3-column grid of 242 dp cards, on Nuvio One's view model. */
    const val NEW_YOUTUBE = true

    /** Home's "YouTube dla Ciebie" shelf (a second YouTube feed loader on Home; turn off if the TV struggles). */
    const val HOME_YOUTUBE = true

    /** Live TV channel card of the mockup (S09 MiniGuide look); zapping and keys stay Reshaped's. */
    const val NEW_MINIGUIDE = true

    /** Live TV opens on the guide with recently watched programmes above it (mockup S07 variant A, the default). */
    const val GUIDE_A = true

    /** Live TV opens on the Freely-style front page "Przeglądaj" (mockup S06, variant B; an option in the mockup). */
    const val TV_BROWSE_B = false

    /** Detail screen hero as the mockup draws it (S03): text title, plain "Gotowe" line, one meta line, no ratings row. */
    const val NEW_DETAIL = true

    /** Screensaver as the mockup (S14): clock and date lower-left (y 380), no slide title. */
    const val SAVER_MOCKUP = true

    /** First run as the mockup's wizard card (S16): Nuvio's steps (mode, layout) + "Źródła". */
    const val NEW_WIZARD = true

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
    const val CHANNEL_FRAMES = true // owner 9.11 (later): only for channels whose guide has no picture of the current programme

    /** Szczegóły (mockup S03, owner 9.11): Obsada / Zwiastuny / Podobne / ... one under the other instead of behind tabs. */
    const val NEW_DETAIL_STACK = true

    /** Owner 9.11: false = the focused programme card does not play its channel live (one decoder less on a weak TV). */
    const val LIVE_CARD_PREVIEW = false
}
