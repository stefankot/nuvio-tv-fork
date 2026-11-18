package com.nuvio.tv.skin.kicia.nav

/**
 * What the Back button does, in one fixed order (owner's decision, LAYOUT_SPEC §2):
 * overlay -> screen's own state (preview on the bar, card, guide history -> now) -> previous screen
 * -> navigation bar -> Home -> "Close the app?" dialog.
 */
enum class ForgeBackAction {
    CloseExitDialog, // "Zostań"
    CloseOverlay,
    ResetScreenState,
    PopScreen,
    FocusNavBar,
    GoHome,
    ShowExitDialog,
}

data class ForgeBackState(
    val exitDialogOpen: Boolean = false,
    val overlayOpen: Boolean = false,
    val screenHasOwnState: Boolean = false,
    val hasPreviousScreen: Boolean = false,
    val focusIsInContent: Boolean = true,
    val onHomeTab: Boolean = true,
)

object ForgeBackPolicy {
    fun resolve(s: ForgeBackState): ForgeBackAction = when {
        s.exitDialogOpen -> ForgeBackAction.CloseExitDialog
        s.overlayOpen -> ForgeBackAction.CloseOverlay
        s.screenHasOwnState -> ForgeBackAction.ResetScreenState
        s.hasPreviousScreen -> ForgeBackAction.PopScreen
        s.focusIsInContent -> ForgeBackAction.FocusNavBar
        !s.onHomeTab -> ForgeBackAction.GoHome
        else -> ForgeBackAction.ShowExitDialog
    }
}

/**
 * Remembers which element we left, per screen, so Back puts focus back exactly there.
 * The key is whatever identifies an element on that screen (spec: data-spec + index + key).
 */
class ForgeFocusMemory {
    private val saved = HashMap<String, String>()

    fun remember(screen: String, elementKey: String) {
        saved[screen] = elementKey
    }

    fun recall(screen: String): String? = saved[screen]

    fun forget(screen: String) {
        saved.remove(screen)
    }
}
