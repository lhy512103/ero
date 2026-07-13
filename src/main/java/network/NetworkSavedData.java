package network;

import energy.EUVoltage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.*;

/**
 * 网络持久化数据
 * <p>
 * 使用NeoForge SavedData机制，仅在存档自动保存时写入，
 * 避免每次注册/注销都触发磁盘IO。
 * </p>
 */
public class NetworkSavedData extends SavedData {

    private static final String DATA_NAME = "ero_energy_networks";

    private Map<String, List<NodeData>> networks = new HashMap<>();
    private Map<Long, String> machineToNetworkRaw = new HashMap<>();

    public NetworkSavedData() {}

    /**
     * 节点序列化数据
     */
    public static class NodeData {
        public final long pos;
        public final String machineName;
        public final String voltage;
        public final boolean isProducer;
        public final boolean isConsumer;
        public final boolean isStorage;

        public NodeData(long pos, String machineName, String voltage,
                 boolean isProducer, boolean isConsumer, boolean isStorage) {
            this.pos = pos;
            this.machineName = machineName;
            this.voltage = voltage;
            this.isProducer = isProducer;
            this.isConsumer = isConsumer;
            this.isStorage = isStorage;
        }

        public NodeData(MachineNetworkNode node) {
            this.pos = node.getPos().asLong();
            this.machineName = node.getMachineName();
            this.voltage = node.getVoltage().name();
            this.isProducer = node.isProducer();
            this.isConsumer = node.isConsumer();
            this.isStorage = node.isStorage();
        }

        MachineNetworkNode toNode() {
            EUVoltage v = EUVoltage.LV;
            try {
                v = EUVoltage.valueOf(voltage);
            } catch (IllegalArgumentException ignored) {}
            return new MachineNetworkNode(BlockPos.of(pos), machineName, v, isProducer, isConsumer, isStorage);
        }
    }

    // ==================== Codec ====================

    private static final Codec<NodeData> NODE_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.LONG.fieldOf("pos").forGetter(n -> n.pos),
            Codec.STRING.fieldOf("machineName").forGetter(n -> n.machineName),
            Codec.STRING.fieldOf("voltage").forGetter(n -> n.voltage),
            Codec.BOOL.fieldOf("isProducer").forGetter(n -> n.isProducer),
            Codec.BOOL.fieldOf("isConsumer").forGetter(n -> n.isConsumer),
            Codec.BOOL.fieldOf("isStorage").forGetter(n -> n.isStorage)
        ).apply(instance, NodeData::new)
    );

    public static final SavedDataType<NetworkSavedData> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("ero", DATA_NAME),
        NetworkSavedData::new,
        codec(),
        null
    );

    private static Codec<NetworkSavedData> codec() {
        return RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.unboundedMap(Codec.STRING, NODE_CODEC.listOf()).fieldOf("networks").forGetter(sd -> sd.networks),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("machineMapping")
                    .forGetter(sd -> {
                        // NBT的Map key必须是String，将Long转为String
                        Map<String, String> result = new HashMap<>();
                        for (Map.Entry<Long, String> entry : sd.machineToNetworkRaw.entrySet()) {
                            result.put(Long.toString(entry.getKey()), entry.getValue());
                        }
                        return result;
                    })
            ).apply(instance, (networks, machineMapping) -> {
                // 反序列化时将String key转回Long
                Map<Long, String> raw = new HashMap<>();
                for (Map.Entry<String, String> entry : machineMapping.entrySet()) {
                    try {
                        raw.put(Long.parseLong(entry.getKey()), entry.getValue());
                    } catch (NumberFormatException ignored) {}
                }
                return new NetworkSavedData(networks, raw);
            })
        );
    }

    private NetworkSavedData(Map<String, List<NodeData>> networks, Map<Long, String> machineMapping) {
        this.networks = networks;
        this.machineToNetworkRaw = machineMapping;
    }

    // ==================== 数据操作 ====================

    /**
     * 从NetworkManager收集数据并标记脏
     */
    public static NetworkSavedData collectAndMarkDirty(NetworkManager manager, ServerLevel level) {
        NetworkSavedData data = getOrCreate(level);
        data.networks = new HashMap<>();
        data.machineToNetworkRaw = new HashMap<>();

        for (Map.Entry<String, Map<String, EnergyNetwork>> dimEntry : manager.getDimensionNetworks().entrySet()) {
            for (Map.Entry<String, EnergyNetwork> netEntry : dimEntry.getValue().entrySet()) {
                String networkKey = dimEntry.getKey() + ":" + netEntry.getKey();
                List<NodeData> nodes = new ArrayList<>();
                for (MachineNetworkNode node : netEntry.getValue().getNodes()) {
                    nodes.add(new NodeData(node));
                }
                data.networks.put(networkKey, nodes);
            }
        }

        for (Map.Entry<BlockPos, String> entry : manager.getMachineToNetworkMap().entrySet()) {
            data.machineToNetworkRaw.put(entry.getKey().asLong(), entry.getValue());
        }

        data.setDirty();
        return data;
    }

    /**
     * 恢复数据到NetworkManager
     */
    public void restoreTo(NetworkManager manager) {
        for (Map.Entry<String, List<NodeData>> entry : networks.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            if (parts.length != 2) continue;

            String dimKey = parts[0];
            String networkUuid = parts[1];

            EnergyNetwork network = new EnergyNetwork(networkUuid);
            for (NodeData nodeData : entry.getValue()) {
                network.addNode(nodeData.toNode());
            }
            manager.addNetwork(dimKey, network);
        }

        for (Map.Entry<Long, String> entry : machineToNetworkRaw.entrySet()) {
            manager.addMachineToNetworkMapping(BlockPos.of(entry.getKey()), entry.getValue());
        }
    }

    /**
     * 获取或创建SavedData实例
     */
    public static NetworkSavedData getOrCreate(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
