package machine.singulary_solar_generator;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import machine.SolarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 奇点太阳能发电机：LuV，32768 EU/t，缓冲 4096000 EU。 */
public class SingularySolarGeneratorBlockEntity extends SolarBlockEntity {
    public SingularySolarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SINGULARY_SOLAR_GENERATOR.get(), pos, state, EUVoltage.LuV, 32768, 4096000, "奇点太阳能发电机");
    }
}
