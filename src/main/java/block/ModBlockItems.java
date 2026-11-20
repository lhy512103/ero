package block;
import ero.Ero;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
public class ModBlockItems {
    public static final DeferredRegister.Items BLOCK_ITEMS = DeferredRegister.createItems(Ero.MOD_ID);
    public static final DeferredItem<BlockItem> TIN_ORE = registerBlockItem(ModBlocks.TIN_ORE);
    public static final DeferredItem<BlockItem> ZINC_ORE = registerBlockItem(ModBlocks.ZINC_ORE);
    public static final DeferredItem<BlockItem> ALUMINIUM_ORE = registerBlockItem(ModBlocks.ALUMINIUM_ORE);
    public static final DeferredItem<BlockItem> COBALT_ORE = registerBlockItem(ModBlocks.COBALT_ORE);
    public static final DeferredItem<BlockItem> LEAD_ORE = registerBlockItem(ModBlocks.LEAD_ORE);
    public static final DeferredItem<BlockItem> NICKEL_ORE = registerBlockItem(ModBlocks.NICKEL_ORE);
    public static final DeferredItem<BlockItem> SILVER_ORE = registerBlockItem(ModBlocks.SILVER_ORE);
    public static final DeferredItem<BlockItem> URANIUM_ORE = registerBlockItem(ModBlocks.URANIUM_ORE);
    public static final DeferredItem<BlockItem> DEEP_TIN_ORE = registerBlockItem(ModBlocks.DEEP_TIN_ORE);
    public static final DeferredItem<BlockItem> DEEP_ZINC_ORE = registerBlockItem(ModBlocks.DEEP_ZINC_ORE);
    public static final DeferredItem<BlockItem> DEEP_ALUMINIUM_ORE = registerBlockItem(ModBlocks.DEEP_ALUMINIUM_ORE);
    public static final DeferredItem<BlockItem> DEEP_COBALT_ORE = registerBlockItem(ModBlocks.DEEP_COBALT_ORE);
    public static final DeferredItem<BlockItem> DEEP_NICKEL_ORE = registerBlockItem(ModBlocks.DEEP_NICKEL_ORE);
    public static final DeferredItem<BlockItem> DEEP_SILVER_ORE = registerBlockItem(ModBlocks.DEEP_SILVER_ORE);
    public static final DeferredItem<BlockItem> DEEP_URANIUM_ORE = registerBlockItem(ModBlocks.DEEP_URANIUM_ORE);
    public static final DeferredItem<BlockItem> TIN_BLOCK = registerBlockItem(ModBlocks.TIN_BLOCK);
    public static final DeferredItem<BlockItem> ZINC_BLOCK = registerBlockItem(ModBlocks.ZINC_BLOCK);
    public static final DeferredItem<BlockItem> ALUMINIUM_BLOCK = registerBlockItem(ModBlocks.ALUMINIUM_BLOCK);
    public static final DeferredItem<BlockItem> COBALT_BLOCK = registerBlockItem(ModBlocks.COBALT_BLOCK);
    public static final DeferredItem<BlockItem> LEAD_BLOCK = registerBlockItem(ModBlocks.LEAD_BLOCK);
    public static final DeferredItem<BlockItem> NICKEL_BLOCK = registerBlockItem(ModBlocks.NICKEL_BLOCK);
    public static final DeferredItem<BlockItem> SILVER_BLOCK = registerBlockItem(ModBlocks.SILVER_BLOCK);
    public static final DeferredItem<BlockItem> URANIUM_BLOCK = registerBlockItem(ModBlocks.URANIUM_BLOCK);
    public static final DeferredItem<BlockItem> BRONZE_BLOCK = registerBlockItem(ModBlocks.BRONZE_BLOCK);
    // 外壳 (Casings)
    public static final DeferredItem<BlockItem> MACHINE_CASING = registerBlockItem(ModBlocks.MACHINE_CASING);
    public static final DeferredItem<BlockItem> MACHINE_CASING_2 = registerBlockItem(ModBlocks.MACHINE_CASING_2);
    // 机器 (Machines)
    public static final DeferredItem<BlockItem> GENERATOR = registerBlockItem(ModBlocks.GENERATOR);
    public static final DeferredItem<BlockItem> MACERATOR = registerBlockItem(ModBlocks.MACERATOR);
    public static final DeferredItem<BlockItem> ELECTRIC_FURNACE = registerBlockItem(ModBlocks.ELECTRIC_FURNACE);
    public static final DeferredItem<BlockItem> COMPRESSOR = registerBlockItem(ModBlocks.COMPRESSOR);
    public static final DeferredItem<BlockItem> METAL_FORMER = registerBlockItem(ModBlocks.METAL_FORMER);
    public static final DeferredItem<BlockItem> GEOTHERMAL_GENERATOR = registerBlockItem(ModBlocks.GEOTHERMAL_GENERATOR);
    public static final DeferredItem<BlockItem> INDUCTION_FURNACE = registerBlockItem(ModBlocks.INDUCTION_FURNACE);
    public static final DeferredItem<BlockItem> TRANSFORMER = registerBlockItem(ModBlocks.TRANSFORMER);
    public static final DeferredItem<BlockItem> CHUTE = registerBlockItem(ModBlocks.CHUTE);
    public static final DeferredItem<BlockItem> CONVEYOR = registerBlockItem(ModBlocks.CONVEYOR);
    public static final DeferredItem<BlockItem> FACTORY_HUB = registerBlockItem(ModBlocks.FACTORY_HUB);
    public static final DeferredItem<BlockItem> PNEUMATIC_PIPE = registerBlockItem(ModBlocks.PNEUMATIC_PIPE);
    public static final DeferredItem<BlockItem> LOGISTICS_PORT = registerBlockItem(ModBlocks.LOGISTICS_PORT);
    public static final DeferredItem<BlockItem> SILO = registerBlockItem(ModBlocks.SILO);
    public static final DeferredItem<BlockItem> FLUID_TANK = registerBlockItem(ModBlocks.FLUID_TANK);
    public static final DeferredItem<BlockItem> PLANT_CASING = registerBlockItem(ModBlocks.PLANT_CASING);
    public static final DeferredItem<BlockItem> PLANT_FRAME = registerBlockItem(ModBlocks.PLANT_FRAME);
    public static final DeferredItem<BlockItem> PLANT_DRUM = registerBlockItem(ModBlocks.PLANT_DRUM);
    public static final DeferredItem<BlockItem> PLANT_CONTROLLER = registerControllerItem("plant_controller", ModBlocks.PLANT_CONTROLLER);
    public static final DeferredItem<BlockItem> ITEM_INPUT_HATCH = registerBlockItem(ModBlocks.ITEM_INPUT_HATCH);
    public static final DeferredItem<BlockItem> ITEM_OUTPUT_HATCH = registerBlockItem(ModBlocks.ITEM_OUTPUT_HATCH);
    public static final DeferredItem<BlockItem> ENERGY_HATCH = registerBlockItem(ModBlocks.ENERGY_HATCH);
    public static final DeferredItem<BlockItem> ENERGY_HATCH_HV = registerBlockItem(ModBlocks.ENERGY_HATCH_HV);
    public static final DeferredItem<BlockItem> MILL_CONTROLLER = registerControllerItem("mill_controller", ModBlocks.MILL_CONTROLLER);
    public static final DeferredItem<BlockItem> REPAIR_BENCH = registerBlockItem(ModBlocks.REPAIR_BENCH);
    public static final DeferredItem<BlockItem> PROCESS_DESIGN_TABLE = registerBlockItem(ModBlocks.PROCESS_DESIGN_TABLE);
    public static final DeferredItem<BlockItem> LAMP = registerBlockItem(ModBlocks.LAMP);
    public static final DeferredItem<BlockItem> CONVERTER = registerBlockItem(ModBlocks.CONVERTER);
    public static final DeferredItem<BlockItem> BUILDER = registerBlockItem(ModBlocks.BUILDER);
    public static final DeferredItem<BlockItem> BATBOX = registerBlockItem(ModBlocks.BATBOX);
    public static final DeferredItem<BlockItem> CESU = registerBlockItem("cesu", ModBlocks.CESU, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> MFE = registerBlockItem("mfe", ModBlocks.MFE, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> MFSU = registerBlockItem("mfsu", ModBlocks.MFSU, p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> PUMP = registerBlockItem(ModBlocks.PUMP);
    // 洗矿机 (Ore Washers)
    public static final DeferredItem<BlockItem> ORE_WASHER = registerBlockItem(ModBlocks.ORE_WASHER);
    // 电解机 (Electrolyzers)
    public static final DeferredItem<BlockItem> ELECTROLYZER = registerBlockItem(ModBlocks.ELECTROLYZER);
    // 提取机 (Extractors)
    public static final DeferredItem<BlockItem> EXTRACTOR = registerBlockItem(ModBlocks.EXTRACTOR);
    // 种植机 (Planters)
    public static final DeferredItem<BlockItem> PLANTER = registerBlockItem(ModBlocks.PLANTER);
    // 公用工程 (Utility plants)
    public static final DeferredItem<BlockItem> BOILER = registerBlockItem(ModBlocks.BOILER);
    public static final DeferredItem<BlockItem> TOY_BRICK = registerBlockItem(ModBlocks.TOY_BRICK);
    public static final DeferredItem<BlockItem> CRATE = registerBlockItem(ModBlocks.CRATE);
    public static final DeferredItem<BlockItem> ROCK_SALT_ORE = registerBlockItem(ModBlocks.ROCK_SALT_ORE);
    public static final DeferredItem<BlockItem> LIMESTONE = registerBlockItem(ModBlocks.LIMESTONE);
    public static final DeferredItem<BlockItem> AIR_COMPRESSOR = registerBlockItem(ModBlocks.AIR_COMPRESSOR);
    public static final DeferredItem<BlockItem> CONVEYOR_II = registerBlockItem(ModBlocks.CONVEYOR_II);
    public static final DeferredItem<BlockItem> LOGISTICS_PORT_II = registerBlockItem(ModBlocks.LOGISTICS_PORT_II);
    public static final DeferredItem<BlockItem> BULK_SILO = registerBlockItem(ModBlocks.BULK_SILO);
    public static final DeferredItem<BlockItem> GAS_TANK = registerBlockItem(ModBlocks.GAS_TANK);
    public static final DeferredItem<BlockItem> FLUID_PIPE = registerBlockItem(ModBlocks.FLUID_PIPE);
    public static final DeferredItem<BlockItem> BRONZE_FLUID_PIPE = registerBlockItem(ModBlocks.BRONZE_FLUID_PIPE);
    public static final DeferredItem<BlockItem> DRIP_PIPE = registerBlockItem(ModBlocks.DRIP_PIPE);
    public static final DeferredItem<BlockItem> MULCHED_FARMLAND = registerBlockItem(ModBlocks.MULCHED_FARMLAND);
    public static final DeferredItem<BlockItem> COOLING_TOWER = registerBlockItem(ModBlocks.COOLING_TOWER);
    public static final DeferredItem<BlockItem> CREATIVE_UTILITY_SOURCE = registerBlockItem(ModBlocks.CREATIVE_UTILITY_SOURCE);
    public static final DeferredItem<BlockItem> STEAM_PIPE = registerBlockItem(ModBlocks.STEAM_PIPE);
    public static final DeferredItem<BlockItem> COOLING_WATER_PIPE = registerBlockItem(ModBlocks.COOLING_WATER_PIPE);
    public static final DeferredItem<BlockItem> AIR_PIPE = registerBlockItem(ModBlocks.AIR_PIPE);
    // 工业工作台 (Industrial Workbench)
    public static final DeferredItem<BlockItem> INDUSTRIAL_WORKBENCH = registerBlockItem(ModBlocks.INDUSTRIAL_WORKBENCH);
    // 太阳能发电机 (Solar Generators) - 保留到奇异
    public static final DeferredItem<BlockItem> SOLAR_GENERATOR = registerBlockItem(ModBlocks.SOLAR_GENERATOR);
    public static final DeferredItem<BlockItem> ADVANCED_SOLAR_GENERATOR = registerBlockItem(ModBlocks.ADVANCED_SOLAR_GENERATOR);
    public static final DeferredItem<BlockItem> HYBRID_SOLAR_GENERATOR = registerBlockItem(ModBlocks.HYBRID_SOLAR_GENERATOR);
    public static final DeferredItem<BlockItem> PERFECT_HYBRID_SOLAR_GENERATOR = registerBlockItem("perfect_hybrid_solar_generator", ModBlocks.PERFECT_HYBRID_SOLAR_GENERATOR, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> QUANTUM_SOLAR_GENERATOR = registerBlockItem("quantum_solar_generator", ModBlocks.QUANTUM_SOLAR_GENERATOR, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> SINGULARY_SOLAR_GENERATOR = registerBlockItem("singulary_solar_generator", ModBlocks.SINGULARY_SOLAR_GENERATOR, p -> p.rarity(Rarity.UNCOMMON));

    // 铜导线 (Copper Cable)
    public static final DeferredItem<BlockItem> COPPER_CABLE = registerBlockItem(ModBlocks.COPPER_CABLE);
    // 锡导线 (Tin Cable)
    public static final DeferredItem<BlockItem> TIN_CABLE = registerBlockItem(ModBlocks.TIN_CABLE);
    // 金导线 (Gold Cable)
    public static final DeferredItem<BlockItem> GOLD_CABLE = registerBlockItem(ModBlocks.GOLD_CABLE);
    // 铁导线 (Iron Cable)
    public static final DeferredItem<BlockItem> IRON_CABLE = registerBlockItem(ModBlocks.IRON_CABLE);
    // 玻璃导线 (Glass Cable)
    public static final DeferredItem<BlockItem> GLASS_CABLE = registerBlockItem(ModBlocks.GLASS_CABLE);
    // 奇异导线 (Singular Cable)
    public static final DeferredItem<BlockItem> SINGULAR_CABLE = registerBlockItem(ModBlocks.SINGULAR_CABLE);

    // 绝缘导线 (Insulated Cables)
    public static final DeferredItem<BlockItem> INSULATED_COPPER_CABLE = registerBlockItem(ModBlocks.INSULATED_COPPER_CABLE);
    public static final DeferredItem<BlockItem> INSULATED_TIN_CABLE = registerBlockItem(ModBlocks.INSULATED_TIN_CABLE);
    public static final DeferredItem<BlockItem> INSULATED_GOLD_CABLE = registerBlockItem(ModBlocks.INSULATED_GOLD_CABLE);
    public static final DeferredItem<BlockItem> INSULATED_HV_CABLE = registerBlockItem(ModBlocks.INSULATED_HV_CABLE);

    // 橡胶树 (Rubber Tree)
    public static final DeferredItem<BlockItem> RUBBER_WOOD = registerBlockItem(ModBlocks.RUBBER_WOOD);
    public static final DeferredItem<BlockItem> RUBBER_LEAVES = registerBlockItem(ModBlocks.RUBBER_LEAVES);
    public static final DeferredItem<BlockItem> RUBBER_SAPLING = registerBlockItem(ModBlocks.RUBBER_SAPLING);

    // 裂变反应堆 (Fission Reactor)
    public static final DeferredItem<BlockItem> FISSION_CONTROLLER = registerBlockItem(ModBlocks.FISSION_CONTROLLER);
    public static final DeferredItem<BlockItem> REACTOR_CASING = registerBlockItem(ModBlocks.REACTOR_CASING);
    public static final DeferredItem<BlockItem> FISSION_CONTROLLER_FRONT = registerBlockItem(ModBlocks.FISSION_CONTROLLER_FRONT);
    public static final DeferredItem<BlockItem> REACTOR_CASING_BACK = registerBlockItem(ModBlocks.REACTOR_CASING_BACK);
    public static final DeferredItem<BlockItem> REACTOR_CASING_SIDE = registerBlockItem(ModBlocks.REACTOR_CASING_SIDE);

    public static DeferredItem<BlockItem> registerBlockItem(DeferredBlock<? extends Block> block) {
        return BLOCK_ITEMS.registerSimpleBlockItem(block);
    }
    public static DeferredItem<BlockItem> registerBlockItem(String name, DeferredBlock<? extends Block> block) {
        return BLOCK_ITEMS.registerSimpleBlockItem(name, block);
    }
    public static DeferredItem<BlockItem> registerBlockItem(String name, DeferredBlock<? extends Block> block, UnaryOperator<Item.Properties> properties) {
        return BLOCK_ITEMS.registerSimpleBlockItem(name, block, properties);
    }
    public static <T extends BlockItem> DeferredItem<T> registerBlockItem(String name, Supplier<T> blockItemSupplier) {
        return BLOCK_ITEMS.register(name, blockItemSupplier);
    }
    private static DeferredItem<BlockItem> registerControllerItem(String name, DeferredBlock<? extends Block> block) {
        return BLOCK_ITEMS.registerItem(name, props -> new BlockItem(block.get(), props.useBlockDescriptionPrefix()) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
                builder.accept(Component.translatable("tooltip.iuce.plant_controller.1").withStyle(ChatFormatting.GRAY));
                builder.accept(Component.translatable("tooltip.iuce.plant_controller.2").withStyle(ChatFormatting.GRAY));
            }
        });
    }
}
