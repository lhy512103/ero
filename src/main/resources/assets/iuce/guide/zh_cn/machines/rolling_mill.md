---
title: 轧制联合机
category: machines
order: 210
icon: iuce:mill_controller
items: iuce:mill_controller, iuce:energy_hatch_hv
---

第二台大机器，金属成型机的放大版：一次几十份地出板、导线和棍。

- 放进控制器的 [模具](machines/metal_former) 决定做什么：
  - {item:iuce:plate_mold}：锭 → 板
  - {item:iuce:wire_mold}：锭 → 导线（辊压和切割一步完成，一个锭 4 根）
  - {item:iuce:rod_mold}：锭 → 棍
- 手持模具右键控制器放入，换下来的模具会还给你；潜行空手右键取出。
- 每批 5 秒，单件耗电是金属成型机的一半。

## 结构

{structure: rolling_mill | 宽 5、高 5、长 9 的空心外壳；可以加长到 17}

和 [选矿联合厂](machines/ore_plant) 用同样的外壳和仓口，只是更长。**长度可以自己定**：从 9 格到 17 格，越长并行越多，17 格时翻倍。长度用 [图纸终端](machines/building) 选：潜行右键控制器，每次加长一格。先搭 9 格用着，以后选个更长的规格，把后墙拆了往后补就行。

{recipe: iuce:mill_controller}

## 并行数看能源仓

| 能源仓 | 并行 | 每 tick 最多收电 |
| --- | --- | --- |
| {item:iuce:energy_hatch} | 16 | 128 EU |
| {item:iuce:energy_hatch_hv} | 64 | 512 EU |

把能源仓换成高一级的，旧机器的并行数直接变成 4 倍，结构不用动。选矿联合厂也一样。放了几种能源仓时按最高的算。

{recipe: iuce:energy_hatch_hv}
