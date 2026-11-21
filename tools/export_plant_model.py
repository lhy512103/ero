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
- tile: boxes are split on the 16-pixel grid and textured by position, one texel per Blockbench
  pixel, so large surfaces repeat the texture instead of stretching it.
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
    "twin_screw_extruder": {
        "offset": (0.0, 0.0, 0.0),
        "tile": True,
        "stretch": {"base_length": 13, "bay_first": 3, "bay_last": 3, "stand_first": 3, "stand_every": 4, "stand_last": 3},
    },
    "carbide_furnace_plant": {
        "offset": (0.0, 0.0, 0.0),
        "tile": True,
        "emissive": ("glow",),
    },
    "ore_processing_plant": {
        "offset": (0.0, 0.0, 0.0),
        "tile": True,
        "spinners": {
            "roller_front": {"axis": "x", "speed": 1.0},
            "roller_back": {"axis": "x", "speed": -1.0},
            "washing_drum": {"axis": "z", "speed": 0.65},
        },
        "emissive": ("glow",),
    },
    "rolling_mill": {
        "offset": (0.0, 0.0, 0.0),
        "tile": True,
        "spinners": {
            "entry_roll": {"axis": "x", "speed": 2.0},
            "bay_roll": {"axis": "x", "speed": 2.0},
            "exit_roll": {"axis": "x", "speed": 2.0},
            "stand_roll_top": {"axis": "x", "speed": 2.4},
            "stand_roll_bottom": {"axis": "x", "speed": -2.4},
            "stand_backup_top": {"axis": "x", "speed": -1.2},
            "stand_backup_bottom": {"axis": "x", "speed": 1.2},
            "coiler": {"axis": "x", "speed": -0.8},
        },
        "stretch": {"base_length": 9, "bay_first": 2, "bay_last": 2,
                    "stand_first": 2, "stand_every": 4, "stand_last": 2},
    },
    "coke_oven_battery": {
        "offset": (0.0, 0.0, 0.0),
        "tile": True,
        "emissive": ("glow",),
        "stretch": {"base_length": 17, "bay_first": 2, "bay_last": 2, "stand_first": 3, "stand_every": 2, "stand_last": 3},
    },
    "bio_ethylene_plant": {"offset": (0.0, 0.0, 0.0), "tile": True, "emissive": ("glow",)},
    "waste_heat_power_plant": {"offset": (0.0, 0.0, 0.0), "tile": True},
}


def pack_atlas(textures, tex_src, atlas_out):
    """Packs every texture at its own size with clamped edge padding. Returns id -> (x, y, name, w, h)."""
    images = []
    for tex in textures:
        name = tex["name"] if tex["name"].endswith(".png") else tex["name"] + ".png"
        src = Image.open(tex_src / name).convert("RGBA")
        if src.size == (TILE_SIZE, TILE_SIZE * 8) or src.height > src.width * 2:
            src = src.crop((0, 0, src.width, src.width))
        images.append((tex, name, src))
    size = 64
    while True:
        placed, x, y, shelf = {}, 0, 0, 0
        for tex, name, src in sorted(images, key=lambda item: -item[2].height):
            w, h = src.width + PAD * 2, src.height + PAD * 2
            if x + w > size:
                x, y, shelf = 0, y + shelf, 0
            placed[int(tex["id"])] = (x, y, name, src)
            x, shelf = x + w, max(shelf, h)
        if y + shelf <= size:
            break
        size *= 2
    atlas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    uv_boxes = {}
    for tid, (ox, oy, name, src) in placed.items():
        w, h = src.size
        for yy in range(h + PAD * 2):
            sy = min(max(yy - PAD, 0), h - 1)
            for xx in range(w + PAD * 2):
                sx = min(max(xx - PAD, 0), w - 1)
                atlas.putpixel((ox + xx, oy + yy), src.getpixel((sx, sy)))
        uv_boxes[tid] = (ox + PAD, oy + PAD, name[:-4], w, h)
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


# Which box axes run along a face's u and v, and the face's own axis and side.
FACE_AXES = {"north": (0, 1, 2, 0), "south": (0, 1, 2, 1), "west": (2, 1, 0, 0),
             "east": (2, 1, 0, 1), "up": (0, 2, 1, 1), "down": (0, 2, 1, 0)}


def cuts(lo, hi):
    """Pieces of lo..hi split on the global tile grid."""
    points = [lo]
    edge = (lo // TILE_SIZE + 1) * TILE_SIZE
    while edge < hi - 1e-6:
        points.append(edge)
        edge += TILE_SIZE
    return list(zip(points, points[1:] + [hi]))


def tiles(elem):
    """Splits a box on the global tile grid, keeping only outside faces. Uvs come from each piece's
    position, so neighbouring and overlapping boxes line up texel for texel."""
    lo, hi = elem["from"], elem["to"]
    pieces = []
    for xs in cuts(lo[0], hi[0]):
        for ys in cuts(lo[1], hi[1]):
            for zs in cuts(lo[2], hi[2]):
                a = [xs[0], ys[0], zs[0]]
                b = [xs[1], ys[1], zs[1]]
                faces = {}
                for fname, fval in elem.get("faces", {}).items():
                    if fval.get("texture") is None:
                        continue
                    u_axis, v_axis, n_axis, side = FACE_AXES[fname]
                    if (b if side else a)[n_axis] != (hi if side else lo)[n_axis]:
                        continue
                    u0 = a[u_axis] % TILE_SIZE
                    # Texture rows run down while y runs up.
                    v0 = (-b[v_axis]) % TILE_SIZE if v_axis == 1 else a[v_axis] % TILE_SIZE
                    faces[fname] = {"uv": [u0, v0, u0 + b[u_axis] - a[u_axis], v0 + b[v_axis] - a[v_axis]],
                                    "texture": fval["texture"]}
                if faces:
                    pieces.append((a, b, faces))
    return pieces


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
    uv_size = {int(t["id"]): (t.get("uv_width", 16), t.get("uv_height", 16)) for t in model["textures"]}
    groups_by_id = {g["uuid"]: g for g in model.get("groups", [])}
    elem_group, group_origins = collect_groups(model.get("outliner", []), groups_by_id)
    spinners = settings.get("spinners", {})

    def box_entry(lo, hi, face_map):
        faces = {}
        for fname, fval in face_map.items():
            tid = fval.get("texture")
            if tid is None:
                continue
            ox, oy, _, w, h = uv_boxes[int(tid)]
            scale = w / uv_size[int(tid)][0], h / uv_size[int(tid)][1]
            u0, v0, u1, v1 = fval.get("uv", [0, 0, 16, 16])
            faces[fname] = [round((ox + u0 * scale[0]) / atlas_size, 6), round((oy + v0 * scale[1]) / atlas_size, 6),
                            round((ox + u1 * scale[0]) / atlas_size, 6), round((oy + v1 * scale[1]) / atlas_size, 6)]
        return {"from": shift(lo), "to": shift(hi), "faces": faces} if faces else None

    def part_of(elem):
        path = elem_group.get(elem["uuid"], [])
        elem_name = elem.get("name", "")
        spinner = next((group for group in reversed(path) if group in spinners), None)
        if spinner:
            return spinner
        if any(elem_name == light or elem_name.startswith(light + "_") for light in settings.get("emissive", ())):
            return "emissive"
        return "static"

    # Free-form meshes: every face becomes a quad (triangles repeat their last corner). Meshes with
    # smooth shading share normals across faces at shared vertices, so round shells shade softly.
    quads = []
    for elem in model["elements"]:
        if elem.get("type") != "mesh" or not elem.get("export", True):
            continue
        part = part_of(elem)
        path = elem_group.get(elem["uuid"], [])
        section = next((group for group in ("tail", "bay", "stand") if group in path), "head")
        origin = elem.get("origin", [0, 0, 0])
        points = {key: [vec[i] + origin[i] for i in range(3)] for key, vec in elem["vertices"].items()}
        face_normals = {}
        for fid, face in elem["faces"].items():
            ks = face["vertices"]
            if len(ks) < 3:
                continue
            p0, p1, p2 = (points[k] for k in ks[:3])
            a = [p1[i] - p0[i] for i in range(3)]
            b = [p2[i] - p1[i] for i in range(3)]
            n = [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
            length = sum(c * c for c in n) ** 0.5 or 1.0
            face_normals[fid] = [c / length for c in n]
        smooth = {}
        if elem.get("shading") == "smooth":
            for fid, face in elem["faces"].items():
                for key in face["vertices"]:
                    acc = smooth.setdefault(key, [0.0, 0.0, 0.0])
                    for i in range(3):
                        acc[i] += face_normals.get(fid, [0, 0, 0])[i]
            for key, acc in smooth.items():
                length = sum(c * c for c in acc) ** 0.5 or 1.0
                smooth[key] = [c / length for c in acc]
        for fid, face in elem["faces"].items():
            tid = face.get("texture")
            if tid is None or len(face["vertices"]) not in (3, 4):
                continue
            ox, oy, tname, w, h = uv_boxes[int(tid)]
            uw, uh = uv_size[int(tid)]
            corners = list(face["vertices"])
            if len(corners) == 3:
                corners.append(corners[2])
            flat = []
            for key in corners:
                u, v = face["uv"][key]
                normal = smooth.get(key, face_normals[fid])
                flat += shift(points[key]) + [round((ox + u * w / uw) / atlas_size, 6), round((oy + v * h / uh) / atlas_size, 6)]
                flat += [round(c, 4) for c in normal]
            entry = {"part": "translucent" if part == "static" and tname in settings.get("translucent", ()) else part, "v": flat}
            if section != "head":
                entry["section"] = section
            quads.append(entry)

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

        rot = elem.get("rotation", [0, 0, 0])
        for sub_from, sub_to, sub_faces in (tiles(elem) if settings.get("tile") else [(elem["from"], elem["to"], elem.get("faces", {}))]):
            entry = box_entry(sub_from, sub_to, sub_faces)
            if entry is None:
                continue
            entry["part"] = part
            if section != "head":
                entry["section"] = section
            if any(abs(a) > 1e-4 for a in rot):
                entry["rot"] = [round(float(a), 4) for a in rot]
                entry["origin"] = shift(elem.get("origin", [0, 0, 0]))
            boxes.append(entry)

    out = {
        "spinners": {key: {"pivot": shift(group_origins[key]), **spec}
                     for key, spec in spinners.items() if key in group_origins},
        "boxes": boxes,
    }
    if quads:
        out["quads"] = quads
    if "stretch" in settings:
        out["stretch"] = settings["stretch"]
    model_out.parent.mkdir(parents=True, exist_ok=True)
    model_out.write_text(json.dumps(out, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"Exported {len(boxes)} boxes, {len(quads)} quads and atlas {atlas_out.relative_to(ROOT)} -> {model_out.relative_to(ROOT)}")


if __name__ == "__main__":
    for target in sys.argv[1:] or SETTINGS:
        export(target)
