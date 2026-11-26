---
title: 洗涤塔
category: plastic
order: 155
icon: iuce:scrubber
items: iuce:scrubber
---

用烧碱液洗去气体中的酸性杂质：净化乙炔、焦炉气和粗氯乙烯。

## 规格

- 电压：LV，内部缓存 8000 EU
- 物品：输入 0 格，输出 1 格
- 流体：输入 2 槽，输出 2 槽（每槽 8000 mB）
- 升级槽：4 个

## 配方

- **粗乙炔** × 500 mB + **烧碱液** × 100 mB → **乙炔** × 500 mB + **废碱液** × 100 mB（100 tick / 5 秒，共 1600 EU）
- **焦炉气** × 1000 mB + **烧碱液** × 100 mB → **硫磺** × 1 + **净化焦炉气** × 1000 mB + **废碱液** × 100 mB（100 tick / 5 秒，共 1600 EU）
- **粗氯乙烯** × 500 mB + **烧碱液** × 100 mB → **氯乙烯气** × 500 mB + **废碱液** × 100 mB（100 tick / 5 秒，共 1600 EU）

## 上下游

- 原料来自：[乙炔发生器](plastic/acetylene_generator)、[炼焦炉](plastic/coking_oven)、[离子膜电解槽](plastic/membrane_cell)、[氯乙烯转化器](plastic/vcm_converter)
- 产物送往：[气体压缩机](plastic/gas_compressor)、[中和槽](plastic/neutralizer)、[密炼机](plastic/rubber_mixer)、[氯乙烯转化器](plastic/vcm_converter)

> [!NOTE]
> 废碱液送中和槽。

## 合成

{recipe: iuce:scrubber}
