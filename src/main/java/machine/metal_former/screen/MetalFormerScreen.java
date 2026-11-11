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
 * 金属成型机界面：布局与压缩机一致，箭头右侧多一个模式切换按钮。
 */
public class MetalFormerScreen extends AbstractContainerScreen<MetalFormerMenu> {

    private static final Identifier TEXTURE = Identifier.parse("iuce:textures/gui/machine/macerator.png");
    private static final Identifier ARROW_ON = Identifier.parse("iuce:textures/gui/com/arrow_on.png");
    private static final Identifier POWER_ON = Identifier.parse("iuce:textures/gui/com/power_on.png");

    /** 模式按钮（相对 GUI 左上角），18×18 */
    private static final int MODE_X = 106;
    private static final int MODE_Y = 38;
    private static final int MODE_SIZE = 18;

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

        super.extractContents(graphics, mouseX, mouseY, a);

        drawProgressArrow(graphics);
        drawPowerOn(graphics);
        drawModeButton(graphics, mouseX, mouseY);
    }

    private int currentMode() {
        int mode = menu.getMode();
        return (mode >= 0 && mode < MetalFormerBlockEntity.MODE_COUNT) ? mode : 0;
    }

    private boolean isOverModeButton(double mouseX, double mouseY) {
        double rx = mouseX - this.leftPos;
        double ry = mouseY - this.topPos;
        return rx >= MODE_X && rx < MODE_X + MODE_SIZE && ry >= MODE_Y && ry < MODE_Y + MODE_SIZE;
    }

    private static ItemStack modeIcon(int mode) {
        return switch (mode) {
            case 1 -> new ItemStack(ModBlockItems.COPPER_CABLE.get());
            case 2 -> new ItemStack(ModItems.IRON_STICK.get());
            default -> new ItemStack(ModItems.IRON_PLATE.get());
        };
    }

    private void drawModeButton(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = this.leftPos + MODE_X;
        int y = this.topPos + MODE_Y;
        boolean hover = isOverModeButton(mouseX, mouseY);
        int mode = currentMode();

        graphics.fill(x, y, x + MODE_SIZE, y + MODE_SIZE, BTN_BORDER);
        graphics.fill(x + 1, y + 1, x + MODE_SIZE - 1, y + MODE_SIZE - 1, hover ? BTN_FACE_HOVER : BTN_FACE);
        graphics.fill(x + 1, y + MODE_SIZE - 2, x + MODE_SIZE - 1, y + MODE_SIZE - 1, BTN_SHADE);
        graphics.fill(x + MODE_SIZE - 2, y + 1, x + MODE_SIZE - 1, y + MODE_SIZE - 1, BTN_SHADE);
        graphics.item(modeIcon(mode), x + 1, y + 1);

        if (hover) {
            graphics.setTooltipForNextFrame(
                Component.literal("模式：" + MetalFormerBlockEntity.MODE_NAMES[mode] + "（点击切换）"),
                mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isOverModeButton(event.x(), event.y())
                && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MetalFormerMenu.BUTTON_CYCLE_MODE);
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
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
