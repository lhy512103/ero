package block;

import com.mojang.serialization.MapCodec;
import machine.utility.UtilityPlantBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * The boiler and the cooling tower. Both are {@link MachineBlock}-shaped blocks with a running
 * state; they differ only in the {@link UtilityPlantBlockEntity.Kind} they create. Water goes in
 * with a bucket, and a boiler also takes fuel in its slot.
 */
public class UtilityPlantBlock extends BaseEntityBlock {

    /** Only used when a data pack or a structure places the block by codec. */
    public static final MapCodec<UtilityPlantBlock> CODEC = BlockBehaviour.simpleCodec(
        properties -> new UtilityPlantBlock(properties, UtilityPlantBlockEntity.Kind.BOILER));

    private final UtilityPlantBlockEntity.Kind kind;

    public UtilityPlantBlock(Properties properties, UtilityPlantBlockEntity.Kind kind) {
        super(properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState()
            .setValue(MachineBlock.FACING, net.minecraft.core.Direction.NORTH)
            .setValue(MachineBlock.RUNNING, false));
    }

    public UtilityPlantBlockEntity.Kind kind() { return kind; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UtilityPlantBlockEntity(pos, state, kind);
    }

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MachineBlock.FACING, MachineBlock.RUNNING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState()
            .setValue(MachineBlock.FACING, context.getHorizontalDirection().getOpposite())
            .setValue(MachineBlock.RUNNING, false);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide() || type != blockentity.ModBlockEntities.UTILITY_PLANT.get()) return null;
        return (tickLevel, pos, tickState, entity) -> {
            if (entity instanceof blockentity.TickableBlockEntity tickable) tickable.tick();
        };
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof UtilityPlantBlockEntity plant) {
            if (plant.useBucket(player, hand, stack)) return InteractionResult.SUCCESS;
            if (plant.insertFuel(player, stack)) {
                player.sendOverlayMessage(plant.describe());
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof UtilityPlantBlockEntity plant) {
            player.sendOverlayMessage(plant.describe());
            if (plant.kind().burnsFuel) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    "message.iuce.utility.shares", plant.occupied(), plant.kind().shares));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof blockentity.MachineBlockEntity machine) machine.onRemove();
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof blockentity.MachineBlockEntity machine) machine.onPlace();
    }
}
