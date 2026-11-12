package machine.perfect_hybrid_solar_generator;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import machine.SolarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 完美混合太阳能发电机：EV，512 EU/t，缓冲 64000 EU。 */
public class PerfectHybridSolarGeneratorBlockEntity extends SolarBlockEntity {
    public PerfectHybridSolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PERFECT_HYBRID_SOLAR_GENERATOR.get(), pos, state, EUVoltage.EV, 512, 64000, "完美混合太阳能发电机");
    }
}
