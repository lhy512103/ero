package init;

import ero.Ero;
import machine.macerator.menu.MaceratorMenu;
import machine.generator.menu.GeneratorMenu;
import machine.electric_furnace.menu.ElectricFurnaceMenu;
import machine.compressor.menu.CompressorMenu;
import machine.extractor.menu.ExtractorMenu;
import machine.induction_furnace.menu.InductionFurnaceMenu;
import machine.ore_washer.menu.OreWasherMenu;
import machine.electrolyzer.menu.ElectrolyzerMenu;
import machine.planter.menu.PlanterMenu;
import machine.storage.menu.StorageMenu;
import machine.workbench.menu.IndustrialWorkbenchMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
        DeferredRegister.create(Registries.MENU, Ero.MOD_ID);

    public static final Supplier<MenuType<MaceratorMenu>> MACERATOR =
        MENUS.register("macerator", () -> IMenuTypeExtension.create(
            (IContainerFactory<MaceratorMenu>) (id, inv, data) -> new MaceratorMenu(id, inv,
                (machine.macerator.MaceratorBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))
        ));

    public static final Supplier<MenuType<GeneratorMenu>> GENERATOR =
        MENUS.register("generator", () -> IMenuTypeExtension.create(
            (IContainerFactory<GeneratorMenu>) (id, inv, data) -> new GeneratorMenu(id, inv,
                (machine.generator.GeneratorBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))
        ));

    public static final Supplier<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE =
        MENUS.register("electric_furnace", () -> IMenuTypeExtension.create(
            (IContainerFactory<ElectricFurnaceMenu>) (id, inv, data) -> new ElectricFurnaceMenu(id, inv,
                (machine.electric_furnace.ElectricFurnaceBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))
        ));

    public static final Supplier<MenuType<CompressorMenu>> COMPRESSOR =
        MENUS.register("compressor", () -> IMenuTypeExtension.create(
            (IContainerFactory<CompressorMenu>) (id, inv, data) -> new CompressorMenu(id, inv,
                (machine.compressor.CompressorBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))
        ));

    public static final Supplier<MenuType<ExtractorMenu>> EXTRACTOR =
        MENUS.register("extractor", () -> IMenuTypeExtension.create(
            (IContainerFactory<ExtractorMenu>) (id, inv, data) -> new ExtractorMenu(id, inv,
                (machine.extractor.ExtractorBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))
        ));

    public static final Supplier<MenuType<InductionFurnaceMenu>> INDUCTION_FURNACE =
        MENUS.register("induction_furnace", () -> IMenuTypeExtension.create(
            (IContainerFactory<InductionFurnaceMenu>) (id, inv, data) -> new InductionFurnaceMenu(id, inv,
                (machine.induction_furnace.InductionFurnaceBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))
        ));

    public static final Supplier<MenuType<StorageMenu>> STORAGE =
        MENUS.register("storage", () -> IMenuTypeExtension.create(
            (IContainerFactory<StorageMenu>) (id, inv, data) -> new StorageMenu(id, inv,
                (machine.StorageGuiProvider) inv.player.level().getBlockEntity(data.readBlockPos()))
        ));

    public static final Supplier<MenuType<OreWasherMenu>> ORE_WASHER =
        MENUS.register("ore_washer", () -> IMenuTypeExtension.create(
            (IContainerFactory<OreWasherMenu>) (id, inv, data) -> new OreWasherMenu(id, inv,
                (machine.ore_washer.OreWasherBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))
        ));

    public static final Supplier<MenuType<ElectrolyzerMenu>> ELECTROLYZER =
        MENUS.register("electrolyzer", () -> IMenuTypeExtension.create(
            (IContainerFactory<ElectrolyzerMenu>) (id, inv, data) -> new ElectrolyzerMenu(id, inv,
                (machine.electrolyzer.ElectrolyzerBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))
        ));

    public static final Supplier<MenuType<PlanterMenu>> PLANTER =
        MENUS.register("planter", () -> IMenuTypeExtension.create(
            (IContainerFactory<PlanterMenu>) (id, inv, data) -> new PlanterMenu(id, inv,
                (machine.planter.PlanterBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))
        ));

    public static final Supplier<MenuType<IndustrialWorkbenchMenu>> INDUSTRIAL_WORKBENCH =
        MENUS.register("industrial_workbench", () -> IMenuTypeExtension.create(
            (IContainerFactory<IndustrialWorkbenchMenu>) (id, inv, data) -> new IndustrialWorkbenchMenu(id, inv,
                (machine.workbench.IndustrialWorkbenchBlockEntity) inv.player.level().getBlockEntity(data.readBlockPos()))
        ));
}
