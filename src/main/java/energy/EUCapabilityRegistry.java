package energy;

import blockentity.ModBlockEntities;
import item.ModItems;
import item.RechargeableBatteryItem;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class EUCapabilityRegistry {

    private EUCapabilityRegistry() {}

    public static void registerAll(RegisterCapabilitiesEvent event) {
        // 发电机
        register(event, ModBlockEntities.GENERATOR.get());
        register(event, ModBlockEntities.GEOTHERMAL_GENERATOR.get());
        register(event, ModBlockEntities.SOLAR_GENERATOR.get());
        register(event, ModBlockEntities.ADVANCED_SOLAR_GENERATOR.get());
        register(event, ModBlockEntities.HYBRID_SOLAR_GENERATOR.get());
        register(event, ModBlockEntities.PERFECT_HYBRID_SOLAR_GENERATOR.get());
        register(event, ModBlockEntities.QUANTUM_SOLAR_GENERATOR.get());
        register(event, ModBlockEntities.SINGULARY_SOLAR_GENERATOR.get());

        // 耗电机器
        register(event, ModBlockEntities.MACERATOR.get());
        register(event, ModBlockEntities.ELECTRIC_FURNACE.get());
        register(event, ModBlockEntities.COMPRESSOR.get());
        register(event, ModBlockEntities.METAL_FORMER.get());
        register(event, ModBlockEntities.EXTRACTOR.get());
        register(event, ModBlockEntities.INDUCTION_FURNACE.get());
        register(event, ModBlockEntities.ORE_WASHER.get());
        register(event, ModBlockEntities.ELECTROLYZER.get());
        register(event, machine.process.ProcessMachines.BLOCK_ENTITY.get());

        // 储能箱
        register(event, ModBlockEntities.TRANSFORMER.get());
        register(event, ModBlockEntities.BATBOX.get());
        register(event, ModBlockEntities.CESU.get());
        register(event, ModBlockEntities.MFE.get());
        register(event, ModBlockEntities.MFSU.get());

        // 泵
        register(event, ModBlockEntities.PUMP.get());

        // 种植机
        register(event, ModBlockEntities.PLANTER.get());

        // 工业工作台
        register(event, ModBlockEntities.INDUSTRIAL_WORKBENCH.get());

        // 公用工程：冷却塔耗电，锅炉不要电但共用同一个方块实体
        register(event, ModBlockEntities.UTILITY_PLANT.get());

        // 滑槽：任意面都可以放入和取出
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.CHUTE.get(), (chute, direction) -> chute.getInventory());
        // 工厂中枢：只进不出
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.FACTORY_HUB.get(), (hub, direction) -> hub.getIntake());
        // 料仓与储罐
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.SILO.get(), (silo, direction) -> silo.getInventory());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.FLUID_TANK.get(), (tank, direction) -> tank.getTank());
        // 大机器的仓口
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.ITEM_HATCH.get(), (hatch, direction) -> hatch.external());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, ModBlockEntities.PLANT_TANK.get(), (hatch, side) -> hatch.getFluidHandler());
        // 大机器控制器的材料仓：任意面都可以放入和取出结构方块
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.PLANT_CONTROLLER.get(), (controller, side) -> controller.materials());
        register(event, ModBlockEntities.ENERGY_HATCH.get());
        // 电动修理台、电灯、能量转换器
        register(event, ModBlockEntities.REPAIR_BENCH.get());
        register(event, ModBlockEntities.LAMP.get());
        register(event, ModBlockEntities.CONVERTER.get());
        register(event, ModBlockEntities.BUILDER.get());
        // 物流接口本身就是整个管网仓库的入口：漏斗、其他模组的管道、应用能源的存储总线都可以接在它上面
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.LOGISTICS_PORT.get(), (port, direction) -> port.networkInventory());

        // 线缆
        registerCable(event, ModBlockEntities.COPPER_CABLE.get());
        registerCable(event, ModBlockEntities.TIN_CABLE.get());
        registerCable(event, ModBlockEntities.GOLD_CABLE.get());
        registerCable(event, ModBlockEntities.IRON_CABLE.get());
        registerCable(event, ModBlockEntities.GLASS_CABLE.get());
        registerCable(event, ModBlockEntities.SINGULAR_CABLE.get());

        // 充电电池物品能量能力
        registerBatteryItems(event);
    }

    /**
     * 注册充电电池物品的EnergyHandler能力
     */
    private static void registerBatteryItems(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Energy.ITEM,
            (stack, ctx) -> {
                if (stack.getItem() instanceof RechargeableBatteryItem battery) {
                    return new RechargeableBatteryItem.BatteryEnergyHandler(ctx, battery.getCapacity(), battery.getMaxTransfer());
                }
                return null;
            },
            ModItems.RE_BATTERY.get(),
            ModItems.ADVANCED_RE_BATTERY.get(),
            ModItems.ENERGY_CRYSTAL.get(),
            ModItems.LAPOTRON_CRYSTAL.get()
        );

        // 电动工具能量能力
        long toolCapacity = 10_000L;
        long toolTransfer = 1_000L;
        event.registerItem(Capabilities.Energy.ITEM,
            (stack, ctx) -> new RechargeableBatteryItem.BatteryEnergyHandler(ctx, toolCapacity, toolTransfer),
            ModItems.ELECTRIC_TREETAP.get(),
            ModItems.ELECTRIC_WRENCH.get(),
            ModItems.ELECTRIC_HOE.get()
        );
    }

    private static void register(RegisterCapabilitiesEvent event,
                                  net.minecraft.world.level.block.entity.BlockEntityType<?> type) {
        event.registerBlockEntity(EUCapability.BLOCK, type,
            (blockEntity, direction) -> {
                if (blockEntity instanceof blockentity.MachineBlockEntity be) {
                    return be.getEUCapability(direction);
                }
                return null;
            });
        // Standard item / fluid access for hoppers and other mods' pipes.
        event.registerBlockEntity(Capabilities.Item.BLOCK, type,
            (blockEntity, direction) -> blockEntity instanceof blockentity.MachineBlockEntity be
                ? be.getAutomationItemHandler(direction) : null);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, type,
            (blockEntity, direction) -> blockEntity instanceof fluid.MultiTankProvider tanks ? tanks.getFluidHandler()
                : blockEntity instanceof fluid.FluidProvider provider ? provider.getTank() : null);
    }

    private static void registerCable(RegisterCapabilitiesEvent event,
                                       net.minecraft.world.level.block.entity.BlockEntityType<?> type) {
        event.registerBlockEntity(EUCapability.BLOCK, type,
            (blockEntity, direction) -> {
                if (blockEntity instanceof blockentity.cable.CableBlockEntity cable) {
                    return cable.getEUCapability(direction);
                }
                return null;
            });
    }
}
