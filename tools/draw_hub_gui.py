#!/usr/bin/env python3
"""Draws the Factory Hub screen texture in the steel control-panel style of the large machines.

    python tools/draw_hub_gui.py

Writes src/main/resources/assets/iuce/textures/gui/factory_hub.png (256x256). The panel sits at
0,0 (PANEL_W x PANEL_H); the sprites the screen blits on top are listed in SPRITES and must match
the constants in client/gui/FactoryHubScreen.java.
"""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "src/main/resources/assets/iuce/textures/gui/factory_hub.png"

PANEL_W, PANEL_H = 196, 160
OUTLINE, HI, BASE, MID, SH, DK = "#1a1c1f", "#f2f4f6", "#bcc2c8", "#a4abb2", "#5f666e", "#3a4047"
SLOT, SLOT_DK = "#7d838a", "#2c3136"

# Sprite positions (u, v, w, h), shared with FactoryHubScreen.
SPRITES = {
    "star_on": (200, 0, 9, 9), "star_off": (210, 0, 9, 9),
    "lamp_green": (220, 0, 5, 5), "lamp_amber": (226, 0, 5, 5), "lamp_red": (232, 0, 5, 5),
    "gauge_good": (200, 12, 18, 3), "gauge_low": (200, 16, 18, 3),
    "bar_good": (0, 200, 176, 6), "bar_bad": (0, 208, 176, 6),
}

image = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
draw = ImageDraw.Draw(image)


def rect(x, y, w, h, colour):
    if w > 0 and h > 0:
        draw.rectangle((x, y, x + w - 1, y + h - 1), fill=colour)


def inset(x, y, w, h, fill=MID):
    rect(x, y, w, h, fill)
    rect(x, y, w - 1, 1, DK)
    rect(x, y, 1, h - 1, DK)
    rect(x + 1, y + h - 1, w - 1, 1, HI)
    rect(x + w - 1, y + 1, 1, h - 1, HI)


def slot(x, y):
    rect(x, y, 18, 18, SLOT)
    rect(x, y, 17, 1, SLOT_DK)
    rect(x, y, 1, 17, SLOT_DK)
    rect(x + 1, y + 17, 17, 1, HI)
    rect(x + 17, y + 1, 1, 17, HI)


def rivet(x, y):
    rect(x, y, 3, 3, MID)
    rect(x, y, 2, 1, HI)
    rect(x, y, 1, 2, HI)
    rect(x + 1, y + 2, 2, 1, SH)
    rect(x + 2, y + 1, 1, 2, SH)
    rect(x + 1, y + 1, 1, 1, "#8d949b")


def screen(x, y, w, h):
    """Recessed green phosphor display with scan lines, a faint grid and corner screws."""
    inset(x, y, w, h, "#2a2f35")
    rect(x + 2, y + 2, w - 4, h - 4, "#141a1d")
    rect(x + 3, y + 3, w - 6, h - 6, "#09130e")
    for yy in range(y + 3, y + h - 3, 2):
        rect(x + 3, yy, w - 6, 1, "#0c1812")
    for xx in range(x + 3, x + w - 3, 19):
        rect(xx, y + 3, 1, h - 6, "#0f2218")
    rect(x + 3, y + 3, w - 6, 1, "#1d3a2a")
    rect(x + 3, y + 3, 1, h - 6, "#1d3a2a")
    for cx, cy in ((x + 1, y + 1), (x + w - 3, y + 1), (x + 1, y + h - 3), (x + w - 3, y + h - 3)):
        rect(cx, cy, 2, 2, "#59616a")
        rect(cx, cy, 1, 1, "#8a929a")


# ------------------------------------------------------------------ panel

W, H = PANEL_W, PANEL_H
rect(1, 0, W - 2, H, OUTLINE)
rect(0, 1, W, H - 2, OUTLINE)
rect(1, 1, W - 2, H - 2, BASE)
rect(1, 1, W - 3, 2, HI)
rect(1, 1, 2, H - 3, HI)
rect(3, H - 3, W - 4, 2, SH)
rect(W - 3, 3, 2, H - 4, SH)
for yy in range(4, H - 4, 2):
    draw.line((3, yy, W - 4, yy), fill="#bfc5cb")  # opaque: PIL writes alpha instead of blending it
for cx, cy in ((5, 5), (W - 8, 5), (5, H - 8), (W - 8, H - 8)):
    rivet(cx, cy)

# Title plate with blue status pips at either end.
inset(11, 4, 174, 13, "#aab1b8")
rect(12, 5, 172, 1, "#959ca4")
for px in (13, 181):
    rect(px, 7, 2, 7, "#4b8fb8")
    rect(px, 7, 2, 1, "#7fc2e8")

# Goal display.
screen(7, 20, 182, 28)

# Product bay: 8 x 2 recessed slots, each with a gauge under it.
inset(7, 51, 182, 52, "#9aa1a8")
for row in range(2):
    for col in range(8):
        sx, sy = 10 + col * 22, 54 + row * 24
        slot(sx, sy)
        rect(sx, sy + 19, 18, 3, "#101418")
        rect(sx, sy + 19, 18, 1, "#06080a")

# Run display: label line and segmented bar.
screen(7, 106, 182, 26)
rect(10, 121, 176, 8, "#06090b")
rect(10, 122, 176, 6, "#101418")
for xx in range(10, 186, 8):
    rect(xx, 122, 1, 6, "#1b2228")

# Engraved hint strip with a warning lamp socket.
inset(7, 135, 182, 16, "#aab1b8")
inset(176, 139, 9, 9, "#2a2f35")

# ------------------------------------------------------------------ sprites


def star(ox, oy, fill, edge, shine):
    shape = ["....#....",
             "....#....",
             "...###...",
             "#########",
             ".#######.",
             "..#####..",
             "..#####..",
             ".##...##.",
             ".#.....#."]
    for y, line in enumerate(shape):
        for x, c in enumerate(line):
            if c != "#":
                continue
            border = any(not (0 <= x + dx < 9 and 0 <= y + dy < 9) or shape[y + dy][x + dx] != "#"
                         for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            image.putpixel((ox + x, oy + y), edge if border else fill)
    if shine:
        image.putpixel((ox + 4, oy + 3), shine)
        image.putpixel((ox + 3, oy + 4), shine)


star(*SPRITES["star_on"][:2], (255, 214, 64, 255), (176, 120, 20, 255), (255, 250, 200, 255))
star(*SPRITES["star_off"][:2], (74, 80, 88, 255), (46, 50, 56, 255), None)

for key, (lit, dark) in {"lamp_green": ("#7dff9a", "#1d6e34"), "lamp_amber": ("#ffd36a", "#8a5a10"),
                         "lamp_red": ("#ff8a7a", "#7a1e16")}.items():
    u, v, w, h = SPRITES[key]
    rect(u, v, w, h, dark)
    rect(u + 1, v + 1, w - 2, h - 2, lit)
    image.putpixel((u + 1, v + 1), (255, 255, 255, 255))

for key, (top, body, low) in {"gauge_good": ("#a8ffb8", "#38d860", "#1f9a40"),
                              "gauge_low": ("#ffe8a0", "#f0b030", "#b07010")}.items():
    u, v, w, h = SPRITES[key]
    rect(u, v, w, 1, top)
    rect(u, v + 1, w, 1, body)
    rect(u, v + 2, w, 1, low)

for key, (top, body, low, gap) in {"bar_good": ("#a8ffb8", "#38d860", "#1f9a40", "#1a5a2a"),
                                   "bar_bad": ("#ffb0a0", "#e8503c", "#a02818", "#5a1a12")}.items():
    u, v, w, h = SPRITES[key]
    for x in range(w):
        lit = x % 8 != 0
        rect(u + x, v, 1, h, body if lit else gap)
        rect(u + x, v, 1, 1, top if lit else gap)
        rect(u + x, v + h - 1, 1, 1, low if lit else gap)

OUT.parent.mkdir(parents=True, exist_ok=True)
image.save(OUT)
print(f"Wrote {OUT.relative_to(ROOT)}")
