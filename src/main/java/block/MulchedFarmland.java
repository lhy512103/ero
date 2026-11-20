package block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.neoforged.neoforge.event.level.BlockEvent;

public class MulchedFarmland extends FarmlandBlock {
    public static final BooleanProperty PLA = BooleanProperty.create("pla");
    public static final IntegerProperty HARVESTS = IntegerProperty.create("harvests",0,7);
    public MulchedFarmland(Properties p) {
        super(p);
        registerDefaultState(defaultBlockState().setValue(MOISTURE,7).setValue(PLA,false).setValue(HARVESTS,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(PLA,HARVESTS);
    }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        var crop=level.getBlockState(pos.above());
        if (crop.getBlock() instanceof CropBlock && random.nextBoolean()) crop.randomTick(level,pos.above(),random);
    }
    public static void harvested(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        if (!(event.getState().getBlock() instanceof CropBlock crop) || !crop.isMaxAge(event.getState())) return;
        var level=event.getLevel();var pos=event.getPos().below();var soil=level.getBlockState(pos);
        if (!soil.is(ModBlocks.MULCHED_FARMLAND.get()) || !soil.getValue(PLA)) return;
        int n=soil.getValue(HARVESTS);
        level.setBlock(pos,n==7 ? Blocks.FARMLAND.defaultBlockState().setValue(MOISTURE,7) : soil.setValue(HARVESTS,n+1),3);
    }
}
