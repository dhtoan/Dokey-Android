package com.dhtoan.dokey.engine

/** Helpers for whole Vietnamese syllables: tones, case and the plain-letter "skeleton". */
object VietText {
    /** "Được" -> ("đươc", 5). Null if [word] has a character that is not a Vietnamese letter. */
    fun split(word: String): Pair<String, Int>? {
        var tone = 0
        val shaped = StringBuilder()
        for (c in word.lowercase()) {
            val (base, mark, t) = VietChars.decompose(c) ?: return null
            if (t != 0) tone = t
            shaped.append(VietChars.shape(base, mark))
        }
        return shaped.toString() to tone
    }

    /** "được" -> "duoc": the keys a user types without any marks. */
    fun skeleton(text: String): String = buildString {
        for (c in text.lowercase()) {
            val d = VietChars.decompose(c)
            append(d?.first ?: c)
        }
    }

    /** Puts [tone] on the lowercase [shaped] letters by the spelling rules. */
    fun compose(shaped: String, tone: Int, modern: Boolean): String = withTone(shaped, tone, modern)

    /** Replaces the tone of [word] (keeping its case); tone 0 removes it. */
    fun withTone(word: String, tone: Int, modern: Boolean): String {
        val letters = ArrayList<Letter>(word.length)
        for (c in word) {
            val lower = c.lowercaseChar()
            val (base, mark, _) = VietChars.decompose(lower) ?: return word
            letters.add(Letter(base, mark, c != lower))
        }
        val pos = if (tone == 0) -1 else Syllable.tonePosition(letters, modern)
        return buildString {
            letters.forEachIndexed { i, l ->
                var c = l.shaped()
                if (i == pos) c = VietChars.withTone(c, tone)
                append(if (l.upper) c.uppercaseChar() else c)
            }
        }
    }

    /** Gives [word] the same capitalisation as [like] (all caps, first capital, or lowercase). */
    fun matchCase(word: String, like: String): String = when {
        like.length > 1 && like.all { !it.isLetter() || it.isUpperCase() } -> word.uppercase()
        like.firstOrNull()?.isUpperCase() == true -> word.replaceFirstChar { it.uppercaseChar() }
        else -> word
    }
}
