package block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Conveyor I: carries loose items (and nudges mobs) along its facing and feeds them into the
 * machine or container at its end. It has no block entity and needs no power.
 */
public class ConveyorBlock extends Block {
    public static final MapCodec<ConveyorBlock> CODEC = BlockBehaviour.simpleCodec(ConveyorBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Blocks per tick: 2.5 blocks per second. */
    public static final double SPEED = 0.125;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public ConveyorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity.getY() > pos.getY() + 0.5) return;
        Direction facing = state.getValue(FACING);
        Vec3 motion = entity.getDeltaMovement();
        if (!(entity instanceof ItemEntity item)) {
            if (entity instanceof Player player && player.isShiftKeyDown()) return;
            entity.setDeltaMovement(motion.add(facing.getStepX() * 0.03, 0, facing.getStepZ() * 0.03));
            return;
        }
        boolean alongX = facing.getAxis() == Direction.Axis.X;
        double along = alongX ? entity.getX() - pos.getX() : entity.getZ() - pos.getZ();
        if (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) along = 1 - along;
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
        // carry along the belt and pull towards its centre line
        double sideways = alongX ? pos.getZ() + 0.5 - entity.getZ() : pos.getX() + 0.5 - entity.getX();
        item.setDeltaMovement(alongX ? facing.getStepX() * SPEED : sideways * 0.2, motion.y,
            alongX ? sideways * 0.2 : facing.getStepZ() * SPEED);
    }
}
