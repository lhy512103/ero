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

{structure: ore_plant | 宽 5、高 6、长 7；包含重型钢制底座、双辊破碎塔、水平洗选滚筒与尾部煅烧炉}

- 用 {item:iuce:plant_casing} 搭底座与主体壳体，{item:iuce:plant_frame} 做龙门承重柱与横梁，{item:iuce:plant_drum} 做破碎辊筒、洗选转筒与排烟筒。梁和滚筒沿轴放置：从端面接放下一段即可延长。
- 控制器的屏幕朝外，紧贴地面（无需向下挖坑）。
- 面朝控制器时，左为输入仓、右为输出仓；能源仓在末端正中，外缘另有可选仓位。旧版矩形厂房继续采用原有规则。
- 搭好后右键控制器：成型后会整体切换为完整的三维重型工业机组，运行期间破碎双辊与洗选滚筒会持续转动；没成型会告诉你哪里不对。
- **搭建投影**：未成型时准星对准控制器，会在空位渲染半透明工业方块虚影与线框（黄色为外壳，蓝色为专用部件/仓口，红色为阻塞或放错）。
- **一键补外壳 / 创造一键搭建**：手持 {item:iuce:plant_casing} 右键控制器会自动补全所有外壳；创造模式下空手潜行右键未成型的控制器即可一键补齐全部结构（含外壳、梁、滚筒与仓口）并直接成型。更省事的办法见 [搭建大机器的工具](machines/building)。

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
