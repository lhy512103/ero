package block;

import com.mojang.serialization.MapCodec;
import init.ModFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.Optional;

public class RubberSapling extends SaplingBlock {

    public static final MapCodec<RubberSapling> CODEC = BlockBehaviour.simpleCodec(RubberSapling::new);

    public RubberSapling(BlockBehaviour.Properties properties) {
        super(new TreeGrower("rubber_tree", 0.1F, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()), properties);
    }

    @Override
    public MapCodec<? extends SaplingBlock> codec() {
        return CODEC;
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
        } else {
            ModFeatures.RUBBER_TREE.get().placeInstantly(level, pos, random);
        }
    }

    public static BlockBehaviour.Properties defaultProps() {
        return Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollision()
            .randomTicks()
            .instabreak()
            .sound(SoundType.GRASS);
    }
}
