package block;

import blockentity.ModBlockEntities;
import item.UpgradeItem;
import machine.pump.PumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;


/**
 * 泵方块
 * <p>
 * 支持水中放置（水下不占位置，保持水源方块）、右键水桶填充水槽。
 * 实现 SimpleWaterloggedBlock 接口，放置在水中时自动变为水下状态。
 * </p>
 */
public class PumpBlock extends MachineBlock implements SimpleWaterloggedBlock {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public PumpBlock(Properties properties) {
        super(properties, PumpBlockEntity::new, ModBlockEntities.PUMP);
        this.registerDefaultState(this.defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        BlockState state = super.getStateForPlacement(context);
        if (state != null) {
            state = state.setValue(WATERLOGGED, fluidState.is(Fluids.WATER) && fluidState.isSource());
        }
        return state;
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        if (state.getValue(WATERLOGGED)) {
            return Fluids.WATER.getSource(false);
        }
        return super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess tickAccess,
                                      BlockPos pos, Direction direction, BlockPos neighborPos,
                                      BlockState neighborState, net.minecraft.util.RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            tickAccess.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, level, tickAccess, pos, direction, neighborPos, neighborState, random);
    }

    /** 右键水桶直接填充水槽，右键升级插件直接安装，Shift+右键取出升级 */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                           BlockPos pos, Player player, InteractionHand hand,
                                           BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return stack.is(Items.WATER_BUCKET) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        // 拦截水桶，直接填充到机器内部
        if (stack.is(Items.WATER_BUCKET)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PumpBlockEntity pump) {
                if (pump.addWaterBucket()) {
                    stack.shrink(1);
                    ItemStack emptyBucket = new ItemStack(Items.BUCKET);
                    if (!player.getInventory().add(emptyBucket)) {
                        player.drop(emptyBucket, false);
                    }
                    return InteractionResult.SUCCESS;
                }
                // 存储已满，不消耗桶
                return InteractionResult.PASS;
            }
        }

        // Shift+右键取出升级插件
        if (player.isShiftKeyDown()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PumpBlockEntity pump) {
                ItemStacksResourceHandler itemHandler = pump.getItemHandler();
                if (itemHandler != null) {
                    int upgradeSlotCount = pump.getUpgradeSlotCount();
                    int upgradeSlotStart = pump.getUpgradeSlotStart();

                    // 从最后一个升级槽开始查找有升级的槽位
                    for (int i = upgradeSlotCount - 1; i >= 0; i--) {
                        int slotIndex = upgradeSlotStart + i;
                        ItemResource resource = itemHandler.getResource(slotIndex);
                        int amount = itemHandler.getAmountAsInt(slotIndex);

                        if (!resource.isEmpty() && amount > 0) {
                            // 找到升级，取出到玩家手中
                            ItemStack upgradeStack = resource.toStack(amount);
                            try (Transaction tx = Transaction.openRoot()) {
                                int extracted = itemHandler.extract(slotIndex, resource, amount, tx);
                                if (extracted > 0) {
                                    tx.commit();
                                    if (!player.getInventory().add(upgradeStack)) {
                                        player.drop(upgradeStack, false);
                                    }
                                    player.sendSystemMessage(
                                        Component.literal("已取出升级").withStyle(ChatFormatting.YELLOW)
                                    );
                                    return InteractionResult.SUCCESS;
                                }
                            }
                        }
                    }

                    // 没有可取出的升级，静默返回
                    return InteractionResult.PASS;
                }
            }
            return InteractionResult.PASS;
        }

        // 拦截升级插件，直接安装到升级槽
        if (stack.getItem() instanceof UpgradeItem) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PumpBlockEntity pump) {
                ItemStacksResourceHandler itemHandler = pump.getItemHandler();
                if (itemHandler != null) {
                    int upgradeSlotCount = pump.getUpgradeSlotCount();
                    int upgradeSlotStart = pump.getUpgradeSlotStart();

                    // 查找空的升级槽
                    for (int i = 0; i < upgradeSlotCount; i++) {
                        int slotIndex = upgradeSlotStart + i;
                        ItemResource resource = itemHandler.getResource(slotIndex);
                        int amount = itemHandler.getAmountAsInt(slotIndex);

                        if (resource.isEmpty() || amount <= 0) {
                            // 找到空槽，放入升级物品
                            ItemResource upgradeResource = ItemResource.of(stack);
                            try (Transaction tx = Transaction.openRoot()) {
                                int inserted = itemHandler.insert(slotIndex, upgradeResource, 1, tx);
                                if (inserted > 0) {
                                    tx.commit();
                                    stack.shrink(1);
                                    player.sendSystemMessage(
                                        Component.literal("已安装升级").withStyle(ChatFormatting.GREEN)
                                    );
                                    return InteractionResult.SUCCESS;
                                }
                            }
                        }
                    }

                    player.sendSystemMessage(
                        Component.literal("没有空的升级槽").withStyle(ChatFormatting.RED)
                    );
                    return InteractionResult.FAIL;
                }
            }
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
