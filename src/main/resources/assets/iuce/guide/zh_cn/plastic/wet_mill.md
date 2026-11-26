---
title: 湿磨机
category: plastic
order: 110
icon: iuce:wet_mill
items: iuce:wet_mill
---

生物路线的起点。把甘蔗、甜菜等糖料作物压榨成糖液，把土豆、小麦等淀粉作物磨成淀粉浆。

## 规格

- 电压：LV，内部缓存 4000 EU
- 物品：输入 1 格，输出 1 格
- 流体：输入 1 槽，输出 1 槽（每槽 8000 mB）
- 升级槽：4 个

## 配方

- **标签 starch_crops** × 4 + **标签 any_water** × 1000 mB → **淀粉浆** × 1000 mB（100 tick / 5 秒，共 1600 EU）
- **标签 sugar_crops** × 4 + **标签 any_water** × 500 mB → **蔗渣** × 1 + **糖液** × 1000 mB（100 tick / 5 秒，共 1600 EU）

## 上下游

- 产物送往：[助剂反应釜](plastic/additive_reactor)、[发酵罐](plastic/fermenter)、[液化罐](plastic/liquefaction_tank)

> [!NOTE]
> 糖料作物的蔗渣可以当燃料或堆肥。
> 淀粉浆还要经过液化罐和糖化罐才能变成糖液。

## 合成

{recipe: iuce:wet_mill}
