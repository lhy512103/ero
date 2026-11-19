package multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Checks the blocks of a structure a limited number per call, so that forming even a very large
 * machine never holds up a tick: the controller calls {@link #step} once per tick until it
 * reports that it is done.
 */
public final class StructureScan {
    /** Blocks checked per tick. */
    public static final int BUDGET = 4096;

    public interface Visitor {
        /**
         * @param state the block at the position, or null when its chunk is not loaded
         * @return false to stop: the structure is not valid
         */
        boolean visit(BlockPos pos, @Nullable BlockState state);
    }

    private final List<BlockPos> positions;
    private int index;

    public StructureScan(List<BlockPos> positions) { this.positions = positions; }

    /** Checks up to {@code budget} more positions; returns true once the scan is over. */
    public boolean step(Level level, int budget, Visitor visitor) {
        int end = Math.min(positions.size(), index + budget);
        while (index < end) {
            BlockPos pos = positions.get(index++);
            if (!visitor.visit(pos, level.isLoaded(pos) ? level.getBlockState(pos) : null)) return true;
        }
        return index >= positions.size();
    }
}
