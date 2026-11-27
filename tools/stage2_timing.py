"""Serial production-time audit for machine construction with upstream equipment online."""
import json
import math
from collections import Counter, defaultdict
from pathlib import Path
from functools import lru_cache

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/iuce/recipe"

def audit():
    routes = defaultdict(list)
    for path in DATA.rglob("*.json"):
        r = json.loads(path.read_text(encoding="utf8"))
        kind = r["type"]
        ingredients = Counter()
        if "crafting" in kind and "input" in r:
            for row in r["input"]:
                for item in row if isinstance(row,list) else [row]:
                    if item: ingredients[item] += 1
            outputs = [r["result"]]
            time = 1
        elif kind == "iuce:machine":
            for key in ("input","input2"):
                if key in r: ingredients[r[key]] += r.get(key+"_count",1)
            outputs = [r["result"]]
            time = r["time"]
        elif kind == "iuce:process":
            for item in r.get("items",[]):
                ref=item["item"]
                if isinstance(ref,dict):ref=ref["items"]
                ingredients[ref]+=item.get("count",1)
            for fluid in r.get("fluids",[]):ingredients[fluid["fluid"]]+=fluid["amount"]
            outputs = r.get("results",[]) + r.get("fluid_results",[])
            time = r["time"]
        elif kind == "minecraft:smelting":
            ref=r["ingredient"]
            ingredients[ref if isinstance(ref,str) else ref["items"]]+=1
            outputs=[r["result"]]
            time=r.get("cookingtime",200)
        else: continue
        for out in outputs:
            key=out.get("id",out.get("fluid"))
            if key and out.get("chance",1)==1:routes[key].append((out.get("count",out.get("amount",1)),time,ingredients))
    @lru_cache(None)
    def cost(ref,qty,path):
        # Raw materials and utilities are already supplied. Bulk metal stocks are
        # the interface with the stage-one metal line.
        if ref.endswith(("_ingot","_dust","_ore")) or ref.startswith(("minecraft:","#")) or ref not in routes:return 0
        if ref in path:return math.inf
        return min(math.ceil(qty/size)*time+sum(cost(k,n*math.ceil(qty/size),path+(ref,)) for k,n in ins.items())
                   for size,time,ins in routes[ref])
    machines=json.loads((ROOT/"tools/content/stage2.json").read_text(encoding="utf8"))["machines"]
    targets=[m["id"] for m in machines]+list(json.loads((ROOT/"tools/content/plants.json").read_text()))+["waste_heat_power_plant","process_design_table","air_compressor"]
    return {k:cost("iuce:"+k,1,()) for k in targets}

if __name__=="__main__":
    values=audit()
    for name,ticks in sorted(values.items(),key=lambda v:-v[1]):
        print("%-28s %7.2f s" % (name,ticks/20))
    print("%d construction recipes, %d over 600 seconds" % (len(values),sum(t>12000 for t in values.values())))
