---
title: 橡胶
category: start
order: 30
icon: iuce:rubber
items: iuce:rubber, iuce:raw_latex, iuce:rubber_wood, iuce:rubber_sapling, iuce:rubber_leaves
---

橡胶是绝缘导线的原料，整条电力线都离不开它。

## 采胶

橡胶树的树干上有**胶口**。拿 {item:iuce:treetap} 右键胶口：

- 胶口湿润时：得到 1~3 个 {item:iuce:raw_latex}，胶口变干，过一阵会重新渗出。
- 胶口干燥时：有 20% 概率硬挤出 1 个，但这个胶口会永久消失。

> [!TIP]
> 等胶口变湿再采，一棵树可以一直用下去。

## 加工

{flow: iuce:raw_latex -> @minecraft:furnace -> iuce:rubber}

{flow: iuce:raw_latex -> @iuce:extractor -> iuce:rubber*3}

熔炉只出 1 份，[提取机](machines/extractor) 出 3 份。提取机还能直接处理橡胶原木和树叶。

## 全自动

{flow: iuce:rubber_sapling + minecraft:dirt -> @iuce:planter -> iuce:rubber_wood*4 -> @iuce:extractor -> iuce:raw_latex -> @iuce:extractor -> iuce:rubber*3}

[种植机](machines/planter) 配两台提取机，橡胶就不用再手采了。
