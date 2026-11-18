#!/usr/bin/env python3
"""Exports tools/models/ore_processing_plant.bbmodel and its textures into
src/main/resources/assets/iuce/textures/entity/ore_processing_plant.png and
src/main/resources/assets/iuce/models/multiblock/ore_processing_plant.json.
"""
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
BBMODEL = ROOT / "tools/models/ore_processing_plant.bbmodel"
TEX_SRC = ROOT / "tools/models/ore_processing_plant_textures"
ATLAS_OUT = ROOT / "src/main/resources/assets/iuce/textures/entity/ore_processing_plant.png"
MODEL_OUT = ROOT / "src/main/resources/assets/iuce/models/multiblock/ore_processing_plant.json"

OFFSET = (8.0, 0.0, 56.0)
SCALE = 1.0 / 16.0
ATLAS_SIZE = 128
CELL_SIZE = 32
TILE_SIZE = 16
PAD = 8


def shift(vec):
    return [round((vec[i] + OFFSET[i]) * SCALE, 6) for i in range(3)]


def pack_atlas(textures):
    atlas = Image.new("RGBA", (ATLAS_SIZE, ATLAS_SIZE), (0, 0, 0, 0))
    uv_boxes = {}
    for idx, tex in enumerate(textures):
        name = tex["name"]
        if not name.endswith(".png"):
            name += ".png"
        src = Image.open(TEX_SRC / name).convert("RGBA")
        if src.size != (TILE_SIZE, TILE_SIZE):
            src = src.resize((TILE_SIZE, TILE_SIZE), Image.Resampling.NEAREST)
        col, row = idx % 4, idx // 4
        ox, oy = col * CELL_SIZE, row * CELL_SIZE
        # Fill 32x32 cell with clamped edge pixels to avoid mip/filter bleeding
        for y in range(CELL_SIZE):
            sy = min(max(y - PAD, 0), TILE_SIZE - 1)
            for x in range(CELL_SIZE):
                sx = min(max(x - PAD, 0), TILE_SIZE - 1)
                atlas.putpixel((ox + x, oy + y), src.getpixel((sx, sy)))
        uv_boxes[int(tex["id"])] = (ox + PAD, oy + PAD, name[:-4])
    ATLAS_OUT.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(ATLAS_OUT)
    return uv_boxes


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


def main():
    model = json.loads(BBMODEL.read_text(encoding="utf-8"))
    textures = model["textures"]
    uv_boxes = pack_atlas(textures)

    groups_by_id = {g["uuid"]: g for g in model.get("groups", [])}
    elem_group, group_origins = collect_groups(model.get("outliner", []), groups_by_id)

    boxes = []
    for elem in model["elements"]:
        if "from" not in elem or "to" not in elem:
            continue
        uuid = elem["uuid"]
        path = elem_group.get(uuid, [])
        name = elem.get("name", "")

        if "roller_front" in path:
            part = "roller_front"
        elif "roller_back" in path:
            part = "roller_back"
        elif "washing_drum" in path:
            part = "washing_drum"
        elif any(name == light or name.startswith(light + "_") for light in ("kiln_core_glow", "beacon_light")):
            part = "emissive"
        else:
            # Check if translucent glass texture
            tex_ids = {fv.get("texture") for fv in elem.get("faces", {}).values() if fv.get("texture") is not None}
            is_translucent = any(uv_boxes[int(tid)][2] == "gt_glass_frame" for tid in tex_ids)
            part = "translucent" if is_translucent else "static"

        faces = {}
        for fname, fval in elem.get("faces", {}).items():
            tid = fval.get("texture")
            if tid is None:
                continue
            ox, oy, _ = uv_boxes[int(tid)]
            u0, v0, u1, v1 = fval.get("uv", [0, 0, 16, 16])
            faces[fname] = [
                round((ox + u0) / ATLAS_SIZE, 6),
                round((oy + v0) / ATLAS_SIZE, 6),
                round((ox + u1) / ATLAS_SIZE, 6),
                round((oy + v1) / ATLAS_SIZE, 6),
            ]
        if not faces:
            continue

        entry = {
            "part": part,
            "from": shift(elem["from"]),
            "to": shift(elem["to"]),
            "faces": faces,
        }
        rot = elem.get("rotation", [0, 0, 0])
        if any(abs(a) > 1e-4 for a in rot):
            entry["rot"] = [round(float(a), 4) for a in rot]
            entry["origin"] = shift(elem.get("origin", [0, 0, 0]))
        boxes.append(entry)

    pivots = {
        key: shift(group_origins[key])
        for key in ("roller_front", "roller_back", "washing_drum")
        if key in group_origins
    }

    out = {"pivots": pivots, "boxes": boxes}
    MODEL_OUT.parent.mkdir(parents=True, exist_ok=True)
    MODEL_OUT.write_text(json.dumps(out, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"Exported {len(boxes)} boxes and atlas {ATLAS_OUT.relative_to(ROOT)} -> {MODEL_OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
