---
title: 低聚釜
category: plastic
order: 121
icon: iuce:oligomerization_reactor
items: iuce:oligomerization_reactor
---

让乳酸缩合成聚乳酸低聚物，同时脱去水。

## 规格

- 电压：LV，内部缓存 4000 EU
- 物品：输入 0 格，输出 0 格
- 流体：输入 1 槽，输出 2 槽（每槽 8000 mB）
- 升级槽：4 个
- 公用工程：蒸汽，见 [公用工程](plastic/utilities)

## 配方

- **乳酸** × 500 mB → **乳酸低聚物** × 500 mB + **工艺水** × 100 mB（160 tick / 8 秒，共 3840 EU），需要蒸汽

## 上下游

- 原料来自：[浓缩罐](plastic/evaporator)
- 产物送往：[解聚釜](plastic/depolymerization_reactor)

## 合成

{recipe: iuce:oligomerization_reactor}
