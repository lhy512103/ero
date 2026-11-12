package block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.EnumMap;
import java.util.Map;

/**
 * Conveyor I: carries loose items (and nudges mobs) along its facing and feeds them into the
 * machine or container at its end. It has no block entity and needs no power.
 * <ul>
 * <li>Turns: a belt simply hands its items to the next belt, whichever way that one faces.</li>
 * <li>Climbing: a belt whose next belt sits one block higher turns into a ramp by itself.</li>
 * <li>Descending: items drop off the end onto the belt below.</li>
 * </ul>
 */
public class ConveyorBlock extends Block {
    public static final MapCodec<ConveyorBlock> CODEC = BlockBehaviour.simpleCodec(ConveyorBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** True when the belt it feeds is one block higher: this belt is a ramp. */
    public static final BooleanProperty ASCENDING = BooleanProperty.create("ascending");
    /** Blocks per tick the belt asks for (ground friction makes the real speed about 1.5 blocks per second). */
    public static final double SPEED = 0.125;
    private static final VoxelShape FLAT = Block.box(0, 0, 0, 16, 4, 16);
    private static final Map<Direction, VoxelShape> RAMPS = new EnumMap<>(Direction.class);

    static {
        int[] tops = {2, 5, 8, 11};
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (int step = 0; step < 4; step++) {
                // defined for north (rising towards z = 0), then turned to the facing
                double x1 = 0, x2 = 16, z1 = 12 - step * 4, z2 = 16 - step * 4, y = tops[step];
                shape = Shapes.or(shape, switch (direction) {
                    case SOUTH -> Block.box(16 - x2, 0, 16 - z2, 16 - x1, y, 16 - z1);
                    case WEST -> Block.box(z1, 0, 16 - x2, z2, y, 16 - x1);
                    case EAST -> Block.box(16 - z2, 0, x1, 16 - z1, y, x2);
                    default -> Block.box(x1, 0, z1, x2, y, z2);
                });
            }
            RAMPS.put(direction, shape);
        }
    }

    public ConveyorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(ASCENDING, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ASCENDING);
    }

    /** Runs the way the player looks; sneaking reverses it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) facing = facing.getOpposite();
        return defaultBlockState().setValue(FACING, facing)
            .setValue(ASCENDING, feedsHigherBelt(context.getLevel(), context.getClickedPos(), facing));
    }

    private static boolean feedsHigherBelt(BlockGetter level, BlockPos pos, Direction facing) {
        return level.getBlockState(pos.relative(facing).above()).getBlock() instanceof ConveyorBlock;
    }

    /** Recomputes whether the belt at {@code pos} is a ramp. */
    private static void refresh(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ConveyorBlock)) return;
        boolean ascending = feedsHigherBelt(level, pos, state.getValue(FACING));
        if (state.getValue(ASCENDING) != ascending) level.setBlock(pos, state.setValue(ASCENDING, ascending), Block.UPDATE_ALL);
    }

    /** A belt appearing or disappearing changes the belts one step below it. */
    private static void refreshAround(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) refresh(level, pos.relative(direction).below());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide()) return;
        refresh(level, pos);
        refreshAround(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        refreshAround(level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(ASCENDING) ? RAMPS.get(state.getValue(FACING)) : FLAT;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        Direction facing = state.getValue(FACING);
        boolean ascending = state.getValue(ASCENDING);
        if (!ascending && entity.getY() > pos.getY() + 0.5) return;
        Vec3 motion = entity.getDeltaMovement();
        if (!(entity instanceof ItemEntity item)) {
            if (entity instanceof Player player && player.isShiftKeyDown()) return;
            entity.setDeltaMovement(motion.add(facing.getStepX() * 0.03, 0, facing.getStepZ() * 0.03));
            return;
        }
        boolean alongX = facing.getAxis() == Direction.Axis.X;
        double along = alongX ? entity.getX() - pos.getX() : entity.getZ() - pos.getZ();
        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) along = 1 - along;
        // pull towards the belt's centre line
        double sideways = alongX ? pos.getZ() + 0.5 - entity.getZ() : pos.getX() + 0.5 - entity.getX();

        if (ascending) {
            // ride the slope; near the top, step onto the higher belt
            double step = along >= 0.8 ? 0.35 : SPEED;
            double y = along >= 0.8 ? pos.getY() + 1.3 : pos.getY() + 0.3 + Math.max(0, along + SPEED) * 0.7;
            item.setPos(entity.getX() + facing.getStepX() * step + (alongX ? 0 : sideways * 0.2), y,
                entity.getZ() + facing.getStepZ() * step + (alongX ? sideways * 0.2 : 0));
            item.setDeltaMovement(facing.getStepX() * SPEED, 0, facing.getStepZ() * SPEED);
            return;
        }

        if (!level.isClientSide() && along >= 0.75) {
            var target = level.getCapability(Capabilities.Item.BLOCK, pos.relative(facing), facing.getOpposite());
            if (target != null) {
                ItemStack stack = item.getItem();
                int moved;
                try (Transaction transaction = Transaction.openRoot()) {
                    moved = target.insert(ItemResource.of(stack), stack.getCount(), transaction);
                    transaction.commit();
                }
                if (moved >= stack.getCount()) {
                    item.discard();
                } else {
                    if (moved > 0) {
                        ItemStack rest = stack.copy();
                        rest.shrink(moved);
                        item.setItem(rest);
                    }
                    item.setDeltaMovement(0, motion.y, 0);   // wait in front of a full target
                }
                return;
            }
        }
        item.setDeltaMovement(alongX ? facing.getStepX() * SPEED : sideways * 0.2, motion.y,
            alongX ? sideways * 0.2 : facing.getStepZ() * SPEED);
    }
}
