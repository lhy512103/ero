#!/usr/bin/env python3
"""Exports a formed large-machine model from Blockbench into the mod.

    python tools/export_plant_model.py ore_processing_plant
    python tools/export_plant_model.py rolling_mill

Reads tools/models/<name>.bbmodel with its textures in tools/models/<name>_textures, and writes
src/main/resources/assets/iuce/textures/entity/<name>.png (atlas) and
src/main/resources/assets/iuce/models/multiblock/<name>.json (boxes in block units, controller
block at the origin, machine extending towards +z).

Per-model settings live in SETTINGS below:
- offset: Blockbench pixels added before scaling, so the controller's block corner lands on 0,0,0.
- spinners: outliner groups that turn while the machine works; their group origin is the pivot.
- emissive: element name prefixes drawn full-bright while working.
- translucent: texture names drawn translucent.
- stretch: for machines that grow; elements under an outliner group named "tail", "bay" or
  "stand" are exported as that section (everything else is "head") and laid out at run time
  the same way the block layout grows. Model each section at the depth of its first copy.
"""
import json
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SCALE = 1.0 / 16.0
CELL_SIZE = 32
TILE_SIZE = 16
PAD = 8

SETTINGS = {
    "twin_screw_extruder": {"offset": (0,0,0), "stretch": {"base_length":13, "bay_first":3, "bay_last":3, "stand_first":3, "stand_every":4, "stand_last":3}},
    "ore_processing_plant": {
        "offset": (8.0, 0.0, 56.0),
        "spinners": {
            "roller_front": {"axis": "x", "speed": 1.0},
            "roller_back": {"axis": "x", "speed": -1.0},
            "washing_drum": {"axis": "z", "speed": 0.65},
        },
        "emissive": ("kiln_core_glow", "beacon_light"),
        "translucent": ("gt_glass_frame",),
    },
    "rolling_mill": {
        "offset": (8.0, -16.0, 0.0),
        "spinners": {
            "entry_roll_top": {"axis": "x", "speed": 2.0},
            "entry_roll_bottom": {"axis": "x", "speed": -2.0},
            "bay_roll": {"axis": "x", "speed": -1.6},
            "stand_roll_top": {"axis": "x", "speed": 2.4},
            "stand_roll_bottom": {"axis": "x", "speed": -2.4},
            "stand_backup_top": {"axis": "x", "speed": -1.2},
            "stand_backup_bottom": {"axis": "x", "speed": 1.2},
            "flywheel": {"axis": "x", "speed": 1.0},
            "coiler": {"axis": "x", "speed": -0.8},
        },
        "emissive": ("glow",),
        "translucent": ("gt_glass_frame",),
        "stretch": {"base_length": 9, "bay_first": 2, "bay_last": 2,
                    "stand_first": 2, "stand_every": 4, "stand_last": 2},
    },
}


def pack_atlas(textures, tex_src, atlas_out):
    columns = 4
    rows = (len(textures) + columns - 1) // columns
    size = 1
    while size < max(columns, rows) * CELL_SIZE:
        size *= 2
    atlas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    uv_boxes = {}
    for idx, tex in enumerate(textures):
        name = tex["name"]
        if not name.endswith(".png"):
            name += ".png"
        src = Image.open(tex_src / name).convert("RGBA")
        if src.size != (TILE_SIZE, TILE_SIZE):
            src = src.resize((TILE_SIZE, TILE_SIZE), Image.Resampling.NEAREST)
        col, row = idx % columns, idx // columns
        ox, oy = col * CELL_SIZE, row * CELL_SIZE
        # Fill the 32x32 cell with clamped edge pixels to avoid mip/filter bleeding
        for y in range(CELL_SIZE):
            sy = min(max(y - PAD, 0), TILE_SIZE - 1)
            for x in range(CELL_SIZE):
                sx = min(max(x - PAD, 0), TILE_SIZE - 1)
                atlas.putpixel((ox + x, oy + y), src.getpixel((sx, sy)))
        uv_boxes[int(tex["id"])] = (ox + PAD, oy + PAD, name[:-4])
    atlas_out.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(atlas_out)
    return uv_boxes, size


def collect_groups(outliner, groups_by_id):
    elem_group = {}
    group_origins = {}

    def walk(node, path):
        if isinstance(node, str):
            elem_group[node] = path
        elif isinstance(node, dict):
            g = groups_by_id.get(node["uuid"], {})
            name = g.get("name", "")
            if name:
                group_origins[name] = g.get("origin", [0, 0, 0])
            next_path = [*path, name] if name else path
            for child in node.get("children", []):
                walk(child, next_path)

    for top in outliner:
        walk(top, [])
    return elem_group, group_origins


def export(name):
    settings = SETTINGS[name]
    bbmodel = ROOT / f"tools/models/{name}.bbmodel"
    tex_src = ROOT / f"tools/models/{name}_textures"
    atlas_out = ROOT / f"src/main/resources/assets/iuce/textures/entity/{name}.png"
    model_out = ROOT / f"src/main/resources/assets/iuce/models/multiblock/{name}.json"
    offset = settings["offset"]

    def shift(vec):
        return [round((vec[i] + offset[i]) * SCALE, 6) for i in range(3)]

    model = json.loads(bbmodel.read_text(encoding="utf-8"))
    uv_boxes, atlas_size = pack_atlas(model["textures"], tex_src, atlas_out)
    groups_by_id = {g["uuid"]: g for g in model.get("groups", [])}
    elem_group, group_origins = collect_groups(model.get("outliner", []), groups_by_id)
    spinners = settings.get("spinners", {})

    boxes = []
    for elem in model["elements"]:
        if "from" not in elem or "to" not in elem or not elem.get("export", True):
            continue
        path = elem_group.get(elem["uuid"], [])
        elem_name = elem.get("name", "")
        spinner = next((group for group in reversed(path) if group in spinners), None)
        tex_ids = {fv.get("texture") for fv in elem.get("faces", {}).values() if fv.get("texture") is not None}
        if spinner:
            part = spinner
        elif any(elem_name == light or elem_name.startswith(light + "_") for light in settings.get("emissive", ())):
            part = "emissive"
        elif any(uv_boxes[int(tid)][2] in settings.get("translucent", ()) for tid in tex_ids):
            part = "translucent"
        else:
            part = "static"
        section = next((group for group in ("tail", "bay", "stand") if group in path), "head")

        faces = {}
        for fname, fval in elem.get("faces", {}).items():
            tid = fval.get("texture")
            if tid is None:
                continue
            ox, oy, _ = uv_boxes[int(tid)]
            u0, v0, u1, v1 = fval.get("uv", [0, 0, 16, 16])
            faces[fname] = [round((ox + u0) / atlas_size, 6), round((oy + v0) / atlas_size, 6),
                            round((ox + u1) / atlas_size, 6), round((oy + v1) / atlas_size, 6)]
        if not faces:
            continue
        entry = {"part": part, "from": shift(elem["from"]), "to": shift(elem["to"]), "faces": faces}
        if section != "head":
            entry["section"] = section
        rot = elem.get("rotation", [0, 0, 0])
        if any(abs(a) > 1e-4 for a in rot):
            entry["rot"] = [round(float(a), 4) for a in rot]
            entry["origin"] = shift(elem.get("origin", [0, 0, 0]))
        boxes.append(entry)

    out = {
        "spinners": {key: {"pivot": shift(group_origins[key]), **spec}
                     for key, spec in spinners.items() if key in group_origins},
        "boxes": boxes,
    }
    if "stretch" in settings:
        out["stretch"] = settings["stretch"]
    model_out.parent.mkdir(parents=True, exist_ok=True)
    model_out.write_text(json.dumps(out, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"Exported {len(boxes)} boxes and atlas {atlas_out.relative_to(ROOT)} -> {model_out.relative_to(ROOT)}")


if __name__ == "__main__":
    for target in sys.argv[1:] or SETTINGS:
        export(target)
