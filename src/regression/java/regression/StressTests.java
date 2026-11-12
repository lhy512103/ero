package regression;

import block.ModBlocks;
import multiblock.StructureScan;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Scale checks for the parts whose cost must not grow with the size of a factory. They print
 * their measurements (search the log for "[IUCE stress]") and fail only on gross regressions,
 * since timings on a shared machine vary. They are no substitute for profiling a real world.
 */
final class StressTests {
    private StressTests() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("IUCE stress");

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("stress_structure_scan_400k", StressTests::structureScan);
    }

    /**
     * Forming the largest planned machine means checking about 400 000 blocks. The scan must
     * spread that over ticks: no single step may look at more than its budget, and one step must
     * stay far below a tick.
     */
    private static void structureScan(GameTestHelper helper) {
        // real blocks in loaded chunks, visited over and over to reach the size of a stage V shell
        List<BlockPos> area = new ArrayList<>();
        for (int x = 0; x < 12; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 5; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    helper.setBlock(pos, ModBlocks.PLANT_CASING.get());
                    area.add(helper.absolutePos(pos));
                }
            }
        }
        int size = 400_000;
        List<BlockPos> positions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) positions.add(area.get(i % area.size()));

        StructureScan scan = new StructureScan(positions);
        int[] visited = {0};
        int steps = 0, largestStep = 0;
        long worst = 0, total = 0;
        boolean done = false;
        while (!done) {
            int before = visited[0];
            long start = System.nanoTime();
            done = scan.step(helper.getLevel(), StructureScan.BUDGET, (pos, state) -> {
                visited[0]++;
                return state != null && state.is(ModBlocks.PLANT_CASING.get());
            });
            long took = System.nanoTime() - start;
            worst = Math.max(worst, took);
            total += took;
            largestStep = Math.max(largestStep, visited[0] - before);
            steps++;
        }
        LOGGER.info("[IUCE stress] structure scan: {} blocks in {} ticks ({} s), worst tick {} ms, average tick {} ms",
            visited[0], steps, steps / 20.0, String.format("%.3f", worst / 1e6), String.format("%.3f", total / 1e6 / steps));

        helper.assertTrue(visited[0] == size, "Every block must be checked exactly once, checked " + visited[0]);
        helper.assertTrue(largestStep <= StructureScan.BUDGET, "No tick may check more than its budget, checked " + largestStep);
        helper.assertTrue(steps == (size + StructureScan.BUDGET - 1) / StructureScan.BUDGET, "400 000 blocks must take 98 ticks, took " + steps);
        helper.assertTrue(worst < 25_000_000L, "One step of the scan took " + worst / 1e6 + " ms");

        // a wrong block ends the scan at once instead of looking at the rest
        helper.setBlock(new BlockPos(0, 0, 0), Blocks.DIRT);
        StructureScan failing = new StructureScan(positions);
        int[] seen = {0};
        boolean ended = failing.step(helper.getLevel(), StructureScan.BUDGET, (pos, state) -> {
            seen[0]++;
            return state != null && state.is(ModBlocks.PLANT_CASING.get());
        });
        helper.assertTrue(ended && seen[0] == 1, "A wrong block must stop the scan immediately, looked at " + seen[0]);
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(0, 0, 0), new BlockPos(11, 4, 4))) helper.setBlock(pos, Blocks.AIR);
        helper.succeed();
    }
}
