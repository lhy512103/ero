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
        int next = (part + 1) * 280;
        if (part < 5 && screen.maxScroll() > 0 && next - 280 < screen.maxScroll()) {
            part++;
            screen.scrollTo(Math.min(next, screen.maxScroll()));
            settle = 4;
        } else {
            index++;
            settle = 0;
        }
    }

    private static void industrialShots(Minecraft minecraft) {
        var server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) { minecraft.stop(); return; }
        settle++;
        if (settle == 1 || settle == 150) {
            boolean mill = settle == 150;
            var uuid = minecraft.player.getUUID();
            if (!mill) industrialOrigin = minecraft.player.blockPosition();
            var playerPos = industrialOrigin;
            minecraft.setScreen(null);
            minecraft.options.hideGui = true;
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid);
                if (player == null) return;
                var level = server.overworld();
                int height = Math.min(level.getMaxY() - 32, Math.max(playerPos.getY() + 28, 120));
                var origin = new BlockPos(playerPos.getX() + (mill ? 52 : -52), height, playerPos.getZ() + 8);
                for (int x = -10; x <= 10; x++) {
                    for (int z = -5; z <= 20; z++) {
                        BlockPos floor = origin.offset(x, -2, z);
                        level.getChunkAt(floor);
                        level.setBlockAndUpdate(floor, Blocks.SMOOTH_STONE.defaultBlockState());
                    }
                }
                var controller = (mill ? ModBlocks.MILL_CONTROLLER : ModBlocks.PLANT_CONTROLLER).get();
                var state = controller.defaultBlockState().setValue(PlantControllerBlock.LEGACY, false);
                if (mill) state = state.setValue(PlantControllerBlock.LENGTH, 17);
                level.setBlockAndUpdate(origin, state);
                for (var cell : PlantStructure.cells(origin, Direction.NORTH, state)) {
                    var part = cell.part().sample(Direction.NORTH);
                    if (part.is(ModBlocks.PLANT_CASING.get())) part = ModBlocks.PLANT_CASING.get().joinedState(level, cell.pos());
                    level.setBlockAndUpdate(cell.pos(), part);
                }
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set day");
                player.setGameMode(GameType.SPECTATOR);
                player.teleportTo(level, origin.getX() + 15, height + (mill ? 9 : 10), origin.getZ() - 10, Set.of(), 44, 14, true);
            });
        } else if (settle == 125 || settle == 274) {
            var name = settle == 125 ? "iuce_ore_plant.png" : "iuce_rolling_mill.png";
            Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(), 1, message -> { });
        } else if (settle > 285) {
            minecraft.options.hideGui = false;
            minecraft.stop();
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
}
