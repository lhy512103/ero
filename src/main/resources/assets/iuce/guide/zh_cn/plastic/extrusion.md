---
title: 挤出
category: plastic
order: 8
icon: iuce:pvc_pipe
---

挤出线连续生产管材、薄膜、滴灌带和绝缘电线。核心是单螺杆挤出机，换模头就换产品。

{flow: iuce:rigid_pvc_pellet -> @iuce:single_screw_extruder -> @iuce:cooling_tank -> @iuce:haul_off_cutter -> iuce:pvc_pipe}

## 产品与后续设备

| 产品 | 原料 | 挤出后 |
| --- | --- | --- |
| 聚乙烯管 | 生物基聚乙烯颗粒 | 冷却水槽 → 牵引切割机 |
| 聚氯乙烯管 | 硬质 PVC 颗粒 | 冷却水槽 → 牵引切割机 |
| 滴灌带 | 生物基聚乙烯颗粒 | 冷却水槽 |
| 绝缘电线 | 护套级颗粒 + 铜导线 | 冷却水槽 |
| 薄膜卷 | 聚乙烯或薄膜级聚乳酸颗粒 | 收卷机 |

## 设备

- [单螺杆挤出机](plastic/single_screw_extruder)：放入对应模头，模头由数控加工中心制造。
- [冷却水槽](plastic/cooling_tank)：冷却定型热管坯、热电线和热滴灌带，需要冷却水。
- [牵引切割机](plastic/haul_off_cutter)：把冷却后的管坯切成定长管材。
- [收卷机](plastic/film_winder)：把吹膜泡管压平收卷。

> [!NOTE]
> 现实挤出线也是连续定型、冷却、牵引，再切割或收卷。挤出机产出的半成品是热的，一定要接后续设备。
