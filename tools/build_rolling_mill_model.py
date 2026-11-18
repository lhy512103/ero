#!/usr/bin/env python3
"""Builds tools/models/rolling_mill.bbmodel, the formed Rolling Mill, and its textures.

    python tools/build_rolling_mill_model.py
    python tools/clean_plant_geometry.py rolling_mill
    python tools/export_plant_model.py rolling_mill

The model is written as a Blockbench project so it can be refined by hand afterwards; rerunning
this script overwrites those edits. Units are Blockbench pixels: x = -72..72 across the machine
(the controller block is -8..8), y = 0 at the bed (one block below the controller), z = 0 at the
controller's face running back along the machine. Top-level groups are the stretch sections:
head (depth 0-1), stand and bay (both modelled at depth 2), tail (modelled at depth 7-8 of a 9-long mill).
"""
import base64
import io
import json
import math
from pathlib import Path
from uuid import NAMESPACE_URL, uuid5

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "tools/models/rolling_mill.bbmodel"
TEX_DIR = ROOT / "tools/models/rolling_mill_textures"
SHARED = ROOT / "tools/models/ore_processing_plant_textures"

SHARED_TEXTURES = ["gt_steel_casing", "gt_dark_frame", "gt_hazard_stripe", "gt_copper_coil", "gt_vent_grille",
                   "gt_pipe_stainless", "gt_controller_front", "gt_hatch_input", "gt_hatch_output", "gt_hatch_energy",
                   "gt_copper_busbar", "gt_glass_frame"]
NEW_TEXTURES = ["gt_roll", "gt_hot_strip", "gt_refractory", "gt_steel_coil", "gt_lamp_red", "gt_lamp_green"]


# ------------------------------------------------------------------ textures

def draw_new_textures():
    TEX_DIR.mkdir(parents=True, exist_ok=True)
    for name in SHARED_TEXTURES:
        Image.open(SHARED / f"{name}.png").convert("RGBA").save(TEX_DIR / f"{name}.png")

    def save(name, pixel):
        image = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                image.putpixel((x, y), pixel(x, y))
        image.save(TEX_DIR / f"{name}.png")

    # Polished roll barrel: bright bands along the axis with a soft highlight.
    def roll(x, y):
        shade = [150, 168, 186, 204, 214, 206, 190, 172, 160, 150, 142, 136, 132, 138, 146, 150][y]
        if x in (0, 15):
            shade -= 40
        elif x % 5 == 2:
            shade += 10
        return (shade - 12, shade, shade + 12, 255)
    save("gt_roll", roll)

    # Hot strip: glowing orange with lighter streaks along the rolling direction.
    def hot(x, y):
        base = (255, 132, 32) if (y + x // 6) % 4 else (255, 196, 92)
        if y in (0, 15):
            base = (214, 82, 18)
        return (*base, 255)
    save("gt_hot_strip", hot)

    # Furnace refractory: dark red firebrick in a running bond.
    def brick(x, y):
        mortar = y % 4 == 3 or (x + (8 if (y // 4) % 2 else 0)) % 8 == 7
        if mortar:
            return (52, 40, 38, 255)
        tone = 96 + ((x * 7 + y * 13) % 5) * 6
        return (tone, tone // 3 + 10, tone // 4 + 8, 255)
    save("gt_refractory", brick)

    # Coiled strip seen side-on: alternating bright and dark wraps.
    def coil(x, y):
        tone = 120 if y % 2 else 160
        if (x + y) % 9 == 0:
            tone += 24
        return (tone + 10, tone - 4, tone - 20, 255)
    save("gt_steel_coil", coil)

    def lamp(colour):
        def pixel(x, y):
            edge = x in (0, 15) or y in (0, 15)
            core = 5 <= x <= 10 and 5 <= y <= 10
            if edge:
                return (40, 44, 48, 255)
            scale = 1.0 if core else 0.75
            return (*(int(c * scale) for c in colour), 255)
        return pixel
    save("gt_lamp_red", lamp((255, 70, 54)))
    save("gt_lamp_green", lamp((90, 255, 120)))


# ------------------------------------------------------------------ geometry

TEX = {name: index for index, name in enumerate(SHARED_TEXTURES + NEW_TEXTURES)}
FACE_NAMES = ("north", "south", "east", "west", "up", "down")
GRID = (-8.0, 0.0, 0.0)  # block boundaries, so pieces tile one texture per block face

elements = []
outliner = []
groups = []
_stack = []
_count = {}


def uid(name):
    _count[name] = _count.get(name, 0) + 1
    return str(uuid5(NAMESPACE_URL, f"iuce/rolling_mill/{name}/{_count[name]}"))


class group:
    def __init__(self, name, origin=(0, 0, 0)):
        self.node = {"uuid": uid("group:" + name), "isOpen": False, "children": []}
        groups.append({"name": name, "uuid": self.node["uuid"], "export": True, "origin": list(origin),
                       "rotation": [0, 0, 0], "color": 0, "visibility": True})

    def __enter__(self):
        (_stack[-1]["children"] if _stack else outliner).append(self.node)
        _stack.append(self.node)

    def __exit__(self, *args):
        _stack.pop()


def face_uv(face, lo, hi):
    """Pixel-true UVs inside one grid cell, so every piece shows its part of the tile."""
    def span(axis, flip):
        a, b = lo[axis] - GRID[axis], hi[axis] - GRID[axis]
        cell = math.floor(a / 16 + 1e-6) * 16
        start, end = a - cell, b - cell
        return (16 - end, 16 - start) if flip else (start, end)
    if face in ("north", "south"):
        u = span(0, face == "north")
        v = span(1, True)
    elif face in ("east", "west"):
        u = span(2, face == "east")
        v = span(1, True)
    else:
        u = span(0, False)
        v = span(2, face == "down")
    return [round(u[0], 4), round(v[0], 4), round(u[1], 4), round(v[1], 4)]


def box(name, lo, hi, tex, faces=None, rotation=None, origin=None, stretch_uv=False):
    """One cube; `tex` names the texture of every face unless `faces` overrides some."""
    faces = faces or {}
    entry = {"name": name, "box_uv": False, "export": True, "from": list(lo), "to": list(hi),
             "autouv": 0, "color": 0, "origin": list(origin or [(a + b) / 2 for a, b in zip(lo, hi)]),
             "faces": {}, "type": "cube", "uuid": uid(name)}
    for face in FACE_NAMES:
        texture = faces.get(face, tex)
        if texture is None:
            entry["faces"][face] = {"uv": [0, 0, 16, 16], "texture": None}
            continue
        uv = [0, 0, 16, 16] if stretch_uv or rotation else face_uv(face, lo, hi)
        entry["faces"][face] = {"uv": uv, "texture": TEX[texture]}
    if rotation:
        entry["rotation"] = list(rotation)
    elements.append(entry)
    _stack[-1]["children"].append(entry["uuid"])


def tiled(name, lo, hi, tex, faces=None):
    """A box split on block boundaries so large surfaces keep the 16-pixel texel density."""
    cuts = []
    for axis in range(3):
        points = [lo[axis]]
        edge = math.floor((lo[axis] - GRID[axis]) / 16) * 16 + GRID[axis] + 16
        while edge < hi[axis] - 1e-6:
            points.append(edge)
            edge += 16
        points.append(hi[axis])
        cuts.append(points)
    for i in range(len(cuts[0]) - 1):
        for j in range(len(cuts[1]) - 1):
            for k in range(len(cuts[2]) - 1):
                box(name, (cuts[0][i], cuts[1][j], cuts[2][k]), (cuts[0][i + 1], cuts[1][j + 1], cuts[2][k + 1]), tex, faces)


def roll_x(name, centre, radius, x0, x1, tex="gt_roll", end="gt_dark_frame"):
    """A barrel along x, built as an octagon from four planks (two of them turned 45 degrees)."""
    _, cy, cz = centre
    thin = radius * math.tan(math.radians(22.5))
    faces = {"east": end, "west": end}
    for index, (hy, hz, angle) in enumerate(((thin, radius, 0), (radius, thin, 0), (thin, radius, 45), (radius, thin, 45))):
        box(f"{name}_{index}", (x0, cy - hy, cz - hz), (x1, cy + hy, cz + hz), tex, faces,
            rotation=(angle, 0, 0) if angle else None, origin=(0, cy, cz), stretch_uv=True)


def roll_z(name, centre, radius, z0, z1, tex, end):
    cx, cy, _ = centre
    thin = radius * math.tan(math.radians(22.5))
    faces = {"north": end, "south": end}
    for index, (hx, hy, angle) in enumerate(((thin, radius, 0), (radius, thin, 0), (thin, radius, 45), (radius, thin, 45))):
        box(f"{name}_{index}", (cx - hx, cy - hy, z0), (cx + hx, cy + hy, z1), tex, faces,
            rotation=(0, 0, angle) if angle else None, origin=(cx, cy, 0), stretch_uv=True)


def mirror(fn, *args):
    """Calls fn for the west (sign -1) and east (sign +1) side."""
    for sign in (-1, 1):
        fn(sign, *args)


def xs(sign, a, b):
    """The span a..b (a < b, both >= 0) on one side of the centre line."""
    return (a, b) if sign > 0 else (-b, -a)


PASS = 30.0          # top of the table rolls; the strip runs just above
STRIP = (PASS, PASS + 1.5)


def strip(name, z0, z1, width=32):
    box(f"glow_{name}", (-width, STRIP[0], z0), (width, STRIP[1], z1), "gt_hot_strip", stretch_uv=True)


# ------------------------------------------------------------------ head: reheating furnace and entry

def head():
    with group("head"):
        tiled("head_bed", (-72, 0, 0), (72, 16, 32), "gt_steel_casing")
        box("head_kick", (-70, 2, -1), (70, 10, 0), "gt_hazard_stripe", stretch_uv=True)
        # Controller desk and hatches, standing where the hidden blocks are.
        box("controller", (-8, 16, 0), (8, 32, 14), "gt_steel_casing", {"north": "gt_controller_front"})
        box("input_hatch", (-24, 16, 1), (-10, 31, 14), "gt_steel_casing", {"north": "gt_hatch_input"})
        box("output_hatch", (10, 16, 1), (24, 31, 14), "gt_steel_casing", {"north": "gt_hatch_output"})
        tiled("furnace_plinth", (-40, 16, 12), (40, 32, 22), "gt_steel_casing")
        # Reheating furnace with an inspection door, burners and a tall stack.
        tiled("furnace", (-36, 32, 2), (36, 80, 22), "gt_refractory")
        tiled("furnace_roof", (-40, 80, 0), (40, 86, 24), "gt_dark_frame")
        box("furnace_door", (-16, 42, 1), (16, 72, 2), "gt_dark_frame", stretch_uv=True)
        box("glow_peep", (-6, 56, 0.5), (6, 62, 1), "gt_hot_strip", stretch_uv=True)
        box("glow_furnace_mouth", (-30, 28, 22), (30, 36, 23), "gt_hot_strip", stretch_uv=True)
        for index, y in enumerate((44, 58, 72)):
            mirror(lambda s, y=y: box(f"burner_{index}", (xs(s, 36, 42)[0], y, 6), (xs(s, 36, 42)[1], y + 6, 18), "gt_pipe_stainless"))
        mirror(lambda s: box("burner_manifold", (xs(s, 42, 46)[0], 40, 10), (xs(s, 42, 46)[1], 80, 14), "gt_copper_busbar"))
        tiled("stack_base", (-14, 86, 4), (14, 96, 20), "gt_steel_casing")
        tiled("stack", (-10, 96, 6), (10, 160, 18), "gt_pipe_stainless")
        box("stack_band", (-11, 140, 5), (11, 146, 19), "gt_hazard_stripe", stretch_uv=True)
        box("stack_cap", (-12, 160, 4), (12, 164, 20), "gt_dark_frame", stretch_uv=True)
        box("glow_beacon", (-3, 164, 10), (3, 168, 14), "gt_lamp_red", stretch_uv=True)
        # Electrical cabinets on both sides.
        mirror(lambda s: tiled("cabinet", (xs(s, 44, 70)[0], 16, 2), (xs(s, 44, 70)[1], 56, 30), "gt_steel_casing",
                               {"north": "gt_vent_grille", "east" if s > 0 else "west": "gt_vent_grille"}))
        mirror(lambda s: box("cabinet_busbar", (xs(s, 48, 66)[0], 56, 8), (xs(s, 48, 66)[1], 60, 24), "gt_copper_busbar"))
        mirror(lambda s: box("glow_cabinet_lamp", (xs(s, 52, 56)[0], 46, 1), (xs(s, 52, 56)[1], 50, 2), "gt_lamp_green", stretch_uv=True))
        # Entry pinch rolls with their small housings.
        mirror(lambda s: tiled("pinch_housing", (xs(s, 38, 48)[0], 16, 23), (xs(s, 38, 48)[1], 48, 32), "gt_dark_frame"))
        with group("entry_roll_top", (0, 36.5, 28)):
            roll_x("entry_roll_top", (0, 36.5, 28), 4, -38, 38)
        with group("entry_roll_bottom", (0, 25, 28)):
            roll_x("entry_roll_bottom", (0, 25, 28), 4, -38, 38)
        strip("head_strip", 23, 32)


# ------------------------------------------------------------------ bay: roller table segment, one block long

def bay():
    z0, z1 = 32, 48
    with group("bay"):
        tiled("bay_bed", (-56, 0, z0), (56, 16, z1), "gt_steel_casing")
        def beam(s):
            box("rail_top", (xs(s, 40, 56)[0], 28, z0), (xs(s, 40, 56)[1], 32, z1), "gt_dark_frame")
            box("rail_web", (xs(s, 46, 50)[0], 20, z0), (xs(s, 46, 50)[1], 28, z1), "gt_dark_frame")
            box("rail_foot", (xs(s, 40, 56)[0], 16, z0), (xs(s, 40, 56)[1], 20, z1), "gt_dark_frame")
            box("bearing", (xs(s, 38, 46)[0], 19, 36), (xs(s, 38, 46)[1], 29, 44), "gt_steel_casing")
            box("pipe_low", (xs(s, 58, 62)[0], 20, z0), (xs(s, 58, 62)[1], 24, z1), "gt_pipe_stainless")
            box("pipe_high", (xs(s, 58, 62)[0], 26, z0), (xs(s, 58, 62)[1], 30, z1), "gt_pipe_stainless")
            box("cable_tray", (xs(s, 58, 66)[0], 34, z0), (xs(s, 58, 66)[1], 36, z1), "gt_copper_busbar")
            # Catwalk on the upper rails.
            box("catwalk", (xs(s, 38, 58)[0], 64, z0), (xs(s, 38, 58)[1], 66, z1), "gt_vent_grille")
            box("catwalk_beam", (xs(s, 44, 52)[0], 60, z0), (xs(s, 44, 52)[1], 64, z1), "gt_dark_frame")
            box("rail_post", (xs(s, 57, 58)[0], 66, z0), (xs(s, 57, 58)[1], 76, z0 + 1), "gt_hazard_stripe", stretch_uv=True)
            box("handrail", (xs(s, 57, 58)[0], 76, z0), (xs(s, 57, 58)[1], 78, z1), "gt_hazard_stripe", stretch_uv=True)
        mirror(beam)
        with group("bay_roll", (0, 24, 40)):
            roll_x("bay_roll", (0, 24, 40), 6, -38, 38)
        strip("bay_strip", z0, z1)


# ------------------------------------------------------------------ stand: four-high roll stand, one block long

def stand():
    z0, z1 = 32, 48
    with group("stand"):
        tiled("stand_bed", (-72, 0, z0), (72, 16, z1), "gt_steel_casing")
        def housing(s):
            for post, (a, b) in (("front", (z0, z0 + 5)), ("back", (z1 - 5, z1))):
                tiled(f"housing_{post}", (xs(s, 54, 72)[0], 16, a), (xs(s, 54, 72)[1], 96, b), "gt_steel_casing")
            box("housing_window", (xs(s, 58, 68)[0], 20, z0 + 5), (xs(s, 58, 68)[1], 92, z1 - 5), "gt_dark_frame")
            box("chock_low", (xs(s, 44, 54)[0], 4, 34), (xs(s, 44, 54)[1], 30, 46), "gt_dark_frame")
            box("chock_high", (xs(s, 44, 54)[0], 31.5, 34), (xs(s, 44, 54)[1], 58, 46), "gt_dark_frame")
            box("screwdown", (xs(s, 46, 52)[0], 58, 37), (xs(s, 46, 52)[1], 96, 43), "gt_pipe_stainless", stretch_uv=True)
            box("catwalk", (xs(s, 38, 54)[0], 64, z0), (xs(s, 38, 54)[1], 66, z1), "gt_vent_grille")
            box("screw_motor", (xs(s, 24, 36)[0], 112, 36), (xs(s, 24, 36)[1], 122, 44), "gt_steel_casing")
            box("glow_stand_lamp", (xs(s, 62, 66)[0], 86, z0 - 0.5), (xs(s, 62, 66)[1], 90, z0), "gt_lamp_green", stretch_uv=True)
        mirror(housing)
        tiled("crown", (-72, 96, z0), (72, 112, z1), "gt_steel_casing")
        for face_z, step in ((z0 - 1, z0), (z1, z1 + 1)):
            box("crown_stripe", (-60, 100, face_z), (60, 108, step), "gt_hazard_stripe", stretch_uv=True)
        box("gearbox", (-20, 112, 35), (20, 124, 45), "gt_dark_frame", stretch_uv=True)
        box("gearbox_coil", (-14, 113, 34), (14, 123, 46), "gt_copper_coil", {"east": None, "west": None}, stretch_uv=True)
        for name, centre, radius in (("stand_backup_bottom", (0, 12, 40), 8), ("stand_roll_bottom", (0, 25, 40), 5),
                                     ("stand_roll_top", (0, 36.5, 40), 5), ("stand_backup_top", (0, 49.5, 40), 8)):
            with group(name, centre):
                roll_x(name, centre, radius, -44, 44)
        strip("stand_strip", z0, z1)


# ------------------------------------------------------------------ tail: down-coiler and main drive

def tail():
    z0, z1 = 112, 144
    with group("tail"):
        tiled("tail_bed", (-72, 0, z0), (72, 16, z1), "gt_steel_casing")
        strip("tail_strip", z0, 118)
        mirror(lambda s: tiled("coiler_cheek", (xs(s, 32, 44)[0], 16, 114), (xs(s, 32, 44)[1], 48, 142), "gt_dark_frame"))
        with group("coiler", (0, 30, 128)):
            roll_x("coiler", (0, 30, 128), 12, -30, 30, tex="gt_steel_coil", end="gt_steel_coil")
        # Main drive motor with cooling ribs, flywheel and energy hatch.
        tiled("motor", (-26, 48, z0), (26, 84, z1), "gt_steel_casing", {"up": "gt_vent_grille"})
        for z in (116, 124, 132, 140):
            box(f"motor_band_{z}", (-28, 46, z), (28, 86, z + 3), "gt_copper_coil", stretch_uv=True)
        box("energy_hatch", (-8, 16, 130), (8, 32, 144), "gt_steel_casing", {"south": "gt_hatch_energy"})
        box("drive_pedestal", (-10, 32, 140), (10, 46, 144), "gt_dark_frame", stretch_uv=True)
        with group("flywheel", (0, 66, 147)):
            roll_z("flywheel", (0, 66, 147), 18, 144.5, 149.5, "gt_dark_frame", "gt_steel_casing")
            box("flywheel_hub", (-4, 62, 149.5), (4, 70, 151), "gt_copper_busbar", stretch_uv=True)
        box("glow_drive_lamp", (-3, 78, 144), (3, 82, 144.5), "gt_lamp_green", stretch_uv=True)
        # Transformer cabinets with insulators.
        mirror(lambda s: tiled("transformer", (xs(s, 46, 70)[0], 16, 114), (xs(s, 46, 70)[1], 60, 142), "gt_steel_casing",
                               {"east" if s > 0 else "west": "gt_vent_grille"}))
        for z in (120, 128, 136):
            mirror(lambda s, z=z: box("insulator", (xs(s, 55, 61)[0], 60, z), (xs(s, 55, 61)[1], 70, z + 4), "gt_copper_busbar"))
        mirror(lambda s: box("glow_transformer_lamp", (xs(s, 56, 60)[0], 50, 142), (xs(s, 56, 60)[1], 54, 142.5), "gt_lamp_red", stretch_uv=True))


# ------------------------------------------------------------------ project file

def texture_entry(name, index):
    data = (TEX_DIR / f"{name}.png").read_bytes()
    return {"name": name, "path": "", "folder": "iuce/block/plant", "namespace": "iuce", "id": str(index),
            "width": 16, "height": 16, "uv_width": 16, "uv_height": 16, "particle": False,
            "render_mode": "default", "render_sides": "auto", "visible": True, "internal": True, "saved": False,
            "uuid": str(uuid5(NAMESPACE_URL, f"iuce/rolling_mill/texture/{name}")),
            "source": "data:image/png;base64," + base64.b64encode(data).decode()}


def main():
    draw_new_textures()
    head()
    stand()
    bay()
    tail()
    model = {
        "meta": {"format_version": "5.0", "model_format": "free", "box_uv": False},
        "name": "rolling_mill",
        "model_identifier": "rolling_mill",
        "resolution": {"width": 16, "height": 16},
        "elements": elements,
        "groups": groups,
        "outliner": outliner,
        "textures": [texture_entry(name, index) for name, index in TEX.items()],
    }
    OUT.write_text(json.dumps(model, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"Wrote {len(elements)} cubes to {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
