#!/usr/bin/env python3
"""Draws illustrative SVG mockups of the keyboard for the README.

Colours come from app/src/main/java/com/dhtoan/dokey/ime/KeyboardTheme.kt and the key layout mirrors
Layouts.kt, but these are drawings, not screenshots of the app running.
Usage: python3 scripts/mockups.py  (writes docs/mockups/*.svg)
"""
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "app/src/main/java/com/dhtoan/dokey/ime/KeyboardTheme.kt")
OUT = os.path.join(ROOT, "docs/mockups")

FIELDS = ["bgTop", "bgBottom", "key", "keyFn", "keyPressed", "text", "textDim", "accent", "onAccent", "popup"]


def themes():
    src = open(SRC, encoding="utf-8").read()
    for m in re.finditer(r'KeyboardTheme\(\s*"(\w+)", "([^"]+)",(.*?)(true|false),\s*\)', src, re.S):
        values = re.findall(r"c\(0x([0-9A-Fa-f]{8})\)", m.group(3))
        if len(values) != len(FIELDS):
            continue
        t = {"id": m.group(1), "name": m.group(2), "dark": m.group(4) == "true"}
        for f, v in zip(FIELDS, values):
            a = int(v[:2], 16) / 255
            t[f] = (f"#{v[2:]}", round(a, 3))
        yield t


def fill(c):
    color, alpha = c
    return f'fill="{color}"' + (f' fill-opacity="{alpha}"' if alpha < 1 else "")


def stroke(c):
    color, alpha = c
    return f'stroke="{color}"' + (f' stroke-opacity="{alpha}"' if alpha < 1 else "")


W, SIDE, HGAP, VGAP, KEY_H, TOOLBAR = 360, 4, 5, 8, 44, 40
ROWS = [
    [(c, 1) for c in "qwertyuiop"],
    [("indent", 0.5)] + [(c, 1) for c in "asdfghjkl"] + [("indent", 0.5)],
    [("SHIFT", 1.5)] + [(c, 1) for c in "zxcvbnm"] + [("DEL", 1.5)],
    [("?123", 1.5), (",", 1), ("VI", 1), ("SPACE", 4), (".", 1), ("ENTER", 1.5)],
]
HINTS = dict(zip("qwertyuiop", "1234567890"))


def keyboard(t, y0, one_hand=None, pressed=None):
    out = []
    unit_w = W - SIDE * 2
    left = SIDE
    if one_hand:
        panel = W * 0.17
        unit_w = W - panel - SIDE
        left = panel if one_hand == "right" else SIDE
    unit = unit_w / 10
    # toolbar
    pill_t = "VI · Telex"
    out.append(f'<rect x="8" y="{y0 + 6}" width="112" height="28" rx="14" {fill(t["accent"])}/>')
    out.append(f'<text x="64" y="{y0 + 25}" text-anchor="middle" font-size="13" font-weight="700" {fill(t["onAccent"])}>{pill_t}</text>')
    sx = W - 22
    s = stroke(t["textDim"])
    out.append(f'<path d="M{sx - 7},{y0 + 17} l7,7 l7,-7" fill="none" stroke-width="1.8" stroke-linecap="round" {s}/>')
    out.append(f'<circle cx="{sx - 44}" cy="{y0 + 20}" r="6.5" fill="none" stroke-width="1.8" {s}/>')
    out.append(f'<circle cx="{sx - 44}" cy="{y0 + 20}" r="2.5" fill="none" stroke-width="1.8" {s}/>')
    out.append(f'<rect x="{sx - 97}" y="{y0 + 13}" width="18" height="14" rx="2" fill="none" stroke-width="1.8" {s}/>')
    out.append(f'<rect x="{sx - 88}" y="{y0 + 16}" width="7" height="8" rx="1" {fill(t["textDim"])}/>')
    y = y0 + TOOLBAR + VGAP / 2
    for row in ROWS:
        x = left
        for label, w in row:
            if label == "indent":
                x += w * unit
                continue
            kx, kw = x + HGAP / 2, w * unit - HGAP
            is_fn = label in ("SHIFT", "DEL", "?123", "VI", "ENTER")
            bg = t["accent"] if label == "ENTER" else t["keyPressed"] if label == pressed else t["keyFn"] if is_fn else t["key"]
            out.append(f'<rect x="{kx:.1f}" y="{y:.1f}" width="{kw:.1f}" height="{KEY_H}" rx="7" {fill(bg)}/>')
            cx, cy = kx + kw / 2, y + KEY_H / 2
            fg = t["onAccent"] if label == "ENTER" else t["text"]
            if label == "SHIFT":
                out.append(f'<path d="M{cx},{cy - 9} l8,8 h-4 v6 h-8 v-6 h-4 z" fill="none" stroke-width="1.7" stroke-linejoin="round" {stroke(fg)}/>')
            elif label == "DEL":
                out.append(f'<path d="M{cx - 11},{cy} l6,-7 h16 v14 h-16 z M{cx},{cy - 3} l6,6 M{cx},{cy + 3} l6,-6" fill="none" stroke-width="1.7" stroke-linejoin="round" stroke-linecap="round" {stroke(fg)}/>')
            elif label == "ENTER":
                out.append(f'<path d="M{cx + 9},{cy - 8} v10 h-18 m5,-5 l-5,5 l5,5" fill="none" stroke-width="1.8" stroke-linejoin="round" stroke-linecap="round" {stroke(fg)}/>')
            elif label == "SPACE":
                out.append(f'<text x="{cx:.1f}" y="{cy + 4:.1f}" text-anchor="middle" font-size="12" {fill(t["textDim"])}>Tiếng Việt</text>')
            elif label == "VI":
                out.append(f'<text x="{cx:.1f}" y="{cy + 5:.1f}" text-anchor="middle" font-size="14" font-weight="700" {fill(t["accent"])}>VI</text>')
            else:
                size = 14 if len(label) > 1 else 20
                out.append(f'<text x="{cx:.1f}" y="{cy + size * 0.35:.1f}" text-anchor="middle" font-size="{size}" {fill(fg)}>{label}</text>')
                if label in HINTS:
                    out.append(f'<text x="{kx + kw - 6:.1f}" y="{y + 12:.1f}" text-anchor="end" font-size="9" {fill(t["textDim"])}>{HINTS[label]}</text>')
            if label == pressed:
                # key preview bubble
                bw, bh = kw + 12, KEY_H * 1.1
                bx, by = cx - bw / 2, y - bh - 4
                out.append(f'<rect x="{bx:.1f}" y="{by:.1f}" width="{bw:.1f}" height="{bh:.1f}" rx="10" {fill(t["popup"])} filter="url(#sh)"/>')
                out.append(f'<text x="{cx:.1f}" y="{by + bh / 2 + 10:.1f}" text-anchor="middle" font-size="28" {fill(t["text"])}>{label}</text>')
            x += w * unit
        y += KEY_H + VGAP
    if one_hand:
        px = 0 if one_hand == "right" else W - W * 0.17
        cx = px + W * 0.085
        mid = y0 + TOOLBAR + (y - y0 - TOOLBAR) / 2
        out.append(f'<path d="M{cx - 11},{mid - 24} h22 M{cx - 11},{mid - 24} l5,-5 M{cx - 11},{mid - 24} l5,5 M{cx + 11},{mid - 24} l-5,-5 M{cx + 11},{mid - 24} l-5,5" fill="none" stroke-width="1.8" stroke-linecap="round" {s}/>')
        out.append(f'<path d="M{cx - 9},{mid + 33} l18,-18 m0,0 h-7 m7,0 v7 M{cx - 9},{mid + 33} h7 m-7,0 v-7" fill="none" stroke-width="1.8" stroke-linecap="round" {s}/>')
    return out, y + 2


def svg(t, one_hand=None, pressed="t"):
    field_h = 64
    body, end = keyboard(t, field_h, one_hand, pressed)
    h = end
    app_bg = "#1F2125" if t["dark"] else "#FAFAFC"
    app_fg = "#E8EAED" if t["dark"] else "#1B1D22"
    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {h:.0f}" width="{W}" height="{h:.0f}" font-family="Roboto, Segoe UI, Helvetica, Arial, sans-serif">',
        '<defs>',
        f'<linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="{t["bgTop"][0]}"/><stop offset="1" stop-color="{t["bgBottom"][0]}"/></linearGradient>',
        '<filter id="sh" x="-30%" y="-30%" width="160%" height="160%"><feDropShadow dx="0" dy="2" stdDeviation="3" flood-opacity="0.25"/></filter>',
        '<clipPath id="r"><rect width="100%" height="100%" rx="18"/></clipPath>',
        '</defs>',
        '<g clip-path="url(#r)">',
        f'<rect width="{W}" height="{h:.0f}" fill="{app_bg}"/>',
        f'<text x="16" y="38" font-size="18" fill="{app_fg}">Xin chào! Gõ tiếng Vi<tspan text-decoration="underline">ệt</tspan></text>',
        f'<rect x="16" y="50" width="{W - 32}" height="1.5" fill="{t["accent"][0]}"/>',
        f'<rect y="{field_h}" width="{W}" height="{h - field_h:.0f}" fill="url(#bg)"/>',
        *body,
        '</g>',
        '</svg>',
    ]
    return "\n".join(parts)


def banner(t):
    """Wide header image for the README: logo, name, tagline and a keyboard in theme [t]."""
    bw, bh = 1200, 400
    kb, kb_end = keyboard(t, 0, pressed="t")
    scale = 1.25
    kx, ky = bw - W * scale - 70, (bh - kb_end * scale) / 2
    chips = ["Telex · VNI", "12 theme", "Một tay", "Không cần mạng"]
    chip_svg, x = [], 96
    for c in chips:
        w = 22 + len(c) * 11
        chip_svg.append(f'<rect x="{x}" y="276" width="{w}" height="38" rx="19" fill="#FFFFFF" fill-opacity="0.16"/>')
        chip_svg.append(f'<text x="{x + w / 2}" y="301" text-anchor="middle" font-size="18" fill="#FFFFFF">{c}</text>')
        x += w + 12
    return "\n".join([
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {bw} {bh}" width="{bw}" height="{bh}" font-family="Roboto, Segoe UI, Helvetica, Arial, sans-serif">',
        '<defs>',
        '<linearGradient id="hero" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#1E6FE8"/><stop offset="1" stop-color="#7B4397"/></linearGradient>',
        f'<linearGradient id="bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="{t["bgTop"][0]}"/><stop offset="1" stop-color="{t["bgBottom"][0]}"/></linearGradient>',
        '<filter id="sh" x="-30%" y="-30%" width="160%" height="160%"><feDropShadow dx="0" dy="2" stdDeviation="3" flood-opacity="0.25"/></filter>',
        '<filter id="card" x="-10%" y="-10%" width="120%" height="130%"><feDropShadow dx="0" dy="10" stdDeviation="14" flood-opacity="0.35"/></filter>',
        '</defs>',
        f'<rect width="{bw}" height="{bh}" rx="28" fill="url(#hero)"/>',
        '<circle cx="1120" cy="40" r="160" fill="#FFFFFF" fill-opacity="0.06"/>',
        '<circle cx="80" cy="380" r="120" fill="#FFFFFF" fill-opacity="0.05"/>',
        # logo tile with the same "Đ" mark as the app icon
        '<g transform="translate(96,70)">',
        '<rect width="92" height="92" rx="24" fill="#FFFFFF" fill-opacity="0.18"/>',
        '<g transform="translate(-8,-8) scale(1)">',
        '<path fill="#FFFFFF" fill-rule="evenodd" d="M36,32 h14 a22,22 0 0 1 0,44 h-14 z M44,40 v28 h6 a14,14 0 0 0 0,-28 z"/>',
        '<path fill="#FFFFFF" d="M30,51 h18 v6 h-18 z"/>',
        '<path fill="#FFD166" d="M70,30 a5,5 0 1 1 0.01,0 z"/>',
        '</g></g>',
        '<text x="210" y="138" font-size="64" font-weight="700" fill="#FFFFFF">Dokey</text>',
        '<text x="96" y="218" font-size="30" fill="#FFFFFF">Bàn phím tiếng Việt cho Android</text>',
        '<text x="96" y="254" font-size="20" fill="#FFFFFF" fill-opacity="0.8">Gõ dấu ở đâu cũng đúng · nhẹ · chạy hoàn toàn trên máy</text>',
        *chip_svg,
        f'<g transform="translate({kx:.0f},{ky:.0f}) scale({scale})" filter="url(#card)">',
        f'<rect width="{W}" height="{kb_end:.0f}" rx="18" fill="url(#bg)"/>',
        *kb,
        '</g>',
        '</svg>',
    ])


def main():
    os.makedirs(OUT, exist_ok=True)
    ts = list(themes())
    assert len(ts) >= 10, f"parsed only {len(ts)} themes"
    for t in ts:
        with open(os.path.join(OUT, f"theme-{t['id']}.svg"), "w", encoding="utf-8") as f:
            f.write(svg(t))
    light = next(t for t in ts if t["id"] == "light")
    with open(os.path.join(OUT, "one-hand.svg"), "w", encoding="utf-8") as f:
        f.write(svg(light, one_hand="right", pressed=None))
    with open(os.path.join(OUT, "banner.svg"), "w", encoding="utf-8") as f:
        f.write(banner(next(t for t in ts if t["id"] == "midnight")))
    print("wrote", len(ts) + 1, "mockups:", ", ".join(t["id"] for t in ts))


if __name__ == "__main__":
    main()
