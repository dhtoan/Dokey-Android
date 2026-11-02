package com.dhtoan.dokey.engine

enum class TypingMethod { TELEX, VNI }

/**
 * Turns the keys of one word into Vietnamese text.
 *
 * The engine keeps the keys typed for the current word and re-renders the whole word after every
 * key, so tone marks can be typed anywhere in the word and are always placed by the spelling rules.
 * The caller shows [composing] as composing text and commits it when the word ends.
 */
class VietEngine(
    var method: TypingMethod = TypingMethod.TELEX,
    /** Modern tone placement ("hoà", "thuý") instead of the classic one ("hòa", "thúy"). */
    var modernTone: Boolean = true,
    /** Leave words that cannot be Vietnamese (e.g. "class", "google") exactly as typed. */
    var spellCheck: Boolean = true,
) {
    private val raw = StringBuilder()
    private var showingRaw = false
    private var forceLiteral = false

    /** The current word as it should be shown. */
    var composing: String = ""
        private set

    val isEmpty: Boolean get() = raw.isEmpty()

    fun reset() {
        raw.setLength(0)
        composing = ""
        showingRaw = false
        forceLiteral = false
    }

    /** Replaces the current word with [word], e.g. a suggestion or a swiped word. */
    fun setWord(word: String) {
        reset()
        if (word.isEmpty()) return
        raw.append(word)
        rebuild()
    }

    /** Puts [tone] (1 sắc … 5 nặng, 0 none) on the current word. Returns false if nothing changed. */
    fun applyTone(tone: Int): Boolean {
        if (raw.isEmpty()) return false
        val toned = VietText.withTone(composing, tone, modernTone)
        if (toned == composing) return false
        setWord(toned)
        return true
    }

    /** True if [c] continues the current word (letters, and VNI digits after a letter). */
    fun accepts(c: Char): Boolean {
        val l = c.lowercaseChar()
        if (l in 'a'..'z' || (l !in '0'..'9' && VietChars.decompose(l) != null)) return true
        return method == TypingMethod.VNI && c in '0'..'9' && raw.isNotEmpty()
    }

    /**
     * Adds [c] to the current word. Returns false if [c] does not belong in a word; the caller then
     * commits the word and inserts [c] itself.
     */
    fun type(c: Char): Boolean {
        if (!accepts(c)) return false
        raw.append(c)
        rebuild()
        return true
    }

    /** Deletes the last visible character of the word. Returns false if there is no word. */
    fun backspace(): Boolean {
        if (raw.isEmpty()) return false
        if (showingRaw) {
            raw.setLength(raw.length - 1)
        } else {
            val target = composing.dropLast(1)
            raw.setLength(0)
            raw.append(target)
            forceLiteral = false
            if (raw.isNotEmpty()) {
                rebuild()
                if (composing != target) {
                    forceLiteral = true
                    rebuild()
                }
            }
        }
        if (raw.isEmpty()) reset() else rebuild()
        return true
    }

    private fun rebuild() {
        val b = Builder(forceLiteral)
        for (c in raw) b.feed(c)
        showingRaw = b.transformed && !b.literal && spellCheck && !Syllable.isPlausible(b.w, b.tone)
        composing = if (showingRaw) raw.toString() else b.render()
    }

    private inner class Builder(var literal: Boolean) {
        val w = ArrayList<Letter>()
        var tone = 0
        var transformed = false

        fun feed(c: Char) {
            val lower = c.lowercaseChar()
            val upper = c != lower
            if (lower !in 'a'..'z' && lower !in '0'..'9') {
                // An already composed letter (from a long-press popup or after backspace).
                val (base, mark, t) = VietChars.decompose(lower) ?: return
                w.add(Letter(base, mark, upper))
                if (mark != Mark.NONE) transformed = true
                if (t != 0) {
                    tone = t
                    transformed = true
                }
                return
            }
            if (!literal) {
                val handled = when (method) {
                    TypingMethod.TELEX -> telex(lower, upper)
                    TypingMethod.VNI -> vni(lower)
                }
                if (handled) {
                    transformed = true
                    return
                }
            }
            w.add(Letter(lower, Mark.NONE, upper))
        }

        private fun telex(k: Char, upper: Boolean): Boolean = when (k) {
            's' -> setTone(1)
            'f' -> setTone(2)
            'r' -> setTone(3)
            'x' -> setTone(4)
            'j' -> setTone(5)
            'z' -> clearTone()
            'a', 'e', 'o' -> hat(k)
            'w' -> horn(includeA = true, standalone = true, upper = upper)
            'd' -> stroke()
            else -> false
        }

        private fun vni(k: Char): Boolean = when (k) {
            in '1'..'5' -> setTone(k - '0')
            '0' -> clearTone()
            '6' -> hat(null)
            '7' -> horn(includeA = false, standalone = false, upper = false)
            '8' -> breve()
            '9' -> stroke()
            else -> false
        }

        /** Applies [change] only if the word stays plausible (when spell checking is on). */
        private inline fun attempt(change: () -> Unit): Boolean {
            if (!spellCheck) {
                change()
                return true
            }
            val saved = w.map { it.copy() }
            val savedTone = tone
            change()
            if (Syllable.isPlausible(w, tone)) return true
            w.clear()
            w.addAll(saved)
            tone = savedTone
            return false
        }

        /** Undoes a mark: the key that undid it is then typed as a plain letter for the rest of the word. */
        private fun undo(): Boolean {
            literal = true
            return false
        }

        private fun setTone(t: Int): Boolean {
            if (w.none { it.isVowel }) return false
            if (tone == t) {
                tone = 0
                return undo()
            }
            return attempt { tone = t }
        }

        private fun clearTone(): Boolean {
            if (tone == 0) return false
            tone = 0
            return true
        }

        /** The last run of vowels in the word. */
        private fun group(): IntRange {
            val end = w.indexOfLast { it.isVowel }
            if (end < 0) return IntRange.EMPTY
            var start = end
            while (start > 0 && w[start - 1].isVowel) start--
            return start..end
        }

        /** â ê ô. Telex passes the vowel that was typed twice; VNI passes null (any of a, e, o). */
        private fun hat(k: Char?): Boolean {
            val g = group()
            val idx = g.reversed().firstOrNull { if (k == null) w[it].base in "aeo" else w[it].base == k }
                ?: return false
            val l = w[idx]
            if (l.mark == Mark.HAT) {
                l.mark = Mark.NONE
                return undo()
            }
            return attempt {
                l.mark = Mark.HAT
                if (l.base == 'o' && idx > 0 && w[idx - 1].base == 'u') w[idx - 1].mark = Mark.NONE
            }
        }

        private fun breve(): Boolean {
            val idx = group().reversed().firstOrNull { w[it].base == 'a' } ?: return false
            val l = w[idx]
            if (l.mark == Mark.BREVE) {
                l.mark = Mark.NONE
                return undo()
            }
            return attempt { l.mark = Mark.BREVE }
        }

        /** ơ ư ươ (and ă in Telex, where "w" does both). */
        private fun horn(includeA: Boolean, standalone: Boolean, upper: Boolean): Boolean {
            val g = group()
            if (g.isEmpty()) {
                // Telex: a lone "w" types "ư".
                return standalone && attempt { w.add(Letter('u', Mark.HORN, upper, fromW = true)) }
            }
            val pair = (g.first until g.last).firstOrNull {
                w[it].base == 'u' && w[it + 1].base == 'o' && !(it == 1 && w[0].base == 'q')
            }
            if (pair != null) {
                val u = w[pair]
                val o = w[pair + 1]
                if (u.mark == Mark.HORN && o.mark == Mark.HORN) {
                    u.mark = Mark.NONE
                    o.mark = Mark.NONE
                    return undo()
                }
                return attempt {
                    u.mark = Mark.HORN
                    o.mark = Mark.HORN
                }
            }
            val applies = ArrayList<Int>()
            var undoAt = -1
            for (i in g.reversed()) {
                val l = w[i]
                val target = when (l.base) {
                    'a' -> if (includeA) Mark.BREVE else null
                    'o', 'u' -> Mark.HORN
                    else -> null
                } ?: continue
                if (l.mark == target) {
                    if (undoAt < 0) undoAt = i
                } else {
                    applies.add(i)
                }
            }
            applies.sortBy { "aou".indexOf(w[it].base) }
            for (i in applies) {
                val l = w[i]
                val target = if (l.base == 'a') Mark.BREVE else Mark.HORN
                if (attempt { l.mark = target }) return true
            }
            if (undoAt >= 0) {
                val l = w[undoAt]
                if (l.fromW) w.removeAt(undoAt) else l.mark = Mark.NONE
                return undo()
            }
            return false
        }

        private fun stroke(): Boolean {
            val d = w.firstOrNull() ?: return false
            if (d.base != 'd') return false
            if (d.mark == Mark.STROKE) {
                d.mark = Mark.NONE
                return undo()
            }
            return attempt { d.mark = Mark.STROKE }
        }

        fun render(): String {
            val pos = if (tone != 0) Syllable.tonePosition(w, modernTone) else -1
            return buildString {
                w.forEachIndexed { i, l ->
                    var c = l.shaped()
                    if (i == pos) c = VietChars.withTone(c, tone)
                    append(if (l.upper) c.uppercaseChar() else c)
                }
            }
        }
    }
}
