---
title: 导线
category: power
order: 30
icon: iuce:insulated_copper_cable
items: iuce:copper_cable, iuce:tin_cable, iuce:gold_cable, iuce:iron_cable, iuce:glass_cable, iuce:singular_cable, iuce:insulated_copper_cable, iuce:insulated_tin_cable, iuce:insulated_gold_cable, iuce:insulated_hv_cable
---

导线把电从电源送到机器。每种导线有自己的**吞吐上限**，一条线路能送多少电由最细的那一段决定。

| 导线 | 每 tick 上限 |
| --- | --- |
| {item:iuce:tin_cable} | 8 EU |
| {item:iuce:copper_cable} | 32 EU |
| {item:iuce:gold_cable} | 128 EU |
| {item:iuce:iron_cable} | 512 EU |
| {item:iuce:glass_cable} | 2048 EU |
| {item:iuce:singular_cable} | 32768 EU |

{items: iuce:tin_cable, iuce:copper_cable, iuce:gold_cable, iuce:iron_cable, iuce:glass_cable, iuce:singular_cable}

## 制作

{flow: iuce:copper_plate -> @iuce:metal_former -> iuce:copper_cable*4}

{recipe: iuce:insulated_copper_cable, iuce:glass_cable}

裸导线和绝缘导线的吞吐相同。造电路板、电池和机器时用的是绝缘导线。

## 断开连接

用 [扳手](start/tools) 右键导线的某一面可以断开这一面，再点一次恢复。断开的面不传电，也不会和旁边的机器相连。
