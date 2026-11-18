package com.nuvio.tv.skin.kicia.profile

/**
 * The PIN entry of the mockup: four digits, each a little wheel. Up raises the digit, Down lowers it
 * (0-9, wrapping), Left/Right move between digits, OK confirms. Logic only; the dialog draws it.
 */
class ForgePinWheel(val length: Int = 4) {
    private val digits = IntArray(length)

    var index: Int = 0
        private set

    fun digit(at: Int): Int = digits[at]

    fun up(at: Int = index) {
        digits[at] = (digits[at] + 1) % 10
    }

    fun down(at: Int = index) {
        digits[at] = (digits[at] + 9) % 10
    }

    fun moveLeft() {
        if (index > 0) index--
    }

    fun moveRight() {
        if (index < length - 1) index++
    }

    fun focus(at: Int) {
        index = at.coerceIn(0, length - 1)
    }

    fun pin(): String = digits.joinToString("")

    fun clear() {
        digits.fill(0)
        index = 0
    }
}
