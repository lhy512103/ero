package item;

import machine.process.PlasticComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class PlasticPailItem extends Item {
    public PlasticPailItem(Properties properties) { super(properties.stacksTo(16)); }

    private void exchange(Player player, InteractionHand hand, Fluid fluid) {
        ItemStack result = new ItemStack(this);
        if (fluid != Fluids.EMPTY) result.set(PlasticComponents.PAIL_FLUID.get(), BuiltInRegistries.FLUID.getKey(fluid));
        player.setItemInHand(hand, ItemUtils.createFilledResult(player.getItemInHand(hand), player, result));
    }
    private Fluid fluid(ItemStack stack) {
        var key = stack.get(PlasticComponents.PAIL_FLUID.get());
        return key == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.getValue(key);
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext ctx) {
        var player = ctx.getPlayer();
        var handler = ctx.getLevel().getCapability(Capabilities.Fluid.BLOCK, ctx.getClickedPos(),ctx.getClickedFace());
        if (player == null || handler == null) return InteractionResult.PASS;
        Fluid stored = fluid(stack);
        for (int i = 0; i < handler.size(); i++) {
            try (Transaction tx = Transaction.openRoot()) {
                FluidResource resource = stored == Fluids.EMPTY ? handler.getResource(i) : FluidResource.of(stored);
                if (resource.isEmpty() || resource.getFluid().getFluidType().getDensity() < 0) continue;
                int moved = stored == Fluids.EMPTY ? handler.extract(i,resource,1000,tx) : handler.insert(i,resource,1000,tx);
                if (moved != 1000) continue;
                if (!ctx.getLevel().isClientSide()) {
                    tx.commit();
                    exchange(player,ctx.getHand(),stored == Fluids.EMPTY ? resource.getFluid() : Fluids.EMPTY);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.FAIL;
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        Fluid stored = fluid(stack);
        var hit = getPlayerPOVHitResult(level,player, stored == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        var pos = hit.getBlockPos();
        if (hit.getType() != HitResult.Type.BLOCK || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos.relative(hit.getDirection()),hit.getDirection(),stack)) return InteractionResult.PASS;
        var state = level.getBlockState(pos);
        if (stored == Fluids.EMPTY) {
            var liquid = state.getFluidState();
            if (!liquid.isSource() || !(liquid.getType()==Fluids.WATER || liquid.getType()==Fluids.LAVA) || !(state.getBlock() instanceof BucketPickup pickup)) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                if (pickup.pickupBlock(player,level,pos,state).isEmpty()) return InteractionResult.FAIL;
                exchange(player,hand,liquid.getType());
            }
        } else {
            if (!(stored.getBucket() instanceof BucketItem bucket)) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                if (!bucket.emptyContents(player,level,pos.relative(hit.getDirection()),hit,stack)) return InteractionResult.FAIL;
                exchange(player,hand,Fluids.EMPTY);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
