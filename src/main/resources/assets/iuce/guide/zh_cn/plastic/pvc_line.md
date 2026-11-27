---
title: 氯碱与聚氯乙烯
category: plastic
order: 5
icon: iuce:pvc_resin
---

电石法聚氯乙烯是最长的一条线：煤化工提供乙炔，氯碱提供氯化氢，两者合成氯乙烯再聚合。产出的 PVC 可以做成硬质管材或软质电线护套。

{flow: iuce:rock_salt -> @iuce:brine_dissolver -> @iuce:membrane_cell -> @iuce:hcl_furnace -> @iuce:vcm_converter -> @iuce:polymerization_reactor -> iuce:pvc_resin}

## 氯碱

1. 在沙漠或海洋下方 Y 20–60 寻找岩盐，[化盐槽](plastic/brine_dissolver) 溶成粗盐水。
2. [精制槽](plastic/brine_purifier) 用生石灰除杂，得到精盐水。
3. [离子膜电解槽](plastic/membrane_cell) 产出氯气、氢气和烧碱液。三样都要接走，否则停机。
4. [氯化氢合成炉](plastic/hcl_furnace) 等量燃烧氯和氢，得到氯化氢。

## 氯乙烯

1. 粗乙炔经 [洗涤塔](plastic/scrubber) 净化（来源见 [煤化工](plastic/coal)）。
2. [氯乙烯转化器](plastic/vcm_converter) 在无汞催化剂上让乙炔与氯化氢加成，得到粗氯乙烯。
3. 洗涤塔洗去酸性杂质，[气体压缩机](plastic/gas_compressor) 液化。
4. [精馏塔](plastic/distillation_column) 两段精馏：先脱轻组分（乙炔回转化器），再除重组分得到精氯乙烯。

## 聚合与后处理

1. [聚合釜](plastic/polymerization_reactor) 插入 PVC 工艺卡，输入氯乙烯、去离子水、引发剂和分散剂，得到 PVC 浆料。
2. [闪蒸罐](plastic/flash_tank) 回收残余氯乙烯，接回气体压缩机。
3. [离心机](plastic/centrifuge) 脱水，[干燥器](plastic/dryer) 烘干得到 PVC 树脂。
4. [高混机](plastic/high_speed_mixer) 配入热稳定剂、聚乙烯蜡和碳酸钙得硬质干混料；改用增塑剂得软质干混料。助剂由 [助剂反应釜](plastic/additive_reactor) 合成。

## 循环

- 电石渣回石灰窑，工艺水送 [水处理器](plastic/water_treatment) 制去离子水。
- 废碱液与氯化氢在 [中和槽](plastic/neutralizer) 变回盐水，送回精制槽。
