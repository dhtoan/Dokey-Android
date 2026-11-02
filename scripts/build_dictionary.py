#!/usr/bin/env python3
"""Builds Dokey's syllable and bigram frequency lists from a folder of text files.

Usage: python3 scripts/build_dictionary.py CORPUS_DIR
Writes engine/src/main/resources/dokey/syllables.txt and bigrams.txt.
Each syllable is stored as its letters without the tone mark plus a tone digit (0 none, 1 sắc, 2 huyền,
3 hỏi, 4 ngã, 5 nặng), so the keyboard can place the tone in either the modern or the classic style.
"""
import collections
import glob
import os
import re
import sys
import unicodedata

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "engine/src/main/resources/dokey")

FORMS = {
    "a": "aáàảãạ", "ă": "ăắằẳẵặ", "â": "âấầẩẫậ", "e": "eéèẻẽẹ", "ê": "êếềểễệ", "i": "iíìỉĩị",
    "o": "oóòỏõọ", "ô": "ôốồổỗộ", "ơ": "ơớờởỡợ", "u": "uúùủũụ", "ư": "ưứừửữự", "y": "yýỳỷỹỵ",
}
REVERSE = {c: (shaped, t) for shaped, forms in FORMS.items() for t, c in enumerate(forms)}
VOWELS = set("aăâeêioôơuưy")

INITIALS = {"", "b", "c", "ch", "d", "đ", "g", "gh", "gi", "h", "k", "kh", "l", "m", "n", "ng", "ngh", "nh",
            "p", "ph", "qu", "r", "s", "t", "th", "tr", "v", "x"}
NUCLEI = set("a ă â e ê i o ô ơ u ư y ai ao au ay âu ây eo êu ia iê iu oa oă oe oi ôi ơi oo ua uâ uê ui uô uơ uy "
             "ưa ưi ưu ươ yê iêu oai oay oeo uây uôi uya uyê uyu ươi ươu yêu".split())
OPEN_ONLY = set("ai ao au ay âu ây eo êu ia iu oi ôi ơi ua ui ưa ưi ưu uơ iêu oai oay oeo uây uôi uya uyu ươi "
                "ươu yêu".split())
FINALS = {"", "c", "ch", "m", "n", "ng", "nh", "p", "t"}
STOPS = {"c", "ch", "p", "t"}

# Everyday words that formal and old texts rarely contain. Counts are a rough boost, chosen by hand.
EVERYDAY = """nhé nha nhá ạ ơi vâng dạ ừ ờ à hả hử nhỉ nhể chứ đâu sao gì thế nào vậy luôn rồi mình bạn tớ cậu
anh em chị ông bà cô chú bác mẹ bố ba má con cháu nhà ăn uống ngủ đi về chơi làm học xem nghe nói biết thích
yêu thương nhớ muốn cần được không có ok vui buồn mệt đói khát đẹp xấu tốt hay dở nhanh chậm sớm muộn hôm nay mai
qua tuần tháng năm giờ phút sáng trưa chiều tối đêm điện thoại tin nhắn gọi chụp ảnh mạng cà phê trà sữa bánh
cơm phở bún xe máy đường phố quán chợ tiền mua bán rẻ đắt cảm ơn xin lỗi chào tạm biệt hẹn gặp lại""".split()


def syllable(word):
    """Returns (shaped, tone) if word is a Vietnamese syllable, else None."""
    letters, tone = [], 0
    for c in word:
        if c == "đ":
            letters.append("đ")
            continue
        if c in REVERSE:
            shaped, t = REVERSE[c]
            if t:
                if tone:
                    return None
                tone = t
            letters.append(shaped)
        elif "a" <= c <= "z":
            letters.append(c)
        else:
            return None
    s = "".join(letters)
    i = 0
    while i < len(s) and s[i] not in VOWELS:
        i += 1
    start = i
    if i == 1 and i + 1 < len(s) and s[i + 1] in VOWELS and ((s[0] == "q" and s[1] == "u") or (s[0] == "g" and s[1] == "i")):
        start = i + 1
    j = start
    while j < len(s) and s[j] in VOWELS:
        j += 1
    initial, nucleus, final = s[:start], s[start:j], s[j:]
    if any(ch in VOWELS for ch in final) or not nucleus:
        return None
    if initial not in INITIALS or nucleus not in NUCLEI or final not in FINALS:
        return None
    if final and nucleus in OPEN_ONLY:
        return None
    if final in STOPS and tone not in (1, 5):
        return None
    return s, tone


def clean(text):
    text = unicodedata.normalize("NFC", text)
    text = text.replace("Ð", "Đ").replace("ð", "đ")  # old texts use the Icelandic eth
    text = re.sub(r"\{\{[^{}]*\}\}", " ", text)
    text = re.sub(r"<[^>]+>", " ", text)
    text = re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r"\1", text)
    return text.lower()


def main(corpus):
    uni, bi = collections.Counter(), collections.Counter()
    for path in sorted(glob.glob(os.path.join(corpus, "*.txt"))):
        text = clean(open(path, encoding="utf-8").read())
        # Bigrams only inside a phrase: split at punctuation and line breaks.
        for phrase in re.split(r"[^\w\s]|\n|_|\d", text):
            prev = None
            for tok in phrase.split():
                syl = syllable(tok)
                if syl is None:
                    prev = None
                    continue
                key = f"{syl[0]}{syl[1]}"
                uni[key] += 1
                if prev:
                    bi[(prev, key)] += 1
                prev = key
    top = max(uni.values())
    for w in EVERYDAY:
        syl = syllable(w)
        if syl:
            uni[f"{syl[0]}{syl[1]}"] += top // 200
    syllables = [(k, n) for k, n in uni.most_common() if n >= 2]
    bigrams = [(a, b, n) for (a, b), n in bi.most_common(60000) if n >= 3]
    os.makedirs(OUT, exist_ok=True)
    with open(os.path.join(OUT, "syllables.txt"), "w", encoding="utf-8") as f:
        f.write("# Dokey syllable frequencies: letters+tone digit, count. Built by scripts/build_dictionary.py\n")
        for k, n in syllables:
            f.write(f"{k}\t{n}\n")
    with open(os.path.join(OUT, "bigrams.txt"), "w", encoding="utf-8") as f:
        f.write("# Dokey syllable pairs: first, second, count. Built by scripts/build_dictionary.py\n")
        for a, b, n in bigrams:
            f.write(f"{a}\t{b}\t{n}\n")
    print(len(syllables), "syllables,", len(bigrams), "bigrams,", sum(uni.values()), "tokens")


if __name__ == "__main__":
    main(sys.argv[1])
