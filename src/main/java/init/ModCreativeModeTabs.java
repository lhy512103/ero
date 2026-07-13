package init;

import ero.Ero;
import block.ModBlockItems;
import item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ero.MOD_ID);

    private static final List<DeferredItem<? extends Item>> MACHINES = List.of(
        ModBlockItems.GENERATOR,
        ModBlockItems.MACERATOR,
        ModBlockItems.ELECTRIC_FURNACE,
        ModBlockItems.COMPRESSOR,
        ModBlockItems.GEOTHERMAL_GENERATOR,
        ModBlockItems.INDUCTION_FURNACE,
        ModBlockItems.BATBOX,
        ModBlockItems.CESU,
        ModBlockItems.MFE,
        ModBlockItems.MFSU,
        ModBlockItems.PUMP,
        ModBlockItems.ORE_WASHER,
        ModBlockItems.ELECTROLYZER,
        ModBlockItems.EXTRACTOR,
        ModBlockItems.PLANTER,
        ModBlockItems.INDUSTRIAL_WORKBENCH,
        // 太阳能发电机 - 保留到奇异
        ModBlockItems.SOLAR_GENERATOR,
        ModBlockItems.ADVANCED_SOLAR_GENERATOR,
        ModBlockItems.HYBRID_SOLAR_GENERATOR,
        ModBlockItems.PERFECT_HYBRID_SOLAR_GENERATOR,
        ModBlockItems.QUANTUM_SOLAR_GENERATOR,
        ModBlockItems.SINGULARY_SOLAR_GENERATOR,
        // 线缆
        ModBlockItems.COPPER_CABLE,
        ModBlockItems.TIN_CABLE,
        ModBlockItems.GOLD_CABLE,
        ModBlockItems.IRON_CABLE,
        ModBlockItems.GLASS_CABLE,
        ModBlockItems.SINGULAR_CABLE,
        // 绝缘导线
        ModBlockItems.INSULATED_COPPER_CABLE,
        ModBlockItems.INSULATED_TIN_CABLE,
        ModBlockItems.INSULATED_GOLD_CABLE,
        ModBlockItems.INSULATED_HV_CABLE,
        // 橡胶树
        ModBlockItems.RUBBER_WOOD,
        ModBlockItems.RUBBER_LEAVES,
        ModBlockItems.RUBBER_SAPLING,
        // 工具
        ModItems.CUTTER,
        ModItems.FORGE_HAMMER,
        ModItems.TREETAP,
        ModItems.ELECTRIC_TREETAP,
        ModItems.WRENCH,
        ModItems.ELECTRIC_WRENCH,
        ModItems.ELECTRIC_HOE
    );

    private static final List<DeferredItem<? extends Item>> INGOTS = List.of(
        ModItems.TIN_INGOT, ModItems.ZINC_INGOT, ModItems.ALUMINIUM_INGOT,
        ModItems.COBALT_INGOT, ModItems.LEAD_INGOT, ModItems.NICKEL_INGOT,
        ModItems.SILVER_INGOT, ModItems.URANIUM_INGOT, ModItems.STEEL_INGOT, ModItems.REFINED_STEEL_INGOT, ModItems.ALLOY_INGOT, ModItems.BRONZE_INGOT,
        ModItems.IRRADIANT_URANIUM_INGOT
    );

    private static final List<DeferredItem<? extends Item>> DUSTS = List.of(
        ModItems.TIN_DUST, ModItems.ZINC_DUST, ModItems.ALUMINIUM_DUST,
        ModItems.COBALT_DUST, ModItems.LEAD_DUST, ModItems.NICKEL_DUST,
        ModItems.SILVER_DUST, ModItems.URANIUM_DUST, ModItems.BRONZE_DUST, ModItems.DIAMOND_DUST, ModItems.ENERGIUM_DUST
    );

    private static final List<DeferredItem<? extends Item>> CRUSHED_ORES = List.of(
        ModItems.COPPER_DUST, ModItems.PURIFIED_TIN_DUST, ModItems.PURIFIED_ZINC_DUST, ModItems.PURIFIED_ALUMINIUM_DUST, ModItems.PURIFIED_COBALT_DUST,
        ModItems.PURIFIED_LEAD_DUST, ModItems.PURIFIED_NICKEL_DUST, ModItems.PURIFIED_SILVER_DUST, ModItems.PURIFIED_URANIUM_DUST
    );

    private static final List<DeferredItem<? extends Item>> PLATES = List.of(
        ModItems.TIN_PLATE, ModItems.IRON_PLATE, ModItems.ZINC_PLATE, ModItems.ALUMINIUM_PLATE,
        ModItems.COBALT_PLATE, ModItems.LEAD_PLATE, ModItems.NICKEL_PLATE, ModItems.SILVER_PLATE,
        ModItems.GOLD_PLATE, ModItems.COPPER_PLATE
    );

    private static final List<DeferredItem<? extends Item>> DENSE_PLATES = List.of(
        ModItems.DENSE_TIN_PLATE, ModItems.DENSE_IRON_PLATE, ModItems.DENSE_GOLD_PLATE,
        ModItems.DENSE_COPPER_PLATE, ModItems.DENSE_BRONZE_PLATE, ModItems.DENSE_STEEL_PLATE,
        ModItems.DENSE_LEAD_PLATE, ModItems.DENSE_LAPIS_PLATE, ModItems.DENSE_OBSIDIAN_PLATE
    );

    private static final List<DeferredItem<? extends Item>> NUGGETS = List.of(
        ModItems.TIN_NUGGET, ModItems.ZINC_NUGGET, ModItems.ALUMINIUM_NUGGET,
        ModItems.COBALT_NUGGET, ModItems.LEAD_NUGGET, ModItems.NICKEL_NUGGET,
        ModItems.SILVER_NUGGET, ModItems.URANIUM_NUGGET, ModItems.STEEL_NUGGET, ModItems.REFINED_STEEL_NUGGET
    );

    private static final List<DeferredItem<? extends Item>> CIRCUITS = List.of(
        ModItems.CIRCUIT, ModItems.ADVANCED_CIRCUIT
    );

    private static final List<DeferredItem<? extends Item>> PLATED_SHELLS = List.of(
        ModItems.PLATED_SHELL, ModItems.BASIC_PLATED_SHELL, ModItems.ADVANCED_PLATED_SHELL,
        ModItems.PERFECT_PLATED_SHELL, ModItems.PHOTON_PLATED_SHELL
    );

    private static final List<DeferredItem<? extends Item>> RUBBER_PRODUCTS = List.of(
        ModItems.RUBBER, ModItems.RAW_LATEX
    );

    private static final List<DeferredItem<? extends Item>> CASINGS = List.of(
        ModBlockItems.MACHINE_CASING, ModBlockItems.MACHINE_CASING_2,
        ModBlockItems.FISSION_CONTROLLER_FRONT, ModBlockItems.REACTOR_CASING_BACK, ModBlockItems.REACTOR_CASING_SIDE
    );

    private static final List<DeferredItem<? extends Item>> UPGRADES = List.of(
        ModItems.EJECTOR_UPGRADE, ModItems.ENERGY_STORAGE_UPGRADE,
        ModItems.FLUID_EJECTOR_UPGRADE, ModItems.FLUID_PULLING_UPGRADE,
        ModItems.OVERCLOCKER_UPGRADE, ModItems.OVERCLOCKER_UPGRADE_1,
        ModItems.OVERCLOCKER_UPGRADE_2, ModItems.PULLING_UPGRADE,
        ModItems.MACHINE_UPGRADE_0, ModItems.MACHINE_UPGRADE_1,
        ModItems.MACHINE_UPGRADE_2, ModItems.MACHINE_UPGRADE_3
    );

    private static final List<DeferredItem<? extends Item>> ORES = List.of(
        ModBlockItems.TIN_ORE, ModBlockItems.ZINC_ORE, ModBlockItems.ALUMINIUM_ORE,
        ModBlockItems.COBALT_ORE, ModBlockItems.LEAD_ORE, ModBlockItems.NICKEL_ORE,
        ModBlockItems.SILVER_ORE, ModBlockItems.URANIUM_ORE
    );

    private static final List<DeferredItem<? extends Item>> DEEP_ORES = List.of(
        ModBlockItems.DEEP_TIN_ORE, ModBlockItems.DEEP_ZINC_ORE, ModBlockItems.DEEP_ALUMINIUM_ORE,
        ModBlockItems.DEEP_COBALT_ORE, ModBlockItems.DEEP_NICKEL_ORE,
        ModBlockItems.DEEP_SILVER_ORE, ModBlockItems.DEEP_URANIUM_ORE
    );

    private static final List<DeferredItem<? extends Item>> METAL_BLOCKS = List.of(
        ModBlockItems.TIN_BLOCK, ModBlockItems.ZINC_BLOCK, ModBlockItems.ALUMINIUM_BLOCK,
        ModBlockItems.COBALT_BLOCK, ModBlockItems.LEAD_BLOCK, ModBlockItems.NICKEL_BLOCK,
        ModBlockItems.SILVER_BLOCK, ModBlockItems.URANIUM_BLOCK, ModBlockItems.BRONZE_BLOCK
    );

    private static final List<DeferredItem<? extends Item>> STICKS = List.of(
        ModItems.ALUMINIUM_STICK, ModItems.COBALT_STICK, ModItems.NICKEL_STICK,
        ModItems.SILVER_STICK, ModItems.ZINC_STICK
    );

    private static final List<DeferredItem<? extends Item>> CELLS = List.of(
        ModItems.EMPTY_CELL, ModItems.COOLANT_CELL, ModItems.COOLANT
    );

    private static final List<DeferredItem<? extends Item>> SOLAR_GLASSES = List.of(
        ModItems.BASIC_SOLAR_GLASS, ModItems.ADVANCED_SOLAR_GLASS,
        ModItems.HYBRID_SOLAR_GLASS, ModItems.PERFECT_HYBRID_SOLAR_GLASS,
        ModItems.QUANTUM_SOLAR_GLASS, ModItems.SINGULARY_SOLAR_GLASS
    );

    private static final List<DeferredItem<? extends Item>> COAL_PRODUCTS = List.of(
        ModItems.COAL_DUST, ModItems.COAL_FUEL, ModItems.COAL_BALL, ModItems.COAL_BLOCK_ITEM
    );

    private static final List<DeferredItem<? extends Item>> BATTERIES = List.of(
        ModItems.RE_BATTERY, ModItems.ADVANCED_RE_BATTERY,
        ModItems.ENERGY_CRYSTAL, ModItems.LAPOTRON_CRYSTAL,
        ModItems.SINGLE_USE_BATTERY
    );

    private static final List<List<DeferredItem<? extends Item>>> ALL_GROUPS = List.of(
        INGOTS, DUSTS, CRUSHED_ORES, PLATES, DENSE_PLATES, NUGGETS, CIRCUITS, PLATED_SHELLS, CASINGS, UPGRADES, STICKS, CELLS, SOLAR_GLASSES, BATTERIES,
        ORES, DEEP_ORES, METAL_BLOCKS, COAL_PRODUCTS, RUBBER_PRODUCTS
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ERO_TAB = TABS.register("ero_tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + Ero.MOD_ID))
            .icon(() -> ModItems.CIRCUIT.get().getDefaultInstance())
            .displayItems((parameters, output) -> ALL_GROUPS.forEach(group -> group.forEach(output::accept)))
            .build()
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MACHINE_TAB = TABS.register("machine_tab",
        () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup." + Ero.MOD_ID + ".machine"))
            .icon(() -> ModBlockItems.GENERATOR.get().getDefaultInstance())
            .displayItems((parameters, output) -> MACHINES.forEach(output::accept))
            .build()
    );
}
