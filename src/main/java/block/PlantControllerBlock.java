package block;

import blockentity.ModBlockEntities;
import blockentity.PlantControllerBlockEntity;
import com.mojang.serialization.MapCodec;
import item.MoldItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * Controller of a large machine. Its front faces outwards.
 * Right-click: open the controller panel. Right-click with plant casing: the
 * casing is placed into every free position of the shell. Right-click a rolling mill with a mold:
 * the mold is put in (the previous one comes back).
 */
public class PlantControllerBlock extends BaseEntityBlock {
    public static final MapCodec<PlantControllerBlock> CODEC = BlockBehaviour.simpleCodec(PlantControllerBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty LEGACY = BooleanProperty.create("legacy");
    /** Length the structure is built to; only the rolling mill uses anything but the minimum. Set with the blueprint terminal. */
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty LENGTH =
        net.minecraft.world.level.block.state.properties.IntegerProperty.create("length", 9, 33);

    public PlantControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(FORMED, false).setValue(LENGTH, 9).setValue(LEGACY, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) && multiblock.PlantStructure.formedModel(state) != null ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state) {
        return state.getValue(FORMED) && multiblock.PlantStructure.formedModel(state) != null
            ? net.minecraft.world.phys.shapes.Shapes.empty() : super.getOcclusionShape(state);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return (state.getValue(FORMED) && multiblock.PlantStructure.formedModel(state) != null) || super.propagatesSkylightDown(state);
    }

    @Override
    protected float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return state.getValue(FORMED) && multiblock.PlantStructure.formedModel(state) != null ? 1.0F : super.getShadeBrightness(state, level, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, FORMED, LENGTH, LEGACY); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(LEGACY, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PlantControllerBlockEntity(pos, state); }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
            : createTickerHelper(type, ModBlockEntities.PLANT_CONTROLLER.get(), (l, pos, s, controller) -> controller.tick());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        boolean casing = stack.is(ModBlocks.PLANT_CASING.get().asItem());
        boolean mold = stack.getItem() instanceof MoldItem && state.is(ModBlocks.MILL_CONTROLLER.get());
        if (!casing && !mold) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof PlantControllerBlockEntity controller)) return InteractionResult.SUCCESS;
        if (casing) {
            if (player.isShiftKeyDown() && player.hasInfiniteMaterials() && !controller.isFormed()) {
                int placed = controller.assembleCreative();
                player.sendOverlayMessage(placed > 0
                    ? Component.translatable("message.iuce.plant.creative_assembled", placed)
                    : controller.describe());
                return InteractionResult.SUCCESS;
            }
            int placed = controller.fill(stack);
            if (!player.hasInfiniteMaterials()) stack.shrink(placed);
            controller.tick();
            player.sendOverlayMessage(placed > 0 ? Component.translatable("message.iuce.plant.filled", placed) : controller.describe());
        } else {
            ItemStack old = controller.swapMold(stack);
            if (!player.hasInfiniteMaterials()) stack.shrink(1);
            if (!old.isEmpty() && !player.getInventory().add(old)) player.drop(old, false);
            player.sendOverlayMessage(controller.describe());
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * A controller whose material store holds many blocks only comes off while sneaking, so it is
     * not broken by accident; the stored blocks stay in the dropped controller either way.
     */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, ItemStack tool,
                                       boolean willHarvest, net.minecraft.world.level.material.FluidState fluid) {
        if (level.getBlockEntity(pos) instanceof PlantControllerBlockEntity controller && !player.isShiftKeyDown()) {
            long stored = controller.materials().total();
            if (stored > PlantControllerBlockEntity.STORAGE_WARNING) {
                if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.iuce.plant.storage_warning", stored));
                return false;
            }
        }
        return super.onDestroyedByPlayer(state, level, pos, player, tool, willHarvest, fluid);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PlantControllerBlockEntity controller) {
            if (player.isShiftKeyDown()) {
                if (!controller.getMold().isEmpty()) {
                    ItemStack old = controller.swapMold(ItemStack.EMPTY);
                    if (!player.getInventory().add(old)) player.drop(old, false);
                } else if (player.hasInfiniteMaterials() && !controller.isFormed()) {
                    int placed = controller.assembleCreative();
                    player.sendOverlayMessage(placed > 0
                        ? Component.translatable("message.iuce.plant.creative_assembled", placed)
                        : controller.describe());
                    return InteractionResult.SUCCESS;
                }
            }
            if (player.isShiftKeyDown()) player.sendOverlayMessage(controller.describe());
            else multiblock.MultiblockMenu.open(player, controller);
        }
        return InteractionResult.SUCCESS;
    }
}
