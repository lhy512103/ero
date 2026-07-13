package blockentity;

/**
 * 可tick的方块实体接口
 * <p>
 * 实现此接口的方块实体会在每个游戏tick被调用tick()方法。
 * </p>
 */
public interface TickableBlockEntity {

    /**
     * 每个游戏tick调用一次
     */
    void tick();
}
