package regression;

import block.ModBlocks;
import blockentity.EnergyDisplay;
import energy.EUEnergyStorageImpl;
import fluid.MachineTank;
import init.ModFluids;
import item.ModItems;
import machine.process.ProcessMachineBlockEntity;
import machine.process.ProcessMachines;
import machine.utility.Utility;
import machine.utility.UtilityNetwork;
import machine.utility.UtilityPlantBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The M1 line: biological ethylene, and the utility shares that keep it running.
 * <p>
 * The boiler and pipe tests use the boiler's eight steam shares with liquefaction tanks, whose only
 * shipped recipe needs steam. The line tests drive every machine by hand the way the test press
 * tests do, so they do not depend on the game loop's timing; the network is reset whenever the
 * layout changes so that the next tick sees it.
 * </p>
 */
final class M1LineTests {
    private M1LineTests() {}

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        tests.put("m1_steam_shares", M1LineTests::steamShares);
        tests.put("m1_pipe_reach", M1LineTests::pipeReach);
        tests.put("m1_ethylene_line", M1LineTests::ethyleneLine);
        tests.put("m1_byproducts", M1LineTests::byproducts);
    }

    // ------------------------------------------------------------------ helpers

    private static final BlockPos ORIGIN = new BlockPos(1, 1, 1);

    private static ProcessMachineBlockEntity machine(GameTestHelper helper, String id, int x) {
        BlockPos pos = ORIGIN.offset(x, 0, 0);
        helper.setBlock(pos, ProcessMachines.get(id).block().get());
        return helper.getBlockEntity(pos, ProcessMachineBlockEntity.class);
    }

    private static EUEnergyStorageImpl energy(ProcessMachineBlockEntity machine) {
        return (EUEnergyStorageImpl) ((EnergyDisplay) machine).getEnergyStorage();
    }

    /** Tops the machine up and ticks it once; every hand-driven test does this before each tick. */
    private static void poweredTick(ProcessMachineBlockEntity machine) {
        energy(machine).setEnergy(energy(machine).getMaxEnergyStored());
        machine.tick();
    }

    private static void give(blockentity.MachineBlockEntity machine, int slot, ItemStack stack) {
        var handler = machine.getItemHandler();
        try (Transaction tx = Transaction.openRoot()) {
            if (handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx) != stack.getCount())
                throw new IllegalStateException("Could not insert " + stack + " into slot " + slot + " of " + machine);
            tx.commit();
        }
    }

    /** A fresh utility round, so a layout built a moment ago is seen on the very next tick. */
    private static void newRound(GameTestHelper helper) {
        UtilityNetwork.of((ServerLevel) helper.getLevel()).clear();
    }

    private static UtilityNetwork network(GameTestHelper helper) {
        return UtilityNetwork.of((ServerLevel) helper.getLevel());
    }

    private static void check(GameTestHelper helper, boolean value, String message) {
        helper.assertTrue(value, message);
    }

    private static int count(ProcessMachineBlockEntity machine, int slot) {
        return machine.getItemHandler().getAmountAsInt(slot);
    }

    private static MachineTank input(ProcessMachineBlockEntity machine, int index) {
        return machine.getTanks().get(index);
    }

    private static MachineTank output(ProcessMachineBlockEntity machine, int index) {
        return machine.getTanks().get(machine.getInputTankCount() + index);
    }

    private static int amount(MachineTank tank, Fluid fluid) {
        return tank.getResource().getFluid().isSame(fluid) ? tank.getAmount() : 0;
    }

    /** Moves up to {@code amount} of {@code fluid} between two tanks; returns how much moved. */
    private static int transfer(MachineTank from, MachineTank to, Fluid fluid, int amount) {
        try (Transaction tx = Transaction.openRoot()) {
            FluidResource resource = FluidResource.of(fluid);
            int taken = from.extract(0, resource, amount, tx);
            int moved = to.insert(0, resource, taken, tx);
            if (moved < taken) from.insert(0, resource, taken - moved, tx);
            tx.commit();
            return moved;
        }
    }

    /** Carries a product tank into the next machine's input tank, the way a pump would. */
    private static int pump(ProcessMachineBlockEntity from, int fromTank, ProcessMachineBlockEntity to, int toTank,
                            Fluid fluid, int amount) {
        return transfer(output(from, fromTank), input(to, toTank), fluid, amount);
    }

    /** A creative source touching the given spot: the machine there gets all the steam and cooling water it asks for. */
    private static void creativeUtilities(GameTestHelper helper, int x) {
        helper.setBlock(ORIGIN.offset(x, 0, 0), ModBlocks.CREATIVE_UTILITY_SOURCE.get());
    }

    /** A run of steam pipes behind the line, touching every machine from {@code from} to {@code to}. */
    private static void utilityBus(GameTestHelper helper, int from, int to) {
        for (int x = from; x < to; x++) helper.setBlock(ORIGIN.offset(x, 0, 1), ModBlocks.STEAM_PIPE.get());
        helper.setBlock(ORIGIN.offset(to, 0, 1), ModBlocks.CREATIVE_UTILITY_SOURCE.get());
    }

    // ------------------------------------------------------------------ utility shares

    /**
     * The boiler gives eight steam shares, so eight tanks next to it run and the ninth stays idle
     * with "missing_utility" and the numbers the menu shows.
     */
    private static void steamShares(GameTestHelper helper) {
        newRound(helper);
        helper.setBlock(ORIGIN, ModBlocks.BOILER.get());
        UtilityPlantBlockEntity boiler = helper.getBlockEntity(ORIGIN, UtilityPlantBlockEntity.class);
        give(boiler, 0, new ItemStack(Items.COAL_BLOCK, 4));
        boiler.getTank().fill(Fluids.WATER, 8_000);

        List<ProcessMachineBlockEntity> tanks = new ArrayList<>();
        for (int i = 1; i <= 9; i++) tanks.add(machine(helper, "liquefaction_tank", i));
        for (ProcessMachineBlockEntity tank : tanks) {
            give(tank, 0, new ItemStack(ModItems.ENZYME_PREPARATION.get()));
            input(tank, 0).fill(ModFluids.chemical("starch_slurry"), 1_000);
        }

        newRound(helper);
        for (ProcessMachineBlockEntity tank : tanks) poweredTick(tank);

        int running = 0, shortOfSteam = 0;
        ProcessMachineBlockEntity idle = null;
        for (ProcessMachineBlockEntity tank : tanks) {
            if (tank.currentRecipe() != null) running++;
            if ("missing_utility".equals(tank.reason())) { shortOfSteam++; idle = tank; }
        }
        check(helper, boiler.occupied() == 8, "the boiler must serve eight shares, it serves " + boiler.occupied());
        check(helper, running == 8, "eight tanks must run on eight shares, " + running + " do");
        check(helper, shortOfSteam == 1, "exactly one tank must report missing_utility, " + shortOfSteam + " do");
        check(helper, idle != null && idle.shortUtility() == Utility.STEAM, "the idle tank must be short of steam");
        check(helper, idle.neededShares() == 1 && idle.freeShares() == 0,
            "the idle tank must say it needs 1 share and 0 are free, it says "
                + idle.neededShares() + " and " + idle.freeShares());
        check(helper, count(idle, 0) == 1 && input(idle, 0).getAmount() >= 1_000, "the idle tank must keep its inputs");

        // the same layout hands out the same shares in a second round
        newRound(helper);
        for (ProcessMachineBlockEntity tank : tanks) poweredTick(tank);
        check(helper, boiler.occupied() == 8, "the second round must also serve eight shares");
        helper.succeed();
    }

    /** Steam crosses a run of pipes; breaking a pipe or running the boiler dry cuts the whole run off. */
    private static void pipeReach(GameTestHelper helper) {
        newRound(helper);
        helper.setBlock(ORIGIN, ModBlocks.BOILER.get());
        UtilityPlantBlockEntity boiler = helper.getBlockEntity(ORIGIN, UtilityPlantBlockEntity.class);
        give(boiler, 0, new ItemStack(Items.COAL_BLOCK, 2));
        boiler.getTank().fill(Fluids.WATER, 8_000);
        for (int i = 1; i <= 4; i++) helper.setBlock(ORIGIN.offset(i, 0, 0), ModBlocks.STEAM_PIPE.get());

        ProcessMachineBlockEntity tank = machine(helper, "liquefaction_tank", 5);
        give(tank, 0, new ItemStack(ModItems.ENZYME_PREPARATION.get()));
        input(tank, 0).fill(ModFluids.chemical("starch_slurry"), 4_000);

        newRound(helper);
        poweredTick(tank);
        check(helper, tank.currentRecipe() != null, "a run of pipes must carry steam to the tank");
        check(helper, network(helper).granted(tank.position(), Utility.STEAM) == 1, "the tank must hold one steam share");

        // break the line in the middle
        helper.setBlock(ORIGIN.offset(2, 0, 0), Blocks.AIR);
        newRound(helper);
        for (int i = 0; i < UtilityNetwork.PERIOD; i++) poweredTick(tank);
        check(helper, "missing_utility".equals(tank.reason()), "a broken pipe must cut the steam off, got '" + tank.reason() + "'");

        // mend it, then run the boiler dry while it burns
        helper.setBlock(ORIGIN.offset(2, 0, 0), ModBlocks.STEAM_PIPE.get());
        boiler.getTank().drain(8_000);
        newRound(helper);
        for (int i = 0; i < UtilityNetwork.PERIOD * 2; i++) poweredTick(tank);
        check(helper, "missing_utility".equals(tank.reason()), "the tank must say the steam is missing, got '" + tank.reason() + "'");
        int stalled = tank.getProgress();
        for (int i = 0; i < UtilityNetwork.PERIOD * 2; i++) poweredTick(tank);
        check(helper, tank.getProgress() == stalled, "a tank without steam must not advance, it went from "
            + stalled + " to " + tank.getProgress());
        helper.succeed();
    }

    // ------------------------------------------------------------------ the line

    /**
     * Potato to ethylene, one machine per step, every fluid carried over by hand. The line has eight
     * machines: wet mill, liquefaction, saccharification, fermenter, column, dehydration reactor,
     * dryer and vent stack. The dryer and the vent stack get the byproducts the other steps make, so
     * nothing fills up: distiller's grains out of the stillage, and the carbon dioxide burnt off.
     * The creative source beside the vent stack feeds the whole line its steam and cooling water.
     */
    private static void ethyleneLine(GameTestHelper helper) {
        newRound(helper);
        ProcessMachineBlockEntity mill = machine(helper, "wet_mill", 1);
        ProcessMachineBlockEntity liquefaction = machine(helper, "liquefaction_tank", 2);
        ProcessMachineBlockEntity saccharification = machine(helper, "saccharification_tank", 3);
        ProcessMachineBlockEntity fermenter = machine(helper, "fermenter", 4);
        ProcessMachineBlockEntity column = machine(helper, "distillation_column", 5);
        ProcessMachineBlockEntity reactor = machine(helper, "dehydration_reactor", 6);
        ProcessMachineBlockEntity dryer = machine(helper, "dryer", 7);
        ProcessMachineBlockEntity vent = machine(helper, "vent_stack", 8);
        // the bus sits one block behind the line, so every machine and the source are in one cluster
        utilityBus(helper, 1, 9);

        Fluid slurry = ModFluids.chemical("starch_slurry");
        Fluid mash = ModFluids.chemical("liquefied_mash");
        Fluid sugar = ModFluids.chemical("sugar_solution");
        Fluid wash = ModFluids.chemical("fermented_wash");
        Fluid co2 = ModFluids.chemical("carbon_dioxide");
        Fluid ethanol = ModFluids.chemical("ethanol");
        Fluid stillage = ModFluids.chemical("stillage");
        Fluid ethylene = ModFluids.chemical("ethylene");
        Fluid processWater = ModFluids.chemical("process_water");

        // two runs of the mill: 8 potatoes and 2 buckets of water make 2 litres of slurry
        give(mill, 0, new ItemStack(Items.POTATO, 8));
        input(mill, 0).fill(Fluids.WATER, 2_000);
        give(liquefaction, 0, new ItemStack(ModItems.ENZYME_PREPARATION.get(), 2));
        give(saccharification, saccharification.def().catalystIndex(), new ItemStack(ModItems.ENZYME_PREPARATION.get()));
        give(fermenter, fermenter.def().catalystIndex(), new ItemStack(ModItems.YEAST_CULTURE.get()));
        give(reactor, reactor.def().catalystIndex(), new ItemStack(ModItems.ACTIVATED_ALUMINA.get()));

        int millRuns = 0, liquefied = 0, saccharified = 0, fermented = 0, distilled = 0, dehydrated = 0;
        boolean millWasRunning = false;
        int tick = 0;
        for (; tick < 3_000; tick++) {
            if (tick % UtilityNetwork.PERIOD == 0) newRound(helper);
            // the pump between each pair of machines
            while (pump(mill, 0, liquefaction, 0, slurry, 1_000) == 1_000) liquefied++;
            while (pump(liquefaction, 0, saccharification, 0, mash, 1_000) == 1_000) saccharified++;
            while (pump(saccharification, 0, fermenter, 0, sugar, 1_000) == 1_000) fermented++;
            while (pump(fermenter, 0, column, 0, wash, 1_000) == 1_000) distilled++;
            while (pump(column, 0, reactor, 0, ethanol, 500) == 500) dehydrated++;
            // byproducts leave on their own machine
            while (transfer(output(fermenter, 1), input(vent, 0), co2, 250) == 250) {}
            while (transfer(output(column, 1), input(dryer, 0), stillage, 500) == 500) {}

            for (ProcessMachineBlockEntity machine : List.of(mill, liquefaction, saccharification, fermenter,
                column, reactor, dryer, vent)) poweredTick(machine);

            boolean millRunning = mill.currentRecipe() != null;
            if (millWasRunning && !millRunning) millRuns++;
            millWasRunning = millRunning;
            if (millRuns >= 2 && ethylene(reactor, ethylene) >= 1_000 && processWater(reactor, processWater) >= 500
                && count(dryer, dryer.def().outputStart()) >= 1) break;
        }

        check(helper, millRuns == 2, "the mill must have run twice, it ran " + millRuns + " time(s)");
        check(helper, liquefied >= 2 && saccharified >= 2 && fermented >= 2 && distilled >= 2 && dehydrated >= 2,
            "every step must have run twice: liquefied " + liquefied + ", saccharified " + saccharified
                + ", fermented " + fermented + ", distilled " + distilled + ", dehydrated " + dehydrated);
        check(helper, amount(output(reactor, 0), ethylene) == 1_000,
            "the line must end with 1 litre of ethylene, it has " + amount(output(reactor, 0), ethylene));
        check(helper, amount(output(reactor, 1), processWater) == 500,
            "the line must also make 500 mB of process water, it has " + amount(output(reactor, 1), processWater));
        check(helper, amount(input(dryer, 0), stillage) == 0, "the dryer must keep up with the stillage");
        check(helper, amount(input(vent, 0), co2) < 1_000,
            "the vent stack must keep up with the carbon dioxide, it holds " + amount(input(vent, 0), co2) + " mB");

        // nothing jammed: every machine is either idle or still has everything it needs
        for (ProcessMachineBlockEntity machine : List.of(mill, liquefaction, saccharification, fermenter,
            column, reactor, dryer, vent)) {
            check(helper, !"output_full".equals(machine.reason()) && !"tank_full".equals(machine.reason()),
                machine.def().id() + " jammed with '" + machine.reason() + "' after " + tick + " ticks");
        }
        helper.succeed();
    }

    private static int ethylene(ProcessMachineBlockEntity reactor, Fluid ethylene) {
        return amount(output(reactor, 0), ethylene);
    }

    private static int processWater(ProcessMachineBlockEntity reactor, Fluid processWater) {
        return amount(output(reactor, 1), processWater);
    }

    /** Carbon dioxide vents and stillage dries, but the vent stack refuses anything but a ventable gas. */
    private static void byproducts(GameTestHelper helper) {
        newRound(helper);
        ProcessMachineBlockEntity vent = machine(helper, "vent_stack", 3);
        ProcessMachineBlockEntity dryer = machine(helper, "dryer", 1);
        creativeUtilities(helper, 2);

        Fluid co2 = ModFluids.chemical("carbon_dioxide");
        Fluid stillage = ModFluids.chemical("stillage");
        Fluid ethanol = ModFluids.chemical("ethanol");

        input(vent, 0).fill(co2, 1_000);
        input(dryer, 0).fill(stillage, 1_000);

        int ticks = 0;
        for (; ticks < 400; ticks++) {
            if (ticks % UtilityNetwork.PERIOD == 0) newRound(helper);
            poweredTick(vent);
            poweredTick(dryer);
            if (amount(input(vent, 0), co2) == 0 && amount(input(dryer, 0), stillage) == 0) break;
        }
        check(helper, amount(input(vent, 0), co2) == 0, "the vent stack must take a litre of carbon dioxide");
        check(helper, amount(input(dryer, 0), stillage) == 0, "the dryer must take a litre of stillage");
        check(helper, count(dryer, dryer.def().outputStart()) == 2,
            "a litre of stillage must give two distiller's grains, got " + count(dryer, dryer.def().outputStart()));

        // ethanol is not ventable, so the vent stack refuses it at the tank rather than running
        check(helper, input(vent, 0).fill(ethanol, 1_000) == 0, "the vent stack must not take ethanol");
        check(helper, input(vent, 0).isEmpty(), "the vent stack's tank must stay empty");
        check(helper, vent.currentRecipe() == null, "the vent stack must not run on ethanol");
        helper.succeed();
    }
}
