package item;

import block.ModBlocks;
import block.MulchedFarmland;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

public class MulchFilmItem extends Item {
    public MulchFilmItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext ctx) {
        var level=ctx.getLevel();var pos=ctx.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.FARMLAND)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlockAndUpdate(pos,ModBlocks.MULCHED_FARMLAND.get().defaultBlockState().setValue(MulchedFarmland.PLA,this==ModItems.MULCH_FILM_PLA.get()));
            ctx.getItemInHand().consume(1,ctx.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }
}
