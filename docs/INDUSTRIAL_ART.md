# 多方块工业外观

新版外观与结构随 IUCE 本体加载，不需要资源包。如果曾启用旧拉丝钢试样，请先禁用它，以免它遮住本体的模型。

- **选矿联合厂**：宽 5、高 6、长 7；未成型阶段使用深钢铆接外壳、工字钢承重梁、带铜圈破碎滚筒、OLED 控制器与凹陷端口仓口，配合半透明工业投影定位；成型后由 `BlockEntityRenderer` 渲染由 Blockbench 导出的完整 3D 重型选矿机组模型（含工作时旋转的双破碎辊与洗选滚筒、热窑炉芯及高空警示灯）。
- **轧制联合机**：宽 9、高 7、长 9–17；未成型时是开放辊床与跨线门架。成型后渲染整机模型：前端是带烟囱的加热炉和电控柜，中段是四辊轧机机架与辊道，尾端是卷取机、主传动电机和飞轮。加长时，模型按方块布局的规则补辊道段和机架。

`PlantStructure` 同时供成型校验、投影、施工机、材料清单和指南使用。旧存档中的控制器保留旧结构规则（`LEGACY=true`）；新放置的控制器使用新规则。外壳在同类外壳或仓口相接的一侧隐藏边条；成型后隐藏单块模型以避免与整体 3D 模型穿模。

- `python tools/export_plant_model.py [ore_processing_plant|rolling_mill]`：从 `tools/models/<名字>.bbmodel` 重新打包图集与 3D 网格。转动部件、发光部件和可伸缩分段（`head`、`bay`、`stand`、`tail` 组）的设置都在脚本开头的 `SETTINGS` 里。
- `python tools/build_rolling_mill_model.py`：重新生成轧机的 Blockbench 源文件和贴图；之后依次运行 `clean_plant_geometry.py rolling_mill` 和导出脚本。重新生成会覆盖在 Blockbench 里的手工修改。
- `python tools/draw_plant_parts.py`：重新生成未成型多方块结构部件的像素贴图、立体方块模型与方块状态。
- `gradlew.bat runClient -PsmokeTest -PregressionTests -PguideShots=industrial -PguideWorld=<临时测试世界>`：在独立的临时测试世界搭建两台示例并输出游戏内截图；此命令会改动指定测试世界，不要传入日常存档。
