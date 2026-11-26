---
title: 闪蒸罐
category: plastic
order: 157
icon: iuce:flash_tank
items: iuce:flash_tank
---

减压闪蒸聚氯乙烯浆料，回收未反应的氯乙烯。

## 规格

- 电压：LV，内部缓存 8000 EU
- 物品：输入 0 格，输出 0 格
- 流体：输入 1 槽，输出 2 槽（每槽 8000 mB）
- 升级槽：4 个
- 公用工程：蒸汽，见 [公用工程](plastic/utilities)

## 配方

- **聚氯乙烯浆料** × 1000 mB → **汽提浆料** × 1000 mB + **氯乙烯气** × 150 mB（100 tick / 5 秒，共 1600 EU），需要蒸汽

## 上下游

- 原料来自：[聚合釜](plastic/polymerization_reactor)
- 产物送往：[离心机](plastic/centrifuge)、[气体压缩机](plastic/gas_compressor)

> [!NOTE]
> 回收的氯乙烯气接回气体压缩机。

## 合成

{recipe: iuce:flash_tank}
