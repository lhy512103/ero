package regression;

import blockentity.EnergyDisplay;
import blockentity.ProcessingDisplay;
import energy.EUEnergyStorageImpl;
import fluid.MachineTank;
import item.ModItems;
import machine.process.ProcessMachineBlockEntity;
import machine.process.ProcessMachineDef;
import machine.process.ProcessMachines;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Behaviour tests for every data-driven process machine, written once for all of them. Each
 * registered machine gets five tests, so adding a machine needs no test code. Two inputs are used
 * throughout: {@code minecraft:iron_ingot} and {@code minecraft:diamond}, which the shipped test
 * recipes route to the plate and steam recipes.
 */
final class ProcessMachineTests {
    private ProcessMachineTests() {}

    /** The shipped test recipe: 2 iron ingots -> 1 iron plate in 40 ticks. */
    private static final int RECIPE_TICKS = 40;

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("process_machines_process", ProcessMachineTests::processAll);
        tests.put("process_machines_power_loss", ProcessMachineTests::powerLossAll);
        tests.put("process_machines_overclock", ProcessMachineTests::overclockAll);
        tests.put("process_machines_save_load", ProcessMachineTests::saveLoadAll);
        tests.put("process_machines_missing_utility", ProcessMachineTests::missingUtilityAll);
        tests.put("process_machines_fluid", ProcessMachineTests::fluidAll);
        tests.put("process_machines_idle_reasons", ProcessMachineTests::idleReasons);
    }

    // ------------------------------------------------------------------ helpers

    private static ProcessMachineBlockEntity place(GameTestHelper helper, ProcessMachineDef def, int x) {
        BlockPos pos = new BlockPos(x, 1, 1);
        helper.setBlock(pos, ProcessMachines.get(def.id()).block().get());
        return helper.getBlockEntity(pos, ProcessMachineBlockEntity.class);
    }

    private static EUEnergyStorageImpl energy(ProcessMachineBlockEntity machine) {
        return (EUEnergyStorageImpl) ((EnergyDisplay) machine).getEnergyStorage();
    }

    private static void put(ItemStack stack, net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler handler, int slot) {
        try (var transaction = Transaction.openRoot()) {
            if (handler.insert(slot, ItemResource.of(stack), stack.getCount(), transaction) != stack.getCount())
                throw new IllegalStateException("Test inventory insertion failed at slot " + slot);
            transaction.commit();
        }
    }

    /** Iron ingots into the first input slot, which is what every test recipe starts from. */
    private static ProcessMachineBlockEntity prepare(GameTestHelper helper, ProcessMachineDef def, int x, int ingots) {
        ProcessMachineBlockEntity machine = place(helper, def, x);
        put(new ItemStack(Items.IRON_INGOT, ingots), machine.getItemHandler(), 0);
        return machine;
    }

    /** Ticks with a full buffer until the iron plate appears; returns the ticks used, or -1. */
    private static int runUntilPlate(ProcessMachineBlockEntity machine, int limit) {
        return runUntil(machine, ModItems.IRON_PLATE.get(), limit);
    }

    /** Ticks with a full buffer until {@code product} appears in the first output slot; -1 if not. */
    private static int runUntil(ProcessMachineBlockEntity machine, net.minecraft.world.item.Item product, int limit) {
        for (int tick = 1; tick <= limit; tick++) {
            energy(machine).setEnergy(energy(machine).getMaxEnergyStored());
            machine.tick();
            if (hasProduct(machine, product)) return tick;
        }
        return -1;
    }

    private static boolean hasPlate(ProcessMachineBlockEntity machine) {
        return hasProduct(machine, ModItems.IRON_PLATE.get());
    }

    private static boolean hasProduct(ProcessMachineBlockEntity machine, net.minecraft.world.item.Item product) {
        var slot = machine.def().outputStart();
        return machine.getItemHandler().getAmountAsInt(slot) > 0
            && machine.getItemHandler().getResource(slot).getItem() == product;
    }

    private static ProcessMachineBlockEntity reload(GameTestHelper helper, ProcessMachineDef def, ProcessMachineBlockEntity machine, int x) {
        var registries = helper.getLevel().registryAccess();
        CompoundTag tag = machine.saveWithoutMetadata(registries);
        Block block = machine.getBlockState().getBlock();
        helper.setBlock(new BlockPos(x, 1, 1), block);
        ProcessMachineBlockEntity copy = helper.getBlockEntity(new BlockPos(x, 1, 1), ProcessMachineBlockEntity.class);
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
        return copy;
    }

    private static void check(GameTestHelper helper, boolean value, String message) {
        helper.assertTrue(value, message);
    }

    // ------------------------------------------------------------------ the tests

    private static void processAll(GameTestHelper helper) {
        int x = 1;
        for (ProcessMachines.Entry entry : ProcessMachines.all()) {
            ProcessMachineDef def = entry.def();
            ProcessMachineBlockEntity machine = prepare(helper, def, x++, 2);
            int used = runUntilPlate(machine, RECIPE_TICKS + 10);
            check(helper, used > 0, def.id() + " must finish its first recipe within " + (RECIPE_TICKS + 10) + " ticks");
            check(helper, used >= RECIPE_TICKS, def.id() + " finished in " + used + " ticks, faster than the recipe");
            check(helper, hasPlate(machine), def.id() + " must produce the recipe output");
            check(helper, machine.getItemHandler().getAmountAsInt(0) == 0, def.id() + " must consume its inputs");
        }
        helper.succeed();
    }

    private static void powerLossAll(GameTestHelper helper) {
        int x = 1;
        for (ProcessMachines.Entry entry : ProcessMachines.all()) {
            ProcessMachineDef def = entry.def();
            ProcessMachineBlockEntity machine = prepare(helper, def, x++, 2);
            energy(machine).setEnergy(0);
            for (int i = 0; i < RECIPE_TICKS + 20; i++) machine.tick();
            check(helper, !hasPlate(machine), def.id() + " must not work without power");
            check(helper, machine.getItemHandler().getAmountAsInt(0) == 2, def.id() + " must keep its inputs without power");
            check(helper, runUntilPlate(machine, RECIPE_TICKS + 10) > 0, def.id() + " must resume once powered");
        }
        helper.succeed();
    }

    private static void overclockAll(GameTestHelper helper) {
        int x = 1;
        for (ProcessMachines.Entry entry : ProcessMachines.all()) {
            ProcessMachineDef def = entry.def();
            if (def.upgrades() < 2) continue;
            ProcessMachineBlockEntity plain = prepare(helper, def, x++, 2);
            int normal = runUntilPlate(plain, RECIPE_TICKS + 10);
            ProcessMachineBlockEntity fast = prepare(helper, def, x++, 2);
            put(new ItemStack(ModItems.OVERCLOCKER_UPGRADE.get()), fast.getItemHandler(), def.upgradeStart());
            int boosted = runUntilPlate(fast, RECIPE_TICKS + 10);
            check(helper, normal > 0 && boosted > 0, def.id() + " must finish with and without overclockers");
            int expected = (int) Math.round(RECIPE_TICKS * 0.75);
            check(helper, Math.abs(boosted - expected) <= 2,
                def.id() + " with one overclocker took " + boosted + " ticks, expected about " + expected);
        }
        helper.succeed();
    }

    private static void saveLoadAll(GameTestHelper helper) {
        int x = 1;
        for (ProcessMachines.Entry entry : ProcessMachines.all()) {
            ProcessMachineDef def = entry.def();
            ProcessMachineBlockEntity machine = prepare(helper, def, x++, 4);
            for (int i = 0; i < RECIPE_TICKS / 3; i++) {
                energy(machine).setEnergy(energy(machine).getMaxEnergyStored());
                machine.tick();
            }
            energy(machine).setEnergy(777);
            float before = ((ProcessingDisplay) machine).getProcessingProgress();
            check(helper, before > 0 && before < 1, def.id() + " must report progress part-way through an operation");

            ProcessMachineBlockEntity copy = reload(helper, def, machine, x++ + 10);
            check(helper, energy(copy).getEnergyStored() == 777,
                def.id() + " must keep its energy across save/load");
            for (int slot = 0; slot < machine.getItemHandler().size(); slot++) {
                check(helper, machine.getItemHandler().getResource(slot).equals(copy.getItemHandler().getResource(slot))
                        && machine.getItemHandler().getAmountAsInt(slot) == copy.getItemHandler().getAmountAsInt(slot),
                    def.id() + " must keep slot " + slot + " across save/load");
            }
            for (int i = 0; i < machine.getTanks().size(); i++) {
                MachineTank tank = machine.getTanks().get(i), same = copy.getTanks().get(i);
                check(helper, tank.getAmount() == same.getAmount() && tank.getResource().equals(same.getResource()),
                    def.id() + " must keep tank " + i + " across save/load");
            }
        }
        helper.succeed();
    }

    private static void missingUtilityAll(GameTestHelper helper) {
        int x = 1;
        for (ProcessMachines.Entry entry : ProcessMachines.all()) {
            ProcessMachineDef def = entry.def();
            ProcessMachineBlockEntity machine = place(helper, def, x++);
            // A diamond only appears in recipes that need steam, and no utility supplier exists yet.
            put(new ItemStack(Items.DIAMOND), machine.getItemHandler(), 0);
            for (int i = 0; i < 5; i++) {
                energy(machine).setEnergy(energy(machine).getMaxEnergyStored());
                machine.tick();
            }
            check(helper, "missing_utility".equals(machine.reason()),
                def.id() + " must ask for the missing utility, got '" + machine.reason() + "'");
            check(helper, machine.getItemHandler().getAmountAsInt(0) == 1, def.id() + " must not consume the input");
            check(helper, machine.getStatusKey().endsWith("missing_utility"), def.id() + " must title its status as missing_utility");
        }
        helper.succeed();
    }

    private static void idleReasons(GameTestHelper helper) {
        ProcessMachineDef def = ProcessMachines.all().iterator().next().def();
        ProcessMachineBlockEntity machine = place(helper, def, 1);
        for (int i = 0; i < 3; i++) machine.tick();
        check(helper, machine.reason().isEmpty(), "an empty machine must not complain, got '" + machine.reason() + "'");
        check(helper, machine.getStatusKey().isEmpty() || !machine.getStatusKey().endsWith("no_recipe"), "an empty machine is idle, not broken");

        put(new ItemStack(Items.STONE, 4), machine.getItemHandler(), 0);
        for (int i = 0; i < 3; i++) machine.tick();
        check(helper, "no_recipe".equals(machine.reason()), "stone must be reported as having no recipe, got '" + machine.reason() + "'");
        check(helper, "tooltip.iuce.status.no_recipe".equals(machine.getStatusKey()), "the status key must name the reason");

        // a full output slot must hold the machine back instead of dropping the product
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            var handler = machine.getItemHandler();
            handler.extract(0, handler.getResource(0), 4, transaction);
            transaction.commit();
        }
        put(new ItemStack(Items.IRON_INGOT, 2), machine.getItemHandler(), 0);
        put(new ItemStack(Items.STONE, 64), machine.getItemHandler(), def.outputStart());
        int used = runUntilPlate(machine, RECIPE_TICKS + 10);
        check(helper, used < 0, "a full output slot must hold the machine back");
        check(helper, "output_full".equals(machine.reason()), "and say so, got '" + machine.reason() + "'");
        helper.succeed();
    }

    private static void fluidAll(GameTestHelper helper) {
        int x = 1;
        for (ProcessMachines.Entry entry : ProcessMachines.all()) {
            ProcessMachineDef def = entry.def();
            if (def.fluidsIn().isEmpty()) continue;
            ProcessMachineBlockEntity machine = place(helper, def, x++);
            MachineTank tank = machine.getTanks().getFirst();
            check(helper, tank.fill(net.minecraft.world.level.material.Fluids.WATER, 500) == 500,
                def.id() + " must take water into its input tank");
            put(new ItemStack(ModItems.COPPER_DUST.get()), machine.getItemHandler(), 0);
            int used = runUntil(machine, ModItems.COPPER_PLATE.get(), RECIPE_TICKS * 2);
            check(helper, used > 0, def.id() + " must run its recipe with fluid");
            check(helper, machine.getItemHandler().getAmountAsInt(0) == 0, def.id() + " must consume the item input");
            check(helper, tank.isEmpty() || tank.getAmount() == 0, def.id() + " must consume 500 mB of water");
            check(helper, machine.getTanks().get(1).getAmount() == 250,
                def.id() + " must put 250 mB of coolant in its output tank, got " + machine.getTanks().get(1).getAmount());
        }
        helper.succeed();
    }
}
