---
title: 水浴切粒机
category: plastic
order: 171
icon: iuce:strand_pelletizer
items: iuce:strand_pelletizer
---

在水浴中冷却料条并切成颗粒，颗粒种类由料条记录的产物决定。

## 规格

- 电压：LV，内部缓存 4000 EU
- 物品：输入 1 格，输出 1 格
- 流体：输入 1 槽，输出 0 槽（每槽 8000 mB）
- 升级槽：4 个
- 公用工程：冷却水，见 [公用工程](plastic/utilities)

## 配方

- **料条** × 4 → **生物基聚乙烯颗粒** × 4（80 tick / 4 秒，共 1280 EU），需要冷却水
- **料条** × 4 → **色母** × 4（80 tick / 4 秒，共 1280 EU），需要冷却水
- **料条** × 4 → **填充聚乙烯颗粒** × 4（80 tick / 4 秒，共 1280 EU），需要冷却水
- **聚乳酸熔体·薄膜级** × 1000 mB → **聚乳酸颗粒·薄膜级** × 4（80 tick / 4 秒，共 1280 EU），需要冷却水
- **聚乳酸熔体·注塑级** × 1000 mB → **聚乳酸颗粒·注塑级** × 4（80 tick / 4 秒，共 1280 EU），需要冷却水
- **料条** × 4 → **硬质聚氯乙烯颗粒** × 4（80 tick / 4 秒，共 1280 EU），需要冷却水
- **料条** × 4 → **软质聚氯乙烯颗粒** × 4（80 tick / 4 秒，共 1280 EU），需要冷却水

## 上下游

- 原料来自：[双螺杆挤出机](plastic/compounding_extruder)、[聚合釜](plastic/polymerization_reactor)
- 产物送往：[双螺杆挤出机](plastic/compounding_extruder)、[除湿干燥机](plastic/dehumidifying_dryer)、[注塑机](plastic/injection_molder)、[单螺杆挤出机](plastic/single_screw_extruder)

> [!NOTE]
> 需要冷却水。

## 合成

{recipe: iuce:strand_pelletizer}
