---
title: 矿物处理
category: materials
order: 20
icon: iuce:purified_tin_dust
items: iuce:crushed_tin_dust, iuce:crushed_lead_dust, iuce:crushed_zinc_dust, iuce:crushed_aluminium_dust, iuce:crushed_nickel_dust, iuce:crushed_silver_dust, iuce:crushed_cobalt_dust, iuce:crushed_uranium_dust, iuce:purified_tin_dust, iuce:purified_lead_dust, iuce:purified_zinc_dust, iuce:purified_aluminium_dust, iuce:purified_nickel_dust, iuce:purified_silver_dust, iuce:purified_cobalt_dust, iuce:purified_uranium_dust, iuce:copper_dust
---

同一块矿石，走的工序越完整，得到的金属越多。

## 三条路线

| 路线 | 1 块矿石得到 |
| --- | --- |
| 直接进熔炉 | 1 锭 |
| 打粉 → 电炉 | 4 锭 |
| 打粉 → 洗矿 → 电炉 | 6 锭，外加伴生金属粒 |

## 完整产线

{flow: iuce:tin_ore -> @iuce:macerator -> iuce:crushed_tin_dust*2 -> @iuce:ore_washer -> iuce:purified_tin_dust*2 -> @iuce:electric_furnace -> iuce:tin_ingot*6}

1. [打粉机](machines/macerator) 把矿石磨成 2 份粗粉。
2. [洗矿机](machines/ore_washer) 每次用 1000 mB 水把粗粉洗成纯净粉，并有 50% 概率带出一粒伴生金属。
3. [电炉](machines/electric_furnace) 把纯净粉炼成 3 锭（粗粉只有 2 锭）。

> [!NOTE]
> 增产只在电炉里兑现。粗粉和纯净粉放进普通熔炉都只出 1 锭。

## 伴生金属

| 洗的是 | 带出 |
| --- | --- |
| 锡 | 锌粒 |
| 锌 | 锡粒 |
| 铅 | 银粒 |
| 银 | 铅粒 |
| 铝 | 钴粒 |
| 钴 | 镍粒 |
| 镍 | 钴粒 |
| 铀 | 铅粒 |

9 个金属粒合成 1 个锭。缺银、缺钴的时候，多洗铅和铝。

## 铜

原版铜矿直接磨成 {item:iuce:copper_dust}，不需要洗矿，进电炉 1 粉 1 锭。铜粉也是合金粉路线的原料。
