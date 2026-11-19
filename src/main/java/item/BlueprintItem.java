package item;

import block.PlantControllerBlock;
import blockentity.PlantControllerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Blueprint terminal: right-click the controller of a large machine for its bill of materials
 * (what the structure needs in total and what is already there); sneak-right-click a machine of
 * adjustable size to step through its sizes. The chosen size is what the projection, the
 * "fill with casing" click and the builder work towards.
 */
public class BlueprintItem extends Item {
    public BlueprintItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        var level = context.getLevel();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof PlantControllerBlockEntity controller)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() && controller.isMill()) {
            BlockState state = controller.getBlockState();
            int length = state.getValue(PlantControllerBlock.LENGTH);
            length = length >= PlantControllerBlockEntity.MILL_MAX ? PlantControllerBlockEntity.MILL_MIN : length + 1;
            level.setBlock(context.getClickedPos(), state.setValue(PlantControllerBlock.LENGTH, length), Block.UPDATE_ALL);
            controller.markStructureDirty();
        }
        player.sendSystemMessage(controller.summary());
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.iuce.blueprint.1").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.iuce.blueprint.2").withStyle(ChatFormatting.GRAY));
    }
}
