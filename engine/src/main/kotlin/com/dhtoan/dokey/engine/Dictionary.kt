package com.dhtoan.dokey.engine

import kotlin.math.ln

/**
 * Vietnamese syllable frequencies and syllable pairs, used for suggestions and swipe typing.
 * Keys are a syllable's letters without the tone plus a tone digit, e.g. "đươc5" for "được".
 */
class Dictionary(syllables: Sequence<String>, bigrams: Sequence<String>) {

    class Entry(val key: String, val shaped: String, val tone: Int, val skeleton: String, val count: Int)

    private val entries: List<Entry>
    private val byKey: Map<String, Entry>
    private val next: Map<String, List<Pair<String, Int>>>

    init {
        val list = ArrayList<Entry>()
        for (line in syllables) {
            if (line.isBlank() || line.startsWith("#")) continue
            val tab = line.indexOf('\t')
            if (tab < 2) continue
            val key = line.substring(0, tab)
            val count = line.substring(tab + 1).trim().toIntOrNull() ?: continue
            val shaped = key.dropLast(1)
            val tone = key.last() - '0'
            if (tone !in 0..5) continue
            list.add(Entry(key, shaped, tone, VietText.skeleton(shaped), count))
        }
        list.sortByDescending { it.count }
        entries = list
        byKey = list.associateBy { it.key }
        val pairs = HashMap<String, MutableList<Pair<String, Int>>>()
        for (line in bigrams) {
            if (line.isBlank() || line.startsWith("#")) continue
            val parts = line.split('\t')
            if (parts.size != 3) continue
            val n = parts[2].trim().toIntOrNull() ?: continue
            pairs.getOrPut(parts[0]) { ArrayList() }.add(parts[1] to n)
        }
        pairs.values.forEach { l -> l.sortByDescending { it.second } }
        next = pairs
    }

    val size get() = entries.size

    /** The dictionary key of [word] ("Được" -> "đươc5"), or null if it is not a known syllable. */
    fun keyOf(word: String): String? {
        val (shaped, tone) = VietText.split(word) ?: return null
        val key = "$shaped$tone"
        return if (byKey.containsKey(key)) key else null
    }

    private fun pairCount(previous: String?, key: String): Int =
        previous?.let { p -> next[p]?.firstOrNull { it.first == key }?.second } ?: 0

    private fun render(e: Entry, modern: Boolean) = VietText.compose(e.shaped, e.tone, modern)

    /**
     * Words the user may mean while typing [typed]: tone and mark variants ("duoc" -> "được", "dược") and
     * completions ("nguo" -> "người"). Letters the user already marked must match. Best first.
     */
    fun suggest(typed: String, previous: String? = null, limit: Int = 3, modern: Boolean = true): List<String> {
        val split = VietText.split(typed) ?: return emptyList()
        val (typedShaped, typedTone) = split
        if (typedShaped.isEmpty()) return emptyList()
        val skel = VietText.skeleton(typedShaped)
        val scored = ArrayList<Pair<Entry, Double>>()
        for (e in entries) {
            if (!e.skeleton.startsWith(skel)) continue
            if (typedTone != 0 && e.tone != typedTone) continue
            if (!marksMatch(typedShaped, e.shaped)) continue
            var score = ln(e.count.toDouble())
            if (e.skeleton.length == skel.length) score += 2.0
            val pair = pairCount(previous, e.key)
            if (pair > 0) score += 2.5 + ln(pair.toDouble())
            scored.add(e to score)
        }
        return scored.sortedByDescending { it.second }
            .take(limit)
            .map { VietText.matchCase(render(it.first, modern), typed) }
    }

    /** Typed letters with a mark (â ă ê ô ơ ư đ) must keep that mark in the suggestion. */
    private fun marksMatch(typed: String, candidate: String): Boolean {
        for (i in typed.indices) {
            val t = typed[i]
            if (t in 'a'..'z') continue
            if (i >= candidate.length || candidate[i] != t) return false
        }
        return true
    }

    /** Syllables that often follow [previousKey], for the suggestion bar after a word. */
    fun predict(previousKey: String, limit: Int = 3, modern: Boolean = true): List<String> =
        next[previousKey].orEmpty().asSequence()
            .mapNotNull { byKey[it.first] }
            .take(limit)
            .map { render(it, modern) }
            .toList()

    /** All entries, most frequent first (for the swipe decoder). */
    internal fun all(): List<Entry> = entries

    internal fun bigram(previous: String?, key: String) = pairCount(previous, key)

    internal fun renderEntry(e: Entry, modern: Boolean) = render(e, modern)

    companion object {
        /** Loads the lists bundled in the engine's resources. */
        fun bundled(): Dictionary {
            fun lines(name: String): Sequence<String> {
                val stream = Dictionary::class.java.getResourceAsStream("/dokey/$name")
                    ?: return emptySequence()
                return stream.bufferedReader(Charsets.UTF_8).use { it.readLines() }.asSequence()
            }
            return Dictionary(lines("syllables.txt"), lines("bigrams.txt"))
        }
    }
}
