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
        ModBlockItems.METAL_FORMER,
        ModBlockItems.GEOTHERMAL_GENERATOR,
        ModBlockItems.INDUCTION_FURNACE,
        ModBlockItems.TRANSFORMER,
        ModBlockItems.CHUTE,
        ModBlockItems.CONVEYOR,
        ModBlockItems.FACTORY_HUB,
        ModBlockItems.PNEUMATIC_PIPE,
        ModBlockItems.LOGISTICS_PORT,
        ModBlockItems.SILO,
        ModBlockItems.FLUID_TANK,
        ModBlockItems.PLANT_CASING,
        ModBlockItems.PLANT_FRAME,
        ModBlockItems.PLANT_DRUM,
        ModBlockItems.CHECKER_PLATE,
        ModBlockItems.STEEL_GRATING,
        ModBlockItems.STEEL_STAIRS,
        ModBlockItems.STEEL_HANDRAIL,
        ModBlockItems.PROCESS_PIPE,
        ModBlockItems.FURNACE_SHELL,
        ModBlockItems.REFRACTORY_BRICKS,
        ModBlockItems.GRAPHITE_ELECTRODE,
        ModBlockItems.TANK_SHELL,
        ModBlockItems.PLANT_CONTROLLER,
        ModBlockItems.ITEM_INPUT_HATCH,
        ModBlockItems.ITEM_OUTPUT_HATCH,
        ModBlockItems.ENERGY_HATCH,
        ModBlockItems.ENERGY_HATCH_HV,
        ModBlockItems.MILL_CONTROLLER,
        ModBlockItems.REPAIR_BENCH, ModBlockItems.PROCESS_DESIGN_TABLE,
        ModBlockItems.LAMP,
        ModBlockItems.CONVERTER,
        ModBlockItems.BUILDER,
        ModBlockItems.BATBOX,
        ModBlockItems.CESU,
        ModBlockItems.MFE,
        ModBlockItems.MFSU,
        ModBlockItems.PUMP,
        ModBlockItems.ORE_WASHER,
        ModBlockItems.ELECTROLYZER,
        ModBlockItems.EXTRACTOR,
        ModBlockItems.PLANTER,
        // 公用工程
        ModBlockItems.BOILER,
        ModBlockItems.ROCK_SALT_ORE,
        ModBlockItems.LIMESTONE,
        ModBlockItems.AIR_COMPRESSOR,
        ModBlockItems.CONVEYOR_II,
        ModBlockItems.LOGISTICS_PORT_II,
        ModBlockItems.BULK_SILO,
        ModBlockItems.GAS_TANK,
        ModBlockItems.FLUID_PIPE,
        ModBlockItems.BRONZE_FLUID_PIPE,
        ModBlockItems.DRIP_PIPE,
        ModBlockItems.TWIN_SCREW_EXTRUDER,
        ModBlockItems.BIO_ETHYLENE_PLANT,
        ModBlockItems.CARBIDE_FURNACE_PLANT,
        ModBlockItems.COKE_OVEN_BATTERY,
        ModBlockItems.WASTE_HEAT_POWER_PLANT,
        ModBlockItems.MAIN_FEED_SEGMENT,
        ModBlockItems.MELTING_SEGMENT,
        ModBlockItems.SIDE_FEED_SEGMENT,
        ModBlockItems.MIXING_SEGMENT,
        ModBlockItems.VENTING_SEGMENT,
        ModBlockItems.DIE_SEGMENT,
        ModBlockItems.FLUID_INPUT_HATCH,
        ModBlockItems.FLUID_OUTPUT_HATCH,
        ModBlockItems.GAS_OUTPUT_HATCH,
        ModBlockItems.GENERATION_HATCH,
        ModBlockItems.MAINTENANCE_HATCH,
        ModBlockItems.COOLING_TOWER,
        ModBlockItems.STEAM_PIPE,
        ModBlockItems.COOLING_WATER_PIPE,
        ModBlockItems.AIR_PIPE,
        ModBlockItems.CREATIVE_UTILITY_SOURCE,
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
        ModItems.GUIDE,
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
        ModItems.PURIFIED_TIN_DUST, ModItems.PURIFIED_ZINC_DUST, ModItems.PURIFIED_ALUMINIUM_DUST,
        ModItems.PURIFIED_COBALT_DUST, ModItems.PURIFIED_LEAD_DUST, ModItems.PURIFIED_NICKEL_DUST,
        ModItems.PURIFIED_SILVER_DUST, ModItems.PURIFIED_URANIUM_DUST, ModItems.BRONZE_DUST, ModItems.DIAMOND_DUST, ModItems.ENERGIUM_DUST
    );

    private static final List<DeferredItem<? extends Item>> CRUSHED_ORES = List.of(
        ModItems.COPPER_DUST, ModItems.CRUSHED_TIN_DUST, ModItems.CRUSHED_ZINC_DUST, ModItems.CRUSHED_ALUMINIUM_DUST, ModItems.CRUSHED_COBALT_DUST,
        ModItems.CRUSHED_LEAD_DUST, ModItems.CRUSHED_NICKEL_DUST, ModItems.CRUSHED_SILVER_DUST, ModItems.CRUSHED_URANIUM_DUST
    );

    private static final List<DeferredItem<? extends Item>> PLATES = List.of(
        ModItems.TIN_PLATE, ModItems.IRON_PLATE, ModItems.ZINC_PLATE, ModItems.ALUMINIUM_PLATE,
        ModItems.COBALT_PLATE, ModItems.LEAD_PLATE, ModItems.NICKEL_PLATE, ModItems.SILVER_PLATE,
        ModItems.GOLD_PLATE, ModItems.COPPER_PLATE, ModItems.BRONZE_PLATE, ModItems.STEEL_PLATE
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

    private static final List<DeferredItem<? extends Item>> BIOLOGICAL = List.of(
        ModItems.BAGASSE, ModItems.DISTILLERS_GRAINS, ModItems.ENZYME_PREPARATION,
        ModItems.YEAST_CULTURE, ModItems.LACTIC_CULTURE, ModItems.ASPERGILLUS_CULTURE,
        ModItems.MOLD_BLANK,
        ModItems.SPRUE,
        ModItems.TOOL_STEEL_INGOT,
        ModItems.PLASTIC_PAIL,
        ModItems.CRATE_MOLD,
        ModItems.PAIL_MOLD,
        ModItems.PAIL_LID_MOLD,
        ModItems.BRICK_MOLD,
        ModItems.FITTING_MOLD,
        ModItems.HOUSING_MOLD,
        ModItems.EMITTER_MOLD,
        ModItems.LAMP_SHADE_MOLD,
        ModItems.PAIL_BODY,
        ModItems.PAIL_LID,
        ModItems.PIPE_FITTING,
        ModItems.MACHINE_HOUSING,
        ModItems.DRIP_EMITTER,
        ModItems.LAMP_SHADE,
        ModItems.ROCK_SALT,
        ModItems.COKE,
        ModItems.COKE_DUST,
        ModItems.NAPHTHALENE,
        ModItems.PITCH,
        ModItems.SULFUR,
        ModItems.CALCIUM_CARBONATE,
        ModItems.QUICKLIME,
        ModItems.CARBIDE_LUMP,
        ModItems.CALCIUM_CARBIDE,
        ModItems.CARBIDE_SLAG,
        ModItems.CARBON_ELECTRODE,
        ModItems.CAUSTIC_SODA,
        ModItems.ACTIVATED_CARBON,
        ModItems.MERCURY_FREE_CATALYST,
        ModItems.HEAVY_RESIDUE,
        ModItems.HEAT_STABILIZER,
        ModItems.PLASTICIZER,
        ModItems.INITIATOR,
        ModItems.DISPERSANT,
        ModItems.PE_WAX,
        ModItems.WET_PVC,
        ModItems.PVC_RESIN,
        ModItems.RIGID_PVC_DRY_BLEND,
        ModItems.SOFT_PVC_DRY_BLEND,
        ModItems.POLYOLEFIN_CATALYST,
        ModItems.WET_PE_POWDER,
        ModItems.PE_POWDER,
        ModItems.BIO_PE_PELLET,
        ModItems.STRAND,
        ModItems.FILLED_PE_PELLET,
        ModItems.RIGID_PVC_PELLET,
        ModItems.SOFT_PVC_PELLET,
        ModItems.COLOR_MASTERBATCH,
        ModItems.PIPE_DIE,
        ModItems.FILM_DIE,
        ModItems.COATING_DIE,
        ModItems.HOT_PVC_PIPE,
        ModItems.HOT_PE_PIPE,
        ModItems.PVC_PIPE_BLANK,
        ModItems.PE_PIPE_BLANK,
        ModItems.HOT_DRIP_TAPE,
        ModItems.DRIP_TAPE,
        ModItems.PLA_FILM_BUBBLE,
        ModItems.PE_FILM_BUBBLE,
        ModItems.HOT_COATED_WIRE,
        ModItems.PVC_PIPE,
        ModItems.PE_PIPE,
        ModItems.PLA_FILM_ROLL,
        ModItems.PE_FILM_ROLL,
        ModItems.MULCH_FILM_PLA,
        ModItems.MULCH_FILM_PE,
        ModItems.WASTE_FILM,
        ModItems.COAGULUM,
        ModItems.RUBBER_SHEET,
        ModItems.RUBBER_COMPOUND,
        ModItems.RUBBER_BELT,
        ModItems.RUBBER_SEAL,
        ModItems.BELT_MOLD,
        ModItems.SEAL_MOLD,
        ModItems.ACTIVATED_ALUMINA, ModItems.SPENT_CATALYST, ModItems.LACTIDE, ModItems.TIN_CATALYST,
        ModItems.PLA_INJECTION_PELLET, ModItems.PLA_FILM_PELLET, ModItems.PROCESS_CARD
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
        ModItems.SILVER_STICK, ModItems.ZINC_STICK, ModItems.IRON_STICK
    );

    private static final List<DeferredItem<? extends Item>> COMPONENTS = List.of(
        ModItems.COPPER_COIL, ModItems.HEATING_COIL, ModItems.ELECTRIC_MOTOR, ModItems.ADVANCED_MOTOR,
        ModItems.IRON_GEAR, ModItems.BRONZE_GEAR, ModItems.BRONZE_ROTOR, ModItems.MAGNET, ModItems.ELECTRODE,
        ModItems.PLATE_MOLD, ModItems.WIRE_MOLD, ModItems.ROD_MOLD, ModItems.CONFIGURATOR, ModItems.BLUEPRINT_TERMINAL
    );

    private static final List<DeferredItem<? extends Item>> CELLS = List.of(
        ModItems.EMPTY_CELL, ModItems.COOLANT_CELL, ModItems.COOLANT_BUCKET
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
        INGOTS, DUSTS, CRUSHED_ORES, PLATES, DENSE_PLATES, NUGGETS, STICKS, COMPONENTS, CIRCUITS, PLATED_SHELLS, CASINGS, UPGRADES, CELLS, SOLAR_GLASSES, BATTERIES,
        ORES, DEEP_ORES, METAL_BLOCKS, COAL_PRODUCTS, RUBBER_PRODUCTS, BIOLOGICAL
    );

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ERO_TAB = TABS.register("iuce_tab",
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
            .displayItems((parameters, output) -> {
                MACHINES.forEach(output::accept);
                // one stack per dye colour so every colour shows in the tab and in JEI
                for (var coloured : List.of(ModBlockItems.TOY_BRICK, ModBlockItems.CRATE)) {
                    for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
                        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(coloured.get());
                        stack.set(net.minecraft.core.component.DataComponents.BLOCK_STATE,
                            net.minecraft.world.item.component.BlockItemStateProperties.EMPTY.with(block.ColoredPlasticBlock.COLOR, dye));
                        output.accept(stack);
                    }
                }
                machine.process.ProcessMachines.all().forEach(entry -> output.accept(entry.item()));
            })
            .build()
    );
}
