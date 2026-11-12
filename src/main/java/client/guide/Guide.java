package client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import item.GuideItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import javax.annotation.Nullable;

/** Entry points and event wiring for the built-in guide. Everything here is client-only. */
public final class Guide {
    private Guide() {}

    public static final KeyMapping.Category KEY_CATEGORY =
        new KeyMapping.Category(Identifier.fromNamespaceAndPath("iuce", "main"));
    @Nullable private static KeyMapping openKey;

    public static void init(IEventBus modBus) {
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.registerCategory(KEY_CATEGORY);
            openKey = new KeyMapping("key.iuce.open_guide", InputConstants.Type.KEYSYM, InputConstants.KEY_G, KEY_CATEGORY);
            event.register(openKey);
        });
        modBus.addListener((AddClientReloadListenersEvent event) -> event.addListener(
            Identifier.fromNamespaceAndPath("iuce", "guide"),
            (ResourceManagerReloadListener) manager -> GuideBook.invalidate()));

        NeoForge.EVENT_BUS.addListener((RecipesReceivedEvent event) -> GuideRecipes.onRecipesReceived(event));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> GuideRecipes.clear());
        NeoForge.EVENT_BUS.addListener(Guide::onClientTick);
        NeoForge.EVENT_BUS.addListener(Guide::onScreenKey);
        NeoForge.EVENT_BUS.addListener(Guide::onTooltip);

        GuideItem.opener = Guide::open;
    }

    /** Opens the guide at {@code pageId}, or at the last viewed page when null. */
    public static void open(@Nullable String pageId) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new GuideScreen(minecraft.screen, pageId));
    }

    /** Opens the page documenting {@code stack}; returns false when there is none. */
    public static boolean openFor(ItemStack stack) {
        if (stack.isEmpty()) return false;
        GuideBook.Page page = GuideBook.get().pageFor(stack.getItem());
        if (page == null) return false;
        open(page.id);
        return true;
    }

    static boolean isOpenKey(KeyEvent event) {
        return openKey != null && openKey.matches(event);
    }

    /** Hands an item over to JEI's recipe view when JEI is installed; otherwise does nothing. */
    static void showExternalRecipes(ItemStack stack, boolean uses) {
        if (ModList.get().isLoaded("jei")) compat.jei.EroJeiPlugin.showRecipes(stack, uses);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (openKey == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        while (openKey.consumeClick()) {
            if (minecraft.screen == null && minecraft.player != null) open(null);
        }
    }

    /** In any inventory screen: hover an item and press the guide key to jump to its page. */
    private static void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen) || !isOpenKey(event.getKeyEvent())) return;
        if (screen.getFocused() instanceof EditBox box && box.isFocused()) return;
        if (ModList.get().isLoaded("jei") && compat.jei.EroJeiPlugin.searchHasFocus()) return;
        Slot slot = screen.getSlotUnderMouse();
        if (slot != null && slot.hasItem() && openFor(slot.getItem())) event.setCanceled(true);
    }

    private static void onTooltip(ItemTooltipEvent event) {
        if (openKey == null || event.getEntity() == null) return;
        ItemStack stack = event.getItemStack();
        if (!"iuce".equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) return;
        if (Minecraft.getInstance().screen instanceof GuideScreen) return;
        if (GuideBook.get().pageFor(stack.getItem()) == null) return;
        event.getToolTip().add(Component.translatable("guide.iuce.tooltip_hint", openKey.getTranslatedKeyMessage())
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
