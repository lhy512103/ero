---
title: 脱水反应器
category: plastic
order: 115
icon: iuce:dehydration_reactor
items: iuce:dehydration_reactor
---

在催化剂作用下把乙醇脱水成乙烯，是生物基聚乙烯的单体来源。

## 规格

- 电压：MV，内部缓存 16000 EU
- 物品：输入 0 格，输出 1 格
- 流体：输入 1 槽，输出 2 槽（每槽 8000 mB）
- 升级槽：4 个
- 有催化剂槽
- 公用工程：蒸汽，见 [公用工程](plastic/utilities)

## 配方

{recipes: dehydration_reactor}

## 上下游

- 原料来自：[精馏塔](plastic/distillation_column)
- 产物送往：[聚合釜](plastic/polymerization_reactor)

> [!NOTE]
> MV 设备，注意供电电压。
> 副产工艺水可送水处理器回用。

## 合成

{recipe: iuce:dehydration_reactor}
