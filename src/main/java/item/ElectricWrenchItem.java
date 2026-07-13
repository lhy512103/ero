package item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class ElectricWrenchItem extends Item {

    private static final long MAX_ENERGY = 10_000L;
    private static final long ENERGY_PER_ROTATE = 50L;
    private static final long ENERGY_PER_REMOVE = 500L;

    public ElectricWrenchItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.isAir()) return InteractionResult.PASS;

        EnumProperty<Direction> facingProp = findFacingProperty(state);
        if (facingProp == null) return InteractionResult.PASS;

        long energy = RechargeableBatteryItem.getEnergy(context.getItemInHand());
        boolean isSneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        long cost = isSneaking ? ENERGY_PER_REMOVE : ENERGY_PER_ROTATE;

        if (energy < cost) return InteractionResult.PASS;

        if (isSneaking) {
            return removeBlock(level, pos, state, context);
        }
        return rotateBlock(level, pos, state, facingProp, context);
    }

    private EnumProperty<Direction> findFacingProperty(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return BlockStateProperties.FACING;
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return BlockStateProperties.HORIZONTAL_FACING;
        }
        return null;
    }

    private InteractionResult rotateBlock(Level level, BlockPos pos, BlockState state,
                                          EnumProperty<Direction> facingProp, UseOnContext context) {
        Direction current = state.getValue(facingProp);
        Direction next = current.getClockWise(Direction.Axis.Y);
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(facingProp, next), 3);
            long energy = RechargeableBatteryItem.getEnergy(context.getItemInHand());
            RechargeableBatteryItem.setEnergy(context.getItemInHand(), energy - ENERGY_PER_ROTATE);
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult removeBlock(Level level, BlockPos pos, BlockState state, UseOnContext context) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            Block.dropResources(state, level, pos, blockEntity);
            level.removeBlock(pos, false);
            long energy = RechargeableBatteryItem.getEnergy(context.getItemInHand());
            RechargeableBatteryItem.setEnergy(context.getItemInHand(), energy - ENERGY_PER_REMOVE);
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
