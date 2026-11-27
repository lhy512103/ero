---
title: 氯化氢合成炉
category: plastic
order: 153
icon: iuce:hcl_furnace
items: iuce:hcl_furnace
---

氯气在氢气中燃烧合成氯化氢。

## 规格

- 电压：LV，内部缓存 8000 EU
- 物品：输入 0 格，输出 0 格
- 流体：输入 2 槽，输出 1 槽（每槽 8000 mB）
- 升级槽：4 个
- 公用工程：冷却水，见 [公用工程](plastic/utilities)

## 配方

{recipes: hcl_furnace}

## 上下游

- 原料来自：[离子膜电解槽](plastic/membrane_cell)
- 产物送往：[中和槽](plastic/neutralizer)、[氯乙烯转化器](plastic/vcm_converter)

> [!NOTE]
> 按等量供给氯和氢。

## 合成

{recipe: iuce:hcl_furnace}
