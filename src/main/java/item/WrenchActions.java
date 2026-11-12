package item;

import blockentity.MachineBlockEntity;
import blockentity.cable.CableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Both wrenches run before block GUI activation and share exactly the same interactions. */
public final class WrenchActions {
    private WrenchActions() {}
    public static InteractionResult use(UseOnContext context, boolean electric) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var entity = level.getBlockEntity(pos);
        if (!(entity instanceof MachineBlockEntity) && !(entity instanceof CableBlockEntity)) return InteractionResult.PASS;
        if (player == null || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        boolean dismantle = player.isShiftKeyDown();
        long cost = dismantle ? 500 : 50;
        if (electric && !player.hasInfiniteMaterials() && RechargeableBatteryItem.getEnergy(context.getItemInHand()) < cost) {
            if (!level.isClientSide()) player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("message.iuce.wrench.no_energy"));
            return InteractionResult.FAIL;
        }
        EnumProperty<Direction> facing = state.hasProperty(BlockStateProperties.FACING) ? BlockStateProperties.FACING
            : state.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? BlockStateProperties.HORIZONTAL_FACING : null;
        if (!dismantle && !(entity instanceof CableBlockEntity) && facing == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (dismantle) {
            // Resource handlers are not vanilla Containers, so their contents need explicit drops.
            java.util.List<ItemStack> contents = new java.util.ArrayList<>();
            if (entity instanceof MachineBlockEntity machine && machine.getItemHandler() != null) {
                var inventory = machine.getItemHandler();
                for (int slot = 0; slot < inventory.size(); slot++) {
                    contents.add(inventory.getResource(slot).toStack(inventory.getAmountAsInt(slot)));
                }
            }
            if (entity instanceof MachineBlockEntity machine && machine.getItemHandler() != null) {
                var inventory = machine.getItemHandler();
                for (int slot = 0; slot < inventory.size(); slot++)
                    inventory.set(slot, net.neoforged.neoforge.transfer.item.ItemResource.EMPTY, 0);
            }
            if (!level.removeBlock(pos, false)) return InteractionResult.FAIL;
            Block.popResource(level, pos, new ItemStack(state.getBlock()));
            for (ItemStack stack : contents) if (!stack.isEmpty()) Block.popResource(level, pos, stack);
        } else if (entity instanceof CableBlockEntity cable) {
            cable.toggleSide(context.getClickedFace());
        } else {
            Direction next = context.getClickedFace();
            if (!facing.getPossibleValues().contains(next) || next == state.getValue(facing)) {
                var values = new java.util.ArrayList<>(facing.getPossibleValues());
                next = values.get((values.indexOf(state.getValue(facing)) + 1) % values.size());
            }
            if (!level.setBlock(pos, state.setValue(facing, next), 3)) return InteractionResult.FAIL;
        }
        network.EnergyTransferManager.invalidate(level);
        if (!player.hasInfiniteMaterials()) {
            if (electric) RechargeableBatteryItem.setEnergy(context.getItemInHand(), RechargeableBatteryItem.getEnergy(context.getItemInHand()) - cost);
            else context.getItemInHand().hurtAndBreak(dismantle ? 10 : 1, player,
                context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResult.SUCCESS;
    }
}
