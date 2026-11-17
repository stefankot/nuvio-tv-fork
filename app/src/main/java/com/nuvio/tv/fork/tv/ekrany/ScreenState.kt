package com.nuvio.tv.fork.tv.ekrany

import com.nuvio.tv.fork.tv.Badge
import com.nuvio.tv.fork.tv.QueueItem

/** Stan chowania rzędu „Dla Ciebie” nad siatką przewodnika (B03-1). */
enum class Zone { NAV_BAR, ROW, CONTROLS, GRID }

class GuideHeaderState {
    var zone: Zone = Zone.NAV_BAR
        private set
    var gridRow: Int = 0
        private set

    /** Rząd kart widać wszędzie poza siatką. */
    val rowVisible: Boolean get() = zone != Zone.GRID

    fun focusGrid(row: Int) { zone = Zone.GRID; gridRow = row }
    fun focusControls() { zone = Zone.CONTROLS }
    fun focusRow() { zone = Zone.ROW }

    /** ▲ z pierwszego wiersza siatki przywraca rząd; z kolejnych tylko przesuwa wiersz. */
    fun up(): Zone {
        if (zone == Zone.GRID) { if (gridRow <= 0) zone = Zone.ROW else gridRow-- }
        else if (zone == Zone.CONTROLS) zone = Zone.ROW
        return zone
    }

    /** ▼ z paska nawigacji ląduje na pierwszej karcie (jeśli rząd ma karty), inaczej na sterowaniu. */
    fun downFromNavBar(hasCards: Boolean): Zone { zone = if (hasCards) Zone.ROW else Zone.CONTROLS; return zone }

    fun down(): Zone {
        when (zone) { Zone.ROW -> zone = Zone.CONTROLS; Zone.CONTROLS -> { zone = Zone.GRID; gridRow = 0 }; Zone.GRID -> gridRow++; else -> Unit }
        return zone
    }
}

/** Akcja po OK na karcie (B03-2). */
sealed interface CardAction {
    data class Resume(val item: QueueItem) : CardAction
    data class PlayLive(val item: QueueItem) : CardAction
    data class PlayFromStart(val item: QueueItem) : CardAction
}

enum class CardMenu { FROM_START, FOLLOW, REMIND, MARK_WATCHED, PROGRAM_PAGE }

object CardActions {
    fun onOk(item: QueueItem, nowMs: Long, archiveAvailable: Boolean): CardAction = when (item.badge) {
        Badge.RESUME -> CardAction.Resume(item)
        Badge.NEW_EPISODE -> if (item.startMs > nowMs - 1 && item.startMs <= nowMs) CardAction.PlayLive(item) else if (archiveAvailable) CardAction.PlayFromStart(item) else CardAction.PlayLive(item)
        Badge.LIVE -> CardAction.PlayLive(item)
    }

    /** Menu długiego OK; „Od początku” tylko gdy kanał ma archiwum. */
    fun menu(archiveAvailable: Boolean): List<CardMenu> = buildList {
        if (archiveAvailable) add(CardMenu.FROM_START)
        add(CardMenu.FOLLOW); add(CardMenu.REMIND); add(CardMenu.MARK_WATCHED); add(CardMenu.PROGRAM_PAGE)
    }
}
