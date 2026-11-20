package blockentity;

import fluid.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.*;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public class PlantTankEntity extends BlockEntity implements MultiTankProvider {
    private final List<MachineTank> tanks;
    private final MachineTanksHandler handler;
    public PlantTankEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLANT_TANK.get(),pos,state);
        tanks=java.util.stream.IntStream.range(0,4).mapToObj(i -> new MachineTank(256000,r->true,this::setChanged)).toList();
        handler=new MachineTanksHandler(tanks,getInputTankCount());
    }
    public List<MachineTank> getTanks(){return tanks;}
    public int getInputTankCount(){return getBlockState().is(block.ModBlocks.FLUID_INPUT_HATCH.get())?4:0;}
    public ResourceHandler<FluidResource> getFluidHandler(){return handler;}
    @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);for(int i=0;i<tanks.size();i++)tanks.get(i).save(out,"tank"+i);}
    @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);for(int i=0;i<tanks.size();i++)in.child("tank"+i).ifPresent(tanks.get(i)::deserialize);}
}
