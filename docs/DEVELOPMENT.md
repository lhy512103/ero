# Development and regression checks

## Commands

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer -PregressionTests
.\gradlew.bat runClient -PsmokeTest
.\gradlew.bat validateContent
```

The regression source set is loaded only with `-PregressionTests` and is not included in the release JAR. Tests use the separate `run-regression` directory; client smoke tests use `run-smoke`. Neither uses the normal development world's saves.

Besides one process / power-loss / overclock / save-load test per processing machine (`MachineTests`, plus generators, storage, the pump and slot roles), the IUCE GameTests cover real-tick power delivery (adjacent, through cables, and from high-tier sources to low-tier machines through a transformer), transformer voltage rules and energy conservation, the wet-creature shock hazard, adjacent energy conservation, storage output, cable disconnection, charging across all storage tiers, transactional item energy rollback, both wrenches, cooking/powder recipes, crafting remainders, and crop discovery/extensions. Vanilla may add its own tests to the reported total.

## Verification record

Validated on 2026-11-11 with NeoForge 26.1.2.126:

- `build`: passed; the release JAR contains no regression classes or test structures.
- Default GameTest runtime: all 11 required tests passed (10 IUCE tests plus a built-in test).
- `-PcompatibilityRuntime`: all 11 required tests passed.
- `-PcompatibilityRuntime -PecoRuntime`: all 198 required tests passed, including tests supplied by dependencies.
- Default client smoke test: completed resource loading and displayed the first-start welcome screen; no existing player world was opened.
- All 16 metal powder recipes in the packaged JAR have the expected 2/3-ingot yields.

These results validate IUCE's tested behaviors, not every feature in optional third-party mods. The imported runtime combinations still log upstream issues, including Jade's shearable-block loot lookup and optional AdvancedAE/NeoEcoAE recipe decoding or missing optional serializers. The client also reports pre-existing IUCE model/texture warnings unrelated to this change. Those warnings were not silently suppressed or treated as fixed.

## Development runtime dependencies

The default runtime and client-only runtime follow the sibling `ae2utility-neoforge-26.1.2` project; Jade remains available for IUCE's existing integration. These mods are development runtime dependencies, not bundled into IUCE or required by its published metadata.

The sibling project's optional combinations remain opt-in:

```powershell
.\gradlew.bat runClient -PcompatibilityRuntime
.\gradlew.bat runClient -PcompatibilityRuntime -PecoRuntime
```

`ecoRuntime` replaces the optional AE2 Lightning Tech combination with the sibling project's NeoEcoAE combination. Client-only visual/input mods are not added to dedicated server runs. Dependency-coordinate changes belong here or in the build configuration, not in the player changelog.

## 种植机扩展

优先级为：显式 `iuce:machine` 种植配方 → 按物品注册的培育方案 → 扩展识别器 → 自动识别。

- 自动识别常见 `CropBlock` 作物、具有年龄属性的植物，以及瓜类、甘蔗、仙人掌等特殊原版植物。
- 树苗、红树胎生苗和下界菌按命名约定产出同命名空间的 `_log` / `_stem` / `_wood`（找不到时产出自身），模组树木同样适用。
- 没有生长阶段的植物（花、蘑菇、藤蔓、睡莲、垂根等）每 600 tick 繁殖出一个自身；草方块、苔藓块等土壤类方块不会被识别。
- 自动培育保留种子，每轮产出一个常见主产物，不批量生成随机稀有副产物；现有数据配方保留其输入消耗和产量规则。
- 普通年龄型作物基准时间为生长阶段数乘以 25 tick，限制在 100–1200 tick，基准耗电为 2 EU/t，并应用现有昼夜和超频倍率。
- 首次使用一种种子时最多查询四次成熟方块掉落来识别主产物；结果与不支持的输入都会缓存，不在启动时遍历物品注册表，也不在世界里放置或催熟方块。
- 特殊作物的自定义生长、方块实体、世界环境条件或特殊掉落可能无法自动推断，应使用数据配方或注册接口显式适配。
- JEI 的种植机页面除数据配方外，还会列出自动识别的全部作物（`CropRegistry.listAll`，在加载配方时遍历一次物品注册表）；这些条目显示「种子 → 产物」，实际培育时种子不消耗。
- 自动识别不是对所有模组生长逻辑的精确模拟；土壤槽的材料消耗由显式双输入配方决定。

### 数据配方

在数据包的 `data/<namespace>/recipe/` 中添加配方；`energy` 是每轮总耗电，`time` 是基准 tick 数：

```json
{
  "type": "iuce:machine",
  "machine_type": "planter",
  "input": "example:crop_seed",
  "result": { "id": "example:crop", "count": 2 },
  "energy": 400,
  "time": 200
}
```

### Java 注册

在 common setup 的 `enqueueWork` 中调用：

```java
CropRegistry.register(seedItem, new ItemStack(cropItem, 2), 200, 2);
```

这里最后一个参数是 **EU/t**，与 JSON 的每轮总耗电字段不同。更复杂的种子可使用 `CropRegistry.registerResolver(context -> Optional<Profile>)`；识别结果按种子物品及组件缓存，因此识别器应返回与机器位置无关的稳定方案。注册变更和数据包重载会使缓存失效。

## 玩家操作

- 普通右键机器：打开界面；持扳手右键：将机器朝向点击面，无法使用该方向时切换到下一个合法方向。
- 持扳手右键线缆：关闭或恢复点击面的连接；断开状态会保存。
- 持扳手潜行右键：拆卸机器或线缆，并掉落机器内部物品；机器内部电量和加工进度不保留。
- 电动扳手转向或切换连接消耗 50 EU，拆卸消耗 500 EU；可放入储电箱的充电槽充电。
- 工业工作台支持原版有序、无序配方及空桶等余物；余物无处存放时整次合成取消，不删除材料或复制产物。

## 电压规则与变压器

- 机器和储电箱只接受**不高于**自身电压等级的电源；更高等级的电源必须经变压器降压，否则不传输（不会爆炸）。
- 变压器只接受高于其输出等级的输入，空手右键在 LV / MV / HV / EV 间切换输出；每 tick 吞吐为输出电压的 4 倍，1:1 无损耗。
- 变压器只给**同等级**储电箱充电，因此「储电箱 → 变压器 → 储电箱」不会形成循环。
- 传输顺序见 `EnergyTransferManager.PASSES`：发电机 → 机器 / 变压器 / 储电箱，储电箱 → 机器 / 变压器，变压器 → 机器 / 储电箱。
- 变压器内有电时每 40 tick 电击一次周围 10 格内处于水中或淋雨的生物（创造模式玩家除外）：伤害 = 电压等级序号（LV 1 … EV 4，取输入侧与输出侧较高者），定身时间 = 20 + 10 × 序号 tick。伤害类型为 `iuce:electric`。

## 内置指南

页面格式、指令、多方块结构数据和截图自检命令见 [GUIDE.md](GUIDE.md)。

## 内容校验

`tools/validate_content.py`（只用 Python 标准库）静态检查全部数据与资源，`gradlew check` 和 `gradlew build` 会自动运行它：

- 所有资源 JSON 能解析；
- 配方：物品 ID 存在、同一机器没有输入相同的两条配方、每个物品都能做出来、每个物品都有用途；
- 资源：每个物品都有模型定义，以及中英文名称；
- 指南：页面里的物品 ID、页面链接、结构引用都能对上。

新增「终端物品」（机器、工具、装饰等不作为原料的东西）时，把它加进脚本里的 `TERMINAL`；新增机器类型时更新 `STATION`。

- 没装 Python 的机器：`gradlew build -PskipContentValidation`；Python 不叫 `python` 时：`-PpythonCommand=python3`。
- `python tools/validate_content.py --verbose` 额外打印每个物品最早能在第几轮做出来。
- `python tools/dump_recipes.py` 把全部配方按一行一条打印出来，调数值时用。

## 机器基类

新增机器时先看能不能套用下面的基类，只写它自己的配方逻辑：

| 基类 | 适用 | 已使用 |
| --- | --- | --- |
| `machine.SingleInputMachineBlockEntity` + `SingleInputMachineMenu` | 一种原料进、一种产物出，跑 `iuce:machine` 配方 | 打粉机、压缩机、提取机、金属成型机 |
| `machine.PoweredMachineBlockEntity` | 其他耗电的加工机：提供电量缓冲、物品槽、进度、超频计数、运行状态和存档 | 洗矿机、电解机、感应炉、电炉、种植机 |
| `machine.SolarBlockEntity` | 太阳能发电机各等级 | 六级太阳能 |
| `machine.storage.StorageBlockEntity` | 储电箱各等级 | 储电箱、CESU、MFE、MFSU |

- 套用 `SingleInputMachineBlockEntity` 的机器只需要给出配方类型、名称和界面，约 30 行（参考 `CompressorBlockEntity`）。
- 槽位布局不是「前一半输入、后一半输出」的机器，要重写 `getInputSlots()` / `getOutputSlots()`，物品传输升级靠它们区分原料和产物。
- 存档字段（`energy`、`progress`、`maxProgress`、物品）与迁移前一致，旧存档可直接读取。
- 发电机、地热发电机、泵、工业工作台还没有迁移：泵和地热会在流体系统里重写，工作台会在装配台改造里重写。

每台加工机在 `MachineTests` 里有一组测试（加工、断电、超频、后装超频、存档往返）。新增机器时在 `cases()` 和 `CASE_NAMES` 里各加一项。

## 流体

- `fluid.MachineTank`：机器内部的流体储罐（单格、固定容量、可限定流体种类），本身就是标准的 NeoForge 流体接口。
- `fluid.FluidProvider`：带一个储罐的机器实现它；`insertFluid` / `extractFluid` / `addBucket` 是机器之间和流体升级使用的简化接口，默认实现都落在储罐上。
- 洗矿机、电解机、泵用水罐（16 000 mB），地热发电机用熔岩罐（64 桶）。
- 旧存档兼容：以前这些机器只存一个数字（`storedWater`、`storedFluid`、`storedLava`），读档时找不到储罐数据就按旧字段换算。地热发电机旧存档里「正在烧的那一桶」仍计在桶数里，换算时会扣掉。
- 储罐目前还没有对外开放给其他模组的管道，这是阶段 0.6 的内容。
