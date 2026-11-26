---
title: 焦油蒸馏釜
category: plastic
order: 141
icon: iuce:tar_distiller
items: iuce:tar_distiller
---

把煤焦油分馏成萘、沥青和溶剂油。

## 规格

- 电压：LV，内部缓存 8000 EU
- 物品：输入 0 格，输出 2 格
- 流体：输入 1 槽，输出 1 槽（每槽 8000 mB）
- 升级槽：4 个
- 公用工程：蒸汽，见 [公用工程](plastic/utilities)

## 配方

- **煤焦油** × 1000 mB → **萘** × 1 + **沥青** × 2 + **溶剂油** × 500 mB（200 tick / 10 秒，共 4800 EU），需要蒸汽

## 上下游

- 原料来自：[炼焦炉](plastic/coking_oven)
- 产物送往：[助剂反应釜](plastic/additive_reactor)、[催化剂配制罐](plastic/catalyst_prep_tank)、[聚合釜](plastic/polymerization_reactor)

> [!NOTE]
> 萘用于增塑剂，沥青用于碳素电极。

## 合成

{recipe: iuce:tar_distiller}
