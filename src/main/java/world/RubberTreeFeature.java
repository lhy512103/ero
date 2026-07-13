package world;

import block.ModBlocks;
import block.RubberWood;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class RubberTreeFeature extends Feature<NoneFeatureConfiguration> {

    private static final Direction[] HORIZONTALS = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    public RubberTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        BlockPos origin = context.origin();
        int chunkX = origin.getX() >> 4;
        int chunkZ = origin.getZ() >> 4;
        int hash = Math.abs((chunkX * 73856093) ^ (chunkZ * 19349663)) % 10;
        if (hash != 0) {
            return false;
        }
        return placeTree(context.level(), origin, context.random(), true);
    }

    public boolean placeInstantly(ServerLevel level, BlockPos origin, RandomSource random) {
        return placeTree(level, origin, random, false);
    }

    private boolean placeTree(WorldGenLevel level, BlockPos origin, RandomSource random, boolean checkGround) {
        int height = 4 + random.nextInt(2);

        for (int y = 0; y <= height + 1; y++) {
            int radius = y <= 1 ? 0 : 2;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (y == 0 && x == 0 && z == 0) continue;
                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir() && !state.is(net.minecraft.tags.BlockTags.LEAVES)) {
                        return false;
                    }
                }
            }
        }

        if (checkGround) {
            BlockPos below = origin.below();
            BlockState ground = level.getBlockState(below);
            if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.DIRT)) {
                return false;
            }
        }

        // Place trunk with random resin face
        BlockState wood = ModBlocks.RUBBER_WOOD.get().defaultBlockState()
            .setValue(RubberWood.AXIS, Direction.Axis.Y);

        for (int y = 0; y < height; y++) {
            BlockPos pos = origin.above(y);
            BlockState state = level.getBlockState(pos);

            if (state.isAir() || state.is(net.minecraft.tags.BlockTags.LEAVES) || (y == 0)) {
                Direction facing = HORIZONTALS[random.nextInt(4)];
                if (y == 0 || (y < height - 1 && random.nextInt(100) < 40)) {
                    // Resin face (random direction) + wet
                    level.setBlock(pos, wood
                        .setValue(RubberWood.FACING, facing)
                        .setValue(RubberWood.WET, true), 3);
                } else {
                    // Plain trunk (facing=UP, no resin)
                    level.setBlock(pos, wood
                        .setValue(RubberWood.FACING, Direction.UP)
                        .setValue(RubberWood.WET, false), 3);
                }
            }
        }

        // Place leaves crown - matching vanilla oak tree
        BlockState leaves = ModBlocks.RUBBER_LEAVES.get().defaultBlockState();

        for (int y = height - 2; y <= height + 1; y++) {
            int radius = y >= height ? 1 : 2;

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) == radius && Math.abs(z) == radius) {
                        if (random.nextBoolean()) continue;
                    }

                    BlockPos pos = origin.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        level.setBlock(pos, leaves, 3);
                    }
                }
            }
        }

        return true;
    }
}
