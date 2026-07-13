package network;

import blockentity.MachineBlockEntity;
import blockentity.cable.CableBlockEntity;
import energy.EUCapability;
import energy.EUVoltage;
import network.performance.NetworkLoadBalancer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网络管理器
 * <p>
 * 全局管理所有能量网络，处理机器注册/注销、自动连接相邻机器、
 * 每tick执行能量传输。使用SavedData机制持久化网络状态。
 * </p>
 */
public class NetworkManager {

    private static final NetworkManager INSTANCE = new NetworkManager();

    /** 按维度分开管理网络 */
    private final Map<String, Map<String, EnergyNetwork>> dimensionNetworks = new ConcurrentHashMap<>();

    /** 机器位置(asLong) -> 所属网络UUID 的映射 */
    private final Long2ObjectOpenHashMap<String> machineToNetwork = new Long2ObjectOpenHashMap<>();

    /** 机器位置(asLong) -> MachineNetworkNode 的映射 */
    private final Long2ObjectOpenHashMap<MachineNetworkNode> machineNodes = new Long2ObjectOpenHashMap<>();

    /** 已初始化的维度集合 */
    private final Set<String> initializedDimensions = ConcurrentHashMap.newKeySet();

    private NetworkManager() {}

    public static NetworkManager getInstance() { return INSTANCE; }

    // ==================== 序列化支持 ====================

    /**
     * 获取维度网络数据（用于序列化）
     */
    public Map<String, Map<String, EnergyNetwork>> getDimensionNetworks() {
        return dimensionNetworks;
    }

    /**
     * 获取机器到网络的映射（用于序列化）
     */
    public Map<BlockPos, String> getMachineToNetworkMap() {
        // 兼容序列化接口，转换为 BlockPos key 的 Map
        Map<BlockPos, String> result = new HashMap<>();
        for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<String> entry : machineToNetwork.long2ObjectEntrySet()) {
            result.put(BlockPos.of(entry.getLongKey()), entry.getValue());
        }
        return result;
    }

    /**
     * 添加网络（用于反序列化）
     */
    public void addNetwork(String dimKey, EnergyNetwork network) {
        dimensionNetworks.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>())
            .put(network.getNetworkUuid(), network);

        // 更新机器节点映射
        for (MachineNetworkNode node : network.getNodes()) {
            machineNodes.put(node.getPos().asLong(), node);
        }
    }

    /**
     * 添加机器到网络的映射（用于反序列化）
     */
    public void addMachineToNetworkMapping(BlockPos pos, String networkKey) {
        machineToNetwork.put(pos.asLong(), networkKey);
    }

    // ==================== 导线连接管理 ====================

    /**
     * 导线放置时调用，检查是否需要合并相邻机器的网络
     * <p>
     * 当导线放置后，通过BFS查找通过导线连接的所有机器，
     * 如果这些机器属于不同的网络，则将它们合并为一个网络。
     * </p>
     */
    public void onCablePlaced(Level level, BlockPos cablePos) {
        if (level == null || level.isClientSide()) return;

        String dimKey = getDimensionKey(level);
        Map<String, EnergyNetwork> networks = dimensionNetworks.get(dimKey);
        if (networks == null) return;

        // 通过导线BFS查找所有可达的机器及其所属网络
        Set<String> adjacentNetworks = new HashSet<>();
        LongOpenHashSet visited = new LongOpenHashSet();
        visited.add(cablePos.asLong());

        Queue<BlockPos> cableQueue = new LinkedList<>();
        cableQueue.add(cablePos);

        while (!cableQueue.isEmpty()) {
            BlockPos currentPos = cableQueue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = currentPos.relative(dir);
                long neighborLong = neighborPos.asLong();
                if (visited.contains(neighborLong)) continue;
                visited.add(neighborLong);

                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (neighborBe instanceof MachineBlockEntity) {
                    String networkKey = machineToNetwork.get(neighborLong);
                    if (networkKey != null) {
                        adjacentNetworks.add(networkKey);
                    }
                } else if (neighborBe instanceof CableBlockEntity) {
                    cableQueue.add(neighborPos);
                }
            }
        }

        // 如果找到多个网络，合并它们
        if (adjacentNetworks.size() <= 1) return;

        Iterator<String> it = adjacentNetworks.iterator();
        String targetKey = it.next();
        EnergyNetwork targetNetwork = networks.get(targetKey);
        if (targetNetwork == null) {
            targetNetwork = new EnergyNetwork();
            networks.put(targetNetwork.getNetworkUuid(), targetNetwork);
            targetKey = targetNetwork.getNetworkUuid();
        }

        while (it.hasNext()) {
            String otherKey = it.next();
            if (otherKey.equals(targetKey)) continue;
            EnergyNetwork otherNetwork = networks.remove(otherKey);
            if (otherNetwork != null) {
                targetNetwork.merge(otherNetwork);
                for (MachineNetworkNode n : otherNetwork.getNodes()) {
                    machineToNetwork.put(n.getPos().asLong(), targetKey);
                }
                NetworkLoadBalancer.getInstance().unregisterNetwork(otherKey);
            }
        }

        markDirty(level);
    }

    /**
     * 导线移除时调用，检查是否需要拆分网络
     */
    public void onCableRemoved(Level level, BlockPos cablePos) {
        if (level == null || level.isClientSide()) return;

        String dimKey = getDimensionKey(level);
        Map<String, EnergyNetwork> networks = dimensionNetworks.get(dimKey);
        if (networks == null) return;

        // 查找通过此导线连接的机器所属网络
        Set<String> affectedNetworks = new HashSet<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = cablePos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof MachineBlockEntity) {
                String networkKey = machineToNetwork.get(neighborPos.asLong());
                if (networkKey != null) {
                    affectedNetworks.add(networkKey);
                }
            } else if (neighborBe instanceof CableBlockEntity) {
                // 通过相邻导线查找可达的机器网络
                findNetworksThroughCable(level, neighborPos, cablePos, affectedNetworks, new LongOpenHashSet());
            }
        }

        // 对受影响的网络检查是否需要拆分
        for (String networkKey : affectedNetworks) {
            EnergyNetwork network = networks.get(networkKey);
            if (network != null) {
                splitNetworkIfNeeded(network, level, networks);
            }
        }
    }

    // ==================== 网络管理 ====================

    /**
     * 重建指定世界的所有能量网络
     */
    public void rebuildNetworks(Level level) {
        if (level.isClientSide()) return;

        String dimKey = getDimensionKey(level);

        // 从SavedData加载持久化数据
        if (level instanceof ServerLevel serverLevel) {
            NetworkSavedData savedData = NetworkSavedData.getOrCreate(serverLevel);
            savedData.restoreTo(this);
        }

        initializedDimensions.add(dimKey);
    }

    /**
     * 标记网络数据为脏（需要保存），在存档自动保存时持久化
     */
    private void markDirty(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            NetworkSavedData.collectAndMarkDirty(this, serverLevel);
        }
    }

    /**
     * 保存指定世界的网络数据（仅在存档自动保存时调用）
     */
    public void saveNetworks(Level level) {
        if (level.isClientSide()) return;
        // SavedData 机制会自动在存档保存时调用 save()，这里只需标记脏
        markDirty(level);
    }

    /**
     * 检查指定维度是否已初始化
     */
    public boolean isDimensionInitialized(Level level) {
        return initializedDimensions.contains(getDimensionKey(level));
    }

    /**
     * 注册机器到网络系统
     */
    public void registerMachine(MachineBlockEntity machine) {
        if (machine == null || machine.getLevel() == null) return;

        Level level = machine.getLevel();
        BlockPos pos = machine.getBlockPos();
        long posLong = pos.asLong();

        // 创建节点
        MachineNetworkNode node = createNode(machine);
        if (node == null) return;

        machineNodes.put(posLong, node);

        // 扫描相邻机器，查找可连接的网络
        Set<String> adjacentNetworks = new HashSet<>();
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof MachineBlockEntity) {
                String networkKey = machineToNetwork.get(neighborPos.asLong());
                if (networkKey != null) {
                    adjacentNetworks.add(networkKey);
                }
            } else if (neighborBe instanceof CableBlockEntity) {
                // 通过线缆查找远程连接的机器网络
                findNetworksThroughCable(level, neighborPos, pos, adjacentNetworks, new LongOpenHashSet());
            }
        }

        String dimKey = getDimensionKey(level);
        Map<String, EnergyNetwork> networks = dimensionNetworks.computeIfAbsent(dimKey, k -> new ConcurrentHashMap<>());

        if (adjacentNetworks.isEmpty()) {
            // 没有相邻网络，创建新网络
            EnergyNetwork network = new EnergyNetwork();
            network.addNode(node);
            networks.put(network.getNetworkUuid(), network);
            machineToNetwork.put(posLong, network.getNetworkUuid());
            // 注册到负载均衡器
            NetworkLoadBalancer.getInstance().registerNetwork(network.getNetworkUuid());
        } else if (adjacentNetworks.size() == 1) {
            // 连接到一个现有网络
            String networkKey = adjacentNetworks.iterator().next();
            EnergyNetwork network = networks.get(networkKey);
            if (network != null) {
                network.addNode(node);
                machineToNetwork.put(posLong, networkKey);
            }
        } else {
            // 连接多个网络，合并它们
            Iterator<String> it = adjacentNetworks.iterator();
            String targetKey = it.next();
            EnergyNetwork targetNetwork = networks.get(targetKey);
            if (targetNetwork == null) {
                targetNetwork = new EnergyNetwork();
                networks.put(targetNetwork.getNetworkUuid(), targetNetwork);
                targetKey = targetNetwork.getNetworkUuid();
            }
            targetNetwork.addNode(node);
            machineToNetwork.put(posLong, targetKey);

            // 合并其他网络
            while (it.hasNext()) {
                String otherKey = it.next();
                EnergyNetwork otherNetwork = networks.remove(otherKey);
                if (otherNetwork != null) {
                    targetNetwork.merge(otherNetwork);
                    for (MachineNetworkNode n : otherNetwork.getNodes()) {
                        machineToNetwork.put(n.getPos().asLong(), targetKey);
                    }
                }
            }
        }

        // 标记脏，而非立即保存
        markDirty(level);
    }

    /**
     * 从网络系统注销机器
     */
    public void unregisterMachine(MachineBlockEntity machine) {
        if (machine == null) return;

        BlockPos pos = machine.getBlockPos();
        long posLong = pos.asLong();
        machineNodes.remove(posLong);
        String networkKey = machineToNetwork.remove(posLong);
        if (networkKey == null) return;

        Level level = machine.getLevel();
        if (level == null) return;

        String dimKey = getDimensionKey(level);
        Map<String, EnergyNetwork> networks = dimensionNetworks.get(dimKey);
        if (networks == null) return;

        EnergyNetwork network = networks.get(networkKey);
        if (network == null) return;

        network.removeNode(pos);

        if (network.isEmpty()) {
            networks.remove(networkKey);
            // 从负载均衡器注销
            NetworkLoadBalancer.getInstance().unregisterNetwork(networkKey);
        } else {
            splitNetworkIfNeeded(network, level, networks);
        }

        // 标记脏，而非立即保存
        markDirty(level);
    }

    /**
     * 每tick执行，更新所有网络的能量传输
     */
    public void tick(Level level) {
        if (level.isClientSide()) return;

        String dimKey = getDimensionKey(level);

        // 如果维度未初始化，先重建网络
        if (!initializedDimensions.contains(dimKey)) {
            rebuildNetworks(level);
        }

        Map<String, EnergyNetwork> networks = dimensionNetworks.get(dimKey);
        if (networks == null) return;

        // 更新负载均衡器
        NetworkLoadBalancer loadBalancer = NetworkLoadBalancer.getInstance();
        loadBalancer.tick();

        for (EnergyNetwork network : networks.values()) {
            // 负载均衡：跳过当前批次之外的网络
            if (!loadBalancer.shouldTickNetwork(network.getNetworkUuid())) {
                continue;
            }

            boolean hadTransfer = network.tick(level);

            // 通知负载均衡器网络活跃/空闲状态
            if (hadTransfer) {
                loadBalancer.notifyNetworkActive(network.getNetworkUuid());
            } else {
                loadBalancer.notifyNetworkIdle(network.getNetworkUuid());
            }
        }
    }

    /**
     * 获取机器所属的网络
     */
    public EnergyNetwork getNetworkForMachine(BlockPos pos) {
        String networkKey = machineToNetwork.get(pos.asLong());
        if (networkKey == null) return null;

        for (Map<String, EnergyNetwork> networks : dimensionNetworks.values()) {
            EnergyNetwork network = networks.get(networkKey);
            if (network != null) return network;
        }
        return null;
    }

    /**
     * 获取指定维度的所有网络
     */
    public Collection<EnergyNetwork> getNetworks(Level level) {
        String dimKey = getDimensionKey(level);
        Map<String, EnergyNetwork> networks = dimensionNetworks.get(dimKey);
        return networks != null ? networks.values() : Collections.emptyList();
    }

    /**
     * 获取网络中的节点信息
     */
    public MachineNetworkNode getNode(BlockPos pos) {
        return machineNodes.get(pos.asLong());
    }

    /**
     * 创建网络节点
     */
    private MachineNetworkNode createNode(MachineBlockEntity machine) {
        EUCapability cap = machine.getEUCapability(null);
        boolean isProducer = false;
        boolean isConsumer = false;
        boolean isStorage = false;

        if (cap != null) {
            if (cap.canExtract() && !cap.canReceive()) isProducer = true;
            else if (cap.canReceive() && !cap.canExtract()) isConsumer = true;
            else if (cap.canReceive() && cap.canExtract()) isStorage = true;
        } else {
            isConsumer = true;
        }

        EUVoltage voltage = cap != null ? cap.getVoltage() : EUVoltage.LV;

        return new MachineNetworkNode(
            machine.getBlockPos(),
            machine.getMachineName(),
            voltage,
            isProducer,
            isConsumer,
            isStorage
        );
    }

    /**
     * 通过线缆迭代BFS查找远程连接的机器所属网络
     * <p>
     * 使用显式队列代替递归，避免超长线缆链导致StackOverflowError。
     * </p>
     */
    private void findNetworksThroughCable(Level level, BlockPos cableStart, BlockPos origin,
                                           Set<String> adjacentNetworks, LongOpenHashSet visited) {
        Queue<BlockPos> cableQueue = new LinkedList<>();
        cableQueue.add(cableStart);
        visited.add(cableStart.asLong());

        while (!cableQueue.isEmpty()) {
            BlockPos cablePos = cableQueue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = cablePos.relative(dir);
                if (neighborPos.equals(origin)) continue;
                long neighborLong = neighborPos.asLong();
                if (visited.contains(neighborLong)) continue;

                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (neighborBe instanceof MachineBlockEntity) {
                    String networkKey = machineToNetwork.get(neighborLong);
                    if (networkKey != null) {
                        adjacentNetworks.add(networkKey);
                    }
                } else if (neighborBe instanceof CableBlockEntity) {
                    visited.add(neighborLong);
                    cableQueue.add(neighborPos);
                }
            }
        }
    }

    /**
     * 检查并拆分不连通的网络
     * <p>
     * 支持线缆桥接：两个机器即使不相邻，只要通过线缆连通就属于同一网络。
     * </p>
     */
    private void splitNetworkIfNeeded(EnergyNetwork network, Level level, Map<String, EnergyNetwork> networks) {
        Collection<MachineNetworkNode> nodes = network.getNodes();
        if (nodes.size() <= 1) return;

        Long2ObjectOpenHashMap<MachineNetworkNode> posMap = new Long2ObjectOpenHashMap<>();
        for (MachineNetworkNode node : nodes) {
            posMap.put(node.getPos().asLong(), node);
        }

        LongOpenHashSet visited = new LongOpenHashSet();
        List<List<MachineNetworkNode>> components = new ArrayList<>();

        for (MachineNetworkNode start : nodes) {
            long startLong = start.getPos().asLong();
            if (visited.contains(startLong)) continue;

            List<MachineNetworkNode> component = new ArrayList<>();
            Queue<MachineNetworkNode> queue = new LinkedList<>();
            queue.add(start);
            visited.add(startLong);

            while (!queue.isEmpty()) {
                MachineNetworkNode current = queue.poll();
                component.add(current);

                // 直接相邻的机器
                for (Direction dir : Direction.values()) {
                    BlockPos neighborPos = current.getPos().relative(dir);
                    long neighborLong = neighborPos.asLong();
                    MachineNetworkNode neighbor = posMap.get(neighborLong);
                    if (neighbor != null && !visited.contains(neighborLong)) {
                        visited.add(neighborLong);
                        queue.add(neighbor);
                    }
                }

                // 通过线缆连接的机器
                findMachinesThroughCable(level, current.getPos(), posMap, visited, queue);
            }

            components.add(component);
        }

        if (components.size() <= 1) return;

        networks.remove(network.getNetworkUuid());
        // 从负载均衡器注销被拆分的网络
        NetworkLoadBalancer.getInstance().unregisterNetwork(network.getNetworkUuid());

        for (List<MachineNetworkNode> component : components) {
            EnergyNetwork newNetwork = new EnergyNetwork();
            for (MachineNetworkNode node : component) {
                newNetwork.addNode(node);
                machineToNetwork.put(node.getPos().asLong(), newNetwork.getNetworkUuid());
            }
            networks.put(newNetwork.getNetworkUuid(), newNetwork);
            // 注册新网络到负载均衡器
            NetworkLoadBalancer.getInstance().registerNetwork(newNetwork.getNetworkUuid());
        }
    }

    /**
     * BFS通过线缆查找可达的机器节点
     */
    private void findMachinesThroughCable(Level level, BlockPos machinePos,
                                           Long2ObjectOpenHashMap<MachineNetworkNode> posMap,
                                           LongOpenHashSet visited, Queue<MachineNetworkNode> queue) {
        LongOpenHashSet cableVisited = new LongOpenHashSet();
        Queue<BlockPos> cableQueue = new LinkedList<>();

        // 将机器相邻的线缆加入队列
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = machinePos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof CableBlockEntity) {
                long cableLong = neighborPos.asLong();
                if (cableVisited.add(cableLong)) {
                    cableQueue.add(neighborPos);
                }
            }
        }

        // 迭代BFS遍历线缆
        while (!cableQueue.isEmpty()) {
            BlockPos cablePos = cableQueue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = cablePos.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);

                if (neighborBe instanceof MachineBlockEntity) {
                    long neighborLong = neighborPos.asLong();
                    MachineNetworkNode neighbor = posMap.get(neighborLong);
                    if (neighbor != null && !visited.contains(neighborLong)) {
                        visited.add(neighborLong);
                        queue.add(neighbor);
                    }
                } else if (neighborBe instanceof CableBlockEntity) {
                    long cableLong = neighborPos.asLong();
                    if (cableVisited.add(cableLong)) {
                        cableQueue.add(neighborPos);
                    }
                }
            }
        }
    }

    private String getDimensionKey(Level level) {
        return level.dimension().toString();
    }
}
