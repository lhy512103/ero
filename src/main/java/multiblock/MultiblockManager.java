package multiblock;

import blockentity.PlantControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps large machines from rescanning their structure. Each controller registers the box its
 * structure occupies; only a block change inside that box makes the controller check again.
 */
public final class MultiblockManager {
    private MultiblockManager() {}

    private static final Map<Level, Map<BlockPos, BoundingBox>> BOXES = new WeakHashMap<>();

    public static void register(Level level, BlockPos controller, BoundingBox box) {
        if (!level.isClientSide()) BOXES.computeIfAbsent(level, l -> new HashMap<>()).put(controller.immutable(), box);
    }

    public static void unregister(Level level, BlockPos controller) {
        Map<BlockPos, BoundingBox> boxes = BOXES.get(level);
        if (boxes != null) boxes.remove(controller);
    }

    public static void onBlockChanged(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) return;
        Map<BlockPos, BoundingBox> boxes = BOXES.get(level);
        if (boxes == null || boxes.isEmpty()) return;
        BlockPos pos = event.getPos();
        for (Map.Entry<BlockPos, BoundingBox> entry : boxes.entrySet()) {
            if (!entry.getValue().isInside(pos) || pos.equals(entry.getKey())) continue;
            if (level.isLoaded(entry.getKey()) && level.getBlockEntity(entry.getKey()) instanceof PlantControllerBlockEntity controller) {
                controller.markStructureDirty();
            }
        }
    }
}
