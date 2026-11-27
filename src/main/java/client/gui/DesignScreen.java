package client.gui;

import machine.process.DesignMenu;
import machine.process.ProcessCards;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DesignScreen extends AbstractContainerScreen<DesignMenu> {
    public DesignScreen(DesignMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 182);
        inventoryLabelY = 88;
    }
    @Override protected void init() {
        super.init();
        button(Component.literal("›"), 150, 17, 18, 0);
        button(Component.translatable("gui.iuce.design.write"), 8, 62, 46, 1);
        button(Component.translatable("gui.iuce.design.copy"), 58, 62, 46, 2);
    }
    private void button(Component label, int x, int y, int width, int action) {
        addRenderableWidget(Button.builder(label, b -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action))
            .bounds(leftPos+x, topPos+y, width, 16).build());
    }
    @Override public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        g.fill(leftPos, topPos, leftPos+imageWidth, topPos+imageHeight, 0xffbcc1c2);
        for (var slot : menu.slots) {
            g.fill(leftPos+slot.x-1, topPos+slot.y-1, leftPos+slot.x+17, topPos+slot.y+17, 0xff555d62);
        }
        var recipe = menu.selected();
        if (recipe != null) {
            g.text(font, ProcessCards.label(recipe.card().orElseThrow()), leftPos+8, topPos+20, 0xff303638, false);
            int y = topPos+36;
            for (var output : recipe.results()) {
                var stack = output.create();
                g.text(font, stack.getCount()+" × "+stack.getHoverName().getString(), leftPos+58, y, 0xff303638, false);
                y += 10;
            }
            for (var output : recipe.fluidResults()) {
                g.text(font, output.create().getHoverName(), leftPos+58, y, 0xff303638, false);
                y += 10;
                g.text(font, output.amount()+" mB", leftPos+58, y, 0xff303638, false);
                y += 10;
            }
            g.text(font, recipe.energyPerTick()+" EU/t", leftPos+114, topPos+66, 0xff303638, false);
        }
        super.extractContents(g, mouseX, mouseY, partial);
    }
}
