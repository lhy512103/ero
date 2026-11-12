package item;

import block.RubberWood;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class TreetapItem extends Item {

    private static final int MAX_DAMAGE = 16;

    public TreetapItem(Properties properties) {
        super(properties.durability(MAX_DAMAGE));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.@Nullable TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        int damage = stack.getOrDefault(net.minecraft.core.component.DataComponents.DAMAGE, 0);
        int remaining = MAX_DAMAGE - damage;
        builder.accept(Component.translatable("item.iuce.treetap.durability", remaining, MAX_DAMAGE));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof RubberWood)) {
            return InteractionResult.PASS;
        }

        Direction facing = state.getValue(RubberWood.FACING);
        boolean wet = state.getValue(RubberWood.WET);

        // facing=UP means plain (no resin), skip
        if (facing == Direction.UP || facing != clickedFace) {
            return InteractionResult.PASS;
        }

        if (wet) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(RubberWood.WET, false), 3);
                int count = level.getRandom().nextInt(3) + 1;
                ejectResin(level, pos, clickedFace, count);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (context.getPlayer() != null) {
                    EquipmentSlot slot = context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                    context.getItemInHand().hurtAndBreak(1, context.getPlayer(), slot);
                }
            }
            return InteractionResult.SUCCESS;
        } else {
            if (!level.isClientSide()) {
                if (level.getRandom().nextInt(5) == 0) {
                    level.setBlock(pos, state.setValue(RubberWood.FACING, Direction.UP), 3);
                    ejectResin(level, pos, clickedFace, 1);
                    if (context.getPlayer() != null) {
                        EquipmentSlot slot = context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                        context.getItemInHand().hurtAndBreak(1, context.getPlayer(), slot);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.PASS;
        }
    }

    private static void ejectResin(Level level, BlockPos pos, Direction side, int quantity) {
        double x = pos.getX() + 0.5 + side.getStepX() * 0.3;
        double y = pos.getY() + 0.5 + side.getStepY() * 0.3;
        double z = pos.getZ() + 0.5 + side.getStepZ() * 0.3;
        ItemEntity itemEntity = new ItemEntity(level, x, y, z,
            new ItemStack(ModItems.RAW_LATEX.get(), quantity));
        level.addFreshEntity(itemEntity);
    }
}
