#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Draws the utility blocks of stage 2 M1 and writes their block assets.

    python tools/art/m1_utility.py [repo root]

Covers the steam boiler, the cooling tower, the creative utility source and the three utility pipes.
The look follows GregTech's steam age: a riveted bronze boiler with a fire grate that glows while it
burns, a louvred cooling tower with a fan that turns while it runs, and round pipes with a dark ring at
open ends. Pipes use the industrial colour code: bronze steam, green water, light-blue air.
Every random choice uses a fixed seed, so running the script again writes the same files.
"""
import json
import math
import os
import random
import sys

from PIL import Image

ROOT = os.path.abspath(sys.argv[1] if len(sys.argv) > 1 else
                       os.path.join(os.path.dirname(__file__), "..", "..")).replace("\\", "/")
ASSETS = ROOT + "/src/main/resources/assets/iuce/"
DATA = ROOT + "/src/main/resources/data/"
TEX = ASSETS + "textures/block/machine/utility/"
T = "iuce:block/machine/utility/"

# dark, mid-dark, mid, light, highlight
BRONZE = [(74, 44, 22), (124, 76, 36), (170, 108, 50), (208, 146, 76), (238, 190, 120)]
STEEL = [(52, 56, 60), (88, 94, 100), (124, 130, 136), (160, 166, 172), (196, 200, 204)]
PIPES = {
    "steam_pipe": BRONZE,
    "cooling_water_pipe": [(18, 58, 30), (36, 98, 52), (56, 138, 74), (88, 172, 104), (140, 208, 150)],
    "air_pipe": [(30, 62, 92), (56, 104, 144), (88, 146, 190), (128, 182, 220), (186, 220, 240)],
}


def write_json(path, value, indent=2):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        json.dump(value, handle, indent=indent, ensure_ascii=False)
        handle.write("\n")


def px(image, x, y, colour):
    image.putpixel((x, y), tuple(colour[:3]) + (255,))


def rect(image, x0, y0, x1, y1, colour):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(image, x, y, colour)


def bevel(image, x0, y0, x1, y1, light, dark):
    """Raised edge: light on the top and left, dark on the bottom and right."""
    rect(image, x0, y0, x1, y0, light)
    rect(image, x0, y0, x0, y1, light)
    rect(image, x0, y1, x1, y1, dark)
    rect(image, x1, y0, x1, y1, dark)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def rivet(image, x, y, palette):
    px(image, x, y, palette[4])
    px(image, x + 1, y + 1, palette[0])
    px(image, x + 1, y, palette[2])
    px(image, x, y + 1, palette[2])


def plate(seed, palette=BRONZE):
    """A riveted metal plate: soft noise, a bevelled edge and rivets in the corners."""
    rnd = random.Random(seed)
    image = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            t = 0.45 + 0.1 * math.sin((x + y) * 0.5) + rnd.uniform(-0.06, 0.06)
            px(image, x, y, mix(palette[1], palette[3], t))
    bevel(image, 0, 0, 15, 15, palette[3], palette[0])
    bevel(image, 1, 1, 14, 14, palette[4], palette[1])
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        rivet(image, x, y, palette)
    return image


def save(name, frames, frametime=None):
    os.makedirs(TEX, exist_ok=True)
    if len(frames) == 1:
        frames[0].save(TEX + name + ".png")
        if os.path.exists(TEX + name + ".png.mcmeta"):
            os.remove(TEX + name + ".png.mcmeta")
        return
    strip = Image.new("RGBA", (16, 16 * len(frames)))
    for index, frame in enumerate(frames):
        strip.paste(frame, (0, 16 * index))
    strip.save(TEX + name + ".png")
    with open(TEX + name + ".png.mcmeta", "w", encoding="utf-8", newline="\n") as handle:
        handle.write('{\n  "animation": {\n    "frametime": %d\n  }\n}\n' % frametime)


# ---------------------------------------------------------------- boiler

def boiler_side():
    image = plate(3)
    # a horizontal seam strap across the shell
    rect(image, 2, 7, 13, 8, BRONZE[2])
    rect(image, 2, 7, 13, 7, BRONZE[4])
    rect(image, 2, 9, 13, 9, BRONZE[0])
    for x in (4, 8, 11):
        rivet(image, x, 7, BRONZE)
    return image


def boiler_front(fire_frame):
    """Firebox door. fire_frame is None for a cold boiler, else the flicker frame number."""
    image = plate(5)
    # nameplate, top left: a long dark plate with engraved lines
    rect(image, 3, 3, 8, 5, BRONZE[0])
    rect(image, 4, 4, 7, 4, BRONZE[2])
    # round pressure gauge, top right; the needle swings up while the fire burns
    for y in range(2, 8):
        for x in range(9, 15):
            d = math.hypot(x - 11.5, y - 4.5)
            if d < 1.7:
                px(image, x, y, (232, 228, 210))
            elif d < 2.6:
                px(image, x, y, BRONZE[4] if x + y < 16 else BRONZE[0])
    needle = (12, 4) if fire_frame is not None else (11, 5)
    px(image, 11, 4, (40, 30, 24))
    px(image, needle[0], needle[1], (190, 30, 24))
    # firebox opening with brick lining
    rect(image, 3, 8, 12, 13, (96, 40, 30))
    for y in range(8, 14):
        for x in range(3, 13):
            if (y - 8) % 2 == 1 or (x + (y // 2) * 2) % 4 == 0:
                px(image, x, y, (60, 26, 20))
    rnd = random.Random(40 + (fire_frame or 0))
    for y in range(9, 13):
        for x in range(4, 12):
            if fire_frame is None:
                shade = 18 + rnd.randint(0, 10)
                px(image, x, y, (shade + 6, shade, shade))
            else:
                heat = (y - 9) / 3.0 + rnd.uniform(-0.25, 0.25)
                px(image, x, y, mix((255, 230, 120), (220, 70, 10), max(0.0, min(1.0, 1.0 - heat))))
    # cast-iron grate bars over the opening
    for x in (5, 7, 9, 11):
        rect(image, x, 9, x, 12, (40, 36, 34))
    bevel(image, 3, 8, 12, 13, BRONZE[0], BRONZE[4])
    return image


def boiler_top():
    image = plate(7)
    # steam outlet: a raised flange with a dark bore
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 1.8:
                px(image, x, y, (20, 16, 14))
            elif d < 3.0:
                px(image, x, y, BRONZE[0] if x + y > 15 else BRONZE[1])
            elif d < 4.2:
                px(image, x, y, BRONZE[4] if x + y < 15 else BRONZE[2])
    return image


# ---------------------------------------------------------------- cooling tower

def tower_side(active, frame=0):
    """Louvred side wall. While running, water runs down behind the slats."""
    image = plate(11, STEEL)
    for y in range(3, 13):
        for x in range(2, 14):
            if (y - 3) % 3 == 0:
                px(image, x, y, STEEL[4])           # slat top edge
            elif (y - 3) % 3 == 1:
                px(image, x, y, STEEL[2])           # slat face
            else:
                if active:
                    drip = (x * 7 + y * 3 + frame * 5) % 11 < 3
                    px(image, x, y, (150, 200, 235) if drip else (46, 96, 140))
                else:
                    px(image, x, y, (26, 30, 34))   # gap shadow
    bevel(image, 2, 3, 13, 12, STEEL[0], STEEL[4])
    return image


def tower_front(active, frame=0):
    image = tower_side(active, frame)
    # water inlet flange low on the front
    for y in range(9, 15):
        for x in range(5, 11):
            d = math.hypot(x - 7.5, y - 11.5)
            if d < 1.4:
                px(image, x, y, (30, 60, 90) if not active else (70, 140, 200))
            elif d < 2.6:
                px(image, x, y, STEEL[4] if x + y < 19 else STEEL[1])
            elif d < 3.1:
                px(image, x, y, STEEL[0])
    return image


def tower_top(phase):
    """Fan in a round shroud seen from above; phase turns the blades (degrees)."""
    image = plate(13, STEEL)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            if d > 7.0:
                continue
            if d > 6.0:
                px(image, x, y, STEEL[4] if dx + dy < 0 else STEEL[0])
            elif d < 1.6:
                px(image, x, y, STEEL[3] if dx + dy < 0 else STEEL[1])
            else:
                angle = (math.degrees(math.atan2(dy, dx)) - phase) % 90.0
                if angle < 34.0:
                    px(image, x, y, STEEL[3] if angle < 10.0 else STEEL[2])
                else:
                    px(image, x, y, (22, 26, 30))
    return image


# ---------------------------------------------------------------- creative source

def creative_face():
    image = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            px(image, x, y, (60, 20, 70) if (x // 2 + y // 2) % 2 == 0 else (40, 12, 48))
    bevel(image, 0, 0, 15, 15, (220, 120, 240), (110, 40, 130))
    ports = [PIPES["steam_pipe"], PIPES["cooling_water_pipe"], PIPES["air_pipe"]]
    for index, palette in enumerate(ports):
        cx = 3.5 + index * 4.5
        for y in range(5, 12):
            for x in range(16):
                d = math.hypot(x - cx, y - 8)
                if d < 1.2:
                    px(image, x, y, palette[0])
                elif d < 2.2:
                    px(image, x, y, palette[4] if x - cx + y - 8 < 0 else palette[2])
    return image


# ---------------------------------------------------------------- pipes

# Pipe faces are 8 pixels across (4..11). Brightness across the width makes the round body.
ACROSS = [0, 1, 3, 4, 3, 2, 2, 1]


def pipe_body(palette, seed):
    """Shading runs across the columns 4..11 and is constant along the rows (the pipe's length)."""
    rnd = random.Random(seed)
    image = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            column = palette[ACROSS[(x - 4) % 8]] if 4 <= x <= 11 else palette[1]
            if rnd.random() < 0.08 and 5 <= x <= 10:
                column = mix(column, palette[0], 0.25)
            px(image, x, y, column)
    return image


def pipe_end(palette):
    """An open end: outer wall, a bright lip and the dark bore, all inside 4..11."""
    image = pipe_body(palette, 0)
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 3.9:
                px(image, x, y, palette[0])
            elif d > 3.0:
                px(image, x, y, palette[3] if x + y < 15 else palette[1])
            elif d > 2.0:
                px(image, x, y, palette[1] if x + y < 15 else palette[2])
            else:
                px(image, x, y, mix((14, 14, 16), palette[0], 0.3))
    return image


def pipe_cap(palette):
    """A blanked fitting: a rim and a flat plate where a pipe could join later."""
    image = pipe_body(palette, 0)
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 3.9:
                px(image, x, y, palette[0])
            elif d > 3.0:
                px(image, x, y, palette[3] if x + y < 15 else palette[1])
            else:
                px(image, x, y, palette[2] if x + y < 15 else mix(palette[1], palette[2], 0.5))
    px(image, 6, 6, palette[4])
    return image


def arm_faces(name, length_from, length_to):
    """Faces for a pipe piece along z, from length_from to length_to, with the end cap facing north."""
    body = T + name
    across = T + name + "_across"
    return {
        "north": {"uv": [4, 4, 12, 12], "texture": "#end"},
        "south": {"uv": [4, 4, 12, 12], "texture": "#end"},
        "east": {"uv": [16 - length_to, 4, 16 - length_from, 12], "texture": "#across"},
        "west": {"uv": [length_from, 4, length_to, 12], "texture": "#across"},
        "up": {"uv": [4, length_from, 12, length_to], "texture": "#body"},
        "down": {"uv": [4, 16 - length_to, 12, 16 - length_from], "texture": "#body"},
    }, {"body": body, "across": across, "end": T + name + "_end", "particle": body}


def pipe_models(name):
    # Junction core: a collar on every side, applied only where the pipe branches or ends.
    core_faces = {side: {"uv": [4, 4, 12, 12], "texture": "#cap"}
                  for side in ("north", "south", "east", "west", "up", "down")}
    core = {"parent": "minecraft:block/block", "textures": {"cap": T + name + "_cap", "particle": T + name},
            "elements": [{"from": [4, 4, 4], "to": [12, 12, 12], "faces": core_faces}]}
    # Straight-run body along z, rotated by the blockstate for x and y runs. Its ends are blanked so
    # a lone pipe that stops mid-block is closed instead of showing a hole into the block.
    straight_faces, straight_textures = arm_faces(name, 4, 12)
    straight_faces["north"] = {"uv": [4, 4, 12, 12], "texture": "#cap"}
    straight_faces["south"] = {"uv": [4, 4, 12, 12], "texture": "#cap"}
    straight_textures = dict(straight_textures, cap=T + name + "_cap")
    straight = {"parent": "minecraft:block/block", "textures": straight_textures,
                "elements": [{"from": [4, 4, 4], "to": [12, 12, 12], "faces": straight_faces}]}
    faces, side_textures = arm_faces(name, 0, 4)
    faces.pop("south")                      # buried in the core
    # Nothing at the outer end either: pipes never occlude, so a disc here is not culled and would
    # sit coplanar with the next pipe's disc, which is what makes a joined run flicker.
    faces.pop("north")
    side = {"parent": "minecraft:block/block", "textures": side_textures,
            "elements": [{"from": [4, 4, 0], "to": [12, 12, 4], "faces": faces}]}
    item_faces, item_textures = arm_faces(name, 0, 16)
    item = {"parent": "minecraft:block/block", "textures": item_textures,
            "elements": [{"from": [4, 4, 0], "to": [12, 12, 16], "faces": item_faces}]}
    return {name + "_core": core, name + "_straight": straight, name + "_side": side, name + "_item": item}


# ---------------------------------------------------------------- json

def cube_model(front, side, top, bottom):
    faces = {
        "north": {"texture": "#front", "cullface": "north"},
        "south": {"texture": "#side", "cullface": "south"},
        "east": {"texture": "#side", "cullface": "east"},
        "west": {"texture": "#side", "cullface": "west"},
        "up": {"texture": "#top", "cullface": "up"},
        "down": {"texture": "#bottom", "cullface": "down"},
    }
    return {"parent": "minecraft:block/block",
            "textures": {"particle": side, "front": T + front, "side": T + side if ":" not in side else side,
                         "top": T + top, "bottom": T + bottom},
            "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]}


def machine_blockstate(name):
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for running in ("false", "true"):
            model = {"model": "iuce:block/" + name + ("_active" if running == "true" else "")}
            if y:
                model["y"] = y
            variants["facing=%s,running=%s" % (facing, running)] = model
    return {"variants": variants}


AXES = {"z": ("north", "south"), "x": ("east", "west"), "y": ("up", "down")}


def pipe_blockstate(name):
    """Everything that runs along a single axis shows the seamless straight body; every other shape
    (a lone pipe, a bend, a T, a cross) gets the capped core, so a junction never has a hollow middle
    for the neighbouring block to show through."""
    sides = [side for pair in AXES.values() for side in pair]
    straight_by_axis = {axis: [] for axis in AXES}
    core_states = []
    for mask in range(64):
        state = {side: bool(mask & (1 << index)) for index, side in enumerate(sides)}
        joined = {side for side in sides if state[side]}
        # One axis only: every joined side belongs to that pair, and at least one of them joins.
        axis = next((a for a, pair in AXES.items()
                     if joined and joined <= set(pair)), None)
        if axis is not None:
            straight_by_axis[axis].append(state)
        else:
            core_states.append(state)
    parts = [{"when": {"OR": core_states}, "apply": {"model": "iuce:block/" + name + "_core"}}]
    for axis, rotation in (("z", {}), ("x", {"y": 90}), ("y", {"x": 90})):
        parts.append({"when": {"OR": straight_by_axis[axis]},
                      "apply": dict({"model": "iuce:block/" + name + "_straight"}, **rotation)})
    side_model = "iuce:block/" + name + "_side"
    for direction, rotation in (("north", {}), ("east", {"y": 90}), ("south", {"y": 180}), ("west", {"y": 270}),
                                ("up", {"x": -90}), ("down", {"x": 90})):
        parts.append({"when": {direction: "true"}, "apply": dict({"model": side_model}, **rotation)})
    return {"multipart": parts}


def loot(name):
    return {"type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                       "conditions": [{"condition": "minecraft:survives_explosion"}],
                       "entries": [{"type": "minecraft:item", "name": "iuce:" + name}]}],
            "random_sequence": "iuce:blocks/" + name}


def main():
    save("boiler_side", [boiler_side()])
    save("boiler_front", [boiler_front(None)])
    save("boiler_front_active", [boiler_front(frame) for frame in range(4)], 3)
    save("boiler_top", [boiler_top()])
    save("boiler_bottom", [plate(9)])
    save("cooling_tower_side", [tower_side(False)])
    save("cooling_tower_side_active", [tower_side(True, frame) for frame in range(11)], 2)
    save("cooling_tower_front", [tower_front(False)])
    save("cooling_tower_front_active", [tower_front(True, frame) for frame in range(11)], 2)
    save("cooling_tower_top", [tower_top(0)])
    save("cooling_tower_top_active", [tower_top(phase * 22.5) for phase in range(4)], 1)
    save("cooling_tower_bottom", [plate(15, STEEL)])
    save("creative_utility_source", [creative_face()])
    for index, (name, palette) in enumerate(PIPES.items()):
        body = pipe_body(palette, 100 + index)
        save(name, [body])
        save(name + "_across", [body.transpose(Image.Transpose.TRANSPOSE)])
        save(name + "_end", [pipe_end(palette)])
        save(name + "_cap", [pipe_cap(palette)])

    models = {
        "boiler": cube_model("boiler_front", "boiler_side", "boiler_top", "boiler_bottom"),
        "boiler_active": cube_model("boiler_front_active", "boiler_side", "boiler_top", "boiler_bottom"),
        "cooling_tower": cube_model("cooling_tower_front", "cooling_tower_side", "cooling_tower_top", "cooling_tower_bottom"),
        "cooling_tower_active": cube_model("cooling_tower_front_active", "cooling_tower_side_active",
                                           "cooling_tower_top_active", "cooling_tower_bottom"),
        "creative_utility_source": {"parent": "minecraft:block/cube_all", "textures": {"all": T + "creative_utility_source"}},
    }
    for model in models.values():
        if "particle" in model["textures"] and ":" not in model["textures"]["particle"]:
            model["textures"]["particle"] = T + model["textures"]["particle"]
    for name in PIPES:
        models.update(pipe_models(name))
    for name, model in models.items():
        write_json(ASSETS + "models/block/" + name + ".json", model)

    for name in ("boiler", "cooling_tower"):
        write_json(ASSETS + "blockstates/" + name + ".json", machine_blockstate(name))
    write_json(ASSETS + "blockstates/creative_utility_source.json",
               {"variants": {"": {"model": "iuce:block/creative_utility_source"}}})
    for name in PIPES:
        write_json(ASSETS + "blockstates/" + name + ".json", pipe_blockstate(name))

    blocks = ["boiler", "cooling_tower", "creative_utility_source"] + list(PIPES)
    for name in blocks:
        model = "iuce:block/" + name + ("_item" if name in PIPES else "")
        write_json(ASSETS + "items/" + name + ".json", {"model": {"type": "minecraft:model", "model": model}})
        write_json(DATA + "iuce/loot_table/blocks/" + name + ".json", loot(name))

    tag_path = DATA + "minecraft/tags/block/mineable/pickaxe.json"
    with open(tag_path, "rb") as handle:
        text = handle.read().decode("utf-8")
    newline = "\r\n" if "\r\n" in text else "\n"
    missing = [name for name in blocks if '"iuce:%s"' % name not in text]
    if missing:
        # Insert as text after the last entry so the rest of the file keeps its layout.
        end = text.index(newline, text.rindex('"'))
        text = text[:end] + "".join(',%s    "iuce:%s"' % (newline, name) for name in missing) + text[end:]
        with open(tag_path, "wb") as handle:
            handle.write(text.encode("utf-8"))


if __name__ == "__main__":
    main()
