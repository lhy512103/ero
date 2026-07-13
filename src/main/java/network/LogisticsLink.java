package network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 物流双向连接
 * <p>
 * 表示目标机器和传输机器之间的双向绑定关系。
 * 目标机器：接收物品/流体的机器
 * 传输机器：发送物品/流体的机器
 * </p>
 */
public class LogisticsLink {

    /** 目标机器位置（接收方） */
    private final BlockPos targetPos;

    /** 传输机器位置（发送方） */
    private final BlockPos transportPos;

    /** 连接是否激活 */
    private boolean active;

    public LogisticsLink(BlockPos targetPos, BlockPos transportPos) {
        this.targetPos = targetPos;
        this.transportPos = transportPos;
        this.active = true;
    }

    public LogisticsLink(BlockPos targetPos, BlockPos transportPos, boolean active) {
        this.targetPos = targetPos;
        this.transportPos = transportPos;
        this.active = active;
    }

    public BlockPos getTargetPos() { return targetPos; }
    public BlockPos getTransportPos() { return transportPos; }
    public boolean isActive() { return active; }

    public void setActive(boolean active) { this.active = active; }

    /**
     * 检查指定位置是否参与此连接
     */
    public boolean involves(BlockPos pos) {
        return targetPos.equals(pos) || transportPos.equals(pos);
    }

    /**
     * 获取连接中另一方的位置
     */
    public BlockPos getOtherSide(BlockPos pos) {
        if (targetPos.equals(pos)) return transportPos;
        if (transportPos.equals(pos)) return targetPos;
        return null;
    }

    /**
     * 保存到NBT
     */
    public void save(ValueOutput output) {
        output.putLong("TargetPos", targetPos.asLong());
        output.putLong("TransportPos", transportPos.asLong());
        output.putBoolean("Active", active);
    }

    /**
     * 从NBT加载
     */
    public static LogisticsLink load(ValueInput input) {
        BlockPos targetPos = BlockPos.of(input.getLong("TargetPos").orElse(0L));
        BlockPos transportPos = BlockPos.of(input.getLong("TransportPos").orElse(0L));
        boolean active = input.getBooleanOr("Active", true);
        return new LogisticsLink(targetPos, transportPos, active);
    }

    @Override
    public String toString() {
        return "LogisticsLink[target=" + targetPos.toShortString() +
            ", transport=" + transportPos.toShortString() +
            ", active=" + active + "]";
    }
}
