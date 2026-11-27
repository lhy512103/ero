---
title: 发酵罐
category: plastic
order: 113
icon: iuce:fermenter
items: iuce:fermenter
---

整条生物链的心脏：用酵母把糖液发酵成乙醇发酵液，也用来扩培酵母、乳酸菌和曲霉。

## 规格

- 电压：LV，内部缓存 4000 EU
- 物品：输入 2 格，输出 1 格
- 流体：输入 2 槽，输出 2 槽（每槽 8000 mB）
- 升级槽：4 个
- 有催化剂槽
- 公用工程：冷却水，见 [公用工程](plastic/utilities)

## 配方

{recipes: fermenter}

## 上下游

- 原料来自：[糖化罐](plastic/saccharification_tank)、[湿磨机](plastic/wet_mill)
- 产物送往：[凝固槽](plastic/coagulation_tank)、[精馏塔](plastic/distillation_column)、[浓缩罐](plastic/evaporator)、[液化罐](plastic/liquefaction_tank)

> [!NOTE]
> 发酵会放热，需要冷却水。
> 菌种放在催化剂槽，按配方缓慢消耗。
> 副产的二氧化碳可以直接送放空火炬。

## 合成

{recipe: iuce:fermenter}
