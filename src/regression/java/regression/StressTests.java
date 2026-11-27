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
        tests.put("stress_logistics_2000", StressTests::logistics);
    }

    /**
     * Forming the largest planned machine means checking about 400 000 blocks. The scan must
     * spread that over ticks: no single step may look at more than its budget, and one step must
     * stay far below a tick.
     */
    private static void logistics(GameTestHelper helper) {
        var level=helper.getLevel();
        var origin=helper.absolutePos(new BlockPos(0,10,0));
        var endpoints=new ArrayList<blockentity.SiloBlockEntity>();
        for(int y=0;y<8;y++) for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
            int index=(y*16+z)*16+x;
            if(index>=2000) continue;
            // Ports face out from the upper and lower surfaces of one connected bundle.
            var pos=origin.offset(x,y,z);
            level.setBlock(pos,ModBlocks.PNEUMATIC_PIPE.get().defaultBlockState(),2);
            if(y==0 || y==7 && index<1836) {
                var dir=y==0 ? net.minecraft.core.Direction.DOWN : net.minecraft.core.Direction.UP;
                var port=pos.relative(dir);
                level.setBlock(port,ModBlocks.LOGISTICS_PORT_II.get().defaultBlockState().setValue(block.LogisticsPortBlock.FACING,dir),2);
                var store=port.relative(dir);
                level.setBlock(store,ModBlocks.BULK_SILO.get().defaultBlockState(),2);
                endpoints.add((blockentity.SiloBlockEntity)level.getBlockEntity(store));
                logistics.LogisticsManager.register(level,port);
            }
        }
        helper.assertTrue(endpoints.size()==300,"benchmark requires 300 endpoints");
        var sourcePos=origin.below();
        helper.assertTrue(logistics.LogisticsManager.networkOf(level,sourcePos).size()==300,"all benchmark interfaces must be connected");
        // A machine consumes iron each round. Refill its source before the measured transfer.
        var machinePos=origin.below(2);
        level.setBlock(machinePos, ModBlocks.COMPRESSOR.get().defaultBlockState(),2);
        var consumer=(blockentity.MachineBlockEntity)level.getBlockEntity(machinePos);
        var iron=net.neoforged.neoforge.transfer.item.ItemResource.of(new net.minecraft.world.item.ItemStack(item.ModItems.IRON_PLATE.get()));
        boolean profile=Boolean.getBoolean("iuce.sparkBenchmark");
        var server=level.getServer();
        if(profile) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"spark profiler start --interval 1");
        long elapsed=0;
        for(int i=0;i<1200;i++){
            endpoints.get(1).getInventory().set(iron,64);
            consumer.getItemHandler().set(0,net.neoforged.neoforge.transfer.item.ItemResource.EMPTY,0);
            long start=System.nanoTime();
            logistics.LogisticsManager.runCycle(level);
            if(i>=200) elapsed+=System.nanoTime()-start;
        }
        if(profile) server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"spark profiler stop --save-to-file");
        double perTick=elapsed/1e6/1000/logistics.LogisticsManager.INTERVAL;
        LOGGER.info("[IUCE stress] 2000 pipes, 300 ports, active transfer: {} ms/tick",perTick);
        helper.assertTrue(consumer.getItemHandler().getAmountAsInt(0)>0,"benchmark moved no iron");
        helper.assertTrue(perTick<0.5,"logistics exceeded 0.5 ms/tick: "+perTick);
        if (profile) {
            long stopped = System.nanoTime();
            helper.succeedWhen(() -> helper.assertTrue(System.nanoTime()-stopped > 3_000_000_000L,"waiting for local Spark report"));
        } else helper.succeed();
    }

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
