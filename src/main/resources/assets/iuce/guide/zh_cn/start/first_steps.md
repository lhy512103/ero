---
title: 第一步
category: start
order: 10
icon: iuce:forge_hammer
---

开局只需要铁、铜、锡和一棵橡胶树。目标是造出第一台 {item:iuce:generator} 和几台低压机器。

## 1. 做工具

{recipe: iuce:forge_hammer, iuce:cutter, iuce:treetap}

{item:iuce:forge_hammer} 把锭敲成板和棍，{item:iuce:cutter} 把板剪成导线，{item:iuce:treetap} 用来采胶。工具在合成时只掉耐久，不会消失。

## 2. 板、导线、橡胶

{flow: minecraft:iron_ingot*2 + iuce:forge_hammer -> iuce:iron_plate}

{flow: iuce:copper_plate + iuce:cutter -> iuce:copper_cable*2 -> iuce:insulated_copper_cable*2}

裸导线加一份 {item:iuce:rubber} 就是绝缘导线。橡胶的来源见 [橡胶](start/rubber)。

## 3. 电路板与机器外壳

{recipe: iuce:circuit, iuce:machine_casing}

> [!TIP]
> 电路板在 [工业工作台](machines/industrial_workbench) 里用同样的摆法能出 2 个，尽早造一台。

## 4. 电池与发电机

{recipe: iuce:re_battery, iuce:generator}

发电机烧煤等燃料发电。把机器紧贴发电机，或者用导线连起来，电就通了，详见 [电力基础](power/basics)。

## 5. 第一批机器

机器还需要 [机械零件](start/components)：电动马达、铁齿轮、加热线圈。

{cards: machines/macerator, machines/electric_furnace, machines/metal_former, machines/compressor, machines/extractor, machines/industrial_workbench}
