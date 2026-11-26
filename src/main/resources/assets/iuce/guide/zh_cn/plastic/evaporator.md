---
title: 浓缩罐
category: plastic
order: 120
icon: iuce:evaporator
items: iuce:evaporator
---

蒸发浓缩：把烧碱液浓缩成固碱，把乳酸发酵液浓缩成乳酸，回收母液中的乳酸。

## 规格

- 电压：LV，内部缓存 4000 EU
- 物品：输入 0 格，输出 1 格
- 流体：输入 1 槽，输出 2 槽（每槽 8000 mB）
- 升级槽：4 个
- 公用工程：蒸汽，见 [公用工程](plastic/utilities)

## 配方

{recipes: evaporator}

## 上下游

- 原料来自：[结晶器](plastic/crystallizer)、[发酵罐](plastic/fermenter)、[离子膜电解槽](plastic/membrane_cell)
- 产物送往：[助剂反应釜](plastic/additive_reactor)、[低聚釜](plastic/oligomerization_reactor)

> [!NOTE]
> 需要蒸汽。

## 合成

{recipe: iuce:evaporator}
