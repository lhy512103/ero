package blockentity.cable;

import energy.EUCapability;
import energy.EUEnergyStorage;
import energy.EUEnergyStorageImpl;
import energy.EUVoltage;
import block.cable.CableBlock;
import blockentity.TickableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 线缆方块实体
 * <p>
 * 负责传输EU能量。连接的导线共享同一个能量缓存（网络级缓存），
 * 每个tick执行三阶段传输：
 * 1. 从连接的产电方（发电机、储能）拉取能量到共享缓冲
 * 2. 优先将缓冲能量推送到非线缆的耗电设备（机器）
 * 3. 将剩余能量推送到相邻线缆（继续传递）
 * </p>
 */
public class CableBlockEntity extends BlockEntity implements TickableBlockEntity {

    /** 静态缓存的 Direction 数组，避免 Direction.values() 每次分配新数组 */
    private static final Direction[] DIRECTIONS = Direction.values();

    private final EUVoltage voltage;
    private final float thickness;

    /** 缓存的连接状态，避免每次能力查询都读 BlockState */
    private boolean connectedNorth, connectedSouth, connectedEast, connectedWest, connectedUp, connectedDown;

    /** 缓存的EUCapability实例，按方向缓存6个 */
    private final EUCapability[] cachedCapabilities = new EUCapability[6];

    /**
     * 共享能量存储引用。
     * 连接的导线共享同一个 EUEnergyStorageImpl 实例，
     * 实现网络级统一缓存。
     */
    private EUEnergyStorageImpl sharedEnergyStorage;

    /** 本导线是否为共享缓存的持有者（负责序列化保存） */
    private boolean isStorageOwner = false;

    /** 世界加载后是否需要重新建立共享缓存 */
    private boolean needsStorageSync = false;

    /** 缓存的网络电缆列表（存储持有者使用），变更时通过 invalidateNetworkCache() 清空 */
    private List<CableBlockEntity> cachedNetworkCables = null;

    /** 缓存的相邻机器 EUCapability（按方向，仅缓存非线缆方块的能力），连接变化时清空 */
    private final EUCapability[] cachedMachineCaps = new EUCapability[6];

    /** 上次构建缓存时的 tick 计数，用于检测缓存失效 */
    private long lastCacheTick = -1;

    public CableBlockEntity(BlockPos pos, BlockState state) {
        this(getTypeForBlock(state), pos, state);
    }

    private static BlockEntityType<CableBlockEntity> getTypeForBlock(BlockState state) {
        if (state.getBlock() instanceof CableBlock cable) {
            return cable.getBlockEntityType();
        }
        return blockentity.ModBlockEntities.COPPER_CABLE.get();
    }

    public CableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        if (state.getBlock() instanceof CableBlock cable) {
            this.voltage = cable.getVoltage();
            this.thickness = cable.getThickness();
        } else {
            this.voltage = EUVoltage.LV;
            this.thickness = 0.25F;
        }
        // 初始创建独立的存储，放置后会通过 findOrCreateSharedStorage 合并
        this.sharedEnergyStorage = createNewStorage();
        this.isStorageOwner = true;
    }

    /**
     * 创建新的共享能量存储
     * 缓冲容量 = 电压等级传输量 * 4，足够容纳短距离导线网络的能量
     */
    private EUEnergyStorageImpl createNewStorage() {
        long capacity = voltage.getMaxVoltage() * 4;
        return new EUEnergyStorageImpl(capacity, voltage.getMaxVoltage(), voltage.getMaxVoltage(), voltage);
    }

    public EUVoltage getVoltage() {
        return voltage;
    }

    public float getThickness() {
        return thickness;
    }

    public EUEnergyStorage getEnergyStorage() {
        return sharedEnergyStorage;
    }

    /**
     * 查找或创建共享能量存储
     * <p>
     * 放置导线时调用，查找相邻导线的共享存储并加入，
     * 如果没有相邻导线则创建新的共享存储。
     * </p>
     */
    public void findOrCreateSharedStorage() {
        if (level == null || level.isClientSide()) return;

        // 查找相邻导线的共享存储
        EUEnergyStorageImpl existingStorage = null;
        CableBlockEntity existingOwner = null;
        for (Direction dir : DIRECTIONS) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof CableBlockEntity neighborCable) {
                if (neighborCable.voltage == this.voltage && neighborCable.sharedEnergyStorage != null) {
                    existingStorage = neighborCable.sharedEnergyStorage;
                    existingOwner = neighborCable.isStorageOwner ? neighborCable : null;
                    break;
                }
            }
        }

        if (existingStorage != null && existingStorage != this.sharedEnergyStorage) {
            // 将当前导线的能量合并到共享存储，然后加入共享
            long myEnergy = sharedEnergyStorage.getEnergyStored();
            if (myEnergy > 0) {
                existingStorage.addEnergy(myEnergy);
            }
            sharedEnergyStorage = existingStorage;
            isStorageOwner = false;

            // 通知所有连接的同电压导线使用同一个共享存储
            propagateSharedStorage(sharedEnergyStorage, new HashSet<>());

            // 使所有相关线缆的网络缓存失效
            for (CableBlockEntity cable : getNetworkCables()) {
                cable.invalidateNetworkCache();
            }
        }
    }

    /**
     * 递归传播共享存储到所有连接的同电压导线
     */
    private void propagateSharedStorage(EUEnergyStorageImpl storage, Set<BlockPos> visited) {
        if (level == null) return;
        visited.add(worldPosition);

        for (Direction dir : DIRECTIONS) {
            BlockPos neighborPos = worldPosition.relative(dir);
            if (visited.contains(neighborPos)) continue;

            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof CableBlockEntity neighborCable) {
                if (neighborCable.voltage == this.voltage && neighborCable.sharedEnergyStorage != storage) {
                    // 合并能量（合并后清空旧存储，防止多个邻居共享同一旧存储时重复添加）
                    EUEnergyStorageImpl oldStorage = neighborCable.sharedEnergyStorage;
                    long neighborEnergy = oldStorage.getEnergyStored();
                    if (neighborEnergy > 0) {
                        storage.addEnergy(neighborEnergy);
                        oldStorage.clearEnergy();
                    }
                    neighborCable.sharedEnergyStorage = storage;
                    neighborCable.isStorageOwner = false;
                    neighborCable.invalidateNetworkCache();
                    neighborCable.invalidateMachineCapCache();
                    neighborCable.propagateSharedStorage(storage, visited);
                }
            }
        }
    }

    /**
     * 导线移除时重新分配共享存储
     * <p>
     * 当导线被移除时，需要检查剩余的导线是否仍然连通。
     * 如果不连通，需要为每个分离的部分创建新的共享存储。
     * </p>
     */
    public static void redistributeStorageOnRemoval(Level level, BlockPos removedPos) {
        if (level == null || level.isClientSide()) return;

        // 查找相邻的导线
        List<CableBlockEntity> neighborCables = new ArrayList<>();
        for (Direction dir : DIRECTIONS) {
            BlockPos neighborPos = removedPos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof CableBlockEntity cable) {
                neighborCables.add(cable);
            }
        }

        if (neighborCables.isEmpty()) return;

        // 获取被移除导线的共享存储（移除前应该还有引用）
        // 对每个相邻导线，BFS查找连通的导线组
        Set<BlockPos> visited = new HashSet<>();
        visited.add(removedPos);

        for (CableBlockEntity startCable : neighborCables) {
            if (visited.contains(startCable.worldPosition)) continue;

            // BFS查找连通的导线组
            List<CableBlockEntity> group = new ArrayList<>();
            Queue<CableBlockEntity> queue = new LinkedList<>();
            queue.add(startCable);
            visited.add(startCable.worldPosition);

            while (!queue.isEmpty()) {
                CableBlockEntity current = queue.poll();
                group.add(current);

                for (Direction dir : DIRECTIONS) {
                    BlockPos neighborPos = current.worldPosition.relative(dir);
                    if (visited.contains(neighborPos)) continue;
                    BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                    if (neighborBe instanceof CableBlockEntity neighborCable
                        && neighborCable.voltage == startCable.voltage) {
                        visited.add(neighborPos);
                        queue.add(neighborCable);
                    }
                }
            }

            // 为这个组创建新的共享存储
            if (!group.isEmpty()) {
                EUVoltage groupVoltage = group.get(0).voltage;
                long capacity = groupVoltage.getMaxVoltage() * 4;
                EUEnergyStorageImpl newStorage = new EUEnergyStorageImpl(
                    capacity, groupVoltage.getMaxVoltage(), groupVoltage.getMaxVoltage(), groupVoltage);

                // 从旧共享存储迁移能量（只取一次，避免重复）
                EUEnergyStorageImpl oldStorage = group.get(0).sharedEnergyStorage;
                long oldEnergy = oldStorage.getEnergyStored();
                if (oldEnergy > 0) {
                    newStorage.addEnergy(oldEnergy);
                    oldStorage.clearEnergy();
                }

                // 分配新存储
                for (CableBlockEntity cable : group) {
                    cable.sharedEnergyStorage = newStorage;
                    cable.isStorageOwner = (cable == group.get(0));
                    cable.invalidateNetworkCache();
                    cable.invalidateMachineCapCache();
                }
            }
        }
    }

    /**
     * 获取网络级能量信息（用于Jade显示）
     * <p>
     * 统计所有共享同一存储的导线数量，返回统一的能量信息。
     * </p>
     */
    public NetworkEnergyInfo getNetworkEnergyInfo() {
        if (level == null) {
            return new NetworkEnergyInfo(sharedEnergyStorage.getEnergyStored(),
                sharedEnergyStorage.getMaxEnergyStored(), 1);
        }

        // 通过BFS统计共享同一存储的导线数量
        int cableCount = 0;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            cableCount++;

            for (Direction dir : DIRECTIONS) {
                BlockPos neighborPos = currentPos.relative(dir);
                if (visited.contains(neighborPos)) continue;
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (neighborBe instanceof CableBlockEntity neighborCable
                    && neighborCable.sharedEnergyStorage == this.sharedEnergyStorage) {
                    visited.add(neighborPos);
                    queue.add(neighborPos);
                }
            }
        }

        return new NetworkEnergyInfo(sharedEnergyStorage.getEnergyStored(),
            sharedEnergyStorage.getMaxEnergyStored(), cableCount);
    }

    /**
     * 网络级能量信息记录
     */
    public record NetworkEnergyInfo(long energy, long capacity, int cableCount) {}

    @Override
    public void tick() {
        if (level == null || level.isClientSide()) return;

        // 世界加载后首次tick，重新建立共享缓存
        if (needsStorageSync) {
            needsStorageSync = false;
            findOrCreateSharedStorage();
        }

        // 只有存储持有者执行传输逻辑，避免重复传输
        if (!isStorageOwner) return;

        long maxTransfer = voltage.getMaxVoltage();

        // 本地缓存网络导线列表，避免3次方法调用
        List<CableBlockEntity> networkCables = getNetworkCables();

        // ===== 阶段1：从连接的产电方拉取能量 =====
        pullFromProducers(maxTransfer, networkCables);

        // ===== 阶段2：优先推送到非线缆的耗电设备（机器、储能） =====
        pushToMachines(maxTransfer, networkCables);

        // ===== 阶段3：将剩余能量推送到相邻线缆（不同共享存储的） =====
        pushToOtherNetworks(maxTransfer, networkCables);
    }

    /** 上次构建机器能力缓存时的 tick 计数 */
    private long lastMachineCapTick = -1;

    /**
     * 缓存获取指定方向的相邻机器 EUCapability。
     * <p>
     * 只在缓存失效时查询一次 level.getCapability()，
     * 后续同一 tick 内直接从缓存返回，避免重复的 capability 查询。
     * 跨 tick 自动失效，防止方块实体被移除后引用残留。
     * </p>
     */
    @Nullable
    private EUCapability getCachedMachineCap(Direction dir) {
        // 跨 tick 自动清空缓存
        if (level != null && lastMachineCapTick != level.getGameTime()) {
            java.util.Arrays.fill(cachedMachineCaps, null);
            lastMachineCapTick = level.getGameTime();
        }

        int idx = dir.get3DDataValue();
        EUCapability cached = cachedMachineCaps[idx];
        if (cached != null) return cached;

        if (!getCachedConnection(dir)) return null;
        BlockPos neighborPos = worldPosition.relative(dir);
        // 跳过线缆（线缆通过 pushToOtherNetworks 处理）
        if (level.getBlockState(neighborPos).getBlock() instanceof CableBlock) return null;

        cached = level.getCapability(EUCapability.BLOCK, neighborPos, dir.getOpposite());
        cachedMachineCaps[idx] = cached;
        return cached;
    }

    /**
     * 阶段1：从连接的产电方（发电机、储能箱）拉取能量到共享缓冲
     */
    private void pullFromProducers(long maxTransfer, List<CableBlockEntity> networkCables) {
        long bufferFree = sharedEnergyStorage.getMaxEnergyStored() - sharedEnergyStorage.getEnergyStored();
        if (bufferFree <= 0) return;

        for (CableBlockEntity cable : networkCables) {
            if (bufferFree <= 0) break;

            for (Direction dir : DIRECTIONS) {
                if (!cable.getCachedConnection(dir)) continue;
                if (bufferFree <= 0) break;

                EUCapability neighborCap = cable.getCachedMachineCap(dir);
                if (neighborCap == null || !neighborCap.canExtract()) continue;
                if (neighborCap.getEnergyStored() <= 0) continue;

                long toPull = Math.min(Math.min(maxTransfer, bufferFree), neighborCap.getMaxTransferRate());
                if (toPull <= 0) continue;

                long extracted = neighborCap.extractEnergy(toPull, false);
                if (extracted > 0) {
                    sharedEnergyStorage.addEnergy(extracted);
                    bufferFree -= extracted;
                }
            }
        }
    }

    /**
     * 阶段2：优先将缓冲能量推送到非线缆的耗电设备
     * <p>
     * 遍历网络中所有导线的相邻非导线方块，推送能量。
     * </p>
     */
    private void pushToMachines(long maxTransfer, List<CableBlockEntity> networkCables) {
        long buffer = sharedEnergyStorage.getEnergyStored();
        if (buffer <= 0) return;

        for (CableBlockEntity cable : networkCables) {
            buffer = sharedEnergyStorage.getEnergyStored();
            if (buffer <= 0) break;

            for (Direction dir : DIRECTIONS) {
                if (!cable.getCachedConnection(dir)) continue;
                buffer = sharedEnergyStorage.getEnergyStored();
                if (buffer <= 0) break;

                EUCapability neighborCap = cable.getCachedMachineCap(dir);
                if (neighborCap == null || !neighborCap.canReceive()) continue;

                long neighborFree = neighborCap.getMaxEnergyStored() - neighborCap.getEnergyStored();
                if (neighborFree <= 0) continue;

                long toPush = Math.min(Math.min(Math.min(buffer, maxTransfer), neighborCap.getMaxTransferRate()), neighborFree);
                if (toPush <= 0) continue;

                long received = neighborCap.receiveEnergy(toPush, false);
                if (received > 0) {
                    sharedEnergyStorage.removeEnergy(received);
                }
            }
        }
    }

    /**
     * 阶段3：将剩余能量推送到相邻的不同网络线缆
     * <p>
     * 只推送到不属于同一共享存储的线缆。
     * </p>
     */
    private void pushToOtherNetworks(long maxTransfer, List<CableBlockEntity> networkCables) {
        long buffer = sharedEnergyStorage.getEnergyStored();
        if (buffer <= 0) return;

        for (CableBlockEntity cable : networkCables) {
            buffer = sharedEnergyStorage.getEnergyStored();
            if (buffer <= 0) break;

            for (Direction dir : DIRECTIONS) {
                if (!cable.getCachedConnection(dir)) continue;
                buffer = sharedEnergyStorage.getEnergyStored();
                if (buffer <= 0) break;

                BlockPos neighborPos = cable.worldPosition.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (!(neighborBe instanceof CableBlockEntity neighborCable)) continue;
                if (neighborCable.sharedEnergyStorage == this.sharedEnergyStorage) continue;

                EUCapability neighborCap = level.getCapability(EUCapability.BLOCK, neighborPos, dir.getOpposite());
                if (neighborCap == null || !neighborCap.canReceive()) continue;

                long neighborFree = neighborCap.getMaxEnergyStored() - neighborCap.getEnergyStored();
                if (neighborFree <= 0) continue;

                long toPush = Math.min(Math.min(Math.min(buffer, maxTransfer), neighborCap.getMaxTransferRate()), neighborFree);
                if (toPush <= 0) continue;

                long received = neighborCap.receiveEnergy(toPush, false);
                if (received > 0) {
                    sharedEnergyStorage.removeEnergy(received);
                }
            }
        }
    }

    /**
     * 使网络电缆缓存失效（网络结构变化时调用）
     */
    public void invalidateNetworkCache() {
        cachedNetworkCables = null;
        lastCacheTick = -1;
    }

    /**
     * 使机器能力缓存失效（连接变化时调用）
     */
    private void invalidateMachineCapCache() {
        java.util.Arrays.fill(cachedMachineCaps, null);
    }

    /**
     * 获取网络中所有导线（共享同一存储的导线）
     * <p>
     * 使用缓存策略：首次调用或缓存失效时通过 BFS 构建，后续直接返回缓存。
     * 网络结构变化（导线放置/移除）时通过 invalidateNetworkCache() 清空缓存。
     * </p>
     */
    private List<CableBlockEntity> getNetworkCables() {
        if (level == null) return List.of();

        // 缓存有效则直接返回
        if (cachedNetworkCables != null && lastCacheTick == level.getGameTime()) {
            return cachedNetworkCables;
        }

        // 构建缓存（BFS 一次，后续复用）
        List<CableBlockEntity> cables = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(worldPosition);
        visited.add(worldPosition);

        while (!queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            BlockEntity be = level.getBlockEntity(currentPos);
            if (be instanceof CableBlockEntity cable && cable.sharedEnergyStorage == this.sharedEnergyStorage) {
                cables.add(cable);
                for (Direction dir : DIRECTIONS) {
                    BlockPos neighborPos = currentPos.relative(dir);
                    if (!visited.contains(neighborPos)) {
                        visited.add(neighborPos);
                        BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                        if (neighborBe instanceof CableBlockEntity) {
                            queue.add(neighborPos);
                        }
                    }
                }
            }
        }

        cachedNetworkCables = cables;
        lastCacheTick = level.getGameTime();
        return cables;
    }

    /**
     * 更新线缆连接状态
     */
    public void updateConnections() {
        if (level == null || level.isClientSide()) return;

        BlockState currentState = getBlockState();
        BlockState newState = currentState;

        for (Direction dir : DIRECTIONS) {
            boolean shouldConnect = shouldConnectTo(dir);
            newState = newState.setValue(
                dir == Direction.NORTH ? CableBlock.NORTH :
                dir == Direction.SOUTH ? CableBlock.SOUTH :
                dir == Direction.EAST  ? CableBlock.EAST :
                dir == Direction.WEST  ? CableBlock.WEST :
                dir == Direction.UP    ? CableBlock.UP :
                                         CableBlock.DOWN,
                shouldConnect
            );
        }

        // 更新缓存连接状态
        connectedNorth = newState.getValue(CableBlock.NORTH);
        connectedSouth = newState.getValue(CableBlock.SOUTH);
        connectedEast  = newState.getValue(CableBlock.EAST);
        connectedWest  = newState.getValue(CableBlock.WEST);
        connectedUp    = newState.getValue(CableBlock.UP);
        connectedDown  = newState.getValue(CableBlock.DOWN);

        // 连接状态变化时清除能力缓存
        if (newState != currentState) {
            java.util.Arrays.fill(cachedCapabilities, null);
            invalidateMachineCapCache();
            invalidateNetworkCache();
            level.setBlockAndUpdate(worldPosition, newState);
        }
    }

    private boolean getCachedConnection(Direction dir) {
        return switch (dir) {
            case NORTH -> connectedNorth;
            case SOUTH -> connectedSouth;
            case EAST  -> connectedEast;
            case WEST  -> connectedWest;
            case UP    -> connectedUp;
            case DOWN  -> connectedDown;
        };
    }

    private boolean shouldConnectTo(Direction dir) {
        if (level == null) return false;

        BlockPos neighborPos = worldPosition.relative(dir);

        BlockState neighborState = level.getBlockState(neighborPos);
        if (neighborState.getBlock() instanceof CableBlock) {
            return true;
        }

        EUCapability cap = level.getCapability(EUCapability.BLOCK, neighborPos, dir.getOpposite());
        if (cap != null) {
            return true;
        }
        return false;
    }

    @Nullable
    public EUCapability getEUCapability(@Nullable Direction direction) {
        if (direction == null) return null;

        int index = direction.get3DDataValue();
        if (cachedCapabilities[index] != null) return cachedCapabilities[index];

        boolean connected = getCachedConnection(direction);
        EUCapability cap = new energy.EUEnergyCapabilityAdapter(sharedEnergyStorage, voltage) {
            @Override
            public boolean canReceive() {
                return connected;
            }

            @Override
            public boolean canExtract() {
                return connected;
            }
        };
        cachedCapabilities[index] = cap;
        return cap;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // 只有存储持有者保存能量数据
        if (isStorageOwner) {
            output.putLong("Energy", sharedEnergyStorage.writeToNBT());
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        long energy = input.getLongOr("Energy", 0);
        sharedEnergyStorage.readFromNBT(energy);
        isStorageOwner = true;
        needsStorageSync = true; // 世界加载后需要重新建立共享缓存
        // 从BlockState恢复缓存连接状态
        BlockState state = getBlockState();
        connectedNorth = state.getValue(CableBlock.NORTH);
        connectedSouth = state.getValue(CableBlock.SOUTH);
        connectedEast  = state.getValue(CableBlock.EAST);
        connectedWest  = state.getValue(CableBlock.WEST);
        connectedUp    = state.getValue(CableBlock.UP);
        connectedDown  = state.getValue(CableBlock.DOWN);
        java.util.Arrays.fill(cachedCapabilities, null);
    }
}
