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
        TESTS.put("transformer_storage_rules", RegressionTests::transformerStorage);
        TESTS.put("transformer_wet_shock", RegressionTests::transformerShock);
        TESTS.put("upgrade_install_and_stacking", RegressionTests::upgrades);
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

    /** High-tier sources need a transformer to reach low-tier machines; direct connections are refused. */
    private static void liveHighTier(GameTestHelper helper) {
        var direct = machine(helper, 0, ModBlocks.COMPRESSOR.get());
        var storage = machine(helper, 1, ModBlocks.MFSU.get());
        helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.IRON_CABLE.get());
        var transformer = (machine.transformer.TransformerBlockEntity) machine(helper, 3, ModBlocks.TRANSFORMER.get());
        var target = machine(helper, 4, ModBlocks.MACERATOR.get());
        energy(storage).setEnergy(100000);
        helper.runAfterDelay(40, () -> {
            check(helper, energy(target).getEnergyStored() > 0, "Transformer must step EV storage down to an LV machine");
            check(helper, energy(direct).getEnergyStored() == 0, "EV storage must not feed an LV machine directly");
            check(helper, transformer.getHazardVoltage() == energy.EUVoltage.EV, "Transformer must remember its input voltage");
            check(helper, helper.getBlockState(new BlockPos(3, 1, 1)).getValue(MachineBlock.RUNNING), "Working transformer must show its active state");
            helper.succeed();
        });
    }
    /** Transformer -> same-tier storage is allowed, but storage can never charge itself back through it. */
    private static void transformerStorage(GameTestHelper helper) {
        var source = machine(helper, 1, ModBlocks.MFE.get());
        var transformer = (machine.transformer.TransformerBlockEntity) machine(helper, 2, ModBlocks.TRANSFORMER.get());
        var batbox = machine(helper, 3, ModBlocks.BATBOX.get());
        var cesu = machine(helper, 5, ModBlocks.CESU.get());
        var lower = (machine.transformer.TransformerBlockEntity) machine(helper, 6, ModBlocks.TRANSFORMER.get());
        var sink = machine(helper, 7, ModBlocks.BATBOX.get());
        lower.setOutputVoltage(energy.EUVoltage.MV);
        energy(source).setEnergy(50000);
        energy(cesu).setEnergy(50000);
        helper.runAfterDelay(40, () -> {
            check(helper, energy(batbox).getEnergyStored() > 0, "LV transformer must charge an LV storage unit");
            check(helper, lower.getEnergyStorage().getEnergyStored() == 0, "MV storage must not push into an MV-output transformer");
            check(helper, energy(sink).getEnergyStored() == 0, "MV transformer output must not charge an LV storage unit");
            long total = energy(source).getEnergyStored() + transformer.getEnergyStorage().getEnergyStored() + energy(batbox).getEnergyStored();
            check(helper, total == 50000, "Transforming must conserve energy exactly, got " + total);
            helper.succeed();
        });
    }
    /** Wet creatures near a live transformer are shocked; damage and stun scale with voltage. */
    private static void transformerShock(GameTestHelper helper) {
        var transformer = (machine.transformer.TransformerBlockEntity) machine(helper, 1, ModBlocks.TRANSFORMER.get());
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            helper.setBlock(new BlockPos(6 + dx, 1, 2 + dz), net.minecraft.world.level.block.Blocks.STONE);
            if (dx != 0 || dz != 0) helper.setBlock(new BlockPos(6 + dx, 2, 2 + dz), net.minecraft.world.level.block.Blocks.STONE);
        }
        helper.setBlock(new BlockPos(6, 2, 2), net.minecraft.world.level.block.Blocks.WATER);
        var wet = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, new BlockPos(6, 2, 2));
        var dry = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, new BlockPos(9, 2, 2));
        helper.setBlock(new BlockPos(9, 1, 2), net.minecraft.world.level.block.Blocks.STONE);
        var T = machine.transformer.TransformerBlockEntity.class;
        check(helper, machine.transformer.TransformerBlockEntity.shockDamage(energy.EUVoltage.EV) > machine.transformer.TransformerBlockEntity.shockDamage(energy.EUVoltage.LV)
            && machine.transformer.TransformerBlockEntity.stunTicks(energy.EUVoltage.EV) > machine.transformer.TransformerBlockEntity.stunTicks(energy.EUVoltage.LV),
            "Shock damage and stun must scale with voltage");
        helper.runAfterDelay(50, () -> {
            check(helper, wet.getHealth() == wet.getMaxHealth(), "An unpowered transformer must be harmless");
            transformer.getEnergyStorage().receiveEnergy(100, false);
        });
        helper.runAfterDelay(99, () -> {
            check(helper, wet.getHealth() < wet.getMaxHealth(), "A wet creature near a live transformer must be shocked");
            check(helper, wet.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "A shocked creature must be stunned");
            check(helper, dry.getHealth() == dry.getMaxHealth(), "A dry creature must not be shocked");
            helper.succeed();
        });
    }
    /** Every upgrade type stacks, fits the upgrade slots, and installs by right-clicking a machine. */
    private static void upgrades(GameTestHelper helper) {
        var target = machine(helper, 1, ModBlocks.MACERATOR.get());
        machine(helper, 3, ModBlocks.COMPRESSOR.get());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        var menu = new machine.macerator.menu.MaceratorMenu(1, player.getInventory(), (machine.macerator.MaceratorBlockEntity) target);
        var all = new net.minecraft.world.item.Item[]{ModItems.OVERCLOCKER_UPGRADE.get(), ModItems.OVERCLOCKER_UPGRADE_1.get(),
            ModItems.OVERCLOCKER_UPGRADE_2.get(), ModItems.EJECTOR_UPGRADE.get(), ModItems.PULLING_UPGRADE.get(),
            ModItems.FLUID_EJECTOR_UPGRADE.get(), ModItems.FLUID_PULLING_UPGRADE.get()};
        int firstUpgradeSlot = -1;
        for (int i = 0; i < menu.slots.size(); i++) if (menu.slots.get(i) instanceof UpgradeSlot) { firstUpgradeSlot = i; break; }
        check(helper, firstUpgradeSlot >= 0, "Macerator menu must expose upgrade slots");
        for (var item : all) {
            var stack = new ItemStack(item, 16);
            check(helper, stack.getMaxStackSize() == 16, "Upgrades must stack: " + item);
            check(helper, menu.slots.get(firstUpgradeSlot).mayPlace(stack), "Upgrade slot must accept " + item);
            check(helper, menu.slots.get(firstUpgradeSlot).getMaxStackSize(stack) == 1, "An upgrade slot holds one upgrade");
        }
        check(helper, !menu.slots.get(firstUpgradeSlot).mayPlace(new ItemStack(Items.DIRT)), "Upgrade slots must reject other items");

        // right-click installs one from a stack, before the GUI opens
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.OVERCLOCKER_UPGRADE.get(), 3));
        var result = player.getMainHandItem().onItemUseFirst(context(helper, player, 1, Direction.NORTH));
        int start = target.getUpgradeSlotStart();
        check(helper, result == InteractionResult.SUCCESS && target.getItemHandler().getResource(start).getItem() == ModItems.OVERCLOCKER_UPGRADE.get()
            && target.getItemHandler().getAmountAsInt(start) == 1, "Right-clicking a machine must install one overclocker");
        check(helper, player.getMainHandItem().getCount() == 2, "Installing must consume exactly one upgrade");

        // an unbound ejector is refused; a bound one installs
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.EJECTOR_UPGRADE.get()));
        check(helper, player.getMainHandItem().onItemUseFirst(context(helper, player, 1, Direction.NORTH)) == InteractionResult.FAIL
            && target.getItemHandler().getAmountAsInt(start + 1) == 0, "An unbound ejector upgrade must not install");
        player.setShiftKeyDown(true);
        check(helper, player.getMainHandItem().onItemUseFirst(context(helper, player, 3, Direction.NORTH)) == InteractionResult.PASS,
            "Sneaking must fall through to binding");
        player.getMainHandItem().useOn(context(helper, player, 3, Direction.NORTH));
        check(helper, UpgradeItem.getBoundPos(player.getMainHandItem()) != null, "Sneak + right-click must bind the target machine");
        player.setShiftKeyDown(false);
        check(helper, player.getMainHandItem().onItemUseFirst(context(helper, player, 3, Direction.NORTH)) == InteractionResult.FAIL,
            "An upgrade must not install into its own target");
        check(helper, player.getMainHandItem().onItemUseFirst(context(helper, player, 1, Direction.NORTH)) == InteractionResult.SUCCESS
            && target.getItemHandler().getResource(start + 1).getItem() == ModItems.EJECTOR_UPGRADE.get(), "A bound ejector upgrade must install");
        check(helper, UpgradeItem.getBoundPos(target.getItemHandler().getResource(start + 1).toStack(1)) != null, "The installed upgrade must keep its binding");

        // fill the remaining slots, then a further install is refused
        for (var item : new net.minecraft.world.item.Item[]{ModItems.FLUID_PULLING_UPGRADE.get(), ModItems.OVERCLOCKER_UPGRADE_2.get()}) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
            check(helper, player.getMainHandItem().onItemUseFirst(context(helper, player, 1, Direction.NORTH)) == InteractionResult.SUCCESS, "Must install " + item);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.OVERCLOCKER_UPGRADE.get()));
        check(helper, player.getMainHandItem().onItemUseFirst(context(helper, player, 1, Direction.NORTH)) == InteractionResult.FAIL
            && player.getMainHandItem().getCount() == 1, "A machine with full upgrade slots must refuse more");
        check(helper, OverclockerUpgradeItem.countOverclockers(target) == 2, "Installed overclockers must be counted");
        helper.succeed();
    }
}
