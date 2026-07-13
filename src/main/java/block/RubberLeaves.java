package block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RubberLeaves extends LeavesBlock {

    private static final VoxelShape COLLISION_SHAPE = Shapes.box(0, 0, 0, 1, 1, 1);

    public static final MapCodec<RubberLeaves> CODEC = BlockBehaviour.simpleCodec(RubberLeaves::new);

    public RubberLeaves(BlockBehaviour.Properties properties) {
        super(0.1F, properties);
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    public static BlockBehaviour.Properties defaultProps() {
        return Properties.of()
            .mapColor(MapColor.PLANT)
            .strength(0.2F)
            .randomTicks()
            .sound(SoundType.GRASS)
            .noOcclusion()
            .isValidSpawn((BlockState state, BlockGetter level, BlockPos pos, EntityType<?> type) -> false)
            .isSuffocating((BlockState state, BlockGetter level, BlockPos pos) -> false)
            .isViewBlocking((BlockState state, BlockGetter level, BlockPos pos) -> false);
    }

    @Override
    public void spawnFallingLeavesParticle(Level level, BlockPos pos, RandomSource random) {
        // No falling leaves particle for rubber leaves
    }

    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 30;
    }

    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }
}
