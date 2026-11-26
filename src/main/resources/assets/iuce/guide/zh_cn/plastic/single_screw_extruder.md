---
title: 单螺杆挤出机
category: plastic
order: 190
icon: iuce:single_screw_extruder
items: iuce:single_screw_extruder
---

连续挤出：换模头可出管坯、薄膜泡管、滴灌带或包覆电线。

## 规格

- 电压：MV，内部缓存 16000 EU
- 物品：输入 2 格，输出 1 格
- 流体：输入 0 槽，输出 0 槽
- 升级槽：4 个
- 有催化剂槽
- 公用工程：冷却水、压缩空气，见 [公用工程](plastic/utilities)

## 配方

- **标签 grade/cable_jacket** × 2 + **铜质导线** × 4 → **热包覆导线** × 4（100 tick / 5 秒，共 6400 EU），需要催化剂 包覆模头
- **生物基聚乙烯颗粒** × 2 → **热滴灌带** × 1（100 tick / 5 秒，共 6400 EU），需要催化剂 圆管模头
- **生物基聚乙烯颗粒** × 4 → **热聚乙烯管坯** × 1（100 tick / 5 秒，共 6400 EU），需要冷却水、催化剂 圆管模头
- **硬质聚氯乙烯颗粒** × 4 → **热聚氯乙烯管坯** × 1（100 tick / 5 秒，共 6400 EU），需要冷却水、催化剂 圆管模头
- **生物基聚乙烯颗粒** × 4 → **聚乙烯膜泡** × 1（100 tick / 5 秒，共 6400 EU），需要压缩空气、催化剂 吹膜模头
- **聚乳酸颗粒·薄膜级** × 4 → **聚乳酸膜泡** × 1（100 tick / 5 秒，共 6400 EU），需要压缩空气、催化剂 吹膜模头

## 上下游

- 原料来自：[双螺杆挤出机](plastic/compounding_extruder)、[除湿干燥机](plastic/dehumidifying_dryer)、[水浴切粒机](plastic/strand_pelletizer)
- 产物送往：[冷却水槽](plastic/cooling_tank)、[收卷机](plastic/film_winder)

> [!NOTE]
> 挤出的半成品是热的，需要后续冷却或收卷。

## 合成

{recipe: iuce:single_screw_extruder}
