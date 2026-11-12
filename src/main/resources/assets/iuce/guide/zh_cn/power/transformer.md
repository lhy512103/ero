---
title: 变压器
category: power
order: 50
icon: iuce:transformer
items: iuce:transformer
---

变压器把高电压降下来，让高等级电源能带低等级的机器。

{recipe: iuce:transformer}

## 用法

{flow: iuce:mfsu -> iuce:iron_cable -> @iuce:transformer -> iuce:insulated_copper_cable -> iuce:macerator}

- **空手右键**切换输出等级：LV → MV → HV → EV。
- 只接收**高于**输出等级的电，输出不损耗。
- 每 tick 吞吐是输出电压的 4 倍：输出 LV 时为 128 EU/t，相当于一路高压拆成四路低压。
- 可以给和输出**同等级**的储电箱充电。

## 潮湿导电

> [!WARN]
> 变压器带电时，周围 **10 格**内站在水里或淋着雨的生物（包括玩家）每 2 秒被电击一次，并被定在原地。

| 电压 | 伤害 | 定身 |
| --- | --- | --- |
| LV | 1 | 1.5 秒 |
| MV | 2 | 2 秒 |
| HV | 3 | 2.5 秒 |
| EV | 4 | 3 秒 |

强度取输入侧和输出侧里较高的电压。干燥的生物不受影响，创造模式玩家也不受影响。

> [!TIP]
> 给变压器搭个屋顶、远离水源就安全了。反过来，在水池边放一台带电变压器就是一座刷怪塔的处决装置。
