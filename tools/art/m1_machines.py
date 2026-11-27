#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Draws the process machines of stage 2 M1.

    python tools/art/m1_machines.py [repo root]

Three shared shells, chosen by "model" in tools/content/stage2.json, give every machine its sides,
top and bottom:
  box     a bolted steel casing with a recessed vent panel (mills, dryers)
  tank    a round vessel inside a steel frame, with weld bands (reactors, fermenters)
  column  a tall vessel with flanged tray rings (distillation, stacks)
Each machine then gets its own front, idle and running. The front shows what happens inside, in the
machine's accent colour. It also has a status lamp that turns green while the machine runs, and
MV machines carry an orange tier stripe, as GregTech marks its voltage tiers. Every random choice uses a fixed seed,
so running the script again writes the same files.
"""
import json
import math
import os
import random
import sys

from PIL import Image

ROOT = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else
                       os.path.join(os.path.dirname(__file__), "..", "..")).replace("\\", "/")
TEX = ROOT + "/src/main/resources/assets/iuce/textures/block/machine/process/"
FRAMES = 8

# dark, mid-dark, mid, light, highlight
STEEL = [(46, 50, 56), (78, 84, 92), (112, 118, 126), (148, 154, 162), (190, 195, 200)]
FRAME = [(30, 32, 36), (54, 58, 64), (78, 82, 90), (104, 110, 118), (140, 146, 152)]
GLASS_DARK = (18, 24, 30)
MV_STRIPE = (214, 124, 40)


def px(image, x, y, colour):
    if 0 <= x < 16 and 0 <= y < 16:
        image.putpixel((x, y), tuple(int(c) for c in colour[:3]) + (255,))


def rect(image, x0, y0, x1, y1, colour):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(image, x, y, colour)


def bevel(image, x0, y0, x1, y1, light, dark):
    rect(image, x0, y0, x1, y0, light)
    rect(image, x0, y0, x0, y1, light)
    rect(image, x0, y1, x1, y1, dark)
    rect(image, x1, y0, x1, y1, dark)


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def hex_colour(text):
    return tuple(int(text[i:i + 2], 16) for i in (1, 3, 5))


def bolt(image, x, y):
    px(image, x, y, STEEL[4])
    px(image, x + 1, y, STEEL[2])
    px(image, x, y + 1, STEEL[2])
    px(image, x + 1, y + 1, FRAME[0])


def noise_fill(image, x0, y0, x1, y1, low, high, seed, base=0.5, spread=0.07):
    rnd = random.Random(seed)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(image, x, y, mix(low, high, base + rnd.uniform(-spread, spread)))


# ---------------------------------------------------------------- shells

def casing(seed):
    """The common steel casing: a dark outer frame, a bevelled plate and a bolt in each corner."""
    image = Image.new("RGBA", (16, 16))
    noise_fill(image, 0, 0, 15, 15, STEEL[1], STEEL[3], seed)
    bevel(image, 0, 0, 15, 15, FRAME[2], FRAME[0])
    bevel(image, 1, 1, 14, 14, STEEL[4], STEEL[1])
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        bolt(image, x, y)
    return image


def vent(image, x0, y0, x1, y1):
    """A recessed grille: dark slots between lit slats."""
    rect(image, x0, y0, x1, y1, FRAME[1])
    for y in range(y0 + 1, y1, 2):
        rect(image, x0 + 1, y, x1 - 1, y, FRAME[0])
    bevel(image, x0, y0, x1, y1, STEEL[1], STEEL[4])


def box_side():
    image = casing(21)
    vent(image, 4, 4, 11, 11)
    return image


def box_top():
    image = casing(22)
    # a round cooling fan under a grille
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 1.3:
                px(image, x, y, STEEL[3])
            elif d < 3.6:
                px(image, x, y, FRAME[0] if (x + y) % 2 else FRAME[2])
            elif d < 4.3:
                px(image, x, y, STEEL[4] if x + y < 15 else FRAME[1])
    return image


def plain_bottom(seed):
    image = casing(seed)
    rect(image, 4, 7, 11, 8, STEEL[1])
    rect(image, 4, 7, 11, 7, STEEL[0])
    return image


def vessel_column(x, left=2, right=13):
    """Brightness for a cylinder seen side-on: lit from the upper left, dark at the far edge."""
    t = (x - left) / float(right - left)
    return 0.35 + 0.55 * math.sin(math.pi * min(1.0, t * 1.15)) - 0.2 * t


def vessel(seed, band_rows):
    image = Image.new("RGBA", (16, 16))
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            px(image, x, y, mix(STEEL[0], STEEL[4], vessel_column(x, 0, 15) + rnd.uniform(-0.03, 0.03)))
    for row in band_rows:
        for x in range(16):
            shade = vessel_column(x, 0, 15)
            px(image, x, row, mix(STEEL[1], STEEL[4], shade + 0.15))
            px(image, x, row + 1, mix(FRAME[0], STEEL[2], shade))
    return image


def tank_side():
    image = vessel(31, (3, 11))
    # the frame posts at both edges hold the vessel
    for x in (0, 15):
        rect(image, x, 0, x, 15, FRAME[1])
    rect(image, 0, 0, 15, 0, FRAME[2])
    rect(image, 0, 15, 15, 15, FRAME[0])
    for y in (4, 12):
        px(image, 7, y, STEEL[4])
    return image


def column_side():
    image = vessel(41, (1, 6, 11))
    # flanges stick out a little and carry bolts, as on a tray column
    for row in (1, 6, 11):
        for x in (2, 6, 10, 13):
            px(image, x, row, STEEL[4])
    rect(image, 0, 0, 0, 15, FRAME[1])
    rect(image, 15, 0, 15, 15, FRAME[0])
    return image


def lid(seed, nozzle):
    """A round lid seen from above, bolted round its rim; nozzle adds a central outlet."""
    image = casing(seed)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            if d < 6.3:
                light = 0.55 - 0.05 * (dx + dy)
                px(image, x, y, mix(STEEL[1], STEEL[4], light))
            if 5.4 <= d < 6.3:
                px(image, x, y, STEEL[4] if dx + dy < 0 else FRAME[1])
    for angle in range(0, 360, 45):
        bx = int(round(7.5 + 4.6 * math.cos(math.radians(angle))))
        by = int(round(7.5 + 4.6 * math.sin(math.radians(angle))))
        px(image, bx, by, FRAME[0])
    if nozzle:
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - 7.5)
                if d < 1.4:
                    px(image, x, y, GLASS_DARK)
                elif d < 2.5:
                    px(image, x, y, STEEL[4] if x + y < 15 else STEEL[1])
    else:
        rect(image, 6, 7, 9, 8, STEEL[3])            # the manhole handle
        rect(image, 6, 8, 9, 8, FRAME[1])
    return image


# ---------------------------------------------------------------- front parts

def lamp(image, x, y, on):
    """IC2-style status lamp: dark when idle, green when running."""
    px(image, x, y, (120, 240, 120) if on else (34, 48, 34))
    px(image, x + 1, y, (60, 190, 70) if on else (26, 36, 26))
    px(image, x, y + 1, (60, 190, 70) if on else (26, 36, 26))
    px(image, x + 1, y + 1, (30, 120, 40) if on else (20, 26, 20))


def tier_stripe(image, voltage):
    if voltage != "MV":
        return
    for x in range(2, 11):              # stops short of the status lamp
        px(image, x, 14, MV_STRIPE if (x // 2) % 2 == 0 else FRAME[0])


def sight_glass(image, x0, y0, x1, y1, liquid, level, frame, bubbles, seed):
    """A framed window with liquid filling it from the bottom. Rising bubbles when running."""
    rect(image, x0, y0, x1, y1, GLASS_DARK)
    surface = y1 - int(round((y1 - y0) * level))
    for y in range(y0 + 1, y1):
        for x in range(x0 + 1, x1):
            if y >= surface:
                depth = (y - surface) / float(max(1, y1 - surface))
                px(image, x, y, mix(liquid, mix(liquid, (0, 0, 0), 0.6), depth * 0.7))
            else:
                px(image, x, y, mix(GLASS_DARK, (70, 86, 96), 0.25 if x == x0 + 1 else 0.0))
    for x in range(x0 + 1, x1):
        px(image, x, surface, mix(liquid, (255, 255, 255), 0.35))
    if bubbles:
        rnd = random.Random(seed)
        height = y1 - surface - 1
        for _ in range(3):
            bx = rnd.randint(x0 + 1, x1 - 1)
            start = rnd.randint(0, max(1, height))
            by = y1 - 1 - (start + frame) % max(1, height)
            px(image, bx, by, mix(liquid, (255, 255, 255), 0.6))
    # glare on the glass and the frame round it
    px(image, x0 + 1, y0 + 1, (150, 170, 180))
    bevel(image, x0, y0, x1, y1, FRAME[0], STEEL[4])


def round_window(image, cx, cy, radius, fill):
    """A porthole: fill(x, y, distance) paints the inside; the rim is drawn here."""
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - cx, y - cy)
            if d < radius - 0.8:
                px(image, x, y, fill(x, y, d))
            elif d < radius + 0.4:
                px(image, x, y, FRAME[0] if x + y < cx + cy else STEEL[4])


def blades(phase, colour, dark):
    def fill(x, y, d):
        if d < 1.0:
            return STEEL[3]
        angle = (math.degrees(math.atan2(y - 7.5, x - 7.5)) - phase) % 120.0
        return colour if angle < 40 else dark
    return fill


# ---------------------------------------------------------------- fronts

def front_wet_mill(m, active, frame):
    image = casing(51)
    accent = hex_colour(m["accent"])
    stone = [(96, 92, 86), (132, 128, 120), (166, 162, 152)]

    def millstone(x, y, d):
        angle = (math.degrees(math.atan2(y - 6.5, x - 7.5)) - (frame * 15 if active else 0)) % 60.0
        if d < 1.2:
            return FRAME[1]
        groove = angle < 8
        base = stone[0] if groove else stone[1 + (int(d) % 2)]
        return mix(base, accent, 0.25) if active and d > 3.5 else base
    round_window(image, 7.5, 6.5, 4.5, millstone)
    # slurry outlet trough below
    rect(image, 4, 12, 11, 13, FRAME[0])
    if active:
        rect(image, 5, 12, 10, 12, accent)
        px(image, 4 + (frame % 6) + 1, 13, mix(accent, (255, 255, 255), 0.4))
    lamp(image, 12, 12, active)
    tier_stripe(image, m["voltage"])
    return image


def front_tank_window(m, active, frame, level, seed, extra=None):
    image = vessel(61 + seed, (2, 13))
    accent = hex_colour(m["accent"])
    sight_glass(image, 4, 3, 11, 12, accent, level, frame, active, seed)
    if extra:
        extra(image, active, frame)
    lamp(image, 12, 13, active)
    tier_stripe(image, m["voltage"])
    return image


def heater_glow(image, active, frame):
    """A row of heating elements along the bottom of the glass."""
    for x in range(5, 11):
        hot = active and (x + frame // 2) % 3 != 0
        px(image, x, 11, (240, 120, 40) if hot else (70, 40, 30))


def stirrer(image, active, frame):
    shaft = (150, 154, 160)
    rect(image, 7, 4, 8, 10, shaft)
    swing = [(-2, 2), (-1, 1), (0, 0), (1, -1), (2, -2), (1, -1), (0, 0), (-1, 1)][frame if active else 2]
    for dx in range(0, 3):
        px(image, 7 - dx + (swing[0] if dx else 0) // 2, 9, shaft)
        px(image, 8 + dx + (swing[1] if dx else 0) // 2, 9, shaft)


def foam(image, active, frame):
    """Fermentation foam on the surface, thicker while yeast works."""
    if not active:
        return
    for x in range(5, 11):
        if (x + frame) % 3:
            px(image, x, 5, (236, 226, 200))


def catalyst_bed(image, active, frame):
    """Pellets of activated alumina packed in the window; they glow while hot."""
    rect(image, 5, 4, 10, 11, (46, 44, 40))
    for y in range(4, 12):
        for x in range(5, 11):
            if (x + y) % 2 == 0:
                hot = active and (x * 3 + y + frame) % 7 < 3
                px(image, x, y, (236, 150, 70) if hot else (214, 214, 204))
    bevel(image, 4, 3, 11, 12, FRAME[0], STEEL[4])


def front_column(m, active, frame):
    image = column_side()
    accent = hex_colour(m["accent"])
    # two small sight glasses on different trays: vapour on top, condensed product below
    sight_glass(image, 4, 2, 9, 5, accent, 0.0, frame, False, 71)
    if active:
        for x in range(5, 9):
            if (x + frame) % 3 == 0:
                px(image, x, 3 + frame % 2, mix(accent, (255, 255, 255), 0.5))
    sight_glass(image, 4, 7, 9, 10, mix(accent, (230, 200, 120), 0.4), 0.6, frame, active, 72)
    # a pressure gauge
    for y in range(7, 12):
        for x in range(10, 15):
            d = math.hypot(x - 12, y - 9)
            if d < 1.6:
                px(image, x, y, (230, 228, 214))
            elif d < 2.4:
                px(image, x, y, FRAME[0])
    px(image, 12, 9, (40, 30, 24))
    px(image, 13 if active else 11, 8 if active else 10, (200, 40, 30))
    lamp(image, 11, 13, active)
    tier_stripe(image, m["voltage"])
    return image


def front_dryer(m, active, frame):
    image = casing(81)
    accent = hex_colour(m["accent"])

    def drum(x, y, d):
        angle = (math.degrees(math.atan2(y - 7.0, x - 7.5)) - (frame * 45 if active else 0)) % 90.0
        base = mix(FRAME[1], STEEL[2], 0.4) if angle < 20 else FRAME[0]
        if y >= 8:                       # the grains tumble along the bottom of the drum
            base = mix(accent, (0, 0, 0), 0.15 * ((x + y + (frame if active else 0)) % 3))
        return base
    round_window(image, 7.5, 7.0, 5.0, drum)
    # heater element above the drum
    for x in range(4, 12):
        hot = active and (x + frame) % 4 != 0
        px(image, x, 1, (240, 120, 40) if hot else (80, 44, 32))
    lamp(image, 12, 12, active)
    tier_stripe(image, m["voltage"])
    return image


def front_vent_stack(m, active, frame):
    image = column_side()
    # hazard stripes and an inlet nozzle
    for y in range(2, 6):
        for x in range(2, 14):
            px(image, x, y, (220, 180, 40) if ((x + y) // 2) % 2 == 0 else (30, 30, 30))
    bevel(image, 2, 2, 13, 5, FRAME[0], STEEL[4])
    for y in range(7, 14):
        for x in range(3, 13):
            d = math.hypot(x - 7.5, y - 10.0)
            if d < 1.6:
                px(image, x, y, GLASS_DARK)
            elif d < 2.8:
                px(image, x, y, STEEL[4] if x + y < 17 else FRAME[1])
    if active:
        # a small pilot flame seen through the nozzle
        flicker = [(255, 220, 110), (255, 160, 60), (240, 100, 30)]
        px(image, 7, 10, flicker[frame % 3])
        px(image, 8, 10, flicker[(frame + 1) % 3])
        px(image, 7 + frame % 2, 9, flicker[(frame + 2) % 3])
    lamp(image, 12, 12, active)
    return image


FRONTS = {
    "wet_mill": front_wet_mill,
    "liquefaction_tank": lambda m, a, f: front_tank_window(m, a, f, 0.7, 1, heater_glow),
    "saccharification_tank": lambda m, a, f: front_tank_window(m, a, f, 0.75, 2, stirrer),
    "fermenter": lambda m, a, f: front_tank_window(m, a, f, 0.7, 3, foam),
    "distillation_column": front_column,
    "dehydration_reactor": lambda m, a, f: front_tank_window(m, a, f, 0.0, 4, catalyst_bed),
    "dryer": front_dryer,
    "vent_stack": front_vent_stack,
}

SHELLS = {
    "box": (box_side, box_top, lambda: plain_bottom(23)),
    "tank": (tank_side, lambda: lid(32, False), lambda: plain_bottom(33)),
    "column": (column_side, lambda: lid(42, True), lambda: plain_bottom(43)),
}


def save(name, frames, frametime=2):
    os.makedirs(TEX, exist_ok=True)
    meta = TEX + name + ".png.mcmeta"
    if len(frames) == 1:
        frames[0].save(TEX + name + ".png")
        if os.path.exists(meta):
            os.remove(meta)
        return
    strip = Image.new("RGBA", (16, 16 * len(frames)))
    for index, frame in enumerate(frames):
        strip.paste(frame, (0, 16 * index))
    strip.save(TEX + name + ".png")
    with open(meta, "w", encoding="utf-8", newline="\n") as handle:
        handle.write('{\n  "animation": {\n    "frametime": %d\n  }\n}\n' % frametime)


def main():
    with open(ROOT + "/tools/content/stage2.json", encoding="utf-8") as handle:
        machines = json.load(handle)["machines"]
    used = {m.get("model", "box") for m in machines}
    for model in sorted(used):
        side, top, bottom = SHELLS[model]
        save(model + "_side", [side()])
        save(model + "_top", [top()])
        save(model + "_bottom", [bottom()])
    for m in machines:
        draw = FRONTS[m["id"]]
        save(m["id"] + "_front", [draw(m, False, 0)])
        save(m["id"] + "_front_active", [draw(m, True, frame) for frame in range(FRAMES)])


if __name__ == "__main__":
    main()
