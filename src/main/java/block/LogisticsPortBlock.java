package block;

import blockentity.LogisticsPortBlockEntity;
import com.mojang.serialization.MapCodec;
import logistics.LogisticsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Logistics interface I. FACING points at the machine or container it serves; the pipe goes on
 * the opposite side. Right-click with an item to only let that item through, with an empty hand to
 * switch between both ways / receive only / supply only, sneaking with an empty hand to clear the filter.
 */
public class LogisticsPortBlock extends BaseEntityBlock {
    public static final MapCodec<LogisticsPortBlock> CODEC = BlockBehaviour.simpleCodec(LogisticsPortBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction direction : Direction.values()) {
            double[] plate = {2, 2, 2, 14, 14, 14}, stub = {5, 5, 5, 11, 11, 11};
            int axis = direction.getAxis().ordinal();
            boolean positive = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            plate[axis] = positive ? 12 : 0;
            plate[axis + 3] = positive ? 16 : 4;
            stub[axis] = positive ? 0 : 4;
            stub[axis + 3] = positive ? 12 : 16;
            SHAPES.put(direction, Shapes.or(Block.box(plate[0], plate[1], plate[2], plate[3], plate[4], plate[5]),
                Block.box(stub[0], stub[1], stub[2], stub[3], stub[4], stub[5])));
        }
    }

    public LogisticsPortBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    /** Faces the block it was placed against. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new LogisticsPortBlockEntity(pos, state); }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        LogisticsManager.register(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        LogisticsManager.invalidate(level);
    }

    public static Component describe(LogisticsPortBlockEntity port) {
        Component filter = port.getFilter() == null ? Component.translatable("message.iuce.port.any")
            : new ItemStack(port.getFilter()).getHoverName();
        return Component.translatable("message.iuce.port.status",
            Component.translatable("message.iuce.port.mode." + port.getMode()).withStyle(ChatFormatting.YELLOW), filter);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty() || stack.getItem() instanceof item.ConfiguratorItem) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LogisticsPortBlockEntity port) {
            port.configure(port.getMode(), stack.getItem());
            player.sendOverlayMessage(describe(port));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LogisticsPortBlockEntity port) {
            if (player.isShiftKeyDown()) port.configure(port.getMode(), null);
            else port.configure((port.getMode() + 1) % 3, port.getFilter());
            player.sendOverlayMessage(describe(port));
        }
        return InteractionResult.SUCCESS;
    }
}
