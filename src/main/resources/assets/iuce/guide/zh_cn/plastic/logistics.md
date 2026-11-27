---
title: 物流第二层
category: plastic
order: 11
icon: iuce:logistics_port_ii
---

塑料工业的物料量远大于前期，人工搬运和一级物流都跟不上。物流第二层用橡胶传送带做出更快的输送线和更大的料仓。

{flow: iuce:rubber_belt -> @iuce:conveyor_ii -> @iuce:bulk_silo}

## 组成

- {item:iuce:conveyor_ii}：二级传送带，用 [天然橡胶](plastic/rubber_line) 线产出的橡胶传送带制造，输送更快。
- {item:iuce:bulk_silo}：大容量料仓，适合存放颗粒、粉料等大宗物料。
- {item:iuce:logistics_port_ii}：二级物流接口。同时邻接物品管道和流体管道时，会把物品和流体分别送入对应的管道。

## 用法

- 把二级接口贴在加工设备上，一个接口就能同时处理固体原料和流体产物。
- 大宗中间品（树脂粉、颗粒）先进料仓缓冲，下游按需取用，产线节拍不一致也不会互相卡住。
- 一级物流的规则仍然适用，见 [物流接口](automation/pipes)。

> [!NOTE]
> 实际化工装置也把固体输送和流体管道分开布置，中间设料仓缓冲。
