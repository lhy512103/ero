package machine.utility;

import blockentity.MachineBlockEntity;
import blockentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Creative-only utility source: hands out as many STEAM, COOLING_WATER and AIR shares as any
 * machine asks for, so a test world or a creative build needs no boiler or cooling tower.
 */
public class CreativeUtilitySourceBlockEntity extends MachineBlockEntity implements UtilitySupplier {

    public CreativeUtilitySourceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_UTILITY_SOURCE.get(), pos, state);
    }

    @Override public String getMachineName() { return "creative_utility_source"; }

    public Component getDisplayName() { return Component.translatable("block.iuce.creative_utility_source"); }

    /** The network asks per utility; the source supplies all of them without limit. */
    @Override public int shares(Utility utility) { return Integer.MAX_VALUE / 4; }

    @Override public void setOccupied(Utility utility, int occupied) { }

    @Override public BlockPos position() { return worldPosition; }

    @Override public void setChanged() { }

    @Override public void setRemoved() {
        if (level instanceof net.minecraft.server.level.ServerLevel server) UtilityNetwork.of(server).remove(worldPosition);
        super.setRemoved();
    }
}
