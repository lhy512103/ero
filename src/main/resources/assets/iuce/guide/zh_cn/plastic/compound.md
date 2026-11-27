---
title: 改性造粒
category: plastic
order: 6
icon: iuce:strand
---

聚合得到的是粉料或熔体，不能直接注塑或挤出。改性造粒把它们熔融塑化、加入色母或填料，再切成统一的颗粒。

{flow: iuce:pe_powder -> @iuce:compounding_extruder -> iuce:strand -> @iuce:strand_pelletizer -> iuce:bio_pe_pellet}

## 单机流程

1. [双螺杆挤出机](plastic/compounding_extruder) 把聚乙烯粉、PVC 干混料等挤成料条。料条会记住最终要切成哪种颗粒。
2. [水浴切粒机](plastic/strand_pelletizer) 用冷却水冷却料条并切粒。
3. 聚乳酸颗粒还要经 [除湿干燥机](plastic/dehumidifying_dryer) 干燥。

## 改性

- **配色**：颗粒加色母重新挤出，得到同牌号的彩色颗粒。
- **填充**：聚乙烯颗粒加碳酸钙粉，得到填充聚乙烯，成本更低、刚性更好。
- 染料也能直接和聚乙烯颗粒挤出成彩色料条。

## 联合双螺杆挤出机

大量生产时搭建联合双螺杆挤出机。主喂段、熔融段和模头段是基本工艺；填料需要侧喂段，软硬 PVC 需要排气段，色母需要混炼段。

> [!NOTE]
> 工业双螺杆用不同的螺纹元件组合完成输送、熔融、分散和排气，联合挤出机的分段就是照这个思路设计的。
