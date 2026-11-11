# Development and regression checks

## Commands

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer -PregressionTests
.\gradlew.bat runClient -PsmokeTest
```

The regression source set is loaded only with `-PregressionTests` and is not included in the release JAR. Tests use the separate `run-regression` directory; client smoke tests use `run-smoke`. Neither uses the normal development world's saves.

The thirteen IUCE GameTests cover real-tick power delivery (adjacent, through cables, and from high-tier sources to low-tier machines), adjacent energy conservation, storage output, cable disconnection, charging across all storage tiers, transactional item energy rollback, both wrenches, cooking/powder recipes, crafting remainders, and crop discovery/extensions. Vanilla may add its own tests to the reported total.

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
- 自动培育保留种子，每轮产出一个常见主产物，不批量生成随机稀有副产物；现有数据配方保留其输入消耗和产量规则。
- 普通年龄型作物基准时间为生长阶段数乘以 25 tick，限制在 100–1200 tick，基准耗电为 2 EU/t，并应用现有昼夜和超频倍率。
- 首次使用一种种子时最多查询四次成熟方块掉落来识别主产物；结果与不支持的输入都会缓存，不在启动时遍历物品注册表，也不在世界里放置或催熟方块。
- 特殊作物的自定义生长、方块实体、世界环境条件或特殊掉落可能无法自动推断，应使用数据配方或注册接口显式适配。
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
