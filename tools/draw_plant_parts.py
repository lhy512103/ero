#!/usr/bin/env python3
"""Generates GT-style industrial textures, 3D block models, and blockstates for all
multiblock plant parts (unformed & mill blocks) matching the palette of
ore_processing_plant.bbmodel.
"""
import json
import shutil
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/iuce"
TARGET = ASSETS / "textures/block/machine/plant"
BB_TEX = ROOT / "tools/models/ore_processing_plant_textures"

# Deep GTCEuM-inspired industrial palette matching ore_processing_plant.bbmodel
STEEL_DARK = (38, 43, 49, 255)
STEEL_MID = (57, 64, 72, 255)
STEEL_LIGHT = (82, 92, 104, 255)
STEEL_HIGHLIGHT = (114, 126, 140, 255)
FRAME_DEEP = (24, 28, 32, 255)
FRAME_EDGE = (46, 52, 60, 255)
CHASSIS_BG = (19, 23, 27, 255)
CYAN = (68, 206, 238, 255)
AMBER = (232, 152, 54, 255)
GREEN = (98, 228, 138, 255)
RED_DIM = (165, 62, 56, 255)


def img16(bg):
    return Image.new("RGBA", (16, 16), bg)


def copy_bb_tex(src_name, dst_name):
    shutil.copy2(BB_TEX / src_name, TARGET / dst_name)


def build_casing_trim():
    im = img16(FRAME_DEEP)
    d = ImageDraw.Draw(im)
    d.line([(0, 1), (15, 1)], fill=STEEL_LIGHT)
    d.line([(0, 2), (15, 2)], fill=STEEL_MID)
    for x in (2, 7, 13):
        im.putpixel((x, 1), STEEL_HIGHLIGHT)
    im.save(TARGET / "trim.png")


def build_bearing():
    im = img16(STEEL_DARK)
    d = ImageDraw.Draw(im)
    d.rectangle([1, 1, 14, 14], outline=STEEL_LIGHT, fill=STEEL_MID)
    d.rectangle([3, 3, 12, 12], outline=FRAME_DEEP, fill=STEEL_DARK)
    d.rectangle([5, 5, 10, 10], outline=AMBER, fill=CHASSIS_BG)
    d.rectangle([7, 7, 8, 8], fill=STEEL_HIGHLIGHT)
    for bx, by in ((2, 2), (13, 2), (2, 13), (13, 13)):
        im.putpixel((bx, by), STEEL_HIGHLIGHT)
    im.save(TARGET / "bearing.png")


def build_girder():
    im = img16(STEEL_DARK)
    d = ImageDraw.Draw(im)
    # Flange highlights & recessed center web
    d.rectangle([0, 0, 2, 15], fill=FRAME_EDGE)
    d.rectangle([3, 0, 3, 15], fill=STEEL_HIGHLIGHT)
    d.rectangle([4, 0, 5, 15], fill=STEEL_MID)
    d.rectangle([6, 0, 9, 15], fill=FRAME_DEEP)
    d.rectangle([10, 0, 11, 15], fill=STEEL_MID)
    d.rectangle([12, 0, 12, 15], fill=STEEL_LIGHT)
    d.rectangle([13, 0, 15, 15], fill=FRAME_EDGE)
    for y in (2, 7, 13):
        im.putpixel((1, y), STEEL_HIGHLIGHT)
        im.putpixel((14, y), STEEL_HIGHLIGHT)
        d.line([(6, y), (9, y)], fill=STEEL_DARK)
    im.save(TARGET / "girder.png")


def build_drum():
    # Take gt_crusher_drum and enhance with copper/amber collar bands at top/bottom
    im = Image.open(BB_TEX / "gt_crusher_drum.png").convert("RGBA")
    d = ImageDraw.Draw(im)
    d.line([(0, 1), (15, 1)], fill=FRAME_DEEP)
    d.line([(0, 2), (15, 2)], fill=AMBER)
    d.line([(0, 13), (15, 13)], fill=AMBER)
    d.line([(0, 14), (15, 14)], fill=FRAME_DEEP)
    im.save(TARGET / "drum.png")


def build_controller(mill, formed):
    im = Image.open(BB_TEX / "gt_steel_casing.png").convert("RGBA")
    d = ImageDraw.Draw(im)
    # Recessed dark display chassis
    d.rectangle([2, 2, 13, 12], outline=FRAME_DEEP, fill=CHASSIS_BG)
    d.rectangle([3, 3, 12, 9], fill=(20, 42, 46, 255) if formed else (24, 30, 36, 255))
    accent = GREEN if formed else AMBER
    sub = CYAN if formed else (110, 135, 150, 255)
    if mill:
        # Rolling bars icon
        d.line([(4, 4), (11, 4)], fill=accent)
        d.line([(4, 8), (11, 8)], fill=accent)
        d.rectangle([5, 5, 6, 7], fill=sub)
        d.rectangle([9, 5, 10, 7], fill=sub)
    else:
        # Three-stage crusher/washer/kiln bar graph
        d.rectangle([4, 6, 5, 8], fill=CYAN)
        d.rectangle([7, 4, 8, 8], fill=accent)
        d.rectangle([10, 5, 11, 8], fill=AMBER if not formed else GREEN)
    # Lower keypad & status LEDs
    d.rectangle([3, 10, 7, 11], fill=STEEL_DARK)
    im.putpixel((9, 11), GREEN if formed else RED_DIM)
    im.putpixel((11, 11), CYAN)
    im.putpixel((3, 10), STEEL_LIGHT)
    im.putpixel((5, 10), STEEL_LIGHT)
    suffix = "_formed" if formed else ""
    prefix = "mill_" if mill else ""
    im.save(TARGET / f"{prefix}controller{suffix}.png")


def build_energy_hv():
    im = Image.open(BB_TEX / "gt_hatch_energy.png").convert("RGBA")
    d = ImageDraw.Draw(im)
    # Brighter gold/amber triple busbar glow for HV tier
    for x in (4, 7, 10):
        d.rectangle([x, 4, x + 1, 9], fill=(245, 185, 66, 255))
        im.putpixel((x, 5), (255, 232, 145, 255))
    d.line([(4, 12), (11, 12)], fill=AMBER)
    im.save(TARGET / "energy_hatch_hv.png")


def write_json(path, data, compact=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(data, separators=(",", ":") if compact else None, indent=None if compact else 2) + "\n"
    path.write_text(text, encoding="utf-8")


def build_casing_models():
    directions = {
        "west": (0, False), "east": (0, True),
        "down": (1, False), "up": (1, True),
        "north": (2, False), "south": (2, True),
    }
    multipart = [{"when": {"formed": "false"}, "apply": {"model": "iuce:block/plant_casing_panel"}}]
    for edge_name, (cross_axis, positive) in directions.items():
        elements = []
        for face, (face_axis, outward) in directions.items():
            if face_axis == cross_axis:
                continue
            start, stop = [0, 0, 0], [16, 16, 16]
            start[cross_axis], stop[cross_axis] = (13, 16) if positive else (0, 3)
            offset = 0.01 + cross_axis * 0.003
            start[face_axis] = stop[face_axis] = 16 + offset if outward else -offset
            tangent_axis = 2 if face_axis == 0 else 0
            if cross_axis == tangent_axis:
                increasing = face in ("south", "west", "up", "down")
                rotation = 90 if positive == increasing else 270
            else:
                increasing = face in ("up", "down")
                rotation = 0 if positive != increasing else 180
            elements.append({
                "from": start, "to": stop,
                "faces": {face: {"uv": [0, 0, 16, 3], "rotation": rotation, "texture": "#trim", "cullface": face}}
            })
        name = f"plant_casing_edge_{edge_name}"
        model = {
            "parent": "minecraft:block/block",
            "textures": {"trim": "iuce:block/machine/plant/trim", "particle": "iuce:block/machine/plant/casing"},
            "elements": elements,
        }
        write_json(ASSETS / "models/block" / f"{name}.json", model, compact=True)
        multipart.append({"when": {"formed": "false", edge_name: "false"}, "apply": {"model": f"iuce:block/{name}"}})

    panel_model = {"parent": "minecraft:block/cube_all", "textures": {"all": "iuce:block/machine/plant/casing"}}
    write_json(ASSETS / "models/block/plant_casing_panel.json", panel_model, compact=True)
    # Item/inventory model: crisp bolted steel casing cube
    write_json(ASSETS / "models/block/plant_casing.json", panel_model)
    write_json(ASSETS / "blockstates/plant_casing.json", {"multipart": multipart})


def build_empty_model():
    write_json(ASSETS / "models/block/plant_empty.json", {"elements": []})


def build_frame_model():
    # Detailed 3D I-beam steel girder (vertical Y-axis base, rotated for X/Z in blockstate)
    model = {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": "iuce:block/machine/plant/girder",
            "side": "iuce:block/machine/plant/girder",
            "end": "iuce:block/machine/plant/bearing",
            "trim": "iuce:block/machine/plant/trim",
        },
        "elements": [
            # Center steel web
            {"from": [6, 0, 4], "to": [10, 16, 12], "faces": {
                "west": {"uv": [4, 0, 12, 16], "texture": "#side"},
                "east": {"uv": [4, 0, 12, 16], "texture": "#side"},
                "up": {"uv": [6, 4, 10, 12], "texture": "#end", "cullface": "up"},
                "down": {"uv": [6, 4, 10, 12], "texture": "#end", "cullface": "down"},
            }},
            # North flange
            {"from": [2, 0, 2], "to": [14, 16, 4], "faces": {
                "north": {"uv": [2, 0, 14, 16], "texture": "#side"},
                "south": {"uv": [2, 0, 14, 16], "texture": "#side"},
                "west": {"uv": [2, 0, 4, 16], "texture": "#side"},
                "east": {"uv": [2, 0, 4, 16], "texture": "#side"},
                "up": {"uv": [2, 2, 14, 4], "texture": "#end", "cullface": "up"},
                "down": {"uv": [2, 2, 14, 4], "texture": "#end", "cullface": "down"},
            }},
            # South flange
            {"from": [2, 0, 12], "to": [14, 16, 14], "faces": {
                "north": {"uv": [2, 0, 14, 16], "texture": "#side"},
                "south": {"uv": [2, 0, 14, 16], "texture": "#side"},
                "west": {"uv": [12, 0, 14, 16], "texture": "#side"},
                "east": {"uv": [12, 0, 14, 16], "texture": "#side"},
                "up": {"uv": [2, 12, 14, 14], "texture": "#end", "cullface": "up"},
                "down": {"uv": [2, 12, 14, 14], "texture": "#end", "cullface": "down"},
            }},
            # Lower & upper bolted collar bands
            {"from": [1.5, 0, 1.5], "to": [14.5, 2.5, 14.5], "faces": {
                "north": {"uv": [0, 0, 16, 3], "texture": "#trim"},
                "south": {"uv": [0, 0, 16, 3], "texture": "#trim"},
                "west": {"uv": [0, 0, 16, 3], "texture": "#trim"},
                "east": {"uv": [0, 0, 16, 3], "texture": "#trim"},
                "up": {"uv": [1.5, 1.5, 14.5, 14.5], "texture": "#end"},
                "down": {"uv": [1.5, 1.5, 14.5, 14.5], "texture": "#end", "cullface": "down"},
            }},
            {"from": [1.5, 13.5, 1.5], "to": [14.5, 16, 14.5], "faces": {
                "north": {"uv": [0, 0, 16, 3], "texture": "#trim"},
                "south": {"uv": [0, 0, 16, 3], "texture": "#trim"},
                "west": {"uv": [0, 0, 16, 3], "texture": "#trim"},
                "east": {"uv": [0, 0, 16, 3], "texture": "#trim"},
                "up": {"uv": [1.5, 1.5, 14.5, 14.5], "texture": "#end", "cullface": "up"},
                "down": {"uv": [1.5, 1.5, 14.5, 14.5], "texture": "#end"},
            }},
        ],
    }
    write_json(ASSETS / "models/block/plant_frame.json", model)
    write_json(ASSETS / "blockstates/plant_frame.json", {
        "variants": {
            "axis=y,formed=false": {"model": "iuce:block/plant_frame"},
            "axis=z,formed=false": {"model": "iuce:block/plant_frame", "x": 90},
            "axis=x,formed=false": {"model": "iuce:block/plant_frame", "x": 90, "y": 90},
            "axis=y,formed=true": {"model": "iuce:block/plant_empty"},
            "axis=z,formed=true": {"model": "iuce:block/plant_empty"},
            "axis=x,formed=true": {"model": "iuce:block/plant_empty"},
        }
    })


def build_drum_model():
    # Octagonal stepped industrial drum with central bearing shaft and copper collar rings
    model = {
        "parent": "minecraft:block/block",
        "textures": {
            "particle": "iuce:block/machine/plant/drum",
            "side": "iuce:block/machine/plant/drum",
            "end": "iuce:block/machine/plant/bearing",
        },
        "elements": [
            {"from": [1, 0, 3], "to": [15, 16, 13], "faces": {
                "north": {"uv": [1, 0, 15, 16], "texture": "#side"},
                "south": {"uv": [1, 0, 15, 16], "texture": "#side"},
                "west": {"uv": [3, 0, 13, 16], "texture": "#side"},
                "east": {"uv": [3, 0, 13, 16], "texture": "#side"},
                "up": {"uv": [1, 3, 15, 13], "texture": "#end", "cullface": "up"},
                "down": {"uv": [1, 3, 15, 13], "texture": "#end", "cullface": "down"},
            }},
            {"from": [3, 0, 1], "to": [13, 16, 15], "faces": {
                "north": {"uv": [3, 0, 13, 16], "texture": "#side"},
                "south": {"uv": [3, 0, 13, 16], "texture": "#side"},
                "west": {"uv": [1, 0, 15, 16], "texture": "#side"},
                "east": {"uv": [1, 0, 15, 16], "texture": "#side"},
                "up": {"uv": [3, 1, 13, 15], "texture": "#end", "cullface": "up"},
                "down": {"uv": [3, 1, 13, 15], "texture": "#end", "cullface": "down"},
            }},
        ],
    }
    write_json(ASSETS / "models/block/plant_drum.json", model)
    write_json(ASSETS / "blockstates/plant_drum.json", {
        "variants": {
            "axis=y,formed=false": {"model": "iuce:block/plant_drum"},
            "axis=z,formed=false": {"model": "iuce:block/plant_drum", "x": 90},
            "axis=x,formed=false": {"model": "iuce:block/plant_drum", "x": 90, "y": 90},
            "axis=y,formed=true": {"model": "iuce:block/plant_empty"},
            "axis=z,formed=true": {"model": "iuce:block/plant_empty"},
            "axis=x,formed=true": {"model": "iuce:block/plant_empty"},
        }
    })


def build_controller_models():
    for name, tex in [
        ("plant_controller", "controller"),
        ("plant_controller_formed", "controller_formed"),
        ("mill_controller", "mill_controller"),
        ("mill_controller_formed", "mill_controller_formed"),
    ]:
        # Recessed screen front with vent sides and dark steel top/bottom
        model = {
            "parent": "minecraft:block/block",
            "textures": {
                "particle": "iuce:block/machine/plant/casing",
                "casing": "iuce:block/machine/plant/casing",
                "vent": "iuce:block/machine/plant/vent",
                "front": f"iuce:block/machine/plant/{tex}",
            },
            "elements": [
                # Main casing body (recessed 0.5px on north front for screen depth)
                {"from": [0, 0, 0.5], "to": [16, 16, 16], "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#front"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#casing", "cullface": "south"},
                    "west": {"uv": [0, 0, 16, 16], "texture": "#vent", "cullface": "west"},
                    "east": {"uv": [0, 0, 16, 16], "texture": "#vent", "cullface": "east"},
                    "up": {"uv": [0, 0, 16, 16], "texture": "#casing", "cullface": "up"},
                    "down": {"uv": [0, 0, 16, 16], "texture": "#casing", "cullface": "down"},
                }},
                # Front outer bezel frame (top, bottom, left, right strips)
                {"from": [0, 14, 0], "to": [16, 16, 0.5], "faces": {
                    "north": {"uv": [0, 0, 16, 2], "texture": "#front", "cullface": "north"},
                    "up": {"uv": [0, 0, 16, 1], "texture": "#casing", "cullface": "up"},
                    "down": {"uv": [0, 0, 16, 1], "texture": "#casing"},
                }},
                {"from": [0, 0, 0], "to": [16, 2, 0.5], "faces": {
                    "north": {"uv": [0, 14, 16, 16], "texture": "#front", "cullface": "north"},
                    "up": {"uv": [0, 0, 16, 1], "texture": "#casing"},
                    "down": {"uv": [0, 0, 16, 1], "texture": "#casing", "cullface": "down"},
                }},
                {"from": [0, 2, 0], "to": [2, 14, 0.5], "faces": {
                    "north": {"uv": [0, 2, 2, 14], "texture": "#front", "cullface": "north"},
                    "east": {"uv": [0, 2, 1, 14], "texture": "#casing"},
                    "west": {"uv": [0, 2, 1, 14], "texture": "#casing", "cullface": "west"},
                }},
                {"from": [14, 2, 0], "to": [16, 14, 0.5], "faces": {
                    "north": {"uv": [14, 2, 16, 14], "texture": "#front", "cullface": "north"},
                    "west": {"uv": [15, 2, 16, 14], "texture": "#casing"},
                    "east": {"uv": [15, 2, 16, 14], "texture": "#casing", "cullface": "east"},
                }},
            ],
        }
        write_json(ASSETS / "models/block" / f"{name}.json", model)


def build_hatch_models():
    for name, tex in [
        ("item_input_hatch", "input_hatch"),
        ("item_output_hatch", "output_hatch"),
    ]:
        # Steel casing top/bottom + 4 industrial hatch port sides
        model = {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": f"iuce:block/machine/plant/{tex}",
                "down": "iuce:block/machine/plant/casing",
                "up": "iuce:block/machine/plant/casing",
                "north": f"iuce:block/machine/plant/{tex}",
                "south": f"iuce:block/machine/plant/{tex}",
                "west": f"iuce:block/machine/plant/{tex}",
                "east": f"iuce:block/machine/plant/{tex}",
            },
        }
        write_json(ASSETS / "models/block" / f"{name}.json", model)
        write_json(ASSETS / "blockstates" / f"{name}.json", {
            "variants": {
                "formed=false": {"model": f"iuce:block/{name}"},
                "formed=true": {"model": "iuce:block/plant_empty"},
            }
        })

    for name, tex in [
        ("energy_hatch", "energy_hatch"),
        ("energy_hatch_hv", "energy_hatch_hv"),
    ]:
        model = {
            "parent": "minecraft:block/orientable",
            "textures": {
                "particle": f"iuce:block/machine/plant/{tex}",
                "top": "iuce:block/machine/plant/casing",
                "front": f"iuce:block/machine/plant/{tex}",
                "side": "iuce:block/machine/plant/vent",
            },
        }
        write_json(ASSETS / "models/block" / f"{name}.json", model)
        variants = {}
        for facing, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
            for running in ("false", "true"):
                variants[f"facing={facing},running={running},formed=false"] = {"model": f"iuce:block/{name}", "y": y}
                variants[f"facing={facing},running={running},formed=true"] = {"model": "iuce:block/plant_empty"}
        write_json(ASSETS / "blockstates" / f"{name}.json", {"variants": variants})


def main():
    TARGET.mkdir(parents=True, exist_ok=True)
    copy_bb_tex("gt_steel_casing.png", "casing.png")
    copy_bb_tex("gt_hatch_input.png", "input_hatch.png")
    copy_bb_tex("gt_hatch_output.png", "output_hatch.png")
    copy_bb_tex("gt_hatch_energy.png", "energy_hatch.png")
    copy_bb_tex("gt_vent_grille.png", "vent.png")
    build_casing_trim()
    build_bearing()
    build_girder()
    build_drum()
    build_energy_hv()
    build_controller(False, False)
    build_controller(False, True)
    build_controller(True, False)
    build_controller(True, True)

    build_empty_model()
    build_casing_models()
    build_frame_model()
    build_drum_model()
    build_controller_models()
    build_hatch_models()
    print("Regenerated GT-style multiblock textures, models, and blockstates.")


if __name__ == "__main__":
    main()
