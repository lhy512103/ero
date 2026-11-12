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
