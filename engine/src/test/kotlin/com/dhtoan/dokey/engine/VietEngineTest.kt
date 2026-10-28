package com.dhtoan.dokey.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VietEngineTest {
    private fun type(
        keys: String,
        method: TypingMethod = TypingMethod.TELEX,
        modern: Boolean = true,
        spellCheck: Boolean = true,
    ): String {
        val e = VietEngine(method, modern, spellCheck)
        keys.forEach { assertTrue(e.type(it), "key '$it' in \"$keys\" was rejected") }
        return e.composing
    }

    private fun check(method: TypingMethod, vararg cases: Pair<String, String>) {
        for ((keys, expected) in cases) assertEquals(expected, type(keys, method), "keys \"$keys\"")
    }

    @Test
    fun telexCommonWords() = check(
        TypingMethod.TELEX,
        "vieetj" to "việt", "tieengs" to "tiếng", "chaof" to "chào", "tooi" to "tôi",
        "khoong" to "không", "dduwowcj" to "được", "ddaay" to "đây", "nhuwngx" to "những",
        "cuar" to "của", "trowif" to "trời", "muoons" to "muốn", "gioongs" to "giống",
        "quyeenf" to "quyền", "khuyur" to "khuỷu", "quas" to "quá", "gias" to "giá", "gif" to "gì",
        "nguwowif" to "người", "nguoiwf" to "người", "dduwowngf" to "đường", "thuongw" to "thương",
        "ruouwj" to "rượu", "muaw" to "mưa", "hoawcj" to "hoặc", "hoacwj" to "hoặc",
        "cacs" to "các", "nawng" to "năng", "dda" to "đa", "uwu" to "ưu",
    )

    @Test
    fun telexToneAnywhereAndChanges() = check(
        TypingMethod.TELEX,
        "vieejt" to "việt", "vieetj" to "việt","tiengse" to "tiengse", "asf" to "à", "asz" to "a", "tieesng" to "tiếng",
    )

    @Test
    fun telexUndoByRepeating() = check(
        TypingMethod.TELEX,
        "ass" to "as", "aaa" to "aa", "ddd" to "dd", "ww" to "w", "www" to "ww", "aww" to "aw",
    )

    @Test
    fun telexLoneW() = check(TypingMethod.TELEX, "w" to "ư", "tw" to "tư", "nhw" to "như")

    @Test
    fun tonePlacementStyles() {
        assertEquals("hoà", type("hoaf"))
        assertEquals("hòa", type("hoaf", modern = false))
        assertEquals("thuý", type("thuys"))
        assertEquals("thúy", type("thuys", modern = false))
        assertEquals("khoẻ", type("khoer"))
        assertEquals("khỏe", type("khoer", modern = false))
        assertEquals("hoàng", type("hoangf", modern = false))
        assertEquals("quý", type("quys", modern = false))
    }

    @Test
    fun keepsCase() = check(TypingMethod.TELEX, "Vieetj" to "Việt", "VIEETJ" to "VIỆT", "DDi" to "Đi", "W" to "Ư")

    @Test
    fun englishWordsStayAsTyped() = check(
        TypingMethod.TELEX,
        "class" to "class", "facebook" to "facebook", "google" to "google", "Windows" to "Windows",
        "hello" to "hello", "user" to "user", "string" to "string",
    )

    @Test
    fun withoutSpellCheckEverythingTransforms() {
        assertEquals("clas", type("class", spellCheck = false))
    }

    @Test
    fun vniWords() = check(
        TypingMethod.VNI,
        "vie65t" to "việt", "tie61ng" to "tiếng", "nguo7i2" to "người", "d9uo7ng2" to "đường",
        "hoa2" to "hoà", "thuy1" to "thuý", "a11" to "a1", "a8n" to "ăn", "u7" to "ư", "Covid19" to "Covid19",
    )

    @Test
    fun vniDigitAloneIsNotPartOfAWord() {
        val e = VietEngine(TypingMethod.VNI)
        assertFalse(e.type('1'))
        assertTrue(e.isEmpty)
    }

    @Test
    fun telexDigitsEndTheWord() {
        val e = VietEngine(TypingMethod.TELEX)
        assertTrue(e.type('a'))
        assertFalse(e.type('1'))
    }

    @Test
    fun backspaceRemovesOneVisibleCharacter() {
        val e = VietEngine()
        "tieengs".forEach { e.type(it) }
        assertTrue(e.backspace())
        assertEquals("tiến", e.composing)
        e.type('g')
        assertEquals("tiếng", e.composing)

        e.reset()
        "hoaf".forEach { e.type(it) }
        e.backspace()
        assertEquals("ho", e.composing)

        e.reset()
        "class".forEach { e.type(it) }
        e.backspace()
        assertEquals("clas", e.composing)

        e.reset()
        "aaaa".forEach { e.type(it) }
        assertEquals("aaa", e.composing)
        e.backspace()
        assertEquals("aa", e.composing)
    }

    @Test
    fun backspaceToEmpty() {
        val e = VietEngine()
        e.type('a')
        assertTrue(e.backspace())
        assertTrue(e.isEmpty)
        assertEquals("", e.composing)
        assertFalse(e.backspace())
    }

    @Test
    fun composedLettersFromPopup() {
        val e = VietEngine()
        "t".forEach { e.type(it) }
        e.type('ư')
        "ows".forEach { e.type(it) }
        assertEquals("tướ", e.composing)
    }
}
