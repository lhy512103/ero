"""Render one guide page per process machine from its registered layout and recipes."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]/"src/main/resources"
GUIDE=ROOT/"assets/iuce/guide/zh_cn/plastic"
LANG=json.loads((ROOT/"assets/iuce/lang/zh_cn.json").read_text(encoding="utf8"))

def label(ref):
    if isinstance(ref,dict): ref=ref["items"]
    if ref.startswith("#"):return "标签 "+ref.split(":")[-1]
    if ref=="minecraft:water":return "水"
    for kind in ("item","block","fluid_type"):
        translated=LANG.get(kind+"."+ref.replace(":","."))
        if translated:return translated
    return ref

def quantity(entry):
    ref=entry.get("fluid",entry.get("item",entry.get("id")))
    return label(ref)+" × "+str(entry.get("amount",entry.get("count",1)))+(" mB" if "fluid" in entry else "")

def main():
    GUIDE.mkdir(exist_ok=True)
    for filename in sorted((ROOT/"iuce/machines").glob("*.json")):
        definition=json.loads(filename.read_text(encoding="utf8"))
        if "id" not in definition:continue
        name=definition["id"]
        content=[f"---\ntitle: {label('iuce:'+name)}\ncategory: plastic\norder: 100\nicon: iuce:{name}\nitems: iuce:{name}\n---",
            f"{definition['voltage']} 设备，物品输入 {definition['items_in']} 格、输出 {definition['items_out']} 格，流体输入 {len(definition['fluids_in'])} 槽、输出 {len(definition['fluids_out'])} 槽。"]
        for path in sorted((ROOT/"data/iuce/recipe"/name).glob("*.json")):
            r=json.loads(path.read_text(encoding="utf8"))
            ins=r.get("items",[])+r.get("fluids",[])
            outs=r.get("results",[])+r.get("fluid_results",[])
            line="、".join(map(quantity,ins))+" → "+("、".join(map(quantity,outs)) or "排放")
            needs=[LANG.get("gui.iuce.utility."+u,u) for u in r.get("utilities",[])]
            if "catalyst" in r:needs.append(label(r["catalyst"]["item"]))
            if "card" in r:needs.append("工艺卡 "+r["card"]["family"]+" "+str(r["card"]["options"]))
            content.append(line+"。"+str(r["time"])+" tick，"+str(r["energy"])+" EU。"+("需要"+"、".join(needs)+"。" if needs else ""))
        content.append("{recipe: iuce:"+name+"}")
        (GUIDE/(name+".md")).write_text("\n\n".join(content)+"\n",encoding="utf8")
if __name__=="__main__":main()
