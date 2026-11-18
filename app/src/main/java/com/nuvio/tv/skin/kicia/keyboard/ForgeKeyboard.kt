package com.nuvio.tv.skin.kicia.keyboard

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import com.nuvio.tv.skin.kicia.components.ForgeFocusBox
import com.nuvio.tv.skin.kicia.tokens.ForgeColors
import com.nuvio.tv.skin.kicia.tokens.ForgeDims
import com.nuvio.tv.skin.kicia.tokens.ForgeText

/** What a key on the on-screen keyboard does. */
sealed interface ForgeKey {
    data class Char(val value: String) : ForgeKey
    data object Space : ForgeKey
    data object Delete : ForgeKey
    data object Clear : ForgeKey
    data object Search : ForgeKey
}

/** Key grid. Characters only; the action row (Spacja, Usuń, Wyczyść, Szukaj) is added by the UI. */
data class ForgeKeyboardLayout(
    val rows: List<List<String>>,
    val keyWidth: Dp,
    val keyHeight: Dp,
    val gap: Dp,
    /** v7.4: the "123" page (digits, Polish letters, signs); null = a single page. */
    val altRows: List<List<String>>? = null,
) {
    val columns: Int get() = rows.maxOf { it.size }

    companion object {
        /** Search (S02): 12 columns, Polish letters in alphabetical order, then digits and . - ' */
        val Search12 = ForgeKeyboardLayout(
            rows = listOf(
                "a ą b c ć d e ę f g h i".split(' '),
                "j k l ł m n ń o ó p q r".split(' '),
                "s ś t u v w x y z ź ż .".split(' '),
                "1 2 3 4 5 6 7 8 9 0 - '".split(' '),
            ),
            keyWidth = 63.dp, keyHeight = 34.dp, gap = 8.dp,
        )

        /** v6 (Apple TV): left-hand keyboard, 6 columns, a-z + Polish letters + digits, key 34x36 dp. Not wired into S02 yet. */
        val Search6 = ForgeKeyboardLayout(
            // The mockup's order (S02): a-z, then ą ż ć ę ł ń ó ś ź, then 1-9 and 0.
            // v7.4 (09): page "abc" = a-z (6 x 5, W-88), page "123" = digits, the Polish letters (a compromise: the mockup has none) and signs; key 36 x 36, gap 2.
            rows = "abcdefghijklmnopqrstuvwxyz".map { it.toString() }.chunked(6),
            keyWidth = 36.dp, keyHeight = 36.dp, gap = 2.dp,
            altRows = "1234567890ąćęłńóśźż.-'".map { it.toString() }.chunked(6),
        )

        /** YouTube search (S11): 9 columns. */
        val YouTube9 = ForgeKeyboardLayout(
            rows = "abcdefghijklmnopqrstuvwxyząćęłńóśźż0123456789".map { it.toString() }.chunked(9),
            keyWidth = 34.dp, keyHeight = 32.dp, gap = 6.dp,
        )
    }
}

/** Text-editing rules of the keyboard, kept apart from the UI so they can be tested. */
object ForgeKeyboardInput {
    fun apply(text: String, key: ForgeKey, maxLength: Int = 120): String = when (key) {
        is ForgeKey.Char -> if (text.length >= maxLength) text else text + key.value
        ForgeKey.Space -> if (text.isEmpty() || text.endsWith(" ") || text.length >= maxLength) text else "$text "
        ForgeKey.Delete -> text.dropLast(1)
        ForgeKey.Clear -> ""
        ForgeKey.Search -> text
    }
}

/**
 * On-screen keyboard panel for the TV: a live preview of the typed text (head style), the key grid and
 * a row with Spacja / Usuń / Wyczyść / Szukaj. OK on a key types it; Back is handled by the host
 * (closes the keyboard, the text stays).
 */
@Composable
fun ForgeOnScreenKeyboard(
    text: String,
    onTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    layout: ForgeKeyboardLayout = ForgeKeyboardLayout.Search12,
    firstKeyFocus: FocusRequester? = null,
    /** Keys only, as in YouTube search (S11): no sheet, no typed line, no Szukaj key (the field above shows the text). */
    bare: Boolean = false,
) {
    var page by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val docked = layout.columns <= 6 && !bare
    val shownRows = if (page == 1 && layout.altRows != null) layout.altRows else layout.rows
    // v7.4 (09): entering the board from outside (▼ from the bar) lands on the key "a", not on the abc | 123 switch
    val enterOnFirstKey = if (firstKeyFocus != null && docked) Modifier.focusProperties {
        @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
        enter = { _ -> firstKeyFocus ?: FocusRequester.Default }
    }.focusGroup() else Modifier
    Column(
        modifier = if (bare) modifier else modifier
            .then(enterOnFirstKey)
            .then(if (layout.columns <= 6) Modifier else Modifier.clip(RoundedCornerShape(ForgeDims.RadiusSheet)).background(ForgeColors.Sheet)) // v6 mockup: the docked 6-column board has no sheet
            .padding(horizontal = if (layout.columns <= 6) 0.dp else 42.dp, vertical = if (layout.columns <= 6) 2.dp else 16.dp), // v6: the 6-column board is docked to the left; v7.4 (09): 2 dp → abc|123 y 96, klawisze y 144
        verticalArrangement = Arrangement.spacedBy(layout.gap),
    ) {
        if (!bare && layout.columns > 6) Text(
            text = text.ifEmpty { " " },
            style = ForgeText.head,
            maxLines = 1,
            modifier = Modifier.height(26.dp),
        )
        if (docked && layout.altRows != null) {
            // v7.4: Pigułka "abc | 123" the width of the column (226 dp); the focus switches the page at once
            Row(Modifier.width(226.dp).height(36.dp).clip(RoundedCornerShape(18.dp)).background(ForgeColors.Surface2)) {
                listOf("abc" to 0, "123" to 1).forEach { (label, index) ->
                    ForgeFocusBox(
                        radius = 18.dp, modifier = Modifier.width(113.dp).height(36.dp).testTag("pigulka.$index"), scaleOnFocus = false,
                        onFocusChange = { if (it) page = index }, onOk = { page = index },
                    ) { focused ->
                        Box(
                            Modifier.width(113.dp).height(36.dp).clip(RoundedCornerShape(18.dp))
                                .background(if (focused) ForgeColors.Focus else if (page == index) ForgeColors.Surface3 else androidx.compose.ui.graphics.Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) { Text(label, style = ForgeText.label.copy(color = if (focused) ForgeColors.OnFocus else ForgeColors.Text)) }
                    }
                }
            }
        }
        if (docked && layout.altRows != null) Spacer(Modifier.height(10.dp)) // v7.4: keys from y 144 (tabs 96-132)
        shownRows.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(layout.gap)) {
                row.forEachIndexed { colIndex, ch ->
                    ForgeKeyCap(
                        label = ch,
                        width = layout.keyWidth,
                        height = layout.keyHeight,
                        tag = if (layout.columns <= 6) "klawisz.${rowIndex * layout.columns + colIndex}" else "", // v7.4 (09)
                        flat = false, // v7.4: tiles (surface, r 8)
                        focusRequester = if (rowIndex == 0 && colIndex == 0) firstKeyFocus else null,
                        onOk = { onTextChange(ForgeKeyboardInput.apply(text, ForgeKey.Char(ch))) },
                    )
                }
            }
        }
        // Action row: spans 4 + 2 + 2 + 4 keys of the 12-column board (276 / 134 / 134 / 276 dp at 63 dp keys).
        fun span(n: Int) = layout.keyWidth * n + layout.gap * (n - 1)
        if (layout.columns <= 6) {
            // v7.4 (09): SPACJA 112 | ⌫ 36 | WYCZYŚĆ 74 (12/700), 2 dp apart, 8 dp under the letters; no SZUKAJ key (the search runs as the text is typed).
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(layout.gap)) {
                ForgeKeyCap("SPACJA", 112.dp, layout.keyHeight, small = true) { onTextChange(ForgeKeyboardInput.apply(text, ForgeKey.Space)) }
                ForgeKeyCap("⌫", 36.dp, layout.keyHeight, small = true, onLongOk = { onTextChange(ForgeKeyboardInput.apply(text, ForgeKey.Clear)) }) { onTextChange(ForgeKeyboardInput.apply(text, ForgeKey.Delete)) }
                ForgeKeyCap("WYCZYŚĆ", 74.dp, layout.keyHeight, small = true) { onTextChange(ForgeKeyboardInput.apply(text, ForgeKey.Clear)) }
            }
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(layout.gap)) {
            ForgeKeyCap("Spacja", span(4), layout.keyHeight, small = true) {
                onTextChange(ForgeKeyboardInput.apply(text, ForgeKey.Space))
            }
            ForgeKeyCap("Usuń", span(2), layout.keyHeight, small = true) {
                onTextChange(ForgeKeyboardInput.apply(text, ForgeKey.Delete))
            }
            ForgeKeyCap("Wyczyść", span(2), layout.keyHeight, small = true) {
                onTextChange(ForgeKeyboardInput.apply(text, ForgeKey.Clear))
            }
            if (!bare) ForgeKeyCap("Szukaj", span(4), layout.keyHeight, small = true, primary = true, onOk = onSearch)
        }
    }
}

@Composable
internal fun ForgeKeyCap(
    label: String,
    width: Dp,
    height: Dp,
    small: Boolean = false,
    primary: Boolean = false,
    /** v6 docked board: letters only, no tile at rest (mockup S02); the focused key is the white one. */
    flat: Boolean = false,
    focusRequester: FocusRequester? = null,
    onLongOk: (() -> Unit)? = null,
    tag: String = "",
    onOk: () -> Unit,
) {
    val corner = 8.dp // v7.4: --klawisz-r 8
    ForgeFocusBox(
        radius = corner,
        modifier = Modifier.width(width).height(height).then(if (tag.isNotEmpty()) Modifier.testTag(tag) else Modifier),
        focusRequester = focusRequester,
        scaleOnFocus = false, // v7.4: key focus = fill only, no scale
        onLongOk = onLongOk,
        onOk = onOk,
    ) { focused ->
        val filled = focused || (primary && !flat)
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(RoundedCornerShape(corner))
                .background(if (filled) (if (focused) ForgeColors.Focus else Color92) else if (flat) androidx.compose.ui.graphics.Color.Transparent else ForgeColors.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                textAlign = TextAlign.Center,
                style = (if (small) ForgeText.captionBold else ForgeText.body)
                    .copy(
                        color = if (filled) ForgeColors.OnFocus else ForgeColors.Text2, // v7.4: klawisz 16/500, 70%
                        fontSize = if (small) 12.sp else 16.sp,
                        letterSpacing = 0.em,
                    ),
            )
        }
    }
}

private val Color92 = androidx.compose.ui.graphics.Color(0xEBFFFFFF) // rgba(255,255,255,.92), the "Szukaj" key
