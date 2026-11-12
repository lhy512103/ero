#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Prints every IUCE recipe on one line, grouped by folder. Handy for reviewing balance.

    python tools/dump_recipes.py [recipe dir]
"""
import json,sys,os,glob
root=sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "data", "iuce", "recipe")
for folder in sorted(os.listdir(root)):
    print("=="+folder)
    for f in sorted(glob.glob(os.path.join(root,folder,"*.json"))):
        d=json.load(open(f,encoding="utf-8"))
        n=os.path.basename(f)[:-5]
        t=d.get("type","").replace("iuce:","").replace("minecraft:","mc:")
        def s(x): return json.dumps(x,ensure_ascii=False,separators=(",",":")).replace("iuce:","").replace("minecraft:","mc:").replace('"','')
        if t=="machine":
            r=d["result"]; 
            print(n,"|",d.get("input_count",1),s(d["input"]),("+ %s %s"%(d.get("input2_count",1),s(d["input2"])) if "input2" in d else ""),"->",s(r),s(d.get("extra_results","")),"E",d["energy"],"T",d["time"])
        elif "input" in d:
            print(n,"|",t,s(d["input"]),"->",s(d["result"]))
        else:
            print(n,"|",s(d))
