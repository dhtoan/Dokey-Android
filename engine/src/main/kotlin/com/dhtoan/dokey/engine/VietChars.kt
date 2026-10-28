package com.dhtoan.dokey.engine

/** Diacritic that changes the shape of a letter (as opposed to a tone mark). */
enum class Mark { NONE, HAT, BREVE, HORN, STROKE }

internal object VietChars {
    /** Each shaped vowel with its six forms: no tone, sắc, huyền, hỏi, ngã, nặng. */
    private val TONE_FORMS = mapOf(
        'a' to "aáàảãạ", 'ă' to "ăắằẳẵặ", 'â' to "âấầẩẫậ",
        'e' to "eéèẻẽẹ", 'ê' to "êếềểễệ",
        'i' to "iíìỉĩị",
        'o' to "oóòỏõọ", 'ô' to "ôốồổỗộ", 'ơ' to "ơớờởỡợ",
        'u' to "uúùủũụ", 'ư' to "ưứừửữự",
        'y' to "yýỳỷỹỵ",
    )

    private val REVERSE: Map<Char, Pair<Char, Int>> = buildMap {
        TONE_FORMS.forEach { (shaped, forms) -> forms.forEachIndexed { tone, c -> put(c, shaped to tone) } }
    }

    fun isVowel(base: Char) = base in "aeiouy"

    fun shape(base: Char, mark: Mark): Char = when (mark) {
        Mark.NONE -> base
        Mark.HAT -> when (base) { 'a' -> 'â'; 'e' -> 'ê'; 'o' -> 'ô'; else -> base }
        Mark.BREVE -> if (base == 'a') 'ă' else base
        Mark.HORN -> when (base) { 'o' -> 'ơ'; 'u' -> 'ư'; else -> base }
        Mark.STROKE -> if (base == 'd') 'đ' else base
    }

    fun withTone(shaped: Char, tone: Int): Char =
        if (tone == 0) shaped else TONE_FORMS[shaped]?.get(tone) ?: shaped

    /** Splits a lowercase letter into (base, mark, tone), or null if it is not a Vietnamese letter. */
    fun decompose(c: Char): Triple<Char, Mark, Int>? {
        if (c == 'đ') return Triple('d', Mark.STROKE, 0)
        if (c in 'a'..'z') return Triple(c, Mark.NONE, 0)
        val (shaped, tone) = REVERSE[c] ?: return null
        val (base, mark) = when (shaped) {
            'ă' -> 'a' to Mark.BREVE
            'â' -> 'a' to Mark.HAT
            'ê' -> 'e' to Mark.HAT
            'ô' -> 'o' to Mark.HAT
            'ơ' -> 'o' to Mark.HORN
            'ư' -> 'u' to Mark.HORN
            else -> shaped to Mark.NONE
        }
        return Triple(base, mark, tone)
    }
}

internal class Letter(var base: Char, var mark: Mark, val upper: Boolean, val fromW: Boolean = false) {
    val isVowel get() = VietChars.isVowel(base)
    val isShaped get() = mark == Mark.HAT || mark == Mark.BREVE || mark == Mark.HORN
    fun shaped() = VietChars.shape(base, mark)
    fun copy() = Letter(base, mark, upper, fromW)
}
