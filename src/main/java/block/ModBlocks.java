package block;
import ero.Ero;
import block.cable.CableBlock;
import block.FissionControllerBlock;
import block.ReactorCasingBlock;
import blockentity.ModBlockEntities;
import energy.EUVoltage;
import machine.compressor.CompressorBlockEntity;
import machine.electric_furnace.ElectricFurnaceBlockEntity;
import machine.generator.GeneratorBlockEntity;
import machine.macerator.MaceratorBlockEntity;
import machine.electrolyzer.ElectrolyzerBlockEntity;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Ero.MOD_ID);
    private static Supplier<BlockBehaviour.Properties> oreProperties() {
        return () -> BlockBehaviour.Properties.of()
            .destroyTime(3.0f)
            .explosionResistance(3.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops();
    }
    private static Supplier<BlockBehaviour.Properties> deepOreProperties() {
        return () -> BlockBehaviour.Properties.of()
            .destroyTime(4.5f)
            .explosionResistance(3.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops();
    }
    private static Supplier<BlockBehaviour.Properties> metalBlockProperties() {
        return () -> BlockBehaviour.Properties.of()
            .destroyTime(5.0f)
            .explosionResistance(6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops();
    }
    private static Supplier<BlockBehaviour.Properties> machineProperties() {
        return () -> BlockBehaviour.Properties.of()
            .destroyTime(3.5f)
            .explosionResistance(6.0f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops();
    }
    private static Supplier<BlockBehaviour.Properties> cableProperties() {
        return () -> BlockBehaviour.Properties.of()
            .destroyTime(0.5f)
            .explosionResistance(1.0f)
            .sound(SoundType.METAL)
            .noOcclusion();
    }

    public static final DeferredBlock<Block> TIN_ORE = BLOCKS.registerSimpleBlock("tin_ore", oreProperties());
    public static final DeferredBlock<Block> ZINC_ORE = BLOCKS.registerSimpleBlock("zinc_ore", oreProperties());
    public static final DeferredBlock<Block> ALUMINIUM_ORE = BLOCKS.registerSimpleBlock("aluminium_ore", oreProperties());
    public static final DeferredBlock<Block> COBALT_ORE = BLOCKS.registerSimpleBlock("cobalt_ore", oreProperties());
    public static final DeferredBlock<Block> LEAD_ORE = BLOCKS.registerSimpleBlock("lead_ore", oreProperties());
    public static final DeferredBlock<Block> NICKEL_ORE = BLOCKS.registerSimpleBlock("nickel_ore", oreProperties());
    public static final DeferredBlock<Block> SILVER_ORE = BLOCKS.registerSimpleBlock("silver_ore", oreProperties());
    public static final DeferredBlock<Block> URANIUM_ORE = BLOCKS.registerSimpleBlock("uranium_ore", oreProperties());
    public static final DeferredBlock<Block> DEEP_TIN_ORE = BLOCKS.registerSimpleBlock("deep_tin_ore", deepOreProperties());
    public static final DeferredBlock<Block> DEEP_ZINC_ORE = BLOCKS.registerSimpleBlock("deep_zinc_ore", deepOreProperties());
    public static final DeferredBlock<Block> DEEP_ALUMINIUM_ORE = BLOCKS.registerSimpleBlock("deep_aluminium_ore", deepOreProperties());
    public static final DeferredBlock<Block> DEEP_COBALT_ORE = BLOCKS.registerSimpleBlock("deep_cobalt_ore", deepOreProperties());
    public static final DeferredBlock<Block> DEEP_NICKEL_ORE = BLOCKS.registerSimpleBlock("deep_nickel_ore", deepOreProperties());
    public static final DeferredBlock<Block> DEEP_SILVER_ORE = BLOCKS.registerSimpleBlock("deep_silver_ore", deepOreProperties());
    public static final DeferredBlock<Block> DEEP_URANIUM_ORE = BLOCKS.registerSimpleBlock("deep_uranium_ore", deepOreProperties());
    public static final DeferredBlock<Block> TIN_BLOCK = BLOCKS.registerSimpleBlock("tin_block", metalBlockProperties());
    public static final DeferredBlock<Block> ZINC_BLOCK = BLOCKS.registerSimpleBlock("zinc_block", metalBlockProperties());
    public static final DeferredBlock<Block> ALUMINIUM_BLOCK = BLOCKS.registerSimpleBlock("aluminium_block", metalBlockProperties());
    public static final DeferredBlock<Block> COBALT_BLOCK = BLOCKS.registerSimpleBlock("cobalt_block", metalBlockProperties());
    public static final DeferredBlock<Block> LEAD_BLOCK = BLOCKS.registerSimpleBlock("lead_block", metalBlockProperties());
    public static final DeferredBlock<Block> NICKEL_BLOCK = BLOCKS.registerSimpleBlock("nickel_block", metalBlockProperties());
    public static final DeferredBlock<Block> SILVER_BLOCK = BLOCKS.registerSimpleBlock("silver_block", metalBlockProperties());
    public static final DeferredBlock<Block> URANIUM_BLOCK = BLOCKS.registerSimpleBlock("uranium_block", metalBlockProperties());
    public static final DeferredBlock<Block> BRONZE_BLOCK = BLOCKS.registerSimpleBlock("bronze_block", metalBlockProperties());
    public static final DeferredBlock<Block> MACHINE_CASING = BLOCKS.registerSimpleBlock("machine_casing", metalBlockProperties());
    public static final DeferredBlock<Block> MACHINE_CASING_2 = BLOCKS.registerSimpleBlock("machine_casing_2", metalBlockProperties());
    // 工业工作台 (Industrial Workbench)
    public static final DeferredBlock<MachineBlock> INDUSTRIAL_WORKBENCH = BLOCKS.registerBlock("industrial_workbench",
        p -> new MachineBlock(p, IndustrialWorkbenchBlockEntity::new, ModBlockEntities.INDUSTRIAL_WORKBENCH), machineProperties());
    public static final DeferredBlock<MachineBlock> GENERATOR = BLOCKS.registerBlock("generator",
        p -> new MachineBlock(p, GeneratorBlockEntity::new, ModBlockEntities.GENERATOR), machineProperties());
    public static final DeferredBlock<MachineBlock> MACERATOR = BLOCKS.registerBlock("macerator",
        p -> new MachineBlock(p, MaceratorBlockEntity::new, ModBlockEntities.MACERATOR), machineProperties());
    public static final DeferredBlock<MachineBlock> ELECTRIC_FURNACE = BLOCKS.registerBlock("electric_furnace",
        p -> new MachineBlock(p, ElectricFurnaceBlockEntity::new, ModBlockEntities.ELECTRIC_FURNACE), machineProperties());
    public static final DeferredBlock<MachineBlock> COMPRESSOR = BLOCKS.registerBlock("compressor",
        p -> new MachineBlock(p, CompressorBlockEntity::new, ModBlockEntities.COMPRESSOR), machineProperties());
    public static final DeferredBlock<MachineBlock> METAL_FORMER = BLOCKS.registerBlock("metal_former",
        p -> new MachineBlock(p, machine.metal_former.MetalFormerBlockEntity::new, ModBlockEntities.METAL_FORMER), machineProperties());
    public static final DeferredBlock<GeothermalGeneratorBlock> GEOTHERMAL_GENERATOR = BLOCKS.registerBlock("geothermal_generator",
        GeothermalGeneratorBlock::new, machineProperties());
    public static final DeferredBlock<InductionFurnaceBlock> INDUCTION_FURNACE = BLOCKS.registerBlock("induction_furnace",
        InductionFurnaceBlock::new, machineProperties());
    public static final DeferredBlock<TransformerBlock> TRANSFORMER = BLOCKS.registerBlock("transformer",
        TransformerBlock::new, machineProperties());
    public static final DeferredBlock<MachineBlock> BATBOX = BLOCKS.registerBlock("batbox",
        p -> new MachineBlock(p, BatboxBlockEntity::new, ModBlockEntities.BATBOX), machineProperties());
    public static final DeferredBlock<MachineBlock> CESU = BLOCKS.registerBlock("cesu",
        p -> new MachineBlock(p, CesuBlockEntity::new, ModBlockEntities.CESU), machineProperties());
    public static final DeferredBlock<MachineBlock> MFE = BLOCKS.registerBlock("mfe",
        p -> new MachineBlock(p, MfeBlockEntity::new, ModBlockEntities.MFE), machineProperties());
    public static final DeferredBlock<MachineBlock> MFSU = BLOCKS.registerBlock("mfsu",
        p -> new MachineBlock(p, MfsuBlockEntity::new, ModBlockEntities.MFSU), machineProperties());
    public static final DeferredBlock<PumpBlock> PUMP = BLOCKS.registerBlock("pump",
        p -> new PumpBlock(p), machineProperties());
    public static final DeferredBlock<OreWasherBlock> ORE_WASHER = BLOCKS.registerBlock("ore_washer",
        p -> new OreWasherBlock(p), machineProperties());
    public static final DeferredBlock<MachineBlock> ELECTROLYZER = BLOCKS.registerBlock("electrolyzer",
        p -> new MachineBlock(p, ElectrolyzerBlockEntity::new, ModBlockEntities.ELECTROLYZER), machineProperties());
    public static final DeferredBlock<MachineBlock> EXTRACTOR = BLOCKS.registerBlock("extractor",
        p -> new MachineBlock(p, ExtractorBlockEntity::new, ModBlockEntities.EXTRACTOR), machineProperties());
    public static final DeferredBlock<MachineBlock> PLANTER = BLOCKS.registerBlock("planter",
        p -> new MachineBlock(p, PlanterBlockEntity::new, ModBlockEntities.PLANTER), machineProperties());

    // 冷却液流体方块
    public static final DeferredBlock<net.minecraft.world.level.block.LiquidBlock> COOLANT_BLOCK =
        BLOCKS.registerBlock("coolant",
            p -> new net.minecraft.world.level.block.LiquidBlock(init.ModFluids.COOLANT.get(), p) {},
            () -> BlockBehaviour.Properties.of()
                .mapColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_BLUE)
                .replaceable()
                .noCollision()
                .strength(100.0F)
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY));
    // 太阳能发电机 (Solar Generators) - 保留到奇异
    public static final DeferredBlock<MachineBlock> SOLAR_GENERATOR = BLOCKS.registerBlock("solar_generator",
        p -> new MachineBlock(p, SolarGeneratorBlockEntity::new, ModBlockEntities.SOLAR_GENERATOR), machineProperties());
    public static final DeferredBlock<MachineBlock> ADVANCED_SOLAR_GENERATOR = BLOCKS.registerBlock("advanced_solar_generator",
        p -> new MachineBlock(p, AdvancedSolarGeneratorBlockEntity::new, ModBlockEntities.ADVANCED_SOLAR_GENERATOR), machineProperties());
    public static final DeferredBlock<MachineBlock> HYBRID_SOLAR_GENERATOR = BLOCKS.registerBlock("hybrid_solar_generator",
        p -> new MachineBlock(p, HybridSolarGeneratorBlockEntity::new, ModBlockEntities.HYBRID_SOLAR_GENERATOR), machineProperties());
    public static final DeferredBlock<MachineBlock> PERFECT_HYBRID_SOLAR_GENERATOR = BLOCKS.registerBlock("perfect_hybrid_solar_generator",
        p -> new MachineBlock(p, PerfectHybridSolarGeneratorBlockEntity::new, ModBlockEntities.PERFECT_HYBRID_SOLAR_GENERATOR), machineProperties());
    public static final DeferredBlock<MachineBlock> QUANTUM_SOLAR_GENERATOR = BLOCKS.registerBlock("quantum_solar_generator",
        p -> new MachineBlock(p, QuantumSolarGeneratorBlockEntity::new, ModBlockEntities.QUANTUM_SOLAR_GENERATOR), machineProperties());
    public static final DeferredBlock<MachineBlock> SINGULARY_SOLAR_GENERATOR = BLOCKS.registerBlock("singulary_solar_generator",
        p -> new MachineBlock(p, SingularySolarGeneratorBlockEntity::new, ModBlockEntities.SINGULARY_SOLAR_GENERATOR), machineProperties());

    // 铜导线 (Copper Cable) - LV等级, 32 EU/t, 细线缆
    public static final DeferredBlock<CableBlock> COPPER_CABLE = BLOCKS.registerBlock("copper_cable",
        p -> new CableBlock(p, EUVoltage.LV, 0.25F, ModBlockEntities.COPPER_CABLE), cableProperties());

    // 锡导线 (Tin Cable) - ULV等级, 8 EU/t, 细线缆
    public static final DeferredBlock<CableBlock> TIN_CABLE = BLOCKS.registerBlock("tin_cable",
        p -> new CableBlock(p, EUVoltage.ULV, 0.25F, ModBlockEntities.TIN_CABLE), cableProperties());

    // 金导线 (Gold Cable) - MV等级, 128 EU/t, 细线缆
    public static final DeferredBlock<CableBlock> GOLD_CABLE = BLOCKS.registerBlock("gold_cable",
        p -> new CableBlock(p, EUVoltage.MV, 0.25F, ModBlockEntities.GOLD_CABLE), cableProperties());

    // 铁导线 (Iron Cable) - HV等级, 512 EU/t, 粗线缆
    public static final DeferredBlock<CableBlock> IRON_CABLE = BLOCKS.registerBlock("iron_cable",
        p -> new CableBlock(p, EUVoltage.HV, 0.25F, ModBlockEntities.IRON_CABLE), cableProperties());

    // 玻璃导线 (Glass Cable) - EV等级, 2048 EU/t, 细线缆
    public static final DeferredBlock<CableBlock> GLASS_CABLE = BLOCKS.registerBlock("glass_cable",
        p -> new CableBlock(p, EUVoltage.EV, 0.25F, ModBlockEntities.GLASS_CABLE), cableProperties());

    // 奇异导线 (Singular Cable) - LuV等级, 32768 EU/t, 细线缆
    public static final DeferredBlock<CableBlock> SINGULAR_CABLE = BLOCKS.registerBlock("singular_cable",
        p -> new CableBlock(p, EUVoltage.LuV, 0.25F, ModBlockEntities.SINGULAR_CABLE), cableProperties());

    // 绝缘导线 (Insulated Cables) - 可放置, 复用导线放置状态
    // 绝缘铜导线 - LV等级, 32 EU/t
    public static final DeferredBlock<CableBlock> INSULATED_COPPER_CABLE = BLOCKS.registerBlock("insulated_copper_cable",
        p -> new CableBlock(p, EUVoltage.LV, 0.25F, ModBlockEntities.INSULATED_COPPER_CABLE), cableProperties());
    // 绝缘锡导线 - ULV等级, 8 EU/t
    public static final DeferredBlock<CableBlock> INSULATED_TIN_CABLE = BLOCKS.registerBlock("insulated_tin_cable",
        p -> new CableBlock(p, EUVoltage.ULV, 0.25F, ModBlockEntities.INSULATED_TIN_CABLE), cableProperties());
    // 绝缘金导线 - MV等级, 128 EU/t
    public static final DeferredBlock<CableBlock> INSULATED_GOLD_CABLE = BLOCKS.registerBlock("insulated_gold_cable",
        p -> new CableBlock(p, EUVoltage.MV, 0.25F, ModBlockEntities.INSULATED_GOLD_CABLE), cableProperties());
    // 绝缘高压导线 - HV等级, 512 EU/t
    public static final DeferredBlock<CableBlock> INSULATED_HV_CABLE = BLOCKS.registerBlock("insulated_hv_cable",
        p -> new CableBlock(p, EUVoltage.HV, 0.25F, ModBlockEntities.INSULATED_HV_CABLE), cableProperties());

    // 橡胶树 (Rubber Tree)
    public static final DeferredBlock<RubberWood> RUBBER_WOOD = BLOCKS.registerBlock("rubber_wood",
        RubberWood::new, () -> RubberWood.defaultProps());
    public static final DeferredBlock<RubberLeaves> RUBBER_LEAVES = BLOCKS.registerBlock("rubber_leaves",
        RubberLeaves::new, () -> RubberLeaves.defaultProps());
    public static final DeferredBlock<RubberSapling> RUBBER_SAPLING = BLOCKS.registerBlock("rubber_sapling",
        RubberSapling::new, () -> RubberSapling.defaultProps());

    // 裂变反应堆 (Fission Reactor)
    public static final DeferredBlock<FissionControllerBlock> FISSION_CONTROLLER = BLOCKS.registerBlock("fission_controller",
        FissionControllerBlock::new, machineProperties());
    public static final DeferredBlock<ReactorCasingBlock> REACTOR_CASING = BLOCKS.registerBlock("reactor_casing",
        ReactorCasingBlock::new, metalBlockProperties());
    public static final DeferredBlock<ReactorControllerBlock> FISSION_CONTROLLER_FRONT = BLOCKS.registerBlock("fission_controller_front",
        ReactorControllerBlock::new, metalBlockProperties());
    public static final DeferredBlock<Block> REACTOR_CASING_BACK = BLOCKS.registerSimpleBlock("reactor_casing_back", metalBlockProperties());
    public static final DeferredBlock<Block> REACTOR_CASING_SIDE = BLOCKS.registerSimpleBlock("reactor_casing_side", metalBlockProperties());

}
