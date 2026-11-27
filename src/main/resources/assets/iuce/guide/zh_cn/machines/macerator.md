---
title: 打粉机
category: machines
order: 10
icon: iuce:macerator
items: iuce:macerator
---

把矿石磨成粗粉，是增产线的第一步。也能把煤、钻石、铜锭、青铜锭磨成粉。

- 电压：LV
- 矿石出 2 份粗粉，深层矿石出 3 份。

## 增产线

{flow: iuce:tin_ore -> @iuce:macerator -> @iuce:ore_washer -> @iuce:electric_furnace}

1. 打粉机把矿石磨成粗粉。
2. [洗矿机](machines/ore_washer) 把粗粉洗成纯净粉，附带伴生金属粒。
3. [电炉](machines/electric_furnace) 熔炼：粗粉出 2 锭，纯净粉出 3 锭。

粗粉直接扔进普通熔炉只出 1 锭，增产必须用电炉。后期可以用 [选矿联合厂](machines/ore_plant) 把三步合成一步。

## 其他用途

- 煤 → 煤粉：压成煤球做高热值燃料。
- 钻石 → 钻石粉：送电解机做能量水晶粉。
- 铜锭、青铜锭 → 粉：用于合金与合成。

{image: gui/machine/schematic/macerator.png | 界面中的打粉机：左侧电量条，上方放原料，下方出产物，右侧四格放升级}

## 合成

{recipe: iuce:macerator}

## 全部配方

{recipes: macerator}
