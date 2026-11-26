---
title: 精馏塔
category: plastic
order: 114
icon: iuce:distillation_column
items: iuce:distillation_column
---

按沸点分离混合流体：把发酵液分成乙醇和酒糟，也负责氯乙烯的两段精馏。

## 规格

- 电压：LV，内部缓存 8000 EU
- 物品：输入 0 格，输出 1 格
- 流体：输入 1 槽，输出 2 槽（每槽 8000 mB）
- 升级槽：4 个
- 公用工程：蒸汽，见 [公用工程](plastic/utilities)

## 配方

- **发酵醪** × 1000 mB → **乙醇** × 500 mB + **酒糟液** × 500 mB（160 tick / 8 秒，共 3840 EU），需要蒸汽
- **液化氯乙烯** × 1000 mB → **脱轻氯乙烯** × 900 mB + **乙炔** × 100 mB（160 tick / 8 秒，共 3840 EU），需要蒸汽
- **脱轻氯乙烯** × 1000 mB → **重残渣** × 1 + **氯乙烯** × 950 mB（160 tick / 8 秒，共 3840 EU），需要蒸汽

## 上下游

- 原料来自：[发酵罐](plastic/fermenter)、[气体压缩机](plastic/gas_compressor)
- 产物送往：[助剂反应釜](plastic/additive_reactor)、[催化剂配制罐](plastic/catalyst_prep_tank)、[脱水反应器](plastic/dehydration_reactor)、[干燥器](plastic/dryer)、[聚合釜](plastic/polymerization_reactor)、[氯乙烯转化器](plastic/vcm_converter)

> [!NOTE]
> 精馏需要蒸汽供热。
> 两个流体输出槽分别出轻、重组分，记得都接管道。

## 合成

{recipe: iuce:distillation_column}
