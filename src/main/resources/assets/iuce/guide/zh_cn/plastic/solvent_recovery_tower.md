---
title: 溶剂回收塔
category: plastic
order: 133
icon: iuce:solvent_recovery_tower
items: iuce:solvent_recovery_tower
---

从聚乙烯母液中回收溶剂油，同时得到聚乙烯蜡。

## 规格

- 电压：MV，内部缓存 16000 EU
- 物品：输入 0 格，输出 1 格
- 流体：输入 1 槽，输出 1 槽（每槽 8000 mB）
- 升级槽：4 个
- 公用工程：蒸汽，见 [公用工程](plastic/utilities)

## 配方

- **聚乙烯母液** × 900 mB → **聚乙烯蜡** × 1 + **溶剂油** × 880 mB（160 tick / 8 秒，共 5120 EU），需要蒸汽

## 上下游

- 原料来自：[离心机](plastic/centrifuge)
- 产物送往：[助剂反应釜](plastic/additive_reactor)、[催化剂配制罐](plastic/catalyst_prep_tank)、[高混机](plastic/high_speed_mixer)、[聚合釜](plastic/polymerization_reactor)

> [!NOTE]
> 回收的溶剂油接回聚合釜可以大幅减少消耗。

## 合成

{recipe: iuce:solvent_recovery_tower}
