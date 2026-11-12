package machine.quantum_solar_generator;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import machine.SolarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 量子太阳能发电机：IV，4096 EU/t，缓冲 512000 EU。 */
public class QuantumSolarGeneratorBlockEntity extends SolarBlockEntity {
    public QuantumSolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUANTUM_SOLAR_GENERATOR.get(), pos, state, EUVoltage.IV, 4096, 512000, "量子太阳能发电机");
    }
}
