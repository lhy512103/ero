---
title: 聚乳酸
category: plastic
order: 2
icon: iuce:lactide
---

聚乳酸（PLA）全部来自作物，不需要煤和盐，是最容易起步的树脂。它分为薄膜级和注塑级两种牌号。

{flow: @iuce:fermenter -> @iuce:evaporator -> @iuce:oligomerization_reactor -> @iuce:depolymerization_reactor -> @iuce:crystallizer -> iuce:lactide -> @iuce:polymerization_reactor}

## 步骤

1. **乳酸发酵**：[发酵罐](plastic/fermenter) 以乳酸菌种发酵糖液，得到乳酸发酵液。
2. **浓缩**：[浓缩罐](plastic/evaporator) 用蒸汽蒸出水分，得到乳酸。
3. **低聚**：[低聚釜](plastic/oligomerization_reactor) 让乳酸缩合成低聚物，脱去工艺水。
4. **解聚**：[解聚釜](plastic/depolymerization_reactor) 把低聚物裂解成粗丙交酯。
5. **结晶提纯**：[结晶器](plastic/crystallizer) 用冷却水得到丙交酯，母液送回浓缩罐回收乳酸。
6. **开环聚合**：[聚合釜](plastic/polymerization_reactor) 插入薄膜级或注塑级工艺卡，把丙交酯聚合成对应熔体。锡催化剂由催化剂配制罐制备。
7. **造粒**：熔体经水浴切粒机变成颗粒，见 [改性造粒](plastic/compound)。

> [!WARN]
> 聚乳酸会吸潮。注塑或吹膜前一定要用 [除湿干燥机](plastic/dehumidifying_dryer) 干燥颗粒。

> [!NOTE]
> 工业上也是乳酸 → 低聚 → 解聚成丙交酯 → 开环聚合，这样能得到分子量高、性能稳定的聚乳酸。
