package com.nuvio.tv.fork.tv.plus

import android.util.Log

/**
 * Zastępniki dwóch zależności kopii silnika NuvioPlusIPTV (żeby skopiowane pliki zostały 1:1):
 * - `LiveTvLoadReport.add` (ich raport wczytywania na ekranie) → log `ForkTvPlus`;
 * - `LiveTvSearchBridge.smallWords` — kopia 1:1 (NuvioPlusIPTV @ e5643387, `livetv/ui/LiveTvSearch.kt` w. 447–456).
 */
internal object PlusShims {
    private const val TAG = "ForkTvPlus"

    fun report(text: String) { Log.i(TAG, text) }

    private const val SMALL = "ᴬᴮᴰᴱᴳᴴᴵᴶᴷᴸᴹᴺᴼᴾᴿᵀᵁⱽᵂᵃᵇᶜᵈᵉᶠᵍʰⁱʲᵏˡᵐⁿᵒᵖʳˢᵗᵘᵛʷˣʸᶻᶦᶫᶰᶸᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘʀꜱᴛᴜᴠᴡʏᴢ⁰¹²³⁴⁵⁶⁷⁸⁹⁺"
    private const val PLAIN = "abdeghijklmnoprtuvwabcdefghijklmnoprstuvwxyzilnuabcdefghijklmnoprstuvwyz0123456789+"
    private val smallRun = Regex("[$SMALL]+(?:\\s*[$SMALL]+)*")

    private fun plainOf(run: String): String = buildString {
        run.forEach { ch -> val i = SMALL.indexOf(ch); if (i >= 0) append(PLAIN[i]) }
    }

    /** The small raised words in [raw], as plain lower-case words ("ᴸᴵⱽᴱ" -> "live"). */
    fun smallWords(raw: String): List<String> = smallRun.findAll(raw).map { plainOf(it.value) }.toList()
}
