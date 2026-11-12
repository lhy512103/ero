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

{structure: ore_plant | 5×5×5 的空心外壳，控制器在正面中间、离地一格}

- 外壳用 {item:iuce:plant_casing}，里面是空的，可以走进去。
- 控制器的屏幕朝外。
- **仓口可以替换任意一块外壳**，至少各要一个：{item:iuce:energy_hatch}、{item:iuce:item_input_hatch}、{item:iuce:item_output_hatch}。想要更大的缓冲就多放几个。
- 搭好后右键控制器：成型了屏幕会变绿；没成型会告诉你哪里不对。
- **搭建投影**：先放控制器，准星对着它，还缺方块的位置会用黄色线框标出来，放错方块的位置是红色。
- **一键补外壳**：手持 {item:iuce:plant_casing} 右键控制器，会把手里的外壳自动放到所有空着的位置。仓口先摆好，剩下的交给它。更省事的办法见 [搭建大机器的工具](machines/building)。

{recipe: iuce:plant_casing, iuce:plant_controller}

{recipe: iuce:item_input_hatch, iuce:item_output_hatch, iuce:energy_hatch}

## 进出料

- 手持物品右键输入仓放入，空手右键仓口取出一组。
- 仓口接受漏斗、滑槽、传送带和其他模组的管道：输入仓只进，输出仓只出。
- 把 [物流接口](automation/pipes) 贴在仓口上，管网会自动把能加工的矿石送进输入仓，把成品从输出仓运走。

## 供电

能源仓是中压（MV）设备，每 tick 最多收 128 EU。满负荷加工矿石时要的电比这多，联合厂会按实际供电降速，不会停。多放几个能源仓、各接一路电，就能跑满。换成 {item:iuce:energy_hatch_hv} 则并行数从 16 变成 64，见 [轧制联合机](machines/rolling_mill)。

> [!WARN]
> 输出仓放不下一整批产物时联合厂会停机等待，不会丢东西。记得把成品运走。

> [!NOTE]
> 成型以后只有控制器在运算，外壳再多也不增加负担；只有在厂房范围内放置或破坏方块时才会重新检查结构。
