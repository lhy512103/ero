#!/usr/bin/env python3
"""Writes block models and blockstates for the structural building blocks.

    python tools/gen_structure_blocks.py

Textures come from tools/art/blockbench/structure.js. Every block shows the empty model once its
machine forms (formed=true), so the assembled 3D model is drawn in its place.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/iuce"
TEX = "iuce:block/structure/"
EMPTY = {"model": "iuce:block/plant_empty"}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8", newline="\n")


def model(name, data):
    write(ASSETS / f"models/block/{name}.json", data)


def state(name, variants=None, multipart=None):
    write(ASSETS / f"blockstates/{name}.json", {"variants": variants} if variants else {"multipart": multipart})


def faces(texture, uv=None, skip=()):
    return {f: {"uv": uv or [0, 0, 16, 16], "texture": texture} for f in ("north", "south", "east", "west", "up", "down") if f not in skip}


def cube(name, texture):
    model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": TEX + texture}})
    state(name, {"formed=false": {"model": f"iuce:block/{name}"}, "formed=true": EMPTY})


def pillar_states(name):
    variants = {}
    for axis, rot in (("y", {}), ("z", {"x": 90}), ("x", {"x": 90, "y": 90})):
        variants[f"axis={axis},formed=false"] = {"model": f"iuce:block/{name}", **rot}
        variants[f"axis={axis},formed=true"] = EMPTY
    state(name, variants)


def h_beam():
    # Two flanges facing north and south with the web between them; runs along y.
    flange = lambda z0, z1: {"from": [0, 0, z0], "to": [16, 16, z1], "faces": {
        "north": {"uv": [0, 0, 16, 16], "texture": "#flange"},
        "south": {"uv": [0, 0, 16, 16], "texture": "#flange"},
        "east": {"uv": [z0, 0, z1, 16], "texture": "#end"},
        "west": {"uv": [z0, 0, z1, 16], "texture": "#end"},
        "up": {"uv": [0, z0, 16, z1], "texture": "#end", "cullface": "up"},
        "down": {"uv": [0, z0, 16, z1], "texture": "#end", "cullface": "down"}}}
    web = {"from": [6.5, 0, 3], "to": [9.5, 16, 13], "faces": {
        "east": {"uv": [3, 0, 13, 16], "texture": "#web"},
        "west": {"uv": [3, 0, 13, 16], "texture": "#web"},
        "up": {"uv": [6.5, 3, 9.5, 13], "texture": "#end", "cullface": "up"},
        "down": {"uv": [6.5, 3, 9.5, 13], "texture": "#end", "cullface": "down"}}}
    model("plant_frame", {"parent": "minecraft:block/block", "textures": {
        "particle": TEX + "h_beam_flange", "flange": TEX + "h_beam_flange", "web": TEX + "h_beam_web", "end": TEX + "h_beam_end"},
        "elements": [flange(0, 3), flange(13, 16), web]})
    pillar_states("plant_frame")


def process_pipe(name="process_pipe"):
    model(name, {"parent": "minecraft:block/block", "textures": {
        "particle": TEX + name, "side": TEX + name, "end": TEX + name + "_end"},
        "elements": [
            {"from": [3, 0, 3], "to": [13, 16, 13], "faces": {
                **{f: {"uv": [3, 0, 13, 16], "texture": "#side"} for f in ("north", "south", "east", "west")},
                "up": {"uv": [3, 3, 13, 13], "texture": "#end", "cullface": "up"},
                "down": {"uv": [3, 3, 13, 13], "texture": "#end", "cullface": "down"}}},
            {"from": [2, 0, 2], "to": [14, 2.5, 14], "faces": {
                **{f: {"uv": [2, 0, 14, 2.5], "texture": "#side"} for f in ("north", "south", "east", "west")},
                "up": {"uv": [2, 2, 14, 14], "texture": "#end"},
                "down": {"uv": [2, 2, 14, 14], "texture": "#end", "cullface": "down"}}},
        ]})
    pillar_states(name)


def grating():
    model("steel_grating", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
        "particle": TEX + "steel_grating", "top": TEX + "steel_grating", "side": TEX + "steel_grating_side"},
        "elements": [{"from": [0, 0, 0], "to": [16, 2, 16], "faces": {
            **{f: {"uv": [0, 14, 16, 16], "texture": "#side", "cullface": f} for f in ("north", "south", "east", "west")},
            "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
            "down": {"uv": [0, 0, 16, 16], "texture": "#top", "cullface": "down"}}}]})
    state("steel_grating", {"formed=false": {"model": "iuce:block/steel_grating"}, "formed=true": EMPTY})


def stairs():
    textures = {"bottom": TEX + "checker_plate", "top": TEX + "checker_plate", "side": TEX + "checker_plate"}
    for kind, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        model("steel_stairs" + kind, {"parent": "minecraft:block/" + parent, "textures": textures})
    base = {"east": 0, "south": 90, "west": 180, "north": 270}
    variants = {}
    for facing, y in base.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                kind = "" if shape == "straight" else "_" + shape.split("_")[0]
                rot = y
                if half == "bottom" and shape.endswith("left"): rot = y - 90
                if half == "top" and shape.endswith("right"): rot = y + 90
                rot %= 360
                entry = {"model": "iuce:block/steel_stairs" + kind}
                if half == "top": entry["x"] = 180
                if rot: entry["y"] = rot
                if half == "top" or rot: entry["uvlock"] = True
                for formed in ("false", "true"):
                    variants[f"facing={facing},formed={formed},half={half},shape={shape}"] = entry if formed == "false" else EMPTY
    state("steel_stairs", variants)


def handrail():
    t = {"particle": TEX + "steel_handrail", "rail": TEX + "steel_handrail"}
    box = lambda a, b: {"from": a, "to": b, "faces": faces("#rail")}
    model("steel_handrail_post", {"textures": t, "elements": [box([7, 0, 7], [9, 16, 9]), box([6.5, 14, 6.5], [9.5, 16, 9.5])]})
    # Top and knee rails running north from the post.
    model("steel_handrail_side", {"textures": t, "elements": [box([7.25, 14, 0], [8.75, 15.5, 7]), box([7.25, 7, 0], [8.75, 8.5, 7])]})
    model("steel_handrail_inventory", {"parent": "minecraft:block/block", "textures": t, "elements": [
        box([2, 0, 7], [4, 16, 9]), box([12, 0, 7], [14, 16, 9]), box([0, 14, 7.25], [16, 15.5, 8.75]), box([0, 7, 7.25], [16, 8.5, 8.75])]})
    parts = [{"when": {"formed": "false"}, "apply": {"model": "iuce:block/steel_handrail_post"}}]
    for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        apply = {"model": "iuce:block/steel_handrail_side", "uvlock": True}
        if y: apply["y"] = y
        parts.append({"when": {"formed": "false", side: "true"}, "apply": apply})
    state("steel_handrail", multipart=parts)


def gas_hatch():
    model("gas_output_hatch", {"parent": "minecraft:block/orientable", "textures": {
        "front": "iuce:block/gas_output_hatch", "side": "iuce:block/machine/process/box_side", "top": "iuce:block/machine/process/box_top"}})
    state("gas_output_hatch", {"formed=false": {"model": "iuce:block/gas_output_hatch"}, "formed=true": EMPTY})


def main():
    for name in ("checker_plate", "refractory_bricks", "furnace_shell", "tank_shell"):
        cube(name, name)
    h_beam()
    process_pipe()
    process_pipe("graphite_electrode")
    grating()
    stairs()
    handrail()
    gas_hatch()


if __name__ == "__main__":
    main()
