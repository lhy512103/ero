package machine.metal_former;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import machine.SingleInputMachineBlockEntity;
import machine.metal_former.menu.MetalFormerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 金属成型机：三种加工模式，各自对应一组配方（machine_type）：
 * 辊压 metal_former_rolling（锭 → 板）、切割 metal_former_cutting（板 → 导线）、
 * 挤压 metal_former_extruding（锭 → 棍）。在 GUI 中点击模式按钮切换。LV，缓冲 2000 EU。
 */
public class MetalFormerBlockEntity extends SingleInputMachineBlockEntity {

    public static final String[] MODE_RECIPE_TYPES = {
        "metal_former_rolling", "metal_former_cutting", "metal_former_extruding"
    };
    public static final String[] MODE_NAMES = {"辊压", "切割", "挤压"};
    public static final int MODE_COUNT = MODE_RECIPE_TYPES.length;

    /** 当前加工模式：0 辊压 / 1 切割 / 2 挤压 */
    private int mode = 0;

    public MetalFormerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METAL_FORMER.get(), pos, state, EUVoltage.LV, 1, 4, 2000, 200);
    }

    @Override protected String recipeType() { return MODE_RECIPE_TYPES[mode]; }

    @Override public String getMachineName() { return "金属成型机"; }

    public int getMode() { return mode; }

    /** 切换到下一个加工模式，并清空当前进度与配方缓存 */
    public void cycleMode() { setMode((mode + 1) % MODE_COUNT); }

    public void setMode(int newMode) {
        if (newMode < 0 || newMode >= MODE_COUNT || newMode == mode) return;
        mode = newMode;
        resetProcessing();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MetalFormerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("mode", mode);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int saved = input.getIntOr("mode", 0);
        mode = saved >= 0 && saved < MODE_COUNT ? saved : 0;
    }
}
