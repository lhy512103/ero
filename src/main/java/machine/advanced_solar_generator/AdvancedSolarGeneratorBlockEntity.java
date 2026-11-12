package machine.advanced_solar_generator;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import machine.SolarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 高级太阳能发电机：MV，8 EU/t，缓冲 1000 EU。 */
public class AdvancedSolarGeneratorBlockEntity extends SolarBlockEntity {
    public AdvancedSolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ADVANCED_SOLAR_GENERATOR.get(), pos, state, EUVoltage.MV, 8, 1000, "高级太阳能发电机");
    }
}
