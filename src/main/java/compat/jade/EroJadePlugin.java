package compat.jade;

import block.MachineBlock;
import block.cable.CableBlock;
import blockentity.MachineBlockEntity;
import machine.compressor.CompressorBlockEntity;
import machine.electric_furnace.ElectricFurnaceBlockEntity;
import machine.electrolyzer.ElectrolyzerBlockEntity;
import machine.extractor.ExtractorBlockEntity;
import machine.generator.GeneratorBlockEntity;
import machine.geothermal_generator.GeothermalGeneratorBlockEntity;
import machine.induction_furnace.InductionFurnaceBlockEntity;
import machine.macerator.MaceratorBlockEntity;
import machine.ore_washer.OreWasherBlockEntity;
import machine.planter.PlanterBlockEntity;
import machine.pump.PumpBlockEntity;
import machine.workbench.IndustrialWorkbenchBlockEntity;
import machine.solar_generator.SolarGeneratorBlockEntity;
import machine.advanced_solar_generator.AdvancedSolarGeneratorBlockEntity;
import machine.hybrid_solar_generator.HybridSolarGeneratorBlockEntity;
import machine.perfect_hybrid_solar_generator.PerfectHybridSolarGeneratorBlockEntity;
import machine.quantum_solar_generator.QuantumSolarGeneratorBlockEntity;
import machine.singulary_solar_generator.SingularySolarGeneratorBlockEntity;
import machine.storage.BatboxBlockEntity;
import machine.storage.CesuBlockEntity;
import machine.storage.MfeBlockEntity;
import machine.storage.MfsuBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class EroJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        // 发电机 - 显示能量条 + 燃料燃烧进度
        registration.registerBlockDataProvider(GeneratorEnergyProvider.INSTANCE, GeneratorBlockEntity.class);

        // 处理机器 - 显示能量条 + 处理进度
        registration.registerBlockDataProvider(ProcessingMachineProvider.INSTANCE, MaceratorBlockEntity.class);
        registration.registerBlockDataProvider(ProcessingMachineProvider.INSTANCE, ElectricFurnaceBlockEntity.class);
        registration.registerBlockDataProvider(ProcessingMachineProvider.INSTANCE, CompressorBlockEntity.class);
        registration.registerBlockDataProvider(ProcessingMachineProvider.INSTANCE, machine.metal_former.MetalFormerBlockEntity.class);
        registration.registerBlockDataProvider(ProcessingMachineProvider.INSTANCE, ExtractorBlockEntity.class);
        registration.registerBlockDataProvider(ProcessingMachineProvider.INSTANCE, InductionFurnaceBlockEntity.class);
        registration.registerBlockDataProvider(ProcessingMachineProvider.INSTANCE, OreWasherBlockEntity.class);
        registration.registerBlockDataProvider(ProcessingMachineProvider.INSTANCE, ElectrolyzerBlockEntity.class);

        // 储能机器 - 显示能量条
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, machine.transformer.TransformerBlockEntity.class);
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, BatboxBlockEntity.class);
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, CesuBlockEntity.class);
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, MfeBlockEntity.class);
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, MfsuBlockEntity.class);

        // 发电/其他机器 - 显示能量条
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, GeothermalGeneratorBlockEntity.class);
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, PumpBlockEntity.class);
        registration.registerBlockDataProvider(ProcessingMachineProvider.INSTANCE, PlanterBlockEntity.class);

        // 工业工作台 - 显示能量条 + 合成进度
        registration.registerBlockDataProvider(ProcessingMachineProvider.INSTANCE, IndustrialWorkbenchBlockEntity.class);

        // 太阳能发电机 - 显示能量条
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, SolarGeneratorBlockEntity.class);
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, AdvancedSolarGeneratorBlockEntity.class);
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, HybridSolarGeneratorBlockEntity.class);
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, PerfectHybridSolarGeneratorBlockEntity.class);
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, QuantumSolarGeneratorBlockEntity.class);
        registration.registerBlockDataProvider(MachineEnergyProvider.INSTANCE, SingularySolarGeneratorBlockEntity.class);

        // 流体机器 - 显示流体液位
        registration.registerBlockDataProvider(FluidInfoProvider.INSTANCE, PumpBlockEntity.class);
        registration.registerBlockDataProvider(FluidInfoProvider.INSTANCE, OreWasherBlockEntity.class);
        registration.registerBlockDataProvider(FluidInfoProvider.INSTANCE, ElectrolyzerBlockEntity.class);

        // 机器 - 显示升级插件信息
        registration.registerBlockDataProvider(UpgradeInfoProvider.INSTANCE, MachineBlockEntity.class);

        // 导线 - 显示能量缓冲和传输速率
        registration.registerBlockDataProvider(CableEnergyProvider.INSTANCE, blockentity.cable.CableBlockEntity.class);
    }
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(GeneratorEnergyProvider.Client.INSTANCE, MachineBlock.class);
        registration.registerBlockComponent(ProcessingMachineProvider.Client.INSTANCE, MachineBlock.class);
        registration.registerBlockComponent(MachineEnergyProvider.Client.INSTANCE, MachineBlock.class);
        registration.registerBlockComponent(FluidInfoProvider.Client.INSTANCE, MachineBlock.class);
        registration.registerBlockComponent(UpgradeInfoProvider.Client.INSTANCE, MachineBlock.class);
        registration.registerBlockComponent(CableEnergyProvider.Client.INSTANCE, CableBlock.class);
    }
}
