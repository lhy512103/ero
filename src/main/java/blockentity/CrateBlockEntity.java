package blockentity;

import block.CrateBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CrateBlockEntity extends DispenserBlockEntity {
    // Same open/close bookkeeping as the vanilla barrel: the lid opens for the first viewer and closes after the last.
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override protected void onOpen(Level level, BlockPos pos, BlockState state) {
            CrateBlockEntity.this.playSound(SoundEvents.BARREL_OPEN);
            CrateBlockEntity.this.setOpen(state, true);
        }
        @Override protected void onClose(Level level, BlockPos pos, BlockState state) {
            CrateBlockEntity.this.playSound(SoundEvents.BARREL_CLOSE);
            CrateBlockEntity.this.setOpen(state, false);
        }
        @Override protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) { }
        @Override public boolean isOwnContainer(Player player) {
            for (Slot slot : player.containerMenu.slots) if (slot.container == CrateBlockEntity.this) return true;
            return false;
        }
    };

    public CrateBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.CRATE.get(), pos, state); }
    @Override protected Component getDefaultName() { return getBlockState().getBlock().getName(); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // The loot table copies the container component onto the crate.
    }

    @Override public void startOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator())
            openersCounter.incrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState(), user.getContainerInteractionRange());
    }
    @Override public void stopOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator())
            openersCounter.decrementOpeners(user.getLivingEntity(), getLevel(), getBlockPos(), getBlockState());
    }
    @Override public List<ContainerUser> getEntitiesWithContainerOpen() {
        return openersCounter.getEntitiesWithContainerOpen(getLevel(), getBlockPos());
    }
    public void recheckOpen() {
        if (!this.remove) openersCounter.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
    }

    private void setOpen(BlockState state, boolean open) {
        if (level != null && state.hasProperty(CrateBlock.OPEN)) level.setBlock(getBlockPos(), state.setValue(CrateBlock.OPEN, open), 3);
    }
    private void playSound(SoundEvent sound) {
        if (level == null) return;
        net.minecraft.core.Direction facing = getBlockState().getValue(CrateBlock.FACING);
        double x = worldPosition.getX() + 0.5 + facing.getStepX() / 2.0;
        double y = worldPosition.getY() + 0.5 + facing.getStepY() / 2.0;
        double z = worldPosition.getZ() + 0.5 + facing.getStepZ() / 2.0;
        level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }
}
