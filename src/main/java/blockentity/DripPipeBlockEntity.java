package blockentity;

import block.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.FarmlandWaterManager;
import net.neoforged.neoforge.common.ticket.AABBTicket;

public class DripPipeBlockEntity extends BlockEntity {
    private AABBTicket water;
    public DripPipeBlockEntity(BlockPos p,BlockState s) { super(ModBlockEntities.DRIP_PIPE.get(),p,s); }
    private boolean supplied(Direction direction) {
        BlockPos pos=worldPosition.relative(direction);
        while (level.hasChunkAt(pos)) {
            var state=level.getBlockState(pos);
            if (!state.is(ModBlocks.DRIP_PIPE.get()) || state.getValue(BlockStateProperties.HORIZONTAL_FACING).getAxis()!=direction.getAxis()) {
                if (state.getBlock() instanceof block.FluidPipeBlock) return logistics.LogisticsManager.hasWater(level,pos);
                var handler=level.getCapability(Capabilities.Fluid.BLOCK,pos,direction.getOpposite());
                if (handler!=null) for(int i=0;i<handler.size();i++) if(handler.getAmountAsLong(i)>0 && handler.getResource(i).getFluid().isSame(Fluids.WATER)) return true;
                return false;
            }
            pos=pos.relative(direction);
        }
        return false;
    }
    public void refresh() {
        Direction dir=getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        boolean wet=supplied(dir)||supplied(dir.getOpposite());
        if(wet && water==null) {
            var side=dir.getClockWise();
            water=FarmlandWaterManager.addAABBTicket(level,new AABB(worldPosition).inflate(Math.abs(side.getStepX()),0,Math.abs(side.getStepZ())));
        } else if (!wet) release();
    }
    private void release() { if(water!=null){water.invalidate();water=null;} }
    @Override public void setRemoved() { release();super.setRemoved(); }
    @Override public void onChunkUnloaded() { release();super.onChunkUnloaded(); }
}
