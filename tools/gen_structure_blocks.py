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
PLANT_TEX = "iuce:block/machine/plant/"
EMPTY = {"model": "iuce:block/plant_empty"}
SEGMENTS = (
    "main_feed_segment",
    "melting_segment",
    "side_feed_segment",
    "mixing_segment",
    "venting_segment",
    "die_segment",
)


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


# ------------------------------------------------------------------ Connected full-block casings / plates
# Edge strips sit just off the face, by an offset that depends on the axis of the edge they trim, so
# on any face the two crossing families of strips meet a corner on different planes (no z-fighting).
EDGE_SPECS = {
    "north": [
        ([-0.05, 0, 0], [-0.05, 16, 2], {"west": {"uv": [0, 0, 16, 2], "rotation": 270, "texture": "#trim", "cullface": "west"}}),
        ([16.05, 0, 0], [16.05, 16, 2], {"east": {"uv": [0, 0, 16, 2], "rotation": 90, "texture": "#trim", "cullface": "east"}}),
        ([0, -0.05, 0], [16, -0.05, 2], {"down": {"uv": [0, 0, 16, 2], "rotation": 0, "texture": "#trim", "cullface": "down"}}),
        ([0, 16.05, 0], [16, 16.05, 2], {"up": {"uv": [0, 0, 16, 2], "rotation": 0, "texture": "#trim", "cullface": "up"}}),
    ],
    "south": [
        ([-0.05, 0, 14], [-0.05, 16, 16], {"west": {"uv": [0, 0, 16, 2], "rotation": 90, "texture": "#trim", "cullface": "west"}}),
        ([16.05, 0, 14], [16.05, 16, 16], {"east": {"uv": [0, 0, 16, 2], "rotation": 270, "texture": "#trim", "cullface": "east"}}),
        ([0, -0.05, 14], [16, -0.05, 16], {"down": {"uv": [0, 0, 16, 2], "rotation": 180, "texture": "#trim", "cullface": "down"}}),
        ([0, 16.05, 14], [16, 16.05, 16], {"up": {"uv": [0, 0, 16, 2], "rotation": 180, "texture": "#trim", "cullface": "up"}}),
    ],
    "west": [
        ([0, 0, -0.025], [2, 16, -0.025], {"north": {"uv": [0, 0, 16, 2], "rotation": 90, "texture": "#trim", "cullface": "north"}}),
        ([0, 0, 16.025], [2, 16, 16.025], {"south": {"uv": [0, 0, 16, 2], "rotation": 270, "texture": "#trim", "cullface": "south"}}),
        ([0, -0.025, 0], [2, -0.025, 16], {"down": {"uv": [0, 0, 16, 2], "rotation": 270, "texture": "#trim", "cullface": "down"}}),
        ([0, 16.025, 0], [2, 16.025, 16], {"up": {"uv": [0, 0, 16, 2], "rotation": 270, "texture": "#trim", "cullface": "up"}}),
    ],
    "east": [
        ([14, 0, -0.025], [16, 16, -0.025], {"north": {"uv": [0, 0, 16, 2], "rotation": 270, "texture": "#trim", "cullface": "north"}}),
        ([14, 0, 16.025], [16, 16, 16.025], {"south": {"uv": [0, 0, 16, 2], "rotation": 90, "texture": "#trim", "cullface": "south"}}),
        ([14, -0.025, 0], [16, -0.025, 16], {"down": {"uv": [0, 0, 16, 2], "rotation": 90, "texture": "#trim", "cullface": "down"}}),
        ([14, 16.025, 0], [16, 16.025, 16], {"up": {"uv": [0, 0, 16, 2], "rotation": 90, "texture": "#trim", "cullface": "up"}}),
    ],
    "down": [
        ([0, 0, -0.075], [16, 2, -0.075], {"north": {"uv": [0, 0, 16, 2], "rotation": 180, "texture": "#trim", "cullface": "north"}}),
        ([0, 0, 16.075], [16, 2, 16.075], {"south": {"uv": [0, 0, 16, 2], "rotation": 180, "texture": "#trim", "cullface": "south"}}),
        ([-0.075, 0, 0], [-0.075, 2, 16], {"west": {"uv": [0, 0, 16, 2], "rotation": 180, "texture": "#trim", "cullface": "west"}}),
        ([16.075, 0, 0], [16.075, 2, 16], {"east": {"uv": [0, 0, 16, 2], "rotation": 180, "texture": "#trim", "cullface": "east"}}),
    ],
    "up": [
        ([0, 14, -0.075], [16, 16, -0.075], {"north": {"uv": [0, 0, 16, 2], "rotation": 0, "texture": "#trim", "cullface": "north"}}),
        ([0, 14, 16.075], [16, 16, 16.075], {"south": {"uv": [0, 0, 16, 2], "rotation": 0, "texture": "#trim", "cullface": "south"}}),
        ([-0.075, 14, 0], [-0.075, 16, 16], {"west": {"uv": [0, 0, 16, 2], "rotation": 0, "texture": "#trim", "cullface": "west"}}),
        ([16.075, 14, 0], [16.075, 16, 16], {"east": {"uv": [0, 0, 16, 2], "rotation": 0, "texture": "#trim", "cullface": "east"}}),
    ],
}


def connected_cube(name, panel_model, item_model, trim_tex, trim_faces=None):
    # Standalone / inventory model
    model(name, item_model)
    # Seamless panel base in world
    model(f"{name}_panel", panel_model)
    parts = [{"when": {"formed": "false"}, "apply": {"model": f"iuce:block/{name}_panel"}}]
    for side, elems in EDGE_SPECS.items():
        filtered = []
        for lo, hi, fdict in elems:
            if trim_faces is not None:
                fdict = {fn: fv for fn, fv in fdict.items() if fn in trim_faces}
            if fdict:
                filtered.append({"from": lo, "to": hi, "faces": fdict})
        if not filtered:
            continue
        model(f"{name}_edge_{side}", {
            "parent": "minecraft:block/block",
            "render_type": "minecraft:cutout",
            "textures": {"trim": trim_tex, "particle": trim_tex},
            "elements": filtered,
        })
        parts.append({"when": {"formed": "false", side: "false"}, "apply": {"model": f"iuce:block/{name}_edge_{side}"}})
    state(name, multipart=parts)


def checker_plate():
    # Top/bottom checker deck merges across horizontal neighbours; side rolled channel merges horizontally too
    connected_cube(
        "checker_plate",
        panel_model={
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {"top": TEX + "checker_plate", "bottom": TEX + "checker_plate_side", "side": TEX + "checker_plate_side"},
        },
        item_model={
            "parent": "minecraft:block/cube_bottom_top",
            "textures": {"top": TEX + "checker_plate_item", "bottom": TEX + "checker_plate_side", "side": TEX + "checker_plate_side_item"},
        },
        trim_tex=TEX + "checker_plate_trim",
        trim_faces=("up", "north", "south", "east", "west"),
    )


def tank_shell():
    connected_cube(
        "tank_shell",
        panel_model={"parent": "minecraft:block/cube_all", "textures": {"all": TEX + "tank_shell"}},
        item_model={"parent": "minecraft:block/cube_all", "textures": {"all": TEX + "tank_shell_item"}},
        trim_tex=TEX + "tank_shell_trim",
    )


def plant_drum():
    # Base seamless roll barrel (runs along Y in base model; rotated by axis in blockstate)
    barrel_elems = [
        {"from": [1, 0, 3], "to": [15, 16, 13], "faces": {
            "north": {"uv": [1, 0, 15, 16], "texture": "#side"},
            "south": {"uv": [1, 0, 15, 16], "texture": "#side"},
            "west": {"uv": [3, 0, 13, 16], "texture": "#side"},
            "east": {"uv": [3, 0, 13, 16], "texture": "#side"},
        }},
        {"from": [3, 0, 1], "to": [13, 16, 15], "faces": {
            "north": {"uv": [3, 0, 13, 16], "texture": "#side"},
            "south": {"uv": [3, 0, 13, 16], "texture": "#side"},
            "west": {"uv": [1, 0, 15, 16], "texture": "#side"},
            "east": {"uv": [1, 0, 15, 16], "texture": "#side"},
        }},
    ]
    # North-facing (-Z) end collar + bearing cap, rotated cleanly via y/x to any open drum end!
    cap_north = [
        {"from": [0.8, 2.8, 0], "to": [15.2, 13.2, 1.5], "faces": {
            "up": {"uv": [1, 0, 15, 1.5], "texture": "#collar"},
            "down": {"uv": [1, 0, 15, 1.5], "texture": "#collar"},
            "west": {"uv": [3, 0, 13, 1.5], "texture": "#collar"},
            "east": {"uv": [3, 0, 13, 1.5], "texture": "#collar"},
            "north": {"uv": [1, 3, 15, 13], "texture": "#end", "cullface": "north"},
            "south": {"uv": [1, 3, 15, 13], "texture": "#collar"},
        }},
        {"from": [2.8, 0.8, 0], "to": [13.2, 15.2, 1.5], "faces": {
            "up": {"uv": [3, 0, 13, 1.5], "texture": "#collar"},
            "down": {"uv": [3, 0, 13, 1.5], "texture": "#collar"},
            "west": {"uv": [1, 0, 15, 1.5], "texture": "#collar"},
            "east": {"uv": [1, 0, 15, 1.5], "texture": "#collar"},
            "north": {"uv": [3, 1, 13, 15], "texture": "#end", "cullface": "north"},
            "south": {"uv": [3, 1, 13, 15], "texture": "#collar"},
        }},
    ]
    # Item model along Y
    cap_up = [
        {"from": [0.8, 14.5, 2.8], "to": [15.2, 16, 13.2], "faces": {
            **{f: {"uv": [1, 0, 15, 1.5], "texture": "#collar"} for f in ("north", "south", "east", "west")},
            "up": {"uv": [1, 3, 15, 13], "texture": "#end"}, "down": {"uv": [1, 3, 15, 13], "texture": "#collar"}}},
        {"from": [2.8, 14.5, 0.8], "to": [13.2, 16, 15.2], "faces": {
            **{f: {"uv": [1, 0, 15, 1.5], "texture": "#collar"} for f in ("north", "south", "east", "west")},
            "up": {"uv": [3, 1, 13, 15], "texture": "#end"}, "down": {"uv": [3, 1, 13, 15], "texture": "#collar"}}},
    ]
    cap_down = [
        {"from": [0.8, 0, 2.8], "to": [15.2, 1.5, 13.2], "faces": {
            **{f: {"uv": [1, 14.5, 15, 16], "texture": "#collar"} for f in ("north", "south", "east", "west")},
            "down": {"uv": [1, 3, 15, 13], "texture": "#end"}, "up": {"uv": [1, 3, 15, 13], "texture": "#collar"}}},
        {"from": [2.8, 0, 0.8], "to": [13.2, 1.5, 15.2], "faces": {
            **{f: {"uv": [1, 14.5, 15, 16], "texture": "#collar"} for f in ("north", "south", "east", "west")},
            "down": {"uv": [3, 1, 13, 15], "texture": "#end"}, "up": {"uv": [3, 1, 13, 15], "texture": "#collar"}}},
    ]
    tex = {"particle": PLANT_TEX + "drum", "side": PLANT_TEX + "drum", "end": PLANT_TEX + "bearing", "collar": PLANT_TEX + "drum_collar"}
    model("plant_drum", {"parent": "minecraft:block/block", "textures": tex, "elements": barrel_elems + cap_up + cap_down})
    model("plant_drum_core", {"parent": "minecraft:block/block", "textures": tex, "elements": barrel_elems})
    model("plant_drum_cap", {"parent": "minecraft:block/block", "textures": tex, "elements": cap_north})

    dir_rots = {
        "north": {},
        "east": {"y": 90},
        "south": {"y": 180},
        "west": {"y": 270},
        "up": {"x": -90},
        "down": {"x": 90},
    }
    parts = [
        {"when": {"formed": "false", "axis": "y"}, "apply": {"model": "iuce:block/plant_drum_core"}},
        {"when": {"formed": "false", "axis": "z"}, "apply": {"model": "iuce:block/plant_drum_core", "x": 90}},
        {"when": {"formed": "false", "axis": "x"}, "apply": {"model": "iuce:block/plant_drum_core", "x": 90, "y": 90}},
        # End caps only on exposed axial ends
        {"when": {"formed": "false", "axis": "y", "neg": "false"}, "apply": {"model": "iuce:block/plant_drum_cap", **dir_rots["down"]}},
        {"when": {"formed": "false", "axis": "y", "pos": "false"}, "apply": {"model": "iuce:block/plant_drum_cap", **dir_rots["up"]}},
        {"when": {"formed": "false", "axis": "z", "neg": "false"}, "apply": {"model": "iuce:block/plant_drum_cap", **dir_rots["north"]}},
        {"when": {"formed": "false", "axis": "z", "pos": "false"}, "apply": {"model": "iuce:block/plant_drum_cap", **dir_rots["south"]}},
        {"when": {"formed": "false", "axis": "x", "neg": "false"}, "apply": {"model": "iuce:block/plant_drum_cap", **dir_rots["west"]}},
        {"when": {"formed": "false", "axis": "x", "pos": "false"}, "apply": {"model": "iuce:block/plant_drum_cap", **dir_rots["east"]}},
    ]
    state("plant_drum", multipart=parts)


def h_beam():
    # Crisp structural H-section column/girder (12x12, flanges 2.5 px thick, web 3 px thick)
    flange = lambda z0, z1: {"from": [2, 0, z0], "to": [14, 16, z1], "faces": {
        "north": {"uv": [2, 0, 14, 16], "texture": "#flange"},
        "south": {"uv": [2, 0, 14, 16], "texture": "#flange"},
        "east": {"uv": [z0, 0, z1, 16], "texture": "#edge"},
        "west": {"uv": [z0, 0, z1, 16], "texture": "#edge"},
        "up": {"uv": [2, z0, 14, z1], "texture": "#end", "cullface": "up"},
        "down": {"uv": [2, z0, 14, z1], "texture": "#end", "cullface": "down"}}}
    web = {"from": [6.5, 0, 4.5], "to": [9.5, 16, 11.5], "faces": {
        "east": {"uv": [4.5, 0, 11.5, 16], "texture": "#web"},
        "west": {"uv": [4.5, 0, 11.5, 16], "texture": "#web"},
        "up": {"uv": [6.5, 4.5, 9.5, 11.5], "texture": "#end", "cullface": "up"},
        "down": {"uv": [6.5, 4.5, 9.5, 11.5], "texture": "#end", "cullface": "down"}}}
    tex = {
        "particle": TEX + "h_beam_flange",
        "flange": TEX + "h_beam_flange",
        "edge": TEX + "h_beam_edge",
        "web": TEX + "h_beam_web",
        "end": TEX + "h_beam_end",
        "gusset": TEX + "h_beam_gusset",
    }
    model("plant_frame", {"parent": "minecraft:block/block", "textures": tex, "elements": [flange(2, 4.5), flange(11.5, 14), web]})

    # Bolted gusset / splice bracket arm reaching towards NORTH (-Z, z=0..5), rotated cleanly to all 6 sides
    gusset_north = [
        {"from": [2.2, 2.2, 0], "to": [13.8, 4.4, 5], "faces": {
            "up": {"uv": [2, 0, 14, 5], "texture": "#gusset"},
            "down": {"uv": [2, 0, 14, 5], "texture": "#gusset"},
            "west": {"uv": [0, 2, 5, 4.5], "texture": "#edge"},
            "east": {"uv": [0, 2, 5, 4.5], "texture": "#edge"},
            "north": {"uv": [2, 2, 14, 4.5], "texture": "#end", "cullface": "north"}}},
        {"from": [2.2, 11.6, 0], "to": [13.8, 13.8, 5], "faces": {
            "up": {"uv": [2, 0, 14, 5], "texture": "#gusset"},
            "down": {"uv": [2, 0, 14, 5], "texture": "#gusset"},
            "west": {"uv": [0, 11.5, 5, 14], "texture": "#edge"},
            "east": {"uv": [0, 11.5, 5, 14], "texture": "#edge"},
            "north": {"uv": [2, 11.5, 14, 14], "texture": "#end", "cullface": "north"}}},
        # Vertical bolted gusset web plate with 4-bolt splice detail
        {"from": [6.2, 3.5, 0], "to": [9.8, 12.5, 6.5], "faces": {
            "west": {"uv": [2, 3, 14, 13], "texture": "#gusset"},
            "east": {"uv": [2, 3, 14, 13], "texture": "#gusset"},
            "up": {"uv": [6, 0, 10, 6.5], "texture": "#edge"},
            "down": {"uv": [6, 0, 10, 6.5], "texture": "#edge"},
            "north": {"uv": [6, 3.5, 10, 12.5], "texture": "#end", "cullface": "north"}}},
        # Bolted end-plate collar right at the beam-to-column interface (z=0..1.5)
        {"from": [1.75, 1.75, 0], "to": [14.25, 14.25, 1.5], "faces": {
            "north": {"uv": [1.5, 1.5, 14.5, 14.5], "texture": "#gusset", "cullface": "north"},
            "south": {"uv": [1.5, 1.5, 14.5, 14.5], "texture": "#gusset"},
            "up": {"uv": [2, 0, 14, 1.5], "texture": "#edge"},
            "down": {"uv": [2, 0, 14, 1.5], "texture": "#edge"},
            "west": {"uv": [0, 2, 1.5, 14], "texture": "#edge"},
            "east": {"uv": [0, 2, 1.5, 14], "texture": "#edge"}}},
    ]
    model("plant_frame_gusset", {"parent": "minecraft:block/block", "textures": tex, "elements": gusset_north})

    dir_rots = {
        "north": {},
        "east": {"y": 90},
        "south": {"y": 180},
        "west": {"y": 270},
        "up": {"x": -90},
        "down": {"x": 90},
    }
    axis_perp = {
        "y": ("north", "south", "east", "west"),
        "z": ("up", "down", "east", "west"),
        "x": ("up", "down", "north", "south"),
    }
    parts = []
    for axis, rot in (("y", {}), ("z", {"x": 90}), ("x", {"x": 90, "y": 90})):
        parts.append({"when": {"formed": "false", "axis": axis}, "apply": {"model": "iuce:block/plant_frame", **rot}})
        for side in axis_perp[axis]:
            parts.append({"when": {"formed": "false", "axis": axis, side: "true"},
                          "apply": {"model": "iuce:block/plant_frame_gusset", **dir_rots[side]}})
    state("plant_frame", multipart=parts)


def process_pipe(name="process_pipe", flanged=True):
    tex = {"particle": TEX + name, "side": TEX + name, "end": TEX + name + "_end"}
    if flanged:
        tex["hub"] = TEX + name + "_hub"
    straight = [
        {"from": [3.5, 0, 4.5], "to": [12.5, 16, 11.5], "faces": {
            "north": {"uv": [3.5, 0, 12.5, 16], "texture": "#side"},
            "south": {"uv": [3.5, 0, 12.5, 16], "texture": "#side"},
            "west": {"uv": [4.5, 0, 11.5, 16], "texture": "#side"},
            "east": {"uv": [4.5, 0, 11.5, 16], "texture": "#side"},
            "up": {"uv": [3.5, 4.5, 12.5, 11.5], "texture": "#end", "cullface": "up"},
            "down": {"uv": [3.5, 4.5, 12.5, 11.5], "texture": "#end", "cullface": "down"}}},
        {"from": [4.5, 0, 3.5], "to": [11.5, 16, 12.5], "faces": {
            "north": {"uv": [4.5, 0, 11.5, 16], "texture": "#side"},
            "south": {"uv": [4.5, 0, 11.5, 16], "texture": "#side"},
            "west": {"uv": [3.5, 0, 12.5, 16], "texture": "#side"},
            "east": {"uv": [3.5, 0, 12.5, 16], "texture": "#side"},
            "up": {"uv": [4.5, 3.5, 11.5, 12.5], "texture": "#end", "cullface": "up"},
            "down": {"uv": [4.5, 3.5, 11.5, 12.5], "texture": "#end", "cullface": "down"}}},
    ]
    item_elems = list(straight)
    if flanged:
        for y0, y1 in ((0, 1.5), (14.5, 16)):
            item_elems.append({"from": [2.5, y0, 2.5], "to": [13.5, y1, 13.5], "faces": {
                **{f: {"uv": [2.5, 0, 13.5, 1.5], "texture": "#hub"} for f in ("north", "south", "east", "west")},
                "up": {"uv": [2.5, 2.5, 13.5, 13.5], "texture": "#end"},
                "down": {"uv": [2.5, 2.5, 13.5, 13.5], "texture": "#end"}}})
    model(name, {"parent": "minecraft:block/block", "textures": tex, "elements": item_elems})

    if not flanged:
        # Graphite electrodes stay clean straight cylinders
        parts = [
            {"when": {"formed": "false", "axis": "y"}, "apply": {"model": f"iuce:block/{name}"}},
            {"when": {"formed": "false", "axis": "z"}, "apply": {"model": f"iuce:block/{name}", "x": 90}},
            {"when": {"formed": "false", "axis": "x"}, "apply": {"model": f"iuce:block/{name}", "x": 90, "y": 90}},
        ]
        state(name, multipart=parts)
        return

    # Cast fitting hub (elbow / tee / cross) + north-facing branch arm rotated cleanly to all 6 directions!
    hub_elems = [
        {"from": [3.0, 3.0, 3.0], "to": [13.0, 13.0, 13.0], "faces": faces("#hub")},
    ]
    model(f"{name}_hub", {"parent": "minecraft:block/block", "textures": tex, "elements": hub_elems})

    # Branch arm pointing NORTH (-Z: z=0..4.5) so rotations match steam_pipe_side (north:0, east:y90, south:y180, west:y270, up:x-90, down:x90)
    arm_north = [
        {"from": [3.5, 4.5, 0], "to": [12.5, 11.5, 4.5], "faces": {
            "up": {"uv": [3.5, 0, 12.5, 4.5], "texture": "#side"},
            "down": {"uv": [3.5, 0, 12.5, 4.5], "texture": "#side"},
            "west": {"uv": [4.5, 0, 11.5, 4.5], "rotation": 90, "texture": "#side"},
            "east": {"uv": [4.5, 0, 11.5, 4.5], "rotation": 90, "texture": "#side"},
            "north": {"uv": [3.5, 4.5, 12.5, 11.5], "texture": "#end", "cullface": "north"}}},
        {"from": [4.5, 3.5, 0], "to": [11.5, 12.5, 4.5], "faces": {
            "up": {"uv": [4.5, 0, 11.5, 4.5], "texture": "#side"},
            "down": {"uv": [4.5, 0, 11.5, 4.5], "texture": "#side"},
            "west": {"uv": [3.5, 0, 12.5, 4.5], "rotation": 90, "texture": "#side"},
            "east": {"uv": [3.5, 0, 12.5, 4.5], "rotation": 90, "texture": "#side"},
            "north": {"uv": [4.5, 3.5, 11.5, 12.5], "texture": "#end", "cullface": "north"}}},
        # Bolted coupling flange collar at z=0..1.4
        {"from": [2.75, 2.75, 0], "to": [13.25, 13.25, 1.4], "faces": {
            "up": {"uv": [2.5, 0, 13.5, 1.5], "texture": "#hub"},
            "down": {"uv": [2.5, 0, 13.5, 1.5], "texture": "#hub"},
            "west": {"uv": [0, 2.5, 1.5, 13.5], "texture": "#hub"},
            "east": {"uv": [0, 2.5, 1.5, 13.5], "texture": "#hub"},
            "north": {"uv": [2.5, 2.5, 13.5, 13.5], "texture": "#end", "cullface": "north"},
            "south": {"uv": [2.5, 2.5, 13.5, 13.5], "texture": "#end"}}},
    ]
    model(f"{name}_arm", {"parent": "minecraft:block/block", "textures": tex, "elements": arm_north})
    model(f"{name}_straight", {"parent": "minecraft:block/block", "textures": tex, "elements": straight})

    flange_cap_north = [
        {"from": [2.75, 2.75, 0], "to": [13.25, 13.25, 1.5], "faces": {
            "up": {"uv": [2.5, 0, 13.5, 1.5], "texture": "#hub"},
            "down": {"uv": [2.5, 0, 13.5, 1.5], "texture": "#hub"},
            "west": {"uv": [0, 2.5, 1.5, 13.5], "texture": "#hub"},
            "east": {"uv": [0, 2.5, 1.5, 13.5], "texture": "#hub"},
            "north": {"uv": [2.5, 2.5, 13.5, 13.5], "texture": "#end", "cullface": "north"},
            "south": {"uv": [2.5, 2.5, 13.5, 13.5], "texture": "#end"}}},
    ]
    model(f"{name}_cap", {"parent": "minecraft:block/block", "textures": tex, "elements": flange_cap_north})

    dir_rots = {
        "north": {},
        "east": {"y": 90},
        "south": {"y": 180},
        "west": {"y": 270},
        "up": {"x": -90},
        "down": {"x": 90},
    }
    no_conn = {"down": "false", "up": "false", "north": "false", "south": "false", "west": "false", "east": "false"}
    any_perp = {
        "y": {"OR": [{d: "true"} for d in ("north", "south", "west", "east")]},
        "z": {"OR": [{d: "true"} for d in ("down", "up", "west", "east")]},
        "x": {"OR": [{d: "true"} for d in ("down", "up", "north", "south")]},
    }
    parts = [
        # 1. Isolated pipe: full straight barrel + end flanges along placed axis
        {"when": {"formed": "false", "axis": "y", **no_conn}, "apply": {"model": f"iuce:block/{name}"}},
        {"when": {"formed": "false", "axis": "z", **no_conn}, "apply": {"model": f"iuce:block/{name}", "x": 90}},
        {"when": {"formed": "false", "axis": "x", **no_conn}, "apply": {"model": f"iuce:block/{name}", "x": 90, "y": 90}},
        # 2. Pure coaxial straight pipe (connected only along its own axis): seamless barrel + flanges only on open ends
        {"when": {"AND": [{"formed": "false", "axis": "y", "north": "false", "south": "false", "west": "false", "east": "false"},
                  {"OR": [{"up": "true"}, {"down": "true"}]}]},
         "apply": {"model": f"iuce:block/{name}_straight"}},
        {"when": {"formed": "false", "axis": "y", "north": "false", "south": "false", "west": "false", "east": "false",
                  "up": "false", "down": "true"},
         "apply": {"model": f"iuce:block/{name}_cap", **dir_rots["up"]}},
        {"when": {"formed": "false", "axis": "y", "north": "false", "south": "false", "west": "false", "east": "false",
                  "up": "true", "down": "false"},
         "apply": {"model": f"iuce:block/{name}_cap", **dir_rots["down"]}},

        {"when": {"AND": [{"formed": "false", "axis": "z", "up": "false", "down": "false", "west": "false", "east": "false"},
                  {"OR": [{"north": "true"}, {"south": "true"}]}]},
         "apply": {"model": f"iuce:block/{name}_straight", "x": 90}},
        {"when": {"formed": "false", "axis": "z", "up": "false", "down": "false", "west": "false", "east": "false",
                  "north": "false", "south": "true"},
         "apply": {"model": f"iuce:block/{name}_cap", **dir_rots["north"]}},
        {"when": {"formed": "false", "axis": "z", "up": "false", "down": "false", "west": "false", "east": "false",
                  "north": "true", "south": "false"},
         "apply": {"model": f"iuce:block/{name}_cap", **dir_rots["south"]}},

        {"when": {"AND": [{"formed": "false", "axis": "x", "up": "false", "down": "false", "north": "false", "south": "false"},
                  {"OR": [{"west": "true"}, {"east": "true"}]}]},
         "apply": {"model": f"iuce:block/{name}_straight", "x": 90, "y": 90}},
        {"when": {"formed": "false", "axis": "x", "up": "false", "down": "false", "north": "false", "south": "false",
                  "west": "false", "east": "true"},
         "apply": {"model": f"iuce:block/{name}_cap", **dir_rots["west"]}},
        {"when": {"formed": "false", "axis": "x", "up": "false", "down": "false", "north": "false", "south": "false",
                  "west": "true", "east": "false"},
         "apply": {"model": f"iuce:block/{name}_cap", **dir_rots["east"]}},

        # 3. Elbow / Tee / Cross fitting (at least one perpendicular neighbour): cast fitting hub + arms on every connected side!
        {"when": {"AND": [{"formed": "false", "axis": "y"}, any_perp["y"]]}, "apply": {"model": f"iuce:block/{name}_hub"}},
        {"when": {"AND": [{"formed": "false", "axis": "z"}, any_perp["z"]]}, "apply": {"model": f"iuce:block/{name}_hub"}},
        {"when": {"AND": [{"formed": "false", "axis": "x"}, any_perp["x"]]}, "apply": {"model": f"iuce:block/{name}_hub"}},
    ]
    for axis in ("y", "z", "x"):
        for side, r in dir_rots.items():
            parts.append({
                "when": {"AND": [{"formed": "false", "axis": axis, side: "true"}, any_perp[axis]]},
                "apply": {"model": f"iuce:block/{name}_arm", **r},
            })
    state(name, multipart=parts)


def grating():
    bar = lambda a, b: {"from": a, "to": b, "faces": faces("#side")}
    model("steel_grating", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
        "particle": TEX + "steel_grating", "top": TEX + "steel_grating", "side": TEX + "steel_grating_side"},
        "elements": [
            {"from": [0, 0.2, 0], "to": [16, 2, 16], "faces": {
                **{f: {"uv": [0, 14, 16, 16], "texture": "#side", "cullface": f} for f in ("north", "south", "east", "west")},
                "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                "down": {"uv": [0, 0, 16, 16], "texture": "#top", "cullface": "down"}}},
            bar([0, 0, 0], [16, 2.2, 1]),
            bar([0, 0, 15], [16, 2.2, 16]),
            bar([0, 0, 1], [1, 2.2, 15]),
            bar([15, 0, 1], [16, 2.2, 15]),
        ]})
    state("steel_grating", {"formed=false": {"model": "iuce:block/steel_grating"}, "formed=true": EMPTY})


def stairs():
    textures = {"bottom": TEX + "checker_plate_side", "top": TEX + "checker_plate", "side": TEX + "checker_plate_side_item"}
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
    model("steel_handrail_post", {"textures": t, "elements": [
        box([6.5, 0, 6.5], [9.5, 1, 9.5]),
        box([7.25, 1, 7.25], [8.75, 15.5, 8.75]),
        box([7, 14.5, 7], [9, 16, 9])]})
    model("steel_handrail_side", {"textures": t, "elements": [
        box([7.5, 14.5, 0], [8.5, 15.5, 7.25]),
        box([7.5, 7.5, 0], [8.5, 8.5, 7.25])]})
    model("steel_handrail_inventory", {"parent": "minecraft:block/block", "textures": t, "elements": [
        box([2, 0, 7.25], [3.5, 16, 8.75]), box([12.5, 0, 7.25], [14, 16, 8.75]),
        box([0, 14.5, 7.5], [16, 15.5, 8.5]), box([0, 7.5, 7.5], [16, 8.5, 8.5])]})
    parts = [{"when": {"formed": "false"}, "apply": {"model": "iuce:block/steel_handrail_post"}}]
    for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        apply = {"model": "iuce:block/steel_handrail_side", "uvlock": True}
        if y: apply["y"] = y
        parts.append({"when": {"formed": "false", side: "true"}, "apply": apply})
    state("steel_handrail", multipart=parts)


def ladder():
    part = lambda a, b, tex: {"from": a, "to": b, "faces": faces(tex)}
    elems = [
        part([2, 0, 14.2], [3.5, 16, 15.6], "#rail"),
        part([12.5, 0, 14.2], [14, 16, 15.6], "#rail"),
    ]
    for y in (1.5, 5.5, 9.5, 13.5):
        elems.append(part([3.5, y, 14.5], [12.5, y + 1.2, 15.4], "#rung"))
    for y in (3, 12):
        elems.append(part([2, y, 15.6], [3.5, y + 1.5, 16], "#rail"))
        elems.append(part([12.5, y, 15.6], [14, y + 1.5, 16], "#rail"))
    model("steel_ladder", {"textures": {
        "particle": TEX + "steel_ladder", "rail": TEX + "steel_handrail", "rung": TEX + "steel_grating_side"},
        "elements": elems})
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        entry = {"model": "iuce:block/steel_ladder"}
        if y: entry["y"] = y
        variants[f"facing={facing},formed=false"] = entry
        variants[f"facing={facing},formed=true"] = EMPTY
    state("steel_ladder", variants)


def gas_hatch():
    model("gas_output_hatch", {"parent": "minecraft:block/orientable", "textures": {
        "front": "iuce:block/gas_output_hatch", "side": "iuce:block/machine/process/box_side", "top": "iuce:block/machine/process/box_top"}})
    state("gas_output_hatch", {"formed=false": {"model": "iuce:block/gas_output_hatch"}, "formed=true": EMPTY})


def segments():
    for name in SEGMENTS:
        model(name, {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": TEX + "segment_top",
            "bottom": TEX + "checker_plate_side",
            "side": f"iuce:block/{name}"}})
        state(name, {"formed=false": {"model": f"iuce:block/{name}"}, "formed=true": EMPTY})


def main():
    for name in ("refractory_bricks", "furnace_shell"):
        cube(name, name)
    tank_shell()
    checker_plate()
    plant_drum()
    h_beam()
    process_pipe("process_pipe", flanged=True)
    process_pipe("graphite_electrode", flanged=False)
    grating()
    stairs()
    handrail()
    ladder()
    gas_hatch()
    segments()


if __name__ == "__main__":
    main()
