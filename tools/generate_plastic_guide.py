"""Render one detailed guide page per process machine from its layout, recipes and the role text below."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]/"src/main/resources"
GUIDE=ROOT/"assets/iuce/guide/zh_cn/plastic"
LANG=json.loads((ROOT/"assets/iuce/lang/zh_cn.json").read_text(encoding="utf8"))
UTIL={"steam":"蒸汽","cooling_water":"冷却水","air":"压缩空气"}

# page order follows the production flow; each entry: (order, role, tips)
ROLE={
 "wet_mill":(110,"生物路线的起点。把甘蔗、甜菜等糖料作物压榨成糖液，把土豆、小麦等淀粉作物磨成淀粉浆。",["糖料作物的蔗渣可以当燃料或堆肥。","淀粉浆还要经过液化罐和糖化罐才能变成糖液。"]),
 "liquefaction_tank":(111,"用酶制剂把淀粉浆中的长链淀粉切断，得到液化醪。",["酶制剂由发酵罐用曲霉培养物制得。"]),
 "saccharification_tank":(112,"把液化醪进一步分解成可发酵的糖液。",["催化剂槽放入酶，配方不消耗催化剂本体。"]),
 "fermenter":(113,"整条生物链的心脏：用酵母把糖液发酵成乙醇发酵液，也用来扩培酵母、乳酸菌和曲霉。",["发酵会放热，需要冷却水。","菌种放在催化剂槽，按配方缓慢消耗。","副产的二氧化碳可以直接送放空火炬。"]),
 "distillation_column":(114,"按沸点分离混合流体：把发酵液分成乙醇和酒糟，也负责氯乙烯的两段精馏。",["精馏需要蒸汽供热。","两个流体输出槽分别出轻、重组分，记得都接管道。"]),
 "dehydration_reactor":(115,"在催化剂作用下把乙醇脱水成乙烯，是生物基聚乙烯的单体来源。",["MV 设备，注意供电电压。","副产工艺水可送水处理器回用。"]),
 "evaporator":(120,"蒸发浓缩：把烧碱液浓缩成固碱，把乳酸发酵液浓缩成乳酸，回收母液中的乳酸。",["需要蒸汽。"]),
 "oligomerization_reactor":(121,"让乳酸缩合成聚乳酸低聚物，同时脱去水。",[]),
 "depolymerization_reactor":(122,"在高温减压下把低聚物解聚成粗丙交酯。",[]),
 "crystallizer":(123,"冷却结晶提纯丙交酯，母液送回浓缩罐回收乳酸。",["需要冷却水。"]),
 "catalyst_prep_tank":(130,"配制各类催化剂：活性氧化铝、无汞催化剂、聚烯烃催化剂和锡催化剂。",["一份催化剂可供多批配方使用，提前多备几份。"]),
 "polymerization_reactor":(131,"把单体聚合成树脂。插入不同工艺卡决定产出牌号：聚乙烯浆液、聚乳酸熔体或聚氯乙烯浆料。",["卡槽里的工艺卡决定配方，没卡或卡不对机器不工作。","聚合放热，需要冷却水。"]),
 "centrifuge":(132,"固液分离，把浆液甩成湿粉和母液。",[]),
 "solvent_recovery_tower":(133,"从聚乙烯母液中回收溶剂油，同时得到聚乙烯蜡。",["回收的溶剂油接回聚合釜可以大幅减少消耗。"]),
 "dryer":(134,"烘干湿粉、湿树脂和酒糟，也用来活化氧化铝。",["需要蒸汽。"]),
 "coking_oven":(140,"隔绝空气干馏煤，得到焦炭、焦炉气和煤焦油，是煤化工的入口。",["焦炉气经洗涤塔脱硫后成为净焦炉气。","成规模生产请搭炼焦炉组。"]),
 "tar_distiller":(141,"把煤焦油分馏成萘、沥青和溶剂油。",["萘用于增塑剂，沥青用于碳素电极。"]),
 "lime_kiln":(142,"煅烧石灰岩得到生石灰，也能把电石渣回烧成生石灰。",["副产二氧化碳。"]),
 "carbide_furnace":(143,"用碳素电极把生石灰和焦炭熔炼成电石块。",["大量生产请搭电石联合炉。"]),
 "acetylene_generator":(144,"电石加水生成粗乙炔，同时排出电石渣浆。",["渣浆送压滤机，滤出的电石渣回石灰窑。"]),
 "filter_press":(145,"压滤电石渣浆，得到电石渣和工艺水。",[]),
 "brine_dissolver":(150,"用水溶解岩盐得到粗盐水。",[]),
 "brine_purifier":(151,"用生石灰除去粗盐水中的杂质，得到可电解的精盐水。",[]),
 "membrane_cell":(152,"电解精盐水，同时产出氯气、氢气和烧碱液，是氯碱工业的核心。",["三种产物都要接出去，任一槽满都会停机。"]),
 "hcl_furnace":(153,"氯气在氢气中燃烧合成氯化氢。",["按等量供给氯和氢。"]),
 "vcm_converter":(154,"乙炔与氯化氢在无汞催化剂上加成，生成粗氯乙烯。",[]),
 "scrubber":(155,"用烧碱液洗去气体中的酸性杂质：净化乙炔、焦炉气和粗氯乙烯。",["废碱液送中和槽。"]),
 "gas_compressor":(156,"把氯乙烯气压缩液化，送往精馏。",[]),
 "flash_tank":(157,"减压闪蒸聚氯乙烯浆料，回收未反应的氯乙烯。",["回收的氯乙烯气接回气体压缩机。"]),
 "neutralizer":(158,"用氯化氢中和烧碱液和废碱液，得到盐水回用。",[]),
 "water_treatment":(159,"把任意水净化成去离子水，供聚合使用。",[]),
 "additive_reactor":(160,"合成塑料助剂：分散剂、热稳定剂、引发剂和增塑剂。",[]),
 "high_speed_mixer":(161,"把聚氯乙烯树脂与助剂高速混合成硬质或软质干混料。",[]),
 "compounding_extruder":(170,"单机版双螺杆挤出：把粉料、干混料熔融塑化成料条，也负责配色和填充改性。",["成规模改性请搭联合双螺杆挤出机。"]),
 "strand_pelletizer":(171,"在水浴中冷却料条并切成颗粒，颗粒种类由料条记录的产物决定。",["需要冷却水。"]),
 "dehumidifying_dryer":(172,"除去聚乳酸颗粒吸收的水分。聚乳酸受潮会在注塑、吹膜时降解。",["注塑和吹膜前都要先干燥。"]),
 "cnc_machining_center":(180,"用工具钢锭加工各种注塑模具与挤出模头。",["模具不消耗，一套可以一直用。"]),
 "injection_molder":(181,"把注塑级颗粒注入模腔，做出周转箱、积木、桶、机壳等制品，同时产出水口料。",["需要压缩空气和冷却水。","水口料送塑料粉碎机回收成颗粒。"]),
 "plastic_granulator":(182,"把水口料粉碎回收成颗粒。",[]),
 "single_screw_extruder":(190,"连续挤出：换模头可出管坯、薄膜泡管、滴灌带或包覆电线。",["挤出的半成品是热的，需要后续冷却或收卷。"]),
 "cooling_tank":(191,"冷却定型热管坯、热电线和热滴灌带。",["需要冷却水。"]),
 "haul_off_cutter":(192,"牵引冷却后的管坯并定长切割成管材。",[]),
 "film_winder":(193,"把吹膜泡管压平收卷成薄膜卷。",[]),
 "coagulation_tank":(200,"用乳酸发酵液让生乳胶凝固成凝块。",[]),
 "sheeting_press":(201,"把凝块压成胶片，挤出工艺水。",[]),
 "smokehouse":(202,"用原木烟熏干燥胶片，得到天然橡胶。",[]),
 "rubber_mixer":(203,"把橡胶、硫磺和焦粉密炼成混炼胶。",[]),
 "vulcanizer":(204,"把混炼胶硫化成传送带、密封件等橡胶制品。",["需要蒸汽。"]),
 "vent_stack":(210,"安全排放：烧掉可燃气体、放空无用气体，避免储罐憋满导致停机。",["把各装置的副产气汇到这里，产线就不会被废气卡住。"]),
}

def label(ref):
    if isinstance(ref,dict): ref=ref["items"]
    if ref.startswith("#"):return "标签 "+ref.split(":")[-1]
    vanilla={"minecraft:water":"水","minecraft:wheat":"小麦","minecraft:sugar":"糖","minecraft:coal":"煤炭","minecraft:sugar_cane":"甘蔗","minecraft:potato":"马铃薯","minecraft:beetroot":"甜菜根"}
    if ref in vanilla:return vanilla[ref]
    if ref.startswith("minecraft:"):return "{item:"+ref+"}"
    for kind in ("item","block","fluid_type"):
        translated=LANG.get(kind+"."+ref.replace(":","."))
        if translated:return translated
    return ref

def ref(entry):
    r=entry.get("fluid",entry.get("item",entry.get("id",entry.get("tag",""))))
    return r["items"] if isinstance(r,dict) else r

def quantity(entry):
    return "**"+label(ref(entry))+"** × "+str(entry.get("amount",entry.get("count",1)))+(" mB" if "fluid" in entry else "")

def recipes(name):
    return [json.loads(p.read_text(encoding="utf8")) for p in sorted((ROOT/"data/iuce/recipe"/name).glob("*.json"))]

def main():
    defs={}
    for f in sorted((ROOT/"iuce/machines").glob("*.json")):
        d=json.loads(f.read_text(encoding="utf8"))
        if "id" in d: defs[d["id"]]=d
    makes,uses={},{}
    for name in defs:
        for r in recipes(name):
            for e in r.get("results",[])+r.get("fluid_results",[]): makes.setdefault(ref(e),set()).add(name)
            for e in r.get("items",[])+r.get("fluids",[]): uses.setdefault(ref(e),set()).add(name)
    link=lambda m:f"[{label('iuce:'+m)}](plastic/{m})"
    for name,d in defs.items():
        order,role,tips=ROLE.get(name,(300,"",[]))
        rs=recipes(name)
        utils=sorted({UTIL.get(u,u) for r in rs for u in r.get("utilities",[])})
        out=[f"---\ntitle: {label('iuce:'+name)}\ncategory: plastic\norder: {order}\nicon: iuce:{name}\nitems: iuce:{name}\n---",role]
        spec=[f"- 电压：{d['voltage']}，内部缓存 {d.get('buffer',0)} EU",
              f"- 物品：输入 {d['items_in']} 格，输出 {d['items_out']} 格",
              f"- 流体：输入 {len(d['fluids_in'])} 槽，输出 {len(d['fluids_out'])} 槽"+(f"（每槽 {max(d['fluids_in']+d['fluids_out'])} mB）" if d['fluids_in']+d['fluids_out'] else ""),
              f"- 升级槽：{d.get('upgrades',0)} 个"]
        if d.get("catalyst_slot"):spec.append("- 有催化剂槽")
        if d.get("card_slot"):spec.append("- 有工艺卡槽：在 [工艺卡](plastic/cards) 页了解如何写卡")
        if utils:spec.append("- 公用工程："+"、".join(utils)+"，见 [公用工程](plastic/utilities)")
        out.append("## 规格\n\n"+"\n".join(spec))
        lines=[]
        for r in rs:
            ins=r.get("items",[])+r.get("fluids",[]);outs=r.get("results",[])+r.get("fluid_results",[])
            line="- "+(" + ".join(map(quantity,ins)) or "无")+" → "+(" + ".join(map(quantity,outs)) or "排放")
            line+=f"（{r['time']} tick / {r['time']/20:g} 秒，共 {r['energy']} EU）"
            needs=[UTIL.get(u,u) for u in r.get("utilities",[])]
            if "catalyst" in r:needs.append("催化剂 "+label(r["catalyst"]["item"]))
            if "card" in r:needs.append("工艺卡 "+str(r["card"].get("family","")))
            if needs:line+="，需要"+"、".join(needs)
            lines.append(line)
        if lines:out.append("## 配方\n\n{recipes: "+name+"}")
        up=sorted({m for r in rs for e in r.get("items",[])+r.get("fluids",[]) for m in makes.get(ref(e),()) if m!=name})
        down=sorted({m for r in rs for e in r.get("results",[])+r.get("fluid_results",[]) for m in uses.get(ref(e),()) if m!=name})
        flow=[]
        if up:flow.append("- 原料来自："+"、".join(map(link,up)))
        if down:flow.append("- 产物送往："+"、".join(map(link,down)))
        if flow:out.append("## 上下游\n\n"+"\n".join(flow))
        if tips:out.append("> [!NOTE]\n> "+"\n> ".join(tips))
        out.append("## 合成\n\n{recipe: iuce:"+name+"}")
        (GUIDE/(name+".md")).write_text("\n\n".join(x for x in out if x)+"\n",encoding="utf8")
if __name__=="__main__":main()
