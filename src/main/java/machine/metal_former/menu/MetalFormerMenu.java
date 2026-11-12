package machine.metal_former.menu;

import init.ModMenus;
import machine.SingleInputMachineMenu;
import machine.metal_former.MetalFormerBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

public class MetalFormerMenu extends SingleInputMachineMenu<MetalFormerBlockEntity> {

    /** clickMenuButton 的按钮 ID：切换加工模式 */
    public static final int BUTTON_CYCLE_MODE = 0;

    private int mode;

    public MetalFormerMenu(int containerId, Inventory playerInv, MetalFormerBlockEntity blockEntity) {
        super(ModMenus.METAL_FORMER.get(), containerId, playerInv, blockEntity);
    }

    @Override protected int extraDataCount() { return 1; }
    @Override protected int getExtraData(int index) { return blockEntity.getMode(); }
    @Override protected void setExtraData(int index, int value) { mode = value; }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId == BUTTON_CYCLE_MODE) {
            blockEntity.cycleMode();
            return true;
        }
        return super.clickMenuButton(player, buttonId);
    }

    public int getMode() { return mode; }
}
