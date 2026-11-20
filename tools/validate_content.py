#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Static checks over IUCE's data and assets. Standard library only.

    python tools/validate_content.py [repo root] [--tiers out.json] [--verbose]

Exits non-zero when an error is found. Run by `gradlew validateContent` (part of `check`).

Checks:
  1. every resource JSON parses;
  2. recipes: ids exist, no two recipes share a machine + input, every item is obtainable,
     no item is left without a use (unless listed in TERMINAL);
  3. assets: every item has a model definition and a name in zh_cn and en_us;
  4. guide: every item id, page link and structure reference resolves.

Vanilla ids come from the patched Minecraft jar under build/moddev/artifacts; when it has not been
generated yet, vanilla references are not checked and a warning is printed.
"""
import glob
import io
import json
import os
import re
import sys
import zipfile
from collections import defaultdict

args = [a for a in sys.argv[1:] if not a.startswith("--")]
ROOT = os.path.abspath(args[0] if args else os.path.join(os.path.dirname(__file__), "..")).replace("\\", "/")
VERBOSE = "--verbose" in sys.argv
TIERS_OUT = sys.argv[sys.argv.index("--tiers") + 1] if "--tiers" in sys.argv else None
RES = ROOT + "/src/main/resources/"
RECIPES = RES + "data/iuce/recipe/"
ASSETS = RES + "assets/iuce/"

errors, warnings = [], []


def read(path):
    with io.open(path, encoding="utf-8") as handle:
        return handle.read()


def java(path):
    return read(ROOT + "/src/main/java/" + path)


# ------------------------------------------------------------------ machine rules
# machine_type -> block that runs it. Keep in sync with the machines themselves.
STATION = {
    "macerator": "macerator", "compressor": "compressor", "extractor": "extractor",
    "electric_furnace": "electric_furnace", "electrolyzer": "electrolyzer", "ore_washer": "ore_washer",
    "alloy_furnace": "induction_furnace", "planter": "planter",
    "metal_former_rolling": "metal_former", "metal_former_cutting": "metal_former",
    "metal_former_extruding": "metal_former",
}
SUPPORTS_EXTRA_RESULTS = {"electric_furnace", "ore_washer", "electrolyzer"}
SUPPORTS_SECOND_INPUT = {"alloy_furnace", "planter", "ore_washer"}
ORDER_INSENSITIVE = {"alloy_furnace"}
# Machines whose code reads the recipe's fluid_input (and the fluid their tank holds).
USES_FLUID_INPUT = {"ore_washer": "minecraft:water", "electrolyzer": "minecraft:water"}
# No machine consumes these fields yet; a recipe using them would silently ignore them.
USES_FLUID_OUTPUT = set()
USES_CATALYST = set()
MAX_EU_PER_TICK = 128
# EU/t per voltage tier, matching energy/EUVoltage.
VOLTAGE = {"ULV": 8, "LV": 32, "MV": 128, "HV": 512, "EV": 2048, "IV": 8192,
           "LuV": 32768, "ZPM": 131072, "UV": 524288, "UHV": 2097152}

# Process machines (iuce:process) come from the generated definitions, not from STATION.
PROCESS_SOURCES = (RES, ROOT + "/src/regression/resources/")
# Whoever supplies these arrives with M1; a recipe may reference them, nothing else checks them.
UTILITIES = {"steam", "cooling_water", "lubricant", "air"}
SEGMENTS = {"side_feed", "screen", "filter", "die"}
CARD_OPTIONS = 4          # each option may be set from 1 to this level
CARD_SLOTS_MAX = 8        # module slots on a process card

# Items that are an end in themselves: machines, tools, upgrades, fuel, decoration.
TERMINAL = set(STATION.values()) | {
    "air_compressor", "crate", "toy_brick", "plastic_pail", "drip_emitter", "pipe_fitting",
    "machine_housing", "rigid_pvc_dry_blend", "soft_pvc_dry_blend", "heavy_residue",
    "process_card", "process_design_table", "pla_injection_pellet", "pla_film_pellet",
    "generator", "geothermal_generator", "industrial_workbench", "pump", "transformer", "chute", "conveyor", "factory_hub",
    "logistics_port", "silo", "fluid_tank", "configurator",
    "plant_controller", "plant_frame", "plant_drum", "item_input_hatch", "item_output_hatch", "energy_hatch",
    "energy_hatch_hv", "mill_controller", "repair_bench", "lamp", "converter", "builder", "blueprint_terminal",
    "batbox", "cesu", "mfe", "mfsu", "singulary_solar_generator",
    "copper_cable", "tin_cable", "gold_cable", "iron_cable", "glass_cable", "singular_cable",
    "insulated_copper_cable", "insulated_tin_cable", "insulated_gold_cable", "insulated_hv_cable",
    "fission_controller", "fission_controller_front", "reactor_casing", "reactor_casing_back", "reactor_casing_side",
    "wrench", "cutter", "forge_hammer", "treetap", "electric_wrench", "electric_treetap", "electric_hoe", "guide",
    "plate_mold", "wire_mold", "rod_mold",
    "ejector_upgrade", "pulling_upgrade", "fluid_ejector_upgrade", "fluid_pulling_upgrade", "energy_storage_upgrade",
    "overclocker_upgrade", "overclocker_upgrade1", "overclocker_upgrade2",
    "coal_fuel", "single_use_battery", "irradiant_uranium_ingot", "coolant_bucket",
    "rubber_sapling", "machine_casing_2", "logo", "coolant",
    # stage 2 utilities: suppliers, their pipes, and by-products that end as boiler fuel or compost
    "boiler", "cooling_tower", "creative_utility_source", "steam_pipe", "cooling_water_pipe", "air_pipe",
    "bagasse", "distillers_grains",
}
# Obtained from the world rather than from a recipe.
WORLD_SOURCES = {"rubber_wood", "rubber_leaves", "rubber_sapling", "raw_latex", "limestone", "rock_salt"}
# Registered but intentionally unobtainable / not shown.
NOT_CRAFTABLE = {"logo", "coolant", "creative_utility_source"}
# Process machines that only take fluids away (venting, flaring): no output slot or tank on purpose.
SINKS = {"vent_stack"}
# Test-only process machines (tools/content/regression.json): never crafted and never used.
TEST_ONLY = {"test_press"}

# ------------------------------------------------------------------ 1. JSON validity
json_count = 0
for path in glob.glob(RES + "**/*.json", recursive=True):
    json_count += 1
    try:
        json.loads(read(path))
    except ValueError as error:
        errors.append("invalid JSON %s: %s" % (os.path.relpath(path, ROOT), error))

# ------------------------------------------------------------------ known ids
items = set(re.findall(r'register(?:Simple)?Item\(\s*"([a-z0-9_]+)"', java("item/ModItems.java")))
blocks = set(re.findall(r'register(?:Simple)?Block\(\s*"([a-z0-9_]+)"', java("block/ModBlocks.java")))
block_item_source = java("block/ModBlockItems.java")
block_items = {name.lower() for name in re.findall(r"registerBlockItem\(ModBlocks\.([A-Z0-9_]+)\)", block_item_source)}
block_items |= set(re.findall(r'register(?:Block|Controller)Item\("([a-z0-9_]+)"', block_item_source))
block_items &= blocks
all_items = items | block_items

vanilla_items, vanilla_tags = None, None
jars = [p for p in glob.glob(ROOT + "/build/moddev/artifacts/minecraft-patched-*.jar")
        if not p.endswith("-sources.jar") and not p.endswith("-merged.jar")]
match = re.search(r"^neo_version=(.+)$", read(ROOT + "/gradle.properties"), re.M)
if match:
    preferred = [p for p in jars if match.group(1).strip() in os.path.basename(p)]
    jars = preferred or jars
if jars:
    with zipfile.ZipFile(sorted(jars)[-1]) as archive:
        names = archive.namelist()
    vanilla_items = {n[len("assets/minecraft/items/"):-5] for n in names
                     if n.startswith("assets/minecraft/items/") and n.endswith(".json")}
    vanilla_tags = {n[len("data/minecraft/tags/item/"):-5] for n in names
                    if n.startswith("data/minecraft/tags/item/") and n.endswith(".json")}
else:
    warnings.append("patched Minecraft jar not found (run a Gradle build first); vanilla ids are not checked")


def check_id(ref, where):
    if ref.startswith("#"):
        namespace, _, name = ref[1:].partition(":")
        if namespace == "minecraft" and vanilla_tags is not None and name not in vanilla_tags:
            errors.append("%s: unknown tag %s" % (where, ref))
        return
    namespace, _, name = ref.partition(":")
    if namespace == "iuce" and name not in all_items:
        errors.append("%s: unknown item %s" % (where, ref))
    elif namespace == "minecraft" and vanilla_items is not None and name not in vanilla_items:
        errors.append("%s: unknown item %s" % (where, ref))


# ------------------------------------------------------------------ 2. recipes
process_machines = {}   # id -> definition, from the generated iuce/machines/_index*.json
for source in PROCESS_SOURCES:
    for index_name in ("iuce/machines/_index.json", "iuce/machines/_index_regression.json"):
        index_path = source + index_name
        if not os.path.exists(index_path):
            continue
        try:
            index = json.loads(read(index_path))
        except ValueError:
            continue
        for machine_id in index.get("machines", []):
            path = source + "iuce/machines/" + machine_id + ".json"
            try:
                definition = json.loads(read(path))
            except (IOError, ValueError):
                errors.append("cannot read machine definition " + os.path.relpath(path, ROOT))
                continue
            if definition.get("id") != machine_id:
                errors.append("%s: id does not match the index" % os.path.relpath(path, ROOT))
            process_machines[machine_id] = definition
# Their blocks and block items are registered from the definitions at startup.
block_items |= set(process_machines)
all_items |= set(process_machines)
for machine_id, definition in process_machines.items():
    if definition.get("items_in", 1) > 9 or definition.get("items_out", 1) > 9:
        errors.append("%s: at most 9 input and 9 output slots" % machine_id)
    if definition.get("items_out", 1) < 1 and not definition.get("fluids_out") and machine_id not in SINKS:
        errors.append("%s: produces nothing (no output slot or tank)" % machine_id)
    for key in ("fluids_in", "fluids_out"):
        for capacity in definition.get(key, []):
            if capacity < 1000:
                errors.append("%s: %s tank of %d mB holds less than one bucket" % (machine_id, key, capacity))
    if definition.get("voltage", "LV") not in VOLTAGE:
        errors.append("%s: unknown voltage %s" % (machine_id, definition.get("voltage")))
    if not os.path.exists(ASSETS + "blockstates/" + machine_id + ".json"):
        errors.append("%s: missing blockstate (run tools/gen_content.py)" % machine_id)

recipes = []  # (name, inputs, outputs, station or None)
by_machine_input = defaultdict(list)
by_crafting_shape = defaultdict(list)
by_process_input = defaultdict(list)
process_paths = [p for p in glob.glob(RECIPES + "*/*.json")]
process_paths += [p for p in glob.glob(ROOT + "/src/regression/resources/data/iuce/recipe/*/*.json")]
for path in sorted(process_paths):
    name = os.path.relpath(path, RECIPES).replace("\\", "/")[:-5]
    if path.startswith(ROOT + "/src/regression/"):
        name = os.path.relpath(path, ROOT + "/src/regression/resources/data/iuce/recipe").replace("\\", "/")[:-5]
    try:
        data = json.loads(read(path))
    except ValueError:
        continue
    kind = data.get("type", "")
    if kind == "iuce:process":
        machine = data["machine"]
        definition = process_machines.get(machine)
        if definition is None:
            errors.append("%s: unknown process machine %s" % (name, machine))
            continue
        capacity = definition.get("voltage", "LV")
        items_in = [entry["item"] for entry in data.get("items", [])]
        fluid_counts = [entry["amount"] for entry in data.get("fluids", [])]
        if len(data.get("items", [])) > definition.get("items_in", 1):
            errors.append("%s: %s has %d input slots, recipe wants %d" % (name, machine, definition.get("items_in", 1), len(items_in)))
        for entry in data.get("items", []):
            if entry.get("count", 1) < 1:
                errors.append("%s: item input count must be positive" % name)
        if len(fluid_counts) > len(definition.get("fluids_in", [])):
            errors.append("%s: %s has %d input tanks, recipe wants %d" % (name, machine, len(definition.get("fluids_in", [])), len(fluid_counts)))
        for i, amount in enumerate(fluid_counts):
            if i < len(definition.get("fluids_in", [])) and amount > definition["fluids_in"][i]:
                errors.append("%s: %d mB does not fit the %d mB input tank" % (name, amount, definition["fluids_in"][i]))
        for entry in data.get("fluid_results", []):
            if not any(entry["amount"] <= cap for cap in definition.get("fluids_out", [])):
                errors.append("%s: %d mB of output has no tank big enough" % (name, entry["amount"]))
        slots_out = definition.get("items_out", 1)
        if len(data.get("results", [])) > slots_out:
            warnings.append("%s: %d products but only %d output slots; the rest cannot leave" % (name, len(data.get("results", [])), slots_out))
        if "catalyst" in data and not definition.get("catalyst_slot"):
            errors.append("%s: %s has no catalyst slot" % (name, machine))
        if "card" in data:
            if not definition.get("card_slot"):
                errors.append("%s: %s has no card slot" % (name, machine))
            options = data["card"].get("options", {})
            if len(options) > CARD_SLOTS_MAX:
                errors.append("%s: a process card holds at most %d modules" % (name, CARD_SLOTS_MAX))
            for option, level in options.items():
                if not 1 <= level <= CARD_OPTIONS:
                    errors.append("%s: card option %s must be 1-%d" % (name, option, CARD_OPTIONS))
        for utility in data.get("utilities", []):
            if utility not in UTILITIES:
                errors.append("%s: unknown utility %s" % (name, utility))
        for segment in data.get("segments", []):
            if segment not in SEGMENTS:
                errors.append("%s: unknown segment %s" % (name, segment))
        if data["time"] <= 0:
            errors.append("%s: time must be positive" % name)
        elif data["energy"] / data["time"] > VOLTAGE[capacity]:
            errors.append("%s: %.0f EU/t exceeds %s's %d EU/t" % (name, data["energy"] / data["time"], capacity, VOLTAGE[capacity]))
        catalyst = data.get("catalyst", {}).get("item")
        if catalyst and data["catalyst"].get("damage", 0) > 0 and definition.get("items_out", 1) < 1:
            errors.append("%s: a wearing catalyst leaves as spent catalyst, but %s has no output slot" % (name, machine))
        key = (machine, catalyst, json.dumps(data.get("card"), sort_keys=True),
               tuple(items_in) + tuple("fluid:" + entry["fluid"] for entry in data.get("fluids", [])))
        if machine == "cnc_machining_center":
            key += (json.dumps(data["results"][0].get("components"), sort_keys=True),)
        by_process_input[key].append(name)
        # the catalyst is not consumed, but the machine cannot run without one, so it counts as an input
        inputs = items_in + ([catalyst] if catalyst else [])
        outputs = [entry["id"] for entry in data.get("results", [])]
        if catalyst and data["catalyst"].get("damage", 0) > 0:
            outputs.append("iuce:spent_catalyst")   # a worn-out catalyst leaves as spent catalyst
        recipes.append((name, inputs, outputs, "iuce:" + machine))
        for ref in inputs + outputs:
            check_id(ref, name)
        continue
    if kind == "iuce:machine":
        machine = data["machine_type"]
        if machine not in STATION:
            errors.append("%s: unknown machine_type %s" % (name, machine))
        inputs = [data["input"]] + ([data["input2"]] if "input2" in data else [])
        outputs = [data["result"]["id"]] + [extra["result"]["id"] for extra in data.get("extra_results", [])]
        key = tuple(sorted(inputs)) if machine in ORDER_INSENSITIVE else tuple(inputs)
        by_machine_input[(machine, key)].append(name)
        if data["time"] <= 0:
            errors.append("%s: time must be positive" % name)
        elif data["energy"] / data["time"] > MAX_EU_PER_TICK:
            warnings.append("%s: %.0f EU/t exceeds %d" % (name, data["energy"] / data["time"], MAX_EU_PER_TICK))
        if data.get("extra_results") and machine not in SUPPORTS_EXTRA_RESULTS:
            errors.append("%s: %s ignores extra_results" % (name, machine))
        fluid_in = data.get("fluid_input")
        if fluid_in is not None:
            if machine not in USES_FLUID_INPUT:
                errors.append("%s: %s ignores fluid_input" % (name, machine))
            elif fluid_in.get("id") != USES_FLUID_INPUT[machine]:
                errors.append("%s: %s only holds %s" % (name, machine, USES_FLUID_INPUT[machine]))
            if not isinstance(fluid_in.get("amount"), int) or fluid_in["amount"] <= 0:
                errors.append("%s: fluid_input needs a positive amount" % name)
        elif machine in USES_FLUID_INPUT:
            warnings.append("%s: %s recipe without fluid_input runs dry" % (name, machine))
        if "fluid_output" in data and machine not in USES_FLUID_OUTPUT:
            errors.append("%s: %s ignores fluid_output" % (name, machine))
        if "catalyst" in data:
            if machine not in USES_CATALYST:
                errors.append("%s: %s ignores catalyst" % (name, machine))
            check_id(data["catalyst"], name)
        if "input2" in data and machine not in SUPPORTS_SECOND_INPUT:
            errors.append("%s: %s has no second input" % (name, machine))
        recipes.append((name, inputs, outputs, "iuce:" + STATION.get(machine, machine)))
    elif kind in ("iuce:crafting", "iuce:standard_crafting", "iuce:optional_crafting"):
        grid = data["input"]
        if grid and isinstance(grid[0], list):
            if len({len(row) for row in grid}) != 1 or len(grid) > 3 or len(grid[0]) > 3:
                errors.append("%s: bad grid" % name)
                continue
            rows = list(grid)
            while rows and all(cell == "" for cell in rows[0]):
                rows = rows[1:]
            while rows and all(cell == "" for cell in rows[-1]):
                rows = rows[:-1]
            columns = [i for i in range(len(rows[0])) if any(row[i] != "" for row in rows)]
            key = ("shaped", tuple(tuple(row[columns[0]:columns[-1] + 1]) for row in rows))
            inputs = [cell for row in grid for cell in row if cell != ""]
        else:
            key = ("shapeless", tuple(sorted(grid)))
            inputs = list(grid)
        by_crafting_shape[(kind, key)].append(name)
        station = None if kind == "iuce:standard_crafting" else "iuce:industrial_workbench"
        recipes.append((name, inputs, [data["result"]["id"]], station))
    elif kind == "minecraft:smelting":
        ingredient = data["ingredient"]
        ref = ingredient["items"] if isinstance(ingredient, dict) else ingredient
        recipes.append((name, [ref], [data["result"]["id"]], None))
    else:
        errors.append("%s: unknown recipe type %s" % (name, kind))
        continue
    for ref in recipes[-1][1] + recipes[-1][2]:
        check_id(ref, name)

for names in list(by_machine_input.values()) + list(by_crafting_shape.values()) + list(by_process_input.values()):
    if len(names) > 1:
        errors.append("recipes with identical inputs: " + ", ".join(names))
flattened = defaultdict(list)
for (kind, (shape, key)), names in by_crafting_shape.items():
    flat = tuple(sorted(cell for row in key for cell in row if cell != "")) if shape == "shaped" else key
    flattened[(kind, flat)].append((shape, names[0]))
for entries in flattened.values():
    if len(entries) > 1 and any(shape == "shapeless" for shape, _ in entries):
        errors.append("shaped and shapeless recipes overlap: " + ", ".join(name for _, name in entries))

# reachability: what can be made starting from ores, world sources and vanilla items
have = {"iuce:" + block for block in blocks if block.endswith("_ore")} | {"iuce:" + name for name in WORLD_SOURCES}
tier = {item: 0 for item in have}
round_number = 0
while True:
    round_number += 1
    found = {}
    for name, inputs, outputs, station in recipes:
        usable = all(ref.startswith("minecraft:") or ref.startswith("#") or ref in have for ref in inputs)
        powered = station is None or (station in have and "iuce:generator" in have)
        if usable and powered:
            for output in outputs:
                if output.startswith("iuce:") and output not in have and output not in found:
                    found[output] = round_number
    if not found:
        break
    have |= set(found)
    tier.update(found)
for item in sorted({"iuce:" + name for name in all_items - NOT_CRAFTABLE - TEST_ONLY} - have):
    errors.append("unobtainable: " + item)

used = {ref for _, inputs, _, _ in recipes for ref in inputs}
# A process machine is an end in itself: it only has to be obtainable (checked above).
for item in sorted(all_items - set(process_machines) - TEST_ONLY):
    if ("iuce:" + item) in used or item in TERMINAL or item.endswith("_block") or item.endswith("_ore"):
        continue
    errors.append("no use for iuce:%s (add a recipe that consumes it, or list it in TERMINAL)" % item)

# ------------------------------------------------------------------ 3. assets and names
languages = {}
for code in ("zh_cn", "en_us"):
    try:
        languages[code] = json.loads(read(ASSETS + "lang/" + code + ".json"))
    except (IOError, ValueError):
        errors.append("cannot read lang/%s.json" % code)
        languages[code] = {}
for item in sorted(all_items):
    if not os.path.exists(ASSETS + "items/" + item + ".json"):
        errors.append("missing item model definition: assets/iuce/items/%s.json" % item)
    key = ("block.iuce." if item in block_items else "item.iuce.") + item
    for code, table in languages.items():
        if key not in table:
            errors.append("missing %s translation: %s" % (code, key))
for block in sorted(block_items):
    if not os.path.exists(RES + "data/iuce/loot_table/blocks/" + block + ".json"):
        errors.append("missing loot table (the block would drop nothing): data/iuce/loot_table/blocks/%s.json" % block)
for path in glob.glob(ROOT + "/src/main/java/**/*.java", recursive=True):
    for key in set(re.findall(r'translatable\("([a-z0-9_.]*\biuce\.[a-z0-9_.]+)"[,)]', read(path))):
        for code, table in languages.items():
            if key not in table:
                errors.append("%s uses %s, which lang/%s.json does not define" % (os.path.basename(path), key, code))
for key in sorted(set(languages["zh_cn"]) - set(languages["en_us"])):
    warnings.append("en_us is missing key present in zh_cn: " + key)

# ------------------------------------------------------------------ 4. guide
GUIDE = ASSETS + "guide/"
structures = {os.path.basename(p)[:-5] for p in glob.glob(GUIDE + "structures/*.json")}
for language_dir in sorted(glob.glob(GUIDE + "*/")):
    language = os.path.basename(os.path.dirname(language_dir))
    if language == "structures":
        continue
    pages = {os.path.relpath(p, language_dir).replace("\\", "/")[:-3]
             for p in glob.glob(language_dir + "**/*.md", recursive=True)}
    try:
        index = json.loads(read(language_dir + "_index.json"))
    except (IOError, ValueError):
        errors.append("guide/%s: cannot read _index.json" % language)
        continue
    categories = {entry["id"] for entry in index.get("categories", [])}
    if index.get("home") not in pages:
        errors.append("guide/%s: home page %r does not exist" % (language, index.get("home")))
    for page in sorted(pages):
        text = read(language_dir + page + ".md")
        where = "guide/%s/%s" % (language, page)
        header = re.match(r"---\n(.*?)\n---\n", text, re.S)
        if not header:
            errors.append(where + ": missing front matter")
            continue
        category = re.search(r"^category:\s*(.+)$", header.group(1), re.M)
        if not category or category.group(1).strip() not in categories:
            errors.append(where + ": unknown category")
        for ref in set(re.findall(r"\b(?:iuce|minecraft):[a-z0-9_]+", text)):
            check_id(ref, where)
        links = re.findall(r"\]\(([a-z0-9_/]+)\)", text)
        for cards in re.findall(r"\{cards:([^}]*)\}", text):
            links += [entry.strip() for entry in cards.split(",") if entry.strip()]
        for link in set(links):
            if link not in pages:
                errors.append("%s: link to missing page %s" % (where, link))
        for structure in re.findall(r"\{structure:\s*([a-z0-9_]+)", text):
            if structure not in structures:
                errors.append("%s: missing structure %s" % (where, structure))
        for machine in re.findall(r"\{recipes:\s*([a-z0-9_]+)", text):
            if machine not in STATION and machine not in process_machines:
                errors.append("%s: unknown machine type %s" % (where, machine))
for structure in sorted(structures):
    data = json.loads(read(GUIDE + "structures/" + structure + ".json"))
    for ref in data.get("palette", {}).values():
        check_id(ref, "guide/structures/" + structure)

# ------------------------------------------------------------------ report
print("checked %d JSON files, %d recipes, %d items" % (json_count, len(recipes), len(all_items)))
for warning in warnings:
    print("warning: " + warning)
for error in errors:
    print("error: " + error)
if VERBOSE or TIERS_OUT:
    rounds = defaultdict(list)
    for item, number in tier.items():
        rounds[number].append(item[5:])
    if VERBOSE:
        for number in sorted(rounds):
            print("round %d: %s" % (number, " ".join(sorted(rounds[number]))))
    if TIERS_OUT:
        with io.open(TIERS_OUT, "w", encoding="utf-8") as handle:
            json.dump(tier, handle, indent=1, sort_keys=True)
print("%d error(s), %d warning(s)" % (len(errors), len(warnings)))
sys.exit(1 if errors else 0)
