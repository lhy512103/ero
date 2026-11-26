---
title: 离子膜电解槽
category: plastic
order: 152
icon: iuce:membrane_cell
items: iuce:membrane_cell
---

电解精盐水，同时产出氯气、氢气和烧碱液，是氯碱工业的核心。

## 规格

- 电压：MV，内部缓存 16000 EU
- 物品：输入 0 格，输出 0 格
- 流体：输入 1 槽，输出 3 槽（每槽 8000 mB）
- 升级槽：4 个

## 配方

{recipes: membrane_cell}

## 上下游

- 原料来自：[精制槽](plastic/brine_purifier)、[中和槽](plastic/neutralizer)
- 产物送往：[助剂反应釜](plastic/additive_reactor)、[催化剂配制罐](plastic/catalyst_prep_tank)、[浓缩罐](plastic/evaporator)、[氯化氢合成炉](plastic/hcl_furnace)、[中和槽](plastic/neutralizer)、[洗涤塔](plastic/scrubber)

> [!NOTE]
> 三种产物都要接出去，任一槽满都会停机。

## 合成

{recipe: iuce:membrane_cell}
