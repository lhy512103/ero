#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Draws the sixteen-pixel item textures for the biological route (stage 2 M1).

    python tools/art/m1_items.py [repo root]

Everything here is plain Python and the standard library: each texture is painted procedurally
from a fixed seed, so re-running the script reproduces the same PNGs. The output is committed,
but keeping the script means the set can be redrawn consistently when more items join it.
"""
import os
import random
import struct
import sys
import zlib

ROOT = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else
                       os.path.join(os.path.dirname(__file__), "..", "..")).replace("\\", "/")
TEX = ROOT + "/src/main/resources/assets/iuce/textures/item/"
MODEL = ROOT + "/src/main/resources/assets/iuce/models/item/"
ITEM_DEF = ROOT + "/src/main/resources/assets/iuce/items/"


def png(path, pixels):
    raw = b""
    for row in pixels:
        raw += b"\x00" + b"".join(bytes(pixel) for pixel in row)

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    data = b"\x89PNG\r\n\x1a\n"
    data += chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw, 9))
    data += chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(data)


def blank():
    return [[(0, 0, 0, 0)] * 16 for _ in range(16)]


def color(text, alpha=255):
    text = text.lstrip("#")
    return (int(text[0:2], 16), int(text[2:4], 16), int(text[4:6], 16), alpha)


def shade(pixel, factor):
    return tuple(max(0, min(255, int(channel * factor))) for channel in pixel[:3]) + (pixel[3],)


def put(canvas, x, y, pixel):
    if 0 <= x < 16 and 0 <= y < 16:
        canvas[y][x] = pixel


def outline(canvas, pixel):
    """Adds a one-pixel border around every painted area, like the vanilla item sprites."""
    source = [row[:] for row in canvas]
    for y in range(16):
        for x in range(16):
            if source[y][x][3]:
                continue
            near = any(0 <= x + dx < 16 and 0 <= y + dy < 16 and source[y + dy][x + dx][3]
                       for dx in (-1, 0, 1) for dy in (-1, 0, 1))
            if near:
                canvas[y][x] = pixel


def bagasse(canvas, rnd):
    """Fibrous, light khaki shreds running diagonally."""
    for _ in range(9):
        x, y = rnd.randint(1, 11), rnd.randint(1, 12)
        for step in range(rnd.randint(4, 8)):
            pixel = shade(color("d8c58a"), 0.8 + 0.35 * rnd.random())
            if rnd.random() < 0.15:
                pixel = color("a8915a")
            put(canvas, x + step, y + step // 3, pixel)
            if rnd.random() < 0.35:
                put(canvas, x + step, y + step // 3 + 1, shade(pixel, 0.85))


def distillers_grains(canvas, rnd):
    """Small amber kernels in a loose heap."""
    for cy in range(5, 15, 3):
        for cx in range(3, 14, 3):
            if rnd.random() < 0.25:
                cx += 1
            for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                put(canvas, cx + dx, cy + dy, shade(color("b98a45"), 0.75 + 0.45 * rnd.random()))
            put(canvas, cx, cy, color("e0b978"))


def bottle(canvas, rnd, liquid):
    """A small flask with a cork: neck at the top, squat body, filled with {@code liquid}."""
    glass, glass_dark, glass_light = color("cfe3ea", 210), color("9fbac4", 220), color("eef7fa", 220)
    for y in range(2, 16):
        half = 1 if y < 6 else (2 if y < 8 else 4)
        for x in range(8 - half, 8 + half):
            fill = y >= 8
            put(canvas, x, y, shade(liquid, 0.78 + 0.3 * rnd.random()) if fill else glass)
        put(canvas, 8 - half, y, glass_dark)
        put(canvas, 7 + half, y, glass_dark)
    for x in range(4, 12):
        put(canvas, x, 8, glass_dark)
        put(canvas, x, 15, glass_dark)
    put(canvas, 5, 10, glass_light)
    put(canvas, 5, 11, glass_light)
    for x in range(7, 9):
        put(canvas, x, 1, color("a87a4a"))
        put(canvas, x, 2, color("8c6239"))


def pellets(canvas, rnd, base, grime=""):
    """A handful of round pellets with a light spot, optionally cracked and grimy."""
    stones = ((4, 6), (9, 5), (6, 10), (11, 10), (8, 8))
    for index, (cx, cy) in enumerate(stones):
        for y in range(16):
            for x in range(16):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                if dx * dx + dy * dy <= 6.5:
                    put(canvas, x, y, shade(base, 0.82 + 0.2 * rnd.random() - 0.05 * (dx + dy)))
        if not grime:
            put(canvas, cx - 1, cy - 1, shade(base, 1.12))
        elif index % 2 == 0:
            for step in range(3):
                put(canvas, cx - 1 + step, cy, color(grime))


def main():
    rnd = random.Random(20261119)
    canvas = blank(); bagasse(canvas, rnd); outline(canvas, color("6b5a34"))
    png(TEX + "bio/bagasse.png", canvas)
    canvas = blank(); distillers_grains(canvas, rnd); outline(canvas, color("6b4a1e"))
    png(TEX + "bio/distillers_grains.png", canvas)
    canvas = blank(); bottle(canvas, rnd, color("c79a4e"))
    png(TEX + "bio/enzyme_preparation.png", canvas)
    canvas = blank(); bottle(canvas, rnd, color("dbc37a"))
    png(TEX + "bio/yeast_culture.png", canvas)
    canvas = blank(); bottle(canvas, rnd, color("efe9d0"))
    png(TEX + "bio/lactic_culture.png", canvas)
    canvas = blank(); bottle(canvas, rnd, color("7d8f5a"))
    png(TEX + "bio/aspergillus_culture.png", canvas)
    canvas = blank(); pellets(canvas, rnd, color("e7e7e2")); outline(canvas, color("6e6e68"))
    png(TEX + "component/activated_alumina.png", canvas)
    canvas = blank(); pellets(canvas, rnd, color("8b8377"), "5c574f"); outline(canvas, color("4a463f"))
    png(TEX + "component/spent_catalyst.png", canvas)

    models = {
        "bagasse": "iuce:item/bio/bagasse",
        "distillers_grains": "iuce:item/bio/distillers_grains",
        "enzyme_preparation": "iuce:item/bio/enzyme_preparation",
        "yeast_culture": "iuce:item/bio/yeast_culture",
        "lactic_culture": "iuce:item/bio/lactic_culture",
        "aspergillus_culture": "iuce:item/bio/aspergillus_culture",
        "activated_alumina": "iuce:item/component/activated_alumina",
        "spent_catalyst": "iuce:item/component/spent_catalyst",
    }
    os.makedirs(MODEL, exist_ok=True)
    os.makedirs(ITEM_DEF, exist_ok=True)
    for name, texture in models.items():
        with open(MODEL + name + ".json", "w", encoding="utf-8", newline="\n") as handle:
            handle.write('{"parent":"minecraft:item/generated","textures":{"layer0":"%s"}}\n' % texture)
        with open(ITEM_DEF + name + ".json", "w", encoding="utf-8", newline="\n") as handle:
            handle.write('{"model":{"type":"minecraft:model","model":"iuce:item/%s"}}\n' % name)
    print("wrote %d item textures and models" % len(models))


main()
