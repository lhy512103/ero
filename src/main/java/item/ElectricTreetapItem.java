package item;

import block.RubberWood;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class ElectricTreetapItem extends Item {

    private static final long MAX_ENERGY = 10_000L;
    private static final long ENERGY_PER_USE = 50L;

    public ElectricTreetapItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = context.getItemInHand();

        if (!(state.getBlock() instanceof RubberWood)) {
            return InteractionResult.PASS;
        }

        Direction facing = state.getValue(RubberWood.FACING);
        boolean wet = state.getValue(RubberWood.WET);

        if (facing == Direction.UP || facing != clickedFace) {
            return InteractionResult.PASS;
        }

        long energy = RechargeableBatteryItem.getEnergy(stack);
        if (energy < ENERGY_PER_USE) {
            return InteractionResult.PASS;
        }

        if (wet) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(RubberWood.WET, false), 3);
                int count = level.getRandom().nextInt(3) + 1;
                ejectResin(level, pos, clickedFace, count);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                RechargeableBatteryItem.setEnergy(stack, energy - ENERGY_PER_USE);
            }
            return InteractionResult.SUCCESS;
        } else {
            if (!level.isClientSide()) {
                if (level.getRandom().nextInt(5) == 0) {
                    level.setBlock(pos, state.setValue(RubberWood.FACING, Direction.UP), 3);
                    ejectResin(level, pos, clickedFace, 1);
                    RechargeableBatteryItem.setEnergy(stack, energy - ENERGY_PER_USE);
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

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        long energy = RechargeableBatteryItem.getEnergy(stack);
        if (energy <= 0 || MAX_ENERGY <= 0) return 0;
        return (int) (13 * energy / MAX_ENERGY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x00FF00;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag flag) {
        long energy = RechargeableBatteryItem.getEnergy(stack);
        builder.accept(Component.literal(RechargeableBatteryItem.formatEnergy(energy) + " / " +
            RechargeableBatteryItem.formatEnergy(MAX_ENERGY) + " EU").withStyle(ChatFormatting.GREEN));
    }
}
