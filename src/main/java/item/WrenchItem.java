package item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class WrenchItem extends Item {

    private static final int MAX_DAMAGE = 128;

    public WrenchItem(Properties properties) {
        super(properties.durability(MAX_DAMAGE));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (state.isAir()) return InteractionResult.PASS;

        EnumProperty<Direction> facingProp = findFacingProperty(state);
        if (facingProp == null) return InteractionResult.PASS;

        if (player != null && player.isShiftKeyDown()) {
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
        if (!state.canSurvive(level, pos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(facingProp, next), 3);
            if (context.getPlayer() != null) {
                EquipmentSlot slot = context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                context.getItemInHand().hurtAndBreak(1, context.getPlayer(), slot);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private InteractionResult removeBlock(Level level, BlockPos pos, BlockState state, UseOnContext context) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            Block.dropResources(state, level, pos, blockEntity);
            level.removeBlock(pos, false);
            if (context.getPlayer() != null) {
                EquipmentSlot slot = context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                context.getItemInHand().hurtAndBreak(10, context.getPlayer(), slot);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.@Nullable TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        int damage = stack.getOrDefault(net.minecraft.core.component.DataComponents.DAMAGE, 0);
        int remaining = MAX_DAMAGE - damage;
        builder.accept(Component.translatable("item.ero.wrench.durability", remaining, MAX_DAMAGE));
    }
}
