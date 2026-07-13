package blockentity;

import ero.Ero;
import block.ModBlocks;
import blockentity.cable.CableBlockEntity;
import machine.compressor.CompressorBlockEntity;
import machine.electric_furnace.ElectricFurnaceBlockEntity;
import machine.generator.GeneratorBlockEntity;
import machine.geothermal_generator.GeothermalGeneratorBlockEntity;
import machine.induction_furnace.InductionFurnaceBlockEntity;
import machine.macerator.MaceratorBlockEntity;
import machine.pump.PumpBlockEntity;
import machine.electrolyzer.ElectrolyzerBlockEntity;
import machine.ore_washer.OreWasherBlockEntity;
import machine.extractor.ExtractorBlockEntity;
import machine.planter.PlanterBlockEntity;
import machine.workbench.IndustrialWorkbenchBlockEntity;
import machine.solar_generator.SolarGeneratorBlockEntity;
import machine.advanced_solar_generator.AdvancedSolarGeneratorBlockEntity;
import machine.quantum_solar_generator.QuantumSolarGeneratorBlockEntity;
import machine.hybrid_solar_generator.HybridSolarGeneratorBlockEntity;
import machine.singulary_solar_generator.SingularySolarGeneratorBlockEntity;
import machine.perfect_hybrid_solar_generator.PerfectHybridSolarGeneratorBlockEntity;
import machine.storage.BatboxBlockEntity;
import machine.storage.CesuBlockEntity;
import machine.storage.MfeBlockEntity;
import machine.storage.MfsuBlockEntity;
import blockentity.FissionControllerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Ero.MOD_ID);

    public static final Supplier<BlockEntityType<GeneratorBlockEntity>> GENERATOR =
        BLOCK_ENTITIES.register("generator", () ->
            new BlockEntityType<>(GeneratorBlockEntity::new, false, ModBlocks.GENERATOR.get())
        );

    public static final Supplier<BlockEntityType<MaceratorBlockEntity>> MACERATOR =
        BLOCK_ENTITIES.register("macerator", () ->
            new BlockEntityType<>(MaceratorBlockEntity::new, false, ModBlocks.MACERATOR.get())
        );

    public static final Supplier<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
        BLOCK_ENTITIES.register("electric_furnace", () ->
            new BlockEntityType<>(ElectricFurnaceBlockEntity::new, false, ModBlocks.ELECTRIC_FURNACE.get())
        );

    public static final Supplier<BlockEntityType<CompressorBlockEntity>> COMPRESSOR =
        BLOCK_ENTITIES.register("compressor", () ->
            new BlockEntityType<>(CompressorBlockEntity::new, false, ModBlocks.COMPRESSOR.get())
        );

    public static final Supplier<BlockEntityType<GeothermalGeneratorBlockEntity>> GEOTHERMAL_GENERATOR =
        BLOCK_ENTITIES.register("geothermal_generator", () ->
            new BlockEntityType<>(GeothermalGeneratorBlockEntity::new, false, ModBlocks.GEOTHERMAL_GENERATOR.get())
        );

    public static final Supplier<BlockEntityType<InductionFurnaceBlockEntity>> INDUCTION_FURNACE =
        BLOCK_ENTITIES.register("induction_furnace", () ->
            new BlockEntityType<>(InductionFurnaceBlockEntity::new, false, ModBlocks.INDUCTION_FURNACE.get())
        );

    public static final Supplier<BlockEntityType<BatboxBlockEntity>> BATBOX =
        BLOCK_ENTITIES.register("batbox", () ->
            new BlockEntityType<>(BatboxBlockEntity::new, false, ModBlocks.BATBOX.get())
        );

    public static final Supplier<BlockEntityType<CesuBlockEntity>> CESU =
        BLOCK_ENTITIES.register("cesu", () ->
            new BlockEntityType<>(CesuBlockEntity::new, false, ModBlocks.CESU.get())
        );

    public static final Supplier<BlockEntityType<MfeBlockEntity>> MFE =
        BLOCK_ENTITIES.register("mfe", () ->
            new BlockEntityType<>(MfeBlockEntity::new, false, ModBlocks.MFE.get())
        );

    public static final Supplier<BlockEntityType<MfsuBlockEntity>> MFSU =
        BLOCK_ENTITIES.register("mfsu", () ->
            new BlockEntityType<>(MfsuBlockEntity::new, false, ModBlocks.MFSU.get())
        );

    public static final Supplier<BlockEntityType<PumpBlockEntity>> PUMP =
        BLOCK_ENTITIES.register("pump", () ->
            new BlockEntityType<>(PumpBlockEntity::new, false, ModBlocks.PUMP.get())
        );

    public static final Supplier<BlockEntityType<OreWasherBlockEntity>> ORE_WASHER =
        BLOCK_ENTITIES.register("ore_washer", () ->
            new BlockEntityType<>(OreWasherBlockEntity::new, false, ModBlocks.ORE_WASHER.get())
        );

    public static final Supplier<BlockEntityType<ElectrolyzerBlockEntity>> ELECTROLYZER =
        BLOCK_ENTITIES.register("electrolyzer", () ->
            new BlockEntityType<>(ElectrolyzerBlockEntity::new, false, ModBlocks.ELECTROLYZER.get())
        );

    public static final Supplier<BlockEntityType<ExtractorBlockEntity>> EXTRACTOR =
        BLOCK_ENTITIES.register("extractor", () ->
            new BlockEntityType<>(ExtractorBlockEntity::new, false, ModBlocks.EXTRACTOR.get())
        );

    public static final Supplier<BlockEntityType<PlanterBlockEntity>> PLANTER =
        BLOCK_ENTITIES.register("planter", () ->
            new BlockEntityType<>(PlanterBlockEntity::new, false, ModBlocks.PLANTER.get())
        );

    // 工业工作台 (Industrial Workbench)
    public static final Supplier<BlockEntityType<IndustrialWorkbenchBlockEntity>> INDUSTRIAL_WORKBENCH =
        BLOCK_ENTITIES.register("industrial_workbench", () ->
            new BlockEntityType<>(IndustrialWorkbenchBlockEntity::new, false, ModBlocks.INDUSTRIAL_WORKBENCH.get())
        );

    // 太阳能发电机 (Solar Generators) - 保留到奇异
    public static final Supplier<BlockEntityType<SolarGeneratorBlockEntity>> SOLAR_GENERATOR =
        BLOCK_ENTITIES.register("solar_generator", () ->
            new BlockEntityType<>(SolarGeneratorBlockEntity::new, false, ModBlocks.SOLAR_GENERATOR.get())
        );
    public static final Supplier<BlockEntityType<AdvancedSolarGeneratorBlockEntity>> ADVANCED_SOLAR_GENERATOR =
        BLOCK_ENTITIES.register("advanced_solar_generator", () ->
            new BlockEntityType<>(AdvancedSolarGeneratorBlockEntity::new, false, ModBlocks.ADVANCED_SOLAR_GENERATOR.get())
        );
    public static final Supplier<BlockEntityType<HybridSolarGeneratorBlockEntity>> HYBRID_SOLAR_GENERATOR =
        BLOCK_ENTITIES.register("hybrid_solar_generator", () ->
            new BlockEntityType<>(HybridSolarGeneratorBlockEntity::new, false, ModBlocks.HYBRID_SOLAR_GENERATOR.get())
        );
    public static final Supplier<BlockEntityType<PerfectHybridSolarGeneratorBlockEntity>> PERFECT_HYBRID_SOLAR_GENERATOR =
        BLOCK_ENTITIES.register("perfect_hybrid_solar_generator", () ->
            new BlockEntityType<>(PerfectHybridSolarGeneratorBlockEntity::new, false, ModBlocks.PERFECT_HYBRID_SOLAR_GENERATOR.get())
        );
    public static final Supplier<BlockEntityType<QuantumSolarGeneratorBlockEntity>> QUANTUM_SOLAR_GENERATOR =
        BLOCK_ENTITIES.register("quantum_solar_generator", () ->
            new BlockEntityType<>(QuantumSolarGeneratorBlockEntity::new, false, ModBlocks.QUANTUM_SOLAR_GENERATOR.get())
        );
    public static final Supplier<BlockEntityType<SingularySolarGeneratorBlockEntity>> SINGULARY_SOLAR_GENERATOR =
        BLOCK_ENTITIES.register("singulary_solar_generator", () ->
            new BlockEntityType<>(SingularySolarGeneratorBlockEntity::new, false, ModBlocks.SINGULARY_SOLAR_GENERATOR.get())
        );

    // 铜导线
    public static final Supplier<BlockEntityType<CableBlockEntity>> COPPER_CABLE =
        BLOCK_ENTITIES.register("copper_cable", () ->
            new BlockEntityType<>(CableBlockEntity::new, false, ModBlocks.COPPER_CABLE.get())
        );

    // 锡导线
    public static final Supplier<BlockEntityType<CableBlockEntity>> TIN_CABLE =
        BLOCK_ENTITIES.register("tin_cable", () ->
            new BlockEntityType<>(CableBlockEntity::new, false, ModBlocks.TIN_CABLE.get())
        );

    // 金导线
    public static final Supplier<BlockEntityType<CableBlockEntity>> GOLD_CABLE =
        BLOCK_ENTITIES.register("gold_cable", () ->
            new BlockEntityType<>(CableBlockEntity::new, false, ModBlocks.GOLD_CABLE.get())
        );

    // 铁导线
    public static final Supplier<BlockEntityType<CableBlockEntity>> IRON_CABLE =
        BLOCK_ENTITIES.register("iron_cable", () ->
            new BlockEntityType<>(CableBlockEntity::new, false, ModBlocks.IRON_CABLE.get())
        );

    // 玻璃导线
    public static final Supplier<BlockEntityType<CableBlockEntity>> GLASS_CABLE =
        BLOCK_ENTITIES.register("glass_cable", () ->
            new BlockEntityType<>(CableBlockEntity::new, false, ModBlocks.GLASS_CABLE.get())
        );

    // 奇异导线
    public static final Supplier<BlockEntityType<CableBlockEntity>> SINGULAR_CABLE =
        BLOCK_ENTITIES.register("singular_cable", () ->
            new BlockEntityType<>(CableBlockEntity::new, false, ModBlocks.SINGULAR_CABLE.get())
        );

    // 绝缘导线
    public static final Supplier<BlockEntityType<CableBlockEntity>> INSULATED_COPPER_CABLE =
        BLOCK_ENTITIES.register("insulated_copper_cable", () ->
            new BlockEntityType<>(CableBlockEntity::new, false, ModBlocks.INSULATED_COPPER_CABLE.get())
        );

    public static final Supplier<BlockEntityType<CableBlockEntity>> INSULATED_TIN_CABLE =
        BLOCK_ENTITIES.register("insulated_tin_cable", () ->
            new BlockEntityType<>(CableBlockEntity::new, false, ModBlocks.INSULATED_TIN_CABLE.get())
        );

    public static final Supplier<BlockEntityType<CableBlockEntity>> INSULATED_GOLD_CABLE =
        BLOCK_ENTITIES.register("insulated_gold_cable", () ->
            new BlockEntityType<>(CableBlockEntity::new, false, ModBlocks.INSULATED_GOLD_CABLE.get())
        );

    public static final Supplier<BlockEntityType<CableBlockEntity>> INSULATED_HV_CABLE =
        BLOCK_ENTITIES.register("insulated_hv_cable", () ->
            new BlockEntityType<>(CableBlockEntity::new, false, ModBlocks.INSULATED_HV_CABLE.get())
        );

    // 裂变反应堆控制器 (Fission Controller)
    public static final Supplier<BlockEntityType<FissionControllerBlockEntity>> FISSION_CONTROLLER =
        BLOCK_ENTITIES.register("fission_controller", () ->
            new BlockEntityType<>(FissionControllerBlockEntity::new, false, ModBlocks.FISSION_CONTROLLER.get())
        );

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
