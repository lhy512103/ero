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
        list.add(of("metal_former_rolling", ModBlocks.METAL_FORMER, new int[]{0}, 1,
            List.of(new ItemStack(Items.IRON_INGOT)), ModItems.IRON_PLATE.get(), 1, 80));
        list.add(new Case("metal_former_extruding", ModBlocks.METAL_FORMER, new int[]{0}, 1,
            List.of(new ItemStack(Items.IRON_INGOT)), ModItems.IRON_STICK.get(), 2, 100, 0,
            machine -> ((MetalFormerBlockEntity) machine).setMode(2)));
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
}
