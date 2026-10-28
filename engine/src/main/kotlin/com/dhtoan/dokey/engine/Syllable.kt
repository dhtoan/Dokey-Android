package com.dhtoan.dokey.engine

/**
 * Vietnamese syllable structure: initial consonant + vowel nucleus + final consonant.
 * Built from the public spelling rules of the language, not from any other input method.
 */
internal object Syllable {
    private val INITIALS = setOf(
        "", "b", "c", "ch", "d", "đ", "g", "gh", "gi", "h", "k", "kh", "l", "m", "n", "ng", "ngh",
        "nh", "p", "ph", "qu", "r", "s", "t", "th", "tr", "v", "x",
    )

    private val NUCLEI = setOf(
        "a", "ă", "â", "e", "ê", "i", "o", "ô", "ơ", "u", "ư", "y",
        "ai", "ao", "au", "ay", "âu", "ây", "eo", "êu", "ia", "iê", "iu", "oa", "oă", "oe", "oi", "ôi",
        "ơi", "oo", "ua", "uâ", "uê", "ui", "uô", "uơ", "uy", "ưa", "ưi", "ưu", "ươ", "yê",
        "iêu", "oai", "oay", "oeo", "uây", "uôi", "uya", "uyê", "uyu", "ươi", "ươu", "yêu",
        // Shapes seen mid-word, before the horn of "ươ" has been typed.
        "uo", "ưo", "uoi", "uou", "ưoi", "ưou",
    )

    /** Nuclei that already end in a glide and therefore take no final consonant. */
    private val OPEN_ONLY = setOf(
        "ai", "ao", "au", "ay", "âu", "ây", "eo", "êu", "ia", "iu", "oi", "ôi", "ơi", "ua", "ui",
        "ưa", "ưi", "ưu", "uơ", "iêu", "oai", "oay", "oeo", "uây", "uôi", "uya", "uyu", "ươi", "ươu",
        "yêu", "uoi", "uou", "ưoi", "ưou",
    )

    private val FINALS = setOf("", "c", "ch", "m", "n", "ng", "nh", "p", "t")
    private val STOP_FINALS = setOf("c", "ch", "p", "t")

    /** [nucleusStart, finalStart) is the vowel nucleus; [wellFormed] is false if vowels follow the final. */
    class Split(val nucleusStart: Int, val finalStart: Int, val wellFormed: Boolean)

    fun split(w: List<Letter>): Split {
        var i = 0
        while (i < w.size && !w[i].isVowel) i++
        var start = i
        // In "qu" and "gi" the vowel letter belongs to the initial when another vowel follows.
        if (i == 1 && i + 1 < w.size && w[i + 1].isVowel) {
            val c0 = w[0].base
            val v = w[i]
            if ((c0 == 'q' && v.base == 'u' && v.mark == Mark.NONE) || (c0 == 'g' && v.base == 'i')) start = i + 1
        }
        var j = start
        while (j < w.size && w[j].isVowel) j++
        var k = j
        while (k < w.size && !w[k].isVowel) k++
        return Split(start, j, k == w.size)
    }

    private fun text(w: List<Letter>, from: Int, to: Int) =
        buildString { for (n in from until to) append(w[n].shaped()) }

    /** True when [w] with [tone] is a Vietnamese syllable, or a sensible step on the way to one. */
    fun isPlausible(w: List<Letter>, tone: Int): Boolean {
        val s = split(w)
        if (!s.wellFormed) return false
        if (text(w, 0, s.nucleusStart) !in INITIALS) return false
        val nucleus = text(w, s.nucleusStart, s.finalStart)
        val final = text(w, s.finalStart, w.size)
        if (nucleus.isEmpty()) return tone == 0
        if (nucleus !in NUCLEI || final !in FINALS) return false
        if (final.isNotEmpty() && nucleus in OPEN_ONLY) return false
        if (final in STOP_FINALS && tone in 2..4) return false
        return true
    }

    /** Index of the letter that carries the tone mark, or -1 if there is no vowel. */
    fun tonePosition(w: List<Letter>, modern: Boolean): Int {
        val s = split(w)
        val a = s.nucleusStart
        val b = s.finalStart
        if (a >= b) return w.indexOfLast { it.isVowel }
        (a until b).lastOrNull { w[it].isShaped }?.let { return it }
        val n = b - a
        if (n == 1) return a
        if (b < w.size) return b - 1
        if (n >= 3) return a + 1
        val pair = "${w[a].base}${w[a + 1].base}"
        return if (modern && pair in setOf("oa", "oe", "uy")) a + 1 else a
    }
}
