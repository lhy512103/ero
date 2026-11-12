---
title: 发展路线
category: advanced
order: 10
icon: iuce:advanced_circuit
items: iuce:circuit, iuce:advanced_circuit, iuce:machine_casing, iuce:machine_casing_2
---

每个阶段的机器都要用上一个阶段的产物。卡住的时候，看看是哪一条产线还没搭。

## 阶段一：手工

锤子、剪刀、木龙头 → 板、导线、橡胶 → 电路板、机器外壳、电动马达。

{items: iuce:forge_hammer, iuce:cutter, iuce:treetap, iuce:circuit, iuce:machine_casing, iuce:electric_motor}

## 阶段二：低压

发电机带起打粉机、电炉、金属成型机、压缩机、提取机和工业工作台。目标是 {item:iuce:dense_copper_plate}。

{items: iuce:generator, iuce:macerator, iuce:electric_furnace, iuce:metal_former, iuce:compressor, iuce:extractor, iuce:industrial_workbench}

## 阶段三：合金

感应炉带来青铜、黄铜和钢：

- 青铜 → 转子 → 洗矿机和泵，增产线打通。
- 黄铜 + 银板 → {item:iuce:advanced_circuit}。
- 钢板 + 铝 + 双层铁板 → {item:iuce:machine_casing_2}。

{recipe: iuce:induction_furnace, iuce:advanced_circuit, iuce:machine_casing_2}

## 阶段四：中压电化学

电解机带来电镀外壳和能量水晶粉，接着是能量水晶、兰波顿水晶、MFE；精炼钢打开 MFSU。从这里开始需要 [变压器](power/transformer)。

{items: iuce:electrolyzer, iuce:plated_shell, iuce:energium_dust, iuce:energy_crystal, iuce:lapotron_crystal, iuce:mfe, iuce:mfsu}

## 阶段五：终局

[太阳能阶梯](power/solar) 一路做到奇异太阳能；[裂变反应堆](advanced/reactor) 仍在建设中。
