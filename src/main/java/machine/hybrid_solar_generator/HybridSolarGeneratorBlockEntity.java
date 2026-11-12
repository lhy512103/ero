package machine.hybrid_solar_generator;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import machine.SolarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 混合太阳能发电机：HV，64 EU/t，缓冲 8000 EU。 */
public class HybridSolarGeneratorBlockEntity extends SolarBlockEntity {
    public HybridSolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HYBRID_SOLAR_GENERATOR.get(), pos, state, EUVoltage.HV, 64, 8000, "混合太阳能发电机");
    }
}
