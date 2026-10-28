package com.dhtoan.dokey.ime

object Codes {
    const val SHIFT = -1
    const val SYMBOLS = -2
    const val ALPHA = -3
    const val ENTER = -4
    const val DELETE = -5
    const val MORE_SYMBOLS = -6
    const val LANGUAGE = -7
    const val SPACE = 32
}

data class Key(
    val code: Int,
    val label: String = if (code > 0) code.toChar().toString() else "",
    val width: Float = 1f,
    val alts: List<String> = emptyList(),
) {
    /** A key that types a visible character. */
    val isChar get() = code > Codes.SPACE
    val isLetter get() = isChar && code.toChar().isLetter()
}

class Row(val keys: List<Key>, val indent: Float = 0f)

class Layout(val id: String, val rows: List<Row>, val columns: Int = 10)

object Layouts {
    private fun row(chars: String, alts: Map<Char, String> = emptyMap(), indent: Float = 0f) =
        Row(chars.map { Key(it.code, alts = alts[it]?.split(' ') ?: emptyList()) }, indent)

    private fun k(c: Char, alts: String = "", width: Float = 1f) =
        Key(c.code, width = width, alts = if (alts.isEmpty()) emptyList() else alts.split(' '))

    private val LETTER_ALTS = mapOf(
        'q' to "1", 'w' to "2", 'e' to "3 ê", 'r' to "4", 't' to "5", 'y' to "6", 'u' to "7 ư",
        'i' to "8", 'o' to "9 ô ơ", 'p' to "0", 'a' to "â ă", 'd' to "đ",
    )

    private val NUMBER_ROW = row("1234567890")

    private fun bottom(switchKey: Key) = Row(
        listOf(
            switchKey,
            k(',', "? ! ; : ' \" -"),
            Key(Codes.LANGUAGE),
            Key(Codes.SPACE, width = 4f),
            k('.', "… ! ? , : ;"),
            Key(Codes.ENTER, width = 1.5f),
        ),
    )

    fun alpha(numberRow: Boolean): Layout {
        val rows = listOf(
            row("qwertyuiop", LETTER_ALTS),
            row("asdfghjkl", LETTER_ALTS, indent = 0.5f),
            Row(listOf(Key(Codes.SHIFT, width = 1.5f)) + row("zxcvbnm").keys + Key(Codes.DELETE, width = 1.5f)),
            bottom(Key(Codes.SYMBOLS, "?123", 1.5f)),
        )
        return Layout(if (numberRow) "alpha_num" else "alpha", if (numberRow) listOf(NUMBER_ROW) + rows else rows)
    }

    val SYMBOLS = Layout(
        "symbols",
        listOf(
            NUMBER_ROW,
            Row(
                listOf(
                    k('@'), k('#'), k('₫', "\$ € £ ¥ ¢"), k('_'), k('&'), k('-', "– — ·"), k('+', "±"),
                    k('(', "< [ {"), k(')', "> ] }"), k('/', "\\"),
                ),
            ),
            Row(
                listOf(Key(Codes.MORE_SYMBOLS, "=\\<", 1.5f)) +
                    listOf(k('*', "★ †"), k('"', "“ ” « »"), k('\'', "‘ ’"), k(':'), k(';'), k('!', "¡"), k('?', "¿")) +
                    Key(Codes.DELETE, width = 1.5f),
            ),
            bottom(Key(Codes.ALPHA, "ABC", 1.5f)),
        ),
    )

    val MORE_SYMBOLS = Layout(
        "more_symbols",
        listOf(
            row("~`|•√π÷×¶∆"),
            row("£¥€\$^°={}\\"),
            Row(listOf(Key(Codes.SYMBOLS, "?123", 1.5f)) + row("%©®™✓[]").keys + Key(Codes.DELETE, width = 1.5f)),
            bottom(Key(Codes.ALPHA, "ABC", 1.5f)),
        ),
    )

    /** For number, phone and date fields. */
    val NUMPAD = Layout(
        "numpad",
        listOf(
            Row(listOf(k('1'), k('2'), k('3'), k('-', "+ * / # ( )"))),
            Row(listOf(k('4'), k('5'), k('6'), Key(Codes.SPACE))),
            Row(listOf(k('7'), k('8'), k('9'), Key(Codes.DELETE))),
            Row(
                listOf(
                    Key(Codes.ALPHA, "ABC", 0.8f), k(',', width = 0.6f), k('0', "+", 1.2f),
                    k('.', width = 0.6f), Key(Codes.ENTER, width = 0.8f),
                ),
            ),
        ),
        columns = 4,
    )
}
