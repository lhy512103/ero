package block;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.Block;

public class FluidPipeBlock extends PneumaticPipeBlock {
    public static final IntegerProperty CLOSED = IntegerProperty.create("closed",0,63);
    public FluidPipeBlock(Properties p) { super(p); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { super.createBlockStateDefinition(b); b.add(CLOSED); }
    public static boolean open(BlockState state, Direction face) {
        return !(state.getBlock() instanceof FluidPipeBlock) || (state.getValue(CLOSED) & (1<<face.get3DDataValue()))==0;
    }
}
