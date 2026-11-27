---
title: 生物基聚乙烯
category: plastic
order: 3
icon: iuce:bio_pe_pellet
---

生物基聚乙烯用生物路线产出的乙烯聚合而成，是管材、薄膜、滴灌带和电线护套的主要原料。

{flow: ~iuce:ethylene*500 -> @iuce:polymerization_reactor -> @iuce:centrifuge -> @iuce:dryer -> iuce:pe_powder}

## 步骤

1. **催化剂**：[催化剂配制罐](plastic/catalyst_prep_tank) 用纯铝粉、氯气和溶剂油配制聚烯烃催化剂。
2. **聚合**：[聚合釜](plastic/polymerization_reactor) 插入聚乙烯工艺卡，输入乙烯和溶剂油，得到聚乙烯浆液；未反应的乙烯会循环输出。
3. **离心**：[离心机](plastic/centrifuge) 把浆液分成湿聚乙烯粉和母液。
4. **干燥**：[干燥器](plastic/dryer) 用蒸汽烘干，得到聚乙烯粉并回收溶剂油。
5. **溶剂回收**：[溶剂回收塔](plastic/solvent_recovery_tower) 从母液中回收溶剂油，同时得到聚乙烯蜡（PVC 助剂原料）。
6. **造粒**：聚乙烯粉进入挤出机做成料条再切粒，见 [改性造粒](plastic/compound)。

> [!NOTE]
> 每批大约能回收 980 mB 溶剂，把干燥器和溶剂回收塔的溶剂油接回聚合釜，几乎不需要补充。
