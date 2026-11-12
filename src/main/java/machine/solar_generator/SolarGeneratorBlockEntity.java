package machine.solar_generator;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import machine.SolarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 太阳能发电机：LV，1 EU/t，缓冲 100 EU。 */
public class SolarGeneratorBlockEntity extends SolarBlockEntity {
    public SolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_GENERATOR.get(), pos, state, EUVoltage.LV, 1, 100, "太阳能发电机");
    }
}
