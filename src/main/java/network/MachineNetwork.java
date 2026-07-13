package network;

import net.minecraft.world.level.Level;

/**
 * 机器网络管理器
 * <p>
 * 每个世界tick调用，委托给NetworkManager执行能量传输。
 * </p>
 */
public class MachineNetwork {

    public void tick(Level level) {
        NetworkManager.getInstance().tick(level);
    }
}
