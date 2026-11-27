---
title: 生物路线
category: plastic
order: 1
icon: iuce:wet_mill
---

生物路线把作物变成糖液，再发酵成乙醇、脱水成乙烯。它同时也是菌种和酶的来源，聚乳酸和天然橡胶都离不开它。

{flow: minecraft:sugar_cane -> @iuce:wet_mill -> ~iuce:sugar_solution*1000 -> @iuce:fermenter -> @iuce:distillation_column -> @iuce:dehydration_reactor -> ~iuce:ethylene*500}

## 1. 制糖液

- **糖料作物**（甘蔗等）：[湿磨机](plastic/wet_mill) 直接榨出糖液，副产蔗渣。
- **淀粉作物**（马铃薯、小麦等）：湿磨机先出淀粉浆，经 [液化罐](plastic/liquefaction_tank) 加酶液化，再由 [糖化罐](plastic/saccharification_tank) 变成糖液。
- 糖料路线短，淀粉路线需要酶，但作物更容易大量种植，可以配合种植机自动化。

## 2. 扩培菌种

[发酵罐](plastic/fermenter) 用 1 份菌种、糖和水扩培成 2 份菌种。先各备几份：

- 酵母菌种：发酵乙醇。
- 乳酸菌种：发酵乳酸，供聚乳酸和凝固橡胶。
- 曲霉菌种：把小麦和糖液做成酶制剂。

## 3. 乙醇与乙烯

1. 发酵罐用酵母把糖液发酵成发酵醪，副产二氧化碳，需要冷却水。
2. [精馏塔](plastic/distillation_column) 用蒸汽分出乙醇和酒糟，酒糟经干燥器得到酒糟饲料。
3. [脱水反应器](plastic/dehydration_reactor)（MV）把乙醇脱水成乙烯，副产工艺水。

> [!NOTE]
> 现实中的生物乙烯也是先发酵制乙醇，再在催化剂上脱水。需要大量乙烯时，可以搭生物乙烯装置一步完成。
