package block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ColoredPlasticBlock extends Block {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);
    public ColoredPlasticBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(COLOR, DyeColor.WHITE));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(COLOR); }
    @Override protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = super.getCloneItemStack(level, pos, state, includeData);
        BlockItemStateProperties existing = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
        stack.set(DataComponents.BLOCK_STATE, existing.with(COLOR, state.getValue(COLOR)));
        return stack;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        DyeColor dye = stack.get(DataComponents.DYE);
        if (dye == null || this == ModBlocks.TOY_BRICK.get()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && state.getValue(COLOR) != dye) {
            level.setBlock(pos,state.setValue(COLOR,dye),3);
            stack.consume(1,player);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected boolean skipRendering(BlockState state, BlockState neighbor, Direction side) {
        return neighbor.is(this) && state.getValue(COLOR) == neighbor.getValue(COLOR) || super.skipRendering(state,neighbor,side);
    }
}
