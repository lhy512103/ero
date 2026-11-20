package blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CrateBlockEntity extends DispenserBlockEntity {
    public CrateBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.CRATE.get(), pos, state); }
    @Override protected Component getDefaultName() { return getBlockState().getBlock().getName(); }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // The loot table copies the container component onto the crate.
    }
}
