"""Build integrated plant batches from the same on-disk recipes that feed small machines."""
import json
from collections import Counter
from pathlib import Path

def compile_plants(root):
    root = Path(root)
    recipes = root / "src/main/resources/data/iuce/recipe"
    result = {}
    for plant, routes in json.loads((root / "tools/content/plants.json").read_text(encoding="utf8")).items():
        batches = []
        for name, definition in routes.items():
            items, fluids, tools = Counter(), Counter(), {}
            energy = duration = 0
            for step, count in definition["steps"]:
                recipe = json.loads((recipes / (step + ".json")).read_text(encoding="utf8"))
                if recipe["type"] == "iuce:machine":
                    items[recipe["input"]] -= count * recipe.get("input_count", 1)
                    items[recipe["result"]["id"]] += count * recipe["result"].get("count", 1)
                else:
                    for entry in recipe.get("items", []):
                        ref = entry["item"]
                        if isinstance(ref, dict):
                            ref = ref["items"] + str(sorted(ref["components"].items()))
                        items[ref] -= count * entry.get("count", 1)
                    for entry in recipe.get("results", []):
                        ref = entry["id"]
                        if entry.get("components"):
                            ref += str(sorted(entry["components"].items()))
                        items[ref] += count * entry.get("count", 1)
                    for sign, key in [(-1,"fluids"),(1,"fluid_results")]:
                        for entry in recipe.get(key, []):
                            fluids[entry["fluid"]] += sign * count * entry["amount"]
                    if "catalyst" in recipe:
                        catalyst = recipe["catalyst"]
                        ref = catalyst["item"]
                        tools[ref] = tools.get(ref,0) + count * catalyst.get("damage",0)
                energy += count * recipe["energy"]
                duration += count * recipe["time"]
            # A coloring run preserves its carrier. Record both sides of that transfer.
            feed = definition.get("carrier")
            inputs = {k:-v for k,v in items.items() if v<0}
            outputs = {k:v for k,v in items.items() if v>0}
            if feed:
                inputs[feed] = outputs[feed] = 25
            batches.append(dict(id=name, inputs=inputs, outputs=outputs,
                fluids={k:-v for k,v in fluids.items() if v<0}, products={k:v for k,v in fluids.items() if v>0},
                tools=tools, energy=energy//2, time=duration, segments=definition.get("segments",[])))
        result[plant] = batches
    return result
