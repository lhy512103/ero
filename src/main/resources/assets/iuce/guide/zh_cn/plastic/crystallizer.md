---
title: 结晶器
category: plastic
order: 123
icon: iuce:crystallizer
items: iuce:crystallizer
---

冷却结晶提纯丙交酯，母液送回浓缩罐回收乳酸。

## 规格

- 电压：LV，内部缓存 4000 EU
- 物品：输入 0 格，输出 1 格
- 流体：输入 1 槽，输出 1 槽（每槽 8000 mB）
- 升级槽：4 个
- 公用工程：冷却水，见 [公用工程](plastic/utilities)

## 配方

- **粗丙交酯** × 1000 mB → **丙交酯** × 4 + **丙交酯母液** × 250 mB（200 tick / 10 秒，共 3200 EU），需要冷却水

## 上下游

- 原料来自：[解聚釜](plastic/depolymerization_reactor)
- 产物送往：[浓缩罐](plastic/evaporator)、[聚合釜](plastic/polymerization_reactor)

> [!NOTE]
> 需要冷却水。

## 合成

{recipe: iuce:crystallizer}
