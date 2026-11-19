#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Draws the two shared chemical-fluid textures (stage 2 M1).

    python tools/art/m1_fluids.py [repo root]

Every chemical fluid renders with these greyscale frames and is tinted per fluid by the client, so
one still and one flowing animation cover all of them. The frames are generated from a fixed seed.
"""
import math
import os
import random
import struct
import sys
import zlib

ROOT = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else
                       os.path.join(os.path.dirname(__file__), "..", "..")).replace("\\", "/")
TEX = ROOT + "/src/main/resources/assets/iuce/textures/block/"
FRAMES = 32
SIZE = 32


def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def save(name, frames, frametime):
    rows = [(x, x, x, 255) for frame in frames for row in frame for x in row]
    flattened = []
    for frame in frames:
        flattened.extend(frame)
    raw = b"".join(b"\x00" + b"".join(bytes(pixel) for pixel in row) for row in flattened)
    data = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE * FRAMES, 8, 6, 0, 0, 0))
    data += chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    with open(TEX + name + ".png", "wb") as handle:
        handle.write(data)
    with open(TEX + name + ".png.mcmeta", "w", encoding="utf-8", newline="\n") as handle:
        handle.write('{\n  "animation": {\n    "interpolate": true,\n    "frametime": %d\n  }\n}\n' % frametime)


def main():
    rnd = random.Random(7)
    still = []
    for frame in range(FRAMES):
        rows = []
        for y in range(SIZE):
            row = []
            for x in range(SIZE):
                band = 0.5 + 0.5 * math.sin(x * 0.55 + y * 0.35 + frame * 0.45)
                value = max(120, min(255, int(150 + 70 * band) + rnd.randint(-6, 6)))
                row.append((value, value, value, 255))
            rows.append(row)
        still.append(rows)
    save("chemical_still", still, 8)

    flow = []
    for frame in range(FRAMES):
        rows = []
        for y in range(SIZE):
            row = []
            for x in range(SIZE):
                streak = 0.5 + 0.5 * math.sin(x * 1.1 - (y + frame * 2) * 0.8)
                value = max(110, min(255, int(140 + 85 * streak) + rnd.randint(-7, 7)))
                row.append((value, value, value, 255))
            rows.append(row)
        flow.append(rows)
    save("chemical_flow", flow, 4)
    print("wrote chemical_still and chemical_flow")


main()
