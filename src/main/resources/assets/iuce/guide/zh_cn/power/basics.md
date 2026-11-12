---
title: 电力基础
category: power
order: 10
icon: iuce:re_battery
---

模组的能量单位是 **EU**。发电设备产电，储电箱存电，机器耗电。

## 怎么通电

- 发电机或储电箱**紧贴**机器，直接供电。
- 距离远就用 [导线](power/cables) 连接。
- 电总是优先送给机器，其次才给储电箱充电。

## 电压等级

| 等级 | 每 tick 上限 | 典型设备 |
| --- | --- | --- |
| LV 低压 | 32 EU | 发电机、大多数机器、储电箱 |
| MV 中压 | 128 EU | 感应炉、电解机、CESU |
| HV 高压 | 512 EU | MFE |
| EV 超高压 | 2048 EU | MFSU |

## 电压规则

机器和储电箱只接受**不高于自身等级**的电。中压电源可以带中压机器，但带不动低压机器。

> [!WARN]
> 高等级电源接低等级机器不会爆炸，只是完全不通电。中间加一台 [变压器](power/transformer) 降压即可。

反过来没有限制：低压发电机可以给中压机器供电，只是速度慢。

{cards: power/generators, power/cables, power/storage, power/transformer, power/solar}
