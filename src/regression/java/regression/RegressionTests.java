package regression;

import block.ModBlocks;
import block.MachineBlock;
import blockentity.MachineBlockEntity;
import blockentity.EnergyDisplay;
import blockentity.cable.CableBlockEntity;
import energy.EUEnergyStorageImpl;
import energy.ItemCharging;
import item.*;
import machine.electric_furnace.ElectricFurnaceBlockEntity;
import machine.planter.CropRegistry;
import machine.planter.PlanterBlockEntity;
import machine.workbench.IndustrialWorkbenchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import network.EnergyTransferManager;
import recipe.RecipeHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Executed in an actual dedicated GameTest server with the mod's registries, capabilities and datapacks. */
public final class RegressionTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();
    static {
        TESTS.put("adjacent_energy_conservation", RegressionTests::adjacent);
        TESTS.put("storage_energy_output", RegressionTests::storageOutput);
        TESTS.put("cable_disconnect_reconnect", RegressionTests::cable);
        TESTS.put("charge_all_storage_tiers", RegressionTests::charge);
        TESTS.put("item_energy_transaction_rollback", RegressionTests::rollback);
        TESTS.put("wrench_rotation_disconnect_dismantle", RegressionTests::wrench);
        TESTS.put("electric_wrench_energy_cost", RegressionTests::electricWrench);
        TESTS.put("furnace_vanilla_and_powder_recipes", RegressionTests::furnace);
        TESTS.put("workbench_shaped_shapeless_and_remainders", RegressionTests::workbench);
        TESTS.put("planter_discovery_and_extension", RegressionTests::planter);
        TESTS.put("live_adjacent_energy", RegressionTests::liveAdjacent);
        TESTS.put("live_cable_energy", RegressionTests::liveCable);
        TESTS.put("live_high_tier_energy", RegressionTests::liveHighTier);
    }
    public static void register(IEventBus bus) {
        var functions = DeferredRegister.<Consumer<GameTestHelper>>create(Registries.TEST_FUNCTION, "iuce");
        TESTS.forEach((name, function) -> functions.register(name, () -> function));
        functions.register(bus);
        bus.addListener((RegisterGameTestsEvent event) -> {
            var environment = event.registerEnvironment(id("regression"));
            TESTS.forEach((name, function) -> event.registerTest(id(name), new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                new TestData<>(environment, id("regression_empty"), 100, 0, true))));
        });
    }
    private static Identifier id(String name) { return Identifier.fromNamespaceAndPath("iuce", name); }
    private static void check(GameTestHelper helper, boolean value, String message) { helper.assertTrue(value, message); }
    private static MachineBlockEntity machine(GameTestHelper helper, int x, Block block) {
        BlockPos pos = new BlockPos(x, 1, 1);
        helper.setBlock(pos, block);
        var machine = helper.getBlockEntity(pos, MachineBlockEntity.class);
        machine.onPlace();
        return machine;
    }
    private static EUEnergyStorageImpl energy(MachineBlockEntity machine) {
        return (EUEnergyStorageImpl) ((EnergyDisplay) machine).getEnergyStorage();
    }
    private static void put(ItemStacksResourceHandler inventory, int slot, ItemStack stack) {
        try (var transaction = Transaction.openRoot()) {
            if (inventory.insert(slot, ItemResource.of(stack), stack.getCount(), transaction) != stack.getCount())
                throw new IllegalStateException("Test inventory insertion failed at " + slot);
            transaction.commit();
        }
    }
    private static void clear(ItemStacksResourceHandler inventory, int slot) {
        var resource = inventory.getResource(slot);
        if (resource.isEmpty()) return;
        try (var transaction = Transaction.openRoot()) {
            inventory.extract(slot, resource, inventory.getAmountAsInt(slot), transaction);
            transaction.commit();
        }
    }
    private static void adjacent(GameTestHelper helper) {
        var source = machine(helper, 1, ModBlocks.GENERATOR.get());
        var target = machine(helper, 2, ModBlocks.MACERATOR.get());
        energy(source).setEnergy(100);
        EnergyTransferManager.tick(helper.getLevel());
        check(helper, energy(target).getEnergyStored() == 32, "Adjacent machine must receive LV power");
        check(helper, energy(source).getEnergyStored() == 68, "Transferred energy must be conserved exactly");
        helper.succeed();
    }
    private static void storageOutput(GameTestHelper helper) {
        var source = machine(helper, 1, ModBlocks.BATBOX.get());
        var target = machine(helper, 2, ModBlocks.MACERATOR.get());
        energy(source).setEnergy(100);
        EnergyTransferManager.tick(helper.getLevel());
        check(helper, energy(target).getEnergyStored() == 32 && energy(source).getEnergyStored() == 68,
            "Storage must supply a consumer without a generator");
        helper.succeed();
    }
    private static void cable(GameTestHelper helper) {
        var source = machine(helper, 1, ModBlocks.GENERATOR.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.COPPER_CABLE.get());
        var wire = helper.getBlockEntity(new BlockPos(2, 1, 1), CableBlockEntity.class);
        var target = machine(helper, 3, ModBlocks.MACERATOR.get());
        energy(source).setEnergy(100);
        EnergyTransferManager.tick(helper.getLevel());
        check(helper, energy(target).getEnergyStored() == 32, "Wire must supply consumer");
        wire.toggleSide(Direction.EAST);
        EnergyTransferManager.tick(helper.getLevel());
        check(helper, energy(target).getEnergyStored() == 32, "Disabled wire side must block actual power, not just rendering");
        wire.toggleSide(Direction.EAST);
        EnergyTransferManager.tick(helper.getLevel());
        check(helper, energy(target).getEnergyStored() == 64, "Re-enabled wire must supply power immediately");
        helper.succeed();
    }
    private static void charge(GameTestHelper helper) {
        Block[] blocks = {ModBlocks.BATBOX.get(), ModBlocks.CESU.get(), ModBlocks.MFE.get(), ModBlocks.MFSU.get()};
        for (int i = 0; i < blocks.length; i++) {
            var storage = machine(helper, i * 2 + 1, blocks[i]);
            var tool = new ItemStack(ModItems.ELECTRIC_WRENCH.get());
            check(helper, ChargeSlot.isChargeableItem(tool) && EquipmentChargeSlot.isEnergyEquipment(tool), "Both slot types must accept electric tools");
            put(storage.getItemHandler(), 4, tool);
            energy(storage).setEnergy(2000);
            ItemCharging.charge(energy(storage), storage.getItemHandler(), 0);
            long charged = RechargeableBatteryItem.getEnergy(storage.getItemHandler().getResource(4).toStack());
            check(helper, charged > 0 && energy(storage).getEnergyStored() + charged == 2000, "Charging must persist and conserve energy at tier " + i);
        }
        helper.succeed();
    }
    private static void rollback(GameTestHelper helper) {
        var inventory = new ItemStacksResourceHandler(1);
        put(inventory, 0, new ItemStack(ModItems.ELECTRIC_WRENCH.get()));
        var handler = ItemAccess.forHandlerIndexStrict(inventory, 0).getCapability(Capabilities.Energy.ITEM);
        check(helper, handler != null, "Tool capability must be registered");
        try (var transaction = Transaction.openRoot()) { handler.insert(100, transaction); }
        check(helper, RechargeableBatteryItem.getEnergy(inventory.getResource(0).toStack()) == 0, "Aborted insertion must roll back");
        try (var transaction = Transaction.openRoot()) { handler.insert(100, transaction); transaction.commit(); }
        check(helper, RechargeableBatteryItem.getEnergy(inventory.getResource(0).toStack()) == 100, "Committed insertion must persist");
        try (var transaction = Transaction.openRoot()) { handler.extract(50, transaction); }
        check(helper, RechargeableBatteryItem.getEnergy(inventory.getResource(0).toStack()) == 100, "Aborted extraction must roll back");
        helper.succeed();
    }
    private static UseOnContext context(GameTestHelper helper, net.minecraft.world.entity.player.Player player, int x, Direction face) {
        BlockPos pos = helper.absolutePos(new BlockPos(x, 1, 1));
        return new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
            new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
    }
    private static void wrench(GameTestHelper helper) {
        var target = machine(helper, 1, ModBlocks.MACERATOR.get());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WRENCH.get()));
        var result = WrenchActions.use(context(helper, player, 1, Direction.EAST), false);
        check(helper, result == InteractionResult.SUCCESS && target.getBlockState().getValue(MachineBlock.FACING) == Direction.EAST, "Wrench must rotate before opening GUI");
        helper.setBlock(new BlockPos(3, 1, 1), ModBlocks.COPPER_CABLE.get());
        var wire = helper.getBlockEntity(new BlockPos(3, 1, 1), CableBlockEntity.class);
        WrenchActions.use(context(helper, player, 3, Direction.EAST), false);
        check(helper, !wire.isSideEnabled(Direction.EAST), "Wrench must toggle a wire face");
        player.setShiftKeyDown(true);
        WrenchActions.use(context(helper, player, 3, Direction.EAST), false);
        check(helper, helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(3, 1, 1))).isAir(), "Wrench must dismantle cables without facing properties");
        helper.succeed();
    }
    private static void electricWrench(GameTestHelper helper) {
        var target = machine(helper, 1, ModBlocks.MACERATOR.get());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var stack = new ItemStack(ModItems.ELECTRIC_WRENCH.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        check(helper, WrenchActions.use(context(helper, player, 1, Direction.EAST), true) == InteractionResult.FAIL, "Empty electric wrench must not work");
        RechargeableBatteryItem.setEnergy(stack, 1000);
        WrenchActions.use(context(helper, player, 1, Direction.EAST), true);
        check(helper, RechargeableBatteryItem.getEnergy(stack) == 950, "Rotation must cost 50 EU");
        player.setShiftKeyDown(true);
        WrenchActions.use(context(helper, player, 1, Direction.EAST), true);
        check(helper, RechargeableBatteryItem.getEnergy(stack) == 450, "Dismantling must cost 500 EU");
        helper.succeed();
    }
    private static void furnace(GameTestHelper helper) {
        var furnace = (ElectricFurnaceBlockEntity) machine(helper, 1, ModBlocks.ELECTRIC_FURNACE.get());
        check(helper, RecipeHelper.findCookingRecipe(helper.getLevel(), new ItemStack(Items.RAW_IRON)).getType()
            == net.minecraft.world.item.crafting.RecipeType.BLASTING, "Electric furnace must use blast furnace recipes");
        check(helper, RecipeHelper.findCookingRecipe(helper.getLevel(), new ItemStack(Items.PORKCHOP)).getType()
            == net.minecraft.world.item.crafting.RecipeType.SMOKING, "Electric furnace must use smoker recipes");
        put(furnace.getItemHandler(), 0, new ItemStack(Items.COBBLESTONE));
        for (int i = 0; i < 210; i++) { energy(furnace).setEnergy(1000); furnace.tick(); }
        check(helper, furnace.getItemHandler().getResource(1).getItem() == Items.STONE, "Actual furnace tick must process vanilla smelting");
        for (String metal : new String[]{"tin", "zinc", "aluminium", "cobalt", "lead", "nickel", "silver", "uranium"}) {
            for (String grade : new String[]{"crushed", "purified"}) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(id(grade + "_" + metal + "_dust"));
                var recipe = RecipeHelper.findMachineRecipe(helper.getLevel(), "electric_furnace", new ItemStack(item));
                check(helper, recipe != null && recipe.getOutput().getCount() == (grade.equals("crushed") ? 2 : 3), "Wrong powder yield: " + grade + " " + metal);
            }
        }
        helper.succeed();
    }
    private static void workbench(GameTestHelper helper) {
        var bench = (IndustrialWorkbenchBlockEntity) machine(helper, 1, ModBlocks.INDUSTRIAL_WORKBENCH.get());
        put(bench.getItemHandler(), 8, new ItemStack(Items.OAK_LOG));
        for (int i = 0; i < 201; i++) bench.tick();
        check(helper, bench.getItemHandler().getResource(9).getItem() == Items.OAK_PLANKS && bench.getItemHandler().getAmountAsInt(9) == 4, "Shapeless recipe must match offset input");
        clear(bench.getItemHandler(), 9);
        var ingredients = new net.minecraft.world.item.Item[]{Items.MILK_BUCKET, Items.MILK_BUCKET, Items.MILK_BUCKET, Items.SUGAR, Items.EGG, Items.SUGAR, Items.WHEAT, Items.WHEAT, Items.WHEAT};
        for (int i = 0; i < ingredients.length; i++) put(bench.getItemHandler(), i, new ItemStack(ingredients[i]));
        for (int i = 0; i < 201; i++) bench.tick();
        check(helper, bench.getItemHandler().getResource(9).getItem() == Items.CAKE, "Shaped vanilla cake recipe must craft");
        for (int i = 0; i < 3; i++) check(helper, bench.getItemHandler().getResource(i).getItem() == Items.BUCKET, "Milk bucket remainder must be returned");
        helper.succeed();
    }
    private static void planter(GameTestHelper helper) {
        var planter = (PlanterBlockEntity) machine(helper, 1, ModBlocks.PLANTER.get());
        for (var item : new net.minecraft.world.item.Item[]{Items.WHEAT_SEEDS, Items.CARROT, Items.NETHER_WART, Items.COCOA_BEANS, Items.SWEET_BERRIES, Items.SUGAR_CANE, Items.CACTUS, Items.MELON_SEEDS}) {
            check(helper, CropRegistry.resolve(helper.getLevel(), planter.getBlockPos(), new ItemStack(item)).isPresent(), "Expected native crop support: " + item);
        }
        for (var item : new net.minecraft.world.item.Item[]{Items.POPPY, Items.BROWN_MUSHROOM, Items.LILY_PAD, Items.VINE, Items.SUNFLOWER, Items.SEA_PICKLE, Items.CRIMSON_FUNGUS}) {
            check(helper, CropRegistry.resolve(helper.getLevel(), planter.getBlockPos(), new ItemStack(item)).isPresent(), "Expected plant propagation support: " + item);
        }
        check(helper, CropRegistry.resolve(helper.getLevel(), planter.getBlockPos(), new ItemStack(Items.OAK_SAPLING)).orElseThrow().result().getItem() == Items.OAK_LOG, "Saplings must grow into their logs");
        check(helper, CropRegistry.resolve(helper.getLevel(), planter.getBlockPos(), new ItemStack(Items.MANGROVE_PROPAGULE)).orElseThrow().result().getItem() == Items.MANGROVE_LOG, "Propagules must grow into mangrove logs");
        for (var item : new net.minecraft.world.item.Item[]{Items.GRASS_BLOCK, Items.MOSS_BLOCK, Items.NETHERRACK})
            check(helper, CropRegistry.resolve(helper.getLevel(), planter.getBlockPos(), new ItemStack(item)).isEmpty(), "Soil blocks must not be cultivated: " + item);
        check(helper, CropRegistry.resolve(helper.getLevel(), planter.getBlockPos(), new ItemStack(Items.COBBLESTONE)).isEmpty(), "Non-crops must not be cultivated");
        CropRegistry.register(Items.AMETHYST_SHARD, new ItemStack(Items.DIRT), 20, 2);
        put(planter.getItemHandler(), 0, new ItemStack(Items.AMETHYST_SHARD));
        for (int i = 0; i < 22; i++) { energy(planter).setEnergy(1000); planter.tick(); }
        check(helper, planter.getItemHandler().getResource(2).getItem() == Items.DIRT, "Registered extension must run through planter tick");
        check(helper, planter.getItemHandler().getAmountAsInt(0) == 1, "Automatic cultivation must keep its planted seed");
        helper.succeed();
    }

    /** Real server ticks only: proves the level-tick hook drives transfer, not a direct manager call. */
    private static void liveAdjacent(GameTestHelper helper) {
        var source = machine(helper, 1, ModBlocks.GENERATOR.get());
        var target = machine(helper, 2, ModBlocks.MACERATOR.get());
        energy(source).setEnergy(4000);
        helper.succeedWhen(() -> check(helper, energy(target).getEnergyStored() > 0,
            "Adjacent machine received no power from real server ticks"));
    }
    private static void liveCable(GameTestHelper helper) {
        var source = machine(helper, 1, ModBlocks.GENERATOR.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.COPPER_CABLE.get());
        helper.setBlock(new BlockPos(3, 1, 1), ModBlocks.COPPER_CABLE.get());
        var target = machine(helper, 4, ModBlocks.MACERATOR.get());
        energy(source).setEnergy(4000);
        helper.succeedWhen(() -> check(helper, energy(target).getEnergyStored() > 0,
            "Machine behind cables received no power from real server ticks"));
    }

    /** High-tier sources must still feed low-tier machines (there are no transformers). */
    private static void liveHighTier(GameTestHelper helper) {
        var storage = machine(helper, 1, ModBlocks.MFSU.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.IRON_CABLE.get());
        var target = machine(helper, 3, ModBlocks.MACERATOR.get());
        var solar = machine(helper, 5, ModBlocks.SINGULARY_SOLAR_GENERATOR.get());
        var direct = machine(helper, 6, ModBlocks.COMPRESSOR.get());
        energy(storage).setEnergy(100000);
        energy(solar).setEnergy(100000);
        helper.succeedWhen(() -> check(helper, energy(target).getEnergyStored() > 0 && energy(direct).getEnergyStored() > 0,
            "LV machines received no power from high-tier storage/generators"));
    }
}
