package blockentity;

import fluid.MachineTank;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Fluid tank: 16 buckets of any one fluid. Filled and emptied with buckets, pipes and fluid upgrades. */
public class FluidTankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16_000;
    private final MachineTank tank = new MachineTank(CAPACITY, resource -> true, this::setChanged);

    public FluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_TANK.get(), pos, state);
    }

    public MachineTank getTank() { return tank; }

    public Component describe() {
        if (tank.isEmpty()) return Component.translatable("message.iuce.tank.empty");
        return Component.translatable("message.iuce.tank.status", tank.getResource().getFluid().getFluidType().getDescription(),
            tank.getAmount(), CAPACITY);
    }

    /** A full bucket empties into the tank, an empty one is filled from it. Returns false for anything that is not a bucket. */
    public boolean useBucket(Player player, InteractionHand hand, ItemStack stack) {
        if (stack.is(Items.BUCKET)) {
            Fluid fluid = tank.getResource().getFluid();
            if (tank.getAmount() >= 1000 && fluid.getBucket() != Items.AIR && tank.drain(1000) == 1000) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(fluid.getBucket())));
            }
        } else if (stack.getItem() instanceof BucketItem bucket && bucket.content != Fluids.EMPTY) {
            boolean fits = tank.getSpace() >= 1000 && (tank.isEmpty() || tank.getResource().getFluid() == bucket.content);
            if (fits && tank.fill(bucket.content, 1000) == 1000 && !player.hasInfiniteMaterials()) {
                player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            }
        } else {
            return false;
        }
        player.sendOverlayMessage(describe());
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output, "Tank");
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("Tank").ifPresent(tank::deserialize);
    }
}
