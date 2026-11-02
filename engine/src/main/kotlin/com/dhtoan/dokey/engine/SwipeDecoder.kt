package com.dhtoan.dokey.engine

import kotlin.math.ln

/**
 * Turns the letters a finger passed over while swiping into Vietnamese syllables.
 *
 * A syllable matches when its plain letters (doubled letters counted once) appear in order along the path,
 * starting at the first key and ending at the last one. Matches are ranked by frequency, by how much of the
 * path they explain, and by how often they follow the previous syllable. Tones come from the dictionary:
 * swiping "duoc" gives "được".
 */
class SwipeDecoder(private val dict: Dictionary) {

    fun decode(path: String, previousKey: String? = null, limit: Int = 4, modern: Boolean = true): List<String> {
        val keys = collapse(path.lowercase().filter { it in 'a'..'z' })
        if (keys.length < 2) return emptyList()
        val best = HashMap<String, Pair<Dictionary.Entry, Double>>()
        for (e in dict.all()) {
            val sk = collapse(e.skeleton)
            if (sk.length < 2 || sk.first() != keys.first() || sk.last() != keys.last()) continue
            if (!isSubsequence(sk, keys)) continue
            var score = ln(e.count.toDouble()) + 0.9 * sk.length - 0.25 * (keys.length - sk.length)
            val pair = dict.bigram(previousKey, e.key)
            if (pair > 0) score += 2.0 + ln(pair.toDouble())
            val old = best[e.skeleton]
            if (old == null || old.second < score) best[e.skeleton] = e to score
        }
        return best.values.sortedByDescending { it.second }
            .take(limit)
            .map { dict.renderEntry(it.first, modern) }
    }

    private fun collapse(s: String) = buildString {
        for (c in s) if (isEmpty() || last() != c) append(c)
    }

    private fun isSubsequence(needle: String, hay: String): Boolean {
        var i = 0
        for (c in hay) if (i < needle.length && needle[i] == c) i++
        return i == needle.length
    }
}
