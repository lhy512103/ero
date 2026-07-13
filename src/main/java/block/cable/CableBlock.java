package block.cable;

import blockentity.cable.CableBlockEntity;
import blockentity.TickableBlockEntity;
import energy.EUVoltage;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public class CableBlock extends BaseEntityBlock {

    public static final MapCodec<CableBlock> CODEC = BlockBehaviour.simpleCodec(
        props -> new CableBlock(props, EUVoltage.LV, 0.25F, () -> {
            // CODEC反序列化时的fallback，通过BlockState查找正确的类型
            return blockentity.ModBlockEntities.COPPER_CABLE.get();
        }));

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private final EUVoltage voltage;
    private final float thickness;
    private final Supplier<BlockEntityType<CableBlockEntity>> blockEntityTypeSupplier;

    // 细线缆碰撞箱 (4px: 6~10)
    private static final VoxelShape CORE_THIN = Block.box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape NORTH_THIN = Block.box(6, 6, 0, 10, 10, 6);
    private static final VoxelShape SOUTH_THIN = Block.box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape EAST_THIN = Block.box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape WEST_THIN = Block.box(0, 6, 6, 6, 10, 10);
    private static final VoxelShape UP_THIN = Block.box(6, 10, 6, 10, 16, 10);
    private static final VoxelShape DOWN_THIN = Block.box(6, 0, 6, 10, 6, 10);

    // 粗线缆碰撞箱 (6px: 5~11)
    private static final VoxelShape CORE_THICK = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape NORTH_THICK = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape SOUTH_THICK = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape EAST_THICK = Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape WEST_THICK = Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape UP_THICK = Block.box(5, 11, 5, 11, 16, 11);
    private static final VoxelShape DOWN_THICK = Block.box(5, 0, 5, 11, 5, 11);

    /** 按厚度预计算所有64种连接状态的VoxelShape缓存 */
    private static final Map<Thickness, VoxelShape[]> SHAPE_CACHE = new EnumMap<>(Thickness.class);

    static {
        // 细线缆：64种连接组合
        VoxelShape[] thinShapes = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = CORE_THIN;
            if ((mask & 1) != 0) shape = Shapes.or(shape, NORTH_THIN);
            if ((mask & 2) != 0) shape = Shapes.or(shape, SOUTH_THIN);
            if ((mask & 4) != 0) shape = Shapes.or(shape, EAST_THIN);
            if ((mask & 8) != 0) shape = Shapes.or(shape, WEST_THIN);
            if ((mask & 16) != 0) shape = Shapes.or(shape, UP_THIN);
            if ((mask & 32) != 0) shape = Shapes.or(shape, DOWN_THIN);
            thinShapes[mask] = shape;
        }
        SHAPE_CACHE.put(Thickness.THIN, thinShapes);

        // 粗线缆：64种连接组合
        VoxelShape[] thickShapes = new VoxelShape[64];
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = CORE_THICK;
            if ((mask & 1) != 0) shape = Shapes.or(shape, NORTH_THICK);
            if ((mask & 2) != 0) shape = Shapes.or(shape, SOUTH_THICK);
            if ((mask & 4) != 0) shape = Shapes.or(shape, EAST_THICK);
            if ((mask & 8) != 0) shape = Shapes.or(shape, WEST_THICK);
            if ((mask & 16) != 0) shape = Shapes.or(shape, UP_THICK);
            if ((mask & 32) != 0) shape = Shapes.or(shape, DOWN_THICK);
            thickShapes[mask] = shape;
        }
        SHAPE_CACHE.put(Thickness.THICK, thickShapes);
    }

    private enum Thickness { THIN, THICK }

    public CableBlock(Properties properties, EUVoltage voltage, float thickness, Supplier<BlockEntityType<CableBlockEntity>> blockEntityTypeSupplier) {
        super(properties);
        this.voltage = voltage;
        this.thickness = thickness;
        this.blockEntityTypeSupplier = blockEntityTypeSupplier;
        this.registerDefaultState(this.defaultBlockState()
            .setValue(NORTH, false).setValue(SOUTH, false)
            .setValue(EAST, false).setValue(WEST, false)
            .setValue(UP, false).setValue(DOWN, false));
    }

    public EUVoltage getVoltage() {
        return voltage;
    }

    public float getThickness() {
        return thickness;
    }

    public BlockEntityType<CableBlockEntity> getBlockEntityType() {
        return blockEntityTypeSupplier.get();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // 直接使用当前CableBlock实例的BlockEntityType，避免从state推断可能出错
        return new CableBlockEntity(blockEntityTypeSupplier.get(), pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return createTickerHelper(type, blockEntityTypeSupplier.get(),
                (lvl, pos, st, be) -> ((TickableBlockEntity) be).tick());
        }
        return null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Thickness t = thickness > 0.3F ? Thickness.THICK : Thickness.THIN;
        int mask = 0;
        if (state.getValue(NORTH)) mask |= 1;
        if (state.getValue(SOUTH)) mask |= 2;
        if (state.getValue(EAST))  mask |= 4;
        if (state.getValue(WEST))  mask |= 8;
        if (state.getValue(UP))    mask |= 16;
        if (state.getValue(DOWN))  mask |= 32;
        return SHAPE_CACHE.get(t)[mask];
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            updateConnections(level, pos);
            // 通知相邻线缆更新连接
            notifyNeighborCables(level, pos);
            // 合并共享能量存储
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CableBlockEntity cable) {
                cable.findOrCreateSharedStorage();
            }
            // 通知网络管理器：导线放置，可能需要合并网络
            network.NetworkManager.getInstance().onCablePlaced(level, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        // 通知网络管理器：导线移除，可能需要拆分网络
        network.NetworkManager.getInstance().onCableRemoved(level, pos);
        // 重新分配共享存储
        CableBlockEntity.redistributeStorageOnRemoval(level, pos);
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        // 通知相邻线缆更新连接
        notifyNeighborCables(level, pos);
    }

    private void notifyNeighborCables(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (neighborState.getBlock() instanceof CableBlock) {
                updateConnections(level, neighborPos);
            }
        }
    }

    private void updateConnections(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof CableBlockEntity cable) {
            cable.updateConnections();
        }
    }
}
