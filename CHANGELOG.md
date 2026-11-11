# 更新日志 / Changelog

## 未发布 / Unreleased

### 2026-11-11

1. 新增：金属成型机，可辊压板材、切割导线、挤压金属棍。
   Added: Metal Former, which rolls plates, cuts cables and extrudes rods.
2. 新增：电动马达、线圈、齿轮、转子、磁铁、电极等 12 种机械零件。
   Added: 12 machine components such as motors, coils, gears, rotors, magnets and electrodes.
3. 优化：配方按产线门槛重做，每一级机器都需要上一级产线的产物。
   Improved: Recipes are reworked into production tiers, so each machine tier needs products of the previous one.
4. 优化：各机器职责分明，压缩机只做致密化，感应炉负责全部合金，电解机负责电镀与能量粉。
   Improved: Each machine now has one role: the compressor densifies, the induction furnace makes all alloys, and the electrolyzer handles plating and energium dust.
5. 优化：机器耗电改为按单次加工总量计算，低压机器可以正常供电运行。
   Improved: Machine energy use is now counted per operation, so low-voltage machines can be powered normally.
6. 修复：锭经打粉机和电炉可以无限复制的问题。
   Fixed: Ingots could be duplicated endlessly through the macerator and electric furnace.
7. 修复：压缩机中同一种锭的板材与金属棍配方互相冲突的问题。
   Fixed: Plate and rod recipes for the same ingot conflicted in the compressor.
8. 修复：电路在工业工作台中用料更少却产出更多的问题。
   Fixed: Circuits cost less yet yielded more in the industrial workbench.
9. 修复：工业工作台无法识别有序摆放配方的问题。
   Fixed: The industrial workbench could not match shaped recipes.
10. 移除：打粉机的锭磨纯净粉配方，以及电解机的洗矿配方。
    Removed: Ingot-to-purified-dust recipes in the macerator and ore refining recipes in the electrolyzer.
