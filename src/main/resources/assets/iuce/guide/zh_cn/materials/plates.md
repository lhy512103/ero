---
title: 板材与双层板
category: materials
order: 40
icon: iuce:iron_plate
items: iuce:tin_plate, iuce:iron_plate, iuce:zinc_plate, iuce:aluminium_plate, iuce:cobalt_plate, iuce:lead_plate, iuce:nickel_plate, iuce:silver_plate, iuce:gold_plate, iuce:copper_plate, iuce:dense_tin_plate, iuce:dense_iron_plate, iuce:dense_gold_plate, iuce:dense_copper_plate, iuce:dense_bronze_plate, iuce:dense_steel_plate, iuce:dense_lead_plate, iuce:dense_lapis_plate, iuce:dense_obsidian_plate, iuce:aluminium_stick, iuce:cobalt_stick, iuce:nickel_stick, iuce:silver_stick, iuce:zinc_stick
---

## 板

{flow: minecraft:iron_ingot -> @iuce:metal_former -> iuce:iron_plate}

[金属成型机](machines/metal_former) 的辊压模式 1 锭出 1 板；用锤子要 2 锭。

## 金属棍

{flow: iuce:nickel_ingot -> @iuce:metal_former -> iuce:nickel_stick*2}

挤压模式 1 锭出 2 根棍；用锤子 1 锭 1 根。

## 双层板

{flow: iuce:copper_plate*9 -> @iuce:compressor -> iuce:dense_copper_plate}

9 块板在 [压缩机](machines/compressor) 里压成 1 块双层板。双层板是各级机器的门槛：

| 双层板 | 用在哪里 |
| --- | --- |
| {item:iuce:dense_copper_plate} | 感应炉 |
| {item:iuce:dense_tin_plate} | 地热发电机 |
| {item:iuce:dense_iron_plate} | 高级机器外壳 |
| {item:iuce:dense_bronze_plate} | CESU |
| {item:iuce:dense_gold_plate} | 电解机 |
| {item:iuce:dense_lapis_plate} | 兰波顿水晶 |
| {item:iuce:dense_lead_plate} | 反应堆外壳 |
| {item:iuce:dense_steel_plate} | 裂变控制器 |
