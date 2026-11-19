package machine.metal_former.screen;

import block.ModBlockItems;
import item.ModItems;
import machine.metal_former.MetalFormerBlockEntity;
import machine.metal_former.menu.MetalFormerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * 金属成型机界面：布局与压缩机一致，箭头右侧多一个模具槽。
 */
public class MetalFormerScreen extends AbstractContainerScreen<MetalFormerMenu> {

    private static final Identifier TEXTURE = Identifier.parse("iuce:textures/gui/machine/macerator.png");
    private static final Identifier ARROW_ON = Identifier.parse("iuce:textures/gui/com/arrow_on.png");
    private static final Identifier POWER_ON = Identifier.parse("iuce:textures/gui/com/power_on.png");

    /** 模具槽的边框（相对 GUI 左上角），18×18 */
    private static final int MOLD_X = MetalFormerMenu.MOLD_X - 1;
    private static final int MOLD_Y = MetalFormerMenu.MOLD_Y - 1;
    private static final int MOLD_SIZE = 18;

    private static final int BTN_BORDER = 0xFF5F6D78;
    private static final int BTN_FACE = 0xFFD3E1DF;
    private static final int BTN_FACE_HOVER = 0xFFEDF6F6;
    private static final int BTN_SHADE = 0xFF88959D;

    public MetalFormerScreen(MetalFormerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            TEXTURE, this.leftPos, this.topPos, 0, 0,
            this.imageWidth, this.imageHeight,
            256, 256);
        drawMoldSlot(graphics);

        super.extractContents(graphics, mouseX, mouseY, a);

        drawProgressArrow(graphics);
        drawPowerOn(graphics);
        drawMoldHint(graphics, mouseX, mouseY);
    }

    /** Frame for the mold slot; drawn before the slots so the mold itself stays on top. */
    private void drawMoldSlot(GuiGraphicsExtractor graphics) {
        int x = this.leftPos + MOLD_X;
        int y = this.topPos + MOLD_Y;
        graphics.fill(x, y, x + MOLD_SIZE, y + MOLD_SIZE, BTN_BORDER);
        graphics.fill(x + 1, y + 1, x + MOLD_SIZE - 1, y + MOLD_SIZE - 1, menu.hasMold() ? BTN_FACE : BTN_SHADE);
    }

    private void drawMoldHint(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int rx = mouseX - this.leftPos, ry = mouseY - this.topPos;
        if (!menu.hasMold() && rx >= MOLD_X && rx < MOLD_X + MOLD_SIZE && ry >= MOLD_Y && ry < MOLD_Y + MOLD_SIZE) {
            graphics.setTooltipForNextFrame(Component.translatable("tooltip.iuce.metal_former.no_mold"), mouseX, mouseY);
        }
    }

    private void drawProgressArrow(GuiGraphicsExtractor graphics) {
        int progress = menu.getProgress(0);
        int maxProgress = menu.getMaxProgress(0);
        if (maxProgress <= 0 || progress <= 0) return;
        int filled = progress * 22 / maxProgress;
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
            ARROW_ON, this.leftPos + 79, this.topPos + 36, 0, 0, 16, filled, 16, 22);
    }

    private void drawPowerOn(GuiGraphicsExtractor graphics) {
        if (menu.getEnergy() > 0) {
            graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                POWER_ON, this.leftPos + 7, this.topPos + 71, 0, 0,
                12, 15, 12, 15);
        }
    }
}
