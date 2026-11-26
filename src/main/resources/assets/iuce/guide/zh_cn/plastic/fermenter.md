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

- **小麦** × 1 + **糖液** × 500 mB → **酶制剂** × 2（200 tick / 10 秒，共 1600 EU），需要催化剂 曲霉菌种
- **糖液** × 1000 mB → **发酵醪** × 1000 mB + **二氧化碳** × 250 mB（200 tick / 10 秒，共 1600 EU），需要冷却水、催化剂 酵母菌种
- **曲霉菌种** × 1 + **糖** × 1 + **标签 any_water** × 250 mB → **曲霉菌种** × 2（200 tick / 10 秒，共 1600 EU），需要催化剂 曲霉菌种
- **乳酸菌种** × 1 + **糖** × 1 + **标签 any_water** × 250 mB → **乳酸菌种** × 2（200 tick / 10 秒，共 1600 EU），需要催化剂 乳酸菌种
- **酵母菌种** × 1 + **糖** × 1 + **标签 any_water** × 250 mB → **酵母菌种** × 2（200 tick / 10 秒，共 1600 EU），需要催化剂 酵母菌种
- **糖液** × 1000 mB → **乳酸发酵液** × 1000 mB（200 tick / 10 秒，共 1600 EU），需要冷却水、催化剂 乳酸菌种

## 上下游

- 原料来自：[糖化罐](plastic/saccharification_tank)、[湿磨机](plastic/wet_mill)
- 产物送往：[凝固槽](plastic/coagulation_tank)、[精馏塔](plastic/distillation_column)、[浓缩罐](plastic/evaporator)、[液化罐](plastic/liquefaction_tank)

> [!NOTE]
> 发酵会放热，需要冷却水。
> 菌种放在催化剂槽，按配方缓慢消耗。
> 副产的二氧化碳可以直接送放空火炬。

## 合成

{recipe: iuce:fermenter}
