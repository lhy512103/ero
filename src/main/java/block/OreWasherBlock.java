package block;

import blockentity.ModBlockEntities;
import item.UpgradeItem;
import machine.ore_washer.OreWasherBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * 洗矿机方块
 * <p>
 * 支持 SHIFT+右键 使用水桶填充内部水槽。
 * 水桶消耗后返回空桶到玩家物品栏。
 * </p>
 */
public class OreWasherBlock extends MachineBlock {

    public OreWasherBlock(Properties properties) {
        super(properties, OreWasherBlockEntity::new, ModBlockEntities.ORE_WASHER);
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
            if (blockEntity instanceof OreWasherBlockEntity washer) {
                if (washer.addBucket()) {
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
            if (blockEntity instanceof OreWasherBlockEntity washer) {
                ItemStacksResourceHandler itemHandler = washer.getItemHandler();
                if (itemHandler != null) {
                    int upgradeSlotCount = washer.getUpgradeSlotCount();
                    int upgradeSlotStart = washer.getUpgradeSlotStart();

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
            if (blockEntity instanceof OreWasherBlockEntity washer) {
                ItemStacksResourceHandler itemHandler = washer.getItemHandler();
                if (itemHandler != null) {
                    int upgradeSlotCount = washer.getUpgradeSlotCount();
                    int upgradeSlotStart = washer.getUpgradeSlotStart();

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
