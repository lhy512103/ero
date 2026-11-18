---
title: 选矿联合厂
category: machines
order: 200
icon: iuce:plant_controller
items: iuce:plant_controller, iuce:plant_casing, iuce:item_input_hatch, iuce:item_output_hatch, iuce:energy_hatch
---

第一台大机器。它把 **打粉 → 洗矿 → 熔炼** 三步合成一步：矿石进去，金属锭和伴生金属粒出来，中间的粗粉、纯净粉不落地。

- 一次同时加工 **16 份**，每批 5 秒。一块矿石出 6 个锭。
- 单件耗电是三台小机器合计的一半。
- 不需要供水。
- 粗粉、纯净粉也可以直接投进去，会从对应的那一步开始。

## 结构

{structure: ore_plant | 宽 11、高 11、长 15；带高塔、露天洗选槽和末端炉段}

- 用 {item:iuce:plant_casing} 搭底座和工艺段，{item:iuce:plant_frame} 做门架和连梁，{item:iuce:plant_drum} 做轴筒。梁和滚筒沿轴放置：从端面接放下一段即可延长。
- 控制器的屏幕朝外。
- 面朝控制器时，输入在右、输出在左；能源仓在末端正中。门架外缘另有可选仓位。旧版矩形厂房继续采用原有规则。
- 搭好后右键控制器：成型了屏幕会变绿；没成型会告诉你哪里不对。
- **搭建投影**：黄色是可自动填充的外壳；蓝色是专用部件或仓口；放错方块或朝向会标红。
- **一键补外壳**：手持 {item:iuce:plant_casing} 右键控制器，会把手里的外壳自动放到所有空着的位置。仓口先摆好，剩下的交给它。更省事的办法见 [搭建大机器的工具](machines/building)。

{recipe: iuce:plant_casing, iuce:plant_controller}

{recipe: iuce:plant_frame, iuce:plant_drum}

{recipe: iuce:item_input_hatch, iuce:item_output_hatch, iuce:energy_hatch}

## 进出料

- 右键输入仓或输出仓打开九格库存；输入仓可存取，输出仓只能取出。右键能源仓查看实时储能，右键控制器查看进度和并行数。
- 仓口接受漏斗、滑槽、传送带和其他模组的管道：输入仓只进，输出仓只出。
- 把 [物流接口](automation/pipes) 贴在仓口上，管网会自动把能加工的矿石送进输入仓，把成品从输出仓运走。

## 供电

能源仓是中压（MV）设备，每 tick 最多收 128 EU。满负荷加工矿石时要的电比这多，联合厂会按实际供电降速，不会停。多放几个能源仓、各接一路电，就能跑满。换成 {item:iuce:energy_hatch_hv} 则并行数从 16 变成 64，见 [轧制联合机](machines/rolling_mill)。

> [!WARN]
> 输出仓放不下一整批产物时联合厂会停机等待，不会丢东西。记得把成品运走。

> [!NOTE]
> 成型以后只有控制器在运算，外壳再多也不增加负担；只有在厂房范围内放置或破坏方块时才会重新检查结构。
