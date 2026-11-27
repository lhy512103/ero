---
title: 注塑机
category: plastic
order: 181
icon: iuce:injection_molder
items: iuce:injection_molder
---

把注塑级颗粒注入模腔，做出周转箱、积木、桶、机壳等制品，同时产出水口料。

## 规格

- 电压：MV，内部缓存 16000 EU
- 物品：输入 1 格，输出 2 格
- 流体：输入 0 槽，输出 0 槽
- 升级槽：4 个
- 有催化剂槽
- 公用工程：冷却水、压缩空气，见 [公用工程](plastic/utilities)

## 配方

{recipes: injection_molder}

## 上下游

- 原料来自：[双螺杆挤出机](plastic/compounding_extruder)、[除湿干燥机](plastic/dehumidifying_dryer)、[塑料粉碎机](plastic/plastic_granulator)、[水浴切粒机](plastic/strand_pelletizer)
- 产物送往：[塑料粉碎机](plastic/plastic_granulator)

> [!NOTE]
> 需要压缩空气和冷却水。
> 水口料送塑料粉碎机回收成颗粒。

## 合成

{recipe: iuce:injection_molder}
