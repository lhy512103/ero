---
title: 液化罐
category: plastic
order: 111
icon: iuce:liquefaction_tank
items: iuce:liquefaction_tank
---

用酶制剂把淀粉浆中的长链淀粉切断，得到液化醪。

## 规格

- 电压：LV，内部缓存 4000 EU
- 物品：输入 1 格，输出 0 格
- 流体：输入 1 槽，输出 1 槽（每槽 8000 mB）
- 升级槽：4 个
- 公用工程：蒸汽，见 [公用工程](plastic/utilities)

## 配方

- **酶制剂** × 1 + **淀粉浆** × 1000 mB → **液化醪** × 1000 mB（120 tick / 6 秒，共 1920 EU），需要蒸汽

## 上下游

- 原料来自：[发酵罐](plastic/fermenter)、[湿磨机](plastic/wet_mill)
- 产物送往：[糖化罐](plastic/saccharification_tank)

> [!NOTE]
> 酶制剂由发酵罐用曲霉培养物制得。

## 合成

{recipe: iuce:liquefaction_tank}
