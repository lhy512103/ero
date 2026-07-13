package block;

import blockentity.ModBlockEntities;
import machine.geothermal_generator.GeothermalGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 地热发电机方块
 * <p>
 * 无GUI交互机器，支持 SHIFT+右键 使用熔岩桶填充内部流体槽。
 * 熔岩桶消耗后返回空桶到玩家物品栏。
 * </p>
 */
public class GeothermalGeneratorBlock extends MachineBlock {

    public GeothermalGeneratorBlock(Properties properties) {
        super(properties, GeothermalGeneratorBlockEntity::new, ModBlockEntities.GEOTHERMAL_GENERATOR);
    }

    /** 无GUI机器，右键不打开任何界面 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    /** 右键熔岩桶直接填充流体槽，阻止流体放置到地面 */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                           BlockPos pos, Player player, InteractionHand hand,
                                           BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return stack.is(Items.LAVA_BUCKET) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        // 拦截熔岩桶，直接填充到机器内部
        if (stack.is(Items.LAVA_BUCKET)) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof GeothermalGeneratorBlockEntity geo) {
                if (geo.addLavaBucket()) {
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

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
