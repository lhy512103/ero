package regression;

import client.guide.GuideBook;
import client.guide.GuideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.List;

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
        } else if (settle > 70) {
            minecraft.stop();
        }
    }
}
