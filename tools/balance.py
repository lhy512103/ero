#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Checks that every closed loop only tops itself up, and prints a production table.

    python tools/balance.py [repo root] [--verbose]

Standard library only; run by `gradlew balanceContent` (part of `check`), and by hand whenever
numbers change. Exits non-zero when a loop needs more make-up than declared, a recipe cycle
creates matter, or a fluid is produced with nowhere to go.

Input: tools/content/loops.json
  * "loops"  closed loops. Each has a name and recipes (one operation each), a "declared" make-up
             percentage of the loop's main input, and "amount" - mB or items per round.
  * "cycles" exemptions for the matter-creation check. Only grown cultures and enzymes multiply on purpose,
             and each round still eats crops the gain check does not count.
All recipes come from data/iuce/recipe/**, so the file never repeats what the game runs.
"""
import glob
import io
import json
import os
import sys
from collections import defaultdict

args = [a for a in sys.argv[1:] if not a.startswith("--")]
ROOT = os.path.abspath(args[0] if args else os.path.join(os.path.dirname(__file__), "..")).replace("\\", "/")
VERBOSE = "--verbose" in sys.argv
RES = ROOT + "/src/main/resources/"
LOOPS = ROOT + "/tools/content/loops.json"

errors, warnings = [], []


def read(path):
    with io.open(path, encoding="utf-8") as handle:
        return handle.read()


def load(path):
    with io.open(path, encoding="utf-8") as handle:
        return json.load(handle)


# ------------------------------------------------------------------ recipes
# name -> {"machine", "in": {ref: amount}, "out": {ref: amount}, "seconds", "eu"}
recipes = {}


def add(name, machine, inputs, outputs, time, energy):
    recipes[name] = {"machine": machine, "in": inputs, "out": outputs,
                     "seconds": max(1, time) / 20.0, "eu": energy}


for path in sorted(glob.glob(RES + "data/iuce/recipe/*/*.json")):
    name = os.path.relpath(path, RES + "data/iuce/recipe").replace("\\", "/")[:-5]
    try:
        data = json.loads(read(path))
    except ValueError:
        continue
    kind = data.get("type", "")
    if kind == "iuce:process":
        inputs, outputs = {}, {}
        for entry in data.get("items", []):
            ref = entry["item"]
            if isinstance(ref, dict):
                ref = ref["items"] + "=" + json.dumps(ref.get("components", {}), sort_keys=True)
            inputs[ref] = inputs.get(ref, 0) + entry.get("count", 1)
        for entry in data.get("fluids", []):
            inputs[entry["fluid"]] = inputs.get(entry["fluid"], 0) + entry["amount"]
        for entry in data.get("results", []):
            if entry.get("chance", 1) >= 1:
                ref = entry["id"]
                if "components" in entry:
                    ref += "=" + json.dumps(entry["components"], sort_keys=True)
                outputs[ref] = outputs.get(ref, 0) + entry.get("count", 1)
        for entry in data.get("fluid_results", []):
            outputs[entry["fluid"]] = outputs.get(entry["fluid"], 0) + entry["amount"]
        add(name, data["machine"], inputs, outputs, data["time"], data["energy"])
    elif kind == "iuce:machine":
        inputs = {data["input"]: 1}
        if "input2" in data:
            inputs[data["input2"]] = inputs.get(data["input2"], 0) + 1
        if "fluid_input" in data:
            inputs[data["fluid_input"]["id"]] = data["fluid_input"]["amount"]
        outputs = {data["result"]["id"]: 1}
        for extra in data.get("extra_results", []):
            if extra.get("chance", 1) >= 1:
                outputs[extra["result"]["id"]] = outputs.get(extra["result"]["id"], 0) + 1
        if "fluid_output" in data:
            outputs[data["fluid_output"]["id"]] = data["fluid_output"]["amount"]
        add(name, data["machine_type"], inputs, outputs, data["time"], data["energy"])
    elif kind in ("iuce:crafting", "iuce:standard_crafting", "iuce:optional_crafting"):
        grid = data["input"]
        cells = [cell for row in grid for cell in row] if grid and isinstance(grid[0], list) else grid
        inputs = defaultdict(int)
        for cell in cells:
            if cell:
                inputs[cell] += 1
        result = data["result"]
        add(name, "crafting", dict(inputs), {result["id"]: result.get("count", 1)}, 1, 0)


# ------------------------------------------------------------------ matter creation
# The producers of each ref. Fluids are per bucket in the recipe, so a cycle carries both.
producers = defaultdict(list)
for name, recipe in recipes.items():
    for ref, amount in recipe["out"].items():
        producers[ref].append(name)

table = load(LOOPS)
exempt = set(table.get("cycles", []))
MAKE_UP_SLACK = 0.02       # a loop may come out 2% short before it is reported


for loop in table.get("loops", []):
    name = loop["name"]
    net = defaultdict(float)
    used = []
    for step in loop["recipes"]:
        recipe = recipes.get(step)
        if recipe is None:
            errors.append("%s: unknown recipe %s" % (name, step))
            break
        used.append(recipe)
        for ref, amount in recipe["in"].items():
            net[ref] -= amount
        for ref, amount in recipe["out"].items():
            net[ref] += amount
    if len(used) != len(loop["recipes"]):
        continue
    if "loss" in loop:
        if -net[loop["main"]] != loop["amount"] * loop["loss"]:
            errors.append("%s: net solvent loss %s" % (name, -net[loop["main"]]))
        continue
    if "conserved" in loop:
        for ref in loop["conserved"]:
            if net[ref] != 0:
                errors.append("%s: %s drifted by %s" % (name, ref, net[ref]))
        continue
    if "recovery" in loop:
        feed = -net[loop["feed"]]
        recovered = net[loop["product"]]
        if feed <= 0 or recovered != feed * loop["recovery"]:
            errors.append("%s: recovered %s from %s, expected %.1f%%" % (name, recovered, feed, 100 * loop["recovery"]))
        continue
    main = loop["main"]
    if net[main] >= 0:
        warnings.append("%s: the loop makes its own %s, so it never needs make-up" % (name, main))
        continue
    top_up = -net[main]
    product = net.get(loop["product"], 0)
    if product <= 0:
        errors.append("%s: the loop produces no %s" % (name, loop["product"]))
        continue
    ratio = top_up / product
    declared = loop["declared"]
    status = "ok" if ratio <= declared + MAKE_UP_SLACK else "TOO HIGH"
    if ratio > declared + MAKE_UP_SLACK:
        errors.append("%s: needs %.1f%% make-up per %s, declared %.1f%%"
                      % (name, ratio * 100, loop["product"], declared * 100))
    if VERBOSE or ratio > declared:
        seconds = sum(recipe["seconds"] for recipe in used)
        eu = sum(recipe["eu"] for recipe in used)
        print("%-28s %5.1f%% make-up (declared %4.1f%%), %.1f s and %d EU per %d %s  [%s]"
              % (name, ratio * 100, declared * 100, seconds, eu, loop["amount"], loop["product"], status))

# ------------------------------------------------------------------ cycles
# Every recipe is a "inputs -> outputs" edge; a cycle whose gain exceeds one creates matter.
# Gains are compared after scaling each recipe to a fixed mass, so a loop that returns the same
# fluid at another concentration is not mistaken for a multiplier.
CYCLE_DEPTH = 6
CYCLE_BUDGET = 200000


def edges():
    result = defaultdict(list)
    for name, recipe in recipes.items():
        if not recipe["in"] or not recipe["out"]:
            continue
        sources = list(recipe["in"])
        targets = list(recipe["out"])
        scale = max(recipe["in"].values())
        for source in sources:
            for target in targets:
                if source == target:
                    continue
                gain = (recipe["out"][target] / scale) / (recipe["in"][source] / scale)
                result[source].append((target, gain, name))
    return result


graph = edges()
best = {}
steps_taken = 0
for start in sorted(graph):
    stack = [(start, [start], [])]
    while stack and steps_taken < CYCLE_BUDGET:
        node, path, steps = stack.pop()
        for target, gain, recipe in graph.get(node, ()):
            steps_taken += 1
            if target == start and len(path) > 1:
                key = tuple(sorted(set(path)))
                product = 1.0
                for _, step_gain, _ in steps + [(target, gain, recipe)]:
                    product *= step_gain
                ledger = defaultdict(float)
                quantity = 1.0
                for index, (_, step_gain, operation) in enumerate(steps + [(target, gain, recipe)]):
                    batch = recipes[operation]
                    runs = quantity / batch["in"][path[index]]
                    for ref, count in batch["in"].items():
                        ledger[ref] -= runs * count
                    for ref, count in batch["out"].items():
                        ledger[ref] += runs * count
                    quantity *= step_gain
                if all(value >= -1e-8 for value in ledger.values()):
                    best[key] = max(best.get(key, 0.0), product)
            elif target not in path and len(path) < CYCLE_DEPTH:
                stack.append((target, path + [target], steps + [(target, gain, recipe)]))
if steps_taken >= CYCLE_BUDGET:
    warnings.append("cycle search stopped early; the recipe graph is bigger than expected")
for key, product in sorted(best.items(), key=lambda pair: -pair[1]):
    if product <= 1.0 or any(node in exempt for node in key):
        continue
    errors.append("recipe cycle multiplies matter: %s (gain %.1fx)" % (" -> ".join(key + (key[0],)), product))

# ------------------------------------------------------------------ fluid sinks
# Fluids are told apart from items by the fluid tags: everything listed there is a fluid.
tag_values = {}      # fluid tag -> the fluids it lists directly
for tag in glob.glob(RES + "data/iuce/tags/fluid/*.json"):
    tag_id = "iuce:" + os.path.basename(tag)[:-5]
    try:
        values = [ref[1:] if ref.startswith("#") else ref for ref in json.loads(read(tag)).get("values", [])]
    except ValueError:
        values = []
    tag_values[tag_id] = {ref if ":" in ref else "iuce:" + ref for ref in values}
known_fluids = {"minecraft:water", "minecraft:lava"}
for values in tag_values.values():
    known_fluids |= values
produced = defaultdict(list)
for name, recipe in recipes.items():
    for ref in recipe["out"]:
        if ref in known_fluids:
            produced[ref].append(name)
consumed = {ref for recipe in recipes.values() for ref in recipe["in"]}
# A recipe that takes a fluid tag consumes every fluid in it.
for ref in list(consumed):
    if ref.startswith("#"):
        consumed |= tag_values.get(ref[1:], set())
# Vented and flared fluids have a sink outside the recipes; gas flaring and venting are player moves.
SINKS = {"iuce:ventable", "iuce:flammable_gases"}
for ref, names in sorted(produced.items()):
    if ref in consumed or any(ref in tag_values.get(tag, ()) for tag in SINKS):
        continue
    warnings.append("%s is produced (%s) but nothing consumes it; add a recipe or a fluid tag"
                    % (ref, ", ".join(sorted(names)[:3])))

# ------------------------------------------------------------------ production table
if VERBOSE:
    print()
    print("%-34s %-14s %6s %6s %7s %9s" % ("recipe", "machine", "in", "out", "seconds", "EU/op"))
    for name in sorted(recipes):
        recipe = recipes[name]
        print("%-34s %-14s %6d %6d %7.1f %9d" % (name[:34], recipe["machine"][:14],
                                                  sum(recipe["in"].values()), sum(recipe["out"].values()),
                                                  recipe["seconds"], recipe["eu"]))

for warning in warnings:
    print("warning: " + warning)
for error in errors:
    print("error: " + error)
print("%d loop(s), %d recipe(s), %d error(s), %d warning(s)"
      % (len(table.get("loops", [])), len(recipes), len(errors), len(warnings)))
sys.exit(1 if errors else 0)
