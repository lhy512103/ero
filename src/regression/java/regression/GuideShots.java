package regression;

import block.ModBlocks;
import block.PlantControllerBlock;
import client.guide.GuideBook;
import client.guide.GuideScreen;
import multiblock.PlantStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Development tool: opens every guide page and saves screenshots to {@code screenshots/guide_*.png},
 * then quits. Run with {@code gradlew runClient -PsmokeTest -PregressionTests -PguideShots[=page,page]}.
 * Recipes only appear when a world is loaded (add {@code -PguideWorld=<save folder>}).
 */
public final class GuideShots {
    private GuideShots() {}

    private static final List<String> pages = new ArrayList<>();
    private static int ticks, settle = -1, index, part;
    private static boolean started;
    private static BlockPos industrialOrigin;

    public static void register() {
        NeoForge.EVENT_BUS.addListener(GuideShots::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.pauseOnLostFocus = false;
        ticks++;
        if (!started) {
            boolean wantWorld = Boolean.getBoolean("iuce.guideShots.world");
            boolean inWorld = minecraft.player != null && minecraft.level != null;
            boolean atTitle = minecraft.screen instanceof TitleScreen;
            if (wantWorld ? !(inWorld || (atTitle && ticks > 2400)) : !(atTitle || inWorld)) return;
            if (settle < 0) settle = ticks + (inWorld ? 100 : 40);
            if (ticks < settle) return;
            started = true;
            String filter = System.getProperty("iuce.guideShots", "");
            if (filter.isBlank() || filter.equals("true")) {
                for (GuideBook.Page page : GuideBook.get().ordered()) pages.add(page.id);
            } else {
                for (String id : filter.split(",")) pages.add(id.trim());
            }
            System.out.println("[GuideShots] capturing " + pages.size() + " pages, world=" + inWorld);
            settle = 0;
        }
        if ("hand".equals(System.getProperty("iuce.guideShots"))) {
            handShots(minecraft);
            return;
        }
        if ("panels".equals(System.getProperty("iuce.guideShots"))) {
            panelShots(minecraft);
            return;
        }
        if ("industrial".equals(System.getProperty("iuce.guideShots"))) {
            industrialShots(minecraft);
            return;
        }
        if (index >= pages.size()) {
            if (++settle > 20) minecraft.stop();
            return;
        }
        if (!(minecraft.screen instanceof GuideScreen screen) || settle == 0) {
            minecraft.setScreen(new GuideScreen(null, pages.get(index)));
            part = 0;
            settle = 1;
            return;
        }
        if (++settle < 8) return;
        String name = "guide_" + pages.get(index).replace('/', '-') + (part == 0 ? "" : "_" + part) + ".png";
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(), 1, message -> { });
        int step = Integer.getInteger("iuce.guideShots.step", 280);
        int next = (part + 1) * step;
        if (part < 5 && screen.maxScroll() > 0 && next - step < screen.maxScroll()) {
            part++;
            screen.scrollTo(Math.min(next, screen.maxScroll()));
            settle = 4;
        } else {
            index++;
            settle = 0;
        }
    }

    /** Machines built side by side for {@code -PguideShots=industrial}: controller, length, screenshot, camera offset. */
    private record Shot(java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> controller, int length,
                        String file, int dx, int camX, int camY, int camZ, float pitch, float yaw) {
        Shot(java.util.function.Supplier<? extends net.minecraft.world.level.block.Block> controller, int length, String file, int dx, int camX, int camY, int camZ, float pitch) {
            this(controller, length, file, dx, camX, camY, camZ, pitch, camX > 0 ? 44 : -44);
        }
    }
    private static final List<Shot> SHOTS = List.of(
        new Shot(ModBlocks.PLANT_CONTROLLER, 0, "iuce_ore_plant.png", -52, 15, 10, -10, 14),
        new Shot(ModBlocks.MILL_CONTROLLER, 17, "iuce_rolling_mill.png", 52, 15, 9, -10, 14),
        new Shot(ModBlocks.CARBIDE_FURNACE_PLANT, 0, "iuce_carbide_furnace_plant.png", 0, 21, 14, -15, 12, 45),
        new Shot(ModBlocks.CARBIDE_FURNACE_PLANT, 0, "iuce_carbide_furnace_plant_back.png", 0, -20, 8, 26, 2, -135),
        new Shot(ModBlocks.CARBIDE_FURNACE_PLANT, 0, "iuce_carbide_furnace_plant_front.png", 0, 5, 3, -6, 12, 32),
        new Shot(ModBlocks.CARBIDE_FURNACE_PLANT, 0, "iuce_carbide_furnace_plant_top.png", 0, 12, 14, -4, 30, 60),
        new Shot(ModBlocks.BIO_ETHYLENE_PLANT, 0, "iuce_bio_ethylene_plant.png", -104, 22, 16, -14, 24, 45),
        new Shot(ModBlocks.WASTE_HEAT_POWER_PLANT, 0, "iuce_waste_heat_power_plant.png", 104, 22, 16, -14, 24, 45),
        new Shot(ModBlocks.COKE_OVEN_BATTERY, 21, "iuce_coke_oven_battery.png", -156, -22, 18, -14, 24, -45),
        new Shot(ModBlocks.TWIN_SCREW_EXTRUDER, 21, "iuce_twin_screw_extruder.png", 156, 16, 10, -10, 20, 45));

    private static void industrialShots(Minecraft minecraft) {
        var server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) { minecraft.stop(); return; }
        settle++;
        int index = (settle - 1) / 150, step = (settle - 1) % 150;
        if (index >= SHOTS.size()) {
            minecraft.options.hideGui = false;
            minecraft.stop();
            return;
        }
        Shot shot = SHOTS.get(index);
        if (step == 0) {
            var uuid = minecraft.player.getUUID();
            if (index == 0) industrialOrigin = minecraft.player.blockPosition();
            var playerPos = industrialOrigin;
            minecraft.setScreen(null);
            minecraft.options.hideGui = true;
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                if (player == null) return;
                var level = server.overworld();
                int height = Math.min(level.getMaxY() - 32, Math.max(playerPos.getY() + 28, 120));
                var origin = new BlockPos(playerPos.getX() + shot.dx(), height, playerPos.getZ() + 8);
                for (int x = -10; x <= 10; x++) {
                    for (int z = -12; z <= 40; z++) {
                        BlockPos floor = origin.offset(x, -2, z);
                        level.getChunkAt(floor);
                        level.setBlockAndUpdate(floor, Blocks.SMOOTH_STONE.defaultBlockState());
                        level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.SMOOTH_STONE.defaultBlockState());
                        for (int y = 0; y <= 22; y++) {
                            BlockPos air = origin.offset(x, y, z);
                            if (!level.getBlockState(air).isAir()) level.setBlockAndUpdate(air, Blocks.AIR.defaultBlockState());
                        }
                    }
                }
                var state = shot.controller().get().defaultBlockState().setValue(PlantControllerBlock.LEGACY, false);
                if (shot.length() > 0) state = state.setValue(PlantControllerBlock.LENGTH, shot.length());
                for (var cell : PlantStructure.cells(origin, Direction.NORTH, state)) {
                    var part = cell.part().sample(Direction.NORTH);
                    if (part.is(ModBlocks.PLANT_CASING.get())) part = ModBlocks.PLANT_CASING.get().joinedState(level, cell.pos());
                    level.setBlockAndUpdate(cell.pos(), part);
                }
                for (var cell : PlantStructure.cells(origin, Direction.NORTH, state)) {
                    if (cell.part().canFillWithCasing() && level.getBlockState(cell.pos()).is(ModBlocks.PLANT_CASING.get())) {
                        level.setBlockAndUpdate(cell.pos(), ModBlocks.PLANT_CASING.get().joinedState(level, cell.pos()));
                    }
                }
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set day");
                player.setGameMode(GameType.SPECTATOR);
                player.teleportTo(level, origin.getX() + shot.camX(), height + shot.camY(), origin.getZ() + shot.camZ(), Set.of(),
                    shot.yaw(), shot.pitch(), true);
            });
        } else if (step == 40) {
            Screenshot.grab(minecraft.gameDirectory, "unformed_" + shot.file(), minecraft.getMainRenderTarget(), 1, message -> { });
        } else if (step == 45) {
            var playerPos = industrialOrigin;
            server.execute(() -> {
                var level = server.overworld();
                int height = Math.min(level.getMaxY() - 32, Math.max(playerPos.getY() + 28, 120));
                var origin = new BlockPos(playerPos.getX() + shot.dx(), height, playerPos.getZ() + 8);
                var state = shot.controller().get().defaultBlockState().setValue(PlantControllerBlock.LEGACY, false);
                if (shot.length() > 0) state = state.setValue(PlantControllerBlock.LENGTH, shot.length());
                level.setBlockAndUpdate(origin, state);
                if (level.getBlockEntity(origin) instanceof blockentity.PlantControllerBlockEntity controller) {
                    controller.assembleCreative();
                }
            });
        } else if (step == 124) {
            Screenshot.grab(minecraft.gameDirectory, shot.file(), minecraft.getMainRenderTarget(), 1, message -> { });
        }
    }

    /** {@code -PguideShots=hand}: puts the guide book in the player's hand and captures first/third person views. */
    private static void handShots(Minecraft minecraft) {
        settle++;
        var server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) { minecraft.stop(); return; }
        if (settle == 1) {
            var uuid = minecraft.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                if (player == null) return;
                var book = new net.minecraft.world.item.ItemStack(item.ModItems.GUIDE.get());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, book);
                player.getInventory().setItem(9, book.copy());
                player.getInventory().setItem(10, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK));
                player.getInventory().setItem(11, new net.minecraft.world.item.ItemStack(item.ModItems.OVERCLOCKER_UPGRADE.get(), 16));
                player.drop(book.copy(), false);
            });
            minecraft.setScreen(null);
            minecraft.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        } else if (settle == 30) {
            Screenshot.grab(minecraft.gameDirectory, "guide_hand_first.png", minecraft.getMainRenderTarget(), 1, message -> { });
        } else if (settle == 34) {
            minecraft.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
        } else if (settle == 50) {
            Screenshot.grab(minecraft.gameDirectory, "guide_hand_third.png", minecraft.getMainRenderTarget(), 1, message -> { });
        } else if (settle == 54) {
            minecraft.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            minecraft.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(minecraft.player));
        } else if (settle == 70) {
            Screenshot.grab(minecraft.gameDirectory, "guide_inventory.png", minecraft.getMainRenderTarget(), 1, message -> { });
        } else if (settle > 76) {
            minecraft.stop();
        }
    }

    /** {@code -PguideShots=panels}: places each multiblock terminal next to the player and captures its panel. */
    private static void panelShots(Minecraft minecraft) {
        var server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) { minecraft.stop(); return; }
        var blocks = List.of(ModBlocks.PLANT_CONTROLLER, ModBlocks.MILL_CONTROLLER, ModBlocks.ITEM_INPUT_HATCH,
            ModBlocks.ITEM_OUTPUT_HATCH, ModBlocks.ENERGY_HATCH_HV, ModBlocks.FACTORY_HUB);
        settle++;
        int step = (settle - 1) / 40, phase = (settle - 1) % 40;
        if (step >= blocks.size()) { minecraft.stop(); return; }
        if (phase == 0) {
            var uuid = minecraft.player.getUUID();
            var block = blocks.get(step).get();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                if (player == null) return;
                var level = player.level();
                BlockPos pos = player.blockPosition().offset(2 + step, 0, 2);
                level.setBlockAndUpdate(pos, block.defaultBlockState());
                var target = level.getBlockEntity(pos);
                if (target instanceof blockentity.HatchBlockEntity hatch) {
                    hatch.getInventory().set(0, net.neoforged.neoforge.transfer.item.ItemResource.of(net.minecraft.world.item.Items.RAW_IRON), 48);
                    hatch.getInventory().set(4, net.neoforged.neoforge.transfer.item.ItemResource.of(net.minecraft.world.item.Items.COPPER_INGOT), 12);
                }
                if (target instanceof blockentity.EnergyHatchBlockEntity hatch) hatch.storage().setEnergy(52_000);
                if (target instanceof blockentity.FactoryHubBlockEntity hub) {
                    // Some products stocked, one short of a minute's worth, to show both gauge colours.
                    var items = factory.FactoryRating.candidates(0);
                    int[] amounts = {32, 32, 20, 9, 0, 32};
                    for (int i = 0; i < items.size() && i < amounts.length; i++) {
                        if (amounts[i] == 0) continue;
                        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
                            hub.getIntake().insert(i, net.neoforged.neoforge.transfer.item.ItemResource.of(items.get(i)), amounts[i], transaction);
                            transaction.commit();
                        }
                    }
                    player.openMenu(hub, pos);
                } else if (target != null) multiblock.MultiblockMenu.open(player, target);
            });
        } else if (phase == 30) {
            String[] names = {"plant", "mill", "input", "output", "energy", "hub"};
            Screenshot.grab(minecraft.gameDirectory, "panel_" + names[step] + ".png", minecraft.getMainRenderTarget(), 1, message -> { });
        }
    }
}
