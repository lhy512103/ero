---
title: 机器升级
category: automation
order: 10
icon: iuce:overclocker_upgrade
items: iuce:overclocker_upgrade, iuce:overclocker_upgrade1, iuce:overclocker_upgrade2, iuce:ejector_upgrade, iuce:pulling_upgrade, iuce:fluid_ejector_upgrade, iuce:fluid_pulling_upgrade, iuce:coolant_cell, iuce:empty_cell, iuce:coolant_bucket
---

大多数机器界面右侧有 4 个升级槽，每格放 1 个升级。

## 超频

每个超频升级让加工时间 ×0.75、耗电 ×1.5，效果相乘，最多 4 个生效。

| 数量 | 加工时间 | 每 tick 耗电 |
| --- | --- | --- |
| 1 | 75% | 1.5 倍 |
| 2 | 56% | 2.25 倍 |
| 3 | 42% | 3.4 倍 |
| 4 | 32% | 5 倍 |

{recipe: iuce:overclocker_upgrade, iuce:overclocker_upgrade1, iuce:overclocker_upgrade2}

> [!NOTE]
> 三种超频升级目前的效果相同，先做最便宜的基础款即可。

> [!WARN]
> 超频后的机器每 tick 耗电可能超过铜质导线的 32 EU 上限，记得换粗一点的导线。

## 物品传输

物品升级要先**绑定目标机器**：手持升级，潜行右键目标机器，然后把升级放进本机的升级槽。目标必须是模组机器，距离不超过 8 格。

| 升级 | 作用 |
| --- | --- |
| {item:iuce:ejector_upgrade} | 把本机的产物送进目标机器的输入槽 |
| {item:iuce:pulling_upgrade} | 把目标机器输入槽里的原料取到本机 |

每秒传送 1 个物品。

{recipe: iuce:ejector_upgrade, iuce:pulling_upgrade}

## 流体传输

流体升级不用绑定：手持升级右键机器即可装上，对紧贴的机器生效。

| 升级 | 作用 |
| --- | --- |
| {item:iuce:fluid_ejector_upgrade} | 把水箱里的水送给相邻机器 |
| {item:iuce:fluid_pulling_upgrade} | 从相邻的泵或带水箱的机器取水 |

{recipe: iuce:fluid_ejector_upgrade, iuce:fluid_pulling_upgrade}

## 冷却单元

{recipe: iuce:empty_cell, iuce:coolant_cell}

冷却液由水桶加青金石混合得到，是超频升级的材料。
