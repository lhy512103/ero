package item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class ElectricHoeItem extends Item {

    private static final long MAX_ENERGY = 10_000L;
    private static final long ENERGY_PER_USE = 50L;

    public ElectricHoeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        long energy = RechargeableBatteryItem.getEnergy(stack);
        if (energy < ENERGY_PER_USE) {
            return InteractionResult.PASS;
        }

        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();

        if (context.getClickedFace() == Direction.DOWN) {
            return InteractionResult.PASS;
        }

        if (block != Blocks.GRASS_BLOCK && block != Blocks.DIRT && block != Blocks.MYCELIUM) {
            return InteractionResult.PASS;
        }

        if (!level.getBlockState(pos.below()).isAir() && block != Blocks.GRASS_BLOCK && block != Blocks.DIRT) {
            return InteractionResult.PASS;
        }

        Block farmland = Blocks.FARMLAND;
        SoundType stepSound = farmland.getSoundType(state, level, pos, context.getPlayer());

        level.playSound(null,
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            stepSound.getStepSound(), SoundSource.BLOCKS,
            (stepSound.getVolume() + 1.0F) / 2.0F,
            stepSound.getPitch() * 0.8F);

        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos, farmland.defaultBlockState());
            RechargeableBatteryItem.setEnergy(stack, energy - ENERGY_PER_USE);
        }

        return InteractionResult.SUCCESS;
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
