package ero;

import init.ModMenus;
import machine.macerator.screen.MaceratorScreen;
import machine.generator.screen.GeneratorScreen;
import machine.electric_furnace.screen.ElectricFurnaceScreen;
import machine.compressor.screen.CompressorScreen;
import machine.extractor.screen.ExtractorScreen;
import machine.induction_furnace.screen.InductionFurnaceScreen;
import machine.ore_washer.screen.OreWasherScreen;
import machine.electrolyzer.screen.ElectrolyzerScreen;
import machine.planter.screen.PlanterScreen;
import machine.storage.screen.StorageScreen;
import machine.workbench.screen.IndustrialWorkbenchScreen;
import compat.jei.EroJeiPlugin;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Ero.MOD_ID, dist = Dist.CLIENT)
public class EroClient {

    public EroClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.getEventBus().addListener(EroClient::registerScreens);
        container.getEventBus().addListener(EroClient::registerFluidModels);
        client.guide.Guide.init(container.getEventBus());
        if (System.getProperty("iuce.guideShots") != null) {
            try {
                Class.forName("regression.GuideShots").getMethod("register").invoke(null);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Guide screenshot tool needs -PregressionTests", error);
            }
        }

        // 移除模组物品 tooltip 底部的模组名标签（模组名在渲染阶段追加，需在 GatherComponents 中处理）
        NeoForge.EVENT_BUS.addListener(EroClient::onGatherTooltipComponents);

        // 机器 GUI 左上角 "i" 信息按钮
        NeoForge.EVENT_BUS.addListener(client.gui.component.MachineInfoPanel::onScreenInit);
        NeoForge.EVENT_BUS.addListener(client.gui.component.MachineInfoPanel::onMouseClicked);
        NeoForge.EVENT_BUS.addListener(client.gui.component.MachineInfoPanel::onRenderForeground);

        // 手持传送带、滑槽时显示摆放预览和方向箭头
        NeoForge.EVENT_BUS.addListener(client.PlacementPreview::onOutline);
    }

    private static void registerFluidModels(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
        event.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(
            new net.minecraft.client.resources.model.sprite.Material(Identifier.parse("iuce:block/coolant_still")),
            new net.minecraft.client.resources.model.sprite.Material(Identifier.parse("iuce:block/coolant_flow")),
            null, null),
            init.ModFluids.COOLANT, init.ModFluids.FLOWING_COOLANT);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MACERATOR.get(), MaceratorScreen::new);
        event.register(ModMenus.GENERATOR.get(), GeneratorScreen::new);
        event.register(ModMenus.ELECTRIC_FURNACE.get(), ElectricFurnaceScreen::new);
        event.register(ModMenus.COMPRESSOR.get(), CompressorScreen::new);
        event.register(ModMenus.METAL_FORMER.get(), machine.metal_former.screen.MetalFormerScreen::new);
        event.register(ModMenus.EXTRACTOR.get(), ExtractorScreen::new);
        event.register(ModMenus.INDUCTION_FURNACE.get(), InductionFurnaceScreen::new);
        event.register(ModMenus.STORAGE.get(), StorageScreen::new);
        event.register(ModMenus.ORE_WASHER.get(), OreWasherScreen::new);
        event.register(ModMenus.ELECTROLYZER.get(), ElectrolyzerScreen::new);
        event.register(ModMenus.PLANTER.get(), PlanterScreen::new);
        event.register(ModMenus.INDUSTRIAL_WORKBENCH.get(), IndustrialWorkbenchScreen::new);
    }

    /**
     * 在 tooltip 渲染前移除模组物品底部的模组名标签。
     * 模组名由 Minecraft/NeoForge 在渲染阶段追加（不在 getTooltipLines 中），
     * 因此使用 RenderTooltipEvent.GatherComponents 在渲染阶段拦截。
     * JEI 使用自己的 JeiTooltip.draw 不触发此事件，由 EroJeiPlugin.forceModNameOverride 处理。
     */
    private static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) return;
        String modId = id.getNamespace();
        // 原版物品不处理
        if ("minecraft".equals(modId)) return;

        String modName = ModList.get().getModContainerById(modId)
            .map(c -> c.getModInfo().getDisplayName())
            .orElse(null);

        if (modName == null || modName.isEmpty()) return;

        var elements = event.getTooltipElements();

        // 从底部向上移除匹配模组名的元素和尾随空元素
        for (int i = elements.size() - 1; i >= 0; i--) {
            var left = elements.get(i).left();
            if (left.isEmpty()) break;
            String lineText = ChatFormatting.stripFormatting(left.get().getString()).trim();
            if (lineText.isEmpty()) {
                elements.remove(i);
            } else if (lineText.equalsIgnoreCase(modName)) {
                elements.remove(i);
            } else {
                break;
            }
        }
    }
}
