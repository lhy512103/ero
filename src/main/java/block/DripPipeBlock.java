package block;

import blockentity.DripPipeBlockEntity;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class DripPipeBlock extends Block implements EntityBlock {
    public DripPipeBlock(Properties properties) { super(properties); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) { b.add(BlockStateProperties.HORIZONTAL_FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c) { return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,c.getHorizontalDirection()); }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return new DripPipeBlockEntity(pos,state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide() ? null : (l,p,s,entity)->{ if ((l.getGameTime()+p.asLong())%20==0 && entity instanceof DripPipeBlockEntity pipe) pipe.refresh(); };
    }
}
