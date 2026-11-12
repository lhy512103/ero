package block;

import blockentity.MachineBlockEntity;
import blockentity.TickableBlockEntity;
// import machine.planter.PlanterBlockEntity;
import machine.GeneratorGuiProvider;
import machine.InductionFurnaceGuiProvider;
import machine.OreWasherGuiProvider;
import machine.ElectrolyzerGuiProvider;
import machine.StorageGuiProvider;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * 通用机器方块基类
 * <p>
 * 所有机器方块的基类，通过构造参数支持不同类型的方块实体。
 * </p>
 */
public class MachineBlock extends BaseEntityBlock {

    public static final MapCodec<MachineBlock> CODEC = BlockBehaviour.simpleCodec(
        props -> new MachineBlock(props, (pos, state) -> null, () -> null));

    /** 方向属性 */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** 是否正在运行的属性 */
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    private final BiFunction<BlockPos, BlockState, ? extends BlockEntity> blockEntityFactory;
    private final Supplier<? extends BlockEntityType<?>> blockEntityTypeSupplier;

    /**
     * 创建通用机器方块
     *
     * @param properties            方块属性
     * @param blockEntityFactory    方块实体工厂方法
     * @param blockEntityTypeSupplier 方块实体类型供应器
     */
    public MachineBlock(Properties properties,
                        BiFunction<BlockPos, BlockState, ? extends BlockEntity> blockEntityFactory,
                        Supplier<? extends BlockEntityType<?>> blockEntityTypeSupplier) {
        super(properties);
        this.blockEntityFactory = blockEntityFactory;
        this.blockEntityTypeSupplier = blockEntityTypeSupplier;
        this.registerDefaultState(this.defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(RUNNING, false));
    }
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return blockEntityFactory.apply(pos, state);
    }
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RUNNING);
    }
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection().getOpposite())
            .setValue(RUNNING, false);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        BlockEntityType<?> expectedType = blockEntityTypeSupplier.get();
        if (type != expectedType) {
            return null;
        }
        return (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof TickableBlockEntity t) {
                t.tick();
            }
        };
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);

            // 空手潜行右键：切换“产物退回进料面”
            if (player.isShiftKeyDown() && blockEntity instanceof MachineBlockEntity returning
                    && returning.getItemHandler() != null && returning.getOutputSlots().length > 0) {
                returning.setReturnProducts(!returning.isReturningProducts());
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(
                    returning.isReturningProducts() ? "message.iuce.return_products.on" : "message.iuce.return_products.off"));
                return InteractionResult.SUCCESS;
            }

            // // Shift+右键切换时间模式（测试模式，需要消耗能量）
            // if (player.isShiftKeyDown() && blockEntity instanceof PlanterBlockEntity planter) {
            //     PlanterBlockEntity.TimeMode before = planter.getTimeMode();
            //     planter.cycleMode();
            //     if (planter.getTimeMode() == before) {
            //         long cost = before == PlanterBlockEntity.TimeMode.NORMAL ? 2_000_000 : 500_000;
            //         player.sendSystemMessage(
            //             Component.literal("能量不足，需要 " + cost + " EU"));
            //     } else {
            //         String mode = planter.getPhaseName();
            //         String debug = planter.getTimeMode() == PlanterBlockEntity.TimeMode.NORMAL ? "" : " [调试]";
            //         player.sendSystemMessage(
            //             Component.literal("时间模式: " + mode + debug));
            //     }
            //     return InteractionResult.SUCCESS;
            // }

            if (blockEntity instanceof MachineBlockEntity machine) {
                if (blockEntity instanceof GeneratorGuiProvider provider) {
                    player.openMenu(provider, pos);
                    return InteractionResult.SUCCESS;
                }
                if (blockEntity instanceof InductionFurnaceGuiProvider provider) {
                    player.openMenu(provider, pos);
                    return InteractionResult.SUCCESS;
                }
                if (blockEntity instanceof StorageGuiProvider provider) {
                    player.openMenu(provider, pos);
                    return InteractionResult.SUCCESS;
                }
                if (blockEntity instanceof OreWasherGuiProvider provider) {
                    player.openMenu(provider, pos);
                    return InteractionResult.SUCCESS;
                }
                if (blockEntity instanceof ElectrolyzerGuiProvider provider) {
                    player.openMenu(provider, pos);
                    return InteractionResult.SUCCESS;
                }
                if (blockEntity instanceof MenuProvider provider) {
                    player.openMenu(provider, pos);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MachineBlockEntity machine) {
                machine.onPlace();
            }
            // 通知相邻导线更新连接状态
            notifyNeighborCables(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MachineBlockEntity machine) {
            machine.onRemove();
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        // 通知相邻导线更新连接状态
        notifyNeighborCables(level, pos);
    }

    /**
     * 通知相邻导线更新连接状态
     */
    private void notifyNeighborCables(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof blockentity.cable.CableBlockEntity cable) {
                cable.updateConnections();
            }
        }
    }

}