---
title: 太阳能阶梯
category: power
order: 60
icon: iuce:solar_generator
items: iuce:solar_generator, iuce:advanced_solar_generator, iuce:hybrid_solar_generator, iuce:perfect_hybrid_solar_generator, iuce:quantum_solar_generator, iuce:singulary_solar_generator, iuce:basic_solar_glass, iuce:advanced_solar_glass, iuce:hybrid_solar_glass, iuce:perfect_hybrid_solar_glass, iuce:quantum_solar_glass, iuce:singulary_solar_glass, iuce:plated_shell, iuce:basic_plated_shell, iuce:advanced_plated_shell, iuce:perfect_plated_shell, iuce:photon_plated_shell, iuce:machine_upgrade_0, iuce:machine_upgrade_1, iuce:machine_upgrade_2, iuce:machine_upgrade_3
---

太阳能发电机不烧燃料，但只在**白天**且**头顶能看到天空**时发电。

| 发电机 | 输出 | 电压 |
| --- | --- | --- |
| {item:iuce:solar_generator} | 1 EU/t | LV |
| {item:iuce:advanced_solar_generator} | 8 EU/t | MV |
| {item:iuce:hybrid_solar_generator} | 64 EU/t | HV |
| {item:iuce:perfect_hybrid_solar_generator} | 512 EU/t | EV |
| {item:iuce:quantum_solar_generator} | 4096 EU/t | IV |
| {item:iuce:singulary_solar_generator} | 32768 EU/t | LuV |

> [!WARN]
> 从高级太阳能开始输出就是中压以上，带低压机器要配 [变压器](power/transformer)。

## 每一级怎么造

每一级 = 2 台上一级 + 3 块太阳能玻璃 + 1 个电镀外壳 + 机器升级组件 + 电池或电路板。

{recipe: iuce:solar_generator, iuce:advanced_solar_generator, iuce:hybrid_solar_generator, iuce:perfect_hybrid_solar_generator, iuce:quantum_solar_generator, iuce:singulary_solar_generator}

## 太阳能玻璃

{recipe: iuce:basic_solar_glass, iuce:advanced_solar_glass, iuce:hybrid_solar_glass, iuce:perfect_hybrid_solar_glass, iuce:quantum_solar_glass, iuce:singulary_solar_glass}

## 电镀外壳

基础的 {item:iuce:plated_shell} 只能由 [电解机](machines/electrolyzer) 电镀机器外壳得到，之后逐级包覆金属板。

{recipe: iuce:plated_shell, iuce:basic_plated_shell, iuce:advanced_plated_shell, iuce:perfect_plated_shell, iuce:photon_plated_shell}

## 机器升级组件

{recipe: iuce:machine_upgrade_0, iuce:machine_upgrade_1, iuce:machine_upgrade_2, iuce:machine_upgrade_3}
