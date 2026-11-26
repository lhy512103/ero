---
title: 聚合釜
category: plastic
order: 131
icon: iuce:polymerization_reactor
items: iuce:polymerization_reactor
---

把单体聚合成树脂。插入不同工艺卡决定产出牌号：聚乙烯浆液、聚乳酸熔体或聚氯乙烯浆料。

## 规格

- 电压：MV，内部缓存 16000 EU
- 物品：输入 2 格，输出 1 格
- 流体：输入 2 槽，输出 2 槽（每槽 8000 mB）
- 升级槽：4 个
- 有催化剂槽
- 有工艺卡槽：在 [工艺卡](plastic/cards) 页了解如何写卡
- 公用工程：冷却水，见 [公用工程](plastic/utilities)

## 配方

{recipes: polymerization_reactor}

## 上下游

- 原料来自：[助剂反应釜](plastic/additive_reactor)、[催化剂配制罐](plastic/catalyst_prep_tank)、[结晶器](plastic/crystallizer)、[脱水反应器](plastic/dehydration_reactor)、[精馏塔](plastic/distillation_column)、[干燥器](plastic/dryer)、[溶剂回收塔](plastic/solvent_recovery_tower)、[焦油蒸馏釜](plastic/tar_distiller)、[水处理器](plastic/water_treatment)
- 产物送往：[离心机](plastic/centrifuge)、[闪蒸罐](plastic/flash_tank)、[水浴切粒机](plastic/strand_pelletizer)

> [!NOTE]
> 卡槽里的工艺卡决定配方，没卡或卡不对机器不工作。
> 聚合放热，需要冷却水。

## 合成

{recipe: iuce:polymerization_reactor}
