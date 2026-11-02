#!/usr/bin/env python3
"""Downloads public-domain Vietnamese text from vi.wikisource.org for the syllable dictionary.

Only texts outside copyright are used: Vietnamese legal documents (not protected under Article 15 of the
Vietnamese Law on Intellectual Property) and literature whose authors died long ago (Nguyễn Du, Nguyễn Đình
Chiểu, Ngô Tất Tố, Thạch Lam, Nam Phong tạp chí 1917–1934).
Usage: python3 scripts/fetch_corpus.py OUT_DIR
"""
import json
import os
import re
import sys
import time
import urllib.parse
import urllib.request

API = "https://vi.wikisource.org/w/api.php"
UA = {"User-Agent": "DokeyDictionaryBuilder/1.0 (https://github.com/dhtoan/Dokey-Android)"}

TITLES = ["Truyện Kiều", "Hiến pháp nước Cộng hòa xã hội chủ nghĩa Việt Nam 2013"]
PREFIXES = [
    "Tắt đèn/", "Hà Nội băm sáu phố phường/", "Nam Phong tạp chí/", "Lục Vân Tiên (bản Quốc ngữ 2082 câu)/",
]
LAW_PREFIXES = ["Bộ luật ", "Luật "]
MAX_LAWS = 160


def get(params):
    url = API + "?" + urllib.parse.urlencode({**params, "format": "json"})
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
        return json.load(r)


def all_pages(prefix, limit=None):
    out, cont = [], {}
    while True:
        d = get({"action": "query", "list": "allpages", "apprefix": prefix, "aplimit": 500, **cont})
        out += [p["title"] for p in d["query"]["allpages"]]
        if "continue" not in d or (limit and len(out) >= limit):
            return out[:limit] if limit else out
        cont = d["continue"]


def raw(title):
    url = "https://vi.wikisource.org/w/index.php?" + urllib.parse.urlencode({"title": title, "action": "raw"})
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
        return r.read().decode("utf-8", "replace")


def main(out_dir):
    os.makedirs(out_dir, exist_ok=True)
    titles = list(TITLES)
    for p in PREFIXES:
        titles += all_pages(p)
    for p in LAW_PREFIXES:
        # Whole laws only (sub-pages are chapters of the same text).
        titles += [t for t in all_pages(p) if "/" not in t][: MAX_LAWS // 2]
    print(len(titles), "pages", file=sys.stderr)
    for i, t in enumerate(titles):
        path = os.path.join(out_dir, f"{i:04d}.txt")
        if os.path.exists(path):
            continue
        try:
            text = raw(t)
        except Exception as e:  # skip missing pages
            print("skip", t, e, file=sys.stderr)
            continue
        if text.lstrip().lower().startswith("#redirect") or text.lstrip().startswith("#đổi"):
            continue
        with open(path, "w", encoding="utf-8") as f:
            f.write(text)
        time.sleep(0.2)
    print("done", file=sys.stderr)


if __name__ == "__main__":
    main(sys.argv[1])
