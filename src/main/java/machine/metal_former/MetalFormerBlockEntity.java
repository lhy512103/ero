package machine.metal_former;

import blockentity.ModBlockEntities;
import energy.EUVoltage;
import item.MoldItem;
import machine.SingleInputMachineBlockEntity;
import machine.metal_former.menu.MetalFormerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * 金属成型机：放进去的模具决定加工方式，各自对应一组配方（machine_type）：
 * 板材模具 metal_former_rolling（锭 → 板）、导线模具 metal_former_cutting（板 → 导线）、
 * 棍材模具 metal_former_extruding（锭 → 棍）。没有模具时不工作。LV，缓冲 2000 EU。
 * <p>
 * 模具槽排在升级槽之后，不对漏斗和管道开放。旧存档里保存的是模式编号，读档时会换成对应的模具。
 * </p>
 */
public class MetalFormerBlockEntity extends SingleInputMachineBlockEntity {

    public static final String[] MODE_RECIPE_TYPES = {
        "metal_former_rolling", "metal_former_cutting", "metal_former_extruding"
    };
    public static final int MODE_COUNT = MODE_RECIPE_TYPES.length;
    /** 输入 0、输出 1、升级 2–5、模具 6 */
    public static final int MOLD_SLOT = 6;

    private boolean loading;

    public MetalFormerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.METAL_FORMER.get(), pos, state, EUVoltage.LV, 1, 4, 2000, 200, 1);
    }

    /** 当前模具对应的模式：0 辊压 / 1 切割 / 2 挤压；没有模具时为 -1 */
    public int getMode() {
        if (itemHandler.getAmountAsInt(MOLD_SLOT) <= 0) return -1;
        return itemHandler.getResource(MOLD_SLOT).getItem() instanceof MoldItem mold ? mold.mode() : -1;
    }

    @Override
    protected String recipeType() {
        int mode = getMode();
        return mode < 0 ? "metal_former_no_mold" : MODE_RECIPE_TYPES[mode];
    }

    /** 换模具会清空当前进度与配方缓存 */
    @Override
    protected void slotChanged(int index) {
        if (index == MOLD_SLOT && !loading) resetProcessing();
    }

    @Override public String getMachineName() { return "金属成型机"; }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MetalFormerMenu(containerId, playerInventory, this);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // 旧存档只记了模式编号：补上对应的模具，原有产线照常运转
        int legacyMode = input.getIntOr("mode", -1);
        Item mold = MoldItem.forMode(legacyMode);
        if (mold != null && itemHandler.getAmountAsInt(MOLD_SLOT) == 0) {
            loading = true;
            itemHandler.set(MOLD_SLOT, ItemResource.of(new net.minecraft.world.item.ItemStack(mold)), 1);
            loading = false;
        }
    }
}
