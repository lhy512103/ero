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
89. Redesigned the Ore Processing Plant and Rolling Mill as a tall processing line and an open gantry mill with connected casing trim.
90. Added industrial panels for plant controllers, item hatches and energy hatches.
91. Fixed overlapping metal and glass surfaces that flickered on the Ore Processing Plant.
92. Improved: Large machine controllers and hatches now use steel control panels with status lamps, energy gauges, and progress bars.
93. Added: A formed Rolling Mill now shows an assembled mill with a reheating furnace, roll stands and a coiler that grows with its length.
94. Improved: Guide structure views are drawn in 3D with correct block orientation and can be rotated by dragging.
95. Added: Guide structure views for large machines can switch to show the formed machine.
96. Improved: The Factory Hub screen uses the steel control-panel style, with star badges, stock gauges and a count of products already stocked.
97. Fixed: The Factory Hub screen frame no longer shows the world through stripes in its steel casing.
98. Added: A biological route from potatoes and sugar cane to ethylene: wet mill, liquefaction tank, saccharification tank, fermenter, distillation column, dehydration reactor, dryer and vent stack.
99. Added: Seven enzymes, cultures and catalysts are bred in the fermenter and wear out into spent catalyst that a macerator recycles.
100. Added: Boilers and cooling towers supply a fixed number of machines each, and pipes carry the supplies to every machine they touch.
101. Added: A boiler or cooling tower that runs dry stops serving at once, and a machine stopped for lack of steam keeps the progress it already made.
102. Added: Ethanol, ethylene, sugar solution, stillage and other biological fluids join the gas and water tags.
103. Added: The vent stack shows what it is throwing away, with a flame on top when it burns flammable gas and a smoke plume otherwise.
104. Fixed: Joined pipes no longer show textures cutting through one another at bends and junctions.
105. Fixed: JEI pages left fluid slots empty and only named the fluid in the tooltip.
106. Fixed: JEI pages drew the catalyst as a second optional ingredient instead of a slot that is not used up.
107. Added: The boiler can run on flammable gas, burning 250 mB per ignition instead of a solid fuel item.
108. Improved: Fluid textures are redrawn as flowing water-like sheets with animated motion.
109. Added: Turn sugar cane into injection-grade or film-grade PLA pellets and recover lactic acid from mother liquor.
110. Added: Write and copy PLA grade cards at a process design table.
111. Added: Dry and injection-mold PLA into eight products with CNC-cut molds and recycle matching sprues.
112. Added: Store goods in portable nine-slot crates, dye studded bricks, and transfer liquids in stackable plastic pails.
113. Added: Process coal, limestone and rock salt into rigid or flexible PVC dry blends.
114. Added: Polymerize bioethylene into polyethylene and recover 98% of the slurry solvent.
115. Added: Compound, color and pelletize plastic strands with a twin-screw extruder.
116. Added: Extrude pipe, film and insulated wire, irrigate fields with drip lines and protect crops with mulch.
117. Added: Smoke natural rubber and vulcanize belts and seals for industrial equipment.
118. Added: Move goods through tier II logistics ports and conveyors, dedicated fluid pipes, bulk silos and gas tanks.
119. Added: Five integrated plants process plastics, coke and carbide or convert fuel into HV power and steam.
120. Added: The plastic industry guide includes production routes, machine recipes and rotatable plant structures.
121. Improved: PLA and PVC line machines, crates, bricks, stones and materials are redrawn, with animated fronts while machines run.
122. Added: Crates open and close their lid with sound effects.
123. Added: The creative tab and JEI list crates and studded bricks in all 16 dye colours.
124. Fixed: Bricks and crates showed the wrong particles when broken.
125. Improved: Crates are redrawn with ribs, a hand slot and a visible interior when open.
126. Improved: The brass ingot texture is redrawn.
127. Improved: Rock salt ore now needs a stone pickaxe or better to drop.
128. Improved: Crates can be placed facing any of the six directions and open like a barrel without an overhanging lid.
129. Improved: Studded bricks can be placed in all six directions and merge seamlessly with adjacent bricks of the same colour and facing.
130. Added: Crafting dyeing recipes for all 16 colours of crates and studded bricks, keeping crate contents intact when re-dyed.
131. Improved: Placing a studded brick or crate against another inherits its facing unless sneaking, making rows and sheets align naturally.
132. Fixed: Middle-click picking, Jade previews and break particles for coloured bricks and crates now match their actual colour.
133. Fixed: Pickaxe mining tags now load properly for all machines and blocks, and mulched farmland mines faster with a shovel; missing break particles on cables and formed plant parts are restored.
134. Improved: Crates now hold 27 slots with a custom industrial GUI matching the electric furnace and compressor panels.
135. Fixed: Upward-facing crates stacked on another crate no longer show missing-texture strips on their front.
136. Added: Structural building blocks — H-beam, checker plate, steel grating, steel stairs, handrail, process pipe, refractory bricks, furnace shell, tank shell and graphite electrode.
137. Added: A Gas Output Hatch; gaseous products of industrial plants now leave through it instead of the fluid output hatch.
138. Improved: All seven multiblocks are built from structural blocks laid out like their formed model, so the unformed frame already resembles the finished machine.
139. Improved: Every multiblock shows a new detailed industrial 3D model once formed, with supported beams, platforms and realistic textures.
140. Added: A climbable Steel Ladder block for multiblock platforms and general building.
141. Improved: Redrew checker plate, steel stairs and steel handrail textures and models to match the industrial steel look.
142. Improved: Redesigned the Twin-Screw Extruder layout and formed model, and replaced oversized bright-blue housings on the Rolling Mill and Ore Processing Plant with cast-steel frames.
143. Improved: Extendable rolling mills, extruders and coke ovens now carry their steel grain and mechanical fittings across repeated sections.
144. Improved: Checker plates, tank shells and coaxial rollers join seamlessly, while perpendicular H-beams and process pipes connect with bolted gussets and flanged elbows.
145. Improved: Redrew structural blocks and formed multiblock materials with clean flat steel surfaces instead of noisy pixel grain, and unified the Rolling Mill's front console, item hatches and corner cabinets.
146. Fixed: Process pipes showing the missing texture.
147. Fixed: Multiblock parts turning invisible like air after the structure was resized or changed.
148. Improved: Redrew structural blocks, controllers, hatches and screw-barrel segments to match the steel look of the process machines.
149. Improved: Redrew the polyethylene, natural rubber and tier-II logistics machines, blocks and items to match the process machine style.
150. Fixed: Thirteen dyeable items, including PE and PVC pipes, plastic pellets, film rolls, drip tape and strands, showing as blank icons in inventories.
151. Fixed: Flickering textures on chimney tops and pipe bands of formed large machines such as the waste heat power plant and coke oven battery.
152. Fixed: Flickering corners on the edge trim of unconnected tank shells and checker plates.
153. Improved: Checker plates now use the same diagonal tread pattern as the formed waste heat power plant deck.
154. Improved: Redrew the tops and sides of screw-barrel segments and hatches to match their fronts, and gave the segments less smooth shading.
155. Improved: Process machine screens are redrawn as bolted steel panels with side tabs for processing, moulds, upgrades, recipe preview, power and machine info.
156. Added: Extruders, cooling tanks, vulcanizers, smokehouses, film winders and haul-off cutters show their own animated progress pictures.
157. Improved: Process machine screens return to the light vanilla look, and the single screw extruder shows a picture of itself with the melt advancing through its barrel.
158. Improved: Cooling tanks, vulcanizers, smokehouses, film winders, haul-off cutters and CNC machining centres show pictures of themselves, with the work animated inside the machine.
159. Improved: Polymerization reactors, distillation columns, lime kilns and fermenters show pictures of themselves, with their tanks drawn into the machine and the work animated inside it.
160. Fixed: Assembled multiblocks no longer cast shadows or block light from their hidden parts.
161. Improved: Steel grating gets an edge frame and steel ladders get proper side rails and rungs.
162. Improved: The ore processing plant roasts in a rotary drum on a cradle under a refractory brick hood, and the bio ethylene plant column platform gets a checker plate deck with a gap in the handrail for the ladder.
163. Improved: Additive reactors, catalyst prep tanks, dehydration, depolymerization and oligomerization reactors, VCM converters, neutralizers and acetylene generators show pictures of themselves, with their tanks drawn into the machine and the work animated inside it.
164. Improved: Solvent recovery towers, tar distillers, scrubbers, flash tanks, evaporators, crystallizers and vent stacks show pictures of themselves, with their tanks drawn into the machine and the work animated inside it.
165. Improved: Carbide furnaces, coking ovens, HCl synthesis furnaces and dryers show pictures of themselves, with their tanks drawn into the machine and the work animated inside it.
166. Improved: Brine dissolvers, brine purifiers, coagulation tanks, liquefaction tanks, saccharification tanks, water treatment units, centrifuges, filter presses, membrane cells, wet mills, gas compressors, strand pelletizers and sheeting presses show pictures of themselves, with their tanks drawn into the machine and the work animated inside it.
167. Improved: Compounding extruders, dehumidifying dryers, high speed mixers, injection molders, plastic granulators and rubber mixers show pictures of themselves, with the work animated inside the machine.
168. Improved: The macerator, compressor, extractor, metal former, electric furnace and induction furnace screens show a picture of the machine with its work animated inside it.
169. Improved: The generator, energy storage, ore washer, electrolyzer and planter screens show a picture of the machine with their fuel, charge, water and crop shown inside it.
170. Improved: The industrial workbench, crate, factory hub and multiblock controller and hatch screens use the same new style, with the workbench showing a picture of itself.
171. Improved: Machine recipe pages in JEI show the machine's picture with the slots and tanks in place and the run animated inside it.
172. Improved: JEI machine and multiblock recipe pages use the machine screens' light panel, fit their contents, mark input, output and catalyst slots by colour, and show energy and time as tooltips on the gauge and progress window; the ore processing plant and rolling mill are listed on the matching single-block machine pages.
173. Improved: The waste heat power plant, twin screw extruder, bio-ethylene plant, carbide furnace plant and coke oven battery have redrawn fronts that match the other machines, and the plant and mill controllers have framed tops and backs.
174. Fixed: The formed rolling mill, twin screw extruder and coke oven battery use the ore processing plant's tread floor, and JEI recipe pages with a single recipe are centred instead of left-aligned.
175. Fixed: The macerator guide page shows the current machine picture instead of the old screen texture.
176. Added: A JEI structure tab for large machines that animates the build layer by layer, previews the formed machine, and lists every block needed.
177. Added: Structure manuals, so encoding a structure page in an AE2 pattern terminal gives a fake-crafting pattern for a whole machine.
178. Improved: The JEI structure tab shows one large view with a layer selector, a formed toggle, scroll zoom, drag rotation, and click-to-highlight block names.
179. Improved: The JEI structure tab picks the exact block under the cursor, turns up and down as well as around, outlines the picked block through other blocks in shifting bright colours, and uses redrawn steel buttons.

180. Fixed: JEI structure previews select the visible block under the cursor and show a subtle depth-aware outline.

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
89. 优化：选矿联合厂改为高塔式产线，轧制联合机改为开放式门架，两者均使用连接式外壳收边。
90. 新增：联合厂控制器、物品仓和能源仓可右键打开工业面板。
91. 修复：选矿联合厂钢架、滚筒和观察窗的重叠表面不再争抢显示。
92. 优化：大型机器控制器和各类仓口改用钢板仪表面板，带状态灯、储能表和进度条。
93. 新增：轧制联合机成型后显示整机模型，有加热炉、轧机机架和卷取机，并随长度伸展。
94. 优化：指南里的多方块结构改为 3D 显示，方块朝向正确，可以拖动旋转。
95. 新增：指南里的大型机器结构可以切换查看成型后的样子。
96. 优化：工厂中枢界面改用钢板仪表风格，带星级徽章、库存表和已达标产品计数。
97. 修复：工厂中枢界面的机壳不再有一条条透出背景的横线。
98. 新增：土豆与甘蔗到乙烯的生物产线，包含湿磨机、液化罐、糖化罐、发酵罐、精馏塔、脱水反应器、干燥机与排气塔。
99. 新增：发酵罐可培育七种酶、菌种与催化剂，耗尽的催化剂打粉后可回收。
100. 新增：锅炉与冷却塔各自供应固定台数的机器，管道可把供应送到接触到的每一台机器。
101. 新增：锅炉或冷却塔断供后立即停供，缺蒸汽的机器会保留已有进度。
102. 新增：乙醇、乙烯、糖液、酒糟液等生物流体加入气体与水的标签。
103. 新增：排气塔会显示排出的东西——烧可燃气体时塔顶有火焰，排其它气体时冒烟。
104. 修复：相连的管道在弯头和岔口处出现材质互相穿透的问题。
105. 修复：JEI 配方页的流体槽是空框，只能在提示里看到流体名。
106. 修复：JEI 配方页把催化剂画成了第二种可选原料，其实它不会被消耗。
107. 新增：锅炉可用可燃气体助燃，每 250 mB 顶一次燃烧，与固体燃料共用一个炉膛。
108. 优化：流体贴图按原版水的观感重绘，并带流动动画。
109. 新增：甘蔗可制成注塑级或薄膜级聚乳酸颗粒，结晶母液可回收乳酸。
110. 新增：工艺设计台可写入和复制聚乳酸牌号卡。
111. 新增：干燥聚乳酸可用八种数控模具注塑成型，同源水口料可粉碎回收。
112. 新增：九格周转箱可带库存搬运，凸点积木可染色，塑料桶可堆叠装液。
113. 新增：煤、石灰岩和岩盐可经煤化工与氯碱产线制成硬质或软质聚氯乙烯干混料。
114. 新增：生物乙烯可聚合成聚乙烯，每批可回收 98% 的浆液溶剂。
115. 新增：双螺杆可填充、着色和挤出料条，切粒后保留牌号与颜色。
116. 新增：单螺杆可挤出管、膜和包覆线，滴灌管与地膜可保持耕地湿润。
117. 新增：天然橡胶可经烟熏、密炼和硫化制成皮带与密封圈。
118. 新增：二级物流接口、传送带、独立流体管网、大料仓与气罐可扩大工厂吞吐能力。
119. 新增：五台联合设备可加工塑料、焦炭和电石，或把燃料转成高压电与蒸汽。
120. 新增：塑料工业指南包含产线路线、机器配方和可旋转的多方块结构图。
121. 优化：重绘聚乳酸与聚氯乙烯产线的机器、周转箱、积木、石料和材料贴图，机器运行时正面带动画。
122. 新增：周转箱开合盖时带有动画和音效。
123. 新增：创造模式物品栏与 JEI 中的周转箱和积木显示全部 16 种颜色。
124. 修复：积木和周转箱被破坏时粒子显示错误的问题。
125. 优化：重绘周转箱，增加筋条、提手孔，打开时可看到内部。
126. 优化：重绘黄铜锭贴图。
127. 优化：盐岩矿需要石镐或更好的镐才能掉落。
128. 优化：周转箱支持六向摆放，并采用类似木桶的开盖表现。
129. 优化：积木支持六向摆放，相邻同色同朝向积木会自动无缝连接纹理。
130. 新增：全部 16 种颜色的周转箱与积木染色合成配方，周转箱染色时保留内部物品。
131. 优化：对着已有积木或周转箱摆放时自动沿用其朝向（潜行时按点击面或视角转向），拼装成排或大片时更自然。
132. 修复：彩色积木与周转箱的鼠标中键拾取、Jade 提示图标及破坏粒子始终显示为白色的问题。
133. 修复：因测试方块混入挖掘标签导致全模组机器与方块镐子挖掘失效的问题，补全地膜耕地铲掘标签及线缆、成型多方块零件的破坏粒子。
134. 优化：周转箱扩容至 27 格，并更换为与电炉、压缩机风格一致的工业面板界面。
135. 修复：朝上摆放并叠在其他周转箱上的周转箱正面不再出现紫黑贴图缺失条纹。
136. 新增：H型钢、花纹钢板、钢格栅、钢楼梯、护栏、工艺管道、耐火砖、炉壳、罐壳与石墨电极等结构建筑方块。
137. 新增：气体输出仓，联合装置的气体产物改由其输出，不再走流体输出仓。
138. 优化：七台多方块全部改用结构方块搭建，摆放位置与成型后的模型一一对应。
139. 优化：所有多方块成型后显示全新的工业化 3D 模型，钢梁均有支撑，贴图更贴近真实设备。
140. 新增：可攀爬的钢爬梯方块，可用于多方块平台上下与建筑装饰。
141. 优化：重绘花纹钢板、钢制楼梯与钢管护栏贴图和模型，风格与成型后的工业钢结构一致。
142. 优化：重做双螺杆挤出机的成型前后结构与模型，并将轧机、矿石处理厂等处的亮蓝色大柱子和箱体改为铸钢机架。
143. 优化：轧制联合机、双螺杆挤出机和焦炉组加长后，钢板纹理连续，机架、管路和炉门细节随各段延伸。
144. 优化：花纹钢板、容器壳体与同轴工业滚筒支持连接纹理，垂直相交的 H 型钢与工艺管道会生成螺栓节点板和法兰弯头三通。
145. 优化：结构方块与多方块成型材质改为干净的平整钢面，去掉杂乱噪点，并统一轧制联合机前排控制台、物料仓与四角机柜的外观。
146. 修复：工艺管道显示为缺失贴图的问题。
147. 修复：多方块改变长度或结构后，原有零件被隐藏、看起来像空气方块的问题。
148. 优化：重绘结构方块、多方块控制器、各类仓口与螺杆段贴图，与注塑机等加工机器的钢制风格统一。
149. 优化：重绘聚乙烯加工、天然橡胶与二级物流的机器、方块和物品贴图，与加工机器风格统一。
150. 修复：PE 管、PVC 管、各类塑料粒料、膜卷、滴灌带和料条等 13 种可染色物品在物品栏中显示为空白的问题。
151. 修复：余热电站、焦炉等大型机器成型后烟囱口和管箍处贴图闪烁的问题。
152. 修复：容器壳体和花纹钢板未连接时边缘四角贴图闪烁的问题。
153. 优化：花纹钢板改用与余热电站成型甲板相同的斜纹防滑花纹。
154. 优化：重绘各类螺杆段和输入输出仓的顶面与侧面，与正面风格统一，螺杆段不再过于平滑。
155. 优化：加工机器界面重绘为带螺栓的钢面板，侧边标签页可切换加工、模具、升级、配方预览、能耗和机器介绍。
156. 新增：挤出机、冷却水槽、硫化机、熏烟房、收卷机和牵引切割机显示各自的动态加工进度。
157. 优化：加工机器界面恢复原版浅色风格，单螺杆挤出机显示自身示意图，熔体沿料筒推进表示进度。
158. 优化：冷却水槽、硫化机、熏烟房、收卷机、牵引切割机和数控加工中心显示自身示意图，加工过程在机器内部动态呈现。
159. 优化：聚合釜、精馏塔、石灰窑和发酵罐显示自身示意图，储罐画在机器上，加工过程在机器内部动态呈现。
160. 修复：多方块成型后，被隐藏的部件不再投下阴影或遮挡光照。
161. 优化：钢格栅加上边框，钢爬梯改为带侧梁和踏棍的立体模型。
162. 优化：选矿联合厂的焙烧窑改为托架上的回转筒和耐火砖炉罩，生物乙烯装置的精馏塔平台铺上花纹钢板，护栏留出爬梯口。
163. 优化：助剂反应釜、催化剂配制罐、脱水反应器、解聚釜、低聚釜、氯乙烯转化器、中和槽和乙炔发生器显示自身示意图，储罐画在机器上，加工过程在机器内部动态呈现。
164. 优化：溶剂回收塔、焦油蒸馏釜、洗涤塔、闪蒸罐、浓缩器、结晶器和放空火炬显示自身示意图，储罐画在机器上，加工过程在机器内部动态呈现。
165. 优化：电石炉、炼焦炉、氯化氢合成炉和干燥器显示自身示意图，储罐画在机器上，加工过程在机器内部动态呈现。
166. 优化：化盐槽、精制槽、凝固槽、液化罐、糖化罐、水处理器、离心机、压滤机、离子膜电解槽、湿磨机、气体压缩机、水浴切粒机和压片机显示自身示意图，储罐画在机器上，加工过程在机器内部动态呈现。
167. 优化：双螺杆挤出机、除湿干燥机、高混机、注塑机、塑料粉碎机和密炼机显示自身示意图，加工过程在机器内部动态呈现。
168. 优化：打粉机、压缩机、提取机、金属成型机、电炉和感应炉界面显示自身示意图，加工过程在机器内部动态呈现。
169. 优化：发电机、储电箱、洗矿机、电解机和种植机界面显示自身示意图，燃料、电量、水量和作物生长画在机器内部。
170. 优化：工业工作台、板条箱、工厂中枢与多方块控制器、仓口界面改用同样的新风格，工作台显示自身示意图。
171. 优化：JEI 中的机器配方页显示机器示意图，槽位与储罐摆在原处，加工过程在机器内部动态呈现。
172. 优化：JEI 机器与多方块配方页改用机器界面的浅色面板并贴合内容，输入、输出与催化剂槽位以颜色区分，耗电与耗时改为电量条和进度窗口的提示；矿石处理厂和轧钢厂并入对应单方块机器的配方页。
173. 优化：重绘余热电站、联合双螺杆挤出机、生物乙烯装置、电石联合炉和炼焦炉组的正面贴图使其与其他机器一致，选矿联合厂与轧制联合机控制器的顶部和背面改为带边框的样式。
174. 修复：轧制联合机、联合双螺杆挤出机和炼焦炉组成型后的地基改用选矿联合厂的花纹钢板，只有一个配方的 JEI 配方页改为居中而非靠左。
175. 修复：指南中的打粉机页面改为显示当前的机器示意图，不再使用旧界面贴图。
176. 新增：JEI 大型机器结构标签页，逐层动画演示搭建过程、预览成型效果并列出所需全部方块。
177. 新增：结构手册，在 AE2 样板编码终端中编码结构页即可得到整台机器的假合成样板。
178. 优化：JEI 结构页改为单个大预览，可逐层选择查看、切换成型后效果、滚轮缩放、拖动旋转，并可点击方块高亮显示其名称。
179. 优化：JEI 结构页点选方块改为精确命中光标下的方块，可上下拖动翻转视角，选中方块的彩色流光边框可穿透其他方块显示，按钮改用重绘的钢板贴图。
180. 修复：JEI 结构预览准确选取光标下可见的方块，并以简洁的深度遮挡边框高亮。
