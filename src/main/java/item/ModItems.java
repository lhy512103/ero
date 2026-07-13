package item;

import ero.Ero;
import energy.EUVoltage;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Ero.MOD_ID);

    // 锭 (Ingots)
    public static final DeferredItem<Item> TIN_INGOT = registerSimpleItem("tin_ingot");
    public static final DeferredItem<Item> ZINC_INGOT = registerSimpleItem("zinc_ingot");
    public static final DeferredItem<Item> ALUMINIUM_INGOT = registerSimpleItem("aluminium_ingot");
    public static final DeferredItem<Item> COBALT_INGOT = registerSimpleItem("cobalt_ingot");
    public static final DeferredItem<Item> LEAD_INGOT = registerSimpleItem("lead_ingot");
    public static final DeferredItem<Item> NICKEL_INGOT = registerSimpleItem("nickel_ingot");
    public static final DeferredItem<Item> SILVER_INGOT = registerSimpleItem("silver_ingot");
    public static final DeferredItem<Item> URANIUM_INGOT = registerSimpleItem("uranium_ingot");
    public static final DeferredItem<Item> STEEL_INGOT = registerSimpleItem("steel_ingot");
    public static final DeferredItem<Item> REFINED_STEEL_INGOT = registerSimpleItem("refined_steel_ingot");
    public static final DeferredItem<Item> ALLOY_INGOT = registerSimpleItem("alloy_ingot");
    public static final DeferredItem<Item> BRONZE_INGOT = registerSimpleItem("bronze_ingot");

    // 粉 (Dusts) - 注册ID与纯净粉互换
    public static final DeferredItem<Item> TIN_DUST = registerSimpleItem("purified_tin_dust");
    public static final DeferredItem<Item> COPPER_DUST = registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> ZINC_DUST = registerSimpleItem("purified_zinc_dust");
    public static final DeferredItem<Item> ALUMINIUM_DUST = registerSimpleItem("purified_aluminium_dust");
    public static final DeferredItem<Item> COBALT_DUST = registerSimpleItem("purified_cobalt_dust");
    public static final DeferredItem<Item> LEAD_DUST = registerSimpleItem("purified_lead_dust");
    public static final DeferredItem<Item> NICKEL_DUST = registerSimpleItem("purified_nickel_dust");
    public static final DeferredItem<Item> SILVER_DUST = registerSimpleItem("purified_silver_dust");
    public static final DeferredItem<Item> URANIUM_DUST = registerSimpleItem("purified_uranium_dust");
    public static final DeferredItem<Item> BRONZE_DUST = registerSimpleItem("bronze_dust");
    public static final DeferredItem<Item> DIAMOND_DUST = registerSimpleItem("diamond_dust");
    public static final DeferredItem<Item> ENERGIUM_DUST = registerSimpleItem("energium_dust");

    // 煤制品 (Coal Products)
    public static final DeferredItem<Item> COAL_DUST = registerSimpleItem("coal_dust");
    public static final DeferredItem<Item> COAL_FUEL = registerSimpleItem("coal_fuel");
    public static final DeferredItem<Item> COAL_BALL = registerSimpleItem("coal_ball");
    public static final DeferredItem<Item> COAL_BLOCK_ITEM = registerSimpleItem("coal_block_item");

    // 纯净粉 (Purified Dusts) - 注册ID与粉互换
    public static final DeferredItem<Item> PURIFIED_TIN_DUST = registerSimpleItem("tin_dust");
    public static final DeferredItem<Item> PURIFIED_ZINC_DUST = registerSimpleItem("zinc_dust");
    public static final DeferredItem<Item> PURIFIED_ALUMINIUM_DUST = registerSimpleItem("aluminium_dust");
    public static final DeferredItem<Item> PURIFIED_COBALT_DUST = registerSimpleItem("cobalt_dust");
    public static final DeferredItem<Item> PURIFIED_LEAD_DUST = registerSimpleItem("lead_dust");
    public static final DeferredItem<Item> PURIFIED_NICKEL_DUST = registerSimpleItem("nickel_dust");
    public static final DeferredItem<Item> PURIFIED_SILVER_DUST = registerSimpleItem("silver_dust");
    public static final DeferredItem<Item> PURIFIED_URANIUM_DUST = registerSimpleItem("uranium_dust");

    // 板 (Plates)
    public static final DeferredItem<Item> TIN_PLATE = registerSimpleItem("tin_plate");
    public static final DeferredItem<Item> IRON_PLATE = registerSimpleItem("iron_plate");
    public static final DeferredItem<Item> ZINC_PLATE = registerSimpleItem("zinc_plate");
    public static final DeferredItem<Item> ALUMINIUM_PLATE = registerSimpleItem("aluminium_plate");
    public static final DeferredItem<Item> COBALT_PLATE = registerSimpleItem("cobalt_plate");
    public static final DeferredItem<Item> LEAD_PLATE = registerSimpleItem("lead_plate");
    public static final DeferredItem<Item> NICKEL_PLATE = registerSimpleItem("nickel_plate");
    public static final DeferredItem<Item> SILVER_PLATE = registerSimpleItem("silver_plate");
    public static final DeferredItem<Item> GOLD_PLATE = registerSimpleItem("gold_plate");
    public static final DeferredItem<Item> COPPER_PLATE = registerSimpleItem("copper_plate");

    // 致密板 (Dense Plates)
    public static final DeferredItem<Item> DENSE_TIN_PLATE = registerSimpleItem("dense_tin_plate");
    public static final DeferredItem<Item> DENSE_IRON_PLATE = registerSimpleItem("dense_iron_plate");
    public static final DeferredItem<Item> DENSE_GOLD_PLATE = registerSimpleItem("dense_gold_plate");
    public static final DeferredItem<Item> DENSE_COPPER_PLATE = registerSimpleItem("dense_copper_plate");
    public static final DeferredItem<Item> DENSE_BRONZE_PLATE = registerSimpleItem("dense_bronze_plate");
    public static final DeferredItem<Item> DENSE_STEEL_PLATE = registerSimpleItem("dense_steel_plate");
    public static final DeferredItem<Item> DENSE_LEAD_PLATE = registerSimpleItem("dense_lead_plate");
    public static final DeferredItem<Item> DENSE_LAPIS_PLATE = registerSimpleItem("dense_lapis_plate");
    public static final DeferredItem<Item> DENSE_OBSIDIAN_PLATE = registerSimpleItem("dense_obsidian_plate");

    // 粒 (Nuggets)
    public static final DeferredItem<Item> TIN_NUGGET = registerSimpleItem("tin_nugget");
    public static final DeferredItem<Item> ZINC_NUGGET = registerSimpleItem("zinc_nugget");
    public static final DeferredItem<Item> ALUMINIUM_NUGGET = registerSimpleItem("aluminium_nugget");
    public static final DeferredItem<Item> COBALT_NUGGET = registerSimpleItem("cobalt_nugget");
    public static final DeferredItem<Item> LEAD_NUGGET = registerSimpleItem("lead_nugget");
    public static final DeferredItem<Item> NICKEL_NUGGET = registerSimpleItem("nickel_nugget");
    public static final DeferredItem<Item> SILVER_NUGGET = registerSimpleItem("silver_nugget");
    public static final DeferredItem<Item> URANIUM_NUGGET = registerSimpleItem("uranium_nugget");
    public static final DeferredItem<Item> STEEL_NUGGET = registerSimpleItem("steel_nugget");
    public static final DeferredItem<Item> REFINED_STEEL_NUGGET = registerSimpleItem("refined_steel_nugget");

    // 电路 (Circuits)
    public static final DeferredItem<Item> CIRCUIT = registerSimpleItem("circuit");
    public static final DeferredItem<Item> ADVANCED_CIRCUIT = registerSimpleItem("advanced_circuit");

    // 升级 (Upgrades)
    public static final DeferredItem<EjectorUpgradeItem> EJECTOR_UPGRADE = registerItem("ejector_upgrade", EjectorUpgradeItem::new);
    public static final DeferredItem<Item> ENERGY_STORAGE_UPGRADE = registerSimpleItem("energy_storage_upgrade");
    public static final DeferredItem<FluidEjectorUpgradeItem> FLUID_EJECTOR_UPGRADE = registerItem("fluid_ejector_upgrade", FluidEjectorUpgradeItem::new);
    public static final DeferredItem<FluidPullingUpgradeItem> FLUID_PULLING_UPGRADE = registerItem("fluid_pulling_upgrade", FluidPullingUpgradeItem::new);
    public static final DeferredItem<OverclockerUpgradeItem> OVERCLOCKER_UPGRADE = registerItem("overclocker_upgrade", OverclockerUpgradeItem::new);
    public static final DeferredItem<OverclockerUpgrade1Item> OVERCLOCKER_UPGRADE_1 = registerItem("overclocker_upgrade1", OverclockerUpgrade1Item::new);
    public static final DeferredItem<OverclockerUpgrade2Item> OVERCLOCKER_UPGRADE_2 = registerItem("overclocker_upgrade2", OverclockerUpgrade2Item::new);
    public static final DeferredItem<PullingUpgradeItem> PULLING_UPGRADE = registerItem("pulling_upgrade", PullingUpgradeItem::new);

    // 棍 (Sticks)
    public static final DeferredItem<Item> ALUMINIUM_STICK = registerSimpleItem("aluminium_stick");
    public static final DeferredItem<Item> COBALT_STICK = registerSimpleItem("cobalt_stick");
    public static final DeferredItem<Item> NICKEL_STICK = registerSimpleItem("nickel_stick");
    public static final DeferredItem<Item> SILVER_STICK = registerSimpleItem("silver_stick");
    public static final DeferredItem<Item> ZINC_STICK = registerSimpleItem("zinc_stick");

    // 特殊锭 (Special Ingots)
    public static final DeferredItem<Item> IRRADIANT_URANIUM_INGOT = registerSimpleItem("irradiant_uranium_ingot");
    public static final DeferredItem<Item> LOGO = registerSimpleItem("logo");

    // 橡胶制品 (Rubber Products)
    public static final DeferredItem<Item> RUBBER = registerSimpleItem("rubber");
    public static final DeferredItem<Item> RAW_LATEX = registerSimpleItem("raw_latex");

    // 电镀外壳 (Plated Shells)
    public static final DeferredItem<Item> PLATED_SHELL = registerSimpleItem("plated_shell");
    public static final DeferredItem<Item> BASIC_PLATED_SHELL = registerSimpleItem("basic_plated_shell");
    public static final DeferredItem<Item> ADVANCED_PLATED_SHELL = registerSimpleItem("advanced_plated_shell");
    public static final DeferredItem<Item> PERFECT_PLATED_SHELL = registerSimpleItem("perfect_plated_shell");
    public static final DeferredItem<Item> PHOTON_PLATED_SHELL = registerSimpleItem("photon_plated_shell");

    // 机器升级组件 (Machine Upgrade Components)
    public static final DeferredItem<Item> MACHINE_UPGRADE_0 = registerSimpleItem("machine_upgrade_0");
    public static final DeferredItem<Item> MACHINE_UPGRADE_1 = registerSimpleItem("machine_upgrade_1");
    public static final DeferredItem<Item> MACHINE_UPGRADE_2 = registerSimpleItem("machine_upgrade_2");
    public static final DeferredItem<Item> MACHINE_UPGRADE_3 = registerSimpleItem("machine_upgrade_3");

    // 工具 (Tools)
    public static final DeferredItem<CutterItem> CUTTER = registerItem("cutter", CutterItem::new);
    public static final DeferredItem<ForgeHammerItem> FORGE_HAMMER = registerItem("forge_hammer", ForgeHammerItem::new);
    public static final DeferredItem<TreetapItem> TREETAP = registerItem("treetap", TreetapItem::new);
    public static final DeferredItem<ElectricTreetapItem> ELECTRIC_TREETAP = registerItem("electric_treetap", ElectricTreetapItem::new);
    public static final DeferredItem<WrenchItem> WRENCH = registerItem("wrench", WrenchItem::new);
    public static final DeferredItem<ElectricWrenchItem> ELECTRIC_WRENCH = registerItem("electric_wrench", ElectricWrenchItem::new);
    public static final DeferredItem<ElectricHoeItem> ELECTRIC_HOE = registerItem("electric_hoe", ElectricHoeItem::new);

    // 单元 (Cells)
    public static final DeferredItem<Item> EMPTY_CELL = registerSimpleItem("empty_cell");
    public static final DeferredItem<Item> COOLANT_CELL = registerSimpleItem("coolant_cell");
    public static final DeferredItem<Item> COOLANT = registerSimpleItem("coolant");
    public static final DeferredItem<Item> WATER = registerSimpleItem("water");

    // 太阳能玻璃板 (Solar Glass Panels) - 保留到奇异
    public static final DeferredItem<Item> BASIC_SOLAR_GLASS = registerSimpleItem("basic_solar_glass");
    public static final DeferredItem<Item> ADVANCED_SOLAR_GLASS = registerSimpleItem("advanced_solar_glass");
    public static final DeferredItem<Item> HYBRID_SOLAR_GLASS = registerSimpleItem("hybrid_solar_glass");
    public static final DeferredItem<Item> PERFECT_HYBRID_SOLAR_GLASS = registerSimpleItem("perfect_hybrid_solar_glass");
    public static final DeferredItem<Item> QUANTUM_SOLAR_GLASS = registerSimpleItem("quantum_solar_glass");
    public static final DeferredItem<Item> SINGULARY_SOLAR_GLASS = registerSimpleItem("singulary_solar_glass");

    // 充电电池 (Rechargeable Batteries) - 注册名复用自 industrialupgrade
    public static final DeferredItem<RechargeableBatteryItem> RE_BATTERY = registerItem("re_battery",
        props -> new RechargeableBatteryItem(props, 10_000, EUVoltage.LV));
    public static final DeferredItem<RechargeableBatteryItem> ADVANCED_RE_BATTERY = registerItem("advanced_re_battery",
        props -> new RechargeableBatteryItem(props, 100_000, EUVoltage.MV));
    public static final DeferredItem<RechargeableBatteryItem> ENERGY_CRYSTAL = registerItem("energy_crystal",
        props -> new RechargeableBatteryItem(props, 1_000_000, EUVoltage.HV));
    public static final DeferredItem<RechargeableBatteryItem> LAPOTRON_CRYSTAL = registerItem("lapotron_crystal",
        props -> new RechargeableBatteryItem(props, 10_000_000, EUVoltage.EV));

    // 一次性电池 (Single-Use Battery) - 注册名复用自 industrialupgrade
    public static final DeferredItem<Item> SINGLE_USE_BATTERY = registerSimpleItem("single_use_battery");

    public static <T extends Item> DeferredItem<T> registerItem(String name, Function<Item.Properties, T> factory) {
        return ITEMS.registerItem(name, factory);
    }

    public static DeferredItem<Item> registerSimpleItem(String name) {
        return ITEMS.registerSimpleItem(name);
    }
}
