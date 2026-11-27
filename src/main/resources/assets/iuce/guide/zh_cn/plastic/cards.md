---
title: 工艺卡
category: plastic
order: 12
icon: iuce:process_card
---

同一台聚合釜可以生产很多种树脂。插在卡槽里的工艺卡决定这台机器现在跑哪个牌号，没插卡或卡不对时机器不会工作。

{flow: iuce:process_card -> @iuce:process_design_table -> @iuce:polymerization_reactor}

## 写卡

1. 合成空白 {item:iuce:process_card}。
2. 放入 {item:iuce:process_design_table}，选择工艺族和牌号，写入卡片。
3. 设计台可以复制已写好的卡：放入样卡和空白卡，复制后样卡保留。

## 用卡

- 把卡插进 [聚合釜](plastic/polymerization_reactor) 等有卡槽的设备，机器只会运行该牌号对应的配方。
- 换牌号只需换卡，不用拆管道；多台聚合釜各插一张卡，就能同时生产不同牌号。
- 设备页面的“规格”一节会标出是否有工艺卡槽，JEI 配方页也会显示所需的卡。

> [!NOTE]
> 现实工厂也是按牌号调用批次配方和操作条件，同一套装置可以切换生产多个牌号。
