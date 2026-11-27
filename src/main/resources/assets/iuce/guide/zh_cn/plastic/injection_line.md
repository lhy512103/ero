---
title: 注塑与模具
category: plastic
order: 7
icon: iuce:brick_mold
---

注塑把注塑级颗粒做成周转箱、积木、桶、灯罩、机壳和滴头等零件。

{flow: iuce:pla_injection_pellet -> @iuce:dehumidifying_dryer -> @iuce:injection_molder -> iuce:toy_brick}

## 准备

1. **模具**：[数控加工中心](plastic/cnc_machining_center) 用工具钢锭加工模坯，再加工成具体模具。模具不消耗。
2. **颗粒**：注塑级颗粒（标签 grade/injection），聚乳酸需先除湿干燥。
3. **公用工程**：压缩空气和冷却水，见 [公用工程](plastic/utilities)。

## 生产

- [注塑机](plastic/injection_molder)（MV）放入模具后，按模具决定产出零件，每次同时产出 1 份水口料。
- 水口料送 [塑料粉碎机](plastic/plastic_granulator) 回收成颗粒，接回注塑机，几乎不浪费材料。
- 不同零件消耗的颗粒数不同：滴头、灯罩用料少，周转箱、机壳和桶身用料多。

> [!NOTE]
> 工业注塑把熔体高压注入闭合模腔，保压冷却后开模顶出，浇道里凝固的部分就是水口料。
