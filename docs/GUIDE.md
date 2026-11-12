# 内置指南：编写与扩展

指南完全内置在模组里，不依赖其他模组。游戏内按 **G** 打开；在背包里悬停模组物品按 **G** 跳到对应页面；也可以右键「工业升级指南」物品。

## 文件位置

```
src/main/resources/assets/iuce/guide/
├── zh_cn/_index.json        分类、分类顺序、首页
├── zh_cn/<分类>/<页面>.md    一页一个文件
└── structures/<名字>.json   多方块结构数据
```

- 页面 ID 就是相对路径去掉 `.md`，例如 `machines/macerator`。
- 新增页面只要放一个 `.md` 文件，不用改代码，也不用登记。
- 语言按游戏当前语言加载，找不到时回退到 `zh_cn`。要加英文版，照样建一个 `en_us/` 目录。
- 游戏内按 F3+T 重载资源即可看到改动。

## 页面格式

```markdown
---
title: 打粉机
category: machines          # 对应 _index.json 里的分类 id
order: 10                   # 分类内排序，小的在前
icon: iuce:macerator
items: iuce:macerator       # 逗号分隔；这些物品按 G 时跳到本页
---

正文……
```

### 支持的 Markdown

| 写法 | 效果 |
| --- | --- |
| `# 标题` `## 标题` `### 标题` | 三级标题 |
| `**粗体**` `*斜体*` `` `代码` `` | 行内样式 |
| `[文字](power/basics)` | 跳到另一页 |
| `{item:iuce:rubber}` | 物品名链接，点击跳到该物品的页面 |
| `- 项目` / `1. 项目` | 列表 |
| `\| a \| b \|` | 表格，第一行是表头 |
| `> [!NOTE]` `> [!TIP]` `> [!WARN]` | 说明 / 提示 / 注意 提示框 |
| `---` | 分隔线 |

### 指令（独占一行）

| 指令 | 作用 |
| --- | --- |
| `{recipe: iuce:a, iuce:b}` | 显示产出这些物品的配方，可翻页 |
| `{uses: iuce:a}` | 显示消耗该物品的配方 |
| `{recipes: macerator}` | 显示某种机器类型的全部配方 |
| `{flow: a -> @机器 -> b*2}` | 流程图；`@` 表示机器，`a + b` 表示多个输入，`*n` 表示数量 |
| `{items: a, b*4}` | 一排物品格 |
| `{cards: 页面id, 页面id}` | 页面入口卡片 |
| `{image: gui/machine/macerator.png, u, v, 宽, 高 \| 说明}` | 贴图，路径相对 `textures/`；省略裁剪参数则显示整张 |
| `{structure: 名字 \| 说明}` | 多方块结构示意，带分层查看和材料清单 |
| `{hero: 标题 \| 副标题}` | 首页横幅 |

配方卡片的数据直接来自游戏当前加载的配方，改了配方 JSON 指南会自动跟着变，不需要同步。

## 多方块结构

```json
{
  "palette": { "C": "iuce:reactor_casing", "F": "iuce:fission_controller" },
  "layers": [
    ["CCC", "CCC", "CCC"],
    ["CCC", "C C", "CFC"],
    ["CCC", "CCC", "CCC"]
  ]
}
```

`layers` 从下到上；每层的行从北到南；每行的字符从西到东；空格是空气。

`client.guide.GuideStructure` 把它解析成「坐标 + 方块 ID」的列表（`cells`），指南里的示意图和材料清单都用这份数据。以后做世界内投影或结构校验，直接复用 `GuideStructure.get(名字).cells` 即可。

## 代码结构（`src/main/java/client/guide/`）

| 类 | 职责 |
| --- | --- |
| `Guide` | 入口：按键、物品提示、资源重载、`Guide.open(页面id)` |
| `GuideBook` | 加载索引和页面，搜索，物品 → 页面的映射 |
| `GuideParser` | 把页面文本解析成元素 |
| `GuideBlocks` | 所有页面元素（标题、表格、配方卡片、结构图……） |
| `GuideRecipes` | 配方索引，随配方重载自动重建 |
| `GuideScreen` | 界面：侧栏、搜索、历史、滚动 |
| `RichText` | 行内样式与中文换行 |
| `GuideStyle` / `Ctx` | 配色、绘制辅助、点击区域 |

### 新增一种页面元素

1. 在 `GuideBlocks` 里实现 `Block`（`layout` 返回高度，`draw` 负责绘制）。
2. 在 `GuideParser.directive` 里加一行，把指令名映射到它。

可点击的东西通过 `Ctx` 登记（`linkZone` / `actionZone` / `itemZone`），不用自己处理鼠标。

### 新增一种配方类型

在 `GuideRecipes.view` 里把它转换成 `View`；如果是新的机器类型，再在 `MACHINE_BLOCKS` 里登记机器方块，并在语言文件里加 `guide.iuce.machine.<类型>`。

## 截图自检

```powershell
.\gradlew.bat runClient -PsmokeTest -PregressionTests -PguideShots -PguideWorld=<run-smoke/saves 下的存档名>
```

会自动打开每一页并截图到 `run-smoke/screenshots/guide_*.png`，然后退出。只截部分页面：`-PguideShots=welcome,power/solar`。不带 `-PguideWorld` 时停在标题界面截图，此时配方卡片为空。截图工具在 `regression` 源集里，不会进入发布的 JAR。
