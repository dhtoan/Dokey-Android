package com.dhtoan.dokey.engine

/** Text shortcuts ("ko" -> "không"), one "short=long" per line. */
class Abbreviations(text: String) {
    private val map: Map<String, String> = text.lines()
        .mapNotNull { line ->
            val i = line.indexOf('=')
            if (i <= 0) return@mapNotNull null
            val short = line.substring(0, i).trim().lowercase()
            val long = line.substring(i + 1).trim()
            if (short.isEmpty() || long.isEmpty() || short.any { it.isWhitespace() }) null else short to long
        }
        .toMap()

    val size get() = map.size

    /** The expansion of [word], with its capitalisation carried over, or null. */
    fun expand(word: String): String? {
        val long = map[word.lowercase()] ?: return null
        return VietText.matchCase(long, word)
    }

    companion object {
        val DEFAULT = listOf(
            "ko=không", "kh=không", "dc=được", "đc=được", "vs=với", "cx=cũng", "ntn=như thế nào",
            "mn=mọi người", "bt=bình thường", "nc=nói chuyện", "ae=anh em", "tl=trả lời", "hnay=hôm nay",
            "hqua=hôm qua", "bjo=bây giờ", "ng=người", "trc=trước", "cmt=bình luận", "nhg=nhưng",
        ).joinToString("\n")
    }
}
