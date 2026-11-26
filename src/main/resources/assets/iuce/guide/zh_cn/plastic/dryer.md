---
title: 干燥器
category: plastic
order: 134
icon: iuce:dryer
items: iuce:dryer
---

烘干湿粉、湿树脂和酒糟，也用来活化氧化铝。

## 规格

- 电压：LV，内部缓存 4000 EU
- 物品：输入 1 格，输出 1 格
- 流体：输入 1 槽，输出 1 槽（每槽 8000 mB）
- 升级槽：4 个
- 公用工程：蒸汽，见 [公用工程](plastic/utilities)

## 配方

- **纯净铝粉** × 2 + **标签 any_water** × 500 mB → **活性氧化铝** × 1（200 tick / 10 秒，共 3200 EU），需要蒸汽
- **酒糟液** × 1000 mB → **干酒糟** × 2（100 tick / 5 秒，共 1600 EU），需要蒸汽
- **湿聚乙烯粉** × 4 → **聚乙烯粉** × 4 + **溶剂油** × 100 mB（100 tick / 5 秒，共 1600 EU），需要蒸汽
- **湿聚氯乙烯** × 4 → **聚氯乙烯树脂** × 4（100 tick / 5 秒，共 1600 EU），需要蒸汽
- **焦粉** × 1 → **蒸汽处理焦粉** × 1（100 tick / 5 秒，共 1600 EU），需要蒸汽

## 上下游

- 原料来自：[离心机](plastic/centrifuge)、[精馏塔](plastic/distillation_column)
- 产物送往：[催化剂配制罐](plastic/catalyst_prep_tank)、[双螺杆挤出机](plastic/compounding_extruder)、[高混机](plastic/high_speed_mixer)、[聚合釜](plastic/polymerization_reactor)

> [!NOTE]
> 需要蒸汽。

## 合成

{recipe: iuce:dryer}
