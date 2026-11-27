---
title: 塑料工业总览
category: plastic
order: 0
icon: iuce:pla_injection_pellet
---

塑料工业是中压阶段的主线。它把作物、煤、石灰岩和岩盐变成树脂，再改性造粒，最后注塑或挤出成制品。整条线比前面的机器长得多，建议按下面的顺序一段一段搭，每段跑通再接下一段。

{flow: minecraft:sugar_cane -> @iuce:wet_mill -> @iuce:fermenter -> @iuce:polymerization_reactor -> iuce:pla_injection_pellet}

## 推荐搭建顺序

1. **公用工程**：先有锅炉供蒸汽、冷却水和压缩空气，大部分装置缺了它们会停机。见 [公用工程](plastic/utilities)。
2. **生物路线**：作物 → 糖液 → 乙醇 → 乙烯，同时扩培各种菌种。见 [生物路线](plastic/biology)。
3. **选一种树脂**：
   - [聚乳酸](plastic/pla_line)：只用作物，最容易起步，适合注塑和吹膜。
   - [生物基聚乙烯](plastic/pe_line)：用乙烯聚合，适合管材、薄膜和滴灌带。
   - [氯碱与聚氯乙烯](plastic/pvc_line)：要煤化工和氯碱，链条最长，产出硬质和软质 PVC。
4. **改性造粒**：树脂粉经挤出、切粒变成可加工的颗粒。见 [改性造粒](plastic/compound)。
5. **成型**：[注塑与模具](plastic/injection_line) 做零件，[挤出](plastic/extrusion) 做管、膜、线。
6. **扩产**：单机跑通后换成联合装置，如联合双螺杆挤出机、生物乙烯装置、电石联合炉和炼焦炉组。

## 通用规则

- 每台加工设备都有电量、物品、流体槽和四个升级槽，界面结构与其他机器一致。
- 有催化剂槽的设备按配方缓慢消耗催化剂；有工艺卡槽的设备由卡片决定产出牌号，见 [工艺卡](plastic/cards)。
- 任一输出槽满都会停机。副产气体接 {item:iuce:vent_stack}，工艺水接水处理器回用。
- 设备之间用管道和 [物流第二层](plastic/logistics) 连接，比人工搬运稳定得多。

> [!NOTE]
> 每个设备都有独立页面，列出规格、全部配方和上下游设备，可以在左侧目录或 JEI 中直接跳转。
