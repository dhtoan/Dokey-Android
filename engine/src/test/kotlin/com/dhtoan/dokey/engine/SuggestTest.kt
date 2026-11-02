package com.dhtoan.dokey.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SuggestTest {
    private val dict = Dictionary.bundled()

    @Test
    fun bundledDictionaryLoads() {
        assertTrue(dict.size > 2000, "only ${dict.size} syllables")
        assertEquals("đươc5", dict.keyOf("được"))
        assertEquals("đươc5", dict.keyOf("Được"))
    }

    @Test
    fun plainLettersSuggestToneVariants() {
        val s = dict.suggest("duoc")
        assertEquals("được", s.first(), "got $s")
    }

    @Test
    fun completesPartialSyllables() {
        assertTrue("người" in dict.suggest("nguo", limit = 5), "got ${dict.suggest("nguo", limit = 5)}")
    }

    @Test
    fun typedMarksAndTonesMustMatch() {
        assertTrue(dict.suggest("đ", limit = 10).all { it.startsWith("đ") })
        assertTrue(dict.suggest("cá", limit = 10).all { VietText.split(it)!!.second == 1 })
    }

    @Test
    fun keepsCapitalisation() {
        assertEquals("Được", dict.suggest("Duoc").first())
    }

    @Test
    fun respectsToneStyle() {
        val modern = dict.suggest("hoa", limit = 6)
        val classic = dict.suggest("hoa", limit = 6, modern = false)
        assertTrue(modern.none { it == "hòa" }, "modern $modern")
        assertTrue(classic.none { it == "hoà" }, "classic $classic")
    }

    @Test
    fun predictsNextSyllable() {
        val key = dict.keyOf("quy")!!
        assertTrue("định" in dict.predict(key, limit = 5), "got ${dict.predict(key, limit = 5)}")
    }

    @Test
    fun swipeDecodesPlainPath() {
        val d = SwipeDecoder(dict)
        // A finger going d -> u -> o -> c also crosses some keys in between.
        assertEquals("được", d.decode("dfyuioc").first(), "got ${d.decode("dfyuioc")}")
        assertTrue("người" in d.decode("nbhgytuioi"), "got ${d.decode("nbhgytuioi")}")
    }

    @Test
    fun abbreviations() {
        val a = Abbreviations(Abbreviations.DEFAULT)
        assertEquals("không", a.expand("ko"))
        assertEquals("Không", a.expand("Ko"))
        assertEquals("như thế nào", a.expand("ntn"))
        assertNull(a.expand("khong"))
        assertEquals("xin chào", Abbreviations("xc = xin chào\nbad line\n=x").expand("xc"))
    }

    @Test
    fun toneHelpers() {
        assertEquals("người", VietText.withTone("ngươi", 2, true))
        assertEquals("Hoà", VietText.withTone("Hoá", 2, true))
        assertEquals("hòa", VietText.withTone("hoa", 2, false))
        assertEquals("duoc", VietText.skeleton("được"))
    }

    @Test
    fun engineApplyToneAndSetWord() {
        val e = VietEngine()
        "nguoi".forEach { e.type(it) }
        assertTrue(e.applyTone(2))
        assertEquals("nguòi", e.composing)
        e.setWord("người")
        assertEquals("người", e.composing)
        e.backspace()
        assertEquals("ngườ", e.composing)
    }
}
