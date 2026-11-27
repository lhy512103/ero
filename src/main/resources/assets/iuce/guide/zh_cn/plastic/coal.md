---
title: 煤化工
category: plastic
order: 4
icon: iuce:coke
---

煤化工为聚氯乙烯路线提供焦炭、电石和多种化工原料，也给溶剂、增塑剂和碳素电极供料。

{flow: minecraft:coal -> @iuce:coking_oven -> iuce:coke -> @iuce:carbide_furnace -> iuce:carbide_lump}

## 炼焦

[炼焦炉](plastic/coking_oven) 隔绝空气加热煤，产出三样东西：

- **焦炭**：送电石炉；打粉得焦粉，用于电极料和橡胶密炼。
- **焦炉气**：经 [洗涤塔](plastic/scrubber) 用烧碱液脱硫，得到净焦炉气和硫磺。
- **煤焦油**：[焦油蒸馏釜](plastic/tar_distiller) 分出萘、沥青和溶剂油。

## 石灰与电石

1. [石灰窑](plastic/lime_kiln) 煅烧石灰岩（Y 0–80）得到生石灰。
2. 焦粉与沥青混成电极料，用压缩机压成碳素电极。
3. [电石炉](plastic/carbide_furnace) 用电极熔炼生石灰和焦炭，得到电石块，副产炉气。
4. 电石块打粉后送 [乙炔发生器](plastic/acetylene_generator)，加水产出粗乙炔和电石渣浆。
5. [压滤机](plastic/filter_press) 滤出电石渣，送回石灰窑重新煅烧，形成循环。

> [!NOTE]
> 规模化时可以换成炼焦炉组和电石联合炉，产量更高，并能接余热电站回收热量发电。
