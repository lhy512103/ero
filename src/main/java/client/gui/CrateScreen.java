package client.gui;

import block.CrateMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The crate: three rows of slots on the plain panel, like a vanilla chest. */
public class CrateScreen extends MachineScreen<CrateMenu> {
    public CrateScreen(CrateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166, null, 84);
    }

    @Override
    protected void init() {
        super.init();
        titleLabelX = 8;
    }

    @Override
    protected void machine(GuiGraphicsExtractor g, int mouseX, int mouseY) {
    }
}
