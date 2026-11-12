---
title: 合金
category: materials
order: 30
icon: iuce:bronze_ingot
items: iuce:bronze_ingot, iuce:alloy_ingot, iuce:steel_ingot, iuce:refined_steel_ingot, iuce:bronze_dust, iuce:bronze_plate, iuce:steel_plate, iuce:coal_ball, iuce:coal_dust
---

所有合金都在 [感应炉](machines/induction_furnace) 里炼，它是中压设备。

## 青铜与黄铜

{flow: minecraft:copper_ingot*3 + iuce:tin_ingot -> @iuce:induction_furnace -> iuce:bronze_ingot*3}

{flow: iuce:copper_dust*3 + iuce:purified_tin_dust -> @iuce:induction_furnace -> iuce:bronze_ingot*4}

用粉炼比用锭多出一锭。黄铜同理，把锡换成锌即可。

- **青铜**：转子（洗矿机、泵）、青铜齿轮、CESU。
- **黄铜**：高级电路板。

## 钢

{flow: iuce:coal_dust*8 -> @iuce:compressor -> iuce:coal_ball}

{flow: minecraft:iron_ingot*4 + iuce:coal_ball -> @iuce:induction_furnace -> iuce:steel_ingot*4}

钢板用来造高级机器外壳和高级电动马达。

## 精炼钢

钢加上纯净镍粉炼成精炼钢，所以要先打通洗矿线。它是 MFSU 和反应堆外壳的材料。

{recipe: iuce:bronze_ingot, iuce:alloy_ingot, iuce:steel_ingot, iuce:refined_steel_ingot}
