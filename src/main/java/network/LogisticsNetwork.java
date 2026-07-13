package network;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 物流网络管理器
 * <p>
 * 管理所有机器之间的物流连接。
 * 支持双向绑定：目标机器（接收方）和传输机器（发送方）。
 * </p>
 */
public class LogisticsNetwork {

    private static final LogisticsNetwork INSTANCE = new LogisticsNetwork();

    /** 所有物流连接，以 "targetPos-transportPos" 为 key */
    private final Map<String, LogisticsLink> links = new ConcurrentHashMap<>();

    /** 机器位置(asLong) -> 参与的连接ID集合 */
    private final Long2ObjectOpenHashMap<Set<String>> machineLinks = new Long2ObjectOpenHashMap<>();

    private LogisticsNetwork() {}

    public static LogisticsNetwork getInstance() { return INSTANCE; }

    private static String linkKey(BlockPos targetPos, BlockPos transportPos) {
        return targetPos.asLong() + "-" + transportPos.asLong();
    }

    /**
     * 创建双向物流连接
     *
     * @param targetPos 目标机器位置（接收方）
     * @param transportPos 传输机器位置（发送方）
     * @return 创建的连接
     */
    public LogisticsLink createLink(BlockPos targetPos, BlockPos transportPos) {
        LogisticsLink link = new LogisticsLink(targetPos, transportPos);
        String key = linkKey(targetPos, transportPos);

        links.put(key, link);
        machineLinks.computeIfAbsent(targetPos.asLong(), k -> ConcurrentHashMap.newKeySet()).add(key);
        machineLinks.computeIfAbsent(transportPos.asLong(), k -> ConcurrentHashMap.newKeySet()).add(key);

        return link;
    }

    /**
     * 移除物流连接
     */
    public void removeLink(BlockPos targetPos, BlockPos transportPos) {
        String key = linkKey(targetPos, transportPos);
        LogisticsLink link = links.remove(key);
        if (link == null) return;

        Set<String> targetLinks = machineLinks.get(targetPos.asLong());
        if (targetLinks != null) {
            targetLinks.remove(key);
            if (targetLinks.isEmpty()) machineLinks.remove(targetPos.asLong());
        }

        Set<String> transportLinks = machineLinks.get(transportPos.asLong());
        if (transportLinks != null) {
            transportLinks.remove(key);
            if (transportLinks.isEmpty()) machineLinks.remove(transportPos.asLong());
        }
    }

    /**
     * 移除机器参与的所有连接
     */
    public void removeMachineLinks(BlockPos machinePos) {
        Set<String> linkKeys = machineLinks.remove(machinePos.asLong());
        if (linkKeys == null) return;

        for (String linkKey : new ArrayList<>(linkKeys)) {
            LogisticsLink link = links.remove(linkKey);
            if (link == null) continue;

            // 移除另一方的引用
            BlockPos otherPos = link.getOtherSide(machinePos);
            if (otherPos != null) {
                Set<String> otherLinks = machineLinks.get(otherPos.asLong());
                if (otherLinks != null) {
                    otherLinks.remove(linkKey);
                    if (otherLinks.isEmpty()) machineLinks.remove(otherPos.asLong());
                }
            }
        }
    }

    /**
     * 获取机器参与的所有连接
     */
    public List<LogisticsLink> getMachineLinks(BlockPos machinePos) {
        Set<String> linkKeys = machineLinks.get(machinePos.asLong());
        if (linkKeys == null) return Collections.emptyList();

        List<LogisticsLink> result = new ArrayList<>();
        for (String linkKey : linkKeys) {
            LogisticsLink link = links.get(linkKey);
            if (link != null) result.add(link);
        }
        return result;
    }

    /**
     * 获取机器作为目标的所有连接（接收方）
     */
    public List<LogisticsLink> getTargetLinks(BlockPos machinePos) {
        List<LogisticsLink> result = new ArrayList<>();
        for (LogisticsLink link : getMachineLinks(machinePos)) {
            if (link.getTargetPos().equals(machinePos)) {
                result.add(link);
            }
        }
        return result;
    }

    /**
     * 获取机器作为传输的所有连接（发送方）
     */
    public List<LogisticsLink> getTransportLinks(BlockPos machinePos) {
        List<LogisticsLink> result = new ArrayList<>();
        for (LogisticsLink link : getMachineLinks(machinePos)) {
            if (link.getTransportPos().equals(machinePos)) {
                result.add(link);
            }
        }
        return result;
    }

    /**
     * 获取所有连接
     */
    public Collection<LogisticsLink> getAllLinks() {
        return Collections.unmodifiableCollection(links.values());
    }

    /**
     * 每tick执行物流传输
     */
    public void tick(Level level) {
        if (level.isClientSide()) return;

        for (LogisticsLink link : links.values()) {
            if (!link.isActive()) continue;
            processLink(link, level);
        }
    }

    /**
     * 处理单个连接的物流传输
     */
    private void processLink(LogisticsLink link, Level level) {
        BlockPos transportPos = link.getTransportPos();
        BlockPos targetPos = link.getTargetPos();

        BlockEntity transportBe = level.getBlockEntity(transportPos);
        BlockEntity targetBe = level.getBlockEntity(targetPos);

        if (transportBe == null || targetBe == null) {
            link.setActive(false);
            return;
        }

        // TODO: 实现具体的物品/流体传输逻辑
        // 这里可以扩展为根据机器类型自动传输
    }

    /**
     * 获取连接统计信息
     */
    public int getLinkCount() { return links.size(); }

    public int getMachineCount() { return machineLinks.size(); }

    /**
     * 清空所有连接（世界卸载时调用）
     */
    public void clear() {
        links.clear();
        machineLinks.clear();
    }
}
