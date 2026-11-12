---
title: 搭一条产线
category: automation
order: 20
icon: iuce:ejector_upgrade
---

把前面的机器串起来，就是一条全自动的矿物产线。

## 布局

{flow: @iuce:macerator -> @iuce:ore_washer -> @iuce:electric_furnace -> iuce:tin_ingot*6}

1. 打粉机、洗矿机、电炉放在一起，彼此不超过 8 格。
2. 拿一个 {item:iuce:ejector_upgrade}，潜行右键洗矿机完成绑定，再放进打粉机的升级槽：粗粉会自动送去洗矿。
3. 再拿一个弹出升级绑定电炉，放进洗矿机：纯净粉会自动送去熔炼。
4. 洗矿机旁边紧贴一台 [泵](machines/pump)，泵旁边留两格水源。
5. 发电机或储电箱贴在任意一台机器旁，用导线把其余机器连上。

之后只要往打粉机里放矿石，从电炉里取锭。

> [!NOTE]
> 物品升级只能在模组机器之间传送。要和箱子对接，用漏斗即可：机器上方的漏斗送料，下方的漏斗取走产物。

## 供电检查

- 三台低压机器加一台泵，不超频时一台 [火力发电机](power/generators) 勉强够用，建议配一个储电箱缓冲。
- 加了超频后先看导线：铜质导线每 tick 只能过 32 EU。

## 再往后

- 在电炉旁贴一台 [感应炉](machines/induction_furnace)，电炉速度 +50%。
- 用 [种植机](machines/planter) 和提取机把橡胶也自动化。
