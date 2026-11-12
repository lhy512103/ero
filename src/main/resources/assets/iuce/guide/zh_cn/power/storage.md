---
title: 储电箱
category: power
order: 40
icon: iuce:batbox
items: iuce:batbox, iuce:cesu, iuce:mfe, iuce:mfsu, iuce:re_battery, iuce:advanced_re_battery, iuce:energy_crystal, iuce:lapotron_crystal, iuce:single_use_battery
---

储电箱把多余的电存起来，发电机停了也能继续带机器。

| 储电箱 | 电压 | 容量 |
| --- | --- | --- |
| {item:iuce:batbox} | LV | 40 000 EU |
| {item:iuce:cesu} | MV | 300 000 EU |
| {item:iuce:mfe} | HV | 4 000 000 EU |
| {item:iuce:mfsu} | EV | 40 000 000 EU |

{recipe: iuce:batbox, iuce:cesu, iuce:mfe, iuce:mfsu}

> [!WARN]
> 储电箱按自己的电压等级输出。MFSU 是超高压，直接接低压机器不通电，要经过 [变压器](power/transformer)。

## 电池与充电

储电箱界面里的充电槽可以给电池和电动工具充电。

| 电池 | 容量 |
| --- | --- |
| {item:iuce:re_battery} | 10 000 EU |
| {item:iuce:advanced_re_battery} | 100 000 EU |
| {item:iuce:energy_crystal} | 1 000 000 EU |
| {item:iuce:lapotron_crystal} | 10 000 000 EU |

{recipe: iuce:re_battery, iuce:advanced_re_battery, iuce:energy_crystal, iuce:lapotron_crystal}

{item:iuce:energy_crystal} 要先在 [电解机](machines/electrolyzer) 里把钻石粉做成能量水晶粉，再用 [压缩机](machines/compressor) 压制。
