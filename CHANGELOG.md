# Changelog / 更新日志

## v1.0.0 — Unreleased / 未发布

### English

1. Added: A Metal Former for rolling plates, cutting cables, and extruding rods.
2. Added: Twelve machine components, including motors, coils, gears, and rotors.
3. Improved: Machine recipes now follow progressive production tiers.
4. Improved: Compressors densify materials, induction furnaces make alloys, and electrolyzers handle plating and energium dust.
5. Improved: Machine energy costs are calculated per operation so low-voltage machines run correctly.
6. Fixed: The macerator and electric furnace could duplicate ingots indefinitely.
7. Fixed: Compressor plate and rod recipes conflicted for the same ingot.
8. Fixed: Industrial workbench circuit recipes used fewer materials while producing more items.
9. Fixed: The industrial workbench could not match shaped recipes.
10. Removed: Ingot-to-purified-dust macerator recipes and electrolyzer ore-washing recipes.
11. Fixed: Coolant block registration could prevent the mod from loading.
12. Fixed: Adjacent generators and storage units now supply machines without requiring cables.
13. Fixed: Storage charging slots accept electric tools and correctly save transferred energy.
14. Added: Both wrenches rotate machines, toggle cable connections, and dismantle blocks while sneaking.
15. Added: Electric furnaces support furnace, blast furnace, and smoker recipes.
16. Improved: Electric furnaces produce two ingots from crushed dust and three from purified dust.
17. Added: Industrial workbenches support vanilla crafting recipes and return containers such as empty buckets.
18. Fixed: Industrial workbench JEI tabs now use the correct title, icon, and shaped-recipe layout.
19. Added: Planters recognize more vanilla and modded crops and allow custom cultivation registrations.
20. Improved: Crop discovery is cached on demand without scanning all items during startup.
21. Added: A Transformer that steps higher voltages down to a selectable LV, MV, HV, or EV output.
22. Added: Planters grow saplings into logs and propagate flowers, mushrooms, vines, and other plants.
23. Improved: Machines and storage units only accept power at or below their own voltage tier, so higher-tier sources need a Transformer.
24. Added: Live Transformers shock and stun creatures standing in water or rain within 10 blocks, scaling with voltage.
25. Fixed: Looking at a machine holding more energy than its capacity could crash the game with Jade installed.
26. Added: A built-in guide book with searchable pages, live recipes, process diagrams, and multiblock previews.
27. Added: Press G to open the guide, or hover a mod item and press G to jump to its page.
28. Added: New players receive a guide book the first time they join a world.
29. Improved: The guide book is now a thick 3D book when held or dropped, with a redrawn inventory icon.
30. Fixed: Overclocker upgrades could not be placed in machine upgrade slots.
31. Added: Right-click a machine with any upgrade to install it without opening the interface.
32. Improved: Upgrades now stack up to 16 in the inventory.
33. Improved: The Planter's JEI page now lists every crop it can grow automatically, including modded ones.
34. Improved: The guide book's inventory icon is redrawn as a tilted book in the style of vanilla books.
35. Fixed: Overclocker upgrades had no effect on dust and other mod recipes in the Electric Furnace.
36. Fixed: Upgrades placed in the Induction Furnace's upgrade slots did nothing.
37. Fixed: Ejector upgrades could push a machine's raw materials, such as Planter soil, instead of its products.
38. Fixed: Overclocker upgrades added to a running Ore Washer or Electrolyzer only took effect after reloading the world.
39. Fixed: The Geothermal Generator kept producing power for up to a second after its lava ran out.
40. Added: Hoppers and other mods' pipes can feed machines, collect their products, and fill or drain their tanks.
41. Fixed: Machines, cables, ores and all other blocks of the mod dropped nothing when mined.
42. Added: Breaking a machine drops the items and upgrades stored inside it.
43. Optimized: Machine titles, tooltips, messages and JEI category names now follow the game language instead of always being Chinese.
44. Fixed: JEI showed no IUCE recipes when playing on a multiplayer server.
45. Optimized: The three overclocker upgrades now differ (time ×0.75 / ×0.60 / ×0.45, energy ×1.5 / ×1.8 / ×2.2) and can be mixed in one machine.
46. Added: Sneak-right-click a machine with an empty hand to make it return products to the container that fed it, so one AE2 pattern provider can both supply and collect.
47. Optimized: JEI machine pages now show input amounts, fluids, by-products with their chance, and the energy and time per operation.
48. Added: Chute, an unpowered block that takes items from above and passes whole stacks down or sideways.
49. Added: Conveyor I, which carries dropped items along and feeds them into the machine or container at its end.
50. Added: The Industrial Workbench remembers its last recipe and resumes whenever its stock receives materials again.
51. Optimized: The Industrial Workbench crafts in 2 seconds instead of 10.
52. Optimized: The Generator, Metal Former and Industrial Workbench are much cheaper to build, so automation starts earlier.
53. Added: Conveyors climb by turning into a ramp when the next belt is one block higher, and can be rotated with a wrench.
54. Added: Holding a conveyor or chute shows a placement preview with a direction arrow.
55. Optimized: JEI machine pages are more compact and show the by-product chance as a badge on its slot.
56. Fixed: Conveyor ramps did not line up with the belts below and above them.
57. Added: A conveyor fed only from one side is shown as a curve.
58. Optimized: The placement preview now shows the half-transparent block with a white frame and a yellow direction arrow.
59. Added: Machine recipes can be encoded into Applied Energistics processing patterns with one click from JEI.
60. Added: Conveyors descend: a belt fed from one block higher turns into a down ramp.
61. Fixed: Machines that received less power than they needed still ran at full speed; they now slow down in proportion.
62. Optimized: The Ore Washer and Electrolyzer stop when their by-product slot is full instead of discarding by-products, and no longer use power while waiting.
63. Added: Jade shows when a machine is underpowered or stopped by a full by-product slot.
64. Added: Factory Hub, which rates your factory with stars for keeping it supplied with products minute after minute.
65. Added: One-star and two-star ratings, with a rating screen showing the wanted products, stock and progress.
66. Added: Plate, wire and rod molds; the mold placed in the Metal Former decides what it makes, replacing the mode button.
67. Optimized: Existing Metal Formers receive the mold matching their previous mode when the world is loaded.
68. Added: Pneumatic Pipes and Logistics Interface I, a network that brings each machine what its recipes can use and carries its products away.
69. Added: Logistics interfaces can be set to receive only or supply only, and filtered to a single item.
70. Added: Silo, which stores 64 stacks of one item.
71. Added: Fluid Tank, which stores 16 buckets of any one fluid.
72. Added: Configurator, which copies the settings of one logistics interface to others.
73. Added: Ore Processing Plant, a 5×5×5 multiblock that crushes, washes and smelts in one step, 16 at a time and at half the energy per item.
74. Added: Plant Casing, Item Input Hatch, Item Output Hatch and Energy Hatch for building large machines.
75. Added: Logistics interfaces on hatches feed a large machine what it can process and carry its products away.
76. Added: Rolling Mill, a multiblock 9 to 17 blocks long whose mold decides between plates, wires and rods; a longer mill runs more at once.
77. Added: HV Energy Hatch, which raises a large machine from 16 to 64 parallel operations.
78. Added: Aiming at an unfinished large machine's controller outlines the blocks still missing, and right-clicking it with Plant Casing places them.
79. Added: Electric Repair Bench, which restores the durability of any tool or armour with electricity.
80. Added: Electric Lamp.
81. Added: Energy Converter, which turns EU into Forge Energy for other mods' machines.
82. Added: A logistics interface works as a container holding its whole network's stored items, so hoppers, other pipes or an Applied Energistics storage bus can use it.
83. Added: Builder I, which places a large machine's casing by itself and asks the pipe network for exactly the blocks still missing.
84. Added: Blueprint Terminal, which lists what a large machine still needs and sets the length of the Rolling Mill.
85. Added: The pipe network carries fluids from pumps and tanks to machines.
86. Added: Dyeing a logistics interface puts it on a colour channel, so one pipe run can serve several separate lines.
87. Optimized: A pipe network with nothing to move checks less often and resumes at full speed when items arrive.
88. Added: An optional brushed-steel resource pack for the Ore Processing Plant and Rolling Mill.

### 中文

1. 新增：金属成型机，支持辊压板材、切割导线和挤压金属棍。
2. 新增：电动马达、线圈、齿轮、转子等十二种机械零件。
3. 优化：机器配方按产线等级逐步解锁。
4. 优化：压缩机负责致密化，感应炉负责合金，电解机负责电镀与能量粉。
5. 优化：机器耗电按单次加工计算，低压机器可正常供电运行。
6. 修复：锭经过打粉机和电炉可无限复制的问题。
7. 修复：压缩机中同一种锭的板材与金属棍配方冲突。
8. 修复：工业工作台电路配方用料更少却产出更多的问题。
9. 修复：工业工作台无法识别有序配方的问题。
10. 移除：打粉机的锭磨纯净粉配方和电解机的洗矿配方。
11. 修复：冷却液方块注册失败导致模组无法加载的问题。
12. 修复：发电设备和储电箱可直接为相邻机器供电。
13. 修复：储电箱充电槽可放入电动工具并正确保存充入的电量。
14. 新增：两种扳手可调整机器朝向、切换线缆连接，并在潜行时拆卸方块。
15. 新增：电炉支持熔炉、高炉和烟熏炉配方。
16. 优化：粗粉经电炉产出两锭，纯净粉产出三锭。
17. 新增：工业工作台支持原版合成配方并返还空桶等容器。
18. 修复：工业工作台JEI分类名称、图标和有序配方布局错误。
19. 新增：种植机自动识别更多原版及模组作物，并支持自定义培育注册。
20. 优化：作物按需识别并缓存，避免进入游戏时扫描全部物品。
21. 新增：变压器，可将高电压降为可切换的低压、中压、高压或超高压输出。
22. 新增：种植机可将树苗培育为原木，并可繁殖花、蘑菇、藤蔓等植物。
23. 优化：机器和储电箱只接受不高于自身电压等级的电力，高等级电源需经变压器降压。
24. 新增：带电变压器会电击并定身周围 10 格内处于水中或淋雨的生物，强度随电压等级提高。
25. 修复：安装 Jade 时查看电量超过容量的机器可能导致游戏崩溃的问题。
26. 新增：内置指南，支持搜索、实时配方、流程图和多方块结构预览。
27. 新增：按 G 打开指南，悬停模组物品按 G 可直接跳到对应页面。
28. 新增：玩家首次进入世界时会获得一本指南。
29. 优化：指南在手持和掉落时显示为有厚度的立体书本，并重绘了物品栏图标。
30. 修复：超频升级无法放入机器升级槽的问题。
31. 新增：手持任意升级右键机器即可直接安装，无需打开界面。
32. 优化：升级在物品栏中可堆叠至 16 个。
33. 优化：种植机的 JEI 页面会自动列出全部可种植的作物，包括其他模组的作物。
34. 优化：指南的物品栏图标重绘为原版书本风格的斜放立体书。
35. 修复：电炉加工矿粉等模组配方时超频升级不生效的问题。
36. 修复：感应炉升级槽里的升级不起作用的问题。
37. 修复：弹出升级会把机器的原料（如种植机的土壤）当成产物送走的问题。
38. 修复：给运行中的洗矿机或电解机加装超频升级后要重进世界才生效的问题。
39. 修复：地热发电机的熔岩烧完后仍会继续发电最多一秒的问题。
40. 新增：漏斗和其他模组的管道可以给机器送料、取走产物，并为储罐注入或抽出流体。
41. 修复：机器、线缆、矿石等本模组方块被挖掉后不掉落任何东西的问题。
42. 新增：机器被破坏时会掉出内部存放的物品和升级。
43. 优化：机器标题、提示、消息和 JEI 分类名称会跟随游戏语言显示，不再固定为中文。
44. 修复：在多人服务器中 JEI 不显示本模组配方的问题。
45. 优化：三种超频升级效果不再相同（时间 ×0.75 / ×0.60 / ×0.45，耗电 ×1.5 / ×1.8 / ×2.2），可在同一台机器里混装。
46. 新增：空手潜行右键机器可开启“产物退回进料面”，一个应用能源样板供应器即可同时发料和回收。
47. 优化：JEI 机器配方页会显示原料数量、流体、副产物及概率，以及每次加工的耗电和时间。
48. 新增：滑槽，不用电，从上方取物品并整组送往下方或侧面。
49. 新增：传送带 I，运送掉落物并送入末端的机器或容器。
50. 新增：工业工作台会记住上一次的配方，原料仓再有原料时自动继续合成。
51. 优化：工业工作台每次合成由 10 秒缩短为 2 秒。
52. 优化：火力发电机、金属成型机和工业工作台的造价大幅降低，更早用上自动化。
53. 新增：下一格传送带高一格时传送带会自动变成斜坡爬升，并可用扳手转向。
54. 新增：手持传送带或滑槽时显示摆放预览和方向箭头。
55. 优化：JEI 机器配方页排版更紧凑，副产物概率显示为槽位角标。
56. 修复：传送带斜坡与上下两格传送带接不上的问题。
57. 新增：只从侧面进料的传送带会显示为弯道。
58. 优化：摆放预览改为半透明方块模型，带白色线框和黄色方向箭头。
59. 新增：机器配方可在 JEI 中一键编码为应用能源的处理样板。
60. 新增：传送带可以下坡，被高一格的传送带送料时会自动变成下行斜坡。
61. 修复：机器供电不足时仍按全速运行的问题，现在会按实际供电比例降速。
62. 优化：洗矿机和电解机的副产物槽满时停机而不再丢弃副产物，等待期间不再耗电。
63. 新增：Jade 会提示机器供电不足或被副产物堵住。
64. 新增：工厂中枢，根据产线能否持续不断地供应产品给工厂评星。
65. 新增：一星和二星评级，评级界面会显示认可的产品、库存和进度。
66. 新增：板材、导线、棍材三种模具，金属成型机放入哪种模具就做哪种产品，取代原来的模式按钮。
67. 优化：读取旧存档时，已有的金属成型机会自动获得与原模式对应的模具。
68. 新增：气动管和物流接口 I，组成的管网会给每台机器送去它用得上的原料，并运走产物。
69. 新增：物流接口可设为只进或只出，并可限定只通过一种物品。
70. 新增：料仓，可存放 64 组同一种物品。
71. 新增：储罐，可存放 16 桶任意一种流体。
72. 新增：配置器，可把一个物流接口的设置复制到其他接口。
73. 新增：选矿联合厂，5×5×5 的多方块大机器，打粉、洗矿、熔炼一步完成，一次 16 份，单件耗电减半。
74. 新增：厂房外壳、物品输入仓、物品输出仓和能源仓，用于搭建大机器。
75. 新增：物流接口贴在仓口上，会给大机器送去它能加工的原料并运走产物。
76. 新增：轧制联合机，长 9 到 17 格的多方块大机器，放入的模具决定出板、导线还是棍，越长并行越多。
77. 新增：高压能源仓，可把大机器的并行数从 16 提高到 64。
78. 新增：准星对着未完成的大机器控制器会标出还缺的方块，手持厂房外壳右键控制器可一键补齐。
79. 新增：电动修理台，用电修复任何工具和护甲的耐久。
80. 新增：电灯。
81. 新增：能量转换器，把 EU 转换成其他模组通用的能量。
82. 新增：物流接口可当作容器使用，里面是整个管网的库存，漏斗、其他管道或应用能源的存储总线都能接入。
83. 新增：施工机 I，自动摆放大机器的外壳，并只向管网索要还缺的数量。
84. 新增：图纸终端，列出大机器还缺的材料，并可设定轧制联合机的长度。
85. 新增：管网可以把泵和储罐里的流体送给机器。
86. 新增：给物流接口染色可划分频道，一组管道能同时服务互不相干的几条线。
87. 优化：没有东西可运的管网会降低检查频率，有物品时立即恢复全速。
88. 新增：选矿联合厂与轧制联合机的可选拉丝钢资源包。
