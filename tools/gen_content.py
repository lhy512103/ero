#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generates everything a data-driven process machine needs, from one content table.

    python tools/gen_content.py            # write
    python tools/gen_content.py --check    # write nothing, fail if anything is stale

Inputs: every tools/content/*.json with a "machines" list.
  * tools/content/stage2.json    ships to players          -> src/main/resources/iuce/machines/
  * tools/content/regression.json  test-only                  -> src/regression/resources/iuce/machines/
A file may set "output": "regression" (test source set) or "output": "main" (the default).

A machine entry is a process machine definition (see ProcessMachineDef.CODEC) plus keys the game
never sees: name_en, name_zh, model and textures. For every machine it writes the definition, its
blockstate, block model (normal and running), item model and loot table; registers it as
mineable/pickaxe; and adds the two name keys when the language files lack them.

Assets always go to the main source set, so a regression-only machine still renders in-game.
Everything is written in a fixed order: running the script twice leaves `git diff` empty.
"""
import glob
import io
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..")).replace("\\", "/")
MAIN = "src/main/resources/"
REGRESSION = "src/regression/resources/"
CONTENT = sorted(glob.glob(ROOT + "/tools/content/*.json"))
CHECK = "--check" in sys.argv

# Keys a content entry carries for the generator only; the mod never reads them.
EXTRAS = {"name_en", "name_zh", "model", "textures"}
# Textures a "box" model needs, as (role, default path), where {id} is replaced by the machine id.
BOX = (("side", "iuce:block/machine/{id}/side"),
       ("top", "iuce:block/machine/{id}/top_1"),
       ("front", "iuce:block/machine/{id}/{id}_front"),
       ("front_active", "iuce:block/machine/{id}/{id}_front_active"))
FACING = (("north", 0), ("east", 90), ("south", 180), ("west", 270))

errors = []
written = 0


def read(path):
    with io.open(path, encoding="utf-8") as handle:
        return handle.read()


def load(path):
    with io.open(path, encoding="utf-8") as handle:
        return json.load(handle)


def write(path, data, compact=False):
    """Writes stable JSON; leaves the file alone when the text already matches."""
    global written
    text = (json.dumps(data, ensure_ascii=False, separators=(",", ":")) if compact
            else json.dumps(data, ensure_ascii=False, indent=2)) + "\n"
    full = ROOT + "/" + path
    if os.path.exists(full) and read(full) == text:
        return
    if CHECK:
        print("stale: " + path)
        errors.append(path)
        return
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with io.open(full, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(text)
    written += 1


def path_of(file, name):
    return file + "/" + name


# ------------------------------------------------------------------ per-machine files


def definition(machine):
    """The part of a content entry the mod reads."""
    return {key: value for key, value in machine.items() if key not in EXTRAS}


def blockstate(machine):
    variant = "iuce:block/process/" + machine["id"]
    variants = {}
    for facing, y in FACING:
        variants["facing=%s,running=false" % facing] = {"model": variant, "y": y}
        variants["facing=%s,running=true" % facing] = {"model": variant + "_active", "y": y}
    return {"variants": variants}


def block_model(machine, active):
    given = machine.get("textures", {})
    if machine.get("model", "box") != "box":
        raise SystemExit("unknown model %r for %s" % (machine.get("model"), machine["id"]))
    textures = {}
    for role, default in BOX:
        if role == "front_active" and role not in given:
            continue
        path = given.get(role, default).replace("{id}", machine["id"])
        textures[role] = path
    front = textures["front_active"] if active and "front_active" in textures else textures["front"]
    textures["particle"] = front
    return {
        "parent": "minecraft:block/block",
        "textures": textures,
        "elements": [{
            "from": [0, 0, 0], "to": [16, 16, 16],
            "faces": {
                "north": {"texture": "#front", "cullface": "north"},
                "south": {"texture": "#side", "cullface": "south"},
                "east": {"texture": "#side", "cullface": "east"},
                "west": {"texture": "#side", "cullface": "west"},
                "up": {"texture": "#top", "cullface": "up"},
                "down": {"texture": "#top", "cullface": "down"},
            },
        }],
    }


def loot_table(machine):
    return {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1.0, "bonus_rolls": 0.0,
            "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item", "name": "iuce:" + machine["id"]}],
        }],
        "random_sequence": "iuce:blocks/" + machine["id"],
    }


def generate(machine):
    name = machine["id"]
    write(MAIN + "assets/iuce/blockstates/%s.json" % name, blockstate(machine))
    write(MAIN + "assets/iuce/models/block/process/%s.json" % name, block_model(machine, False))
    write(MAIN + "assets/iuce/models/block/process/%s_active.json" % name, block_model(machine, True))
    write(MAIN + "assets/iuce/items/%s.json" % name,
          {"model": {"type": "minecraft:model", "model": "iuce:block/process/" + name}}, compact=True)
    write(MAIN + "data/iuce/loot_table/blocks/%s.json" % name, loot_table(machine))


# ------------------------------------------------------------------ main
by_output = {"main": [], "regression": []}
seen = {}
for file in CONTENT:
    table = load(file)
    output = table.get("output", "main")
    if output not in by_output:
        raise SystemExit("%s: unknown output %r" % (file, output))
    for machine in table.get("machines", []):
        if machine["id"] in seen:
            raise SystemExit("duplicate machine %r in %s and %s" % (machine["id"], seen[machine["id"]], file))
        seen[machine["id"]] = file
        by_output[output].append(machine)

for output, target in (("main", MAIN), ("regression", REGRESSION)):
    machines = sorted(by_output[output], key=lambda machine: machine["id"])
    if not machines:
        continue
    for machine in machines:
        write(target + "iuce/machines/%s.json" % machine["id"], definition(machine))
    write(target + "iuce/machines/_index%s.json" % ("" if output == "main" else "_regression"),
          {"machines": [machine["id"] for machine in machines]})

every = sorted(by_output["main"] + by_output["regression"], key=lambda machine: machine["id"])
for machine in every:
    generate(machine)

# mineable/pickaxe, keeping the existing order and adding only missing ids
pickaxe_path = MAIN + "data/minecraft/tags/block/mineable/pickaxe.json"
pickaxe = load(ROOT + "/" + pickaxe_path)
present = pickaxe.get("values", [])
absent = ["iuce:" + machine["id"] for machine in every if "iuce:" + machine["id"] not in present]
if absent:
    pickaxe["values"] = present + absent
    write(pickaxe_path, pickaxe)

# names: only fill in what the language files do not have yet
for code, field in (("zh_cn", "name_zh"), ("en_us", "name_en")):
    lang_path = MAIN + "assets/iuce/lang/%s.json" % code
    lang = load(ROOT + "/" + lang_path)
    changed = False
    for machine in every:
        name = machine.get(field)
        if name and "block.iuce." + machine["id"] not in lang:
            lang["block.iuce." + machine["id"]] = name
            changed = True
    if changed:
        write(lang_path, lang)

if CHECK:
    if errors:
        raise SystemExit("%d generated file(s) out of date; run tools/gen_content.py" % len(errors))
    print("generated content is up to date")
else:
    print("%d machine(s) (%d test-only); %d file(s) written"
          % (len(every), len(by_output["regression"]), written))
