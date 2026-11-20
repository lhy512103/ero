package regression;

import block.ModBlocks;
import blockentity.EnergyDisplay;
import blockentity.MachineBlockEntity;
import blockentity.ProcessingDisplay;
import energy.EUEnergyStorageImpl;
import fluid.FluidProvider;
import item.ModItems;
import machine.generator.GeneratorBlockEntity;
import machine.geothermal_generator.GeothermalGeneratorBlockEntity;
import machine.metal_former.MetalFormerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Per-machine behaviour tests: one real recipe each, checked for processing, power loss,
 * overclocking and a save/load round trip. They pin down current behaviour so the machine
 * code can be restructured safely; when a machine's slot layout changes, update its
 * {@link Case} here.
 */
final class MachineTests {
    private MachineTests() {}

    /**
     * @param inputSlots  slots receiving {@code inputs}, in order
     * @param outputSlot  slot holding the main product
     * @param water       millibuckets of water to preload (0 for dry machines)
     * @param setup       extra preparation, e.g. selecting a mode
     */
    private record Case(String name, Supplier<? extends Block> block, int[] inputSlots, int outputSlot,
                        List<ItemStack> inputs, Item output, int outputCount, int ticks, int water,
                        Consumer<MachineBlockEntity> setup) {}

    private static Case of(String name, Supplier<? extends Block> block, int[] in, int out, List<ItemStack> inputs,
                           Item output, int count, int ticks) {
        return new Case(name, block, in, out, inputs, output, count, ticks, 0, machine -> { });
    }

    private static ItemStack stack(Supplier<? extends Item> item, int count) { return new ItemStack(item.get(), count); }

    private static List<Case> cases() {
        List<Case> list = new ArrayList<>();
        list.add(of("macerator", ModBlocks.MACERATOR, new int[]{0}, 1,
            List.of(new ItemStack(ModBlocks.TIN_ORE.get())), ModItems.CRUSHED_TIN_DUST.get(), 2, 200));
        list.add(of("compressor", ModBlocks.COMPRESSOR, new int[]{0}, 1,
            List.of(stack(ModItems.IRON_PLATE, 9)), ModItems.DENSE_IRON_PLATE.get(), 1, 300));
        list.add(of("extractor", ModBlocks.EXTRACTOR, new int[]{0}, 1,
            List.of(stack(ModItems.RAW_LATEX, 1)), ModItems.RUBBER.get(), 3, 100));
        list.add(new Case("metal_former_rolling", ModBlocks.METAL_FORMER, new int[]{0}, 1,
            List.of(new ItemStack(Items.IRON_INGOT)), ModItems.IRON_PLATE.get(), 1, 80, 0,
            machine -> mold(machine, ModItems.PLATE_MOLD.get())));
        list.add(new Case("metal_former_extruding", ModBlocks.METAL_FORMER, new int[]{0}, 1,
            List.of(new ItemStack(Items.IRON_INGOT)), ModItems.IRON_STICK.get(), 2, 100, 0,
            machine -> mold(machine, ModItems.ROD_MOLD.get())));
        list.add(of("electric_furnace", ModBlocks.ELECTRIC_FURNACE, new int[]{0}, 1,
            List.of(stack(ModItems.PURIFIED_TIN_DUST, 1)), ModItems.TIN_INGOT.get(), 3, 120));
        list.add(of("induction_furnace", ModBlocks.INDUCTION_FURNACE, new int[]{0, 1}, 2,
            List.of(new ItemStack(Items.COPPER_INGOT, 3), stack(ModItems.TIN_INGOT, 1)), ModItems.BRONZE_INGOT.get(), 3, 300));
        list.add(new Case("ore_washer", ModBlocks.ORE_WASHER, new int[]{0}, 2,
            List.of(stack(ModItems.CRUSHED_TIN_DUST, 1)), ModItems.PURIFIED_TIN_DUST.get(), 1, 160, 4000, machine -> { }));
        list.add(new Case("electrolyzer", ModBlocks.ELECTROLYZER, new int[]{0}, 1,
            List.of(stack(ModItems.DIAMOND_DUST, 1)), ModItems.ENERGIUM_DUST.get(), 2, 400, 4000, machine -> { }));
        return list;
    }

    private static final List<String> CASE_NAMES = List.of("macerator", "compressor", "extractor", "metal_former_rolling",
        "metal_former_extruding", "electric_furnace", "induction_furnace", "ore_washer", "electrolyzer");

    private static Case byName(String name) {
        List<Case> all = cases();
        if (all.size() != CASE_NAMES.size()) throw new IllegalStateException("CASE_NAMES is out of sync with cases()");
        for (Case c : all) if (c.name().equals(name)) return c;
        throw new IllegalStateException("No machine test case named " + name);
    }

    static void register(Map<String, Consumer<GameTestHelper>> tests) {
        // Registered while the mod is still loading, before blocks and items exist, so only the
        // names are known here; each test resolves its case when it runs.
        for (String name : CASE_NAMES) {
            tests.put("machine_" + name + "_process", helper -> process(helper, byName(name)));
            tests.put("machine_" + name + "_power_loss", helper -> powerLoss(helper, byName(name)));
            tests.put("machine_" + name + "_overclock", helper -> overclock(helper, byName(name)));
            tests.put("machine_" + name + "_save_load", helper -> saveLoad(helper, byName(name)));
        }
        tests.put("generator_burns_fuel", MachineTests::generator);
        tests.put("geothermal_burns_lava", MachineTests::geothermal);
        tests.put("solar_needs_sky_and_daylight", MachineTests::solar);
        tests.put("storage_save_load", MachineTests::storageSaveLoad);
        tests.put("pump_fills_from_water", MachineTests::pump);
        tests.put("machine_output_blocked", MachineTests::outputBlocked);
        tests.put("builder_completes_structure", MachineTests::builder);
        tests.put("pipe_network_fluids_channels_rest", MachineTests::networkExtras);
        tests.put("rolling_mill_molds_and_hatch_tiers", MachineTests::rollingMill);
        tests.put("repair_bench_lamp_converter", MachineTests::utilities);
        tests.put("interface_exposes_network_storage", MachineTests::networkStorage);
        tests.put("ore_plant_forms_and_processes", MachineTests::orePlant);
        tests.put("open_industrial_multiblocks", MachineTests::industrialStructures);
        tests.put("pipe_network_feeds_and_collects", MachineTests::pipeNetwork);
        tests.put("silo_and_fluid_tank", MachineTests::siloAndTank);
        tests.put("metal_former_molds", MachineTests::molds);
        tests.put("factory_hub_rates_sustained_supply", MachineTests::factoryHub);
        tests.put("underpowered_machine_slows_down", MachineTests::underpowered);
        tests.put("full_byproduct_slot_stops_machine", MachineTests::byproductBlocks);
        tests.put("conveyor_descends", MachineTests::conveyorDown);
        tests.put("conveyor_turns_and_climbs", MachineTests::conveyorRoute);
        tests.put("chute_moves_items_down", MachineTests::chute);
        tests.put("conveyor_carries_items_into_container", MachineTests::conveyor);
        tests.put("workbench_remembers_recipe", MachineTests::workbenchMemory);
        tests.put("products_return_to_feeding_side", MachineTests::returnProducts);
        tests.put("overclocker_tiers_differ", MachineTests::overclockerTiers);
        tests.put("blocks_drop_themselves_and_contents", MachineTests::drops);
        tests.put("automation_item_and_fluid_access", MachineTests::automation);
        tests.put("hoppers_feed_and_drain_machines", MachineTests::hoppers);
        tests.put("recipe_fluids_and_catalyst", MachineTests::recipeFluids);
        tests.put("fluid_tanks_and_legacy_saves", MachineTests::tanks);
        for (String name : CASE_NAMES)
            tests.put("machine_" + name + "_overclocker_added_later", helper -> lateOverclock(helper, byName(name)));
        tests.put("transfer_respects_slot_roles", MachineTests::slotRoles);
    }

    // ------------------------------------------------------------------ helpers

    private static void check(GameTestHelper helper, boolean value, String message) { helper.assertTrue(value, message); }

    private static MachineBlockEntity place(GameTestHelper helper, int x, Block block) {
        BlockPos pos = new BlockPos(x, 1, 1);
        helper.setBlock(pos, block);
        MachineBlockEntity machine = helper.getBlockEntity(pos, MachineBlockEntity.class);
        machine.onPlace();
        return machine;
    }

    private static EUEnergyStorageImpl energy(MachineBlockEntity machine) {
        return (EUEnergyStorageImpl) ((EnergyDisplay) machine).getEnergyStorage();
    }

    private static void put(ItemStacksResourceHandler inventory, int slot, ItemStack stack) {
        try (var transaction = Transaction.openRoot()) {
            if (inventory.insert(slot, ItemResource.of(stack), stack.getCount(), transaction) != stack.getCount())
                throw new IllegalStateException("Test inventory insertion failed at slot " + slot);
            transaction.commit();
        }
    }

    private static MachineBlockEntity prepare(GameTestHelper helper, Case c, int x, int batches) {
        MachineBlockEntity machine = place(helper, x, c.block().get());
        c.setup().accept(machine);
        if (c.water() > 0) ((FluidProvider) machine).insertFluid(c.water());
        for (int i = 0; i < c.inputSlots().length; i++) {
            ItemStack input = c.inputs().get(i);
            put(machine.getItemHandler(), c.inputSlots()[i], input.copyWithCount(input.getCount() * batches));
        }
        return machine;
    }

    /** Ticks with a full buffer until the output slot holds {@code wanted} items; returns ticks used or -1. */
    private static int runUntil(MachineBlockEntity machine, Case c, int wanted, int limit) {
        for (int tick = 1; tick <= limit; tick++) {
            energy(machine).setEnergy(energy(machine).getMaxEnergyStored());
            machine.tick();
            if (machine.getItemHandler().getAmountAsInt(c.outputSlot()) >= wanted) return tick;
        }
        return -1;
    }

    private static int amount(MachineBlockEntity machine, int slot) { return machine.getItemHandler().getAmountAsInt(slot); }

    private static MachineBlockEntity reload(GameTestHelper helper, MachineBlockEntity machine, int x) {
        var registries = helper.getLevel().registryAccess();
        CompoundTag tag = machine.saveWithoutMetadata(registries);
        MachineBlockEntity copy = place(helper, x, machine.getBlockState().getBlock());
        copy.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
        return copy;
    }

    // ------------------------------------------------------------------ processing machines

    private static void process(GameTestHelper helper, Case c) {
        MachineBlockEntity machine = prepare(helper, c, 1, 1);
        int used = runUntil(machine, c, c.outputCount(), c.ticks() + 10);
        check(helper, used > 0, c.name() + " must finish within " + (c.ticks() + 10) + " ticks");
        check(helper, used >= c.ticks(), c.name() + " finished in " + used + " ticks, faster than the recipe time " + c.ticks());
        check(helper, machine.getItemHandler().getResource(c.outputSlot()).getItem() == c.output()
            && amount(machine, c.outputSlot()) == c.outputCount(), c.name() + " must produce exactly the recipe output");
        for (int slot : c.inputSlots()) check(helper, amount(machine, slot) == 0, c.name() + " must consume its inputs");
        helper.succeed();
    }

    private static void powerLoss(GameTestHelper helper, Case c) {
        MachineBlockEntity machine = prepare(helper, c, 1, 1);
        energy(machine).setEnergy(0);
        for (int i = 0; i < c.ticks() + 40; i++) machine.tick();
        check(helper, amount(machine, c.outputSlot()) == 0, c.name() + " must not work without power");
        for (int slot : c.inputSlots()) check(helper, amount(machine, slot) > 0, c.name() + " must keep its inputs without power");
        check(helper, runUntil(machine, c, c.outputCount(), c.ticks() + 10) > 0, c.name() + " must resume once powered");
        helper.succeed();
    }

    private static void overclock(GameTestHelper helper, Case c) {
        MachineBlockEntity plain = prepare(helper, c, 1, 1);
        int normal = runUntil(plain, c, c.outputCount(), c.ticks() + 10);
        MachineBlockEntity fast = prepare(helper, c, 3, 1);
        if (fast.getUpgradeSlotCount() == 0) {
            // Machines without upgrade slots are simply exempt.
            helper.succeed();
            return;
        }
        int start = fast.getUpgradeSlotStart();
        for (int i = 0; i < 2; i++) put(fast.getItemHandler(), start + i, new ItemStack(ModItems.OVERCLOCKER_UPGRADE.get()));
        int boosted = runUntil(fast, c, c.outputCount(), c.ticks() + 10);
        check(helper, normal > 0 && boosted > 0, c.name() + " must finish with and without overclockers");
        int expected = (int) Math.round(c.ticks() * 0.75 * 0.75);
        check(helper, Math.abs(boosted - expected) <= 2,
            c.name() + " with two overclockers took " + boosted + " ticks, expected about " + expected);
        helper.succeed();
    }

    private static void saveLoad(GameTestHelper helper, Case c) {
        MachineBlockEntity machine = prepare(helper, c, 1, 2);
        int start = machine.getUpgradeSlotCount() > 0 ? machine.getUpgradeSlotStart() : -1;
        if (start >= 0) put(machine.getItemHandler(), start, new ItemStack(ModItems.OVERCLOCKER_UPGRADE.get()));
        // run halfway through the first batch
        for (int i = 0; i < c.ticks() / 3; i++) {
            energy(machine).setEnergy(energy(machine).getMaxEnergyStored());
            machine.tick();
        }
        energy(machine).setEnergy(777);
        float progressBefore = machine instanceof ProcessingDisplay display ? display.getProcessingProgress() : 0;
        check(helper, progressBefore > 0, c.name() + " must report progress while working");

        MachineBlockEntity copy = reload(helper, machine, 3);
        check(helper, energy(copy).getEnergyStored() == 777, c.name() + " must keep its energy across save/load");
        for (int slot = 0; slot < machine.getItemHandler().size(); slot++) {
            check(helper, machine.getItemHandler().getResource(slot).equals(copy.getItemHandler().getResource(slot))
                && amount(machine, slot) == amount(copy, slot), c.name() + " must keep slot " + slot + " across save/load");
        }
        if (machine instanceof FluidProvider before && copy instanceof FluidProvider after)
            check(helper, before.getStoredFluid() == after.getStoredFluid(), c.name() + " must keep its fluid across save/load");
        float progressAfter = copy instanceof ProcessingDisplay display ? display.getProcessingProgress() : 0;
        check(helper, Math.abs(progressAfter - progressBefore) < 0.02f,
            c.name() + " must keep its progress across save/load (" + progressBefore + " -> " + progressAfter + ")");
        c.setup().accept(copy);
        check(helper, runUntil(copy, c, c.outputCount() * 2, c.ticks() * 2 + 20) > 0,
            c.name() + " must finish both batches after reloading");
        helper.succeed();
    }

    /** A full output slot stalls the machine without consuming inputs. */
    private static void outputBlocked(GameTestHelper helper) {
        Case c = cases().get(0);
        MachineBlockEntity machine = prepare(helper, c, 1, 1);
        put(machine.getItemHandler(), c.outputSlot(), new ItemStack(Items.DIRT, 64));
        for (int i = 0; i < c.ticks() + 40; i++) {
            energy(machine).setEnergy(energy(machine).getMaxEnergyStored());
            machine.tick();
        }
        check(helper, amount(machine, 0) == 1, "A blocked output must not consume the input");
        check(helper, machine.getItemHandler().getResource(c.outputSlot()).getItem() == Items.DIRT
            && amount(machine, c.outputSlot()) == 64, "A blocked output slot must be left untouched");
        helper.succeed();
    }

    // ------------------------------------------------------------------ generators and storage

    private static void generator(GameTestHelper helper) {
        var generator = (GeneratorBlockEntity) place(helper, 1, ModBlocks.GENERATOR.get());
        put(generator.getItemHandler(), 0, new ItemStack(Items.COAL));
        for (int i = 0; i < 20; i++) generator.tick();
        long afterOneSecond = energy(generator).getEnergyStored();
        check(helper, amount(generator, 0) == 0, "Generator must consume the fuel item");
        check(helper, afterOneSecond >= 180 && afterOneSecond <= 200, "Generator must produce about 10 EU/t, got " + afterOneSecond + " after 20 ticks");
        for (int i = 0; i < 400; i++) generator.tick();
        check(helper, energy(generator).getEnergyStored() == energy(generator).getMaxEnergyStored(), "Generator buffer must fill and stop at capacity");

        var copy = (GeneratorBlockEntity) reload(helper, generator, 3);
        check(helper, energy(copy).getEnergyStored() == energy(generator).getEnergyStored(), "Generator must keep energy across save/load");
        check(helper, copy.getBurnTime() == generator.getBurnTime(), "Generator must keep remaining burn time across save/load");
        helper.succeed();
    }

    private static void geothermal(GameTestHelper helper) {
        var generator = (GeothermalGeneratorBlockEntity) place(helper, 1, ModBlocks.GEOTHERMAL_GENERATOR.get());
        for (int i = 0; i < 40; i++) generator.tick();
        check(helper, energy(generator).getEnergyStored() == 0, "Geothermal generator must not run without lava");
        check(helper, generator.addLavaBucket(), "Geothermal generator must accept a bucket of lava");
        for (int i = 0; i < 20; i++) generator.tick();
        check(helper, energy(generator).getEnergyStored() == 400, "Geothermal generator must produce 20 EU/t, got " + energy(generator).getEnergyStored());
        var copy = (GeothermalGeneratorBlockEntity) reload(helper, generator, 3);
        check(helper, copy.getStoredLava() == generator.getStoredLava() && energy(copy).getEnergyStored() == 400,
            "Geothermal generator must keep lava and energy across save/load");
        helper.succeed();
    }

    private static void solar(GameTestHelper helper) {
        // Solar generators read the overworld clock; set it to noon (the clock is shared by all tests,
        // and no other test depends on the time of day).
        var level = helper.getLevel();
        var clock = level.registryAccess().get(net.minecraft.world.clock.WorldClocks.OVERWORLD).orElseThrow();
        long now = level.clockManager().getTotalTicks(clock);
        level.clockManager().setTotalTicks(clock, now - Math.floorMod(now, 24000L) + 6000);
        MachineBlockEntity open = place(helper, 1, ModBlocks.SOLAR_GENERATOR.get());
        MachineBlockEntity covered = place(helper, 3, ModBlocks.SOLAR_GENERATOR.get());
        helper.setBlock(new BlockPos(3, 2, 1), Blocks.STONE);
        helper.runAfterDelay(60, () -> {
            check(helper, energy(open).getEnergyStored() > 0, "Solar generator under open sky must generate at noon");
            check(helper, energy(covered).getEnergyStored() == 0, "A covered solar generator must not generate");
            helper.succeed();
        });
    }

    private static void storageSaveLoad(GameTestHelper helper) {
        Block[] blocks = {ModBlocks.BATBOX.get(), ModBlocks.CESU.get(), ModBlocks.MFE.get(), ModBlocks.MFSU.get()};
        long[] capacities = {40_000L, 300_000L, 4_000_000L, 40_000_000L};
        for (int i = 0; i < blocks.length; i++) {
            MachineBlockEntity storage = place(helper, 1, blocks[i]);
            check(helper, energy(storage).getMaxEnergyStored() == capacities[i], "Unexpected capacity for " + blocks[i]);
            energy(storage).setEnergy(capacities[i] - 3);
            put(storage.getItemHandler(), 4, new ItemStack(Items.DIRT, 5));
            MachineBlockEntity copy = reload(helper, storage, 3);
            check(helper, energy(copy).getEnergyStored() == capacities[i] - 3, blocks[i] + " must keep energy across save/load");
            check(helper, copy.getItemHandler().getResource(4).getItem() == Items.DIRT && amount(copy, 4) == 5,
                blocks[i] + " must keep its items across save/load");
        }
        helper.succeed();
    }

    private static void pump(GameTestHelper helper) {
        MachineBlockEntity pump = place(helper, 3, ModBlocks.PUMP.get());
        for (BlockPos basin : new BlockPos[]{new BlockPos(2, 1, 1), new BlockPos(4, 1, 1)}) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos wall = basin.offset(dx, 0, dz);
                if (!wall.equals(basin) && !wall.equals(new BlockPos(3, 1, 1))) helper.setBlock(wall, Blocks.STONE);
            }
            helper.setBlock(basin.below(), Blocks.STONE);
            helper.setBlock(basin, Blocks.WATER);
        }
        for (int i = 0; i < 40; i++) pump.tick();
        check(helper, ((FluidProvider) pump).getStoredFluid() == 0, "Pump must not run without power");
        for (int i = 0; i < 40; i++) {
            energy(pump).setEnergy(100);
            pump.tick();
        }
        check(helper, ((FluidProvider) pump).getStoredFluid() > 0, "A powered pump next to two water sources must fill");
        check(helper, helper.getBlockState(new BlockPos(2, 1, 1)).is(Blocks.WATER), "Two adjacent sources count as infinite and must not be drained");
        MachineBlockEntity copy = reload(helper, pump, 6);
        check(helper, ((FluidProvider) copy).getStoredFluid() == ((FluidProvider) pump).getStoredFluid(), "Pump must keep its water across save/load");
        helper.succeed();
    }

    /** Ejector upgrades move products only, and only into input slots, whatever the machine's layout. */
    private static void slotRoles(GameTestHelper helper) {
        MachineBlockEntity furnace = place(helper, 1, ModBlocks.INDUCTION_FURNACE.get());
        MachineBlockEntity planter = place(helper, 3, ModBlocks.PLANTER.get());
        MachineBlockEntity electrolyzer = place(helper, 5, ModBlocks.ELECTROLYZER.get());
        MachineBlockEntity target = place(helper, 7, ModBlocks.MACERATOR.get());

        put(furnace.getItemHandler(), 1, new ItemStack(Items.REDSTONE, 4));
        put(planter.getItemHandler(), 1, new ItemStack(Items.DIRT, 4));
        check(helper, !item.EjectorUpgradeItem.pushOutputToInput(furnace, target) && amount(furnace, 1) == 4,
            "An ejector must not push the induction furnace's second input");
        check(helper, !item.EjectorUpgradeItem.pushOutputToInput(planter, target) && amount(planter, 1) == 4,
            "An ejector must not push the planter's soil");

        put(furnace.getItemHandler(), 2, new ItemStack(Items.IRON_INGOT, 2));
        check(helper, item.EjectorUpgradeItem.pushOutputToInput(furnace, target) && amount(furnace, 2) == 1 && amount(target, 0) == 1,
            "An ejector must push the induction furnace's product into the target's input");
        put(electrolyzer.getItemHandler(), 1, new ItemStack(Items.IRON_INGOT, 2));
        check(helper, item.EjectorUpgradeItem.pushOutputToInput(electrolyzer, target) && amount(electrolyzer, 1) == 1,
            "An ejector must push the electrolyzer's first output slot");
        check(helper, amount(target, 1) == 0, "An ejector must never insert into the target's output slot");
        helper.succeed();
    }

    /** Upgrades installed after the machine has already worked must apply to the next batch. */
    private static void lateOverclock(GameTestHelper helper, Case c) {
        MachineBlockEntity machine = prepare(helper, c, 1, 2);
        if (machine.getUpgradeSlotCount() == 0) { helper.succeed(); return; }
        check(helper, runUntil(machine, c, c.outputCount(), c.ticks() + 10) > 0, c.name() + " must finish the first batch");
        int start = machine.getUpgradeSlotStart();
        for (int i = 0; i < 2; i++) put(machine.getItemHandler(), start + i, new ItemStack(ModItems.OVERCLOCKER_UPGRADE.get()));
        int second = runUntil(machine, c, c.outputCount() * 2, c.ticks() + 10);
        int expected = (int) Math.round(c.ticks() * 0.75 * 0.75);
        check(helper, second > 0 && Math.abs(second - expected) <= 2,
            c.name() + ": second batch with late overclockers took " + second + " ticks, expected about " + expected);
        helper.succeed();
    }

    /** Tanks hold real fluids, reject the wrong one, and worlds saved before tanks existed still load. */
    private static void tanks(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var water = net.minecraft.world.level.material.Fluids.WATER;
        var lava = net.minecraft.world.level.material.Fluids.LAVA;

        MachineBlockEntity washer = place(helper, 1, ModBlocks.ORE_WASHER.get());
        var washerTank = ((FluidProvider) washer).getTank();
        check(helper, washerTank.fill(lava, 1000) == 0, "A water tank must reject lava");
        check(helper, washerTank.fill(water, 20000) == 16000 && washerTank.getAmount() == 16000, "A tank must stop at its capacity");
        check(helper, washerTank.getResource().getFluid() == water, "The tank must report the fluid it holds");
        check(helper, !((FluidProvider) washer).addBucket(), "A full tank must refuse another bucket");
        check(helper, washerTank.drain(15000) == 15000 && ((FluidProvider) washer).addBucket() && washerTank.getAmount() == 2000,
            "Buckets must add exactly 1000 mB");

        // legacy ore washer save: a bare millibucket count
        CompoundTag old = washer.saveWithoutMetadata(registries);
        old.remove("tank");
        old.putInt("storedWater", 5000);
        MachineBlockEntity loaded = place(helper, 3, ModBlocks.ORE_WASHER.get());
        loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, old));
        check(helper, ((FluidProvider) loaded).getStoredFluid() == 5000, "An old ore washer save must keep its water");

        // geothermal: one bucket burns for exactly 1000 ticks; old saves counted whole buckets
        var geo = (GeothermalGeneratorBlockEntity) place(helper, 5, ModBlocks.GEOTHERMAL_GENERATOR.get());
        check(helper, geo.getTank().fill(water, 1000) == 0, "The geothermal generator must reject water");
        check(helper, geo.addLavaBucket() && geo.addLavaBucket(), "The geothermal generator must accept lava buckets");
        long produced = 0;
        for (int i = 0; i < 2100; i++) {
            long before = energy(geo).getEnergyStored();
            geo.tick();
            produced += energy(geo).getEnergyStored() - before;
            energy(geo).setEnergy(0);
        }
        check(helper, produced == 2 * 1000 * 20, "Two lava buckets must yield 40000 EU, got " + produced);
        check(helper, geo.getStoredLava() == 0, "The lava must be used up");

        CompoundTag oldGeo = geo.saveWithoutMetadata(registries);
        oldGeo.remove("tank");
        oldGeo.putInt("storedLava", 3);
        oldGeo.putInt("lavaTickRemaining", 400);
        var loadedGeo = (GeothermalGeneratorBlockEntity) place(helper, 7, ModBlocks.GEOTHERMAL_GENERATOR.get());
        loadedGeo.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, oldGeo));
        check(helper, loadedGeo.getStoredLava() == 3 && loadedGeo.getTank().getAmount() == 2000,
            "An old geothermal save with 3 buckets (one burning) must load as 2 buckets plus the burning one");
        produced = 0;
        for (int i = 0; i < 2500; i++) {
            long before = energy(loadedGeo).getEnergyStored();
            loadedGeo.tick();
            produced += energy(loadedGeo).getEnergyStored() - before;
            energy(loadedGeo).setEnergy(0);
        }
        check(helper, produced == (400 + 2000) * 20L, "The migrated lava must burn for its remaining 2400 ticks, got " + produced / 20);
        helper.succeed();
    }

    /** Recipes carry fluid input/output and a catalyst through JSON and the network; machines use the recipe's water amount. */
    private static void recipeFluids(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var water = net.minecraft.world.level.material.Fluids.WATER;
        var lava = net.minecraft.world.level.material.Fluids.LAVA;

        String json = "{\"machine_type\":\"ore_washer\",\"input\":\"minecraft:gravel\",\"result\":{\"id\":\"minecraft:flint\",\"count\":1},"
            + "\"energy\":100,\"time\":20,\"fluid_input\":{\"id\":\"minecraft:water\",\"amount\":250},"
            + "\"fluid_output\":{\"id\":\"minecraft:lava\",\"amount\":40},\"catalyst\":\"minecraft:stick\"}";
        var ops = registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        recipe.MachineRecipe parsed = recipe.MachineRecipeSerializer.CODEC.codec()
            .parse(ops, com.google.gson.JsonParser.parseString(json)).getOrThrow();
        check(helper, parsed.fluidInputAmount(water) == 250 && parsed.fluidInputAmount(lava) == -1, "fluid_input must parse");
        check(helper, parsed.getFluidOutput().getFluid() == lava && parsed.getFluidOutput().getAmount() == 40, "fluid_output must parse");
        check(helper, parsed.getCatalyst().isPresent() && parsed.getCatalyst().get().test(new ItemStack(Items.STICK)), "catalyst must parse");

        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), registries);
        recipe.MachineRecipeSerializer.STREAM_CODEC.encode(buffer, parsed);
        recipe.MachineRecipe synced = recipe.MachineRecipeSerializer.STREAM_CODEC.decode(buffer);
        check(helper, synced.fluidInputAmount(water) == 250 && synced.getFluidOutput().getAmount() == 40
            && synced.getCatalyst().isPresent() && synced.getProcessTime() == 20, "Fluids and catalyst must survive network sync");
        check(helper, buffer.readableBytes() == 0, "Network encoding and decoding must read the same number of bytes");

        // a plain recipe still round-trips with nothing extra
        recipe.MachineRecipe plain = new recipe.MachineRecipe("macerator",
            net.minecraft.world.item.crafting.Ingredient.of(Items.GRAVEL), new ItemStack(Items.FLINT), 100, 20);
        buffer.clear();
        recipe.MachineRecipeSerializer.STREAM_CODEC.encode(buffer, plain);
        recipe.MachineRecipe plainSynced = recipe.MachineRecipeSerializer.STREAM_CODEC.decode(buffer);
        check(helper, plainSynced.getFluidInput().isEmpty() && plainSynced.getFluidOutput().isEmpty() && plainSynced.getCatalyst().isEmpty()
            && plainSynced.fluidInputAmount(water) == 0, "A recipe without fluids must stay without fluids");

        // the shipped washer recipes ask for one bucket, and the machine uses exactly that
        recipe.MachineRecipe shipped = recipe.RecipeHelper.findMachineRecipe(helper.getLevel(), "ore_washer",
            new ItemStack(ModItems.CRUSHED_TIN_DUST.get()));
        check(helper, shipped != null && shipped.fluidInputAmount(water) == 1000, "Shipped ore washer recipes must declare 1000 mB of water");
        Case washer = byName("ore_washer");
        MachineBlockEntity machine = prepare(helper, washer, 1, 2);
        var tank = ((FluidProvider) machine).getTank();
        check(helper, runUntil(machine, washer, washer.outputCount(), washer.ticks() + 10) > 0 && tank.getAmount() == 3000,
            "One wash must consume exactly the recipe's 1000 mB");
        tank.drain(2500);
        for (int i = 0; i < washer.ticks() + 40; i++) {
            energy(machine).setEnergy(energy(machine).getMaxEnergyStored());
            machine.tick();
        }
        check(helper, amount(machine, washer.outputSlot()) == washer.outputCount() && tank.getAmount() == 500,
            "The washer must not start a wash with less water than the recipe needs");
        helper.succeed();
    }

    private static int insert(net.neoforged.neoforge.transfer.ResourceHandler<ItemResource> handler, ItemStack stack) {
        try (var tx = Transaction.openRoot()) {
            int inserted = handler.insert(ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
            return inserted;
        }
    }

    private static int extract(net.neoforged.neoforge.transfer.ResourceHandler<ItemResource> handler, ItemStack stack) {
        try (var tx = Transaction.openRoot()) {
            int extracted = handler.extract(ItemResource.of(stack), stack.getCount(), tx);
            tx.commit();
            return extracted;
        }
    }

    /** Other mods see machines through the standard capabilities: inputs in, outputs out, upgrades hidden. */
    private static void automation(GameTestHelper helper) {
        var level = helper.getLevel();
        var itemCap = net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK;
        var fluidCap = net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK;
        var side = net.minecraft.core.Direction.UP;

        MachineBlockEntity macerator = place(helper, 1, ModBlocks.MACERATOR.get());
        var items = level.getCapability(itemCap, macerator.getBlockPos(), side);
        check(helper, items != null && items.size() == 2, "The macerator must expose exactly its input and output slot");
        check(helper, insert(items, new ItemStack(ModBlocks.TIN_ORE.get(), 3)) == 3 && amount(macerator, 0) == 3,
            "Inserted items must land in the input slot");
        check(helper, extract(items, new ItemStack(ModBlocks.TIN_ORE.get(), 3)) == 0, "Inputs must not be extractable");
        put(macerator.getItemHandler(), 1, new ItemStack(ModItems.CRUSHED_TIN_DUST.get(), 2));
        check(helper, extract(items, new ItemStack(ModItems.CRUSHED_TIN_DUST.get(), 2)) == 2 && amount(macerator, 1) == 0,
            "Products must be extractable");
        put(macerator.getItemHandler(), macerator.getUpgradeSlotStart(), new ItemStack(ModItems.OVERCLOCKER_UPGRADE.get()));
        check(helper, extract(items, new ItemStack(ModItems.OVERCLOCKER_UPGRADE.get())) == 0, "Upgrades must not be extractable");
        check(helper, insert(items, new ItemStack(ModItems.OVERCLOCKER_UPGRADE.get())) == 0
            || amount(macerator, macerator.getUpgradeSlotStart() + 1) == 0, "Automation must never fill upgrade slots");

        MachineBlockEntity generator = place(helper, 3, ModBlocks.GENERATOR.get());
        var fuel = level.getCapability(itemCap, generator.getBlockPos(), side);
        check(helper, fuel != null && insert(fuel, new ItemStack(Items.DIRT, 4)) == 0, "The generator must refuse non-fuel items");
        check(helper, insert(fuel, new ItemStack(Items.COAL, 4)) == 4, "The generator must accept fuel");
        check(helper, amount(generator, 2) == 0, "Automation must not reach the generator's charging slot");

        MachineBlockEntity storage = place(helper, 5, ModBlocks.BATBOX.get());
        check(helper, level.getCapability(itemCap, storage.getBlockPos(), side) == null, "Storage units expose no item slots");

        MachineBlockEntity bench = place(helper, 7, ModBlocks.INDUSTRIAL_WORKBENCH.get());
        var stock = level.getCapability(itemCap, bench.getBlockPos(), side);
        check(helper, stock != null && insert(stock, new ItemStack(Items.IRON_INGOT, 8)) == 8, "The workbench must accept materials");
        for (int slot = 0; slot < 9; slot++) check(helper, amount(bench, slot) == 0, "Materials must go to the stock, not the crafting grid");

        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);
        var lava = net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.LAVA);
        MachineBlockEntity washer = place(helper, 9, ModBlocks.ORE_WASHER.get());
        var tank = level.getCapability(fluidCap, washer.getBlockPos(), side);
        check(helper, tank != null, "The ore washer must expose its tank");
        try (var tx = Transaction.openRoot()) {
            check(helper, tank.insert(lava, 1000, tx) == 0, "The washer tank must refuse lava from pipes");
            check(helper, tank.insert(water, 2500, tx) == 2500, "The washer tank must accept water from pipes");
            tx.commit();
        }
        check(helper, ((FluidProvider) washer).getStoredFluid() == 2500, "Piped water must reach the washer");
        try (var tx = Transaction.openRoot()) {
            tank.insert(water, 1000, tx);
        }
        check(helper, ((FluidProvider) washer).getStoredFluid() == 2500, "An aborted transfer must leave the tank unchanged");
        check(helper, level.getCapability(fluidCap, macerator.getBlockPos(), side) == null, "Machines without a tank expose no fluid handler");
        helper.succeed();
    }

    /** A vanilla hopper on top feeds a machine and one underneath collects its products. */
    private static void hoppers(GameTestHelper helper) {
        BlockPos machinePos = new BlockPos(1, 2, 1);
        helper.setBlock(machinePos, ModBlocks.EXTRACTOR.get());
        MachineBlockEntity machine = helper.getBlockEntity(machinePos, MachineBlockEntity.class);
        machine.onPlace();
        helper.setBlock(machinePos.above(), Blocks.HOPPER);
        helper.setBlock(machinePos.below(), Blocks.HOPPER);
        var top = helper.getBlockEntity(machinePos.above(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        var bottom = helper.getBlockEntity(machinePos.below(), net.minecraft.world.level.block.entity.HopperBlockEntity.class);
        // Products are preloaded so the test fits the time limit: a full process cycle is 100 ticks,
        // and hoppers move one item every 8 ticks.
        top.setItem(0, new ItemStack(ModItems.RAW_LATEX.get(), 2));
        put(machine.getItemHandler(), 1, new ItemStack(ModItems.RUBBER.get(), 6));
        helper.succeedWhen(() -> {
            int rubber = 0;
            for (int slot = 0; slot < bottom.getContainerSize(); slot++) {
                if (bottom.getItem(slot).is(ModItems.RUBBER.get())) rubber += bottom.getItem(slot).getCount();
            }
            check(helper, top.isEmpty() && amount(machine, 0) == 2, "The hopper above must feed the machine's input slot");
            check(helper, rubber == 6 && amount(machine, 1) == 0, "The hopper below must collect all 6 rubber, has " + rubber);
        });
    }

    /** Every block has a loot table, and a broken machine spills what it held. */
    private static void drops(GameTestHelper helper) {
        var level = helper.getLevel();
        var pickaxe = new ItemStack(Items.NETHERITE_PICKAXE);
        for (var holder : ModBlocks.BLOCKS.getEntries()) {
            Block block = holder.get();
            if (block.asItem() == Items.AIR) continue;
            var loot = block.defaultBlockState().getDrops(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, pickaxe));
            if (block == ModBlocks.RUBBER_LEAVES.get()) continue;   // leaves drop saplings by chance
            if (block == ModBlocks.MULCHED_FARMLAND.get()) {
                check(helper, loot.size()==2 && loot.stream().anyMatch(s -> s.is(ModItems.WASTE_FILM.get())), "PE mulch returns waste film");
                continue;
            }
            var expected = block == ModBlocks.ROCK_SALT_ORE.get() ? ModItems.ROCK_SALT.get() : block.asItem();
            check(helper, loot.size() == 1 && loot.get(0).is(expected), holder.getId() + " must drop " + expected + ", dropped " + loot);
        }

        BlockPos pos = new BlockPos(1, 1, 1);
        MachineBlockEntity machine = place(helper, 1, ModBlocks.MACERATOR.get());
        put(machine.getItemHandler(), 0, new ItemStack(ModBlocks.TIN_ORE.get(), 5));
        put(machine.getItemHandler(), machine.getUpgradeSlotStart(), new ItemStack(ModItems.OVERCLOCKER_UPGRADE.get()));
        level.destroyBlock(helper.absolutePos(pos), true);
        helper.runAfterDelay(2, () -> {
            var found = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(2));
            int ore = 0, upgrades = 0, machines = 0;
            for (var entity : found) {
                ItemStack stack = entity.getItem();
                if (stack.is(ModBlocks.TIN_ORE.get().asItem())) ore += stack.getCount();
                if (stack.is(ModItems.OVERCLOCKER_UPGRADE.get())) upgrades += stack.getCount();
                if (stack.is(ModBlocks.MACERATOR.get().asItem())) machines += stack.getCount();
            }
            check(helper, ore == 5 && upgrades == 1, "A broken machine must drop its contents, got ore=" + ore + " upgrades=" + upgrades);
            check(helper, machines == 1, "A broken machine must drop itself once, got " + machines);
            helper.succeed();
        });
    }

    /** Basic, advanced and ultimate overclockers have their own factors and can be mixed. */
    private static void overclockerTiers(GameTestHelper helper) {
        Case c = byName("macerator");
        var tiers = java.util.List.of(ModItems.OVERCLOCKER_UPGRADE.get(), ModItems.OVERCLOCKER_UPGRADE_1.get(), ModItems.OVERCLOCKER_UPGRADE_2.get());
        double[] time = {0.75, 0.60, 0.45};
        double[] energy = {1.5, 1.8, 2.2};
        for (int tier = 0; tier < 3; tier++) {
            MachineBlockEntity machine = prepare(helper, c, 1 + tier * 2, 1);
            put(machine.getItemHandler(), machine.getUpgradeSlotStart(), new ItemStack(tiers.get(tier)));
            var effect = item.OverclockerUpgradeItem.effect(machine);
            check(helper, Math.abs(effect.time() - time[tier]) < 1e-9 && Math.abs(effect.energy() - energy[tier]) < 1e-9,
                "Tier " + tier + " overclocker has the wrong factors: " + effect);
            int took = runUntil(machine, c, c.outputCount(), c.ticks() + 10);
            int expected = (int) Math.round(c.ticks() * time[tier]);
            check(helper, took > 0 && Math.abs(took - expected) <= 2, "Tier " + tier + " overclocker took " + took + " ticks, expected about " + expected);
        }
        MachineBlockEntity mixed = prepare(helper, c, 7, 1);
        int start = mixed.getUpgradeSlotStart();
        for (int i = 0; i < 3; i++) put(mixed.getItemHandler(), start + i, new ItemStack(tiers.get(i)));
        var effect = item.OverclockerUpgradeItem.effect(mixed);
        check(helper, Math.abs(effect.time() - 0.75 * 0.60 * 0.45) < 1e-9 && Math.abs(effect.energy() - 1.5 * 1.8 * 2.2) < 1e-9,
            "Mixed tiers must multiply: " + effect);
        check(helper, effect.time(1) == 1 && effect.energy(10) == 59, "Overclocked values must round and never drop below one tick");
        helper.succeed();
    }

    /** With the option on, products go back into the container the raw materials came from. */
    private static void returnProducts(GameTestHelper helper) {
        BlockPos machinePos = new BlockPos(1, 2, 1);
        helper.setBlock(machinePos, ModBlocks.EXTRACTOR.get());
        MachineBlockEntity machine = helper.getBlockEntity(machinePos, MachineBlockEntity.class);
        machine.onPlace();
        helper.setBlock(machinePos.above(), Blocks.BARREL);
        helper.setBlock(machinePos.below(), Blocks.BARREL);
        var feeder = helper.getBlockEntity(machinePos.above(), net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
        var other = helper.getBlockEntity(machinePos.below(), net.minecraft.world.level.block.entity.BarrelBlockEntity.class);

        check(helper, !machine.isReturningProducts() && machine.getFeedSide() == null, "The option must be off by default");
        var top = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            machine.getBlockPos(), net.minecraft.core.Direction.UP);
        check(helper, insert(top, new ItemStack(ModItems.RAW_LATEX.get(), 2)) == 2, "The machine must accept materials from above");
        check(helper, machine.getFeedSide() == net.minecraft.core.Direction.UP, "The machine must remember the feeding side");
        machine.setReturnProducts(true);

        var tag = machine.saveWithoutMetadata(helper.getLevel().registryAccess());
        check(helper, tag.getBooleanOr("ReturnProducts", false) && tag.getIntOr("FeedSide", -1) == 1, "The option and the side must be saved");

        // processing takes longer than a test may run, so the products are preloaded
        put(machine.getItemHandler(), 1, new ItemStack(ModItems.RUBBER.get(), 6));
        helper.succeedWhen(() -> {
            int rubber = 0;
            for (int slot = 0; slot < feeder.getContainerSize(); slot++) {
                if (feeder.getItem(slot).is(ModItems.RUBBER.get())) rubber += feeder.getItem(slot).getCount();
            }
            check(helper, rubber == 6, "All 6 rubber must return to the feeding barrel, it has " + rubber);
            check(helper, other.isEmpty(), "Nothing may go to the other side");
        });
    }

    private static int count(net.minecraft.world.Container container, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(item)) total += container.getItem(slot).getCount();
        }
        return total;
    }

    /** A chute empties the container above it into the one it points at, and picks up loose items. */
    private static void chute(GameTestHelper helper) {
        BlockPos chutePos = new BlockPos(1, 2, 1);
        helper.setBlock(chutePos.above(), Blocks.BARREL);
        helper.setBlock(chutePos, ModBlocks.CHUTE.get());
        helper.setBlock(chutePos.below(), Blocks.BARREL);
        var source = helper.getBlockEntity(chutePos.above(), net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
        var target = helper.getBlockEntity(chutePos.below(), net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
        source.setItem(0, new ItemStack(Items.STICK, 64));
        source.setItem(5, new ItemStack(Items.COAL, 7));

        // a second chute with nothing above it collects dropped items
        BlockPos loosePos = new BlockPos(3, 2, 1);
        helper.setBlock(loosePos, ModBlocks.CHUTE.get());
        helper.setBlock(loosePos.below(), Blocks.BARREL);
        var looseTarget = helper.getBlockEntity(loosePos.below(), net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
        helper.spawnItem(Items.FLINT, 3.5f, 3.2f, 1.5f);

        helper.succeedWhen(() -> {
            check(helper, source.isEmpty(), "The chute must empty the container above it");
            check(helper, count(target, Items.STICK) == 64 && count(target, Items.COAL) == 7, "Everything must arrive below the chute");
            check(helper, count(looseTarget, Items.FLINT) == 1, "A chute must pick up items lying on it");
        });
    }

    /** Items dropped on a conveyor travel along it and enter the container at its end. */
    private static void conveyor(GameTestHelper helper) {
        var east = net.minecraft.core.Direction.EAST;
        var facing = helper.getTestRotation().rotate(east);
        for (int x = 0; x < 3; x++) {
            helper.setBlock(new BlockPos(x, 1, 1), ModBlocks.CONVEYOR.get());
            BlockPos absolute = helper.absolutePos(new BlockPos(x, 1, 1));
            helper.getLevel().setBlockAndUpdate(absolute, ModBlocks.CONVEYOR.get().defaultBlockState().setValue(block.ConveyorBlock.FACING, facing));
        }
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.BARREL);
        var target = helper.getBlockEntity(new BlockPos(3, 1, 1), net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
        helper.spawnItem(Items.IRON_INGOT, 0.5f, 1.4f, 1.5f);
        helper.succeedWhen(() -> check(helper, count(target, Items.IRON_INGOT) == 1, "The item must reach the container at the end of the belt"));
    }

    /** The workbench keeps its layout and continues as soon as the stock has materials again. */
    private static void workbenchMemory(GameTestHelper helper) {
        MachineBlockEntity bench = place(helper, 1, ModBlocks.INDUSTRIAL_WORKBENCH.get());
        var inventory = bench.getItemHandler();
        put(inventory, 4, new ItemStack(Items.OAK_LOG));
        put(inventory, 15, new ItemStack(Items.OAK_LOG, 2));
        for (int i = 0; i < 200; i++) bench.tick();
        check(helper, amount(bench, 9) == 12, "Three logs must become 12 planks, got " + amount(bench, 9));
        check(helper, amount(bench, 4) == 0 && amount(bench, 15) == 0, "All logs must be used up");

        // materials arriving later (as from a hopper) are laid out again without anyone touching the grid
        put(inventory, 20, new ItemStack(Items.OAK_LOG));
        for (int i = 0; i < 60; i++) bench.tick();
        check(helper, amount(bench, 9) == 16, "The remembered recipe must continue when materials return, got " + amount(bench, 9));

        // the memory survives a reload
        put(inventory, 21, new ItemStack(Items.OAK_LOG));
        MachineBlockEntity loaded = reload(helper, bench, 3);
        for (int i = 0; i < 60; i++) loaded.tick();
        check(helper, amount(loaded, 9) == 20, "The remembered recipe must survive saving, got " + amount(loaded, 9));

        // other items in the stock are never pulled into the grid, and a new layout replaces the memory
        put(loaded.getItemHandler(), 22, new ItemStack(Items.STICK, 4));
        for (int i = 0; i < 20; i++) loaded.tick();
        for (int slot = 0; slot < 9; slot++) check(helper, amount(loaded, slot) == 0, "Unrelated stock must stay out of the grid");
        helper.succeed();
    }

    private static void belt(GameTestHelper helper, int x, int y, int z, net.minecraft.core.Direction direction) {
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(new BlockPos(x, y, z)), ModBlocks.CONVEYOR.get().defaultBlockState()
            .setValue(block.ConveyorBlock.FACING, helper.getTestRotation().rotate(direction)));
    }

    /** A belt below a higher belt becomes a ramp, and a belt facing sideways makes the turn. */
    private static void conveyorRoute(GameTestHelper helper) {
        var east = net.minecraft.core.Direction.EAST;
        belt(helper, 0, 1, 1, east);
        belt(helper, 1, 1, 1, east);
        belt(helper, 2, 2, 1, east);
        belt(helper, 3, 2, 1, net.minecraft.core.Direction.SOUTH);
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.BARREL);
        check(helper, helper.getBlockState(new BlockPos(1, 1, 1)).getValue(block.ConveyorBlock.ASCENDING), "The belt under a higher belt must become a ramp");
        check(helper, !helper.getBlockState(new BlockPos(0, 1, 1)).getValue(block.ConveyorBlock.ASCENDING)
            && !helper.getBlockState(new BlockPos(2, 2, 1)).getValue(block.ConveyorBlock.ASCENDING), "Other belts must stay flat");
        check(helper, helper.getBlockState(new BlockPos(3, 2, 1)).getValue(block.ConveyorBlock.SHAPE) == block.ConveyorShape.RIGHT,
            "A belt fed only from its right side must become a curve");
        check(helper, helper.getBlockState(new BlockPos(2, 2, 1)).getValue(block.ConveyorBlock.SHAPE) == block.ConveyorShape.STRAIGHT
            && helper.getBlockState(new BlockPos(1, 1, 1)).getValue(block.ConveyorBlock.SHAPE) == block.ConveyorShape.STRAIGHT,
            "Belts fed from behind (also by a ramp) must stay straight");
        var target = helper.getBlockEntity(new BlockPos(3, 2, 2), net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
        helper.spawnItem(Items.IRON_INGOT, 0.5f, 1.4f, 1.5f);
        helper.succeedWhen(() -> check(helper, count(target, Items.IRON_INGOT) == 1, "The item must climb the ramp, take the turn and reach the container"));
    }

    /** A belt fed from one block higher becomes a down ramp and carries the item on. */
    private static void conveyorDown(GameTestHelper helper) {
        var east = net.minecraft.core.Direction.EAST;
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.STONE);
        belt(helper, 0, 2, 1, east);
        belt(helper, 1, 1, 1, east);
        belt(helper, 2, 1, 1, east);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.BARREL);
        check(helper, helper.getBlockState(new BlockPos(1, 1, 1)).getValue(block.ConveyorBlock.DESCENDING), "The belt below a higher feeding belt must become a down ramp");
        check(helper, !helper.getBlockState(new BlockPos(2, 1, 1)).getValue(block.ConveyorBlock.DESCENDING)
            && !helper.getBlockState(new BlockPos(0, 2, 1)).getValue(block.ConveyorBlock.DESCENDING), "Other belts must stay flat");
        var target = helper.getBlockEntity(new BlockPos(3, 1, 1), net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
        helper.spawnItem(Items.IRON_INGOT, 0.5f, 2.4f, 1.5f);
        helper.succeedWhen(() -> check(helper, count(target, Items.IRON_INGOT) == 1, "The item must ride down the ramp and reach the container"));
    }

    /** Half the power means half the speed, not a free ride and not a stop. */
    private static void underpowered(GameTestHelper helper) {
        Case c = byName("macerator");
        MachineBlockEntity machine = prepare(helper, c, 1, 1);
        long perTick = recipe.RecipeHelper.findMachineRecipe(helper.getLevel(), "macerator", c.inputs().get(0)).getEnergyPerTick();
        energy(machine).setEnergy(0);
        int took = -1;
        boolean reported = false;
        for (int tick = 1; tick <= c.ticks() * 2 + 10; tick++) {
            if (tick % 2 == 0) energy(machine).addEnergy(perTick);
            machine.tick();
            reported |= ((blockentity.ProcessingDisplay) machine).getStatusKey().equals("tooltip.iuce.status.undervoltage");
            if (amount(machine, c.outputSlot()) >= c.outputCount()) { took = tick; break; }
        }
        check(helper, took > 0 && Math.abs(took - c.ticks() * 2) <= 2, "At half power the macerator must take about " + c.ticks() * 2 + " ticks, took " + took);
        check(helper, reported, "An underpowered machine must report it");
        helper.succeed();
    }

    /** A washer whose by-product slot is full waits instead of throwing the by-product away. */
    private static void byproductBlocks(GameTestHelper helper) {
        Case c = byName("ore_washer");
        MachineBlockEntity machine = prepare(helper, c, 1, 1);
        put(machine.getItemHandler(), 3, new ItemStack(Items.DIRT, 64));
        for (int i = 0; i < c.ticks() + 40; i++) {
            energy(machine).setEnergy(energy(machine).getMaxEnergyStored());
            machine.tick();
        }
        check(helper, amount(machine, c.outputSlot()) == 0 && amount(machine, c.inputSlots()[0]) > 0,
            "A full by-product slot must hold the whole operation back");
        check(helper, ((blockentity.ProcessingDisplay) machine).getStatusKey().equals("tooltip.iuce.status.byproduct_full"),
            "The machine must report the full by-product slot");
        long full = energy(machine).getMaxEnergyStored();
        energy(machine).setEnergy(full);
        for (int i = 0; i < 20; i++) machine.tick();
        check(helper, energy(machine).getEnergyStored() == full, "A machine waiting for room must not keep using power");
        machine.getItemHandler().set(3, ItemResource.EMPTY, 0);
        check(helper, runUntil(machine, c, c.outputCount(), 20) > 0, "The machine must continue once the by-product slot has room");
        helper.succeed();
    }

    /** The hub counts minutes of sustained supply, forgives one missed minute, and awards the star. */
    private static void factoryHub(GameTestHelper helper) {
        var level = helper.getLevel();
        var data = factory.FactoryData.get(level);
        var owner = blockentity.FactoryHubBlockEntity.NO_OWNER;
        data.setStars(owner, 0);
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.FACTORY_HUB.get());
        var hub = helper.getBlockEntity(pos, blockentity.FactoryHubBlockEntity.class);
        var tier = factory.FactoryRating.tier(0);
        var items = factory.FactoryRating.candidates(0);
        check(helper, tier != null && items.size() >= tier.kinds(), "The first star needs at least " + 3 + " candidate products, has " + items.size());
        var handler = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, helper.absolutePos(pos), net.minecraft.core.Direction.UP);
        check(helper, handler != null, "The hub must accept items from pipes");

        check(helper, insert(handler, new ItemStack(Items.DIRT, 8)) == 0, "The hub must refuse things it does not ask for");
        check(helper, insert(handler, new ItemStack(items.get(0), 64)) == 64, "The hub must take a wanted product");
        hub.tick();
        check(helper, hub.getStock(items.get(0)) == tier.capacity(), "The hub keeps at most two minutes' worth, has " + hub.getStock(items.get(0)));
        int waiting = 0;
        for (int slot = 0; slot < hub.getIntake().size(); slot++) waiting += hub.getIntake().getAmountAsInt(slot);
        check(helper, waiting == 64 - tier.capacity(), "What does not fit must wait in the intake, not vanish");
        check(helper, extract(handler, new ItemStack(items.get(0), 1)) == 0, "Nothing can be pulled back out of the hub");

        Runnable deliver = () -> {
            for (int i = 0; i < tier.kinds(); i++) insert(handler, new ItemStack(items.get(i), tier.rate()));
            hub.tick();
        };
        deliver.run();
        hub.endMinute();
        check(helper, hub.getProgressMinutes() == 1, "A supplied minute must count");
        hub.endMinute();
        check(helper, hub.getProgressMinutes() == 1 && hub.getMisses() == 1, "One missed minute must be forgiven");
        deliver.run();
        hub.endMinute();
        check(helper, hub.getProgressMinutes() == 2 && hub.getMisses() == 0, "Supply resuming must clear the miss");
        hub.endMinute();
        hub.endMinute();
        check(helper, hub.getProgressMinutes() == 0, "Two missed minutes in a row must start the count over");

        for (int minute = 0; minute < tier.minutes(); minute++) {
            check(helper, data.stars(owner) == 0, "The star must not come early");
            deliver.run();
            hub.endMinute();
        }
        check(helper, data.stars(owner) == 1 && hub.getStars() == 1, "Sustained supply must earn the first star");
        check(helper, hub.getProgressMinutes() == 0 && hub.getStock(items.get(0)) == 0, "A promotion must start the next rating from scratch");
        check(helper, factory.FactoryRating.tier(1) != null && factory.FactoryRating.candidates(1).size() >= factory.FactoryRating.tier(1).kinds(),
            "The second star must have enough candidate products");

        var loaded = level.getBlockEntity(helper.absolutePos(pos));
        var tag = hub.saveWithoutMetadata(level.registryAccess());
        check(helper, tag.contains("Stock") && tag.contains("Minutes"), "The hub must save its progress");
        data.setStars(owner, 0);
        helper.setBlock(pos, Blocks.AIR);
        helper.succeed();
    }

    private static void mold(MachineBlockEntity machine, Item mold) {
        machine.getItemHandler().set(MetalFormerBlockEntity.MOLD_SLOT,
            mold == null ? ItemResource.EMPTY : ItemResource.of(new ItemStack(mold)), mold == null ? 0 : 1);
    }

    private static void power(MachineBlockEntity machine, int ticks) {
        for (int i = 0; i < ticks; i++) {
            energy(machine).setEnergy(energy(machine).getMaxEnergyStored());
            machine.tick();
        }
    }

    /** The mold decides what the metal former makes; old saves that stored a mode number get the matching mold. */
    private static void molds(GameTestHelper helper) {
        MachineBlockEntity former = place(helper, 1, ModBlocks.METAL_FORMER.get());
        var inventory = former.getItemHandler();
        put(inventory, 0, new ItemStack(Items.IRON_INGOT, 2));
        power(former, 140);
        check(helper, amount(former, 1) == 0 && amount(former, 0) == 2, "Without a mold the metal former must do nothing");

        mold(former, ModItems.PLATE_MOLD.get());
        power(former, 90);
        check(helper, inventory.getResource(1).getItem() == ModItems.IRON_PLATE.get() && amount(former, 1) == 1,
            "The plate mold must make a plate");
        inventory.set(1, ItemResource.EMPTY, 0);
        mold(former, ModItems.ROD_MOLD.get());
        power(former, 110);
        check(helper, inventory.getResource(1).getItem() == ModItems.IRON_STICK.get() && amount(former, 1) == 2,
            "The rod mold must make rods");

        var handler = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            former.getBlockPos(), net.minecraft.core.Direction.UP);
        check(helper, handler != null && handler.size() == 2, "Pipes must not see the mold slot");
        check(helper, insert(handler, new ItemStack(ModItems.WIRE_MOLD.get())) == 0 || amount(former, 0) == 1,
            "A mold piped in is just an input, never a mold change");
        inventory.set(0, ItemResource.EMPTY, 0);
        inventory.set(1, ItemResource.EMPTY, 0);

        // a save from before molds existed: six slots and a mode number
        mold(former, null);
        var registries = helper.getLevel().registryAccess();
        CompoundTag old = former.saveWithoutMetadata(registries);
        old.putInt("mode", 1);
        var stacks = old.getListOrEmpty("stacks");
        if (stacks.size() == 7) stacks.remove(6);
        MachineBlockEntity loaded = place(helper, 3, ModBlocks.METAL_FORMER.get());
        loaded.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, old));
        check(helper, loaded.getItemHandler().size() == 7, "An old save must not shrink the inventory, size " + loaded.getItemHandler().size());
        check(helper, loaded.getItemHandler().getResource(MetalFormerBlockEntity.MOLD_SLOT).getItem() == ModItems.WIRE_MOLD.get(),
            "An old save in cutting mode must load with a wire mold");
        put(loaded.getItemHandler(), 0, new ItemStack(ModItems.COPPER_PLATE.get()));
        power(loaded, 120);
        check(helper, amount(loaded, 1) == 4, "The migrated machine must keep cutting wires, made " + amount(loaded, 1));
        helper.succeed();
    }

    private static blockentity.LogisticsPortBlockEntity port(GameTestHelper helper, int x, net.minecraft.core.Direction facing) {
        BlockPos pos = new BlockPos(x, 1, 1);
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(pos), ModBlocks.LOGISTICS_PORT.get().defaultBlockState()
            .setValue(block.LogisticsPortBlock.FACING, helper.getTestRotation().rotate(facing)));
        return helper.getBlockEntity(pos, blockentity.LogisticsPortBlockEntity.class);
    }

    /** Storage feeds a machine what it can use, and the machine's products come back to storage. */
    private static void pipeNetwork(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.BARREL);
        var barrel = helper.getBlockEntity(new BlockPos(0, 1, 1), net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
        port(helper, 1, net.minecraft.core.Direction.WEST);
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.PNEUMATIC_PIPE.get());
        helper.setBlock(new BlockPos(3, 1, 1), ModBlocks.PNEUMATIC_PIPE.get());
        var machinePort = port(helper, 4, net.minecraft.core.Direction.EAST);
        MachineBlockEntity macerator = place(helper, 5, ModBlocks.MACERATOR.get());
        check(helper, logistics.LogisticsManager.networkCount(level) >= 1, "Two interfaces joined by pipes must form a network");

        barrel.setItem(0, new ItemStack(ModBlocks.TIN_ORE.get(), 4));
        barrel.setItem(1, new ItemStack(Items.DIRT, 4));
        machinePort.configure(blockentity.LogisticsPortBlockEntity.AUTO, Items.DIRT);
        logistics.LogisticsManager.runCycle(level);
        check(helper, amount(macerator, 0) == 0, "A filter must keep other items out, and dirt is of no use to a macerator");
        machinePort.configure(blockentity.LogisticsPortBlockEntity.SUPPLY, null);
        logistics.LogisticsManager.runCycle(level);
        check(helper, amount(macerator, 0) == 0, "A supply-only interface must not take anything in");
        machinePort.configure(blockentity.LogisticsPortBlockEntity.AUTO, null);
        logistics.LogisticsManager.runCycle(level);
        check(helper, amount(macerator, 0) == 4 && count(barrel, ModBlocks.TIN_ORE.get().asItem()) == 0, "The ore must be delivered to the macerator");
        check(helper, count(barrel, Items.DIRT) == 4, "What no machine can use must stay in storage");

        put(macerator.getItemHandler(), 1, new ItemStack(ModItems.CRUSHED_TIN_DUST.get(), 2));
        logistics.LogisticsManager.runCycle(level);
        check(helper, amount(macerator, 1) == 0 && count(barrel, ModItems.CRUSHED_TIN_DUST.get()) == 2, "Products nobody needs must go into storage");
        check(helper, amount(macerator, 0) == 4, "Raw materials must not be taken back out of a machine");

        // cutting the pipe splits the network
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.AIR);
        barrel.setItem(2, new ItemStack(ModBlocks.TIN_ORE.get(), 1));
        macerator.getItemHandler().set(0, ItemResource.EMPTY, 0);
        logistics.LogisticsManager.runCycle(level);
        check(helper, amount(macerator, 0) == 0, "A cut pipe must stop deliveries");
        helper.succeed();
    }

    /** A silo holds far more than a stack of one item; a tank holds one fluid. */
    private static void siloAndTank(GameTestHelper helper) {
        var level = helper.getLevel();
        var up = net.minecraft.core.Direction.UP;
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.SILO.get());
        var silo = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, helper.absolutePos(new BlockPos(1, 1, 1)), up);
        check(helper, silo != null, "The silo must expose its inventory");
        var iron = ItemResource.of(new ItemStack(Items.IRON_INGOT));
        try (var tx = Transaction.openRoot()) {
            check(helper, silo.insert(iron, 5000, tx) == 4096, "A silo holds 64 stacks");
            check(helper, silo.insert(ItemResource.of(new ItemStack(Items.GOLD_INGOT)), 1, tx) == 0, "A silo holds one kind of item");
            tx.commit();
        }
        try (var tx = Transaction.openRoot()) {
            silo.extract(iron, 1000, tx);
        }
        check(helper, silo.getAmountAsLong(0) == 4096, "An aborted transfer must leave the silo unchanged");
        check(helper, extract(silo, new ItemStack(Items.IRON_INGOT, 64)) == 64 && silo.getAmountAsLong(0) == 4032, "Items must come back out");
        var saved = helper.getBlockEntity(new BlockPos(1, 1, 1), blockentity.SiloBlockEntity.class).saveWithoutMetadata(level.registryAccess());
        check(helper, saved.getLongOr("Amount", 0) == 4032 && saved.contains("Item"), "The silo must save its contents");

        helper.setBlock(new BlockPos(3, 1, 1), ModBlocks.FLUID_TANK.get());
        var tank = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK, helper.absolutePos(new BlockPos(3, 1, 1)), up);
        check(helper, tank != null, "The tank must expose its fluid");
        var water = net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER);
        var lava = net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.LAVA);
        try (var tx = Transaction.openRoot()) {
            check(helper, tank.insert(water, 20000, tx) == 16000, "A tank holds 16 buckets");
            check(helper, tank.insert(lava, 1000, tx) == 0, "A tank holds one fluid at a time");
            check(helper, tank.extract(water, 16000, tx) == 16000 && tank.insert(lava, 1000, tx) == 1000, "An emptied tank takes any fluid");
            tx.commit();
        }
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.AIR);
        helper.succeed();
    }

    /** The 5x5x5 plant forms, turns 16 ores into 96 ingots in one batch, and notices a missing casing. */
    private static void orePlant(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 5; z++) {
                    boolean shell = x == 0 || x == 4 || y == 0 || y == 4 || z == 0 || z == 4;
                    if (shell) helper.setBlock(new BlockPos(x, y, z), ModBlocks.PLANT_CASING.get());
                }
            }
        }
        helper.setBlock(new BlockPos(1, 1, 0), ModBlocks.ITEM_INPUT_HATCH.get());
        helper.setBlock(new BlockPos(3, 1, 0), ModBlocks.ITEM_OUTPUT_HATCH.get());
        BlockPos controllerPos = new BlockPos(2, 1, 0);
        level.setBlockAndUpdate(helper.absolutePos(controllerPos), ModBlocks.PLANT_CONTROLLER.get().defaultBlockState()
            .setValue(block.PlantControllerBlock.FACING, helper.getTestRotation().rotate(net.minecraft.core.Direction.NORTH)));
        var controller = helper.getBlockEntity(controllerPos, blockentity.PlantControllerBlockEntity.class);
        controller.tick();
        check(helper, !controller.isFormed(), "A plant without an energy hatch must not form");
        helper.setBlock(new BlockPos(2, 1, 4), ModBlocks.ENERGY_HATCH.get());
        controller.tick();
        check(helper, controller.isFormed(), "The complete plant must form once the missing hatch is placed");
        check(helper, helper.getBlockState(controllerPos).getValue(block.PlantControllerBlock.FORMED), "A formed controller must show it");

        var input = helper.getBlockEntity(new BlockPos(1, 1, 0), blockentity.HatchBlockEntity.class);
        var output = helper.getBlockEntity(new BlockPos(3, 1, 0), blockentity.HatchBlockEntity.class);
        var energyHatch = helper.getBlockEntity(new BlockPos(2, 1, 4), blockentity.EnergyHatchBlockEntity.class);
        check(helper, input.wants(ItemResource.of(new ItemStack(ModBlocks.TIN_ORE.get()))), "The input hatch must ask for ore");
        check(helper, !input.wants(ItemResource.of(new ItemStack(Items.DIRT))), "The input hatch must not ask for dirt");
        check(helper, insert(input.external(), new ItemStack(ModBlocks.TIN_ORE.get(), 20)) == 20, "Pipes must be able to fill the input hatch");
        check(helper, extract(input.external(), new ItemStack(ModBlocks.TIN_ORE.get(), 1)) == 0, "Nothing comes back out of an input hatch");
        check(helper, insert(output.external(), new ItemStack(Items.DIRT)) == 0, "Nothing can be put into an output hatch from outside");

        for (int tick = 0; tick < blockentity.PlantControllerBlockEntity.DURATION + 15; tick++) {
            energyHatch.storage().setEnergy(energyHatch.storage().getMaxEnergyStored());
            controller.tick();
            if (tick == 12) {
                check(helper, controller.isWorking(), "The plant must start on its own");
                int left = 0;
                for (int slot = 0; slot < 9; slot++) left += input.getInventory().getAmountAsInt(slot);
                check(helper, left == 4, "One batch must take 16 ores at once, " + left + " left");
            }
        }
        int ingots = 0;
        for (int slot = 0; slot < 9; slot++) {
            if (output.getInventory().getResource(slot).getItem() == ModItems.TIN_INGOT.get()) ingots += output.getInventory().getAmountAsInt(slot);
        }
        check(helper, ingots == 96, "16 ores must become 96 ingots, got " + ingots);
        check(helper, extract(output.external(), new ItemStack(ModItems.TIN_INGOT.get(), 96)) == 96, "Pipes must be able to empty the output hatch");

        helper.setBlock(new BlockPos(4, 3, 2), Blocks.AIR);
        controller.tick();
        check(helper, !controller.isFormed(), "Removing a casing block must be noticed without anyone touching the controller");
        helper.setBlock(new BlockPos(4, 3, 2), ModBlocks.PLANT_CASING.get());
        controller.tick();
        check(helper, controller.isFormed(), "Putting the casing back must form the plant again");
        helper.succeed();
    }

    private static int held(blockentity.HatchBlockEntity hatch, Item item) {
        int total = 0;
        for (int slot = 0; slot < hatch.getInventory().size(); slot++) {
            if (hatch.getInventory().getResource(slot).getItem() == item) total += hatch.getInventory().getAmountAsInt(slot);
        }
        return total;
    }

    /** A 9-long mill is completed from the controller, follows its mold, and quadruples with an HV hatch. */
    private static void rollingMill(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos controllerPos = new BlockPos(0, 1, 2);
        helper.setBlock(new BlockPos(0, 1, 1), ModBlocks.ITEM_INPUT_HATCH.get());
        helper.setBlock(new BlockPos(0, 1, 3), ModBlocks.ITEM_OUTPUT_HATCH.get());
        helper.setBlock(new BlockPos(8, 1, 2), ModBlocks.ENERGY_HATCH.get());
        level.setBlockAndUpdate(helper.absolutePos(controllerPos), ModBlocks.MILL_CONTROLLER.get().defaultBlockState()
            .setValue(block.PlantControllerBlock.FACING, helper.getTestRotation().rotate(net.minecraft.core.Direction.WEST)));
        var controller = helper.getBlockEntity(controllerPos, blockentity.PlantControllerBlockEntity.class);
        controller.tick();
        check(helper, !controller.isFormed(), "A controller with only its hatches must not form");

        int placed = 0;
        for (int round = 0; round < 5; round++) placed += controller.fill(new ItemStack(ModBlocks.PLANT_CASING.get(), 64));
        check(helper, placed == 158, "Filling must place the 158 missing casing blocks of a 9-long mill, placed " + placed);
        check(helper, helper.getBlockState(new BlockPos(4, 2, 2)).isAir(), "The inside of the mill must stay empty");
        controller.tick();
        check(helper, controller.isFormed() && controller.getLength() == 9 && controller.getParallel() == 16,
            "The filled mill must form: length " + controller.getLength() + ", parallel " + controller.getParallel());

        var input = helper.getBlockEntity(new BlockPos(0, 1, 1), blockentity.HatchBlockEntity.class);
        var output = helper.getBlockEntity(new BlockPos(0, 1, 3), blockentity.HatchBlockEntity.class);
        var iron = ItemResource.of(new ItemStack(Items.IRON_INGOT));
        check(helper, !input.wants(iron), "Without a mold the mill must not ask for anything");
        controller.swapMold(new ItemStack(ModItems.PLATE_MOLD.get()));
        check(helper, input.wants(iron), "With a plate mold the mill must ask for ingots");
        insert(input.external(), new ItemStack(Items.IRON_INGOT, 20));
        for (int tick = 0; tick < blockentity.PlantControllerBlockEntity.DURATION + 15; tick++) {
            var hatch = helper.getBlockEntity(new BlockPos(8, 1, 2), blockentity.EnergyHatchBlockEntity.class);
            hatch.storage().setEnergy(hatch.storage().getMaxEnergyStored());
            controller.tick();
            if (tick == 20) controller.swapMold(new ItemStack(ModItems.WIRE_MOLD.get()));   // takes effect with the next batch
        }
        check(helper, held(output, ModItems.IRON_PLATE.get()) == 16,
            "One batch must roll 16 ingots into 16 plates, got " + held(output, ModItems.IRON_PLATE.get()));
        check(helper, held(input, Items.IRON_INGOT) == 0 && controller.isWorking(), "The next batch must start as soon as the first is delivered");

        helper.setBlock(new BlockPos(8, 1, 2), ModBlocks.ENERGY_HATCH_HV.get());
        controller.tick();
        check(helper, controller.isFormed() && controller.getParallel() == 64, "An HV hatch must raise the parallel count to 64, is " + controller.getParallel());
        for (int tick = 0; tick < blockentity.PlantControllerBlockEntity.DURATION + 15; tick++) {
            var hatch = helper.getBlockEntity(new BlockPos(8, 1, 2), blockentity.EnergyHatchBlockEntity.class);
            hatch.storage().setEnergy(hatch.storage().getMaxEnergyStored());
            controller.tick();
        }
        check(helper, held(output, ModBlocks.IRON_CABLE.get().asItem()) == 16, "The wire mold must turn 4 ingots straight into 16 wires, got "
            + held(output, ModBlocks.IRON_CABLE.get().asItem()));
        helper.succeed();
    }

    private static void run(MachineBlockEntity machine, int ticks) {
        for (int i = 0; i < ticks; i++) machine.tick();
    }

    /** The repair bench mends with power only, the lamp follows its supply, an idle converter keeps its charge. */
    private static void utilities(GameTestHelper helper) {
        var level = helper.getLevel();
        MachineBlockEntity bench = place(helper, 1, ModBlocks.REPAIR_BENCH.get());
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        pickaxe.setDamageValue(10);
        bench.getItemHandler().set(0, ItemResource.of(pickaxe), 1);
        run(bench, 20);
        check(helper, bench.getItemHandler().getResource(0).toStack(1).getDamageValue() == 10, "Without power nothing is repaired");
        energy(bench).setEnergy(4000);
        run(bench, 15);
        check(helper, amount(bench, 0) == 0 && amount(bench, 1) == 1 && !bench.getItemHandler().getResource(1).toStack(1).isDamaged(),
            "The repaired tool must move to the output slot");
        check(helper, energy(bench).getEnergyStored() == 4000 - 10 * blockentity.RepairBenchBlockEntity.COST, "Ten points of durability cost ten times the price");
        check(helper, !bench.acceptsAutomationInput(0, ItemResource.of(new ItemStack(Items.IRON_PICKAXE)))
            && bench.acceptsAutomationInput(0, ItemResource.of(pickaxe)), "Pipes may only bring damaged items");

        MachineBlockEntity lamp = place(helper, 3, ModBlocks.LAMP.get());
        run(lamp, 20);
        check(helper, level.getBlockState(lamp.getBlockPos()).getLightEmission() == 0, "A lamp without power must be dark");
        energy(lamp).setEnergy(200);
        run(lamp, 20);
        check(helper, level.getBlockState(lamp.getBlockPos()).getLightEmission() == 15, "A powered lamp must shine");
        check(helper, energy(lamp).getEnergyStored() == 180, "A lamp uses 1 EU per tick");
        energy(lamp).setEnergy(0);
        run(lamp, 20);
        check(helper, level.getBlockState(lamp.getBlockPos()).getLightEmission() == 0, "The lamp must go out when the power does");

        MachineBlockEntity converter = place(helper, 5, ModBlocks.CONVERTER.get());
        energy(converter).setEnergy(100);
        run(converter, 5);
        check(helper, energy(converter).getEnergyStored() == 100, "A converter with nothing to feed must keep its energy");
        helper.succeed();
    }

    /** Whatever is attached to an interface sees and uses the stock of the whole network's storage. */
    private static void networkStorage(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.setBlock(new BlockPos(0, 1, 1), Blocks.BARREL);
        var barrel = helper.getBlockEntity(new BlockPos(0, 1, 1), net.minecraft.world.level.block.entity.BarrelBlockEntity.class);
        port(helper, 1, net.minecraft.core.Direction.WEST);
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.PNEUMATIC_PIPE.get());
        port(helper, 3, net.minecraft.core.Direction.EAST);
        MachineBlockEntity macerator = place(helper, 4, ModBlocks.MACERATOR.get());
        put(macerator.getItemHandler(), 1, new ItemStack(Items.DIAMOND, 3));
        barrel.setItem(4, new ItemStack(Items.STICK, 10));

        var view = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            helper.absolutePos(new BlockPos(3, 1, 1)), net.minecraft.core.Direction.UP);
        check(helper, view != null && view.size() == barrel.getContainerSize(), "The view must cover the network's storage and nothing else");
        check(helper, extract(view, new ItemStack(Items.DIAMOND, 1)) == 0, "What sits in a machine is not part of the stock");
        check(helper, extract(view, new ItemStack(Items.STICK, 4)) == 4 && count(barrel, Items.STICK) == 6, "Taking from the view must take from the barrel");
        check(helper, insert(view, new ItemStack(Items.FLINT, 5)) == 5 && count(barrel, Items.FLINT) == 5, "Putting into the view must store in the barrel");

        helper.setBlock(new BlockPos(2, 1, 1), Blocks.AIR);
        check(helper, level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK,
            helper.absolutePos(new BlockPos(3, 1, 1)), net.minecraft.core.Direction.UP).size() == 0, "An interface cut off from the network sees no stock");
        helper.succeed();
    }

    /** The builder fills in the shell from its own stock, asks the network only for what is missing, then stops. */
    private static void builder(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos controllerPos = new BlockPos(0, 1, 2);
        helper.setBlock(new BlockPos(0, 1, 1), ModBlocks.ITEM_INPUT_HATCH.get());
        helper.setBlock(new BlockPos(0, 1, 3), ModBlocks.ITEM_OUTPUT_HATCH.get());
        helper.setBlock(new BlockPos(8, 1, 2), ModBlocks.ENERGY_HATCH.get());
        var millState = ModBlocks.MILL_CONTROLLER.get().defaultBlockState()
            .setValue(block.PlantControllerBlock.FACING, helper.getTestRotation().rotate(net.minecraft.core.Direction.WEST));
        level.setBlockAndUpdate(helper.absolutePos(controllerPos), millState);
        var controller = helper.getBlockEntity(controllerPos, blockentity.PlantControllerBlockEntity.class);
        check(helper, blockentity.PlantControllerBlockEntity.targetLength(millState.setValue(block.PlantControllerBlock.LENGTH, 11)) == 11
            && multiblock.PlantStructure.cells(BlockPos.ZERO, net.minecraft.core.Direction.NORTH,
                millState.setValue(block.PlantControllerBlock.LENGTH, 11)).size() == 193,
            "The blueprint's chosen length must decide the shape of the shell");
        check(helper, controller.summary() != null && controller.missing().size() == 158, "A 9-long mill with three hatches lacks 158 casing blocks");

        helper.setBlock(new BlockPos(1, 1, 2), ModBlocks.BUILDER.get());
        var machine = helper.getBlockEntity(new BlockPos(1, 1, 2), blockentity.BuilderBlockEntity.class);
        var casing = ItemResource.of(new ItemStack(ModBlocks.PLANT_CASING.get()));
        check(helper, machine.controller() == controller, "The builder must find the controller next to it");
        check(helper, machine.requestSlot(casing) < 0, "Before looking at the structure the builder asks for nothing");
        energy(machine).setEnergy(4000);
        for (int i = 0; i < 5; i++) machine.tick();
        check(helper, machine.requestSlot(casing) >= 0 && machine.requestSlot(ItemResource.of(new ItemStack(Items.DIRT))) < 0,
            "With casing missing the builder must ask the network for casing and nothing else");
        check(helper, controller.missing().size() == 158, "Without casing nothing is placed");
        check(helper, !machine.acceptsAutomationInput(0, ItemResource.of(new ItemStack(Items.DIRT))), "Hoppers may only bring casing");

        for (int slot = 0; slot < 3; slot++) put(machine.getItemHandler(), slot, new ItemStack(ModBlocks.PLANT_CASING.get(), 64));
        for (int i = 0; i < 5; i++) machine.tick();
        check(helper, controller.missing().size() == 154, "One round places four blocks, missing " + controller.missing().size());
        for (int i = 0; i < 400; i++) {
            energy(machine).setEnergy(4000);
            machine.tick();
        }
        check(helper, controller.missing().isEmpty() && machine.held() == 192 - 158, "The builder must place exactly the 158 missing blocks, holds " + machine.held());
        check(helper, helper.getBlockState(new BlockPos(4, 2, 2)).isAir(), "The inside must stay empty");
        controller.tick();
        check(helper, controller.isFormed(), "The machine must form once the builder is done");
        check(helper, machine.requestSlot(casing) < 0, "A finished machine needs no more casing");
        helper.succeed();
    }

    /** Whether a multiblock part is hidden; every part block names its flag {@code formed}. */
    private static boolean formed(net.minecraft.world.level.block.state.BlockState state) {
        return state.getProperties().stream().anyMatch(property -> property.getName().equals("formed")
            && Boolean.TRUE.equals(state.getValue(property)));
    }

    private static void industrialStructures(GameTestHelper helper) {
        var level = helper.getLevel();
        var direction = net.minecraft.core.Direction.NORTH;
        for (boolean mill : List.of(false, true)) {
            var controllerBlock = (mill ? ModBlocks.MILL_CONTROLLER : ModBlocks.PLANT_CONTROLLER).get();
            var state = controllerBlock.defaultBlockState().setValue(block.PlantControllerBlock.LEGACY, false);
            BlockPos origin = helper.absolutePos(new BlockPos(mill ? 16 : 5, 2, 2));
            var cells = multiblock.PlantStructure.cells(origin, direction, state);
            check(helper, cells.stream().map(multiblock.PlantStructure.Cell::pos).distinct().count() == cells.size(), "Every part must have a unique position");
            check(helper, cells.stream().allMatch(cell -> multiblock.PlantStructure.bounds(origin, direction, state).isInside(cell.pos())),
                "Every part must be monitored for changes");
            var pillar = cells.stream().filter(cell -> cell.part() == multiblock.PlantStructure.Part.FRAME_UP).findFirst().orElseThrow();
            check(helper, !pillar.part().accepts(pillar.part().sample(direction).setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS,
                net.minecraft.core.Direction.Axis.X), direction), "A girder with the wrong axis must not pass");
            level.setBlockAndUpdate(origin, state);
            var controller = (blockentity.PlantControllerBlockEntity) level.getBlockEntity(origin);
            check(helper, controller != null && !controller.missing().contains(pillar.pos()), "The builder must not fill girder positions with casing");
            int assembled = controller.assembleCreative();
            check(helper, assembled > 0 && controller.isFormed() && controller.getParallel() == 16,
                "Creative assembly must build and form the structure (mill=" + mill + ", placed=" + assembled
                    + ", formed=" + controller.isFormed() + ", parallel=" + controller.getParallel()
                    + ", desc=" + controller.describe().getString() + ")");
            check(helper, Boolean.TRUE.equals(level.getBlockState(origin).getValue(block.PlantControllerBlock.FORMED)),
                "A formed controller must hide behind its assembled model (mill=" + mill + ")");
            check(helper, cells.stream().allMatch(cell -> formed(level.getBlockState(cell.pos()))),
                "All formed parts must be marked formed (mill=" + mill + ")");
            var inputHatch = cells.stream().filter(cell -> cell.part() == multiblock.PlantStructure.Part.INPUT).findFirst().orElseThrow();
            level.setBlockAndUpdate(pillar.pos(), ModBlocks.PLANT_CASING.get().defaultBlockState());
            controller.tick();
            check(helper, !controller.isFormed(), "A casing cannot replace a structural girder");
            check(helper, !formed(level.getBlockState(inputHatch.pos())),
                "Unforming the machine must make remaining parts visible again");
            level.setBlockAndUpdate(pillar.pos(), pillar.part().sample(direction));
            controller.tick();
            check(helper, controller.isFormed() && formed(level.getBlockState(inputHatch.pos())),
                "Restoring the girder must reform and re-hide the parts");
            level.removeBlock(origin, false);
            check(helper, !formed(level.getBlockState(inputHatch.pos())),
                "Removing the formed controller must restore part visibility");
            for (var cell : cells) level.removeBlock(cell.pos(), false);
        }
        BlockPos casing = helper.absolutePos(new BlockPos(8, 4, 2));
        var state = ModBlocks.PLANT_CASING.get().defaultBlockState();
        var east = net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST;
        level.setBlockAndUpdate(casing, state);
        check(helper, !level.getBlockState(casing).getValue(east), "An exposed edge must keep its trim");
        level.setBlockAndUpdate(casing.east(), ModBlocks.ITEM_INPUT_HATCH.get().defaultBlockState());
        check(helper, level.getBlockState(casing).getValue(east), "A hatch must hide the adjacent casing trim");
        level.removeBlock(casing.east(), false);
        check(helper, !level.getBlockState(casing).getValue(east), "Removing the hatch must restore the trim");
        level.removeBlock(casing, false);
        helper.succeed();
    }

    /** Fluids go from the pump to the machine's tank, channels keep interfaces apart, an idle network rests. */
    private static void networkExtras(GameTestHelper helper) {
        var level = helper.getLevel();
        var water = net.minecraft.world.level.material.Fluids.WATER;
        MachineBlockEntity pump = place(helper, 0, ModBlocks.PUMP.get());
        var pumpTank = ((FluidProvider) pump).getTank();
        var pumpPort = port(helper, 1, net.minecraft.core.Direction.WEST);
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.PNEUMATIC_PIPE.get());
        var washerPort = port(helper, 3, net.minecraft.core.Direction.EAST);
        MachineBlockEntity washer = place(helper, 4, ModBlocks.ORE_WASHER.get());
        var washerTank = ((FluidProvider) washer).getTank();
        check(helper, pumpPort.fluidRole() == blockentity.LogisticsPortBlockEntity.FLUID_PUMP
            && washerPort.fluidRole() == blockentity.LogisticsPortBlockEntity.FLUID_CONSUMER, "Pump and washer must be recognised");

        pumpTank.setAmount(water, 3000);
        logistics.LogisticsManager.runCycle(level);
        check(helper, washerTank.getAmount() == 3000 && pumpTank.getAmount() == 0, "The pump's water must reach the washer, has " + washerTank.getAmount());
        logistics.LogisticsManager.runCycle(level);
        check(helper, washerTank.getAmount() == 3000, "Water must never be pulled back out of a machine");

        washerPort.setChannel(3);
        pumpTank.setAmount(water, 2000);
        put(pump.getItemHandler() == null ? washer.getItemHandler() : washer.getItemHandler(), 2, new ItemStack(ModItems.PURIFIED_TIN_DUST.get(), 1));
        logistics.LogisticsManager.runCycle(level);
        check(helper, washerTank.getAmount() == 3000 && pumpTank.getAmount() == 2000, "Interfaces on different channels must not deal with each other");
        pumpPort.setChannel(3);
        logistics.LogisticsManager.runCycle(level);
        check(helper, washerTank.getAmount() == 5000, "Interfaces on the same channel must deal with each other");

        // nothing left to move: the network rests, and a change wakes it
        for (int i = 0; i < 3; i++) logistics.LogisticsManager.runDueCycle(level);
        check(helper, logistics.LogisticsManager.restingCount(level) >= 1, "A network with nothing to move must rest");
        pumpTank.setAmount(water, 1000);
        logistics.LogisticsManager.wake(level);
        logistics.LogisticsManager.runDueCycle(level);
        check(helper, washerTank.getAmount() == 6000, "A woken network must work again at once");
        helper.succeed();
    }
}
